# 2026-10-04 / 五种外部矿物形态与粉碎数量

整体移植仍未完成。代码提交 `5152d70ff56855fe1408dbd3ae816d6f5c540b10`。本批接续外部形态，不增加 GT 自有物品或资产；按减少测试要求只做集中编译、一次共享检查及双版打包，没有启动客户端或 GameTest。

## 来源与处理结果

原版 `OP:143–144,149–151` 定义 clump、reduced、crystalline、cleanGravel 为1U，cluster 为3U；U为648,648,000。它们都是其他模组可提供的矿物形态。共享层新增类型化前缀元数据，仍排除在 GT 自有物品的显式定义流之外。

原版 `Loader_Recipes_Handlers:120–124,132–136,161–165` 有十条 Shredder 快慢分支声明和五条 Anvil 声明；`:84/:86/:87/:89` 有四条 Mortar 声明。共享层以十四条新路线表示这些十九条声明，Shredder 按 MORTAR 选择倍率，所有行16 EU/t、排除反物质并关闭数量优化。

| 输入 ×1 | Shredder / Anvil 指定结果 | Shredder 等级0：MORTAR / 非MORTAR | Anvil 等级0 | Mortar |
|---|---|---|---|---|
| clump / reduced / crystalline / cleanGravel | dust ×1 | 16 / 256 ticks | 16 ticks | OM.pulverize 全量余料，16 ticks |
| cluster | dust ×3 | 48 / 768 ticks | 48 ticks | 源码没有该行，不生成 |

这五种形态的 Shredder/Anvil 原版行没有微量粉末，不沿用 chunk/rubble/pebbles 的 dustTiny 副产物。Anvil 仍要求 MORTAR 和第二工作位为空；Mortar 只要求 MORTAR，不引入 Anvil 空位。时间取较大的输入/指定输出总重量，再乘倍率及输入材料的 `(toolQuality+1)`；等级2时间为上表的三倍。

`PrefixShredding` 指定输出使用粉碎目标材料而保留显式数量；Mortar 的空输出前缀则走原版 `OM.pulverize`，按粉碎目标数量换算余料。这两种规则不能混用。

## 原有粉碎数量修正

原版 `OM:370–371` 调用 `UT.Code.units(input, U, targetPerUnit, false)`，即 `floor(input × targetPerUnit / U)`。两平台原有 MortarGrindingRecipes 写成了反比 `input × U / targetPerUnit`，现用共享 PulverizationRules 修正。例如3U输入、每U产半U的目标应得1.5U，旧式会得到6U。

源 UT 在同单位时直接返回输入，并在整除时先约分。共享实现保留单位恒等快捷分支，其余使用精确整数中间量后向下取整，避免大数量乘法先溢出；64U输入的恒等/半量结果样例均通过。零目标返回零，非法粉碎目标不再退回输入材料。超过 long 可表达结果时封顶 Long.MAX_VALUE 是本地数量边界保护，不宣称复现原版溢出后的结果。

## 双平台标签与组成

两平台沿已有 ExternalOreProcessing 标签事件适配器，对实际加载的形态/材料标签绑定配方。新的九形态列表单独建立临时组成，再生成机器行，避免只有 Crusher/Sifting 输入得到组成而遗漏新研磨输入。显式组成仍优先，旧临时组成在索引本轮标签前清除；重载只移除本适配器拥有的行及索引成员。

标签沿现有 `gregtech/forge/c` 命名空间的明确来源形态和材料路径识别，cleanGravel 对应 clean_gravel；本批没有猜测其他模组的所有复数标签，也没有安装真实外部模组进行兼容验证。缺少实际输入或指定输出就不注册残缺配方。已有 Anvil/Mortar 诊断同步改为标签绑定说明，dirtyGravel/crystal 两个 Mortar 形态仍待接续。

## 共享证据

快速双版编译49s通过，日志 `work/external-rest-compile-20261004.log`。一次共享行为2,244、机器34、材料7,652、温度53，共9,983条断言通过；新增102项来源重量/时间/产物/余料数量与自有物品边界样例，两项完整材料样例核对 Mortar 的 MORTAR 允许/拒绝。

定义与post-init全图逐行对比均保留全部133,876条旧记录，只增加五个前缀及5,800条材料形态观察；材料对象、别名、旧属性/目标/形态没有变化。前缀总数118，指纹依据见 [完整差异回执](external-rest-material-differential-20261004.json)，不是直接改期望值使检查通过。

正式 `:core:check :assemble :neoforge:assemble -x :core:processResources` 2m06s终态通过，日志 `work/external-rest-build-20261004.log`。共享资源本批未修改，正式命令使用现有资源输出和正常classpath；没有重复共享检查或追加游戏运行。

双包646个共享类与当前编译输出一致，metadata、NOTICE、重复条目及独立测试夹具排除通过，见 [成品回执](external-rest-artifacts-20261004.json)。每包六个变更原生类验包通过：Forge检查重混淆后存在，NeoForge检查与当前编译字节相同；原版八份源文件与日志散列见 [内容回执](external-rest-content-20261004.json)。source_commit表示本批构建代码树，build_commit为null，不代表JAR含该字段。成品位于 `C:\Dev\GregTech6\build\libs\gregtech6-1.20.1-forge-0.0.0.jar` 和 `C:\Dev\GregTech6\neoforge\build\libs\gregtech6-neoforge-1.21.1-0.0.0.jar`。

本批没有真实外部模组、原生标签重载、研磨执行/消耗/空位、JEI/EMI 页面及转移、客户端或存档的新运行证据。完整材料回调、其他外部形态和更广处理链继续留在缺口账本中。
