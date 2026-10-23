# 地表石堆／木棍堆对齐核查

第五十六批，2026-10-21；观察基线 `f42d88cb2bf2d4444c624e722e8bb722d42d35d8`，登记 ALIGN-196–201。先完成只读源码核查，再归档本报告；没有修改生产行为，没有运行 Gradle、编译、游戏、世界生成、构建或新增生产测试。有限概率／噪声数学推导不代表世界实测。来源、两端投影、原生依赖及文件哈希由[本批统一回执](verification/surface-plant-audit-20261021.json)归档。

原源码根：`C:/Dev/gregtech6-master/gregtech6-master/src/main/java`。当前根：`C:/Dev/GregTech6`。原文件保留 GregTech-6 Team、Gregorius Techneticies、LGPL-3.0-or-later 头。本次没有复制原生第三方方法体。

## 主结论与历史修复补偿

历史“只有石子／矿子，没有陨铁和燧石；木棍只有原版木棍”的状态已经不能代表当前代码。现代普通石堆有完整 flint／MeteoricIron rockGt／MeteoricIron oreRaw 分支，显式物品模式已经避免再额外掉一份材料矿物；木棍堆有按群系／维度选择木材及 Dead／Mossy／Rotten 混合逻辑，两端使用同一业务实现。

本轮确认仍有以下不同合同：木棍额外列被外层概率再次门控；层岩石子取扫描结束时 Y112 的层而非接触地面的最后岩层／矿物；基岩指示石多掉一份 rockGt；自然石堆破坏未处理 Fortune 数量；普通石堆未接入失去支撑后的邻居移除；原版木棍／燧石潜行摆放未找到内置对应路径。下面逐项限定证据和可达条件，不扩成全部地质、全部容器或全部工具问题。

## 实际注册和生成入口

原 `Loader_Worldgen.java:618` 注册主世界 `WorldgenRocks("overworld.rocks", true, 2, 3, ...)`，`:630` 注册 `WorldgenSticks("sticks", true, 2, 2, ...)`，均放入 GEN_OVERWORLD／GEN_GT。GT_API_Proxy 注册 Forge IWorldGenerator 后经 GT6WorldGenerator 调用实际生成器；该入口已在灌木世界生成稿追到区块群系集合和启用列表。原 `Loader_MultiTileEntities.java:2033–2036` 注册自然石 32757、手摆石 32074、自然木棍 32756、手摆木棍 32073。misc/MultiTileEntityRock 只是向 placeables/MultiTileEntityRock 转接的兼容类，不是第二套掉落实现。

当前共享路径缩写均相对于 `multiversion/src/main/java/com/gregtech/gregtech/`。

| 环节 | 现代实际实现与双端入口 |
| --- | --- |
| 石块 | `registry/GTBlocks.java:54–61` 两分支均注册 rock → RockBlock；`GTBlockEntities.java:56–57,472–475` 将相同 RockBlockEntity 绑定到该方块 |
| 木棍块 | `GTBlocks.java:63–70` 两分支注册 twigs → TwigBlock；没有 BE，掉落时按当前位置选择木种，不应因缺少原 mStick 类／字段误报 |
| 普通石生成 | `GTFeatures` 两端注册 GTRocksFeature；该类 place → castRay → litterFor → placeRock → GTRockPlacement.place → RockBlockEntity |
| 木棍生成 | `GTFeatures` 两端注册 GTSurfaceFloraFeature；place 首先调用 placeTwigs，经 placeTwig 放 twigs 方块 |
| 地质指示石 | GTStoneLayerFeature、GTOreVeinFeature、GTBedrockOreFeature 调用 GTRockPlacement，实际进入同一 RockBlock／BE 掉落逻辑 |
| 框架提交 | Forge GregTech:39 注册 features，DeferredRegistrySubmission:24 提交 GTBlocks，MachineBlockBootstrap:50 提交 BEs；Neo GregTechNeoForge:87–90 提交 BLOCKS／BLOCK_ITEMS／BEs／features |

