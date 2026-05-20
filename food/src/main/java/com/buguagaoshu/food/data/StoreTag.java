package com.buguagaoshu.food.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-20
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "store_tag_table")
public class StoreTag {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String name;

    private String description;

    private Long createTime;

    /**
     *
     * 越小越靠前
     * */
    private Integer sort = 0;
}
