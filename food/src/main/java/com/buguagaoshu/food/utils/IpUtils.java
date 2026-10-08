package com.buguagaoshu.food.utils;

import jakarta.servlet.http.HttpServletRequest;

import java.util.regex.Pattern;

/**
 * 客户端请求信息工具：提取真实 IP、清洗 User-Agent
 * <p>
 * 经过 Nginx 等反向代理时依赖代理正确设置 X-Forwarded-For / X-Real-IP，
 * 生产环境应禁止外部客户端直接伪造这些头。
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-10-08
 */
public final class IpUtils {

    /** 客户端 IP 字段长度上限（IPv6 最长 45 字符，留余量） */
    public static final int IP_MAX_LENGTH = 64;

    /** User-Agent 字段长度上限 */
    public static final int USER_AGENT_MAX_LENGTH = 500;

    /** IP 位置信息字段长度上限 */
    public static final int IP_LOCATION_MAX_LENGTH = 128;

    /** IPv4 点分十进制（各段 0-255） */
    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)");

    /** IPv6 字面量允许的字符集（含内嵌 IPv4 尾段与 %zone 接口标识） */
    private static final Pattern IPV6_CHARS_PATTERN = Pattern.compile("[0-9a-fA-F:.%\\d]+");

    /** User-Agent 中的控制字符（换行等，防止伪造 UA 污染日志与数据库） */
    private static final Pattern UA_CONTROL_CHARS = Pattern.compile("\\p{Cntrl}");

    private IpUtils() {
    }

    /**
     * 获取客户端 IP：优先 X-Forwarded-For 首个地址，其次 X-Real-IP，最后直连地址
     * <p>
     * 只接受合法 IP 字面量：代理头可能被伪造为域名或任意字符串，
     * 这类值直接跳过（后续归属地查询若把非字面量交给 DNS 解析会产生额外外联）。
     *
     * @return 归一化后的 IP，取不到时返回 null
     */
    public static String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null) {
            // 多级代理时第一个为最初客户端地址
            for (String part : xff.split(",")) {
                String ip = normalizeIp(part);
                if (ip != null) {
                    return ip;
                }
            }
        }
        String realIp = normalizeIp(request.getHeader("X-Real-IP"));
        if (realIp != null) {
            return realIp;
        }
        return normalizeIp(request.getRemoteAddr());
    }

    /**
     * 校验并归一化 IP：非法值返回 null；IPv4 映射的 IPv6（::ffff:1.2.3.4）还原为点分十进制
     */
    public static String normalizeIp(String raw) {
        if (raw == null || raw.isBlank() || "unknown".equalsIgnoreCase(raw)) {
            return null;
        }
        String ip = raw.trim();
        // 去掉 IPv6 可能的方括号（如 [::1]:8080 形式）
        if (ip.length() > 2 && ip.charAt(0) == '[') {
            int end = ip.indexOf(']');
            if (end < 0) {
                return null;
            }
            ip = ip.substring(1, end);
        }
        if (IPV4_PATTERN.matcher(ip).matches()) {
            return ip;
        }
        if (isValidIpv6(ip)) {
            // 内嵌 IPv4 尾段：::ffff:1.2.3.4 / 0:0:0:0:0:ffff:1.2.3.4 统一按 IPv4 处理
            int lastColon = ip.lastIndexOf(':');
            String tail = ip.substring(lastColon + 1);
            if (tail.indexOf('.') > 0) {
                return IPV4_PATTERN.matcher(tail).matches() ? tail : null;
            }
            return ip.toLowerCase();
        }
        return null;
    }

    /**
     * 清洗 User-Agent：控制字符（换行等）替换为空格并截断到 {@link #USER_AGENT_MAX_LENGTH}
     * （截断保证超长 UA 不会导致落库失败，去控制字符避免伪造头污染日志）
     */
    public static String sanitizeUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return null;
        }
        String cleaned = UA_CONTROL_CHARS.matcher(userAgent).replaceAll(" ").trim();
        if (cleaned.isEmpty()) {
            return null;
        }
        return cleaned.length() > USER_AGENT_MAX_LENGTH
                ? cleaned.substring(0, USER_AGENT_MAX_LENGTH) : cleaned;
    }

    /**
     * IPv6 字面量宽松校验：字符集合法 + 冒号分组数量合法 + 最多一个 "::" 缩写
     */
    private static boolean isValidIpv6(String ip) {
        if (ip.indexOf(':') < 0 || ip.length() > 64 || !IPV6_CHARS_PATTERN.matcher(ip).matches()) {
            return false;
        }
        String body = ip;
        int zone = body.indexOf('%');
        if (zone >= 0) {
            if (zone == body.length() - 1 || !body.substring(zone + 1).chars().allMatch(Character::isDigit)) {
                return false;
            }
            body = body.substring(0, zone);
        }
        int doubleColon = body.indexOf("::");
        if (doubleColon >= 0 && body.indexOf("::", doubleColon + 1) >= 0) {
            return false;
        }
        String[] groups = body.split(":", -1);
        if (groups.length > 9) {
            return false;
        }
        for (String group : groups) {
            if (group.isEmpty()) {
                continue;
            }
            if (IPV4_PATTERN.matcher(group).matches()) {
                continue;
            }
            if (group.length() > 4 || !group.chars().allMatch(c -> Character.digit(c, 16) >= 0)) {
                return false;
            }
        }
        return true;
    }
}
