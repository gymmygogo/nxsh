package com.mmy.nxsh.controller.dto;

import lombok.Data;

@Data
public class FamilyLoginResponse {
    private String token;
    private Long familyId;
    // 新增：绑定的老人ID
    private Long elderlyId;
    private Boolean hasBound; // 新增：是否已绑定老人
}
