package com.mmy.nxsh.controller;

import com.mmy.nxsh.controller.dto.SaveMedicineRequest;
import com.mmy.nxsh.service.MedicineService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)public class SaveMedicineTest {

    @Autowired
    private MedicineService medicineService;

    @Test
    public void testSaveMedicine() {
        SaveMedicineRequest req = new SaveMedicineRequest();
        req.setElderlyId(1L);
        req.setName("测试药品");
        req.setDosageDesc("每次1片");
        req.setPhotoUrl("/uploads/photo/test.jpg");
        req.setCurrentStock(10);
        req.setLowStockThreshold(2);
        req.setVoiceRemindUrl("/uploads/voice/test.mp3");
        req.setStartDate(LocalDate.now());
        req.setEndDate(LocalDate.now().plusMonths(1));
        req.setFrequencyType("daily");
        
        List<LocalTime> takeTimes = new ArrayList<>();
        takeTimes.add(LocalTime.of(8, 0));
        takeTimes.add(LocalTime.of(20, 0));
        req.setTakeTimes(takeTimes);

        try {
            Long medicineId = medicineService.saveMedicine(req);
            System.out.println("保存成功，药品ID: " + medicineId);
        } catch (Exception e) {
            System.out.println("保存失败，错误信息: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
