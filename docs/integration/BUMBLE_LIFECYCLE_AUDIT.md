# 养蜂世代与鲜基因核查

第四十九批，2026-10-21，基线 `bbdcb7f73`。新增 ALIGN-164–167，自然巢温度计扩展 ALIGN-085。两份 core 规则只做排版，执行词法流及字面量位置与基线相同，没有借整理修改任何生命周期、随机顺序或基因数值。来源及比较见[本批回执](verification/inventory-bumble-audit-20261021.json)。没有 Gradle、编译、新测试、游戏、构建或推送。

原版根：`C:/Dev/gregtech6-master/gregtech6-master/src/main/java`。当前根：`C:/Dev/GregTech6`。原版文件保有 GregTech-6 Team / Gregorius Techneticies、LGPL-3.0-or-later 源文件头，本次只引用行为证据。

## 活跃入口与一个完整周期

原版不是只有物品名。`gregtech/loaders/a/Loader_Items.java:47` 实例化 `MultiItemBumbles`；`gregtech/loaders/b/Loader_MultiTileEntities.java:2041,2222–2223` 注册自然巢、普通/高级养蜂箱；`Loader_Worldgen.java:635–644` 把 `WorldgenHives` 加入维度生成队列。养蜂箱的 `onTick2` 是实际生产/繁殖逻辑，不能把对应 UI 配方 `RM.BumbleQueens` 当成全部功能。

当前两版本实际入口：

| 环节 | 1.20.1 Forge | 1.21.1 NeoForge |
| --- | --- | --- |
| 注册调用 | `versions/1.20.1-forge/.../GregTech.java:46,53` 引导初始化；`bootstrap/registration/MachineBlockBootstrap.java:34,41` 调 `GTToolBlocks.registerAll`、`GTDecorBlocks.registerAll` | `platform/neoforge/GregTechNeoForge.java:58` 调 `GTBumbleBlocks.initialize`；87–90 注册方块、BE、世界生成 |
| 巢/箱工厂 | `GTToolBlocks.java:195` 调 `GTBumbleBlocks.initializeApiaries`；平台 `GTDecorBlocks.java:122` 调 `initializeHives` | 相同 `GTBumbleBlocks.java:16–27` 工厂 |
| BE 类型 | 共享 `registry/GTBlockEntities.java:406–409,547–550` 的 Forge 分支 | 同文件 `:84–85` 的 Neo 分支 |
| 服务端周期 | 共享 `block/tool/BumbliaryBlock.java:83–87` → `BumbliaryBlockEntity.serverTick:250–258` → `tickLogic:296` | 同一逻辑，Stonecutter 分支只改变 Minecraft/loader 边界 |
| 自动化 | `BumbliaryBlockEntity.java:757–759` 暴露映射过的输出 handler | `platform/neoforge/machine/BasicMachineRegistries.java:59` 注册 `BumbliaryBlockEntity::itemCapability`（共享 BE `:742`） |
| 自然巢生成 | `GregTech.java:39` → 共享 `GTFeatures.java:146–147`；Forge `biome_modifier/gt_bumble_hives*.json` | `GregTechNeoForge.java:90` → `GTFeatures.java:66`；Neo 对应 `biome_modifier` |

以上当前 Java 简写路径均在 `multiversion/src/main/java/com/gregtech/gregtech/`；明确写 versions/platform 的是版本目录。

生命周期对应：

