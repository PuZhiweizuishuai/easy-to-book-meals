[中文](README.md) | **English**

# Easy To Book Meals (餐易订)

A QR-code ordering system for small and mid-sized restaurants. Customers scan the QR code on a table card and start ordering without logging in; restaurant staff accept orders, drive the kitchen queue and manage dishes / tables / members from the Web console or the mobile app, while the platform side handles store approval and role-permission administration.

The repository is a single tree containing three independently runnable projects:

| Directory | Description | Tech |
| --- | --- | --- |
| [`food/`](food) | Backend service, also serves the built Web frontend | Spring Boot 4.1 + Java 21 + JPA + Sa-Token |
| [`FoodWeb/`](FoodWeb) | Customer site + store console + platform console (one SPA) | Vue 3.5 + Vite + Pinia + Tailwind/DaisyUI |
| [`store/`](store) | Store-side mobile app | Flutter (Riverpod + Dio) |
| [`Prompt/`](Prompt) | AI prompts, development plans and issue notes from the build process | Docs only, never referenced by code |

---

## Screenshots

### Home

![home](/img/home.png)


### Store

![store](/img/store.png)

### Admin

![admin](/img/admin.png)


![admin01](/img/admin01.png)


![admin02](/img/admin02.png)


![admin03](/img/admin03.png)


### Store App

![app01](/img/app01.jpg)

![app02](/img/app02.jpg)


![app03](/img/app03.jpg)




## Core capabilities

### Customer side (web page, entered via QR code)
- **Login-free QR ordering**: the table QR URL has the form `{site}/store/{storeId}?table={qrCodeKey}`. Opening it binds the table and automatically attaches to / continues that table's order (`/api/order/guest-current`, `/api/order/guest-create`, `/api/order/guest-add-items/{id}`), so adding dishes never requires a new order.
- Store browsing, dish categories with spec/SKU selection, cart, order list and detail.
- Register / login (phone or email + verification code), account settings, notifications, coupon claiming and redemption display, store reviews.
- Membership: store-level membership tier and points linked by phone number, applied automatically at checkout.

### Store side (Web console `/admin` and the app share the same APIs)
- **Order management**: accept / reject / cooking / ready / settle, with both order-level and **dish-level** ready actions, dish removal, quantity changes, manual discounts and coupon redemption.
- **Kitchen display (KDS)**: per-dish pending queue, color-coded by waiting time (urgent after 15/30 minutes), auto-refreshing.
- **Order counter**: staff places orders on behalf of customers, adds dishes to unsettled orders, checkout panel (subtotal − coupon − manual discount = amount due).
- **Dish management**: create / edit / delete, on- and off-shelf, categories, spec groups / spec values with SKU price and stock, image upload and cropping.
- **Table management**: areas and table numbers, table status, QR generation / download / print / reset, same-day reservation timeline.
- **Reservations**: pending → confirmed → seated → completed, plus cancellation and no-show (NO_SHOW).
- **Store management**: store profile, staff invitations, role assignment; **member management** and **reports** (revenue trend, best-selling dishes, ECharts).
- **New-order alerts**: background polling in the app (default 15s, configurable 5/10/15/30/60s) + sound + system notification tray + vibration.

### Platform side (`/system`)
- Global user, role and permission management; store onboarding review, on/off shelf and tags; system parameters (e.g. login-failure lockout threshold).

---

## Quick start

### 1. Backend

Uses a local H2 file database by default — zero external dependencies, runs out of the box:

```bash
cd food
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run

# Package
mvn clean package -Dmaven.test.skip=true
```

- Service URL: `http://localhost:8081`
- H2 console: `http://localhost:8081/h2-console` (JDBC URL `jdbc:h2:~/OrderFood`, user `sa`, password `123456`)
- Tables are created automatically by `spring.jpa.hibernate.ddl-auto: update`; **there are no `.sql` scripts in the repository**.

The first start runs `config/RolePermissionDataInitializer`, which seeds roles, permissions and the default super admin:

> phone `13800000000` / password `123456` (created automatically on first start in dev only — change it immediately in production)

### 2. Web frontend (development mode)

```bash
cd FoodWeb
npm install
npm run dev        # Vite default 5173, /api proxied to http://127.0.0.1:8081
```

Requires Node `^20.19.0 || >=22.12.0`. The request layer lives in `FoodWeb/src/utils/fetch.js`; the base URL comes from `VITE_SERVER_API_URL` and falls back to the same-origin `/api` when unset (there are no `.env` files in the repo, so development relies on the Vite proxy above).

### 3. Frontend build output must be synced (important)

Vite has no `build.outDir` configured, so output lands in `FoodWeb/dist/`, while the backend serves the committed `food/src/main/resources/static/`. Therefore **you must copy manually after every frontend change**, otherwise `:8081` still shows the old pages:

