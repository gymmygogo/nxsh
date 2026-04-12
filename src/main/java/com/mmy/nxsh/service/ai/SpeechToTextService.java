package com.mmy.nxsh.service.ai;

import org.springframework.web.multipart.MultipartFile;

public interface SpeechToTextService {
    String transcribe(MultipartFile voiceFile);
}
