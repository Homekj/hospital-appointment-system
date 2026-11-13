package com.aaa.backend.contract.appointment;

/** Public state-changing contract for management and doctor domains. */
public interface AppointmentCommandService {
    void cancelByAdmin(Long appointmentId, Long operatorUserId);

    void startConsultation(Long appointmentId, Long doctorId);

    void completeConsultation(Long appointmentId, Long doctorId);
}
