package com.mmy.nxsh.task;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmy.nxsh.entity.FamilyElderlyBind;
import com.mmy.nxsh.entity.HealthRemindConfig;
import com.mmy.nxsh.entity.UserElderly;
import com.mmy.nxsh.mapper.FamilyElderlyBindMapper;
import com.mmy.nxsh.mapper.HealthRemindConfigMapper;
import com.mmy.nxsh.mapper.UserElderlyMapper;
import com.mmy.nxsh.websocket.ElderlyWebSocketServer;
import com.mmy.nxsh.websocket.FamilyWebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.List;

@Slf4j
@Component
public class BloodPressureReminderTask {

    private final HealthRemindConfigMapper healthRemindConfigMapper;
    private final UserElderlyMapper userElderlyMapper;
    private final FamilyElderlyBindMapper familyElderlyBindMapper;

    public BloodPressureReminderTask(HealthRemindConfigMapper healthRemindConfigMapper,
                                      UserElderlyMapper userElderlyMapper,
                                      FamilyElderlyBindMapper familyElderlyBindMapper) {
        this.healthRemindConfigMapper = healthRemindConfigMapper;
        this.userElderlyMapper = userElderlyMapper;
        this.familyElderlyBindMapper = familyElderlyBindMapper;
    }

    /**
     * 每分钟检查一次：是否有到达提醒时间的血压测量提醒
     */
    @Scheduled(cron = "0 * * * * ?")
    public void checkBloodPressureReminders() {
        LocalTime now = LocalTime.now().withSecond(0).withNano(0);

        // 查询当前分钟匹配的、已激活的血压提醒配置
        List<HealthRemindConfig> configs = healthRemindConfigMapper.selectList(
                new LambdaQueryWrapper<HealthRemindConfig>()
                        .eq(HealthRemindConfig::getType, 1)  // 1=血压
                        .eq(HealthRemindConfig::getIsActive, 1)
                        .eq(HealthRemindConfig::getRemindTime, now));

        for (HealthRemindConfig config : configs) {
            try {
                UserElderly elder = userElderlyMapper.selectById(config.getElderlyId());
                if (elder == null || elder.getStatus() == null || elder.getStatus() != 1) {
                    continue;
                }

                // 检查免打扰时段
                if (isInDndHours(elder, now)) {
                    continue;
                }

                String elderName = elder.getNickname() != null ? elder.getNickname() : elder.getName();

                // 推送给老人端
                JSONObject elderlyMsg = JSONUtil.createObj()
                        .set("type", "BP_REMINDER")
                        .set("elderlyId", elder.getId())
                        .set("remindTime", now.toString())
                        .set("msg", elderName + "，时间到了，请测量您的血压");
                ElderlyWebSocketServer.sendMessage(elder.getId(), elderlyMsg.toString());

                // 推送给家属端
                notifyFamily(elder.getId(), elderName, now);

                log.info("血压提醒推送: elderlyId={}, time={}", elder.getId(), now);
            } catch (Exception e) {
                log.warn("血压提醒异常: configId={}, error={}", config.getId(), e.getMessage());
            }
        }
    }

    private boolean isInDndHours(UserElderly elder, LocalTime now) {
        LocalTime dndStart = elder.getDndStartTime() != null ? elder.getDndStartTime() : LocalTime.of(22, 0);
        LocalTime dndEnd = elder.getDndEndTime() != null ? elder.getDndEndTime() : LocalTime.of(6, 0);

        if (dndStart.isAfter(dndEnd)) {
            return !now.isBefore(dndStart) || now.isBefore(dndEnd);
        } else {
            return !now.isBefore(dndStart) && now.isBefore(dndEnd);
        }
    }

    private void notifyFamily(Long elderlyId, String elderName, LocalTime remindTime) {
        List<FamilyElderlyBind> binds = familyElderlyBindMapper.selectList(
                new LambdaQueryWrapper<FamilyElderlyBind>()
                        .eq(FamilyElderlyBind::getElderlyId, elderlyId));

        for (FamilyElderlyBind bind : binds) {
            JSONObject msg = JSONUtil.createObj()
                    .set("type", "BP_REMINDER")
                    .set("elderlyId", elderlyId)
                    .set("elderlyName", elderName)
                    .set("remindTime", remindTime.toString())
                    .set("msg", elderName + "的血压测量时间到了");
            FamilyWebSocketServer.sendMessage(bind.getFamilyId(), msg.toString());
        }
    }
}
