# 第四十三批：水体共享面回归与 GT6 内容续补

本批以 `F:\Dev\GregTech6\gregtech6-master\gregtech6-master` 为 GT6 行为依据。当前主要在 IDEA 中运行 `runClient`，所以本批没有打包 jar，也没有执行 `git commit`。

## 原版水与 GT 世界水

`WaterFluidMixin` 仍覆盖原版静止／流动水的 `isSame`；GT 海水、河水、沼泽水两态由 `GTWorldWaterFluid` 覆盖反方向，六种流体在 `#minecraft:water` 中。客户端启动时已有主动调用 Mixin 与透明层检查。本批进一步在 `WaterFluidParityTests.vanillaWaterKnowsTheWorldWaters` 对六种 GT 水逐一调用原版水和 GT 水方块的 `skipRendering`，断言双方都隐藏交界共享面。该测试走 `LiquidBlock.skipRendering` 和 `GTWorldFluidBlock.skipRendering` 的实际入口，补上仅检查 `Fluid.isSame` 时漏掉的方块层回归点。

上批 `build/runclient_water_batch42.log` 证实 Gradle 客户端确实注入 `WaterFluidMixin` 并加载了材质图集，但在进入世界前主动停止。当前 `.idea/runConfigurations/runClient.xml` 已包含 `--mixin.config gregtech.mixins.json`；从那以后没有一份 IDEA 客户端进入世界的日志或截图，因此**不能仅凭 GameTest 声称实机看到的像素问题已消失**。请完整停止旧 Java 进程后重启 IDEA `runClient`，分别查看原版水与海水／河水／沼泽水的静水、流水交界。如果仍异常，需区分接缝、透明度、颜色或水下画面，并查看该次 `run/logs/debug.log` 的 Mixin 加载记录。

## 本批其它实现

- Mass Storage 按 GT6 加入胶带封存与剪刀／小刀拆封。封存方块保留整件库存，零头材料仍单独掉落；未封存破坏时掉落空外壳、整件库存与可表示的零头，避免复制。封存时停用正面按钮与物品能力；新增实际胶带点击、掉落和重放置 GameTest。
- 书架扩展为 15 种已有真实板材纹理的木材和 GT6 `metalset` 全部 60 种金属款，共 75 种。共用 28 格双面藏书，按原版修正书架光遮挡为 0；变体清单与可重生成模型见 `data/gregtech/bookshelf_variants.json` 和 `tools/generate_bookshelf_family.py`。
- 地牢花盆补齐 17 种 GT 矿物指示花，维持 GT6 抽样顺序；刷怪农场最多 8 个外围平台和连接管道按目标区块独立生成，跨区块写入受到测试约束。原版使用的 Minecraft 工作台和铁砧仍保留。
- GT6 32072 物流储罐成为独立方块实体，可无仓储总线直连物流网络；容量 1,000,000 L，抽空后保留流体过滤类型，补原版外观和合成。此批曾把 `NBT_CAPACITY_HU=100000` 误读为热容量；第四十四批核对原版基类后更正为 **100,000 K 熔毁温度**并已接入。

## 验证与后续

`compileJava` 通过。第一次全新世界门禁 `build/gametest_batch43_water.log` 发现两项新花盆测试准备错误：掉落上下文缺坐标／工具，连续种子同一状态使 `setBlock` 返回 false；均仅修改测试。最终独立新世界 `build/gametest_batch43_water_green.log` 在 2026-09-27 15:47:48 报告 **953/953 项 GameTest 全部通过**，`BUILD SUCCESSFUL in 8m 48s`，退出码 0，日志确认 `Mixing WaterFluidMixin`。书架 Python 资源测试 5/5，17 种地牢指示花资源检查通过，中英语言 JSON 可解析；资源审计检查 55,056 文件和 74,131 引用，GregTech 模型缺失 0、BOM 0。`docs/porting-progress.json` 已用该次日志刷新：1156 材料、74,230 物品、15,510 方块、925 流体、49 个缺失内容 token。

仍需 IDEA `runClient` 目视确认水面、书架材质与地牢花盆；书架第三方木材、更多储罐原版参数以及其它未完成的 GT6 内容继续列为后续移植工作。75 种已注册书架的生存配方与物流罐原版熔毁温度在第四十四批补齐。文件数和代码行数不是功能完成率。
