package com.mmy.nxsh.controller.dto;

import lombok.Data;

@Data
public class AiCareChatResponse {
    private Long replyId;
    private String aiText;
    private String aiVoiceUrl;
}
