package com.buguagaoshu.food.data;

import com.buguagaoshu.food.config.WebConstant;
import com.buguagaoshu.food.utils.JsonUtils;
import com.buguagaoshu.food.utils.MD5Util;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-04-26
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "product_sku_table", indexes = {
        @Index(name = "idx_sku_dishId", columnList = "dishId"),
        @Index(name = "idx_sku_status", columnList = "status"),
        @Index(name = "idx_sku_storefront", columnList = "status")
})
public class ProductSku {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    /**
     * 关联的菜品ID
     */
    private Long dishId;

    /**
     * TODO 所属店面
     * */
    private Long storefrontId;

    /**
     * 该sku的描述
     * */
    @Column(columnDefinition = "text")
    private String description = "";

    /**
     * 该SKU的价格
     */
    private BigDecimal unitPrice;   // 单价（固定价格=总价，称重=每斤单价）

    private String unit;            // 单位：份、斤、克

    /**
     * 该SKU的库存
     * -1 表示不限库存
     */
    private Long stock = WebConstant.UNLIMITED_INVENTORY;

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
     * 总销量
     */
    private Long sales;

    /**
     * 规格值ID的JSON数组，例如 "[1, 5]"
     * 1 和 5 分别代表 spec_value 表中的ID
     * 由小到大排序
     */
    @Column(columnDefinition = "text")
    private String specs;

    /**
     * 规格值ID的JSON数组 MD5 值
     * */
    private String specsHash;


    /**
     * 规格值名称缓存
     * ["", ""]
     * */
    @Column(columnDefinition = "text")
    private String specNames;

    /**
     * 是否启用：1-启用，0-禁用
     */
    private Integer status;

    public void setSpecs(List<Long> specs) {
        // 由小到大排序
        Collections.sort(specs);
        //
        try {
            this.specs = JsonUtils.toJson(specs);
        } catch (Exception e) {
            this.specs = "[]";
        }
    }

    public void setSpecNames(Map<Long, String> specNames) {
        try {
            this.specNames = JsonUtils.toJson(specNames);
        } catch (Exception e) {
            this.specNames = "[]";
        }
    }


    public void setSpecsHash(String specs) {
        this.specsHash = MD5Util.getMD5(specs);
    }
}
