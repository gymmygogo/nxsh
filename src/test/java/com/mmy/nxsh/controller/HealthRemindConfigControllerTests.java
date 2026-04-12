package com.mmy.nxsh.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmy.nxsh.controller.dto.HealthRemindConfigRequest;
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

import java.time.LocalTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class HealthRemindConfigControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserElderlyMapper userElderlyMapper;

    @Test
    void saveAndList() throws Exception {
        UserElderly elderly = new UserElderly();
        elderly.setPhone("18800000021");
        elderly.setName("王五");
        userElderlyMapper.insert(elderly);

        HealthRemindConfigRequest req = new HealthRemindConfigRequest();
        req.setElderlyId(elderly.getId());
        req.setRemindTime(LocalTime.of(9, 0));
        req.setActive(true);

        MvcResult saveResult = mockMvc.perform(post("/api/health/remind/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode saveRoot = objectMapper.readTree(saveResult.getResponse().getContentAsString());
        Assertions.assertEquals(200, saveRoot.get("code").asInt());
        Assertions.assertEquals(1, saveRoot.get("data").get("type").asInt());

        MvcResult listResult = mockMvc.perform(get("/api/health/remind/configs")
                        .param("elderlyId", String.valueOf(elderly.getId())))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode listRoot = objectMapper.readTree(listResult.getResponse().getContentAsString());
        Assertions.assertEquals(200, listRoot.get("code").asInt());
        Assertions.assertTrue(listRoot.get("data").isArray());
        Assertions.assertEquals(1, listRoot.get("data").size());
    }
}
