package com.mmy.nxsh.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mmy.nxsh.controller.dto.HealthRecordRequest;
import com.mmy.nxsh.controller.dto.HealthRecordResponse;
import com.mmy.nxsh.entity.HealthRecord;
import com.mmy.nxsh.service.dto.BloodPressureTrendPoint;

import java.time.LocalDate;
import java.util.List;

public interface HealthRecordService extends IService<HealthRecord> {
    HealthRecordResponse record(HealthRecordRequest request);

    List<HealthRecord> listBloodPressureRecords(Long elderlyId, int page, int size);

    List<BloodPressureTrendPoint> trendDaily(Long elderlyId, LocalDate from, LocalDate to);
}
