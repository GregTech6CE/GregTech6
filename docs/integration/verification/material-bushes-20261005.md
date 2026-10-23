# 2026-10-05 双版材料浆果灌木

承接持续移植及灌木扁平化、原版颜色/tooltip和双平台反馈。前批各色书籍已经接回，本批继承其资源，不重复书籍完整检查。整体移植仍未完成，本地提交，不推送。

现有1034种 `plantGtBerry` 物品此前能被识别，但注册表没有对应灌木，无法种入空灌木。本批从同一共享材料物品定义生成1034个 `bush_plant_gt_berry_*` 独立方块，两版同时注册物品和BE支持；新身份通过注册ID保存，原阶段/附着方向和生长计数各沿用原职责，不为种类新增NBT。未知旧外部数据保留已有适配器。

对照原版 `MultiTileEntityBush` 材料分支，叶片/开花/未熟分别用009000/ff9090/80ff80，成熟和物品栏使用材料固有RGB。食品浆果与棉花的物品栏改为来源未熟颜色；原来的九种自然生成池保持。对应物品译名组成新的方块名称，世界和物品一致；青色提示显示产物，空灌木提示用浆果右键设置。新方块保留原灌木的Untyped页和来源顺序。见[来源记录](material-bushes-source-20261005.json)。

现有低优先级资源包复用28种原生阶段/方向模型和六张原版贴图，为独立ID提供模型绑定；没有生成替代贴图或复制两千余份JSON，普通资源包可以覆盖独立身份。

共享现有检查10302条通过。每版各一次有限新世界，实际对1034种提供的样本执行空灌木种入且不消耗样本、原生生长计数溢出、独立方块掉落、1–2枚对应浆果采收和阶段重置；1034个物品模型/颜色/译名及28952个阶段/附着方向烘焙绑定检查通过，九种食品/棉花与七种泉的16个模型检查保留。两个世界均正常保存/停止，截图已查看，不运行完整GameTests。

世界回执后的最后改动只涉及世界名称委托和共享创造栏顺序；没有为相同采收入口再跑完整世界。最终普通双版assemble及启动探针4分18秒通过，697共享class与当前原生class、ZIP CRC、元数据和Forge生产Mixin映射通过。两包各28原生灌木模型、6贴图、2语言文件和62继承书籍资源逐字节匹配。实际安装Forge47.4.26/NeoForge21.1.252的隔离正式成品主菜单分别55.33/46.73秒正常启动并退出0；各检查最终1034种物品/世界名称、模型和颜色，截图已查看。

生长检查使用原生grow调用及提供的样本；采收前明确设为成熟阶段，没有验证自然等待完成整个生存取得链。28952是烘焙模型绑定数量，不代表所有材料枝条的碰撞/放置交互都重跑。没有独立存档重启、旧存档迁移、专服、外部浆果模组提供方或完整植物环境规则验证。既有12个外部形态创造图标缺失诊断保留在开发日志。详细范围、终态日志/截图哈希和成品证明见[机器回执](material-bushes-20261005.json)。

复现受影响世界：Java21及现有离线Gradle8.8，`-I work/emi-local.init.gradle -PdirectCoreClasspath=true -PdirectCoreResources=true -PclientCreateWorldSmoke=true -PclientSmoke=false -PclientSmokeTimeout=600 -PclientSmokeHeap=4g -PmaterialBushRuntimeOnly=true :runClient`或`:neoforge:runClient`。源码探针仅在隔离world-creation-smoke-run中运行，不进入成品。

成对验收包：`C:\Dev\GregTech6\build\verified\20261005-024516Z-b70be24d`。

- Forge：`gregtech6-1.20.1-forge-0.0.0.jar`，`b70be24d072cbd8f6fa9687daf7a02375cad105c92495f4c2e208b596f154d85`。
- NeoForge：`gregtech6-neoforge-1.21.1-0.0.0.jar`，`4c50d7fd0fc0ceceea673c0365fdc054a78c34549992a689759f929cfd91252b`。
