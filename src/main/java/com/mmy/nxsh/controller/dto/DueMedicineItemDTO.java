package com.mmy.nxsh.controller.dto;

import lombok.Data;

@Data
public class DueMedicineItemDTO {
    private Long medicineId;
    private String name;
    private String dosageDesc;
    private String photoUrl;
    private Integer currentStock;
    private Integer lowStockThreshold;
    private String voiceRemindUrl;
}