1. `WorldgenHives.java:195–203` 取环境生成基因，填蜂脾、公主、雄蜂。当前 `worldgen/GTBumbleHivesFeature.java:291–305,318–334` 同样把真实环境基因给三组物品，蜂脾数量 `(work+999)/1000` 对应原版向上取整。当前生成的是 `bumble_hive_<color>` 固定方块 ID，遵循用户扁平化要求。`FixedBumbleHiveBlock.java:17–18` 不注册颜色状态；旧 `COLOR` 属性和 `migrateLegacyVariant` 是兼容分支，不是当前新巢身份。
2. 自然巢是生成时填满的九格静态容器。原 `MultiTileEntityBumbleHive` 没有养殖 tick，当前固定自然巢也没有生产 tick，不能据此报“自然巢不会繁殖”。原 `:93–99` 禁止自动化、只在玩家采集时释放库存；当前 `BumbleHiveBlock.java:90–92` 在玩家破坏时释放/清空，BE `:121–124` 的 Forge handler 禁用，Neo 未发现给自然巢注册 item capability。两版巢的方块 loot table 都是空 pools，不额外再生成自身。非玩家破坏未原生复现。
3. 养蜂箱先读温度、湿度、天空环境。原普通/高级 `:95–112` 和当前 BE `:250–290` 首 tick / 每 1200 tick 更新，天空标志均一旦 true 后不主动回 false。环境影响见下方边界，不把刻意保留的天空锁存判成新 bug。
4. 普通箱 36 格、高级箱 20 格，以及 comb、royal、drone、dead 分组已由 `core/.../BumbliarySlotLayouts.java` 提供。高级格 11 同时出现在蜂脾/备用雄蜂列表，是原版 `SLOTS_COMBS`/`SLOTS_DRONE` 原样情况，不应擅自认定重复槽位错误。
5. 公主等待倒计时、选雄蜂、同种突变、异种四路交配、逐基因遗传、生成 queen + pending brood 均已实现。当前 `BumbleBreeding.java:154–186` 与原箱 `:218–266` 对照；BE `:358–384` 传入是否主雄蜂槽，因此“主槽两只雄蜂成本/备用槽一只成本”保留。不能用不带布尔参数的公开 overload 默认值误报实际机器成本。
6. 女王环境有效时逐 tick 扣寿命，周期攻击并尝试蜂脾。当前 `tickLogic:311–323`、`produceComb:444–458`；原 `:120–186`。普通/高级产品随机上界分别 10000/20000，工作地点半径分别 3/1，均已对应。原 `MultiItemBumbles.java:492–499` 内建每种只有一个产品且 `bumbleCanProduct` 总通过；当前一个产品不等于漏掉原版内建次产品。
7. 女王死亡清理、分配后代、晋升公主主链存在，但有下面两个确定差异，会改变跨代行为。
8. 玩家从上方开箱、scoop 模式、5 分钟扰动、攻击、温湿计读数都在 `BumbliaryBlock.java:98–127`。当前普通开箱和 scoop 工具不是空壳。温度计工具消耗合同及 GUI 全槽位权限没有本轮完全核完。
9. 养殖箱库存、女王寿命、配对冷却、未出生 brood 的 NBT 写读存在：当前 BE `:630–685` 对应原 `:66–85`；蜂基因在 1.20 NBT / 1.21 components 存储，`BumbleBeeGenes.java:70–90` 有实际分支。运行时拆箱杀死活蜂、释放库存路径 `BumbliaryBlock.java:154–172` → BE `:694–712` 也存在。此处只确认实现链，未声称存档重启通过。

## 确认差异

### ALIGN-164：女王寿终没有清空旧死蜂槽

原普通/高级 `MultiTileEntityBumbliary*.java:124` 对死蜂槽里的所有 `IItemBumbleBee` 调 `slotKill`。原 `gregapi/tileentity/base/TileEntityBase05Inventories.java:96` 明确是 `mInventory[aIndex]=null`，并非把活蜂变死蜂。

当前 `BumbliaryBlockEntity.java:399–402` 调 `kill(found)` 后重新写回原槽；已死蜂会继续占槽。后续 `:404–412` 再存刚死亡的女王/备用蜂，容量及保留世代因此变化。`kill:587–592` 保持数量、基因与扫描状态，这一差异不能归因于扁平 ID。

