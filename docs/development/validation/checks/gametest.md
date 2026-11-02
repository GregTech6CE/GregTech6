# gametest：GameTest 运行时测试

## 验证方式

GameTest 在真实 Minecraft 世界中验证运行时行为：配方加载、`RecipeManager` 查找、合成网格匹配与产出、tag 成员、`RecipeMap` 注册与删除、机器 tick 与状态、多方块成形等。它补足 JUnit 无法覆盖的游戏对象、注册表与资源加载。

GameTest 不接入 `check` 或 CI，只在本地通过两个节点的 `runGameTestServer` 运行。修改配方加载、匹配、产出与重载，方块实体、机器 tick、输入消耗、输出阻塞与能量状态，多方块成形与结构追踪，或注册表存在性、tag 成员与基础方块交互时执行。

## 验证原理

### 位置

- 共享测试：`minecraft/src/test`，由 Stonecutter 生成到两个节点的 `test` source set。
- 版本专属测试：`minecraft/versions/<node>/src/test`，仅用于加载器 API 差异；目录当前为空或尚未创建。
- 测试类：`gregtech.gametest.CraftingRecipeIssueSmokeTests`（4 个 `@GameTest`）、`gregtech.gametest.RecipeMapRegistrationTests`（8 个 `@GameTest`）、`gregtech.gametest.ItemPipeRoutingTests`（7 个 `@GameTest`）与 `gregtech.gametest.TransportSchedulingTests`（4 个 `@GameTest`）。
- 案例表：`minecraft/src/test/resources/gregtech/gametest/crafting-issue-cases.tsv`。
- 结构模板：`minecraft/src/test/templates/test_empty.nbt`。

### 配方回归案例表

针对性回归用数据表维护，不为每个 issue 新建测试类、namespace 或结构模板。GameTest 表是运行时权威；core 静态子集由 JUnit 检查维护。

引擎契约不属于某个配方 issue，例如 `RecipeMap` 的注册、删除与碰撞判定，直接放在 `minecraft/src/test` 的独立测试类里，参考 `RecipeMapRegistrationTests`。

物品管道的 `ItemPipeRoutingTests` 覆盖最近出口饱和后续搜、逐路径带宽、库存更换与提前唤醒、含水状态和普通盖板不重建、取回器与 I/O 失效、拆管恢复及卸载回调保护。夹具使用竖直柱避免相邻测试互相连接；卸载用例只验证回调使快照失效，真实跨区块卸载与重新加载仍需游戏内确认。

#### 全量表

`minecraft/src/test/resources/gregtech/gametest/crafting-issue-cases.tsv`。每行一个 issue 回归案例，同一 issue 可以有多行；`level` 列决定该行的断言方式。单输入形态转换中，单 variant 在任意槽匹配；多 variant 按完整网格的槽位序号（从 0 开始，行优先）取模选择输出（见 `setup`）；多放物品的网格不匹配。多物品配方由 pattern/相对位置区分输出，不适用这条规则。

表头：

| 列 | 用途 |
| --- | --- |
| `issue` | issue 编号；失败信息和覆盖检查使用。 |
| `recipe` | 配方 id、`prefix:<path>` 或 tag id。 |
| `level` | 断言级别 R/S/T/P；决定其余字段如何使用。 |
| `expected_type` | 期望的 recipe serializer id。 |
| `expected_pattern` | S 行的形状掩码。 |
| `result_item` | 期望的结果物品 id；T 行可用 `*`。 |
| `result_count` | 期望的结果数量。 |
| `setup` | R 行的网格构造动作。 |
| `notes` | 自由文本，测试不解析。 |

空单元格写 `-`。

**`issue`**

填 issue 编号（如 `176`）。每个 issue 至少一行；失败信息会带上 `#issue recipe`。新增一个此前未覆盖的 issue 编号时，要同步更新测试类的 `FIXED_ISSUES`；给已有 issue 追加行只需改表。

**`recipe`**

- 精确 id：如 `minecraft:paper`、`gt6ce:tool_blocks/sap_bag`。
- `prefix:<path>`：同前缀的配方族，如 `prefix:hand_material/craft/rotor/`。测试只扫描 `gt6ce` 命名空间，按 `RecipeManager` 迭代顺序取第一个 serializer、形状和 ingredients 都匹配的配方；需要稳定选择时写精确 id。适合一个 issue 对应整族材料变体、表里只放代表行的情况。
- T 行：填 tag id，如 `minecraft:stone_crafting_materials`。
- 位置敏感的多物品配方：写精确 recipe id；同一组输入的不同相对位置是不同 recipe。

**`level`**

| 级别 | 断言内容 | 使用字段 |
| --- | --- | --- |
| `R` | 构造网格，断言 `matches` 和 `assemble` | `recipe`、`expected_type`、`setup`、`result_item`、`result_count` |
| `S` | 断言 serializer、形状和结果结构 | `recipe`、`expected_type`、`expected_pattern`、`result_item`、`result_count` |
| `T` | 断言 tag 包含指定物品 | `recipe`、`result_item` |
| `P` | 断言配方存在且 serializer 匹配 | `recipe`、`expected_type` |

