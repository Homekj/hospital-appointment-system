package com.aaa.backend.admin.dao;

import com.aaa.backend.admin.vo.AdminUserVO;
import com.aaa.backend.admin.vo.DashboardTrendVO;
import com.aaa.backend.entity.SysLoginLog;
import com.aaa.backend.entity.SysMenu;
import com.aaa.backend.entity.SysOperationLog;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/** Read-only admin projections and dashboard aggregations. */
public interface AdminDataDao {

    @Select("SELECT COUNT(*) FROM appointment WHERE DATE(create_time) = CURDATE()")
    long countTodayAppointments();

    @Select("""
            SELECT COALESCE(SUM(amount), 0)
              FROM appointment
             WHERE pay_time >= CURDATE()
               AND pay_time < DATE_ADD(CURDATE(), INTERVAL 1 DAY)
               AND status <> 'cancelled'
            """)
    java.math.BigDecimal sumTodayRevenue();

    @Select("SELECT COUNT(*) FROM appointment WHERE status IN ('unpaid', 'pending', 'confirmed', 'checked_in')")
    long countPendingAppointments();

    @Select("SELECT COUNT(*) FROM appointment WHERE status = 'completed'")
    long countCompletedAppointments();

    @Select("SELECT COUNT(*) FROM doctor WHERE status = 1")
    long countActiveDoctors();

    @Select("SELECT COUNT(*) FROM department WHERE status = 1")
    long countActiveDepartments();

    @Select("SELECT COUNT(*) FROM sys_user WHERE deleted = 0")
    long countActiveUsers();

    @Select("""
            SELECT DATE(create_time) AS date,
                   COUNT(*) AS appointmentCount,
                   COALESCE(SUM(CASE WHEN pay_time IS NOT NULL AND status <> 'cancelled'
                                     THEN amount ELSE 0 END), 0) AS revenue
              FROM appointment
             WHERE create_time >= #{start}
               AND create_time < #{endExclusive}
             GROUP BY DATE(create_time)
             ORDER BY DATE(create_time)
            """)
    List<DashboardTrendVO> selectDashboardTrend(@Param("start") LocalDateTime start,
                                                @Param("endExclusive") LocalDateTime endExclusive);

    @Select("""
            <script>
            SELECT id, username, nickname, email, phone, avatar, balance,
                   doctor_id AS doctorId, status, role,
                   create_time AS createTime, update_time AS updateTime
              FROM sys_user
             WHERE deleted = 0
            <if test='keyword != null and keyword != ""'>
               AND (username LIKE CONCAT('%', #{keyword}, '%')
                    OR nickname LIKE CONCAT('%', #{keyword}, '%')
                    OR phone LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test='role != null and role != ""'> AND role = #{role} </if>
            <if test='status != null'> AND status = #{status} </if>
             ORDER BY id DESC LIMIT #{offset}, #{size}
            </script>
            """)
    List<AdminUserVO> selectUsers(@Param("keyword") String keyword,
                                  @Param("role") String role,
                                  @Param("status") Integer status,
                                  @Param("offset") long offset,
                                  @Param("size") int size);

    @Select("""
            <script>
            SELECT COUNT(*) FROM sys_user
             WHERE deleted = 0
            <if test='keyword != null and keyword != ""'>
               AND (username LIKE CONCAT('%', #{keyword}, '%')
                    OR nickname LIKE CONCAT('%', #{keyword}, '%')
                    OR phone LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test='role != null and role != ""'> AND role = #{role} </if>
            <if test='status != null'> AND status = #{status} </if>
            </script>
            """)
    long countUsers(@Param("keyword") String keyword,
                    @Param("role") String role,
                    @Param("status") Integer status);

    @Select("""
            SELECT id, username, nickname, email, phone, avatar, balance,
                   doctor_id AS doctorId, status, role,
                   create_time AS createTime, update_time AS updateTime
              FROM sys_user
             WHERE id = #{id} AND deleted = 0
            """)
    AdminUserVO selectUser(@Param("id") Long id);

