# 2026-10-04 / 双版本启动崩溃

反馈 NeoForge 1.21.1 报 `Neo machine recipes initialized twice`，随后附上 Forge 1.20.1 启动日志。本批优先修复启动，暂缓太阳能板和占位电池；整体移植仍未完成。

## 实际故障链

- Forge 附件显示 `ModelEvent.ModifyBakingResult → SmelteryClientModels.aliasHullBlocks → MoldItemBakedModel.grid → FaceBakery`，纹理 sprite 为 null。游戏实例为 Forge 47.4.26 / Java 25.0.2。
- 从附件中已知实例目录读取同机 NeoForge 21.1.252 日志和崩溃报告，确认首次错误在 12:30:13：`TextureAtlas.getSprite → MoldItemBakedModel.grid` 报 `Tried to lookup sprite, but atlas is not initialized`。
- 同一 Neo 日志显示 12:30:25.992 公共初始化首次成功；12:30:26.000 客户端因模型加载错误清除选中资源包并重试，随后公共初始化在 deferred queue 再次执行，触发实机看到的二次配方初始化异常。因此两个版本有相同的模具模型根因，Neo 的二次初始化是资源重试后的后续错误。
- 没有复制整个日志或登录参数到仓库。仅与错误有关的摘录保存在 ignored `work/user-neo-startup-excerpt-20261004.txt`。

## 修正

1. 两平台模具物品包装器在本轮模型烘焙时，从已烘焙的 metallic 模具模板取得基础 tinted quad 的 sprite，构造器显式接收该 sprite；形状变体继续使用相同的本轮 sprite。默认与变体均不访问全局 Minecraft/ModelManager/TextureAtlas。模板缺省时使用当前模具模板，凹槽位图、几何、着色、32项形状缓存保持。
2. Neo 的整个公共初始化与机器配方入口分别使用 `SetupOnce`：串行调用、成功后重复调用直接返回、递归调用诊断、首次异常缓存并原样重抛，禁止失败后的部分注册再次执行。不会用“已经初始化”覆盖第一次失败，也不会在失败后宣称初始化成功。
3. 两平台材料模型的额外注册按 `GTItems.hasBoundItems(prefix)` 筛选。最近添加的外部兼容前缀没有 GT 自有物品和模板，不再要求烘焙不存在的共享模型。没有补假贴图或新增材料物品。

Forge/NeoForge 的固定依赖 `ModelEvent.ModifyBakingResult` 文档都说明该事件在 worker thread 执行，不可读取尚未完成的 ModelManager；本修正只读取事件的当前模型表。

## 集中验证

- 一次正式双版本 assemble 和 `:core:compileTestJava`，7m14s 成功；日志 `work/startup-fixes-build-20261004.log`。未重新执行全套共享检查。
- `SetupOnceContracts` 独立执行，10项检查通过，覆盖顺序重复、8线程并发、首次普通异常、类加载 Error、失败后的重复与递归。此夹具接入未来 coreBehaviorContracts，不打入成品。
- 材料模型目录轻量探针：107个实际绑定前缀、4,387个所需共享模板，缺失0。首次探针因没有绑定平台 ModData 存在性而失败；补上探针独立平台初始化顺序后通过，没有据此修改生产材料定义。
- [成品验包](startup-fixes-artifacts-20261004.json)：653个当前共享 class 与 loader metadata/NOTICE 正确；没有重复条目或测试夹具。
- [原生内容回执](startup-fixes-content-20261004.json)：Forge五个变更原生 class 在重混淆后存在，NeoForge七个变更原生 class 字节等于当前编译输出。两包 MoldItemBakedModel.class 均没有 `net/minecraft/client/Minecraft` 引用；Neo 配方入口不再含旧二次初始化错误文案。

## 运行边界

NeoForge 21.1.243 / Java 21.0.7 搭配 EMI 1.1.24 与 Jade 的开发运行环境只启动一次，成功完成模型加载并到达实际渲染的 TitleScreen，自动截图及退出；CLIENT_SMOKE_SUCCESS 显示105,693ms、151帧、1280×720，Gradle 2m57s终态成功。日志 `work/startup-fixes-neo-client-20261004.log`。这验证当前编译输出的开发客户端，不把它称作 NeoForge 21.1.252 / Java25生产实例的复测。本批没有 Forge 新客户端启动、GameTest、世界/存档/机器/EMI重载实测。实际运行的 Forge/NeoForge 小版本和 Java 版本见上文，构建固定目标仍为 Forge 47.2.20 / NeoForge 21.1.243。

Neo 原日志还有既有非法路径、动画帧及外部前缀 tab_icon 的警告，本批不宣称全量警告清零。太阳能板生成条件/活动贴图、占位电池和更广移植缺口继续接续。
