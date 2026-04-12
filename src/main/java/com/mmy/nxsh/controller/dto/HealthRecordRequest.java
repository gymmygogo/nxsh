package com.mmy.nxsh.controller.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HealthRecordRequest {
    private Long elderlyId;
    private String deviceId;

    /**
     * 按键触发类型：SHORT=单击(偏高) DOUBLE=双击(偏低) LONG=长按(正常)
     */
    private String pressType;

    // 血压收缩压/舒张压（允许不传，按 pressType 使用默认值）
    private Integer sys;
    private Integer dia;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime recordTime;
    private String note;
}
