package com.buguagaoshu.food.service;

import com.buguagaoshu.food.data.SpecGroup;
import com.buguagaoshu.food.data.SpecValue;
import com.buguagaoshu.food.dto.CreateDishes;
import com.buguagaoshu.food.dto.CreateDishesResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class DishesServiceTest {
    @Autowired
    private DishesService dishesService;

    @Test
    public void testCreateDishes() {
        // 创建测试数据
        CreateDishes createDishes = new CreateDishes();
        createDishes.setName("宫保鸡丁");
        createDishes.setDescription("经典川菜，口味麻辣鲜香");
        createDishes.setTopImage("https://example.com/gongbao.jpg");
        
        List<String> imageList = new ArrayList<>();
        imageList.add("https://example.com/gongbao1.jpg");
        imageList.add("https://example.com/gongbao2.jpg");
        createDishes.setImageList(imageList);
        
        createDishes.setPriceType(0); // 按份计价
        createDishes.setStock(100L);
        createDishes.setStatus(1); // 上架
        
        // 添加规格组和规格值
        List<SpecGroup> specGroups = new ArrayList<>();
        
        // 辣度规格组
        SpecGroup spicyGroup = new SpecGroup();
        spicyGroup.setName("辣度");
        spicyGroup.setSort(1);
        
        List<SpecValue> spicyValues = new ArrayList<>();
        SpecValue mild = new SpecValue();
        mild.setName("微辣");
        mild.setSort(1);
        spicyValues.add(mild);
        
        SpecValue medium = new SpecValue();
        medium.setName("中辣");
        medium.setSort(2);
        spicyValues.add(medium);
        
        SpecValue hot = new SpecValue();
        hot.setName("特辣");
        hot.setSort(3);
        spicyValues.add(hot);
        
        spicyGroup.setValues(spicyValues);
        specGroups.add(spicyGroup);
        
        // 分量规格组
        SpecGroup sizeGroup = new SpecGroup();
        sizeGroup.setName("分量");
        sizeGroup.setSort(2);
        
        List<SpecValue> sizeValues = new ArrayList<>();
        SpecValue small = new SpecValue();
        small.setName("小份");
        small.setSort(1);
        sizeValues.add(small);
        
        SpecValue large = new SpecValue();
        large.setName("大份");
        large.setSort(2);
        sizeValues.add(large);
        
        sizeGroup.setValues(sizeValues);
        specGroups.add(sizeGroup);
        
        createDishes.setSpecGroups(specGroups);
        
        // 调用createDishes方法
        CreateDishesResult result = dishesService.createDishes(createDishes);
        
        // 验证结果
        assertNotNull(result, "返回结果不能为空");
        assertNotNull(result.getFood(), "菜品信息不能为空");
        assertNotNull(result.getFood().getId(), "菜品ID不能为空");
        assertTrue(result.getSkuList().size() > 0, "SKU列表不能为空");
        
        // 验证SKU数量是否正确（辣度3种 * 分量2种 = 6种组合）
        assertTrue(result.getSkuList().size() == 6, "SKU数量应该为6");
        
        System.out.println("测试通过，菜品创建成功！");
        System.out.println("菜品ID: " + result.getFood().getId());
        System.out.println("SKU数量: " + result.getSkuList().size());
    }
}
