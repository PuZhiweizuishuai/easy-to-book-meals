package com.buguagaoshu.food.service;

import com.buguagaoshu.food.data.ProductSku;
import com.buguagaoshu.food.repository.ProductSkuRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-04-27
 */
@Service
public class ProductSkuService {
    private final ProductSkuRepository productSkuRepository;

    public ProductSkuService(ProductSkuRepository productSkuRepository) {
        this.productSkuRepository = productSkuRepository;
    }

    public void saveAllProductSku(Iterable<ProductSku> productSkus) {
        productSkuRepository.saveAll(productSkus);
    }

    public List<ProductSku> findByDishId(Long dishId) {
        return productSkuRepository.findByDishId(dishId);
    }
}
