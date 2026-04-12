package com.mmy.nxsh.controller.dto;

import lombok.Data;

@Data
public class FamilyMedicineItemStatusDTO {
    private Long medicineId;
    private String name;
    private String dosageDesc;
    private String photoUrl;
    private Integer currentStock;
    private Integer lowStockThreshold;
    // true = 当天已服用
    private Boolean taken;
    // 低库存预警标记
    private Boolean lowStock;
}