core 的 configured_feature／placed_feature 中已有 gt_rocks、gt_surface_flora、gt_stone_layers 和 gt_bedrock_ores；两版各自 biome_modifier 把对应 feature 添加到主世界。gt_rocks 和 gt_surface_flora 为 vegetal_decoration，placed feature 无附加 modifier。core 资源通过发行配置嵌入两版。原生 AddFeaturesBiomeModifier／PlacedFeature 的实际调用已核对，来源回执复用并补充本轮所需原生类。

## 石堆内容池：已有补偿，不再报陨铁／燧石缺失

原 `WorldgenRocks.tryPlaceStuff:59–64` 在接触地面及替换检查成功后，以 nextInt(mAmount) 决定是否保存物品；主世界 mAmount=2。物品分支先 1/12 选陨铁，再 1/4 选 oreRaw，否则 rockGt；非陨铁为 vanilla flint。当前 `TerrainWorldgenRules.litter:13` 保留同一 2／12／4 嵌套，`GTRocksFeature.litterFor:143–144` 实际调用该规则，并通过 MaterialStackItemHelper 解析材料物品 ID。

| 每个成功普通石堆的内容分支 | 原／当前概率 |
| --- | --- |
| 无显式物品，默认 Stone rockGt | 1/2 |
| minecraft:flint | 11/24 |
| MeteoricIron rockGt | 1/32 |
| MeteoricIron oreRaw | 1/96 |

`CompoundMaterials.java:463` 定义 MeteoricIron，MaterialForms 的 meteoriciron 行包含 ORES，MaterialPrefix 的 rockGt／oreRaw 及 MaterialItemDefinitions 的候选列表接入生成。Forge ItemBootstrap 和 Neo GTItems.register 均消费同一 MaterialItemDefinitions.all 流，因此不是只有规则表、没有两端物品入口。没有启动游戏验证某世界已生成样本，也不把罕见的 1/96 当成必然短时可见。

`RockBlock.yields:120–139` 先处理非空 itemId，直接返回该明确物品并停止，不追加 material rock／raw。普通陨铁原矿石堆会把 oreRaw ID 存进去，故历史“一个显式物品再加额外矿物”的问题已在此补偿。这个正确分支与下述基岩 material+rawOre 分支必须分开。

## 木棍池：多木种已接入，选择发生在采收时

原 `WorldgenSticks.tryPlaceStuff:61–65` 只放自然木棍块，不预存木种。`MultiTileEntityStick.getDefaultStick:101–162` 在采收时按维度及群系名称选择。当前 TwigBlock.stick:50–63 在采收时将维度 ID／群系 ID交给 SurfaceTwigRules.material，解析材料后调用 GTItems.getStack(stick, material, count)。不是每堆生成时抽一个固定木种。

规则表保留 Spruce／Taiga／Boreal → Spruce，Birch → Birch，Jungle → Junglewood，Savanna → Acacia，Roofed／Dark Oak → DarkOak，Maple、Aspen、SilverPine、Towerwood、Redwood、Cypress、Pine、Fir 等分支。普通分支中 1/16 为 Dead、1/16 Mossy、1/16 Rotten，剩余 13/16 为群系木种或普通 stick；冰寒／沼泽等带 ! 的分支直接选 Frozen／Mossy 等，不再追加该混合。current dark_forest 映射 roofed forest，避免把现代原版黑森林误当旧外部 dark forest 的 Towerwood。

TwigBlock 先尝试原 WoodX 拼写，无效则去掉 Wood 前缀，再经 MaterialRegistryExtras 别名（例如 Jungle→Junglewood、Coconut→Coconutwood、Autumn→Autumnwood）解析。WoodMaterials 有这些材料，wood 工厂和 stick 前缀允许生成，相同定义流在双端注册。因此旧注释“twigs drop vanilla sticks”不代表实际只能掉 vanilla stick。没有触碰正在整理的 WoodMaterials.java，来源回执仅记录读取时哈希。

