package com.buguagaoshu.food.config;

import com.buguagaoshu.food.cache.CommonlyUsedDataCache;
import com.buguagaoshu.food.service.FoodTagService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-04-26
 */
public class WebConfig {
    /**
     * 初始化方法
     * */
    @Bean
    public CommandLineRunner dataLoader(FoodTagService foodTagService) {
        return new CommandLineRunner() {

            @Override
            public void run(String... args) throws Exception {
                CommonlyUsedDataCache.foodTagMap = foodTagService.getFoodTagMap();
            }
        };
    }
}
