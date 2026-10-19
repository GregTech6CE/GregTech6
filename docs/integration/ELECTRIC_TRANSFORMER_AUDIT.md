# 电变压器参数、输入包与控制状态核查

日期：2026-10-19；基线 `0848c890b`。本批核查九档电变压器，整理控制状态与 NBT 适配的压行格式。只记录玩法差异，没有改变能源算法；电池盒、旋转变速器和长距离变压器仍在后续队列。

## 已对应的额定目录和主要控制路径

原版 `gregtech/loaders/b/Loader_MultiTileEntities.java:880` 后九条电变压器注册使用 Electric_T[0..8]、V[1..9] 输入、V[0..8] 输出和四包倍率。`TileEntityBase10EnergyConverter.readEnergyBehavior()` 的缓存为输入额定值乘二。当前 `EnergyNodeDefinitions.specifications()` 的九项与这些数值、材料对应；core 实际枚举结果见回执。

| 当前 ID 后缀 | 材料 | 输入／输出 EU | 缓存 EU |
| --- | --- | --- | --- |
| ulv_lv | TinAlloy | 32／8 | 64 |
| lv_mv | SteelGalvanized | 128／32 | 256 |
| mv_hv | Aluminium | 512／128 | 1024 |
| hv_ev | StainlessSteel | 2048／512 | 4096 |
| ev_iv | Chromium | 8192／2048 | 16384 |
| iv_luv | Titanium | 32768／8192 | 65536 |
| luv_zpm | Iridium | 131072／32768 | 262144 |
| zpm_uv | OsmiumElemental | 524288／131072 | 1048576 |
| uv_xv | Trinitanium | 2097152／524288 | 4194304 |

源码调用链确认：

- 正向从正面输入、其余面输出；反向相反。原版 `TileEntityBase11Bidirectional` 与当前 `isEnergyAcceptingFrom/isEnergyEmittingTo` 对应。
- 升压时输入推荐值仍是原来的高压额定值，并非低压值；双方的最小、最大包与反向输出最小值 3/4 高压额定值对应。
- 正向按缓存除四计算输出包大小，最多四包；反向用缓存大小输出一包。输出成功后按实际包数扣除能量；原版换算分母在这九项上约去，不产生额外效率损失。
- `toggleInverted()` 会清空缓存并切换独立的可输出／已输出标志，符合原版交换两个转换器的主要状态行为。
- 停机阻止新输入，但仍可输出已有缓存；原版 `doConversion()` 也不以 mStopped 提前返回，不能把此行为误报为停机失效。
- core 观察到输出被拒时停止接受新包、两个方向的输出状态独立、64 个连续可输出 tick 后视觉状态为 1、停机视觉为 0。模式 15 将最大 64 限到 4；这些不是游戏画面实测。
- NBT 仅保存当前方向的输出标志，再用同一组标志初始化两个方向；原版也是仅保存当前转换器、重建两个转换器读取同一 NBT。不能仅凭 restore 填充两个数组判为错误。

## ALIGN-059：最后一整包的接收边界不同

原版 `TE_Behavior_Energy_Stats.doInject()` 在缓存未满且包未超过最大值时，先限制可用能量，再向上取整需要接收的包数。因此最后一包可以把缓存暂时推到额定容量以上。它不是按字节截断能量包。

当前 `EnergyNodeBlockEntity.doInject()` 的电变压器分支使用 `EnergyPackets.fitting()`，按 `space / magnitude` 向下取整。ULV–LV 缓存 60/64、输入一包 32 时，当前拒收，原版接收一包并得到 92。32 满足正常输入范围 16–64，这个例子不涉及欠压吞包或过压保护。

core 直接调用当前帮助函数观察到剩余 4、包 32 返回 0；原版接收 1／变为 92 来自原版公式推导，没有启动原版 Minecraft。该差异会改变缓存补充、输出大小和后续过载路径。它与 ALIGN-047 基础机器“接收后把缓存截断”不同：这里是在接收前拒收最后一包。

## ALIGN-060：合法幅值的负号 EU 输入被后续分支拒收

原版 Root 的注入入口用绝对值检查最小包，`TE_Behavior_Energy_Stats.doInject()` 再取绝对值储存；电能不属于负输出能量，因此不会因此输出负 EU。

当前 `EnergyBlockDefaults.doEnergyInjection()` → `EnergyGate.gateInjection()` 同样允许幅值足够的负包进入 doInject，但电变压器随后调用 `EnergyPackets.magnitude(size, energyType == RU)`，EU 的 -32 变成 0 并拒收。core 观察确认 -32 通过最小 16 的门限回调，而 EU magnitude 返回 0。

范围限定为能源接口接收到负号 EU 包的情况；未发现或宣称普通发电机必然生成这类包。第三方供能、网络符号处理仍需继续核查，不能把它描述为所有正电压变压器不可用。

## 环境、过载和覆盖板的核查边界

电变压器 `emitElectricTransformer()` 每 600 tick 仅在不可输出时衰减过载计数，没有原版活动时的 `doDefaultStructuralChecks()`；这扩展了既有 ALIGN-017 的确认范围，不再另立重复编号。其运行中的具体火、水、雨、雷条件继续以原版父类与配置为准。

原版转换器在加载后前两 tick 对输出过载清空缓存而不设置 mOverloaded；当前输出路径直接调用 magnetOverload，没有这层时间条件。当前正常正包接收采用向下取整，已限制了自然形成超额缓存的路径；因此这里只记录条件控制流差异，尚未证明现有正常存档会触发，不新增高确定性故障编号。

电变压器已调用 `panels().beforeTick/afterTick`，不能宣称所有覆盖板都不工作。共享 ComponentCoverRuntime 的搬运 tick 没有进入此分支，但其泵／传送带／机械臂首先要求对应流体或物品槽；尚不能据此构造电变压器缺失搬运功能的有效案例，本批不立缺陷。

## 整理、验证和后续

`TransformerControlState` 与 `ElectricTransformerControl` 展开为逐方法、逐语句格式，保持原有字段顺序、方法顺序、NBT 键、计算和调用不变。代码及字符串签名与基线相同。新增 core 观察入口 `tools/integration/audit-probes/TransformerAudit.java`。

Java 17 独立编译控制状态、能源包／门限、额定目录和观察入口通过；原生 NBT 适配只做代码／字面量一致核对，未编译 Minecraft 适配器。没有运行 Gradle、全套测试、Minecraft、世界重启、打包或推送。来源哈希、九项原版目录比对与原始观察见 [回执](verification/electric-transformer-audit-20261019.json)。

下一步继续电池盒／单体电池的充放电、库存和保存，再核旋转／长距离转换以及能源网络；全面审计仍未完成。