原 IL.Stick 在 LoaderItemList.java:774 默认绑定 vanilla stick，TFC 存在时可替换；下文 vanilla stick 结论均按默认无该可选提供者限定。原 Twig getDrops 使用 `1+rng(1+fortune)`，当前 TwigBlock.getDrops:36–47 读取真实工具 Fortune 并传相同数量公式；右键两边均取 1 并移除块。手摆旧 32073 的 getDefaultStick 覆写固定 vanilla stick×1，不能误以为旧手摆任意木棍能保留原木种。

旧 Aether／Erebus／Betweenlands 等维度分支在当前规则表有对应前缀；旧 Twilight live root 等特殊提供者和完整维度生成名单不在本次默认主世界验收内，不能当无条件现代缺失。

## ALIGN-196：木棍列数的概率双重门控

原 WorldgenSticks.canGenerate:48–57 先对区块群系集合决定数量倍数：woods／swamp 为 3，plains／river／savanna 等为 2，taiga／mesa 等为 1；WorldgenOnSurface.generate 随后先抽 `2×倍数` 个目标，用 boolean[16][16] 去重，再对每个被标记列独立 roll 1/2。

当前 GTSurfaceFloraFeature.placeTwigs:64–79 只循环 TWIG_AMOUNT=2。每次先通过外层 1/2，才读取群系倍数，再为额外的 `倍数−1` 列各 roll 1/2。于是额外列同时依赖外层和内层成功，不是原来先增加列数再各 roll。forest 的每个外层理论尝试数期望为 `1/2×(1+2×1/2)=1`，两轮共 2；原六列各 roll 的未去重近似期望为 3。

更精确地，在整个区块所有列均为同一合格群系、地面 Y64、上方为空、无其他阻挡且抽样按均匀独立 Random 解释的理想模型，计入重复列不能重复放置，令 s=255/256：原期望 `128×(1−s^(2m))`；当前期望 `256×(1−(1/2+1/2×s×(1/2+1/2×s)^(m−1))²)`。

| 倍数 m | 原期望不同有效列 | 当前期望不同有效列 |
| --- | --- | --- |
| 1（如 taiga） | 0.998046875 | 0.999023438 |
| 2（如 plains） | 1.988311738 | 1.495855328 |
| 3（如 forest） | 2.970855267 | 1.991233788 |

这是同一平地模型下嵌套概率合同的确定差异，不只是 RNG 消费顺序，也不是实际世界密度测量。m=1 的微小不同来自旧先去重再 roll、当前 roll 后抽列。该模型有意选无树枝／同群系列，不依赖已归档灌木共用 surface／biome 差异。

## ALIGN-197：层岩石子未追踪接触地面的最后岩层／矿物

原 WorldgenStoneLayers.java:80–118,177–188 在逐高度处理中维护 tLastRock、tLastOre、tCanPlaceRocks；暴露为空气／可替换块时，以 1/128 尝试在该实际接触位置放石子，材料在 `tLastOre.mTargetCrushing.mMaterial`（条件命中时）与 tLastRock 之间选择。只有 mGenerateIndicators 的实际矿物生成才更新 tLastOre，不是任意随机矿物。

当前 GTStoneLayerFeature.java:101–166 完成 MIN_GEN_Y 到 MAX_GEN_Y=112 的整列循环后，才在 :168–173 以 1/128 调用 GTRockPlacement.placeRock。循环每步滑窗，因此结束时 scan[3] 为 **噪声 Y112 的层**，不是当时地表高度的层。当前 Ore record 和 resolveOres 也没有保留 OreDef.indicators 标志或最后矿物，石子没有走旧 tLastOre／targetCrushing 选择合同。

