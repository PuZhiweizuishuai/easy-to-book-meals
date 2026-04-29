package com.buguagaoshu.food.cache;

import com.buguagaoshu.food.data.FoodTag;

import java.util.List;
import java.util.Map;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-04-26
 * 用于保存需要构造各种数据的缓存
 * 如缓存食品分类
 */
public class CommonlyUsedDataCache {
    public static Map<Long, FoodTag> foodTagMap;

    public static List<FoodTag> foodTagList;
}
