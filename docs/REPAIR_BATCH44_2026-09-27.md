# 第四十四批：GT 世界流体渲染与生存细节

本批以 `F:\Dev\GregTech6\gregtech6-master\gregtech6-master` 的 GT6 源码为依据。反馈原版水与 GregTech 水的渲染异常复发；目前没有该次 IDEA 游戏画面或明确的流体名称，因此客户端像素仍需实际进世界复核。

## 水体和世界流体

- `GTWorldWaterFluid.isWaterFamily` 直接识别原版静止／流动水和移植版海水、河水、沼泽水的六个状态，再通过 `#minecraft:water` 兼容其他模组。这样在客户端流体标签尚未同步或被数据包改写时，这三种 GT 水仍能与原版水双向合并。`GregTechClient.clientSetup` 在启动时逐一检查六种水双向 `isSame` 及透明层注册，配置缺失会直接报错。
- 原版 GT6 `BlockBaseFluid.getRenderBlockPass()==1` 适用于全部可放进世界的流体，不仅三种水。现在海／河／沼泽水、四种石油、天然气及地热泉水的静止／流动状态共 18 个流体都注册到 `RenderType.translucent()`；世界方块注册和客户端渲染共享同一份流体名单，防止增删时遗漏。地热泉水继续使用与 GT6 原图 SHA-256 一致的专用贴图及自身颜色，没有被改成原版水贴图或普通水流动物理。
- GT6 的地热泉水与普通水相邻时还具有独立的侧面消隐规则。当前只修复其错误的不透明层，未把地热水加入 `Fluid.isSame` 水族，以免错误改变源方块转换和上浮行为。若实机看到的仍是地热泉边界竖面或水面凹陷，需要针对客户端网格做窄范围修复。

## 同批独立修正

- 复核 GT6 `Loader_MultiTileEntities:2170` 与 `TileEntityBase08Barrel:66` 后确定，物流储罐的 `NBT_CAPACITY_HU=100000` 实际赋给 `mMeltingPoint`，单位是 K，并非 100000 HU 储能。移植版物流储罐现用 100000 K；所有储罐的灌装入口按实际流体温度拒收过热流体，服务端 tick 对 NBT 已存入的过热流体执行熔毁检查。新增高温氦等离子与木桶拒收熔岩 GameTest。
- 15 种木质和 60 种金属书架新增 GT6 对应的 `tool_shaped` 生存配方；生成器检查 75 条配方，游戏测试逐条核验输出、材料形态和工具返还。
- 普通／高级 Bumbliary 分别按 GT6 在半径 3／1 的已加载区域寻找蜜蜂物种所需的花、水、石材等工作场所；没有工作场所不再产梳。Galacticraft 缺席时不虚构其氧气规则。蜂刺、两条机器合成和部分第三方作物仍待补。
- 地牢工坊酒窖抽饮料改用独立随机流，不再消耗房间 `next(7)` 并使后续桶层数、容器和内容的抽签整体偏移。原版地牢所使用的 Minecraft 工作台和铁砧保留。

## 验证

`compileJava` 通过。独立新世界 `build/gametest_batch44_water.log` 于 2026-09-27 16:48:57 报告 **960/960 项 GameTest 全部通过**，Gradle `BUILD SUCCESSFUL in 9m 9s`，退出码 0；日志确认 `Mixing WaterFluidMixin`。75 条书架配方生成检查、4 项 Python 书架单测和模型资源审计通过：55,056 个文件、74,131 条引用、GregTech 模型缺失 0、BOM 0。`docs/porting-progress.json` 已据本次门禁刷新，注册量为 1,156 材料／74,230 物品／15,510 方块／925 流体，缺失内容 token 49。

`build/runclient_batch44_water.log` 证明 Gradle 客户端注入 `WaterFluidMixin`，通过启动期六种水双向互认及 18 个流体透明层检查，并加载到方块纹理图集／主菜单后台服务阶段；为避免留下占内存的游戏进程，随后主动停止。**该日志没有进入世界，不能代替在 IDEA `runClient` 中目视确认水面像素。** 请在同一世界分别看原版水与海／河／沼泽水交界，以及地热泉的透明度，并记录异常究竟是侧面、表面高度、颜色还是水下画面。

本批未构建 jar，也没有执行 `git commit`。
