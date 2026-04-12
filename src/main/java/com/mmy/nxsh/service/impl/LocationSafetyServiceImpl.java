package com.mmy.nxsh.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmy.nxsh.controller.dto.LastLocationDTO;
import com.mmy.nxsh.controller.dto.LocationReportRequest;
import com.mmy.nxsh.controller.dto.SosReportResponse;
import com.mmy.nxsh.entity.EmergencyContact;
import com.mmy.nxsh.entity.FamilyElderlyBind;
import com.mmy.nxsh.entity.LocationLog;
import com.mmy.nxsh.mapper.EmergencyContactMapper;
import com.mmy.nxsh.mapper.FamilyElderlyBindMapper;
import com.mmy.nxsh.mapper.LocationLogMapper;
import com.mmy.nxsh.service.GeoCodingService;
import com.mmy.nxsh.service.LocationSafetyService;
import com.mmy.nxsh.websocket.FamilyWebSocketServer;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class LocationSafetyServiceImpl implements LocationSafetyService {

    public static final String SCENE_SHARE = "SHARE";
    public static final String SCENE_SOS = "SOS";
    public static final String SCENE_ACTIVE = "ACTIVE";

    private final LocationLogMapper locationLogMapper;
    private final EmergencyContactMapper emergencyContactMapper;
    private final FamilyElderlyBindMapper familyElderlyBindMapper;
    private final GeoCodingService geoCodingService;

    public LocationSafetyServiceImpl(LocationLogMapper locationLogMapper,
                                     EmergencyContactMapper emergencyContactMapper,
                                     FamilyElderlyBindMapper familyElderlyBindMapper,
                                     GeoCodingService geoCodingService) {
        this.locationLogMapper = locationLogMapper;
        this.emergencyContactMapper = emergencyContactMapper;
        this.familyElderlyBindMapper = familyElderlyBindMapper;
        this.geoCodingService = geoCodingService;
    }

    @Override
    public void reportShare(LocationReportRequest req) {
        String address = resolveAddress(req);
        insertLog(req, SCENE_SHARE, address);
        CompletableFuture.runAsync(() -> notifyFamilies(req.getElderlyId(), JSONUtil.createObj()
                .set("type", "LOCATION_SHARE")
                .set("elderlyId", req.getElderlyId())
                .set("latitude", req.getLatitude())
                .set("longitude", req.getLongitude())
                .set("address", address)
                .set("msg", "老人向您分享了当前位置")));
    }

    @Override
    public void reportActive(LocationReportRequest req) {
        String address = resolveAddress(req);
        insertLog(req, SCENE_ACTIVE, address);
    }

    @Override
    public SosReportResponse reportSos(LocationReportRequest req) {
        String address = resolveAddress(req);
        insertLog(req, SCENE_SOS, address);
        SosReportResponse resp = new SosReportResponse();
        resp.setAddressSummary(address);
        EmergencyContact first = emergencyContactMapper.selectOne(
                new LambdaQueryWrapper<EmergencyContact>()
                        .eq(EmergencyContact::getElderlyId, req.getElderlyId())
                        .orderByAsc(EmergencyContact::getPriority)
                        .last("LIMIT 1"));
        resp.setFirstContactPhone(first != null ? first.getPhone() : null);

        CompletableFuture.runAsync(() -> notifyFamilies(req.getElderlyId(), JSONUtil.createObj()
                .set("type", "SOS")
                .set("elderlyId", req.getElderlyId())
                .set("latitude", req.getLatitude())
                .set("longitude", req.getLongitude())
                .set("address", address)
                .set("msg", "紧急求救！位置：" + address)));
        return resp;
    }

    @Override
    public LastLocationDTO getLastLocationForFamily(Long familyId, Long elderlyId) {
        assertFamilyBound(familyId, elderlyId);
        LocationLog log = locationLogMapper.selectOne(
                new LambdaQueryWrapper<LocationLog>()
                        .eq(LocationLog::getElderlyId, elderlyId)
                        .in(LocationLog::getScene, SCENE_SHARE, SCENE_SOS, SCENE_ACTIVE)
                        .orderByDesc(LocationLog::getLogTime)
                        .last("LIMIT 1"));
        if (log == null) {
            return null;
        }
        LastLocationDTO dto = new LastLocationDTO();
        BeanUtils.copyProperties(log, dto);
        return dto;
    }

    @Override
    public List<LastLocationDTO> listSosLocationsForFamily(Long familyId, Long elderlyId) {
        assertFamilyBound(familyId, elderlyId);
        List<LocationLog> logs = locationLogMapper.selectList(
                new LambdaQueryWrapper<LocationLog>()
                        .eq(LocationLog::getElderlyId, elderlyId)
                        .eq(LocationLog::getScene, SCENE_SOS)
                        .orderByDesc(LocationLog::getLogTime));
        return logs.stream().map(log -> {
            LastLocationDTO dto = new LastLocationDTO();
            BeanUtils.copyProperties(log, dto);
            return dto;
        }).collect(Collectors.toList());
    }

    private void insertLog(LocationReportRequest req, String scene, String address) {
        LocationLog log = new LocationLog();
        log.setElderlyId(req.getElderlyId());
        log.setScene(scene);
        log.setLatitude(req.getLatitude());
        log.setLongitude(req.getLongitude());
        log.setAddress(address);
        log.setLogTime(LocalDateTime.now());
        log.setTriggerType(1);
        locationLogMapper.insert(log);
    }

    private String resolveAddress(LocationReportRequest req) {
        String address = normalizeAddress(req.getAddress());
        if (StrUtil.isNotBlank(address)) {
            return address;
        }
        return geoCodingService.reverseGeocode(req.getLatitude(), req.getLongitude());
    }

    private String normalizeAddress(String address) {
        if (StrUtil.isBlank(address)) {
            return null;
        }
        return address.trim();
    }

    private void notifyFamilies(Long elderlyId, JSONObject payload) {
        List<FamilyElderlyBind> binds = familyElderlyBindMapper.selectList(
                new LambdaQueryWrapper<FamilyElderlyBind>().eq(FamilyElderlyBind::getElderlyId, elderlyId));
        String text = payload.toString();
        for (FamilyElderlyBind bind : binds) {
            FamilyWebSocketServer.sendMessage(bind.getFamilyId(), text);
        }
    }

    private void assertFamilyBound(Long familyId, Long elderlyId) {
        Long cnt = familyElderlyBindMapper.selectCount(
                new LambdaQueryWrapper<FamilyElderlyBind>()
                        .eq(FamilyElderlyBind::getFamilyId, familyId)
                        .eq(FamilyElderlyBind::getElderlyId, elderlyId));
        if (cnt == null || cnt == 0) {
            throw new IllegalArgumentException("未绑定该老人，无法查看位置");
        }
    }
}
