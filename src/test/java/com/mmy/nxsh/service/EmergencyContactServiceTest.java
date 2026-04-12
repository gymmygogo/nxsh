package com.mmy.nxsh.service;

import com.mmy.nxsh.controller.dto.EmergencyContactSaveRequest;
import com.mmy.nxsh.entity.FamilyElderlyBind;
import com.mmy.nxsh.mapper.FamilyElderlyBindMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EmergencyContactServiceTest {

    @Autowired
    private EmergencyContactService emergencyContactService;

    @Autowired
    private FamilyElderlyBindMapper familyElderlyBindMapper;

    @Test
    void addContact() {
        Long familyId = 5001L;
        Long elderlyId = 6001L;
        bindFamily(familyId, elderlyId);

        EmergencyContactSaveRequest first = buildRequest(familyId, elderlyId, 1, "张三", "13900000001");
        emergencyContactService.addContact(first);

        EmergencyContactSaveRequest second = buildRequest(familyId, elderlyId, 1, "李四", "13900000002");
        Assertions.assertThrows(IllegalArgumentException.class, () -> emergencyContactService.addContact(second));
    }

    @Test
    // 优先级无效时应失败
    void priorityInvalid() {
        Long familyId = 5002L;
        Long elderlyId = 6002L;
        bindFamily(familyId, elderlyId);

        EmergencyContactSaveRequest req = buildRequest(familyId, elderlyId, 6, "王五", "13900000003");
        Assertions.assertThrows(IllegalArgumentException.class, () -> emergencyContactService.addContact(req));
    }

    private void bindFamily(Long familyId, Long elderlyId) {
        FamilyElderlyBind bind = new FamilyElderlyBind();
        bind.setFamilyId(familyId);
        bind.setElderlyId(elderlyId);
        bind.setRelationName("家属");
        bind.setIsPrimary(1);
        familyElderlyBindMapper.insert(bind);
    }

    private EmergencyContactSaveRequest buildRequest(Long familyId, Long elderlyId, int priority,
                                                     String name, String phone) {
        EmergencyContactSaveRequest req = new EmergencyContactSaveRequest();
        req.setFamilyId(familyId);
        req.setElderlyId(elderlyId);
        req.setName(name);
        req.setPhone(phone);
        req.setRelationship("儿子");
        req.setPriority(priority);
        return req;
    }
}
