package com.mmy.nxsh.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmy.nxsh.controller.dto.ElderlyRegisterDTO;
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

import java.nio.charset.StandardCharsets;
import java.time.LocalTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)@AutoConfigureMockMvc
@Transactional
class ElderlyControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserElderlyMapper userElderlyMapper;

    @Test
    void register() throws Exception {
        ElderlyRegisterDTO dto = new ElderlyRegisterDTO();
        dto.setPhone("18800000001");
        dto.setName("张三");
        dto.setGender(1);
        dto.setAge(70);
        dto.setPassword("123456");

        MvcResult result = mockMvc.perform(post("/elderly/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        Assertions.assertTrue(response.contains("\"code\":200"));

        UserElderly saved = userElderlyMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserElderly>().eq("phone", "18800000001"));
        Assertions.assertNotNull(saved);
        Assertions.assertEquals(0, saved.getGuardianActive());
        Assertions.assertEquals(LocalTime.of(22, 0), saved.getDndStartTime());
        Assertions.assertEquals(LocalTime.of(6, 0), saved.getDndEndTime());
        Assertions.assertEquals("张三", saved.getNickname());
        Assertions.assertNotNull(saved.getCreateTime());
    }

    @Test
    void profile() throws Exception {
        UserElderly elderly = new UserElderly();
        elderly.setPhone("18800000002");
        elderly.setName("王五");
        elderly.setNickname("王爷爷");
        userElderlyMapper.insert(elderly);

        MvcResult result = mockMvc.perform(get("/elderly/profile")
                        .param("elderlyId", String.valueOf(elderly.getId())))
                .andExpect(status().isOk())
                .andReturn();

        Assertions.assertTrue(result.getResponse().getContentAsString(StandardCharsets.UTF_8).contains("王爷爷"));    }
}
