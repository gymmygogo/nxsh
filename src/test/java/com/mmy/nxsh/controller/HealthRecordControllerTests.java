package com.mmy.nxsh.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmy.nxsh.controller.dto.HealthRecordRequest;
import com.mmy.nxsh.entity.UserElderly;
import com.mmy.nxsh.mapper.UserElderlyMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class HealthRecordControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserElderlyMapper userElderlyMapper;

    @Test
    void short_high() throws Exception {
        Long elderlyId = createElderly();

        HealthRecordRequest request = new HealthRecordRequest();
        request.setElderlyId(elderlyId);
        request.setPressType("SHORT");
        request.setDeviceId("esp32-01");

        MvcResult result = mockMvc.perform(post("/api/health/record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        Assertions.assertEquals(200, root.get("code").asInt());
        Assertions.assertEquals(1, root.get("data").get("status").asInt());
    }

    @Test
    void double_low() throws Exception {
        Long elderlyId = createElderly();

        HealthRecordRequest request = new HealthRecordRequest();
        request.setElderlyId(elderlyId);
        request.setPressType("DOUBLE");
        request.setDeviceId("esp32-01");

        MvcResult result = mockMvc.perform(post("/api/health/record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        Assertions.assertEquals(200, root.get("code").asInt());
        Assertions.assertEquals(2, root.get("data").get("status").asInt());
    }

    @Test
    void long_normal() throws Exception {
        Long elderlyId = createElderly();

        HealthRecordRequest request = new HealthRecordRequest();
        request.setElderlyId(elderlyId);
        request.setPressType("LONG");
        request.setDeviceId("esp32-01");

        MvcResult result = mockMvc.perform(post("/api/health/record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        Assertions.assertEquals(200, root.get("code").asInt());
        Assertions.assertEquals(0, root.get("data").get("status").asInt());
    }

    @Test
    void list() throws Exception {
        Long elderlyId = createElderly();
        HealthRecordRequest request = new HealthRecordRequest();
        request.setElderlyId(elderlyId);
        request.setPressType("LONG");
        mockMvc.perform(post("/api/health/record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/api/health/records")
                        .param("elderlyId", String.valueOf(elderlyId))
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        Assertions.assertEquals(200, root.get("code").asInt());
        Assertions.assertTrue(root.get("data").isArray());
        Assertions.assertTrue(root.get("data").size() >= 1);
    }

    @Test
    void trend() throws Exception {
        Long elderlyId = createElderly();
        HealthRecordRequest request = new HealthRecordRequest();
        request.setElderlyId(elderlyId);
        request.setPressType("LONG");
        mockMvc.perform(post("/api/health/record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        LocalDate today = LocalDate.now();
        MvcResult result = mockMvc.perform(get("/api/health/records/trend")
                        .param("elderlyId", String.valueOf(elderlyId))
                        .param("from", today.toString())
                        .param("to", today.toString()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        Assertions.assertEquals(200, root.get("code").asInt());
        Assertions.assertTrue(root.get("data").isArray());
    }

    @Test
    void range() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/health/records/range"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        Assertions.assertEquals(200, root.get("code").asInt());
        Assertions.assertNotNull(root.get("data").get("sysLow"));
        Assertions.assertNotNull(root.get("data").get("diaHigh"));
    }

    private Long createElderly() {
        UserElderly elderly = new UserElderly();
        elderly.setPhone("18800000011");
        elderly.setName("李四");
        userElderlyMapper.insert(elderly);
        return elderly.getId();
    }
}
