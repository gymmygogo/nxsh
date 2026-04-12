package com.mmy.nxsh.task;

import com.mmy.nxsh.controller.dto.DueMedicineGroupDTO;
import com.mmy.nxsh.entity.MedicineInfo;
import com.mmy.nxsh.entity.MedicinePlan;
import com.mmy.nxsh.mapper.MedicineInfoMapper;
import com.mmy.nxsh.mapper.MedicinePlanMapper;
import com.mmy.nxsh.service.impl.MedicineServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 用药提醒聚合与模式测试 - 真实集成测试
 */
@Transactional
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MedicineAggregationTest {

    @Autowired
    private MedicinePlanMapper medicinePlanMapper;
    @Autowired
    private MedicineInfoMapper medicineInfoMapper;
    @Autowired
    private MedicineServiceImpl medicineService;

    @BeforeEach
    void setBaseMapper() {
        ReflectionTestUtils.setField(medicineService, "baseMapper", medicineInfoMapper);
    }

    @Test
    void multipleMedicines_sameTime_aggregatedToOneGroup() {
        // 同一时间点 3 种药，应该合并成 1 个提醒组
        LocalDate day = LocalDate.of(2024, 3, 18);
        LocalTime slot = LocalTime.of(8, 0);
        LocalDateTime now = LocalDateTime.of(day, slot);
        Long elderlyId = 1L;

        // 插入 3 种药品
        Long m1Id = insertMedicine(elderlyId, "aspirin", null);
        Long m2Id = insertMedicine(elderlyId, "vitamin c", null);
        Long m3Id = insertMedicine(elderlyId, "calcium", null);

        // 插入 3 条计划，都在 8:00
        insertPlan(elderlyId, m1Id, slot, 1);
        insertPlan(elderlyId, m2Id, slot, 1);
        insertPlan(elderlyId, m3Id, slot, 1);

        List<DueMedicineGroupDTO> result = medicineService.getDueMedicines(elderlyId, now);

        assertEquals(1, result.size());
        assertEquals(3, result.get(0).getMedicines().size());
        assertEquals("standard", result.get(0).getRemindMode());
        assertEquals(LocalDateTime.of(day, slot), result.get(0).getPlanTime());
    }

    @Test
    void familyVoice_exists_switchToFamilyMode() {
        // 任意一条有亲情语音，整组切换为 family 模式
        LocalDate day = LocalDate.of(2024, 3, 18);
        LocalTime slot = LocalTime.of(8, 0);
        LocalDateTime now = LocalDateTime.of(day, slot);
        Long elderlyId = 1L;

        Long m1Id = insertMedicine(elderlyId, "blood pressure", null);
        Long m2Id = insertMedicine(elderlyId, "vitamin", "/voice/family_voice.mp3");

        insertPlan(elderlyId, m1Id, slot, 1);
        insertPlan(elderlyId, m2Id, slot, 1);

        List<DueMedicineGroupDTO> result = medicineService.getDueMedicines(elderlyId, now);

        assertEquals(1, result.size());
        assertEquals("family", result.get(0).getRemindMode());
        assertEquals("/voice/family_voice.mp3", result.get(0).getFamilyVoiceUrl());
    }

    @Test
    void standardMode_hasNoFamilyVoiceUrl() {
        LocalDate day = LocalDate.of(2024, 3, 18);
        LocalTime slot = LocalTime.of(8, 0);
        LocalDateTime now = LocalDateTime.of(day, slot);
        Long elderlyId = 1L;

        Long mId = insertMedicine(elderlyId, "regular medicine", null);
        insertPlan(elderlyId, mId, slot, 1);

        List<DueMedicineGroupDTO> result = medicineService.getDueMedicines(elderlyId, now);

        assertEquals(1, result.size());
        assertEquals("standard", result.get(0).getRemindMode());
        assertNull(result.get(0).getFamilyVoiceUrl());
    }

    @Test
    void noPlan_returnsEmpty() {
        List<DueMedicineGroupDTO> result = medicineService.getDueMedicines(1L, LocalDateTime.now());
        assertTrue(result.isEmpty());
    }

    @Test
    void expiredMedicine_notReminded() {
        LocalDate queryDay = LocalDate.of(2024, 3, 18);
        LocalTime slot = LocalTime.of(8, 0);
        LocalDateTime now = LocalDateTime.of(queryDay, slot);
        Long elderlyId = 1L;

        // 插入过期药品
        MedicineInfo info = new MedicineInfo();
        info.setElderlyId(elderlyId);
        info.setName("expired medicine");
        info.setDosageDesc("1 tablet");
        info.setStartDate(LocalDate.of(2024, 1, 1));
        info.setEndDate(LocalDate.of(2024, 3, 10)); // 已过期
        info.setIsActive(1);
        info.setCurrentStock(10);
        info.setLowStockThreshold(2);
        medicineInfoMapper.insert(info);

        // 插入计划
        insertPlan(elderlyId, info.getId(), slot, 1);

        List<DueMedicineGroupDTO> result = medicineService.getDueMedicines(elderlyId, now);

        assertTrue(result.isEmpty());
    }

    @Test
    // 停用药品应不提醒
    void inactiveNotRemind() {
        LocalDate today = LocalDate.of(2024, 3, 18);
        LocalTime slot = LocalTime.of(8, 0);
        LocalDateTime now = LocalDateTime.of(today, slot);
        Long elderlyId = 1L;

        // 插入停用药品
        MedicineInfo info = new MedicineInfo();
        info.setElderlyId(elderlyId);
        info.setName("inactive medicine");
        info.setDosageDesc("1 tablet");
        info.setStartDate(today.minusDays(1));
        info.setEndDate(today.plusDays(30));
        info.setIsActive(0); // 停用
        info.setCurrentStock(10);
        info.setLowStockThreshold(2);
        medicineInfoMapper.insert(info);

        // 插入计划
        insertPlan(elderlyId, info.getId(), slot, 1);

        List<DueMedicineGroupDTO> result = medicineService.getDueMedicines(elderlyId, now);

        assertTrue(result.isEmpty());
    }

    private Long insertMedicine(Long elderlyId, String name, String voiceUrl) {
        MedicineInfo m = new MedicineInfo();
        m.setElderlyId(elderlyId);
        m.setName(name);
        m.setDosageDesc("1 tablet");
        m.setPhotoUrl("/p.jpg");
        m.setCurrentStock(10);
        m.setLowStockThreshold(2);
        m.setVoiceRemindUrl(voiceUrl);
        m.setStartDate(LocalDate.of(2024, 3, 1));
        m.setEndDate(LocalDate.of(2024, 12, 31));
        m.setIsActive(1);
        medicineInfoMapper.insert(m);
        return m.getId();
    }

    private void insertPlan(Long elderlyId, Long medicineId, LocalTime time, Integer freqType) {
        MedicinePlan plan = new MedicinePlan();
        plan.setElderlyId(elderlyId);
        plan.setMedicineId(medicineId);
        plan.setTakeTime(time);
        plan.setFrequencyType(freqType);
        medicinePlanMapper.insert(plan);
    }
}
