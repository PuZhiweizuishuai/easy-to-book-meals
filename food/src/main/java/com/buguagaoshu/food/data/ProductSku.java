package com.buguagaoshu.food.data;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-04-26
 */
@Data
@Entity
@Table(name = "product_sku_table")
public class ProductSku {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    /**
     * 关联的菜品ID
     */
    private Long dishId;

    /**
     * 该SKU的价格
     */
    private BigDecimal unitPrice;   // 单价（固定价格=总价，称重=每斤单价）

    private String unit;            // 单位：份、斤、克

    /**
     * 该SKU的库存
     */
    private Integer stock;

    /**
     * 折扣
     * */
    private Double discount;

    /**
     * 优惠截至日期
     * 0 为永远优惠
     * */
    private Long discountEndTime;

    /**
     * 规格值ID的JSON数组，例如 "[1, 5]"
     * 1 和 5 分别代表 spec_value 表中的ID
     */
    private String specs;

    /**
     * 是否启用：1-启用，0-禁用
     */
    private Integer status;
}
