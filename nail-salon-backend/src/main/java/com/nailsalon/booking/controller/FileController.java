package com.nailsalon.booking.controller;

import com.nailsalon.booking.dto.ApiResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class FileController {

    private final String UPLOAD_DIR = System.getProperty("user.dir") + "/uploads/";

    @PostMapping("/upload")
    public ApiResponse<String> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ApiResponse.error(400, "文件为空");
        }
        List<String> ALLOWED_EXTENSIONS = List.of(".jpg", ".jpeg", ".png", ".gif", ".webp");
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            return ApiResponse.error(400, "文件名不能为空");
        }
        int dotIndex = originalFilename.lastIndexOf(".");
        if (dotIndex < 0) {
            return ApiResponse.error(400, "文件缺少扩展名");
        }
        String extension = originalFilename.substring(dotIndex).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return ApiResponse.error(400, "仅支持 JPG/PNG/GIF/WEBP 格式图片");
        }
        try {
            File dir = new File(UPLOAD_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String newFilename = UUID.randomUUID().toString() + extension;
            
            File dest = new File(dir, newFilename);
            file.transferTo(dest);
            
            String fileUrl = "/uploads/" + newFilename;
            return ApiResponse.success(fileUrl);
        } catch (IOException e) {
            e.printStackTrace();
            return ApiResponse.error(500, "文件上传失败");
        }
    }
}
