package com.mmy.nxsh.controller.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class LastLocationDTO {
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String address;
    private String scene;
    private LocalDateTime logTime;
}
