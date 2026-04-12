package com.mmy.nxsh.service;

import com.mmy.nxsh.controller.dto.DueMedicineGroupDTO;
import com.mmy.nxsh.controller.dto.SaveMedicineRequest;
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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
@Transactional
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MedicineServiceReminderTest {

    @Autowired
    private MedicinePlanMapper medicinePlanMapper;

    @Autowired
    private MedicineInfoMapper medicineInfoMapper;

    @Autowired
    private MedicineServiceImpl medicineService;

    @BeforeEach
    void setBaseMapper() {
        // 指定 baseMapper，保证 MyBatis-Plus 正常工作
        ReflectionTestUtils.setField(medicineService, "baseMapper", medicineInfoMapper);
    }

    @Test
    // 无计划时应返回空列表
    void dueEmpty() {
        List<DueMedicineGroupDTO> list = medicineService.getDueMedicines(1L, LocalDateTime.of(2024, 3, 18, 8, 0));
        assertTrue(list.isEmpty());
    }

    @Test
    // 周期不匹配时应不提醒
    void weeklyMismatch() {
        LocalDate monday = LocalDate.of(2024, 3, 18);
        LocalDateTime now = LocalDateTime.of(monday, LocalTime.of(8, 0));

        MedicinePlan plan = new MedicinePlan();
        plan.setElderlyId(1L);
        plan.setMedicineId(insertMedicine());
        plan.setTakeTime(LocalTime.of(8, 0));
        plan.setFrequencyType(2);
        plan.setWeekDays("2");
        medicinePlanMapper.insert(plan);

        List<DueMedicineGroupDTO> list = medicineService.getDueMedicines(1L, now);
        assertTrue(list.isEmpty());
    }

    @Test
    // 同一时间的多种药应合并成一组
    void mergeSameTime() {
        LocalDate day = LocalDate.of(2024, 3, 18);
        LocalTime slot = LocalTime.of(8, 0);
        LocalDateTime now = LocalDateTime.of(day, slot);

        Long m1Id = insertMedicine();
        Long m2Id = insertMedicine();

        insertPlan(m1Id, slot);
        insertPlan(m2Id, slot);

        List<DueMedicineGroupDTO> list = medicineService.getDueMedicines(1L, now);

        assertEquals(1, list.size());
        assertEquals(2, list.get(0).getMedicines().size());
        assertEquals("standard", list.get(0).getRemindMode());
        assertEquals(LocalDateTime.of(day, slot), list.get(0).getPlanTime());
    }

    @Test
    // 有亲情语音时应使用家族模式
    void familyVoice() {
        LocalDate day = LocalDate.of(2024, 3, 18);
        LocalTime slot = LocalTime.of(8, 0);
        LocalDateTime now = LocalDateTime.of(day, slot);

        Long mId = insertMedicineWithVoice("/v/a.mp3");
        insertPlan(mId, slot);

        List<DueMedicineGroupDTO> list = medicineService.getDueMedicines(1L, now);

        assertEquals(1, list.size());
        assertEquals("family", list.get(0).getRemindMode());
        assertEquals("/v/a.mp3", list.get(0).getFamilyVoiceUrl());
    }

    @Test
    // 推迟功能应返回新的提醒时间
    void snooze() {
        LocalDateTime planTime = LocalDateTime.of(2024, 3, 18, 8, 0);
        List<Long> ids = List.of(1L, 2L);

        LocalDateTime next = medicineService.snooze(1L, planTime, ids);

        assertEquals(planTime.plusMinutes(10), next);
    }

    @Test
    // 标记服用后库存应减少
    void markTaken() {
        LocalDateTime planTime = LocalDateTime.of(2024, 3, 18, 8, 0);
        Long mid = insertMedicine();

        medicineService.markTaken(1L, planTime, List.of(mid));

        MedicineInfo after = medicineInfoMapper.selectById(mid);
        assertEquals(9, after.getCurrentStock());
    }

    @Test
    // 库存为0时标记服用应失败
    void stockZero() {
        LocalDateTime planTime = LocalDateTime.of(2024, 3, 18, 8, 0);
        Long mid = insertMedicineWithStock(1L, 0);

        assertThrows(IllegalStateException.class,
                () -> medicineService.markTaken(1L, planTime, List.of(mid)));
    }

    @Test
    // 无服用时间时保存应失败
    void saveNoTakeTime() {
        SaveMedicineRequest req = new SaveMedicineRequest();
        req.setElderlyId(1L);
        req.setName("药");
        req.setTakeTimes(new ArrayList<>());

        assertThrows(IllegalArgumentException.class, () -> medicineService.saveMedicine(req));
    }

    private Long insertMedicine() {
        return insertMedicineWithStock(1L, 10);
    }

    private Long insertMedicineWithStock(Long elderlyId, int stock) {
        MedicineInfo m = new MedicineInfo();
        m.setElderlyId(elderlyId);
        m.setName("测试药" + System.nanoTime());
        m.setDosageDesc("1片");
        m.setPhotoUrl("/p.jpg");
        m.setCurrentStock(stock);
        m.setLowStockThreshold(2);
        m.setStartDate(LocalDate.of(2024, 3, 1));
        m.setEndDate(LocalDate.of(2024, 12, 31));
        m.setIsActive(1);
        medicineInfoMapper.insert(m);
        return m.getId();
    }

    private Long insertMedicineWithVoice(String voiceUrl) {
        MedicineInfo m = new MedicineInfo();
        m.setElderlyId(1L);
        m.setName("测试药语音" + System.nanoTime());
        m.setDosageDesc("1片");
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

    private void insertPlan(Long medicineId, LocalTime time) {
        MedicinePlan plan = new MedicinePlan();
        plan.setElderlyId(1L);
        plan.setMedicineId(medicineId);
        plan.setTakeTime(time);
        plan.setFrequencyType(1);
        medicinePlanMapper.insert(plan);
    }
}
