package com.mmy.nxsh.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mmy.nxsh.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("emergency_contact")
public class EmergencyContact extends BaseEntity {
    private Long elderlyId;
    private String name;
    private String phone;
    private String relationship;
    private Integer priority;
}