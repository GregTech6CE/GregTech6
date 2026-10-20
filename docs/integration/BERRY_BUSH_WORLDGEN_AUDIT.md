# 浆果灌木世界生成核查

第五十三批，2026-10-21。观察基线为 `21f68f1b1874e0d99a95e11de5d3bdfe68c9e1fb`；本批排版和注释复核基线为 `e3f08722c`。登记 ALIGN-186–188。本报告补完 [灌木生命周期审计](BERRY_BUSH_AUDIT.md) 留下的世界生成调用链，不重复登记生命周期、扁平化身份或 ALIGN-174–178。

两版均已实际接入共享灌木 feature，基础九种自然灌木、成熟核心和枝条放置已经存在。确认差异是：整数半坐标变成浮点半坐标，改变种类分区；上方原木会截断现代地表扫描；群系判断从整区块名称集合变为候选列海平面的固定 ID 名单。高度扩展、随机调用顺序和中心／道路保护保留为取舍或未证实边界，不能据此直接要求回退现代适配。

本批没有运行 Gradle、编译、游戏、世界生成或新增生产测试，没有修改世界生成行为。有限数学推导只读取实际噪声源码和向量表，不是默认种子地形实测。来源、原生依赖、两端投影和最终文件哈希由[本批统一回执](verification/bush-physical-worldgen-audit-20261021.json)归档。

## 实际入口与来源范围

原版源码根为 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java`，当前项目为 `C:/Dev/GregTech6`。原文件保留 GregTech-6 Team、Gregorius Techneticies 及 LGPL-3.0-or-later 头；本次没有复制原生第三方方法体。原版是本地保存快照，不代表已核对实时上游。

原 `gregapi/GT_API_Proxy.java:150–153` 注册 Forge 世界生成器，`:1432–1433` 把区块坐标转换成方块坐标，调用 `GT6WorldGenerator.generate`。后者 `:48–79` 构造整个 16×16 区块的群系名称集合并调用启用的生成器；`:122–125` 的主世界分派使用 `GEN_OVERWORLD` 或 `GEN_GT`。`gregtech/loaders/b/Loader_Worldgen.java:633` 已将默认启用、数量 1、概率分母 4 的 `WorldgenBushes` 加入这两组。

当前共享 Java 下列缩写均相对于 `multiversion/src/main/java/com/gregtech/gregtech/`；版本入口相对于各自版本目录。

| 环节 | Forge 1.20.1 | NeoForge 1.21.1 |
| --- | --- | --- |
| feature 注册 | `versions/1.20.1-forge/.../GregTech.java:39` 调用 `GTFeatures.register` | `versions/1.21.1-neoforge/.../platform/neoforge/GregTechNeoForge.java:90` 调用同一注册方法 |
| 灌木方块注册 | `MachineBlockBootstrap.java:38` 调用 `GTBushes.registerAll` | `GregTechNeoForge.java:59` 调用 `GTSurfaceBlocks.initialize`，后者 `:10` 调用 `GTBushes.registerAll` |
| 共享 feature | `worldgen/GTFeatures.java` 的 Forge 分支注册 `gt_bushes → GTBushesFeature` | 同文件 Neo 分支注册相同 feature；不是缺少 Neo 专用实现 |
| 世界生成资源 | 版本 `forge/biome_modifier/gt_bushes.json` 向 `#minecraft:is_overworld` 的 `vegetal_decoration` 添加 feature | 版本 `neoforge/biome_modifier/gt_bushes.json` 做相同添加 |

core 的 `data/gt6ce/worldgen/configured_feature/gt_bushes.json` 指向 `gt6ce:gt_bushes`；同名 placed feature 的 `placement` 为空列表。Forge 47.4.20 的 `ForgeBiomeModifiers.AddFeaturesBiomeModifier.modify` 和 NeoForge 21.1.243 的 `BiomeModifiers.AddFeaturesBiomeModifier.modify` 均在 ADD 阶段把 feature 加入匹配群系的生成阶段。原生 `PlacedFeature.placeWithContext` 从 origin 开始遍历 placement modifiers，再调用 configured feature；这里没有额外修正灌木半坐标的步骤。Forge 实际 class 字节码与 Neo 实际 source 均已核查。

`multiversion/build.gradle:24–36` 接入生成源码／资源及版本源码／资源；Forge `gradle/platform-forge.gradle:361–368`、Neo `gradle/platform-neoforge.gradle:189–195` 将 core 输出放入发行包。以上是实际配置和源码入口证据，本次没有重新构建确认最终包内容。

## ALIGN-186：整数半坐标改成浮点，种类分区不再等价

原 `WorldgenBushes.java:66` 传入 `aX/2, 300, aZ/2`。因为坐标是 int，Java 先执行向零截断的整数除法，再转换成 `NoiseGenerator.get(float,...)` 参数。当前 `GTBushesFeature.berryIndex` 使用 `x / 2.0F, 300.0F, z / 2.0F`，保留奇数坐标的半格。

