# NeoForge early workbench tool and copper/tin harvest — 2026-10-02

NeoForge 1.21.1 在生存模式服务器 FakePlayer 下实际右键拾取三个燧石石块和树枝，用所得原料填入原版 CraftingMenu，点击真正的结果槽得到 Flint 镐头/Wood 手柄的 GT 镐，九个输入槽全部消耗。随后用该成品经过 ServerPlayerGameMode.destroyBlock 采掉三个普通铜矿、一个普通锡矿，获得实际矿块生成的 ItemEntity：3 铜原矿、1 锡原矿；镐的损耗为300/4800，仍可用。

实际磨损镐和原矿置于箱子2/3/4槽，正常运行200tick并保存停止。独立第二JVM读同一真实世界，在任何tick前逐槽比较实际保存的完整ItemStack SNBT语义，继续200普通tick再比较并正常保存停止。verify没有重新采矿、合成、直接load或补造物品。工具属性、头/柄、损耗和两种原矿数量均保留。

复用已有 NeoForgeDedicatedSmoke，增加默认关闭的 earlyToolChainSmoke。生产代码与资源未变，不修改矿物等级或工具平衡来使检查通过；现有双JAR仍是4a8f7b246构建，实际SHA一致。本批只进行该流程和一次重载，不重复完整构建/客户端检查。

- prepare：1m 54s，PID 77032，200普通tick，实际退出0、正常保存停止。
- verify：1m 52s，PID 78896，200普通tick，实际退出0、正常保存停止。

检查入口最终编译20s。首次检查代码误用MaterialPrefix.ore，编译失败38s；改为BlockMaterialPrefix.ore后通过，失败日志保留，不计作模组功能故障。

## 范围与后续

- Standing flint rocks, twigs, crafting table, supports and normal copper/tin ore are placed by the opt-in checkpoint. Natural generation/discovery and survival workbench acquisition are not proved.
- A server FakePlayer in survival performs actual right-click collection, real CraftingMenu result-slot click and ServerPlayerGameMode.destroyBlock. No real client GUI or wall-clock mining delay is claimed.
- Actual ore ItemEntity drops are transferred to the actor inventory by the checkpoint; walking-based native client pickup is not proved.
- Exact saved chest ItemStack SNBT components are compared semantically using TagParser before any reload ticks and again at tick 200; no synthetic item load or stock repair. Only these selected three chest slots are claimed.
- Selected NeoForge Flint/Wood pick and four normal Cu/Tin ores; no all-tool/all-material/fortune/Forge counterpart runtime coverage this batch.
- Smelting these actual raw ores into bronze, bronze head casting/assembly, natural world survival, powered clients, installed JAR runtime and complete three-source integration remain pending.

复现：Java17启动Gradle、Neo使用已有Java21工具链，新的flat/offline/loopback隔离目录及既有获同意EULA副本。`-PdirectCoreResources=true -PdirectCoreClasspath=true -PearlyToolChainSmoke=true -PserverSmokePhase=prepare|verify -PserverSmokeId=<同UUID> -PserverDirectory=build/<新隔离目录> -PserverSmokeX=0 -PserverSmokeY=240 -PserverSmokeZ=0 :neoforge:runServer`。整体移植仍未完成，来源和许可不变。
