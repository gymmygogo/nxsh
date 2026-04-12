package com.mmy.nxsh.controller;

import com.mmy.nxsh.common.ApiResponse;
import com.mmy.nxsh.controller.dto.DueMedicineGroupDTO;
import com.mmy.nxsh.controller.dto.FamilyMedicineDashboardDTO;
import com.mmy.nxsh.controller.dto.MarkMedicineTakenRequest;
import com.mmy.nxsh.controller.dto.SaveMedicineRequest;
import com.mmy.nxsh.controller.dto.SnoozeRequest;
import com.mmy.nxsh.service.MedicineService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/medicine")
public class MedicineController {

    private final MedicineService medicineService;

    public MedicineController(MedicineService medicineService) {
        this.medicineService = medicineService;
    }

    @GetMapping("/due")
    public ApiResponse<List<DueMedicineGroupDTO>> getDueMedicines(@RequestParam Long elderlyId,
                                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime time) {
        return ApiResponse.success(medicineService.getDueMedicines(elderlyId, time));
    }

    @PostMapping("/take")
    public ApiResponse<Void> markTaken(@RequestBody MarkMedicineTakenRequest request) {
        medicineService.markTaken(request.getElderlyId(), request.getPlanTime(), request.getMedicineIds());
        return ApiResponse.success(null);
    }

    // 稍后提醒（10分钟后），返回新的提醒时间
    @PostMapping("/snooze")
    public ApiResponse<LocalDateTime> snooze(@RequestBody SnoozeRequest request) {
        LocalDateTime nextRemindTime = medicineService.snooze(
                request.getElderlyId(), request.getPlanTime(), request.getMedicineIds());
        return ApiResponse.success(nextRemindTime);
    }

    // 家属端：保存完整药品配置（4步流程）
    @PostMapping("/family/save")
    public ApiResponse<Long> saveMedicine(@RequestBody SaveMedicineRequest req) {
        return ApiResponse.success(medicineService.saveMedicine(req));
    }

    // 家属端首页看板：今日服药状态 + 低库存预警
    @GetMapping("/family/dashboard")
    public ApiResponse<FamilyMedicineDashboardDTO> getFamilyDashboard(
            @RequestParam Long elderlyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate queryDate = date != null ? date : LocalDate.now();
        return ApiResponse.success(medicineService.getFamilyDashboard(elderlyId, queryDate));
    }

    // 上传亲情语音文件（第3步），subDir传 "voice" 或 "photo"
    @PostMapping("/family/upload")
    public ApiResponse<String> uploadFile(@RequestParam MultipartFile file,
                                          @RequestParam(defaultValue = "voice") String subDir) {
        return ApiResponse.success(medicineService.uploadFile(file, subDir));
    }
}


