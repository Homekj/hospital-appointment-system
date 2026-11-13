package com.aaa.backend.wallet.dao;

import com.aaa.backend.entity.BalanceLog;
import com.aaa.backend.wallet.vo.BalanceLogVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface BalanceLogDao extends BaseMapper<BalanceLog> {

    /** 手动分页：项目未配置 MyBatis-Plus 分页插件，这里用 LIMIT + COUNT 两条 SQL。 */
    @Select("""
            SELECT id, user_id, type, amount, balance_after, order_id, pay_method, remark, create_time
              FROM balance_log
             WHERE user_id = #{userId}
             ORDER BY create_time DESC, id DESC
             LIMIT #{offset}, #{size}
            """)
    List<BalanceLogVO> selectLogs(@Param("userId") Long userId,
                                  @Param("offset") long offset,
                                  @Param("size") int size);

    @Select("SELECT COUNT(*) FROM balance_log WHERE user_id = #{userId}")
    long countLogs(@Param("userId") Long userId);
}
