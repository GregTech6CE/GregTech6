# 2026-10-04 / 来源形态别名与外部标签转换

整体移植仍未完成。本批接续两条新 issues 的已提交修正，按减少测试要求使用一轮共享检查与集中双版构建，没有启动客户端或 GameTest。

## 来源与实际缺口

原版 `OP:219` 的 `casingSmall` 为半单位材料，`Loader_Items:116` 注册其自有物品。当前移植早已通过 `PrefixRegistry.ALIASES` 映射至 `itemCasing`，材质也沿用 casingsmall；源 `Loader_Recipes_Handlers:609–612` 的5/9个 plateTiny、plateGemTiny 转换已经生成。真正遗漏是来源 Form 身份没有统一拼写，导致普通旧配方替换和自动合成判断不能匹配该别名。旧 skipped 诊断称此内容未注册属于误报；先前验证记录保留为历史，本记录更正该结论。

原版 `OP:142` 的 `rawOreChunk` 明示 Harder Ores 前缀、27U/72即3U/8重量。`Loader_Items` 注册 oreRaw，没有注册 rawOreChunk；不据兼容前缀制造 GT 自有物品。源 Handler:553/576 规定1 oreRaw→3 rawOreChunk、3 rawOreChunk→1 oreRaw，均禁止自动合成。单输入碎块是 oreRaw 的第二种结果，保留 offset=1、variants=2，未提供碎块时也不把 gem 结果移动格位。数量直接保留来源值，不按近似材料重量反推。

## 实现

代码提交 `e1f74708da977326220472a82886bf891b74583c`，NeoForge 标签重绑收尾提交 `063d47d9`。`PrefixRegistry.sourceName` 与共享 Form 构造器把 itemCasing 身份统一为 casingSmall；生成脚本同步。两平台原生索引接受源拼写及相同形态的移植拼写，原生 GT 物品身份优先于外部标签。

外部碎块示例标签为 `gregtech:raw_ore_chunk/iron`，明确前缀拼写也接受 forge/c；通用等价标签为 `forge:raw_ore_chunks/iron`、`c:raw_ore_chunks/iron`。材料名必须对应现有有效材料，标签必须含实际已注册物品，通用 storage_blocks 不猜形态。输入合并实际同形态候选，结果使用已有 GT/原版统一物品或真实外部物品；源前缀/材料匹配及 NO_AUTO 字段保留。

Forge 在加载后的标签中构建候选，再生成源转换。NeoForge pack 生成阶段只索引原生身份，避免读取尚未绑定的标签；ServerStarted/OnDatapackSync 的现有生命周期在标签完成后重建本加载器拥有的 `gregtech:form_conversion/` ToolShapelessRecipe 行，补实际存在的可选固定及普通/致密矿石转换，保留稳定 ID。已有行也重绑当前标签，避免仅新增缺失 ID 时遗漏外部同类输入。导线、管道和其他加载器配方沿原有流程处理，生命周期仍以实际 RecipeHolder 身份判断是否已应用。

## 验证与边界

快速双版编译48s通过，日志 `work/prefix-aliases-compile-20261004.log`。一次正式 `:core:check :assemble :neoforge:assemble -x :core:processResources` 1m50s通过，日志 `work/prefix-aliases-build-20261004.log`。共享行为2,094、机器34、材料7,646、温度53，共9,827条断言通过；九项新增来源样例覆盖5/9小板别名、不同材料拒绝、GT接口豁免、碎块精确输入数量、原版格位和不虚构自有碎块物品。60条固定转换生成目录检查通过，完整材料指纹保持原值，109个类型前缀和自有物品注册内容没有增长。

标签重绑收尾只运行增量双版 assemble，5m14s终态通过，日志 `work/prefix-aliases-native-final-20261004.log`，没有重复共享检查。共享资源本批未改变，正式命令使用原有已验资源缓存，未使用快速编译classpath。最终两包包含642个与当前编译输出相同的共享类，平台 metadata/NOTICE、重复条目与测试夹具排除均通过，见 [成品回执](prefix-aliases-artifacts-20261004.json)。Forge两个变更原生类在重混淆包内存在，NeoForge两个与当前编译字节相同；只读来源及日志哈希见 [内容回执](prefix-aliases-content-20261004.json)。回执中的源码提交是构建代码树，不表示 JAR 内含该字段；build_commit为null。

成品路径为 `C:\Dev\GregTech6\build\libs\gregtech6-1.20.1-forge-0.0.0.jar`、`C:\Dev\GregTech6\neoforge\build\libs\gregtech6-neoforge-1.21.1-0.0.0.jar`。没有外部 Harder Ores 安装、原生标签/配方重载、客户端展示/转移、蓝图机器执行或真实存档的新运行证据。原版 Handler:65 的外部 rawOreChunk→3 crushedTiny Crusher 路线及完整外部材料组成/处理回调仍待实现。
