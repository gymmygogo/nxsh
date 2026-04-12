package com.mmy.nxsh.service;

import com.mmy.nxsh.controller.dto.FamilyMedicineDashboardDTO;
import com.mmy.nxsh.controller.dto.FamilyMedicineItemStatusDTO;
import com.mmy.nxsh.entity.MedicineInfo;
import com.mmy.nxsh.entity.MedicineLog;
import com.mmy.nxsh.entity.MedicinePlan;
import com.mmy.nxsh.mapper.MedicineInfoMapper;
import com.mmy.nxsh.mapper.MedicineLogMapper;
import com.mmy.nxsh.mapper.MedicinePlanMapper;
import com.mmy.nxsh.service.impl.MedicineServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 家属端看板测试 - 今日服药状态 + 低库存标记
 */
@Transactional
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MedicineDashboardTest {

    @Autowired
    private MedicineLogMapper medicineLogMapper;
    @Autowired
    private MedicineInfoMapper medicineInfoMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private MedicineServiceImpl medicineService;
    @Autowired
    private MedicinePlanMapper medicinePlanMapper;

    @BeforeEach
    void setBaseMapper() {
        // baseMapper 指向真实 Mapper，避免 MyBatis-Plus 报空
        ReflectionTestUtils.setField(medicineService, "baseMapper", medicineInfoMapper);
    }

    @Test
    void dashboard_todayStatus() {
        LocalDate today = LocalDate.now();
        Long elderlyId = 1L;

        MedicineInfo m1 = buildInfo(1L, elderlyId, "aspirin", 5, 2);
        // 补充：手动设置有效期，确保 today 包含在有效范围内
        m1.setStartDate(today.minusDays(1)); // 开始时间设为昨天
        m1.setEndDate(today.plusDays(30));   // 结束时间设为 30 天后

        MedicineInfo m2 = buildInfo(2L, elderlyId, "blood pressure", 8, 3);
        // 补充：手动设置有效期
        m2.setStartDate(today.minusDays(1));
        m2.setEndDate(today.plusDays(30));
        medicineInfoMapper.insert(m1);
        medicineInfoMapper.insert(m2);

        // m1 今天已服用
        MedicineLog log = new MedicineLog();
        log.setMedicineId(m1.getId());
        log.setElderlyId(elderlyId);
        log.setPlanTime(LocalDateTime.of(today, LocalTime.of(8, 0)));
        log.setStatus(1);
        medicineLogMapper.insert(log);

        MedicinePlan plan1 = new MedicinePlan();
        plan1.setElderlyId(elderlyId);
        plan1.setMedicineId(m1.getId());
        plan1.setTakeTime(LocalTime.of(8, 0)); // 对应你下面 log 的 8点
        plan1.setFrequencyType(1); // 重点：一定要用我们改好的数字 1（每日用药）
        medicinePlanMapper.insert(plan1);

        MedicinePlan plan2 = new MedicinePlan();
        plan2.setElderlyId(elderlyId);
        plan2.setMedicineId(m2.getId());
        plan2.setTakeTime(LocalTime.of(8, 0));
        plan2.setFrequencyType(1); // 重点：数字 1
        medicinePlanMapper.insert(plan2);

        FamilyMedicineDashboardDTO dashboard = medicineService.getFamilyDashboard(elderlyId, today);

        assertEquals(elderlyId, dashboard.getElderlyId());
        assertEquals(2, dashboard.getMedicines().size());

        FamilyMedicineItemStatusDTO item1 = dashboard.getMedicines().stream()
                .filter(m -> m.getMedicineId().equals(m1.getId()))
                .findFirst()
                .orElse(null);
        assertTrue(item1.getTaken());
        assertFalse(item1.getLowStock());

        FamilyMedicineItemStatusDTO item2 = dashboard.getMedicines().stream()
                .filter(m -> m.getMedicineId().equals(m2.getId()))
                .findFirst()
                .orElse(null);
        assertFalse(item2.getTaken());
        assertFalse(item2.getLowStock());

        assertFalse(dashboard.getHasLowStock());
    }

    @Test
    void dashboard_lowStockFlag() {
        LocalDate today = LocalDate.now();
        Long elderlyId = 1L;

        MedicineInfo m1 = buildInfo(1L, elderlyId, "vitamin", 2, 3);
        // 补充 m1 的有效期和状态
        m1.setStartDate(today.minusDays(1));
        m1.setEndDate(today.plusDays(30));
        m1.setIsActive(1);

        MedicineInfo m2 = buildInfo(2L, elderlyId, "calcium", 1, 2);
        // 补充 m2 的有效期和状态
        m2.setStartDate(today.minusDays(1));
        m2.setEndDate(today.plusDays(30));
        m2.setIsActive(1);

        MedicineInfo m3 = buildInfo(3L, elderlyId, "cold medicine", 10, 2);
        // 补充 m3 的有效期和状态
        m3.setStartDate(today.minusDays(1));
        m3.setEndDate(today.plusDays(30));
        m3.setIsActive(1);

        medicineInfoMapper.insert(m1);
        medicineInfoMapper.insert(m2);
        medicineInfoMapper.insert(m3);

        // ================= 补充以下代码：为这三种药插入今日服药计划 =================
        MedicinePlan plan1 = new MedicinePlan();
        plan1.setElderlyId(elderlyId);
        plan1.setMedicineId(m1.getId());
        plan1.setTakeTime(LocalTime.of(8, 0));
        plan1.setFrequencyType(1);
        medicinePlanMapper.insert(plan1);

        MedicinePlan plan2 = new MedicinePlan();
        plan2.setElderlyId(elderlyId);
        plan2.setMedicineId(m2.getId());
        plan2.setTakeTime(LocalTime.of(8, 0));
        plan2.setFrequencyType(1);
        medicinePlanMapper.insert(plan2);

        MedicinePlan plan3 = new MedicinePlan();
        plan3.setElderlyId(elderlyId);
        plan3.setMedicineId(m3.getId());
        plan3.setTakeTime(LocalTime.of(8, 0));
        plan3.setFrequencyType(1);
        medicinePlanMapper.insert(plan3);
        // ====================================================================

        // 模拟 Redis 低库存标记：这里简单直接，用固定 key 写入任意值
        stringRedisTemplate.opsForValue().set("medicine:low_stock:" + elderlyId + ":" + m1.getId(), "1");
        stringRedisTemplate.opsForValue().set("medicine:low_stock:" + elderlyId + ":" + m2.getId(), "1");

        FamilyMedicineDashboardDTO dashboard = medicineService.getFamilyDashboard(elderlyId, today);

        assertTrue(dashboard.getHasLowStock());

        for (FamilyMedicineItemStatusDTO item : dashboard.getMedicines()) {
            if (item.getMedicineId().equals(m1.getId()) || item.getMedicineId().equals(m2.getId())) {
                assertTrue(item.getLowStock());
            } else {
                assertFalse(item.getLowStock());
            }
        }
    }

    @Test
    void dashboard_noMedicine() {
        LocalDate today = LocalDate.now();
        Long elderlyId = 1L;

        FamilyMedicineDashboardDTO dashboard = medicineService.getFamilyDashboard(elderlyId, today);

        assertEquals(elderlyId, dashboard.getElderlyId());
        assertTrue(dashboard.getMedicines().isEmpty());
        assertFalse(dashboard.getHasLowStock());
    }

    @Test
    void dashboard_sameMedicineMultipleTimes_markTakenOnce() {
        LocalDate today = LocalDate.now();
        Long elderlyId = 1L;

        MedicineInfo m1 = buildInfo(1L, elderlyId, "vitamin", 10, 2);
        // 加上这三行，强行让药处于“不过期”且“启用”的状态
        m1.setStartDate(today.minusDays(1));
        m1.setEndDate(today.plusDays(30));
        m1.setIsActive(1);
        medicineInfoMapper.insert(m1);

        // 同一药一天服用多次
        MedicineLog log1 = new MedicineLog();
        log1.setMedicineId(m1.getId());
        log1.setElderlyId(elderlyId);
        log1.setPlanTime(LocalDateTime.of(today, LocalTime.of(8, 0)));
        log1.setStatus(1);

        MedicineLog log2 = new MedicineLog();
        log2.setMedicineId(m1.getId());
        log2.setElderlyId(elderlyId);
        log2.setPlanTime(LocalDateTime.of(today, LocalTime.of(12, 0)));
        log2.setStatus(1);

        medicineLogMapper.insert(log1);
        medicineLogMapper.insert(log2);
        // ================= 补充以下代码：插入两条对应的服药计划 =================
        // 计划1：早上 8:00 吃药
        MedicinePlan plan1 = new MedicinePlan();
        plan1.setElderlyId(elderlyId);
        plan1.setMedicineId(m1.getId());
        plan1.setTakeTime(LocalTime.of(8, 0));
        plan1.setFrequencyType(1); // 1: 每日用药
        medicinePlanMapper.insert(plan1);

        // 计划2：中午 12:00 吃药
        MedicinePlan plan2 = new MedicinePlan();
        plan2.setElderlyId(elderlyId);
        plan2.setMedicineId(m1.getId());
        plan2.setTakeTime(LocalTime.of(12, 0));
        plan2.setFrequencyType(1); // 1: 每日用药
        medicinePlanMapper.insert(plan2);

        FamilyMedicineDashboardDTO dashboard = medicineService.getFamilyDashboard(elderlyId, today);

        assertEquals(1, dashboard.getMedicines().size());
        assertTrue(dashboard.getMedicines().get(0).getTaken());
    }

    @Test
    // 不同老人的数据应隔离
    void elderlyIsolation() {
        LocalDate today = LocalDate.now();
        Long elderly1 = 1L;

        MedicineInfo m1 = buildInfo(100L, elderly1, "medicine for elderly1", 10, 2);
        //  补充这两行，给这个药续命，防止它在查询时被当成过期药过滤掉
        m1.setStartDate(today.minusDays(1));
        m1.setEndDate(today.plusDays(30));
        //  ========================================================
        medicineInfoMapper.insert(m1);

        MedicinePlan plan1 = new MedicinePlan();
        plan1.setElderlyId(elderly1);
        plan1.setMedicineId(m1.getId());
        plan1.setTakeTime(LocalTime.of(8, 0));
        plan1.setFrequencyType(1);
        medicinePlanMapper.insert(plan1);

        FamilyMedicineDashboardDTO dashboard1 = medicineService.getFamilyDashboard(elderly1, today);

        assertEquals(1, dashboard1.getMedicines().size());
        assertEquals("medicine for elderly1", dashboard1.getMedicines().get(0).getName());
        assertEquals(elderly1, dashboard1.getElderlyId());
    }

    private static MedicineInfo buildInfo(Long id, Long elderlyId, String name, int currentStock, int lowThreshold) {
        MedicineInfo m = new MedicineInfo();
        m.setElderlyId(elderlyId);
        m.setName(name);
        m.setDosageDesc("1 tablet");
        m.setPhotoUrl("/p.jpg");
        m.setCurrentStock(currentStock);
        m.setLowStockThreshold(lowThreshold);
        m.setStartDate(LocalDate.of(2024, 3, 1));
        m.setEndDate(LocalDate.of(2024, 12, 31));
        m.setIsActive(1);
        m.setVersion(0);
        return m;
    }
}
