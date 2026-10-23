# Steam engine crafting parity / 2026-10-02

两版保留的 28 种普通/强化蒸汽活塞原来只有注册和运行，没有制作入口。本批依据本机原始 GT6 Loader_MultiTileEntities.java:584–612 的原图样，将 14 普通 + 14 强化配方译为共享 core JSON，现有 Forge 静态数据包与 Neo 原生版本路径适配读取同一份资源。没有改引擎效率、功率、压力、预热或断电行为，未新建能源底层。

图样都是 `PhP / SIS / PwP`：普通用四双板、两杆、一小弹簧；强化用四致密板、两杆、一弹簧。锤、扳手使用现有 GT 工具序列化器，损耗分别 400/800，返回工具不修改查询输入。原 MultiTileEntityRegistry 使用 CR.DEF_REV_NCC，无 MIR，保留 allow_mirror=false（该图样本身左右对称，不能据镜像图样匹配宣称检验了镜像拒绝）。

普通 Steel/W 是原 ANY 组，而非单一材料：十个共享配方专用标签分别保留 Steel/Knightmetal/MeteoricSteel 和 Tungsten/TungstenSintered 的五形态原成员。实际不存在的成员形态使用可选嵌套引用，实际运行仍严格要求配方候选非空；不以其他前缀、同色物品或整组金属替代。强铅的原 HBM 特殊精确堆栈分支不适用当前移植，采用原常规材料标签分支。原文件/每行/材料/键表达式、译后 SHA 与原 flags 证据都记录于 core/provenance/steam-engine-crafting-20261002.json，来源目录与许可未修改。

## 一批双版短运行

复用现有 ForgeDedicatedSmoke / NeoForgeDedicatedSmoke 与 NeoManualToolCheckpoint，engineCraftingSmoke 默认关闭，没有新增独立系统夹具。两平台的真实 RecipeManager 各检查全部 28 图样、实际注册结果、非空原料候选和工具剩余物；拒绝双/致密板退成普通板，以及普通/强化弹簧互换；青铜样例拒绝铜板替代，Native 两青铜序列化器真实网络往返保持匹配。该配方验证是方法调用，未称作玩家点击工作台。

每个平台在 prepare 将实际 assemble 的普通/强化青铜产物各一个写入识别 UUID 箱，普通服务器运行 200 tick 后自然保存停止；第二独立 JVM 的 verify 不创建/修复产物，读原箱、两产物及原物品/槽位，再运行 200 tick 保存退出。证明指定物品的真实世界重载，不证明已放置引擎全部机器状态、旧存档、玩家取得原料或完整生存。未在当前批再次启动客户端，先前 Native 真实 GUI 与冷启动蒸汽动力链证据仍独立保留。

集中双版生产/既有 bootstrap 编译：27s。

- neoforge prepare：1m 51s；PID 59892，实际退出 0，全部 28 入口、200 tick、正常保存停止，重载标志 False。
- neoforge verify：1m 58s；PID 29612，实际退出 0，全部 28 入口、200 tick、正常保存停止，重载标志 True。
- forge prepare：2m 18s；PID 80728，实际退出 0，全部 28 入口、200 tick、正常保存停止，重载标志 False。
- forge verify：2m 43s；PID 37368，实际退出 0，全部 28 入口、200 tick、正常保存停止，重载标志 True。

最终双 JAR 构建 3m 26s，573 当前共享 class、两平台元数据/关键玩法类/无重复和 bootstrap 污染检查通过，本批 28+10 资源全部原 SHA 在两包核对。共享静态目录总计 2174 配方、17 标签；另有既有动态/机器配方，不能据目录数声明所有玩法完成。

- gregtech6-1.20.1-forge-0.0.0.jar：38,582,714 字节，SHA-256 `a652fd1c80de31ff2add6367e7beac8cb67a4ea45b204386df486f69b2682bd1`。
- gregtech6-neoforge-1.21.1-0.0.0.jar：36,773,769 字节，SHA-256 `3d4c029bcbb1eb39f91c59b25793344c84be18a9307655417589e1e8509c5818`。

复现：新隔离本机 flat 服务器目录，既有同意的 eula=true 配置副本；根 Java17 Gradle launcher，Native 实际 Java21 工具链。串行 `-PdirectCoreResources=true -PdirectCoreClasspath=true -PengineCraftingSmoke=true -PserverSmokePhase=prepare|verify -PserverSmokeId=<同平台同 UUID> -PserverDirectory=build/<新隔离目录> -PserverSmokeX=0 -PserverSmokeY=240 -PserverSmokeZ=0 :runServer` 或 `:neoforge:runServer`。每个版本独立世界，不能将 1.21 世界交给 1.20。正式双包 `-PdirectCoreResources=true :distributionJar :neoforge:jar` 不使用 directCoreClasspath。

电力/通量/柴油引擎制作入口、真实玩家工作台、设备/材料可得性、Forge 对应动力链、成品安装、旧档与完整三源整合继续待验。整体移植仍未完成。
