package com.mmy.nxsh.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class SmsService {

    @Value("${sms.ihuyi.account}")
    private String account;

    @Value("${sms.ihuyi.api-key}")
    private String apiKey;

    private static final String API_URL = "https://api.ihuyi.com/sms/Submit.json";

    /**
     * 通过互亿无线发送验证码短信
     * 使用系统默认模板 ID=1：「您的验证码是：【变量】。请不要把验证码泄露给其他人。」
     */
    public boolean sendVerifyCode(String phone, String code) {
        try {
            String params = "account=" + URLEncoder.encode(account, StandardCharsets.UTF_8)
                    + "&password=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
                    + "&mobile=" + URLEncoder.encode(phone, StandardCharsets.UTF_8)
                    + "&content=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                    + "&templateid=1";

            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(params.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
            }

            String responseBody = sb.toString();
            log.info("互亿无线短信响应: {}", responseBody);

            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            int resultCode = json.get("code").getAsInt();

            if (resultCode == 2) {
                log.info("验证码发送成功: phone={}, smsid={}", phone, json.get("smsid").getAsString());
                return true;
            }

            log.warn("验证码发送失败: phone={}, code={}, msg={}", phone, resultCode, json.get("msg").getAsString());
            return false;

        } catch (Exception e) {
            log.error("发送验证码异常: phone={}", phone, e);
            return false;
        }
    }
}
