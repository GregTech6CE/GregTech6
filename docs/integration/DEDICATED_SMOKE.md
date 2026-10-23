# 普通开发专服与真实世界重启探针

日期：2026-09-30。实现属于bootstrapGameTest开发源集，已接线、编译并串行运行两平台prepare/verify，实际读取重启后的标本通过。两平台只在明确的 `gregtech.integration.serverSmokePhase=prepare|verify` 开启；默认空字符串关闭。探针不进入生产jar，证据见VERIFICATION.md及verification/。

证据范围是普通DedicatedServer启动、200次真实服务器tick、正常停止路径，以及同一版本同一世界第二次启动恢复原版箱子/物品/方块。箱子标本为3个原版铜锭+1个原版铁锭，不是铜锡合金配方，不能称GT6机器、铜锡/青铜玩法、成品jar、跨版本世界迁移或旧存档兼容通过。

## 隔离运行与世界标本

Forge入口 `src/bootstrapGameTest/java/com/gregtech/gregtech/integration/server/ForgeDedicatedSmoke.java`，Neo入口 `neoforge/src/bootstrapGameTest/java/com/gregtech/gregtech/integration/server/NeoForgeDedicatedSmoke.java`。两者限定Dist.DEDICATED_SERVER并检查真实对象是DedicatedServer，拒绝GameTestServer。客户端探针仍限定Dist.CLIENT；此代码没有net.minecraft.client类型。

Root使用新的开发run目录与单独level-name，server.properties仅绑定127.0.0.1、有限独立端口，禁止把prepare指向存档。两个阶段使用同一平台、同一runDir、同一level-name、同一worldRoot和同一UUID，分别启动独立JVM；不要在prepare/verify之间删除或重建世界。两个版本使用各自的世界，不能让1.20.1和1.21.1共写一个世界。

游戏JVM必传 `gregtech.integration.serverSmokeId`，必须规范UUID字符串；prepare和verify相同。可选 `gregtech.integration.serverSmokeX/Y/Z` 必须三项同时给整数；缺省取overworld保存的出生点偏移(8,12,8)。坐标必须在世界边界和建筑高度内，east邻块为Dirt。准备位置及邻块必须都是空气，探针拒绝覆盖非空气；失败时改用新的测试世界或显式安全坐标，不能复用已经prepare的位置再prepare。

prepare真实加载区块、放置原版single chest及east Dirt、把UUID写进箱子CustomName、slot0放3 CopperIngot、slot1放1 IronIngot、调用setChanged。检查箱子方块/type、实体27槽、CustomName和全部库存；其余25槽必须为空。verify只读取真实已加载世界，不放置、不补写、不清空/修复箱子；要求同一UUID、同一箱子、同一Dirt、两槽数量和其余空槽全部吻合。

两阶段都在ServerStarted后累计200个服务器Post/END tick，再重复世界检查并halt(false)。watchdog从ServerStarted起120秒覆盖阶段执行和停止回执；默认不在探针订阅前初始化窗口计时，Root需对本次子进程另设启动与停止上限。超时/配置/状态异常立即记录唯一FAILED，再安排server线程正常halt，不调用System.exit、不杀全局java进程。主线程死锁或订阅类加载前失败仍由Root本次进程限时器处理。

## 停服、保存与回执门禁

只有收到正常路径的ServerStopping，随后ServerStopped，且确由200tick探针请求停止时才产生SUCCESS。停服后要求level.dat非空，并检查标本实际region文件的8192字节header及对应chunk有合法已分配sector。此区块分配检查不是NBT内容验证；完整保存重启证据必须有第二个JVM的verify读取匹配UUID、方块及库存。

单行JSON标记：

- `SERVER_SMOKE_STARTED`：platform、minecraft、phase、sessionId、pid、specimenId、serverClass、worldRoot、标本/Dirt坐标、elapsedMs、observedTicks。
- `SERVER_SMOKE_PREPARED`：prepare真实标本检查通过，尚未证明保存重启。
- `SERVER_SMOKE_WORLD_VERIFIED`：verify真实世界加载读取全部标本状态通过，restartWorldReadVerified=true。
- `SERVER_SMOKE_STOP_REQUESTED`：200tick后二次检查通过并请求正常halt。
- `SERVER_SMOKE_STOPPING`、`SERVER_SMOKE_STOPPED`：观察生命周期事件。
- `SERVER_SMOKE_SUCCESS`：上述身份与worldRoot/坐标/计时/tick，normalStopObserved=true、restartWorldReadVerified（prepare=false、verify=true）、levelDat/region绝对路径及文件字节数。
- `SERVER_SMOKE_FAILED`：身份、reason、error和堆栈；出现任何FAILED不得因为Gradle退出码0判通过。

