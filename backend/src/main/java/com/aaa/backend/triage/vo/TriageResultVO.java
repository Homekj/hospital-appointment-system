package com.aaa.backend.triage.vo;

import lombok.Data;

import java.util.List;

/**
 * 分诊结果，对应 docs/api.md 7.5。
 * source 固定为 fallback：本项目未配置外部 AI，全部走本地关键词规则，
 * 因此不会出现「AI 超时导致分诊接口 500」这种情况。
 */
@Data
public class TriageResultVO {
    private String summary;
    private String symptomAnalysis;
    private List<String> commonSigns;
    private String preliminaryConclusion;
    private String advice;
    private boolean urgent;
    private boolean requiresHumanHelp;
    private String helpPhone;
    private String source;
    private List<TriageDepartmentVO> departments;
}
