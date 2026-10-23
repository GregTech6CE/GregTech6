# 第六十五批：电动工具材料寿命与破损废料

## 原版核对

- MultiItemTool 构造工具时以材料 mToolDurability×100×工具倍率确定上限；本轮四种 LV 工具的倍率均为 1（电锯继承斧）。
- MultiItemTool.doDamage:433 起：电动工具每次耗能，只有 nextInt(max(10, materialQuality×20))==0 时增加对应材料磨损。磨损与电量分开，原版 GT.ToolStats.k 保存磨损。
- ToolStats.getBrokenItem:186 以 1+nextInt(1+4×materialAmount/U) 返回同材质 scrapGt。Loader_Tools:158–169 声明电锯 2U、扳手 4U、螺丝刀 U、手钻 U/2，对应废料随机数量 1–9、1–17、1–5、1–3。没有完整电池返还规则。

## 实现

- 新增 ElectricToolWear 管理材料耐久上限、NBT 磨损、品质概率及原版废料范围，使用 long 并对异常极值饱和处理。
- ElectricToolItem 的机器点击耗电同时抽取材料磨损；电钻 consumeEnergy 路径同样接入，BehaviorPlaceDynamite 传入真实玩家以使用其随机源和处理废料。未成功放置不消耗、不磨损。
- canInteract / hasEnergyForUse 检查工具材料未损坏，损坏但有电也不能继续工作。
- 本轮已接入的动作结束后处理破损：消耗工具，废料放入玩家背包，放不下则掉落；非玩家生物在原地掉落。没有玩家实体的消耗保留损坏 NBT，服务端 inventoryTick 在持有者出现后处理，兼顾存档重载；空栈不会重复返还。
- 创造能力免除电量、磨损和破损回收；耐久 tooltip 使用现有双语键，电量条继续展示电量，不将材料磨损伪装为电量。

## 验证

新增 ElectricToolWearTests 三项：材料公式/抽样边界/独立电量/NBT/溢出；使用真实工具和玩家随机源强制磨损到破损，验证同材质废料范围与不重复返还；重载损坏手钻拒绝工作并在库存 tick 回收。原有电动交互、工具装配和炸药放置回归同属全套门禁。

首次门禁在复核零耐久边界后主动停止，确认进程退出再将最低寿命修正为 1，并换新世界重跑。最终世界 build/gametest-batch65-electric-wear-final，日志 build/gametest_batch65_electric_wear_final.log：07:26:14 报告 All 1024 required tests passed，07:26:47 正常退出，Gradle exit 0，BUILD SUCCESSFUL in 9m 6s。包含原有交互/炸药/装配以及新增三项测试。所测 Java 和资源保持冻结，没有构建 jar。

## 待完成与验收

本轮只将材料磨损接到已有动作路径，尚未实现电动机器拆卸、电锯采掘/剪叶/冰块、工具作为合成催化剂的完整替代、扳手/活动扳手切换及玩家随身电池补电。它们仍是后续工作，不计作本轮完成。

实机测试：充电工具的 tooltip 应同时显示 EU 与材料耐久；长期操作偶尔降低材料耐久，品质更高材料的磨损概率更低。寿命耗尽后获得同材质废料，重新进世界不得重复掉落。大型坩埚及工具方向预览像素仍待客户端验收。

没有执行 git commit。
