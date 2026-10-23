# 流体管道目录、危险与仪表接口核查

日期：2026-10-19，基线 `bc98c1477`。原版根目录：`C:/Dev/gregtech6-master/gregtech6-master/src/main/java`。本轮读取 `gregtech/loaders/b/Loader_MultiTileEntities.java:1846–1885`、`gregapi/tileentity/connectors/MultiTileEntityPipeFluid.java` 及温度计／压力计源码；保留原作者与 LGPL 来源，不修改来源文件。

## 目录中已对应的参数

当前 40 个材料家族各七种规格，共 280 项。逐组读取原版注册调用，对照材料符号、基础容量、气密／耐酸／耐等离子／耐魔法四个独立标志、燃烧性，以及显式温度上限。七种规格的单通道容量倍率为 1／2／6／12／24／6／2，通道数为 1／1／1／1／1／4／9，直径为 4／6／8／12／16／16／16 像素，当前对应。

木、处理木、塑料、橡胶的显式温限分别为 340／340／370／350 K；其它家族的默认公式仍是材料熔点乘 1.25。这里验证的是公式及显式值，没有独立重建原版全部材料的最终熔点，不能扩大为所有默认温限数值已完全一致。材料对应通过当前旧符号门面解引用。

Forge 的 FluidPipeDefinitions → GTFluidPipes.register 和 Neo 的 FluidTransportRegistries.register 都读取同一 core 目录，并构造共享 FluidPipeBlock／FluidPipeBlockEntity。普通储罐目录同处 FluidTransportDefinitions，本轮整理它的局部命名和格式，但未宣称储罐玩法已完成对齐。

## 已存在的运行逻辑

- FluidPipeChannels.select 优先选择同种流体的已有通道，再选择空通道；共享原生代码在两版分别比较 tags／components，不以面号或邻居槽号充当通道身份。
- 均衡目标包含源管和可接受的机器，机器自身库存量不加入平均数；先分配管道，再分配机器，源剩余超过半容量时再向管道做一次压力分配。随机插入邻居列表的顺序与原版策略对应。当前平均值先除后加以防 long 溢出，是保护性实现。
- 普通外部侧面包装检查连接、流体覆盖板和部件权限；成功执行填充后记入该通道的接收面。不能把物品管道 ALIGN-082 的缺口套到流体管道。
- 魔法、气体、等离子、酸性检查独立进行；原版对应损耗为魔法液体 4／气体 16，加上物理泄漏气体 8、等离子 64、酸 16。当前接收危险流体后处理损失，不在 FluidTankGT.fill 中把它一律拒绝。
- 第一个非空通道覆盖旧温度，随后取较高温度；只在全空时每 tick 向环境温度移动 1 K。逐通道安全检查后分配、清除接收面掩码；过热和腐蚀随机销毁、泄漏伤害已有实际代码。
- 原版优先填充炼药锅，334／667／1000 mB 分档；当前也先处理炼药锅，且处理成功扣除量不进入 transferredAmount，与原版该分支相同。
- 原版拆管尝试向相邻连接目标排出，再丢弃残余；当前 dumpFluidsToAdjacent 有对应路径。完整拆除交互及所有外部能力异常行为未在本轮运行验证。

上述为源码控制流核查，并辅以现有 core 合同；没有用它们冒充真实多通道网络或伤害场景通过。

## ALIGN-083：材料接触伤害开关未进入管道规格

原版 addFluidPipes 的 aContactDamage 独立于四种防护标志，写入 NBT_CONTACTDAMAGE。原版碰撞要求 `mContactDamage && !mFoamDried`。

16 组原版材料关闭接触伤害：Ironwood、Aluminium、Invar、Efrine、Netherite、GaiaSpirit、Draconium、DraconiumAwakened、Infinity、Adamantium、BedrockHSLAAlloy、Thaumium、Manasteel、VoidMetal、Terrasteel、Carbon，共 112 种规格。

当前 FluidPipeMat 与 PipeSpec 都没有 contactDamage；FluidPipeBlock.entityInside 对所有管道调用 applyContactTemperatureDamage，仅先排除客户端和旁观者。因而这些原版本应免接触伤害的管道，在温度及实体条件满足伤害函数时也会伤害。不是说常温触碰一定受伤，也不是说所有防护服失效。旁观者豁免符合此前要求，本轮不撤销。

## ALIGN-084：管道方块硬度与橡胶的非阻挡属性丢失

