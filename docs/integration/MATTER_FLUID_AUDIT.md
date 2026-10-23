# 物质制造的流体关联、单位与注册核查

日期：2026-10-19；基线 `cca4bd48b`。本批沿材料→流体目录→物质制造配方→碰撞检查追踪，补充两项具体差异；仅抽取数据目录与修正错误注释，没有修复玩法。

## ALIGN-057：已有材料关联的八种元素流体没有进入物质制造配方

原版 `gregtech/loaders/c/Loader_Recipes_Other.java:969–986` 对满足 ELEMENT、非 ANTIMATTER、质子加中子大于零的材料生成物品／块和液态、气态、等离子态分离配方。相态从 `OreDictMaterial.liquid/gas/plasma(U, true)` 取得，关联由原版 `FL.create()` 的状态分支写入材料对象。

当前 `FluidCatalog.registerGeneratedGasLiquidFluids()` 为已有专用流体复用原条目，材料关联放在 `BOUND_MATERIALS` 旁表中，以 `boundMaterial(registryName)` 查询；条目的 `materialKey()` 仍为 null。这是实际设计，不能把 null 当作完全没有材料。渲染策略、材料标签和熔融流体接口已经有读取旁表的入口。

但 `MatterRecipeLoader.run()` 的流体循环直接跳过 `definition.materialKey() == null`，没有查询旁表。独立 core 初始化、材料后处理与 `FluidDefinitions.prepare()` 后实际观察到以下八个**可见、已绑定有效元素**的条目会在该分支被跳过：

| 流体字段 | 注册名 | 旁表材料 |
| --- | --- | --- |
| Hydrogen | hydrogen | Hydrogen |
| Deuterium | deuterium | Deuterium |
| Tritium | tritium | Tritium |
| Helium | helium | Helium |
| Nitrogen | nitrogen | Nitrogen |
| Oxygen | oxygen | Oxygen |
| Neon | neon | Neon |
| Argon | argon | Argon |

不是这八种流体没有注册，也不是全部元素分离都不存在。生成熔融铁等条目自带 materialKey，仍能进入当前加载分支。八项是当前 core 目录快照，不是原生配方管理器最终计数。

原版另在 `Loader_Fluids.java:40–41` 将 HeliumPlasma／NitrogenPlasma 绑定到 He／N 的等离子态，每单位 1000 mB。当前这两个条目都没有材料字段／旁表关联，且标为 HIDDEN，当前物质制造循环也跳过它们。此为现有两种显式等离子体路径的附加缺口；不因源码存在 `createPlasma()` 就声称原版为所有元素生成了等离子体。原版通用 `createPlasma()` 的单位是 L×L=20736 mB，是否有对应材料启用该标志须独立核查，不能套到显式的两项上。

## ALIGN-058：普通汞液体物质分离按 1000 mB，而原版按 144 mB

原版 `Loader_Fluids.java:618` 明确调用 `FL.create("mercury", ..., MT.Hg, 1, L, 300, ...)`，L 为 144；`FL.create` 将该数量存为材料液态。`OreDictMaterial` 的液态单位默认 U，`liquid(U, true)` 因而返回 144 mB。Hg 的 mercury→precmetal→transmetal→metal 工厂链没有给它 LIQUID／MOLTEN 自动覆盖标志；不能把后面通用 `createLiquid()` 的 1000 mB 规则套到这条显式定义上。源 Loader_Fluids 中旧 `createMoltenFluid` 循环是注释，未计入活动路径。

当前 `FluidDefinitions.prepare()` 给 Mercury 添加 LIQUID，生成 `GenLiquid_Mercury`（注册名 mercury）。它有正确材料身份，但 `MatterRecipeLoader` 仅按名称是否以 `molten.` 开头或以 `.molten` 结尾选择 144，否则一律 1000。因此当前普通 mercury 输入为 1000 mB。

core 实际观察 Mercury 为 80 质子、120 中子；按现有加载公式，同一分离配方输出 80 mB 带电物质＋120 mB 中性物质，耗时 26214400 tick、功率 1 QU/t。原版同份输出耗汞 144 mB，当前普通液体耗汞 1000 mB。当前另有 `molten.mercury`，其计算量为 144；这不能替代普通 mercury 的物质量合同。

本批将 `FluidDefinitions` 中“原版 1U=1000mB”的错误注释改为原版 144，并注明当前数量差异仍开放。没有通过改注释悄悄改变现有配方。原生实际注册／运行未执行，结论为 core 目录和实际原生公式的组合证据。

## 特殊转换是否重复注册、选择器是否消耗

同样的 28 条特殊转换既出现在手写加载目录，也出现在 `GTOtherGen`。但仅凭两处声明不能判定游戏中重复：

1. `MachineRecipeBootstrap` 先运行 MatterRecipeLoader，后运行 GeneratedRecipeResolver.loadAll。
2. 两平台生成解析器都将候选配方交给 `RecipeMap.addRecipe()`。
3. `addRecipe()` 默认开启碰撞检查；`findCollision()` 按物品／流体索引查已有输入，`inputsSatisfiedBy()` 检查身份及数量，命中返回 null。

因此，同身份、同数量且都解析成功的重复行会被后到的注册拒绝。无法据源码断言最终恰好拒绝 28 条：形态、流体和其它先到配方仍可能影响原生解析。`MatterRecipeLoader` 的 added 目前在尝试后无条件递增，而生成解析器仅在注册成功后计数，两者日志不能直接合并当最终有效配方数量；该诊断限制已记录，尚未改动。

特殊转换的选择器虽然构造成可见的一件 ItemStack，但 `SelectorTagItem` 继承 `TechItem` 时传入 catalyst=true；`Recipe.isCatalystInput()` 检查该接口，`RecipeInputs.consume()` 预留后恢复催化物部分。不能把栈数量为一直接判为选择器被消耗。原生合成行为仍未实测。

## 本批架构整理

28 条特殊转换迁入 core 的 `content/recipe/MatterConversionCatalog.java`，以 `Conversion(material, form, circuit, duration, outputFluid, outputAmount)` 和有序只读列表表达。共享原生 `MatterRecipeLoader` 只遍历目录并解析物品／流体；元素展开、碰撞处理和实际配方构造保留原样。避免纯数据继续混在平台对象创建代码里。

实际 core 枚举的 28 行与迁移前调用序列及原版数值声明逐项相同。将新目录循环还原为这些调用后，两版 Stonecutter 投影的其余代码／字面量均与基线一致。新目录、注释改动文件和观察探针通过 Java 17 独立编译；未编译整套原生工程。

证据与源摘要见 [回执](verification/matter-fluid-audit-20261019.json)。本批没有 Gradle、全套测试、GameTest、游戏运行、最终原生配方枚举、打包或推送。下一步继续其他相态单位／外部覆盖，转入储能、变压器与网络系统；全面核查仍未完成。
