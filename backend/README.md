# 功过格后端

基于 Spring Boot 3.3.3 + Java 17 + MyBatis-Plus + MySQL 8 + Redis + JWT 的功过格后端服务。

## 已实现接口

### 认证
- `POST /api/auth/sms-code` 发送短信验证码（已接入阿里云短信，可通过 `ALIYUN_SMS_ENABLED=false` 回退 mock）
- `POST /api/auth/register` 注册并登录
- `POST /api/auth/login` 验证码登录：已注册直接登录，未注册自动注册并登录；也支持密码登录
- `POST /api/auth/logout` 退出登录
- `PUT /api/auth/password` 修改密码

### 用户
- `GET /api/user/profile`
- `PUT /api/user/profile`

### 分类
- `GET /api/categories`
- `POST /api/categories`
- `DELETE /api/categories/{id}`

### 记录
- `GET /api/records/today`
- `GET /api/records?date=2026-08-05`
- `POST /api/records`
- `PUT /api/records/{id}`
- `DELETE /api/records/{id}`
- `GET /api/records/recycle`
- `POST /api/records/recycle/{id}/restore`
- `DELETE /api/records/recycle/{id}`

### 统计
- `GET /api/statistics?range=7&year=2026&month=8`
- `GET /api/statistics/overview`
- `GET /api/statistics/trend?range=7`
- `GET /api/statistics/category`

### 数据
- `GET /api/data/export`
- `DELETE /api/data/clear`

### 反馈
- `POST /api/feedback`
- `GET /api/feedback`

### 同步
- `POST /api/sync/batch`

## 启动方式

1. 先启动 MySQL 和 Redis，可用项目根目录的 `docker-compose.yml`：
   ```bash
   docker compose up -d mysql redis
   ```

2. 初始化数据库：
   ```bash
   mysql -uroot -proot123456 < ../database/init/01_schema.sql
   ```

3. 启动后端：
   ```bash
   mvn spring-boot:run
   ```

4. 接口文档：
   - Swagger UI: http://localhost:8080/swagger-ui.html

## 配置

默认连接：
- MySQL: `localhost:3306/gongguoge`，用户 `root`，密码 `root123456`
- Redis: `localhost:6379`
- JWT 密钥/有效期可通过环境变量覆盖

短信验证码已恢复为阿里云真实发送。  
如果本地不想真实发送，可设置环境变量 `ALIYUN_SMS_ENABLED=false`，此时验证码固定为 `123456`。
