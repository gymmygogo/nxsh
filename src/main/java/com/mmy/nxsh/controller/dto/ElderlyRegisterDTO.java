package com.mmy.nxsh.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ElderlyRegisterDTO {
    @NotBlank(message = "手机号不能为空")
    private String phone;

    @NotBlank(message = "姓名不能为空")
    private String name;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "验证码不能为空")
    private String verifyCode;

    private Integer gender;

    private Integer age;

    private BigDecimal height;

    private BigDecimal weight;

    private String chronicDiseases;

    private String nickname;
}
