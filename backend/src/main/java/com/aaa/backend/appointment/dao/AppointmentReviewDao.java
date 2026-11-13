package com.aaa.backend.appointment.dao;

import com.aaa.backend.entity.AppointmentReview;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/** 评价的查重、插入都用 BaseMapper 的 LambdaQueryWrapper，无需自定义 SQL。 */
public interface AppointmentReviewDao extends BaseMapper<AppointmentReview> {
}