原版 aBlocking 为真时硬度 2、抗爆 6，为假时硬度 1、抗爆 2；本组只有橡胶 aBlocking=false，且原版 isObstructingBlockAt 返回 mBlocking。

当前 PipeSpec 未携带 blocking，两平台注册均调用 FluidPipeBlock.defaultProperties，固定 `.strength(1.5F, 6.0F)`。因此普通家族硬度 2→1.5，橡胶硬度 1→1.5、抗爆 2→6。PipeSpec.blastResistance 的材料计算方法并没有用于这里的方块构造；物品工具提示里另一个 MachineSpec 也不能当作方块属性的证据。

原版快速阻挡查询与现代碰撞／遮蔽系统不是同一 API，本轮只确认元数据未传递及硬度／抗爆差异，不擅自把所有现代橡胶碰撞情况归结为同一个结果。

## ALIGN-085：流体管道温度接口未接温度计

原版 MultiTileEntityPipeFluid 提供 ITileEntityTemperature 和 ITileEntityGibbl：温度值／最大温度、各通道总量／总容量。MultiTileEntityThermometer 与 MultiTileEntityGibblometer 按接口读取它们。

当前管道虽有 getTemperature 和 PipeSpec.maxTemperature，SensorBlockEntity 的 THERMOMETER 分支却只识别坩埚。因此温度测量接口没有贯通；直接调试输出能显示温度不代表温度计能读。

压力计路径已经存在：FluidPipeBlockEntity 实现 CompressionSensorSource，gibblValue 汇总 long 通道量，gibblMaximum 返回单通道容量乘通道数；GIBBLOMETER／KILOGIBBLOMETER 按该接口读取。不能因为温度计未接入就把压力计也报成缺失。

当前 FLUID、BUCKETOMETER 等分支仍能通过 IFluidHandler 读量，不能说管道所有仪表都失效。但该接口返回 int，极大 long 容量的显示精度边界另需核查；压力接口自身保留 long。原版管道还有 progress=mTransferredAmount、max=mCapacity*tankCount；当前没有 ProgressSensorSource／MachineControl.Provider，作为 ALIGN-077 的流体管道扩展记录。

2026-10-19 第三十七批补充：CoverProgress 会为具备库存／流体能力的宿主提供覆盖板进度回退，读取当前内容物占比；因此本处缺少的是原版吞吐／发酵测量合同，不能扩大为没有任何进度覆盖板读数。见 [显示计量补查](DISPLAY_METER_AUDIT.md)。

## 待继续追踪的时序与兼容边界

- 原版按 WD.even 坐标奇偶将流体管道分进 SERVER_TICK_PRE／SERVER_TICK_PR2 两阶段，当前通过普通方块实体 ticker 执行。均衡算法对应不能证明同一 tick 的整网结果和顺序对应；尚未运行线形、环形和区块重载场景。
- 原版外部 fill 在实际请求后即设置接收位，即使返回零；当前只有 accepted>0 时设置。内部管道候选预标记则已经对应。满罐后的执行顺序影响尚待专门场景。
- 当前保存 temperature，所读原版 writeToNBT2 未保存运行时 mTemperature。原版持久化的是额定 mMaxTemperature 来源字段；重载空热管的差异需要核对完整父类和世界场景。
- 原版炼药锅允许 BlockCauldron 子类，现代当前检查两个原版方块身份。模组炼药锅、流体动态温度、魔法污染集成和泡沫包覆仍未完整验收；当前无污染模组时销毁为空气的路径不能当作模组集成已存在。

## 标准化与验证

展开 core FluidTransportDefinitions 的规格／桶目录方法，mat／w／d 改为 family／wood／drum；Forge 注册的 s 改为 spec；展开 Neo 注册和能力绑定，删除“原版完整实现”的过度归因。保持静态字段初始化、目录遍历、注册次序、Item 构造器、数值和保存键不变。

Java 17 定向编译四个 core 生产类、当前 CoreBehaviorContracts 与 FluidPipeAudit，仅执行 fluidPipeCatalog、fluidPipeChannels、fluidPipeSafety 三个既有方法，分别通过 1710／21／27 个断言。目录回执独立从原版 40 条注册行读取参数，对照 280 个实际 core 输出；三份修改的生产源码在局部命名归一后代码及字面量等价。

没有原生编译、游戏、随机伤害实测、存档重启、完整构建或推送。其它 core/API/test 依赖使用已有编译输出。报告与来源哈希见 [回执](verification/fluid-pipe-audit-20261019.json)；全面核查仍未结束。
