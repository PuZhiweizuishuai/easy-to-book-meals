# 项目记忆

## 项目结构
- 后端：Spring Boot + JPA + H2（`food/`），端口 8081
- 前端：Vue 3 + Pinia + DaisyUI（`FoodWeb/`），构建产物部署到 `food/src/main/resources/static/`
- 前端源码在 `FoodWeb/src`，非 `food/src/main/resources/static`（后者是编译产物）

## 移动端店家版 App（`store/`，Flutter）
- 代码结构：`lib/core`（网络/存储/主题/工具/tts+通知服务）、`lib/data`（models + api）、
  `lib/shared/widgets`（通用组件）、`lib/providers`、`lib/features`
- 移动端需先配置后端地址（无法用 `/api` 代理）：`ServerAddress.normalize/apiBase` + `preferences.apiBaseUrl`，
  Dio baseUrl 由 [serverConfigVersionProvider] 自增触发重建
- **图标库统一用 `material_symbols_icons`（`Symbols.xxx`），已废弃 material_design_icons_flutter**
- riverpod 3 无 `StateProvider` / `StateNotifierProvider`，统一用 `Notifier` + `NotifierProvider`
- json 反序列化：所有 double 字段全部用 `(json[x] as num?)?.toDouble()` 兼容后端返回的整型金额；bool 需 `@JsonKey(defaultValue:)`
- 分页兼容：后端 Spring `Page` 序列化可能平铺（`number` 顶层）或嵌套（`page` 对象），`PageResult.fromJson` 都收；
  后端 page 参数是 1 基，`PageResult` 对外也是 1 基（内部 +1 换算）
- 提醒机制：无 WebSocket，`AlertProvider` 轮询 /order/manage/list(status=0) 与 /order/manage/kitchen/queue，
  首次只建基线，后续 diff 才触发提示音（audioplayers AssetSource）+ 本地通知
- 优惠券扫码核销：`CouponCodeExtractor` 支持纯码 / 带横线 UUID / URL `?code=`、`coupon:` 前缀，
  校验走 GET `/api/coupon/check`(storeId,code,amount)，核销在 POST `/order/manage/complete/{id}` 带 couponCode + manualDiscount

## 编译验证方式

### Flutter（`store/`）
- 沙箱禁止创建子进程（`flutter`、`mvn`、build_runner 均报 CreateFile failed 231 / 管道范例都在使用中），
  所以用 `dart --packages=.dart_tool/package_config.json tool/analyze.dart <dir> [--info]`（package:analyzer 进程内分析）替代 `flutter analyze`
- 因此 `.g.dart` 全部手写；`flutter test` / `flutter build apk` 需在有外网的正常机器上自跑一次
- 测试用例在 `store/test`（core/utils、data/models、features/orders/extractor、providers/settings、widget/smoke）

### 后端 / Web
- 后端：`mvn -DskipTests compile`（Maven 在 /e/Maven/apache-maven-3.9.12/bin/mvn）
- 后端无 Maven 环境时，可用 javac + Lombok processorpath 验证：
  ```
  javac -cp "<jars>;target/classes" -processorpath <lombok.jar> -d <out> <src files>
  ```
  必须加 `-processorpath lombok.jar` 否则 @Getter/@Data 不生成方法
- 启动冒烟：本机常有实例占用 `~/OrderFood.mv.db`，直接 spring-boot:run 会报 H2 IO Exception 90028；
  需指定临时库 `--spring.datasource.url=jdbc:h2:./target/xxx;NON_KEYWORDS=KEY --server.port=8099`
- 前端：`cd FoodWeb && npm run build`，产物需同步到 `food/src/main/resources/static/`

## 订单状态（菜品级出餐驱动主单状态）
- 出餐标记在 OrderItem.served；订单状态由 `OrderService.syncOrderStatusByItems` 推导：
  全部正常明细出餐 → READY；部分出餐 → COOKING（PENDING 也会隐式接单并补 confirmTime）；
  READY 下出现未出餐明细（加菜）→ 回退 COOKING
