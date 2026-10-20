# 浆果灌木生命周期核查

第五十一批的灌木子域，2026-10-21。初查观察基线为 `2d14d400914e0e2567d20dd0c546efdf43cb65c1`；本批排版与交叉复核基线为 `396a9ee55168c9297b2db76a5eda0e73a73b0d43`。新增 ALIGN-174–178。当前 `BushGrowthRules.java`、`BushBlockEntity.java` 仅展开排版并修订过强注释，未在整理中改变生长、环境或交互逻辑；执行词法项、字面量及最终文件哈希的证据由[本批统一回执](verification/bush-menu-audit-20261021.json)记录。以下当前行号按本批排版后的文件。

本批没有运行 Gradle、编译、新测试、游戏、世界重启或构建，没有推送；源码与核查文档仅在本地保存。原模组根为 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java`，当前根为 `C:/Dev/GregTech6`。原文件保留 GregTech-6 Team／Gregorius Techneticies 与 LGPL-3.0-or-later 头；本报告只引用行为证据，没有移入原生依赖源码。

## 身份政策与实际入口

用户要求的扁平化已实现：食品、棉花、材料莓分别使用独立灌木注册 ID。物种不再依赖新堆叠的 NBT；`stage=0..3` 和 `support=0..6` 表示成熟阶段和支撑方向，继续使用方块状态是合理的实现。旧 bare ID 的 `berry` 键属于兼容入口，不能因其与原版字段不同就报物种功能缺失，也不建议恢复以 NBT 表示新物种身份。

原主体为 `gregtech/tileentity/plants/MultiTileEntityBush.java`。`gregtech/loaders/b/Loader_MultiTileEntities.java:2030` 注册 32759，`Loader_Worldgen.java:633` 加入活跃 `WorldgenBushes`。它继承 GT 方块实体 ticker，不是 Minecraft 甜浆果丛，也不靠随机 tick 生长。

当前共享 Java 简写路径以 `multiversion/src/main/java/com/gregtech/gregtech/` 为根；`versions/...` 及 `platform/neoforge/...` 路径位于对应版本目录，后者的完整根为 `multiversion/versions/1.21.1-neoforge/src/main/java/com/gregtech/gregtech/`：

| 环节 | Forge 1.20.1 | NeoForge 1.21.1 |
| --- | --- | --- |
| 变种注册调用 | `versions/1.20.1-forge/.../bootstrap/registration/MachineBlockBootstrap.java:38` 调 `GTBushes.registerAll` | `platform/neoforge/GregTechNeoForge.java:59` 调 `GTSurfaceBlocks.initialize`，共享 `GTSurfaceBlocks.java:10` 再调相同入口 |
| 共用工厂 | `registry/GTBushes.java:25–42` 根据食品／棉花目录及材料目录注册 `BushBlock`、`BushBlockItem` | 相同工厂的 Neo 分支；入口 `GregTechNeoForge.java:87–90` 注册方块、物品、BE 和世界生成 |
| BE 类型 | `registry/GTBlockEntities.java:541–544` 的 Forge 分支，包含 `GTBushes.allBlocks()` | 同文件 `:86` 的 Neo 分支，包含同一变种集合 |
| 实际生长 | `block/plant/BushBlock.java:204–207` → `blockentity/BushBlockEntity.tick:90–103` | 同一共享 ticker 与业务；不是缺少一个 Neo 专用生长类 |
| 实际交互 | `BushBlock.java:215–235` 的 use | 同文件 `:260–262` 的 useItemOn／useWithoutItem 转发到共享 interact |
| 世界生成 | `GTFeatures.java:106–107` 注册 `GTBushesFeature`；Forge biome_modifier 接入 | `GTFeatures.java:67` 注册相同 feature；Neo biome_modifier 接入 |

世界生成的 configured／placed feature 数据在 `core/src/main/resources/data/gt6ce/worldgen/`，两版本各自的 biome_modifier 把 `gt_bushes` 加入主世界 vegetal_decoration。食品八种、棉花及材料莓的种类目录已有主体实现；不能按没有原同名类或字段认定未移植。

## 已有生命周期对应

| 环节 | 原版证据 | 当前实现与范围 |
| --- | --- | --- |
| 设定输出 | `MultiTileEntityBush.java:165–177`，空灌木采用手持输出，不消耗该物品 | `BushBlock.java:231–235` 不 shrink；`BushBlockEntity.setBerry:50–67` 切注册变种时保留阶段、支撑和增长余数。准入范围的原 helper 陷阱另列政策边界。 |
| 核心与枝条 | 原 `:183–197`，可种地面或附在相容核心六方向 | `BushBlock.java:121–132` 已检查核心和种类相容；`:55–65,98–105` 的核心整块、枝条 4px 选框／2px 碰撞对应原 `:252–263,284–320`。 |
| 扩张 | 原普通 tick 没有自动复制灌木；世界生成才创建附近核心与枝条 | `GTBushesFeature.java:92–120` 已有生成时扩张。不能把没有随成熟自动 spread 当成缺口。 |
| 成长与成熟 | 原 `:131–138`，每 128 tick 累加速度，byte 每 256 次回零进一阶段，阶段 3 停长 | 当前 `BushBlockEntity.java:101–140` 与 core `BushGrowthRules.advance:10–16` 实现相同周期、`&255` 余数及四阶段。成熟当轮剩余累加保留；采收只重置阶段，不清增长余数。环境输入的实际差异见 ALIGN-174／175。 |
| 日夜与季节 | 原见天空时不另要求白天，没有季节日历 | 当前也没有昼夜／季节门控。生成群系限制不等于已有灌木只能在这些群系生长。 |
| 枝条继承 | 原 `:110–123` 取得父莓种／速度，父块消失后 popOff | 当前 `BushBlockEntity.java:147–177` 已有继承与失支撑掉落；检查时机差异见 ALIGN-176，不能报成完全不继承或永不掉落。 |
| 采收 | 原 `:167–171`，成熟返回 1–2 个输出并 stage=0 | 当前 `BushBlock.java:220–227` 返回 1–2 个输出并 stage=0；背包满时可掉落。原溢出位置取点击面，当前取玩家附近，没有声称掉落坐标完全一致。 |
| 携带与重新种植 | 原 `writeItemNBT2:80–82` 仅携带输出，不携成熟度／增长余数 | 当前 `BushBlock.java:150–180` 返回普通变种物品，正常放置从未成熟开始。旧 `berry` 转换已实现，但独立旧存档重载未测。 |
| 自动化 | 原继承库存最小尺寸为 0，`:350` 不掉内部槽，没有成熟莓内部库存 | 当前 `BushBlockEntity` 无 item handler，Neo 能力注册也没有 BUSH。不能把漏斗无法直接取成熟莓说成漏掉原本存在的产物库存；外部自动右键器须另测。 |
| 碰撞伤害 | 原灌木主体没有甜浆果荆棘伤害／减速逻辑 | 当前没有增加该机制，不能以现代 Minecraft 甜浆果丛为基准误报。 |
| 保存 | 原保存输出、阶段、增长，继承 GT 周期标脏 | 当前物种在注册 ID、阶段／支撑在 block state，增长在 BE `:188–202`；成功增长 `:139` 明确 setChanged，阶段改变还 setBlock，切物种 `:66` 也标脏。不能照搬养蜂纯寿命／倒计时的未标脏结论；本批未真实重启。 |

## ALIGN-174：雨天加速额外受群系降雨／降雪判定影响

原 `MultiTileEntityBush.java:133–135` 在见天空时，使用 `world.isRaining() && getRainOffset(...)` 再增加一次 speed。实际 helper `gregapi/tileentity/base/TileEntityBase01Root.java:266–270` 的 getRain 检查维度 hasNoSky、未加载边界及 `getPrecipitationHeight <= y`，没有群系是否干燥或降雪的条件。

当前 `BushBlockEntity.java:124–126` 使用 `level.isRainingAt(sample)`。本机 Forge 47.4.20 mapped class 和 NeoForge 21.1.243 的原生 `Level.isRainingAt` 均在全局雨、见天空、高度检查后，要求 `biome.getPrecipitationAt(pos) == RAIN`。Neo 源 `Level.java:1016–1025` 与 Forge 方法字节码 offset53–61 已交叉核对，依赖 jar／entry 身份见统一回执。

因此，手动种在沙漠或降雪群系、地面／天空等条件满足且世界全局处于雨天的灌木，原可获第二次增长，当前不获该加速。普通降雨群系仍可对应；自然生成不选这些群系也不排除手动种植的可达性。本结论是实际门控差异，没有进行游戏天气复现，不扩大为所有天气增长错误。

## ALIGN-175：屋顶下光照 helper 的截断行为不同

原 `MultiTileEntityBush.java:137` 表面上检查 `getLightLevelOffset(...) > 9`；实际 `Base01Root.java:251–254` 是 `UT.Code.bind4((long)worldObj.getLightBrightness(...) * 15)`，先转 long，再乘 15。`UT.java:1556` 只钳制到 0..15，不会恢复已经截掉的小数。

独立原生证据来自本机原始 Minecraft 1.7.10 client，经本机 Forge SRG 确认：`ahb.n(III)F` 为 World.getLightBrightness／func_72801_o，读取 `WorldProvider.lightBrightnessTable`；`aqo.a()V` 为 generateLightBrightnessTable／func_76556_a，普通维度表使用 `(1-f)/(3*f+1)`。该表光照 0–14 的亮度小于 1，15 才为 1。因此原普通维度 helper 实际给出 0 或 15，屋顶下要满亮度才通过 >9。

当前 `BushBlockEntity.java:127` 直接检查 `getMaxLocalRawBrightness(sample) > 9`，故原始光照 10–14 也可增长。不能仅因两处注释都写“光照大于 9”就说实际行为完全相同。

这很可能属于原版 helper 的缺陷或历史瑕疵。正式记录的是实际行为差异与后续取舍，**不自动要求恢复该截断，也没有在排版中恢复它**。第三方维度可覆盖亮度表，不能推广成所有维度均只接受 15；旧 Forge 二进制补丁没有重放，也未运行旧客户端。原 helper 源码与普通原生亮度表字节证据分别保留。

## ALIGN-176：支撑／父种变化缺少邻居更新的即时重查

原活跃链为 `gregapi/block/multitileentity/MultiTileEntityBlock.java:302` 转发邻居变化，`TileEntityBase04MultiTileEntities.java:51,222–224` 实现该接口并令 `mBlockUpdated=true`，`MultiTileEntityBush.java:105–123` 下次服务端 tick 立即重查父块、取得种类／速度或 popOff。Base03 `:159` 在 tick 末尾清标志。

当前 `BushBlock` 只有 canSurvive，没有覆写 neighborChanged／updateShape。两版原生 BlockBehaviour 的默认更新不调用这个灌木的 refreshSupport；Neo 源 `BlockBehaviour.java:157–166` 与 Forge 实际类均已核对。首次检查完成后，`BushBlockEntity.tick:97–102` 在世界时钟 `%128==64` 重查，`grow:115` 在 `%128==0` 也重查。

所以失支撑枝条仍会掉落，但可等约一个 64 tick 间隔；父物种变化也有同类继承延迟。范围限定在仍正常 ticking、已完成首次检查的灌木，不把它写成永久悬空或永久不继承。本批没有游戏计时测量。

## ALIGN-177：自有六种彩色草没有进入灌木可种地面集合

原 `MultiTileEntityBush.java:127,191–195` 接受 `BlocksGT.plantableGreens` 或支持黄花的地面；`gregtech/blocks/BlockGrass.java:68` 把自有彩色草加入该集合。这是原内建支持，不能只按旧 Forge API 名称判断。

当前 `BushBlock.isPlantableGround:116–117` 仅接受 DIRT 标签、MOSS_BLOCK 或 FARMLAND。共享 `GTGrassBlock.java:43–65` 在两版本已有现代植物支持接口，但灌木没有查询它。Forge `GTIconSetBlocks.java:118–143` 和 Neo `GTGrassSoils.java:3` 均注册 `GrassSoilCatalog.SPECS` 的六种草：`grassblock_brown`、`grassblock_dark`、`grassblock_light`、`grassblock_medium`、`grassblock_normal`、`grassblock_yellow`。

自带资源里，这六种 ID 只出现在 mineable/shovel 标签，没有 DIRT 成员资格。Forge 自带 `data/minecraft/tags/blocks/dirt.json` 只额外列 plant_pot／mud／turf。交叉复核也检查了动态标签／资源包，没有发现把六种草加入 DIRT 的补偿。因此在项目自带默认数据下，它们声明能支持普通花，却被灌木核心放置和 `BushBlockEntity.speed:154–155` 拒绝。

外部数据包可额外赋予 DIRT，故不声称任意整合包都必现，也不泛化成所有第三方土壤拒绝。本批没有重载实际标签或游戏放置验证；两平台注册与默认数据的静态证据见统一回执及交叉复核来源。

## ALIGN-178：放大镜的原 Grows 输出读数未接入

原 `MultiTileEntityBush.java:155–161` 对 TOOL_magnifyingglass 输出 `Grows <物品名>`，返回工具开销 1，不进入成熟采收。

两版实际注册的 MAGNIFYING_GLASS 都是共享 `GTToolItem`：`GTToolItems.java:37,48–52`；Neo 的特殊 SmelteryToolItem 只替换 CHISEL／PINCERS，不替换放大镜。`GTToolItem.onItemUseFirst:83–118` 的放大镜路径先到 `MultiblockTools.use`，但后者 `:27–34` 要求目标是 MultiblockToolTarget，`BushBlockEntity` 并非该接口。后续 `GTToolItem.useOn:145–153` 对这种工具也返回 PASS。交叉复核的全局右键处理器没有提供灌木 Grows 读数补偿。

当前 `BushBlock.java:218–235` 无放大镜分支。因此在**已配置有效输出、普通非潜行交互且没有外部事件拦截**时，未熟灌木没有原 Grows 读数；成熟灌木可继续进入普通采收，而非放大镜读取。这里不是宣称所有潜行／第三方交互都必定采收，也未进行客户端／专服工具实测。

物品栏 `BushBlockItem` 的青色输出 tooltip 已存在，但不能替代世界灌木上的工具读数；该缺口不代表工具整体未移植。

## 原版宽松输出与扁平化政策边界

原 `onBlockActivated3:174` 的 `BushesGT.get(aStack) != null` 不能读成“只允许已注册莓”。原 `CS.java:1583` 对未知物品返回 DEFAULT，`UT.Code.getWithDefault:1375–1376` 返回非空默认数组。因此原空灌木实际上可以采用任意有效物品为输出，并使用默认外观。

当前 `GTBerryBushes.of` 与 `GTBushes.byBerry` 要求可识别类型和已注册变种，收窄了这个原宽松／疑似缺陷行为。用户已明确要求独立变种 ID，本次不建议以 NBT 恢复任意物品身份，也不把禁止普通石头等任意物品种入直接编号为应恢复玩法。合法外部材料莓／食品提供者应另核现代注册扩展合同；食品八种、棉花、已定义材料莓的主体路径已经存在。

## 未完成范围

- 全种类／外部提供者及完整生存链没有穷举；没有以历史 GameTest 文件存在冒充本批通过。成功增长会标脏的事实不同于前批养蜂纯寿命／倒计时问题。
- 原 Harvestcraft 棉花替换、Aether enchanted grass 的 speed=2、Galacticraft 氧气、额外维度和旧群系字典都属于条件整合。没有对应提供方时，不按缺同名旧 API 直接新增功能缺失编号。
- 当前世界生成已有成熟核心、相邻核心、枝条与食品／棉花池，但全部群系、扫描高度、农田停止、随机选择顺序及分布尚未核完。原 `WorldgenBushes.java:66` 使用整数 `aX/2,aZ/2`，当前 `GTBushesFeature.java:83` 使用 `/2.0F`，仅记录奇数坐标输入差异；未计算具体输出反例，不扩大成全部噪声错误。
- 原全局 SERVER_TIME 与当前 level.getGameTime 的相位及首次检查时序仍有边界；同为 128 周期不表示逐 tick 相位相同，正式账本应与已有定时项避免重复计数。
- 原雪层移除、当前 `refreshSupport:161–162` 都有实现；响应时机受 ALIGN-176 影响。核心失去地面后停止生长与枝条失去父块掉落是不同条件，不能混写。
- 原硬度、可燃传播／易燃各 300、核心光遮挡与当前 instabreak／noOcclusion 等物理属性还需单独核对。本批没有燃烧／光传播试验，不另编号。
- 未实测自动右键收割器、保护事件、第三方数据包、跨区块父块加载、专服和独立旧存档重启。

来源包含初查 `work/berry-bush-audit-20261021/sources.json` 与独立 `work/berry-crosscheck-20261021/sources.json`；作者、原始文件、实际原生依赖 jar／entry 哈希、排版前后身份与本批未验证范围由[统一回执](verification/bush-menu-audit-20261021.json)汇总。没有宣称浆果灌木全部边界或世界生成已核查完成。
