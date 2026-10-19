# 通用前缀配方审计

日期 2026-10-19，基线 `758769b41`。本批核查原版 `gregtech/loaders/c/Loader_Recipes_Handlers.java` 与当前共享通用配方入口；不把单物品显式配方、外部模组补充配方和所有运行时展开结果视为已经核查。

## 范围索引

去除注释和字符串后，该原版文件中有 **478 个 `RM.<map>.add(new ...)` 调用点，涉及 28 个配方组**。它们包含循环、条件分支和动态处理器，不是 478 条最终配方。索引保留行号、处理器类型及来源摘要，见 [注册位置](verification/prefix-handler-sites-20261019.json)。工具 `tools/integration/inventory_recipe_handler_calls.py` 可按显式原版文件路径重建。

| 配方组 | 调用点数 | 当前审计状态 |
| --- | ---: | --- |
| RollingMill / Wiremill / RollBender / ClusterMill / RollFormer | 22 / 8 / 8 / 2 / 2 | 确认本页 ALIGN-014；各自完整输入形态矩阵仍待核查 |
| Lathe | 22 | 确认 ALIGN-014、015；条件及备用输出仍待逐材料枚举 |
| Cutter | 20 | 确认 ALIGN-015、016；其他显式切割配方未整体核查 |
| Extruder | 8 | 前批 ALIGN-011；这 8 个调用点在辅助方法内被多种前缀循环使用 |
| Mortar / Compressor | 34 / 24 | 前批 ALIGN-010、013；外部模组植物压缩条件仍待核查 |
| Anvil / AnvilBendBig / AnvilBendSmall | 45 / 3 / 2 | 只核过目标使用及部分目录表示，未完成全部行 |
| Autoclave | 12 | 工作属性错误会影响晶化过滤，全部环境／流体分支待核查 |
| Sharpening / Shredder / Crusher | 20 / 36 / 12 | 已找到移植入口；本批仅把磨粉余料实现用作对照，未判整体等价 |
| Welder | 46 | 当前已有按材料工作性选择耗时的实现，不能归入“全部固定耗时”；其他条件待核查 |
| Boxinator / Unboxinator | 55 / 35 | 已找到通用形态和弹药入口，完整矩阵与附加输入待核查 |
| Press / Bath | 11 / 42 | 待逐输入、模具、流体及条件核查 |
| Freezer / Generifier / Loom / Polarizer / Sifting / Sluice | 1 / 1 / 1 / 3 / 1 / 2 | 待核查；动态处理器的一次注册可能覆盖多种材料，不能按调用数判完成度 |

## 固定耗时替代了原版工作性分支（ALIGN-014）

原版 `Loader_Recipes_Handlers:262–319,371–393` 将易加工材料（FURNACE 或 SOFT）与其余材料分开。困难分支传 duration=0，由 `RecipeMapHandlerPrefix.getCosts:225–226` 计算：

`向上取整(max(输入物质量, 输出物质量) / U × multiplier × (toolQuality + 1))`，随后至少 1 tick。

| 机器／路径 | 原版困难分支 multiplier | 当前 `PartRecipeLoader` |
| --- | ---: | --- |
| RollingMill | 256 | 所有通过 SMITHABLE 筛选的材料使用 2、16、32 等固定时长 |
| Wiremill：杆→细导线 | 128 | 固定 8／16 tick |
| RollBender | 256 | 固定 4／16 tick |
| ClusterMill | 256 | 固定 16 tick |
| RollFormer | 64 | 固定 16 tick |
| Lathe | 64 | 固定各行时长，不读取工作性 |

例如原版 Iron 的熔点为 1811、工具品质为 3，不满足 SOFT／FURNACE 的易加工条件；1U 铁锭→板的困难分支成本是 1024 tick，当前注册代码传入 16 tick。该例是源码参数对照，未作为游戏中最终配方观察。

