# #71：流体物性与气体运动修正

2026-10-22，用户明确授权修改密度、黏度及气体游泳行为。保留当前 core / 双加载器架构；不重写世界流体或危险效果。

## 权威源码与数值

只读读取 GregTech6/gregtech6 的 `src/main/java/`：

| 源码 | 证据 | SHA-256 |
|---|---|---|
| [gregapi/data/FL.java](https://github.com/GregTech6/gregtech6/blob/master/src/main/java/gregapi/data/FL.java) | 1077 molten 使用 STATE_LIQUID；1103–1107 液体黏度1000、气体200、等离子10；气体默认密度-100、等离子-100000；1128–1135 材料密度转换 | `87ec6759b7c3b1ad7d7c6d574e24ec7d7371fa285e93ab67cfa26cd83f2b18b3` |
| [gregapi/data/CS.java](https://github.com/GregTech6/gregtech6/blob/master/src/main/java/gregapi/data/CS.java) | 859 空气密度0.0012 g/cm³ | `407edce9f541bcf34b6573d59e4ae14e477fb9c145439deddb831e2a2e2887a5` |
| [gregtech/loaders/a/Loader_Fluids.java](https://github.com/GregTech6/gregtech6/blob/master/src/main/java/gregtech/loaders/a/Loader_Fluids.java) | 45–52 指定气体覆盖；59–64 油600/700/800/900、灵魂沙油650、天然气-500；70–71 物质-5000；99 pahoehoe；130–133 四种热力流体；666–668 三种空气密度0 | `fdc9a697adc7822159cb8ea82389cc83e6494fc9723551f2983607ed46505fcb` |

来源为 GregTech-6 Team，原仓库 LGPL-3.0-or-later 声明保持。此处按公开规则实现共享适配，没有写入参考仓库。读取时内容哈希固定上述证据，链接 master 后续可能变化。

材料字段沿用现有 `GTMaterial.getDensity()`（g/cm³）：重于空气时截断 `1000*d`，轻于空气时截断 `-0.1/d`，等于空气时0，并钳制整数边界。显式流体密度/黏度优先，不按气体标记强制负密度；三种空气的显式0单独处理，避免和旧目录“0表示未设置”哨兵混淆。未知材料仍使用原版相态默认，不编造每种材料的现实黏度。

保留 pahoehoe 50000/250000、Pyrotheum 2000/1200、Cryotheum 4000/3000、Petrotheum 4000/1500、Aerotheum -800/100，以及既有油、蒸汽、丙烷、丁烷、龙息覆盖。补入原有遗漏的灵魂沙油650、带电/中性物质-5000及亮度15、Aerotheum气态。原移植的生成熔体统一3000/6000不是原版 FL.create 的数值，因此改为材料密度及液态1000。并非所有流体原先都没有物性。

## #71 运动原因和边界

密度与黏度元数据不会自动改变 LivingEntity 的浸没移动分支：Neo `LivingEntity.travel` 对非熔岩自定义流体调用 moveInFluid，默认返回false后按水处理。仅 canSwim(false) 或 viscosity(200) 不会让实体按空气运动。

已核对的加载器 API 是 [NeoForge 1.21.1 Entity 补丁](https://github.com/neoforged/NeoForge/blob/1.21.1/patches/net/minecraft/world/entity/Entity.java.patch)（SHA-256 `6390380526748cd991cdece8535909292bf212e73ba4544478c4239b80a81288`）和 [Forge 1.20.x Entity 补丁](https://github.com/MinecraftForge/MinecraftForge/blob/1.20.x/patches/minecraft/net/minecraft/world/entity/Entity.java.patch)（`7335c803ac8e57b9bfbdd201c853236fac51f04753abf1810112631544120aff`）。这些是分支源码，不是项目锁定的 Forge47.4.20 / Neo21.1.243 工件证明；尝试直接版本标签取源码不可得，必须等锁定版本 CI 验证。

双加载器 `Entity.setFluidTypeHeight(FluidType,double):void` 是 protected final、加载器新增且未混淆的方法。`GasImmersionMixin` 使用 `remap=false` 的 HEAD 注入，只取消本模组实际创建且气态/等离子态的 FluidType 身份登记。两端每轮先 clear 高度表，故不会残留上一 tick 气体高度；水、油、其他模组流体不受影响。流体状态和眼部状态不改。

同一气体类型明确 canPushEntity=false、motionScale=0、fallDistanceModifier=1、canExtinguish=false、canSwim=false、canDrown=false。这样不产生流动推动，也不把气体当水减速、跳泳、重置跌落或灭火。普通液体保持现有移动策略，不将黏度强制转换为全局阻力公式。

原版 `BlockBaseFluid` 的 MaterialGas 不是液体；只读对照 wolfram0108/gregtech6_w `BlockBaseFluid` 构造的 NO_ENGINE_FLUID 也采用这一边界，但未复制其兼容层。当前 `BreathingGasEvents.headFluid` 直接读取眼位置世界 FluidState，`GTWorldFluidBlock.entityInside` 及 WorldFluidEffects 不改，所以毒气/头部伤害仍有独立入口。

## 验证及未完成项

- 新 core `FluidPhysicalPropertiesChecks` 已接入既有 `MachineSpecBehaviorContracts`，由 `core:check` 驱动；检查所有目录黏度均解析为正值、熔铁材料密度、气/液/等离子差异、源覆盖、空气密度换算及整数边界。
- 本地 JDK17 编译修改的 core 文件和测试，使用实际缓存 core/API 类运行 `MachineSpecBehaviorContracts`：9274 assertions 通过。不是完整重新编译，也不是游戏运行。
- 双版本分别新增 `GasMotionRegressionTests.gasUsesAirMotionWithoutRemovingHazards`，命名空间 `gregtech_issue_audit`；模板使用同一命名空间的 `test_empty`，两端从已有模板原样复制 NBT（Forge `structures/`、Neo `structure/`）：真实 baseTick 采集浸没，气体/空气 travel 速度比较，跌落距离、眼部气体伤害、水浸没及油不误排除。必须在原生 CI 执行，不能用源码检查代替。
- 本地 Gradle 依赖网络受阻；原生编译、mixin应用和上述 GameTest 尚待 CI。不把新增测试源或 core 通过记为 #71 运行验收。