可审计表值反例：实际当前表有 131 项，种子0、x340、z128，噪声 Y64 为索引76 Limestone；噪声 Y112 为索引96 Gneiss。若合法列在 Y64 为可替换石头且生成该 Limestone 层、Y65 以上为空、矿物分支未命中、石子 roll 通过，当前会在该露头上方摆 Gneiss rock，而不是实际接触的 Limestone。纯源码 float 推导值和静态表索引存于 sources.json；未声称默认种子实际恰有这个地形。

不能据此说“没有岩层石子／没有矿子”：当前层岩、传统矿脉和基岩各自都调用石子放置器，整个生成机制已存在；传统 GTOreVeinFeature 也仍有矿材 rock 指示分支。确认的是层岩指示石材料／位置关联和原最后矿物合同，而非重新审计全部矿脉生成。

## ALIGN-198：基岩指示石多一份材料岩石

原 WorldgenOresBedrock.java:147–168 创建 `oreRaw(mMaterial,1)` 并保存为自然石块 mRock。MultiTileEntityRock.getRock:166–167 对非空 mRock 返回该物品，普通右键只给这一份原矿，不额外给 rockGt。

当前 GTBedrockOreFeature.java:83–88 实际调用 GTRockPlacement.placeRock(...,material,true)；后者只设置 material 与 rawOre，不设置 explicit itemId。RockBlock.yields:128–137 先加入 rockGt(material,count=1)，再因 rawOre=true 加入 oreRaw(material,1)。这些矿材能生成 oreRaw 时也满足 rockGt 的 ORE_PROCESSING 条件，双端共享定义与同一块/BE实际可达。

因此在默认内置物品完整、右键采收这一基岩指示石时，原为原矿×1，当前为岩石×1+原矿×1。不要把普通陨铁 litter 的正确 explicit-item 分支一起判为仍重复，也不将基岩花／石数量分布等另一组生成政策全部并入此项。

## ALIGN-199：自然石堆 Fortune 数量没有接入

原自然石 32757 的 MultiTileEntityRock.getDrops:80–81 返回 getRock(1+rng(1+fortune))；getRock:166–167 对已有 mRock 也按该数量取物。MultiTileEntityBlock.harvestBlock:261 从玩家读 Fortune／SilkTouch 并实际调用该接口，不是未调用辅助函数。手摆石 32074 的 getRock 覆写直接返回 mRock，则有意不受这个数量影响。

当前 RockBlock.getDrops:142–145 只调用 yields，未读 TOOL／Fortune，显式物品和默认材料的数量都是 BE count，普通自然堆为 1。原生现代 Block.playerDestroy→dropResources→getDrops 会把真实工具传入 LootParams，TwigBlock 也正用该入口正确读取 Fortune，但 RockBlock 没有消费它。因此 Fortune1 破坏普通自然石／flint 堆，原可得 1或2，当前仍固定1；右键本来两边都只取1，不应把右键也报错。外部修改掉落事件不属于本模块的默认补偿。

不能简单给所有当前 explicit-item 模式一起加 Fortune：现代该模式同时承担自然 flint／陨铁与手摆石，原手摆模式恰好不放大。地牢携带完整栈的 count 支持也已存在，不能作为“所有石头只能存1”的误报。

## ALIGN-200：石堆失去支撑未接邻居移除；木棍已有

原自然石的 onNeighborBlockChange:150–163 在下方不再上表面坚固时丢 getRock(1) 并移除；上方／水平有液体也执行该收起。MultiTileEntityBlock:302 实际把邻居变化转给 IMTE_OnNeighborBlockChange。原自然木棍有同样的支持／液体处理。

当前 RockBlock 只有 canSurvive，没有 updateShape／neighborChanged 或 ticker 补偿。核对 Forge 47.4.20 实际 BlockBehaviour／BlockStateBase 字节码与 NeoForge 21.1.243 实际源码：默认 updateShape 原样返回状态，默认 neighborChanged 只发 debug 通知，不会自动以 canSurvive 失败来销毁普通 Block。项目石块／木棍引用及相关事件／mixin 检索未发现替代回调。因此普通移除下方支撑的邻居更新下，石堆没有原接线；这是具体回调合同，不是只因没同名方法就判缺。

