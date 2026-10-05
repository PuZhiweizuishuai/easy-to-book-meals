package com.buguagaoshu.food.service;

import cn.dev33.satoken.stp.StpUtil;
import com.buguagaoshu.food.data.BaseFood;
import com.buguagaoshu.food.data.DiningTable;
import com.buguagaoshu.food.data.Membership;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

    /**
     * 手机号格式（中国大陆 11 位）
     */
    private static final java.util.regex.Pattern PHONE_PATTERN =
            java.util.regex.Pattern.compile("^1[3-9]\\d{9}$");

    private final OrderDataRepository orderDataRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductSkuRepository productSkuRepository;
    private final BaseFoodRepository baseFoodRepository;
    private final DiningTableService diningTableService;
    private final StoreService storeService;
    private final UserDataService userDataService;
    private final MembershipService membershipService;
    private final CouponService couponService;
    /**
     * 逐订单独立事务模板（超时自动取消专用，REQUIRES_NEW）：
     * 单个订单取消失败只回滚该订单，不影响同批其他订单，也避免大事务长时间持锁
     */
    private final TransactionTemplate requiresNewTransaction;

    public OrderService(OrderDataRepository orderDataRepository,
                        OrderItemRepository orderItemRepository,
                        ProductSkuRepository productSkuRepository,
                        BaseFoodRepository baseFoodRepository,
                        DiningTableService diningTableService,
                        StoreService storeService,
                        UserDataService userDataService,
                        MembershipService membershipService,
                        CouponService couponService,
                        PlatformTransactionManager transactionManager) {
        this.orderDataRepository = orderDataRepository;
        this.orderItemRepository = orderItemRepository;
        this.productSkuRepository = productSkuRepository;
        this.baseFoodRepository = baseFoodRepository;
        this.diningTableService = diningTableService;
        this.storeService = storeService;
        this.userDataService = userDataService;
        this.membershipService = membershipService;
        this.couponService = couponService;
        this.requiresNewTransaction = new TransactionTemplate(transactionManager);
        this.requiresNewTransaction.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW);
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

        // 手机号（可选）：下单即关联会员信息，用于识别回头客并在结算后累积积分
        String phone = normalizePhone(dto.getContactPhone());
        dto.setContactPhone(phone);
        if (phone != null) {
            // 本店已存在该手机号会员时，用会员昵称作为订单展示名，便于店家识别
            // 注：积分在结算完成后才发放（避免退菜/改量导致积分虚高），此处仅做身份识别
            Membership member = membershipService.findByStoreAndPhone(table.getStorefrontId(), phone);
            if (member != null && member.getNickname() != null && !member.getNickname().isBlank()) {
                nickname = member.getNickname();
            }
        }

        UserData guest = userDataService.createGuestUser(nickname);
        // 堂食订单强制覆盖类型与餐桌绑定
        dto.setOrderType(OrderType.DINE_IN.getCode());
        OrderData order = doCreateOrder(guest.getId(), dto, nickname);
        logger.info("扫码免登录下单成功: orderNo={}, guestUserId={}, tableId={}, phone={}",
                order.getOrderNo(), guest.getId(), order.getTableId(), phone == null ? "未填写" : phone);
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
        // 店员录入的顾客电话兜底写入订单联系电话（堂食也记录，便于结算时关联会员积分）
        if (isBlank(dto.getContactPhone()) && !isBlank(dto.getCustomerPhone())) {
            dto.setContactPhone(dto.getCustomerPhone().trim());
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

        // 加菜后新菜品未出餐，订单状态同步回退到「制作中」，避免误判为可结算
        List<OrderItem> allItems = orderItemRepository.findByOrderId(order.getId());
        syncOrderStatusByItems(order, allItems);
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
        // 首次下单未填手机号时，允许在加菜时补填（后续结算即可累积会员积分）
        if (isBlank(order.getContactPhone())) {
            String phone = normalizePhone(dto.getContactPhone());
            if (phone != null) {
                order.setContactPhone(phone);
                orderDataRepository.save(order);
                logger.info("扫码加菜补填手机号成功: orderNo={}, phone={}", order.getOrderNo(), phone);
            }
        }
        return addItemsToOrder(orderId, dto);
    }

    /**
     * 手机号归一化：去空格/横线，空值返回 null，格式非法抛业务异常
     */
    private String normalizePhone(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String phone = raw.trim().replace(" ", "").replace("-", "");
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new BusinessException(400, "手机号格式不正确，请填写 11 位手机号");
        }
        return phone;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * 开始制作（认领菜品）：标记某个菜品由当前操作员制作
     * <p>
     * 认领后该菜品从其他人的待制作列表中消失，仅对认领人可见。
     * 已出餐或已被他人认领的菜品不可重复认领。
     *
     * @param itemId  订单明细ID
     * @param storeId 门店ID（权限校验）
     * @param userId  当前操作人ID
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderData startPreparingItem(Long itemId, Long storeId, Long userId) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(404, "菜品明细不存在"));
        if (item.getStatus() != null && item.getStatus() == OrderItem.STATUS_CANCELLED) {
            throw new BusinessException(400, "该菜品已退菜，无法制作");
        }
        if (item.getServed() != null && item.getServed() == 1) {
            throw new BusinessException(400, "该菜品已出餐");
        }
        if (item.getPreparingBy() != null && !item.getPreparingBy().equals(userId)) {
            throw new BusinessException(400, "该菜品已被其他人认领制作");
        }
        if (item.getPreparingBy() != null && item.getPreparingBy().equals(userId)) {
            throw new BusinessException(400, "您已开始制作该菜品");
        }
        OrderData order = orderDataRepository.findById(item.getOrderId())
                .orElseThrow(() -> new BusinessException(404, "订单不存在"));
        if (!order.getStoreId().equals(storeId)) {
            throw new BusinessException(403, "订单不属于该门店");
        }
        OrderStatus current = OrderStatus.fromCode(order.getStatus());
        if (current == OrderStatus.PENDING) {
            throw new BusinessException(400, "订单尚未接单，请先接单后再制作");
        }
        if (current == OrderStatus.COMPLETED || current == OrderStatus.CANCELLED) {
            throw new BusinessException(400, "订单当前状态为「" + current.getName() + "」，无法制作");
        }

        item.setPreparingBy(userId);
        item.setPreparingTime(System.currentTimeMillis());
        orderItemRepository.save(item);
        logger.info("开始制作: orderNo={}, itemName={}, operator={}",
                order.getOrderNo(), item.getDishName(), userId);

        // 推进订单到制作中（如果还在已接单状态）
        if (current == OrderStatus.CONFIRMED) {
            order.setStatus(OrderStatus.COOKING.getCode());
            orderDataRepository.save(order);
        }

        List<OrderItem> allItems = orderItemRepository.findByOrderId(order.getId());
        order.setItems(allItems);
        return order;
    }

    /**
     * 释放制作（取消认领）：将菜品退回待制作队列
     */
    @Transactional(rollbackFor = Exception.class)
    public void releasePreparingItem(Long itemId, Long storeId, Long userId) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(404, "菜品明细不存在"));
        if (item.getPreparingBy() == null) {
            throw new BusinessException(400, "该菜品尚未被认领");
        }
        if (!item.getPreparingBy().equals(userId)) {
            throw new BusinessException(403, "只能释放自己认领的菜品");
        }
        OrderData order = orderDataRepository.findById(item.getOrderId())
                .orElseThrow(() -> new BusinessException(404, "订单不存在"));
        if (!order.getStoreId().equals(storeId)) {
            throw new BusinessException(403, "订单不属于该门店");
        }
        item.setPreparingBy(null);
        item.setPreparingTime(null);
        orderItemRepository.save(item);
        logger.info("释放制作: orderNo={}, itemName={}, operator={}",
                order.getOrderNo(), item.getDishName(), userId);
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
        // 出餐前置条件：订单必须先接单（后厨确认接单后才开始制作）
        // 避免「顾客刚下单、门店还没确认」就被标记出餐，导致制作流程与状态机脱节
        if (current == OrderStatus.PENDING) {
            throw new BusinessException(400, "订单尚未接单，请先接单后再出餐");
        }

        item.setServed(1);
        orderItemRepository.save(item);
        logger.info("菜品出餐: orderNo={}, itemName={}, quantity={}",
                order.getOrderNo(), item.getDishName(), item.getQuantity());

        // 依据菜品出餐情况同步推进订单状态
        List<OrderItem> allItems = orderItemRepository.findByOrderId(order.getId());
        if (syncOrderStatusByItems(order, allItems)) {
            orderDataRepository.save(order);
        }
        order.setItems(allItems);
        return order;
    }

    /**
     * 后厨待制作队列（KDS：Kitchen Display System）
     * <p>
     * 返回<b>已接单且未出餐</b>的单个菜品扁平列表，按菜品下单时间
     * （{@code OrderItem.createTime}）升序排序——先下单先做，FIFO。
     * <p>
     * 采用「菜品维度」而非「订单分组」展示：
     * <ul>
     *   <li>顾客加菜时，新加的菜品有自己的 createTime，会自然排到队列后面，
     *       不会和原订单菜品混在一起造成遗漏</li>
     *   <li>后厨只关注「下一道要做的菜」，按订单分组反而割裂视线</li>
     *   <li>每条记录自带所属订单信息（订单号、桌号、类型、备注、手机号）</li>
     * </ul>
     * 业界 KDS 实践：
     * <ul>
     *   <li>未接单（PENDING）订单不进入后厨，避免顾客刚下单后厨就开工后又被拒单</li>
     *   <li>已完成/已取消订单不进入队列</li>
     *   <li>退菜明细（status=1）不进入队列</li>
     *   <li>只取 status=0(served=0) 的正常未出餐菜品</li>
     * </ul>
     * <p>
     * 等待时长以菜品 createTime 起算（接单后加的菜品等待从加菜时刻开始计）。
     *
     * @param storeId 门店ID
     * @return 扁平菜品列表，每条含菜品与所属订单信息
     */
    public List<Map<String, Object>> kitchenQueue(Long storeId, Long userId) {
        List<OrderData> orders = orderDataRepository.findKitchenOrders(storeId);
        fillOrderItems(orders);
        long now = System.currentTimeMillis();
        List<Map<String, Object>> queue = new ArrayList<>();
        for (OrderData order : orders) {
            List<OrderItem> pending = order.getItems() == null
                    ? List.of()
                    : order.getItems().stream()
                    .filter(i -> i.getStatus() != null && i.getStatus() == OrderItem.STATUS_NORMAL)
                    .filter(i -> i.getServed() == null || i.getServed() == 0)
                    // 过滤掉被其他人认领的菜品：只展示无人认领或自己认领的
                    .filter(i -> i.getPreparingBy() == null || i.getPreparingBy().equals(userId))
                    .toList();
            if (pending.isEmpty()) {
                continue;
            }
            for (OrderItem i : pending) {
                Map<String, Object> vo = new LinkedHashMap<>();
                // 菜品信息
                vo.put("itemId", i.getId());
                vo.put("dishName", i.getDishName());
                vo.put("specNames", i.getSpecNames());
                vo.put("quantity", i.getQuantity());
                vo.put("topImage", i.getTopImage());
                vo.put("addedLater", i.getAddedLater());
                vo.put("itemCreateTime", i.getCreateTime());
                // 菜品已等待时长：以菜品 createTime 起算
                vo.put("waitedMinutes", (now - i.getCreateTime()) / 60_000);
                // 制作状态：null=待制作(无人认领), 非null=制作中(已认领)
                vo.put("preparingBy", i.getPreparingBy());
                vo.put("preparingTime", i.getPreparingTime());
                vo.put("isMine", i.getPreparingBy() != null && i.getPreparingBy().equals(userId));
                // 所属订单信息（供后厨联系服务员/顾客）
                vo.put("orderId", order.getId());
                vo.put("orderNo", order.getOrderNo());
                vo.put("orderType", order.getOrderType());
                vo.put("tableNumber", order.getTableNumber());
                vo.put("remark", order.getRemark());
                vo.put("contactPhone", order.getContactPhone());
                vo.put("status", order.getStatus());
                queue.add(vo);
            }
        }
        // 排序：我认领的排前面（制作中优先展示），其余按 createTime 升序
        queue.sort(Comparator
                .<Map<String, Object>, Boolean>comparing(m -> !(Boolean) m.get("isMine"))
                .thenComparing(m -> (Long) m.get("itemCreateTime")));
        return queue;
    }

    /**
     * 整单出餐：一次性把该订单所有未出餐的正常明细标记为已出餐（后厨批量完成）
     * <p>
     * 内部复用单个菜品出餐逻辑，保证状态推进与库存/餐桌处理一致。
     *
     * @return 更新后的订单详情
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderData serveAllOrderItems(Long orderId, Long storeId) {
        OrderData order = orderDataRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(404, "订单不存在"));
        if (!order.getStoreId().equals(storeId)) {
            throw new BusinessException(403, "订单不属于该门店");
        }
        OrderStatus current = OrderStatus.fromCode(order.getStatus());
        if (current == OrderStatus.COMPLETED || current == OrderStatus.CANCELLED) {
            throw new BusinessException(400, "订单当前状态为「" + current.getName() + "」，无法出餐");
        }
        if (current == OrderStatus.PENDING) {
            throw new BusinessException(400, "订单尚未接单，请先接单后再出餐");
        }
        List<OrderItem> pending = orderItemRepository.findByOrderId(order.getId()).stream()
                .filter(i -> i.getStatus() != null && i.getStatus() == OrderItem.STATUS_NORMAL)
                .filter(i -> i.getServed() == null || i.getServed() == 0)
                .toList();
        if (pending.isEmpty()) {
            throw new BusinessException(400, "该订单没有待出餐的菜品");
        }
        for (OrderItem item : pending) {
            item.setServed(1);
        }
        orderItemRepository.saveAll(pending);
        logger.info("整单出餐: orderNo={}, items={}", order.getOrderNo(), pending.size());

        List<OrderItem> allItems = orderItemRepository.findByOrderId(order.getId());
        if (syncOrderStatusByItems(order, allItems)) {
            orderDataRepository.save(order);
        }
        order.setItems(allItems);
        return order;
    }

    /**
     * 依据菜品出餐情况同步推进订单状态（出餐已下沉到菜品级，订单状态由明细推导）
     * <p>
     * 规则（业界通行做法：明细状态驱动主单状态，避免主单卡在中间态无法结算）：
     * <ul>
     *   <li>所有正常明细均已出餐 → 推进到「已出餐 READY」（允许从 PENDING/CONFIRMED/COOKING 直接到达，
     *       覆盖「未接单就出餐」「订单只有一个菜品」等场景，保证可以正常结算）</li>
     *   <li>存在部分已出餐 → 说明厨房已开工，推进到「制作中 COOKING」（隐式接单，补写接单时间）</li>
     *   <li>存在未出餐明细且当前为「已出餐 READY」（如出餐后加菜）→ 回退到「制作中 COOKING」</li>
     * </ul>
     * 已完成/已取消为终态，不做自动变更。
     *
     * @param order 订单（会被就地修改状态）
     * @param items 订单全部明细（含已退菜）
     * @return true=订单状态已变更，调用方需持久化
     */
    private boolean syncOrderStatusByItems(OrderData order, List<OrderItem> items) {
        OrderStatus current = OrderStatus.fromCode(order.getStatus());
        if (current == null || current == OrderStatus.COMPLETED || current == OrderStatus.CANCELLED) {
            return false;
        }
        List<OrderItem> normalItems = items.stream()
                .filter(i -> i.getStatus() != null && i.getStatus() == OrderItem.STATUS_NORMAL)
                .toList();
        if (normalItems.isEmpty()) {
            return false;
        }
        boolean allServed = normalItems.stream()
                .allMatch(i -> i.getServed() != null && i.getServed() == 1);
        boolean anyServed = normalItems.stream()
                .anyMatch(i -> i.getServed() != null && i.getServed() == 1);

        long now = System.currentTimeMillis();
        Integer origin = order.getStatus();
        if (allServed) {
            if (current.canAdvanceTo(OrderStatus.READY)) {
                // 未点击接单直接出餐的场景：补全接单时间，保证经营统计口径完整
                if (order.getConfirmTime() == null) {
                    order.setConfirmTime(now);
                }
                order.setStatus(OrderStatus.READY.getCode());
                logger.info("订单全部菜品已出餐，自动推进到已出餐状态: orderNo={}, from={}",
                        order.getOrderNo(), current.getName());
            }
        } else if (current == OrderStatus.READY) {
            // 已出餐后又新增/恢复了未出餐明细，回到制作中等待剩余菜品
            order.setStatus(OrderStatus.COOKING.getCode());
            logger.info("订单存在未出餐菜品，回退到制作中状态: orderNo={}", order.getOrderNo());
        } else if (anyServed && current.canAdvanceTo(OrderStatus.COOKING)) {
            if (order.getConfirmTime() == null) {
                order.setConfirmTime(now);
            }
            order.setStatus(OrderStatus.COOKING.getCode());
            logger.info("订单已有菜品出餐，自动推进到制作中状态: orderNo={}, from={}",
                    order.getOrderNo(), current.getName());
        }
        return !origin.equals(order.getStatus());
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

        // 恢复库存（停用/锁定 SKU 不回补，避免禁用 SKU 被凭空累加库存）
        int restored = productSkuRepository.restoreStock(item.getSkuId(), item.getQuantity().longValue());
        if (restored == 0) {
            logger.warn("SKU 已停用/锁定或不存在，退菜时跳过库存回补: skuId={}, orderNo={}",
                    item.getSkuId(), order.getOrderNo());
        }
        // 标记明细为已取消
        item.setStatus(OrderItem.STATUS_CANCELLED);
        orderItemRepository.save(item);
        // 重算订单金额
        recalcOrderAmount(order);

        // 退掉最后一个未出餐菜品后，订单可能已满足「全部出餐」，同步推进状态以便结算
        List<OrderItem> remainItems = orderItemRepository.findByOrderId(order.getId());
        syncOrderStatusByItems(order, remainItems);
        orderDataRepository.save(order);
        logger.info("退菜成功: orderNo={}, itemName={}, quantity={}, reason={}",
                order.getOrderNo(), item.getDishName(), item.getQuantity(),
                reason == null ? "无" : reason);

        order.setItems(remainItems);
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
            int restoredRows = productSkuRepository.restoreStock(item.getSkuId(), -delta);
            if (restoredRows == 0) {
                logger.warn("SKU 已停用/锁定或不存在，调整数量时跳过库存回补: skuId={}, orderNo={}",
                        item.getSkuId(), order.getOrderNo());
            }
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
            if (!cancelOrderInternal(order, reason)) {
                throw new BusinessException(409, "订单状态已变更，无法取消，请刷新后重试");
            }
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
     * 积分规则：按实际应付金额 1 元 = 1 积分（向下取整，不足 1 元不计）
     *
     * @param orderId 订单ID
     * @param storeId 门店ID（权限校验）
     * @param phone 顾客手机号（可选，为空表示未录入）
     * @param couponCode 使用的优惠券码（可选）
     * @param manualDiscount 店员手动减免金额（可选，抹零/议价）
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderData completeOrder(Long orderId, Long storeId, String phone,
                                   String couponCode, java.math.BigDecimal manualDiscount) {
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

        // 0.1 状态自愈：历史订单可能存在「菜品全部出餐但订单状态未推进」的脏数据，
        //     结算前按菜品出餐情况补齐状态，保证可以正常流转到已完成
        if (syncOrderStatusByItems(checkOrder, items)) {
            orderDataRepository.save(checkOrder);
        }

        // 0.2 结算金额试算：菜品折后合计 → 优惠券减免 → 手动减免
        java.math.BigDecimal baseAmount = items.stream()
                .filter(i -> i.getStatus() != null && i.getStatus() == OrderItem.STATUS_NORMAL)
                .map(i -> nvl(i.getTotalPrice()))
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        String normalizedCouponCode = (couponCode == null || couponCode.isBlank()) ? null : couponCode.trim();
        java.math.BigDecimal couponDiscount = java.math.BigDecimal.ZERO;
        if (normalizedCouponCode != null) {
            couponDiscount = couponService.check(storeId, normalizedCouponCode, baseAmount).getDiscountAmount();
        }
        java.math.BigDecimal manualCut = manualDiscount == null
                ? java.math.BigDecimal.ZERO : manualDiscount;
        if (manualCut.compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new BusinessException(400, "手动减免金额不能为负数");
        }
        if (manualCut.compareTo(baseAmount.subtract(couponDiscount)) > 0) {
            throw new BusinessException(400, "手动减免金额不能超过订单应付金额");
        }

        // 1. 状态流转到已完成（内部校验状态机、onOrderCompleted 累加销量、释放餐桌）
        transitionOrder(orderId, storeId, OrderStatus.COMPLETED, null);

        // 2. 写入优惠券与手动减免并重算应付金额
        OrderData order = orderDataRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(404, "订单不存在"));
        if (normalizedCouponCode != null) {
            order.setCouponCode(normalizedCouponCode);
            order.setCouponAmount(couponDiscount);
        }
        order.setManualDiscountAmount(manualCut);
        recalcOrderAmount(order);
        orderDataRepository.save(order);

        // 3. 核销优惠券（放在订单完成后，失败则整单回滚）
        if (normalizedCouponCode != null) {
            couponService.use(storeId, normalizedCouponCode, order.getId(), order.getOrderNo(),
                    phone == null ? null : phone.trim());
        }

        // 4. 处理结算联系电话与会员积分
        String normalizedPhone = (phone == null) ? null : phone.trim();
        if (normalizedPhone != null && normalizedPhone.isEmpty()) {
            normalizedPhone = null;
        }

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

        if (!cancelOrderInternal(order, "顾客取消")) {
            throw new BusinessException(409, "订单状态已变更，取消失败，请刷新后重试");
        }
        return getOrderDetail(order.getId());
    }

    /**
     * 取消订单内部逻辑：原子翻转状态 → 恢复库存 → 释放餐桌 → 标记明细
     * <p>
     * 幂等保护：通过条件 UPDATE 抢占式把订单从「待接单/已接单」翻转为「已取消」。
     * 顾客取消、店家拒单、超时定时任务并发时只有一方抢占成功，
     * 抢占失败（返回 false）的一方不再恢复库存，杜绝库存被重复回补。
     *
     * @return true=本事务成功取消；false=订单已被其他流程取消或状态已推进，调用方应放弃/报错
     */
    private boolean cancelOrderInternal(OrderData order, String reason) {
        int affected = orderDataRepository.cancelIfCancellable(
                order.getId(),
                OrderStatus.CANCELLED.getCode(),
                reason,
                System.currentTimeMillis(),
                List.of(OrderStatus.PENDING.getCode(), OrderStatus.CONFIRMED.getCode()));
        if (affected == 0) {
            logger.warn("订单取消抢占失败（状态已变更），跳过库存恢复: orderNo={}, currentStatus={}",
                    order.getOrderNo(), order.getStatus());
            return false;
        }

        // 恢复库存（停用/锁定 SKU 不回补，避免禁用 SKU 被凭空累加库存）
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        for (OrderItem item : items) {
            if (item.getStatus() != null && item.getStatus() == OrderItem.STATUS_NORMAL) {
                int restored = productSkuRepository.restoreStock(
                        item.getSkuId(), item.getQuantity().longValue());
                if (restored == 0) {
                    logger.warn("SKU 已停用/锁定或不存在，取消订单时跳过库存回补: skuId={}, orderNo={}",
                            item.getSkuId(), order.getOrderNo());
                }
                item.setStatus(OrderItem.STATUS_CANCELLED);
            }
        }
        orderItemRepository.saveAll(items);

        // 释放餐桌（取消订单时餐桌未真正使用，回到空闲）
        releaseTable(order, false);
        return true;
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
     * 定时任务：每分钟扫描，自动取消超时（30分钟）未接单的订单。
     * <p>
     * 事务策略：扫描本身不开启事务（查询自动提交，不持有锁）；
     * 每个订单在独立的 REQUIRES_NEW 事务中取消，单条失败仅回滚该订单，
     * 不会把整批任务标记为 rollback-only，也避免大事务长时间锁定多行。
     */
    @Scheduled(fixedDelay = 60_000)
    public void cancelTimeoutPendingOrders() {
        long deadline = System.currentTimeMillis() - PENDING_TIMEOUT_MS;
        List<OrderData> timeoutOrders;
        try {
            timeoutOrders = orderDataRepository
                    .findByStatusAndCreateTimeBefore(OrderStatus.PENDING.getCode(), deadline);
        } catch (Exception e) {
            logger.error("扫描超时未接单订单失败", e);
            return;
        }
        if (timeoutOrders.isEmpty()) {
            return;
        }
        for (OrderData order : timeoutOrders) {
            Long orderId = order.getId();
            try {
                requiresNewTransaction.executeWithoutResult(
                        status -> cancelTimeoutOrder(orderId, deadline));
            } catch (Exception e) {
                logger.error("超时订单自动取消失败: orderId={}", orderId, e);
            }
        }
    }

    /**
     * 取消单个超时订单（运行于 REQUIRES_NEW 独立事务内）。
     * <p>
     * 重新加载订单并双重校验状态与超时时间：可能在扫描后已被店家接单或顾客取消，
     * 此时直接跳过；状态翻转走 CAS，保证与其他取消路径并发时只恢复一次库存。
     */
    private void cancelTimeoutOrder(Long orderId, long deadline) {
        OrderData order = orderDataRepository.findById(orderId).orElse(null);
        if (order == null) {
            return;
        }
        if (order.getStatus() == null || order.getStatus() != OrderStatus.PENDING.getCode()) {
            // 已被接单/取消/完成，跳过
            return;
        }
        if (order.getCreateTime() != null && order.getCreateTime() >= deadline) {
            // 双重校验：尚未超时（理论上不会发生，防御性判断）
            return;
        }
        boolean cancelled = cancelOrderInternal(order, "超时未接单，系统自动取消");
        if (cancelled) {
            logger.info("超时订单自动取消: orderNo={}", order.getOrderNo());
        }
        // cancelled=false 说明并发下被其他流程处理，正常跳过
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
        // 菜品小计基础上再扣除优惠券与手动减免，得到最终应付（不低于 0）
        java.math.BigDecimal finalPay = pay
                .subtract(nvl(order.getCouponAmount()))
                .subtract(nvl(order.getManualDiscountAmount()));
        if (finalPay.compareTo(java.math.BigDecimal.ZERO) < 0) {
            finalPay = java.math.BigDecimal.ZERO;
        }
        order.setPayAmount(finalPay.setScale(2, java.math.RoundingMode.HALF_UP));
    }

    /** 空值安全的 BigDecimal 取值 */
    private static java.math.BigDecimal nvl(java.math.BigDecimal value) {
        return value == null ? java.math.BigDecimal.ZERO : value;
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
     * 校验餐桌可用餐（未删除、未停用、无进行中的订单）
     * <p>
     * 此处为前置快速失败校验，给出友好提示并避免无谓扣库存；
     * 真正的并发安全由 {@code DiningTableRepository.bindOrderIfFree} 条件 UPDATE 兜底。
     */
    private void checkTableOrderable(DiningTable table) {
        if (table.getStatus() != null && table.getStatus() == TableStatus.DISABLED.getCode()) {
            throw new BusinessException(400, "餐桌已停用: " + table.getTableNumber());
        }
        if (table.getCurrentOrderId() != null) {
            throw new BusinessException(400, "该餐桌已有进行中的订单，可直接在本桌订单中加菜");
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
        // 联系电话（扫码点餐时由顾客填写，用于关联会员；外送订单为配送联系方式）
        order.setContactPhone(dto.getContactPhone());

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
