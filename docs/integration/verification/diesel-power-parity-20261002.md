# Diesel power / RU parity — 2026-10-02

修正两版继承移植的三项柴油机偏差：输出 KU 改回原 RU；完整燃料配方能量不截到两倍输出；排气由正面改到背面。原 MultiTileEntityMotorLiquid 每tick先尝试一额定RU包并无条件扣除，再于低于两包时补燃料，循环直至达到阈值，保留完整配方能量；未接收的功率原本就丢失。两版适配消费新的共享 LiquidFuelCycle 算术，默认其他引擎仍KU；柴油不接外部注能，不可额外拉取，避免一tick两次输出。消费真实燃料配方量并检查排气接纳，不硬编码一mB。精确额定RU/t提示已更新，本批未客户端目视确认。

## 同一隔离旧世界，三个真实专服进程

修复前生产class与交付d63e117f1旧包相同（两class实际字节SHA比对）。baseline供现有钢柴油机100mB、青铜旋转活塞、粉碎机注册钻石碎片和输出箱；另给孤立青铜柴油机1mB并在其真实燃烧后停供。200普通tick：青铜只留32而原配方64×8=512，API为KU，旋转/RU链不能启动。旧世界正常保存，实际库存/能量/排气/停止开关保存快照。

新实现第二独立进程的verify先从原磁盘读三机全部精确旧库存（青铜旧kuEnergy=32、钢64/99mB/1mB排气），不补造原状态；再只给预先空置第三柴油机一mB实体燃料，真实tick观察完整512能量，并自动把一mBCO2排到后方铜鼓。原100mB余料的钢柴油RU→青铜双极旋转活塞KU→实际注册钻石粉碎及输出箱完成，无注HU/RU/KU、无手动机器tick、无tick sprint。200tick正常保存，停新燃料/机器开关，保留选定残余能量/燃料/排气。

第三独立进程readOnly verify不供应燃料，先读第二次保存的完整精确数值，再验证实际产物/排气和200普通tick保存停止。原残余RU即使停止仍继续按原每tick一包消耗，不能把末状态不变当作验收。所给设备/原料不等于完整生存获取或全部机器状态覆盖。

- baseline：2m，PID 15704、200tick、实际退出0、正常保存停止。
- fixed：1m 49s，PID 78388、200tick、实际退出0、正常保存停止。
- reload：1m 52s，PID 17332、200tick、实际退出0、正常保存停止。

集中两版编译 44s，最终两包 3m 27s，574 当前共享class及关键玩法类/元数据/无重复或bootstrap污染通过；两包均含当前共享cycle和柴油输出类型方法。来源/改文件/旧class哈希和实际回执见同名JSON与core/provenance清单。

- gregtech6-1.20.1-forge-0.0.0.jar：38,595,714字节，SHA-256 `efe8696f18fe6876db79b2af1179ce3a7038da944ca68218f2104f5208e1cd5a`。
- gregtech6-neoforge-1.21.1-0.0.0.jar：36,786,813字节，SHA-256 `272a32d9f259871b6e43b56ea3633a32eed77d8638908b6b123b7aac020b634c`。

兼容范围：保留当前gregtech注册、燃料/排气tank、stopped和历史kuEnergy字段数值；柴油输出语义恢复RU，旧直接KU负载必须加RU→KU旋转活塞。只证明本次d63e117f1隔离Neo世界，不覆盖原1.7.10/其他来源或跨Minecraft存档。原固定输出min/max提示仍继承通用范围待对齐；动画活动三态、全部燃料/姿态/过压、Forge动力运行、客户端/成品安装和完整生存继续待验。

复现：baseline需d63e117f1生产源码加既有probe，`-PdirectCoreResources=true -PdirectCoreClasspath=true -PdieselPowerSmoke=true -PdieselPowerBaseline=true -PserverSmokePhase=prepare ... :neoforge:runServer`；新源码同世界`-PdieselPowerSmoke=true -PserverSmokePhase=verify`，第三次再加`-PdieselPowerReadOnly=true`。使用显式同UUID/新隔离directory及0,240,0坐标。Java17 Gradle、Native现有Java21；无第三方授权变化，整体移植仍未完成。
