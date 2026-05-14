package com.mmy.nxsh.service.ai.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mmy.nxsh.entity.AiChatLog;
import com.mmy.nxsh.mapper.AiChatLogMapper;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class SimpleChatModelServiceImpl implements ChatModelService {

    @Value("${ai.siliconflow.api-key}")
    private String apiKey;

    @Value("${ai.siliconflow.api-url}")
    private String apiUrl;

    @Value("${ai.siliconflow.model}")
    private String model;

    /**
     * How many previous turns to load from DB.
     * A "turn" here equals one row in ai_chat_log (user input + ai response).
     */
    @Value("${ai.siliconflow.history-turns:6}")
    private int historyTurns;

    private final AiChatLogMapper aiChatLogMapper;

    public SimpleChatModelServiceImpl(AiChatLogMapper aiChatLogMapper) {
        this.aiChatLogMapper = aiChatLogMapper;
    }

    private static final String SYSTEM_PROMPT = """
        你的身份是“暖夕”，一个专门陪伴老年人的AI助手。你必须用“我”来自称。
        
        重要原则：
        1. 绝对不要对用户说“您是暖夕”或“你是暖夕”。你的名字就是暖夕。
        2. 用尊敬和温暖的语气，称呼用户为\"爷爷\"、\"奶奶\"或\"您\"。
        3. 回复简短（50字以内最好），直接回答问题，语气像家里的晚辈。
        4. 不要编造任何经历（比如看到了什么风景、去过哪里），你是AI，没有物理身体。
        5. 不要给确诊性的医疗建议，如果涉及严重健康问题，请温和地建议去看医生。
        6. 诚实原则：你目前没有联网。如果你不知道答案（比如明天的天气、最新新闻），直接温和地道歉说不知道，绝对不能转移话题或胡乱编造。
        7. 多听老人说话，偶尔主动问问他们的生活、回忆或身体感觉。
        """;

    @Override
    public String reply(Long elderlyId, String userText) {
        if (userText == null || userText.isBlank()) {
            throw new IllegalArgumentException("用户文本不能为空");
        }

        try {
            JsonObject requestBody = buildRequestBody(elderlyId, userText.trim());
            String responseJson = doPost(requestBody.toString());
            return extractReply(responseJson);
        } catch (Exception e) {
            log.error("调用硅基流动AI接口失败", e);
            return "。哎呀，我刚才走神了。您再说一遍好吗？我一直在听着呢。";
        }
    }

    private JsonObject buildRequestBody(Long elderlyId, String userText) {
        JsonArray messages = new JsonArray();

        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", SYSTEM_PROMPT);
        messages.add(systemMsg);

        // Load recent chat memory from DB (if elderlyId is present)
        for (JsonObject his : buildHistoryMessages(elderlyId)) {
            messages.add(his);
        }

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userText);
        messages.add(userMsg);

        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.add("messages", messages);
        body.addProperty("max_tokens", 256);
        body.addProperty("temperature", 0.8);
        return body;
    }

    /**
     * Convert last N turns of ai_chat_log into OpenAI-style messages:
     * user -> assistant -> user -> assistant ...
     */
    private List<JsonObject> buildHistoryMessages(Long elderlyId) {
        if (elderlyId == null || historyTurns <= 0) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<AiChatLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiChatLog::getElderlyId, elderlyId)
                .orderByDesc(AiChatLog::getChatTime)
                .last("LIMIT " + historyTurns);

        List<AiChatLog> logs = aiChatLogMapper.selectList(wrapper);
        if (logs == null || logs.isEmpty()) {
            return Collections.emptyList();
        }

        // We queried DESC, but we need chronological order for messages.
        Collections.reverse(logs);

        List<JsonObject> messages = new ArrayList<>();
        for (AiChatLog logRow : logs) {
            // user
            if (logRow.getUserMessage() != null && !logRow.getUserMessage().isBlank()) {
                JsonObject u = new JsonObject();
                u.addProperty("role", "user");
                u.addProperty("content", logRow.getUserMessage());
                messages.add(u);
            }

            // assistant
            if (logRow.getAiResponse() != null && !logRow.getAiResponse().isBlank()) {
                JsonObject a = new JsonObject();
                a.addProperty("role", "assistant");
                a.addProperty("content", logRow.getAiResponse());
                messages.add(a);
            }
        }

        return messages;
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
