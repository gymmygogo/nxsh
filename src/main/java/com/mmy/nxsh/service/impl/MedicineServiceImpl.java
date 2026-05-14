package com.mmy.nxsh.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mmy.nxsh.common.MinioUtil;
import com.mmy.nxsh.controller.dto.*;
import com.mmy.nxsh.entity.FamilyElderlyBind;
import com.mmy.nxsh.entity.MedicineInfo;
import com.mmy.nxsh.entity.MedicineLog;
import com.mmy.nxsh.entity.MedicinePlan;
import com.mmy.nxsh.mapper.FamilyElderlyBindMapper;
import com.mmy.nxsh.mapper.MedicineInfoMapper;
import com.mmy.nxsh.mapper.MedicineLogMapper;
import com.mmy.nxsh.mapper.MedicinePlanMapper;
import com.mmy.nxsh.service.MedicineService;
import com.mmy.nxsh.websocket.FamilyWebSocketServer;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class MedicineServiceImpl extends ServiceImpl<MedicineInfoMapper, MedicineInfo> implements MedicineService {

    private final MedicinePlanMapper medicinePlanMapper;
    private final MedicineLogMapper medicineLogMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final FamilyElderlyBindMapper familyElderlyBindMapper;
    private final MinioUtil minioUtil;

    // Redis key前缀：低库存标记，TTL 7天（家属端看板读取）
    private static final String LOW_STOCK_KEY_PREFIX = "medicine:low_stock:";

    public MedicineServiceImpl(MedicinePlanMapper medicinePlanMapper,
                               MedicineLogMapper medicineLogMapper,
                               StringRedisTemplate stringRedisTemplate,
                               FamilyElderlyBindMapper familyElderlyBindMapper,
                               MinioUtil minioUtil) {
        this.medicinePlanMapper = medicinePlanMapper;
        this.medicineLogMapper = medicineLogMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.familyElderlyBindMapper = familyElderlyBindMapper;
        this.minioUtil = minioUtil;
    }

    @Override
    public List<DueMedicineGroupDTO> getDueMedicines(Long elderlyId, LocalDateTime time, Integer windowMinutes) {
        LocalTime targetTime = time.toLocalTime().truncatedTo(ChronoUnit.MINUTES);
        LocalDate today = time.toLocalDate();
        int window = windowMinutes == null ? 0 : Math.max(windowMinutes, 0);

        LambdaQueryWrapper<MedicinePlan> planWrapper = new LambdaQueryWrapper<>();
        planWrapper.eq(MedicinePlan::getElderlyId, elderlyId);
        List<MedicinePlan> plans = medicinePlanMapper.selectList(planWrapper);
        if (plans.isEmpty()) {
            return new ArrayList<>();
        }

        List<MedicinePlan> timeMatchedPlans = plans.stream()
                .filter(plan -> isPlanInTimeWindow(plan.getTakeTime(), targetTime, window))
                .collect(Collectors.toList());
        if (timeMatchedPlans.isEmpty()) {
            return new ArrayList<>();
        }

        // 过滤出符合周期条件的计划
        List<MedicinePlan> filteredPlans = timeMatchedPlans.stream()
                .filter(plan -> isPlanActiveOnDate(plan, today))
                .collect(Collectors.toList());
        
        if (filteredPlans.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> medicineIds = filteredPlans.stream().map(MedicinePlan::getMedicineId).toList();
        LambdaQueryWrapper<MedicineInfo> infoWrapper = new LambdaQueryWrapper<>();
        infoWrapper.in(MedicineInfo::getId, medicineIds)
                .eq(MedicineInfo::getElderlyId, elderlyId)
                .le(MedicineInfo::getStartDate, today)
                .ge(MedicineInfo::getEndDate, today)
                .eq(MedicineInfo::getIsActive, 1);
        List<MedicineInfo> infos = this.list(infoWrapper);
        Map<Long, MedicineInfo> infoMap = infos.stream().collect(Collectors.toMap(MedicineInfo::getId, m -> m));

        Map<LocalTime, List<DueMedicineItemDTO>> grouped = new HashMap<>();
        for (MedicinePlan plan : filteredPlans) {
            MedicineInfo info = infoMap.get(plan.getMedicineId());
            if (info == null) {
                continue;
            }
            DueMedicineItemDTO item = new DueMedicineItemDTO();
            BeanUtils.copyProperties(info, item);
            item.setMedicineId(info.getId());
            grouped.computeIfAbsent(plan.getTakeTime(), k -> new ArrayList<>()).add(item);
        }

        List<DueMedicineGroupDTO> result = new ArrayList<>();
        for (Map.Entry<LocalTime, List<DueMedicineItemDTO>> entry : grouped.entrySet()) {
            DueMedicineGroupDTO group = new DueMedicineGroupDTO();
            group.setPlanTime(LocalDateTime.of(today, entry.getKey()));
            group.setMedicines(entry.getValue());

            // 判断提醒模式：只要有一个药品录了家属语音，就用亲情模式
            String familyVoice = entry.getValue().stream()
                    .map(DueMedicineItemDTO::getVoiceRemindUrl)
                    .filter(url -> url != null && !url.isBlank())
                    .findFirst()
                    .orElse(null);
            if (familyVoice != null) {
                group.setRemindMode("family");
                group.setFamilyVoiceUrl(familyVoice);
            } else {
                group.setRemindMode("standard");
            }

            result.add(group);
        }
        result.sort((a, b) -> a.getPlanTime().compareTo(b.getPlanTime()));
        return result;
    }

    private boolean isPlanInTimeWindow(LocalTime planTime, LocalTime targetTime, int windowMinutes) {
        if (planTime == null) {
            return false;
        }
        int planMinutes = planTime.getHour() * 60 + planTime.getMinute();
        int targetMinutes = targetTime.getHour() * 60 + targetTime.getMinute();
        if (windowMinutes <= 0) {
            return planMinutes == targetMinutes;
        }
        int endMinutes = targetMinutes + windowMinutes;
        if (endMinutes < 1440) {
            return planMinutes >= targetMinutes && planMinutes <= endMinutes;
        }
        return planMinutes >= targetMinutes || planMinutes <= (endMinutes - 1440);
    }
    
    /**
     * 判断用药计划在指定日期是否激活
     */
    private boolean isPlanActiveOnDate(MedicinePlan plan, LocalDate date) {
        Integer freqType = plan.getFrequencyType();
        if (freqType == null) {
            return true;
        }

        // 约定：1=once, 2=daily, 3=weekly, 4=interval（与 mapFrequencyType 一致）
        if (freqType == 1) {
            return true;
        } else if (freqType == 2) {
            return true;
        } else if (freqType == 3) {
            String weekDays = plan.getWeekDays();
            if (weekDays != null && !weekDays.trim().isEmpty()) {
                int dayOfWeek = date.getDayOfWeek().getValue();
                String[] days = weekDays.split(",");
                for (String day : days) {
                    try {
                        if (Integer.parseInt(day.trim()) == dayOfWeek) {
                            return true;
                        }
                    } catch (NumberFormatException e) {
                        // 忽略无效的数字
                    }
                }
                return false;
            }
            return true;
        } else if (freqType == 4) {
            Integer interval = plan.getFrequencyValue();
            if (interval != null && interval > 0) {
                MedicineInfo medicineInfo = this.getById(plan.getMedicineId());
                if (medicineInfo != null && medicineInfo.getStartDate() != null) {
                    LocalDate startDate = medicineInfo.getStartDate();
                    if (startDate.isAfter(date)) {
                        return false;
                    }
                    long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(startDate, date);
                    return daysDiff % interval == 0;
                }
                return true;
            }
            return true;
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markTaken(Long elderlyId, LocalDateTime planTime, List<Long> medicineIds) {
        LocalDate today = planTime.toLocalDate();
        LocalTime takeTime = planTime.toLocalTime();
        for (Long medicineId : medicineIds) {
            MedicineInfo info = this.getById(medicineId);
            if (info == null || info.getIsActive() == null || info.getIsActive() != 1) {
                throw new IllegalArgumentException("药品不存在或已停用");
            }
            if (!info.getElderlyId().equals(elderlyId)) {
                throw new IllegalArgumentException("药品不属于该老人");
            }
            if (info.getStartDate() != null && info.getStartDate().isAfter(today)) {
                throw new IllegalArgumentException("未到服用日期");
            }
            if (info.getEndDate() != null && info.getEndDate().isBefore(today)) {
                throw new IllegalArgumentException("已过服用结束日期");
            }

            // 使用乐观锁进行并发安全的库存扣减
            // 条件：库存>=1 且版本号相同，同时版本号+1
            int currentVersion = info.getVersion() != null ? info.getVersion() : 0;
            LambdaUpdateWrapper<MedicineInfo> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(MedicineInfo::getId, medicineId)
                    .eq(MedicineInfo::getVersion, currentVersion)
                    .ge(MedicineInfo::getCurrentStock, 1)
                    .setSql("current_stock = current_stock - 1, version = version + 1");
            int updated = this.baseMapper.update(null, updateWrapper);
            if (updated == 0) {
                throw new IllegalStateException("库存不足或已被其他请求修改，请重试");
            }

            MedicineLog log = new MedicineLog();
            log.setMedicineId(medicineId);
            log.setElderlyId(elderlyId);
            log.setPlanTime(LocalDateTime.of(today, takeTime));
            log.setTakeTime(LocalDateTime.now());
            log.setStatus(1);
            medicineLogMapper.insert(log);

            MedicineInfo latest = this.getById(medicineId);
            boolean isLowStock = false;
            if (latest.getCurrentStock() != null && latest.getLowStockThreshold() != null
                    && latest.getCurrentStock() <= latest.getLowStockThreshold()) {
                String lowStockKey = LOW_STOCK_KEY_PREFIX + elderlyId + ":" + medicineId;
                stringRedisTemplate.opsForValue().set(lowStockKey, latest.getName(), 7, TimeUnit.DAYS);
                isLowStock = true;
            }

            LambdaQueryWrapper<FamilyElderlyBind> bindWrapper = new LambdaQueryWrapper<>();
            bindWrapper.eq(FamilyElderlyBind::getElderlyId, elderlyId);
            List<FamilyElderlyBind> binds = familyElderlyBindMapper.selectList(bindWrapper);

            for (FamilyElderlyBind bind : binds) {
                Long familyId = bind.getFamilyId();

                JSONObject takenMsg = JSONUtil.createObj()
                        .set("type", "TAKEN")
                        .set("elderlyId", elderlyId)
                        .set("medicineName", info.getName())
                        .set("msg", "老人已服用药品: " + info.getName());
                FamilyWebSocketServer.sendMessage(familyId, takenMsg.toString());

                if (isLowStock) {
                    JSONObject stockMsg = JSONUtil.createObj()
                            .set("type", "LOW_STOCK")
                            .set("elderlyId", elderlyId)
                            .set("medicineName", info.getName())
                            .set("msg", "备药提醒: [" + info.getName() + "] 库存不足，请及时补货");
                    FamilyWebSocketServer.sendMessage(familyId, stockMsg.toString());
                }
            }
        }
    }

    @Override
    public LocalDateTime snooze(Long elderlyId, LocalDateTime planTime, List<Long> medicineIds) {
        LocalDate today = planTime.toLocalDate();
        LocalTime takeTime = planTime.toLocalTime();
        for (Long medicineId : medicineIds) {
            // 写一条推迟记录，status=2 表示已推迟
            MedicineLog log = new MedicineLog();
            log.setMedicineId(medicineId);
            log.setElderlyId(elderlyId);
            log.setPlanTime(LocalDateTime.of(today, takeTime));
            log.setTakeTime(null);
            log.setStatus(2);
            medicineLogMapper.insert(log);
        }
        return planTime.plusMinutes(10);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveMedicine(SaveMedicineRequest req) {
        if (req.getTakeTimes() == null || req.getTakeTimes().isEmpty()) {
            throw new IllegalArgumentException("至少设置一个服用时间");
        }
        if (req.getElderlyId() == null) {
            throw new IllegalArgumentException("老人ID不能为空");
        }
        if (req.getName() == null || req.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("药品名称不能为空");
        }
        if (req.getCurrentStock() == null || req.getCurrentStock() < 0) {
            throw new IllegalArgumentException("当前库存必须大于等于0");
        }
        if (req.getLowStockThreshold() == null || req.getLowStockThreshold() < 0) {
            throw new IllegalArgumentException("低库存阈值必须大于等于0");
        }
        if (req.getStartDate() == null) {
            throw new IllegalArgumentException("开始日期不能为空");
        }
        if (req.getEndDate() == null) {
            throw new IllegalArgumentException("结束日期不能为空");
        }
        if (req.getEndDate().isBefore(req.getStartDate())) {
            throw new IllegalArgumentException("结束日期不能早于开始日期");
        }
        if (req.getFrequencyType() == null) {
            req.setFrequencyType("daily");
        }
        if ("weekly".equalsIgnoreCase(req.getFrequencyType())
                && (req.getWeekDays() == null || req.getWeekDays().trim().isEmpty())
                && req.getFrequencyValue() != null
                && req.getFrequencyValue() >= 1
                && req.getFrequencyValue() <= 7) {
            // 兼容前端只传 frequencyValue 未传 weekDays 的情况
            req.setWeekDays(String.valueOf(req.getFrequencyValue()));
        }

        MedicineInfo info = new MedicineInfo();
        info.setElderlyId(req.getElderlyId());
        info.setName(req.getName());
        info.setDosageDesc(req.getDosageDesc());
        info.setPhotoUrl(req.getPhotoUrl());
        info.setCurrentStock(req.getCurrentStock());
        info.setLowStockThreshold(req.getLowStockThreshold());
        info.setVoiceRemindUrl(req.getVoiceRemindUrl());
        info.setStartDate(req.getStartDate());
        info.setEndDate(req.getEndDate());
        info.setIsActive(1);
        this.save(info);

        // 批量插入服用时间计划
        for (LocalTime t : req.getTakeTimes()) {
            MedicinePlan plan = new MedicinePlan();
            plan.setMedicineId(info.getId());
            plan.setElderlyId(req.getElderlyId());
            plan.setTakeTime(t);

            // frequencyType 前端/请求里是 once/daily/weekly/interval，这里落库用 int 编码
            plan.setFrequencyType(mapFrequencyType(req.getFrequencyType()));
            plan.setFrequencyValue(req.getFrequencyValue());
            plan.setWeekDays(req.getWeekDays());
            medicinePlanMapper.insert(plan);
        }
        return info.getId();
    }

    private Integer mapFrequencyType(String frequencyType) {
        if (frequencyType == null || frequencyType.isBlank()) {
            return 2; // daily
        }
        String ft = frequencyType.trim().toLowerCase();
        // 兼容老数据/误传：如果是纯数字字符串则直接按数字存
        if (ft.matches("\\d+")) {
            return Integer.valueOf(ft);
        }
        return switch (ft) {
            case "once" -> 1;
            case "daily" -> 2;
            case "weekly" -> 3;
            case "interval" -> 4;
            default -> throw new IllegalArgumentException("不支持的用药频率类型: " + frequencyType);
        };
    }

    @Override
    public FamilyMedicineDashboardDTO getFamilyDashboard(Long elderlyId, LocalDate date) {
        // 查询该老人当天有效的所有药品
        LambdaQueryWrapper<MedicineInfo> infoWrapper = new LambdaQueryWrapper<>();
        infoWrapper.eq(MedicineInfo::getElderlyId, elderlyId)
                .eq(MedicineInfo::getIsActive, 1)
                .le(MedicineInfo::getStartDate, date)
                .ge(MedicineInfo::getEndDate, date);
        List<MedicineInfo> infos = this.list(infoWrapper);

        if (infos.isEmpty()) {
            FamilyMedicineDashboardDTO empty = new FamilyMedicineDashboardDTO();
            empty.setElderlyId(elderlyId);
            empty.setMedicines(new ArrayList<>());
            empty.setHasLowStock(false);
            return empty;
        }

        // 查询今天已服用的药品id（status=1）
        List<Long> medicineIds = infos.stream().map(MedicineInfo::getId).toList();
        LambdaQueryWrapper<MedicineLog> logWrapper = new LambdaQueryWrapper<>();
        logWrapper.in(MedicineLog::getMedicineId, medicineIds)
                .eq(MedicineLog::getElderlyId, elderlyId)
                .ge(MedicineLog::getPlanTime, date.atStartOfDay())
                .lt(MedicineLog::getPlanTime, date.plusDays(1).atStartOfDay())
                .eq(MedicineLog::getStatus, 1);
        List<MedicineLog> logs = medicineLogMapper.selectList(logWrapper);
        // 用Set快速判断哪些药今天已服用
        java.util.Set<Long> takenIds = logs.stream().map(MedicineLog::getMedicineId).collect(Collectors.toSet());

        boolean hasLowStock = false;
        List<FamilyMedicineItemStatusDTO> items = new ArrayList<>();
        for (MedicineInfo info : infos) {
            FamilyMedicineItemStatusDTO item = new FamilyMedicineItemStatusDTO();
            item.setMedicineId(info.getId());
            item.setName(info.getName());
            item.setDosageDesc(info.getDosageDesc());
            item.setPhotoUrl(info.getPhotoUrl());
            item.setCurrentStock(info.getCurrentStock());
            item.setLowStockThreshold(info.getLowStockThreshold());
            item.setTaken(takenIds.contains(info.getId()));

            // 检查Redis中是否有低库存标记
            String lowStockKey = LOW_STOCK_KEY_PREFIX + elderlyId + ":" + info.getId();
            boolean isLow = Boolean.TRUE.equals(stringRedisTemplate.hasKey(lowStockKey));
            item.setLowStock(isLow);
            if (isLow) {
                hasLowStock = true;
            }
            items.add(item);
        }

        FamilyMedicineDashboardDTO dashboard = new FamilyMedicineDashboardDTO();
        dashboard.setElderlyId(elderlyId);
        dashboard.setMedicines(items);
        dashboard.setHasLowStock(hasLowStock);
        return dashboard;
    }

    @Override
    public String uploadFile(MultipartFile file, String subDir) {
        // 直接丢给 MinIO 工具类处理，返回 HTTP 访问地址
        return minioUtil.upload(file, subDir);
    }
}

