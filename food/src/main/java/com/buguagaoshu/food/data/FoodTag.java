package com.buguagaoshu.food.data;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "food_tag_table")
public class FoodTag {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String name;

    private Integer count;

    private Integer status;

    private Long createTime;
}
