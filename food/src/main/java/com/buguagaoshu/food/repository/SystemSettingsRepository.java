package com.buguagaoshu.food.repository;

import com.buguagaoshu.food.data.SystemSettingsData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 系统设置仓库
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 */
@Repository
public interface SystemSettingsRepository extends JpaRepository<SystemSettingsData, Long> {
}
