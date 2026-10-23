# 2026-10-02：恢复后玩法与双平台熔炼修复

当前目录 `F:\Dev\GregtTech6New\GregTech6`。起点提交 `499150156795d2ea97c6730bcd247007e68ee433`。遵循速度优先与双版本对齐要求，复用既有青铜流程和普通服务器检查；没有新增独立子系统测试工程。名称、加载 ID、版本号和第三方许可未更改。

## 修复及选择

两版保留 saltnya 的模具/盆/龙头/交叉流道实现，同步修正已记录的原始缺陷：

- 叠放模具每次只消费形状所需的完整物质量，盆中余料及其温度保留；不足一次铸造时拒绝输出。物质量判定放在共享 `MoldCastingRules.castingAmount`，两版使用同一规则。
- 叠放模具以盆的实际温度判定凝固和过热，避免环境冷就立刻取出熔融金属。
- 盆的完整金属块输出要求至少 9U；找不到已注册输出时保留内容，不再自动清空。拆掉有余料的盆时，按已注册的锭/粉及小份粉输出，避免把余料变成完整 9U 方块。
- 龙头按实际世界最低高度向下查找，支持 Y=0 与负高度；保留 16 层上限。
- 交叉流道使用 `finally` 释放本轮全局锁，保留原本的防环路和红石行为。

这些是对保留实现的整合修复，没有引入另外一套材料、注册或物流底层。原作者、来源文件原始 SHA-256、修改前后哈希与许可证据引用见 `core/provenance/smeltery-parity-20261002.json`。三个来源目录未改动。

## 实际结果

第一次 NeoForge 玩法检查在凿刻断言失败。恢复后的完整模具通过 `RightClickBlock` 事件处理交互，旧检查直接调用方块而绕过事件。修正既有检查的点击入口，使其发出真实 NeoForge 事件；不改生产事件处理、不放宽工具磨损和守恒断言。只改变该入口的复跑通过，定位了失败原因。

随后集中执行两版编译和同一个 NeoForge 玩法检查，2m 16s 通过：

- 实际注册的燃烧箱消耗煤炭并产生灰，给坩埚供热；真实铜锡物品实体被吸入，1357K 时产生恰好 4U 青铜。
- 15 次凿刻形成 1U 锭模；四次取出四个青铜锭，钳子磨损 4；空模再次点击不增产。
- 同一检查追加叠放盆检查：给定 9U 青铜，高温取出被拒绝；调用盆的实际冷却 tick 后，九次右键逐次产出九锭，每次保留全部余料，空盆不增产。

日志 `work/smeltery-parity-20261002.log` 同时包含 `GT6_NEO_PLAYFLOW_ALLOY`、`GT6_NEO_PLAYFLOW_COMPLETE`、`GT6_NEO_STACKED_BASIN_COMPLETE`、`All 1 required tests passed` 和构建成功。前两次定位日志为 `work/neo-playflow-20261002.log` 与 `work/neo-playflow-events-20261002.log`。

普通 NeoForge `DedicatedServer` 使用隔离世界运行 prepare/verify，各 200 个普通服务器 tick、正常保存退出，分别 1m 53s / 1m 55s；两个独立 JVM 的 PID 为 4728 / 65580，sessionId 不同，世界路径和 specimenId 相同。verify 不创建或修复样本，实际从磁盘读取：

- 原版箱子的 UUID、27 槽内容和相邻方块；
- 铜模具的材料、1U 内容及 1U 凿刻形状；
- 青铜盆的 8U 余料及空的完整方块输出槽。

世界：`neoforge/build/world-restart-20261002/world`；日志：`work/neo-world-prepare-20261002.log`、`work/neo-world-verify-20261002.log`。两份 `SERVER_SMOKE_SUCCESS` 记录及日志 SHA-256 保存在同名 JSON 验证回执。此处是新开发世界的真实重启，不是同进程 NBT 往返，也不是旧存档迁移证据。

复现已有检查：

```powershell
./gradlew.bat --console=plain -PdirectCoreResources=true -PdirectCoreClasspath=true -PgameTestNamespaces=gregtech_playflow -PneoforgeGameTestDirectory=build/playflow-20261002 :compileJava :neoforge:runGameTestServer

# 使用全新的隔离目录，第一次 prepare；同一目录、UUID 和坐标再次 verify。
./gradlew.bat --console=plain -PdirectCoreResources=true -PdirectCoreClasspath=true -PserverSmokePhase=prepare -PserverSmokeId=eb3764a6-ab5c-4275-a30b-c2bb1aa311ce -PsmelteryWorldSmoke=true -PserverDirectory=build/world-restart-20261002 -PserverSmokeX=0 -PserverSmokeY=240 -PserverSmokeZ=0 :neoforge:runServer
./gradlew.bat --console=plain -PdirectCoreResources=true -PdirectCoreClasspath=true -PserverSmokePhase=verify -PserverSmokeId=eb3764a6-ab5c-4275-a30b-c2bb1aa311ce -PsmelteryWorldSmoke=true -PserverDirectory=build/world-restart-20261002 -PserverSmokeX=0 -PserverSmokeY=240 -PserverSmokeZ=0 :neoforge:runServer
```

两个 direct 选项仅用于本地加速，正常双版 CI 不依赖它们。服务器隔离目录配置只监听本机；未修改 Minecraft 配置或写入生产 EULA 接受文件。

## 范围和后续

本批最终 `:distributionJar :neoforge:jar` 构建 3m 51s 通过。两个 JAR 的 571 个当前共享 core class 字节一致，平台身份、元数据、无重复/测试内容打包检查通过；Forge 38,528,484 字节，NeoForge 35,891,355 字节。文件 SHA-256 纳入 JSON 回执与聊天交付目录的 `SHA256SUMS.txt`。这仍是开发源集运行证据加成品打包证据，未称成品安装运行已通过。

设备和原料由检查提供，不是完整生存获取证据。叠放盆检查直接填充材料并调用冷却 tick，不把它说成完整的九锭煤炭冶炼流程。重启证明仅覆盖箱子、模具和盆的上述字段，不覆盖全部机器、热量/进度/燃料或跨版本迁移。

龙头负高度、交叉流道异常恢复、缺失输出材料、拆除余料的全材料/分数单位覆盖仍没有对应游戏运行实证。小于最小可用注册形态的任意旧 NBT 余数，以及没有可用小份形态的材料，拆除仍存在恢复缺口；不声明所有材料/异常存档完全守恒。现有目标凝固材料 identity fallback、龙头自动模式的交互入口、39 材质设备生存取得范围继续列为缺口。

Forge 此批编译通过，未重跑对应玩法、客户端与存档重启；两版成品 JAR 安装启动、当前改动的世界内视觉、全生存链和旧存档仍待验。当前只是一个完成的整合批次，整体移植仍未完成。
