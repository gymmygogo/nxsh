package com.mmy.nxsh.controller.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MarkMedicineTakenRequest {
    @NotNull
    private Long elderlyId;

    @NotNull
    private LocalDateTime planTime;

    @NotEmpty
    private List<Long> medicineIds;
}

