package com.mmy.nxsh.service.ai.impl;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mmy.nxsh.service.ai.ChatModelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class SimpleChatModelServiceImpl implements ChatModelService {

    @Value("${ai.siliconflow.api-key}")
    private String apiKey;

    @Value("${ai.siliconflow.api-url}")
    private String apiUrl;

    @Value("${ai.siliconflow.model}")
    private String model;

    private static final String SYSTEM_PROMPT = """
            你是"暖夕"，一位温暖、耐心、善解人意的AI心理关怀陪伴助手，专门陪伴老年人聊天。
            请遵循以下原则：
            1. 用温暖亲切的语气，像晚辈关心长辈一样说话，称呼对方为"您"。
            2. 回复要简短温馨，每次不超过100字，避免长篇大论，方便老人阅读和收听。
            3. 积极倾听，多用共情和肯定的话语，让老人感到被理解和关心。
            4. 如果老人表达孤独、难过等负面情绪，给予安慰和鼓励，但不要说教。
            5. 适当引导老人回忆美好往事、聊聊日常生活、兴趣爱好等积极话题。
            6. 绝对不提供任何医疗建议或诊断，如遇健康问题建议咨询医生。
            7. 保持对话的连贯性和自然感，像朋友聊天一样轻松。
            """;

    private static final Gson GSON = new Gson();

    @Override
    public String reply(String userText) {
        if (userText == null || userText.isBlank()) {
            throw new IllegalArgumentException("用户文本不能为空");
        }

        try {
            JsonObject requestBody = buildRequestBody(userText);
            String responseJson = doPost(requestBody.toString());
            return extractReply(responseJson);
        } catch (Exception e) {
            log.error("调用硅基流动AI接口失败", e);
            return "哎呀，我刚才走神了。您再说一遍好吗？我一直在听着呢。";
        }
    }

    private JsonObject buildRequestBody(String userText) {
        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", SYSTEM_PROMPT);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userText);

        JsonArray messages = new JsonArray();
        messages.add(systemMsg);
        messages.add(userMsg);

        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.add("messages", messages);
        body.addProperty("max_tokens", 256);
        body.addProperty("temperature", 0.8);
        return body;
    }

    private String doPost(String jsonBody) throws Exception {
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(60000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
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
            log.error("硅基流动API返回异常: status={}, body={}", status, resp);
            throw new RuntimeException("AI接口调用失败，HTTP状态码: " + status);
        }
        return resp;
    }

    private String extractReply(String responseJson) {
        JsonObject root = JsonParser.parseString(responseJson).getAsJsonObject();
        JsonArray choices = root.getAsJsonArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("AI返回的choices为空");
        }
        JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
        String content = message.get("content").getAsString();

        // Qwen3模型可能返回<think>...</think>标签包裹的思考过程，需要去除
        if (content.contains("</think>")) {
            content = content.substring(content.indexOf("</think>") + "</think>".length());
        }

        return content.trim();
    }
}
