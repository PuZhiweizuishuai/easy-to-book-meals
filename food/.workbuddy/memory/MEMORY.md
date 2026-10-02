# 项目记忆

## 项目结构
- 后端：Spring Boot + JPA + H2（`food/`），端口 8081
- 前端：Vue 3 + Pinia + DaisyUI（`FoodWeb/`），构建产物部署到 `food/src/main/resources/static/`
- 前端源码在 `FoodWeb/src`，非 `food/src/main/resources/static`（后者是编译产物）

## 编译验证方式
- 后端无 Maven 环境时，可用 javac + Lombok processorpath 验证：
  ```
  javac -cp "<jars>;target/classes" -processorpath <lombok.jar> -d <out> <src files>
  ```
  必须加 `-processorpath lombok.jar` 否则 @Getter/@Data 不生成方法
- 前端：`cd FoodWeb && npm run build`

## 订单类型（OrderType）
- 0=堂食(扫码点餐，绑定餐桌) / 1=外送 / 2=线上(到店自取)
- 堂食订单必须绑定餐桌；非堂食清空餐桌绑定

## 下单方式
- `/api/order/create` — 登录用户自助下单
- `/api/order/guest-create` — 扫码免登录下单（创建匿名顾客用户，固定堂食）
- `/api/order/manage/staff-create` — 店员协助下单（order:manage 权限）
- `/api/order/add-items/{id}` — 顾客加菜（校验归属）
- `/api/order/manage/add-items/{id}` — 店员加菜

## 关键约定
- 订单字段 `orderType` 默认 0（向后兼容历史数据）
- 匿名顾客：UserDataService.createGuestUser 直接落库，绕过 @NotBlank 校验
- 下单后 bindCurrentOrder 自动将餐桌置为"已占用"
- 加菜仅允许 PENDING/CONFIRMED 状态
