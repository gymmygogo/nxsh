package com.mmy.nxsh.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mmy.nxsh.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_chat_log")
public class AiChatLog extends BaseEntity {
    private Long elderlyId;
    private Long userId;
    private String userMessage;
    private String aiResponse;
    private String contentText;
    private LocalDateTime chatTime;
}