不是只凭参数文本判断：原 `NoiseGenerator.java:24–96` 与 core `GTCellNoise.java:11–57` 的种子转 int、频率、27 个候选格搜索、哈希常数、32 位溢出、最近格输出和选项索引公式已逐项比对。主世界原维度偏移为零；原向量表 `CELL_3D` 与当前 `CELL` 的 **256 个向量、768 个 float 分量转 binary32 后完全相同**，大端 float 位串 SHA-256 为 `f8a923f008d4438b3ebd875ac51c612cdc53907ec4e84f28d59ce3c377372ebd`。本次候选距离远小于原初始值 999999，因此当前改用 Float.MAX_VALUE 不改变这些反例的胜出格。

用实际源码表、种子 0、主世界、选项数 9 做有限数学推导：

| 候选坐标 (x,z) | 原整数半坐标 (x,z) | 原索引 | 当前浮点半坐标 (x,z) | 当前索引 |
| --- | --- | --- | --- | --- |
| (340,128) | (170,64) | 8 | (170,64) | 8 |
| (341,128) | (170,64) | 8 | (170.5,64) | 2 |
| (-302,-128) | (-151,-64) | 4 | (-151,-64) | 4 |
| (-303,-128) | (-151,-64) | 4 | (-151.5,-64) | 5 |

正坐标反例中，原 x341 经过频率缩放得到 float x=`1.5299999714`，最近格为 `(1,23,1)`，噪声值 `0.7946521044`；当前缩放后 x=`1.5344998837`，最近格变为 `(2,23,1)`，噪声值 `-0.4806706011`。推导按 Java float 每步舍入、有符号 32 位溢出和 Math.round 计算；没有把双精度实数近似直接当成原执行结果。完整输入、距离和中间值见统一回执所收来源稿。

原 `CS.BushesGT.MAP` 使用 `ItemStackMap`，后者实际继承 HashMap（`CS.java:1581–1589`、`ItemStackMap.java:35`），不能把源码注册顺序直接当成旧 keySet 遍历顺序。本反例不依赖该顺序：原 x340／341 输入完全相同，任何固定旧 Map 顺序都会选同一种；当前两点分别选当前列表中的 raspberry 与 candleberry。对旧九项作任何固定置换，也不能同时让这两个点与当前一致。

两个正坐标都位于区块 `(minX336,minZ128)`，列偏移 `(4,0)`／`(5,0)` 是抽列的合法输入，并避开旧中心和道路保护带。满足允许群系、Y64 可种植地面、上方为空、概率通过和注册表可用时，实际种类选择分支可以处理这些列。**没有声称种子 0 的默认地形正好在两点满足条件、固定 RNG 实际抽中了两点、同一次生成会生成两棵，或已经测量全世界种类频率。**确认的是可处理输入上的种类分区合同不同。

## ALIGN-187：上方原木截断地表扫描

原 `WorldgenOnSurface.generate:54–72` 在有限高度范围从上向下扫描，跳过非液体的非完整块、wood 和 leaves；碰到 farmland 停止；第一次实际接触后仅尝试一次。`WorldgenBushes.tryPlaceStuff`／`placeBushCore` 随后检查接触块属于 plantableGreens，且上方满足 `WD.easyRep`。原 GT 的 `BlockBaseLog.java:78` 明确返回 isWood=true。

当前 `GTBushesFeature.placeBush` 调用 `GTSurfaceFloraFeature.surface:189–202`。helper 从 MOTION_BLOCKING_NO_LEAVES 高度图扫描，只有“不坚固且不在 LEAVES／LOGS 标签中”的块才被跳过；原木或扫描途中遇到的树叶会成为返回的接触点。之后 `BushBlock.isPlantableGround` 拒绝该原木，feature 不再继续向下寻找地面。

确定的列条件反例是：允许群系中，Y64 为可种植地面、Y65 为空、Y70 为悬出的实心原木枝干，中间为空；原木分别满足旧 isWood 和当前 LOGS 识别。旧默认射线经过 Y70 时跳过木头，继续找到 Y64，可以放置核心；当前高度图顶为 Y71，helper 先返回 Y70 原木，地面检查失败。两版原生 `logs → logs_that_burn → oak_logs` 标签链均包含 oak_log；原生 NO_LEAVES 高度图排除树叶而保留阻挡移动的原木，相关 source／class／资源 entry 哈希已收集。

这是合法列状态下的确定控制流差异，不等于“所有树林都不生成”，也不等于已经在默认种子世界定位并实测某棵枝干。旧 easyRep 与当前 canBeReplaced 对所有雪、火、树叶和其他替换物的差别尚未逐项核实，不能将其全部并入本项。

## ALIGN-188：整区块群系门控改成候选列海平面 ID 门控

原 `GT6WorldGenerator` 汇总整个区块 16×16 的群系名称，`WorldgenBushes.canGenerate:54–57` 只要集合中任意名称同时满足非 frozen 且属于 plains／woods，就返回生成数量；抽中的列随后不再检查自身群系。`BiomeNameSet.java:44–56` 做不区分大小写的精确名称匹配，并自动补“ m”变种，不是名称里含 forest／woods 就自动通过。

