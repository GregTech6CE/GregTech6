# 第六十九批：木工工具放置树苗与工作台

## 原版依据

Behavior_Place_Sapling / Behavior_Place_Workbench：服务端、玩家可编辑、背包 mainInventory 逆序扫描；树苗只接受顶面，工作台拒绝木材/叶/植物/藤/瓜/仙人掌。每个行为只尝试第一个匹配物品，失败返回，随后仍可进入后续行为。创造保持材料数量，两种放置都没有工具损耗。GT_Tool_Axe/GT_Tool_Saw/GT_Tool_Chainsaw_LV 注册了两个行为；GT_Tool_AxeDouble 继承斧。

MultiItem.onItemUse 先 useEnergy(0) 更新状态再检查可用 metadata；MultiItemTool.isUsableMeta 只允许偶数。现代版本生存电锯要求正电量、单件、未损坏，实际放置不扣 EU/磨损。携带电池自动补能仍未实现，不在此批假装实现。

## 实现

- 新 BehaviorPlaceWoodworkingSupplies 共用服务端流程：先树苗后工作台、每类末槽优先、调用真正 ItemStack.useOn，保留 Forge 放置快照和取消事件回滚。
- 材料使用副本执行放置，成功后从源栈扣实际消耗数量；保留源 NBT 和手持工具；创造不扣材料。
- 检查点击位置和实际 BlockPlaceContext 目标的加载/编辑权限。
- 树苗接受 minecraft:saplings 和现有 gregtech:saplings；工作台新增 gregtech:crafting_workbenches 内部扩展标签，默认 minecraft:crafting_table。该标签不是对外宣称的 Forge 通用标准，不把高级合成台等具有不同用途的方块自动视为等价工作台。
- ElectricToolItem 的 Chainsaw 分支及 GTToolItem 的 AXE/DOUBLE_AXE/SAW 共用行为，其它工具保留原逻辑。

## 验证

新增 WoodworkingPlacementTests 三项：

1. 电锯背包末槽树苗优先，消耗一颗，材料 NBT 保留，工具完整 NBT 不变，创造保留材料。
2. 工作台拒绝木头、允许石头侧面，树苗拒绝侧面，斧/双头斧/手锯均可种植且不损耗。
3. Forge EntityPlaceEvent 取消后方块和源栈回滚，空电/权限拒绝不消耗材料。

新世界 build/gametest-batch69-woodworking-placement，日志 build/gametest_batch69_woodworking_placement.log；15:08:53 报告 All 1036 required tests passed，15:09:24 正常关闭，进程退出码 0，BUILD SUCCESSFUL（9m19s）。已刷新 docs/porting-progress.json。运行期间 Java/资源冻结，不构建 jar。

## 人工验收与剩余

背包后排放树苗/工作台，用上述工具右键泥土顶面种树、右键石头放工作台；确认背包材料扣一、工具电量/寿命不变；木头侧面不放工作台。空电电锯需先充电。其它树苗可由相应标签扩展。

电锯连伐及树高/梁速度、枯灌木额外掉落、配方工具替代和携带电池充电仍待补。大型坩埚、板锭、硬币、书架等客户端模型验收仍待实测。完整目标未完成。

没有执行 git commit。
