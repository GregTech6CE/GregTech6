# 第四十二批：水体回归、原版堆叠放置、地牢板条箱与手工器皿

本批延续 GT6 原版行为核对，源码根目录为 `F:\Dev\GregTech6\gregtech6-master\gregtech6-master`。没有从 GTM 复制实现。仍主要通过 IDEA `runClient` 测试，因此本批不打包 jar。

## 水体交界

2026-09-23 的最近一次客户端日志没有加载 `gregtech.mixins.json`，其时原版水无法将 GT 海/河/沼泽水判为同一流体，正好解释一个方向的交界面接缝。2026-09-27 的 IDEA 配置已有该参数，但目前没有配置更新后的客户端日志，所以旧日志不能直接证明这次复现的成因。现在 `WaterFluidMixin` 的 `isSame` 注入要求命中；客户端 setup 实际调用原版水方法，确认注入代码运行，若缺失就以明确错误提示重新生成 IDEA 运行配置。客户端还核验六种静止/流动 GT 世界水确实被注册为透明流体层。

另修正 `GTWaterBodyFeature.convertColumn`：GT6 的 `WorldgenOcean`、`WorldgenRiver`、`WorldgenSwamp` 只把每列首个水格写为源水，后续格通过 `ExtendedBlockStorage.func_150818_a` 只换方块 ID，保留原元数据。移植版此前把整列流动/下落水都变成源水；现保留首格以下的 `LiquidBlock.LEVEL`，`WaterBodyTests.columnConversionMatchesGt6` 用 3/6/8 级水验证输出 0/6/8。此修正涉及多层流动水和瀑布，平坦满源海面的接缝仍须由实际客户端启动确认。细节见 `docs/WATER_MIXIN_IDEA_LAUNCH_2026-09-27.md`。

## 可放置堆、硬币与书架

- 按 GT6 `GT_Proxy.java:307-326` 补回手持锭、板、宝石板整叠潜行右键直接放置对应已填充堆的入口；检查放置许可、阻挡、创造/生存消耗和堆叠高度。六张堆贴图与原版字节一致，锭堆的 64 个模型小盒坐标逐一一致。新增 `PileShapeAndSyncTests` 定向覆盖。
- 硬币默认 32 行压印位图与 192 个像素体模型、四张贴图均对齐 GT6。书架 7 个框架部件、28 个书位的四向点击映射对齐；修正书架展示面被当成完整坚固面的问题，同时保留完整碰撞体，并在 `BookShelfGeometryTests` 覆盖四向。GT6 的大量材料书架变体尚未移植。

## 地牢存储与材料板条箱

GT6 `Loader_PrefixBlocks.java:55-67` 和 `DungeonChunkRoomStorage.java:135-183` 同时使用 16 件小板条箱与 64 件普通板条箱，此前移植版把两者都解析成 64 件。现在六种材料形态各有独立的 `crateGt*`/`crateGt64*` 前缀与材料重量，地牢按原版比例选取对应尺寸，创造栏按原版隐藏小板条箱。六组小板条箱复用 GT6 对应外观；资源生成器复跑不再无意义重写未变文件。板条箱采掘工具恢复撬棍，扫描器同步显示名称。

GT6 地牢锭/板堆的随机过程先分别生成两份材料与数量，再决定实际放锭或板；原移植版只抽一份候选，改变了种子结果。仓储室与工坊现共用按原版顺序抽取的实现，并增加定向测试。Minecraft 工作台与铁砧按 GT6 原版继续保留原版方块。

## 四种遗漏的手工器皿

补齐 32705 陶瓷混合碗桌、32707 钢浸洗盆桌、32720 木浸洗盆桌、32721 木浸洗盆。它们有实际加工、材质/容量限制、台座几何、原版纹理、合成与测试。木盆配方使用通用 `forge:long_rods/lead` 标签并禁止未获 GT6 允许的镜像。实现细节及当前占位判读边界见 `docs/REPAIR_BATCH42_VESSEL_TABLES_2026-09-27.md`。

## 新世界测试暴露的采掘标签

本批第一次新世界门禁暴露动态 `BlockLootPack` 没把岩层矿、原版型矿石、混凝土与 GT 石材的已有等级值写进 Minecraft 采掘等级标签。已把这些类型接到 `BlockHarvestPolicy.level`。多方块结构件现在按实际的镐/斧/扳手标签验证，旧测试仅接受镐/斧造成误报。旧复用世界曾掩盖这类缺口，因此本批每次回归使用独立新世界。

## 验证与边界

`compileJava` 已通过；`generate_block_assets.py` 二次运行报告小板条箱资源更新为 0；两轮新世界测试分别找出 4 项采掘/断言问题和 1 项配方镜像问题，均已针对性修正。最终全新世界 `build/gametest_water_batch42_green.log` 于 2026-09-27 14:11 完成：**942/942 项 GameTest 全部通过，`BUILD SUCCESSFUL in 8m 56s`，退出码 0**；日志确认 `WaterFluidMixin` 注入。`tools/audit_porting_progress.py` 已据此刷新 `docs/porting-progress.json`。另启动 Gradle `runClient` 到材质图集与声音引擎加载阶段，`build/runclient_water_batch42.log` 确认加载 GT Mixin 并注入 `WaterFluidMixin`，没有触发客户端水体校验异常；验证后主动停止客户端，因此该次 Gradle 退出码 1 不作构建失败解读。客户端水面、书架/堆外观以及新器皿的实际像素效果还需要 IDEA `runClient` 目视验收。本批未构建 jar，也没有执行 `git commit`；不宣称 GT6 全内容已完成。
