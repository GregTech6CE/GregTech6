# Electric / flux engine crafting parity — 2026-10-02

补齐现有五档电力活塞、五档通量活塞的制作入口，共享 core JSON 经 Forge 原路径、Neo 原生资源包读取，没有新增机器注册或能源后端。

原始 GT6 Loader_MultiTileEntities.java:833–844：电力图样 `PhP / CIC / PwP`，四三重板、两导线、一磁性长杆与锤/扳手。LV/MV 导线为原 ANY.Cu 01/02，HV/EV/IV 为退火铜 04/08/16；长杆依次磁铁/磁钢/磁钢/磁钕/磁钕。工具损耗 400/800，原 DEF_REV_NCC 无镜像标志。原 LV 行含 `TODO: Temp Recipes`，保留来源临时配方性质。通量图样 `G / M / G`，两相应材料大齿轮，中间原注册号 10011..10015 是同级电力活塞，**不是通量电动机**；保留这一依赖且实际消耗前一个 assemble 产物。

复用既有 NeoManualToolCheckpoint/NeoForgeDedicatedSmoke，新增默认关闭开关 electricFluxCraftingSmoke。单次普通 Neo 专服检查十条真实 RecipeManager 图样、产物和原料候选，十次实际网络序列化往返；全部档位电力制作产物进入通量制作查询，拒绝错级前置、错线宽、双板替代三重板、非磁性长杆，高档拒绝普通铜线，低两档验证退火铜等价成员，工具返回损耗且查询输入不变。原料由检查提供，这不是玩家工作台或完整生存。200 tick 正常保存停止；该批没有引擎方块放置/供能/产物重载，前批双版蒸汽物品重载保持独立范围。

集中双版编译 18s；Native 单次普通专服 1m 58s，PID 78948，退出 0。最终双包构建 47s，573 当前共享 class、元数据/关键类/无重复或检查污染验包，以及十新 JSON 原 SHA 在两包核对通过。共享静态目录 2184 条，不以配方数声明完整整合。

- gregtech6-1.20.1-forge-0.0.0.jar：38,587,007 字节，SHA-256 `29fa2b044fecef0d557de161396b8e9a5b0044541be064c1fb0c89600dbc4117`。
- gregtech6-neoforge-1.21.1-0.0.0.jar：36,778,062 字节，SHA-256 `61ce5c4aedf0fb9452a4d5c4e3173818cfb1c3f34a4ceb74064185896618358a`。

复现：Java17 Gradle launcher，Native 使用现有 Java21 工具链。新的本机 flat/offline/loopback 隔离目录，既有获同意的 eula=true 副本；`-PdirectCoreResources=true -PdirectCoreClasspath=true -PelectricFluxCraftingSmoke=true -PserverSmokePhase=prepare -PserverSmokeId=<新 UUID> -PserverDirectory=build/<新隔离目录> -PserverSmokeX=0 -PserverSmokeY=240 -PserverSmokeZ=0 :neoforge:runServer`。双包 `-PdirectCoreResources=true :distributionJar :neoforge:jar`。源码/原始行/键/SHA、实际进程回执和限制见同名 .json 与 core/provenance 清单。

当前新增十行未在 Forge 运行，Forge 动力玩法、玩家取得原料、成品安装、柴油入口、客户端供能与旧档继续待验。隔离 offline 专服记录 Mojang public-key 请求超时，未阻止启动/配方/tick/正常保存；保留原日志。第三方来源、许可未修改，整体移植仍未完成。
