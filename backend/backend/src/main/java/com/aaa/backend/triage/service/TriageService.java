package com.aaa.backend.triage.service;

import com.aaa.backend.catalog.dao.DepartmentDao;
import com.aaa.backend.entity.Department;
import com.aaa.backend.triage.vo.TriageDepartmentVO;
import com.aaa.backend.triage.vo.TriageResultVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

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

    private static final Pattern TOKEN_SPLIT = Pattern.compile("[^\\u4e00-\\u9fa5a-zA-Z0-9]+");

    private static final Set<String> URGENT_KEYWORDS = Set.of(
            "呼吸困难", "意识不清", "意识模糊", "昏迷", "大出血", "剧烈胸痛",
            "持续高热", "抽搐", "休克", "急性腹痛", "咯血", "猝倒");

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
                    List.of("尿痛", "尿频", "尿急", "血尿", "腰痛", "肾结石"), 3),
            new Rule(List.of("急诊", "急救"),
                    List.of("呼吸困难", "意识不清", "昏迷", "大出血", "剧烈胸痛", "持续高热", "休克"), 5),
            // 演示科室表里没有「急诊」，急症词必须能落到现有科室上，否则只会走兜底推荐
            new Rule(List.of("全科", "内科", "传染"),
                    List.of("呼吸困难", "意识不清", "意识模糊", "昏迷", "大出血", "剧烈胸痛",
                            "持续高热", "抽搐", "休克", "急性腹痛", "咯血", "猝倒"), 8)
    );

    private static final String BASE_ADVICE =
            "若持续高热、呼吸困难或意识异常，请立即线下就医；本结果仅为科室推荐，不构成诊断或医疗建议。";

    private final DepartmentDao departmentDao;

    public TriageService(DepartmentDao departmentDao) {
        this.departmentDao = departmentDao;
    }

    public TriageResultVO analyze(String rawSymptom) {
        String symptom = rawSymptom == null ? "" : rawSymptom.trim();

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

        boolean urgent = URGENT_KEYWORDS.stream().anyMatch(symptom::contains);
        scored.sort(Comparator
                .comparingInt((TriageDepartmentVO vo) -> nullToZero(vo.getScore())).reversed()
                .thenComparingInt(vo -> nullToZero(vo.getSortOrder())));

        List<TriageDepartmentVO> recommended = scored.stream()
                .filter(vo -> nullToZero(vo.getScore()) > 0)
                .limit(5)
                .toList();
        if (recommended.isEmpty()) {
            // 没有任何规则命中时给一个兜底推荐，避免前端拿到空列表
            recommended = scored.stream().limit(3).toList();
        }

        List<String> commonSigns = new ArrayList<>(matchedSigns);
        if (commonSigns.isEmpty()) {
            commonSigns = TOKEN_SPLIT.splitAsStream(symptom)
                    .filter(token -> !token.isBlank() && token.length() <= 20)
                    .distinct().limit(3).toList();
        } else {
            commonSigns = commonSigns.stream().limit(5).toList();
        }

        TriageResultVO result = new TriageResultVO();
        result.setSummary(commonSigns.isEmpty()
                ? "未识别到明确的症状关键词"
                : "症状以" + String.join("、", commonSigns) + "为主");
        result.setSymptomAnalysis(recommended.isEmpty()
                ? "建议携带既往检查资料到院咨询导诊台"
                : "结合您描述的症状，建议优先排查" + recommended.get(0).getName() + "相关疾病");
        result.setCommonSigns(commonSigns);
        result.setPreliminaryConclusion(recommended.isEmpty()
                ? "请到院由导诊台协助选择科室"
                : "可优先考虑" + recommended.stream().limit(2)
                        .map(TriageDepartmentVO::getName).reduce((a, b) -> a + "或" + b).orElse("") + "就诊");
        result.setAdvice(urgent
                ? "⚠️ 描述中出现急症信号，请立即前往急诊或拨打急救电话，不要等待线上预约。" + BASE_ADVICE
                : BASE_ADVICE);
        result.setUrgent(urgent);
        result.setSource("fallback");
        result.setDepartments(recommended);
        return result;
    }

    private static boolean containsAny(String text, List<String> hints) {
        return hints.stream().anyMatch(text::contains);
    }

    private static int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }
}
