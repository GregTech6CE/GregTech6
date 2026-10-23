# Diesel engine crafting parity — 2026-10-02

原始 GT6 Loader_MultiTileEntities.java:722–729 的八种柴油引擎 `PLP / SMS / GPC` 制作图样译为共享 JSON：三弯板、两杆、一个双层机器外壳、一大齿轮、一小齿轮和工业润滑剂。钢使用原 ANY.Steel（Steel/Knightmetal/MeteoricSteel）四新增形态标签及既有杆标签，缺失成员形态为可选嵌套引用，不退到其他材料或前缀。原 DEF_REV_NCC 无镜像标志，实际底行大/小齿轮交换被拒绝。

原 OreDictReRegistrations:978–989：工业 itemLubricant 接 container250lubricant/container1000lubricant；早期植物/动物/种子油属于 itemLubricantEarly。本移植现有工业瓶为 250mB，实体 cell 可装1000mB，LubRoCant 是原 Lubricant 瓶的另一个登记流体。复用已有 finite_fluid_container_1000 原料，新增默认 false 的 allow_lubricant_bottle 选项，仅 Lubricant 合法；柴油开启，原灌瓶仍关闭。实际消费瓶并返 GT 空瓶，或消耗1000mB并返排空实体容器；display物品与早期油不代替工业润滑。Forge 自定义原料网络新增 bool，Native 使用自身 Codec，两端需同一构建，未建立旧构建网络兼容；保存/注册身份未变。

复用现有普通专服 checkpoint，两版各检查八配方真实 matches/assemble，250瓶与1000实体容器两条入口，容器返还/输入不变、原料候选非空、无镜像/早期油拒绝；真实 recipe/ingredient 包往返 Forge 17 次，Native 25 次，Native 对每行额外用真实1000mB LubRoCant。原四瓶灌装仍需1000mB且不能把250mB单瓶复制成四瓶；Native 额外确认空/999mB/创造流体显示拒绝及实际灌瓶返空容器。每版本随后200普通tick、正常保存停止。这是方法查询，不是玩家点击、完整生存或柴油供能；没有新柴油产物/方块重载。

- neoforge：1m 57s，PID 78356，实际退出0，全部八行/200tick/正常保存停止通过。
- forge：2m 17s，PID 80936，实际退出0，全部八行/200tick/正常保存停止通过。

最终集中双版编译 17s；首轮probe错误引用 api.prefix 的类至 data 包失败43s，原日志保留。双包构建 1m 1s，573 当前 core class、关键类/元数据/无重复或检查污染，新增8配方+4标签原SHA双包核对通过。共享静态目录2192条。

- gregtech6-1.20.1-forge-0.0.0.jar：38,593,987 字节，SHA-256 `01e0c89dc97cbaca816c1a4f0a3b7ba2dc4c0694023745dc616a68b2d74a9895`。
- gregtech6-neoforge-1.21.1-0.0.0.jar：36,785,077 字节，SHA-256 `d4333c223d0fdee543baf06f76445abae7ed16cc77eaa5c1231cf5466236693b`。

**已知动力缺口：**原此柴油系列输出 RU，当前两版 source1 延续 KineticDieselEngine 输出 KU。本批只补原制作入口并对齐容器语义，不将这项原设计偏差称为已完成；后续需结合原柴油/旋转活塞、负载运行与旧状态兼容处理。未新增发电注能或伪造运行成功。

复现：Java17 Gradle launcher，Native 现有Java21工具链；新本机flat/offline/loopback隔离目录，既有获同意eula=true副本。`-PdirectCoreResources=true -PdirectCoreClasspath=true -PdieselCraftingSmoke=true -PserverSmokePhase=prepare -PserverSmokeId=<新UUID> -PserverDirectory=build/<新目录> -PserverSmokeX=0 -PserverSmokeY=240 -PserverSmokeZ=0 :runServer` 或 `:neoforge:runServer`；正式双包 `-PdirectCoreResources=true :distributionJar :neoforge:jar`。没有测试任意第三方容器或全部ANY钢成员，客户端供能/完整生存/成品安装/旧档仍待验。来源、逐行、改文件哈希与实际进程回执见同名JSON及core/provenance/diesel-crafting-20261002.json；整体移植仍未完成。
