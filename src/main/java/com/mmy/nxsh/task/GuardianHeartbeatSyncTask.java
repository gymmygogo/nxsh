package com.mmy.nxsh.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmy.nxsh.entity.UserElderly;
import com.mmy.nxsh.mapper.UserElderlyMapper;
import com.mmy.nxsh.service.impl.ElderlyServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;

@Slf4j
@Component
public class GuardianHeartbeatSyncTask {

    @Value("${nxsh.tasks.guardian-heartbeat-sync.enabled:false}")
    private boolean enabled;

    private final StringRedisTemplate stringRedisTemplate;
    private final UserElderlyMapper userElderlyMapper;

    public GuardianHeartbeatSyncTask(StringRedisTemplate stringRedisTemplate,
                                      UserElderlyMapper userElderlyMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.userElderlyMapper = userElderlyMapper;
    }

    /**
     * 每5分钟将 Redis 中的心跳时间戳异步同步到 MySQL user_elderly.last_active_time
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void syncHeartbeatToMySQL() {
        if (!enabled) {
            return;
        }

        Set<String> keys = stringRedisTemplate.keys(ElderlyServiceImpl.HEARTBEAT_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return;
        }

        for (String key : keys) {
            try {
                String elderlyIdStr = key.substring(ElderlyServiceImpl.HEARTBEAT_PREFIX.length());
                Long elderlyId = Long.parseLong(elderlyIdStr);
                String tsStr = stringRedisTemplate.opsForValue().get(key);
                if (tsStr == null || tsStr.isBlank()) {
                    continue;
                }

                long ts = Long.parseLong(tsStr);
                LocalDateTime lastActive = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault());

                UserElderly elderly = new UserElderly();
                elderly.setId(elderlyId);
                elderly.setLastActiveTime(lastActive);
                userElderlyMapper.updateById(elderly);

                log.debug("同步心跳: elderlyId={}, lastActive={}", elderlyId, lastActive);
            } catch (Exception e) {
                log.warn("同步心跳失败: key={}, error={}", key, e.getMessage());
            }
        }
    }
}
