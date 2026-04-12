package com.mmy.nxsh.controller.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DueMedicineGroupDTO {
    private LocalDateTime planTime;
    private List<DueMedicineItemDTO> medicines;
    // "family" 或 "standard"
    private String remindMode;
    // 亲情模式下家属录音地址
    private String familyVoiceUrl;
}
