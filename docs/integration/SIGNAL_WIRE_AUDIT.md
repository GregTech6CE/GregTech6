# 红石导线与魔法场吸收器核查

2026-10-22，第一百二十批，基线 `88b5c66f3`。上一 goal 回合复核 #78 原版避难所的成组 Sky Stone，取得直接源码证据，属于进展。本批新增 ALIGN-414、415，见[回执](verification/signal-wire-audit-20261022.json)。九份原版、16份当前生产 Java、32段源码引用及两份实际原生 Direction 工件留痕。引用只支持列明合同，不代表这些文件或整个能源系统已经完成验收。原树只读，GregTech-6 Team／Gregorius Techneticies 的 LGPL 来源声明保留。

## 魔法场吸收器确实缺失

原版 `Loader_MultiTileEntities:1005` 正式注册10180，名称 Magic Field Absorber，材料钯，物品上限16，硬度／抗性4／4。声明了 `GOG / LBL / CMC` 配方：钯机器外壳、魔法电路、金细导线、黑曜石板、青金石宝石及原版信标。不是调试空壳。

原 `MultiTileEntityMagicFieldAbsorber.onTick2` 读取上方方块：龙蛋输出64 QU，原版头颅输出1 TU；暮色奖杯为可选扩展，按元数据输出64 KU／QU／HU／LU／CU。KU每64 tick换向，按能源的包尺寸政策选择发包形式。停机、活动状态、重新检查和输出面各有实际消费者；默认向下，可向下或水平，不能向上。它包含原版 Minecraft 可用来源，不能因为暮色奖杯可选，就把整个设备列为纯可选兼容。

当前两端入口调用 GTEnergyNodes，实际目录来自 EnergyNodeDefinitions／MagnetMachineDefinitions，原生 EnergyNodeBlockEntity 分派电池、太阳能、既有转换器及普通输出。沿目录、注册和执行入口未发现此设备或上方龙蛋／头颅发能流程。另对明确维护根的1827份 Java 做名称／旧编号筛查，只命中 GTBooksGen 的书籍文字；说明文字不会注册一个可运行设备。因此新增414：独立方块、制作和上述实际发能功能尚未移植。本批未新增一个仅有名字的替代方块。

## 红石导线已有功能和输入面差异

原三材料各有裸线／绝缘线，共六项：红色合金27000／27006，Signalum27050／27056，Lumium27500／27506。原最大量值为 Integer.MAX_VALUE，一个原版红石等级等于该量值；范围16或64对应整数除法损耗。当前 SignalWireCatalog 与 GTSignalWires 有相同三族、六个注册分支及损耗参数。裸线直径2/16、绝缘线4/16，当前半厚度1或2对应；Lumium裸线随强度发光，绝缘线不发光，也与原类分工相符。这些对应不等于全部模型／配方／工具已核完。

原版从其它红石导线读取其 `getRedstoneMinusLoss`，即扣发送方损耗；当前传播队列也扣 wave.wire 的损耗，不误报成扣错一端。合法非负强度的比较器向下取整、普通输出向上取整，以及向红石粉／导体输出减一的数值合同对应。本批没有运行整个原生网络。

发现415：**两个外部输入同强度时，输入面的保留规则不同。** 原 updateRedstone 先读上一次 mReceived，只有严格更大才换到另一面。当前每次重建 inputSides，从 Direction.values 顺序读取，同样只在严格更大时更新；两端实际 Direction 均为 DOWN、UP、NORTH、SOUTH、WEST、EAST。

有限源码见证：模式0、范围16，上次输入面EAST，WEST与EAST都给出 `15 × UNIT − floor(UNIT/16)` 的正候选值。原版保留EAST，当前先选WEST并在平值时保持WEST。总强度相同，但“不给输入面输出”的谓词会作用到不同面。这个见证没有执行 Minecraft 电路，不宣称已经观察到闪烁、环路错误或玩家具体产线故障。现有防环路自锁的重建策略保留，不能借恢复旧输入面顺便撤销它。

原信号保存还存在“数值写入、byte读取”的来源问题；当前重载时清瞬态强度并重算，原模式0可能暂为负损耗而当前夹到0。这些差别不直接当作可见故障，也不照搬原读取截断。完整保存、真实重启、队列平值传播和跨区块生命周期继续核查。

## 源码整理与边界

整理 SignalWireCatalog、GTSignalWires、SignalWireBlock、SignalWireBlockEntity：注册族／参数、形状边界和实体上下文、待处理队列／输入面／变化列表分别展开和命名。42处普通标识符出现受控归一，手持工具的Player与ticker的BlockPos另作限定作用域改名，避免把两种 `p` 混为一类。成员名守卫避免误改 Direction.values。四源446→670行。

将现有损耗、比较器与原版输出的三个数值表达式抽到23行的 core SignalWireRules；世界采样、连接、覆盖板、加载判断、队列和通知仍由原生实体负责。两平台通过同一实体正文消费它。没有增加另一份业务目录，也没有改ID、状态字段、版本分支或网络求解策略。注释由“每tick只求解一次”改成准确的“成员缓存已解tick，模式变更使该成员失效”。

四源七个完整平台代码视图，在列明的局部更名及三段精确抽取归一后，剩余有序代码、字面量位置、其它注释和Stonecutter指令保持。新helper完整正文另核。不是把整个修改称为原始字节相等，也不是用行数增加证明整个架构完成。

使用真实 GtSignalCarrier 与新 SignalWireRules，经 Java17 编译和短程序执行；8238个合法强度样本覆盖等级边界及固定种子随机量，独立 BigInteger 除法／余数核比较器、两种输出和16／64范围，共24716项，约0.505秒。没有伪造 Minecraft 类，但同样没有执行原生求解器。双端 Direction 分别来自实际Forge字节码／Neo源码，读取摘要和枚举顺序，不是重新构建或启动两平台。

本批未跑Gradle、整模组编译、GameTest、游戏、原版1.7.10、真实存档重启、打包、推送或GitHub写操作；上一批的双版编译不冒充本批新代码的整平台编译证据。用户WoodMaterials.java和CompoundMaterials.java摘要保持。414／415为累计差异编号，不是未修数量或完成率。剩余真实消费者、资源／配方、物理／生命周期以及全系统和源码标准化继续，goal保持active。
