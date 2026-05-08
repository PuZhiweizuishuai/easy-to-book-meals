package com.buguagaoshu.food.controller;

import com.buguagaoshu.food.dto.ResponseDetails;
import com.buguagaoshu.food.service.SpecGroupService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
  *@author  Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
  *@create  2026-05-08
  */
@RestController
@RequestMapping("/api/spec")
public class SpecController {
    private final SpecGroupService specGroupService;

    public SpecController(SpecGroupService specGroupService) {
        this.specGroupService = specGroupService;
    }


    @GetMapping("/group/{id}")
    public ResponseDetails getSpecGroup(@PathVariable(value = "id") Long id) {
        return ResponseDetails.ok().put("data", specGroupService.getSpecGroupsByDishId(id));
    }
}
