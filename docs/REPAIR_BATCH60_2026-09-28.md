# 第六十批：电力变压器活动外观与实际控制

## 已实现

- 新增独立 ElectricTransformerControl，负责模式、停止状态、正反转换器各自的工作/实际输出标志和 64 tick 活动窗口；不把这些状态混进材料注册定义。
- 正常/反向转换各保留自己的工作与输出标志。能输出却没有接收者时，下次拒绝实际输入；理论输入面不变。反转时交换所用状态并清缓冲。
- 模式 1–15 按原版限制输出包大小为 floor(最大输出包 × (16−模式)/16)，模式 0 不额外限压；原有分包数量和实际能量扣除规则保持。
- 输出停止开关阻止新的输入，但已有缓冲仍可以送出。界面停止外观与实际最后一批缓冲输出分开，不误把停止当作删除缓存或绝对禁止发电。
- 普通、稳定工作、间歇工作三个外观对应 idle/active/blinking。最近 64 tick 都达到阈值为 active，全未达到为 idle，其余为 blinking；停止时外观为 idle。达到阈值但堵塞也可能显示 active，实际送电需看 active 控制状态。
- 九档变压器增加 ElectricTransformerBlock.ACTIVITY，6 面 × 3 状态 × 2 含水共 324 个模型状态，使用 GT6 原版三套覆盖层/动画，底层材料色与覆盖层分开。
- 变压器接入现有选择器/状态/能源/红石控制面板交互、渲染、NBT、红石输出和破坏掉落。模式/停止状态通过 MachineControl 可被远程控制系统读取。
- 非电池盒变压器不接受没有物品 IO 可控制的 shutter；不声称接通不存在的加工进度接口。

## 原版证据与纠正前轮判断

- MultiTileEntityTransformerElectric.getTexture2：按 mActivity.mState 选择三套覆盖层。
- TE_Behavior_Active_Trinary.check：64 位历史窗口，使用 mActive（达到输出阈值），不是 mEmitsEnergy（实际成功传输）。
- TE_Behavior_Energy_Converter.doConversion：模式限制输出包大小；保存 mCanEmitEnergy 和 mEmitsEnergy。
- TileEntityBase10EnergyConverter.isEnergyAcceptingFrom：停止时不接收；非浪费型转换器要求 canEmit 与 emits 相同才接收。
- TileEntityBase10EnergyConverter.onTick2 在停止时仍执行 doConversion。因此本轮测试明确验证停止后缓冲可继续输出。
- **过载计数不持久化是原版行为，不能继续列为漏实现。** 原版 mExplosionPrevention 只在内存累计；NBT 方法未写入它。原版未工作时每 600 tick 衰减一次，本轮在电力变压器路径补上闲置衰减。当前使用世界 tick 的 600 周期，原版使用方块自身计时；启动前两 tick 的输出过载宽限与完整结构损坏检查仍待进一步核对。
- 活动历史原版 save 方法并未实际写入 NBT，本轮加载时重新建立 64 tick 窗口；保存的是转换器工作/输出标志、模式和停止开关。

## 验证

- 初始 compileJava 通过。
- 8 项资源检查通过，build/assets_batch60.log；包括九档变压器 324 状态的分层着色/覆盖层和原版纹理/动画逐字节一致。
- 新增四项 ElectricTransformerControlTests：64 tick 状态窗口与正反方向独立状态；真实世界三态/堵塞拒收/理论端口；模式限压与停止后缓冲能量守恒及 NBT；选择器/状态面板配置、同步与掉落。
- 完整门禁 build/gametest_batch60_transformer_controls.log：05:44:22 **1012/1012 required tests passed**，退出码 0，BUILD SUCCESSFUL in 9m 30s，05:44:52 正常退出；无 failed! 或 Parsing error。使用新目录 build/gametest-batch60-transformer-controls。运行期间冻结 Java/资源，最新 Java 时间早于最终日志；未启动客户端目视。
- docs/porting-progress.json 已刷新，缺失 token 仍为 49。

## 实机测试与后续

重启 runClient，用有稳定输入的变压器观察启动闪烁、持续约 3.2 秒后的 active 覆盖层和断能后的回落；停止开关应立即令外观 idle。输出堵塞可能保持工作外观，但状态面板的实际输电指示应区别于“达到工作阈值”。

贴选择器后提高模式数字，观察输出电压包大小下降；选择过高模式可能低于输出阈值而不送电，这是原版规则。停止输入后原缓冲仍可能送出少量能量。保存重进应保留模式、停止和面板；撬下选择器恢复模式 0，破坏应掉落剩余面板。

下一轮有明确代码证据的占位：EnergyNodeDefinitions.java:114 的 battery_eu_8/32/128/512/2048 仍为通用 EnergyNodeBlock，固定 V×64000 容量并使用普通 BlockItem；与第五十四批真实化学电池的物品充放电、放置和保电掉落不一致，材质也仍按机壳梯度配色。应先核对原版高级化学电池身份再整合，避免留下两套矛盾规则。

高档电路和原版缺失 10049 制作依赖、旧通用电池电压规则、其它能源占位物、Actually Additions 兼容材料仍未完成。大型坩埚仍待实机验证。本轮未构建 jar，没有执行 git commit，完整目标仍在进行中。
