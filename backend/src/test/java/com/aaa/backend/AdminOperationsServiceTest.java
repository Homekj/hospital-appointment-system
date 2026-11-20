package com.aaa.backend;

import com.aaa.backend.admin.dao.AdminDataDao;
import com.aaa.backend.admin.dao.AdminMedicineDao;
import com.aaa.backend.admin.dao.AdminPrescriptionDao;
import com.aaa.backend.admin.service.AdminOperationsService;
import com.aaa.backend.auth.CurrentUserContext;
import com.aaa.backend.common.BusinessException;
import com.aaa.backend.contract.appointment.AppointmentCommandService;
import com.aaa.backend.prescription.dao.PrescriptionItemDao;
import com.aaa.backend.prescription.vo.PrescriptionDetailVO;
import com.aaa.backend.prescription.vo.PrescriptionItemVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminOperationsServiceTest {
    private final AdminDataDao dataDao = mock(AdminDataDao.class);
    private final AdminMedicineDao medicineDao = mock(AdminMedicineDao.class);
    private final AdminPrescriptionDao prescriptionDao = mock(AdminPrescriptionDao.class);
    private final PrescriptionItemDao prescriptionItemDao = mock(PrescriptionItemDao.class);
    private final AppointmentCommandService appointmentCommandService = mock(AppointmentCommandService.class);
    private final AdminOperationsService service = new AdminOperationsService(
            dataDao, medicineDao, prescriptionDao, prescriptionItemDao,
            appointmentCommandService, new CurrentUserContext());

    @BeforeEach
    void resetMocks() {
        org.mockito.Mockito.reset(dataDao, medicineDao, prescriptionDao,
                prescriptionItemDao, appointmentCommandService);
    }

    @Test
    void dispenseDeductsStockAndCompletesAppointmentInOneCommandFlow() {
        PrescriptionDetailVO paid = prescription(18L, 91L, "paid");
        PrescriptionDetailVO dispensed = prescription(18L, 91L, "dispensed");
        PrescriptionItemVO item = item(33L, 2);
        when(prescriptionDao.lockForUpdate(18L)).thenReturn(18L);
        when(prescriptionDao.selectDetail(18L)).thenReturn(paid, paid, dispensed);
        when(prescriptionItemDao.selectByPrescription(18L)).thenReturn(List.of(item));
        when(medicineDao.lockStockForUpdate(33L)).thenReturn(5);
        when(medicineDao.deductStock(33L, 2)).thenReturn(1);
        when(prescriptionDao.markDispensed(18L)).thenReturn(1);

        PrescriptionDetailVO result = service.dispense(18L);

        assertEquals("dispensed", result.getStatus());
        verify(medicineDao).deductStock(33L, 2);
        verify(appointmentCommandService).completeAfterPrescriptionDispense(91L);
    }

    @Test
    void insufficientStockDoesNotChangePrescriptionOrAppointment() {
        PrescriptionDetailVO paid = prescription(18L, 91L, "paid");
        when(prescriptionDao.selectDetail(18L)).thenReturn(paid, paid);
        when(prescriptionDao.lockForUpdate(18L)).thenReturn(18L);
        when(prescriptionItemDao.selectByPrescription(18L)).thenReturn(List.of(item(33L, 2)));
        when(medicineDao.lockStockForUpdate(33L)).thenReturn(1);
        when(medicineDao.deductStock(33L, 2)).thenReturn(0);

        assertThrows(BusinessException.class, () -> service.dispense(18L));

        verify(prescriptionDao, never()).markDispensed(18L);
        verify(appointmentCommandService, never()).completeAfterPrescriptionDispense(91L);
    }

    @Test
    void cancellingUnpaidPrescriptionCompletesPrescriptionStageThroughContract() {
        PrescriptionDetailVO unpaid = prescription(18L, 91L, "unpaid");
        PrescriptionDetailVO cancelled = prescription(18L, 91L, "cancelled");
        when(prescriptionDao.lockForUpdate(18L)).thenReturn(18L);
        when(prescriptionDao.selectDetail(18L)).thenReturn(unpaid, cancelled);
        when(prescriptionDao.cancelUnpaid(18L)).thenReturn(1);
        when(prescriptionItemDao.selectByPrescription(18L)).thenReturn(List.of());

        PrescriptionDetailVO result = service.cancelPrescription(18L);

        assertEquals("cancelled", result.getStatus());
        verify(appointmentCommandService).completeAfterPrescriptionCancellation(91L);
    }

    private static PrescriptionDetailVO prescription(Long id, Long appointmentId, String status) {
        PrescriptionDetailVO vo = new PrescriptionDetailVO();
        vo.setId(id);
        vo.setAppointmentId(appointmentId);
        vo.setStatus(status);
        return vo;
    }

    private static PrescriptionItemVO item(Long medicineId, int quantity) {
        PrescriptionItemVO item = new PrescriptionItemVO();
        item.setMedicineId(medicineId);
        item.setQuantity(quantity);
        return item;
    }
}
