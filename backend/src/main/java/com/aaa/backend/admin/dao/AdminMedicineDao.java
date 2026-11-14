package com.aaa.backend.admin.dao;

import com.aaa.backend.entity.Medicine;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface AdminMedicineDao extends BaseMapper<Medicine> {

    @Select("""
            <script>
            SELECT id, name, spec, unit, price, stock, usage_desc AS usageDesc,
                   status, create_time AS createTime, update_time AS updateTime
              FROM medicine WHERE 1 = 1
            <if test='keyword != null and keyword != ""'>
               AND (name LIKE CONCAT('%', #{keyword}, '%') OR spec LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test='status != null'> AND status = #{status} </if>
             ORDER BY id DESC LIMIT #{offset}, #{size}
            </script>
            """)
    List<Medicine> selectAdminPage(@Param("keyword") String keyword,
                                   @Param("status") Integer status,
                                   @Param("offset") long offset,
                                   @Param("size") int size);

    @Select("""
            <script>
            SELECT COUNT(*) FROM medicine WHERE 1 = 1
            <if test='keyword != null and keyword != ""'>
               AND (name LIKE CONCAT('%', #{keyword}, '%') OR spec LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test='status != null'> AND status = #{status} </if>
            </script>
            """)
    long countAdminPage(@Param("keyword") String keyword, @Param("status") Integer status);

    @Update("UPDATE medicine SET stock = stock - #{quantity} WHERE id = #{id} AND stock >= #{quantity}")
    int deductStock(@Param("id") Long id, @Param("quantity") int quantity);
}
