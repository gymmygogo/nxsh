package com.mmy.nxsh.task;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmy.nxsh.entity.FamilyElderlyBind;
import com.mmy.nxsh.entity.UserElderly;
import com.mmy.nxsh.mapper.FamilyElderlyBindMapper;
import com.mmy.nxsh.mapper.UserElderlyMapper;
import com.mmy.nxsh.service.impl.ElderlyServiceImpl;
import com.mmy.nxsh.websocket.FamilyWebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
public class GuardianAlertTask {

    @Value("${nxsh.tasks.guardian-alert.enabled:false}")
    private boolean enabled;

    private final UserElderlyMapper userElderlyMapper;
    private final FamilyElderlyBindMapper familyElderlyBindMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public GuardianAlertTask(UserElderlyMapper userElderlyMapper,
                              FamilyElderlyBindMapper familyElderlyBindMapper,
                              StringRedisTemplate stringRedisTemplate) {
        this.userElderlyMapper = userElderlyMapper;
        this.familyElderlyBindMapper = familyElderlyBindMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 每10分钟检查一次：开启守护的老人是否超过阈值未活跃
     */
    @Scheduled(cron = "0 */10 * * * ?")
    public void checkInactiveElderly() {
        if (!enabled) {
            return;
        }

        // 查询所有开启守护的老人
        List<UserElderly> elders = userElderlyMapper.selectList(
                new LambdaQueryWrapper<UserElderly>()
                        .eq(UserElderly::getGuardianActive, 1)
                        .eq(UserElderly::getIsDeleted, 0)
                        .eq(UserElderly::getStatus, 1));

        long now = System.currentTimeMillis();

        for (UserElderly elder : elders) {
            try {
                // 检查当前时间是否在该老人的免打扰时段内
                if (isInDndHours(elder)) {
                    continue;
                }

                // 优先从 Redis 读取心跳时间戳
                Long lastActiveMs = getLastActiveFromRedis(elder.getId());

                // Redis 没有则回退到 MySQL
                if (lastActiveMs == null && elder.getLastActiveTime() != null) {
                    lastActiveMs = elder.getLastActiveTime()
                            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                }

                // 如果从未有活跃记录，跳过（刚注册未使用）
                if (lastActiveMs == null) {
                    continue;
                }

                int thresholdHours = elder.getGuardianThreshold() != null ? elder.getGuardianThreshold() : 24;
                long thresholdMs = thresholdHours * 3600_000L;

                if (now - lastActiveMs > thresholdMs) {
                    // 超时未活跃，向家属推送预警
                    notifyFamily(elder, thresholdHours);
                    log.info("守护预警: elderlyId={}, 超过{}小时未活跃", elder.getId(), thresholdHours);
                }
            } catch (Exception e) {
                log.warn("守护检测异常: elderlyId={}, error={}", elder.getId(), e.getMessage());
            }
        }
    }

    private Long getLastActiveFromRedis(Long elderlyId) {
        String tsStr = stringRedisTemplate.opsForValue().get(ElderlyServiceImpl.HEARTBEAT_PREFIX + elderlyId);
        if (tsStr != null && !tsStr.isBlank()) {
            try {
                return Long.parseLong(tsStr);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private boolean isInDndHours(UserElderly elder) {
        LocalTime dndStart = elder.getDndStartTime() != null ? elder.getDndStartTime() : LocalTime.of(22, 0);
        LocalTime dndEnd = elder.getDndEndTime() != null ? elder.getDndEndTime() : LocalTime.of(6, 0);
        LocalTime nowTime = LocalTime.now();

        if (dndStart.isAfter(dndEnd)) {
            // 跨日，如 22:00 ~ 06:00
            return !nowTime.isBefore(dndStart) || nowTime.isBefore(dndEnd);
        } else {
            return !nowTime.isBefore(dndStart) && nowTime.isBefore(dndEnd);
        }
    }

    private void notifyFamily(UserElderly elder, int thresholdHours) {
        List<FamilyElderlyBind> binds = familyElderlyBindMapper.selectList(
                new LambdaQueryWrapper<FamilyElderlyBind>()
                        .eq(FamilyElderlyBind::getElderlyId, elder.getId()));

        String elderName = elder.getNickname() != null ? elder.getNickname() : elder.getName();
        for (FamilyElderlyBind bind : binds) {
            JSONObject msg = JSONUtil.createObj()
                    .set("type", "GUARDIAN_ALERT")
                    .set("elderlyId", elder.getId())
                    .set("elderlyName", elderName)
                    .set("thresholdHours", thresholdHours)
                    .set("msg", elderName + "已超过" + thresholdHours + "小时未操作，请及时关注");
            FamilyWebSocketServer.sendMessage(bind.getFamilyId(), msg.toString());
        }
    }
}
