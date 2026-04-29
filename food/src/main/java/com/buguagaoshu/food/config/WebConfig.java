package com.buguagaoshu.food.config;

import com.buguagaoshu.food.cache.CommonlyUsedDataCache;
import com.buguagaoshu.food.data.FoodTag;
import com.buguagaoshu.food.service.FoodTagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-04-26
 */
@Configuration
@Slf4j
public class WebConfig {
    /**
     * 初始化方法
     * */
    @Bean
    public CommandLineRunner dataLoader(FoodTagService foodTagService) {
        return new CommandLineRunner() {

            @Override
            public void run(String... args) throws Exception {
                long startTime = System.currentTimeMillis();
                // 初始化食品标签缓存
                CommonlyUsedDataCache.foodTagMap = foodTagService.getFoodTagMap();
                // 如果读取到的食品标签为空，则写入默认标签
                if (CommonlyUsedDataCache.foodTagMap.isEmpty()) {
                    FoodTag foodTag = new FoodTag();
                    foodTag.setName("推荐");
                    foodTagService.createTag(foodTag);
                    log.info("默认食品标签缓存成功！");
                }
            }
        };
    }

}