- `OrderStatus.canAdvanceTo()` 允许跳过中间态（不含终态），供自动推进使用；
  严格状态机仍是 `canTransitionTo()`

## 优惠券
- 券模板 Coupon（规则/发行量）+ 券码 UserCoupon（32 位无横线 UUID，可生成二维码）
- 类型：1 满减 / 2 折扣（可限最高优惠）/ 3 无门槛；券码未关联用户也可凭码核销
- 结算金额 = 菜品小计 − couponAmount − manualDiscountAmount；积分按实付累积
- 权限：coupon:manage（店主）/ coupon:view

## 会员昵称（门店维度，重要）
- `Membership.nickname` 是**门店级**展示名，与 `UserData.nickname`（顾客账号昵称）相互独立
- 门店改昵称只允许改本店 membership 行（membershipId + storeId 双重定位），
  **禁止回写 UserData**，否则污染账号昵称并跨店串数据
- 同一手机号在 A/B 店各有一条 membership，昵称互不影响

## 数据报表
- 后端 `ReportService` / `ReportController`（`/api/manage/report`，权限 stats:store）
- 口径：营业额=已完成订单实付；客单价=实付/已完成单；畅销榜排除退菜；按 createTime 归属营业日
- 聚合放内存做（避免 native SQL 日期方言差异），明细按 500 单分批查

## 前端可复用组件
- `components/chart/EChart.vue`：echarts 按需引入（core + line/bar + grid/tooltip/legend + canvas），
  ResizeObserver 自适应、卸载 dispose。echarts 已在 vite manualChunks 中单独分包
- `components/dish/SkuPickerModal.vue`：规格分组单选 + 数量 + 价格；props(dish, confirmText, boxClass)、
  emits(close, confirm, loaded)、header 插槽。已被协助下单 / 加菜 / 顾客端点餐三处复用
- **坑**：SkuPickerModal 内部必须 `watch(() => props.dish?.id)`，不能 watch 整个 dish 对象——
  顾客端会在 loaded 回调里合并数据替换引用，watch 对象会造成无限请求循环
- `components/order/OrderDetailModal.vue`：订单详情（props: order, editable, busy；emits: close/serve/remove/adjust/add）
- `components/common/QrCode.vue`：基于 qrcode 包生成二维码

## 订单类型（OrderType）
- 0=堂食(扫码点餐，绑定餐桌) / 1=外送 / 2=线上(到店自取)
- 堂食订单必须绑定餐桌；非堂食清空餐桌绑定

## 下单方式
- `/api/order/create` — 登录用户自助下单
- `/api/order/guest-create` — 扫码免登录下单（创建匿名顾客用户，固定堂食）
- `/api/order/manage/staff-create` — 店员协助下单（order:manage 权限）
- `/api/order/add-items/{id}` — 顾客加菜（校验归属）
- `/api/order/manage/add-items/{id}` — 店员加菜

## 手机号 / 会员关联
- 扫码下单、顾客自助下单、店员协助下单均可在下单时填手机号 → 写入 OrderData.contactPhone
- 手机号格式 `^1[3-9]\d{9}$`（后端 `OrderService.normalizePhone` 校验，前端同步校验）
- 下单时只做会员识别（有会员记录则用会员昵称做订单展示名，**不改 userId**）；
  积分一律在结算完成时发放，避免退菜/改量导致积分虚高
- 已关联手机号的订单不允许被覆盖；加菜时若订单未填手机号可补填

## 关键约定
- 订单字段 `orderType` 默认 0（向后兼容历史数据）
- 匿名顾客：UserDataService.createGuestUser 直接落库，绕过 @NotBlank 校验
- 下单后 bindCurrentOrder 自动将餐桌置为"已占用"
- 加菜仅允许 PENDING/CONFIRMED 状态
