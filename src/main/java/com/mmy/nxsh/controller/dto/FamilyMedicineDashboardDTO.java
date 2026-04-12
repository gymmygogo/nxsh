package com.mmy.nxsh.controller.dto;

import lombok.Data;

import java.util.List;

@Data
public class FamilyMedicineDashboardDTO {
    private Long elderlyId;
    // 今日所有药品的服用状态列表
    private List<FamilyMedicineItemStatusDTO> medicines;
    // 是否有低库存药品，前端用来决定是否显示红色缺药标签
    private Boolean hasLowStock;
}