易加工分支也有边界差异：原版 nugget→plateTiny、nugget→round 的 `16/9` 使用 Java 整数除法，为 1 tick；当前传 2 tick。RollingMill、Wiremill、RollFormer 和部分 Lathe 原版还排除 COATED／LAYERED，当前相应通用方法未完整保留这些条件。

物理导线的锭→导线由另一个 `WireProcessingRecipes` 入口负责，已有手写 EASY 集合；不能把本页杆→细导线的结论扩大成所有导线配方相同。该手写集合与原版成员是否一致还需核查。`WelderFamilyRecipes` 已有工作性与品质耗时计算，也不属于上述固定时长问题。

## 车床和切割机漏掉粉碎余料（ALIGN-015）

原版上述 Lathe、Cutter 行启用 `aOutputPulverizedRemains`。`RecipeMapHandlerPrefix` 在材料差额至少为 `dustDiv72` 时，调用 `OM.pulverize(输入材料, 输入量−输出量)`，按粉碎目标输出余料。

当前 `PartRecipeLoader.roll` 只传主输出；`cut` 同样没有余料。可直接核对的例子：

- plateGem 1U → lens 3/4U：原版返还 1/4U 粉碎余料，当前普通车床调用只输出透镜。
- lens 3/4U → ring 1/4U：原版返还 1/2U 粉碎余料，当前只有环。
- plate 1U → plateTiny×8，共 8/9U：原版返还 1/9U 粉碎余料，当前切割调用只有小板。

这不是“所有加工没有副产物”：当前 ingot/gem→stick 的专用 `latheToStick` 确实输出两小撮粉，但直接取原材料而非粉碎目标；`SharpeningRecipes` 则已经调用 `MortarGrindingRecipes.pulverize` 计算余料。两边前缀的上述物质量一致（原版 `OP` 与当前 `MaterialPrefix.getMaterialWeight`），因此不能把差额解释成单位迁移。

## 通用切割的冷却液矩阵和倍率不同（ALIGN-016）

原版 `Loader_Recipes_Handlers:628–652` 定义 5 个候选冷却液，但循环边界是 **i < 4**，只处理 Water、SpDew、DistW、Lubricant；第 5 个 LubRoCant 不能因为出现在数组中就算实际生成。前四种倍率分别为 4、4、3、1；流体存在性是外部条件。

切割时间使用倍率；流体量先按对应时间建立，`RecipeMapHandlerPrefix:218` 进一步按工具品质+1 缩放（此处未启用 flatFluidCosts）。当前 `PartRecipeLoader.cut` 固定 Minecraft 水，时间使用未乘冷却液倍率的基础值，水量为 `max(16,duration)`，也没有品质倍率。

以品质 3 的 long stick→两根 stick 为参数例：原版水路线是 64 tick、256 mB 水；当前通用注册传 16 tick、16 mB。原版蒸馏水与润滑油路线也不由当前这段通用矩阵生成。这里不声称整个模组所有 Cutter 配方都只用水，专门木材／方块等入口仍需各自核查。

## 生成器标准化

`extract_gt6_workability.py`、`extract_gt6_processing_targets.py` 现在都要求 `--source <MT.java>`，默认输出为真实 core 源文件，提供 `--check`，不再在导入时创建原生投影视图或使用固定 `F:` 路径。可选报告必须显式指定，避免默认重写历史溯源文件。共享 `generator_output.write_or_check` 处理 UTF-8 输出与只读比较。

两个生成器可重现当前 Java 表；目标表的覆盖去重也改为明确的“首出现位置、末次值”映射。**提取算法的属性继承／材料名遗漏没有在这个结构改动中修复**，ALIGN-010、012 仍开放。核对模式通过只能证明可重复输出，不能证明与原版行为相等。

本批没有修改生产 Java 或资源、没有启动游戏、没有运行全套测试或重新构建包。调用点、源码摘要和生成器检查结果见 [本批回执](verification/prefix-handler-audit-20261019.json)。后续仍需将每种材料的条件、形态存在性和最终注册冲突顺序一起核对。
