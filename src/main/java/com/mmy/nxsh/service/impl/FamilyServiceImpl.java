package com.mmy.nxsh.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mmy.nxsh.controller.dto.FamilyBindRequestDTO;
import com.mmy.nxsh.controller.dto.FamilyBindStatusResponse;
import com.mmy.nxsh.controller.dto.FamilyLoginDTO;
import com.mmy.nxsh.controller.dto.FamilyLoginResponse;
import com.mmy.nxsh.controller.dto.FamilyRegisterDTO;
import com.mmy.nxsh.entity.FamilyElderlyBind;
import com.mmy.nxsh.entity.UserElderly;
import com.mmy.nxsh.entity.UserFamily;
import com.mmy.nxsh.mapper.FamilyElderlyBindMapper;
import com.mmy.nxsh.mapper.UserElderlyMapper;
import com.mmy.nxsh.mapper.UserFamilyMapper;
import com.mmy.nxsh.service.FamilyService;
import com.mmy.nxsh.service.SmsService;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
public class FamilyServiceImpl extends ServiceImpl<UserFamilyMapper, UserFamily> implements FamilyService {
    private final FamilyElderlyBindMapper familyElderlyBindMapper;
    private final UserElderlyMapper userElderlyMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final SmsService smsService;

    private static final String TOKEN_PREFIX = "family:token:";
    private static final String VERIFY_PREFIX = "family:verify:";
    private static final String REGISTER_CODE_PREFIX = "family:register:code:";

    public FamilyServiceImpl(FamilyElderlyBindMapper familyElderlyBindMapper,
                             UserElderlyMapper userElderlyMapper,
                             StringRedisTemplate stringRedisTemplate,
                             SmsService smsService) {
        this.familyElderlyBindMapper = familyElderlyBindMapper;
        this.userElderlyMapper = userElderlyMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.smsService = smsService;
    }

    @Override
    public void sendRegisterCode(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("手机号不能为空");
        }
        // 检查是否已注册
        boolean exists = this.lambdaQuery().eq(UserFamily::getPhone, phone).exists();
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
    public Long register(FamilyRegisterDTO dto) {
        // 校验验证码
        String cacheCode = stringRedisTemplate.opsForValue().get(REGISTER_CODE_PREFIX + dto.getPhone());
        if (cacheCode == null || cacheCode.isBlank()) {
            throw new IllegalArgumentException("验证码已过期，请重新获取");
        }
        if (!cacheCode.equals(dto.getVerifyCode())) {
            throw new IllegalArgumentException("验证码错误");
        }

        // 手机号查重，避免重复注册
        boolean exists = this.lambdaQuery().eq(UserFamily::getPhone, dto.getPhone()).exists();
        if (exists) {
            throw new IllegalArgumentException("手机号已存在");
        }

        UserFamily entity = new UserFamily();
        BeanUtils.copyProperties(dto, entity);
        // 密码加盐哈希存储，避免明文
        entity.setPassword(BCrypt.hashpw(dto.getPassword()));
        entity.setStatus(1);
        this.save(entity);
        // 注册成功后清除验证码
        stringRedisTemplate.delete(REGISTER_CODE_PREFIX + dto.getPhone());
        return entity.getId();
    }

