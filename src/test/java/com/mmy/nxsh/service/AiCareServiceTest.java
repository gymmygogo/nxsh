package com.mmy.nxsh.service;

import com.mmy.nxsh.controller.dto.AiCareChatResponse;
import com.mmy.nxsh.mapper.AiCareReplyMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
class AiCareServiceTest {

    @Autowired
    private AiCareService aiCareService;

    @Autowired
    private AiCareReplyMapper aiCareReplyMapper;

    @Test
    // 语音聊天成功后，应保存一条回复记录
    void chatByVoice() {
        MockMultipartFile voice = new MockMultipartFile(
                "voiceFile",
                "a.wav",
                "application/octet-stream",
                "fake".getBytes()
        );

        AiCareChatResponse resp = aiCareService.chatByVoice(1L, voice);
        Assertions.assertNotNull(resp.getReplyId());
        Assertions.assertNotNull(resp.getAiText());

        Assertions.assertEquals(1, aiCareReplyMapper.selectList(null).size());
    }

    @Test
    // 没有历史数据时，“再听一遍”应抛异常
    void lastReplyNoData() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> aiCareService.getLastReply(99L));
    }
}
