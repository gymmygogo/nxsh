package com.mmy.nxsh.service.ai.impl;

import com.google.gson.JsonObject;
import com.mmy.nxsh.common.MinioUtil;
import com.mmy.nxsh.service.ai.TextToSpeechService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class SimpleTextToSpeechServiceImpl implements TextToSpeechService {

    @Value("${ai.siliconflow.tts-api-key}")
    private String apiKey;

    @Value("${ai.siliconflow.tts-api-url}")
    private String apiUrl;

    @Value("${ai.siliconflow.tts-model}")
    private String model;

    @Value("${ai.siliconflow.tts-voice}")
    private String voice;

    private final MinioUtil minioUtil;

    public SimpleTextToSpeechServiceImpl(MinioUtil minioUtil) {
        this.minioUtil = minioUtil;
    }

    @Override
    public String synthesize(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("文本不能为空");
        }

        try {
            // 构建请求体：MOSS-TTSD 需要 [S1] 标签包裹单人对话
            String input = "[S1]" + text;

            JsonObject body = new JsonObject();
            body.addProperty("model", model);
            body.addProperty("input", input);
            body.addProperty("voice", voice);
            body.addProperty("response_format", "mp3");

            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(120000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int status = conn.getResponseCode();
            if (status < 200 || status >= 300) {
                // 读取错误信息
                try (InputStream errStream = conn.getErrorStream()) {
                    if (errStream != null) {
                        String errBody = new String(errStream.readAllBytes(), StandardCharsets.UTF_8);
                        log.error("硅基流动TTS API返回异常: status={}, body={}", status, errBody);
                    }
                }
                throw new RuntimeException("TTS接口调用失败，HTTP状态码: " + status);
            }

            // 读取二进制音频数据
            byte[] audioData;
            try (InputStream in = conn.getInputStream();
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) != -1) {
                    baos.write(buf, 0, len);
                }
                audioData = baos.toByteArray();
            }

            if (audioData.length == 0) {
                throw new RuntimeException("TTS返回的音频数据为空");
            }

            log.info("TTS合成成功, 音频大小: {} bytes", audioData.length);

            // 上传到 MinIO 并返回可访问的 URL
            String audioUrl = minioUtil.uploadBytes(audioData, "tts", ".mp3", "audio/mpeg");
            log.info("TTS音频已上传: {}", audioUrl);
            return audioUrl;

        } catch (Exception e) {
            log.error("调用硅基流动TTS接口失败", e);
            throw new RuntimeException("语音合成失败: " + e.getMessage(), e);
        }
    }
}
