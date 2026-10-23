# 双版本初期对齐：原有配方和事件

先只对齐1.20.1 Forge与1.21.1 NeoForge，并尽快交付初期可用结果。本轮以现有Forge源码、PORTING_TODO、docs/FUSION_PORT_2026-09-12.md、docs/MATTER_FABRICATOR_PORT_2026-09-12.md及对应实际注册为参考；不将旧文档的完成声明当作Native验收。彩色owned泡沫扩展仅在忽略区准备，未执行正式源码修改；brokestar/masson新增差异融合暂停，原整体移植仍未完成。

完整接入原14个配方家族：砧粉碎、基岩指示花、生物材料、坩埚熔炼、可挖泥土、炉炼、Glowtus、机器外壳、粉碎回收、石材工具生存链、石材形态、结构件、生存用品及车辆装拆。Native不另建材料/配方框架，使用现有共享材料身份、GT6 U、真实Recipe/RecipeMap及原形态注册；显式家族优先于生成表，恢复Source1相对次序。原Native提前注册的FermenterFood及材料postInit保持已有生命周期，未重复注册。补回可挖方块/球组成接线。

补Native原完整18条聚变配方，保留选择电路、LU启动量、产能方向和原缺失相位报告；补元素/特殊物质制造器，沿用原注册形态和流体绑定。两加载器放回原相对次序，未用已有聚变控制器类当作配方已经存在的证据。

环境效果：原油接触失明、地热水恢复/抗性、油气头部毒/恶心/每20tick溺水、沼泽饥饿/恶心及史莱姆与化学/呼吸防护，完整由Native EntityTickEvent.Pre驱动。九种世界流体改用原GTWorldFluidBlock，保留流动调度、水同族判定、取液和重油蛛网减速。Minecraft/效果Holder与事件接口留在平台层。

战利品：原91行入口、十个原版箱表、权重/数量/一次pool及原书内容通过原GTLootTables加载，SetComponentsFunction保存Native实际组件。loot_crate升级为Source1实际撬棍开启、随机原版箱表、返还crate与工具损耗；原ID/物品保留，3个原模型/方块状态按字节移入共享资源。原模型仍指向Source1金属墙纹理，不冒称新绘制。

挖掘：恢复Source1 HarvestCheck对特殊/电动工具与高级GT材料的接线；恢复硬锤/凿子/万能铲/撬棍的方块或逐掉落配方转换、概率产物和工具损耗。Native被移除的Tier.getLevel由六个原版Tier显式等级替代，第三方自定义Tier留缺口；未引入新工具底层。

验证：两组集中编译22s/13s通过。单次Neo普通专服启动1m54s，通过14家族注册/数据加载、200tick、正常保存退出；已有网络公钥获取Read timed out未阻止启动，不重试。后加Fusion/Matter/loot_crate只被编译，未重复启动。本轮打包结果另记；开发启动不能证明成品jar启动。

- Named recipe registration counts and short boot are not actual smelting/crafting/plant processing or survival proof. No new gameplay fixtures were added.
- The 18-reaction Fusion loader, elemental/special Matter loader and loot crate were added after the single dedicated startup; their Java compilation passed, their runtime/loot interactions remain unverified.
- Loot chest injection now preserves native components rather than legacy SetNbtFunction; ten table hooks are implemented but random chest content/books have not been exercised.
- Bathing/breathing, oil web movement, utility-tool harvesting and hammer/chisel/unpacking conversion hooks compile and are subscribed, but no actual entity/block gameplay has been tested.
- Native vanilla tier fallback uses the six known Tiers because Tier.getLevel was removed; external custom tiers without GT metadata remain unverified.
- Current native JEI/Jade plugins and inherited client resource/model gaps remain pending. Their Forge documentation does not establish native parity.
- Full client world/gameplay, Forge runtime for this wave, independent-process machine reload, old-world compatibility and final three-donor differences remain pending. D011 prioritizes Forge-to-Neo alignment, not a completion claim. Original authors/licenses/history remain unchanged and publication unresolved.

首次正式打包发现Native与共享core存在27个字节完全相同的旧资源副本，触发jar重复路径拒绝。移除Native原文件及对应已加工的27个副本，共享原字节为唯一来源；没有用EXCLUDE策略隐藏未知重复或删除平台loader分支。首次失败日志p3-version-alignment-jars-20261001.log，最终重打包结果另记。


2026-10-01 D011初期双版本测试JAR产出：Forge1.20.1 38522701 bytes，NeoForge1.21.1 35452249 bytes；首次Native打包重复路径失败，27完全相同资源副本归一后最终打包54s通过。已有验包器确认两包570当前共享class、67498共享资源/通知字节一致、平台元数据正确、无重复或测试专用条目。只是构建/包内容证据，未执行新成品jar启动/玩法/独立重载；可选Native JEI/Jade和客户端缺口仍待对齐，原整体移植仍未完成。回执verification/p3-version-alignment-artifacts.json。
