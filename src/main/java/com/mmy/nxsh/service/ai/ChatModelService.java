package com.mmy.nxsh.service.ai;

public interface ChatModelService {
    /**
     * Backward-compatible single-turn call.
     */
    default String reply(String userText) {
        return reply(null, userText);
    }

    /**
     * Multi-turn chat with memory scoped by elderlyId.
     */
    String reply(Long elderlyId, String userText);
}
