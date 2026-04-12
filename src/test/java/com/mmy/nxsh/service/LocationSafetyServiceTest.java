package com.mmy.nxsh.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmy.nxsh.controller.dto.LocationReportRequest;
import com.mmy.nxsh.controller.dto.SosReportResponse;
import com.mmy.nxsh.entity.EmergencyContact;
import com.mmy.nxsh.entity.FamilyElderlyBind;
import com.mmy.nxsh.entity.LocationLog;
import com.mmy.nxsh.mapper.EmergencyContactMapper;
import com.mmy.nxsh.mapper.FamilyElderlyBindMapper;
import com.mmy.nxsh.mapper.LocationLogMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Transactional
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LocationSafetyServiceTest {

    @Autowired
    private LocationSafetyService locationSafetyService;

    @Autowired
    private LocationLogMapper locationLogMapper;

    @Autowired
    private FamilyElderlyBindMapper familyElderlyBindMapper;

    @Autowired
    private EmergencyContactMapper emergencyContactMapper;

    @Test
    // SOS 上报后，应补齐地址并落库
    void reportSos() {
        Long elderlyId = 1001L;
        Long familyId = 2001L;
        bindFamily(elderlyId, familyId);
        insertContact(elderlyId);

        LocationReportRequest req = new LocationReportRequest();
        req.setElderlyId(elderlyId);
        req.setLatitude(new BigDecimal("31.2304"));
        req.setLongitude(new BigDecimal("121.4737"));

        SosReportResponse resp = locationSafetyService.reportSos(req);

        Assertions.assertNotNull(resp.getAddressSummary());
        Assertions.assertTrue(resp.getAddressSummary().contains("纬度"));
        Assertions.assertEquals("13900000001", resp.getFirstContactPhone());

        LocationLog log = locationLogMapper.selectOne(
                new LambdaQueryWrapper<LocationLog>()
                        .eq(LocationLog::getElderlyId, elderlyId)
                        .eq(LocationLog::getScene, "SOS")
                        .orderByDesc(LocationLog::getLogTime)
                        .last("LIMIT 1"));
        Assertions.assertNotNull(log);
        Assertions.assertEquals(resp.getAddressSummary(), log.getAddress());
    }

    @Test
    // 查询 SOS 轨迹时，只应返回 SOS 场景的数据
    void listSosLocations() {
        Long elderlyId = 1002L;
        Long familyId = 2002L;
        bindFamily(elderlyId, familyId);

        LocationLog sos = new LocationLog();
        sos.setElderlyId(elderlyId);
        sos.setScene("SOS");
        sos.setLatitude(new BigDecimal("30.0001"));
        sos.setLongitude(new BigDecimal("120.0001"));
        sos.setAddress("测试地址A");
        sos.setLogTime(LocalDateTime.now().minusMinutes(1));
        sos.setTriggerType(1);
        locationLogMapper.insert(sos);

        LocationLog active = new LocationLog();
        active.setElderlyId(elderlyId);
        active.setScene("ACTIVE");
        active.setLatitude(new BigDecimal("30.0002"));
        active.setLongitude(new BigDecimal("120.0002"));
        active.setAddress("测试地址B");
        active.setLogTime(LocalDateTime.now());
        active.setTriggerType(2);
        locationLogMapper.insert(active);

        List<?> list = locationSafetyService.listSosLocationsForFamily(familyId, elderlyId);
        Assertions.assertEquals(1, list.size());
    }

    private void bindFamily(Long elderlyId, Long familyId) {
        FamilyElderlyBind bind = new FamilyElderlyBind();
        bind.setElderlyId(elderlyId);
        bind.setFamilyId(familyId);
        bind.setRelationName("女儿");
        bind.setIsPrimary(1);
        familyElderlyBindMapper.insert(bind);
    }

    private void insertContact(Long elderlyId) {
        EmergencyContact contact = new EmergencyContact();
        contact.setElderlyId(elderlyId);
        contact.setName("家属");
        contact.setPhone("13900000001");
        contact.setRelationship("女儿");
        contact.setPriority(1);
        emergencyContactMapper.insert(contact);
    }
}
