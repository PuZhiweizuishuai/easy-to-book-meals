[**中文**](README.md) | [English](README_EN.md)

# 餐易订

面向中小餐馆的扫码点餐系统。顾客扫描桌牌二维码即可免登录点餐，店家通过 Web 后台或手机 App 接单、后厨出餐、管理菜品/餐桌/会员，平台侧提供店铺审核与角色权限管理。

仓库为单体多模块结构，包含三个可独立运行的工程：

| 目录 | 说明 | 技术 |
| --- | --- | --- |
| [`food/`](food) | 后端服务，同时托管 Web 前端构建产物 | Spring Boot 4.1 + Java 21 + JPA + Sa-Token |
| [`FoodWeb/`](FoodWeb) | 顾客端 + 店家后台 + 平台后台（单 SPA） | Vue 3.5 + Vite + Pinia + Tailwind/DaisyUI |
| [`store/`](store) | 店家版移动端 App | Flutter (Riverpod + Dio) |
| [`Prompt/`](Prompt) | 开发过程中的 AI 提示词、开发计划与问题记录 | 纯文档，不被代码引用 |

---

## 截图

### 主页

![home](/img/home.png)


### 商家

![store](/img/store.png)

### 管理

![admin](/img/admin.png)


![admin01](/img/admin01.png)


![admin02](/img/admin02.png)


![admin03](/img/admin03.png)


### 店家版APP

![app01](/img/app01.jpg)

![app02](/img/app02.jpg)


![app03](/img/app03.jpg)




## 核心能力

### 顾客端（网页，扫码进入）
- **免登录扫码点餐**：桌牌二维码 URL 形如 `{站点}/store/{storeId}?table={qrCodeKey}`，进入即绑定餐桌并自动关联/续写当桌订单（`/api/order/guest-current`、`/api/order/guest-create`、`/api/order/guest-add-items/{id}`），加菜无需重复下单。
- 门店浏览、菜品分类与规格/SKU 选购、购物车、订单列表与详情。
- 注册登录（手机号/邮箱 + 验证码）、账号设置、消息通知、优惠券领取与核销展示、门店评价。
- 会员：按手机号关联门店会员等级与积分，结账时自动计入。

### 店家端（Web 后台 `/admin` 与 App 共用同一套接口）
- **订单管理**：接单 / 拒单 / 制作中 / 已出餐 / 完成结算，订单级与**菜品级**出餐操作、退菜、改数量、手动折扣、优惠券核销。
- **后厨看板（KDS）**：菜品粒度待制作队列，按等待时长变色（15/30 分钟转急），自动刷新。
- **点餐台**：店员代客下单、给未结算订单加菜、结账面板（小计 − 优惠券 − 手动折扣 = 应付）。
- **菜品管理**：菜品增删改上下架、分类、规格组/规格值与 SKU 价格库存、图片上传裁剪。
- **餐桌管理**：区域/桌号管理、桌位状态、二维码生成/下载/打印/重置、当日预订时间轴。
- **预订管理**：待确认 → 已确认 → 已到店 → 已完成，支持取消与爽约（NO_SHOW）。
- **店铺管理**：门店资料、员工邀请、角色分配；**会员管理**与**数据报表**（营业额趋势、热销菜品，ECharts）。
- **新单提醒**：App 后台轮询（默认 15s，可配 5/10/15/30/60s）+ 提示音 + 系统通知栏 + 振动。

### 平台端（`/system`）
- 全局用户、角色与权限管理；店铺入驻审核、上下架与标签；系统参数（如登录失败锁定阈值）。

---

## 快速开始

### 1. 后端

默认使用本地 H2 文件数据库，零外部依赖，可直接启动：

```bash
cd food
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run

# 打包
mvn clean package -Dmaven.test.skip=true
```

- 服务地址：`http://localhost:8081`
- H2 控制台：`http://localhost:8081/h2-console`（JDBC URL `jdbc:h2:~/OrderFood`，用户 `sa`，密码 `123456`）
- 建表由 `spring.jpa.hibernate.ddl-auto: update` 自动完成，**仓库中没有 `.sql` 脚本**。

首次启动会由 `config/RolePermissionDataInitializer` 初始化角色、权限与默认超级管理员：

> 手机号 `13800000000` / 密码 `123456`（仅开发环境首启自动创建，生产请立即修改）

### 2. Web 前端（开发模式）

```bash
cd FoodWeb
npm install
npm run dev        # Vite 默认 5173，/api 已代理到 http://127.0.0.1:8081
```

需要 Node `^20.19.0 || >=22.12.0`。请求层封装在 `FoodWeb/src/utils/fetch.js`，基址取 `VITE_SERVER_API_URL`，未设置时回落到同源 `/api`（仓库中无 `.env` 文件，开发依赖上面的 Vite 代理）。

### 3. 前端构建产物需要同步（重要）

Vite 未配置 `build.outDir`，产物默认落在 `FoodWeb/dist/`，而后端托管的是已提交的 `food/src/main/resources/static/`。因此**改完前端必须手动拷贝**，否则 `:8081` 上仍是旧页面：

