package com.buguagaoshu.food.repository;

import com.buguagaoshu.food.data.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-10-02
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    /**
     * 批量查询多个订单的明细（避免 N+1 查询）
     */
    List<OrderItem> findByOrderIdIn(Collection<Long> orderIds);
}
