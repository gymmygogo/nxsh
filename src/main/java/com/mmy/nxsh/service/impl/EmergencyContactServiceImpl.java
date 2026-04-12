package com.mmy.nxsh.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mmy.nxsh.controller.dto.EmergencyContactSaveRequest;
import com.mmy.nxsh.entity.EmergencyContact;
import com.mmy.nxsh.entity.FamilyElderlyBind;
import com.mmy.nxsh.mapper.EmergencyContactMapper;
import com.mmy.nxsh.mapper.FamilyElderlyBindMapper;
import com.mmy.nxsh.service.EmergencyContactService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EmergencyContactServiceImpl extends ServiceImpl<EmergencyContactMapper, EmergencyContact>
        implements EmergencyContactService {

    private static final int MAX_CONTACTS = 5;
    private static final int MIN_PRIORITY = 1;
    private static final int MAX_PRIORITY = 5;

    // 说明：按当前需求，仅限制最多 5 名联系人，并通过 priority 排序；不强制必须满 3 名。

    private final FamilyElderlyBindMapper familyElderlyBindMapper;

    public EmergencyContactServiceImpl(FamilyElderlyBindMapper familyElderlyBindMapper) {
        this.familyElderlyBindMapper = familyElderlyBindMapper;
    }

    @Override
    public List<EmergencyContact> listForElderly(Long familyId, Long elderlyId) {
        assertFamilyBound(familyId, elderlyId);
        return baseMapper.selectList(
                new LambdaQueryWrapper<EmergencyContact>()
                        .eq(EmergencyContact::getElderlyId, elderlyId)
                        .orderByAsc(EmergencyContact::getPriority)
                        .orderByAsc(EmergencyContact::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addContact(EmergencyContactSaveRequest req) {
        assertFamilyBound(req.getFamilyId(), req.getElderlyId());
        validatePriority(req.getPriority());
        long count = baseMapper.selectCount(
                new LambdaQueryWrapper<EmergencyContact>().eq(EmergencyContact::getElderlyId, req.getElderlyId()));
        if (count >= MAX_CONTACTS) {
            throw new IllegalArgumentException("紧急联系人最多" + MAX_CONTACTS + "人");
        }
        assertPriorityUnique(req.getElderlyId(), req.getPriority(), null);
        EmergencyContact c = new EmergencyContact();
        c.setElderlyId(req.getElderlyId());
        c.setName(req.getName().trim());
        c.setPhone(req.getPhone().trim());
        c.setRelationship(req.getRelationship() != null ? req.getRelationship().trim() : null);
        c.setPriority(req.getPriority());
        baseMapper.insert(c);
        return c.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateContact(Long id, EmergencyContactSaveRequest req) {
        assertFamilyBound(req.getFamilyId(), req.getElderlyId());
        validatePriority(req.getPriority());
        EmergencyContact existing = baseMapper.selectById(id);
        if (existing == null || !existing.getElderlyId().equals(req.getElderlyId())) {
            throw new IllegalArgumentException("联系人不存在");
        }
        assertPriorityUnique(req.getElderlyId(), req.getPriority(), id);
        existing.setName(req.getName().trim());
        existing.setPhone(req.getPhone().trim());
        existing.setRelationship(req.getRelationship() != null ? req.getRelationship().trim() : null);
        existing.setPriority(req.getPriority());
        baseMapper.updateById(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeContact(Long familyId, Long id) {
        EmergencyContact existing = baseMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("联系人不存在");
        }
        assertFamilyBound(familyId, existing.getElderlyId());
        baseMapper.deleteById(id);
    }

    private void assertFamilyBound(Long familyId, Long elderlyId) {
        Long cnt = familyElderlyBindMapper.selectCount(
                new LambdaQueryWrapper<FamilyElderlyBind>()
                        .eq(FamilyElderlyBind::getFamilyId, familyId)
                        .eq(FamilyElderlyBind::getElderlyId, elderlyId));
        if (cnt == null || cnt == 0) {
            throw new IllegalArgumentException("未绑定该老人");
        }
    }

    private void assertPriorityUnique(Long elderlyId, Integer priority, Long excludeId) {
        LambdaQueryWrapper<EmergencyContact> wrapper = new LambdaQueryWrapper<EmergencyContact>()
                .eq(EmergencyContact::getElderlyId, elderlyId)
                .eq(EmergencyContact::getPriority, priority);
        if (excludeId != null) {
            wrapper.ne(EmergencyContact::getId, excludeId);
        }
        Long count = baseMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new IllegalArgumentException("该优先级已被占用");
        }
    }

    private void validatePriority(Integer priority) {
        if (priority == null || priority < MIN_PRIORITY || priority > MAX_PRIORITY) {
            throw new IllegalArgumentException("优先级范围为" + MIN_PRIORITY + "-" + MAX_PRIORITY);
        }
    }
}
