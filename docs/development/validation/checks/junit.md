# junit：JUnit 单元测试

## 验证方式

JUnit 5 测试覆盖不依赖 Minecraft 运行时的规则与静态数据：公共 API 契约消费者、配方 JSON 与目录表、材料与成分、能源与流体计算。它证明“纯规则在 JVM 内成立”，不证明游戏内加载、注册或世界行为。

| 模块 | 测试位置 | 负责 |
| --- | --- | --- |
| `:api` | `api/common/src/test` | 公共契约、能源、机器、材料、多方块、附属声明 |
| `:api-platform` | `api/minecraft/src/test` | 含游戏类型的 API 契约与平台适配 |
| `:core` | `core/src/test` | 配方表、材料事实、流体与配方引擎等平台无关逻辑 |
| `:buildtools` | `tools/buildtools/src/test` | 1.21.1 配方与标签转换（构建输入，不随模组发布） |

core 测试不得引入 Minecraft、Forge、NeoForge 类型。API 测试优先使用打包后的 API JAR，不借用 main 或 core 输出。

## 验证原理

### 配方静态案例表

`core/src/test/resources/gregtech/crafting-issue-static-cases.tsv`，5 列：

```text
issue  source  kind  target  expected
```

- `source`：`json`、`vanilla`、`form`、`mold`。
- `kind`：JSON 用 `shaped`/`shapeless`/`custom`；vanilla 用 `row`；form 用 `conversion`；mold 用 `form`。
- `target`：JSON 相对路径、vanilla row id、`input->output` 或 `prefix/family`。
- `expected`：分号分隔的 `key=value`，例如 `type=...;pattern=...;key:B=tag:...;result=...;count=...`。测试会拒绝未知键，并检查该 kind 的必需键。

新增案例只改表；不要在 Java 里散落逐条断言，也不要扫描整个配方目录。

## 注意事项

- 按修改范围运行对应模块，不要只跑一个空测试任务来证明覆盖。
- `:minecraft:<node>:test` 存在，但 GameTest 没有接入该任务；运行时行为必须使用 [gametest](gametest.md) 的 `runGameTestServer`。
- core JUnit 随 `:core:check` 生命周期执行。
- 配方根目录由 `core/build.gradle` 的 `gt6ce.recipeRoot` 系统属性提供；从 IDE 直接运行时回退到 `core/src/main/resources/data/gt6ce/recipes`。
- 不为通过而放宽断言、跳过测试或把测试改为空实现。

## 运行命令

```bash
./gradlew :api:test
./gradlew :core:test
./gradlew :buildtools:test
./gradlew :api-platform:1.20.1-forge:test
./gradlew :api-platform:1.21.1-neoforge:test

# 单个类或包
./gradlew :core:test --tests "gregtech.engine.recipe.*"
./gradlew :api:test --tests "gregtech.api.material.GTMaterialTest"
```

JUnit XML 在 `<module>/build/test-results/test/TEST-*.xml`，HTML 报告在 `<module>/build/reports/tests/test/`。

材料基线入口为 `gregtech.content.material.MaterialBaselineTest`。它在三个独立 JVM 中区分无附属重复启动和测试 SPI 附属，原始投影写入调试输出目录；不使用生产 reset。源表引用的逐行历史依据在 `core/src/test/resources/gregtech/material-reference-history.tsv`；悬空引用与 ID/name 矛盾必须为零，不能用快照允许列表认可错误。历史分类必须核对来源，不能自动重录。材料系统设计、周期与权威口径见[材料系统设计](../../../decisions/material-system/README.md)。
