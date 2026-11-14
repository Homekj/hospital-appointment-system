package com.aaa.backend;

import com.aaa.backend.appointment.dao.AppointmentDao;
import com.aaa.backend.appointment.dto.CreateAppointmentRequest;
import com.aaa.backend.appointment.service.AppointmentService;
import com.aaa.backend.appointment.vo.AppointmentDetailVO;
import com.aaa.backend.catalog.dao.ScheduleDao;
import com.aaa.backend.common.BusinessException;
import com.aaa.backend.entity.Schedule;
import com.aaa.backend.notification.dao.UserNotificationDao;
import com.aaa.backend.wallet.dao.BalanceLogDao;
import com.aaa.backend.wallet.dao.WalletUserDao;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppointmentServiceTest {
    private final AppointmentDao appointmentDao = mock(AppointmentDao.class);
    private final ScheduleDao scheduleDao = mock(ScheduleDao.class);
    private final UserNotificationDao notificationDao = mock(UserNotificationDao.class);
    private final AppointmentService service = new AppointmentService(
            appointmentDao, scheduleDao, mock(WalletUserDao.class),
            mock(BalanceLogDao.class), notificationDao);

    @Test
    void createLocksScheduleBeforeCheckingDuplicateAndTakingQuota() {
        Schedule schedule = activeSchedule();
        when(scheduleDao.selectByIdForUpdate(12L)).thenReturn(schedule);
        when(appointmentDao.countActiveBySchedule(7L, 12L)).thenReturn(0L);
        when(scheduleDao.increaseBookedCount(12L)).thenReturn(1);
        doAnswer(invocation -> {
            com.aaa.backend.entity.Appointment appointment = invocation.getArgument(0);
            appointment.setApId(99L);
            return 1;
        }).when(appointmentDao).insert(any(com.aaa.backend.entity.Appointment.class));
        AppointmentDetailVO detail = new AppointmentDetailVO();
        detail.setId(99L);
        detail.setUserId(7L);
        detail.setScheduleId(12L);
        detail.setStatus("unpaid");
        when(appointmentDao.selectDetail(99L)).thenReturn(detail);

        AppointmentDetailVO result = service.create(7L, request());

        assertEquals(99L, result.getId());
        InOrder order = inOrder(scheduleDao, appointmentDao);
        order.verify(scheduleDao).selectByIdForUpdate(12L);
        order.verify(appointmentDao).countActiveBySchedule(7L, 12L);
        order.verify(scheduleDao).increaseBookedCount(12L);
        order.verify(appointmentDao).insert(any(com.aaa.backend.entity.Appointment.class));
    }

    @Test
    void duplicateBookingDoesNotConsumeAnotherQuota() {
        when(scheduleDao.selectByIdForUpdate(12L)).thenReturn(activeSchedule());
        when(appointmentDao.countActiveBySchedule(7L, 12L)).thenReturn(1L);

        assertThrows(BusinessException.class, () -> service.create(7L, request()));

        verify(scheduleDao, never()).increaseBookedCount(12L);
        verify(appointmentDao, never()).insert(any(com.aaa.backend.entity.Appointment.class));
    }

    private static Schedule activeSchedule() {
        Schedule schedule = new Schedule();
        schedule.setId(12L);
        schedule.setScheduleDate(LocalDate.now());
        schedule.setStatus(1);
        schedule.setTotalQuota(5);
        schedule.setBookedCount(0);
        return schedule;
    }

    private static CreateAppointmentRequest request() {
        return new CreateAppointmentRequest(12L, "患者", "13800138000", null, null);
    }
}