**`expected_type`**

填 `BuiltInRegistries.RECIPE_SERIALIZER` 的 key，如 `gt6ce:tool_shaped`、`minecraft:crafting_shapeless`、`gt6ce:tool_repair`。T 行留空；其他行留空时测试跳过 serializer 检查，R/S/P 案例应填写。

**`expected_pattern`**

仅 S 行使用。`#` 表示非空槽，`_` 表示空槽，`/` 分行：

- `###/###/###`：3×3 全满；
- `#_/_#`：2×2 的左上和右下有物品；
- `#/#`：1×2 的两格都有物品。

shapeless 配方的形状留空。R/T/P 行留空。

**`result_item`**

期望的结果物品 id，如 `minecraft:stick`、`gt6ce:sap_bag`。T 行必须填具体物品或 `*`（表示“任意 gt6ce 物品”）；P 行忽略该字段。R 行的形态转换可以留空：此时只断言结果非空和 `result_count`。S 行的前缀配方也可以留空：只断言结果非空。

**`result_count`**

期望的结果数量，如 `9`、`36`、`4`。T/P 行不使用该字段；不需要断言时留空；R 行的 `reject@` 用法留空。

**`setup`**

仅 R 行使用；其他级别填 `none`。

| setup | 含义 |
| --- | --- |
| `fill` | 按配方自身的 ingredients 填网格；shaped 按 width×height pattern（多物品相对位置由 pattern 决定），shapeless 用 3×3 网格按列表顺序填入。 |
| `one@<slot>/<W>x<H>` | 仅用于单输入形态转换；在 `<W>×<H>` 网格的第 `<slot>` 槽放一个物品，物品取自配方的第一个可用 ingredient。例如 `one@4/3x3` 是 3×3 中心。 |
| `reject@<slot>[,<slot>]/<W>x<H>` | 仅用于单输入形态转换；把同一物品放入列出的多个槽，断言配方不匹配。例如 `reject@0,1/2x2`。 |
| `none` | 不构造网格；S/T/P 行使用。 |

多 variant 形态转换的 `one@` 槽位要按 GT6 源数据的取模规则选择输出：例如 `blockIngot → ingot` 与 `blockIngot → chunkGt` 在槽位序号为偶数/奇数时切换。这里的序号是完整网格从 0 开始、按行优先的槽位序号：2×2 为 0–3，3×3 为 0–8。

多物品配方（例如工具与电路的不同相对位置）不适用这条取模规则；它们由 pattern/相对位置区分输出。此类案例写精确 recipe id，用 `fill` 按该配方的 pattern 填格，不要用 `one@`/`reject@`。当前 `setup` 只支持单输入的 `one@`/`reject@` 和按 pattern 的 `fill`；需要任意多物品槽位排布时，先扩展 `setup`。

**`notes`**

自由文本，记录该行针对的 issue 行为；测试不读取。

### 结构模板

GameTest 只保留一份 canonical 模板 `minecraft/src/test/templates/test_empty.nbt`，由 `minecraft/build.gradle` 的 `processTestResources` 按节点映射到 `data/gt6ce/structures`（1.20.1）或 `data/gt6ce/structure`（1.21.1）。

不要把 NBT 放进 `gradle/`、`src/main/resources` 或每个版本节点各复制一份；单 namespace `gt6ce` 配单模板即可。复杂结构可以在测试代码中用 `setBlock` 和 helper 搭建，只有确实需要复用的复杂布局才增加模板。

### 维护规则

- 所有 `src/test` 默认常驻；不要恢复测试目录的 ignore 规则。
- GameTest 保持单 namespace `gt6ce`、单结构模板；测试类保持少量，用例优先用数据表扩展。
- 不新增 per-issue JavaExec 任务；不为每个 issue 建类、namespace 或 NBT。
- 平台脚本只保留 `gameTestServer` 所需的最小接线，不改现有 mods 注入。
- 生产 `jar`/`distributionJar` 不得包含测试类、NBT 或 TSV。
- 共享测试放 `minecraft/src/test`；只有 loader API 差异才放版本节点。

## 注意事项

- 两个节点分别运行；只运行一个节点时，明确写出未覆盖节点。
- 首次运行会下载游戏资源；EULA 由运行环境处理，不把 `eula.txt` 提交到仓库。
- 不放宽断言或跳过用例来绕过失败。
- 运行目录与堆参数按需指定，完整输出放 `work/<任务名>/`。

## 运行命令

```bash
./gradlew :minecraft:1.20.1-forge:runGameTestServer
./gradlew :minecraft:1.21.1-neoforge:runGameTestServer

# 指定堆与运行目录
./gradlew :minecraft:1.20.1-forge:runGameTestServer -PgameTestHeap=4g -PgameTestDirectory=../../../work/gametest-forge
./gradlew :minecraft:1.21.1-neoforge:runGameTestServer -PgameTestHeap=4g -PgameTestDirectory=../../../work/gametest-neoforge
```

`-PgameTestDirectory` 由 Gradle 按节点项目目录解析；要放到仓库根 `work/` 时使用上面的 `../../../work/...`。
