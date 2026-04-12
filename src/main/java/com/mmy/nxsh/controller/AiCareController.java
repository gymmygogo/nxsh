package com.mmy.nxsh.controller;

import com.mmy.nxsh.common.ApiResponse;
import com.mmy.nxsh.controller.dto.AiCareChatResponse;
import com.mmy.nxsh.service.AiCareService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/ai/care")
public class AiCareController {

    private final AiCareService aiCareService;

    public AiCareController(AiCareService aiCareService) {
        this.aiCareService = aiCareService;
    }

    // 老人端：按住说话 -> 松手发送
    @PostMapping("/chat/voice")
    public ApiResponse<AiCareChatResponse> chatByVoice(@RequestParam Long elderlyId,
                                                       @RequestParam MultipartFile voiceFile) {
        return ApiResponse.success(aiCareService.chatByVoice(elderlyId, voiceFile));
    }

    // 一键重听：拿上一条 AI 回复
    @GetMapping("/chat/last")
    public ApiResponse<AiCareChatResponse> lastReply(@RequestParam Long elderlyId) {
        return ApiResponse.success(aiCareService.getLastReply(elderlyId));
    }

    // H5降级：文字聊天
    @PostMapping("/chat/text")
    public ApiResponse<AiCareChatResponse> chatByText(@RequestBody java.util.Map<String, Object> body) {
        Object elderlyIdObj = body.get("elderlyId");
        Object textObj = body.get("text");
        Long elderlyId = elderlyIdObj == null ? null : Long.valueOf(String.valueOf(elderlyIdObj));
        String text = textObj == null ? null : String.valueOf(textObj);
        return ApiResponse.success(aiCareService.chatByText(elderlyId, text));
    }
}
