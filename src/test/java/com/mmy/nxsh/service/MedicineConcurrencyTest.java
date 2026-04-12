package com.mmy.nxsh.service;

import com.mmy.nxsh.entity.MedicineInfo;
import com.mmy.nxsh.mapper.MedicineInfoMapper;
import com.mmy.nxsh.service.impl.MedicineServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 并发库存扣减集成测试（直接操作真实 DB + Service）
 */
@Transactional
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MedicineConcurrencyTest {

    @Autowired
    private MedicineInfoMapper medicineInfoMapper;

    @Autowired
    private MedicineServiceImpl medicineService;

    @BeforeEach
    void setBaseMapper() {
        // 把 baseMapper 指向真实的 MedicineInfoMapper，避免 MyBatis-Plus 报空
        ReflectionTestUtils.setField(medicineService, "baseMapper", medicineInfoMapper);
    }

    @Test
    // 版本冲突时应抛异常
    void versionConflict() {
        // 准备测试数据：库存 1，version 0
        Long mid = insertTestMedicine(1);
        LocalDateTime planTime = LocalDateTime.of(2024, 3, 18, 8, 0);

        // 第一次扣减，应该成功：库存 1 -> 0，version 0 -> 1
        medicineService.markTaken(1L, planTime, List.of(mid));
        MedicineInfo afterFirst = medicineInfoMapper.selectById(mid);
        assertEquals(0, afterFirst.getCurrentStock());
        assertEquals(1, afterFirst.getVersion());

        // 第二次再扣同一条，version 已变，update 条数为 0，应抛出异常
        assertThrows(IllegalStateException.class,
                () -> medicineService.markTaken(1L, planTime, List.of(mid)));
    }

    @Test
    // 库存等于阈值时应能扣减
    void threshold() {
        // 库存 = 阈值 = 1，扣减一次后变 0
        Long mid = insertTestMedicine(1);
        LocalDateTime planTime = LocalDateTime.of(2024, 3, 18, 8, 0);

        medicineService.markTaken(1L, planTime, List.of(mid));

        MedicineInfo after = medicineInfoMapper.selectById(mid);
        assertEquals(0, after.getCurrentStock());
        // version 递增 1
        assertEquals(1, after.getVersion());
    }

    @Test
    // 库存为0时应不能扣减
    void stockZero() {
        // 库存为 0，直接扣减应失败
        Long mid = insertTestMedicine(0);
        LocalDateTime planTime = LocalDateTime.of(2024, 3, 18, 8, 0);

        assertThrows(IllegalStateException.class,
                () -> medicineService.markTaken(1L, planTime, List.of(mid)));
    }

    /**
     * 往 medicine_info 插入一条测试数据，返回自增 ID
     */
    private Long insertTestMedicine(int stock) {
        MedicineInfo info = new MedicineInfo();
        info.setElderlyId(1L);
        info.setName("test-medicine-" + System.nanoTime());
        info.setDosageDesc("1粒");
        info.setCurrentStock(stock);
        info.setLowStockThreshold(1);
        info.setStartDate(LocalDate.of(2024, 3, 1));
        info.setEndDate(LocalDate.of(2024, 12, 31));
        info.setIsActive(1);
        info.setVersion(0);
        medicineInfoMapper.insert(info);
        return info.getId();
    }
}
