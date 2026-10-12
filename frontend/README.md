# 功过格前端

UniApp Vue2 工程，使用 HBuilderX 打开。

## 运行到微信开发者工具

1. 安装并登录微信开发者工具，开启“服务端口”。
2. 使用 HBuilderX 打开本目录 `frontend`。
3. 在 [manifest.json](manifest.json) 的 `mp-weixin.appid` 填入你的微信小程序 AppID。
4. 菜单选择：运行 -> 运行到小程序模拟器 -> 微信开发者工具。

## 微信开发者工具报错处理

如果控制台出现：

```text
Error: timeout
at Function.<anonymous> (WAServiceMainContext.js...)
```

这是微信开发者工具内部通信超时，通常不是业务代码导致。按下面方式处理：

1. 在微信开发者工具点击“详情” -> “本地设置” -> “调试基础库”，选择 `2.26.0`。
2. 点击“清缓存” -> “全部清除”，然后重新编译。
3. 在“设置” -> “安全设置”中确认“服务端口”已开启。
4. 如果仍出现且页面正常渲染，可忽略该报错。

## 运行到浏览器

使用 HBuilderX 打开本目录后，菜单选择：运行 -> 运行到浏览器。

## 目录

- `pages/`：页面
- `components/PageNav.vue`：底部导航
- `utils/store.js`：本地数据读写
- `utils/request.js`：后端接口请求封装
