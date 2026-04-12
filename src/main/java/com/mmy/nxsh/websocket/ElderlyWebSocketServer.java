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
@ServerEndpoint("/ws/elderly/{elderlyId}")
@Component
public class ElderlyWebSocketServer {

    // 存放老人的WebSocket session
    private static final ConcurrentHashMap<Long, Session> sessionMap = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("elderlyId") Long elderlyId) {
        sessionMap.put(elderlyId, session);
    }

    @OnClose
    public void onClose(@PathParam("elderlyId") Long elderlyId) {
        sessionMap.remove(elderlyId);
    }

    @OnError
    public void onError(Session session, Throwable error, @PathParam("elderlyId") Long elderlyId) {
        sessionMap.remove(elderlyId);
        if (error instanceof java.io.EOFException) {
            log.debug("老人端 WebSocket 连接断开: elderlyId={}", elderlyId);
        } else {
            log.warn("老人端 WebSocket 异常: elderlyId={}, error={}", elderlyId, error.getMessage());
        }
    }

    public static void sendMessage(Long elderlyId, String message) {
        Session session = sessionMap.get(elderlyId);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}