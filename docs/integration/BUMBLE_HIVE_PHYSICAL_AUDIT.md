# 自然蜂巢火焰与末影龙保护核查

第五十批，2026-10-21，基线 `2d14d4009`；新增 ALIGN-169–170。检查的是新注册的十六种固定颜色自然巢，不把用户要求的扁平 ID 判为移植错误，也不恢复以 NBT 表示颜色身份。

原来源为 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java`，作者 Gregorius Techneticies／GregTech-6 Team，保留 LGPL-3.0-or-later 源文件头。本批只记录行为差异和修正说明，没有改变火焰、实体破坏或世界生成逻辑。

## 实际注册与原生边界

原 `Loader_MultiTileEntities:2041` 注册 MultiTileEntityBumbleHive。该实体继承的火焰接口通过 `MultiTileEntityBlock:253–254` 转发，实体破坏接口通过 `:256` 转发；不是只有未使用的两个数值或一个保护方法。

当前 Forge 的 MachineBlockBootstrap／GTToolBlocks 与 GTDecorBlocks 分别调用养殖箱／自然巢工厂，NeoForge 启动调用 GTBumbleBlocks.initialize。两者的新自然巢均由 GTBumbleBlocks.initializeHives 构造 `FixedBumbleHiveBlock → BumbleHiveBlock → Block`，默认属性来自同一 defaultProperties。RegisteredBlock 只提交方块与物品工厂，没有附加火焰映射或实体破坏保护。自然生成由 GTFeatures／GTBumbleHivesFeature 选取这些固定变种。

依赖审阅限定项目实际使用的 Forge 47.4.20／MC 1.20.1 和 NeoForge 21.1.243／MC 1.21.1。另读取相应原生客户端资源，内部 version.json 分别确认 1.20.1 和 1.21.1；没有拿另一版本的标签或最新网上 API 替代。

## ALIGN-169：遗漏原自然巢的两项火焰参数

原 `MultiTileEntityBumbleHive:83–84` 在任意面返回 fireSpreadSpeed=300、flammability=300。官方 1.7.10 [BlockFire 补丁](https://raw.githubusercontent.com/MinecraftForge/MinecraftForge/1.7.10/patches/minecraft/net/minecraft/block/BlockFire.java.patch)的 tryCatchFire 和 getChanceToEncourageFire 确实查询这两个方块接口，再经上述 GT 转发进入自然巢。

当前两个自然巢类没有覆写这两个接口。Forge 的 IForgeBlock 默认调用 FireBlock.getBurnOdds／getIgniteOdds；Neo 的 IBlockExtension 同样如此。原生 FireBlock 对具体方块查 burnOdds／igniteOdds 映射；普通火焰的消费和传播也实际调用 state.getFlammability／getFireSpreadSpeed。没有为这些新 GT 方块填入映射的启动代码，不能把木头音效或 MapColor 当作火焰参数来源。

defaultProperties 虽有 `ignitedByLava()`，LavaFluid 及加载器的同名接口走的是熔岩点火条件，不能替代原 300／300 的普通火焰参数。这项是两个数值及其消费路径的缺口，不表示所有天气、方向、游戏规则下都应保证一次烧毁，也没有运行实际火灾场景。

## ALIGN-170：遗漏自然巢的末影龙破坏保护

原 `MultiTileEntityBumbleHive:91` 拒绝 EntityDragon。官方 1.7.10 [EntityDragon 补丁](https://raw.githubusercontent.com/MinecraftForge/MinecraftForge/1.7.10/patches/minecraft/net/minecraft/entity/boss/EntityDragon.java.patch)将破坏条件接到 block.canEntityDestroy，并保留 mobGriefing 门控，因此 GT 的实体转发保护在原版有真实消费。

当前自然巢没有 canEntityDestroy 覆写。两加载器默认对末影龙检查 DRAGON_IMMUNE；EnderDragon.checkWalls 还先排除 DRAGON_TRANSPARENT，并调用 ForgeHooks／CommonHooks，结合生物破坏规则、方块许可和可取消事件后才 removeBlock。

已同时核对两个原生标签：immune 中只有原生明确方块，transparent 中只有 light 和 fire 标签。项目生产、资源及标签工具没有给自然巢添加这两类标签，也未发现其专用实体破坏事件补偿。因此在默认数据、允许生物破坏且没有外部事件取消时，当前自然巢可进入原生移除路径，原版的自身拒绝条件没有对应实现。

外部数据包、保护模组或关闭生物破坏规则仍可能阻止移除；不能据此声称所有世界都会被龙摧毁。这里只核内置默认合同，没有运行龙碰撞场景，也没有扩大为凋灵、玩家或一切爆炸都应免疫。

## 既有对应、说明整理与边界

自然巢是生成时填入的九槽静态容器，原版没有养殖生产 tick，当前不生产不是缺失。十六色固定身份、旧颜色状态转换和养殖箱周期分开，主链及温度计接口已在[前批报告](BUMBLE_LIFECYCLE_AUDIT.md)记录。

本轮 BumbleHiveBlock 仅纠正三处说明：新变种是注册身份；COLOR 仅用于早期状态式数据；defaultProperties 本身没有提供原版两项火焰参数。Forge／Neo 的 725／754 个执行词法项及字面量位置与基线相同，未用注释修改冒充功能修复。

末影龙补丁的网页读取失败后，改用只读 HTTPS 获取这两份官方补丁的原始字节并计算哈希；没有为未保存的网页内容编造摘要。原生源码和反汇编留在忽略的 work/，没有纳入生产源。具体文件、依赖、标签、官方补丁、排版和未测边界见[第五十批回执](verification/inventory-persistence-audit-20261021.json)。本批没有 Gradle、编译、新测试、游戏、保存重启、构建或推送；全面核查继续。