Root验收prepare/verify的完整独立日志：两者各恰一STARTED/SUCCESS、无FAILED、观察到请求→Stopping→Stopped→Success顺序，observedTicks=200；平台/版本/UUID/worldRoot/坐标一致，sessionId不同并由两个真实JVM启动。两次实际命令均退出0且BUILD SUCCESSFUL；排除crash-report、Encountered an unexpected exception、Exception stopping the server、Failed to save等启动/保存异常。保存日志和region/level.dat校验和，verify日志应有WORLD_VERIFIED且SUCCESS restartWorldReadVerified=true。ServerStopped在框架finally里发生，单个事件不能单独保证没有保存错误，故必须保留异常日志门禁和真实重启读取。

## 已核对的本机API与EULA开发行为

Forge解析源码：`C:\Users\Asus\.gradle\caches\forge_gradle\minecraft_user_repo\net\minecraftforge\forge\1.20.1-47.4.20_mapped_official_1.20.1\forge-1.20.1-47.4.20_mapped_official_1.20.1-sources.jar`。

- `TickEvent.java:37–64` 的ServerTickEvent含phase/getServer；使用END一次计tick。
- `MinecraftServer.java:638,671–698`：initServer完成后Started；正常loop退出触发Stopping；finally执行stopServer后Stopped；`halt:620–629`只清running并可选join，故用false；`stopServer:549–590`保存与关闭区块；`isStopped:1175`、`getWorldPath:1690–1692`可用。
- `BaseContainerBlockEntity.java:45–59`：setCustomName/getCustomName；`ChestBlockEntity.java:61–82`：27槽、实际NBT load/saveAllItems；`RandomizableContainerBlockEntity.java:96–123`真实getItem/setItem；`Level.java:195–200`真实setBlock。
- `Eula.java:19` 条件为IDE||ForgeGameTestHooks.isGametestServer()||readFile；不能据此假定Forge普通开发专服绕过文件。当前Forge普通run实际生成eula=false并拒绝启动，首次prepare未通过。随后明确同意Minecraft EULA用于本项目隔离本地测试，只在对应目录写eula=true，重跑prepare和verify通过。探针本身不编辑EULA。

Neo解析源码：`F:\Dev\GregtTech6New\gregtech6\neoforge\build\moddev\artifacts\neoforge-21.1.243-sources.jar`。

- `ServerTickEvent.java:39–40,59–63`：Post/getServer；`MinecraftServer.java:674,724–751`：普通生命周期及finally/停止事件；`halt:657–666`、`stopServer:594–628`、`isStopped:1333`、`getWorldPath:1927–1929`。
- `BaseContainerBlockEntity.java:34–49,64,145–149`：实际CustomName加载/保存，1.21.1已没有Forge1.20.1的setCustomName。使用原版named Chest ItemStack的DataComponents.CUSTOM_NAME，再 `BlockEntity.applyComponentsFromItemStack:296–300`；其applyImplicitComponents写实体name。随后setItem并setChanged。`ChestBlockEntity.java:67–90`为27槽及新版带HolderLookup的NBT库存加载/保存，verify经过原版世界加载，不自行绕过版本NBT适配。
- `SharedConstants.java:121` 明确 `IS_RUNNING_IN_IDE = !net.neoforged.fml.loading.FMLLoader.isProduction()`；`Eula.java:20` 为IDE||GameTestHooks.isGametestServer()||readFile。非production开发模式直接短路，包括普通开发DedicatedServer，所以可以没有eula.txt而正常启动；不局限GameTestServer。`Main.java:103–105`仍正常构建Eula并检查hasAgreedToEULA。生产启动不能据此视作已授权，探针不创建、更改eula.txt或接受生产EULA。

## 已接线属性与串行命令

Forge普通 `minecraft.runs.server` 必须继续加载bootstrapGameTest开发源集，并将project properties转发游戏JVM：

