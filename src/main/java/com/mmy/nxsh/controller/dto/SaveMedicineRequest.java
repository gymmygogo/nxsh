package com.mmy.nxsh.controller.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class SaveMedicineRequest {
    // 家属id，用来校验权限
    private Long familyId;
    private Long elderlyId;

    // 第1步
    private String name;
    private String dosageDesc;
    private String photoUrl;
    private Integer currentStock;
    private Integer lowStockThreshold;

    // 第3步：亲情语音URL（上传后回填），为空则用系统默认播报
    private String voiceRemindUrl;

    // 第2步：服用时间列表，一个药可以设多个时间点，如 ["08:00", "21:00"]
    private List<LocalTime> takeTimes;

    // 服用周期
    private LocalDate startDate;
    private LocalDate endDate;
    
    // 用药频率类型：once(单次), daily(每日), weekly(每周), interval(间隔天数)
    private String frequencyType = "daily";
    // 用药频率值：如每周几(1-7)，间隔天数等
    private Integer frequencyValue;
    // 星期几用药，用逗号分隔，如"1,3,5"表示周一、三、五用药
    private String weekDays;
}
