# 开发上手

所有命令在仓库根目录执行。首次工作先读[根工作规则](../../AGENTS.md)，用 `git status --short` 确认已有变更。

## 环境与首次检查

1. 普通 `git clone` 即可；API 源码已内联，无需初始化子模块，来源与版本见[附属 API](../ADDON_API.zh-CN.md#独立库与-sdk-构建)。
2. 准备 JDK 17 与 21。core 和 Forge 节点使用 17，NeoForge 节点使用 21；`JAVA_HOME` 决定启动 Gradle 的 JVM，`JAVA17_HOME`、`JAVA21_HOME` 用于发现工具链。
3. 使用仓库 Wrapper：Linux/macOS 为 `./gradlew`，Windows PowerShell 为 `.\gradlew.bat`。当前 Wrapper 为 Gradle 8.8，Stonecutter 为 0.7.11。
4. 首次运行需要下载 Gradle、插件及游戏依赖。执行 `./gradlew --version`，按需用 `./gradlew projects` 和目标节点的 `tasks --all` 查询任务。

配置依据：[settings.gradle](../../settings.gradle)、[Wrapper](../../gradle/wrapper/gradle-wrapper.properties)、[工具链属性](../../gradle.properties)、[CI 环境](../../.github/actions/setup-build/action.yml)。

`gradle.properties` 当前没有代理配置。个人代理或 JDK 路径放用户级 `~/.gradle/gradle.properties`，或使用命令行覆盖。`local.properties` 虽被忽略，本项目没有通用自动读取它的逻辑。

## 选择构建与检查

| 目的 | 命令与说明 |
| --- | --- |
| 编译两个平台 | [compile](validation/checks/compile.md#运行命令) |
| 生成发行模组、公共 API 库或 SDK | [assemble](validation/checks/assemble.md#运行命令)；产物区别见该页[产物位置](validation/checks/assemble.md#产物位置) |
| 运行公共 API、core、buildtools 或平台 API 单元测试 | [junit](validation/checks/junit.md#运行命令) |
| 检查外部消费者看到的 API 契约 | [api-contracts](validation/checks/api-contracts.md#运行命令) |
| 格式检查与格式化 | [format](validation/checks/format.md) |
| 启动开发客户端、服务端或可选集成 | [manual-ingame](validation/checks/manual-ingame.md#运行命令) |
| 运行 Minecraft 场景测试 | [gametest](validation/checks/gametest.md#运行命令) |

改动后的必需检查按[验证与验收](validation/README.md)选择。全仓检查可用 `./gradlew check`，但不包含 GameTest；单节点 `build` 同样不能代替游戏验收。

根 `compileJava`、`runClient`、`runServer` 等兼容入口委托给 Forge 节点。开发运行和单平台检查应明确节点，任务关系见 [stonecutter-native.gradle](../../gradle/stonecutter-native.gradle)。

## 常见排错顺序

先看首个实际失败原因：工具链、代理、依赖解析、源码编译、Mixin 或运行期异常。保留失败命令及必要输出于 `work/<任务名>/`，修复共享输入后重跑受影响检查。不要先删全部缓存、反复全量 clean，或从生成副本修复问题。

Forge `runData` 会写源码资源，使用前读[资源指南](../architecture/resources.md#数据生成与打包)。
