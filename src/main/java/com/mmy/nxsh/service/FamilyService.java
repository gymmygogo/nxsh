package com.mmy.nxsh.service;

import com.mmy.nxsh.controller.dto.FamilyBindRequestDTO;
import com.mmy.nxsh.controller.dto.FamilyBindStatusResponse;
import com.mmy.nxsh.controller.dto.FamilyLoginDTO;
import com.mmy.nxsh.controller.dto.FamilyLoginResponse;
import com.mmy.nxsh.controller.dto.FamilyRegisterDTO;

public interface FamilyService {
    void sendRegisterCode(String phone);

    Long register(FamilyRegisterDTO dto);

    FamilyLoginResponse login(FamilyLoginDTO dto);

    Long bindElderly(FamilyBindRequestDTO dto);

    /**
     * 发送绑定老人用的验证码（写入 Redis + 调用短信服务）
     */
    void sendBindVerifyCode(String elderlyPhone);

    /**
     * 查询家属是否已绑定老人
     */
    FamilyBindStatusResponse getBindStatus(Long familyId);

    void logout(String token);

    void updateRelation(Long familyId, Long elderlyId, String relationName);
}
