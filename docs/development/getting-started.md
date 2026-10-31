# 开发上手

所有命令在仓库根目录执行。下面的命令来自当前 Gradle 配置，是否在某台机器成功必须以该次输出为准。

## 环境与首次检查

1. 阅读[根工作规则](../../AGENTS.md)，运行 `git status --short`，确认已有变更。
   API 源码已内联在 `api/`（GregTech6API a9c879176），普通 `git clone` 即可，不需要子模块初始化。
2. 准备 JDK 17 与 21。`core` 和 Forge 节点使用 17，NeoForge 节点使用 21；`JAVA_HOME` 决定启动 Gradle 的 JVM，`JAVA17_HOME`、`JAVA21_HOME` 被项目配置用于发现工具链。
3. 使用仓库 Wrapper：Linux/macOS 为 `./gradlew`，Windows PowerShell 为 `.\gradlew.bat`。当前 Wrapper 指向 Gradle 8.8，Stonecutter 为 0.7.11，以配置文件为准。
4. 初次运行需要下载 Gradle、插件及游戏依赖。先执行 `./gradlew --version`，再按需查询 `./gradlew projects` 和目标节点的 `tasks --all`。

版本依据见 [settings.gradle](../../settings.gradle)、[Wrapper 配置](../../gradle/wrapper/gradle-wrapper.properties)、[工具链属性](../../gradle.properties)及 [CI 环境配置](../../.github/actions/setup-build/action.yml)。

`gradle.properties` 当前带有 localhost 代理设置。连接失败先核实本机代理与网络，不要将本机代理、JDK 绝对路径或密码写入共享配置。个人配置优先放用户级 `~/.gradle/gradle.properties` 或使用命令行覆盖；`local.properties` 虽被忽略，但本项目没有通用自动读取它的逻辑。

## 构建目标

| 目的 | 命令 |
| --- | --- |
| 公共 API 检查与库产物 | `./gradlew :api:check :api:assemble` |
| core 检查 | `./gradlew :core:check` |
| 两个平台编译 | `./gradlew compileBothPlatforms` |
| Forge 发行 JAR | `./gradlew :minecraft:1.20.1-forge:distributionJar` |
| NeoForge 发行 JAR | `./gradlew :minecraft:1.21.1-neoforge:jar` |
| 两个平台组装 | `./gradlew assembleBothPlatforms` |
| 全仓检查 | `./gradlew check` |
| 代码格式化（core、共享与节点源码） | `./gradlew format` |
| 单节点完整构建，与 CI 一致 | `./gradlew :minecraft:1.20.1-forge:build` 或 `./gradlew :minecraft:1.21.1-neoforge:build` |

`format` 使用 Spotless + Palantir Java Format（4 空格、120 列）。`api/` 内联源码、`**/generated/**` 与带 Stonecutter `//?` 标记的共享文件不在本轮范围内；后两者需要按节点处理，不要直接套用该任务。`spotlessCheck` 已接入各项目的 `check`。

发行文件在 `minecraft/versions/<version-loader>/build/libs/`。Forge `jar` 输出到 `build/forge-intermediates/`，最终交付使用 `distributionJar`；两平台发行包均携带 core、公共 API 和游戏 API。独立公共库位于 `api/common/build/libs/`，平台 SDK 位于节点 `build/addon-sdk/`，使用方式见[附属 API](../ADDON_API.zh-CN.md)。不要把 API 库、SDK、sources 或 javadoc JAR 当模组安装。

根 `compileJava`、`runClient`、`runServer` 等兼容入口委托给 Forge 节点，并不表示同时处理两个平台。任务关系见 [stonecutter-native.gradle](../../gradle/stonecutter-native.gradle)。

## 开发运行与可选集成

```bash
./gradlew :minecraft:1.20.1-forge:runClient
./gradlew :minecraft:1.21.1-neoforge:runClient
./gradlew :minecraft:1.21.1-neoforge:runServer
```

Forge 默认实例在节点内 `run/`，NeoForge 客户端/服务端在 `run-client/`、`run-server/`。都属于本机数据，不提交存档或配置；测试服务端 EULA 由操作者自行处理。

当前配置提供 `-PkubejsRuntime=true`、`-PcompatRuntime=ie|mek|ae|pr|hc`（每次选择一个实际值）、`-PemiSmoke=true` 与 `-PemiOnlySmoke=true` 等开发开关，具体依赖以 [Forge 适配](../../gradle/platform-forge.gradle)和 [NeoForge 适配](../../gradle/platform-neoforge.gradle)为准。开关仅装载开发运行依赖，不代表集成已通过验证。

`-PdirectCoreClasspath=true`、`-PdirectCoreResources=true` 是本地运行优化；常规打包验收保持默认配置。Forge `runData` 会写源码资源，先读[资源指南](../architecture/resources.md)。

## 常见排错顺序

先看首个实际失败原因：工具链、代理、依赖解析、源码编译、Mixin 或运行期异常。保留失败命令及必要输出于 `work/<任务名>/`，修复共享输入后重跑受影响检查。不要先删全部缓存、反复全量 clean，或从生成副本修复问题。API 的 `apiContracts` 与 core 的 JUnit 回归使用 `src/test/` 中的定向检查并接入各自 `check`；针对性 GameTest 使用 `minecraft/src/test`，通过两个节点的 `runGameTestServer` 运行。不能把其他空测试任务当覆盖证明。
