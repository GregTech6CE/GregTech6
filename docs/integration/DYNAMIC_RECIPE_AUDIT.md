# 动态配方与批处理核查（2026-10-19）

基线 `4dc1031df`。原版来源根为 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java`。这批沿 Bath／Unboxinator 的配方生成、查找、执行入口核查，并完成 85 张配方表四个构造标志的源码比对。只整理可读性与审计记录，没有修改配方或玩法。

## ALIGN-048：战利品袋和落灰书缺少机器拆包路径

原版 `gregapi/recipes/maps/RecipeMapUnboxinator.findRecipe:56–65` 读取 MultiItem 的 Behavior_Drop_Loot，按每个表各抽一个产物，构造动态配方；不是使用展示行中罗列的全部物品。成功抽取的正常配方为 16 EU/t、16 tick，要求输出为空，并关闭配方缓存。如果全部表抽空，另有返还输入的分支，不能照常吞掉袋子。

当前共享 `content/recipe/GTMainRecipes:125–141` 的 dusty_guide_book、bagged_sapling、seed_pouch、gem_pouch、loot_pouch 等行标记 fake，register 按 addFakeRecipe 写入；`api/recipe/RecipeMap.findRecipe:718` 明确跳过 fake 行。内置动态入口只有材料扫描／打印／复制、蜂后分析及特定上下文处理，未绑定 Unboxinator 战利品处理。NeoForge 的 `MachineContextRecipes` 仅有 USB 路径与可选 resolver，活动源码未绑定战利品 resolver；外部附属仍可提供自己的配方，这不代表内置已实现。

这些物品本身已经存在：`registry/GTMultiItems` 将四种袋子及落灰书注册为 LootBagItem，`content/loot/LootBagItem.useOn:61` 调用 GTLootTables.roll，所以玩家右键开袋不是缺失项。材料板条箱、火柴盒、管道拆包也有正常静态配方，不能扩大成“拆包机完全不可用”。战利品板条箱／其他模组袋子与 API 失败返还仍待进一步核查。

## ALIGN-049：82 张配方表丢失必须有输出的标志

新只读工具按 internal name 对齐原版 `gregapi/data/RM.java` 与当前 core `data/MachineRecipeMapDefinitions.java` 的显式构造行。本次两侧各 85 个同名条目，没有未解析构造点或未配对条目。在所核四个参数中，82 个 mNeedsOutputs 从 true 变 false；mCombinePower、mUseBucketSizeIn、mUseBucketSizeOut 全部相同。未据此宣称其它字段或全部配方已经对齐。

该参数含义由原版 RecipeMap 构造器的 `mNeedsOutputs=aNeedsOutputs`（Recipe:109）及无输出注册检查确认；不是查看器标题开关。当前 RecipeMapSpec 实际 core 枚举 85 张表的 mNeedsOutputs 全为 false，原生 MachineRecipeMaps.fromDefinition 两平台均直接透传。结果是 `RecipeMap.addRecipe:517` 的无输出配方拒绝条件被绕过。

此外当前 `RecipeMap.findRecipe:721/729/737` 把 mNeedsEmptyOutput 检查又放在 needsOutputs 条件下；基础机器传入的正是此表标志。因此即使后续补一个要求空输出的动态拆包配方，当前 Bath／Unboxinator 标志仍会让基础机跳过这层检查。原版 BasicMachine.canOutput:633/652 消费 mNeedsEmptyOutput 并不依赖表级 mNeedsOutputs。当前手作 ProcessingToolBlockEntity.canOutput:153 则直接检查物品槽和流体罐，两种执行路径不能混写。

这是参数及调用链差异，不等于 82 张表里的每条配方都无法加工，也没有声称已经在游戏中利用随机产物筛选。当前内置随机动态入口缺失（048／050）会遮蔽部分后果。完整逐项清单在 [标志清单](verification/recipe-map-flags-20261019.json)。

## ALIGN-050：Bath 的通用动态处理尚未接入

原版 `RecipeMapBath.findRecipe` 在静态表无匹配时还有以下按当前物品数据生成的分支：

| 来源分支 | 原版行为 | 当前证据／边界 |
| --- | --- | --- |
| :119–134，护甲和 IItemColorableRGB | 依据现有装备数据染色；氯去色、液体染料重染 | 当前 TextileFinishingRecipes／CanvasRecipes 提供指定方块与帆布的静态染色，不能代替任意可染装备／物品数据的动态处理；Bath 未绑定对应 provider |
| :135–139，IItemProjectile | 圣水赋 Smite 10，FieryBlood／FieryTears 赋 Fire Aspect 3，使用 25 L 并检查已有等级 | 当前工具材料自带附魔与箭命中处理是另一条路径；机器配方入口没有此按输入附魔状态生成的 Bath 分支 |
| :140 起，可食用且不含流体的物品 | 没有既存效果数据时，以 250 L 药剂／药水添加效果及概率，512 tick、0 EU/t | 静态特定食物配方不覆盖任意食物实例；无对应动态入口。逐种药剂在当前的注册、食用效果消费和现代组件保存仍待核，不在本批声称所有药剂缺失 |

原版 Atum 未鉴定战利品洗涤（:110–117）是外部模组条件分支，应归兼容核查，不能当作无依赖环境必缺的内置物品。原版未处理木板的油浴已有当前 TreatedWoodRecipes 的固定原版／GT 树种与七油路径，以及部分 Immersive Engineering 静态兼容；外部木字典成员全集和楼梯／半砖覆盖未在本批验收。

手作浸洗盆还有执行入口差异：原版 `MultiTileEntityBathingPot:194` 调用 mRecipes.findRecipe，当前 `ProcessingToolBlockEntity.process:130` 直接遍历 mRecipeList。仅给基础机增加 Bath provider 仍不会让手作工具调用它。当前手作的空输出检查本身已经存在，不能把动态入口遗漏误写成空输出检查缺失。

## ALIGN-051：链式并行缺少原版总工作量上限

原版 `MultiTileEntityBasicMachine.canOutput:626–628` 对 mParallelDuration 限制批数：在批数大于 1 时，若 `abs(EUt×duration)×批数 > mInputMax×600` 就递减。它是在计算效率／超频以前、以原配方总工作量限制本次扣料数量，不能拿“最后算出了成本”代替这个条件。

当前 `BasicMachineBlockEntity.computeParallel:740` 只按配置并行、库存、普通功率缩放、流体 int 数量上限及输出空间收缩；`MachineWorkCost.calculate` 计算成本但不执行上述工作量准入。

实际来源例子为原版 `Loader_Recipes_Ores:370`，当前 `GTOresGen:155`：一个离心钴矿 → 钴粉、概率小撮钴60粉（8），512 EU/t、2304 tick。大型离心机参数上限 16、最大输入 4096；原版工作量上限 `4096×600=2457600`，单份工作量 `512×2304=1179648`，因此一次最多 2 份。若投入 8 份且输出为空，当前数量／空间路径可接收 8 份（第一产物 8，第二最坏 64，都能放下），实际 core 成本为 512 最低功率、18874368 工作量。差异是提前扣料与批处理规模，不据此断言总产量或单位成本错误。

实际 core MaterialItemDefinitions 已观察到 crushed_centrifuged_cobalt、dust_cobalt、dust_tiny_cobalt60 三个所需定义；这提高了样例依据，但没有在 Minecraft 注册表中解析或运行该配方，也没有完整生存获得链验收。原始大表经过优化、碰撞优先级等步骤后的最终行仍需原生验证。

## 可读性与证据

RecipeMapSpec 的字段分组、参数、标签设置和返回语句已经展开；可执行代码／字面量保持不变，Java 17 独立编译通过。新增 inventory_recipe_map_flags.py 支持显式来源／当前文件／输出路径，记录不能解析的行，并拒绝覆盖输入源码。首次检查发现空格和内联注释导致漏读，已修正为读取去注释代码中的布尔尾参；最终 85 对均解析，并用有内联注释和不支持表达式的独立样本核查解析边界。

core 观察来自 `DynamicRecipeAudit.java`：85 张表的标志、两张表槽数、实际机器参数／成本和材料物品定义。动态配方缺失、手作入口、空输出和批数的原生分支均为源码证据，不冒称游戏实测。来源哈希及结果见 [回执](verification/dynamic-recipe-audit-20261019.json)。未运行 Gradle、全套测试、Minecraft、GameTest、存档重启、打包或推送。

后续继续特殊启动能量、负功率输出、其它动态 RecipeMap 子类与控制器、材料／能源／物流等全面队列；本批不结束总目标。
