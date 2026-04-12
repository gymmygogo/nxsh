package com.mmy.nxsh.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mmy.nxsh.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_care_reply")
public class AiCareReply extends BaseEntity {
    private Long elderlyId;
    private String userText;
    private String aiText;
    private String aiVoiceUrl;
}
