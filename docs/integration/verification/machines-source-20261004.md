# 2026-10-04 / 原版设备、箱子、太阳能板与基岩矿

整体移植仍未完成。本批按要求减少测试：集中编译、正式双版 assemble、一次轻量共享检查和验包；不启动客户端或 GameTest 服务器。

## 原版对照与实现

- 小型 GT 传送门对照 `gregtech/tileentity/portals/MultiTileEntityMiniPortal*.java`：远端相反邻居的物品/流体/能源能力委托，六面红石及比较器缓存、下一拍输出、断开清零和超时。下界小门用点火器切换、灭火器关闭；末地小门用末影之眼激活。移除自加的实体传送和搭平台。`gregtech/loaders/b/Loader_MultiTileEntities.java:2003–2004` 的两条配方恢复为黑曜石长杆加锯，以及末地石长杆、四颗末影之眼和恶魂之泪。普通地牢房间的原生传送门用途保持原版。
- NeoForge 能源节点、电线、信号线、物流线和轴补 `ToolInteractionTarget`；轴向发电机及锅炉两平台使用同一旋转提示和操作规则。变压器活动扳手的模式切换保留。
- GT 容器掉落保留完整堆叠，只有超过真实堆叠上限才分组，不再随机拆成 10–30 个。helper 消费传入堆叠，保持原调用方的所有权语义。
- 锅炉按 `MultiTileEntityBoilerTank:165–178,202–215,242–243` 恢复热势 `HU + steam/2`、接触伤害严格大于 2000 且最多 10、未结垢不处理、除垢伤害不套接触上限、压力超过 15 格时除垢爆炸、两像素内缩碰撞及 `max(1,sqrt(steam)/100)` 爆炸强度。热伤害复用坩埚等设备使用的 GT helper。Jade 显示当前压力、拆除风险/强度、实体影响半径及接触/除垢伤害；半径不等于保证破坏的地形范围。26 个锅炉物品模型增加零压力仪表盘，世界仪表盘继续由原渲染器更新。
- NeoForge 六种容器界面移除重复 `renderBackground`；1.21.1 的 `AbstractContainerScreen.render` 已经调用它。
- EMI 流体槽使用 GT `fluiditem` 图标，委托的 key/components 及输入/输出索引仍为真实流体。JEI 和 EMI 共用耗能、功率、时间及启动消耗文字，页面高度统一为 146。该改动没有证明 EMI 标签警告和加载延迟已解决。
- 战利品箱仅首次创建开启菜单时生成战利品和经验；`onRemove/getDrops` 不再开箱。拆除物品携带未开启的表/种子或已开启标记，已有库存单独整组掉落。挖掘及重新放置不产生经验，不重置已消费的表。来源：`gregapi/block/multitileentity/example/MultiTileEntityChest.generateDungeonLoot`。
- 原版 `MultiTileEntitySolarPanelElectric` 未覆盖方块边界，继承 `TileEntityBase01Root:833–835` 的完整方块。移植的四像素薄片没有来源，已修成完整模型和碰撞。默认向下、允许水平、不允许顶面输出；收集器保持在上方，输出贴图随侧面选择改变。依据原版 `getValidSides/getDefaultSide/useInversePlacementRotation/getTexture2`。工作状态贴图及全部发电条件仍待逐项对照。
- 原版 `gregapi/worldgen/WorldgenOresBedrock.generateVein:180–230` 保留底层矿点，在其上方 1–6 层的 muffin 范围先填深板岩（下界填下界岩），再随机生成矿石。共享规则漏掉填母岩步骤，导致残留多层原生基岩，现已补齐两平台；空白随机结果也填母岩，范围外和底层不变。同时修正强制核心的成功返回值，以及负世界高度下小矿脉到海平面的绝对高度。只影响新生成，不自动改写旧世界。
- NeoForge 反应堆核心/外壳补铅材质 RGB 和不透明物品颜色。两种核心恢复原版两像素厚、只画轴向内外面的六个壳体 pass，`hot_outlet/cold_outlet` 的 36 种状态分别选择 face1/face2。2×2 模型状态不再引用不存在的 facing。电磁铁/通量磁铁的 40 个平台模型补根显示父模型；普通铁/钢磁铁注册和用途仍待原版对照。

## 证据范围

`work/check-current-assets.py` 解析本批 244 个资产 JSON，分别合并平台和 core 资源核对模型与贴图引用，没有缺失引用。这不证明实际渲染像素正确。

两次集中编译发现并修正 NeoForge 能源节点内部交互方法的适配问题，失败日志为 `machines-source-compile-20261004.log` 和 `machines-source-compile2-20261004.log`。最终 `:assemble :neoforge:assemble :core:check` 正式构建 9m11s 成功，日志为 `work/machines-source-build-20261004.log`；该构建没有使用绕过 core 资源或 classpath 的快速参数。共享 9,617 条断言通过（行为 1,907、机器参数 34、材料 7,623、温度 53），包括小门信号缓存轨迹、锅炉伤害边界和原版基岩矿 840 个母岩位置的固定样例。

两版成品的 600 个当前共享 class、平台元数据、重复条目及测试夹具排除验包通过，见 [本批验包回执](machines-source-artifacts-20261004.json)。成品为 `C:\Dev\GregTech6\build\libs\gregtech6-1.20.1-forge-0.0.0.jar` 和 `C:\Dev\GregTech6\neoforge\build\libs\gregtech6-neoforge-1.21.1-0.0.0.jar`。验包不替代游戏实测。

已有 Forge 传送门和箱子 GameTest 夹具更新为当前合同并参加编译，但本批没有运行，不能沿用历史运行结果。跨维度委托、工具提示、碰撞伤害、箱子搬运、EMI 菜单/GUI 暗度、新地形生成、模型渲染及真实存档重启没有新增游戏证据。Vanilla 比较器只能读取单个模拟输出，暂取六面的最大值；GT 小门/extender 之间保留分面查询。小门还不是原版全部机器控制接口的完整代理。

其余配方、tooltips、蜜蜂、覆盖板、占位电池、Nexus/roads 及 EMI 标签警告见 [反馈接续账本](../user-feedback-backlog-20261004.md)。Nexus 与 roads 已获明确要求先默认开启，cfg 在收尾实现。
