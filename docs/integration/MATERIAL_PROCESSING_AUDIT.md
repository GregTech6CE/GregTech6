# 材料工厂、处理目标与前缀配方复核

本批基线为 `618e31b8b`，日期 2026-10-19。核查对象是材料属性导入、七种处理目标及其直接使用路径，不代表全部材料、配方或机器已经完成盘点。原版来源仍为 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java`。

## 属性表有两种独立的遗漏（ALIGN-010）

`tools/extract_gt6_workability.py:38,77–102` 将工厂**方法声明**也当成材料名称记录，却没有把工厂属性传给调用它的材料。生成表出现 `food`、`dustfood`、`metal`、`metalloid` 等方法名；这些不能替代各材料实际继承的属性。

另外，表按原版字段名归一化，`MaterialWorkability:87–113` 却按当前 `material.getName()` 查询。移除空格或改大小写不能把 `NaCl` 变成 `Salt`、把 `SiO2` 变成 `SiliconDioxide`。材料形态表已有按数值 ID 映射的方案，工作属性表还没有采用它。

直接运行已编译 core，注册 1,160 个材料后观察如下。这里只列有独立原版依据的属性，不把其他返回值一律判为错误：

| 原版材料 | 原版依据 | 当前观察 |
| --- | --- | --- |
| MeatRaw、MeatCooked、FishRaw | `MT:168,172,176`：meat → dustfood，继承 FOOD、MORTAR | 两项均为 false |
| Cheese | `MT:1333` → orefood；`:171,175` 提供 FOOD、MORTAR | 两项均为 false |
| Chocolate | `MT:1336` → mixfood；`:173,177` 提供 FOOD、MORTAR | 两项均为 false |
| NaCl／Salt | `MT:1129` 明确 FOOD、MORTAR、BRITTLE | 三项均为 false |
| Sn／Tin、Pb／Lead | `MT:439,472` → posttrans → metal；`:62,78` 为熔点不高于 1200 的金属赋 FURNACE、MORTAR，锡 505、铅 600 | 两项均为 false |
| SiO2／SiliconDioxide | `MT:1056` 明确 BRITTLE、FURNACE、CRYSTALLISABLE | 三项均为 false；oredustdcmp → oredust 并非 dust 工厂，不据此要求 MORTAR |
| Paper、Wood（对照） | `MT:1300,1244,299` 的 MORTAR 路径 | 当前 MORTAR 为 true，不能把整个表说成完全失效 |
| Au／Gold（对照） | `MT:468` 明确 MORTAR | 当前 MORTAR 为 true |

影响已追到共享 `MortarGrindingRecipes` 的材料过滤、`AutoclaveRecipes` 的晶化过滤、`FurnaceSmeltingRules.allows` 以及焊接／压缩耗时分支。未运行完整 Minecraft 注册和配方枚举，因此不声称上述每种材料的每条最终配方都缺失：显式配方或其他入口可能提供局部替代。观察结果、代码摘要和范围见 [回执](verification/material-processing-audit-20261019.json)。

## 七种目标需要分别判断

对原版全部 Java 源码去除注释和字符串后，索引七种 `mTarget*` 字段的引用，见 [调用位置索引](verification/processing-target-consumers-20261019.json)。可用 `tools/integration/inventory_processing_target_consumers.py --source-root <原版 src/main/java> --output <报告路径>` 重建；索引是导航，不等于完成行为验收。

| 目标 | 原版直接使用位置（定义、复制及手册之外） | 当前结论 |
| --- | --- | --- |
| Solidifying | Furnace、大小坩埚、铸造模具 | 小坩埚冷却已确认 ALIGN-009；其余路径继续核查，不能以小坩埚结果代替全部路径 |
| Forging | `OreDictMaterialCondition`、`RecipeMapHandlerPrefixForging` | 自锻造砧配方已有 `AnvilMaterialCatalog` 替代表示；挤压机目标／条件存在 ALIGN-011 |
| Smashing | `MultiTileEntityAnvil:128` 损坏后按目标材料掉落碎片 | 当前砧按自身材料掉落；需检查哪些实际注册砧具有不同目标，暂不判具体可见物品缺陷 |
| Bending、Compressing、Cutting、Working | 未找到定义／复制／手册之外的直接字段使用 | 不把“移植版未用于对应机器”单独列为行为缺失；需继续检查间接依赖 |

### 挤压配方与原版不一致（ALIGN-011）

原版 `Loader_Recipes_Handlers:741–800` 按 EXTRUDER_FODDER、INGOT_BASED、GEM_BASED 等前缀条件生成矩阵；`:807–838` 区分 EXTRUDER_SIMPLE 与普通挤压，要求 EXTRUDER、fullforge 并排除反物质。`RecipeMapHandlerPrefixForging.getOutputMaterial` 使用锻造目标，耗时也使用输出材料的熔点。

当前共享 `PartRecipeLoader:228–266` 仅遍历 ingot、plate、gem、dust 四个输入前缀，对存在输入／输出的组合同时注册普通和低温模具路径，输出为 `out.stack(m)`；没有读取上述材料条件或锻造目标。Rubber、Plastic 已单独交给 `PolymerFormingRecipes`，不将其算入这个通用分支的缺口。

原版 `MeatRaw.setForging(MeatCooked,U)`、`IronMagnetic.setForging(Fe,U)` 是目标差异的具体例子。当前通用分支会保留输入材料身份。除此之外，模具查找失败时允许无模具配方，属于需要清点的回退策略；本批**没有证明现有模具发生查找失败**，不将其单独写成已发生的无模具漏洞。

### 手册目标遗漏复制继承（ALIGN-012）

原版 `OreDictMaterial.setAllToTheOutputOf:749–777` 复制目标材料及份额。当前 `MaterialProcessingTargets` 提取器只读取显式七种 setter，没有展开此复制和工厂继承。core 观察确认：

- WoodTreated、WoodPolished 的 Forging／Compressing 没有记录，手册回退为自身 1U；原版 `MT:1245–1246` 复制 Wood 的目标。
- Brick 的 Forging／Compressing 没有记录，回退为自身 1U；原版 `MT:1279–1280` 继承 Ceramic 的禁用目标，份额应为 0。
- TungstenSintered 的 Forging 显式恢复自身 1U 已记录，但 Compressing 缺省为自身；原版 `MT:1732` 先复制 W，未把 Compressing 改回自身。

这是已确认的材料手册信息错误；对应实际机器是否也错，需要逐消费者判断，不能从展示表直接推出。

## 通用配方过滤还存在差异（ALIGN-013）

- 原版 `Loader_Recipes_Handlers:79` 的 rockGt 研钵行只排除反物质；当前 `MortarGrindingRecipes:82–83` 在遍历所有普通行前统一要求 MORTAR，包括 rockGt。即使属性导入修好，这个额外条件仍然存在。
- 原版 `:217–233` 的压缩行排除 COATED，dust→plateGem 还排除 LAYERED。当前 `MaterialFormConversionRecipes` 只排除反物质和对应宝石形态／Ice，没有这两个条件。此结论针对该注册分支，不声称每种涂层材料都有可达物品形态。
- `MaterialFormConversionRecipes` 仍跳过 compressed、plateSteamcraft、casingSmall 对应转换，须另查是否为现代替代形态。其旧注释还称 plateTiny、rawOreChunk 不存在，但当前 `MaterialPrefix` 已定义这两个前缀；本批修正了这段过时说明。

## 本批代码整理与验证边界

将 `ExtrusionWorkRules`、`MaterialRecoveryRules`、`PipeWeldingRules` 的压行公式和表格展开；删去 `PartRecipeLoader` 的重复类注释，修正模具、研钵材料数量、缺失前缀等过时说明。六个 Java 文件与基线比较时，去除注释和空白后的代码及字符串保持一致。本批不改配方数值、不自动修复上述行为差异，不重新打包或启动游戏。

仍需完成：所有属性的工厂继承／成员核对、完整前缀条件矩阵、其他处理目标与机器消费、动态注册及外部模组来源。旧生成器的固定 `F:` 路径及把原版路径传入 native_path 的写法，也需在统一生成器入口时处理；本批没有执行这些旧写入工具。

后续进展：第四批已将这里的两个生成器改为显式来源参数和直接 core 输出，并验证可重现当前数据。固定路径问题已处理，属性继承与目标复制缺口仍然存在；见 [前缀配方与生成器记录](PREFIX_HANDLER_AUDIT.md)。
