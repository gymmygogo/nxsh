package com.mmy.nxsh.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mmy.nxsh.controller.dto.HealthRecordRequest;
import com.mmy.nxsh.controller.dto.HealthRecordResponse;
import com.mmy.nxsh.entity.FamilyElderlyBind;
import com.mmy.nxsh.entity.HealthRecord;
import com.mmy.nxsh.mapper.FamilyElderlyBindMapper;
import com.mmy.nxsh.mapper.HealthRecordMapper;
import com.mmy.nxsh.service.HealthRecordService;
import com.mmy.nxsh.service.dto.BloodPressureTrendPoint;
import com.mmy.nxsh.websocket.FamilyWebSocketServer;
import cn.hutool.json.JSONUtil;
import cn.hutool.json.JSONObject;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class HealthRecordServiceImpl extends ServiceImpl<HealthRecordMapper, HealthRecord>
        implements HealthRecordService {

    private static final int TYPE_BP = 1;
    private static final int STATUS_NORMAL = 0;
    private static final int STATUS_HIGH = 1;
    private static final int STATUS_LOW = 2;

    private static final String PRESS_SHORT = "SHORT";
    private static final String PRESS_LONG = "LONG";
    private static final String PRESS_DOUBLE = "DOUBLE";

    private final FamilyElderlyBindMapper familyElderlyBindMapper;

    public HealthRecordServiceImpl(FamilyElderlyBindMapper familyElderlyBindMapper) {
        this.familyElderlyBindMapper = familyElderlyBindMapper;
    }

    @Override
    public HealthRecordResponse record(HealthRecordRequest request) {
        validateRequest(request);

        BloodPressure bp = resolveBloodPressure(request);

        HealthRecord record = new HealthRecord();
        record.setElderlyId(request.getElderlyId());
        record.setDeviceId(trimToNull(request.getDeviceId()));
        record.setSys(bp.sys);
        record.setDia(bp.dia);
        record.setRecordTime(request.getRecordTime() != null ? request.getRecordTime() : LocalDateTime.now());
        record.setNote(trimToNull(request.getNote()));
        record.setStatus(bp.status);
        record.setType(TYPE_BP);

        baseMapper.insert(record);

        notifyFamily(record);

        return toResponse(record);
    }

    @Override
    public List<HealthRecord> listBloodPressureRecords(Long elderlyId, int page, int size) {
        if (elderlyId == null) {
            throw new IllegalArgumentException("elderlyId不能为空");
        }
        if (page < 1) {
            page = 1;
        }
        if (size < 1 || size > 200) {
            size = 20;
        }

        LambdaQueryWrapper<HealthRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(HealthRecord::getElderlyId, elderlyId)
                .eq(HealthRecord::getType, TYPE_BP)
                .orderByDesc(HealthRecord::getRecordTime);

        IPage<HealthRecord> p = baseMapper.selectPage(new Page<>(page, size), qw);
        return p.getRecords();
    }

    @Override
    public List<BloodPressureTrendPoint> trendDaily(Long elderlyId, LocalDate from, LocalDate to) {
        if (elderlyId == null) {
            throw new IllegalArgumentException("elderlyId不能为空");
        }
        if (from == null || to == null) {
            throw new IllegalArgumentException("from/to不能为空");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from不能晚于to");
        }

        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();

        List<HealthRecord> list = baseMapper.selectList(new LambdaQueryWrapper<HealthRecord>()
                .eq(HealthRecord::getElderlyId, elderlyId)
                .eq(HealthRecord::getType, TYPE_BP)
                .ge(HealthRecord::getRecordTime, start)
                .lt(HealthRecord::getRecordTime, end)
                .orderByAsc(HealthRecord::getRecordTime));

        List<BloodPressureTrendPoint> result = new ArrayList<>();
        if (list.isEmpty()) {
            return result;
        }

        LocalDate current = null;
        List<HealthRecord> bucket = new ArrayList<>();
        for (HealthRecord r : list) {
            LocalDate day = r.getRecordTime().toLocalDate();
            if (current == null) {
                current = day;
            }
            if (!day.equals(current)) {
                result.add(aggregateDay(current, bucket));
                bucket.clear();
                current = day;
            }
            bucket.add(r);
        }
        result.add(aggregateDay(current, bucket));

        result.sort(Comparator.comparing(BloodPressureTrendPoint::getDay));
        return result;
    }

    private BloodPressureTrendPoint aggregateDay(LocalDate day, List<HealthRecord> bucket) {
        BloodPressureTrendPoint p = new BloodPressureTrendPoint();
        p.setDay(day);
        p.setCount((long) bucket.size());

        int sumSys = 0;
        int sumDia = 0;
        int maxSys = Integer.MIN_VALUE;
        int minSys = Integer.MAX_VALUE;
        int maxDia = Integer.MIN_VALUE;
        int minDia = Integer.MAX_VALUE;
        int validCount = 0;

        for (HealthRecord r : bucket) {
            Integer sys = r.getSys();
            Integer dia = r.getDia();
            if (sys == null || dia == null) {
                continue;
            }
            validCount++;
            sumSys += sys;
            sumDia += dia;
            maxSys = Math.max(maxSys, sys);
            minSys = Math.min(minSys, sys);
            maxDia = Math.max(maxDia, dia);
            minDia = Math.min(minDia, dia);
        }

        if (validCount == 0) {
            return p;
        }

        p.setAvgSys(sumSys / validCount);
        p.setAvgDia(sumDia / validCount);
        p.setMaxSys(maxSys);
        p.setMinSys(minSys);
        p.setMaxDia(maxDia);
        p.setMinDia(minDia);
        return p;
    }

    private void notifyFamily(HealthRecord record) {
        List<FamilyElderlyBind> binds = familyElderlyBindMapper.selectList(
                new LambdaQueryWrapper<FamilyElderlyBind>().eq(FamilyElderlyBind::getElderlyId, record.getElderlyId()));
        if (binds.isEmpty()) {
            return;
        }

        JSONObject refreshMsg = JSONUtil.createObj()
                .set("type", "HEALTH_REFRESH")
                .set("elderlyId", record.getElderlyId())
                .set("recordId", record.getId())
                .set("recordTime", record.getRecordTime())
                .set("msg", "老人已录入血压");

        String refreshText = refreshMsg.toString();
        for (FamilyElderlyBind bind : binds) {
            FamilyWebSocketServer.sendMessage(bind.getFamilyId(), refreshText);
        }

        if (record.getStatus() != null && record.getStatus() != STATUS_NORMAL) {
            JSONObject alertMsg = JSONUtil.createObj()
                    .set("type", "HEALTH_ALERT")
                    .set("elderlyId", record.getElderlyId())
                    .set("recordId", record.getId())
                    .set("status", record.getStatus())
                    .set("sys", record.getSys())
                    .set("dia", record.getDia())
                    .set("recordTime", record.getRecordTime())
                    .set("msg", "健康警报：老人血压异常");

            String alertText = alertMsg.toString();
            for (FamilyElderlyBind bind : binds) {
                FamilyWebSocketServer.sendMessage(bind.getFamilyId(), alertText);
            }
        }
    }

    private void validateRequest(HealthRecordRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求不能为空");
        }
        if (request.getElderlyId() == null) {
            throw new IllegalArgumentException("elderlyId不能为空");
        }

        boolean hasSys = request.getSys() != null;
        boolean hasDia = request.getDia() != null;
        boolean hasPressType = trimToNull(request.getPressType()) != null;

        if (!hasPressType && !(hasSys && hasDia)) {
            throw new IllegalArgumentException("请提供pressType，或同时提供sys和dia");
        }
        if (hasSys ^ hasDia) {
            throw new IllegalArgumentException("血压需同时提供sys和dia");
        }

        if (hasPressType) {
            String pressType = normalizePressType(request.getPressType());
            if (pressType == null) {
                throw new IllegalArgumentException("pressType只能是SHORT/LONG/DOUBLE或single/long/double");
            }
        }

        if (hasSys) {
            validateBloodPressure(request.getSys(), request.getDia());
        }
    }

    private BloodPressure resolveBloodPressure(HealthRecordRequest request) {
        if (request.getSys() != null && request.getDia() != null) {
            int status = calcBloodPressureStatus(request.getSys(), request.getDia());
            return new BloodPressure(request.getSys(), request.getDia(), status);
        }

        String pressType = normalizePressType(request.getPressType());
        if (pressType == null) {
            throw new IllegalArgumentException("pressType只能是SHORT/LONG/DOUBLE或single/long/double");
        }

        // 按需求：单击(这里用 SHORT) = 偏高，双击 = 偏低，长按 = 正常
        if (PRESS_SHORT.equals(pressType)) {
            return new BloodPressure(160, 100, STATUS_HIGH);
        }
        if (PRESS_DOUBLE.equals(pressType)) {
            return new BloodPressure(90, 60, STATUS_LOW);
        }
        return new BloodPressure(120, 80, STATUS_NORMAL);
    }

    private String normalizePressType(String raw) {
        String v = trimToNull(raw);
        if (v == null) {
            return null;
        }
        v = v.trim().toUpperCase();

        // 先兼容原有 SHORT/LONG/DOUBLE
        if (PRESS_SHORT.equals(v) || PRESS_DOUBLE.equals(v) || PRESS_LONG.equals(v)) {
            return v;
        }

        // 再兼容前端/蓝牙模块常用的 single/double/long（大小写均可）
        if ("SINGLE".equals(v)) {
            return PRESS_SHORT;
        }
        if ("DOUBLE".equals(v)) {
            return PRESS_DOUBLE;
        }
        if ("LONG".equals(v)) {
            return PRESS_LONG;
        }

        return null;
    }

    private void validateBloodPressure(Integer sys, Integer dia) {
        if (sys == null || dia == null) {
            throw new IllegalArgumentException("血压需同时提供sys和dia");
        }
        if (sys < 60 || sys > 250) {
            throw new IllegalArgumentException("sys超出合理范围(60~250)");
        }
        if (dia < 40 || dia > 150) {
            throw new IllegalArgumentException("dia超出合理范围(40~150)");
        }
        if (sys <= dia) {
            throw new IllegalArgumentException("sys必须大于dia");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private HealthRecordResponse toResponse(HealthRecord record) {
        HealthRecordResponse resp = new HealthRecordResponse();
        resp.setId(record.getId());
        resp.setElderlyId(record.getElderlyId());
        resp.setStatus(record.getStatus());
        resp.setSys(record.getSys());
        resp.setDia(record.getDia());
        resp.setDeviceId(record.getDeviceId());
        resp.setRecordTime(record.getRecordTime());
        resp.setNote(record.getNote());
        return resp;
    }

    private int calcBloodPressureStatus(Integer sys, Integer dia) {
        if (sys >= 140 || dia >= 90) {
            return STATUS_HIGH;
        }
        if (sys < 90 || dia < 60) {
            return STATUS_LOW;
        }
        return STATUS_NORMAL;
    }

    private static class BloodPressure {
        private final int sys;
        private final int dia;
        private final int status;

        private BloodPressure(int sys, int dia, int status) {
            this.sys = sys;
            this.dia = dia;
            this.status = status;
        }
    }
}