    @Override
    public FamilyLoginResponse login(FamilyLoginDTO dto) {
        // 按手机号查询账号
        UserFamily family = this.lambdaQuery().eq(UserFamily::getPhone, dto.getPhone()).one();
        if (family == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (family.getStatus() != null && family.getStatus() == 0) {
            throw new IllegalArgumentException("账号已禁用");
        }
        // 校验密码
        if (family.getPassword() == null || !BCrypt.checkpw(dto.getPassword(), family.getPassword())) {
            throw new IllegalArgumentException("手机号或密码错误");
        }
        // 生成登录 token 并写入 Redis，设置 30 天过期
        String token = UUID.randomUUID().toString();

        stringRedisTemplate.opsForValue().set(TOKEN_PREFIX + token, family.getId().toString(), 30, TimeUnit.DAYS);

        FamilyLoginResponse resp = new FamilyLoginResponse();
        resp.setToken(token);
        resp.setFamilyId(family.getId());

        // 🌟 修改后的查询逻辑：查出当前家属绑定的（主）老人记录
        FamilyElderlyBind bind = lambdaQueryBind()
                .eq(FamilyElderlyBind::getFamilyId, family.getId())
                .orderByDesc(FamilyElderlyBind::getIsPrimary)
                .last("limit 1")
                .one();

        // 🌟 根据查询结果组装返回值
        if (bind != null) {
            resp.setHasBound(true);
            resp.setElderlyId(bind.getElderlyId());
        } else {
            resp.setHasBound(false);
            resp.setElderlyId(null);
        }
        return resp;
    }

    @Override
    public void sendBindVerifyCode(String elderlyPhone) {
        if (elderlyPhone == null || elderlyPhone.isBlank()) {
            throw new IllegalArgumentException("老人手机号不能为空");
        }

        // 6 位数字验证码
        String code = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
        stringRedisTemplate.opsForValue().set(VERIFY_PREFIX + elderlyPhone, code, 5, TimeUnit.MINUTES);

        boolean ok = smsService.sendVerifyCode(elderlyPhone, code);
        if (!ok) {
            throw new IllegalArgumentException("验证码发送失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long bindElderly(FamilyBindRequestDTO dto) {
        UserFamily family = this.getById(dto.getFamilyId());
        if (family == null) {
            throw new IllegalArgumentException("家属不存在");
        }

        UserElderly elderly = null;
        if (dto.getElderlyId() != null) {
            elderly = userElderlyMapper.selectById(dto.getElderlyId());
        } else if (dto.getElderlyPhone() != null && !dto.getElderlyPhone().isBlank()) {
            elderly = userElderlyMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserElderly>().eq("phone", dto.getElderlyPhone()));
        }
        if (elderly == null) {
            throw new IllegalArgumentException("老人不存在");
        }

        if (dto.getVerifyCode() == null || dto.getVerifyCode().isBlank()) {
            throw new IllegalArgumentException("验证码不能为空");
        }
        String verifyKey = VERIFY_PREFIX + elderly.getPhone();
        String cacheCode = stringRedisTemplate.opsForValue().get(verifyKey);
        if (cacheCode == null || cacheCode.isBlank()) {
            throw new IllegalArgumentException("验证码已过期");
        }
        if (!cacheCode.equals(dto.getVerifyCode())) {
            throw new IllegalArgumentException("验证码错误");
        }

        boolean exists = lambdaQueryBind().eq(FamilyElderlyBind::getFamilyId, dto.getFamilyId())
                .eq(FamilyElderlyBind::getElderlyId, elderly.getId())
                .exists();
        if (exists) {
            throw new IllegalArgumentException("已绑定，无需重复绑定");
        }

        int isPrimary = dto.getIsPrimary() == null ? 0 : dto.getIsPrimary();
        if (isPrimary == 1) {
            familyElderlyBindMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<FamilyElderlyBind>()
                            .eq("elderly_id", elderly.getId())
                            .set("is_primary", 0));
        }

        FamilyElderlyBind bind = new FamilyElderlyBind();
        bind.setFamilyId(dto.getFamilyId());
        bind.setElderlyId(elderly.getId());
        bind.setRelationName(dto.getRelationName());
        bind.setIsPrimary(isPrimary);
        familyElderlyBindMapper.insert(bind);
        return bind.getId();
    }

    @Override
    public void logout(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        stringRedisTemplate.delete(TOKEN_PREFIX + token);
    }

    @Override
    public FamilyBindStatusResponse getBindStatus(Long familyId) {
        if (familyId == null) {
            throw new IllegalArgumentException("家属ID不能为空");
        }

        FamilyElderlyBind bind = lambdaQueryBind()
                .eq(FamilyElderlyBind::getFamilyId, familyId)
                .orderByDesc(FamilyElderlyBind::getIsPrimary)
                .last("limit 1")
                .one();

        FamilyBindStatusResponse resp = new FamilyBindStatusResponse();
        if (bind == null) {
            resp.setBound(false);
            resp.setElderlyId(null);
        } else {
            resp.setBound(true);
            resp.setElderlyId(bind.getElderlyId());
            resp.setRelationName(bind.getRelationName());
            UserElderly elderly = userElderlyMapper.selectById(bind.getElderlyId());
            if (elderly != null) {
                resp.setElderlyPhone(elderly.getPhone());
            }
        }
        return resp;
    }

    @Override
    public void updateRelation(Long familyId, Long elderlyId, String relationName) {
        if (familyId == null || elderlyId == null) {
            throw new IllegalArgumentException("familyId和elderlyId不能为空");
        }
        if (relationName == null || relationName.isBlank()) {
            throw new IllegalArgumentException("关系称谓不能为空");
        }
        FamilyElderlyBind bind = lambdaQueryBind()
                .eq(FamilyElderlyBind::getFamilyId, familyId)
                .eq(FamilyElderlyBind::getElderlyId, elderlyId)
                .one();
        if (bind == null) {
            throw new IllegalArgumentException("未找到绑定关系");
        }
        bind.setRelationName(relationName);
        familyElderlyBindMapper.updateById(bind);
    }

    private com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper<FamilyElderlyBind> lambdaQueryBind() {
        return new com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper<>(familyElderlyBindMapper);
    }
}
