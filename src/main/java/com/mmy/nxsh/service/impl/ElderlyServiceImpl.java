package com.mmy.nxsh.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mmy.nxsh.controller.dto.ElderlyLoginDTO;
import com.mmy.nxsh.controller.dto.ElderlyLoginResponse;
import com.mmy.nxsh.controller.dto.ElderlyProfileUpdateDTO;
import com.mmy.nxsh.controller.dto.ElderlyRegisterDTO;
import com.mmy.nxsh.controller.dto.GuardianSettingsDTO;
import com.mmy.nxsh.entity.UserElderly;
import com.mmy.nxsh.mapper.UserElderlyMapper;
import com.mmy.nxsh.service.ElderlyService;
import com.mmy.nxsh.service.SmsService;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
public class ElderlyServiceImpl extends ServiceImpl<UserElderlyMapper, UserElderly> implements ElderlyService {
    private final StringRedisTemplate stringRedisTemplate;
    private final SmsService smsService;
    private static final String TOKEN_PREFIX = "elderly:token:";
    private static final String REGISTER_CODE_PREFIX = "elderly:register:code:";
    public static final String HEARTBEAT_PREFIX = "elderly:heartbeat:";

    public ElderlyServiceImpl(StringRedisTemplate stringRedisTemplate, SmsService smsService) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.smsService = smsService;
    }

    @Override
    public void sendRegisterCode(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("手机号不能为空");
        }
        // 检查是否已注册
        boolean exists = this.lambdaQuery().eq(UserElderly::getPhone, phone).exists();
        if (exists) {
            throw new IllegalArgumentException("该手机号已注册，请直接登录");
        }
        // 生成 6 位验证码
        String code = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
        stringRedisTemplate.opsForValue().set(REGISTER_CODE_PREFIX + phone, code, 5, TimeUnit.MINUTES);
        boolean ok = smsService.sendVerifyCode(phone, code);
        if (!ok) {
            throw new IllegalArgumentException("验证码发送失败，请稍后重试");
        }
    }

    @Override
    public Long register(ElderlyRegisterDTO dto) {
        // 校验验证码
        String cacheCode = stringRedisTemplate.opsForValue().get(REGISTER_CODE_PREFIX + dto.getPhone());
        if (cacheCode == null || cacheCode.isBlank()) {
            throw new IllegalArgumentException("验证码已过期，请重新获取");
        }
        if (!cacheCode.equals(dto.getVerifyCode())) {
            throw new IllegalArgumentException("验证码错误");
        }

        // 手机号查重，避免重复注册
        boolean exists = this.lambdaQuery().eq(UserElderly::getPhone, dto.getPhone()).exists();
        if (exists) {
            throw new IllegalArgumentException("手机号已存在");
        }

        UserElderly entity = new UserElderly();
        BeanUtils.copyProperties(dto, entity);
        entity.setNickname(dto.getNickname() == null || dto.getNickname().isBlank() ? dto.getName() : dto.getNickname());
        // 密码加盐哈希存储，避免明文
        entity.setPassword(BCrypt.hashpw(dto.getPassword()));
        entity.setStatus(1);
        // 默认开启监护人未激活、设置夜间免打扰时间段
        entity.setGuardianActive(0);
        entity.setDndStartTime(LocalTime.of(22, 0));
        entity.setDndEndTime(LocalTime.of(6, 0));

        this.save(entity);
        // 注册成功后清除验证码
        stringRedisTemplate.delete(REGISTER_CODE_PREFIX + dto.getPhone());
        return entity.getId();
    }

    @Override
    public ElderlyLoginResponse login(ElderlyLoginDTO dto) {
        // 按手机号查询账号
        UserElderly elderly = this.lambdaQuery().eq(UserElderly::getPhone, dto.getPhone()).one();
        if (elderly == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (elderly.getStatus() != null && elderly.getStatus() == 0) {
            throw new IllegalArgumentException("账号已禁用");
        }
        // 校验密码
        if (elderly.getPassword() == null || !BCrypt.checkpw(dto.getPassword(), elderly.getPassword())) {
            throw new IllegalArgumentException("手机号或密码错误");
        }
        // 生成登录 token 并写入 Redis，设置 30 天过期（老人端长期有效）
        String token = UUID.randomUUID().toString();
        stringRedisTemplate.opsForValue().set(TOKEN_PREFIX + token, elderly.getId().toString(), 30, TimeUnit.DAYS);
        return ElderlyLoginResponse.of(token, elderly.getId(), elderly.getName(), elderly.getNickname());
    }

    @Override
    public UserElderly getProfile(Long elderlyId) {
        UserElderly elderly = this.getById(elderlyId);
        if (elderly == null) {
            throw new IllegalArgumentException("老人不存在");
        }
        return elderly;
    }

    @Override
    public void logout(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        stringRedisTemplate.delete(TOKEN_PREFIX + token);
    }

    @Override
    public void heartbeat(Long elderlyId) {
        if (elderlyId == null) {
            throw new IllegalArgumentException("elderlyId不能为空");
        }
        // 写入 Redis，记录最后活跃时间戳（毫秒）
        String now = String.valueOf(System.currentTimeMillis());
        stringRedisTemplate.opsForValue().set(HEARTBEAT_PREFIX + elderlyId, now, 48, TimeUnit.HOURS);
    }

    @Override
    public void updateGuardianSettings(GuardianSettingsDTO dto) {
        if (dto.getElderlyId() == null) {
            throw new IllegalArgumentException("elderlyId不能为空");
        }
        UserElderly elderly = this.getById(dto.getElderlyId());
        if (elderly == null) {
            throw new IllegalArgumentException("老人不存在");
        }
        if (dto.getGuardianActive() != null) {
            elderly.setGuardianActive(dto.getGuardianActive());
        }
        if (dto.getGuardianThreshold() != null) {
            elderly.setGuardianThreshold(dto.getGuardianThreshold());
        }
        if (dto.getDndStartTime() != null) {
            elderly.setDndStartTime(dto.getDndStartTime());
        }
        if (dto.getDndEndTime() != null) {
            elderly.setDndEndTime(dto.getDndEndTime());
        }
        this.updateById(elderly);
    }

    @Override
    public void updateProfile(ElderlyProfileUpdateDTO dto) {
        if (dto.getElderlyId() == null) {
            throw new IllegalArgumentException("elderlyId不能为空");
        }
        UserElderly elderly = this.getById(dto.getElderlyId());
        if (elderly == null) {
            throw new IllegalArgumentException("老人不存在");
        }
        if (dto.getGender() != null) {
            elderly.setGender(dto.getGender());
        }
        if (dto.getAge() != null) {
            elderly.setAge(dto.getAge());
        }
        if (dto.getHeight() != null) {
            elderly.setHeight(dto.getHeight());
        }
        if (dto.getWeight() != null) {
            elderly.setWeight(dto.getWeight());
        }
        if (dto.getChronicDiseases() != null) {
            elderly.setChronicDiseases(dto.getChronicDiseases());
        }
        this.updateById(elderly);
    }
}
