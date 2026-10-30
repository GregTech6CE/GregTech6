# 配方缺口对比

当前工具用于发现原版与本端静态机器配方之间的候选差异。它还不是完整行为等价检查器；不能把 `matched` 或 `missing` 直接当作已证明覆盖或实际待新增行数。

配方补齐采用[决策 0001](../decisions/0001-recipe-representation.md)的混合存储路线；完整语义对账、注册身份目录和最终有效配方采集仍待实现，下文区分当前能力与计划要求。

## 基准数据

原版 GT6 的全量配方 dump（95 个 RecipeMap、约 72 万条）在开发机的 CrucibleCraft 仓库里，不在本仓库版本控制内：

```
<CrucibleCraft>/gt6_dump/gt6_recipe_dump/
```

结构：`index.json`（图清单）+ `maps/<map>.json`（逐条配方）+ `oredict/fluid_map.json`（流体原名称表）。脚本读取上述数据，但不校验原版提交或运行环境；比较记录需另存源提交、原始数据校验值、本端代码状态、平台、模组、配置及筛选口径。换基准时保留旧快照，不能覆盖旧分母后直接比较百分比。

## 一、产生本端 dump

`RecipeDump` 在机器配方静态注册完成后写盘，仅在显式指定目录时启用。Gradle 的 run 任务会把环境变量传给游戏进程：

```bash
GT6CE_RECIPE_DUMP=work/recipe-dump ./gradlew :minecraft:runServer
```

也可以在启动参数里用 `-Dgt6ce.recipeDump=<目录>`。输出为同一结构的 `index.json` + `maps/`，启动日志会打印地图数、条数与耗时。

只包含 `MachineRecipeBootstrap` 建立的静态表：附属模组注册的配方和数据包 overlay 都在其后，不在 dump 内。依赖后续标签/外部绑定的行、动态/上下文规则，以及工作台等其他配方入口也不能用这份快照证明完整性。当前没有完整族展开或 reload 后有效配方的采集模式。

## 二、比对

```bash
python tools/compare_gt6_recipes.py \
  --original <CrucibleCraft>/gt6_dump/gt6_recipe_dump \
  --port work/recipe-dump \
  --json work/recipe-gap.json
```

常用参数：`--map gt.recipe.crusher`（可重复）只看指定图；`--limit N` 控制每个图的明细条数；`--include-fake` / `--include-hidden` 把只用于展示或隐藏的行纳入比较。

dump 会为材料方块（存储块、机器外壳、箱、线缆、管道）写出与材料物品相同的 `gt.meta.<形态>#<材料ID>` 身份；旧版 dump 里这些方块只有注册名，不能用于这几种形态的身份比较，需要重新生成。原版把这些方块放在 `gt.multitileentity` 元值或 `storage.*` / `machine.*` / `crate.*` 元物品里，映射表由 `tools/extract_gt6_multiblock_identities.py` 从固定提交的注册代码生成到 `tools/gt6_multiblock_identities.json`：

```bash
python tools/extract_gt6_multiblock_identities.py --source <GT6>/src/main/java
```

## 报告怎么读

- **matched**：归一化输入组能配对，并在本端同组输出集合中找到相同输出；不是完整字段的逐行等价证明。
- **missing**：旧输入签名未找到足够对应行的候选。可能来自实际缺规则、身份映射、注册/绑定、堆叠数据或生命周期差异，需核实后归类。
- **extra**：同输入组按行数相减得到的本端余量；可能是替代、重复、条件内容或归一化误差。不能直接据此删除配方。
- **outputs**：同输入组中找不到相应归一化输出的原版行；组内变体关系及身份仍需核实。

`coverage` 是 `matched / 筛选后的原版条数`，只表示当前 I/O 口径。比较器不检查 duration、EU/t、概率、特殊值、保留输入、必要槽位、空输出约束等行为，即使 dump 里已有其中一些字段也未纳入判断。它使用输出集合而非完整变体的逐个配对，可能忽略输出重数分布与数据对应错误。当前脚本退出码也不代表“无缺口”，不能直接作为行为通过门禁。

补齐优先级按身份事实、规则缺口和加工链影响决定，不只按旧 missing 数排序。

## 身份约定与局限

