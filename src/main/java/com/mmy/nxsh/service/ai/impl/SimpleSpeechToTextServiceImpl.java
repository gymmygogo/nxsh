package com.mmy.nxsh.service.ai.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mmy.nxsh.service.ai.SpeechToTextService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Service
public class SimpleSpeechToTextServiceImpl implements SpeechToTextService {

    @Value("${ai.siliconflow.stt-api-key}")
    private String apiKey;

    @Value("${ai.siliconflow.stt-api-url}")
    private String apiUrl;

    @Value("${ai.siliconflow.stt-model}")
    private String model;

    @Override
    public String transcribe(MultipartFile voiceFile) {
        if (voiceFile == null || voiceFile.isEmpty()) {
            throw new IllegalArgumentException("语音文件不能为空");
        }

        try {
            String boundary = "----Boundary" + UUID.randomUUID().toString().replace("-", "");
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(60000);

            try (OutputStream os = conn.getOutputStream()) {
                // model 字段
                writeFormField(os, boundary, "model", model);

                // file 字段
                String fileName = voiceFile.getOriginalFilename();
                if (fileName == null || fileName.isBlank()) {
                    fileName = "audio.mp3";
                }
                String contentType = voiceFile.getContentType();
                if (contentType == null || contentType.isBlank()) {
                    contentType = "audio/mpeg";
                }
                log.info("STT上传文件: name={}, contentType={}, size={}bytes", fileName, contentType, voiceFile.getSize());

                os.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
                os.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n").getBytes(StandardCharsets.UTF_8));
                os.write(("Content-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));

                try (InputStream fileIn = voiceFile.getInputStream()) {
                    byte[] buf = new byte[4096];
                    int len;
                    while ((len = fileIn.read(buf)) != -1) {
                        os.write(buf, 0, len);
                    }
                }
                os.write("\r\n".getBytes(StandardCharsets.UTF_8));

                // 结束标记
                os.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int status = conn.getResponseCode();
            BufferedReader br;
            if (status >= 200 && status < 300) {
                br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            } else {
                br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
            }

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();

            String resp = sb.toString();
            if (status < 200 || status >= 300) {
                log.error("硅基流动STT API返回异常: status={}, body={}", status, resp);
                throw new RuntimeException("STT接口调用失败，HTTP状态码: " + status);
            }

            // 解析 {"text": "..."}
            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            String text = root.get("text").getAsString();
            log.info("STT转写结果: {}", text);
            return text;

        } catch (Exception e) {
            log.error("调用硅基流动STT接口失败", e);
            throw new RuntimeException("语音识别失败: " + e.getMessage(), e);
        }
    }

    private void writeFormField(OutputStream os, String boundary, String name, String value) throws Exception {
        os.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        os.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        os.write((value + "\r\n").getBytes(StandardCharsets.UTF_8));
    }
}
