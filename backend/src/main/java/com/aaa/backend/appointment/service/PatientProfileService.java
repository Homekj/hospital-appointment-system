package com.aaa.backend.appointment.service;

import com.aaa.backend.common.BusinessErrorCode;
import com.aaa.backend.common.BusinessException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PatientProfileService {
    public record Profile(Long id, String name, String phone, String relationship, boolean defaultPatient) {}

    public record SaveRequest(
            @NotBlank @Size(max = 50) String name,
            @NotBlank @Pattern(regexp = "^1[3-9]\\d{9}$") String phone,
            @NotBlank @Pattern(regexp = "本人|父母|子女|配偶|其他") String relationship,
            boolean defaultPatient) {}

    private static final RowMapper<Profile> ROW = (rs, index) -> new Profile(rs.getLong("id"),
            rs.getString("name"), rs.getString("phone"), rs.getString("relationship"), rs.getBoolean("is_default"));

    private final JdbcTemplate jdbc;

    public PatientProfileService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Profile> list(Long userId) {
        return jdbc.query("""
                SELECT id, name, phone, relationship, is_default FROM patient_profile
                WHERE user_id = ? ORDER BY is_default DESC, id
                """, ROW, userId);
    }

    public Profile require(Long userId, Long id) {
        return jdbc.query("""
                SELECT id, name, phone, relationship, is_default FROM patient_profile
                WHERE user_id = ? AND id = ?
                """, ROW, userId, id).stream().findFirst().orElseThrow(
                () -> new BusinessException(BusinessErrorCode.NOT_FOUND, "常用就诊人不存在"));
    }

    @Transactional
    public List<Profile> save(Long userId, Long id, SaveRequest request) {
        lockAccount(userId);
        if (id != null) {
            require(userId, id);
        }
        if (request.defaultPatient()) {
            jdbc.update("UPDATE patient_profile SET is_default = FALSE WHERE user_id = ? AND is_default", userId);
        }
        try {
            if (id == null) {
                jdbc.update("""
                        INSERT INTO patient_profile (user_id, name, phone, relationship, is_default)
                        VALUES (?, ?, ?, ?, ?)
                        """, userId, request.name().strip(), request.phone(), request.relationship(), request.defaultPatient());
            } else {
                jdbc.update("""
                        UPDATE patient_profile SET name = ?, phone = ?, relationship = ?, is_default = ?
                        WHERE user_id = ? AND id = ?
                        """, request.name().strip(), request.phone(), request.relationship(), request.defaultPatient(), userId, id);
            }
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(BusinessErrorCode.CONFLICT, "已保存姓名和联系电话相同的就诊人");
        }
        return list(userId);
    }

    @Transactional
    public List<Profile> remove(Long userId, Long id) {
        lockAccount(userId);
        if (jdbc.update("DELETE FROM patient_profile WHERE user_id = ? AND id = ?", userId, id) == 0) {
            throw new BusinessException(BusinessErrorCode.NOT_FOUND, "常用就诊人不存在");
        }
        return list(userId);
    }

    private void lockAccount(Long userId) {
        jdbc.queryForObject("SELECT id FROM sys_user WHERE id = ? FOR UPDATE", Long.class, userId);
    }
}
