package com.mmy.nxsh.controller;

import com.mmy.nxsh.common.ApiResponse;
import com.mmy.nxsh.controller.dto.FamilyBindRequestDTO;
import com.mmy.nxsh.controller.dto.FamilyBindStatusResponse;
import com.mmy.nxsh.controller.dto.FamilyLoginDTO;
import com.mmy.nxsh.controller.dto.FamilyLoginResponse;
import com.mmy.nxsh.controller.dto.FamilyRegisterDTO;
import com.mmy.nxsh.controller.dto.FamilySendVerifyDTO;
import com.mmy.nxsh.controller.dto.SendCodeDTO;
import com.mmy.nxsh.service.FamilyService;
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
@RequestMapping("/family")
public class FamilyController {

    private final FamilyService familyService;

    @Autowired
    public FamilyController(FamilyService familyService) {
        this.familyService = familyService;
    }

    @PostMapping("/sendCode")
    public ApiResponse<Void> sendCode(@Valid @RequestBody SendCodeDTO dto) {
        familyService.sendRegisterCode(dto.getPhone());
        return ApiResponse.success(null);
    }

    @PostMapping("/register")
    public ApiResponse<Long> register(@Valid @RequestBody FamilyRegisterDTO dto) {
        return ApiResponse.success(familyService.register(dto));
    }

    @PostMapping("/login")
    public ApiResponse<FamilyLoginResponse> login(@Valid @RequestBody FamilyLoginDTO dto) {
        return ApiResponse.success(familyService.login(dto));
    }

    @GetMapping("/bind/status")
    public ApiResponse<FamilyBindStatusResponse> bindStatus(@RequestParam Long familyId) {
        return ApiResponse.success(familyService.getBindStatus(familyId));
    }

    @PostMapping("/sendVerify")
    public ApiResponse<Void> sendVerify(@Valid @RequestBody FamilySendVerifyDTO dto) {
        familyService.sendBindVerifyCode(dto.getElderlyPhone());
        return ApiResponse.success(null);
    }

    @PostMapping("/bind")
    public ApiResponse<Long> bind(@Valid @RequestBody FamilyBindRequestDTO dto) {
        return ApiResponse.success(familyService.bindElderly(dto));
    }

    @PostMapping("/bind/updateRelation")
    public ApiResponse<Void> updateRelation(@RequestBody java.util.Map<String, Object> body) {
        Long fId = body.get("familyId") != null ? Long.valueOf(body.get("familyId").toString()) : null;
        Long eId = body.get("elderlyId") != null ? Long.valueOf(body.get("elderlyId").toString()) : null;
        String relationName = body.get("relationName") != null ? body.get("relationName").toString() : null;
        familyService.updateRelation(fId, eId, relationName);
        return ApiResponse.success(null);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        familyService.logout(token);
        return ApiResponse.success(null);
    }
}
