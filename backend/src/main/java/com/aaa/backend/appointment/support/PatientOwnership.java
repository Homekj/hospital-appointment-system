package com.aaa.backend.appointment.support;

import com.aaa.backend.appointment.dao.AppointmentDao;
import com.aaa.backend.appointment.vo.AppointmentDetailVO;
import com.aaa.backend.common.BusinessErrorCode;
import com.aaa.backend.common.BusinessException;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 患者侧「预约归属」统一校验点。
 *
 * 病历、评价、按预约查处方这几个 P1 接口散落在不同模块，但都必须遵守同一条规则：
 * 预约的 user_id 必须等于 JWT 里的当前用户 ID。抽出来避免每个 Service 各写一遍、
 * 也避免某处漏校验导致越权读取他人就诊资料。
 */
@Component
public class PatientOwnership {

    private final AppointmentDao appointmentDao;

    public PatientOwnership(AppointmentDao appointmentDao) {
        this.appointmentDao = appointmentDao;
    }

    /** 返回预约投影（含 doctorId / status 等后续业务需要的字段）；不存在或不属于本人直接抛异常。 */
    public AppointmentDetailVO requireAppointment(Long userId, Long appointmentId) {
        AppointmentDetailVO vo = appointmentDao.selectDetail(appointmentId);
        if (vo == null) {
            throw new BusinessException(BusinessErrorCode.APPOINTMENT_NOT_FOUND);
        }
        if (!Objects.equals(userId, vo.getUserId())) {
            throw new BusinessException(BusinessErrorCode.FORBIDDEN);
        }
        return vo;
    }
}
