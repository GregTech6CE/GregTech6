# 2026-10-02：共享引擎侧面与实际蒸汽供能链

起点 `4914a00244da2096aa5362987c650108b61f97c3`。继续优先对齐两平台；保留原始效率、预热、压力、功率、保存身份及配方，不引入新来源的独立能源底层。

## 实现取舍

两版保留的 `EngineBaseBlockEntity` 用一套转换判断能力入口、另一套转换寻找输出邻居。水平方向 LEFT/RIGHT 的正反转换不一致；朝上/下时，反转换把 TOP/BOTTOM 仍映射到世界 UP/DOWN，与 FRONT/BACK 重叠。蒸汽活塞寻找冷凝水接收端时会走错侧面。

新增共享纯 Java `EngineFaceRotation`，保留已有能力查询的六朝向约定，输出方向取其严格逆映射。两平台同一类的两个转换方法委托该规则，移除两份重复开关表。未改变 FaceConfig 保存位、入口身份或设备注册；已有错误输出方向得到纠正。纯规则与两版代码来源/前后 SHA 见 `core/provenance/engine-steam-parity-20261002.json`。原作者与许可保留，没有新增三家底层拼接。

## 一次集中编译与既有普通专服检查

双平台生产编译和既有 NeoForge bootstrap 编译通过，54s，日志 `work/engine-face-compile-20261002.log`。复用 `NeoForgeDedicatedSmoke`，增加默认关闭的 `steamChainSmoke`，不建立新的检查工程或独立 fixture 类。

给定设备、64 煤炭、4000mB 锅炉蒸馏水及 50000mB 储备水、真实 Diamond gemChipped 输入；通过真实右键事件把煤炭放入、用打火石点火并发生一次耐久损耗。锅炉和活塞均从零热量/零 KU/空蒸汽开始：

- 致密青铜燃烧箱 → 强化青铜锅炉 → 中型钢流体管 → 强化青铜蒸汽活塞 → 相邻青铜粉碎机 → 下方原版箱子。
- 水鼓自动向下输出，通过两根钢管从侧面给锅炉补水。
- 由服务器执行实际世界 tick；没有直接注入 HU/KU、设置能量缓冲或调用机器处理 tick。使用原生 `tick sprint 12000` 加速调度，压缩预热等待；不把其耗时当作正常游戏墙钟时间或性能基准。
- 同一检查额外给朝上的普通青铜活塞 200mB 蒸汽，从真实 DOWN 能力入口进入；实际 tick 后 NORTH 邻接鼓得到 1mB 蒸馏水，覆盖本批竖直反向映射修复。
- 最终按已注册的实际粉碎配方核对全部确定性输出，输入已消耗、作业完成、燃料消耗、灰产生、水储备减少；未通过的情形不会只因 Gradle 返回 0 而记为通过。

无玩家的隔离世界显式保持设备区块 ticking。仅监听本机，未修改玩家世界或 `.minecraft` 配置。原版箱子中的 specimen UUID 继续用来区分真实世界。

## 保留的失败反馈

首次准备直接调用设备内部交互，漏过打火石的右键事件：在点火阶段失败。补正实际事件路径后，初次运行没有保留 ticking 区块，锅炉仅煮掉少量水；补正隔离世界的真实区块票据。接着普通青铜锅炉仅提供低流量，强化活塞不能持续给粉碎机供能，实际作业未完成。使用原有匹配功率的强化锅炉、致密燃烧箱及自动补水后继续运行。以上均保留日志并标记失败，没有降低机器能耗/预热/断电重置要求去满足检查。

## 范围与后续

最终 prepare 用时 1m 55s、PID 80632，运行 12000 个普通世界 tick；随后同一世界 verify 用时 1m 49s、PID 43172，读取后再运行 200 个普通 tick。两 JVM 的 sessionId 不同、世界路径/UUID 相同；verify 未创建或修复设备，检查原实际配方结果和竖直排水内容，并有 `restartWorldReadVerified=true`、正常保存退出的成功回执。只统计这些成功回执，前三次 Gradle 返回 0 但实际反馈失败的检查均不计通过。

复现：Java 17 运行根 Gradle，Java 21 用于 NeoForge；使用新隔离目录的本机 flat 开发服务器配置。

```powershell
./gradlew.bat --console=plain -PdirectCoreResources=true -PdirectCoreClasspath=true -PserverSmokePhase=prepare -PserverSmokeId=7215bc34-71ac-464b-a350-22b592e9f2a5 -PsteamChainSmoke=true -PserverDirectory=build/steam-chain-supply-20261002 -PserverSmokeX=0 -PserverSmokeY=240 -PserverSmokeZ=0 :neoforge:runServer
./gradlew.bat --console=plain -PdirectCoreResources=true -PdirectCoreClasspath=true -PserverSmokePhase=verify -PserverSmokeId=7215bc34-71ac-464b-a350-22b592e9f2a5 -PsteamChainSmoke=true -PserverDirectory=build/steam-chain-supply-20261002 -PserverSmokeX=0 -PserverSmokeY=240 -PserverSmokeZ=0 :neoforge:runServer
```

prepare 的坐标必须全为空气，不能在已保存的本次世界上重复 prepare；重载只运行 verify。开关默认关闭，正式 CI/模组 jar 不包含 bootstrap 检查类。

证明范围是上述给定设备、原料的 NeoForge 蒸汽动力链、竖直排水与其同一世界重启结果。源代码中的设备制作配方和原材料获取仍需逐项补齐可达性；例如 Forge `smeltery_survival` JSON 尚未全部接入 Native 1.21 单数 recipe 路径，蒸汽活塞的制作入口也需核实。不能把设备给定的检查称为完整生存玩法。

尚未证明 Forge 对应运行、正常墙钟速率客户端体验、所有引擎朝向的邻居交互、发电机/电缆、多方块、完整热量/燃料/压力精确快照、旧存档或成品安装；保留既有保存键不等于旧存档兼容。整体移植仍未完成。

构建和独立 JVM 结果、日志 SHA、产物 SHA 记录于同名 JSON；当前进度和实现选择同步进入 STATUS / DECISIONS。

## 最终构建

最终双 JAR 构建通过，用时 3m 49s；572 个当前编译 core class 两版字节一致、平台入口与关键玩法类齐全、名称/ID/0.0.0 正确、无重复或 bootstrap 专用内容。成品安装运行尚未验证。

- `gregtech6-1.20.1-forge-0.0.0.jar`：38,530,001 字节，SHA-256 `793124653a8e6532444ed95b3d9f0e49ce7a704373443ef8380da06b4c53a840`。
- `gregtech6-neoforge-1.21.1-0.0.0.jar`：35,892,913 字节，SHA-256 `ec45f3ba6941bb765171d0367a8b0e3790246887a158002325d6af7cd486a415`。
