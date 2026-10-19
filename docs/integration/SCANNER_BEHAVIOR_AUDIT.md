# 便携扫描仪内容、费用与扩展行为核查

日期：2026-10-19；基线 `3e644ac22`。原版根目录 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java`，Gregorius Techneticies / GregTech-6 Team，LGPL 文件只读。

本批追踪原版 Behavior_Scanner → WD.scan → EnergyStat，与当前注册、ItemBehaviors、BehaviorScanner 和 ScannerItem。没有完成作物分析、全部附属模组兼容、所有充电提供者或游戏扫描验收。

## 先排除休眠的 Sensor Kit

原版 Behavior_SensorKit 的 Nuclear Control 卡片绑定主体位于注释中；全原版 Java 搜索只找到该类自身声明，没有活动注册引用。因此不把“移植版没有同名 Sensor Kit”直接报成缺少正常手持仪表，也不据此凭空补出新功能。

实际注册的 Debug Scanner、Portable Scanner 和 Portable Cropnalyzer 在 MultiItemRandomTools 中分别是 9001、9002、9003，均有作物行为，前两种另外有扫描行为。当前 GTMultiItems 三种 ID 均创建 ScannerItem，ItemBehaviors 先尝试作物分析，再按 scansBlocks 进入普通扫描。不能因为行为表是另一份清单就判定扫描仪没有注册行为。

## ALIGN-121：扫描内容没有接入现有物理／进度来源

原版 WD.scan 分别读取重量、温度、压缩体积和进度接口，合格时输出对应读数并收取各段费用。当前 BehaviorScanner.scan 只读取 MachineControl、能源和流体能力，没有独立的物理段，也不查询 ProgressSensorSource。

旧类注释把 ITileEntityGibbl 等全部解释成“移植版没有对应接口”，已不准确：FluidPipeBlockEntity 实现 CompressionSensorSource，已有 gibblValue／gibblMaximum；原生坩埚也有温度／熔毁上限 getter。独立传感器或 Jade 能读取某些数据，不代表扫描仪会自动读到。

本项记录实际扫描消费者遗漏，不把它扩大成这些设备本身没有压力／温度。其他原版重量和进度接口没有完整现代对应的部分，仍与 ALIGN-077、085、110–111 等状态边界缺口相关。具体数据支持、输出单位及每种宿主的诊断覆盖需继续对照。

## ALIGN-122：可选控制能力被当成全部状态能力，并产生额外段费用

原版按独立接口决定是否输出 State、Mode、Running，每个存在的段各收 512 EU。当前只要 MachineControl.find 非空，就无条件输出 State、Running 并分别加 512；只有模式和进度做可选判断。

明确例子是 USB 交换器：当前 control 显式 supportsSwitch=false、supportsProgress=false、supportsMode=true；enabled 返回 available，running／active 返回 false。扫描仪仍显示 State: ON、Running: Not Possible 并收这两个段的费用。原版 USB 交换器的父类 TileEntityBase08DataSwitch 只提供模式接口，不提供开关／运行接口。

这里确认的是两个额外段的资格与费用，不声称该方块完整扫描的总价只相差固定数额；其他能源、流体、调试段的存在也会影响总价。MachineControl 缺少 possible／success 还使扫描文本不能出现原版相应状态，沿用 ALIGN-110–111，不再重复编号。

## ALIGN-123：丢失逐诊断段的异常隔离

原版 WD.scan 对方块头部和各个原生诊断段分别 try/catch，某个接口／getter 抛错时记录异常，然后继续其他诊断段。当前 BehaviorScanner 中对方块属性、控制、能源、流体能力的读取没有相应隔离；ScannerItem → ItemBehaviors.useOn → BehaviorScanner.scan 这条本地路径也没有捕获恢复。

因此一个异常的能力提供者或 getter 可使这次扫描在收集完其他段之前退出，异常会沿调用栈传播。该结论来自源码，没有注入故障能力、没有复现客户端／服务器崩溃；也不据此推断所有外部模组都会抛异常。不能把一个整体 catch 后返回空结果当成已经恢复原版的逐段容错。

## ALIGN-124：缺少方块调试信息与可修改扫描事件

原版先向 IBlockDebugable 请求 getDebugInfo。原版自己的 MultiTileEntityBlock 实现此接口，并转发给具体实体的 IMTE_GetDebugInfo，所以这一层不仅是旧外部模组兼容。

然后原版发布可取消的 BlockScanningEvent：包含玩家、坐标、点击面、点击位置、等级、方块／实体和可修改的文本列表，mEUCost 也可改写；返回事件后的费用，只在事件未取消时追加结果。

当前 scan 收集固定段后直接 out.addAll(lines) 并返回 cost，没有提供者或事件调用，维护源码也没有相应的诊断扩展合同。移植需要设计共用的现代扩展边界及两版桥接，不能把“没有旧 Forge 事件总线”当作已经实现等价扩展的解释。这里确认能力缺项，未运行事件兼容测试。

## 已有行为和条件兼容

- 便携扫描等级 2，调试等级 Integer.MAX_VALUE；方块 Java 类从等级 10 显示，实体 Java 类要求大于 100。当前对应入口存在，实体点击路径未游戏验收。
- 便携扫描容量 4096000、分析器容量 1024000，以及各段 512 EU 常量对应注册。按 packet 范围充电、部分余量不足时扣空、创作／调试免费、先计费后聊天已有实现。没有因此声称所有充电路径或每个段费用都正确。
- 状态属性取代 1.7.10 metadata、使用注册 ID 和当前方块显示名，是现代状态表达，单列为适配。
- 原版 EnergyStat.useEnergy 前后调用 rechargeFromPlayer，但该函数受旧 COMPAT_EU_ITEM 提供者／tier 条件限制。当前扫描仪没有等价的装备补电步骤；现代提供者合同尚未映射，先记条件兼容，不报成无条件缺少所有盔甲充电。
- IC2 反应堆／能源和 Galacticraft 密封分支也属于条件兼容，不能要求现代依赖沿用旧类名。
- 原版扫描声、点击坐标转发、方块特殊抗爆值与实际能耗显示还需后续逐项核查。当前 ScannerItem 传入的点击坐标为 0，未来诊断事件桥接不能忽略其语义。
- 扫描诊断文本直接拼接英文的语言问题沿用 ALIGN-003，本批没有修改语言内容。

## 可读性整理与验证

将 ItemBehaviorCatalog 的 46 条归属记录逐行展开并按既有定义表工具对齐，ItemBehaviors.PORTED 的类选择 switch 分行，保留 ID、行为名、顺序及 empty_spray_can 的空行为记录；该表是入口清单，不证明全部 46 个物品已完整验收。

展开 ScannerEnergyRules 的能力方法；将 BehaviorScanner 的长说明改为当前真实支持边界，删除“没有接口所以无法读取”和不准确的等价归因。四份生产文件相对基线保持代码／字面量一致，两个原生文件分别投影 Forge／Neo，共六份比较通过。

未新增模拟测试、原生编译、完整测试、GameTest、游戏扫描、异常注入、保存重启、构建或推送。来源及边界见 [回执](verification/scanner-behavior-audit-20261019.json)。

后续继续作物分析、放大镜／工具专属交互、扫描事件与源数据适配，以及全面审计队列。
