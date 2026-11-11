package com.aaa.backend.appointment.service;

import com.aaa.backend.appointment.dao.AppointmentReviewDao;
import com.aaa.backend.appointment.dao.ConsultationRecordDao;
import com.aaa.backend.appointment.dto.CreateReviewRequest;
import com.aaa.backend.appointment.support.PatientOwnership;
import com.aaa.backend.appointment.vo.AppointmentDetailVO;
import com.aaa.backend.appointment.vo.AppointmentReviewVO;
import com.aaa.backend.appointment.vo.ConsultationVO;
import com.aaa.backend.common.BusinessErrorCode;
import com.aaa.backend.common.BusinessException;
import com.aaa.backend.entity.AppointmentReview;
import com.aaa.backend.entity.enums.AppointmentStatus;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** 患者侧 P1：查看病历、提交评价。 */
@Service
public class PatientClinicalService {

    private final PatientOwnership ownership;
    private final ConsultationRecordDao consultationRecordDao;
    private final AppointmentReviewDao appointmentReviewDao;

    public PatientClinicalService(PatientOwnership ownership,
                                  ConsultationRecordDao consultationRecordDao,
                                  AppointmentReviewDao appointmentReviewDao) {
        this.ownership = ownership;
        this.consultationRecordDao = consultationRecordDao;
        this.appointmentReviewDao = appointmentReviewDao;
    }

    /** 先校验预约归属，再查病历；尚无病历按契约返回 404「暂无病历」。 */
    public ConsultationVO consultation(Long userId, Long appointmentId) {
        ownership.requireAppointment(userId, appointmentId);
        ConsultationVO vo = consultationRecordDao.selectByAppointment(appointmentId);
        if (vo == null) {
            throw new BusinessException(BusinessErrorCode.NOT_FOUND, "暂无病历");
        }
        return vo;
    }

    /**
     * 提交评价：仅 completed 可评价、每个预约仅一次。
     * 唯一索引 uk_appointment 是最终防线，捕获 DuplicateKeyException 转成 409，
     * 避免并发重复提交时漏出 500。
     */
    @Transactional(rollbackFor = Exception.class)
    public AppointmentReviewVO review(Long userId, Long appointmentId, CreateReviewRequest request) {
        AppointmentDetailVO appointment = ownership.requireAppointment(userId, appointmentId);
        AppointmentStatus status = AppointmentStatus.fromDatabaseValue(appointment.getStatus());
        if (status != AppointmentStatus.COMPLETED) {
            throw new BusinessException(BusinessErrorCode.INVALID_APPOINTMENT_STATE,
                    "仅已完成的预约可评价");
        }

        Long existing = appointmentReviewDao.selectCount(new LambdaQueryWrapper<AppointmentReview>()
                .eq(AppointmentReview::getAppointmentId, appointmentId));
        if (existing != null && existing > 0) {
            throw new BusinessException(BusinessErrorCode.CONFLICT, "该预约已评价，请勿重复提交");
        }

        List<String> tags = request.tags() == null ? List.of() : request.tags().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .toList();
        if (tags.size() > 5) {
            throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "最多 5 个标签");
        }
        for (String tag : tags) {
            if (tag.length() > 20) {
                throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "单个标签不能超过 20 个字符");
            }
        }

        AppointmentReview entity = new AppointmentReview();
        entity.setAppointmentId(appointmentId);
        entity.setUserId(userId);
        entity.setDoctorId(appointment.getDoctorId());
        entity.setRating(request.rating());
        entity.setTags(tags.isEmpty() ? null : String.join(",", tags));
        entity.setComment(trimToNull(request.comment()));
        // createTime 交给数据库默认值，不在此赋值
        try {
            appointmentReviewDao.insert(entity);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(BusinessErrorCode.CONFLICT, "该预约已评价，请勿重复提交");
        }

        // 回查一次只为拿到数据库生成的 create_time
        AppointmentReview saved = appointmentReviewDao.selectById(entity.getId());
        AppointmentReview source = saved == null ? entity : saved;
        AppointmentReviewVO vo = new AppointmentReviewVO();
        vo.setId(source.getId());
        vo.setAppointmentId(source.getAppointmentId());
        vo.setUserId(source.getUserId());
        vo.setDoctorId(source.getDoctorId());
        vo.setRating(source.getRating());
        vo.setTags(splitTags(source.getTags()));
        vo.setComment(source.getComment());
        vo.setCreateTime(source.getCreateTime());
        return vo;
    }

    private static List<String> splitTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .toList();
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
