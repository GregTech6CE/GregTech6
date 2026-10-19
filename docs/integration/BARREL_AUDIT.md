# 普通储液桶的封口、排液与发酵核查

日期：2026-10-19；基线 `b232d8cbe`。原版根目录 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java`。主要来源 `gregapi/tileentity/tank/TileEntityBase08Barrel.java`，并追踪 Root、Covers、FluidTankGT、FL、RecipeMap 和木桶子类。原作者 GregTech-6 Team / Gregorius Techneticies；保留 LGPL 来源，原始文件只读。

范围是共享 `TankBlockEntity` 使用的普通木桶、塑料罐、金属桶，及两版发酵／环境边界。不是物流桶、所有便携容器或多方块储罐的完整验收，也未逐项宣称桶目录数值对应。

## 已有实现与需要区分的入口

当前有软锤封口状态、活扳手自动排液开关、物品掉落保存液体与设置、气体／酸／等离子处理、发酵配方查询及液量缩放。Neo 的 FermentationAccess 已由 GregTechNeoForge 调用 bindOriginalRecipeMaps；旧类注释“尚未绑定”已纠正，不能把 Neo 发酵整个报成缺失。

原版配方查询传入 selector-tag(0)，RecipeMap 的匹配调用 `isRecipeInputEqual(F, T, ...)`，这里 T 表示忽略输入堆数量。因此少于标准配方输入量的缩放本身不能直接报成缺陷；原版也支持按桶内量计算。当前两版拒绝额外物品需求和气态输入／输出，完整缓存优先级、标签／components 和动态配方规则尚未验收。

原版放置桶通过 Root.fill → Covers.getFluidTankFillable → Barrel.getFluidTankFillable2 → FluidTankGT.fill；最后一个只处理身份／容量，没有防护检测。原版 `fill(ItemStack, ...)` 则是另一条便携物品接口，会先检查温度和防护。不能把这个重载当成世界方块填充的依据。

## ALIGN-086：封口没有阻止普通进出液体和自动排液

原版封口位同时约束便携填充／抽取、漏斗／龙头、连接储罐接口及放置桶的可填／可抽罐选择；tick 中封口发酵与自动排液属于互斥分支。

当前 fill 和两个 drain 均不检查 softHammerState，能力包装也没有封口检查；右键容器交互将当前实体作为 IFluidHandler。tick 则先按 autoOutput 排液，再按 softHammerState 发酵。因而封口只是启用发酵标志，没有实现上述隔离，可能一边发酵一边改变桶内量。

原版柱塞工具另有直接丢弃液体的入口，不能修成“封口禁止一切工具操作”。空桶软锤也存在状态差异：原版 `mMode &= +B[1]` 清除自动排液位而保留封口位；当前清除封口布尔值、保留自动排液。已纠正把当前空桶动作直接称为原版的注释，未改变执行行为。

## ALIGN-087：排液方向、限量和侧面能力不同

| 内容 | 原版 | 当前共享实现 |
| --- | --- | --- |
| 气体 | 上下两个垂直面 | 只用 isLighterThanAir 判断；为真时向六面，为假时仅向下 |
| 非气体但密度小于零 | 仅向上 | 同样向六面 |
| 普通液体 | 向下 | 向下 |
| 单面发送量 | FL.move 默认 Long.MAX_VALUE，底层以 int 上限及两罐能力约束 | 额外固定每面最多 1000 mB/t |
| 自动输出面的外部能力 | 未封口的桶仍提供罐，覆盖板可以拦截 | 非空且自动输出时，对选定上／下面直接隐藏整个 capability，包括读取和抽取 |

原版 `FL.gas` 与 `FL.lighter` 是两项独立判断，不能用浮力替代物态。上述是源码合同，不是实际环网吞吐或第三方接收器测试。

## ALIGN-088：加载后丢失发酵进度，初始 tick 也提前推进

当前 load/loadAdditional 虽读取 sealedTime 和 maxSealedTime，却把 fermentRecipe 设为 null。下一个 tickFermentation 查到配方后无条件将两个时间清零；接着计算总耗时并把进度增加到 1。

原版读取 mSealedTime，重新找到有效配方时只重算 mMaxSealedTime，保留已存进度。查配方的 tick 不进入后面的递增分支。以总工作 640、保存进度 200 为例，重新加载后第一次有效查配方，原版仍是 200，当前成为 1。该例为调用链推导，未声称做过存档重启测试。

未加载的新发酵也有一 tick 相位差：当前在找到配方当 tick 就推进，原版下一 tick 才推进。

## ALIGN-089：危险流体的世界／物品准入混用，且漏掉三类限制

原版世界桶先接收入罐，再在 tick 顺序处理温度、魔法、酸、等离子、气体及 allowFluid。便携物品入口则在填充前拒绝不适合的流体。

当前世界桶在 fill 前检查温度和气／酸／等离子三种防护，因此常规能力填充不会进入原版对应的破坏／丢弃路径。保留相关腐蚀 tick 并不能证明普通输入行为一致；那些分支仍可由存档数据或直接储罐操作触发。

同时存在遗漏：

- spec.simpleOnly 分支只有占位判断，没有 return false；原版木桶 onlySimple=true，并检查 FluidsGT.SIMPLE。
- spec.magicProof 没有进入准入和 tick；proofRejects 只接收气、酸、等离子三个参数。原版魔法液体还有污染替换分支，当前桶没有这条路径。
- 原版 allowFluid 拒绝 POWER_CONDUCTING；当前桶的准入与 tick 没有消费这个标志。当前 FluidCatalog 已有蒸汽、热氦等相关标记，不能归因于完全没有流体数据。

现有气／酸／等离子判断和酸性直接移除桶仍然存在。本轮修正了过度归因的注释，没有修改保护策略，也没有运行危险场景。

## ALIGN-090：过热结果总是熔岩，未保留原版按内容物分支

原版 meltdown 只有内容物是 lava 且至少 1000 mB 才替换为 level 0 flowing_lava，否则 setToFire，并调用 WD.burn。当前 Forge 的 SmelteryBlockEntityHelper 与 Neo 的 FluidTransportEnvironment 总是放置 level 4 熔岩，并对附近实体施加 4 点 heat。

例如过热的非熔岩流体，原版走火焰路径，当前仍产生熔岩。当前普通 fill 会先拦截超温，故此例需要已有超温内容物（如读取存档／直接罐操作）才到达该分支，不能描述成当前玩家普通灌入必然立即制造熔岩。

## ALIGN-091：发酵产量被强制至少 1、最多桶容量

原版使用 `FL.mul(..., F)` 缩放，再由 FluidTankGT.setFluid 直接设置数量；没有当前桶容量钳制。当前在纯比例计算后额外使用 `min(max(1, produced), tank.getCapacity())`。

Java 17 观察到纯计算的两个边界：每 1000 输入产 1、桶里只有 1 时，比例结果为 0，当前实体会改成 1；16000 输入按二倍产出时，比例结果为 32000，若桶容量为 16000，当前实体会截到 16000，原版 setFluid 不做这一容量截断。这是源码支持的比例边界，不是对所有内置发酵配方可达性的承诺。产物标签／components 及 long 溢出还需独立核查。

## 仪表与后续范围

原版进度接口返回 mSealedTime／mMaxSealedTime；当前实体既没有 ProgressSensorSource，也没有 MachineControl.Provider。SensorBlockEntity.PROGRESS 因而不能用普通桶已有的两个 getter 读取发酵进度，作为 ALIGN-077 的扩展记录。液量读取、放大镜提示和进度面板是不同入口，不能一起判缺。

2026-10-19 第三十七批补充：CoverProgress 会为具备库存／流体能力的宿主提供覆盖板进度回退，读取当前内容物占比；因此本处缺少的是原版吞吐／发酵测量合同，不能扩大为没有任何进度覆盖板读数。见 [显示计量补查](DISPLAY_METER_AUDIT.md)。

桶的物品能力、物流桶的 retained filter、装饰覆盖板许可、拆除恢复、所有发酵配方、标签匹配和世界更新时序仍待后续核查。dumpFluidsToAdjacent 方法存在，不等于方块拆除一定调用；当前 TankBlock 的 getDrops 已保存桶数据，本轮不把方法名当成实际掉落行为证据。

## 标准化及验证

展开 BarrelFermentationRules、Neo FermentationAccess、FluidTransportEnvironment 的压行代码；纠正尚未绑定、原版过热和封口动作等不准确注释。三个文件的代码／字面量相同；共享 TankBlockEntity 只改注释，两版显式展开后代码／字面量相同。

Java 17 只编译纯计算类和观察入口，没有缓存依赖，记录七项耗时／产量结果。没有原生编译、游戏、存档重启、完整构建或推送。来源哈希、等价核对和观察值见 [回执](verification/barrel-audit-20261019.json)。
