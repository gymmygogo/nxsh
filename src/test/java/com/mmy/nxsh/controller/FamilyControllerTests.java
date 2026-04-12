package com.mmy.nxsh.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmy.nxsh.controller.dto.FamilyBindRequestDTO;
import com.mmy.nxsh.controller.dto.FamilyRegisterDTO;
import com.mmy.nxsh.controller.dto.FamilySendVerifyDTO;
import com.mmy.nxsh.entity.FamilyElderlyBind;
import com.mmy.nxsh.entity.UserElderly;
import com.mmy.nxsh.entity.UserFamily;
import com.mmy.nxsh.mapper.FamilyElderlyBindMapper;
import com.mmy.nxsh.mapper.UserElderlyMapper;
import com.mmy.nxsh.mapper.UserFamilyMapper;
import com.mmy.nxsh.service.SmsService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class FamilyControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserFamilyMapper userFamilyMapper;

    @Autowired
    private UserElderlyMapper userElderlyMapper;

    @Autowired
    private FamilyElderlyBindMapper familyElderlyBindMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @MockBean
    private SmsService smsService;

    private static final String VERIFY_PREFIX = "family:verify:";

    @Test
    void register() throws Exception {
        FamilyRegisterDTO dto = new FamilyRegisterDTO();
        dto.setPhone("19900000001");
        dto.setName("李四");
        dto.setPassword("123456");

        MvcResult result = mockMvc.perform(post("/family/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        Assertions.assertTrue(response.contains("\"code\":200"));

        UserFamily saved = userFamilyMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserFamily>().eq("phone", "19900000001"));
        Assertions.assertNotNull(saved);
        Assertions.assertEquals("李四", saved.getName());
        Assertions.assertNotNull(saved.getCreateTime());
    }

    @Test
    void sendVerify() throws Exception {
        when(smsService.sendVerifyCode(eq("17700000009"), anyString())).thenReturn(true);

        FamilySendVerifyDTO dto = new FamilySendVerifyDTO();
        dto.setElderlyPhone("17700000009");

        mockMvc.perform(post("/family/sendVerify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        String cacheCode = stringRedisTemplate.opsForValue().get(VERIFY_PREFIX + "17700000009");
        Assertions.assertNotNull(cacheCode);
        Assertions.assertEquals(6, cacheCode.length());
    }

    @Test
    void bind() throws Exception {
        UserFamily family = new UserFamily();
        family.setPhone("18800000001");
        family.setName("家属A");
        userFamilyMapper.insert(family);

        UserElderly elderly = new UserElderly();
        elderly.setPhone("17700000001");
        elderly.setName("老人A");
        userElderlyMapper.insert(elderly);

        // 先写入 Redis 验证码，匹配 bind 的新校验逻辑
        stringRedisTemplate.opsForValue().set(VERIFY_PREFIX + elderly.getPhone(), "123456", 5, TimeUnit.MINUTES);

        FamilyBindRequestDTO dto = new FamilyBindRequestDTO();
        dto.setFamilyId(family.getId());
        dto.setElderlyId(elderly.getId());
        dto.setElderlyPhone(elderly.getPhone());
        dto.setVerifyCode("123456");
        dto.setRelationName("女儿");
        dto.setIsPrimary(1);

        mockMvc.perform(post("/family/bind")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        FamilyElderlyBind bind = familyElderlyBindMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<FamilyElderlyBind>()
                .eq("family_id", family.getId())
                .eq("elderly_id", elderly.getId()));
        Assertions.assertNotNull(bind);
        Assertions.assertEquals("女儿", bind.getRelationName());
        Assertions.assertEquals(1, bind.getIsPrimary());
    }

    @Test
    void bindDuplicate() throws Exception {
        UserFamily family = new UserFamily();
        family.setPhone("18800000002");
        family.setName("家属B");
        userFamilyMapper.insert(family);

        UserElderly elderly = new UserElderly();
        elderly.setPhone("17700000002");
        elderly.setName("老人B");
        userElderlyMapper.insert(elderly);

        stringRedisTemplate.opsForValue().set(VERIFY_PREFIX + elderly.getPhone(), "123456", 5, TimeUnit.MINUTES);

        FamilyBindRequestDTO dto = new FamilyBindRequestDTO();
        dto.setFamilyId(family.getId());
        dto.setElderlyId(elderly.getId());
        dto.setElderlyPhone(elderly.getPhone());
        dto.setVerifyCode("123456");
        dto.setRelationName("儿子");
        dto.setIsPrimary(0);

        mockMvc.perform(post("/family/bind")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult result = mockMvc.perform(post("/family/bind")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andReturn();

        Assertions.assertTrue(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8).contains("已绑定"));
    }

    @Test
    void switchPrimary() throws Exception {
        UserFamily family1 = new UserFamily();
        family1.setPhone("18800000003");
        family1.setName("家属C1");
        userFamilyMapper.insert(family1);

        UserFamily family2 = new UserFamily();
        family2.setPhone("18800000004");
        family2.setName("家属C2");
        userFamilyMapper.insert(family2);

        UserElderly elderly = new UserElderly();
        elderly.setPhone("17700000003");
        elderly.setName("老人C");
        userElderlyMapper.insert(elderly);

        stringRedisTemplate.opsForValue().set(VERIFY_PREFIX + elderly.getPhone(), "123456", 5, TimeUnit.MINUTES);

        FamilyBindRequestDTO dto1 = new FamilyBindRequestDTO();
        dto1.setFamilyId(family1.getId());
        dto1.setElderlyId(elderly.getId());
        dto1.setElderlyPhone(elderly.getPhone());
        dto1.setVerifyCode("123456");
        dto1.setRelationName("女儿");
        dto1.setIsPrimary(1);
        mockMvc.perform(post("/family/bind")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto1)))
                .andExpect(status().isOk());

        // refresh code for second bind
        stringRedisTemplate.opsForValue().set(VERIFY_PREFIX + elderly.getPhone(), "123456", 5, TimeUnit.MINUTES);

        FamilyBindRequestDTO dto2 = new FamilyBindRequestDTO();
        dto2.setFamilyId(family2.getId());
        dto2.setElderlyId(elderly.getId());
        dto2.setElderlyPhone(elderly.getPhone());
        dto2.setVerifyCode("123456");
        dto2.setRelationName("儿子");
        dto2.setIsPrimary(1);
        mockMvc.perform(post("/family/bind")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto2)))
                .andExpect(status().isOk());

        FamilyElderlyBind oldBind = familyElderlyBindMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<FamilyElderlyBind>()
                .eq("family_id", family1.getId())
                .eq("elderly_id", elderly.getId()));
        FamilyElderlyBind newBind = familyElderlyBindMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<FamilyElderlyBind>()
                .eq("family_id", family2.getId())
                .eq("elderly_id", elderly.getId()));

        Assertions.assertNotNull(oldBind);
        Assertions.assertEquals(0, oldBind.getIsPrimary());
        Assertions.assertNotNull(newBind);
        Assertions.assertEquals(1, newBind.getIsPrimary());
    }

    @Test
    void missingParams() throws Exception {
        FamilyBindRequestDTO dto = new FamilyBindRequestDTO();

        MvcResult result = mockMvc.perform(post("/family/bind")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andReturn();

        String response = result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        Assertions.assertTrue(response.contains("不能为空"));
    }
}
