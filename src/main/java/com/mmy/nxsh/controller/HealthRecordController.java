package com.mmy.nxsh.controller;

import com.mmy.nxsh.common.ApiResponse;
import com.mmy.nxsh.controller.dto.HealthRecordRequest;
import com.mmy.nxsh.controller.dto.HealthRecordResponse;
import com.mmy.nxsh.service.HealthRecordService;
import com.mmy.nxsh.service.dto.BloodPressureTrendPoint;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/health")
public class HealthRecordController {

    private final HealthRecordService healthRecordService;

    public HealthRecordController(HealthRecordService healthRecordService) {
        this.healthRecordService = healthRecordService;
    }

    @PostMapping("/record")
    public ApiResponse<HealthRecordResponse> record(@RequestBody HealthRecordRequest request) {
        log.info("==> /api/health/record 收到请求: elderlyId={}, pressType={}, sys={}, dia={}, recordTime={}",
                request.getElderlyId(), request.getPressType(), request.getSys(), request.getDia(), request.getRecordTime());
        return ApiResponse.success(healthRecordService.record(request));
    }

    /**
     * 家属端：血压历史记录（分页）
     */
    @GetMapping("/records")
    public ApiResponse<List<com.mmy.nxsh.entity.HealthRecord>> list(
            @RequestParam Long elderlyId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(healthRecordService.listBloodPressureRecords(elderlyId, page, size));
    }

    /**
     * 家属端：血压趋势（按天）
     */
    @GetMapping("/records/trend")
    public ApiResponse<List<BloodPressureTrendPoint>> trend(
            @RequestParam Long elderlyId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to
    ) {
        return ApiResponse.success(healthRecordService.trendDaily(elderlyId, from, to));
    }

    /**
     * 家属端：参考范围（先固定返回）
     */
    @GetMapping("/records/range")
    public ApiResponse<Map<String, Integer>> range() {
        Map<String, Integer> r = new HashMap<>();
        r.put("sysLow", 90);
        r.put("sysHigh", 140);
        r.put("diaLow", 60);
        r.put("diaHigh", 90);
        return ApiResponse.success(r);
    }
}
