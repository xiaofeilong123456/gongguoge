package com.gongguoge.controller;

import com.gongguoge.common.Result;
import com.gongguoge.common.exception.BusinessException;
import com.gongguoge.common.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 文件上传与访问（当前用于头像）。
 *
 * <p>文件保存到本机目录（gongguoge.upload.dir），并通过 GET /api/files/avatar/{date}/{name}
 * 由本控制器直接回传。这里没有用 Spring 的静态资源映射，
 * 因为本地目录映射在打包/相对路径场景下容易失效，控制器方式更可控、也便于加鉴权。</p>
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private static final List<String> ALLOWED_EXT = Arrays.asList("jpg", "jpeg", "png", "webp", "gif");
    private static final long MAX_BYTES = 2L * 1024 * 1024;
    private static final String SUB_DIR = "avatar";

    @Value("${gongguoge.upload.dir:./uploads}")
    private String uploadDir;

    @PostMapping("/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BusinessException("文件大小不能超过 2MB");
        }

        String ext = extensionOf(file.getOriginalFilename());
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BusinessException("仅支持 jpg / png / webp / gif 格式的图片");
        }

        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path dir = Paths.get(uploadDir, SUB_DIR, datePath).toAbsolutePath().normalize();
        Path target = dir.resolve(filename);

        try {
            Files.createDirectories(dir);
            file.transferTo(target.toFile());
        } catch (IOException e) {
            throw new BusinessException("文件保存失败：" + e.getMessage());
        }

        Map<String, String> data = new HashMap<>();
        // 存库与访问统一用这个相对地址，换域名无需改数据
        data.put("url", "/api/files/" + SUB_DIR + "/" + datePath + "/" + filename);
        data.put("filename", filename);
        data.put("size", String.valueOf(file.getSize()));
        data.put("userId", String.valueOf(UserContext.getUserId()));
        return Result.success("上传成功", data);
    }

    /**
     * 读取已上传的文件。日期与文件名都做了格式校验，避免路径穿越。
     */
    @GetMapping("/" + SUB_DIR + "/{date}/{filename}")
    public ResponseEntity<Resource> read(
            @PathVariable String date,
            @PathVariable String filename) {

        if (!date.matches("\\d{8}") || !filename.matches("[A-Za-z0-9]+\\.[A-Za-z0-9]+")) {
            return ResponseEntity.notFound().build();
        }
        Path base = Paths.get(uploadDir, SUB_DIR, date).toAbsolutePath().normalize();
        Path target = base.resolve(filename).normalize();
        // 双保险：解析后的路径必须仍在基目录内
        if (!target.startsWith(base) || !Files.isRegularFile(target)) {
            return ResponseEntity.notFound().build();
        }

        String ext = extensionOf(filename);
        MediaType mediaType = MediaType.IMAGE_JPEG;
        if ("png".equals(ext)) {
            mediaType = MediaType.IMAGE_PNG;
        } else if ("webp".equals(ext)) {
            mediaType = MediaType.parseMediaType("image/webp");
        } else if ("gif".equals(ext)) {
            mediaType = MediaType.IMAGE_GIF;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(new FileSystemResource(target));
    }

    private String extensionOf(String name) {
        if (name == null) {
            return "";
        }
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    }
}
