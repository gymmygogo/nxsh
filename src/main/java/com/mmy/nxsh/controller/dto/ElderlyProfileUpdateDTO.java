package com.mmy.nxsh.controller.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ElderlyProfileUpdateDTO {
    private Long elderlyId;
    private Integer gender;
    private Integer age;
    private BigDecimal height;
    private BigDecimal weight;
    private String chronicDiseases;
}
