package com.mmy.nxsh.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmy.nxsh.controller.dto.AiCareChatResponse;
import com.mmy.nxsh.entity.AiCareReply;
import com.mmy.nxsh.entity.AiChatLog;
import com.mmy.nxsh.mapper.AiCareReplyMapper;
import com.mmy.nxsh.mapper.AiChatLogMapper;
import com.mmy.nxsh.service.AiCareService;
import com.mmy.nxsh.service.ai.ChatModelService;
import com.mmy.nxsh.service.ai.SpeechToTextService;
import com.mmy.nxsh.service.ai.TextToSpeechService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Slf4j
@Service
public class AiCareServiceImpl implements AiCareService {

    private final SpeechToTextService speechToTextService;
    private final ChatModelService chatModelService;
    private final TextToSpeechService textToSpeechService;
    private final AiChatLogMapper aiChatLogMapper;
    private final AiCareReplyMapper aiCareReplyMapper;

    public AiCareServiceImpl(SpeechToTextService speechToTextService,
                             ChatModelService chatModelService,
                             TextToSpeechService textToSpeechService,
                             AiChatLogMapper aiChatLogMapper,
                             AiCareReplyMapper aiCareReplyMapper) {
        this.speechToTextService = speechToTextService;
        this.chatModelService = chatModelService;
        this.textToSpeechService = textToSpeechService;
        this.aiChatLogMapper = aiChatLogMapper;
        this.aiCareReplyMapper = aiCareReplyMapper;
    }

    @Override
    public AiCareChatResponse chatByVoice(Long elderlyId, MultipartFile voiceFile) {
        if (elderlyId == null) {
            throw new IllegalArgumentException("elderlyId不能为空");
        }
        if (voiceFile == null || voiceFile.isEmpty()) {
            throw new IllegalArgumentException("语音文件不能为空");
        }

        String userText = speechToTextService.transcribe(voiceFile);
        String aiText = chatModelService.reply(userText);
        String aiVoiceUrl = safeSynthesize(aiText);

        AiChatLog log = new AiChatLog();
        log.setElderlyId(elderlyId);
        log.setUserId(elderlyId);
        log.setSenderRole(1);
        log.setUserType(1);
        log.setUserMessage(userText);
        log.setAiResponse(aiText);
        log.setChatTime(LocalDateTime.now());
        aiChatLogMapper.insert(log);

        AiCareReply reply = new AiCareReply();
        reply.setElderlyId(elderlyId);
        reply.setUserText(userText);
        reply.setAiText(aiText);
        reply.setAiVoiceUrl(aiVoiceUrl);
        aiCareReplyMapper.insert(reply);

        AiCareChatResponse resp = new AiCareChatResponse();
        resp.setReplyId(reply.getId());
        resp.setAiText(aiText);
        resp.setAiVoiceUrl(aiVoiceUrl);
        return resp;
    }

    @Override
    public AiCareChatResponse chatByText(Long elderlyId, String text) {
        if (elderlyId == null) {
            throw new IllegalArgumentException("elderlyId不能为空");
        }
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("文本不能为空");
        }

        String userText = text.trim();
        String aiText = chatModelService.reply(userText);
        String aiVoiceUrl = safeSynthesize(aiText);

        AiChatLog log = new AiChatLog();
        log.setElderlyId(elderlyId);
        log.setUserId(elderlyId);
        log.setSenderRole(1);
        log.setUserType(1);
        log.setUserMessage(userText);
        log.setAiResponse(aiText);
        log.setChatTime(LocalDateTime.now());
        aiChatLogMapper.insert(log);

        AiCareReply reply = new AiCareReply();
        reply.setElderlyId(elderlyId);
        reply.setUserText(userText);
        reply.setAiText(aiText);
        reply.setAiVoiceUrl(aiVoiceUrl);
        aiCareReplyMapper.insert(reply);

        AiCareChatResponse resp = new AiCareChatResponse();
        resp.setReplyId(reply.getId());
        resp.setAiText(aiText);
        resp.setAiVoiceUrl(aiVoiceUrl);
        return resp;
    }

    @Override
    public AiCareChatResponse getLastReply(Long elderlyId) {
        if (elderlyId == null) {
            throw new IllegalArgumentException("elderlyId不能为空");
        }

        LambdaQueryWrapper<AiCareReply> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiCareReply::getElderlyId, elderlyId)
                .orderByDesc(AiCareReply::getCreateTime)
                .last("LIMIT 1");
        AiCareReply last = aiCareReplyMapper.selectOne(wrapper);
        if (last == null) {
            throw new IllegalArgumentException("暂无可重听的内容");
        }

        AiCareChatResponse resp = new AiCareChatResponse();
        resp.setReplyId(last.getId());
        resp.setAiText(last.getAiText());
        resp.setAiVoiceUrl(last.getAiVoiceUrl());
        return resp;
    }

    private String safeSynthesize(String aiText) {
        try {
            return textToSpeechService.synthesize(aiText);
        } catch (Exception e) {
            log.warn("TTS合成失败，已降级为纯文本回复: {}", e.getMessage());
            return null;
        }
    }
}
