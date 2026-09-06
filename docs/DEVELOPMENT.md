# 开发指南

## 1. 准备环境

用 Android Studio 打开包含 `settings.gradle.kts` 的目录。使用 IDE 自带 JDK，并安装工程要求的 Android SDK（compileSdk 36.1）。Gradle Wrapper 会使用 9.2.1；无需全局安装 Gradle。

本项目只有 `app` 模块，Kotlin 由 AGP 9 的内置支持处理，不必重复添加 Kotlin Android 插件。最低 Android 版本为 API 29。

## 产品变体与默认地址

namespace 和 Kotlin 包为 `com.icego.gamecenter`。产品变体 `demo` 的 applicationId 是 `com.icego.gamecenter.demo`，`store` 是 `com.icego.gamecenter`，两版可共存；数据不会从旧包名 `com.example.gamecenter` 自动迁移。

Android Studio 的 Build Variants 中选择 `demoDebug` 或 `storeDebug` 调试相应版本。`storeDebug` 与 `storeRelease` 包名相同，调试签名与正式签名不同，因此通常不能相互覆盖安装；先卸载会清除应用数据。

```sh
cp store.properties.example store.properties
```

编辑本地文件：

```properties
gomokuUrl=https://games.example.com/wuziqi/
blokusUrl=https://games.example.com/blokus/
```

默认地址须为完整 HTTPS 目录地址并以 `/` 结尾，不含凭据、查询参数或片段。文件不提交 Git，也不用于隐藏域名；真实地址会进入 Store 安装包。CI 可创建临时配置并通过 `-PstoreConfigFile=/path/to/config.properties` 指定路径。

`app/build.gradle.kts` 注入 `BuildConfig.IS_DEMO` 和两个默认 URL。Demo 的默认 URL 永远为空；Store 构建依赖 `validateStoreConfig`，缺失或非法配置时失败，避免发布无法游玩的空配置包。Demo 无需本地文件。

`GameSettings.serverFor` 统一按“有效自定义地址 → 当前游戏的变体默认地址”解析。默认地址不写入 SharedPreferences，因此升级 APK 修改默认地址后，未自定义的用户会随之使用新默认值。保存了自定义地址的用户不受影响。

## 2. 部署自己的游戏

Android 应用不运行 Python。Python 只负责服务器上的在线房间；页面和单机规则由网页执行。

分别获取两个上游项目：

```sh
git clone https://github.com/iceGoX/gomoku.git
git clone https://github.com/iceGoX/blokus.git
```

在两个独立终端启动本地服务，避免端口冲突：

```sh
# 终端一，在 gomoku 目录执行
python3 server/app.py --host 127.0.0.1 --port 4173
```

```sh
# 终端二，在 blokus 目录执行
python3 server/app.py --host 127.0.0.1 --port 4174
```

这些 HTTP 地址用于电脑浏览器联调。Android Demo 只接受 HTTPS；接入手机时，应使用带设备信任证书的 HTTPS 测试域名，通过反向代理连接服务。手机里的 `127.0.0.1` 指手机自身，不能用于访问电脑。不要关闭 TLS 校验或全局打开明文来绕过部署问题。

在准备好的 Linux 服务器上，先安装 Nginx 并创建 `/etc/nginx/sites-available/games.example.com` 站点文件，完成 DNS、HTTPS 证书及站点启用。两个上游安装脚本读取既有站点文件，并查找 `location / {` 区块后插入游戏配置，不是从零建站脚本。确认站点结构符合脚本预期后，分别在对应目录执行：

```sh
sudo python3 deployment/install.py --source . --domain games.example.com
```

安装脚本会修改 Nginx、systemd 和部署目录。先查看上游 `deployment` 目录及当前服务器配置，确认两个站点配置可共存；此命令不承诺自动完成 DNS 和可信 HTTPS 证书配置。

默认路径：

| 项目 | 网页目录 | API 代理路径 | 房间服务 |
| --- | --- | --- | --- |
| gomoku | `/wuziqi/` | `/wuziqi/api/` | `127.0.0.1:8791` |
| blokus | `/blokus/` | `/blokus/api/` | `127.0.0.1:8790` |

保留上游默认子路径最容易接入，尤其不要只改网页目录而漏改 API 路由。检查游戏页面、静态资源、创建房间、加入房间和 SSE 实时同步；代理需正确处理 SSE，不应缓存或缓冲事件流。部署细节以对应上游版本为准。

## 3. 在安装后的 App 中配置

