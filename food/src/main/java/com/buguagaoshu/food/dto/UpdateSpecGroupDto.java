package com.buguagaoshu.food.dto;

import com.buguagaoshu.food.data.SpecGroup;
import lombok.Data;

import java.util.List;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-08
 */
@Data
public class UpdateSpecGroupDto {
    private Long dishesId;

    private List<SpecGroup> specGroups;
}
