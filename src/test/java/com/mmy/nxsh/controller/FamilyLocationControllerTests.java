package com.mmy.nxsh.controller;

import com.mmy.nxsh.entity.FamilyElderlyBind;
import com.mmy.nxsh.entity.LocationLog;
import com.mmy.nxsh.mapper.FamilyElderlyBindMapper;
import com.mmy.nxsh.mapper.LocationLogMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)@AutoConfigureMockMvc
class FamilyLocationControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FamilyElderlyBindMapper familyElderlyBindMapper;

    @Autowired
    private LocationLogMapper locationLogMapper;

    @Test
    void sosList() throws Exception {
        Long familyId = 3001L;
        Long elderlyId = 4001L;

        FamilyElderlyBind bind = new FamilyElderlyBind();
        bind.setFamilyId(familyId);
        bind.setElderlyId(elderlyId);
        bind.setRelationName("儿子");
        bind.setIsPrimary(1);
        familyElderlyBindMapper.insert(bind);

        LocationLog log = new LocationLog();
        log.setElderlyId(elderlyId);
        log.setScene("SOS");
        log.setLatitude(new BigDecimal("31.1200"));
        log.setLongitude(new BigDecimal("121.1200"));
        log.setAddress("测试地址C");
        log.setLogTime(LocalDateTime.now());
        log.setTriggerType(1);
        locationLogMapper.insert(log);

        MvcResult result = mockMvc.perform(get("/family/location/sos/list")
                        .param("familyId", familyId.toString())
                        .param("elderlyId", elderlyId.toString()))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        Assertions.assertTrue(body.contains("测试地址C"));
    }
}