影响：旧尸体没有按原版在世代结束时清掉，新的死亡输出可能更早找不到容纳槽。代码确认；尚未原生复现最终掉落/丢失体验。普通、高级及两个平台共用。

### ALIGN-165：空主雄蜂槽会吞进第一只公主，可能中断世代衔接

原普通/高级 `:135–143`，尤其 `:137`：`if (!slotHas(SLOT_DRONE) || !addStackToSlot(SLOT_DRONE, tOffSpring))`。主槽为空时，短路直接往备用槽放后代，不尝试填主槽。

当前 BE `:415–420` 无 `!slotHas` 条件，总先尝试主槽。`addStackToSlot:538–544` 对空槽接收任何内部写入堆叠，没有 GUI 类型过滤；`BumbleBreeding.java:154–161` 构建 offspring 时公主排列在前。随后当前 BE `:428–439` 和原 `:147–163` 一样，只扫描备用雄蜂槽挑下一任公主。

可明确推导的边界：主雄蜂槽为空、本窝仅一只公主且该公主通过环境检查时，当前唯一公主进入主雄蜂槽，晋升扫描找不到她，royal 保持空；原版放备用槽后可晋升。这个边界自然可能出现（公主数 `1+nextInt(5)/2` 可等于 1，繁殖后雄蜂可被耗尽），不只是图标/排序差异。未创建测试或启动游戏。

### ALIGN-166：鲜基因雨/雷随机阈值截断（细小边界）

原 `gregapi/item/bumble/IItemBumbleBee.java:147–148` 比较 `nextInt(10000/20000) < rainfall*10000` 的 float 阈值。当前 `core/.../BumbleGenomeRules.java:33–34` 先 `(int)(rainfall*10000)` 截断。

实际可达链：`GTBumbleHivesFeature.genesFor:321` 直接取任意群系 `downfall()` float → `:334` → `BumbleBeeGenes.fromEnvironment:110–113` → core。没有提前量化雨量。

例如 `hasSky=true` 且 `rainfall=.314159f`，原 `3141 < 3141.59...` 为 true，当前 `3141 < 3141` 为 false。正的非整数阈值处于该随机取值范围内时，雨基因减少一个 1/10000 随机点，雷基因减少一个 1/20000 随机点；地下不做这份天气抽样，部分原版常见湿度乘积恰为整数，也不表现差别。不要扩大为“所有天气基因概率错误”。

### ALIGN-167：无基因蜂默认生成温度使用另一计算式

原 `IItemBumbleBee.java:130` 是 `WD.envTemp(BiomeGenBase.plains)`；原 `gregapi/util/WD.java:412–413` 算 `max(1,(long)(C-3+biome.temperature*20))`，`gregapi/data/CS.java:132` 的 `C=273`。当前 `BumbleBeeGenes.random:102–103` 直接固定 `273+round(.8*20)=289`。两式不一致，原公式包含 `-3` 和截断。

另从本机原始 Minecraft 1.7.10 客户端读取 BiomeGenBase／BiomeGenPlains 类字节，用本机 Forge 映射确认 plains 字段和 setTemperatureRainfall。BiomeGenBase 静态初始化创建 Plains；BiomeGenPlains 构造器明确传入 `.8f, .4f`，并写 temperature／rainfall，因此原默认环境温度确为 286K，当前为 289K。相同随机序列下鲜基因的最低／最高温带相差 3K。jar、class entry 和 javap 摘要哈希已归档；没有运行旧客户端或将其它 GTNH 模组效果当作证据。此项限定于无基因蜂的 `random()` 回退，不等同所有世界生成环境温度都固定 289K。世界生成走实际 helper（前述第 1 项）。

### 扩展 ALIGN-085：自然巢有温度计提示，却没有读数路径

原 `MultiTileEntityBumbleHive.java:69–75` 温度计返回环境温度与湿度；当前 `BumbleHiveBlock.java:101–109` 只添加温度计 tooltip，本类没有 use/interact 处理。共享 `item/behavior/BehaviorThermometer.java:20–36` 也不接受 `BumbleHiveBlockEntity`，返回 refused。物品行为 `ItemBehaviors.java:261–262` 的温度计实际入口已核到。

