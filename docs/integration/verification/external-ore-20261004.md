# 2026-10-04 / 外部矿物形态的来源处理链

整体移植仍未完成。本批接续 rawOreChunk 的工作台转换，代码提交 `fd78049ab4b5064ab1d68d26ada8df5b7a594c39`。按减少测试要求，只运行一次共享检查、集中编译和双版打包，没有启动客户端或 GameTest。

## 来源规则

原版 `Loader_Recipes_Handlers:62,65–67` 使用通用 `RecipeMapHandlerPrefix` 注册以下四条路线，均16 EU/t、禁止反物质材料、关闭数量优化，没有额外流体/催化剂或粉末余料：

| 输入 | 机器与输出 | 材料重量 | 等级0时间 |
|---|---|---|---|
| rawOreChunk ×1 | Crusher → crushedTiny ×3 | 输入和输出均3/8 U | 24 ticks |
| chunk ×1 | Crusher → rubble ×1 | 输入和输出均2 U | 256 ticks |
| rubble ×1 | Crusher → pebbles ×1 | 输入2 U、输出3 U | 384 ticks |
| pebbles ×1 | Sifting → dust ×3 | 输入和输出均3 U | 1536 ticks |

`RecipeMapHandlerPrefix:getCosts` 使用较大的输入/输出总材料量，乘来源倍率及 `(toolQuality+1)`，向上取整并至少1 tick。不能只按输入量算 rubble→pebbles，不能把筛选误接入 Crusher。重量来自 `OP:142,146–148`，U=648,648,000来自 `CS:120`；保留来源给定的数量和重量，不调整其材料量差异。

## 双平台实现与生命周期

共享 core 增加四个真正的来源前缀元数据及 `ExternalOreProcessingRules`。这些前缀没有进入 `MaterialItemDefinitions.candidatePrefixes()`，没有新增 GT 自有物品、材质或变种。两平台在真实绑定的标签里识别明确前缀/材料，对每个实际输入物品注册对应机器行；没有实际输入或输出时不伪造路线。

`ExternalOreRecipeLifecycle` 接入 Forge/NeoForge 的 TagsUpdatedEvent，按公开 `shouldUpdateStaticData()` 规则处理服务器数据加载和远程客户端标签包，跳过整合服务器重复客户端更新。机器显示与服务端查找使用同一组原生 RecipeMap 行。每轮重建先移除自身记录的 Recipe 身份及输入索引，再更新候选；不按物品删除其他模组的全部配方。ServerStopped 清理本适配器的临时行。

临时组成表按原版重量及实际材料提供类型化数据，`ItemMaterialRegistry.base/get` 优先返回已有显式组成，再查询该表，外部标签不会覆盖已注册组成。表与行随标签重建/停服清理，不写入存档或永久 BY_ITEM；损耗缩放、组成查询、扫描、tooltip 与已有材料身份入口能读取该数据。`entries()` 保留原有静态注册表视图，不在 EMI 热路径临时复制整个大表；这不等于移植了全部跨模组材料回调。

## 已有证据与未验证范围

快速双版编译1m1s通过，日志 `work/external-ore-compile-20261004.log`。首次全图探针因 classpath 缺少测试观察类失败，补测试输出目录后执行成功；没有由此重复共享测试。两阶段的全图差异均只有4条前缀记录和4,640条对应材料形态观察新增，129,232条原有记录全部保留、没有移除，见 [差异回执](external-ore-material-differential-20261004.json)，据此更新完整指纹和113个类型前缀期望。

正式共享行为2,115、机器34、材料7,646、温度53，共9,848条断言通过；新增21项固定来源样例核对准确重量、四种处理时间、质量倍率、3份产物、正确机器、无GT自有物品及无效材料拒绝。正式 `:core:check :assemble :neoforge:assemble -x :core:processResources` 2m33s终态成功，日志 `work/external-ore-build-20261004.log`。两次构建使用已有离线依赖；正式构建不使用快速classpath，共享资源没有改动，没有额外原生收尾或重复共享测试。

两包644个共享类与当前编译输出逐字节相同，metadata、NOTICE、重复条目与独立测试夹具排除通过，见 [成品回执](external-ore-artifacts-20261004.json)。Forge七个变更原生类在重混淆包内存在；NeoForge七个与当前编译字节相同，来源及日志哈希见 [内容回执](external-ore-content-20261004.json)。源码提交为本记录的代码树，build_commit为null，不表示JAR内含该提交字段。

成品为 `C:\Dev\GregTech6\build\libs\gregtech6-1.20.1-forge-0.0.0.jar`、`C:\Dev\GregTech6\neoforge\build\libs\gregtech6-neoforge-1.21.1-0.0.0.jar`。

没有真实外部模组安装、原生标签包或 `/reload` 执行、JEI/EMI候选显示、机器消耗/输出、坩埚执行、客户端或存档的新运行证据。外部 chunk/rubble/pebbles 的 Shredder/Anvil 专门路线、其余来源外部前缀以及完整材料回调仍待接续，不能据四条路线声称所有外部兼容完成。
