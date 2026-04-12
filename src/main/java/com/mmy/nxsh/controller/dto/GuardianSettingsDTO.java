package com.mmy.nxsh.controller.dto;

import lombok.Data;

import java.time.LocalTime;

@Data
public class GuardianSettingsDTO {
    private Long elderlyId;
    private Integer guardianActive;      // 0-关闭 1-开启
    private Integer guardianThreshold;   // 预警阈值（小时）：12/24/48
    private LocalTime dndStartTime;      // 免打扰开始
    private LocalTime dndEndTime;        // 免打扰结束
}
