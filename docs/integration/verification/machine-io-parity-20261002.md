# 2026-10-02：基础机器库存、搬运与配方存档

起点 `dfd16e227d6107d44a78b83b4725f6e6e87467fa`，当前目录 `F:\Dev\GregtTech6New\GregTech6`。保留全部现有规格、机器注册、配方图、保存键及能源包语义，优先双版本对齐；没有导入新的第三方子系统或建立新的测试工程。

## 六个生产文件的同步修复

两版 `BasicMachineBlockEntity` 的实例库存回调现在标记机器/区块待保存并通知配方检查，修复原本为空的 `onContentsChanged`。机器自动输出和机械臂调用现有共享 `ItemPipeTransfer`，只扣除接收端实际确认的数量。自动输入和传送带处理实际未接收余量，尝试放回原库存；无法放回时在机器旁掉出，避免静默丢弃。

泵/自动流体输入复制完整流体再调整数量，保留 Forge NBT / NeoForge components；自动流体输出只根据实际填充量扣除本机库存。两版 `FluidTankGT` 的混合和按指定流体排出必须匹配完整流体身份及其附加数据，避免同一流体类型的不同数据被混合或错误排出。

两版 `CoverUtilityBehaviors` 的取物覆盖物复用同一个确认余量的搬运引擎，处理回送/掉落，并按实际交付数量记账。压力阀根据实际填充量扣除储罐内容。覆盖物节奏、过滤、容量、机器侧面配置和配方规格保持原有约定。

实现基础来自 saltnya 的保留机器/覆盖物；复用的共享搬运引擎来自此前已经合入的 masson 余量合同。原始 SHA、修改前后哈希、作者与许可引用见 `core/provenance/machine-io-parity-20261002.json`，旧来源记录继续保留。未更改三份来源或第三方许可，未引入第二套库存/配方/物流注册。

## 集中检查与真实世界重启

第一批四个生产文件的双版编译通过，59s。日志 `work/machine-io-compile-20261002.log`。覆盖物余量修复在随后最终双 JAR 构建中编译，前一个运行证据不覆盖这两个覆盖物路径。

复用既有普通专服 prepare/verify 检查，加入显式关闭默认值的 `machineWorldSmoke` 开关。两次各 200 个普通服务器 tick，分别 1m 59s / 1m 51s，正常保存退出。PID 69044 / 61524、sessionId 不同，世界路径和 specimenId 相同；verify 不重建样本：

- 实际注册的 `crusher_bronze` 投入 Diamond `gemChipped`，由实际配方图选配方；通过后侧输入两包 32KU，调用真实机器 tick，已消耗输入、累积 64 工作量并产生待完成输出，然后暂停并保存。
- 第二 JVM 从同一世界读出原作业进度、已消耗输入与 `gt.pending_outputs`；恢复供能并调用真实处理 tick，完成原作业，输出数量与实际注册配方一致。
- 实际 `mixer_bronze` 的暂停状态、三件带 UUID 名称的铜锭和 1000mB 带 UUID components 的水从磁盘恢复；不同 components 的水不能混入或按该身份排出，拒绝操作没有改变数量。
- 原版箱子的保存 UUID/槽位与相邻方块也照常核对。

`work/neo-machine-world-prepare-20261002.log`、`work/neo-machine-world-verify-20261002.log` 的成功回执及 SHA-256 纳入同名 JSON。verify 包含 `MACHINE_WORLD_JOB_COMPLETED` 和 `SERVER_SMOKE_SUCCESS`，后者明确 `restartWorldReadVerified=true`、`machineWorldChecked=true`、`machineJobCompleted=true`。世界保留在 `neoforge/build/machine-world-20261002/world`。

复现（首次 prepare 必须选择全新隔离目录和空位置）：

```powershell
./gradlew.bat --console=plain -PdirectCoreResources=true -PdirectCoreClasspath=true -PserverSmokePhase=prepare -PserverSmokeId=cb23b5a6-244c-4102-bce8-0b817bc7f390 -PmachineWorldSmoke=true -PserverDirectory=build/machine-world-20261002 -PserverSmokeX=0 -PserverSmokeY=240 -PserverSmokeZ=0 :neoforge:runServer
./gradlew.bat --console=plain -PdirectCoreResources=true -PdirectCoreClasspath=true -PserverSmokePhase=verify -PserverSmokeId=cb23b5a6-244c-4102-bce8-0b817bc7f390 -PmachineWorldSmoke=true -PserverDirectory=build/machine-world-20261002 -PserverSmokeX=0 -PserverSmokeY=240 -PserverSmokeZ=0 :neoforge:runServer
```

隔离开发世界只监听本机。未修改`.minecraft` 配置，未写入生产 EULA 接受文件。正常 CI 不依赖这些本地 direct 选项。

## 验证边界

设备、输入和 KU 由检查提供，处理 tick 在准备/恢复检查中直接调用；没有证明完整生存取得、发电机/线路持续供能、全部配方/机器或客户端界面。实际世界重启覆盖上述粉碎机作业与搅拌机状态，不能代替 Forge 对应运行、旧存档、成品 JAR 安装或多方块/随机产物/流体作业的完整验收。

动态接收端的所有物品/流体余量、回送掉落、覆盖物、能力替换/重入/卸载尚未做相应场景的实际运行。违反处理器合同、在抛异常前发生不可观测副作用的第三方接收端，不作事务保证。输入流体在源排出后本机被重入修改、物品源在目标确认后被重入修改等边界仍需后续整合；此批不是所有传输场景的守恒完成声明。

整体移植仍未完成，后续继续生存可达性、平台运行、渲染与接口、各系统保存和兼容范围。

## 最终双版本产物

最终构建 `:distributionJar :neoforge:jar` 用时 4m 3s，通过；覆盖全部六个生产修改文件。随后验包通过：571 个当前编译共享 core class 在两版字节一致，平台入口与关键玩法类齐全，名称/ID/0.0.0 正确，无重复或检查专用类。验包不代表成品安装运行。

- `gregtech6-1.20.1-forge-0.0.0.jar`：38,530,445 字节；SHA-256 `3762e9527625b54a7333e0ec252feb4834463d756e36bac0cd5903294e28a905`。
- `gregtech6-neoforge-1.21.1-0.0.0.jar`：35,893,375 字节；SHA-256 `c54737376ed8e1d8f6827c3ba5344a6560586ced95794920c484f05cb2f37be7`。
