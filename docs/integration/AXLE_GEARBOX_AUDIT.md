# 轴目录与可调齿轮箱状态核查

日期：2026-10-19；基线 `670aafb49`。本批接续旋转变速器核查，区分直通轴与安装齿轮的可调箱体，检查输送状态和输出调度。没有修改玩法。

## 目录与来源

原版 `gregtech/loaders/b/Loader_MultiTileEntities.java` 的 Axles and Gearboxes 注册 13 种材料、每种四种粗细。NBT_PIPESIZE 为转速上限，NBT_PIPEBANDWIDTH 为功率包数上限，NBT_DIAMETER 为 6／9／12／16 像素直径；无每格转速损耗。

当前 AxleCatalog → GTAxles → AxleBlock／AxleBlockEntity 路径存在，52 项目录的材料、尺寸、转速、功率与原版行逐项核对，core 观察见 [回执](verification/axle-gearbox-audit-20261019.json)。不要把规格中的 maxPower 与 RU 包大小 maxSpeed 混为一个“电压”。

原版轴逻辑位于 `gregapi/tileentity/connectors/MultiTileEntityAxle.java`，继承 ConnectorStraight；齿轮箱位于 `gregtech/tileentity/energy/transformers/MultiTileEntityGearBox.java`。原版根目录为 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java`。当前二者分别由共享 AxleBlockEntity 和 GearboxBlockEntity 实现。

## 已对应或不应误报的轴行为

原版轴在 mTimer<1 时不传输；静止时首个输入间隔用于启动，计入传输但不向下一根轴输出。当前 timer／previousRotationDir 对应此流程。后续只向输入面的反面传输，并沿轴链传递访问集合，最终进入能源工具入口。

原版按每次输入的绝对速度与本 tick 累计实际接收包数检查上限，超限 popOff 并返回原始提供量；当前 recordTransfer 对应这两个量并破坏掉落轴。不要仅凭原版“空转可无限转速”的注释作判断，其实际代码也无 used>0 的速度超限门槛。本轮未进行游戏内破坏、丢物或声音验证。

当前 signed speed 求和、累计包数和传输能量使用饱和计算，并拒绝 Long.MIN_VALUE 等异常输入；原版为普通 long 运算。这是边界保护差异，没有为了表面一致而撤销。直线连接六面掩码的 64 种组合中，空、六个单面和三对相对面共 10 种有效；core 已观察该结果。

当前块实体已在 setRemoved／onChunkUnloaded 解除 EnergyNet 登记，不能继续将历史“卸载不释放图节点”算作尚未修复。本轮没有复跑跨区块加载和回收测试。

## ALIGN-074：齿轮箱的余量输出不再要求当 tick 有齿轮输入

原版 onTick2 仅在 `mUsedGear && mCurrentPower > 0` 时尝试输出，tick 末将 mUsedGear=false。下次 doInject 的第一个有效齿轮输入会设置 mUsedGear；若仍有旧缓存，则增加 mIgnorePower 并拒绝新能量，但这次触发仍允许随后尝试输出旧量。这个状态既防止旧量被新速度覆盖，也使余量输送依赖持续的输入触发。

当前 serverTick 只要求未 jam、齿轮布局可用、currentPower／currentSpeed>0，随后每 tick 都尝试输出旧缓存。即使供能设备断开且没有新的 doEnergyInjection，也会继续尝试。

源码场景：收到动力但输出暂时堵塞，随后停止供能并让目标恢复接收。原版在没有新输入触发时保留旧量；当前继续输出。此为控制流推导，不是本轮世界实测。输入面集合也会在 tick 末清空，缺少新输入时的可输出面变化须随实际网络一起验证。

相关回压差异：原版模拟注入在 mIgnorePower!=0 时返回零；当前通过基本校验后直接返回 amount，执行时才按 inputTick／旧缓存拒收。模拟结果和实际接收可能不同。不得把“缓存没有直接丢失”当成整个回压合同一致。

## ALIGN-075：输出轮次与分配策略改变

原版输出循环在有接收进展且仍有余量时继续；每轮固定 `max(1,currentPower/3)` 为各有效面的提供量，每轮推进 mOrder。

当前最多运行三轮，按剩余输出面数计算份额 `max(1,currentPower/remainingOutputs--)`，只在 tick 末推进一次 outputOrder。原版按几何上限三面划分，当前按实际可输出面划分，调用次数、每次包数及优先顺序均不同。

条件示例：箱内十包、一处有效输出，接收器每次调用只收一包，但同 tick 可接受更多。原版有进展便继续，能在本次输出中送完十包；当前三轮最多送三包，剩七包保留。该例明确假设接收器行为，为源码推导，未用它冒充某一原生设备已经复现。

当前不丢余量、且有限轮次可限制单 tick 工作量，这些保护／性能取舍应保留在修复决策中。本轮不直接改回无界循环。

## ALIGN-076：可调齿轮箱没有接入通用开关／运行状态接口

原版 GearBox 实现 ITileEntitySwitchableOnOff／ITileEntityRunningActively，开关直接控制 mJammed，运行状态来自齿轮活动位。

当前 GearboxBlockEntity 只实现 IEnergyBlock，GearboxBlock 仅实现 EntityBlock；MachineControl.find 只认 MachineControl.Provider，因此返回 null。手动软锤仍调用 toggleJammed，可以停启，这部分已存在，不能误报成完全没有开关。缺口是供其它控制器查询／控制的通用接口；覆盖板能否安装及各类外部控制器实际交互仍需分别核对，不能据此宣称所有控制方式都已失败。

## 已有的齿轮箱机制与剩余边界

六面齿轮和可选 X／Y／Z 轴、同 tick 冲突方向 jam、同向多源取最低转速、无齿轮轴直通、错误布局吞掉输入、超速前十 tick 宽限、齿轮废料／剩余齿轮掉落及拆卸保存安装配置，都有对应当前路径。具体视觉和资源回收尚不能只靠这些分支视为完整验证。

原版冲突／超速有破裂声和运行音；当前所读 jam／breakGears 分支未发出同样声音。齿轮箱损坏反馈、装入齿轮的材料回收、覆盖板和客户端细节继续核查。本轮不重复登记此前 ALIGN-067–069 的固定倍率变速器问题，它与此处可调齿轮箱不是同一控制器。

## 可读性与验证

AxleCatalog 的私有 Tier 字段 mat／speed／powers 改为 material／maxSpeed／powerLimits，局部 t／s 改为 family／sizeTier，公开 AxleSpec 与注册 ID 不变。AxleConnectionRules 展开；AxleBlockEntity 的传输／累计／溢出分支展开，执行代码不变。

核对私有重命名后的代码／字面量等价，原生轴实体两版显式展开后等价。Java 17 局部编译目录／连接规则与观察入口，记录 52 项目录和连接布局计数。没有原生编译、Gradle、游戏、实际接收器吞吐／冲突测试、重启、构建或推送。新差异是源码链结论；全面审计继续。