Demo：大厅 → 未配置游戏卡片的“设置网站”（或大厅右上角设置）→ 输入 HTTPS 网页目录 → 保存 → 开始游戏。

Store：初次启动直接使用默认网站；大厅右上角设置 → 选择游戏 → 输入地址 → 保存。编辑页会展示“清空恢复默认”的行为说明。

- 可以分别填不同域名，例如 `https://gomoku.example.com/` 和 `https://blokus.example.com/`，前提是该部署在根目录提供对应游戏。
- 支持 HTTPS 非默认端口，例如 `https://games.example.com:8443/wuziqi/`。
- 忽略输入首尾空白，自动补目录末尾 `/`；入口不接受账号密码、查询参数或 URL 片段。
- 仅填写网页目录，不填写 `game.js`、`index.html` 或 `/api/`。
- 地址保存在本应用的 SharedPreferences；保存后无需重编译。清空并保存会移除该游戏自定义配置；Store 回退到默认服务器，Demo 回到未配置状态。
- 清空地址不会清除原网站的 Cookie、localStorage 或服务器房间；需要结束在线对局时先在网页内退出房间。
- 这只控制应用入口与 WebView 导航，不是对子资源或 API 的网络沙箱；配置的网站仍可发起自身的网络请求，只连接自己信任的网站。

`GameServer` 根据当前游戏的 scheme、host、有效端口判断同源页面跳转；默认 HTTPS 与显式 `:443` 等价。配置的游戏页面、HTTPS 重定向及 HTTP(S) 主页面链接均留在应用内 WebView；其他协议拒绝。没有硬编码的公共站点白名单。

## 4. 修改与扩展

目录结构：

```text
app/src/main/java/com/icego/gamecenter/
  Game.kt           游戏标识、标题和描述
  GameServer.kt     HTTPS 地址解析与同源规则
  GameSettings.kt   每款游戏的本地配置
  MainActivity.kt   大厅与游戏卡片入口
  SettingsActivity.kt 设置列表
  ServerActivity.kt 网站编辑与地址校验反馈
  GameActivity.kt   页面加载、导航与 WebView 生命周期
app/src/main/res/layout/
  activity_main.xml
  item_game.xml
  activity_settings.xml
  item_server.xml
  activity_server.xml
  dialog_exit.xml
  dialog_server.xml
  activity_game.xml
app/src/test/java/com/icego/gamecenter/
  GameServerTest.kt
```

新增一个网页游戏：在 `Game` 枚举增加稳定的标识及字符串资源。大厅自动生成卡片，地址可直接在应用中配置，不必新建 Activity。枚举名称同时作为配置存储键，发布后不要随意重命名；确需重命名时迁移旧配置。

修改棋盘、规则、房间协议：改上游网页或服务端仓库，按其测试流程验证并部署。Android 容器不会同步或编译上游源码；当前加载的是你部署的网站。更新网页后是否立即生效由 HTTP 缓存策略决定。

做自己的应用：按需要修改 `applicationId`、应用名称和图标（当前正式包名已设为 `com.icego.gamecenter`）；若修改 Kotlin 包名，应同步 namespace、目录和 Manifest。签名密钥与密码不要加入源码。

## 5. 离线能力路线

Issue #1 希望“第一次安装、完全断网也能同屏玩”，目前仍需补齐：

1. 固定上游版本，把 HTML、CSS、JS、规则代码与所需数据完整纳入 Android assets，保留来源、版本及授权信息。
2. 用受控的本地资源加载方式（例如 AndroidX WebKit 的 WebViewAssetLoader）提供本地页面，避免启用任意 `file://` 访问。
3. 默认进入完全不发网络请求的单机模式，检查字体、图片及其他外部资源也已内置。Blokus 上游提供 `offline.html`，可作为后续接入素材；本项目当前没有打包该文件。
4. 联机模式选择：打开配置的远端整站（沿用当前方案）；或修改上游前端让本地页面接受独立 API 地址。后一种需同时设计 CORS、来源校验、令牌与 SSE 重连，不能只替换 Android 入口 URL。
5. 清除 App 数据、断网冷启动并完成整局，才算离线验收通过。

该路线不在当前实现内，不要把网络缓存命中当作离线交付。

## 6. 验证

```sh
./gradlew :app:testDemoDebugUnitTest :app:assembleDemoDebug :app:lintDemoDebug
```

报告位置：

- 单元测试：`app/build/reports/tests/testDemoDebugUnitTest/index.html`
- Lint：`app/build/reports/lint-results-demoDebug.html`
- Debug APK：`app/build/outputs/apk/demo/debug/app-demo-debug.apk`

