# 内容注册指南

2026-09-11。目标仍是还原 GT6；GTM 只用来观察定义、注册、运行状态的分层方式，没有复制其实现或引入 GT5 的内容规则。

后续修复：原子属性、隐藏粒子、模型旋转及资源重载检查见 [渲染与材料校验记录](RENDERING_AND_MATERIAL_VALIDATION.md)。下文 1137 个材料快照是上轮改造记录；补入五种粒子后当前为 1142 个。

## 从哪里开始

公共扳手/剪线钳交互以及 F10、F11 的本轮重做边界见 [GT6 行为重做记录](GT6_REBUILD_2026-09.md)。新增机器应声明工具操作策略，不能再复制九宫格算法或在客户端增加方块类型白名单。

- `content/material/Materials.java`：常用材料的可读引用，例如 `Materials.Copper`、`Materials.Iron`、`Materials.Steel`。
- `content/material/generated/`：元素、化合物、矿物、石材、木材五组导入定义。由脚本生成，不直接编辑。
- `content/material/MaterialDefinitions.java`：材料声明与关系关联的启动入口。
- `content/machine/BasicMachineDefinitions.java`：基础机器及材质变体的内容定义。
- `content/energy/EnergyNodeDefinitions.java`：能源设备定义。
- `content/multiblock/MultiblockDefinitions.java`：多方块控制器和部件的注册入口。
- `api/material/MaterialDefinition.java`、`api/machine/BasicMachineSpec.java`、`api/energy/EnergyNodeSpec.java`、`api/fluid/FluidDefinition.java`：新增内容使用的具名参数接口。
- `registry/`：把内容定义提交为 Forge 对象；`blockentity/` 等实例实现保存库存、进度、结构状态。

以上 Java 路径均相对于 `src/main/java/com/gregtech/gregtech/`。

## 材料

新内容使用完整材料名。导入表的 `ImportedMaterialData.Cu` 与 `Materials.Copper` 指向同一对象；旧 `MT.java` 已移除。化学式中的 `Cu`、能源类型 `EU/RU/KU` 以及已有注册 ID 保持领域含义，不进行盲目展开。

新增材料可以放在单独的手写目录类中，通过 `MaterialDefinitions.declare()` 调用它的声明方法。先声明全部材料，再在 `link()` 阶段设置跨材料关系。不要在静态初始化期间互相访问尚未声明的目录字段。

```java
// 示例：availableId 必须由维护者选择一个尚未占用的稳定 ID。
GTMaterial sample = MaterialDefinition.builder(availableId, "ExampleCompound")
        .displayName("Example Compound")
        .color(0xAABBCC)
        .dust()
        .meltingPointKelvin(1200)
        .boilingPointKelvin(2400)
        .density(2.5f)
        .chemicalFormula("Example")
        .register();
```

`build()` 生成定义快照；`register()` 才写入注册表。新入口拒绝重复 ID、重复名称、非法属性，以及材料关联完成后的补注册。`density` 沿用现有 GT6 材料模型的标度，不等于流体密度参数。

生命周期为 `DEFINITIONS → LINKS → READY`，失败进入 `FAILED`。这是新注册入口的约束，不代表所有旧 `GTMaterial` 对象已不可变。旧导入工厂仍允许历史数据的特殊 ID 和覆盖行为；不要用它们绕过新增内容的校验。

## 机器

```java
BasicMachineSpec spec = BasicMachineSpec.builder("example_mixer", Materials.Steel)
        .machineType("mixer")
        .recipes(MachineRecipeMaps.Mixer)
        .energy(GregTechTags.Energy.RU, 32)
        .tier(1)
        .parallel(1)
        .strength(6, 6)
        .build();
```

将定义加入基础机器目录，由已有注册流程消费。配方表与并行数直接保存在定义里，机器实体不再根据机器名字猜配方表和并行数。`machineType` 仍用于既有纹理等约定；新增类型需要相应资源。旧构造器保留给导入数据，未知机器名现在会报错，不再悄悄回退成熔炉。

定义不包含运行中的输入输出、能源缓存、当前配方或 NBT。定义一个机器不等于完成它的行为、配方、模型、菜单和掉落。