```groovy
property 'gregtech.integration.serverSmokePhase', (project.findProperty('serverSmokePhase') ?: '').toString()
property 'gregtech.integration.serverSmokeId', (project.findProperty('serverSmokeId') ?: '').toString()
// 如果显式给坐标，三项均转发，不能默认为0。
['X', 'Y', 'Z'].each { axis ->
    def value = project.findProperty('serverSmoke' + axis)
    if (value != null) property 'gregtech.integration.serverSmoke' + axis, value.toString()
}
```

Neo普通 `neoForge.runs.server` 必须使用包含main/core/bootstrap的开发mod源集，属性用systemProperty和rootProject.findProperty；runDir/端口由Root统一配置，不在探针里修改：

```groovy
systemProperty 'gregtech.integration.serverSmokePhase', (rootProject.findProperty('serverSmokePhase') ?: '').toString()
systemProperty 'gregtech.integration.serverSmokeId', (rootProject.findProperty('serverSmokeId') ?: '').toString()
['X', 'Y', 'Z'].each { axis ->
    def value = rootProject.findProperty('serverSmoke' + axis)
    if (value != null) systemProperty 'gregtech.integration.serverSmoke' + axis, value.toString()
}
```

```powershell
.\gradlew.bat --no-daemon --console=plain :compileBootstrapGameTestJava :neoforge:compileBootstrapGameTestJava
$specimenUuid = [guid]::NewGuid().ToString()
$forgeRun = "build/persistence-forge-$specimenUuid"
# 先创建该新目录的server.properties，绑定127.0.0.1/独立端口、flat、view/simulation=2。
# 本次已明确接受EULA，允许在这个隔离本地测试目录写eula=true。
.\gradlew.bat --no-daemon --console=plain :runServer "-PserverDirectory=$forgeRun" -PserverSmokePhase=prepare "-PserverSmokeId=$specimenUuid"
.\gradlew.bat --no-daemon --console=plain :runServer "-PserverDirectory=$forgeRun" -PserverSmokePhase=verify "-PserverSmokeId=$specimenUuid"
# Neo用自己的独立世界和新UUID，分别保存两个阶段日志。
$neoSpecimenUuid = [guid]::NewGuid().ToString()
$neoRun = "build/persistence-neoforge-$neoSpecimenUuid"
# Neo相对目录基于neoforge模块：neoforge/build/persistence-neoforge-UUID。
.\gradlew.bat --no-daemon --console=plain :neoforge:runServer "-PserverDirectory=$neoRun" -PserverSmokePhase=prepare "-PserverSmokeId=$neoSpecimenUuid"
.\gradlew.bat --no-daemon --console=plain :neoforge:runServer "-PserverDirectory=$neoRun" -PserverSmokePhase=verify "-PserverSmokeId=$neoSpecimenUuid"
```

所有Gradle/游戏由Root串行启动；不能把GameTestServer命令当普通专服替代。bootstrap源码不进入成品jar，真实发布包启动与完整玩法/机器存档需要后续独立测试。

## 实际两阶段验收

Neo46s/44s，Forge2m35s/2m29s通过，均为不同JVM/同UUID/同world读取。Forge首跑EULA失败单独保留；随后同意后重跑，不把首跑当通过。完整原版库存、邻接方块、200tick、正常三维度保存和文件门禁已验，汇总见verification/p2-{forge,neoforge}-dedicated.json。

验收工具tools/integration/verify_dedicated_smoke.py必须传两个真实退出码、两个完整日志、期望平台/UUID/world绝对或仓内路径；不能只检查SUCCESS文本。示例使用此次Forge回执：

```powershell
python tools/integration/verify_dedicated_smoke.py --platform forge --prepare-log work/p2-forge-dedicated-prepare-authorized.log --verify-log work/p2-forge-dedicated-verify-authorized.log --prepare-exit-code 0 --verify-exit-code 0 --specimen-id 80e1a426-1a4f-41d7-af43-80fa179585b8 --world build/persistence-forge-80e1a426-1a4f-41d7-af43-80fa179585b8/world --output work/p2-forge-dedicated-verification.json
```

prepare后存盘文件会被verify正常保存更新，最终大小/哈希以verify后的文件为准，不强求两阶段level.dat/region字节相同。标本内容由新游戏JVM实际读取，工具对文件结构的校验是补充。
