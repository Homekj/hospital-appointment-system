package com.aaa.backend.vo;

import lombok.Data;

/**
 * 预约管理查询条件。
 *
 * <p><b>日期区间约定（前后端固定，不再变更）：</b>
 * scheduleDateStart / scheduleDateEnd 一律按【就诊日期 schedule.schedule_date】过滤，
 * 不按预约创建时间 appointment.create_time 过滤；系统不提供第二套日期字段，避免歧义。
 * 格式固定 yyyy-MM-dd。
 */
@Data
public class AppointmentQuery {

    private Integer pageNum = 1;
    private Integer pageSize = 10;

    /** 就诊人姓名 / 手机号，模糊匹配 */
    private String keyword;

    /** 预约状态，必须是 appointment.status 七个合法值之一（VARCHAR，不是数字） */
    private String status;

    /** 医生 ID，按排班所属的医生过滤 */
    private Long doctorId;

    /** 科室 ID，按医生所属科室过滤 */
    private Long departmentId;

    private String scheduleDateStart;
    private String scheduleDateEnd;

    /** 单日快捷筛选 */
    private String scheduleDate;
}
