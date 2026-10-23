# 2026-10-04 / 原版材料拆分的格位选择

整体移植仍未完成。承接原版对齐与减少测试要求，本批只集中编译、一次共享检查、正式双版打包和验包，没有启动客户端或 GameTest。

## 来源与行为

- `gregapi/recipes/AdvancedCrafting1ToY.java:55–67,175–200`：构造时在输入前缀的 `mShapelessManagersSingle` 中保留偏移；匹配时以第一件物品之前的空格数对该前缀全部单输入转换取模。输出不存在也占偏移，不应压缩成当前材料可用的输出列表。全部实例仅由 `gregtech/loaders/c/Loader_Recipes_Handlers.java` 注册。
- `Loader_Recipes_Handlers:552–574`：补回锭到四个碎粒、粉末到四小堆、粉末块到三十六小堆、锭块到三十六碎粒。粗矿到三个 rawOreChunk 仍缺对应前缀，但保留偏移；其反向和 casingSmall 的五/九格转换仍未实现。
- 单输入锭/粉末的第一结果适用九格工作台的第 1、3、5、7、9 格，第二结果适用第 2、4、6、8 格（按行从左到右计数）；两格宽背包同样按其实际格数计算。
- `Loader_Recipes_Handlers:619–625`：每个大导线前缀按从小到大的因数尺寸登记拆分结果；各材料使用相同偏移。大尺寸不存在的输出仍保留位置。合并九格上限与拆分十至十六根输出的旧修复保留。
- `AdvancedCraftingXToY.matches:206–231` 的空格早退出是在剩余格数已不足时拒绝；有效网格不要求物品紧靠第一格。更正生成器中原有的“向第一格排列”和“竞争结果只能保留第一条”说明。
- 两种源构造器的默认条件均为 `MT.NULL.NOT`；移除移植生成器误加的反物质排除条件。

Forge 保留 `CraftingContainer` 的原始位置。NeoForge 1.21.1 的原生 `CraftingInput.ofPositioned` 会裁去空边；新增 Mixin 只记原始网格大小和裁切位置，原生宽高、物品列表与剩余物品索引保持裁切语义。共享空输入单例不写入位置。依据本地 NeoForge 合并源码的 `CraftingInput.java` 和 `CraftingContainer.java`，没有执行 Mixin 的运行证据。

`ToolShapelessRecipe` 的可选 `gregtech_form_variants` / `gregtech_form_offset` 随 Forge JSON/网络和 NeoForge Codec/StreamCodec 传递；普通工具及允许的管道拆分默认无格位限制。原生生成行及 456 条已有静态拆分行都使用同一选择器。静态绑定脚本保持 1,272 条数量/材料/前缀绑定，复查为 0 pending，见 [来源绑定](form-conversion-bindings-20261004.json)。

自动合成权限快照增加输出及其统一物品身份；普通配方使用其静态结果检查，不再仅因相同输入而拒绝其他产物。原版 XToY 还检查输出材料；1ToY 的旧配方移除只检查输出前缀。当前快照绑定实际存在的 GT/统一输出，没有据此声称完成全部跨模组前缀识别或原版配方移除。

## 验证

快速双版编译 25s 通过，日志 `work/form-conversions-compile-20261004.log`。一次共享检查通过：行为 2,077、机器参数 34、材料 7,646、温度 53，合计 9,810 条断言。其中新增三十项原版格位固定样例，原有权限样例补两项不同输出边界。

456 份变更共享 JSON 解析与偏移合法性检查通过，原字节同步至既有 `core/build/resources/main` 并逐份比对，见 `work/form-conversions-resources-20261004.json`。正式打包使用正常 core 依赖，只跳过已同步的 `:core:processResources`，未使用快速 classpath 参数。

正式双版 `:core:check :assemble :neoforge:assemble -x :core:processResources` 在 5m08s 后终态成功，日志 `work/form-conversions-build-20261004.log`。没有重复共享检查或原生收尾构建。两包均验过 641 个当前共享 class、metadata、NOTICE、重复条目及独立测试夹具排除，见 [成品回执](form-conversion-artifacts-20261004.json)。另逐份比对每包 456 个变更共享配方；Forge 七个原生 class 在重混淆后存在，NeoForge 十个原生 class 与当前编译字节相同，Mixin 配置与源码字节相同，见 [变更内容与来源哈希](form-conversion-content-20261004.json)。

代码提交 `dcf5eea8f191c63bafa980345bc73aa0f87a82cb`。双版成品为 `C:\Dev\GregTech6\build\libs\gregtech6-1.20.1-forge-0.0.0.jar` 和 `C:\Dev\GregTech6\neoforge\build\libs\gregtech6-neoforge-1.21.1-0.0.0.jar`。验包是打包证据，不是游戏或网络/Mixin 执行证据。

## 保留缺口

统一后的原版物品及其他模组物品仍未全面接入手动转换；其普通配方与源转换的替换范围仍需对照，不能用本批格位选择声称完成所有转换。普通矿石到粗矿的源前缀循环、rawOreChunk/casingSmall 内容与配方仍待实现。配方书自动填充、JEI/EMI 展示与转移、原生网络往返、Mixin 实际生效、客户端合成和真实存档重载均没有本批运行证据。

基岩矿提问再次确认：`WorldgenOresBedrock.generateVein:196–207` 在上方六层矿团轮廓内换母岩，无深板岩时 `WD.removeBedrock:783–798` 使用当地石头；下界用下界岩。底层及轮廓外基岩保留，不清成空气。两平台已有 `7bff1d9f`，只影响新生成区块，本批未重复修改或运行世界。
