# 功过格 · 部署手册（阿里云 ECS / CentOS 7.9）

面向：2 核 2G、CentOS 7.9 的阿里云 ECS。

> **重要前提**：CentOS 7 已于 2024-06-30 停止维护，官方 yum 源已下线。
> 脚本里已经处理了这个问题（切到 vault 归档站），否则你第一步 `yum install` 就会失败。

全程命令可直接复制。命令前的 `#` 是注释，不用复制。

---

## 目录

1. [阿里云控制台准备](#1-阿里云控制台准备)
2. [连接服务器并初始化环境](#2-连接服务器并初始化环境)
3. [数据库设置与数据迁移](#3-数据库设置与数据迁移)
4. [部署后端](#4-部署后端)
5. [配置 Nginx 反向代理（可选但推荐）](#5-配置-nginx-反向代理可选但推荐)
6. [前端配置与打包](#6-前端配置与打包)
7. [验收清单](#7-验收清单)

---

## 1. 阿里云控制台准备

### 1.1 公网 IP

本实例当前公网 IP 为 **`47.114.84.8`**（实例已运行，无需再绑）。本文档中所有命令都已填好该 IP，可直接复制。

> 如果以后重开实例导致公网 IP 变了，把本文档里的 `47.114.84.8` 全局替换成新 IP 即可
> （同时也要改 `frontend/utils/config.js` 里的 `PROD_HOST`）。

### 1.2 配置安全组（放行端口）

实例 → 安全组 → 配置规则 → **入方向** → 手动添加：

| 端口 | 授权对象 | 用途 |
| --- | --- | --- |
| 22 | 0.0.0.0/0（或你的 IP） | SSH 登录 |
| 80 | 0.0.0.0/0 | HTTP（Nginx / 直连测试） |
| 443 | 0.0.0.0/0 | HTTPS（配了证书后） |
| 8080 | 0.0.0.0/0 | 后端直连测试用；**配好 Nginx 后建议删掉这条** |

> 只放开 80/443、把 8080 关掉是更安全的做法：所有流量走 Nginx，后端只监听本机。

---

## 2. 连接服务器并初始化环境

```bash
# 用 root 登录（把公网IP换成你的）
ssh root@47.114.84.8
```

上传并执行初始化脚本（脚本在项目 `deploy/` 目录下）：

```bash
# 本地执行：上传脚本
scp deploy/setup-server.sh root@47.114.84.8:/root/

# 服务器执行
cd /root
chmod +x setup-server.sh
./setup-server.sh
```

脚本会依次完成：

1. 修复 CentOS 7 的 yum 源（切到 vault）
2. 安装 **JDK 17**（Adoptium Temurin）
3. 安装 **MySQL 8** 并启动
4. 安装 **Redis**（限制只监听 127.0.0.1）并启动
5. 创建应用用户 `gongguoge` 与目录 `/opt/gongguoge`

⚠️ **MySQL 初始 root 密码会在输出里打印一次**，记下来，下一步要用。

---

## 3. 数据库设置与数据迁移

### 3.1 设置 root 密码

```bash
# 用上一步拿到的临时密码登录（复制时注意密码里的特殊字符要加引号）
mysql -uroot -p'<临时密码>'

# 登录后执行：MySQL 8 默认有密码强度校验，这里设一个够强的
ALTER USER 'root'@'localhost' IDENTIFIED BY 'Root@Gongguoge2026';
FLUSH PRIVILEGES;
EXIT;
```

### 3.2 创建业务库与专用用户

```bash
mysql -uroot -p'Root@Gongguoge2026' <<'SQL'
CREATE DATABASE IF NOT EXISTS gongguoge
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- 业务账号只允许本机连接，避免数据库直接暴露
CREATE USER IF NOT EXISTS 'gongguoge'@'127.0.0.1' IDENTIFIED BY 'Gongguoge@Db2026';
GRANT ALL PRIVILEGES ON gongguoge.* TO 'gongguoge'@'127.0.0.1';
FLUSH PRIVILEGES;
SQL
```

### 3.3 导入表结构

```bash
# 本地执行：上传建表脚本
scp database/init/01_schema.sql root@47.114.84.8:/root/

# 服务器执行
mysql -uroot -p'Root@Gongguoge2026' < /root/01_schema.sql

# 确认表已创建（应看到 t_user / t_category / t_merit_record / t_feedback / t_sms_log）
mysql -uroot -p'Root@Gongguoge2026' -e "USE gongguoge; SHOW TABLES;"
```

### 3.4 迁移你本地的数据（可选）

想把本机已有的账号和记录一起搬过去：

```bash
# ===== 本地（Windows PowerShell）=====
$mysql = 'D:\software\111\bin\mysql.exe'
& $mysql -uroot -p1234 --default-character-set=utf8mb4 gongguoge > gongguoge-data.sql

# 上传（先建表结构再导数据，避免顺序问题）
scp gongguoge-data.sql root@47.114.84.8:/root/
```

```bash
# ===== 服务器 =====
# 先建结构（若已在 3.3 建过可跳过），再导数据
mysql -uroot -p'Root@Gongguoge2026' < /root/01_schema.sql
mysql -uroot -p'Root@Gongguoge2026' gongguoge < /root/gongguoge-data.sql

# 核对：应看到你的账号
mysql -uroot -p'Root@Gongguoge2026' -e "SELECT id,phone,nickname FROM gongguoge.t_user;"
```

### 3.5 迁移头像文件（重要，容易漏）

头像存在文件系统里，**不在数据库里**。不迁的话换服务器后头像会全部裂图。

```bash
# 本地执行：把本地上传目录传上去
scp -r backend/uploads/* root@47.114.84.8:/opt/gongguoge/uploads/
```

```bash
# 服务器：修正属主
chown -R gongguoge:gongguoge /opt/gongguoge/uploads
ls -R /opt/gongguoge/uploads | head -20
```

---

## 4. 部署后端

### 4.1 本地打包

```bash
# 本地项目根目录执行
cd backend
mvn clean package -DskipTests
# 产物：backend/target/gongguoge-backend-1.0.0.jar
```

### 4.2 上传 jar

```bash
scp backend/target/gongguoge-backend-1.0.0.jar root@47.114.84.8:/opt/gongguoge/
ssh root@47.114.84.8 "chown gongguoge:gongguoge /opt/gongguoge/gongguoge-backend-1.0.0.jar"
```

### 4.3 生成 JWT 密钥

```bash
# 服务器执行，输出一串随机值，复制备用
openssl rand -base64 48
```

### 4.4 配置 systemd 服务

服务文件里**不含任何密钥**，全部变量从 `/opt/gongguoge/app.env` 读取（该文件只存在于服务器，不进仓库）。

```bash
# 本地执行：上传服务文件（内容通用，无需修改）
scp deploy/gongguoge.service root@47.114.84.8:/etc/systemd/system/
```

```bash
# 服务器执行：创建环境变量文件
# 复制下面整段（含最后的 EOF）一次性粘贴执行，然后把空值填上
cat > /opt/gongguoge/app.env <<'EOF'
MYSQL_HOST=127.0.0.1
MYSQL_PORT=3306
MYSQL_DATABASE=gongguoge
MYSQL_USER=gongguoge
MYSQL_PASSWORD=Gongguoge@Db2026

REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_PASSWORD=

JWT_SECRET=把4.3生成的那串随机值粘到这里
JWT_EXPIRATION=604800

ALIYUN_SMS_ENABLED=true
ALIYUN_SMS_MOCK_CODE=
ALIYUN_SMS_ACCESS_KEY_ID=你的AccessKeyId
ALIYUN_SMS_ACCESS_KEY_SECRET=你的AccessKeySecret
ALIYUN_SMS_SIGN_NAME=速通互联验证码
ALIYUN_SMS_TEMPLATE_CODE=100001
ALIYUN_SMS_ENDPOINT=dypnsapi.aliyuncs.com

UPLOAD_DIR=/opt/gongguoge/uploads
UPLOAD_MAX_SIZE=2MB
EOF

# 收紧权限：只有应用用户能读，防止密钥被其他用户看到
chown gongguoge:gongguoge /opt/gongguoge/app.env
chmod 600 /opt/gongguoge/app.env
```

**必须填真实值的几项**：

| 变量 | 填什么 |
| --- | --- |
| `MYSQL_PASSWORD` | `Gongguoge@Db2026`（3.2 里设的；改了要同步改这里） |
| `JWT_SECRET` | 4.3 生成的那串随机值 |
| `ALIYUN_SMS_ACCESS_KEY_ID` | 你的 AccessKey ID |
| `ALIYUN_SMS_ACCESS_KEY_SECRET` | 你的 AccessKey Secret |

> ⚠️ systemd 的 `EnvironmentFile` **不支持行内注释**，所以值后面不要写注释、也不要加引号。

**关于短信验证码（务必看懂）**：

| 阶段 | 配置 | 效果 |
| --- | --- | --- |
| **先跑通服务** | `ALIYUN_SMS_ENABLED=false` + `ALIYUN_SMS_MOCK_CODE=123456` | 不发短信，你自己装 App 时验证码固定填 `123456`。**真实用户无法登录**，仅供自测 |
| **正式上线** | `ALIYUN_SMS_ENABLED=true` + 填好 AccessKey / 签名 / 模板，且 `MOCK_CODE` **留空** | 真的给用户发短信，用户从短信里拿验证码 |

> 本项目用的是阿里云「**号码认证服务 Dypnsapi**」，不是普通短信服务。
> 短信签名与模板需在控制台先申请并通过审核（模板必须含 `${code}` 与 `${min}` 两个变量，
> 代码里传的正是这两个参数）。

> 改动 `app.env` 后需要 `systemctl restart gongguoge` 才生效。

### 4.5 启动并验证

```bash
# 服务器执行
systemctl daemon-reload
systemctl enable --now gongguoge

# 看启动日志（出现 Started GongguogeApplication 即成功）
journalctl -u gongguoge -f
```

**另开一个终端验证接口**：

```bash
# 期望返回 {"code":401,"message":"未登录","data":null} —— 说明服务正常且鉴权生效
curl http://127.0.0.1:8080/api/records/today
```

**从本机验证外网可达**：

```bash
# 本地 PowerShell
curl.exe http://47.114.84.8:8080/api/records/today
```

能返回 401 就说明「服务器 → 外网」这条路通了。

> ⚠️ 如果启动失败，先看日志第一段异常。两种最常见：
> - `Could not resolve placeholder 'JWT_SECRET'` → systemd 里没设 JWT_SECRET
> - `短信服务未启用…且未配置 mock 验证码` → 短信两个变量都没填（这是防呆，不是 bug）

---

## 5. 配置 Nginx 反向代理（可选但推荐）

好处：用户访问 80/443，后端 8080 可以只监听内网，更安全；后面加 HTTPS 也方便。

### 5.1 安装

```bash
yum install -y nginx
systemctl enable --now nginx
```

### 5.2 配置

```bash
scp deploy/nginx-gongguoge.conf root@47.114.84.8:/etc/nginx/conf.d/
```

```bash
# 服务器：编辑配置
vi /etc/nginx/conf.d/gongguoge.conf
```

- **有域名**：用文件里的 A 段，把 `api.example.com` 换成你的域名
- **只有 IP**：把 A 段整段注释掉，取消 B 段的注释

```bash
nginx -t && systemctl reload nginx
curl http://47.114.84.8/api/records/today   # 期望 401
```

### 5.3 申请 HTTPS 证书（有域名时）

```bash
yum install -y certbot python2-certbot-nginx
certbot --nginx -d api.example.com
# 按提示填邮箱，选自动跳转 HTTPS
systemctl status certbot-renew.timer   # 确认自动续期已开启
```

---

## 6. 前端配置与打包

### 6.1 确认后端地址

`frontend/utils/config.js` 里已经内置两套地址，靠一个开关切换：

```js
// 云服务器（正式使用）—— 已填好你的公网 IP
export const PROD_HOST = '47.114.84.8'
export const PROD_PORT = 8080
export const PROD_HTTPS = false

// 本机局域网（本地联调）
export const LOCAL_HOST = '10.201.65.43'
export const LOCAL_PORT = 8080

// 打包正式版给用户用：true；本地联调后端：false
const USE_PROD = true
```

**打包给用户时确认 `USE_PROD = true`** 即可，不用改别的地方。

以后配了域名 + HTTPS，改成：

```js
export const PROD_HOST = 'api.你的域名.com'
export const PROD_PORT = 443
export const PROD_HTTPS = true
```

> 若服务器重开导致公网 IP 变化，改 `PROD_HOST` 后重新打包。
> 若你改用 Nginx（第 5 步）且没有域名，把 `PROD_PORT` 改成 `80`、`PROD_HOST` 保持 IP 不变。

### 6.2 重新打包 App

HBuilderX 打开 `frontend` 目录 → **发行 → 原生App-云打包** → Android(apk) → 使用云端证书 → 打包。

装到手机后，**用 4G 流量**（不连家里 WiFi）测试，能登录就说明真的走了公网。

---

## 7. 验收清单

部署完逐项确认：

- [ ] `systemctl status gongguoge` 是 active (running)
- [ ] `curl http://127.0.0.1:8080/api/records/today` 返回 401
- [ ] 本机 `curl http://<公网IP>:8080/api/records/today` 也返回 401（外网可达）
- [ ] App 用**手机流量**能登录
- [ ] 记一条 → 首页出现
- [ ] 头像能上传并显示（验证 `/opt/gongguoge/uploads` 有文件）
- [ ] 统计页趋势图正常
- [ ] 开飞行模式记 3 条 → 关飞行模式 → 「待同步」自动消失
- [ ] 提醒设 2 分钟后 → 切后台 → 能收到
- [ ] 导出 CSV 正常

### 上线前最后检查（安全）

- [ ] `JWT_SECRET` 已改成随机值（**不是**默认或示例值）
- [ ] `ALIYUN_SMS_ENABLED=true` 且真实密钥已填（否则**用户无法登录**）
- [ ] `ALIYUN_SMS_MOCK_CODE` 已删除或留空
- [ ] MySQL root 密码已改（不是临时密码）
- [ ] 安全组里 8080 已关闭（走 Nginx 时）
- [ ] Redis 只监听 127.0.0.1（脚本已设）
- [ ] 数据库已配定时备份

### 数据库每日备份（建议）

```bash
cat > /etc/cron.daily/gongguoge-backup <<'EOF'
#!/bin/bash
BACKUP_DIR=/opt/backup
mkdir -p $BACKUP_DIR
mysqldump -uroot -p'Root@Gongguoge2026' gongguoge | gzip > $BACKUP_DIR/gongguoge-$(date +%F).sql.gz
# 只保留最近 7 天
find $BACKUP_DIR -name 'gongguoge-*.sql.gz' -mtime +7 -delete
EOF
chmod +x /etc/cron.daily/gongguoge-backup
```

---

## 常见问题

| 现象 | 原因与处理 |
| --- | --- |
| `yum` 报 `Could not resolve host: mirrorlist.centos.org` | CentOS 7 已 EOL，执行脚本第 0 步的 sed 命令换源 |
| 后端启动报 `Could not resolve placeholder 'JWT_SECRET'` | systemd 里没设 JWT_SECRET |
| 后端启动报 `短信服务未启用…未配置 mock 验证码` | 短信两个变量都没填；自测就填 `ALIYUN_SMS_MOCK_CODE=123456` |
| App 所有接口失败 | config.js 地址没改 / 安全组没放行 / 手机在别的网络 |
| 头像能传但看不到 | `/opt/gongguoge/uploads` 属主不是 `gongguoge`，或没迁移旧头像 |
| MySQL 8 装完连不上 | 服务没起来：`systemctl status mysqld`；或密码里特殊字符没加引号 |
| 用户收不到验证码 | `ALIYUN_SMS_ENABLED` 还是 false，或签名/模板审核没过 |
| 脚本在 Linux 报 `\r: command not found` | Windows 换行符问题：`sed -i 's/\r$//' 脚本名` |
