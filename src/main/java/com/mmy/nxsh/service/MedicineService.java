package com.mmy.nxsh.service;

import com.mmy.nxsh.controller.dto.DueMedicineGroupDTO;
import com.mmy.nxsh.controller.dto.FamilyMedicineDashboardDTO;
import com.mmy.nxsh.controller.dto.SaveMedicineRequest;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface MedicineService {
    List<DueMedicineGroupDTO> getDueMedicines(Long elderlyId, LocalDateTime time, Integer windowMinutes);

    void markTaken(Long elderlyId, LocalDateTime planTime, List<Long> medicineIds);

    // 稍后提醒，返回新的提醒时间
    LocalDateTime snooze(Long elderlyId, LocalDateTime planTime, List<Long> medicineIds);

    // 家属端：保存完整药品配置（4步流程）
    Long saveMedicine(SaveMedicineRequest req);

    // 家属端首页看板：今日服药状态 + 低库存预警
    FamilyMedicineDashboardDTO getFamilyDashboard(Long elderlyId, LocalDate date);

    // 上传亲情语音或药品照片，返回可访问URL
    String uploadFile(MultipartFile file, String subDir);
}
