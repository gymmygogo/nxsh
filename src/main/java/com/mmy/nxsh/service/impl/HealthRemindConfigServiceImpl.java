package com.mmy.nxsh.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mmy.nxsh.entity.HealthRemindConfig;
import com.mmy.nxsh.mapper.HealthRemindConfigMapper;
import com.mmy.nxsh.service.HealthRemindConfigService;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;

@Service
public class HealthRemindConfigServiceImpl extends ServiceImpl<HealthRemindConfigMapper, HealthRemindConfig>
        implements HealthRemindConfigService {

    private static final int TYPE_BP = 1;

    @Override
    public HealthRemindConfig saveOrUpdateConfig(Long elderlyId, LocalTime remindTime, boolean active) {
        if (elderlyId == null) {
            throw new IllegalArgumentException("elderlyId不能为空");
        }
        if (remindTime == null) {
            throw new IllegalArgumentException("remindTime不能为空");
        }

        LambdaQueryWrapper<HealthRemindConfig> qw = new LambdaQueryWrapper<>();
        qw.eq(HealthRemindConfig::getElderlyId, elderlyId)
                .eq(HealthRemindConfig::getType, TYPE_BP)
                .eq(HealthRemindConfig::getRemindTime, remindTime);

        HealthRemindConfig exist = baseMapper.selectOne(qw);
        if (exist != null) {
            exist.setIsActive(active ? 1 : 0);
            baseMapper.updateById(exist);
            return exist;
        }

        HealthRemindConfig cfg = new HealthRemindConfig();
        cfg.setElderlyId(elderlyId);
        cfg.setType(TYPE_BP);
        cfg.setRemindTime(remindTime);
        cfg.setIsActive(active ? 1 : 0);
        baseMapper.insert(cfg);
        return cfg;
    }

    @Override
    public void deleteConfig(Long elderlyId, LocalTime remindTime) {
        if (elderlyId == null) {
            throw new IllegalArgumentException("elderlyId不能为空");
        }
        if (remindTime == null) {
            throw new IllegalArgumentException("remindTime不能为空");
        }
        baseMapper.delete(new LambdaQueryWrapper<HealthRemindConfig>()
                .eq(HealthRemindConfig::getElderlyId, elderlyId)
                .eq(HealthRemindConfig::getType, TYPE_BP)
                .eq(HealthRemindConfig::getRemindTime, remindTime));
    }

    @Override
    public List<HealthRemindConfig> listConfigs(Long elderlyId) {
        if (elderlyId == null) {
            throw new IllegalArgumentException("elderlyId不能为空");
        }
        return baseMapper.selectList(new LambdaQueryWrapper<HealthRemindConfig>()
                .eq(HealthRemindConfig::getElderlyId, elderlyId)
                .eq(HealthRemindConfig::getType, TYPE_BP)
                .orderByAsc(HealthRemindConfig::getRemindTime));
    }
}
