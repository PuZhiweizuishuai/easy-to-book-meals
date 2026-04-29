package com.buguagaoshu.food.data;

import jakarta.persistence.*;
import lombok.Data;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-04-26
 * 用于关联食品与分类信息
 */
@Data
@Entity
@Table(name = "association_food_tag_table", indexes = {
        @Index(name = "idx_association_food_foodId", columnList = "foodId"),
        @Index(name = "idx_association_food_tagId", columnList = "tagId")
})
public class FoodWithTag {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Long foodId;

    private Long tagId;

    private Long createTime;
}
