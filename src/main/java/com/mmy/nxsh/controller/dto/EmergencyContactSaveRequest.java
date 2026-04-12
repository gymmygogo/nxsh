package com.mmy.nxsh.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EmergencyContactSaveRequest {
    @NotNull
    private Long familyId;
    @NotNull
    private Long elderlyId;
    @NotBlank
    private String name;
    @NotBlank
    private String phone;
    private String relationship;
    @NotNull
    private Integer priority;
}
