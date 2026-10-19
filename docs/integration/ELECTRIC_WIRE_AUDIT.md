# 导线、电缆目录与输电边界核查

日期：2026-10-19。基线：`861f803d9`。这是全面核查的一批，未完成整个项目验收。

原版根目录为 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java`。主要依据 `gregtech/loaders/b/Loader_MultiTileEntities.java:1914–1950`、`gregapi/tileentity/connectors/MultiTileEntityWireElectric.java` 及其父类 `TileEntityBase10ConnectorRendered`。原版文件保留 GregTech-6 Team／Gregorius Techneticies 与 LGPL 声明；本轮只读来源，未复制第三方实现。

## 已核目录和正常输电路径

30 组材料注册对应 480 个裸导线、140 个绝缘电缆，共 620 项。`WireCatalog` 的材料字段通过旧符号门面对应到原版 MT 符号，电压用原版 CS.V 展开；基本安培数、两种损耗、接触伤害标志、是否提供电缆逐项对应。每组裸线 1–16x，电缆 1／2／4／8／12x；尺寸直径和堆叠上限按原版 addElectricWires 的显式值逐项核对。材料量为尺寸乘 U/2，原版依据 OP.wireGt／cableGt 的 setMaterialStats；不是每份一整单位。未将此结论扩大为所有材料的属性一致。

两版实际消费者分别是 Forge `content/energy/WireDefinitions.register` 和共享 `registry/GTWires.register(IEventBus)` 的 Neo 分支，均读此目录。原版石墨烯和超导材料均没有电缆、没有接触伤害；超导材料仍损耗 1 EU/m，这一点当前对应，不应仅根据名称强改零损耗。

当前 `ElectricWireBlockEntity` 的主要行为与原版对应：

- 模拟注入返回连接接受的包数，不预留下游空间。
- 每一段先按符号扣损耗，再向相邻面递归；没有接收方实际耗包就不累计导线过载。
- 累计本 tick 的实际安培数和损耗后的功率；超额时增加 burnCounter，调用者消耗剩余提供包数。不是达到额定电压就立刻删线。
- burnCounter 达到 16 后在 tick 着火；每 512 tick 的余数 2 时衰减一次；功率、电流、烧毁计数没有写入存档，原版也未保存这些运行时计数。
- 裸线接触伤害使用上一 tick 功率，绝缘电缆和上述不伤害材料不触发。当前已有 GTEntityHelper 伤害入口，不能再把这一点报成完全没做。
- 普通线对非线邻居使用 EnergyTransfer.insertEnergyInto，可进入 EnergyBlockViews／EnergyCompat；不能套用长距离端点绕过适配的 ALIGN-071。

这里的“对应”来自源代码控制流核对。原生调度顺序、复杂分叉网络和真实玩家碰撞未运行。

## ALIGN-077：导线没有接入进度计读取电流的接口

原版 `MultiTileEntityWireElectric` 实现 ITileEntityProgress，getProgressValue 返回 mTransferredAmperes，getProgressMax 返回 mAmperage。原版 `MultiTileEntityProgressmeter.getCurrentValue/getCurrentMax` 直接读取该接口。

当前 `ElectricWireBlockEntity extends BlockEntity implements IEnergyConductor, IEnergyBlock`，有 transferredAmperes() 数值但既不实现 ProgressSensorSource，也不实现 MachineControl.Provider。`SensorBlockEntity` 的 PROGRESS 分支仅接受这两个入口或刷怪笼；MachineControl.find 也只认 Provider，因此面对导线取不到原版电流／上限。

ENERGY 分支已有专门的 wire.lastWattage() 读取，表示功率计支持仍在；它不能替代进度计读取电流。这是确认的源代码接口缺口，待实际游戏复核显示及采样顺序，本轮不修改玩法。

## ALIGN-078：导线缺少建筑泡沫包覆状态与硬化绝缘

原版导线父类 `TileEntityBase10ConnectorRendered` 实现 ITileEntityFoamable。对直径不足整块且未包覆的连接器，applyFoam 设置湿泡沫、颜色和可选所有权；dryFoam 硬化，removeFoam 移除硬化外层。原版导线碰撞明确要求 `mContactDamage && !mFoamDried`，硬化外层还能改变表面、光遮蔽和碰撞行为。

当前导线方块状态仅有六面连接与含水状态；实体没有湿／干泡沫状态及保存入口。碰撞只检查 contactDamage 与输电功率。现用 BehaviorSprayFoamHardener.harden 只识别 CFoamBlockEntity，BehaviorSprayFoamRemover.remove 只识别 CFoamBlock，不能给导线提供原版包覆后的状态转换。

这项缺口限定在导线包覆与硬化绝缘，不代表独立建筑泡沫方块、硬化剂和去除剂全部未实现，也未据此判定所有管道／所有喷罐的完整功能。

## 仍需继续核查的边界

- 原版 transferElectricity 有 mTimer<1 门槛，当前 canTransfer 没有时间条件；新实体首次 tick 前的真实注入顺序待原生场景确认。原版父类 TileEntityBase02AdjacentTEBuffer.updateEntity 增加 mTimer。
- 原版有条件的 IC2 主动取电路径，当前 serverTick 没有相同路径。旧 IC2 与现代 FE 能力不同，未直接据此宣称所有外部发电机不兼容。
- 原版以 TileEntity 身份记录已访问集合；当前按 BlockPos。当前先通过线的接受面检查才进入子递归，原版先将邻居加入集合。复杂环路／多个入口拒绝面的可达影响尚未验证。
- 当前输电遇到未加载邻区块会跳过，不能据此证明辅助连接索引、渲染查询和其它生命周期也从不访问区块。实体已有 setRemoved／onChunkUnloaded 注销，不能重报为完全未清理。
- 辅助 EnergyNet 在 onConnectionChange 清空本端边，但没有同时移除旧邻端的反向边；查询可能保留旧连通性。目前维护 Java 源没有 getConnectedWires／getReceivers 的调用者，实际 EU 传输也不读该图，因此没有将它判作已发生的“断线仍输电”。其删除／保留和公开接口兼容需要单独决定。
- Long.MIN_VALUE 拒绝与饱和加乘是当前防溢出保护，原版直接 long 算术；本轮保留保护，不称作原版逐位复刻。

## 本轮结构整理与验证

展开 WirePacketRules、WireCatalog 和 Forge WireDefinitions 的压行代码；说明有符号损耗的来源及算术函数调用前提。纠正 EnergyNet 的调度归因，删除未引用的私有 DUMMY_PROPS；保留索引 API 与生命周期。修正 GTWires 过时加载类名及“每尺寸一单位材料”的说明。材料导航补充形态生成表与属性容器入口。

Java 17 定向编译三个 core 类与 WireCatalogAudit，输出 620 个目录条目和九条算术观察。五份生产 Java 相对基线代码／字面量等价（仅排除明确删除的私有未用空数组）；共享注册／索引代码先显式展开 Stonecutter 注释分支，再分别比较 Forge 和 NeoForge。

见 [回执](verification/wire-audit-20261019.json)。其余 core 依赖使用已有编译输出；未原生编译、未运行世界／游戏测试、未构建或推送。本报告的接口和包覆缺口来自源码消费链，不能将纯函数观察当作游戏通过。
