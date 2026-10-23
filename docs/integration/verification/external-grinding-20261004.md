# 2026-10-04 / 外部 Shredder 与 Anvil 研磨路线

整体移植仍未完成。代码提交 `7eb66493`；本批接续已恢复的四种外部矿物形态，不增加前缀、材料或 GT 自有物品。按减少测试要求采用集中编译、一次共享检查及双版打包，没有启动客户端或 GameTest。

## 来源与结果

原版 `Loader_Recipes_Handlers:117–119,129–131,158–160` 是六条 Shredder 分支声明和三条 Anvil 声明，均16 EU/t、排除反物质、关闭数量优化。

| 输入 ×1 | Shredder 结果 | Anvil 结果 | Shredder 等级0时间：MORTAR / 非MORTAR | Anvil 等级0时间 |
|---|---|---|---|---|
| chunk | dust ×2 + dustTiny ×1 | dust ×2 + dustTiny ×1 | 34 / 541 ticks | 34 ticks |
| rubble | dust ×2 + dustTiny ×1 | dust ×2 + dustTiny ×1 | 34 / 541 ticks | 34 ticks |
| pebbles | dust ×3 + dustTiny ×1 | dust ×2 + dustTiny ×1 | 50 / 797 ticks | 48 ticks |

Shredder 两分支用 MORTAR 来源标记选择倍率16或256；Anvil 只允许 MORTAR 且要求一个工作位为空。`RecipeMapHandlerPrefix:getCosts` 取输入/输出总材料量较大的值，乘倍率及输入材料的 `(toolQuality+1)` 后向上取整。必须计入1/9U微量粉末，不能先把基础时间取整再乘质量；例如 chunk 的非MORTAR等级2时间是1622 ticks，并非1623。Anvil 的 pebbles 行只产2份主粉末，不补源码未指定的余料，耗时由3U输入决定。

`RecipeMapHandlerPrefixShredding:getOutputMaterial` 使用 `mTargetPulver.mMaterial`，不会按目标数量字段再次缩放。这既用于新外部行，也用于原有八种铁砧研磨输入；两平台旧 AnvilShreddingRecipes 原先把结果仍绑定输入材料，现修正主产物和微量产物的粉碎目标。例如原版 WroughtIron 粉碎为 Iron。来源 MORTAR 集合中 Iron 也可研磨，本批负例使用实际不带该标记的 Tungstensteel。

## 双平台接入

共享 `ExternalOreProcessingRules.GRINDING` 的六种路线表示全部九条声明；Shredder 的两种时间由来源标记选择，避免重复候选。原生适配器在加载后的标签中找到实际输入，对相同粉碎目标取得主粉末和微量粉末；任一指定产物不存在时不注册残缺行。沿已有 TagsUpdatedEvent 生命周期生成、清理自身机器行及索引。

Anvil 直接构造包含第二个 EMPTY 输入的 Recipe，沿用 MaterialAnvilBlockEntity→AnvilWorkInputs 的实际工作位匹配入口，保持原版空位要求；不将 EMPTY 送进拒绝无效原料的 RecipeMap.make。Shredder 走关闭优化的 addRecipe1。普通已注册机器配方不因本适配器重建而被删除；实际物品及已有临时组成仍沿前一批规则。

## 验证与边界

快速双版编译37s通过，日志 `work/external-grinding-compile-20261004.log`。共享行为2,142、机器34、材料7,650、温度53，共9,879条断言通过。新增27项固定来源时间、产物数量与空位标记样例，四项完整材料样例检查 MORTAR 允许/拒绝、Shredder 非MORTAR分支与 WroughtIron→Iron 目标。完整定义/post-init图继续匹配前一批指纹，113个前缀和自有物品定义流不变。

正式 `:core:check :assemble :neoforge:assemble -x :core:processResources` 1m45s终态通过，日志 `work/external-grinding-build-20261004.log`。共享资源本批未修改，正式命令使用已有资源缓存和正常classpath；没有追加原生收尾或重复共享测试。

两包645个共享类与当前编译输出一致，metadata、NOTICE、重复条目与独立测试夹具排除通过，见 [成品回执](external-grinding-artifacts-20261004.json)。Forge四个变更原生类在重混淆包内存在，NeoForge四个与当前编译字节相同；来源、空位入口文件及日志哈希见 [内容回执](external-grinding-content-20261004.json)。源码提交为本批构建代码树，build_commit为null，不代表JAR含该字段。成品位于 `C:\Dev\GregTech6\build\libs\gregtech6-1.20.1-forge-0.0.0.jar` 和 `C:\Dev\GregTech6\neoforge\build\libs\gregtech6-neoforge-1.21.1-0.0.0.jar`。

没有原生空位执行、真实外部模组、标签重载、JEI/EMI 页面/转移、机器消耗/产物、客户端或存档的新运行证据；源码入口与编译/共享样例不替代游戏证明。clump/reduced/crystalline/cleanGravel/cluster 等其余外部形态、全部材料回调及现有更广的处理链仍待接续。
