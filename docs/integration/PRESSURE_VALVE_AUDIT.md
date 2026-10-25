# 压力阀连接与流体模板入口核查

日期：2026-10-19；基线 `e25e2fb04`。原版来源 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java`，GregTech-6 Team / Gregorius Techneticies，LGPL 源码保持只读。

## ALIGN-106：压力阀缺少原版断开与阻止连接

原版 `CoverPressureValve.onTickPost` 在服务端、未停止、实体年龄 aTimer > 2、流体管道宿主的条件下，每次先执行 `disconnect(aSide, true)`，再判断第一罐是否满。即使罐不满或没有流体，也先断开该面。另有 `interceptConnect`：前方是流体管道时返回 true，含义是拒绝连接。

当前 `FluidPipeBlockEntity.tickCovers` 直接将 tanks[0] 与 coverTicks 交给 tickPressureValve；调用者没有断开连接，helper 在空罐时直接返回，也不修改连接位。CoverConnections.blocks／attached 处理挡板、红石覆盖板和物品过滤器，但没有压力阀分支。两版扳手入口都调用 CoverConnections.canConnect，因而缺口不只是一处没有引用的工具函数。

后果是已连接的阀面不会按原版变成阀门专用出口；正常管道分配和外部侧面能力仍可能沿这一面的连接位运行。先在空面装阀、之后放置相邻管道并操作扳手的路径，也没有原版压力阀 interceptConnect 的限制。这里是源码路径结论，未搭建游戏场景或测量实际吞吐。

旧方法 `valveConnectsThrough(boolean)` 返回 neighbourIsFluidPipe，实际表示拦截；生产代码只有定义，引用来自旧 GameTest。已改名为 `valveBlocksPipeConnection`，同步两份测试中的调用／说明，并纠正测试把 true 解释成“允许连接”的断言消息。真值表、断言条件没有改变，仍未接入实际连接逻辑。本轮未运行旧 GameTest，不把一个纯函数断言当作管网验收。

## 已有实现，不列为缺失

- PanelCoverRuntime 限制为流体管道；FluidPipeBlockEntity.attachCover 进一步检查单通道且前方不是流体管道。四通道／九通道安装限制已经接入。
- tickCovers 在调度前检查整个覆盖板系统的停止状态，虽然 tickPressureValve 本身没有 stopped 参数。不能只读 helper 判为停止失效。
- 满罐才动作，前方有流体能力时尝试填充；没有前方容器时，只有气体且前方无碰撞形状才向空气排放。液体不直接排到空气；前方容器存在但拒收时也不继续排气，分支次序对应原版。
- 排气清空罐、播放熄灭声，并遍历以宿主方块为中心扩展两格的 5×5×5 包围盒。原版 `box(-2,-2,-2,+3,+3,+3)` 与当前 AABB(blockPos).inflate(2) 范围对应。
- 阀门调用温度伤害 multiplier=2、cap=10；热阈值 320 K、冷阈值 260 K 和封顶公式已对照 UT.Entities。实体免疫、防护装备、伤害类型、音量及所有第三方流体温度不在本轮完整验收范围。
- `moveInto` 按对方接受量扣除源罐，并检查返回数量范围。未因此声称原版 FL.move 所有特殊容器、超大数量或执行失败回滚均等价。

## ALIGN-100 补充：压力阀的启动计数

原版的 >2 使用实体年龄 aTimer。当前 coverTicks 只在管道带覆盖板时的 tickCovers 中增加，且没有证明与实体生命周期一致。例如一根已存在很久、此前没有任何覆盖板的管道，第一次装阀仍要经历自己的覆盖板前三次调度。它也不是“每次装阀固定重新计时”，因为当前删除覆盖板没有在此路径重置计数。沿用 ALIGN-100 的时钟差异，不单独重复编号；实际存档重载未验。

## 流体模板：完整事件链比单个方块方法更宽

原版 CoverFilterFluid 先 FL.getFluid，失败后查询物品材料关联；前缀为容器且非板条箱时，取对应材料的 fluid(U, true)。

当前 BasicMachineBlockEntity／FluidPipeBlockEntity 的 clickFilterCover 确实只查物品流体能力，但不能据此断言普通瓶子或桶无法设置：ComponentCoverEvents.use 在最高事件优先级调用 UtilityCoverInteraction.use，成功处理后取消事件。后者使用 sample，依次尝试：

1. 物品流体能力，模拟抽取。
2. GT BottleItem 声明的流体。
3. 原版 BucketItem 内置流体。
4. gt6ce 名称空间下 bottle_／capsule_／cell_／bucket_ 的同名流体注册项。

原版“材料关联＋容器前缀”的通用回退，没有以相同方式出现在 sample 中。当前名称规则和物品能力可能补偿部分现存物品；本轮没有建立确切的已注册失败样本，因此只记录待核差异，不追加一个“所有材料容器不可用”的缺陷。外部附加物品的兼容范围也尚未验证。

已展开 sample 的各级回退、CoverConnections 的连接位操作，并纠正压力阀注释中“实际宿主仍是基础机器／没有连接状态可断开”的过时解释。历史注册 ID `pressure_value` 保持不变，本批只重命名含义相反的 Java 方法。

## 2026-10-25 / ALIGN-106 针对性修复

在 `849c89202` 后续分支中，流体管的压力阀调度在未暂停且覆盖计数超过 2 时，先通过 `CoverConnections.update` 断开阀面，再调用释放判断。因此空罐也会断开连接。共用连接门控同时拒绝阀面前方的流体管，覆盖原生工具和自动连接的两侧查询。没有把阀门改成可装在基础机器上的通用排液覆盖板，也没有改变 ALIGN-100 的计时差异。

旧输送夹具改用已注册单通道钢管，经实际 serverTick 驱动并检查整罐精确守恒；新增夹具检查空罐、暂停、后来放入相邻管道、双向连接门控及双方连接位清理。验证结果在本批回执中记录；历史的未修复观察保持原文。

## 验证与后续

三份生产文件分别投影两版；两份旧 Forge GameTest 源码另外比较。仅归一方法重命名与一条断言文案后，八份代码／字面量比较通过。未新增模拟测试、原生编译、GameTest 执行、游戏、保存重启、构建或推送。来源哈希见 [回执](verification/pressure-valve-audit-20261019.json)。

继续检查材料容器的实际注册／流体能力补偿、阀门几何／渲染、控制面板的输入输出与状态保存，以及完整覆盖板与其余系统队列。
