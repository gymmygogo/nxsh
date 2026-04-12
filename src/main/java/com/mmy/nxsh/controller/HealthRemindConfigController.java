package com.mmy.nxsh.controller;

import com.mmy.nxsh.common.ApiResponse;
import com.mmy.nxsh.controller.dto.HealthRemindConfigRequest;
import com.mmy.nxsh.entity.HealthRemindConfig;
import com.mmy.nxsh.service.HealthRemindConfigService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/health/remind")
public class HealthRemindConfigController {

    private final HealthRemindConfigService healthRemindConfigService;

    public HealthRemindConfigController(HealthRemindConfigService healthRemindConfigService) {
        this.healthRemindConfigService = healthRemindConfigService;
    }

    @PostMapping("/config")
    public ApiResponse<HealthRemindConfig> save(@RequestBody HealthRemindConfigRequest req) {
        boolean active = req.getActive() == null || req.getActive();
        return ApiResponse.success(
                healthRemindConfigService.saveOrUpdateConfig(req.getElderlyId(), req.getRemindTime(), active));
    }

    @GetMapping("/configs")
    public ApiResponse<List<HealthRemindConfig>> list(@RequestParam Long elderlyId) {
        return ApiResponse.success(healthRemindConfigService.listConfigs(elderlyId));
    }
}
