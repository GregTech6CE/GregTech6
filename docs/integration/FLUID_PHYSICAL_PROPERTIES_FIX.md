# #71：流体物性与气体运动修正

2026-10-22：修正流体物性，并让气体不进入液体浸没运动分支。保留当前 core / 双加载器架构；不移植原版整套流体运动系统，不重写世界流体或危险效果。

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

初查使用了移动分支；随后下载并核对项目锁定版本的官方工件，取代仅凭分支推断：

| 锁定工件 | SHA-256 |
|---|---|
| [Forge 1.20.1-47.4.20 sources.jar](https://maven.minecraftforge.net/net/minecraftforge/forge/1.20.1-47.4.20/forge-1.20.1-47.4.20-sources.jar) | `de56d18d759d17d780416e47fb01b72c38633bdd2e2d071cdedfe486eab35d7b` |
| [NeoForge 21.1.243 sources.jar](https://maven.neoforged.net/releases/net/neoforged/neoforge/21.1.243/neoforge-21.1.243-sources.jar) | `c11c83b344cb92ce9e815db2ed4b53fd7770b97098d5ebd10edde28595c12af0` |
| [NeoForge 21.1.243 userdev.jar](https://maven.neoforged.net/releases/net/neoforged/neoforge/21.1.243/neoforge-21.1.243-userdev.jar) | `a934308fe0dae5a27c1d2b982cfeba7d012fd857791fa19f10cb39edae6f03b6` |

Forge sources.jar 和 Neo userdev.jar 中的 `patches/net/minecraft/world/entity/Entity.java.patch` 分别为 `b3c47ee667bba947aa8f37068a0e47706747aa4015467a3bf6f5f39b09cafb4d`、`ec3c090439eec44be9b088cde9ba03a2f36754496d278e36b315712c3c3fa55d`。两者都确认下述 loader-added 方法及每轮清空高度表的路径。两端锁定 sources.jar 中 GameTestHooks 的 `getTemplateNamespace` 都读取 `@GameTestHolder.value()`；Forge 注册补丁把它与裸模板名连接。因此保留 `template="test_empty"`，不使用会被重复前缀的显式 `namespace:path`。源码包的 Neo GameTestHooks 也确认报告器使用的 `isGametestServer()` 入口。

双加载器 `Entity.setFluidTypeHeight(FluidType,double):void` 是 protected final、加载器新增且未混淆的方法。`GasImmersionMixin` 使用 `remap=false` 的 HEAD 注入，只取消本模组实际创建且气态/等离子态的 FluidType 身份登记。两端每轮先 clear 高度表，故不会残留上一 tick 气体高度；水、油、其他模组流体不受影响。流体状态和眼部状态不改。

同一气体类型明确 canPushEntity=false、motionScale=0、fallDistanceModifier=1、canExtinguish=false、canSwim=false、canDrown=false。这样不产生流动推动，也不把气体当水减速、跳泳、重置跌落或灭火。普通液体保持现有移动策略，不将黏度强制转换为全局阻力公式。

原版 `BlockBaseFluid` 的 MaterialGas 不是液体；只读对照 wolfram0108/gregtech6_w `BlockBaseFluid` 构造的 NO_ENGINE_FLUID 也采用这一边界，但未复制其兼容层。当前 `BreathingGasEvents.headFluid` 直接读取眼位置世界 FluidState，`GTWorldFluidBlock.entityInside` 及 WorldFluidEffects 不改，所以毒气/头部伤害仍有独立入口。

## 验证及未完成项

- 新 core `FluidPhysicalPropertiesChecks` 已接入既有 `MachineSpecBehaviorContracts`，由 `core:check` 驱动；检查所有目录黏度均解析为正值、熔铁材料密度、气/液/等离子差异、源覆盖、空气密度换算及整数边界。
- 本地 JDK17 编译修改的 core 文件和测试，使用实际缓存 core/API 类运行 `MachineSpecBehaviorContracts`：9274 assertions 通过。不是完整重新编译，也不是游戏运行。
- 双版本分别新增 `GasMotionRegressionTests.gasUsesAirMotionWithoutRemovingHazards`，命名空间 `gregtech_issue_audit`；模板使用同一命名空间的 `test_empty`，两端从已有模板原样复制 NBT（Forge `structures/`、Neo `structure/`）：真实 baseTick 采集浸没，气体/空气 travel 速度比较，跌落距离、眼部气体伤害、水浸没及油不误排除。原生 CI 实际结果见下；不以源码检查代替。
- [原生 CI 运行39833408402](https://github.com/GregTech6CE/GregTech6/actions/runs/39833408402)，提交 `973081e57925a0224d8e90ebb2cd4a96964ed83b`：双端原生编译完成且 gas GameTest 实际通过，说明该测试执行中的 mixin 应用、气体/空气速度对照、水对照及头部危险效果均已成立。不是只检查元数据或测试源。
- 下载 XML 的气体 testcase 都是 `name="gasusesairmotionwithoutremovinghazards"`、`classname="gregtech_issue_audit:test_empty"`，无 failure/error/skipped 子节点；Forge耗时0.087秒、Neo0.055秒。原始工件路径为 `work/validation/ci-973-artifacts/{forge,neoforge}-issue-regressions-973081e57925a0224d8e90ebb2cd4a96964ed83b/multiversion/versions/{1.20.1-forge,1.21.1-neoforge}/build/reports/gametest/issues-64-81.xml`。
- **该历史运行并非全绿**：同次 Forge 的钻头测试期望失败，另有报告解析与 intentional-unmapped mixin 打包检查问题。这些不抹去已记录的双端气体测试通过，也不能因单项通过宣称完整发布验收。
- #71 获得上述限定场景的双端原生回归证据；真实玩家持续游玩、混合流体边界、所有实体/全部气体种类以及存档重启仍不由这一个测试覆盖。本地 Gradle 网络限制仍在。

## 后续完整定向回归

提交 `15d3832802973f9e40a250f652dc7380b38a97e9` 的[定向回归运行39834719407](https://github.com/GregTech6CE/GregTech6/actions/runs/39834719407)已通过：Forge 五项（气体运动、手钻矿石、板堆覆盖板同步、创造栏分组、地牢水流调度），NeoForge 一项（气体运动）。XML 校验同时要求精确测试名称、命名空间和零失败，避免“服务器启动成功但没有执行目标测试”的假通过。手钻测试按当前目录的每次200 EU验证精确消耗，不改变生产耗电规则。

同一提交的 Forge 打包报告出现脚本变量作用域错误，随后统一使用 `UNMAPPED_FORGE_API_MIXINS` 常量，并增加覆盖真实归档检查返回路径的测试；其他 Mixin 映射检查不放宽。完整构建结果应以所用最终提交的 Actions 为准，不能把上面的定向回归结果替代双平台打包验证。
