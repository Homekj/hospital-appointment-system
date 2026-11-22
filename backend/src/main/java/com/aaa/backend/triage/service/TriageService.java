package com.aaa.backend.triage.service;

import com.aaa.backend.catalog.dao.DepartmentDao;
import com.aaa.backend.entity.Department;
import com.aaa.backend.triage.vo.TriageDepartmentVO;
import com.aaa.backend.triage.vo.TriageResultVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 智能分诊（科室推荐）。
 *
 * 本项目没有配置外部 AI 服务，按 docs/api.md 7.5 的降级要求直接走本地关键词规则，
 * source 恒为 fallback。规则是纯内存计算 + 一次科室查询，不写库、不调用第三方，
 * 因此不存在 AI 超时把分诊接口拖垮的情况。
 *
 * ⚠️ 结果只用于科室推荐，不构成诊断或医疗建议（见接口契约声明）。
 */
@Service
public class TriageService {

    private static final Set<String> URGENT_KEYWORDS = Set.of(
            "呼吸困难", "意识不清", "意识模糊", "昏迷", "大出血", "剧烈胸痛",
            "持续高热", "抽搐", "休克", "急性腹痛", "咯血", "猝倒");

    private static final List<String> HUMAN_REVIEW_KEYWORDS = List.of(
            "孕", "产后", "儿童", "新生儿", "婴儿", "婴幼儿", "小儿", "宝宝");

    /** 每条规则 = 科室名称/简介命中词 + 该科室关注的症状词 + 命中权重。 */
    private record Rule(List<String> departmentHints, List<String> keywords, int weight) {
    }

    private static final List<Rule> RULES = List.of(
            new Rule(List.of("呼吸", "感染", "内科", "全科"),
                    List.of("发热", "咳嗽", "咽痛", "流涕", "鼻塞", "咳痰", "感冒", "嗓子疼", "扁桃体", "气喘"), 3),
            new Rule(List.of("消化", "肠胃", "内科", "全科"),
                    List.of("腹痛", "腹泻", "恶心", "呕吐", "胃痛", "反酸", "便秘", "腹胀", "食欲不振"), 3),
            new Rule(List.of("心内", "心血管", "心脏", "内科"),
                    List.of("胸痛", "心悸", "心慌", "高血压", "胸闷", "心律不齐"), 3),
            new Rule(List.of("神经", "脑", "内科"),
                    List.of("头痛", "头晕", "眩晕", "失眠", "手脚麻木", "偏头痛", "记忆力下降"), 3),
            new Rule(List.of("骨科", "外科", "创伤"),
                    List.of("骨折", "扭伤", "关节痛", "腰痛", "颈痛", "外伤", "肿胀", "拉伤"), 3),
            new Rule(List.of("皮肤", "皮肤科"),
                    List.of("皮疹", "瘙痒", "过敏", "湿疹", "红肿", "脱皮", "痘痘", "荨麻疹"), 3),
            new Rule(List.of("眼科", "眼"),
                    List.of("眼痛", "视力下降", "视物模糊", "眼红", "流泪", "眼干", "飞蚊"), 3),
            new Rule(List.of("耳鼻喉", "五官", "耳鼻"),
                    List.of("耳痛", "耳鸣", "听力下降", "声音嘶哑", "鼻出血", "打鼾"), 3),
            new Rule(List.of("口腔", "牙"),
                    List.of("牙痛", "牙龈", "蛀牙", "口腔溃疡", "牙齿松动"), 3),
            new Rule(List.of("妇", "产科"),
                    List.of("月经", "痛经", "白带", "下腹痛", "阴道", "孕"), 3),
            new Rule(List.of("儿", "小儿", "儿科"),
                    List.of("小儿", "儿童", "宝宝", "婴幼儿", "发烧", "接种"), 3),
            new Rule(List.of("泌尿", "肾", "外科"),
                    List.of("尿痛", "尿频", "尿急", "血尿", "腰痛", "肾结石"), 3)
    );

    private static final String BASE_ADVICE =
            "科室建议仅供导航。出现紧急情况，请立即寻求线下急救。";

    private final DepartmentDao departmentDao;

    private final String helpPhone;

