package com.buguagaoshu.food.service;

import cn.dev33.satoken.stp.StpUtil;
import com.buguagaoshu.food.data.BaseFood;
import com.buguagaoshu.food.data.DiningTable;
import com.buguagaoshu.food.data.OrderData;
import com.buguagaoshu.food.data.OrderItem;
import com.buguagaoshu.food.data.ProductSku;
import com.buguagaoshu.food.data.Store;
import com.buguagaoshu.food.data.UserData;
import com.buguagaoshu.food.dto.AddOrderItemsDto;
import com.buguagaoshu.food.dto.CreateOrderDto;
import com.buguagaoshu.food.dto.StaffCreateOrderDto;
import com.buguagaoshu.food.enums.OrderStatus;
import com.buguagaoshu.food.enums.OrderType;
import com.buguagaoshu.food.enums.StoreStatus;
import com.buguagaoshu.food.enums.TableStatus;
import com.buguagaoshu.food.exception.BusinessException;
import com.buguagaoshu.food.repository.BaseFoodRepository;
import com.buguagaoshu.food.repository.OrderDataRepository;
import com.buguagaoshu.food.repository.OrderItemRepository;
import com.buguagaoshu.food.repository.ProductSkuRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 订单服务
 * <p>
 * 核心流程：
 * 1. 顾客下单：校验门店/餐桌/SKU → 原子扣减库存 → 生成价格快照 → 绑定餐桌
 * 2. 店家处理：接单 → 制作 → 出餐 → 完成（状态机校验）
 * 3. 取消：顾客仅能取消待接单订单；店家可拒单（恢复库存）
 * 4. 超时未接单订单定时自动取消（恢复库存、释放餐桌）
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-10-02
 */
