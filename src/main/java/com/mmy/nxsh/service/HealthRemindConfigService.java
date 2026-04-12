package com.mmy.nxsh.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mmy.nxsh.entity.HealthRemindConfig;

import java.time.LocalTime;
import java.util.List;

public interface HealthRemindConfigService extends IService<HealthRemindConfig> {

    HealthRemindConfig saveOrUpdateConfig(Long elderlyId, LocalTime remindTime, boolean active);

    List<HealthRemindConfig> listConfigs(Long elderlyId);
}
