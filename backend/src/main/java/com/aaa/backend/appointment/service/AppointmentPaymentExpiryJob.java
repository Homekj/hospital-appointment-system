package com.aaa.backend.appointment.service;

import com.aaa.backend.appointment.dao.AppointmentDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentPaymentExpiryJob {

    private static final int BATCH_SIZE = 100;
    private final AppointmentDao appointments;
    private final AppointmentService appointmentService;

    @Scheduled(initialDelayString = "${patient.appointment.expiry-scan-delay:PT15S}",
               fixedDelayString = "${patient.appointment.expiry-scan-delay:PT15S}")
    public void closeExpiredPayments() {
        for (Long id : appointments.expiredPaymentIds(BATCH_SIZE)) {
            try {
                appointmentService.expireUnpaid(id);
            } catch (RuntimeException exception) {
                log.warn("超时预约处理失败，将在下一批重试: appointmentId={}", id, exception);
            }
        }
    }
}
