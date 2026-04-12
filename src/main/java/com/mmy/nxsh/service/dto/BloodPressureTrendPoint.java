package com.mmy.nxsh.service.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class BloodPressureTrendPoint {
    private LocalDate day;
    private Integer avgSys;
    private Integer avgDia;
    private Integer maxSys;
    private Integer minSys;
    private Integer maxDia;
    private Integer minDia;
    private Long count;
}
