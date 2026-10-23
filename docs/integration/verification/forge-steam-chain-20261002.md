# Forge natural steam chain and exact saved thermal state — 2026-10-02

Forge 1.20.1 的致密青铜燃烧箱→强化青铜锅炉→中型钢蒸汽管→强化青铜蒸汽活塞→青铜粉碎机→原版箱子，在零热量/空蒸汽/零KU冷启动下完成实际已注册钻石碎片粉碎，并自动把全部产物送进箱子。真实右键加煤和打火石点火/耐久损耗一次，铜鼓自动供水经两根钢管进入锅炉。Forge 无 tick sprint，按真实正常世界速度预热，在加工完成后立即保存停止，未注HU/KU、设能量缓冲或手动处理机器tick。另置朝上青铜活塞由DOWN实际入口给200mB蒸汽，NORTH鼓得到1mB蒸馏冷凝水。

复用现有 ForgeDedicatedSmoke 和 Neo 已通过的选定链布局；只增加默认关闭的 steamChainSmoke 和实际磁盘状态读取，不增加检查工程或新的 fixture 类。生产代码和资源未修改，因此没有重复构建两包；当前 4a8f7b246 的双版交付 JAR 实际 SHA 与打包记录一致。

## 世界重载

准备进程在停止前记录11个实际block entity的完整持久化NBT：燃烧箱、锅炉、蒸汽管、强活塞、粉碎机、输出箱、两水管、自动供水鼓、竖直活塞、冷凝水鼓。正常关闭保存真实region。独立第二JVM从同一真实世界加载，先逐项对比block身份和完整NBT语义（TagParser/CompoundTag.equals），没有直接调用load或补造状态；验证原箱中产物与冷凝水后再运行200普通tick，正常保存退出。该证据覆盖持久化的热量/冷却/效率/压力库存、燃料灰、缓冲和管内容等，未保存的动画计时不扩大声明。

- prepare：6m 50s，PID 69008，5559普通tick，实际退出0、正常保存停止。
- verify：2m 40s，PID 79680，200普通tick，实际退出0、正常保存停止。

检查入口编译19s；双包仍为生产构建`4a8f7b246f54b5e430b8070fba7ce0ff16f166e3`，574共享class。

- Given machines, 64 coal, 4000mB boiler distilled water plus 50000mB supply and registered chipped-diamond feed; not full survival acquisition/tool crafting.
- Selected dense-bronze box/strong bronze boiler/steel pipes/strong bronze engine/bronze crusher and vertical bronze condensate path only.
- Actual exact persisted NBT before first reload tick covers selected heat/cooldown/efficiency/tanks/engine buffer and pressure state/fuel/ash/pipe inventory/temperature/transfer/sides/tank automation/output; volatile unsaved animation clocks are not claimed.
- Current local Forge world; no original GT6, other-source or cross-Minecraft save import. After reload normal machines continue changing state.
- No current Forge client powered-world or installed-JAR launch; full integration remains active. Existing Neo steam evidence retains its earlier snapshot/sprint scope.

复现：Java17 Gradle，显式同UUID和新的flat/offline/loopback隔离目录，既有获同意EULA副本。`-PdirectCoreResources=true -PdirectCoreClasspath=true -PsteamChainSmoke=true -PserverSmokePhase=prepare|verify -PserverSmokeId=<同UUID> -PserverDirectory=build/<新隔离目录> -PserverSmokeX=0 -PserverSmokeY=240 -PserverSmokeZ=0 :runServer`。prepare最多12000普通tick/660s，实际产物出箱后且至少200tick即停；verify固定200tick。第三方许可未改，整体移植仍未完成。
