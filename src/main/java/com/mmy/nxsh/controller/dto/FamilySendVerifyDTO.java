package com.mmy.nxsh.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FamilySendVerifyDTO {
    @NotBlank(message = "老人手机号不能为空")
    private String elderlyPhone;
}
