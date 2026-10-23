# 第五十二批：电钻嵌入炸药与快捷栏遥控绑定

## 本轮范围

补充大型坩埚尚未实际测试，要求继续移植。大型坩埚沿用第四十五批的锅体及 Jade 修复，本轮未改其 Java；重新运行 compileJava / processResources 成功，见 `build/crucible_recheck_2026-09-28.log`。仍需客户端目视验收，不宣称本轮重现或修复了一个新的坩埚故障。

## 原版依据与实现

- `Behavior_Place_Dynamite.java:45-90`：电钻右键可钻入的支撑面，从主背包最后一格往前寻找普通炸药、强力炸药或 Boomstick。原版仅电钻挂载此行为，手摇钻不加入。
- `BehaviorPlaceDynamite` 使用炸药栈副本与 Forge 的 `ItemStack.useOn` 放置通道，临时写 `BlockStateTag.sunk`。成功后才消耗背包中的一枚炸药及现有电钻的单次 100 EU；创造模式不扣除。原始物品 NBT 不受临时状态污染。
- 放置取消保留 Forge 快照回滚；禁止编辑、目标占用、不可钻入或电量不足时不扣物品、不扣电、不写遥控坐标。
- `Behavior_Remote.java:92-108`：从快捷栏第 0 到 8 格找第一个可绑定的遥控器，已满 64 项则继续找下一个；已有同一坐标不会重复添加。坐标仍按维度保存。成功绑定使用音符提示，未移植 GT6 专属蜂鸣音效。
- `CS.java:1689` 的原版土层／石头白名单映射为 `gregtech:drillable_dynamite` 方块标签；16 种染色陶瓦与普通陶瓦均覆盖，并映射粗泥／灰化土、切制／錾制砂岩、六种旧版受虫蚀方块。GT 草地、可挖土层、岩石矿的原版类注册规则保留。自然石与矿石共用现代通用标签；GT 岩层仅允许原石／圆石／苔石三种原版状态。
- `DynamiteBlock.refreshSupport` 改用同一规则，避免刚钻入泥土的炸药随后错误退出嵌入状态。
- 中文提示取自提供的 `GregTech.lang:839`，英文取自原版行为类。

## 验证

新增四项 GameTest：背包逆序选择、100 EU 消耗与 NBT 保留、满遥控器跳过；创造放置；无效支撑／占用／缺电拒绝及原版白名单；Forge 取消放置完整回滚。新增两项 Python 资源测试检查白名单和双语提示。

第一轮门禁在初始化期间主动停止：复核发现 `ItemBehaviors.itemId` 只返回 path，不能用于完整命名空间判断。已改用完整注册 ID，避免空格 NPE 与绑定漏判；之后 1536 MiB 门禁在启动期间出现 OutOfMemoryError，未计为测试通过。仅调整测试 JVM 为 2304 MiB、SerialGC、4 个工作处理器，不修改 runClient。另核对 Minecraft 源码发现 makeMockPlayer 固定 isCreative=true，已换用独立模拟玩家测试真实生存消耗；该初始化运行也主动终止。`gametest_batch52_drill_final.log` 跑至第九组时仅报一项旧蜂巢测试错误：按世界种子切换平原／沙漠后仍固定使用沙漠夜行预期。已停止该失败运行并修正测试为两个生物群系 × 32 个固定随机种子，不修改蜂巢玩法。`build/gametest-batch52-drill-verified` 使用 2304 MiB / SerialGC / 8 处理器，前八组无失败，但后段在地牢／工具配方序列化时发生 Java heap space；该轮不计通过。源码保持冻结，最终改为 `build/gametest-batch52-drill-g1`，4096 MiB / G1GC / 8 处理器，新世界重跑。这轮迅速跑到最后一组，暴露了实际地牢生成越界和旧砧测试问题，已停止失败运行并修复如下；最终新目录重跑结果见下文。

## 人工验收

1. 先从创造栏取得电钻并充电，再切换生存模式，准备背包炸药和快捷栏遥控器；右键天然石、矿石、泥土、黏土或陶瓦，应露出短引信模型。
2. 放置一次减少一枚炸药与 100 EU；遥控器应自动记录它。右键遥控器后约 1 秒起爆，中心位于背后的被钻方块。
3. 再测试向上、向下及四个水平面；泥土支撑更新后应保持嵌入，移除支撑应恢复完整炸药外形。
4. 创造模式放置不消耗；缺电、基岩、石砖、木板及目标被占用时不应放置。领地保护取消也不应扣物品或电量。
5. 大型坩埚按第四十五批清单测试空锅成型、拆解恢复、主控／墙面 Jade 温度和内容；本轮未进行像素验收。