    @org.apache.ibatis.annotations.Update("UPDATE sys_user SET status = #{status} WHERE id = #{id} AND deleted = 0")
    int updateUserStatus(@Param("id") Long id, @Param("status") int status);

    @Select("SELECT id, parent_id AS parentId, name, path, icon, sort_order AS sortOrder, roles FROM sys_menu WHERE roles = 'ADMIN' ORDER BY sort_order, id")
    List<SysMenu> selectAdminMenus();

    @Select("""
            <script>
            SELECT id, username, ip, status, message, create_time AS createTime
              FROM sys_login_log
             WHERE 1 = 1
            <if test='username != null and username != ""'> AND username LIKE CONCAT('%', #{username}, '%') </if>
            <if test='status != null'> AND status = #{status} </if>
            <if test='start != null'> AND create_time &gt;= #{start} </if>
            <if test='endExclusive != null'> AND create_time &lt; #{endExclusive} </if>
             ORDER BY create_time DESC, id DESC LIMIT #{offset}, #{size}
            </script>
            """)
    List<SysLoginLog> selectLoginLogs(@Param("username") String username,
                                      @Param("status") Integer status,
                                      @Param("start") LocalDateTime start,
                                      @Param("endExclusive") LocalDateTime endExclusive,
                                      @Param("offset") long offset,
                                      @Param("size") int size);

    @Select("""
            <script>
            SELECT COUNT(*) FROM sys_login_log WHERE 1 = 1
            <if test='username != null and username != ""'> AND username LIKE CONCAT('%', #{username}, '%') </if>
            <if test='status != null'> AND status = #{status} </if>
            <if test='start != null'> AND create_time &gt;= #{start} </if>
            <if test='endExclusive != null'> AND create_time &lt; #{endExclusive} </if>
            </script>
            """)
    long countLoginLogs(@Param("username") String username,
                        @Param("status") Integer status,
                        @Param("start") LocalDateTime start,
                        @Param("endExclusive") LocalDateTime endExclusive);

    @Select("""
            <script>
            SELECT id, username, module, action, method, url, ip, cost_ms AS costMs,
                   status, create_time AS createTime
              FROM sys_operation_log
             WHERE 1 = 1
            <if test='username != null and username != ""'> AND username LIKE CONCAT('%', #{username}, '%') </if>
            <if test='module != null and module != ""'> AND module LIKE CONCAT('%', #{module}, '%') </if>
            <if test='status != null'> AND status = #{status} </if>
            <if test='start != null'> AND create_time &gt;= #{start} </if>
            <if test='endExclusive != null'> AND create_time &lt; #{endExclusive} </if>
             ORDER BY create_time DESC, id DESC LIMIT #{offset}, #{size}
            </script>
            """)
    List<SysOperationLog> selectOperationLogs(@Param("username") String username,
                                              @Param("module") String module,
                                              @Param("status") Integer status,
                                              @Param("start") LocalDateTime start,
                                              @Param("endExclusive") LocalDateTime endExclusive,
                                              @Param("offset") long offset,
                                              @Param("size") int size);

    @Select("""
            <script>
            SELECT COUNT(*) FROM sys_operation_log WHERE 1 = 1
            <if test='username != null and username != ""'> AND username LIKE CONCAT('%', #{username}, '%') </if>
            <if test='module != null and module != ""'> AND module LIKE CONCAT('%', #{module}, '%') </if>
            <if test='status != null'> AND status = #{status} </if>
            <if test='start != null'> AND create_time &gt;= #{start} </if>
            <if test='endExclusive != null'> AND create_time &lt; #{endExclusive} </if>
            </script>
            """)
    long countOperationLogs(@Param("username") String username,
                            @Param("module") String module,
                            @Param("status") Integer status,
                            @Param("start") LocalDateTime start,
                            @Param("endExclusive") LocalDateTime endExclusive);
}
