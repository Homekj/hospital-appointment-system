package com.aaa.backend.file;

import com.aaa.backend.auth.CurrentUserContext;
import com.aaa.backend.common.ApiResponse;
import com.aaa.backend.file.dto.UploadFileResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {
    private final FileStorageService fileStorageService;
    private final CurrentUserContext currentUserContext;

    public FileUploadController(
            FileStorageService fileStorageService,
            CurrentUserContext currentUserContext
    ) {
        this.fileStorageService = fileStorageService;
        this.currentUserContext = currentUserContext;
    }

    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApiResponse<UploadFileResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", required = false) String category
    ) {
        Long userId = currentUserContext.requireUserId();
        return ApiResponse.success(
                fileStorageService.storeAvatar(userId, file, category)
        );
    }
}