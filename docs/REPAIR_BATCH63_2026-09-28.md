# 第六十三批：旧电池能源规则与电压隔离

## 原版与边界

原版 gregapi/tileentity/energy/TileEntityBase08Battery.java:155–173 对充放电包取绝对值、校验输入/输出范围、限制每次包数为额定电压数值，并允许充满时接收最后一个不足整包容量的包（多余能量丢弃）。228–229 要求物品数量为 1，理论能力只看能源类型和电压。

本轮对象为早期移植的 battery_lv/mv/hv/ev/iv 非放置物品。它们的容量不是已证实的原版化学型号，不能声称已还原为某类化学电池。因此保留原 ID、容量与旧电动工具变体，只修正明确错误的任意电压充放电。正规五类化学电池已经遵循这些规则。

## 修改

- BatteryItem 按 LV–IV 各档限定最小包为 V/2、最大包为 2V；理论充放电能力同步采用限制，电池盒不再把旧 LV 电池计作 MV/HV 能源提供者。
- 实际充放电接受正负包的绝对值；拒绝 Long.MIN_VALUE、超范围、非正包数和非单件电池。
- 每次包数最多为 V，提取只能消耗完整能量包；注入最后一个包可补满，存储值不越过容量。模拟调用不创建或修改 NBT。
- 补电量/容量与输入电压范围 tooltip，复用已有双语键。
- ChargingCraftingRepairTests 原先用 LV 电池接收 512 EU 来测试掉落，现先断言拒绝 512，再用合法 32 EU 检查保电掉落。
- ElectricWireRepairTests 原先期待负包被旧电池拒绝，改为原版绝对值接收并检查累计三次真实输电。铜线电压边界测试改装 MV 和 HV 电池：铜线额定值为 256，MV 最大允许 256，越界 257 必须由 HV 电池接收才能形成真实过载电流，不能依赖旧 LV 电池无限制接收。

## 验证

新增 LegacyBatteryVoltageTests 两项：五个注册型号的电压边界、极值、模拟、负包、包数限制、最后一包、非法堆叠；电池盒对不同电压的真实 provider 计数。

首轮 build/gametest_batch63_legacy_voltage.log 出现上述线缆边界测试接收端不匹配，已停止（拥有的 Java 进程确认退出），改为 MV+HV 后更换新世界重跑。

最终日志 build/gametest_batch63_legacy_voltage_green.log，新世界 build/gametest-batch63-legacy-voltage-green。06:43:53 报告 All 1018 required tests passed，06:44:20 正常关闭，Gradle 退出码 0，BUILD SUCCESSFUL in 9m 8s。新增两项和修正后的充电台/线缆测试全部包含在全套门禁内。运行中代码与资源冻结，没有构建 jar。

## 待完成与实机测试

在充电台使用旧 LV 电池时，应只能接收 16–64 EU/包；128/512 EU 应拒收。同档电池盒可使用旧电池，错档不能把它作为输出电源。正规化学电池和工具装配配方继续保留。不要把旧通用物品的历史容量解释成原版化学规格。

下一步继续核对电动工具实际挖掘/使用行为与材料磨损，而非只验证其配方和储能。大型坩埚客户端外观/Jade 仍待实机验证；完整占位清理目标未完成。

下一批已核实的起点：GTToolHelper.matchesTool/isTool 主要识别 GTToolItem，ElectricToolItem 不在该继承链，damageForToolClickReturn 也有 isTool 闸门。不要只扩识别而漏掉耗电。原版 GT_Tool_Drill_LV.isMiningTool 为 false，仅匹配 harvestTool=drill，不应套普通镐；GT_Tool_Chainsaw_LV 继承斧行为，另有剪叶、冰块掉落、速度及品质规则。电动扳手还有 Behavior_Switch_Metadata 模式切换，需与普通扳手/活动扳手区别核对。

没有执行 git commit。
