# 2026-10-04 / 双版本创建世界与构建前自检

先反馈 NeoForge 创建世界崩溃，随后提供 Forge 日志并要求下一次构建前自启动检查。整体移植仍未完成；本批优先处理实际启动/世界加载故障，不增加全套共享检查或机器测试。

## 故障与修复

- NeoForge 21.1.252 / Java25 日志：`CreateWorldScreen.openFresh → RegistryDataLoader → ManualToolRecipePack.data → Loader_FormConversionCraftingRecipes.register → CraftingMaterialForms.canonical`，`preferred.get(direct(output))` 把没有材料形态的普通产物作为 null 键传给 `Map.copyOf` 产生的不可变映射，触发 `Object.hashCode` 空指针。
- Forge 47.4.26 / Java25 日志：同一规范化代码在 `FormConversionCraftingHandler.onServerStarted` 建立转换配方时崩溃。两平台先取得 form，仅非 null 时查 preferred；其余产物继续走既有 `MaterialUnification.canonical`。不跳过配方，不吞异常，不删除材料统一功能；NBT/组件/损坏状态仍提前保留。
- Forge 日志另有多次 `SourceStreets.generate → NativeOriginWorld.sign → SignBlockEntity.setText/markUpdated`：生成中的 proto-chunk 方块实体尚未绑定 Level，实时文本设置却发送世界更新。两平台改用原生 `SignText.DIRECT_CODEC` 编码 front_text，经原生实体加载接口写入，再标记 dirty；保留后面文本、颜色/发光/蜡封及朝向。Neo 使用 registry serialization context，Forge 使用 NbtOps，均不假绑 ServerLevel 或丢弃路牌。
- 没有把完整日志、账号/启动参数发布到仓库；相关异常摘录仅保存在 ignored `work/world-creation-user-{neo,forge}-excerpt-20261004.txt`。

固定依赖源码已核对：两版 SignBlockEntity 的 setText 更新路径和 load/loadAdditional 读取路径，Neo SignText 使用注册表序列化上下文；Minecraft CreateWorldScreen.openFresh/onCreate 为实际创建入口。读取本地 Gradle 缓存的官方映射源码，未更改原版作者目录。

## 先运行，再构建

新增两端 opt-in `WorldCreationSmoke` bootstrap 夹具：从实际 TitleScreen 调用 openFresh，等待创建页面，设定全新 UUID 世界名并按创建按钮，使用普通主世界预设，等待真实玩家/集成服务器和30个游戏画面帧。服务器线程遍历物品注册表的规范化，检查数量保留、普通无形态物品、带名称数据物品和 GT 铁锭→原版铁锭；要求实际配方注册数大于1000、level.dat存在、截图保存，然后自动退出。所有夹具均不进入成品。测试目录是各平台 `build/world-creation-smoke-run`，没有打开或改动游戏实例/真实存档。

| 实测 | 新世界/玩家/主世界 | 规范化物品 | 实际配方 | 游戏画面帧 | 成功回执耗时 | Gradle终态 |
| --- | --- | ---: | ---: | ---: | ---: | --- |
| NeoForge 21.1.243 / Java21.0.7 | 成功 | 75,945 | 43,482 | 30 | 131,850ms | 3m07s成功 |
| Forge 47.4.20 / Java17.0.10 | 成功 | 75,868 | 43,366 | 30 | 184,720ms | 4m成功 |

日志分别为 `work/world-creation-neoforge-20261004.log`、`work/world-creation-forge-final-20261004.log`。两张1280×720截图均实际查看，显示游戏HUD、roads路面和有文字的路牌。这只是出生区的实际新世界证据，不能替代 Nexus/roads 的全部布局/跨区块/交互验收。

首次 Forge 检查已经创建世界、启动服务器并进入失焦暂停菜单，但夹具等待游戏画面，360s watchdog 明确 FAILED；Gradle正常退出7m24s也未视为检查成功。关闭隔离测试实例的 pauseOnLostFocus 后，只重跑 Forge 并获得上述成功回执。Neo 已通过的生产代码没有改变；其夹具同步加入失焦选项，正式构建时另做编译，没有重复 Neo 客户端。

成功日志未再出现反馈的两处 NullPointerException。既有非法资源路径、动画/外部 tab_icon 和大量 EMI 未翻译标签仍在，本批不把“能创建世界”写成“全部日志无错误”或完整配方/EMI兼容验收。

## 构建入口

新增 [build_verified.ps1](../../../tools/integration/build_verified.ps1)：默认串行自启动 NeoForge 和 Forge、创建隔离新世界；没有唯一成功回执、截图/物品/配方/帧数证据不齐、出现 FAILED/NullPointerException/意外服务器异常或 Gradle失败时，停止，不进入 assemble。两端通过后执行正式双版 assemble 与当前共享类/metadata/NOTICE验包。`-PreflightOnly` 只做运行检查，`-Gradle` 可指定 Gradle 8.8；本机已有缓存入口作为默认。常规 CI assemble 保持原有行为，GUI入口供本地交付使用。

脚本语法检查通过；最初检查发现字符串 `$platform:` 的 PowerShell 插值问题，已改为 `${platform}:`。本批直接调用同一 opt-in 游戏入口并验证两份回执后再构建，没有为了重复测试脚本再启动两端。

## 正式成品

成功回执验证后，正式 `:assemble :neoforge:assemble :neoforge:compileBootstrapGameTestJava` 9m15s 终态成功。共享资源本批未改变，沿用当前缓存并跳过 `:core:processResources`；未重复完整共享套件。新入口正常运行时不跳过该任务。

两包655个当前共享class、metadata/NOTICE、无重复ZIP条目、bootstrap夹具排除验包通过。每端两个改变原生类已核对：Forge记录重混淆后存在及哈希，Neo与当前编译输出逐字节一致；`WorldCreationSmoke` 没有进入成品。正式日志为 `work/world-creation-build-20261004.log`，原始运行日志与截图留在ignored构建目录，仓库记录哈希及回执，不复制整个日志。

[通用成品报告](world-creation-artifacts-20261004.json) 与 [实际世界、失败尝试及当前原生类报告](world-creation-content-20261004.json) 记录结果和边界。交付路径：`build/libs/gregtech6-1.20.1-forge-0.0.0.jar`、`neoforge/build/libs/gregtech6-neoforge-1.21.1-0.0.0.jar`。构建发生于源改动提交前；报告区分工作区构建与随后确认的源提交，不把文档提交当作实际构建提交。

## 边界

上述是项目开发环境新世界检查；实际 NeoForge21.1.252/Forge47.4.26/Java25 实例没有原地改动或复测。正式固定目标与本次开发运行均为 NeoForge21.1.243/Forge47.4.20。先前 Forge47.2.20 的正式版本标签误用了本地 API 源码缓存版本，已依据本批源提交的 gradle.properties 更正；Forge 开发依赖是 JEI，未加载 EMI。没有旧存档重载、完整生存配方链、世界迁移、其他模组组合或新的机器功能验证。来源对齐、占位电池及其他缺口继续推进，整体任务未完成。
