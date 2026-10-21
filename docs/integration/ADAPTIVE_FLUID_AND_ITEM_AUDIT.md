# 可调储液容量与桶物品能力核查

日期：2026-10-19；基线 `1f87ed351`。原版来源根目录 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java`，重点追踪 `Recipe.RecipeMap`、`FluidTankGT`、`MultiTileEntityBasicMachine`、`TileEntityBase08Barrel` 和 `MultiTileEntityItemInternal`。原作者 GregTech-6 Team / Gregorius Techneticies，原始 LGPL 文件只读。

本轮继续上一批储液审计，补齐可调容量的键、初始化、倍率和桶物品态入口。没有把全部物流、全部机器、所有便携容器或物品恢复流程算作已验收。

## ALIGN-095：可调容量使用流体类型 ID，空罐首次填充也未按输入流体查表

原版配方表以 Fluid.getName() 建立 mMinInputTankSizes；FluidTankGT.capacity_(Fluid) 使用同一名称查表。原版 fill 的空罐分支使用 capacity(aFluid)，模拟和实际填充都会考虑即将进入的流体。

当前 RecipeMap.fluidKey 使用实际 Fluid 的注册 ID，而 FluidTankGT.key() 和 capacity(FluidStack) 使用 getFluidType().toString()。这两个标识属于不同注册表。

不能笼统判断 toString 返回 Java 对象地址。本轮读取本机缓存的 Forge 47.4.20 与 NeoForge 21.1.243 源码，确认两者 FluidType.toString 均返回类型的注册 ID。原版水／熔岩类型也注册在 minecraft 命名空间；普通 GT 静态流体的 type 和 fluid 通常使用同一路径，因此这些正常情况并非都查表失败。

确定受影响的本模组路径是 GTWorldWaterFluid：海水、河水、沼泽水的实际 ID 各不相同，但 Source 与 Flowing 的 getFluidType 都返回原版 WATER_TYPE。所以它们的配方容量会被作为 minecraft:water 查询。共享类型的第三方流体和流动变体也有同类风险，本轮不列出未经核实的第三方名单。

此外当前空罐 fill(resource, action) 调用 capacity()，此时内部 fluid 身份为空，键为 null，第一次只使用基本容量。普通 ID 正确的流体在首次执行填入后，下次调用可以识别自己的容量，不能说它们永久无法扩容；要求一次模拟接受完整输入的调用方则可能拒绝该操作。

纯 core 合成样本：基本容量 8000，表中 water=1000、seawater=32000。null 或水类型键返回 8000，海水自己的流体键返回 32000。这是可复核的查询差异示例，数值不是声称来自某条内置海水配方，未运行原生注册或管道填充。

## ALIGN-096：基础配方机的罐默认值、并行倍率及输出容量不同

原版 MultiTileEntityBasicMachine.readFromNBT2 使用默认 1000（可由 NBT_TANK_CAPACITY 覆盖）构建输入罐，再关联配方容量表，倍率为 mParallel * 2。输出罐使用无参 FluidTankGT，默认容量 Long.MAX_VALUE。

当前 BasicMachineBlockEntity 初始化输入罐为 8000，关联同一配方最小容量表时倍率固定 1；输出罐也初始化为 8000。checkTankCapacity 方法只调用 setChanged，没有补上容量重算。当前机器确实有 parallelLimit，不能归因于所有并行功能都缺失。

两项输入差异互相独立：即使流体键正确，倍率仍与原版不同。例如单批 32000、并行上限 1，原版这条公式给 64000，当前关联表给 32000；而没有较大配方要求时，当前默认 8000 又可能高于原版默认 1000。不能笼统说所有输入罐都更小。原版各机器 NBT 容量覆盖和所有现行规格的逐项对应另待核查。

输出差异限定继承基础初始化且没有覆写的路径。焦炉、蒸馏塔、部分大型配方机等已有显式 Long.MAX_VALUE 输出罐设置，不能重复判它们只有 8000。原版无限名义容量与 canOutput 的积压限制是两回事，后者另见 ALIGN-038／043，不能用一个有限罐替代原版停产条件。

## ALIGN-097：普通桶及物流桶物品缺少流体容器能力

原版 MultiTileEntityItemInternal 实现 IFluidContainerItem，通过注册项建立对应临时实体，并转发 getFluid、getCapacity、fill、drain。Barrel 的便携重载检查封口、流体许可及防护，实际装／抽后把结果写回物品 NBT。物流桶继承这条入口。

当前 Forge 的桶物品注册为 GTMachineBlockItem，Neo 注册为 FluidTransportBlockItem；两者都是 BlockItem 子类，提供提示和含液堆叠限制，但没有提供流体物品 handler。维护中的生产源码没有为这些桶附加 Forge 流体物品能力；Neo FluidCapabilities 的两条物品注册分别覆盖 FluidDisplayItem 和 PortableFluidContainerItem，也没有覆盖普通桶／物流桶。

因此本模组自身未给这些桶实现对应的物品态装液／抽液合同。这不代表桶完全不能保存或运输液体：TankBlock.getDrops 会把 saveItemData 写入 BlockEntityTag／BLOCK_ENTITY_DATA，含液桶的堆叠数限制为 1，放置依赖 BlockItem 的方块实体数据恢复。此保存路径与物品流体能力是两个不同入口；本轮只确认前者源码存在，没有宣称完成实际掉落、放置、重启验收。

PortableFluidContainerItem（量杯、胶囊等）的 handler 已存在并注册，不能将其归入本缺口。其他模组可能为桶额外附加能力，但那不能作为本模组已移植的证据。

## 标准化与验证

展开共享 FluidTankGT 的填充、抽取、容量和 NBT 压行代码；整理 Neo FluidTransportBlockItem、FluidCapabilities，并明确各自职责。保留所有执行代码、数值、身份字符串及注册顺序，共享类分别展开 Forge／Neo 条件分支进行代码和字面量等价核对。

Java 17 只编译当前 LongFluidStorage 和 AdaptiveFluidCapacityAudit，记录七项查询／构造参数观察。使用同一纯类输入两套参数，仅用于检验对应公式，不能冒充运行原版类或原生机器。框架源码版本来自本地缓存，标识行为不是联网检查，也不声称覆盖所有 Forge／NeoForge 版本。

未原生编译、全量测试、游戏、存档重启、构建或推送。来源哈希、源码等价、框架证据与观察值见 [回执](verification/adaptive-fluid-audit-20261019.json)。后续仍需检查标签／components 的比较、库存和物品容器交互、覆盖板、完整物流网络与其余系统。

## 第五十九批：ALIGN-095 的反应堆热罐实例

核反应堆虽覆写`capacity(FluidStack)`为Steam提供10240000容量，实际`fill`的空罐和非空分支仍走无参`capacity()`，未消费这项覆写。空热罐、401单位蒸馏水及32080HU可生成64160单位Steam，当前模拟只允许64000而失败；历史超量因已有量下限仍保留，不报全旧罐截断。该实例扩展本项填充入口合同，不另凑容量编号，与满普通热液罐先冷却后排出另属独立次序问题，见[核心专题](NUCLEAR_CORE_AUDIT.md)及[本批回执](verification/nuclear-contract-audit-20261021.json)。没有在本补证中修正生产行为。
