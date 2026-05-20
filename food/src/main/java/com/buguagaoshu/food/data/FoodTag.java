package com.buguagaoshu.food.data;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
  *@author  Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
  *@create  2026-04-26
 * 食物分类
  */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "food_tag_table")
public class FoodTag {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    /**
     * 分类名称
     * */
    private String name;

    /**
     * 分类下菜品数量
     * */
    private Integer count;

    /**
     * 由小到大
     * 默认 1
     * */
    private Integer sort = 1;


    /**
     * 创建时间
     * */
    private Long createTime;
}
