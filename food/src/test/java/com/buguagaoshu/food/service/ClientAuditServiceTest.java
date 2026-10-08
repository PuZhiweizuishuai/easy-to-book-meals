package com.buguagaoshu.food.service;

import com.buguagaoshu.food.data.OrderData;
import com.buguagaoshu.food.utils.IpUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;

/**
 * IP 审计信息解析测试（依赖 food/ip 下的 ip2region 离线库，纯本地查询无外网依赖）
 */
class ClientAuditServiceTest {

    private static final String XDB_DIR = "ip";

    private final ClientAuditService service =
            new ClientAuditService(true, XDB_DIR, "ip2region_v4.xdb", "ip2region_v6.xdb");

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void resolveLocationShouldReturnRegionForPublicIp() {
        String location = service.resolveLocation("223.5.5.5");
        assertNotNull(location, "公网 IP 应能解析出位置信息");
        assertTrue(location.contains("中国"), "实际解析结果: " + location);
        assertFalse(location.contains("0|"), "未知占位段应被丢弃: " + location);
    }

    @Test
    void resolveLocationShouldReturnNullWhenDisabledOrUnresolvable() {
        ClientAuditService disabled =
                new ClientAuditService(false, XDB_DIR, "ip2region_v4.xdb", "ip2region_v6.xdb");
        assertNull(disabled.resolveLocation("223.5.5.5"), "关闭开关后位置字段应为 null");
        assertNull(service.resolveLocation(null));

        // 离线库加载失败只降级，不抛异常
        ClientAuditService broken = new ClientAuditService(true, "not-exist-dir", "a.xdb", "b.xdb");
        assertNull(broken.resolveLocation("223.5.5.5"));
    }

    @Test
    void fillShouldWriteIpUaAndLocationFromCurrentRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "223.5.5.5, 10.0.0.1");
        request.addHeader("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)\nFake");
        request.setRemoteAddr("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        OrderData order = new OrderData();
        service.fill(order);

        assertEquals("223.5.5.5", order.getClientIp());
        assertEquals("Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) Fake", order.getClientUa());
        assertNotNull(order.getClientIpLocation());
    }

    @Test
    void fillShouldLeaveFieldsNullOutsideRequestThread() {
        OrderData order = new OrderData();
        service.fill(order);
        assertNull(order.getClientIp());
        assertNull(order.getClientUa());
        assertNull(order.getClientIpLocation());
    }

    @Test
    void getClientIpShouldSkipForgedNonIpHeaderValue() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        // 伪造的 XFF 为域名/任意串：不接受，避免触发 DNS 解析与脏数据入库
        request.addHeader("X-Forwarded-For", "evil.example.com");
        request.addHeader("X-Real-IP", "not-an-ip");
        request.setRemoteAddr("10.11.12.13");
        assertEquals("10.11.12.13", IpUtils.getClientIp(request));
    }

    @Test
    void normalizeIpShouldValidateAndUnmapIpv4MappedIpv6() {
        assertEquals("1.2.3.4", IpUtils.normalizeIp(" 1.2.3.4 "));
        assertEquals("1.2.3.4", IpUtils.normalizeIp("::ffff:1.2.3.4"));
        assertEquals("::1", IpUtils.normalizeIp("::1"));
        assertEquals("2001:db8::1", IpUtils.normalizeIp("2001:DB8::1"));
        assertEquals("::1", IpUtils.normalizeIp("[::1]:8080"));
        assertNull(IpUtils.normalizeIp("256.1.1.1"));
        assertNull(IpUtils.normalizeIp("example.com"));
        assertNull(IpUtils.normalizeIp("1.2.3.4; DROP TABLE"));
        assertNull(IpUtils.normalizeIp("unknown"));
        assertNull(IpUtils.normalizeIp(null));
    }
}
