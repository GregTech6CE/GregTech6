# 储物集中成品自检修正（2026-10-07）

9d386098、62d60705、e692ec5e已通过每端三个有限原生世界场景，c133b989普通双包构建4m19s和745当前共享类/CRC验包通过。首个稳定副本 `build/verified/20261007-004412Z-223faa28` 尚不计本批完整交付。

成品实际启动暴露五次失败，均保留日志：

1. Forge进入主菜单，独立探针遗漏CommonBlockDeliveryChecks。双探针打包改为所有DeliveryChecks类，启动前核对编译类集合，避免再次进入游戏才发现漏类。日志 `build/production-smoke/forge-819f8756-2855-4d46-91c8-515bf23c2c9b/launch.log`；探针重编12s通过。
2. Forge进入主菜单，1311具名来源身份与BasicMachine实例被错误当成互斥集合计数。改为独立集合，1311路径逐条查缺失，输出数量/重叠诊断。日志 `build/production-smoke/forge-8668402d-87db-4a15-b3eb-94e96a42e6c3/launch.log`；探针重编13s通过。后续Forge实测1311/250、重叠3、无缺失，全部材料和储物提示检查通过。
3. 同次Neo进入主菜单，探针错误供应静态注册表TooltipContext，缺少数据包附魔表。改用Minecraft原生无世界TooltipContext.EMPTY，与现有成品材料检查一致。日志 `build/production-smoke/neoforge-1f76e739-2fb1-4d17-9df7-576c81b06219/launch.log`；仅Neo探针9s重编通过。
4. 仅Neo重跑后1311/251身份、重叠4、无缺失，但类代表陶瓷坩埚保留旧`tooltip.gregtech.crucible.harvest_pickaxe`行，其译文与通用采集行相同，实际重复。日志 `build/production-smoke/neoforge-1324f242-ffc8-4bf4-a17c-7d7ff946c487/launch.log`。同时代码审计发现导线旧`tooltip.gregtech.wire.harvest_tool`行亦未纳入通用去重。双原生CommonBlockTooltips统一删除这两个旧行，再加入原通用采集行；独立成品探针检查旧行消失并在失败时显示真实行内容。
5. 新普通双包1m9s构建/验包通过，稳定副本 `build/verified/20261007-010100Z-4bb1f8ba` 的Forge实际自启动53.44s成功。Neo全部1311/251身份、原生类代表/覆盖板/默认储物工具行通过，随后四保存模板名称检查因EMPTY无物品注册表失败。日志 `build/production-smoke/neoforge-aed5cfe5-98f9-4da9-8c49-d24a073d284f/launch.log`。仅这四项改为真实BlockItem.appendHoverText配真实内置物品表；避免把不完整注册表传入原版全数据包提示链。默认全提示事件仍用EMPTY，保存数据、生产提示代码与普通包未改；只重编Neo探针、重跑Neo。此证据属于实际物品方法/序列化调用，不代替世界内悬停或完整数据包环境。

第四项是模组提示修正；两端采集策略、配方、材料登记/数量、保存格式和语言均未改。原版基准仍为 `MultiTileEntityItemInternal`、`ItemBlockBase`、`LH.getToolTipHarvest`；来源作者GregTech-6 Team / Gregorius Techneticies、LGPL-3.0-or-later及SHA沿用[本批来源回执](common-block-properties-source-20261007.json)。原生共同提示与来源策略以实际BlockItem决定；未映射来源坩埚的采集属性完整对齐仍留后续，不由重复行修正冒充完成。

普通双包必须重新生成/验包后再做正式安装自启动，之前通过的Forge旧成品检查不替代新包检查。提示变化不影响已通过的采集/制作回收场景，按用户减少测试要求不重复世界套。完整生存、独立存档重启、历史存档、普通专服和玩家实际悬停尚未新增验收。仅本地，无推送/PR，整体goal保持active。
