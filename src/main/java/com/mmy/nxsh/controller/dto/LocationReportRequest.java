package com.mmy.nxsh.controller.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LocationReportRequest {
    @NotNull
    private Long elderlyId;
    @NotNull
    private BigDecimal latitude;
    @NotNull
    private BigDecimal longitude;
    private String address;
}
