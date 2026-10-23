# Forge diesel power and fixed rated RU packets — 2026-10-02

两版柴油引擎输出尺寸 min / recommended / max 现均为原额定 RU，而非通用一半至两倍范围；其他能量类型为零。依据原始 MultiTileEntityMotorLiquid 229–231 行；前批每 tick 一包、完整燃料能量、后方排气和旧数值保存语义不变。仅复用既有 ForgeDedicatedSmoke / NeoForgeDedicatedSmoke，不增加测试类或注册后端。

## 实际动力与独立进程重载

Forge 1.20.1 新隔离世界给定实际钢柴油机 100mB、青铜旋转活塞、青铜粉碎机一块注册钻石碎片，产物自动进入真实下方箱子。两个另置青铜柴油机各一 mB，观察真实单次燃烧 512 能量，并确认其中一台把 1mB CO2 自动排到后方青铜鼓。没有有效外部 HU/RU/KU 注能、手动机器 tick 或 tick sprint。200 普通世界 tick 后停止供新燃料及加工，正常保存退出。

第二个 Forge JVM 不供应任何燃料或新产物，先比较前一进程保存的三台引擎能量、燃料、排气和 stopped 全部精确数值，再确认箱中产物及鼓中 1mB CO2。三个实际引擎检查 RU 尺寸范围、KU 范围为零和拒绝外部 KU/RU 注能，继续 200 普通 tick 并正常保存退出。停止开关不保留残余功率，其随原每 tick 一包语义自然消耗，不误称全部状态不变。

Neo 1.21.1 对前批同一真实隔离柴油世界再次独立 JVM 重载，读原保存值、箱中产物和鼓中排气，执行新增精确额定范围检查，200 普通 tick 正常保存退出；没有补燃料或替换原状态。此前修复前 KU/32 与修复后 RU/512 的实际差分证据保留在 diesel-power-parity-20261002.md。

- forge_prepare：2m 24s，PID 75524，实际退出 0，200 普通 tick，正常保存停止。
- forge_reload：2m 42s，PID 13356，实际退出 0，200 普通 tick，正常保存停止。
- neo_rate_reload：1m 57s，PID 81096，实际退出 0，200 普通 tick，正常保存停止。

双版集中编译 29s，最终双包 3m 13s，574 共享 class 精确一致和生产柴油固定尺寸 class 验包通过。

- Physical Forge chain uses supplied registered machines, 100mB Diesel and one chipped-diamond input; no full survival acquisition or actual crafting UI.
- Selected steel/bronze horizontal machines prove this layout only; not all fuels/tier/orientations/activity animations/overvoltage.
- Selected Forge current-source world is saved/reloaded, not the earlier pre-RU Forge layout, original GT6 or cross-Minecraft/source save import.
- Client powered-world/GUI, installed production-JAR launch, Forge thermal steam chain and complete donor integration remain pending.

复现：Java17 Gradle，Neo 使用现有 Java21 工具链；Forge 新 flat/offline/loopback 隔离目录和既有获同意的 EULA 副本。`-PdirectCoreResources=true -PdirectCoreClasspath=true -PdieselPowerSmoke=true -PserverSmokePhase=prepare|verify -PserverSmokeId=<同UUID> -PserverDirectory=build/<新隔离目录> -PserverSmokeX=0 -PserverSmokeY=240 -PserverSmokeZ=0 :runServer`。Neo 同前批世界用 `verify` 加 `-PdieselPowerReadOnly=true :neoforge:runServer`。正式双包不加 directCoreClasspath。第三方许可未改，整体移植仍未完成。
