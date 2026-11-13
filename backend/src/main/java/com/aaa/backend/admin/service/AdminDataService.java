package com.aaa.backend.admin.service;

import com.aaa.backend.admin.dto.MedicineSaveRequest;
import com.aaa.backend.common.BusinessErrorCode;
import com.aaa.backend.common.BusinessException;
import com.aaa.backend.common.PageResult;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * api.md 第 12 节中后端原本缺失的管理端能力：数据概览、药品、用户、菜单、日志。
 *
 * <p>实现约定：
 * <ul>
 *   <li>全部使用 JdbcTemplate + 参数绑定，不拼接用户输入，避免 SQL 注入；</li>
 *   <li>时间字段在 SQL 里格式化成 {@code yyyy-MM-dd HH:mm:ss} 字符串，
 *       与 api.md 3.4 的字段类型约定一致，也避免不同 Jackson 版本对 JavaTime 的序列化差异；</li>
 *   <li>分页统一输出 {@link PageResult} 的 records/total/current/size 四字段；</li>
 *   <li>用户接口不返回 password / openid / deleted。</li>
 * </ul>
 */
@Service
public class AdminDataService {

    private static final String MEDICINE_COLUMNS = "id, name, spec, unit, price, stock, "
            + "usage_desc AS usageDesc, status, "
            + "DATE_FORMAT(create_time, '%Y-%m-%d %H:%i:%s') AS createTime, "
            + "DATE_FORMAT(update_time, '%Y-%m-%d %H:%i:%s') AS updateTime";

    private static final String USER_COLUMNS = "id, username, nickname, email, phone, avatar, balance, "
            + "role, status, doctor_id AS doctorId, "
            + "DATE_FORMAT(create_time, '%Y-%m-%d %H:%i:%s') AS createTime, "
            + "DATE_FORMAT(update_time, '%Y-%m-%d %H:%i:%s') AS updateTime";

    private final JdbcTemplate jdbc;

    public AdminDataService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ------------------------------------------------------------------
    // 数据概览 / 菜单
    // ------------------------------------------------------------------

