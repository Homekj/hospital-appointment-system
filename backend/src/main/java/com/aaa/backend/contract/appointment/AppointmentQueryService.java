package com.aaa.backend.contract.appointment;

import com.aaa.backend.common.PageResult;

/** Public query contract for the appointment domain. Implementations belong in the appointment module. */
public interface AppointmentQueryService {
    PageResult<AppointmentSummary> pageForAdmin(AppointmentPageQuery query);

    AppointmentSummary getDetailForAdmin(Long appointmentId);

    PageResult<AppointmentSummary> pageForDoctor(Long doctorId, AppointmentPageQuery query);

    AppointmentSummary getDetailForDoctor(Long appointmentId, Long doctorId);
}