## 仍有差异

现有 ElectricToolItem 尚无 GT6 材料工具头磨损系统，本批复用其 EU 单次使用代价，不宣称电钻本体全量还原。GT6 对旧第三方模组土层的显式白名单没有猜测现代注册 ID，现代附属模组可通过新增标签接入。后续还需复核电动工具制作路线。整项目的其余占位家族、客户端外观、完整生存闭环仍需继续审计。

本轮未构建 jar，没有执行 git commit。

## 下一批电动工具入口核对（只读，尚未实现）

- 原版 `Loader_Tools.java:352-377` 以材料条件（非反物质／木材／弹性材质，类型与品质门槛）、电池标签和 LV/MV/HV 电机生成工具配方；钻头 LV 图案为 `fSY/TXW/dVZ`，不是固定铁工具合成。
- 内部 `OreProcessing_Tool:382-513` 负责材料工具头、机身材料、电池容量和工具 NBT 成品。必须核实符号映射和电池容量继承，不能把显示材料名称当真实工具属性。
- 移植版 `GTElectricItems.java:50-53` 仅四种固定电动工具；`GTToolType` 只有手摇钻而没有这些电动类型；电动工具并未接入现有 `GTToolAssemblyRecipe` 材料组装系统。`MaterialPrefix` 已有钻头／链锯头前缀可复用。
- 后续应先闭合材料化电动工具实例与原版装配配方，再补工具行为、级别与磨损。该核对不计为本批已移植内容。

## 本轮文件清单

- 新增 Java：`content/tool/DynamiteSubstrates.java`、`item/behavior/BehaviorPlaceDynamite.java`、`gametest/DrillDynamiteTests.java`。
- 修改 Java：`item/ElectricToolItem.java`、`item/behavior/BehaviorRemote.java`、`block/tool/DynamiteBlock.java`、`gametest/BumbleWorldgenRepairTests.java`。这些路径均相对 `src/main/java/com/gregtech/gregtech/`。
- 新增资源：`src/main/resources/data/gregtech/tags/blocks/drillable_dynamite.json`。
- 修改语言：`src/main/resources/assets/gregtech/lang/en_us.json`、`zh_cn.json`，各新增钻头行为提示。
- 新增 Python 测试：`tools/tests/test_dynamite_substrates.py`。
- 进度：本报告、`docs/NEXT_GOAL.md`、`PORTING_AUDIT.md`、`docs/REPAIR_TRACKER_2026-09-20.md`、`docs/porting-progress.json`（已按最终日志刷新）。

## 门禁追加修复

- `GTDungeonLayout.connectionCount` 以前无条件访问四个相邻数组项。刷怪农场外围平台允许占用最外层空格，因此实际世界生成触发 `Index 7 out of bounds for length 7`，并连带使矿脉测试加载区块失败。现在数组外视为空，内部连接数保持原语义。`DungeonMobFarmOuterTests` 新增全边界计数及真实 feature 在 `(0,0)` 外围空格生成屋顶的测试，不通过跳过平台掩盖错误。
- `AnvilSmashingTests` 原先只按“可研磨”挑材料，未考虑原版 oreRaw 配方的自粉碎条件；还在设置锤子后调用 clearContent，导致实际上空手取回原料也可能被当作通过。现使用独立模拟玩家、选择三个矿石形态均有配方且满足自粉碎的材料、保留实际锤子并从其它物品槽检查产物；新增产物不得等于输入的断言。未修改砧处理或配方规则。
- 对应追加文件：`worldgen/dungeon/GTDungeonLayout.java`、`gametest/DungeonMobFarmOuterTests.java`、`gametest/AnvilSmashingTests.java`（相对 Java 包根）。

## 最终验证结果

- `build/gametest_batch52_drill_g1_green.log`：2026-09-28 02:36:17 **All 982 required tests passed**，0 条 `failed!`；`BUILD SUCCESSFUL in 12m 48s`，退出码 0。测试目录 `build/gametest-batch52-drill-g1-green`，4096 MiB / G1GC / 8 处理器。测试期间 Java 和资源冻结，最新 Java 时间早于最终门禁日志。
- 编译通过；新增钻头 4 项及地牢边界 1 项回归已包含在 982 项中，修正后的蜂巢／砧旧测试也通过。
- `python -m unittest discover -s tools/tests -p test_dynamite*.py`：4 项通过（原有模型／贴图 2 项 + 本批标签／语言 2 项）。
- `audit_porting_progress.py --log build/gametest_batch52_drill_g1_green.log` 已刷新进度；缺失 token 仍为 49，不能据此宣称全量移植完成。
- 未构建 jar，未进行客户端像素验收。没有执行 git commit。
