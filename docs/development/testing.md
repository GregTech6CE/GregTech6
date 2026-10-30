# 测试与验证

本仓库的自动测试是常驻回归；测试目录不使用 `.gitignore` 的 transient 规则。一次性验证放 `work/<任务名>/`（已忽略）或手动删除，不通过忽略测试目录来管理。

当前接入状态：`core` JUnit 与 GameTest 最小接线已接入；两个节点的 `runGameTestServer` 可运行。GameTest 案例表尚未建立，新增运行时案例时按下方 schema 创建。

## 分层与边界

| 层 | 位置 | 负责 |
| --- | --- | --- |
| core JUnit | `core/src/test` | 不需要 Minecraft 的静态回归：随 jar 发布的配方 JSON、core 目录表与生成行 |
| GameTest | `minecraft/src/test` | 运行时回归：配方加载、`RecipeManager` 查找、合成网格匹配与产出、tag 成员 |

core 测试不得引入 Minecraft、Forge、NeoForge 类型。GameTest 共享测试放 `minecraft/src/test`；只有 loader API 差异才放 `minecraft/versions/<node>/src/test`。

## 运行

### core JUnit

```bash
./gradlew :core:test
./gradlew :core:check        # test + spotless
```

测试结果在 `core/build/test-results/test/*.xml`。配方根目录由 `core/build.gradle` 的 `gt6ce.recipeRoot` 系统属性提供；从 IDE 直接运行时回退到 `core/src/main/resources/data/gt6ce/recipes`。

### GameTest

以下命令在对应节点可用；案例表和模板按下方位置维护。

```bash
./gradlew :minecraft:1.20.1-forge:runGameTestServer
./gradlew :minecraft:1.21.1-neoforge:runGameTestServer
```

GameTest 不接入 `check` 或 CI；`check` 只编译测试源集。首次运行会下载游戏资源，失败时先区分环境问题与断言失败。EULA 由运行环境处理，不把 `eula.txt` 提交到仓库。

## 案例表

针对性回归用数据表维护，不为每个 issue 新建测试类、namespace 或结构模板。

### core 静态子集

`core/src/test/resources/gregtech/crafting-issue-static-cases.tsv`，5 列：

```text
issue  source  kind  target  expected
```

- `source`：`json`、`vanilla`、`form`、`mold`。
- `kind`：JSON 用 `shaped`/`shapeless`/`custom`；vanilla 用 `row`；form 用 `conversion`；mold 用 `form`。
- `target`：JSON 相对路径、vanilla row id、`input->output` 或 `prefix/family`。
- `expected`：分号分隔的 `key=value`，例如 `type=...;pattern=...;key:B=tag:...;result=...;count=...`。测试会拒绝未知键，并检查该 kind 的必需键。

新增案例只改表；不要在 Java 里散落逐条断言，也不要扫描整个配方目录。

### GameTest 全量表

GameTest 案例表路径为 `minecraft/src/test/resources/gregtech/gametest/crafting-issue-cases.tsv`；当前仅有最小接线，尚未建立该文件，新增案例时按以下列创建：

```text
issue  recipe  level  expected_type  result_item  result_count  setup  notes
```

断言级别：

- `R`：运行时匹配/产出，按 `setup` 构造网格并断言 `matches`、`assemble`。
- `S`：存在 + pattern/ingredients/result 结构，不依赖复杂 GT 工具 NBT。
- `T`：tag 成员，断言指定 tag 包含要求的物品。
- `P`：存在 + 自定义 serializer。

`setup` 只允许少量动作：`fill`、`one:<item>@<slot>/<w>x<h>`、`pattern-first`、`none`。每个 issue 至少一行；#209 等位置相关案例用多行覆盖不同槽位。GameTest 表是运行时权威，core 表是静态子集。

## 结构模板

GameTest 只保留一份 canonical `minecraft/src/test/templates/test_empty.nbt`，由 `minecraft/build.gradle` 的 `processTestResources` 按节点映射到 `data/gt6ce/structures`（1.20.1）或 `data/gt6ce/structure`（1.21.1）。

不要把 NBT 放进 `gradle/`、`src/main/resources` 或每个版本节点各复制一份；单 namespace `gt6ce` 配单模板即可。

## 维护规则

- 所有 `src/test` 默认常驻；不要恢复测试目录的 ignore 规则。
- GameTest 单 namespace `gt6ce`、单结构模板、单测试文件；用例用数据表扩展。
- 不新增 per-issue JavaExec 任务；不为每个 issue 建类、namespace 或 NBT。
- 平台脚本只保留 `gameTestServer` 所需的最小接线，不改现有 mods 注入。
- 生产 `jar`/`distributionJar` 不得包含测试类、NBT 或 TSV。
- CI 不新增 GameTest job；core JUnit 随现有 `check` 生命周期执行。

## 故障排查

- Spotless 失败：`./gradlew :core:spotlessApply` 后重跑 `:core:check`。
- core 测试找不到配方：确认 `gt6ce.recipeRoot` 已设置，或从 `core` 目录运行。
- GameTest 找不到模板：确认测试类使用 `@GameTestHolder("gt6ce")`、`@PrefixGameTestTemplate(false)`，并检查 NBT 输出路径是 `structures`（1.20.1）还是 `structure`（1.21.1）。
- GameTest 没有运行：确认 `runGameTestServer` 任务存在、`forge.enabledGameTestNamespaces`/`neoforge.enabledGameTestNamespaces` 为 `gt6ce`。
- EULA、代理、资源下载失败：先检查运行环境，不要通过放宽断言绕过。
