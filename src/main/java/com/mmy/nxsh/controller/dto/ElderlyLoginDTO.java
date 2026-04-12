package com.mmy.nxsh.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ElderlyLoginDTO {
    @NotBlank(message = "手机号不能为空")
    private String phone;

    @NotBlank(message = "密码不能为空")
    private String password;
}