```bash
cd FoodWeb && npm run build
# 将 FoodWeb/dist/* 覆盖到 food/src/main/resources/static/
```

`food/src/main/resources/static/` 是纳入版本控制的构建产物；SPA 深层路由由 `config/WebConfig` 的 404 → `/index.html` ErrorPage 兜底。

### 4. Flutter 店家端

```bash
cd store
flutter pub get
flutter run                       # Android / iOS；也已生成桌面平台脚手架
```

- 应用名 **店家工作台**，Android `applicationId` 为 `com.buguagaoshu.store.store`。
- 首次启动进入「服务器地址」页，填写后端地址（如 `http://192.168.x.x:8081`），App 自动拼接 `/api` 前缀；真机调试时不能填 `127.0.0.1`。
- 登录使用与 Web 后台相同的店家账号。

---

## 技术栈明细

**后端** `food/pom.xml`：Spring Boot 4.1.1、Java 21、`spring-boot-starter-data-jpa`、`validation`、`sa-token-spring-boot4-starter 1.45.0`、H2（开发）+ `mysql-connector-j`（生产）、`spring-boot-starter-data-redis` + `commons-pool2`、Lombok。无 MyBatis、无 JWT、无 Swagger、无 WebSocket。

**Web** Vue 3.5、vue-router 5、Pinia 3、Tailwind CSS 3 + DaisyUI 4、ECharts 6、`qrcode`、FilePond + cropperjs（图片上传裁剪）。

**App** `flutter_riverpod 3`、`dio 5`、`shared_preferences`、`flutter_local_notifications`、`audioplayers`、`mobile_scanner`（扫码核销优惠券）、`qr_flutter`（本地生成桌牌二维码）、`image_picker`、`permission_handler`（锁定 12.x，13.x 要求 compileSdk 37）。

---

## 关键设计

### 餐桌二维码 `qrCodeKey`
`DiningTable`（表 `dining_table`）持有全局唯一的 `qr_code_key`（64 位，未自定义时由 `DiningTableService.generateQrCodeKey()` 生成），后端据此定位「哪家店的哪张桌」。`current_order_id` 缓存该桌未结算账单，实现同桌多人加菜续写同一订单。免登录公开入口：`GET /api/dining-table/qr/{qrCodeKey}`、`/api/order/guest-*`、`/api/files/{y}/{m}/{d}/{name}`，其余 `/api/**` 由 Sa-Token 拦截。

### 状态机
- **订单** `enums/OrderStatus`：`PENDING(0 待接单) → CONFIRMED(1 已接单) → COOKING(2 制作中) → READY(3 已出餐) → COMPLETED(4 已完成)`，`PENDING/CONFIRMED → CANCELLED(5)`。`canTransitionTo()` 严格相邻流转；`canAdvanceTo()` 允许菜品级出餐时单调跳档，永不进入终态。
- **订单类型** `enums/OrderType`：`DINE_IN(0 堂食扫码)` / `TAKEOUT(1 外送)` / `ONLINE(2 到店自取)`。
- **餐桌** `enums/TableStatus`：`FREE(0)` / `OCCUPIED(1)` / `RESERVED(2)` / `CLEANING(3)` / `DISABLED(4)`。
- **预订** `enums/ReservationStatus`：`PENDING(0) → CONFIRMED(1) → SEATED(2) → COMPLETED(5)`，另有 `CANCELLED(3)`、`NO_SHOW(4)`；到店时餐桌置为占用。
- **支付** `enums/PayStatus`：`UNPAID/PAID/REFUNDED` —— 见下方「已知限制」。

订单号格式：`yyyyMMdd-门店ID(4位)-随机6位`（如 `20261002-0001-483920`，冲突时重试，见 `OrderService.generateOrderNo`）。订单明细 `OrderItem` 保存下单时的菜名、规格与单价快照。

### 双域 RBAC
`config/StpInterfaceImpl` + `cache/RolePermissionCache` 为 Sa-Token 提供角色/权限码；表：`role_table`、`permission_table`、`user_role_table`、`role_permission_table`。权限定义集中在 `config/RbacDefinition`。

- `enums/RoleDomainEnum`：`GLOBAL` / `STORE`
- 全局角色 `SUPER_ADMIN`（超级管理员）、`PLATFORM_OPERATOR`（平台运营）、`PLATFORM_AUDITOR`（平台审核）、`CUSTOMER`（顾客）
- 门店角色 `STORE_OWNER`（老板）、`STORE_CASHIER`（收银员）、`STORE_KITCHEN`（后厨）、`STORE_WAITER`（服务员）、`STORE_INVENTORY`（库存管理员）
- 权限码形如 `order:manage`、`dish:view`、`table:manage`、`member:manage`、`coupon:manage`、`stats:store`、`system:config`、`store:audit`；跨店越权由 `utils/StoreAuthHelper.checkStorePermission(storeId, code)` 拦截，操作人一律取 `StpUtil.getLoginIdAsLong()`，不信任前端。

