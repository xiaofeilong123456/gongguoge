// 后端服务地址配置
//
// 这是真机调试最容易踩的坑：
//   「localhost」只在**电脑上的浏览器**里指向电脑。
//   手机、模拟器、小程序里的 localhost 指向**手机自己**，所以必须用电脑/服务器的地址。
//
// 各环境说明：
//   - 电脑浏览器调试：本文件无需改动（H5 会跟随地址栏主机名）
//   - 手机连同一 WiFi 调试：用下面的 LOCAL_*（电脑局域网 IP）
//   - 已部署到云服务器：用下面的 PROD_*（公网 IP 或域名）
//   - 微信小程序真机：除本文件外，还需在开发者工具里勾选
//     「详情 → 本地设置 → 不校验合法域名、web-view、TLS 版本以及 HTTPS 证书」

// ========== 云服务器（已部署，正式使用）==========
// 阿里云 ECS 公网 IP，后端直连 8080，安全组需放行 8080
// 以后若配了域名 + Nginx，改成：
//   PROD_HOST = 'api.你的域名.com'，PROD_PORT = 443，PROD_HTTPS = true
export const PROD_HOST = '47.114.84.8'
export const PROD_PORT = 8080
export const PROD_HTTPS = false

// ========== 本机局域网（本地联调）==========
// 电脑的局域网 IPv4 地址：CMD 执行 ipconfig，找 WLAN 网卡的 IPv4
// ⚠️ 换 WiFi 或路由器重新分配后这个 IP 会变，届时需要同步修改
export const LOCAL_HOST = '10.201.65.43'
export const LOCAL_PORT = 8080

// ========== 切换使用哪一套 ==========
// 打包正式版（给用户用 / 上架）：true
// 本地联调后端：false
const USE_PROD = true

const CHOSEN = USE_PROD
  ? { host: PROD_HOST, port: PROD_PORT, https: PROD_HTTPS }
  : { host: LOCAL_HOST, port: LOCAL_PORT, https: false }

export const SERVER_HOST = CHOSEN.host

// 后端端口（与 backend/src/main/resources/application.yml 的 server.port 一致）
export const SERVER_PORT = CHOSEN.port

// 是否为 HTTPS（上线到域名且配了证书时改为 true）
export const SERVER_HTTPS = CHOSEN.https
