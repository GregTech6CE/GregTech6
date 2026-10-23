# IDEA runClient 水面渲染回归

2026-09-27 核对了 2026-09-23 最近一次 IDEA 客户端启动日志 `run/logs/debug.log`：
启动参数没有 `--mixin.config gregtech.mixins.json`，日志中也没有
`Registering mixin config: gregtech.mixins.json` 或 `Mixing WaterFluidMixin`。
当时 `.idea/runConfigurations/runClient.xml` 的 `PROGRAM_PARAMETERS` 同样缺少该参数。
因此原版 `WaterFluid.isSame` 没有被 `WaterFluidMixin` 扩展，GT 水与原版水交界处的
“原版水看 GT 水”方向又会出现渲染接缝。Gradle 的 GameTest 启动参数包含此配置，
所以它的水家族测试通过并不能证明 IDEA 运行配置正确。

已执行 `./gradlew --offline --no-daemon --console=plain genIntellijRuns`；任务成功后，
本机 `.idea/runConfigurations/runClient.xml`、`runServer.xml` 和
`runGameTestServer.xml` 的程序参数都含有
`--mixin.config gregtech.mixins.json`。这些 IDE 配置在 `.gitignore` 中，不应该提交
其中的本机绝对路径；项目的 `build.gradle` 已有 `mixin { config 'gregtech.mixins.json' }`，
重新生成时会自动写入参数。

以后若在 IDEA 中看到同样的交界处问题，先检查 `runClient` 的程序参数是否包含该配置，
然后重新运行 `genIntellijRuns` 并重启客户端。新日志应出现
`Registering mixin config: gregtech.mixins.json`；详细 `debug.log` 还应出现
`Mixing WaterFluidMixin ... WaterFluid`。游戏内在海水、河水、沼泽水与
`minecraft:water` 的交界处各检查静水和流动水，确认水面与侧面没有台阶或缝隙。

第四十一批的首次排查只修复了 IDEA 启动配置，当时未改水体 Java 逻辑和材质。上述日志证明此前
Mixin 未加载，重新生成后的参数检查证明配置已补齐；最终像素效果仍需
在重新启动的 IDEA 客户端中核对。

最终新世界 GameTest 日志 `build/gametest_batch45.log` 明确记录
`Registering mixin config: gregtech.mixins.json` 与
`Mixing WaterFluidMixin ... WaterFluid`，并且 938/938 项通过。它验证了生成后的
测试启动链；IDEA `runClient` 的参数也已核对包含同一配置。客户端画面仍未目视验收。

## 再次报告后的防回归处理

再次报告交界渲染异常时，项目中最新的客户端 `run/logs/latest.log` 仍是
2026-09-23 的旧启动记录；它确实缺少 Mixin 参数，但不能代表 2026-09-27 修正后的
`runClient`。当前 `.idea/runConfigurations/runClient.xml` 已带参数，jar manifest 也有
`MixinConfigs: gregtech.mixins.json`。因此不能仅凭旧日志宣称当前视觉问题已消失。

`WaterFluidMixin` 的关键 `isSame` 注入现要求必须命中；客户端 setup 另会实际调用
原版水的该方法并核验注入标记。若 IDEA 再次漏加载 Mixin，启动会明确指出
`gregtech.mixins.json` 未激活及 `genIntellijRuns` 修复命令，而不会让客户端带着
无声的水面接缝进入游戏。服务端既有 `vanillaWaterKnowsTheWorldWaters` GameTest
仍检查两方向的流体同族关系。

另核对了 GT6 原版三个水体世界生成器：每列首个水格写成源水，后续格只换方块 ID，
原有流动/下落水的元数据保留。移植版此前把整列都重置为源水，现已改成只重置首格，
并在 `WaterBodyTests.columnConversionMatchesGt6` 固定输入等级 3/6/8，验证输出为
0/6/8。这能修复多层流动水或瀑布处的状态偏差，但不能解释平坦满源水海面接缝。
平坦交界若在重新启动的客户端仍异常，需要先查看那次新日志里 Mixin 是否实际注入、
再区分交界面缝隙与颜色/透明层差异。

最终全新世界 `build/gametest_water_batch42_green.log` 记录了
`Mixing WaterFluidMixin ... WaterFluid`，**942/942 项 GameTest 通过**，退出码 0。
它证明测试启动链与服务器侧流体同族测试通过；IDEA 客户端的实际渲染结果仍需
完全停止旧进程后重新运行 `runClient`，在原版水与三种 GT 水的静水、流水交界处目视核对。

随后本机启动了一次 Gradle `runClient`，见 `build/runclient_water_batch42.log`：
14:22:37 注册 `gregtech.mixins.json`，14:22:39 将 `WaterFluidMixin` 注入 `WaterFluid`，
14:23:45 到达材质图集与声音引擎加载阶段，未触发新的客户端校验异常。加载链验证后主动停止进程，
故该次 `runClient` 的退出码 1 代表中断，不代表编译或启动异常。此检查没有进入世界观察像素；
IDEA 自己的 `runClient` 及实际水面交界仍待人工验收。

## 第四十三批补充

`WaterFluidParityTests.vanillaWaterKnowsTheWorldWaters` 现对海水、河水、沼泽水的静止和流动状态逐一调用原版水与 GT 方块的 `skipRendering`，验证两侧都隐藏交界共享面。独立新世界 `build/gametest_batch43_water_green.log` 中 953/953 项测试通过，日志确认 `WaterFluidMixin` 注入。它覆盖代码级共享面判定，仍不等于 IDEA 世界内的像素验收。若重启 `runClient` 后仍看到异常，请以那次新日志和具体表现为准继续排查，不能拿 2026-09-23 的旧日志或仅凭服务器测试推断客户端画面。

## 第四十四批补充

三种 GT 世界水的静止／流动类和原版水现在通过类与实例直接互认；`#minecraft:water` 只作为其他模组水的兼容后备。客户端 setup 不再只验证 Mixin 被调用，还逐个验证六种 GT 水与原版水双向 `isSame`。GT6 的地热泉水原本也是透明世界流体，但先前落到 Forge 默认实心层；九种世界流体共 18 个源／流动态现都登记为透明层，地热水仍用自己的原版贴图与颜色。

最终独立新世界 `build/gametest_batch44_water.log` 为 **960/960 项通过**；Gradle `runClient` 日志 `build/runclient_batch44_water.log` 证实 Mixin 注入并加载方块材质图集，随后主动停止。尚无 IDEA 进入世界后的像素证据。请优先确认 F3 指向的是海水、河水、沼泽水还是地热泉水，并区分交界侧面、水面高度、颜色、透明度或水下画面。地热泉与原版水的专用侧面消隐还需要按具体表现决定客户端渲染修法；不应把地热泉简单并入普通水的 `Fluid.isSame`，那会改变 GT6 的流动物理。