当前 `GTBushesFeature.placeBush` 先对抽中的列调用 `biomeId`，该方法采样 `(x, seaLevel, z)`，再按固定资源 ID 名单检查。因此混合群系区块、垂直群系以及名单映射的允许范围与原合同不同；不能只因两边都写着 plains／woods 就判定等价。

无需可选模组的名单反例是纯 Taiga 区块：原 `CS.java:271,276` 的 woods／plains 未包含原生 Taiga 或 Mega Taiga。原生 1.7.10 `BiomeGenBase` 的本地 ahu 字节码显示 id5 名称 Taiga，SRG 映射已核对；当前名单明确包含 `minecraft:taiga`，两版原生 `is_overworld` 标签也包含该 ID。在其余生成条件满足时，旧 canGenerate 为 0，当前可以到达 1/4 尝试。

本项记录门控合同与实际允许范围，不盲目要求删除现代 taiga、meadow、cherry 或 old-growth 映射。哪些现代群系应视为旧群系的继承、哪些扩围有意保留，需要明确适配政策；也不能把旧可选模组名称整份缺少逐项登记为无条件现代缺陷。

## 已有实现与不新增编号的边界

基础自然池成员对应：默认 string／cotton，加 blueberry、candleberry、cranberry、black currants、white currants、red currants、blackberry、raspberry，共九项。原默认项位于 `CS.java:1589`，八次登记位于 `MultiItemFood.java:397,405,409,413,417,421,425,429`；当前 `BerryBushCatalog.worldgenTypes` 提供相同成员。当前材料莓果的手动识别是另一条 catalog 路径，不代表自然池漏注册。旧 HashMap 索引顺序没有证明与当前列表相同。

成熟 stage3 核心、四个同地面高度邻居核心的独立布尔尝试、每核心四个侧枝加顶枝、朝向核心的 support 均已有对应，见原 `WorldgenBushes:71–94` 和当前 `GTBushesFeature.placeBush`／`placeBranches`。默认数量 1、概率分母 4 仍在，不能把以上整体写成“灌木世界生成未移植”。

- **高度政策：** 原默认主世界世界高 256、`WD.waterLevel` 为 62，接触地面扫描范围为 Y61–138。当前按现代高度图扫描到 `minBuildHeight+1`，min=-64 时最低接触 Y=-63，允许旧射线不覆盖的低地／高地。无遮挡、可种植地面 Y50 的合法列可说明范围不同；现代高度适配可能有意如此，不直接要求恢复旧范围。TFC 等可选整合的水位 143 不是默认值。
- **随机消费顺序：** 原先抽列再 roll 概率，当前先 roll 再抽列；横向四个布尔分支的遍历顺序也不同。会改变固定 RNG 序列消费，但不足以单独证明边际概率或全世界密度错误。
- **中心和道路：** 原 `WorldgenObject.checkForMajorWorldgen:70–75` 排除道路／中心保护带；当前灌木 feature 没有该本地调用。本次未把现代 origin feature 的阶段排序、覆盖和实际地面条件追到足以证明受侵入，不登记“必定覆盖 Nexus／roads”。
- **其余条件：** 原 farmland 停止与当前碰撞面／替换判定的完整组合、旧 easyRep 与现代 canBeReplaced 的全部情况、setBlock flags 的最终影响仍需具体场景核对。Harvestcraft cotton、旧 Galacticraft／Aether／其他维度注册均为条件整合，不能在提供者不存在时直接报默认缺功能。

本次没有客户端／专服世界实测，没有旧配置、旧存档、动态数据包或所有外部种类提供者兼容验收。有限坐标数学推导不替代这些验证。

## 排版、来源与原生证据

观察稿位于 `work/berry-worldgen-audit-20261021/README.md` 和 `sources.json`，收集 14 份原来源、19 份当前来源、14 个两端内存投影及 7 个原生依赖 artifact。现代依赖为 Minecraft 1.20.1／Forge 47.4.20、Minecraft 1.21.1／NeoForge 21.1.243；原生 1.7.10 客户端和解码 SRG 只用于群系名称证据，没有重放全部旧 Forge 二进制补丁。

本批对共享 `GTBushes.java` 仅排版，对 `GTBushesFeature.java` 修正过强归因注释并调整换行，未改变注册、群系名单、噪声输入、随机顺序或玩法。精确四视图执行词法／字面量位置证明位于 `work/bush-worldgen-readability-20261021/proof.json`：

| 文件 | 排版后行数 | 排版后 SHA-256 |
| --- | --- | --- |
| `registry/GTBushes.java` | 58 | `60a8b34f84e6f1e547bd436209eb03034b26c183646c4f2cc2a77a7806bdcddc` |
| `worldgen/GTBushesFeature.java` | 135 | `06e66ec4e793271cda75b017b713fd977c9f21adbea51f4aaf4ad9e0ee96cc36` |

源码行号可能随纯排版移动，本报告当前 feature 优先使用方法锚点。旧观察稿保留当时哈希，统一回执记录最终文件及词法等价证明；不能把注释更正当作已经修复 ALIGN-186–188 的行为差异。
