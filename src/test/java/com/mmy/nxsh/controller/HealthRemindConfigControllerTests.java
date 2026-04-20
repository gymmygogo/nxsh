package com.mmy.nxsh.controller;

import com.mmy.nxsh.entity.HealthRemindConfig;
import com.mmy.nxsh.entity.UserElderly;
import com.mmy.nxsh.mapper.UserElderlyMapper;
import com.mmy.nxsh.service.HealthRemindConfigService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
class HealthRemindConfigControllerTests {

    @Autowired
    private UserElderlyMapper userElderlyMapper;

    @Autowired
    private HealthRemindConfigService healthRemindConfigService;

    @Test
    void saveListAndDelete() {
        UserElderly elderly = new UserElderly();
        elderly.setPhone("18800000021");
        elderly.setName("王五");
        userElderlyMapper.insert(elderly);

        HealthRemindConfig saved = healthRemindConfigService.saveOrUpdateConfig(
                elderly.getId(), LocalTime.of(9, 0), true);
        Assertions.assertNotNull(saved.getId());
        Assertions.assertEquals(1, saved.getType().intValue());

        List<HealthRemindConfig> list = healthRemindConfigService.listConfigs(elderly.getId());
        Assertions.assertEquals(1, list.size());

        healthRemindConfigService.deleteConfig(elderly.getId(), LocalTime.of(9, 0));
        Assertions.assertTrue(healthRemindConfigService.listConfigs(elderly.getId()).isEmpty());
    }
}