养殖箱不在这个缺口里：其 `BumbliaryBlock.java:98–108` 有明确专用读数。作为既有 ALIGN-085 温度计接口缺失的自然巢扩围，不另增加一条正式编号。

## 已排除的误报：蜂针与铁傀儡

不能孤立比较 `MultiItemBumbles.bumbleAttack:448–454` 和当前 `BumbleSting` switch，声称移植遗漏铁傀儡保护或军蜂应穿透保护。

原先在 `MultiItemBumbles.java:441` 调 `UT.Entities.isWearingFullInsectHazmat`，`gregapi/util/UT.java:2892–2894` 已全局保护原生 `EntityIronGolem`、Wither、创造模式和整套防蜂装备。当前 `BumbleSting.java:24` → `damage/GTHazmat.java:115–117` 同样先保护。实际养蜂攻击入口 BE `:315–320` → `attackEntity:728–732` → `BumbleSting.attack`。军蜂分支对原生铁傀儡在两者都不可达；现代 instanceof 与原精确类对子类差别只留扩展兼容边界，不列内建玩法缺陷。

## 仍未核完与不能宣称的内容

- 本次完整串联了一个“自然蜂巢产生公主/雄蜂 → 箱内配对 → 女王工作 → 后代分配 → 下一代公主”的主周期，没有穷举所有种类、组合、可选整合和所有工作地点判定。
- 原版 Galacticraft 氧气、额外维度、第三方植物触发属于条件整合；当前没有同名模组，不直接报成内建蜂系全部缺失。世界生成源码把部分旧群系名单映射为现代代表群系，这是移植策略，未证明语义全等。
- 原 `WorldgenHives.java:153–154` 的 space 判断被注释，不能列成原活跃功能；本次没把它当缺口。其他原活跃额外维度分支仍保留为整合后续范围。
- 本批存档结构已齐备，但当时未核完纯寿命／倒计时变化的保存时机。**第五十批补查**已沿原全局每秒标脏和两版 isUnsaved 门控确认条件性回退，登记 ALIGN-171，仍未运行真实保存重启，见[保存报告](BUMBLE_PERSISTENCE_AUDIT.md)。
- 本批待核的自然巢 300／300 火焰参数及末影龙保护，已在**第五十批补查**中核对默认接口、原生标签、全局事件与原 Forge 补丁，登记 ALIGN-169–170；没有用 ignitedByLava 顶替普通火焰参数，见[物理报告](BUMBLE_HIVE_PHYSICAL_AUDIT.md)。
- 旧自然巢颜色转换保留库存的实现已读，但旧存档缺失旧注册 ID 映射、跨版本物品 components 迁移、库存异常 Size、世界重启均未实测。
- 现有 GameTest 文件本次只定位目录，没有运行，也没有把“存在测试代码”写成“通过”。

## 可读性与继续范围

`BumbleBeeGenes.java:112–113,123–126,228–230`、`BumbliaryBlockEntity.Layout:158–172`、`GTBumbleBlocks.java:10–27` 的多语句单行可继续展开。保留基因随机调用次序、原短路条件和高级箱重叠槽位；先记录差异，再由独立玩法修复任务决定是否调整，不能在排版里顺手修玩法。

本轮先展开 core BumbleGenomeRules（21→58 行）和 BumbleSpeciesRules（42→75 行），667／420 个执行词法项及字面量位置保持不变；Genome 的过强“完全原版”说明改为职责描述。二十条物种组合、突变与产物概率的分支数据没有改变。更大的原生职责类仍待整理。来源、哈希、两版实际入口与排版证据见[本批回执](verification/inventory-bumble-audit-20261021.json)。未宣称全部蜂系及运行边界已经核完。
