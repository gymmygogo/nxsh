package com.mmy.nxsh.service;

import com.mmy.nxsh.controller.dto.ElderlyLoginDTO;
import com.mmy.nxsh.controller.dto.ElderlyLoginResponse;
import com.mmy.nxsh.controller.dto.ElderlyProfileUpdateDTO;
import com.mmy.nxsh.controller.dto.ElderlyRegisterDTO;
import com.mmy.nxsh.controller.dto.GuardianSettingsDTO;
import com.mmy.nxsh.entity.UserElderly;

public interface ElderlyService {
    void sendRegisterCode(String phone);
    Long register(ElderlyRegisterDTO dto);
    ElderlyLoginResponse login(ElderlyLoginDTO dto);
    UserElderly getProfile(Long elderlyId);
    void logout(String token);
    void heartbeat(Long elderlyId);
    void updateGuardianSettings(GuardianSettingsDTO dto);
    void updateProfile(ElderlyProfileUpdateDTO dto);
}
