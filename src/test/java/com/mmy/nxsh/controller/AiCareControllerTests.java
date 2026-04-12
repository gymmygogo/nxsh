package com.mmy.nxsh.controller;

import com.mmy.nxsh.mapper.AiCareReplyMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class AiCareControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AiCareReplyMapper aiCareReplyMapper;

    @Test
    // 缺少语音文件时，应返回 400
    void paramMissing() throws Exception {
        mockMvc.perform(multipart("/ai/care/chat/voice")
                        .param("elderlyId", "1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    // 正常语音聊天后，应能“再听一遍”拿到上一条回复
    void chatAndLast() throws Exception {
        MockMultipartFile voice = new MockMultipartFile(
                "voiceFile",
                "a.wav",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                "fake".getBytes()
        );

        MvcResult chatResult = mockMvc.perform(multipart("/ai/care/chat/voice")
                        .file(voice)
                        .param("elderlyId", "1"))
                .andExpect(status().isOk())
                .andReturn();

        String chatResp = chatResult.getResponse().getContentAsString();
        Assertions.assertTrue(chatResp.contains("aiText"));

        Assertions.assertFalse(aiCareReplyMapper.selectList(null).isEmpty());

        MvcResult last = mockMvc.perform(get("/ai/care/chat/last")
                        .param("elderlyId", "1"))
                .andExpect(status().isOk())
                .andReturn();

        String lastResp = last.getResponse().getContentAsString();
        Assertions.assertTrue(lastResp.contains("aiVoiceUrl"));
    }
}