    public TriageService(DepartmentDao departmentDao, @Value("${triage.help-phone:}") String helpPhone) {
        this.departmentDao = departmentDao;
        String phone = helpPhone.trim();
        this.helpPhone = phone.matches("[0-9+() -]{5,30}") ? phone : "";
    }

    public TriageResultVO analyze(String rawSymptom) {
        String symptom = rawSymptom == null ? "" : rawSymptom.trim();
        if (URGENT_KEYWORDS.stream().anyMatch(symptom::contains)) {
            return referToStaff("描述中匹配到紧急提示词",
                    "请立即寻求线下急救，不要等待线上挂号。", true);
        }
        if (containsAny(symptom, HUMAN_REVIEW_KEYWORDS)) {
            return referToStaff("建议由人工导诊协助选择科室",
                    "涉及儿童、孕期或产后就诊，请联系医院导诊台确认。", false);
        }

        // 只取启用科室，按展示顺序排好；排序放到后面按得分重排
        LambdaQueryWrapper<Department> query = new LambdaQueryWrapper<>();
        query.eq(Department::getStatus, 1)
                .orderByAsc(Department::getSortOrder, Department::getId);
        List<Department> departments = departmentDao.selectList(query);

        Set<String> matchedSigns = new LinkedHashSet<>();
        List<TriageDepartmentVO> scored = new ArrayList<>();
        for (Department department : departments) {
            String deptText = String.valueOf(department.getName()) + " "
                    + String.valueOf(department.getDescription());
            int score = 0;
            Set<String> hits = new LinkedHashSet<>();
            for (Rule rule : RULES) {
                if (!containsAny(deptText, rule.departmentHints())) {
                    continue;
                }
                for (String keyword : rule.keywords()) {
                    if (symptom.contains(keyword)) {
                        score += rule.weight();
                        hits.add(keyword);
                        matchedSigns.add(keyword);
                    }
                }
            }
            TriageDepartmentVO vo = new TriageDepartmentVO();
            vo.setId(department.getId());
            vo.setName(department.getName());
            vo.setDescription(department.getDescription());
            vo.setLocation(department.getLocation());
            vo.setSortOrder(department.getSortOrder());
            vo.setStatus(department.getStatus());
            vo.setScore(score);
            vo.setSymptoms(new ArrayList<>(hits));
            scored.add(vo);
        }

        scored.sort(Comparator
                .comparingInt((TriageDepartmentVO vo) -> nullToZero(vo.getScore())).reversed()
                .thenComparingInt(vo -> nullToZero(vo.getSortOrder())));

        List<TriageDepartmentVO> recommended = scored.stream()
                .filter(vo -> nullToZero(vo.getScore()) > 0)
                .limit(5)
                .toList();
        if (recommended.isEmpty()) {
            return referToStaff("暂未找到匹配的科室", "请联系医院导诊台确认就诊科室。", false);
        }

        TriageResultVO result = baseResult();
        result.setSummary("匹配到科室导航关键词");
        result.setSymptomAnalysis("以下开放科室与描述中的关键词相关。");
        result.setCommonSigns(matchedSigns.stream().limit(5).toList());
        result.setPreliminaryConclusion("可了解" + recommended.get(0).getName() + "的门诊安排");
        result.setAdvice(BASE_ADVICE);
        result.setDepartments(recommended);
        return result;
    }

    private TriageResultVO referToStaff(String title, String advice, boolean urgent) {
        TriageResultVO result = baseResult();
        result.setSummary(title);
        result.setSymptomAnalysis(advice);
        result.setPreliminaryConclusion(title);
        result.setCommonSigns(List.of());
        result.setAdvice(advice + BASE_ADVICE);
        result.setUrgent(urgent);
        result.setRequiresHumanHelp(true);
        result.setDepartments(List.of());
        return result;
    }

    private TriageResultVO baseResult() {
        TriageResultVO result = new TriageResultVO();
        result.setSource("fallback");
        result.setHelpPhone(helpPhone);
        return result;
    }

    private static boolean containsAny(String text, List<String> hints) {
        return hints.stream().anyMatch(text::contains);
    }

    private static int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }
}
