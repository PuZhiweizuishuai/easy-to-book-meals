package com.buguagaoshu.food.controller;

import com.buguagaoshu.food.dto.ResponseDetails;
import com.buguagaoshu.food.service.FileDataService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/files")
public class FileController {
    private final FileDataService fileDataService;

    public FileController(FileDataService fileDataService) {
        this.fileDataService = fileDataService;
    }


    @PostMapping("/upload")
    public ResponseDetails upload() {
        return ResponseDetails.ok();
    }
}
