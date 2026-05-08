package com.buguagaoshu.food.controller;

import com.buguagaoshu.food.dto.ResponseDetails;
import com.buguagaoshu.food.dto.UpdateSpecGroupDto;
import com.buguagaoshu.food.service.ProductSkuService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-08
 */
@RestController
@RequestMapping("/api/sku")
public class ProductSkuController {
    private final ProductSkuService productSkuService;

    public ProductSkuController(ProductSkuService productSkuService) {
        this.productSkuService = productSkuService;
    }


    @PostMapping("adjust")
    public ResponseDetails adjustSkuList(@RequestBody UpdateSpecGroupDto updateSpecGroupDto) {
        return ResponseDetails.ok().put("data", productSkuService.adjustSkuList(updateSpecGroupDto));
    }
}
