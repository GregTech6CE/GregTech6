# 旋转变速器目录与运行控制核查

日期：2026-10-19；基线 `7b5508a08`。本批核对十三种旋转变速器的目录和共享能源路径，展开两份 core 压行代码，没有修改玩法。

## 来源与范围

原版入口为 `gregtech/loaders/b/Loader_MultiTileEntities.java` 的十三行 `MultiTileEntityTransformerRotation`，沿该类进入 `TileEntityBase11Bidirectional`、`TileEntityBase10EnergyConverter`、`TE_Behavior_Energy_Stats` 和 `TE_Behavior_Energy_Converter`。当前入口为 `GearboxCatalog.transformer` → 共享 `GTGearboxes.registerTransformer` → `EnergyNodeBlock`／`EnergyNodeBlockEntity`；公共注入经过 `GTEnergyBlockEntity`、`EnergyBlockDefaults`、`EnergyGate`。

原版源码根目录为 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java`。精确来源哈希和观察结果见 [回执](verification/rotation-transformer-audit-20261019.json)。

## 已对应的目录与分支

十三种材料依次是防腐木、青铜、黄铜、砷铜、砷青铜、钢、钛、钨钢、铱、钛铱、Trinitanium、Trinaquadalloy、Adamantium。原版额定输入为 V[t]、输出为 V[t-1]（木制输出 2），缓存为两倍输入；当前 core 目录逐项对应。两版本共享注册消费者，均构造 EnergyNodeBlock，物品堆叠上限 16；未运行完整原生注册表。

正常方向正面输入、背面输出，反向交换；正向输出四包，反向一包。反向的输入推荐值仍为原始输入额定值，不是输出额定值。当前这点与原版对应。当前保存 inverted 和负向 RU 标记，换向清缓存；保存代码存在不等于已经通过世界重启验证。

未停止且模式为零时，当前按 `floor(buffer * output / input)` 正向换算，反向输出大小等于 buffer，并无论接收器是否接收都扣至多 inputMax。此固定耗损对应原版 `NBT_WASTE_ENERGY=T`，不能把无接收器耗损误报为缺陷。

## ALIGN-067：停止状态的输入与缓存处理相反

原版 `TileEntityBase10EnergyConverter.isEnergyAcceptingFrom` 在实际注入时要求 `!mStopped`；`onTick2` 仍调用 `doConversion`，所以停机后拒绝新输入，但仍处理和耗损已有缓存。

当前 `EnergyNodeBlockEntity.isEnergyAcceptingFrom` 的旋转变速器分支只检查输入面，没有检查 converterStopped；`emitOutput` 却在 converterStopped 时直接返回。`machineControl` 的通用回退确实能写这个停止字段，因此不是不可达的布尔值差异。

以青铜缓存 32、正向模式零为例：原版停止后拒绝下一包，并仍可尝试四包大小 8 的输出，然后缓存归零；当前停止后保留缓存 32，仍能再收一包 32 直到满 64。此例来自调用链与算术推导，没有运行原生世界。

## ALIGN-068：旋转变速器没有输入／输出过载路径

原版 Energy_Stats 对绝对包大小超过最大输入的情况返回全部提供包数，执行注入时标记过载；父类消费标志，前一百次清缓存并累计保护计数，之后走 overcharge。输出换算超过最大值也有过载处理，加载最初两 tick 另有清缓存宽限。

当前 `doInject` 仅为电磁铁／电变压器调用超限处理，旋转变速器落到 `EnergyPackets.fitting`。公共 EnergyGate 仅做最小包检查，不替它补最大包判断。青铜空缓存 64，输入大小 65 时，当前返回 0、没有过载；原版返回提供量并进入过载保护。core 探针观察最大包门限没有拦截回调、fitting 返回零；原版结论为源码推导。

当前 `emitRotationTransformer` 对超过 outputMax 的大小只是不输出，然后按固定耗损扣缓存，也没有输出过载标志。正常新注入受当前容量／整包向下取整限制，未证明该输出超限状态在正常玩法中可达；不能把这一分支缺失说成已复现的日常爆炸差异。输入超限差异不依赖该可达性问题。

## ALIGN-069：原版模式限速没有接入旋转变速器

原版父类提供 `setStateMode/getStateMode`；转换器 mode>0 时把输出大小限制为 `floor(outputMax * (16-mode) / 16)`，耗损改为 `ceil(inputMax * (16-mode) / 16)`。

当前旋转变速器不属于 hasOriginalConverter，也不属于 electric transformer 控制器；machineControl 落到 supportsMode=false 的通用分支。`hasControlPanels` 排除它，专用输出函数也没有消费任何 mode。以青铜缓存 64、mode=8 的原版状态为例，输出大小最多 8、耗损 32；当前仅有模式零公式，输出大小 16、耗损 64。不能将“反向切换”当成这套限速模式已经实现。

## 扩展已有差异与未完成部分

- ALIGN-059 的最后整包向下取整同样覆盖 RU：青铜缓存 60/64、提供一包 32 时当前拒绝；原版接受后为 92。当前拒包具有避免超缓存的保护效果，不在本轮擅自撤销。与 ALIGN-047 的接收后截断不同。
- ALIGN-017 的周期环境检查在旋转变速器路径也缺少；具体 RU 设备应触发哪些风险仍由原版类型、材料、侧面和覆盖板条件决定，不能推断所有材料遇水都坏。
- 当前负向 RU 用绝对值计量并保留符号；没有复用电变压器 ALIGN-060 的负 EU 缺陷结论。
- 原版活动三态、正负向纹理、声音、覆盖板反馈仍需沿模型和客户端消费完整核查；本批不声称渲染已对齐。
- 可调齿轮箱仅展开 GearboxRotationRules，核读 checkGears/getRotations。原版无有效齿轮输入的诊断回退返回上次 mRotationData，当前无状态函数返回零；已修正文档说明，未证明该回退在实际调用中可达，未单独登记玩法缺陷。轴直通、交错输入和破坏逻辑继续排队。
- 长距离变压器、跨区块网络和其它未核系统保持在全面审计范围内。

## 可读性与验证

GearboxCatalog 和 GearboxRotationRules 展开构造器、参数与控制分支，保留代码和字面量；目录注释限定为额定数据，避免暗示整个运行逻辑完全等同原版。改动没有改变 ID、数值、接口或存档键。

Java 17 局部编译两份改动类、EnergyGate／EnergyPackets 和 RotationTransformerAudit，通过十三行目录及五行观察。齿轮函数枚举 256 个布局与 3072 个方向／符号组合，结果为 121 个可工作布局、600 个非零旋转结果；这只是当前 core 观察，不是原版实体全行为等价证明。整理前后另以去注释／空白的代码和字面量比较核对不变。

未运行 Gradle、原生编译、Minecraft、存档重启或全量测试；没有构建、推送。全面目标尚未完成。
