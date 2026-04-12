package com.mmy.nxsh.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mmy.nxsh.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("health_record")
public class HealthRecord extends BaseEntity {
    private Long elderlyId;
    // 1=血压 2=血糖
    private Integer type;
    // 0=正常 1=偏高 2=偏低
    private Integer status;
    // 血压收缩压/舒张压
    // 在你的 HealthRecord 实体类里加上这个注解
    private Integer sys;
    private Integer dia;
    // 血糖值
    private BigDecimal glucose;
    private String deviceId;
    private LocalDateTime recordTime;
    private String note;
}