- 材料物品按 GT6 元物品身份比较：`gt.meta.<prefix>#<材料ID>`。本端一物一注册名，但材料 ID 与 prefix 名沿用 GT6，所以两侧可以直接对齐。
- 非材料物品两侧没有共同注册名（原版用 `gt.multiitem.*` + meta，本端一个概念一个物品），因此按**显示名归一**比较：去掉翻译键前缀（`item.`/`block.`/`fluid.`）与命名空间段、大小写和标点后比对。服务端运行时没有语言文件，本端 dump 里会留下 `item.gt6ce.xxx` 这类键名，脚本按注册路径还原成同样的词。
- `capcellcon` 是已固化的替代身份：原版按材料注册 `gt.multitileentity` 元值 32600–32719，本端是 `gt6ce:fluid_cell_<材料>`。脚本把两者都归一到 `gt.meta.capcellcon#<材料ID>`；映射表取自 `Loader_MultiTileEntities` 的注册与 `MT.java` 的材料 ID（固定提交 3703e403），40 种材料两侧 ID 完全一致。没有这层映射时，挤出与粉碎图中原版 capcellcon 行只能按显示名对比，永远配不上。新增同类替代身份时按此方式显式建表，不要放宽显示名规则。
- 管道、线缆与红石线同样住在 `gt.multitileentity` 元值里，由 `gt6_multiblock_identities.json` 归一到 `gt.meta.<形态>#<材料ID>`：流体管道 `pipe*`（base+0..6）、物品管道 `ItemPipe*`（base+2..7；原版对两种管道共用同一 OP 前缀，所以物品管道单独加 `ItemPipe` 前缀避免混淆）、电线 `wireGt01..16` 与线缆 `cableGt01/02/04/08/12`、红石线 `wireGt01`/`cableGt01`。表的计数断言（40/21/30/6/40）在生成时校验，源文件哈希与 `MT.java` 一样按固定提交钉死。
- 三类非材料物品由脚本内的规则归一，不靠显示名：
  - 选择器：原版是 `gregapi:gt.integrated_circuit` 加元值 N（显示名恒为 "Selector Tag"），本端一物一注册 `gt6ce:integrated_circuit_N`，两者都归一到 `gt.item.integrated_circuit#N`。
  - 石料方块：原版 `gregtech:gt.stone.<stone>` 的元值按 `BlockStones` 的 16 种变体顺序（stone、cobble、cobble_mossy、bricks、bricks_cracked、bricks_mossy、bricks_chiseled、smooth、bricks_reinforced、bricks_redstoned、tiles、small_tiles、small_bricks、windmill_tiles_a/b、square_bricks），本端是 `gt6ce:stone_<stone>_<variant>`；`granite_red`/`granite_black`/`prismarine_light`/`prismarine_dark` 映射回原版的点分路径。
  - 玻璃：原版 `gregtech:gt.block.glass[.glow][.slab.0]` 的元值是颜色索引（`DYE_OREDICTS` 顺序：黑 0、红 1、绿 2、棕 3、蓝 4、紫 5、青 6、浅灰 7、灰 8、粉 9、黄绿 10、黄 11、浅蓝 12、品红 13、橙 14、白 15），本端是 `gt6ce:glass_<clear|glow>[_slab]_<color>`。
  - 电路链：原版整条链在 `gregtech:gt.multiitem.technological` 的 30000–30499 元值里（电路板、焊接电路、电路板/元件/接线、晶体电路），本端一物一注册（`circuit_plate_*`、`circuit_wire_*`、`circuit_part_*`、`circuit_board_*`、`circuit_*`、`crystal_circuit_*`），两者归一到 `gt.tech#<元值>`。原版的 BuildCraft 电路（30280–30287、30380–30387）本端没有对应物品，保持缺失。
  - 面板与建筑台阶：原版面板是 `gt.multitileentity` 元值 32452–32499（混凝土/C-Foam/沥青各 16 色），本端是 `panel_<kind>_<color>`；原版台阶是 `gt.block.<family>.slab.0` 加颜色元值（染白输入用通配元值），本端目前只有 `cfoam_slab_<color>`。两者归一到 `gt.panel#元值` 与 `gt.slab.<family>#颜色序号`；通配元值按白色计，因此“任意颜色可染”这一原版行为在本端仍是白色输入，需要单独核对。
- 流体名额外做空格归一（原版保留 `molten hsla` 这类带空格的名字，本端净化成下划线），并给本端自定的注册名建别名（目前只有 `rc lubricant` → `lubricant`）。
- 数量为 0 的输入在当前签名中归一为 1，用来归并存在型催化剂；此处理不能证明本端加工时不会消耗它。原版空槽标记也被跳过，相关约束需要独立验证。
- 流体经 `fluid_map.json` 归一，`molten.cobalt` 与本端清洗后的注册路径视为同一流体。
- 堆叠数据（NBT / 数据组件）没有完整落盘，只靠数据区分的行（木材种类、电路档位等）被并入同输入组；这能减少任意配对造成的噪声，也可能掩盖变体错误，不能保证无误报或漏报。
- 未注册身份、已注册但未出现在静态配方中的对象、别名未识别和可选模组条件缺失都可能对不上。应查询实际注册/绑定目录；从现有配方提取的词表不能证明注册不存在。
- 原版 dump 在空的输入/输出槽位里保留 `null` 占位，脚本会跳过。

## 后续对账契约（尚未实现）

完整工具应固定源范围并保留所有行的处置状态，采用有来源的身份映射和注册事实，比较全部影响行为的字段、变体及重数。内置基线、适用条件和生命周期完成后的有效配方分开报告；紧凑族支持独立的完整流式枚举，开发对账不能改变运行时缓存。

缺口台账需保留一行的全部原因及共享依赖，并关联责任族、issue 和验证证据。显示名相似、逻辑行计数、仅 I/O 匹配或经批准的范围排除分别记账，不混算为实际修复。数据缺失或动态输入无法穷举时，标明未证明范围并提供固定来源的契约验证，按实际适用域的等价类别与边界检查，不裁剪原版输入域。

新 issue 应先定位到共同规则及受影响域，修复后检查同族和前后加工链；单条样例通过不足以证明整族完成。上述能力由计划卡承接，本次文档更新没有为现有工具增加新的命令参数。
