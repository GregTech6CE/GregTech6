# 2026-10-03 材质、植物与蜂巢批次

工作目录 `C:\Dev\GregTech6`。Forge 1.20.1 / NeoForge 1.21.1；所有来源目录只读。

## 改动与提交

| 提交 | 行为 |
|---|---|
| aa1baeec | 材料方块自带 tooltip 与外来材料事件不再重复添加属性 |
| 927f4b14 | Neo 31 类、51 个旧 RGB 物品颜色回调转为不透明 ARGB；块颜色和原生流体颜色保留 |
| 5b48c62b | 16 种光莲和黑克斯百合使用睡莲的水面物品放置与碰撞 |
| 73cc80c1 | 灌木完整核心、六面枝条、类型/速度继承、支撑丢失掉落、成熟生成、棉花、计数与 Forge 物品着色 |
| 59c27bd3 | 16 色蜂巢独立方块/物品及旧状态迁移 |
| 56686b7f | 双客户端 tooltip、库存模型和 28 个灌木状态的实际几何检查及截图 |

蜂巢变种 ID 为 `gregtech:bumble_hive_<dye_color>`。新变种没有 `color` 属性，普通物品栈即可表示颜色；旧 `bumble_hive` 保留色彩属性仅用于兼容迁移。蜂群库存不是外观变种编码，仍保存蜂蜜、蜜蜂和基因，迁移保留九槽物品。

## 最终验证

- `:core:check`：四组共享合同合计 8,107 条断言通过。
- `:runGameTestServer :neoforge:runGameTestServer`：使用 `gregtech_issues` 命名空间，双版各 17 项通过并正常保存退出，3m22s。既有九项 Issues 回归与新增八项植物/蜂巢回归同跑。
- 光莲：17 个实际注册物品，通过原生 use 水源射线放置、单个消耗、薄片碰撞、发光值、流动水拒绝和船接触销毁。
- 灌木：真实 BlockItem 六面附着，原作 4/16 选框及 2/16 碰撞厚度，核心完整立方体；根莓种变化传给枝条、无土枝条取根速度、根无效土壤停止生长、根消失后枝条掉落、生成枝条成熟、去除顶端雪层。string 设置输出不消耗，采摘给 1–2 线并归零阶段；成熟当轮第二个增量仍保留。棉花加八种莓种进入世界生成池。
- 蜂巢：16 个独立 plain item、无变种状态属性、实体类型有效；对所有旧颜色执行注册的服务端 ticker，九槽内容逐槽保留。
- 双客户端：原生 `ItemStack.getTooltipLines` 同时经过自身 callback 和事件总线；56,036 个材料物品、12,127 个材料/石材方块、三种 vanilla 金属锭的普通和详细 tooltip 均只有一份材料段。
- 双客户端：16 种蜂巢库存模型有效，28 个灌木状态全部顶点范围与实际选框一致，截图中蜂巢、带 blueberry 数据的完整灌木和白/红/蓝光莲正常。Forge 另断言灌木物品读取蓝莓颜色。
- Neo：全部 56,036 个材料物品的被绘制颜色层不透明；323 种细导线都有 base/inner-ring overlay；四种电动工具都有四层且颜色不透明；37 种手动工具的真实与占位栈共 74 模型通过，包括木柄；既有 JEI 工具槽和三种泉库存模型回归通过。
- 正式 `:build :neoforge:build` 没有使用 directCore 的开发捷径，4m32s 成功；[验包回执](visual-plants-artifacts-20261003.json) 核对两包 589 个当前共享 class、元数据、许可/NOTICE、无重复及无测试条目。

最终日志在忽略的 `work/plant-variants-gametest-final-20261003.log`、`work/visual-plants-{forge,neoforge}-client-final-20261003.log`、`work/visual-plants-production-build-20261003.log`。客户端机器回执见 [Forge](visual-plants-forge-client-20261003.json)、[NeoForge](visual-plants-neoforge-client-20261003.json)；截图查看结果另记于 [汇总回执](visual-plants-runtime-20261003.json)，不把标题截图的 gameplay 标志改成 true。

## 重试与范围

初轮夹具有 AABB、ticker 静态类型以及 Neo mock player/受保护 API 的编译差异，已修正；这两次编译失败不计通过。初轮 Forge 标题 smoke 进入了实际世界/JEI 页面，未取得标题截图而 300 秒超时，即使 Gradle 退出 0 也不计通过；后续两次独立运行均成功。前一轮 16 项世界回归成功后，补棉花与成熟计数，最终 17 项重新通过。

本批不是成品安装启动、自然生存链或独立旧世界重启验收。旧蜂巢迁移是实际世界实体/ticker 的证据，未在旧存档或独立 JVM 加载上验证。原作氧气、Aether 附魔草加速、HarvestCraft 棉花兼容、枝条雪覆盖外观及其他灌木边界继续留在移植范围；不声明 GT6 所有第三方行为完整复刻。原生成池基于 HashMap，本项目保留确定性的九类顺序，不声明与原 JVM 的 HashMap 遍历或旧坐标类型一致。

## 来源

原作者 Gregorius Techneticies；沿用现有 NOTICE 和许可。以下读取自原版快照，无文件复制或来源目录写入；蜂巢纹理使用本工程已整合资源。

| 原 GT6 文件 | SHA-256 |
|---|---|
| `gregtech/tileentity/plants/MultiTileEntityBush.java` | b56bed74378f647c66d98e4b241da47c838114165088ac020a952bf258bf890a |
| `gregtech/worldgen/WorldgenBushes.java` | e1a1f5f3262f9a00c900485ca44b8b3843b9a5def54ba8fa4b8d39c934e60a7d |
| `gregapi/data/CS.java` | 407edce9f541bcf34b6573d59e4ae14e477fb9c145439deddb831e2a2e2887a5 |

实际 source 根为 `C:\Dev\gregtech6-master\gregtech6-master\src\main\java`。材料提示/ARGB 修正是对当前端口平台 API 的修正；睡莲直接复用当前 Minecraft/loader 的原生实现。整体移植没有在本批结束。
