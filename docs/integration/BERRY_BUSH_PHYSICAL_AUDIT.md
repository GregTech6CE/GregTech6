# 浆果灌木物理属性核查

第五十三批的灌木物理子域，2026-10-21。行为来源初查基线为 `21f68f1b1874e0d99a95e11de5d3bdfe68c9e1fb`，本批归档／排版基线为 `e3f08722c4934a9478b38e7b3395012eedd803b1`。新增 ALIGN-183–185；此前 [ALIGN-174–178](BERRY_BUSH_AUDIT.md) 的生命周期差异不重复编号。本批执行词法、最终文件与原生依赖的摘要由[统一回执](verification/bush-physical-worldgen-audit-20261021.json)记录，原始观察及只读命令另存 `work/berry-physical-audit-20261021/`。注册类的定位使用类／方法锚点，避免把本批纯排版造成的行号变化当作行为变化。

本报告只归档源码与本机实际依赖的证据，没有修复玩法，也没有运行 Gradle、编译、新测试、游戏、真实挖掘／燃烧／爆炸／墙内伤害、存档重启、构建或推送。原模组根为 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java`，当前根为 `C:/Dev/GregTech6`。原文件保留 GregTech-6 Team／Gregorius Techneticies 与 LGPL-3.0-or-later 头；本报告没有移入原生依赖源码。

## 身份政策、原型与实际入口

食品、棉花和材料莓继续采用要求的扁平变种 ID。核心与六方向枝条的区别是支撑状态，不是恢复以 NBT 记录物种身份的理由。以下比较限定项目默认实现、正常无覆盖板灌木，没有外部模组／数据包额外补偿。

原 `gregtech/loaders/b/Loader_MultiTileEntities.java:103` 创建 `aBush`，使用 `Material.leaves`、草音效，并明确 `aOpaque=false`、`aNormalCube=false`；`:2030` 把 `MultiTileEntityBush` 注册为 32759，传入硬度 `0.5F`、抗爆参数 `0.3F`。原宿主 `gregapi/block/multitileentity/MultiTileEntityBlock.java:299–300` 把实际硬度／抗爆查询转发给 tile 接口，`:253–254,266` 转发火属性与光遮挡，`:191–192` 转发碰撞形状，不能把这些声明当成未调用的接口。

原实际抗爆不是注册参数 `0.3F`。`TileEntityBase07Paintable.java:59–60,109–110` 读取并提供硬度／抗爆字段，但 `MultiTileEntityBush.java:347` 覆写 `getExplosionResistance2()` 返回 **0.2F**；`TileEntityBase01Root.java:511–515` 的带位置／实体查询最终虚调用该覆写。正常未设置额外爆炸强度的灌木，实际为硬度 **0.5**、抗爆 **0.2**。

当前共享路径根为 `multiversion/src/main/java/com/gregtech/gregtech/`。`registry/GTBushes.register(String,String)` 为 bare、食品／棉花及材料变种共同创建 `BushBlock`，属性链为 `mapColor(PLANT).strength(.2f).sound(GRASS).noOcclusion().instabreak()`。Forge 的 `multiversion/versions/1.20.1-forge/.../bootstrap/registration/MachineBlockBootstrap.java:38` 调 `GTBushes.registerAll()`；Neo 的 `multiversion/versions/1.21.1-neoforge/.../platform/neoforge/GregTechNeoForge.java:59` 调 `GTSurfaceBlocks.initialize()`，共享 `registry/GTSurfaceBlocks.java:10` 再调用同一注册入口。两平台实际采用共享 `block/plant/BushBlock`，不是只检查了一个没有接入的备用类。

## 默认有效属性对照

| 属性 | 原核心 | 原枝条 | 当前两版核心 | 当前两版枝条 |
| --- | --- | --- | --- | --- |
| 硬度 | 0.5 | 0.5 | 0 | 0 |
| 正常抗爆查询 | 0.2 | 0.2 | 0 | 0 |
| 火消耗参数 getFlammability | 300 | 300 | 0 | 0 |
| 火传播参数 getFireSpreadSpeed | 300 | 300 | 0 | 0 |
| 光遮挡／LightBlock | 1 | 0 | 1 | 0 |
| 基础实体碰撞 | 整立方体 | 厚 2px，另外两维 12px | 整立方体 | 对应六向 2px 形状 |
| 选取形状 | 整立方体 | 厚 4px，另外两维 12px | 整立方体 | 对应六向 4px 形状 |
| 实体进入钩子的荆棘伤害／减速 | 无 | 无 | 无 | 无 |
| 墙内检测相关合同 | normal-cube=false | false | 现代默认 isSuffocating 可为 true | 薄形状为 false |

前三组差异沿原生查询确认；光遮挡与无覆盖板几何已有对应。墙内一项还受实体眼部是否与核心相交等条件限制，不能读成普通碰撞伤害。

## ALIGN-183：instabreak 同时把硬度和有效抗爆清零

本机 Forge **1.20.1-47.4.20 mapped official** 与 NeoForge **21.1.243／Minecraft 1.21.1** 的实际依赖均显示：`Properties.instabreak()` 调单参数 `strength(0.0F)`，后者调 `strength(f,f)`，同时写入 `destroyTime` 与 `explosionResistance`。当前最后的 `.instabreak()` 因而覆盖此前 `.strength(.2f)`，最终是 **0／0**，不是 **0／0.2**。

Neo 原生源 `net/minecraft/world/level/block/state/BlockBehaviour.java:1110–1120` 提供上述属性链，`:102–105,452` 将抗爆／破坏时间复制到实际方块和状态；`net/minecraft/world/level/block/Block.java:357–358` 的抗爆 getter 返回该字段。Forge 对应真实类字节中，`Properties.instabreak` offset 0–5、`strength(float)` offset 0–6 和 `Block.getExplosionResistance` offset 0–4 给出同一调用／取值链。

`BushBlock` 没有覆写破坏速度、破坏进度或抗爆方法。原生挖掘进度使用实际 `destroySpeed`，在普通正挖掘速度下硬度 0 进入即时破坏路径。项目工具／BreakSpeed 事件没有为灌木恢复原正硬度，也没有恢复抗爆字段。因此正常原灌木 **0.5／0.2** 与当前 **0／0** 是一项明确参数差异；本批没有测量实际挖掘耗时或爆炸结果。

## ALIGN-184：普通火传播与消耗的 300／300 未接入

原 `MultiTileEntityBush.java:348–349` 对所有面分别返回火传播 **300**、火消耗 **300**；继承的接口由宿主实际转发。当前 `BushBlock` 继承普通 `Block`，没有火属性覆写；全树共享、core、Forge／Neo 业务、事件、Mixin 和动态资源入口也未发现为 `GTBushes` 写入原生火表的路径。

Forge `net/minecraftforge/common/extensions/IForgeBlock.java:614–616,655–657` 与 Neo `net/neoforged/neoforge/common/extensions/IBlockExtension.java:646–647,685–686` 的默认方法到 `Blocks.FIRE.getBurnOdds(state)`／`getIgniteOdds(state)`。它们按具体方块对象查询 `FireBlock` 的整型映射，不按名称、植物颜色或标签自动继承 Minecraft 甜浆果丛的值。

Neo `net/minecraft/world/level/block/FireBlock.java:61–62,243–254,323–326` 显示映射创建、读取和注册；Forge 实际类亦已交叉核对。原生 bootstrap 仅为列出的具体 Minecraft 方块对象注册参数。依赖 fastutil **8.5.9／8.5.12** 的实际字节码确认，未命中的 `Object2IntOpenHashMap.getInt` 返回默认 `defRetValue`，此处为 **0**，没有额外默认值补偿。

这两个值确有消费者：Neo `FireBlock.java:257–267` 的 `checkBurnOut`、Forge 同功能 `tryCatchFire` 读取目标的 `getFlammability` 后比较随机阈值；邻域传播读取各面 `getFireSpreadSpeed` 的最大值（Neo `:287–295`），`isFlammable` 也以消耗值大于 0 判断。故默认当前为 **0／0**，没有保留原 **300／300** 的普通火消耗／传播合同。

这不表示灌木绝对无法被任何火焰、指令、爆炸或其他模组销毁，也不表示原 300 会无视天气、游戏规则或其他条件立即燃烧。标签若没有另行写入这条对象表，不能代替属性注册；本批没有运行火蔓延实验。

## ALIGN-185：核心眼部嵌入时会进入现代默认墙内判定

原 `aBush` 的 `mNormalCube=false` 同时由宿主 `MultiTileEntityBlock.java:270` 的无参 `isNormalCube()` 与 `:214` 的带位置查询返回；灌木没有特殊覆写把它变回 true。本机 Minecraft 1.7.10 client 经本地 Forge SRG 对应到 `Entity.isEntityInsideOpaqueBlock`／`func_70094_T`（`sa.aa()`），该路径采样眼部附近方块并调用 `Block.func_149721_r`（`aji.r()`）。旧 Forge 二进制补丁没有重放，不把 vanilla jar 单独当成完整旧 Forge 运行；GT 同时提供的两种 normal-cube 接口另有源码证据。

当前 `.noOcclusion()` 没有把 `isSuffocating` 设为 false。两版实际原生默认条件是 `blocksMotion && isCollisionShapeFullBlock`（Neo `BlockBehaviour.java:998–999`；Forge 对应 `Properties` 的默认 lambda）。核心完整碰撞体在默认 `calculateSolid`／`blocksMotion` 中为 true；六向薄枝条不满足满碰撞体条件，为 false。

Neo `net/minecraft/world/entity/Entity.java:1900–1917` 的 `isInWall` 先排除 `noPhysics`，再把眼部薄 AABB 与满足 `state.isSuffocating` 的方块碰撞形状求交。`LivingEntity.java:402–406` 在服务器端满足墙内条件时进入普通 `inWall` 1 点伤害路径。Forge 对应真实类中也核到眼部求交及 `LivingEntity` 的 `isInWall → inWall → hurt(1.0F)` 调用。

因此，**非无物理、可受该伤害的实体，眼部被放入／嵌入灌木核心碰撞体时**，当前可能命中现代墙内检测，而原 GT normal-cube 合同不把灌木当作墙。正常行走通常被相同的实体碰撞框挡住，这不是“普通碰到灌木就受伤”；枝条也不满足该满块条件。创造／旁观、伤害免疫等仍受原生条件限制，本批没有实际实体嵌入或伤害复现。

## 已有对应与排除误报

### 核心／枝条 LightBlock 已对应 1／0

原 `MultiTileEntityBush.java:332` 对核心返回 `LIGHT_OPACITY_LEAVES`，枝条返回 NONE；`gregapi/data/CS.java:245` 明确数值 **1／0**。当前两版 `.noOcclusion()` 仅令 `canOcclude=false`，没有删除碰撞，也没有直接指定 LightBlock=0。

实际原生 `getLightBlock` 在非 solid-render 分支按 `propagatesSkylightDown` 返回 0 或 1；默认传播条件还检查 **getShape 是否满立方体** 及流体是否为空。Neo `BlockBehaviour.java:292–296,381–382,628–633,1080–1082` 与 Forge 对应真实类同义。当前 `BushBlock.shape/getShape` 中核心整块，向下传播为 false，LightBlock=**1**；六方向枝条为部分形状且流体为空，传播为 true，LightBlock=**0**。不能因 `noOcclusion` 把这一项误报成完全透光；它与 ALIGN-175 的屋下生长光照 helper 是不同合同。

### 无覆盖板的基础几何已对应

原 `MultiTileEntityBush.java:285–307` 的核心碰撞／选框为整块，六向枝条分别为 2px／4px 厚、另外两维 12px；继承 Base06 的碰撞列表与宿主实际调用这套形状。当前 `BushBlock.shape/getShape/getCollisionShape` 给出同样的六向结构。

两版实际 `BlockCollisions` 均向 `state.getCollisionShape` 取形状（Neo `BlockCollisions.java:81`，Forge 字节码 offset 167）；实体移动的 `collideBoundingBox/collectColliders` 经世界 block collisions 到此入口。不能按渲染标志推断当前没有碰撞。

项目 `CoverCollisionMixin` 能在确实装有覆盖板时合并形状，但 `CoverWorldInteraction.collision` 在无已装覆盖板时保留原形状；手持工具还可改变选择框。这里确认的是默认无覆盖板几何，不宣称覆盖后的形状／安装政策完全相同，也不把覆盖板当成硬度／火属性的补偿。

### 原版没有甜浆果荆棘行为

原灌木没有普通实体进入时的甜浆果荆棘伤害／减速逻辑，当前 `entityInside` 默认钩子也没有这项行为。ALIGN-185 是眼部嵌入的墙内条件差异，不能据此新增“漏移植荆棘”或“正常走碰伤害”记录。

## 证据身份与未验证边界

来源收据包含原／当前文件的绝对路径、相对路径和 SHA-256，以及本机 Forge 47.4.20 mapped official、NeoForge 21.1.243 原生源／类、原 1.7.10 client／Forge SRG、fastutil 8.5.9／8.5.12 的 jar／entry 身份及只读 `javap` 输出摘要。观察基线与排版后摘要分别保存，不将格式变化伪装成最初读到的原始字节；最终汇总见本批统一回执。

补偿检索覆盖默认属性、火表、工具事件、Mixin 与共享／两平台入口，未找到把上述三项恢复到原合同的项目内路径。AxeColumn 的 BreakSpeed、PoweredToolHarvest、ToolBlockConversion 和 SmelteryInteraction 各有目标或工具范围，不能代替原灌木的默认硬度、抗爆或火表。外部保护、数据包、模组及玩家附加覆盖板可影响最终运行表现，均不在这次默认合同的结论内。

本批没有真实注册值运行断言、挖掘耗时、火蔓延、爆炸、光照引擎、移动／墙内伤害或存档重启验证。面实心、刷怪支持、雪覆盖渲染、火源存活地面规则及其他可选整合尚未全部逐消费者核完；这些边界不新增编号，也不由本报告推断灌木全部物理属性已对齐。世界生成的独立核查由本批其他报告记录。
