package com.buguagaoshu.food.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.buguagaoshu.food.config.FileConfig;
import com.buguagaoshu.food.dto.ResponseDetails;
import com.buguagaoshu.food.enums.FileExternalTypeEnum;
import com.buguagaoshu.food.service.FileDataService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;


@RestController
@RequestMapping("/api/files")
public class FileController {
    private final FileDataService fileDataService;

    public FileController(FileDataService fileDataService) {
        this.fileDataService = fileDataService;
    }


    @SaCheckLogin
    @PostMapping("/upload")
    public ResponseDetails upload(@RequestParam(value = "files[]") MultipartFile[] files) {
        return ResponseDetails.ok().put("data",
                fileDataService.uploadAndSave(files, FileExternalTypeEnum.UNKNOWN, StpUtil.getLoginIdAsLong()));
    }

    @GetMapping("/{year}/{month}/{day}/{filename:.+}")
    public ResponseEntity<Resource> get(@PathVariable String year,
                              @PathVariable String month,
                              @PathVariable String day,
                              @PathVariable String filename,
                              HttpServletRequest request) {
        try {
            // 路径穿越防护：解析后的绝对路径必须仍位于文件根目录内
            Path root = Paths.get(FileConfig.ROOT).toAbsolutePath().normalize();
            Path target = root.resolve(year).resolve(month).resolve(day).resolve(filename).normalize();
            if (!target.startsWith(root)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(target.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add(HttpHeaders.CONTENT_TYPE, contentType);
            httpHeaders.add("X-Content-Type-Options", "nosniff");
            httpHeaders.add("Cache-Control", "max-age=86400");
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .headers(httpHeaders)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
