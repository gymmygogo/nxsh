package com.mmy.nxsh.controller;

import com.mmy.nxsh.common.ApiResponse;
import com.mmy.nxsh.controller.dto.LocationReportRequest;
import com.mmy.nxsh.controller.dto.SosReportResponse;
import com.mmy.nxsh.service.LocationSafetyService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/elderly/location")
public class ElderlyLocationController {

    private final LocationSafetyService locationSafetyService;

    public ElderlyLocationController(LocationSafetyService locationSafetyService) {
        this.locationSafetyService = locationSafetyService;
    }

    @PostMapping("/share")
    public ApiResponse<Void> share(@Valid @RequestBody LocationReportRequest req) {
        locationSafetyService.reportShare(req);
        return ApiResponse.success(null);
    }

    @PostMapping("/active")
    public ApiResponse<Void> active(@Valid @RequestBody LocationReportRequest req) {
        locationSafetyService.reportActive(req);
        return ApiResponse.success(null);
    }

    @PostMapping("/sos")
    public ApiResponse<SosReportResponse> sos(@Valid @RequestBody LocationReportRequest req) {
        return ApiResponse.success(locationSafetyService.reportSos(req));
    }
}
