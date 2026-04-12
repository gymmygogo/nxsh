package com.mmy.nxsh.controller;

import com.mmy.nxsh.common.ApiResponse;
import com.mmy.nxsh.controller.dto.ElderlyLoginDTO;
import com.mmy.nxsh.controller.dto.ElderlyLoginResponse;
import com.mmy.nxsh.controller.dto.ElderlyProfileUpdateDTO;
import com.mmy.nxsh.controller.dto.ElderlyRegisterDTO;
import com.mmy.nxsh.controller.dto.GuardianSettingsDTO;
import com.mmy.nxsh.controller.dto.SendCodeDTO;
import com.mmy.nxsh.entity.UserElderly;
import com.mmy.nxsh.service.ElderlyService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/elderly")
public class ElderlyController {

    private final ElderlyService elderlyService;

    @Autowired
    public ElderlyController(ElderlyService elderlyService) {
        this.elderlyService = elderlyService;
    }

    @PostMapping("/sendCode")
    public ApiResponse<Void> sendCode(@Valid @RequestBody SendCodeDTO dto) {
        elderlyService.sendRegisterCode(dto.getPhone());
        return ApiResponse.success(null);
    }

    @PostMapping("/register")
    public ApiResponse<Long> register(@Valid @RequestBody ElderlyRegisterDTO dto) {
        return ApiResponse.success(elderlyService.register(dto));
    }

    @PostMapping("/login")
    public ApiResponse<ElderlyLoginResponse> login(@Valid @RequestBody ElderlyLoginDTO dto) {
        ElderlyLoginResponse resp = elderlyService.login(dto);
        return ApiResponse.success(resp);
    }

    @GetMapping("/profile")
    public ApiResponse<UserElderly> profile(@RequestParam Long elderlyId) {
        return ApiResponse.success(elderlyService.getProfile(elderlyId));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        elderlyService.logout(token);
        return ApiResponse.success(null);
    }

    @PostMapping("/heartbeat")
    public ApiResponse<Void> heartbeat(@RequestParam Long elderlyId) {
        elderlyService.heartbeat(elderlyId);
        return ApiResponse.success(null);
    }

    @PostMapping("/guardian/settings")
    public ApiResponse<Void> saveGuardianSettings(@RequestBody GuardianSettingsDTO dto) {
        elderlyService.updateGuardianSettings(dto);
        return ApiResponse.success(null);
    }

    @GetMapping("/guardian/settings")
    public ApiResponse<UserElderly> getGuardianSettings(@RequestParam Long elderlyId) {
        return ApiResponse.success(elderlyService.getProfile(elderlyId));
    }

    @PostMapping("/profile/update")
    public ApiResponse<Void> updateProfile(@RequestBody ElderlyProfileUpdateDTO dto) {
        elderlyService.updateProfile(dto);
        return ApiResponse.success(null);
    }
}
