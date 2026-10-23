# 光莲放置与物理属性核查

第五十六批植物专题的光莲子域，2026-10-21。初查观察基线为 `f42d88cb2bf2d4444c624e722e8bb722d42d35d8`；正式归档前 HEAD 为 `93b993c95`，未变化的产品源码与后续说明更新分别核对。本稿新增 **ALIGN-194、ALIGN-195**，只涉及两项 Forge 实现差异，不合并石头／木棍堆结论。来源与原生工件摘要由[本批统一回执](verification/surface-plant-audit-20261021.json)归档；完整初查观察保留于 `work/glowtus-alignment-audit-20261021/README.md`、`sources.json`。

本次只读核查与文档归档没有修复玩法，没有运行 Gradle、编译、新测试、游戏、放置／碰撞／掉落实测、存档重启、构建或推送。原模组根为 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java`，当前根为 `C:/Dev/GregTech6`。原文件保留 GregTech-6 Team／Gregorius Techneticies 与 LGPL-3.0-or-later 头，本报告不移入原生依赖源码或字节码。

## 保留现代睡莲和通用水族政策

要求光莲与 Minecraft 睡莲使用相同放置、碰撞，并要求 GT 水拥有通用水属性。[既有决策](DECISIONS.md)的“2026-10-03 / 物品 ARGB、植物附着与固定蜂巢身份”已采用 `WaterlilyBlock`、`PlaceOnWaterBlockItem`；同文2026-10-04水兼容段要求采用一般水标签／类型／支撑语义，而不是睡莲专用标志。

因此，现代睡莲的内缩选框、冰面准入和船接触销毁先作为已有政策边界，不能自动列为待恢复的原版玩法。本稿确认的是当前 Forge 连现代睡莲基本合同也偏离的两项实现；保持 16 色扁平 ID，不建议恢复以 metadata／NBT 表示新颜色，也不建议削弱 GT 水的通用水属性。

## 原版对象和实际注册所有权

原 `gregtech/loaders/a/Loader_Blocks.java:125` 创建 `BlockGlowtus("gt.block.lilypad.glowtus")`。[BlockGlowtus](C:/Dev/gregtech6-master/gregtech6-master/src/main/java/gregtech/blocks/plants/BlockGlowtus.java:33) 继承的是自有 `BlockBaseLilyPad`，不是 Minecraft `BlockLilyPad`；它注册 16 色名称、U/4 Glowstone 组成及发光 15／光遮挡 0。父类沿 `BlockBaseMeta → BlockBaseSealable → BlockBase`，`ItemBlockBase.java:112` 真正把物品右键转发到自有水面放置方法。

当前共享 Java 路径以下简写为 `multiversion/src/main/java/com/gregtech/gregtech/`；版本专有根为 `multiversion/versions/<version>/src/main/java/com/gregtech/gregtech/`。

| 环节 | Forge 1.20.1 | NeoForge 1.21.1 |
| --- | --- | --- |
| 活跃注册入口 | 共享 `bootstrap/content/BlockBootstrap.java:64` → [GTIconSetBlocks.registerAll](C:/Dev/GregTech6/multiversion/versions/1.20.1-forge/src/main/java/com/gregtech/gregtech/registry/GTIconSetBlocks.java:60) | `GregTechNeoForge.java:55` 执行 BlockBootstrap；共享 Neo 分支 `:60–61` 先调 [GTPlants.registerAll](C:/Dev/GregTech6/multiversion/versions/1.21.1-neoforge/src/main/java/com/gregtech/gregtech/registry/GTPlants.java:5) |
| 光莲方块属性 | `GTIconSetBlocks.java:199–206` 的光莲工厂传 `instabreak`、**noCollission**、LILY_PAD 音效、发光 15 | `GTPlants.java:6` 的光莲分支在 `p.noCollission()` 前返回，保留碰撞 |
| 实际物品 | `GTIconSetBlocks.java:69–74` 对睡莲类创建 `PlaceOnWaterBlockItem` | `GTPlants.java:5` 经共享 `GTBlocks.registerBlockItem:157–160` 的 Neo 分支，同样创建水面物品 |
| 共用方块体 | [IconSetLilyBlock](C:/Dev/GregTech6/multiversion/src/main/java/com/gregtech/gregtech/block/IconSetLilyBlock.java:6) 继承 WaterlilyBlock，没有几何／实体接触覆写 | 同一维护源；Neo 条件体只增加 codec 和 1 个自身物品掉落，没有另一个几何实现 |

**排除错误的 Neo 所有权候选：** `GregTechNeoForge.java:116` 后来的 `GTRemainingIconBlocks.initialize()` 的确有 noCollission 和普通 BlockItem fallback，但该类 `:6–9` 先把已排队的 `GTBlocks.BLOCKS` 加入 seen。光莲已被前置 GTPlants 占有，`seen.add(id)` 阻止重复注册。不能将这个 fallback 当成当前 Neo 光莲，更不能据此报 Neo 没有水面物品或碰撞。

## ALIGN-194：Forge 注册将普通实体薄片碰撞清空

原 [BlockBaseLilyPad](C:/Dev/gregtech6-master/gregtech6-master/src/main/java/gregapi/block/misc/BlockBaseLilyPad.java:71) 仅从碰撞列表中排除 `EntityBoat`；其他实体继续使用原生 Block 碰撞列表。`:62,110–112` 的自有 AABB 为 X/Z 0..1、Y 0..0.015625，即整面 **0.25px** 高薄片。原生 Block 的列表消费者会虚调用这个 AABB 并在相交时加入碰撞，不是只有渲染贴图。

当前 Forge 活跃工厂明确传 `noCollission()`。本机 Forge **1.20.1-47.4.20 mapped official** 的 `BlockBehaviour$Properties.noCollission` offset 0–11 设置 `hasCollision=false`、`canOcclude=false`；默认 `BlockBehaviour.getCollisionShape` offset 0–19 因而返回 `Shapes.empty()`。原生 WaterlilyBlock 与当前 IconSetLilyBlock 均未覆写碰撞方法来补偿。

实际移动消费者也已核对：Forge `net.minecraft.world.level.BlockCollisions.computeNext` offset 167 调 `state.getCollisionShape`；Neo 同类源 `:81` 对应世界实体移动的相同形状查询。因此在默认无已装覆盖板／无外部补偿条件下，Forge 光莲没有普通薄片支撑，不能仅因继承睡莲类就说碰撞已保留。

Neo 的前置 GTPlants 没有关闭光莲碰撞，保留现代睡莲 **(1,0,1)..(15,1.5,15)** 像素 AABB。原整面 0.25px 与现代内缩 1.5px 的区别属于既有现代睡莲政策；本项不扩大为两版都没有碰撞，也没有声称已测得站立、落水或船行结果。

## ALIGN-195：Forge 植物类型的 PLAINS 回退额外允许陆地放置

原 `BlockBaseLilyPad.java:72–73` 存活／放置要求下方是 `Material.water` 且 metadata=0；`:103–105` 的右键要求水源、上方为空，按颜色放置并对非无限物品玩家扣 1。泥土／耕地不符合条件。现代世界高度扩展不照抄原 0..255 上限作为缺口。

Forge 的实际链还包含植物支撑回退，不能只看 `WaterlilyBlock.mayPlaceOn`：

1. 实际 `BushBlock.canSurvive` offset 6–32 对自身候选 state 调下方 `canSustainPlant(..., this)`，放置前检查也经过它。
2. 原生 `Block.canSustainPlant` offset 89–112 **先调用 bush.mayPlaceOn**，成功直接返回 true。现有 GT 水族适配因此确有消费者，不能误报水面放置整体失效。
3. mayPlaceOn 失败后，offset 218–251 仍会按 `PlantType.PLAINS` 接受 `BlockTags.DIRT` 或 `Blocks.FARMLAND`。
4. Forge `IPlantable.getPlantType` offset 51–61 只把精确 `Blocks.LILY_PAD` 对象归 WATER；其他未特列对象在 offset 106–109 返回 PLAINS。当前 IconSetLilyBlock 没有覆写植物类型，落入这个回退。
5. 实际水面物品虽然选择 SOURCE_ONLY 流体射线，但射线也能命中普通方块；命中位置上移后，经 `BlockItem.useOn/canPlace → state.canSurvive`，并没有独立的“必须命中液体”检查。因此在泥土／耕地上方空间、权限等条件满足时，这是一条实际可达的额外陆地准入，而不只是未消费的类型标签。

源码与类字节证据交叉对应：Forge sources 中 `patches/net/minecraft/world/level/block/BushBlock.java.patch:14–18`、`Block.java.patch:152–189`、`net/minecraftforge/common/IPlantable.java:18–28`；原生 `BlockItem.canPlace` 也确实执行存活与碰撞许可。

Neo 的 `BushBlock.canSurvive:34–39` 先询问土壤 TriState；默认 `IBlockExtension.canSustainPlant:393–394` 返回 DEFAULT，再进入睡莲 mayPlaceOn。普通泥土没有 Forge 的 PLAINS 回退。本项限 Forge，不表示全部植物／水族失效，也不要求给 GT 水另加睡莲属性。

## GT 水与普通睡莲已有通用兼容

原 GT `BlockWaterlike.java:65` 使用 `Material.water`，`:208` 射线碰撞只接受 metadata=0，原光莲因此能够使用正常 GT 源水。当前已有以下活跃链：

- 两版本 `data/minecraft/tags/fluids/water.json`／`tags/fluid/water.json` 均包含海／河／沼泽水及其 flowing，共六个真实 ID。
- `registry/GTWorldWaterFluid.isWaterFamily:181–187` 识别自身 Source／Flowing、原版水和 `FluidTags.WATER`；这些世界水的 FluidType 为原生水类型，注册和存储身份仍独立。
- [WaterFamilyIdentity.forVanillaCheck](C:/Dev/GregTech6/multiversion/src/main/java/com/gregtech/gregtech/api/fluid/WaterFamilyIdentity.java:12) 在行为检查中按 isSource 映到原版 WATER／FLOWING_WATER，保留源水与流动之别。
- [WaterSurfaceMixin](C:/Dev/GregTech6/multiversion/src/main/java/com/gregtech/gregtech/mixin/WaterSurfaceMixin.java:12) 重定向所有 WaterlilyBlock.mayPlaceOn 的两次流体身份查询，两版本实际 mixin 配置均启用；原版 Minecraft 睡莲和 GT 光莲共用，下方源水／冰与上方空流体条件仍存在。
- Forge `block/GTWorldFluidBlock.canSustainPlant:82–90` 对 GT 水族构造同 LEVEL 的原版水 state，再交给原版水的一般植物支撑；这同时服务其他水生植物，不是睡莲专用标志。
- 原生 `PlaceOnWaterBlockItem.use` 经 `ClipContext.Fluid.SOURCE_ONLY` 按 `FluidState.isSource` 选流体，因此能命中 GT 源水；最终仍需满足实际存活、权限、空间等条件。

普通 Minecraft 睡莲在 Forge 的 WATER 类型回退还带有其原生准入语义，不能只用 GT 光莲的 mayPlaceOn 结果代替全部原版睡莲分支，进而声称任何流动水在所有路径都必被拒绝。本批确认通用兼容入口存在，没有运行所有第三方水／植物组合。

## 船、掉落、发光与颜色的已有内容和政策边界

当前两版原生 `WaterlilyBlock.entityInside` 在服务器遇 Boat 时调用 `destroyBlock(..., true, boat)`（Neo 源 `:31–35`，Forge 类 offset 9–40）；实际 Entity 块内回调会调用它。原 GT 光莲则显式不向船提供碰撞，自有继承链未增加接触回调。旧 vanilla `EntityBoat.onUpdate` offset 1289–1317 只比较精确 `ajn.bi`；`ajn` 初始化 offset 1568–1579 明确从字符串 `waterlily` 填入 bi，不能推广到原 GT 自有方块。

这是原 GT 与现代睡莲的策略区别，但已由现代睡莲决策覆盖，**不在本稿新增船缺陷**。旧 Forge 1.7.10 二进制补丁未重放；原 GT 源码与旧 vanilla 字节证据分别成立，不能把后者单独冒充完整旧 Forge 运行。官方 EntityBoat／BlockLilyPad patch 获取先遇 web cache miss，直接请求又遭本机网络代理拒绝；没有取得内容就没有伪造远端哈希。内存 pack200 解包也未成功，没有写出中间工件。

| 内容 | 已有实现／核查边界 |
| --- | --- |
| 普通掉落 | 原 `BlockBase.java:82–87` 返回 1 个自身并保留颜色 metadata；Forge `data/BlockLootPack.java:65–95` 为真实 ID 提供 1 个自身掉落，Neo IconSetLilyBlock 条件体直接返回 1 个自身物品。颜色保存在 ID，不需要 NBT；外部爆炸／Loot 事件的最终结果未实测。 |
| 失去支撑 | 原 `BlockBaseLilyPad.java:75–76,90–94` 的邻居／tick 检查会掉落；当前继承 BushBlock.updateShape，失效返回 AIR，原生 Block.updateOrDestroy（Neo `:171–175`，Forge 对应类）执行 destroyBlock／掉落。已有邻居链，不报成永不脱落。 |
| 硬度／抗爆 | 原委托 Minecraft waterlily；旧原生注册、构造／getter 字节对应 0／0。当前 instabreak 也为 0／0，不能套用灌木的正硬度／抗爆遗漏。 |
| 发光／遮光 | 原 Glowtus 明确 15／0；两版实际工厂设置发光 15，BushBlock 的空流体向下传播与部分形状给出 LightBlock=0。客户端光引擎未实测。 |
| 16 色扁平身份 | `IconPlantCatalog.java:22–25,72` 提供 16 个独立 ID；`SurfaceFloraRules.GLOWTUS_COLOURS:37–39` 保留原 black/red/.../white 顺序。各色 blockstate／物品／方块模型按 ID 指向自身纹理，不依靠统一 RGB 回调。未逐像素对比或做 GPU 画面验收。 |
| 数量与处理 | 原物品上限 64，当前水面物品亦默认 64。`content/recipe/GlowtusProcessingRecipes.java:27–41` 已为各色提供 mortar／shredder 与 biomass 主体，MachineRecipeBootstrap `:41` 调用；Forge `GTMaterialRegistration.java:130–137` 显式登记 U/4 Glowstone。没有据此宣称所有处理配方／可选整合已验收。 |

原 Chisel、Railcraft、Forestry 入口受可选模组门控；`RM.chisel:932–933` 首先检查对应模组加载。旧 API 没有同名现代入口不等于基础颜色或采收失效。原指定剑工具／镰刀排除尚未逐消费者核完，不增加工具编号。

## 证据与未验证范围

本子域收据记录 15 份原版、80 份当前来源，以及 Forge 47.4.20／NeoForge 21.1.243／原 1.7.10 client 和 Forge 10.13.4.1614 的六个原生工件、42 个 entry；24 次只读 javap 输出均取得摘要。没有将现存 build 依赖目录当作本基线已编译的证明，来源 SHA-256 和最终统一汇总见本批回执。

历史 `verification/visual-plants-20261003.md` 的放置／船碰撞回归属于当时快照，不能覆盖当前 Forge 属性和植物类型证据，也不重写其历史结果。任意第三方水／植物支撑 override、外部事件取消、附加覆盖板、全工具矩阵、自然生成概率／群系、可选整合、独立旧档重启、各色实际渲染和成品客户端尚未在本批验证。只确认上述两项 Forge 差异，不将未穷举范围说成所有光莲或 GT 水方块未移植。
