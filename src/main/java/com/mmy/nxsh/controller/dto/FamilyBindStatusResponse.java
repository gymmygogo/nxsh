package com.mmy.nxsh.controller.dto;

import lombok.Data;

@Data
public class FamilyBindStatusResponse {
    private boolean bound;
    private Long elderlyId;
    private String elderlyPhone;
    private String relationName;
}
