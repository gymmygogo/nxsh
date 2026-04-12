package com.mmy.nxsh.service.ai;

public interface TextToSpeechService {

    // 返回音频可访问 URL（后续可接真实 TTS）
    String synthesize(String text);
}
