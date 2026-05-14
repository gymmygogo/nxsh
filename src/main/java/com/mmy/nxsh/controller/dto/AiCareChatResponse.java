package com.mmy.nxsh.controller.dto;

import lombok.Data;

@Data
public class AiCareChatResponse {
    private Long replyId;

    /**
     * 用户本次输入的文字。
     * - 语音聊天：为语音识别结果
     * - 文字聊天：为用户发送的原文（trim 后）
     */
    private String userText;

    private String aiText;
    private String aiVoiceUrl;
}
