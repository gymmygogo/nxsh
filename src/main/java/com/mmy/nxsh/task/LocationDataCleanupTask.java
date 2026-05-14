package com.mmy.nxsh.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmy.nxsh.entity.LocationLog;
import com.mmy.nxsh.mapper.LocationLogMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class LocationDataCleanupTask {

    @Value("${nxsh.tasks.location-data-cleanup.enabled:false}")
    private boolean enabled;

    private final LocationLogMapper locationLogMapper;

    public LocationDataCleanupTask(LocationLogMapper locationLogMapper) {
        this.locationLogMapper = locationLogMapper;
    }

    @Scheduled(cron = "0 0 4 * * ?")
    public void cleanOldLocationLogs() {
        if (!enabled) {
            return;
        }

        LocalDateTime before = LocalDateTime.now().minusDays(7);
        locationLogMapper.delete(
                new LambdaQueryWrapper<LocationLog>().lt(LocationLog::getLogTime, before));
    }
}
