package com.mmy.nxsh.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mmy.nxsh.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_elderly")
public class UserElderly extends BaseEntity {
    private String phone;
    private String name;
    private String nickname;
    private Integer gender;
    private Integer age;
    private BigDecimal height;
    private BigDecimal weight;
    private String chronicDiseases;
    private Integer guardianActive;
    private Integer guardianThreshold;
    private LocalDateTime lastActiveTime;
    private LocalTime dndStartTime;
    private LocalTime dndEndTime;
    private String password;
    private Integer status;
}