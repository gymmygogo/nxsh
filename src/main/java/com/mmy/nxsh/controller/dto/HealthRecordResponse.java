package com.mmy.nxsh.controller.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HealthRecordResponse {
    private Long id;
    private Long elderlyId;

    // 0=正常 1=偏高 2=偏低
    private Integer status;

    private Integer sys;
    private Integer dia;

    private String deviceId;
    private LocalDateTime recordTime;
    private String note;
}
