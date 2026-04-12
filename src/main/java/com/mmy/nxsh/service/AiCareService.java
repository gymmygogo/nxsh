package com.mmy.nxsh.service;

import com.mmy.nxsh.controller.dto.AiCareChatResponse;
import org.springframework.web.multipart.MultipartFile;

public interface AiCareService {

    // 老人端：上传语音并得到 AI 的文字+语音回复
    AiCareChatResponse chatByVoice(Long elderlyId, MultipartFile voiceFile);

    // H5降级：发送文字并得到 AI 的文字+语音回复
    AiCareChatResponse chatByText(Long elderlyId, String text);

    // 再听一遍：拿到上一条回复
    AiCareChatResponse getLastReply(Long elderlyId);
}
