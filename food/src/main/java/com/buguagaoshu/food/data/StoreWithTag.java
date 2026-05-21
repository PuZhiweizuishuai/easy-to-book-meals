package com.buguagaoshu.food.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-20
 * 用于关联店面与标签信息
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "association_store_tag_table", indexes = {
        @Index(name = "idx_association_store_storeId", columnList = "storeId"),
        @Index(name = "idx_association_store_tagId", columnList = "tagId")
})
@EntityListeners(AuditingEntityListener.class)
public class StoreWithTag {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Long storeId;

    private Long tagId;

    @CreatedDate
    private Long createTime;
}
