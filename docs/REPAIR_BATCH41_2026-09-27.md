# 第四十一批：IDEA 水体 Mixin、物流过滤与物流量子存储器、地牢手册

本批按 `F:\Dev\GregTech6\gregtech6-master\gregtech6-master` 的 GT6 源码核对行为和数据。GTM 未作为代码来源。使用 IDEA `runClient` 测试，因此没有构建 jar。

## 水面渲染回归

最近一次 IDEA 客户端日志里既没有注册 `gregtech.mixins.json`，也没有注入 `WaterFluidMixin`；本机 `.idea/runConfigurations/runClient.xml` 的程序参数缺少 `--mixin.config gregtech.mixins.json`。已重新运行 Gradle `genIntellijRuns`，三个 IDEA 运行配置现都带这个参数。`build.gradle` 已配置该 Mixin，重新生成时会保持。`runClient` 需完整重启才能使用新参数。完整诊断和复测位置见 [WATER_MIXIN_IDEA_LAUNCH_2026-09-27.md](WATER_MIXIN_IDEA_LAUNCH_2026-09-27.md)。服务器 GameTest 可证明 Mixin 加载与水体同族逻辑，客户端水面最终像素效果仍需 IDEA 实测。

## GT6 物流

- 普通 Filter、Prefix Filter 被普通物流总线指向时提供半过滤优先级，白名单样品会阻止对应物品进入 Dump；黑名单和未选前缀不声明半过滤。Prefix Filter 按注册物品形态前缀匹配，不能把同材料的锭、板等混成一个集合。普通物流总线指向物品半过滤器时不误走流体通道。依据原版 `MultiTileEntityFilterPrefix.java:124-126` 与 `MultiTileEntityLogisticsCore.java:278-281,401-407,478-483`。
- 新增 GT6 `6200+aID` 的 60 种物流量子存储器。每种有独立注册方块、物品与生存配方，按原版材质列表映射，使用固定黑色机壳、原版十张贴图和青色数字；满仓显示红色 `100%`。中文名按原版 `GregTech.lang` 的数字 ID 精确关联，而不是按列表下标猜测。配方 `TQT / wCd / TMT` 使用对应普通仓、通用物流存储总线、至少四级电路、材料螺丝以及扳手和螺丝刀；原版出处 `Loader_MultiTileEntities.java:131-142,186-245`。
- 物流仓使用独立 BlockEntityType，避免区块重新载入后退化成普通仓；它继承 1,000,000 件容量、材料形态换算、操作及保留内容的方块掉落。六面直接作为网络节点，无需贴存储覆盖板。空仓为 Generic，有模板后为 Semi，流体优先级为 0。物品路由同时可向其存入、从中取出，并在 Dump 阶段保留模板家族。六十种均加入创造栏。普通仓与物流仓还共同恢复 GT6 的扳手采掘和配方材料对应的采掘等级；黑色物流外观不再误决定工具等级。资源生成器 `tools/generate_logistics_mass_storage_assets.py --check` 核对 131 个文件。

## 地牢和材料手册

- 根据 GT6 `Loader_Books.java:570-596` 补回动态《元素周期》与《合金之书》。元素书依据已注册元素的原子数、相变温度和密度生成；合金书依据已移植的坩埚反应生成。因 1.20.1 原版书名最多 32 字符，合金书在物品 NBT 中使用短标题，原版完整标题仍保留在数据目录。
- 修正 16 种气态元素和另外 39 处元素原子数据/标记；`gt.books` 战利品表恢复原版 17 条。地牢工坊书架固定槽 0、1 放动态元素与合金书，其余六本手册在槽 2–7，槽 8、9 继续放两份胶带。GT6 原版使用的 Minecraft 工作台和铁砧仍保留。

## 边界

物流罐尚未接成专用网络存储节点；普通 Mass Storage 的非包装模式破坏掉落与 GT6 仍有差异。元素/合金手册只展示当前已经移植且可注册的数据，不能据此宣称 GT6 的配方全集齐全。新物流仓的物品与世界外观、水体交界处以及书本页在客户端的排版仍需 `runClient` 目视验收。

## 静态核验与回归说明

`compileJava` 全量编译通过；`generate_logistics_mass_storage_assets.py --check` 核对 131 份资源；`sync_gt6_element_atomic_data.py --check` 为 0 处原子数据差异；书籍生成器 `transpile_gt6_books.py --check` 核对 18 本固定文本手册的两个输出文件且不改其修改时间。新增的 60 个物流仓配方、物流双向收发、保存重载、原版水 Mixin、前缀过滤及地牢八本手册均有 GameTest。

首轮全量 `build/gametest_batch41.log` 的 937 项中有 1 项前缀保留测试假红：箱子直接保存测试传入的可变 `ItemStack`，路由抽空后原测试变量的 `getItem()` 变成 AIR，错误地拿它与成功送达的物品比较。入箱时改用副本后，新世界 `build/gametest_batch43.log` **937/937 通过**。再补普通仓／物流仓的扳手采掘和一项实际采掘测试，新世界 `build/gametest_batch44.log` **938/938 通过**。之后发现书籍生成器之前的 `--check` 形同普通写入；已改成真正只读并且只在内容有变化时更新文件，以免无意义地触发重编译。

最终干净门禁 `build/gametest_batch45.log` 于 2026-09-27 12:35:34 **938/938 项通过**，Gradle `BUILD SUCCESSFUL in 8m 54s`、退出码 0，`compileJava` 和 `processResources` 均为 `UP-TO-DATE`。同一日志中可见 `Registering mixin config: gregtech.mixins.json` 和 `Mixing WaterFluidMixin ... WaterFluid`，证实测试启动链加载了水体 Mixin；IDEA 客户端渲染仍需重新启动 `runClient` 后目视检查。本批未构建 jar，未运行客户端视觉验收。