TwigBlock.updateShape:66–71 已检查 canSurvive 和非下方邻居的流体状态，返回 AIR。原生 Block.updateOrDestroy 在常规未抑制掉落的形状更新中会走 destroyBlock，不应把木棍也报成“不掉落直接消失”或“同样不响应支撑”。水实际流入 rock 本格可能通过原生流体路径替换并产生掉落；本轮没有将所有邻水场景都断言为永久悬浮，仅用移除支撑、无流体参与的情形作确定结论。

## ALIGN-201：普通木棍／燧石潜行手摆的内置路径未对应

原 GT_Proxy.java:293–299 接受 vanilla stick／IL.Stick／stickAnyNormalWood，潜行放置手摆木棍32073；vanilla flint 潜行放置手摆石32074。后者保存明确的 flint×1。32073 的 getDefaultStick 明确固定 vanilla stick×1，不保留被消耗木棍的原木种。

当前 MaterialItem.useOn:113–115 调用 RockItemPlacement，后者 :21–24 仅接 rockGt 或 oreRaw；PilePlacementHandler 的全局 RightClickBlock 事件先进入该 helper，随后只处理 ingot／plate／plateGem。VanillaUnificationDefinitions 把 vanilla stick 记为 prefix=stick，把 flint 记为 prefix=null；MaterialEquivalence.form 因而无法将 vanilla flint 误认成可摆的 rockGt。普通原生 Item.useOn 默认 PASS。

全生产 Java 检索得到 twigs 的创建入口只有共享 GTSurfaceFloraFeature.placeTwigs／worldgen 和 NativeOriginWorld 的中心模板路径，没有 stick 手摆入口。rockGt／oreRaw 自身的现代潜行摆放已经接入，而且保存确切 itemId、count=1、rawOre=false，不能误报“石头全部不能摆放”。结论限定本模组默认内置交互，无外部事件替换；没有实测玩家按键。

## 保存、展示和边界

RockBlockEntity 已保存 material、rawOre、itemId、count，并提供更新包／model data；placeRock 和手摆 helper 实际设置这些字段。TwigBlock 无 BE，类似原自然木棍，实际采收时按当前位置重新决定内容，不应要求保存未在原中保存的固定木种。Rock 地面外观族与随机模型、木棍模型、雪覆盖、硬度、放大镜检查、采收音效等未在本轮做完整渲染／原生工具分派验收；Jade 内容提示不能自动等同于已实现原放大镜交互。

普通 GTRocksFeature 已有自己独立的 boolean target 去重、有界射线、farmland 停止、LOGS／LEAVES 跳过、replaceable 检查及区块群系集合门控，不等同于 GTSurfaceFloraFeature 的共用 surface helper。不要把灌木已登记的原木拦截问题复制给普通 rocks。现代海平面值和地面标签可带来适配边界；木棍沿用共用 helper 的扫描／群系差异仅引用既有 [灌木世界生成审计](BERRY_BUSH_WORLDGEN_AUDIT.md) 中 ALIGN-187／188 的范围，不新增相同条目。

旧中心／道路保护、可选维度生成、群系名称到资源 ID 的全映射及本轮未进入的地质生成规则不作默认缺失扩报。未进行实际世界、客户端、专服、旧存档恢复、数据包覆盖或其他模组事件兼容测试。原始工作稿 `work/surface-piles-alignment-audit-20261021/README.md` 与 `sources.json` 收集21份原来源、61份当前来源、48个两端内存投影和4个原生依赖 artifact；生产检索覆盖1242份 Java。现代原生版本为 Minecraft1.20.1／Forge47.4.20 和 Minecraft1.21.1／NeoForge21.1.243。统一回执保留实际文件及 jar／entry 哈希、原生方法锚点、检索范围与有限数学推导；没有将静态入口核查记作已通过游戏验收。
