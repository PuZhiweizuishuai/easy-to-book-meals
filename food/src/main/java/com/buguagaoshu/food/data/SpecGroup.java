package com.buguagaoshu.food.data;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-04-26
 * 规格组
 * 代表一个规格的类别，例如“辣度”、“温度”、“分量”。它与菜品 (dish) 关联。
 */
@Data
@Entity
@Table(name = "spec_group_table", indexes = {
        @Index(name = "idx_spec_group_dishId", columnList = "dishId")
})
public class SpecGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    /**
     * 关联的菜品ID
     */
    private Long dishId;

    /**
     * 规格组名称，例如：辣度、温度、分量
     */
    private String name;

    /**
     * 排序权重
     */
    private Integer sort = 0;

    /**
     * 【非数据库字段】用于前端展示，包含该组下的所有规格值
     */
    @Transient
    private List<SpecValue> values;
}
