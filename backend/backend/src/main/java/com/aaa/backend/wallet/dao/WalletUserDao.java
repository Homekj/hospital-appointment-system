package com.aaa.backend.wallet.dao;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * 余额账户的原子操作。
 * 刻意不继承 BaseMapper&lt;SysUser&gt;：登录模块已有 UserDao 映射同一个实体，
 * 这里只需要三条手写 SQL，避免同一实体被注册两套通用 CRUD。
 */
public interface WalletUserDao {

    /** 条件扣款：余额不足时影响行数为 0，由 Service 转成 409「余额不足，请先充值」。 */
    @Update("""
            UPDATE sys_user
               SET balance = balance - #{amount}
             WHERE id = #{userId}
               AND balance >= #{amount}
            """)
    int deductBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /** 原子加款，用于充值和退款；充值在 Service 里是独立事务。 */
    @Update("""
            UPDATE sys_user
               SET balance = balance + #{amount}
             WHERE id = #{userId}
            """)
    int addBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    @Select("SELECT balance FROM sys_user WHERE id = #{userId}")
    BigDecimal selectBalance(@Param("userId") Long userId);
}
