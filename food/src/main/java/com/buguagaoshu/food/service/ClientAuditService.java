package com.buguagaoshu.food.service;

import com.buguagaoshu.food.data.OrderData;
import com.buguagaoshu.food.data.Store;
import com.buguagaoshu.food.data.UserData;
import com.buguagaoshu.food.utils.IpUtils;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import org.lionsoul.ip2region.service.Config;
import org.lionsoul.ip2region.service.Ip2Region;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.File;

/**
 * 客户端审计信息服务
 * <p>
 * 为「创建订单 / 创建店面 / 注册账户」等写库动作补充来源留痕：客户端 IP、User-Agent、
 * IP 位置信息（离线库 <a href="https://gitee.com/lionsoul/ip2region">ip2region</a> 解析，无外网请求），
 * 便于后期数据审计与风控追溯。
 * <p>
 * 设计要点：
 * <ul>
 *   <li>IP/UA 从当前请求上下文（{@link RequestContextHolder}）读取，业务方法无需层层透传参数；
 *       定时任务等非 HTTP 线程取不到请求，三个字段均为 null</li>
 *   <li>{@code food.ip-audit.enabled=false} 时不做归属地解析，位置字段落库为 null（IP、UA 仍保存）</li>
 *   <li>xdb 文件缺失或解析失败只降级为「位置为 null」，不影响下单/注册等主流程</li>
 * </ul>
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-10-08
 */
@Service
public class ClientAuditService {

    private static final Logger logger = LoggerFactory.getLogger(ClientAuditService.class);

    /** 归属地原始格式：国家|区域|省份|城市|ISP */
    private static final String REGION_SEPARATOR = "\\|";

    /** ip2region 中代表「未知/不适用」的占位段 */
    private static final String UNKNOWN_PLACEHOLDER = "0";

    /** 离线库实例，为 null 表示未开启或加载失败（位置信息一律记为 null） */
    private final Ip2Region ip2Region;

    public ClientAuditService(
            @Value("${food.ip-audit.enabled:true}") boolean locationEnabled,
            @Value("${food.ip-audit.xdb-dir:ip}") String xdbDir,
            @Value("${food.ip-audit.v4-file-name:ip2region_v4.xdb}") String v4FileName,
            @Value("${food.ip-audit.v6-file-name:ip2region_v6.xdb}") String v6FileName) {
        if (!locationEnabled) {
            logger.info("IP 位置记录已关闭（food.ip-audit.enabled=false），仅保存客户端 IP 与 User-Agent");
            this.ip2Region = null;
            return;
        }
        this.ip2Region = loadXdb(xdbDir, v4FileName, v6FileName);
        logger.info("IP 位置记录已开启: xdbDir={}, 离线库={}",
                xdbDir, ip2Region == null ? "加载失败（位置字段记为 null）" : "加载成功");
    }

    /**
     * 为订单写入下单来源信息（顾客下单 / 扫码下单 / 店员协助下单共用）
     */
    public void fill(OrderData order) {
        AuditInfo info = currentAudit();
        order.setClientIp(info.ip());
        order.setClientUa(info.userAgent());
        order.setClientIpLocation(info.ipLocation());
    }

    /**
     * 为门店写入开店申请来源信息
     */
    public void fill(Store store) {
        AuditInfo info = currentAudit();
        store.setClientIp(info.ip());
        store.setClientUa(info.userAgent());
        store.setClientIpLocation(info.ipLocation());
    }

    /**
     * 为用户写入注册来源信息（含扫码点餐自动创建的匿名顾客）
     */
    public void fill(UserData user) {
        AuditInfo info = currentAudit();
        user.setClientIp(info.ip());
        user.setClientUa(info.userAgent());
        user.setClientIpLocation(info.ipLocation());
    }

    /**
     * 解析 IP 位置信息
     *
     * @param ip 已校验的 IP 字面量
     * @return 形如「中国|广东省|深圳市|电信」；未开启记录、内网/无法解析、查询失败时返回 null
     */
    public String resolveLocation(String ip) {
        if (ip2Region == null || ip == null) {
            return null;
        }
        try {
            return formatRegion(ip2Region.search(ip));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("IP 位置查询被中断: ip={}", ip);
            return null;
        } catch (Exception e) {
            // 位置解析失败不影响下单/注册等主流程
            logger.warn("IP 位置查询失败: ip={}, cause={}", ip, e.getMessage());
            return null;
        }
    }

    /**
     * 当前 HTTP 请求的来源信息；非请求线程（定时任务等）返回全 null
     */
    private AuditInfo currentAudit() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return new AuditInfo(null, null, null);
        }
        HttpServletRequest request = servletAttributes.getRequest();
        String ip = IpUtils.getClientIp(request);
        return new AuditInfo(ip, IpUtils.sanitizeUserAgent(request.getHeader("User-Agent")),
                resolveLocation(ip));
    }

    /**
     * 位置串归一化：丢弃代表未知的 0 占位段，超长截断
     */
    private static String formatRegion(String region) {
        if (region == null || region.isBlank()) {
            return null;
        }
        StringBuilder sb = new StringBuilder(region.length());
        for (String part : region.split(REGION_SEPARATOR)) {
            String value = part.trim();
            if (value.isEmpty() || UNKNOWN_PLACEHOLDER.equals(value)) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append('|');
            }
            sb.append(value);
        }
        String formatted = sb.toString();
        if (formatted.isEmpty()) {
            return null;
        }
        return formatted.length() > IpUtils.IP_LOCATION_MAX_LENGTH
                ? formatted.substring(0, IpUtils.IP_LOCATION_MAX_LENGTH) : formatted;
    }

    /**
     * 加载离线库：IPv4 库较小（约 11MB）全量驻留内存，查询最快且不持有文件句柄；
     * IPv6 库较大（约 37MB）只缓存向量索引，按句柄池读文件，避免常驻内存过大
     */
    private static Ip2Region loadXdb(String xdbDir, String v4FileName, String v6FileName) {
        File v4File = new File(xdbDir, v4FileName);
        File v6File = new File(xdbDir, v6FileName);
        try {
            Config v4Config = Config.custom().setXdbFile(v4File).setCachePolicy(Config.BufferCache).asV4();
            Config v6Config = Config.custom().setXdbFile(v6File)
                    .setCachePolicy(Config.VIndexCache).setSearchers(8).asV6();
            return Ip2Region.create(v4Config, v6Config);
        } catch (Exception e) {
            logger.error("ip2region 离线库加载失败，IP 位置字段将全部记为 null: v4={}, v6={}, cause={}",
                    v4File.getAbsolutePath(), v6File.getAbsolutePath(), e.toString());
            return null;
        }
    }

    @PreDestroy
    public void close() {
        if (ip2Region == null) {
            return;
        }
        try {
            ip2Region.close();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 单次请求解析出的客户端来源信息
     */
    private record AuditInfo(String ip, String userAgent, String ipLocation) {
    }
}
