package com.mmy.nxsh.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mmy.nxsh.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("health_remind_config")
public class HealthRemindConfig extends BaseEntity {
    private Long elderlyId;
    private Integer type;
    private LocalTime remindTime;
    private String voiceUrl;
    private Integer isActive;
}