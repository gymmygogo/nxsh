package com.mmy.nxsh.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mmy.nxsh.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("location_log")
public class LocationLog extends BaseEntity {
    /** SHARE 分享位置 / SOS 紧急 / ACTIVE 活跃上报（地图取三者中最近一次） */
    private String scene;
    private Long elderlyId;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String addressDetail;
    private String address;
    private LocalDateTime logTime;
    private Integer triggerType;
}