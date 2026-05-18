package com.buguagaoshu.food.config;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-04-26
 */
public class WebConstant {
    /**
     * 启用
     * */
    public final static int ENABLE = 1;

    /**
     * 禁用
     * */
    public final static int DISABLE = 0;

    /**
     * 临时文件
     * */
    public final static int TEMP_FILE = 0;

    public final static int USE_FILE = 1;

    /**
     * 锁定状态，不允许修改
     * 如当规格组发生变化导致没有当前规格，旧规格的sku将被锁定
     * 无法被编辑，只能查看
     * */
    public final static int LOCK = 2;

    /**
     * 无限库存
     * */
    public final static long UNLIMITED_INVENTORY = -1;
}