```bash
cd FoodWeb && npm run build
# copy FoodWeb/dist/* over food/src/main/resources/static/
```

`food/src/main/resources/static/` is a version-controlled build artifact; deep SPA routes fall back to `/index.html` through the 404 ErrorPage configured in `config/WebConfig`.

### 4. Flutter store app

```bash
cd store
flutter pub get
flutter run                       # Android / iOS; desktop scaffolding also generated
```

- App name **店家工作台 (Store Workbench)**, Android `applicationId` is `com.buguagaoshu.store.store`.
- The first launch shows the "server address" page; enter the backend address (e.g. `http://192.168.x.x:8081`) and the app appends the `/api` prefix automatically. On a real device you cannot use `127.0.0.1`.
- Login uses the same store account as the Web console.

---

## Tech stack details

**Backend** `food/pom.xml`: Spring Boot 4.1.1, Java 21, `spring-boot-starter-data-jpa`, `validation`, `sa-token-spring-boot4-starter 1.45.0`, H2 (dev) + `mysql-connector-j` (production), `spring-boot-starter-data-redis` + `commons-pool2`, Lombok. No MyBatis, no JWT, no Swagger, no WebSocket.

**Web**: Vue 3.5, vue-router 5, Pinia 3, Tailwind CSS 3 + DaisyUI 4, ECharts 6, `qrcode`, FilePond + cropperjs (image upload and cropping).

**App**: `flutter_riverpod 3`, `dio 5`, `shared_preferences`, `flutter_local_notifications`, `audioplayers`, `mobile_scanner` (QR coupon redemption), `qr_flutter` (local table-card QR generation), `image_picker`, `permission_handler` (pinned to 12.x — 13.x requires compileSdk 37).

---

## Key design

### Table QR code `qrCodeKey`
`DiningTable` (table `dining_table`) holds a globally unique `qr_code_key` (64-bit, generated by `DiningTableService.generateQrCodeKey()` unless customized), which is how the backend resolves "which table of which store". `current_order_id` caches the table's unsettled bill so multiple people at the same table keep adding dishes to one order. Public login-free endpoints: `GET /api/dining-table/qr/{qrCodeKey}`, `/api/order/guest-*`, `/api/files/{y}/{m}/{d}/{name}`; all other `/api/**` routes are intercepted by Sa-Token.

### State machines
- **Order** `enums/OrderStatus`: `PENDING(0) → CONFIRMED(1) → COOKING(2) → READY(3) → COMPLETED(4)`, plus `PENDING/CONFIRMED → CANCELLED(5)`. `canTransitionTo()` enforces strictly adjacent transitions; `canAdvanceTo()` allows monotonic jumps for dish-level readiness and never enters a terminal state.
- **Order type** `enums/OrderType`: `DINE_IN(0 QR ordering at table)` / `TAKEOUT(1 delivery)` / `ONLINE(2 self pickup)`.
- **Table** `enums/TableStatus`: `FREE(0)` / `OCCUPIED(1)` / `RESERVED(2)` / `CLEANING(3)` / `DISABLED(4)`.
- **Reservation** `enums/ReservationStatus`: `PENDING(0) → CONFIRMED(1) → SEATED(2) → COMPLETED(5)`, plus `CANCELLED(3)` and `NO_SHOW(4)`; seating marks the table as occupied.
- **Payment** `enums/PayStatus`: `UNPAID/PAID/REFUNDED` — see "Known limitations" below.

Order number format: `yyyyMMdd-storeId(4 digits)-random 6 digits` (e.g. `20261002-0001-483920`, retried on collision, see `OrderService.generateOrderNo`). Each `OrderItem` stores a snapshot of the dish name, spec and unit price at order time.

### Two-domain RBAC
`config/StpInterfaceImpl` + `cache/RolePermissionCache` feed role and permission codes to Sa-Token; tables: `role_table`, `permission_table`, `user_role_table`, `role_permission_table`. Permission definitions are centralized in `config/RbacDefinition`.

- `enums/RoleDomainEnum`: `GLOBAL` / `STORE`
- Global roles: `SUPER_ADMIN`, `PLATFORM_OPERATOR`, `PLATFORM_AUDITOR`, `CUSTOMER`
- Store roles: `STORE_OWNER`, `STORE_CASHIER`, `STORE_KITCHEN`, `STORE_WAITER`, `STORE_INVENTORY`
- Permission codes look like `order:manage`, `dish:view`, `table:manage`, `member:manage`, `coupon:manage`, `stats:store`, `system:config`, `store:audit`; cross-store access is blocked by `utils/StoreAuthHelper.checkStorePermission(storeId, code)`. The acting user always comes from `StpUtil.getLoginIdAsLong()` — the frontend is never trusted.

Login: `service/AuthService`, phone or email + password, stored with BCrypt and transparently upgraded from legacy MD5; `service/LoginRateLimiter` throttles by account + client IP, with a threshold super admins can adjust in system settings.

