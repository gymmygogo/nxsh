package com.mmy.nxsh.task;

import com.mmy.nxsh.entity.FamilyElderlyBind;
import com.mmy.nxsh.entity.MedicineInfo;
import com.mmy.nxsh.entity.UserElderly;
import com.mmy.nxsh.mapper.FamilyElderlyBindMapper;
import com.mmy.nxsh.mapper.MedicineInfoMapper;
import com.mmy.nxsh.mapper.UserElderlyMapper;
import com.mmy.nxsh.service.MedicineService;
import com.mmy.nxsh.websocket.ElderlyWebSocketServer;
import com.mmy.nxsh.websocket.FamilyWebSocketServer;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class MedicineReminderTask {

    private final MedicineService medicineService;
    private final UserElderlyMapper userElderlyMapper;
    private final FamilyElderlyBindMapper familyElderlyBindMapper;
    private final MedicineInfoMapper medicineInfoMapper;

    public MedicineReminderTask(MedicineService medicineService,
                               UserElderlyMapper userElderlyMapper,
                               FamilyElderlyBindMapper familyElderlyBindMapper,
                               MedicineInfoMapper medicineInfoMapper) {
        this.medicineService = medicineService;
        this.userElderlyMapper = userElderlyMapper;
        this.familyElderlyBindMapper = familyElderlyBindMapper;
        this.medicineInfoMapper = medicineInfoMapper;
    }

    /**
     * 每分钟检查一次用药提醒
     */
    @Scheduled(cron = "0 * * * * ?") // 每分钟执行一次
    public void checkMedicineReminders() {
        // 获取当前时间
        LocalDateTime now = LocalDateTime.now();
        
        // 检查是否在免打扰时段（22:00-06:00）
        if (isInQuietHours(now)) {
            return;
        }

        // 获取所有活跃的老人用户（假设status=1表示活跃用户，isDeleted=0表示未删除）
        LambdaQueryWrapper<UserElderly> elderlyWrapper = new LambdaQueryWrapper<>();
        elderlyWrapper.eq(UserElderly::getStatus, 1) // 假设status=1表示活跃用户
                     .eq(UserElderly::getIsDeleted, 0); // 未删除的用户
        List<UserElderly> elders = userElderlyMapper.selectList(elderlyWrapper);

        for (UserElderly elder : elders) {
            // 检查当前时间是否有待服用的药物
            List<com.mmy.nxsh.controller.dto.DueMedicineGroupDTO> dueMedicines = 
                medicineService.getDueMedicines(elder.getId(), now);
            
            if (!dueMedicines.isEmpty()) {
                // 向老人推送用药提醒
                notifyElderly(elder.getId(), dueMedicines);
                
                // 向绑定的家属推送用药提醒
                notifyFamilyMembers(elder.getId(), dueMedicines);
            }
        }
    }

    /**
     * 检查是否在免打扰时段（22:00-06:00）
     */
    private boolean isInQuietHours(LocalDateTime time) {
        int hour = time.getHour();
        return hour >= 22 || hour < 6; // 22:00-05:59为免打扰时段
    }

    /**
     * 向老人推送用药提醒
     */
    private void notifyElderly(Long elderlyId, List<com.mmy.nxsh.controller.dto.DueMedicineGroupDTO> dueMedicines) {
        // 推送用药提醒到老人端
        JSONObject reminderMsg = JSONUtil.createObj()
                .set("type", "MEDICINE_REMINDER")
                .set("elderlyId", elderlyId)
                .set("dueMedicines", dueMedicines)
                .set("msg", "到了用药时间，请按时服药");
        ElderlyWebSocketServer.sendMessage(elderlyId, reminderMsg.toString());
    }

    /**
     * 向绑定的家属推送用药提醒
     */
    private void notifyFamilyMembers(Long elderlyId, List<com.mmy.nxsh.controller.dto.DueMedicineGroupDTO> dueMedicines) {
        // 获取绑定的家属
        LambdaQueryWrapper<FamilyElderlyBind> bindWrapper = new LambdaQueryWrapper<>();
        bindWrapper.eq(FamilyElderlyBind::getElderlyId, elderlyId);
        List<FamilyElderlyBind> binds = familyElderlyBindMapper.selectList(bindWrapper);

        for (FamilyElderlyBind bind : binds) {
            Long familyId = bind.getFamilyId();

            // 推送用药提醒
            JSONObject reminderMsg = JSONUtil.createObj()
                    .set("type", "MEDICINE_REMINDER")
                    .set("elderlyId", elderlyId)
                    .set("dueMedicines", dueMedicines)
                    .set("msg", "老人到了用药时间，请关注");
            FamilyWebSocketServer.sendMessage(familyId, reminderMsg.toString());
        }
    }
}