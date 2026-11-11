package com.aaa.backend.file;

import com.aaa.backend.common.BusinessErrorCode;
import com.aaa.backend.common.BusinessException;
import com.aaa.backend.file.dto.UploadFileResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {
    public static final String AVATAR_CATEGORY = "avatar";
    public static final String PUBLIC_AVATAR_PATH = "/uploads/avatar/";

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
    private static final long MAX_FILE_BYTES = 5L * 1024 * 1024;

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "webp");

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    public UploadFileResponse storeAvatar(
            Long userId,
            MultipartFile file,
            String category
    ) {
        if (userId == null) {
            throw new BusinessException(
                    BusinessErrorCode.UNAUTHORIZED,
                    "请先登录"
            );
        }

        String actualCategory =
                category == null || category.isBlank()
                        ? AVATAR_CATEGORY
                        : category.trim().toLowerCase(Locale.ROOT);

        if (!AVATAR_CATEGORY.equals(actualCategory)) {
            throw new BusinessException(
                    BusinessErrorCode.BAD_REQUEST,
                    "当前只支持头像上传"
            );
        }

        if (file == null || file.isEmpty()) {
            throw new BusinessException(
                    BusinessErrorCode.BAD_REQUEST,
                    "请选择要上传的图片"
            );
        }

        if (file.getSize() > MAX_FILE_BYTES) {
            throw new BusinessException(
                    BusinessErrorCode.BAD_REQUEST,
                    "文件不能超过5MB"
            );
        }

        String extension = getExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(
                    BusinessErrorCode.BAD_REQUEST,
                    "只允许上传 jpg、jpeg、png、webp 图片"
            );
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            log.error("Failed to read uploaded avatar bytes, userId={}", userId, e);
            throw new BusinessException(
                    BusinessErrorCode.INTERNAL_ERROR,
                    "读取上传文件失败"
            );
        }

        if (!hasValidImageSignature(content, extension)) {
            throw new BusinessException(
                    BusinessErrorCode.BAD_REQUEST,
                    "文件内容与图片格式不匹配"
            );
        }

        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path userDirectory = root
                .resolve(AVATAR_CATEGORY)
                .resolve(userId.toString())
                .normalize();

        if (!userDirectory.startsWith(root)) {
            throw new BusinessException(
                    BusinessErrorCode.BAD_REQUEST,
                    "上传路径不合法"
            );
        }

        String fileName = UUID.randomUUID() + "." + extension;
        Path destination = userDirectory.resolve(fileName).normalize();

        if (!destination.startsWith(userDirectory)) {
            throw new BusinessException(
                    BusinessErrorCode.BAD_REQUEST,
                    "上传路径不合法"
            );
        }

        try {
            Files.createDirectories(userDirectory);
            Files.write(
                    destination,
                    content,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE
            );
        } catch (IOException e) {
            log.error("Failed to store uploaded avatar, userId={}", userId, e);
            throw new BusinessException(
                    BusinessErrorCode.INTERNAL_ERROR,
                    "保存上传文件失败"
            );
        }

        String publicPath = PUBLIC_AVATAR_PATH + userId + "/" + fileName;

        return new UploadFileResponse(publicPath, publicPath);
    }

    public boolean isOwnedAvatarPath(Long userId, String path) {
        if (userId == null || path == null || path.isBlank()) {
            return false;
        }

        String prefix = PUBLIC_AVATAR_PATH + userId + "/";
        if (!path.startsWith(prefix)) {
            return false;
        }

        String fileName = path.substring(prefix.length());

        // 只接受一个文件名，不接受目录、路径穿越或 URL
        if (fileName.isBlank()
                || fileName.contains("/")
                || fileName.contains("\\")
                || fileName.contains("..")) {
            return false;
        }

        String extension = getExtension(fileName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return false;
        }

        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path userDirectory = root
                .resolve(AVATAR_CATEGORY)
                .resolve(userId.toString())
                .normalize();
        Path storedFile = userDirectory.resolve(fileName).normalize();

        return storedFile.startsWith(userDirectory)
                && Files.isRegularFile(storedFile);
    }

    private String getExtension(String originalFileName) {
        if (originalFileName == null) {
            return "";
        }

        int dot = originalFileName.lastIndexOf('.');
        if (dot < 0 || dot == originalFileName.length() - 1) {
            return "";
        }

        return originalFileName.substring(dot + 1)
                .toLowerCase(Locale.ROOT);
    }

    private boolean hasValidImageSignature(byte[] bytes, String extension) {
        if (bytes == null) {
            return false;
        }

        if ("jpg".equals(extension) || "jpeg".equals(extension)) {
            return bytes.length >= 3
                    && (bytes[0] & 0xFF) == 0xFF
                    && (bytes[1] & 0xFF) == 0xD8
                    && (bytes[2] & 0xFF) == 0xFF;
        }

        if ("png".equals(extension)) {
            byte[] signature = {
                    (byte) 0x89, 0x50, 0x4E, 0x47,
                    0x0D, 0x0A, 0x1A, 0x0A
            };

            if (bytes.length < signature.length) {
                return false;
            }

            for (int i = 0; i < signature.length; i++) {
                if (bytes[i] != signature[i]) {
                    return false;
                }
            }
            return true;
        }

        if ("webp".equals(extension)) {
            return bytes.length >= 12
                    && "RIFF".equals(
                    new String(bytes, 0, 4, StandardCharsets.US_ASCII)
            )
                    && "WEBP".equals(
                    new String(bytes, 8, 4, StandardCharsets.US_ASCII)
            );
        }

        return false;
    }
}
