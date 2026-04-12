package com.mmy.nxsh.controller.dto;

import lombok.Data;

@Data
public class FamilyLoginResponse {
    private String token;
    private Long familyId;
}
