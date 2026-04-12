package com.mmy.nxsh.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mmy.nxsh.controller.dto.EmergencyContactSaveRequest;
import com.mmy.nxsh.entity.EmergencyContact;

import java.util.List;

public interface EmergencyContactService extends IService<EmergencyContact> {

    List<EmergencyContact> listForElderly(Long familyId, Long elderlyId);

    Long addContact(EmergencyContactSaveRequest req);

    void updateContact(Long id, EmergencyContactSaveRequest req);

    void removeContact(Long familyId, Long id);
}
