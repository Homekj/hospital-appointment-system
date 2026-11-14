package com.aaa.backend.notification.dao;

import com.aaa.backend.entity.UserNotification;
import com.aaa.backend.notification.vo.NotificationVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface UserNotificationDao extends BaseMapper<UserNotification> {

    /** read_flag 用别名映射成接口要求的布尔字段 read。 */
    @Select("""
            <script>
            SELECT id, user_id, type, title, content, biz_type, biz_id,
                   read_flag AS `read`, create_time
              FROM user_notification
             WHERE user_id = #{userId}
            <if test='readFlag != null'> AND read_flag = #{readFlag} </if>
             ORDER BY create_time DESC, id DESC
             LIMIT #{offset}, #{size}
            </script>
            """)
    List<NotificationVO> selectPage(@Param("userId") Long userId,
                                    @Param("readFlag") Integer readFlag,
                                    @Param("offset") long offset,
                                    @Param("size") int size);

    @Select("""
            <script>
            SELECT COUNT(*) FROM user_notification WHERE user_id = #{userId}
            <if test='readFlag != null'> AND read_flag = #{readFlag} </if>
            </script>
            """)
    long countPage(@Param("userId") Long userId, @Param("readFlag") Integer readFlag);

    @Select("SELECT COUNT(*) FROM user_notification WHERE user_id = #{userId} AND read_flag = 0")
    long countUnread(@Param("userId") Long userId);

    /** 用于区分「通知不存在/不属于本人」(404) 与「本来就已读」(幂等成功)。 */
    @Select("SELECT COUNT(*) FROM user_notification WHERE id = #{id} AND user_id = #{userId}")
    long countOwned(@Param("id") Long id, @Param("userId") Long userId);

    @Update("""
            UPDATE user_notification SET read_flag = 1
             WHERE id = #{id} AND user_id = #{userId} AND read_flag = 0
            """)
    int markRead(@Param("id") Long id, @Param("userId") Long userId);

    @Update("UPDATE user_notification SET read_flag = 1 WHERE user_id = #{userId} AND read_flag = 0")
    int markAllRead(@Param("userId") Long userId);
}
