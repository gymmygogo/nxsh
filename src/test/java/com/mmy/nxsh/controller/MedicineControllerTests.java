package com.mmy.nxsh.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmy.nxsh.controller.dto.MarkMedicineTakenRequest;
import com.mmy.nxsh.entity.MedicineInfo;
import com.mmy.nxsh.entity.MedicineLog;
import com.mmy.nxsh.entity.MedicinePlan;
import com.mmy.nxsh.mapper.MedicineInfoMapper;
import com.mmy.nxsh.mapper.MedicineLogMapper;
import com.mmy.nxsh.mapper.MedicinePlanMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)@AutoConfigureMockMvc
@Transactional
class MedicineControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MedicineInfoMapper medicineInfoMapper;

    @Autowired
    private MedicinePlanMapper medicinePlanMapper;

    @Autowired
    private MedicineLogMapper medicineLogMapper;

    @Test
    void due() throws Exception {
        Long elderlyId = 101L;
        LocalTime takeTime = LocalTime.of(8, 0);
        LocalDate today = LocalDate.now();

        MedicineInfo info = new MedicineInfo();
        info.setElderlyId(elderlyId);
        info.setName("阿司匹林");
        info.setDosageDesc("1粒");
        info.setCurrentStock(10);
        info.setLowStockThreshold(3);
        info.setStartDate(today.minusDays(1));
        info.setEndDate(today.plusDays(1));
        info.setIsActive(1);
        medicineInfoMapper.insert(info);

        MedicinePlan plan = new MedicinePlan();
        plan.setElderlyId(elderlyId);
        plan.setMedicineId(info.getId());
        plan.setTakeTime(takeTime);
        medicinePlanMapper.insert(plan);

        String timeParam = LocalDateTime.of(today, takeTime).format(DateTimeFormatter.ISO_DATE_TIME);
        MvcResult result = mockMvc.perform(get("/medicine/due")
                        .param("elderlyId", String.valueOf(elderlyId))
                        .param("time", timeParam))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        Assertions.assertTrue(response.contains("阿司匹林"));
        Assertions.assertTrue(response.contains(takeTime.toString()));
    }

    @Test
    void take() throws Exception {
        Long elderlyId = 202L;
        LocalTime takeTime = LocalTime.of(9, 0);
        LocalDate today = LocalDate.now();
        LocalDateTime planTime = LocalDateTime.of(today, takeTime);

        MedicineInfo info = new MedicineInfo();
        info.setElderlyId(elderlyId);
        info.setName("维生素C");
        info.setDosageDesc("2粒");
        info.setCurrentStock(2);
        info.setLowStockThreshold(1);
        info.setStartDate(today.minusDays(1));
        info.setEndDate(today.plusDays(1));
        info.setIsActive(1);
        medicineInfoMapper.insert(info);

        MarkMedicineTakenRequest req = new MarkMedicineTakenRequest();
        req.setElderlyId(elderlyId);
        req.setPlanTime(planTime);
        req.setMedicineIds(Collections.singletonList(info.getId()));

        mockMvc.perform(post("/medicine/take")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        MedicineInfo updated = medicineInfoMapper.selectById(info.getId());
        Assertions.assertEquals(1, updated.getCurrentStock());

        MedicineLog log = medicineLogMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MedicineLog>()
                .eq("medicine_id", info.getId())
                .eq("elderly_id", elderlyId));
        Assertions.assertNotNull(log);
        Assertions.assertEquals(planTime.withSecond(0).withNano(0), log.getPlanTime().withSecond(0).withNano(0));
        Assertions.assertEquals(1, log.getStatus());
    }
}
