package com.aaa.backend;

import com.aaa.backend.appointment.dao.AppointmentContractDao;
import com.aaa.backend.appointment.dao.AppointmentDao;
import com.aaa.backend.appointment.service.AppointmentContractService;
import com.aaa.backend.appointment.vo.AppointmentDetailVO;
import com.aaa.backend.catalog.dao.ScheduleDao;
import com.aaa.backend.common.BusinessErrorCode;
import com.aaa.backend.common.BusinessException;
import com.aaa.backend.entity.Schedule;
import com.aaa.backend.entity.UserNotification;
import com.aaa.backend.notification.dao.UserNotificationDao;
import com.aaa.backend.wallet.dao.BalanceLogDao;
import com.aaa.backend.wallet.dao.WalletUserDao;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.mockito.ArgumentMatchers;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AppointmentQueueTest {
    private final AppointmentContractDao commands = mock(AppointmentContractDao.class);
    private final AppointmentDao appointments = mock(AppointmentDao.class);
    private final ScheduleDao schedules = mock(ScheduleDao.class);
    private final UserNotificationDao notifications = mock(UserNotificationDao.class);
    private final AppointmentContractService service = new AppointmentContractService(
            commands, appointments, schedules, mock(WalletUserDao.class), mock(BalanceLogDao.class), notifications);

    @Test
    void otherDoctorCannotChangeQueue() {
        stubSchedule();
        BusinessException failure = assertThrows(BusinessException.class,
                () -> service.setQueueDeferred(12L, 9L, true));
        assertEquals(BusinessErrorCode.FORBIDDEN, failure.getErrorCode());
        verifyNoInteractions(commands, appointments, notifications);
    }

    @Test
    void skippingCheckedInAppointmentNotifiesItsOwner() {
        stubSchedule();
        stubCheckedIn();
        when(commands.deferQueue(12L)).thenReturn(1);

        service.setQueueDeferred(12L, 1L, true);

        verify(commands).deferQueue(12L);
        verify(notifications).insert(ArgumentMatchers.<UserNotification>argThat(notification -> notification.getBizId().equals(12L)
                && notification.getUserId().equals(7L)
                && notification.getType().equals("appointment_queue_deferred")));
    }

    @Test
    void retryingRejoinDoesNotMoveAnActivePatientAgain() {
        stubSchedule();
        stubCheckedIn();

        service.setQueueDeferred(12L, 1L, false);

        verifyNoInteractions(commands, notifications);
    }

    private void stubSchedule() {
        LocalDate date = LocalDate.of(2026, 11, 23);
        Schedule schedule = new Schedule();
        schedule.setDoctorId(1L);
        schedule.setStatus(1);
        schedule.setScheduleDate(date);
        when(schedules.selectByAppointmentForUpdate(12L, null)).thenReturn(schedule);
        when(schedules.currentDate()).thenReturn(date);
    }

    private void stubCheckedIn() {
        AppointmentDetailVO appointment = new AppointmentDetailVO();
        appointment.setId(12L);
        appointment.setUserId(7L);
        appointment.setStatus("checked_in");
        when(appointments.lockByIdForUpdate(12L)).thenReturn(appointment);
    }
}
