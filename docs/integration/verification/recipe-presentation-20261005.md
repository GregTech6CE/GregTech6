# 2026-10-05 原版配方浏览器显示规则

承接持续移植以及 Assembler 对应机器反馈；整体移植仍未完成，本批仅本地提交。原版 `RM.Assembler` 是旧兼容/辅助配方池，`Loader_MultiTileEntities` 没有独立 Assembler 注册机器，`NEI_GT_API_Config` 也不显示该池。因此保留实际数据和查询入口，恢复原版隐藏规则，不新增占位机器。依据及原始作者/许可/来源 SHA-256 见[来源记录](recipe-presentation-source-20261005.json)。

共享85机器池加8个现有燃料池接回原版浏览器允许标志和电压/电流提示标志；原版额外的 `FM.Furnace` 不属于当前8燃料池，本批不声明补齐。默认隐藏 autocrafting、blastfurnace、vacuumfreezer、assembler、cncmachine、chisel 六池。非零功率的消耗/产出总量仍显示；使用率、等级和功率按原版 `NEI_RecipeMap.drawExtras` 分支决定。零功率的信息页只保留实际时间等适用文本。两版 JEI 和 EMI 共用纯显示判断；游戏配方查询和已有特殊坩埚/Fusion 显示规则保留。

实际视觉核对发现 EMI 会把146高的机器页裁至当前可用高度，固定坐标导致耗能/时间文字缺失。两版现按 `WidgetHolder` 真正提供的区域统一缩放图案、文字和原生槽，槽仍是 `SlotWidget`，保留输出上下文、原料查询、快捷键和tooltip。实际 Neo 浏览器检查可见槽的边界在当前区域内及每格tooltip；缩放后的功率/时间文字截图完整。没有把字体缩放替换为另一套耗能文字。

JEI 与 EMI 同装时，机器配方由原生 EMI 注册，JEI 不再次向桥接层投放同一批行；独立工具装配的 JEI 配方仍注册。运行时按每个实际配方池逐项核对允许显示且启用/未隐藏的行数，并确认 EMI 行全部是原生 `MachineEmiRecipe`。

一次共享检查及双端编译28秒、初次两端探针编译9秒通过，现有共享合同合计10493断言，其中独立源表固定93池标志及显示边界。按减少测试要求，最终接受两次受影响的新世界：Forge 独立 JEI，NeoForge JEI+EMI。两端各93原生标志/6隐藏池通过；原生 Assembler 数据仍有1100行，chisel有8行，其余四隐藏池当前为空。Forge显示 74 类/208955 行；Neo EMI显示 74 类/208946 行、JEI机器行0。实际 Mortar 功率页和 DidYouKnow 信息页截图已查看，信息页没有无意义的电压/功率字段。两个接受的新世界正常保存停止、实际退出0；不是完整生存或独立存档重启验收。

此前 Neo 世界虽然配方注册检查和退出0通过，截图存在裁切，明确拒绝视觉验收；同期6分25秒中间构建不交付。隔离探针初次错误引用仅运行时存在的 EMI 内部类，API编译失败17秒，改为经实际实例反射读取并使用公开WidgetHolder/SlotWidget，未放宽产品依赖。随后探针误把 EMI 外部工作台栏的零尺寸容器算入机器槽边界，3分29秒世界标记失败（Gradle退出0不能代替成功标志）；按真实 MachineEmiRecipe 实例限定检查后再核对界面。仅重跑受影响 Neo 世界，Forge JEI入口未改动、未重复成功世界。最终普通构建核对两版更新后的全部原生类，Forge EMI缩放尚无本批实际世界截图；早期日志/截图在完整回执中保留，不当作最终视觉证据。

普通双版打包 10m 49s，698共享class和当前全部原生class逐字节、ZIP CRC、元数据及 Forge 生产 Mixin 映射通过，探针不进成品。两包各62书籍资源、28原生灌木模型、6贴图和2语言文件匹配当前源文件；彩色书籍阅读/绑定/染色/复制沿用[独立书籍验收](coke-books-20261005.md)，本批不冒充重新执行。新世界之后仅补两个大小成书标签的中英文译名，使用 EMI 支持的 `tag.item.gregtech.written_books_small/large`，最终两语言文件字节核对，不再重复低影响文字修改的世界。正式成品分别用已安装 Forge47.4.26/NeoForge21.1.252、Java25在隔离目录启动到标题画面并退出0，截图已查看，不改游戏实例。

原版 Assembler 印刷页 NBT/外部自动化兼容、其他机器配方池完整输入输出契约、高阶电池盒依赖、覆盖板/面板、完整生存、专服、真实存档重启及旧世界迁移仍继续保留。Forge 同装和 Neo 独立浏览器未在本批重跑。EMI最终加载状态和精确行数通过，截图仍可见 Baking recipes 进度字样，本批没有声明解决所有EMI延迟/进度显示。既有外部形态图标、源资源路径、JEMI标签及原版重复行诊断没有隐藏；只处理机器池的重复桥接。完整终态、日志/截图/成品哈希见[机器回执](recipe-presentation-20261005.json)。

复现世界：Java21、现有离线Gradle8.8，`-I work/emi-local.init.gradle -PdirectCoreClasspath=true -PdirectCoreResources=true -PclientCreateWorldSmoke=true -PclientSmoke=false -PclientSmokeTimeout=600 -PclientSmokeHeap=4g -PrecipePresentationRuntimeOnly=true :runClient`；Neo增加`-PemiSmoke=true`并使用`:neoforge:runClient`。正式打包不带 directCore 参数。

成对验收包：`C:\Dev\GregTech6\build\verified\20261005-035620Z-aa04bd97`。

- Forge：`gregtech6-1.20.1-forge-0.0.0.jar`，`aa04bd979a425ddfe07e331df6f42305129d480582cddbb49e0ba91cb636e173`。
- NeoForge：`gregtech6-neoforge-1.21.1-0.0.0.jar`，`d31cbba168afe599fe4d19d0a7c22fccf7a6ed840591fdf3311dc29e56f10732`。