### Data model
Entities live in `com.buguagaoshu.food.data`, and **JPA relations are deliberately avoided in favor of plain ID foreign keys** (which keeps historical snapshots and cross-store queries simple). Main tables: `store_table`, `base_food_table` (dishes), `product_sku_table`, `spec_group_table`/`spec_value_table`, `food_tag_table` + `association_food_tag_table`, `store_tag_table` + `association_store_tag_table`, `dining_table`, `table_reservation`, `order_table` + `order_item_table`, `user_table`, the four RBAC tables, `coupon_table` + `user_coupon_table`, `membership_table` + `points_transaction_table`, `review_table`, `notification_table`, `files_data_table`, `system_settings`.

### Real-time alerts
There is no WebSocket / SSE. Both Web and app use **polling**: `providers/alert_provider.dart` in the app keeps a self-renewing timer that checks for pending orders and newly ready items, combined with `sound_service.dart` (`assets/sounds/new_order.mp3`, `new_item.wav`) and system notifications; on the web side the bell badge calls `GET /api/notification/unread-count`. Returning to the foreground restarts polling immediately.

---

## API groups

| Prefix | Controller | Purpose |
| --- | --- | --- |
| `/api/auth`, `/api/user` | `AuthController`, `UserController` | Login/logout, current user info, registration and account settings |
| `/api/store`, `/api/storetag` | `StoreController`, `StoreTagController` | Public store browsing and detail |
| `/api/dishes`, `/api/foodtag`, `/api/sku`, `/api/spec` | dish-related | Dishes, categories, SKUs, specs |
| `/api/dining-table` | `DiningTableController` | Table management and the scan entry point `qr/{qrCodeKey}` |
| `/api/order` | `OrderController` | Customer orders, login-free QR ordering and dish additions |
| `/api/table-reservation` | `TableReservationController` | Reservation create/update and status transitions |
| `/api/review`, `/api/coupon`, `/api/notification` | reviews, coupons, in-app messages |
| `/api/files` | `FileController` | Image upload and date-sharded reads |
| `/api/order/manage`, `/api/store/manage`, `/api/manage/coupon`, `/api/manage/membership`, `/api/manage/report` | `controller/manage` | Store-side order lifecycle and kitchen, stores, coupons, members, reports |
| `/api/sys/user`, `/api/sys/role`, `/api/sys/permission`, `/api/sys/store`, `/api/sys/settings` | `controller/sys` | Platform-side users/roles/permissions, store review, system settings |

---

## Production deployment

The production profile injects secrets through environment variables, so nothing sensitive is written to config files:

```bash
export SPRING_PROFILES_ACTIVE=prod
export MYSQL_HOST=... MYSQL_PORT=3306 MYSQL_DATABASE=food MYSQL_USERNAME=... MYSQL_PASSWORD=...
export REDIS_HOST=... REDIS_PORT=6379 REDIS_PASSWORD=... REDIS_DATABASE=0
cd food && ./mvnw clean package
java -jar target/food-0.0.1-SNAPSHOT.jar
```

`application-prod.yml` uses MySQL + HikariCP + Redis (`token-name: Token` sessions), timezone `Asia/Shanghai`, and disables the H2 console. Uploads are written to `files/yyyy/MM/dd/` under the process working directory (`config/FileConfig`: image extensions only, renamed with UUIDs, directory-traversal checked on read), and must be persisted/backed up together with the jar's working directory. HTTPS and real client IP forwarding are left to your reverse proxy (e.g. Nginx).

---

## Known limitations

- **No payment integration**: `PayStatus` is only a process marker; dine-in is settled at the counter, and nothing in the codebase ever sets an order to `PAID`. Revenue reports therefore count every completed order as income. See `food/功能完善建议.md`.
- **No real push notifications**: relies on polling, so the app must stay in the foreground or alive in the background; no Android foreground-service permission has been requested.
- **No SQL scripts or migration tool**: depends on Hibernate `ddl-auto: update`; production should switch to `validate` + Flyway/Liquibase.
- **Verification codes / SMS are placeholders**: `food.sms.provider: log` (`LogSmsSender`/`LogEmailCodeSender`) prints codes to the log, so real registration does not work until Aliyun/Tencent Cloud SMS is wired in.
- No Dockerfile, no CI configuration; the frontend UI is Chinese-only.

## Further documentation

- `food/功能完善建议.md` — defect and TODO list graded P0/P1/P2.
- `Prompt/开发计划.md`, `Prompt/问题记录.md` — development plan, API conventions and troubleshooting notes; `Prompt/*.txt` are AI prompts saved during development and **are not referenced by any code**.

> Note: the original Chinese README titled this project 「餐易定」, while the code and UI consistently use 「餐易订」; this document follows the code.

## License

MIT (see the metadata declaration in `FoodWeb/index.html`).
