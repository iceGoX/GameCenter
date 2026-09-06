# GameCenter 项目约定

## 定位

GameCenter 是供开发者二次开发的 Android WebView 游戏容器 Demo，起因是 [gomoku Issue #1](https://github.com/iceGoX/gomoku/issues/1) 的安卓打包、自定义服务地址和离线游玩诉求。

同一代码提供 `demo` 和 `store` 产品变体。Demo 没有默认服务器；Store 从不提交 Git 的 `store.properties` 注入默认地址。两个版本都允许安装后配置自定义网站。完全离线首启尚未实现，不要把当前版本描述为已完整解决 Issue #1。

## 阅读顺序

先读 `README.md` 了解边界，再读 `docs/DEVELOPMENT.md` 了解配置、部署和验证。

## 代码地图

- `app/src/main/java/com/icego/gamecenter/Game.kt`：游戏标识与展示资源。
- `GameServer.kt`：地址校验、目录规范化与同源导航规则，无 Android 依赖。
- `GameSettings.kt`：设备本地服务地址存储。
- `MainActivity.kt`：大厅与游戏卡片入口。
- `SettingsActivity.kt`：网站设置列表。
- `ServerActivity.kt`：单款游戏的网站编辑与校验反馈。
- `GameActivity.kt`：共用 WebView 容器与生命周期。
- `app/src/main/res/layout/`：XML 布局。
- `app/src/test/java/com/icego/gamecenter/GameServerTest.kt`：地址与导航边界测试。

## 修改原则

- 保持单 app 模块、Kotlin + XML；当前不使用 Lynx。不要为了两个入口引入通用路由框架、仓库层或依赖注入。
- 不在公开源码中硬编码真实域名。文档使用 `example.com`；商店版的默认地址放在已忽略的 `store.properties`，Demo 不得自动使用这些地址。
- 正式 applicationId 为 `com.icego.gamecenter`，Demo 增加 `.demo`。namespace 与 Kotlin 包统一为 `com.icego.gamecenter`。产品用途用 flavor 区分，不能用 debug/release 代替。
- 自定义地址优先于变体默认地址。商店版清空自定义后恢复默认；Demo 清空后未配置。商店版默认地址缺失或非法必须阻止构建，但不影响 Demo 构建。大厅和设置页面的入口仍按当前配置状态引导，Demo 未配置卡片可直接进入编辑页。
- 网页入口与 API 地址不同。Android 加载网页；规则、房间协议、API 路由由上游游戏维护，不在容器内复制。
- 更换网站后，同源判断必须跟随配置，不能另维护固定域名白名单。
- 保留 HTTPS、默认 TLS 校验、禁用文件访问及混合内容的边界。不能为了调试全局放开明文或绕过证书错误。
- 不添加能让任意已配置网页调用 Android 特权能力的通用 JS Bridge。
- 保留退出对局的后果提示、横竖屏处理和 WebView 释放逻辑。区分浏览历史恢复与 JS 对局状态恢复。
- 文档、代码和测试同步更新；不在文件末尾增加空白行。
- 不编译抖音客户端，除非用户明确要求；本项目是独立 GameCenter 工程。
- 不提交密钥、签名文件或环境凭据；未经用户要求不发布 APK、不上架、不发 Issue 评论、不改线上服务器。

## 验证

运行 `./gradlew :app:testDemoDebugUnitTest :app:assembleDemoDebug :app:lintDemoDebug`。另运行 `:app:testStoreDebugUnitTest :app:assembleStoreDebug :app:lintStoreDebug` 验证商店版，需要本地配置。发布 AAB 使用 `:app:bundleStoreRelease`，仍需正式签名。地址规则变化应覆盖非法地址、主机伪装、端口与跨域导航。

报告时区分：静态检查、单元测试、APK 构建、设备验证、后端压测。构建通过不能当作联机、离线或商店审核通过。设备验收步骤见开发指南。