    public Map<String, Object> dashboardStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("todayAppointments", count("SELECT COUNT(*) FROM appointment WHERE DATE(create_time) = CURDATE()"));
        stats.put("todayRevenue", amount("SELECT COALESCE(SUM(amount), 0) FROM appointment "
                + "WHERE DATE(pay_time) = CURDATE() AND status <> 'cancelled'"));
        stats.put("pendingAppointments", count("SELECT COUNT(*) FROM appointment "
                + "WHERE status IN ('unpaid', 'pending', 'checked_in')"));
        stats.put("completedAppointments", count("SELECT COUNT(*) FROM appointment WHERE status = 'completed'"));
        stats.put("doctorCount", count("SELECT COUNT(*) FROM doctor WHERE status = 1"));
        stats.put("departmentCount", count("SELECT COUNT(*) FROM department WHERE status = 1"));
        stats.put("userCount", count("SELECT COUNT(*) FROM sys_user WHERE deleted = 0"));

        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            String day = LocalDate.now().minusDays(i).toString();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", day);
            item.put("appointmentCount", jdbc.queryForObject(
                    "SELECT COUNT(*) FROM appointment WHERE DATE(create_time) = ?", Long.class, day));
            item.put("revenue", jdbc.queryForObject(
                    "SELECT COALESCE(SUM(amount), 0) FROM appointment "
                            + "WHERE DATE(pay_time) = ? AND status <> 'cancelled'", BigDecimal.class, day));
            trend.add(item);
        }
        stats.put("last7Days", trend);
        return stats;
    }

    public List<Map<String, Object>> menus() {
        return jdbc.queryForList("SELECT id, parent_id AS parentId, name, path, icon, "
                + "sort_order AS sortOrder, roles FROM sys_menu "
                + "WHERE roles = ? ORDER BY sort_order, id", "ADMIN");
    }

    // ------------------------------------------------------------------
    // 药品管理
    // ------------------------------------------------------------------

    public PageResult<Object> medicinePage(Integer current, Integer size, String keyword, Integer status) {
        int c = normalizeCurrent(current);
        int s = normalizeSize(size);
        StringBuilder where = new StringBuilder(" FROM medicine WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        if (hasText(keyword)) {
            where.append(" AND (name LIKE CONCAT('%', ?, '%') OR spec LIKE CONCAT('%', ?, '%'))");
            args.add(keyword.trim());
            args.add(keyword.trim());
        }
        if (status != null) {
            where.append(" AND status = ?");
            args.add(status);
        }
        return queryPage(MEDICINE_COLUMNS, where.toString(), "ORDER BY id DESC", args, c, s);
    }

    public Map<String, Object> medicineDetail(Long id) {
        return one("SELECT " + MEDICINE_COLUMNS + " FROM medicine WHERE id = ?", id, "药品不存在");
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> medicineCreate(MedicineSaveRequest request) {
        String name = requireName(request.getName());
        BigDecimal price = request.getPrice() == null ? BigDecimal.ZERO : request.getPrice();
        int stock = request.getStock() == null ? 0 : request.getStock();
        int status = request.getStatus() == null ? 1 : request.getStatus();
        if (price.compareTo(BigDecimal.ZERO) < 0 || stock < 0) {
            throw badRequest("单价和库存不能为负");
        }
        Long exists = jdbc.queryForObject("SELECT COUNT(*) FROM medicine WHERE name = ?", Long.class, name);
        if (exists != null && exists > 0) {
            throw new BusinessException(BusinessErrorCode.CONFLICT, "药品名称已存在");
        }

        String sql = "INSERT INTO medicine (name, spec, unit, price, stock, usage_desc, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        KeyHolder holder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, name);
            ps.setString(2, emptyToNull(request.getSpec()));
            ps.setString(3, hasText(request.getUnit()) ? request.getUnit().trim() : "盒");
            ps.setBigDecimal(4, price);
            ps.setInt(5, stock);
            ps.setString(6, emptyToNull(request.getUsageDesc()));
            ps.setInt(7, status);
            return ps;
        }, holder);
        Number key = holder.getKey();
        return one("SELECT " + MEDICINE_COLUMNS + " FROM medicine WHERE id = ?",
                key == null ? -1L : key.longValue(), "新增药品后未找到记录");
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> medicineUpdate(Long id, MedicineSaveRequest request) {
        medicineDetail(id);
        String name = requireName(request.getName());
        BigDecimal price = request.getPrice() == null ? BigDecimal.ZERO : request.getPrice();
        int stock = request.getStock() == null ? 0 : request.getStock();
        int status = request.getStatus() == null ? 1 : request.getStatus();
        if (price.compareTo(BigDecimal.ZERO) < 0 || stock < 0) {
            throw badRequest("单价和库存不能为负");
        }
        Long duplicate = jdbc.queryForObject(
                "SELECT COUNT(*) FROM medicine WHERE name = ? AND id <> ?", Long.class, name, id);
        if (duplicate != null && duplicate > 0) {
            throw new BusinessException(BusinessErrorCode.CONFLICT, "药品名称已存在");
        }
        jdbc.update("UPDATE medicine SET name = ?, spec = ?, unit = ?, price = ?, stock = ?, "
                        + "usage_desc = ?, status = ? WHERE id = ?",
                name, emptyToNull(request.getSpec()),
                hasText(request.getUnit()) ? request.getUnit().trim() : "盒",
                price, stock, emptyToNull(request.getUsageDesc()), status, id);
        return medicineDetail(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void medicineStatus(Long id, Integer status) {
        requireStatus(status);
        medicineDetail(id);
        jdbc.update("UPDATE medicine SET status = ? WHERE id = ?", status, id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void medicineDelete(Long id) {
        medicineDetail(id);
        Long used = jdbc.queryForObject(
                "SELECT COUNT(*) FROM prescription_item WHERE medicine_id = ?", Long.class, id);
        if (used != null && used > 0) {
            throw new BusinessException(BusinessErrorCode.CONFLICT, "该药品已被处方引用，只能停用不能删除");
        }
        jdbc.update("DELETE FROM medicine WHERE id = ?", id);
    }

    // ------------------------------------------------------------------
    // 用户管理（只读 + 启停用）
    // ------------------------------------------------------------------

    public PageResult<Object> userPage(Integer current, Integer size, String keyword, String role, Integer status) {
        int c = normalizeCurrent(current);
        int s = normalizeSize(size);
        StringBuilder where = new StringBuilder(" FROM sys_user WHERE deleted = 0");
        List<Object> args = new ArrayList<>();
        if (hasText(keyword)) {
            where.append(" AND (username LIKE CONCAT('%', ?, '%') OR nickname LIKE CONCAT('%', ?, '%') "
                    + "OR phone LIKE CONCAT('%', ?, '%'))");
            String value = keyword.trim();
            args.add(value);
            args.add(value);
            args.add(value);
        }
        if (hasText(role)) {
            where.append(" AND role = ?");
            args.add(role.trim().toUpperCase());
        }
        if (status != null) {
            where.append(" AND status = ?");
            args.add(status);
        }
        return queryPage(USER_COLUMNS, where.toString(), "ORDER BY id ASC", args, c, s);
    }

    public Map<String, Object> userDetail(Long id) {
        return one("SELECT " + USER_COLUMNS + " FROM sys_user WHERE id = ? AND deleted = 0",
                id, "用户不存在");
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> userStatus(Long id, Integer status) {
        requireStatus(status);
        userDetail(id);
        jdbc.update("UPDATE sys_user SET status = ? WHERE id = ? AND deleted = 0", status, id);
        return userDetail(id);
    }

    // ------------------------------------------------------------------
    // 日志
    // ------------------------------------------------------------------

    public PageResult<Object> loginLogs(Integer current, Integer size, String username,
                                        Integer status, String startDate, String endDate) {
        return logs(current, size, username, status, startDate, endDate,
                "id, username, ip, status, message, "
                        + "DATE_FORMAT(create_time, '%Y-%m-%d %H:%i:%s') AS createTime",
                "sys_login_log");
    }

    public PageResult<Object> operationLogs(Integer current, Integer size, String username,
                                            String module, Integer status, String startDate, String endDate) {
        int c = normalizeCurrent(current);
        int s = normalizeSize(size);
        StringBuilder where = new StringBuilder(" FROM sys_operation_log WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        appendLogConditions(where, args, username, status, startDate, endDate);
        if (hasText(module)) {
            where.append(" AND module LIKE CONCAT('%', ?, '%')");
            args.add(module.trim());
        }
        return queryPage("id, username, module, action, method, url, ip, cost_ms AS costMs, status, "
                        + "DATE_FORMAT(create_time, '%Y-%m-%d %H:%i:%s') AS createTime",
                where.toString(), "ORDER BY id DESC", args, c, s);
    }

    private PageResult<Object> logs(Integer current, Integer size, String username, Integer status,
                                    String startDate, String endDate, String columns, String table) {
        int c = normalizeCurrent(current);
        int s = normalizeSize(size);
        StringBuilder where = new StringBuilder(" FROM " + table + " WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        appendLogConditions(where, args, username, status, startDate, endDate);
        return queryPage(columns, where.toString(), "ORDER BY id DESC", args, c, s);
    }

    private void appendLogConditions(StringBuilder where, List<Object> args, String username,
                                     Integer status, String startDate, String endDate) {
        if (hasText(username)) {
            where.append(" AND username LIKE CONCAT('%', ?, '%')");
            args.add(username.trim());
        }
        if (status != null) {
            where.append(" AND status = ?");
            args.add(status);
        }
        if (hasText(startDate)) {
            where.append(" AND DATE(create_time) >= ?");
            args.add(startDate.trim());
        }
        if (hasText(endDate)) {
            where.append(" AND DATE(create_time) <= ?");
            args.add(endDate.trim());
        }
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    private PageResult<Object> queryPage(String columns, String fromWhere, String orderBy,
                                         List<Object> args, int current, int size) {
        Long total = jdbc.queryForObject("SELECT COUNT(*) " + fromWhere, Long.class, args.toArray());
        List<Object> pagedArgs = new ArrayList<>(args);
        pagedArgs.add(size);
        pagedArgs.add((long) (current - 1) * size);
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT " + columns + " " + fromWhere + " " + orderBy + " LIMIT ? OFFSET ?",
                pagedArgs.toArray());
        return new PageResult<>(new ArrayList<Object>(rows), total == null ? 0L : total, current, size);
    }

    private Map<String, Object> one(String sql, Object id, String notFoundMessage) {
        try {
            return jdbc.queryForMap(sql, id);
        } catch (EmptyResultDataAccessException ex) {
            throw new BusinessException(BusinessErrorCode.NOT_FOUND, notFoundMessage);
        }
    }

    private long count(String sql) {
        Long value = jdbc.queryForObject(sql, Long.class);
        return value == null ? 0L : value;
    }

    private BigDecimal amount(String sql) {
        BigDecimal value = jdbc.queryForObject(sql, BigDecimal.class);
        return value == null ? BigDecimal.ZERO : value;
    }

    private static int normalizeCurrent(Integer current) {
        return current == null || current < 1 ? 1 : current;
    }

    private static int normalizeSize(Integer size) {
        return size == null || size < 1 ? 10 : Math.min(size, 100);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String emptyToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private static String requireName(String name) {
        if (!hasText(name)) {
            throw badRequest("药品名称不能为空");
        }
        if (name.trim().length() > 100) {
            throw badRequest("药品名称最多 100 字符");
        }
        return name.trim();
    }

    private static void requireStatus(Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw badRequest("状态只能是 0 或 1");
        }
    }

    private static BusinessException badRequest(String message) {
        return new BusinessException(BusinessErrorCode.BAD_REQUEST, message);
    }
}
