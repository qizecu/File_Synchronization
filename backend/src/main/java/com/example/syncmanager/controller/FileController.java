package com.example.syncmanager.controller;

import com.example.syncmanager.common.BusinessException;
import com.example.syncmanager.common.Result;
import com.example.syncmanager.dto.FileBatchDownloadDTO;
import com.example.syncmanager.dto.FileBrowseVO;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 文件浏览、下载、预览、上传
 */
@Slf4j
@RestController
@RequestMapping("/api/files")
public class FileController {

    @Value("${sync.local.storage-path}")
    private String baseStoragePath;

    /** 允许的图片 MIME 类型 */
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp"
    );

    /** 允许的图片扩展名 */
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "bmp"
    );

    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024; // 50MB

    // ==================== 浏览 ====================

    /**
     * 浏览目录，返回文件/子目录列表
     * @param path 相对路径（空或 / 表示根目录）
     */
    @GetMapping("/browse")
    public Result<List<FileBrowseVO>> browse(@RequestParam(required = false, defaultValue = "") String path) {
        validatePath(path);

        java.io.File dir = resolveDir(path);
        if (!dir.exists()) {
            throw new BusinessException("目录不存在: " + path);
        }
        if (!dir.isDirectory()) {
            throw new BusinessException("路径不是目录: " + path);
        }

        java.io.File[] files = dir.listFiles();
        if (files == null) {
            return Result.success(Collections.emptyList());
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        List<FileBrowseVO> result = Arrays.stream(files)
                .filter(f -> !f.isHidden())
                .sorted((a, b) -> {
                    // 目录排前面
                    if (a.isDirectory() && !b.isDirectory()) return -1;
                    if (!a.isDirectory() && b.isDirectory()) return 1;
                    return a.getName().compareToIgnoreCase(b.getName());
                })
                .map(f -> {
                    String ext = getExtension(f.getName());
                    boolean isImage = !f.isDirectory() && ALLOWED_IMAGE_EXTENSIONS.contains(ext);
                    // 构建相对路径
                    String relativePath = (path == null || path.isEmpty() || "/".equals(path))
                            ? f.getName()
                            : path + "/" + f.getName();
                    return FileBrowseVO.builder()
                            .name(f.getName())
                            .path(relativePath)
                            .size(f.isDirectory() ? 0 : f.length())
                            .lastModified(sdf.format(new Date(f.lastModified())))
                            .isDirectory(f.isDirectory())
                            .extension(ext)
                            .isImage(isImage)
                            .build();
                })
                .toList();
        return Result.success(result);
    }

    // ==================== 下载 ====================

    /** 单文件下载 */
    @GetMapping("/download")
    public void download(@RequestParam String path, HttpServletResponse response) throws IOException {
        validatePath(path);
        java.io.File file = resolveFile(path);
        assertFileExists(file);

        setDownloadHeaders(response, file.getName(), file.length());
        streamFile(file, response);
    }

    /** 批量下载：流式打包 ZIP */
    @PostMapping("/download-batch")
    public void downloadBatch(@Valid @RequestBody FileBatchDownloadDTO dto,
                              HttpServletResponse response) throws IOException {
        List<String> paths = dto.getPaths();
        if (paths == null || paths.isEmpty()) {
            throw new BusinessException("文件列表不能为空");
        }

        String zipName = "batch-download-" + System.currentTimeMillis() + ".zip";
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + URLEncoder.encode(zipName, StandardCharsets.UTF_8) + "\"");

        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            for (String path : paths) {
                validatePath(path);
                java.io.File file = resolveFile(path);
                if (!file.exists() || file.isDirectory()) continue;

                try (FileInputStream fis = new FileInputStream(file)) {
                    zos.putNextEntry(new ZipEntry(file.getName()));
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = fis.read(buf)) != -1) {
                        zos.write(buf, 0, len);
                    }
                    zos.closeEntry();
                }
            }
        }
    }

    // ==================== 预览 ====================

    /** 图片在线预览（返回图片流） */
    @GetMapping("/preview")
    public void preview(@RequestParam String path, HttpServletResponse response) throws IOException {
        validatePath(path);
        java.io.File file = resolveFile(path);
        assertFileExists(file);

        // 禁止预览非图片
        String ext = getExtension(file.getName());
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(ext)) {
            throw new BusinessException("仅支持预览图片文件");
        }

        String contentType = Files.probeContentType(file.toPath());
        if (contentType == null || !contentType.startsWith("image/")) {
            contentType = "image/" + (ext.equals("jpg") ? "jpeg" : ext);
        }
        response.setContentType(contentType);
        response.setHeader("Cache-Control", "max-age=3600");
        streamFile(file, response);
    }

    // ==================== 上传 ====================

    /** 上传图片文件（支持多文件） */
    @PostMapping("/upload")
    public Result<List<String>> upload(
            @RequestParam(required = false, defaultValue = "") String targetDir,
            @RequestParam("files") List<MultipartFile> files) throws IOException {

        validatePath(targetDir);

        if (files == null || files.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }

        // 解析目标目录
        java.io.File dir = resolveDir(targetDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        List<String> uploadedPaths = new ArrayList<>();
        for (MultipartFile file : files) {
            validateUploadFile(file);
            String fileName = file.getOriginalFilename();
            java.io.File destFile = new java.io.File(dir, fileName);
            // 避免覆盖：重名加时间戳
            if (destFile.exists()) {
                String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                String ext = getExtension(fileName);
                fileName = baseName + "_" + System.currentTimeMillis() + "." + ext;
                destFile = new java.io.File(dir, fileName);
            }
            file.transferTo(destFile);
            // 构建相对路径
            String relativePath = (targetDir == null || targetDir.isEmpty())
                    ? fileName : targetDir + "/" + fileName;
            uploadedPaths.add(relativePath);
            log.info("文件上传成功: {}", destFile.getAbsolutePath());
        }
        return Result.success(uploadedPaths);
    }

    // ==================== 安全校验 ====================

    /** 路径安全校验：防目录穿越 */
    private void validatePath(String path) {
        if (path == null || path.isEmpty()) return;
        if (path.contains("..")) {
            throw new BusinessException("非法路径，禁止目录穿越: " + path);
        }
        // 去除开头的 /
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
    }

    /** 上传文件类型 + 大小校验 */
    private void validateUploadFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("文件大小超过限制（最大 50MB）");
        }

        String originalName = file.getOriginalFilename();
        String ext = getExtension(originalName);
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(ext)) {
            throw new BusinessException("仅支持上传图片文件: jpg/png/gif/webp/bmp");
        }

        // Content-Type 双重校验
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new BusinessException("文件类型不合法，仅支持图片格式");
        }
    }

    // ==================== 辅助方法 ====================

    /** 解析为文件对象 */
    private java.io.File resolveFile(String relativePath) {
        return Paths.get(baseStoragePath, relativePath).normalize().toFile();
    }

    /** 解析为目录对象 */
    private java.io.File resolveDir(String relativePath) {
        String normalizedPath = relativePath;
        if (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }
        return Paths.get(baseStoragePath, normalizedPath).normalize().toFile();
    }

    private void assertFileExists(java.io.File file) {
        if (!file.exists()) {
            throw new BusinessException("文件不存在");
        }
        if (file.isDirectory()) {
            throw new BusinessException("路径为目录，不能下载");
        }
    }

    /** 获取文件扩展名（小写） */
    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    /** 设置下载响应头 */
    private void setDownloadHeaders(HttpServletResponse response, String fileName, long fileSize) {
        String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replace("+", "%20");
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + encodedName + "\"; filename*=UTF-8''" + encodedName);
        response.setContentLengthLong(fileSize);
    }

    /** 流式写入文件到响应 */
    private void streamFile(java.io.File file, HttpServletResponse response) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(fis);
             OutputStream os = response.getOutputStream()) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = bis.read(buf)) != -1) {
                os.write(buf, 0, len);
            }
            os.flush();
        }
    }
}