登录：`service/AuthService`，手机号/邮箱 + 密码，BCrypt 存储并兼容旧 MD5 透明升级；`service/LoginRateLimiter` 按账号 + 客户端 IP 限流，阈值可由超管在系统设置中调整。

### 数据模型
实体集中在 `com.buguagaoshu.food.data`，**刻意不建 JPA 关联，只用 ID 外键**（便于历史快照与跨店查询）。主要表：`store_table`、`base_food_table`（菜品）、`product_sku_table`、`spec_group_table`/`spec_value_table`、`food_tag_table` + `association_food_tag_table`、`store_tag_table` + `association_store_tag_table`、`dining_table`、`table_reservation`、`order_table` + `order_item_table`、`user_table`、RBAC 四表、`coupon_table` + `user_coupon_table`、`membership_table` + `points_transaction_table`、`review_table`、`notification_table`、`files_data_table`、`system_settings`。

### 实时提醒
没有 WebSocket / SSE。Web 与 App 均采用**轮询**：App 的 `providers/alert_provider.dart` 自续期定时器检查待接单单与新出餐项，配合 `sound_service.dart`（`assets/sounds/new_order.mp3`、`new_item.wav`）与系统通知；Web 侧铃铛角标走 `GET /api/notification/unread-count`。App 从后台回到前台会立即重启轮询。

---

## API 分组一览

| 前缀 | 控制器 | 用途 |
| --- | --- | --- |
| `/api/auth`、`/api/user` | `AuthController`、`UserController` | 登录登出、当前用户信息、注册与账号设置 |
| `/api/store`、`/api/storetag` | `StoreController`、`StoreTagController` | 门店公开浏览与详情 |
| `/api/dishes`、`/api/foodtag`、`/api/sku`、`/api/spec` | 菜品相关 | 菜品、分类、SKU、规格 |
| `/api/dining-table` | `DiningTableController` | 餐桌管理与扫码入口 `qr/{qrCodeKey}` |
| `/api/order` | `OrderController` | 顾客下单、免登录扫码下单与加菜 |
| `/api/table-reservation` | `TableReservationController` | 预订增改与状态流转 |
| `/api/review`、`/api/coupon`、`/api/notification` | 评价、优惠券、站内消息 |
| `/api/files` | `FileController` | 图片上传与按日期分片读取 |
| `/api/order/manage`、`/api/store/manage`、`/api/manage/coupon`、`/api/manage/membership`、`/api/manage/report` | `controller/manage` | 店家侧订单生命周期与后厨、门店、优惠券、会员、报表 |
| `/api/sys/user`、`/api/sys/role`、`/api/sys/permission`、`/api/sys/store`、`/api/sys/settings` | `controller/sys` | 平台侧用户/角色/权限、店铺审核、系统设置 |

---

## 生产部署

生产 profile 通过环境变量注入密钥，配置文件不落盘：

```bash
export SPRING_PROFILES_ACTIVE=prod
export MYSQL_HOST=... MYSQL_PORT=3306 MYSQL_DATABASE=food MYSQL_USERNAME=... MYSQL_PASSWORD=...
export REDIS_HOST=... REDIS_PORT=6379 REDIS_PASSWORD=... REDIS_DATABASE=0
cd food && ./mvnw clean package
java -jar target/food-0.0.1-SNAPSHOT.jar
```

`application-prod.yml` 使用 MySQL + HikariCP + Redis（`token-name: Token` 会话）、时区 `Asia/Shanghai`，并关闭 H2 控制台。上传文件写在进程工作目录下的 `files/yyyy/MM/dd/`（`config/FileConfig`，仅允许图片扩展名，UUID 重命名，读取路径做了目录穿越校验），需与 jar 工作目录一起做持久化/备份。反向代理（如 Nginx）自行配置 HTTPS 与真实客户端 IP 透传。

---

## 已知限制

- **支付未对接**：`PayStatus` 仅作流程标记，堂食按到店收银处理，全库没有把订单置为 `PAID` 的写入；营业额统计因此把已完成订单一律计为收入。详见 `food/功能完善建议.md`。
- **无真实推送**：依赖轮询，App 需保持前台或后台存活；未申请 Android 前台服务权限。
- **无 SQL 脚本与数据库迁移工具**：依赖 Hibernate `ddl-auto: update`，生产建议改为 `validate` + Flyway/Liquibase。
- **验证码/短信为占位实现**：`food.sms.provider: log`（`LogSmsSender`/`LogEmailCodeSender`），验证码打印在日志中，接入阿里云/腾讯云短信前无法用于真实注册。
- 无 Dockerfile、无 CI 配置；前端仅中文界面。

## 补充文档

- `food/功能完善建议.md` —— 按 P0/P1/P2 分级的缺陷与待办清单。
- `Prompt/开发计划.md`、`Prompt/问题记录.md` —— 开发计划、接口约定与问题排查记录；`Prompt/*.txt` 为开发过程中保存的 AI 提示词，**不被任何代码引用**。

> 注：仓库根 README 原标题写作「餐易定」，代码与页面统一使用「餐易订」，本文以代码为准。

## License

MIT（见 `FoodWeb/index.html` 元信息声明）。
