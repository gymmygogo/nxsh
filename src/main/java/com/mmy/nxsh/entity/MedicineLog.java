package com.mmy.nxsh.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mmy.nxsh.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("medicine_log")
public class MedicineLog extends BaseEntity {
    private Long medicineId;
    private Long elderlyId;
    private LocalDateTime planTime;
    private LocalDateTime takeTime;
    private Integer status;
}