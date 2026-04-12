package com.mmy.nxsh.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@ServerEndpoint("/ws/family/{familyId}")
@Component
public class FamilyWebSocketServer {

    // 存放家属的WebSocket session
    private static final ConcurrentHashMap<Long, Session> sessionMap = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("familyId") Long familyId) {
        sessionMap.put(familyId, session);
    }

    @OnClose
    public void onClose(@PathParam("familyId") Long familyId) {
        sessionMap.remove(familyId);
    }

    @OnError
    public void onError(Session session, Throwable error, @PathParam("familyId") Long familyId) {
        sessionMap.remove(familyId);
        if (error instanceof java.io.EOFException) {
            log.debug("家属端 WebSocket 连接断开: familyId={}", familyId);
        } else {
            log.warn("家属端 WebSocket 异常: familyId={}, error={}", familyId, error.getMessage());
        }
    }

    public static void sendMessage(Long familyId, String message) {
        Session session = sessionMap.get(familyId);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}