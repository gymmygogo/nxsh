package com.mmy.nxsh.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FamilyBindRequestDTO {
    @NotNull(message = "家属ID不能为空")
    private Long familyId;

    private Long elderlyId;

    @NotBlank(message = "老人手机号不能为空")
    private String elderlyPhone;

    @NotBlank(message = "验证码不能为空")
    private String verifyCode;

    @NotBlank(message = "称谓不能为空")
    private String relationName;

    private Integer isPrimary;
}