@Service
public class OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    /**
     * 待接单订单超时时间：30 分钟（毫秒）
     */
    private static final long PENDING_TIMEOUT_MS = 30 * 60 * 1000L;

    private static final DateTimeFormatter ORDER_NO_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final OrderDataRepository orderDataRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductSkuRepository productSkuRepository;
    private final BaseFoodRepository baseFoodRepository;
    private final DiningTableService diningTableService;
    private final StoreService storeService;
    private final UserDataService userDataService;
    private final MembershipService membershipService;

    public OrderService(OrderDataRepository orderDataRepository,
                        OrderItemRepository orderItemRepository,
                        ProductSkuRepository productSkuRepository,
                        BaseFoodRepository baseFoodRepository,
                        DiningTableService diningTableService,
                        StoreService storeService,
                        UserDataService userDataService,
                        MembershipService membershipService) {
        this.orderDataRepository = orderDataRepository;
        this.orderItemRepository = orderItemRepository;
        this.productSkuRepository = productSkuRepository;
        this.baseFoodRepository = baseFoodRepository;
        this.diningTableService = diningTableService;
        this.storeService = storeService;
        this.userDataService = userDataService;
        this.membershipService = membershipService;
    }

    /**
     * 顾客创建订单（需登录）
     * 事务保证：任一环节失败（如库存不足）整体回滚
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderData createOrder(CreateOrderDto dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        UserData user = userDataService.findById(userId).orElse(null);
        String nickname = user != null ? user.getNickname() : null;
        return doCreateOrder(userId, dto, nickname);
    }

    /**
     * 餐桌二维码免登录下单
     * <p>
     * 扫码顾客无需注册/登录：系统创建匿名用户承载订单，订单类型固定为堂食，
     * 通过二维码定位餐桌并在下单后绑定（餐桌自动置为「已占用」）。
     *
     * @param dto 下单数据，qrCodeKey 必填
     * @return 已创建的订单
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderData createGuestOrder(CreateOrderDto dto) {
        // 扫码免登录必须携带二维码标识以定位餐桌
        if (dto.getQrCodeKey() == null || dto.getQrCodeKey().isBlank()) {
            throw new BusinessException(400, "扫码点餐需要餐桌二维码标识");
        }
        DiningTable table = diningTableService.getTableByQrCode(dto.getQrCodeKey());
        String nickname = "扫码顾客-" + (table.getTableNumber() != null ? table.getTableNumber() : "匿名");
        UserData guest = userDataService.createGuestUser(nickname);
        // 堂食订单强制覆盖类型与餐桌绑定
        dto.setOrderType(OrderType.DINE_IN.getCode());
        OrderData order = doCreateOrder(guest.getId(), dto, nickname);
        logger.info("扫码免登录下单成功: orderNo={}, guestUserId={}, tableId={}",
                order.getOrderNo(), guest.getId(), order.getTableId());
        return order;
    }

    /**
     * 店主/服务员协助下单（需登录 + order:manage 权限由控制器校验）
     * <p>
     * 店员代客下单：系统为顾客创建匿名用户承载订单，
     * 操作人（店员）记录在日志中用于审计。线下餐桌订单需指定餐桌。
     *
     * @param dto       店员下单数据（含顾客昵称/电话）
     * @param operatorId 操作店员ID（当前登录用户）
     * @return 已创建的订单
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderData createStaffOrder(StaffCreateOrderDto dto, Long operatorId) {
        // 确定顾客昵称
        String customerName = (dto.getCustomerName() != null && !dto.getCustomerName().isBlank())
                ? dto.getCustomerName() : "到店顾客";
        // 堂食订单必须有餐桌
        OrderType type = OrderType.fromCode(dto.getOrderType());
        if (type.isDineIn() && dto.getTableId() == null
                && (dto.getQrCodeKey() == null || dto.getQrCodeKey().isBlank())) {
            throw new BusinessException(400, "线下餐桌订单需要选择餐桌");
        }
        // 非堂食订单不应绑定餐桌（外送/线上自取）
        if (!type.isDineIn()) {
            dto.setTableId(null);
            dto.setQrCodeKey(null);
        }
        UserData guest = userDataService.createGuestUser(customerName);
        OrderData order = doCreateOrder(guest.getId(), dto, customerName);
        logger.info("店员协助下单成功: orderNo={}, operatorId={}, customerName={}, orderType={}",
                order.getOrderNo(), operatorId, customerName, type.getName());
        return order;
    }

    /**
     * 订单加菜（未结算前追加菜品）
     * <p>
     * 只要订单未结算（即非「已完成」）且未取消，均可加菜：
     * 待接单 / 已接单 / 制作中 / 已出餐 均允许追加，方便顾客随时补单。
     * 已扣减库存、追加明细、重算订单金额。
     *
     * @param orderId 订单ID
     * @param dto     加菜明细
     * @return 更新后的订单详情
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderData addItemsToOrder(Long orderId, AddOrderItemsDto dto) {
        OrderData order = orderDataRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(404, "订单不存在"));
        OrderStatus current = OrderStatus.fromCode(order.getStatus());
        if (current == OrderStatus.COMPLETED || current == OrderStatus.CANCELLED) {
            throw new BusinessException(400, "订单当前状态为「" + current.getName() + "」，无法加菜");
        }

        // 加载并校验 SKU
        List<Long> skuIds = dto.getItems().stream().map(CreateOrderDto.Item::getSkuId).toList();
        Map<Long, ProductSku> skuMap = new HashMap<>();
        for (ProductSku sku : productSkuRepository.findAllById(skuIds)) {
            skuMap.put(sku.getId(), sku);
        }
        Map<Long, BaseFood> dishMap = loadAndCheckDishes(order.getStoreId(), dto.getItems(), skuMap);

        // 扣减库存
        for (CreateOrderDto.Item item : dto.getItems()) {
            ProductSku sku = skuMap.get(item.getSkuId());
            int affected = productSkuRepository.deductStock(item.getSkuId(), item.getQuantity().longValue());
            if (affected == 0) {
                throw new BusinessException(400, "「" + dishMap.get(sku.getDishId()).getName() + "」库存不足");
            }
        }

        // 追加明细（价格快照）并重算订单金额
        List<OrderItem> newItems = buildOrderItems(order.getId(), dto.getItems(), skuMap, dishMap, true);
        orderItemRepository.saveAll(newItems);

        recalcOrderAmount(order);
        orderDataRepository.save(order);

        logger.info("订单加菜成功: orderNo={}, addedItems={}", order.getOrderNo(), newItems.size());
        return getOrderDetail(order.getId());
    }

    /**
     * 顾客加菜（校验订单归属本人）
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderData addItemsToMyOrder(Long orderId, AddOrderItemsDto dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        OrderData order = orderDataRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(404, "订单不存在"));
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(403, "只能为自己的订单加菜");
        }
        return addItemsToOrder(orderId, dto);
    }

    /**
     * 扫码免登录加菜（校验餐桌二维码归属）
     * <p>
     * 顾客扫码刷新页面后继续为本桌订单加菜：通过二维码定位餐桌，
     * 校验订单绑定的餐桌与二维码匹配后放行，避免未登录身份下的越权。
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderData addItemsToGuestOrder(Long orderId, String qrCodeKey, AddOrderItemsDto dto) {
        OrderData order = orderDataRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(404, "订单不存在"));
        if (order.getTableId() == null) {
            throw new BusinessException(400, "该订单未绑定餐桌，无法通过扫码加菜");
        }
        DiningTable table = diningTableService.getTable(order.getTableId(), order.getStoreId());
        if (table.getQrCodeKey() == null || !table.getQrCodeKey().equals(qrCodeKey)) {
            throw new BusinessException(403, "餐桌二维码不匹配，无权操作该订单");
        }
        return addItemsToOrder(orderId, dto);
    }

    /**
     * 菜品级出餐：标记单个订单明细为已出餐
     * <p>
     * 当该订单所有正常明细均 served=1 时，自动把订单状态推进到「已出餐(READY)」。
     * 仅店家可操作（控制器校验 order:manage 权限）。
     *
     * @param itemId 订单明细ID
     * @param storeId 门店ID（权限校验）
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderData serveOrderItem(Long itemId, Long storeId) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(404, "菜品明细不存在"));
        if (item.getStatus() != null && item.getStatus() == OrderItem.STATUS_CANCELLED) {
            throw new BusinessException(400, "该菜品已退菜，无法出餐");
        }
        if (item.getServed() != null && item.getServed() == 1) {
            throw new BusinessException(400, "该菜品已出餐");
        }
        OrderData order = orderDataRepository.findById(item.getOrderId())
                .orElseThrow(() -> new BusinessException(404, "订单不存在"));
        if (!order.getStoreId().equals(storeId)) {
            throw new BusinessException(403, "订单不属于该门店");
        }
        OrderStatus current = OrderStatus.fromCode(order.getStatus());
        if (current == OrderStatus.COMPLETED || current == OrderStatus.CANCELLED) {
            throw new BusinessException(400, "订单当前状态为「" + current.getName() + "」，无法出餐");
        }

        item.setServed(1);
        orderItemRepository.save(item);
        logger.info("菜品出餐: orderNo={}, itemName={}, quantity={}",
                order.getOrderNo(), item.getDishName(), item.getQuantity());

        // 检查是否所有正常明细都已出餐，是则自动推进订单到 READY
        List<OrderItem> allItems = orderItemRepository.findByOrderId(order.getId());
        boolean allServed = allItems.stream()
                .allMatch(i -> i.getStatus() == OrderItem.STATUS_CANCELLED || i.getServed() == 1);
        if (allServed && current != OrderStatus.READY) {
            // 状态机允许 CONFIRMED/COOKING → READY，若当前是 PENDING 则不允许直接跳 READY
            if (current != null && current.canTransitionTo(OrderStatus.READY)) {
                order.setStatus(OrderStatus.READY.getCode());
                orderDataRepository.save(order);
                logger.info("订单全部菜品已出餐，自动推进到已出餐状态: orderNo={}", order.getOrderNo());
            }
        }
        order.setItems(allItems);
        return order;
    }

    /**
     * 退菜：移除订单内未出餐的菜品明细
     * <p>
     * 仅允许退「未出餐(served=0)」且「正常状态」的明细。
     * 退菜后恢复库存、标记明细为已取消、重算订单金额。
     * 仅店家可操作（控制器校验 order:manage 权限）。
     *
     * @param itemId 订单明细ID
     * @param storeId 门店ID（权限校验）
     * @param reason  退菜原因（可选）
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderData removeOrderItem(Long itemId, Long storeId, String reason) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(404, "菜品明细不存在"));
        if (item.getStatus() != null && item.getStatus() == OrderItem.STATUS_CANCELLED) {
            throw new BusinessException(400, "该菜品已退菜");
        }
        if (item.getServed() != null && item.getServed() == 1) {
            throw new BusinessException(400, "已出餐的菜品无法退菜");
        }
        OrderData order = orderDataRepository.findById(item.getOrderId())
                .orElseThrow(() -> new BusinessException(404, "订单不存在"));
        if (!order.getStoreId().equals(storeId)) {
            throw new BusinessException(403, "订单不属于该门店");
        }
        OrderStatus current = OrderStatus.fromCode(order.getStatus());
        if (current == OrderStatus.COMPLETED || current == OrderStatus.CANCELLED) {
            throw new BusinessException(400, "订单当前状态为「" + current.getName() + "」，无法退菜");
        }

        // 恢复库存
        productSkuRepository.restoreStock(item.getSkuId(), item.getQuantity().longValue());
        // 标记明细为已取消
        item.setStatus(OrderItem.STATUS_CANCELLED);
        orderItemRepository.save(item);
        // 重算订单金额
        recalcOrderAmount(order);
        orderDataRepository.save(order);
        logger.info("退菜成功: orderNo={}, itemName={}, quantity={}, reason={}",
                order.getOrderNo(), item.getDishName(), item.getQuantity(),
                reason == null ? "无" : reason);

        order.setItems(orderItemRepository.findByOrderId(order.getId()));
        return order;
    }

    /**
     * 后台调整菜品数量/重量（支持称重菜按实际重量重新计价）
     * <p>
     * 仅订单未结算（非「已完成/已取消」）时可调整。调整后按 dealPrice × 新数量重算小计，
     * 并联动重算订单总额；数量差的整数部分同步回补/追加库存（称重菜库存通常为不限，无影响）。
     *
     * @param itemId      订单明细ID
     * @param storeId     门店ID（权限校验）
     * @param newQuantity 新数量/重量（必须大于 0，最多 3 位小数）
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderData adjustOrderItemQuantity(Long itemId, Long storeId, java.math.BigDecimal newQuantity) {
        if (newQuantity == null || newQuantity.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "数量必须大于 0");
        }
        if (newQuantity.scale() > 3) {
            throw new BusinessException(400, "数量最多支持 3 位小数");
        }
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(404, "菜品明细不存在"));
        if (item.getStatus() != null && item.getStatus() == OrderItem.STATUS_CANCELLED) {
            throw new BusinessException(400, "已退菜的菜品无法调整数量");
        }
        OrderData order = orderDataRepository.findById(item.getOrderId())
                .orElseThrow(() -> new BusinessException(404, "订单不存在"));
        if (!order.getStoreId().equals(storeId)) {
            throw new BusinessException(403, "订单不属于该门店");
        }
        OrderStatus current = OrderStatus.fromCode(order.getStatus());
        if (current == OrderStatus.COMPLETED || current == OrderStatus.CANCELLED) {
            throw new BusinessException(400, "订单当前状态为「" + current.getName() + "」，无法调整数量");
        }

        java.math.BigDecimal oldQuantity = item.getQuantity() == null
                ? java.math.BigDecimal.ZERO : item.getQuantity();

        // 库存联动：仅处理数量差的整数部分（称重菜的小数增量不影响计件库存）
        long delta = newQuantity.subtract(oldQuantity).setScale(0, java.math.RoundingMode.DOWN).longValue();
        if (delta > 0) {
            int affected = productSkuRepository.deductStock(item.getSkuId(), delta);
            if (affected == 0) {
                throw new BusinessException(400, "库存不足，无法将数量增加到 " + formatQuantity(newQuantity));
            }
        } else if (delta < 0) {
            productSkuRepository.restoreStock(item.getSkuId(), -delta);
        }

        java.math.BigDecimal qty = newQuantity.stripTrailingZeros();
        item.setQuantity(qty);
        java.math.BigDecimal deal = item.getDealPrice() == null
                ? java.math.BigDecimal.ZERO : item.getDealPrice();
        item.setTotalPrice(deal.multiply(qty).setScale(2, java.math.RoundingMode.HALF_UP));
        orderItemRepository.save(item);

        recalcOrderAmount(order);
        orderDataRepository.save(order);
        logger.info("调整菜品数量: orderNo={}, itemName={}, {} -> {}, itemTotal={}",
                order.getOrderNo(), item.getDishName(), formatQuantity(oldQuantity),
                formatQuantity(qty), item.getTotalPrice());

        order.setItems(orderItemRepository.findByOrderId(order.getId()));
        return order;
    }

    /**
     * 数量/重量的展示格式化：去除多余的小数尾零（如 1.600 -> 1.6，2 -> 2）
     */
    private static String formatQuantity(java.math.BigDecimal qty) {
        return qty == null ? "0" : qty.stripTrailingZeros().toPlainString();
    }

    /**
     * 扫码免登录查询本桌当前未结算订单（刷新页面后展示已下单菜品）
     * <p>
     * 通过餐桌二维码定位餐桌 → 取其 currentOrderId → 返回订单详情（含明细）。
     * 订单已结算或取消时返回 null（无当前订单可展示）。
     */
    public OrderData getGuestCurrentOrder(String qrCodeKey) {
        if (qrCodeKey == null || qrCodeKey.isBlank()) {
            return null;
        }
        DiningTable table;
        try {
            table = diningTableService.getTableByQrCode(qrCodeKey);
        } catch (BusinessException e) {
            return null;
        }
        if (table.getCurrentOrderId() == null) {
            return null;
        }
        OrderData order = orderDataRepository.findById(table.getCurrentOrderId()).orElse(null);
        if (order == null) {
            return null;
        }
        OrderStatus status = OrderStatus.fromCode(order.getStatus());
        // 仅展示未结算订单（待接单/已接单/制作中/已出餐）
        if (status == OrderStatus.COMPLETED || status == OrderStatus.CANCELLED) {
            return null;
        }
        order.setItems(orderItemRepository.findByOrderId(order.getId()));
        return order;
    }

    /**
     * 店家处理订单（接单/制作/出餐/完成/拒单），统一走状态机校验
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderData transitionOrder(Long orderId, Long storeId, OrderStatus target, String reason) {
        OrderData order = getOrderInStore(orderId, storeId);
        OrderStatus current = OrderStatus.fromCode(order.getStatus());

        if (current == null) {
            throw new BusinessException(500, "订单状态数据异常");
        }
        if (!current.canTransitionTo(target)) {
            throw new BusinessException(400,
                    "订单当前状态为「" + current.getName() + "」，不能流转为「" + target.getName() + "」");
        }

        // 拒单/取消：恢复库存、释放餐桌
        if (target == OrderStatus.CANCELLED) {
            cancelOrderInternal(order, reason);
            return getOrderDetail(order.getId());
        }

        order.setStatus(target.getCode());
        if (target == OrderStatus.CONFIRMED) {
            order.setConfirmTime(System.currentTimeMillis());
        }
        if (target == OrderStatus.COMPLETED) {
            order.setCompleteTime(System.currentTimeMillis());
            onOrderCompleted(order);
        }
        orderDataRepository.save(order);
        return getOrderDetail(order.getId());
    }

    /**
     * 结算完成订单（店员可录入顾客手机号，关联会员并累积积分）
     * <p>
     * 流程：状态流转到已完成 → 若提供手机号则关联会员/累积积分
     * <p>
     * 会员关联策略：
     * - 手机号对应已注册用户 → 把订单归属（userId/userNickname）切换到该会员
     * - 手机号无对应用户 → 回填到当前下单用户（匿名顾客升级为可识别会员）
     * - 手机号已被其他用户占用且非当前用户 → 仅累积积分，不改动订单归属
     * <p>
     * 积分规则：按应付金额 1 元 = 1 积分（向下取整，不足 1 元不计）
     *
     * @param orderId 订单ID
     * @param storeId 门店ID（权限校验）
     * @param phone 顾客手机号（可选，为空表示未录入）
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderData completeOrder(Long orderId, Long storeId, String phone) {
        // 0. 结算前校验：所有正常菜品必须已出餐
        OrderData checkOrder = orderDataRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(404, "订单不存在"));
        if (!checkOrder.getStoreId().equals(storeId)) {
            throw new BusinessException(403, "订单不属于该门店");
        }
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        List<OrderItem> unserved = items.stream()
                .filter(i -> i.getStatus() != null && i.getStatus() == OrderItem.STATUS_NORMAL
                        && (i.getServed() == null || i.getServed() == 0))
                .toList();
        if (!unserved.isEmpty()) {
            String names = unserved.stream()
                    .map(i -> i.getDishName() + "×" + formatQuantity(i.getQuantity()))
                    .collect(Collectors.joining("、"));
            throw new BusinessException(400, "以下菜品尚未出餐，无法结算：" + names);
        }

        // 1. 状态流转到已完成（内部校验状态机、onOrderCompleted 累加销量、释放餐桌）
        transitionOrder(orderId, storeId, OrderStatus.COMPLETED, null);

        // 2. 处理结算联系电话与会员积分
        String normalizedPhone = (phone == null) ? null : phone.trim();
        if (normalizedPhone != null && normalizedPhone.isEmpty()) {
            normalizedPhone = null;
        }

        OrderData order = orderDataRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(404, "订单不存在"));

        if (normalizedPhone != null) {
            order.setContactPhone(normalizedPhone);

            // 关联会员：查找手机号对应的已注册用户
            UserData existing = userDataService.findByPhone(normalizedPhone).orElse(null);
            if (existing != null && !existing.getId().equals(order.getUserId())) {
                // 订单归属切换到该会员
                order.setUserId(existing.getId());
                order.setUserNickname(existing.getNickname());
            } else if (existing == null) {
                // 无对应用户：尝试把手机号回填到当前下单用户（匿名顾客升级）
                userDataService.bindPhone(order.getUserId(), normalizedPhone);
                // 若回填失败（手机号被占用），订单归属不变，仅累积积分
            }

            // 累积会员积分：1 元 = 1 积分（向下取整）
            long points = order.getPayAmount() == null ? 0L
                    : order.getPayAmount().setScale(0, java.math.RoundingMode.DOWN).longValue();
            if (points > 0) {
                membershipService.addPointsForOrder(storeId, normalizedPhone, points,
                        order.getId(), order.getOrderNo());
                logger.info("会员积分累积: orderNo={}, phone={}, points={}",
                        order.getOrderNo(), normalizedPhone, points);
            }
        }

        orderDataRepository.save(order);
        return getOrderDetail(order.getId());
    }

    /**
     * 顾客取消订单（仅本人 + 待接单状态）
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderData cancelMyOrder(Long orderId) {
        Long userId = StpUtil.getLoginIdAsLong();
        OrderData order = orderDataRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(404, "订单不存在"));

        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(403, "只能取消自己的订单");
        }
        OrderStatus current = OrderStatus.fromCode(order.getStatus());
        if (current == null || !current.canTransitionTo(OrderStatus.CANCELLED)) {
            throw new BusinessException(400, "订单已被接单，无法取消，请联系店家");
        }

        cancelOrderInternal(order, "顾客取消");
        return getOrderDetail(order.getId());
    }

    /**
     * 取消订单内部逻辑：标记状态、恢复库存、释放餐桌、标记明细
     */
    private void cancelOrderInternal(OrderData order, String reason) {
        order.setStatus(OrderStatus.CANCELLED.getCode());
        order.setCancelReason(reason);
        orderDataRepository.save(order);

        // 恢复库存
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        for (OrderItem item : items) {
            if (item.getStatus() == OrderItem.STATUS_NORMAL) {
                productSkuRepository.restoreStock(item.getSkuId(), item.getQuantity().longValue());
                item.setStatus(OrderItem.STATUS_CANCELLED);
            }
        }
        orderItemRepository.saveAll(items);

        // 释放餐桌（取消订单时餐桌未真正使用，回到空闲）
        releaseTable(order, false);
    }

    /**
     * 订单完成后的统计处理：累加菜品/SKU 销量
     */
    private void onOrderCompleted(OrderData order) {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        for (OrderItem item : items) {
            if (item.getStatus() == OrderItem.STATUS_NORMAL) {
                productSkuRepository.addSales(item.getSkuId(), item.getQuantity().longValue());
                baseFoodRepository.addSales(item.getDishId(), item.getQuantity().longValue());
            }
        }
        // 完成就餐后餐桌转入清理状态
        releaseTable(order, true);
    }

    /**
     * 释放订单绑定的餐桌（仅当餐桌当前绑定的仍是该订单）
     *
     * @param completed true=订单完成（客人离席→清理中）；false=订单取消（未就餐→空闲）
     */
    private void releaseTable(OrderData order, boolean completed) {
        if (order.getTableId() == null) {
            return;
        }
        try {
            DiningTable table = diningTableService.getTable(order.getTableId(), order.getStoreId());
            if (order.getId().equals(table.getCurrentOrderId())) {
                int targetStatus = completed
                        ? TableStatus.CLEANING.getCode()
                        : TableStatus.FREE.getCode();
                diningTableService.clearCurrentOrder(table.getId(), table.getStorefrontId(), targetStatus);
            }
        } catch (BusinessException e) {
            // 餐桌可能已被删除，忽略
            logger.warn("释放餐桌失败: tableId={}, {}", order.getTableId(), e.getMessage());
        }
    }

    /**
     * 店主端分页查询门店订单（附带明细，可按状态/订单类型过滤）
     */
    public Page<OrderData> listStoreOrders(Long storeId, Integer status, Integer orderType,
                                           String key, int page, int size) {
        String likeKey = (key == null || key.isBlank()) ? null : key;
        Page<OrderData> orders = orderDataRepository.findStoreOrders(
                storeId, status, orderType, likeKey,
                PageRequest.of(page - 1, size));
        fillOrderItems(orders.getContent());
        return orders;
    }

    /**
     * 顾客端分页查询自己的订单（附带明细）
     */
    public Page<OrderData> listMyOrders(Integer status, int page, int size) {
        Long userId = StpUtil.getLoginIdAsLong();
        Page<OrderData> orders = orderDataRepository.findUserOrders(
                userId, status, PageRequest.of(page - 1, size));
        fillOrderItems(orders.getContent());
        return orders;
    }

    /**
     * 查询订单详情
     * 顾客仅能查看自己的订单；店家需拥有该门店订单查看权限（由控制器校验）
     */
    public OrderData getOrderDetail(Long orderId) {
        OrderData order = orderDataRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(404, "订单不存在"));
        order.setItems(orderItemRepository.findByOrderId(order.getId()));
        return order;
    }

    /**
     * 按门店+手机号分页查询订单（会员消费记录）
     */
    public org.springframework.data.domain.Page<OrderData> findStoreOrdersByPhone(
            Long storeId, String phone, org.springframework.data.domain.Pageable pageable) {
        return orderDataRepository.findStoreOrdersByPhone(storeId, phone, pageable);
    }

    /**
     * 统计门店某手机号的消费记录数量（会员用餐次数）
     */
    public long countStoreOrdersByPhone(Long storeId, String phone) {
        return orderDataRepository.countByStoreIdAndContactPhone(storeId, phone);
    }

    /**
     * 查询订单详情（顾客端，校验归属）
     */
    public OrderData getMyOrderDetail(Long orderId) {
        Long userId = StpUtil.getLoginIdAsLong();
        OrderData order = getOrderDetail(orderId);
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权查看该订单");
        }
        return order;
    }

    /**
     * 门店经营统计（数据看板）
     */
    public Map<String, Object> storeStats(Long storeId) {
        long todayStart = LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant().toEpochMilli();

        Map<String, Object> stats = new HashMap<>();
        stats.put("todayOrderCount", orderDataRepository.countByStoreIdAndCreateTimeGreaterThanEqual(storeId, todayStart));
        stats.put("todayRevenue", orderDataRepository.sumStoreRevenue(storeId, todayStart));
        stats.put("pendingCount", orderDataRepository.countByStoreIdAndStatus(storeId, OrderStatus.PENDING.getCode()));
        stats.put("cookingCount", orderDataRepository.countByStoreIdAndStatus(storeId, OrderStatus.CONFIRMED.getCode())
                + orderDataRepository.countByStoreIdAndStatus(storeId, OrderStatus.COOKING.getCode()));
        return stats;
    }

    /**
     * 定时任务：每分钟扫描，自动取消超时（30分钟）未接单的订单
     * 恢复库存并释放餐桌（@Modifying 恢复库存需要事务）
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional(rollbackFor = Exception.class)
    public void cancelTimeoutPendingOrders() {
        long deadline = System.currentTimeMillis() - PENDING_TIMEOUT_MS;
        List<OrderData> timeoutOrders = orderDataRepository
                .findByStatusAndCreateTimeBefore(OrderStatus.PENDING.getCode(), deadline);
        for (OrderData order : timeoutOrders) {
            try {
                cancelOrderInternal(order, "超时未接单，系统自动取消");
                logger.info("超时订单自动取消: orderNo={}", order.getOrderNo());
            } catch (Exception e) {
                logger.error("超时订单自动取消失败: orderNo={}", order.getOrderNo(), e);
            }
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 订单创建核心逻辑（顾客/免登录/店员协助下单共用）
     * <p>
     * 流程：校验门店 → 解析餐桌 → 校验SKU → 扣库存 → 构建订单与明细 → 绑定餐桌
     * 订单类型：dto.orderType 优先，否则按是否绑定餐桌自动判定（堂食/线上）
     *
     * @param userId    订单归属用户ID（顾客本人或匿名顾客）
     * @param dto       下单数据
     * @param nickname  下单人昵称快照
     */
    private OrderData doCreateOrder(Long userId, CreateOrderDto dto, String nickname) {
        // 1. 校验门店状态（仅营业中可下单）
        Store store = storeService.getStore(dto.getStoreId());
        if (!store.getStatus().equals(StoreStatus.OPEN.getCode())) {
            throw new BusinessException(400, "门店暂未营业，无法下单");
        }

        // 2. 解析餐桌（扫码点餐通过二维码定位；非堂食订单不应携带餐桌）
        OrderType orderType = OrderType.fromCode(dto.getOrderType());
        DiningTable table = null;
        if (orderType.isDineIn()) {
            table = resolveTable(dto);
        } else {
            // 外送/线上订单不绑定餐桌
            dto.setTableId(null);
            dto.setQrCodeKey(null);
        }

        // 3. 加载并校验 SKU（归属门店、启用、在售）
        List<Long> skuIds = dto.getItems().stream().map(CreateOrderDto.Item::getSkuId).toList();
        Map<Long, ProductSku> skuMap = new HashMap<>();
        for (ProductSku sku : productSkuRepository.findAllById(skuIds)) {
            skuMap.put(sku.getId(), sku);
        }
        Map<Long, BaseFood> dishMap = loadAndCheckDishes(dto.getStoreId(), dto.getItems(), skuMap);

        // 4. 原子扣减库存（并发安全，失败抛异常回滚）
        for (CreateOrderDto.Item item : dto.getItems()) {
            ProductSku sku = skuMap.get(item.getSkuId());
            int affected = productSkuRepository.deductStock(item.getSkuId(), item.getQuantity().longValue());
            if (affected == 0) {
                throw new BusinessException(400, "「" + dishMap.get(sku.getDishId()).getName() + "」库存不足");
            }
        }

        // 5. 构建订单与明细（价格快照）
        OrderData order = buildOrder(userId, dto, store, table, skuMap, dishMap, orderType, nickname);
        OrderData savedOrder = orderDataRepository.save(order);

        List<OrderItem> orderItems = buildOrderItems(savedOrder.getId(), dto.getItems(), skuMap, dishMap, false);
        orderItemRepository.saveAll(orderItems);
        savedOrder.setItems(orderItems);

        // 6. 绑定餐桌（堂食，餐桌自动置为「已占用」）
        if (table != null) {
            diningTableService.bindCurrentOrder(table.getId(), table.getStorefrontId(), savedOrder.getId());
        }

        logger.info("订单创建成功: orderNo={}, storeId={}, userId={}, orderType={}, payAmount={}",
                savedOrder.getOrderNo(), savedOrder.getStoreId(), userId,
                orderType.getName(), savedOrder.getPayAmount());
        return savedOrder;
    }

    /**
     * 根据全部正常明细重算订单金额（用于加菜后）
     */
    private void recalcOrderAmount(OrderData order) {
        List<OrderItem> allItems = orderItemRepository.findByOrderId(order.getId());
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        java.math.BigDecimal discount = java.math.BigDecimal.ZERO;
        java.math.BigDecimal pay = java.math.BigDecimal.ZERO;
        for (OrderItem item : allItems) {
            if (item.getStatus() != null && item.getStatus() == OrderItem.STATUS_NORMAL) {
                java.math.BigDecimal unit = item.getUnitPrice() == null
                        ? java.math.BigDecimal.ZERO : item.getUnitPrice();
                java.math.BigDecimal sub = unit.multiply(item.getQuantity());
                total = total.add(sub);
                if (item.getDealPrice() != null && unit.compareTo(item.getDealPrice()) > 0) {
                    discount = discount.add(
                            unit.subtract(item.getDealPrice())
                                    .multiply(item.getQuantity()));
                }
                pay = pay.add(item.getTotalPrice() == null
                        ? java.math.BigDecimal.ZERO : item.getTotalPrice());
            }
        }
        order.setTotalAmount(total.setScale(2, java.math.RoundingMode.HALF_UP));
        order.setDiscountAmount(discount.setScale(2, java.math.RoundingMode.HALF_UP));
        order.setPayAmount(pay.setScale(2, java.math.RoundingMode.HALF_UP));
    }

    /**
     * 解析下单餐桌：优先二维码标识，其次餐桌ID
     */
    private DiningTable resolveTable(CreateOrderDto dto) {
        if (dto.getQrCodeKey() != null && !dto.getQrCodeKey().isBlank()) {
            DiningTable table = diningTableService.getTableByQrCode(dto.getQrCodeKey());
            if (!table.getStorefrontId().equals(dto.getStoreId())) {
                throw new BusinessException(400, "餐桌二维码与门店不匹配");
            }
            checkTableOrderable(table);
            return table;
        }
        if (dto.getTableId() != null) {
            DiningTable table = diningTableService.getTable(dto.getTableId(), dto.getStoreId());
            checkTableOrderable(table);
            return table;
        }
        return null;
    }

    /**
     * 校验餐桌可用餐（未删除、未停用）
     */
    private void checkTableOrderable(DiningTable table) {
        if (table.getStatus() != null && table.getStatus() == TableStatus.DISABLED.getCode()) {
            throw new BusinessException(400, "餐桌已停用: " + table.getTableNumber());
        }
    }

    /**
     * 加载并校验订单中的菜品（归属当前门店、上架状态）
     */
    private Map<Long, BaseFood> loadAndCheckDishes(Long storeId, List<CreateOrderDto.Item> items,
                                                   Map<Long, ProductSku> skuMap) {
        List<Long> dishIds = items.stream()
                .map(i -> skuMap.get(i.getSkuId()))
                .filter(sku -> sku != null)
                .map(ProductSku::getDishId)
                .distinct()
                .toList();

        Map<Long, BaseFood> dishMap = new HashMap<>();
        for (BaseFood food : baseFoodRepository.findAllById(dishIds)) {
            dishMap.put(food.getId(), food);
        }

        for (CreateOrderDto.Item item : items) {
            ProductSku sku = skuMap.get(item.getSkuId());
            if (sku == null) {
                throw new BusinessException(404, "菜品规格不存在或已下架");
            }
            if (sku.getStatus() == null || sku.getStatus() != 1) {
                throw new BusinessException(400, "菜品规格已停用");
            }
            BaseFood food = dishMap.get(sku.getDishId());
            if (food == null) {
                throw new BusinessException(404, "菜品不存在");
            }
            if (!storeId.equals(food.getStorefrontId())) {
                throw new BusinessException(400, "菜品不属于该门店");
            }
            if (food.getStatus() == null || food.getStatus() != 1) {
                throw new BusinessException(400, "菜品已下架: " + food.getName());
            }
        }
        return dishMap;
    }

    /**
     * 构建订单主体（金额计算 + 订单号生成 + 订单类型）
     */
    private OrderData buildOrder(Long userId, CreateOrderDto dto, Store store,
                                 DiningTable table, Map<Long, ProductSku> skuMap,
                                 Map<Long, BaseFood> dishMap, OrderType orderType, String nickname) {
        OrderData order = new OrderData();
        order.setOrderNo(generateOrderNo(store.getId()));
        order.setStoreId(store.getId());
        order.setUserId(userId);
        order.setOrderType(orderType.getCode());
        // 下单人昵称快照（顾客本人或匿名顾客）
        order.setUserNickname(nickname);

        if (table != null) {
            order.setTableId(table.getId());
            order.setTableNumber(table.getTableNumber());
        }
        order.setStatus(OrderStatus.PENDING.getCode());
        order.setRemark(dto.getRemark());

        // 金额计算
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        java.math.BigDecimal discount = java.math.BigDecimal.ZERO;
        for (CreateOrderDto.Item item : dto.getItems()) {
            ProductSku sku = skuMap.get(item.getSkuId());
            java.math.BigDecimal unitPrice = sku.getUnitPrice() == null
                    ? java.math.BigDecimal.ZERO : sku.getUnitPrice();

            // 折扣判定：discount 有效且在优惠期内
            java.math.BigDecimal deal = unitPrice;
            if (isDiscountActive(sku)) {
                deal = unitPrice.multiply(java.math.BigDecimal.valueOf(sku.getDiscount()))
                        .setScale(2, java.math.RoundingMode.HALF_UP);
                java.math.BigDecimal diff = unitPrice.subtract(deal)
                        .multiply(java.math.BigDecimal.valueOf(item.getQuantity()));
                discount = discount.add(diff);
            }
            java.math.BigDecimal subtotal = deal.multiply(java.math.BigDecimal.valueOf(item.getQuantity()));
            total = total.add(unitPrice.multiply(java.math.BigDecimal.valueOf(item.getQuantity())));
            order.setPayAmount(order.getPayAmount().add(subtotal));
        }
        order.setTotalAmount(total.setScale(2, java.math.RoundingMode.HALF_UP));
        order.setDiscountAmount(discount.setScale(2, java.math.RoundingMode.HALF_UP));
        order.setPayAmount(order.getPayAmount().setScale(2, java.math.RoundingMode.HALF_UP));
        return order;
    }

    /**
     * 判断 SKU 折扣是否生效
     */
    private boolean isDiscountActive(ProductSku sku) {
        if (sku.getDiscount() == null || sku.getDiscount() <= 0 || sku.getDiscount() >= 1) {
            return false;
        }
        // discountEndTime 为 0 或 null 表示长期有效
        return sku.getDiscountEndTime() == null || sku.getDiscountEndTime() == 0
                || sku.getDiscountEndTime() >= System.currentTimeMillis();
    }

    /**
     * 构建订单明细（价格快照）
     *
     * @param addedLater 是否为加菜追加的明细（true=加菜，false=初始下单）
     */
    private List<OrderItem> buildOrderItems(Long orderId, List<CreateOrderDto.Item> items,
                                            Map<Long, ProductSku> skuMap, Map<Long, BaseFood> dishMap,
                                            boolean addedLater) {
        List<OrderItem> orderItems = new ArrayList<>();
        for (CreateOrderDto.Item item : items) {
            ProductSku sku = skuMap.get(item.getSkuId());
            BaseFood food = dishMap.get(sku.getDishId());

            OrderItem oi = new OrderItem();
            oi.setOrderId(orderId);
            oi.setDishId(sku.getDishId());
            oi.setSkuId(sku.getId());
            oi.setDishName(food.getName());
            oi.setTopImage(food.getTopImage());
            oi.setSpecNames(sku.getSpecNames());
            oi.setQuantity(java.math.BigDecimal.valueOf(item.getQuantity()));
            oi.setUnitPrice(sku.getUnitPrice());

            java.math.BigDecimal deal = sku.getUnitPrice();
            if (isDiscountActive(sku)) {
                deal = sku.getUnitPrice().multiply(java.math.BigDecimal.valueOf(sku.getDiscount()))
                        .setScale(2, java.math.RoundingMode.HALF_UP);
            }
            oi.setDealPrice(deal);
            oi.setTotalPrice(deal.multiply(java.math.BigDecimal.valueOf(item.getQuantity())));
            oi.setStatus(OrderItem.STATUS_NORMAL);
            oi.setServed(0);
            oi.setAddedLater(addedLater ? 1 : 0);
            orderItems.add(oi);
        }
        return orderItems;
    }

    /**
     * 生成订单号：日期 + 门店ID(4位) + 随机序列(6位)
     * 日期部分保证按天递增，随机序列避免并发冲突
     */
    private String generateOrderNo(Long storeId) {
        String date = LocalDate.now().format(ORDER_NO_DATE);
        for (int i = 0; i < 3; i++) {
            String orderNo = String.format("%s-%04d-%06d",
                    date, storeId % 10000, ThreadLocalRandom.current().nextInt(1000000));
            if (orderDataRepository.findByOrderNo(orderNo).isEmpty()) {
                return orderNo;
            }
        }
        // 极端情况：加入时间戳保证唯一
        return String.format("%s-%04d-%d", date, storeId % 10000, System.currentTimeMillis() % 10000000);
    }

    /**
     * 校验订单归属门店
     */
    private OrderData getOrderInStore(Long orderId, Long storeId) {
        OrderData order = orderDataRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(404, "订单不存在"));
        if (!order.getStoreId().equals(storeId)) {
            throw new BusinessException(403, "订单不属于该门店");
        }
        return order;
    }

    /**
     * 为订单列表批量填充明细（避免 N+1）
     */
    private void fillOrderItems(List<OrderData> orders) {
        if (orders == null || orders.isEmpty()) {
            return;
        }
        List<Long> orderIds = orders.stream().map(OrderData::getId).collect(Collectors.toList());
        Map<Long, List<OrderItem>> itemMap = orderItemRepository.findByOrderIdIn(orderIds).stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));
        orders.forEach(o -> o.setItems(itemMap.getOrDefault(o.getId(), new ArrayList<>())));
    }
}
