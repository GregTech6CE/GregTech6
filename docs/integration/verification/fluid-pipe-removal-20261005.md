# 双版流体管拆除与组件盖板

日期：2026-10-05。实际项目目录：`C:/Dev/GregTech6`。承接 #18 的组件盖板修复及 [流体分配批次](fluid-pipe-distribution-20261002.md)。

## 原版对照与改动

原 GT6 `MultiTileEntityPipeFluid.breakBlock:432–444` 先逐面检查连接及盖板的排出拦截，再逐通道转移，累计实际送达量，最后将残液交给 `GarbageGT.trash`。转移调用 `FL.move_:846`；垃圾系统定义见 `CS.GarbageGT:1419–1442`。原版泵盖板在管道上强制输入，且其 `interceptFluidDrain` 会阻止该面的普通排出。作者、LGPL-3.0-or-later 和四个只读来源的 SHA-256 见 [来源记录](fluid-pipe-removal-source-20261005.json)；既有 LICENSE/NOTICE 保留，没有新增素材或依赖。

两端之前的拆除分支漏过了组件盖板方向，未统计转移，也未将拒收的残液接入现有垃圾存储。本批修正：

- 先遍历面、后遍历通道；连接、现有流体过滤及共享组件方向规则共同控制输出。接收侧仍由原生面能力执行其实时限制。
- 在盖板掉落前执行排液，保留已有移除回调顺序。不会借拆除绕过强制输入的泵盖板。
- 只将实际从源罐扣除并送给邻居的量累加至传输统计，保留此前正常 tick 的统计。
- 拒收和容量不足的残液进入现有服务器级 `GarbageData`，源管道各通道清空；垃圾不计作正常输出。

两版复用现有 core `ComponentCoverRules`；平台只处理流体、能力、世界移除及 SavedData。注册 ID、NBT 字段、汉化和生产资源均未改变。新增结构模板仅属于测试 source set。

## 有限运行检查

只运行新命名空间 `gregtech_fluid_removal` 的三项场景，每平台各一次终态运行；没有重跑所有 GameTests 或未改动的完整 core 合同。

1. 六组实际方块移除：关闭源面、关闭接收面、源白名单过滤器、接收桶的输出泵、源管道的输入泵、双面开放。前五组均拒绝，开放组实际送达 600 mB。
2. 正常分配已送出 200 mB 后拆除；另一真实储液桶仅剩 150 mB 空间。统计累加为 350，余 250 进入垃圾存储。
3. 四通道管的水／熔岩遇到单流体桶：500 mB 水送达，200 mB 熔岩进入垃圾存储，各源通道清空，统计仅 500。

两端 `All 3 required tests passed`，Forge 14:52:34、NeoForge 14:54:08。Gradle 退出 0、成功 3m57s，两个服务端正常保存停止；日志 `work/fluid-removal-20261005-final-tests.log`。

停服后实际读取两端 `world/data/gregtech_garbage.dat`，各为水 3250 mB、熔岩 200 mB，与全部拒收样例总量一致。不是直接调用 load 的合成样本，也不代替独立进程存档重载。回执 `work/fluid-removal-saved-data-20261005.json`。

首轮失败仅因新命名空间未提供空结构模板，尚未执行场景；日志 `work/fluid-removal-20261005-tests.log` 和崩溃记录保留。补入两端已有空模板后终态通过，不把首轮算作玩法通过。

复现：现有 Java 17/21 工具链、本地 Gradle 8.8、`--offline --no-daemon -I work/emi-local.init.gradle -PdirectCoreResources=true -PdirectCoreClasspath=true -PgameTestHeap=3g -PgameTestJvmArgs=-XX:+UseSerialGC -PgameTestNamespaces=gregtech_fluid_removal -PgameTestDirectory=build/fluid-removal-final-20261005 -PneoforgeGameTestDirectory=build/fluid-removal-final-20261005 :runGameTestServer :neoforge:runGameTestServer`。正式打包不用 directCore 参数。

## 证据边界

这是实际开发服务端、真实注册设备和实际移除／存储检查；未验证自然生存取得设备、玩家计时挖掘、所有第三方接收器、大网络性能或旧存档重启。

继续使用既有垃圾系统政策：最多 256 类条目，流体总量在 Integer.MAX_VALUE 饱和。没有借本批声称恢复原版无界 long 垃圾池，或全部气体泄漏／阀门等垃圾调用者。超大遗留储量和完整垃圾系统仍是后续范围。

## 正式包与客户端

代码提交 `6cb3e166`。普通 `:assemble :neoforge:assemble :productionSmokeJar :neoforge:productionSmokeJar` 成功，1m15s、退出 0，没有 directCore 参数。703 个当前共享 class、Forge 1727 / NeoForge 1533 个当前原生 class、ZIP CRC、元数据、LICENSE/NOTICE、Forge 生产 refmap 及无重复／测试条目检查通过。包内两端汉化 JSON 与上一批原版补丁绑定结果逐字节一致，各 138456 键。

使用安装的 Forge 47.4.26 / NeoForge 21.1.252 及 Java 25，在本项目隔离目录加载实际交付包；各渲染主菜单 181 帧、正常退出 0，耗时 56.47s / 47.95s，截图已查看。继承物品图集启动检查通过；它不代替成品世界中的流体网络行为。整合包文件未修改。

| 平台 | 稳定目录中的文件 | 字节数 | SHA-256 |
| --- | --- | ---: | --- |
| Forge 1.20.1 | `build/verified/20261005-065817Z-6bb63436/gregtech6-1.20.1-forge-0.0.0.jar` | 41057314 | `6bb634363e7d186eee40caae6c6b2267d64f43acc1ddec99a10fe2b3b55c1f64` |
| NeoForge 1.21.1 | `build/verified/20261005-065817Z-6bb63436/gregtech6-neoforge-1.21.1-0.0.0.jar` | 39273451 | `6dd395dd3d881112d933cacb228483fed6f82d8cc07ceaee4b4412cb23aacbec` |

成对复制完成后一次发布本地稳定目录，两个副本的 CRC／字节数／哈希均通过。[本批实际回执](fluid-pipe-removal-20261005.json) 分开记录开发服务端、实际保存文件、普通构建、验包和正式成品主菜单证据。整体移植仍未完成；没有远程推送或 issue 操作。
