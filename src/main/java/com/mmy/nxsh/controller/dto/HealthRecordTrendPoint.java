package com.mmy.nxsh.controller.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class HealthRecordTrendPoint {
    private LocalDate day;
    private Integer avgSys;
    private Integer avgDia;
    private Integer maxSys;
    private Integer minSys;
    private Integer maxDia;
    private Integer minDia;
    private Long count;
}
