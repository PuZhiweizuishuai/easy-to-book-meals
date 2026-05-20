package com.buguagaoshu.food.config;

import com.buguagaoshu.food.cache.CommonlyUsedDataCache;
import com.buguagaoshu.food.data.FoodTag;
import com.buguagaoshu.food.data.StoreTag;
import com.buguagaoshu.food.service.FoodTagService;
import com.buguagaoshu.food.service.StoreTagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.web.error.ErrorPage;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;

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
    public CommandLineRunner dataLoader(FoodTagService foodTagService, StoreTagService storeTagService) {
        return new CommandLineRunner() {

            @Override
            public void run(String... args) throws Exception {
                // 初始化食品标签缓存
                CommonlyUsedDataCache.foodTagMap = foodTagService.getFoodTagMap();
                try {
                    CommonlyUsedDataCache.foodTagList =
                            CommonlyUsedDataCache.foodTagMap.values().stream()
                                    .sorted(Comparator.comparingInt(FoodTag::getSort)) // 依据 sort 属性升序排序
                                    .collect(Collectors.toList());
                } catch (Exception e) {
                    CommonlyUsedDataCache.foodTagList = new ArrayList<>(CommonlyUsedDataCache.foodTagMap.values());
                }
                // 如果读取到的食品标签为空，则写入默认标签
                if (CommonlyUsedDataCache.foodTagMap.isEmpty()) {
                    FoodTag foodTag = new FoodTag();
                    foodTag.setName("推荐");
                    foodTag.setSort(0);
                    foodTagService.createTag(foodTag);
                    log.info("默认食品标签缓存成功！");
                }

                // 初始化店面标签缓存
                CommonlyUsedDataCache.storeTagMap = storeTagService.getStoreTagMap();
                try {
                    CommonlyUsedDataCache.storeTagList =
                            CommonlyUsedDataCache.storeTagMap.values().stream()
                                    .sorted(Comparator.comparingInt(StoreTag::getSort))
                                    .collect(Collectors.toList());
                } catch (Exception e) {
                    CommonlyUsedDataCache.storeTagList = new ArrayList<>(CommonlyUsedDataCache.storeTagMap.values());
                }
                log.info("店面标签缓存初始化完成，共 {} 个标签", CommonlyUsedDataCache.storeTagMap.size());
            }
        };
    }


    @Bean
    public WebServerFactoryCustomizer<ConfigurableWebServerFactory> webServerFactoryCustomizer(){
        return factory -> {
            ErrorPage error404Page = new ErrorPage(HttpStatus.NOT_FOUND, "/index.html");
            factory.addErrorPages(error404Page);
        };
    }

}