## 能源设备

```java
EnergyNodeSpec spec = EnergyNodeSpec.builder("example_converter", Materials.Steel)
        .kind(EnergyNodeSpec.Kind.CONVERTER)
        .texture("example_converter")
        .input(GregTechTags.Energy.EU, 32)
        .output(GregTechTags.Energy.RU, 24)
        .capacity(128)
        .name("Example Converter")
        .build();
```

中文名称只从共享语言文件及 `tools/localization/aliases.json` 中的原文绑定读取；定义只保存英文后备名称。旧双名称方法与构造器保留用于二进制兼容，但不再保存第二套翻译。

将定义加入 `EnergyNodeDefinitions`。输入、输出、容量与名称使用具名参数，避免长串位置参数混淆。数值遵循当前设备实现的 GT6 能量包/速率语义，不能统一当作 FE。此改造没有改变转换器或汽轮机算法。

## 流体

```java
FluidDefinition.builder("example_fluid")
        .temperatureKelvin(400)
        .density(1000)
        .viscosity(1000)
        .color(0xFFAABBCC)
        .material(Materials.Copper)
        .register("ExampleFluid");
```

此处颜色为 ARGB；材料颜色为 RGB。气体可调用 `.gas()`，流体密度允许负值以表达气体行为。声明必须在 `Loader_Fluids` 提交定义之前执行；提交后拒绝补注册。新入口同时检查 source/flowing 名称经过规范化后的冲突。

`.material(...)` 保存材料关联键，不会自动补齐旧系统中尚未实现的熔融/气态材料绑定，也不会凭空生成贴图或配方。

## 多方块

控制器和部件中重复的方块、物品注册已统一为 `MachineBlockRegistration.block(id, factory).strength(...).register()`。一次调用注册同名方块和对应物品，延迟创建时使用参数快照。

这次没有实现通用结构 pattern 系统。结构旋转、匹配、部件角色、端口能力和失效解绑仍属于现有控制器逻辑，后续需要单独设计与验收；不能只新增一条注册就认为完整多方块已实现。

## 旧简称与生成工具

手写代码使用 `RegisteredFluids`、`MachineRecipeMaps`、`FuelRecipeMaps`、`MaterialPrefixes`、`GregTechTags`、`ModReferences`、`ItemReferences` 等可读名称。14 个旧简称兼容类已删除（包括旧 `OD` 枚举）；统一使用 `OreDictionaryNames`。原版导入别名和特殊行集中到 `ImportedMaterialData`，不是手写新内容的扩展入口。详细变更见 [内存与数据入口清理](MEMORY_AND_DATA_CLEANUP.md)。

`tools/transpile_gt6_materials.py` 生成五组材料目录及旧符号兼容层；`tools/generate_mt_fields.py` 刷新旧别名及可读引用。其他历史解析器通过 `tools/generated/materials-source.java.txt` 读取保留的中间数据。中间文件用于工具兼容，不是另一套运行时注册表。

`tools/transpile_gt6_data_registries.py` 是历史骨架生成器，输出改到 `tools/generated/data-skeletons/`，不会再覆盖已经维护的正式注册代码。生成工具不等于自动更新验收：从原版重新导入后仍需审查差异。

## 验证与限制

```text
python -m unittest discover -s tools/tests
python tools/check_registration.py --offline
```

第二条要求 Java 17、Python 3 和已缓存的 Gradle 编译依赖；首次下载依赖可去掉 `--offline`。它执行 `compileJava`，在临时目录编译只模拟模组存在性的 `ModList` 测试替身，并在两个独立 JVM 中检查正常启动和先访问材料引用的启动顺序。替身不进入正式模组。

本次检查覆盖重复注册、阶段约束、材料引用身份、253 个基础机器变体、53 个能源设备，以及流体命名冲突。生成测试检查 1056 条导入声明重新生成后与现有目录一致，及世界生成材料引用可解析。

改造前后额外比较了 1137 个材料的属性、组分和加工目标快照，结果一致。它不代表完整存档兼容性验证。本轮没有完成游戏内运行验收；配方概率输出、处理中输出的持久化和溢出等既有问题仍需后续处理。
