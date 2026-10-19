# 基础机器中断与供能核查（2026-10-19）

基线 `d011da00c`，继续上一批共用加工执行审计。原版来源根目录为 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java`，当前原生调用链在 `multiversion/src/main/java/com/gregtech/gregtech`。本批只整理代码与记录差异，不改变玩法。

## ALIGN-044：软锤取消当前加工未接入

原版 `gregapi/tileentity/machines/MultiTileEntityBasicMachine.onToolClick2:366–372` 在软锤分支清零进度、最低能量、总工作量、输出能量、启动能量，并丢弃当前尚未交付的物品／流体产物，更新库存，返回 10000 工具消耗。已经扣掉的输入不会返还；它不是无损暂停。

当前 `block/machine/BasicMachineBlock:412–421` 的软锤用途是调用 `configureFilterCover` 清除覆盖板过滤器；`PanelCoverInteraction` 和父类旋转处理没有对应加工取消。`BasicMachineBlockEntity` 的当前进度、成本和 `pendingOutputs` 是私有状态，赋值路径只包含正常加工／完成／读档等，没有软锤取消入口。独立坩埚的软锤逻辑不能证明基础机器已经支持。这里限定基础配方机，不扩展到全体设备的所有软锤用途。

## ALIGN-045：关停／失效结构绕过恒定供能进度重置

原版 `MultiTileEntityBasicMachine.doWork:779–792` 在能量不足或结构不完整时进入 inactive 分支（本方块计时超过 40 tick 后）；`doInactive:889–900` 根据全局 `CONSTANT_ENERGY` 与 `mNoConstantEnergy` 清零进度，但没有清空已扣料对应的待输出产品。原版默认 `CONSTANT_ENERGY=true`，设备豁免必须单独考虑。关停拒绝后续注入，仍会消耗残余缓存；不是按下开关就立刻清零所有状态。

当前 `BasicMachineBlockEntity.tickServer:239–248` 在结构失效时提前返回，关闭时清空能量后提前返回，都不经过第 287 行的进度重置。`MachineControl.setEnabled:140` 也只清能量／运行状态。只有正常进入“能量不足”分支时才按 `requiresConstantEnergy()` 清进度。

例如大型离心机的非零进行中进度：其 `LargeRecipeMachineBlockEntity.requiresConstantEnergy:118` 使用 spec 标志，`LargeMachineProcessingRules.constantEnergy(false,"largecentrifuge")` 实际返回 true。结构失效时当前进度保持，原版在上述计时和默认配置条件下清零。大型破碎机的豁免返回 false，不能把同一结论套过去；普通离心机／浸洗机的豁免值也分别为 false／true，已作 core 观察。

另一个时序差异是当前普通缺电分支没有原版前 40 tick 的 inactive 宽限。是否影响刚加载的有进度机器，要经存档重启验证；这里只确认控制流差异。当前关闭时仍可能执行覆盖板 tick，具体覆盖板是否同 tick 重开需按类型另核，不将“全部关闭场景”一概而论。

## ALIGN-046：KU 完成加工缺少正转负的条件

原版 `TD.Energy.ALL_ALTERNATING`（TD:219）仅列 KU。`BasicMachine.doInject:489–508` 保存注入大小的符号到 `mStateNew`；`doActive:815` 除要求工作量达到成本，还要求旧状态正、新状态负，最后第 865 行更新旧状态。正／负状态在 NBT 中保存。此限制不能误套到 RU、EU 或所有能源。

当前基础机器 `doInject:1231–1246` 用大小的绝对值累加能量，没有记录符号；`doActive:350` 达到成本便进入产物交付。通用 `EnergyTransfer:48` → `GTEnergyBlockEntity` → `EnergyBlockDefaults:25` → `EnergyGate` 保留传入符号，缺口在基础机器消费处。

当前 `PistonEngineState.tick` 本来就会返回正负 KU，原生 `PistonEngineBlockEntity:122` 把这个带符号包交给 EnergyTransfer。独立 core 观察指定活塞状态分别得到 +32 和 -32，BasicMachineEnergy 对二者均累加 32。绝对值用于计量本身没有错，遗漏的是额外的完成条件／状态。

来源注册行 1300–1303、1312–1315、1324–1327、1343–1346、1425–1428 对应 Crusher、Sifter、Squeezer、Compressor、Press 各四档。当前 BasicMachineOriginalParams 也有这五组共 20 个 KU 参数项。这里是参数目录与共用执行路径核查，没有对 20 个方块逐一启动游戏测试；普通 RU 大型破碎机不属于这个 KU 结论。

## ALIGN-047：整包接收后过量部分被截掉

原版 `BasicMachine.doInject:503–505` 将本次可用空间换算成向上取整的整包数量，实际增加完整的 `数量×包大小`，允许缓存短暂超过 mInputMax。`doWork:791` 再每 tick 减去 mInputMax。

当前 `BasicMachineEnergy.accepted` 同样向上取整，但 `add` 在 packets >= demand 时直接返回 capacity，越过容量的部分不保留；原生注入和主动抽能都用这个 helper。它的旧注释误称原版缓存也会截断，本批纠正该说明，代码行为保持。

明确的条件例子：输入最大 64、最小 16，先送 3 个 20 单位包，缓存为 60；再送一个 32 单位包，两者都报告接收 1 包。当前 core 实际结果为 64，原版源码算术为 92；当 tick 使用并扣除最大 64 后，当前剩 0，原版仍剩 28。两个包大小均不低于 16，也不超过 64，因此不是最低包大小吞包／超载分支造成的差异。这个组合说明合法接口调用下的计量差异，尚未运行原生供能网络。

这与 ALIGN-040 不同：040 是任务完成时的工作进度余量；047 是任务之外的接收缓存余量。安全溢出处理仍有必要，不能直接用无界乘法替换当前实现。

## 整理、证据与后续

`OriginalBasicMachineRules` 的机器族列表、各类标志、面／能源语言键、IO 行计算已分行展开，保留原有 247 项识别范围、顺序、公开接口、代码和字面量。`BasicMachineEnergy` 仅改错注释。两文件与基线归一化代码／字面量比较一致，Java 17 独立编译通过。

实际 core 观察由 `tools/integration/audit-probes/BasicMachineInterruptAudit.java` 提供：247 个处理范围项、20 个 KU 项、四个恒定供能样本、活塞正负包和接收边界。原版结果、软锤、关停、结构与完成门槛是源码推导，不是运行游戏后的断言。来源哈希、结果和未执行项见 [回执](verification/basic-machine-interruption-audit-20261019.json)。没有运行 Gradle、整套测试、Minecraft、GameTest、存档重启、打包或推送。

尚未完成：链式并行的 `总工作量×批数 <= mInputMax×600` 限制、配方特殊启动能量与负功率输出、动态 Bath／Unboxinator 空输出要求、手工具其余交互、其他控制器与能源／物流系统。并行限制已找到原版 canOutput:626 的分支和当前缺少该过滤的入口，实际配方与库存条件仍需下批跟进，未在本批把条件推测当作已完成验证。
