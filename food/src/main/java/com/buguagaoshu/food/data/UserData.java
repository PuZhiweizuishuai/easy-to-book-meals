package com.buguagaoshu.food.data;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.List;


/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-20
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_table", indexes = {
        @Index(name = "idx_user_table_wxopenid", columnList = "wxOpenid"),
        @Index(name = "idx_user_table_phone", columnList = "phone"),
        @Index(name = "idx_user_table_email", columnList = "email"),
})
@EntityListeners(AuditingEntityListener.class)
public class UserData {

    /**
     * 主键ID 自增
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;


    //@Size(max = 25, message = "密码最长不能超过25位")
    private String password;

    /**
     * 微信小程序用户唯一标识（必须）
     * 小程序登录获取，用于唯一标识用户
     */
    @Column(unique = true, length = 64)
    private String wxOpenid;

    /**
     * 支付宝
     * TODO 基本没啥用，先占着，万一后面支持呢
     * */
    @Column(unique = true, length = 64)
    private String zfbOpenid;

    /**
     * 抖音
     * TODO 基本没啥用，先占着，万一后面支持呢
     * */
    @Column(unique = true, length = 64)
    private String dyOpenid;

    /**
     * 用户昵称（微信昵称）
     */
    @Size(max = 50, message = "昵称最长不超过50个字符")
    @Column(length = 50)
    private String nickname;

    /**
     * 用户头像URL
     */
    @Column(columnDefinition = "TEXT")
    private String avatarUrl;

    /**
     * 手机号（用于外卖配送、短信通知、会员积分关联）
     * <p>
     * 允许为空：扫码免登录下单时由系统创建的匿名顾客无手机号，
     * 结算时由店员录入后回填到用户记录并用于会员积分累积。
     */
    @Size(max = 20, message = "手机号最长不超过20个字符")
    @Column(unique = true, length = 20)
    private String phone;


    @Size(max = 100, message = "邮箱最长不超过100个字符")
    @Column(unique = true)
    private String email;

    /**
     * 性别 0-未知 1-男 2-女
     */
    private Integer gender = 0;

    /**
     * 收货地址（可存JSON字符串，支持多个地址）
     */
    @Column(columnDefinition = "TEXT")
    private String address;


    /**
     * 账号状态 0-正常 1-禁用
     */
    private Integer status = 0;

    /**
     * 创建时间（自动插入）
     */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Long createTime;

    /**
     * 更新时间（自动更新）
     */
    @LastModifiedDate
    @Column(nullable = false)
    private Long updateTime;

    @Transient
    private List<RoleData> roleDataList;
}