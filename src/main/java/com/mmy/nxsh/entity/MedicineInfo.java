package com.mmy.nxsh.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mmy.nxsh.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("medicine_info")
public class MedicineInfo extends BaseEntity {
    private Long elderlyId;
    private String name;
    private String dosageDesc;
    private String photoUrl;
    private Integer currentStock;
    private Integer lowStockThreshold;
    private String voiceRemindUrl;
    private String familyVoiceUrl;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer isActive;
    // 乐观锁版本号，用于并发扣减库存时的冲突检测
    private Integer version;
}