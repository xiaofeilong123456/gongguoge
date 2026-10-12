# 功过格 · 接口测试

## 端到端测试

`e2e-test.mjs` 会对**运行中的后端**发起真实 HTTP 请求，覆盖认证、记录、统计、回收站、离线同步等主流程（30 个用例）。

`sync-test.mjs` 是离线同步专项测试，覆盖 `create` / `update` / `delete` / `purge` 与幂等语义（16 个用例）。

两者只依赖 Node 18+（内置 `fetch`），无需安装任何依赖。

### 前置条件

1. **MySQL 已启动**，且已执行 `database/init/01_schema.sql` 建库建表。
2. **Redis 已启动**（登录态与短信验证码依赖它，未启动时注册/登录会失败）。
3. **后端已启动**（默认 `http://localhost:8080`）。
4. 建议以短信 mock 模式启动，这样验证码固定为 `123456`：

   ```powershell
   $env:MYSQL_PASSWORD="你的密码"
   $env:ALIYUN_SMS_ENABLED="false"
   mvn spring-boot:run
   ```

### 运行

```bash
# 端到端（默认地址 http://127.0.0.1:8080/api）
node scripts/e2e-test.mjs

# 离线同步专项
node scripts/sync-test.mjs

# 自定义地址
API_BASE=http://127.0.0.1:8080/api node scripts/e2e-test.mjs
```

两个脚本失败时都会打印失败项并以非零状态码退出，便于接入 CI。

### 覆盖范围

**e2e-test.mjs**

| 分类 | 用例 |
| --- | --- |
| 认证 | 发送验证码、mock 验证码登录、场景隔离（register 的码不能用于 login）、1 分钟限流、验证码防重放、退出登录 |
| 鉴权 | 无 token 访问受保护接口被拒（401） |
| 用户 | 获取资料 |
| 分类 | 列表、自定义新增与删除 |
| 记录 | 新增功/过、更新、今日列表、删除进回收站、恢复、彻底删除 |
| 幂等 | 相同 `clientId` 重复提交只写一条且返回同一条记录 |
| 统计 | 汇总、概览、趋势、分类占比 |
| 其他 | 意见反馈、数据导出 |
| 离线同步 | `create`（按 `categoryName` 回退解析分类）、重复提交幂等、`update`、`delete`、`purge`、未知 op 被跳过 |

**sync-test.mjs**

| 用例 | 说明 |
| --- | --- |
| create 成功应用 | 含按 `categoryName` 解析分类 |
| create 幂等 | 同 `clientId` 重复同步只有一条 |
| update 生效 | 内容与类型均变更 |
| update 回退为新增 | 服务端不存在该 `clientId` 时不丢数据 |
| delete 进回收站 | 移出正常列表且出现在回收站 |
| delete 幂等 | 重复删除仍返回成功 |
| purge 彻底删除 | 回收站中也不再存在 |
| 未知 op | 被跳过且不中断整个批次 |
| 混合批次 | update + delete + create 一次提交全部正确 |
| 缺 clientId | 整批被拒（400），属请求格式错误 |
| create 缺 content | 单项被跳过并给出原因 |

---

## 离线同步契约

`POST /api/sync/batch` 接受一组操作，每个操作按 `op` 分支处理：

| op | 语义 | 幂等性 |
| --- | --- | --- |
| `create` | 新增（缺省值） | 已存在同 `clientId` 则视为成功，不重复写入 |
| `update` | 修改类型/分类/内容/日期 | 服务端不存在时**回退为新增**，避免离线数据丢失 |
| `delete` | 删除进回收站（软删） | 已删除或不存在均视为成功 |
| `purge` | 从回收站彻底删除 | 不存在视为成功 |

响应结构：

```json
{
  "code": 200,
  "data": {
    "applied": [{ "clientId": "c_xxx", "op": "create", "serverId": 12 }],
    "skipped": [{ "clientId": "c_yyy", "op": "create", "reason": "记录内容不能为空" }],
    "appliedCount": 1,
    "skippedCount": 1
  }
}
```

- 客户端依据 `applied[].clientId` 清理本地队列；`skipped` 中的条目保留待处理。
- 缺少 `clientId` 属请求格式错误，整批返回 400（由参数校验拦截）。
- `type` / `content` 未加 `@NotBlank`，因为删除操作不带这些字段；由服务层按 `op` 分支校验，不合法项记入 `skipped`。
- 分类解析顺序：`categoryId` → 本人自定义分类同名 → 系统预设分类同名。因此离线时客户端只带 `categoryName` 也能正确落库。

---

## 清理测试数据

```bash
# 预览（非交互环境会自动取消，不会误删）
node scripts/clean-test-data.mjs

# 确认执行
node scripts/clean-test-data.mjs --yes

# 清理指定手机号（用于处理历史遗留数据）
node scripts/clean-test-data.mjs --phone 13912345678,13800000000 --yes
```

### 安全设计（重要）

**本脚本只删除测试脚本自己显式标记的数据，绝不猜测。**

早期版本曾用「昵称为默认值 + 手机号 13x 开头」来推断哪些是测试数据，**结果误删了一个真实用户账号**。现在改为：

1. 测试脚本（`e2e-test.mjs` / `sync-test.mjs`）注册账号时**显式传入昵称 `测试账号`**；
2. 清理脚本只删除昵称**恰好等于** `测试账号` 的账号、这些账号名下的记录，以及内容以「端到端测试」「同步测试」开头的反馈；
3. 需要处理历史遗留数据时，用 `--phone` 显式列出手机号，不做任何推断。

因此**你手工注册的账号（昵称「功过格用户」）永远不会被这个脚本删除**。脚本执行前会打印将删除与将保留的账号数量，需二次确认。

> 如果你自己手工注册账号时把昵称改成了「测试账号」，它会被清理。请避免使用这个昵称。
