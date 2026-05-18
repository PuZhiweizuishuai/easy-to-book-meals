package com.buguagaoshu.food.controller;

import com.buguagaoshu.food.dto.ResponseDetails;
import com.buguagaoshu.food.enums.FileExternalTypeEnum;
import com.buguagaoshu.food.service.FileDataService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileController {
    private final FileDataService fileDataService;

    public FileController(FileDataService fileDataService) {
        this.fileDataService = fileDataService;
    }


    @PostMapping("/upload")
    public ResponseDetails upload(@RequestParam(value = "files[]")MultipartFile[] files) {
        return ResponseDetails.ok().put("data", fileDataService.uploadAndSave(files, FileExternalTypeEnum.FOOD, 0L));
    }
}
