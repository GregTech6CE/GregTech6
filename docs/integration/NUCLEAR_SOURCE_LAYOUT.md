# 核反应堆源码职责与核心流程展开

第五十八批，2026-10-21，基线`67dff604d`。本批只改ReactorCoreBlockEntity的空格、换行和方法分隔，没有修复核反应堆玩法，不新增ALIGN编号。原版核对分为棒物理／目录、核心冷却／控制／保存、中子网络三组继续，不能以此导航代替行为验收。

## 定义和实际状态分别在哪

以下路径相对于对应Java根：core为`core/src/main/java/com/gregtech/gregtech/`，共享原生为`multiversion/src/main/java/com/gregtech/gregtech/`。

| 输入层 | 文件 | 当前职责 |
| --- | --- | --- |
| core | `content/nuclear/ReactorRodCatalog.java` | 棒目录、种类、寿命、自中子、发射、分母、最大通量及产物ID |
| core | `content/nuclear/ReactorCoolantRules.java` | 冷却剂身份键、换热单位／膨胀／热量除数与棒参数修饰 |
| core | `content/nuclear/RodPhysicsMath.java` | 数量限制、向上取整、热量与寿命损耗算术 |
| 共享原生 | `registry/GTFuelRods.java`、`item/FuelRodItem.java` | 注册棒物品，目录与实际物品的连接 |
| 共享原生 | `content/nuclear/RodPhysics.java` | ItemStack身份、寿命／慢化数据、单棒收发与反应，适配NBT／组件 |
| 共享原生 | `content/nuclear/ReactorNetwork.java` | 世界tick队列、两阶段中子交换、参与核心集合及邻棒映射 |
| 共享原生 | `content/nuclear/ReactorCoolants.java` | 将core冷却剂键解析到实际原生Fluid |
| 共享原生 | `blockentity/energy/ReactorCoreBlockEntity.java` | rods／neutrons、两罐、运行／冷却／推液、能力、存档／网络状态 |
| 共享原生 | `blockentity/energy/ReactorCore2x2BlockEntity.java` | 传入四棒槽；2×2指单个核心内四棒，不是四个外壳结构 |
| 共享原生 | `content/nuclear/ReactorPorts.java`、`ReactorInteraction.java` | 两个流体出口方向、工具与玩家交互 |
| 共享原生 | `content/nuclear/ReactorHazards.java`、`ReactorRadiation.java` | 方块接触及实体辐射处理；是否完整对应原版仍单独审计 |

这些是当前职责入口，不表示表值、执行周期、所有外部提供者或每种冷却剂已经验收。核心仍承载多个原生职责，进一步拆分需要先明确实际状态／保存合同，不能为降低行数直接移动字段。

## 这次具体展开了什么

核心维护源236→560行，最长代码／文本行305→152字符。展开棒插入／取出、开关／禁用槽、react、coolHeat、故障／辐射、推液、匿名物品／流体接口和两版保存／恢复／更新包；长参数分行，注释和原方法顺序保留。

Forge与Neo并非两份完整核心业务；不同的能力、NBT／HolderLookup保存和同步方法通过现有条件选择。本次连注释包裹内的Neo代码一起展开，保留跨条件配对的声明和方法结束位置。没有改变花括号、增加作用域、重排步骤、改名称／导入或偷偷修正正在核查的容量、侧面、时序问题。

当前阅读顺序可从`serverTick → ReactorNetwork.enqueue`开始，再看网络end／exchange调用`react`；核心内`react`依次调用冷却、推液与条件辐射。`fluids`是自动化接口，`handFluids`有额外回收输入冷却剂的路径；它们只是当前行为说明，不能据此推断原版准入完全相同。

## 保持证明与剩余工作

[回执](verification/reactor-core-readability-20261021.json)保留Git基线、最初工作树换行身份、中间排版与最终源hash。两端分别展开后：Forge3703项、Neo3657项完整执行词法相同，各31个字面量的位置、5条注释和原30条条件指令保持。包括匿名类、lambda和版本保存方法的控制范围，没有只比较去空白字符串。

辅助预览脚本只在忽略的work目录中使用，不接入构建或启动，不新增生产格式化依赖。生产仅写入通过执行保持比较的预览；未运行生成器、Gradle、Java编译／初始化、新测试、游戏、存档恢复、构建或推送，用户材料表未动。

棒物理／参数、网络相位、冷却真实容量消费、侧面自动化与故障次序等原版证据还在各自专题核对，结论归档后更新主账本；本批不提前给候选编号或宣称核反应堆已经对齐。其它压行、过大职责、方法级共享以及全面目标继续。

## 第五十九批专题归档

随后完成的[棒目录／物理](NUCLEAR_ROD_AUDIT.md)、[核心](NUCLEAR_CORE_AUDIT.md)、[网络](NUCLEAR_NETWORK_AUDIT.md)登记ALIGN-202–210，扩展095容量实例。46目录、11冷却表、常规公式和直连布局已有；phase18及模具消耗候选排除。58批排版保留原行为，不能用本导航的历史“仍在核对”段落覆盖59批已有证据，也不能把59批局部静态核查当全反应堆验收。

## 第六十批纯领域辅助类展开

2026-10-21，以b11081f4f为基线，RodPhysicsMath由3→29行，ReactorCoolantRules由22→68行，最长行分别646→101与244→86。向上取整使用numerator／denominator，限制使用value，棒参数使用rod，冷却构造器使用inputKey／outputKey、heatPerUnit、outputExpansion、heatDivisor和moderating；公开类型／方法／字段名未变。

两份文件的全部206／522项执行词法在七个方法／构造器参数绑定各自归一后相同，字符串位置、枚举顺序、数值、运算和导入也保持。不能用全局替换掩盖字段或其他作用域变化，具体映射和引用次数见[回执](verification/nuclear-helper-readability-20261021.json)。

RodPhysicsMath的旧“Exact original”说明已更正：当前long运算／转int饱和与原版int溢出极值不同，不能由类注释担保完全等价。ReactorCoolantRules只保留换热数据与棒修饰职责说明；实际Fluid查找位于ReactorCoolants。本次未修核玩法，不新增ALIGN编号；启停、重载、异常分派与配置等专题继续。
