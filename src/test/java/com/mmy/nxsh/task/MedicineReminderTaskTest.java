package com.mmy.nxsh.task;

import com.mmy.nxsh.mapper.FamilyElderlyBindMapper;
import com.mmy.nxsh.mapper.MedicineInfoMapper;
import com.mmy.nxsh.mapper.UserElderlyMapper;
import com.mmy.nxsh.service.MedicineService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class MedicineReminderTaskTest {

    @Test
    void isInQuietHours_22点至次日6点前为免打扰() throws Exception {
        MedicineReminderTask task = new MedicineReminderTask(
                mock(MedicineService.class),
                mock(UserElderlyMapper.class),
                mock(FamilyElderlyBindMapper.class)
        );
        Method m = MedicineReminderTask.class.getDeclaredMethod("isInQuietHours", LocalDateTime.class);
        m.setAccessible(true);

        assertTrue((Boolean) m.invoke(task, LocalDateTime.of(2024, 6, 1, 22, 0)));
        assertTrue((Boolean) m.invoke(task, LocalDateTime.of(2024, 6, 1, 23, 30)));
        assertTrue((Boolean) m.invoke(task, LocalDateTime.of(2024, 6, 1, 5, 59)));
        assertFalse((Boolean) m.invoke(task, LocalDateTime.of(2024, 6, 1, 6, 0)));
        assertFalse((Boolean) m.invoke(task, LocalDateTime.of(2024, 6, 1, 12, 0)));
        assertFalse((Boolean) m.invoke(task, LocalDateTime.of(2024, 6, 1, 21, 59)));
    }
}
