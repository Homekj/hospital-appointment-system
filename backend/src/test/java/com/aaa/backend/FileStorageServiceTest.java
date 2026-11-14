package com.aaa.backend;

import com.aaa.backend.file.FileStorageService;
import com.aaa.backend.file.dto.UploadFileResponse;
import com.aaa.backend.common.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void storesAndServesUploadedAvatarForUploader() {
        FileStorageService service = service();
        byte[] pngSignature = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        MockMultipartFile file = new MockMultipartFile("file", "doctor.png", "image/png", pngSignature);

        UploadFileResponse result = service.storeAvatar(42L, file, "avatar");
        Path storedFile = tempDir.resolve(result.path().substring("/uploads/".length()));

        assertTrue(result.path().startsWith("/uploads/avatar/42/"));
        assertTrue(Files.isRegularFile(storedFile));
        assertTrue(service.isOwnedAvatarPath(42L, result.path()));
        assertFalse(service.isOwnedAvatarPath(7L, result.path()));
    }

    @Test
    void rejectsImageWithMismatchedFileSignature() {
        FileStorageService service = service();
        MockMultipartFile file = new MockMultipartFile("file", "doctor.png", "image/png",
                "not a png".getBytes());

        assertThrows(BusinessException.class, () -> service.storeAvatar(42L, file, "avatar"));
    }

    private FileStorageService service() {
        FileStorageService service = new FileStorageService();
        ReflectionTestUtils.setField(service, "uploadDir", tempDir.toString());
        return service;
    }
}
