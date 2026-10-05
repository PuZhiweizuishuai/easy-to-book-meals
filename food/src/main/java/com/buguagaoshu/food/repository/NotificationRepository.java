package com.buguagaoshu.food.repository;

import com.buguagaoshu.food.data.NotificationData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-10-05
 */
@Repository
public interface NotificationRepository extends JpaRepository<NotificationData, Long> {

    /**
     * 分页查询某用户的通知，按时间倒序
     */
    Page<NotificationData> findByUserIdOrderByCreateTimeDesc(Long userId, Pageable pageable);

    /**
     * 统计某用户指定状态的通知数量（未读角标 / 待处理数量）
     */
    long countByUserIdAndStatus(Long userId, Integer status);

    /**
     * 查询某门店指定类型与状态的通知（店主端「待确认」员工列表）
     */
    List<NotificationData> findByStoreIdAndTypeAndStatus(Long storeId, String type, Integer status);

    /**
     * 查询某用户在某门店是否已有待处理的同类型通知（防止重复邀请）
     */
    List<NotificationData> findByUserIdAndStoreIdAndTypeAndStatus(Long userId, Long storeId,
                                                                  String type, Integer status);

    /**
     * 批量把某用户的未读通知标记为已读
     */
    @Modifying
    @Query("UPDATE NotificationData n SET n.status = 1, n.handleTime = :now " +
            "WHERE n.userId = :userId AND n.status = 0 AND n.type <> :excludeType")
    int markAllRead(@Param("userId") Long userId, @Param("now") Long now,
                    @Param("excludeType") String excludeType);
}
