package com.buguagaoshu.food.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-04-26
 * 规格值
 * 代表一个规格组下的具体选项，例如“辣度”组下的“微辣”、“中辣”、“不辣”。它与规格组关联。
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "spec_value_table", indexes = {
        @Index(name = "idx_spec_value_groupId", columnList = "groupId")
})
public class SpecValue {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    /**
     * 关联的规格组ID
     */
    private Long groupId;

    /**
     * 规格值名称，例如：微辣、中辣、大份、去冰
     */
    private String name;

    /**
     * 排序权重
     */
    private Integer sort = 0;
}
