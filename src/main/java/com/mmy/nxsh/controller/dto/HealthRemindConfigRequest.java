package com.mmy.nxsh.controller.dto;

import lombok.Data;

import java.time.LocalTime;

@Data
public class HealthRemindConfigRequest {
    private Long elderlyId;
    private LocalTime remindTime;
    private Boolean active;
}
