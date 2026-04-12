package com.mmy.nxsh.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mmy.nxsh.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("medicine_plan")
public class MedicinePlan extends BaseEntity {
    private Long medicineId;
    private Long elderlyId;
    private LocalTime takeTime;
    // 用药频率类型：once(单次), daily(每日), weekly(每周), interval(间隔天数
    private Integer frequencyType;
    // 用药频率值：如每周几(1-7)，间隔天数等
    private Integer frequencyValue;
    // 星期几用药，用逗号分隔，如"1,3,5"表示周一、三、五用药
    private String weekDays;
}