设备验收：

1. 全新安装，无任何默认网站，两个“开始游戏”按钮不可用。
2. 分别配置自己的两个站点；杀进程重开仍显示配置。输入 HTTP、账号密码、非法地址时不保存；空值可以清除。Store 清空自定义地址后应恢复默认，两款游戏的覆盖配置相互独立。
3. 进入五子棋完成本地落子，进入 Blokus 完成触摸预览与拖拽；分别横竖屏旋转、切后台返回。
4. 两台设备连接同一服务，完成创建/加入房间、复制邀请、对弈、断线重连及网页内退出。
5. 测试加载失败与重试；确认 HTTPS 页面跳转仍留在游戏 WebView，其他协议不会被放行。
6. 返回大厅有退出确认。系统回收进程后不承诺恢复 JS 内存中的单机对局；在线恢复取决于上游会话策略。

Store 验证与发布构建：

```sh
./gradlew :app:testStoreDebugUnitTest :app:assembleStoreDebug :app:lintStoreDebug
./gradlew :app:bundleStoreRelease
```

发布构建未配置正式签名；请通过 Android Studio 的 Generate Signed Bundle / APK 或自己的安全签名流程完成。不能使用调试密钥代替正式发布密钥。

`GameDefaultsTest` 在两个变体中分别检查包名与默认地址状态。无配置验证可执行 `./gradlew :app:assembleDemoDebug -PstoreConfigFile=/tmp/nonexistent-gamecenter.properties`（预期成功），再执行对应的 `:app:assembleStoreDebug`（预期在校验阶段失败）；请先确保指定文件确实不存在。

当前地址规则由 JVM 单元测试覆盖；构建或单元测试通过不代表以上设备流程已通过。

## 7. 提交前的隐私检查

仓库只提交源码、资源、通用构建配置和占位配置模板。`.gitignore` 排除 `store.properties` 及其备份、`local.properties`、IDE/Agent 本地设置、Gradle/Kotlin 缓存、构建目录、APK/AAB、日志和常见签名密钥。`store.properties.example` 使用示例域名，可以提交；`gradle.properties` 和 Gradle Wrapper 是可复现构建所需的通用文件，不要在其中写入本机路径、凭据或真实服务地址。

自定义 `-PstoreConfigFile` 建议指向仓库外；若放在仓库内，应使用已忽略的 `store.<环境>.properties` 命名。新增其他私有配置时，先补充忽略规则，再检查 `git status --short`、`git diff --cached` 和 `git ls-files`。不要使用 `git add -f` 强制加入私有文件。`.gitignore` 不会移除已经跟踪的文件，也不会清理历史提交；上传前必须检查实际提交内容。

## 8. 开源 Demo 与商店产品

建议让公开 Demo 保持地址为空，由部署者自行配置。面对普通玩家的商店产品则应有明确可用的默认体验，优先考虑内置离线玩法，再决定是否运营官方联机服务。当前两者通过 productFlavors 共用同一容器，但应分别定义交付标准。

不要把生产域名视为秘密：无论写在源码、配置文件还是 APK 中，都可能被获取。默认移除作者域名的意义是避免所有 Demo 用户自动消耗同一个后端，不是访问控制。

当前两个上游后端使用 Python ThreadingHTTPServer 和进程内房间状态，通过 SSE 同步；源码有房间上限、单 IP 房间上限、请求频率限制和过期清理。代码中的上限不是机器承载量保证，也不能证明已经覆盖所有连接滥用情形。

上线前在自己的测试环境压测，重点观察并发 SSE 连接、线程/文件描述符、CPU、内存、响应延迟、错误率以及断线重连。静态资源可用缓存或 CDN 减少带宽，但不会消除在线房间的长连接和规则计算成本。服务重启会丢失内存房间，不能简单开启多个互不共享状态的实例分流同一房间。

建议先小规模分发，按实测降低连接/房间配额，补齐连接限额、监控与告警后再扩大规模；本次未做服务器压测，不能据此给出支持人数。

上架需要先确定具体商店；Google Play 与国内各渠道要求不同。应准备独立应用标识、正式签名、真实可用的玩法、隐私说明及对应渠道材料。WebView 技术本身不能保证审核通过，空配置的开发 Demo 也不宜直接作为面向普通玩家的成品提交。

参考：[Google Play 政策](https://support.google.com/googleplay/android-developer/answer/16329168)、[Android WebView](https://developer.android.com/develop/ui/views/layout/webapps/webview)。本项目没有自动发布或上架流程。
