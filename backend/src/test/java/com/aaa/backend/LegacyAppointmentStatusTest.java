package com.aaa.backend;

import com.aaa.backend.catalog.dao.ScheduleDao;
import com.aaa.backend.dao.AppointmentDao;
import com.aaa.backend.dto.AppointmentStatusDTO;
import com.aaa.backend.entity.Appointment;
import com.aaa.backend.entity.Schedule;
import com.aaa.backend.service.serviceimpl.AppointmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LegacyAppointmentStatusTest {
    private final AppointmentDao appointments = mock(AppointmentDao.class);
    private final ScheduleDao schedules = mock(ScheduleDao.class);
    private final AppointmentServiceImpl service = new AppointmentServiceImpl();

    @BeforeEach
    void wireMappers() {
        ReflectionTestUtils.setField(service, "appointmentDao", appointments);
        ReflectionTestUtils.setField(service, "scheduleDao", schedules);
    }

    @ParameterizedTest
    @ValueSource(strings = {"pending", "in_consultation", "pending_rx_pay", "completed", "cancelled"})
    void genericStatusChangeCannotBypassBusinessActions(String status) {
        AppointmentStatusDTO request = new AppointmentStatusDTO();
        request.setStatus(status);

        assertEquals(500, service.changeStatus(1L, request).code());
        verifyNoInteractions(appointments, schedules);
    }

    @Test
    void adminCheckInAcceptsHistoricalConfirmedAppointment() {
        Schedule schedule = new Schedule();
        schedule.setStatus(1);
        Appointment appointment = new Appointment();
        appointment.setStatus("confirmed");
        when(schedules.selectByAppointmentForUpdate(1L, null)).thenReturn(schedule);
        when(appointments.selectById(1L)).thenReturn(appointment);
        when(appointments.checkInOnScheduleDate(1L)).thenReturn(1);

        assertEquals(200, service.checkIn(1L).code());
    }
}
