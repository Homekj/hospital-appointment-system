package com.aaa.backend.contract.appointment;

/** Public state-changing contract for management and doctor domains. */
public interface AppointmentCommandService {
    void cancelByAdmin(Long appointmentId, Long operatorUserId);

    int stopSchedule(Long scheduleId, String reason, Long operatorUserId);

    void startConsultation(Long appointmentId, Long doctorId);

    void completeConsultation(Long appointmentId, Long doctorId);

    void completeAfterPrescriptionDispense(Long appointmentId);

    void completeAfterPrescriptionCancellation(Long appointmentId);
}
