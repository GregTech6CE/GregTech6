# 太阳能板原版合同审计

第六十三批，2026-10-21，观察c4a95c5c4。核两种电力太阳能板真实注册、天气／天空、方向、发电／输出、覆盖板、保存和展示链；218登记最大堆叠16→64，环境火／邻液沿既有017扩围。不是游戏验收，也不回滚用户取消waterlogged的政策。

## 结论与范围

主要发电合同已有实现，不应再报告成“只有薄片”“只在白天工作”“蓄满额定值才输出”或“完全没有面板控制”。确认一个额外注册规格差异：**硅／锗太阳能板最大堆叠由原版16变为当前两端默认64**。原版活动时的火／邻液环境检查仍没有接入，属于既有 `ALIGN-017` 的具体宿主，历史 `verification/solar-output-20261004.md` 已明确保留这个缺口，不新报一条。

用户取消太阳能板含水状态是既有政策。`SolarPanelBlock` 不含 WATERLOGGED，并拒绝加液／取水，不作为本稿差异；邻接液体的原版环境检查与把水存进方块是两种合同。

## 实际注册与平台边界

- 原 `Loader_MultiTileEntities.java:899–902` 实际注册10050硅板／10051锗板，材料分别 `MT.DATA.Electric_T[0]`／`[2]`；`MT.java:3691` 解析为 TinAlloy／Al。两项 `NBT_OUTPUT` 分别8／16，明确覆盖类默认QU为EU，硬度／抗爆均4，注册堆叠16；不能按类默认QU误判实际能源。
- 当前 `EnergyNodeDefinitions.java:72–81` 对应 `solar_panel_silicon`／`solar_panel_germanium`、TinAlloy／Aluminium、EU、8／16及同一个原纹理目录。规格里的 capacity8／16不是电池容量合同：BE专门分支只保留本tick能量，并不暴露电容器。
- Forge `MachineBlockBootstrap.java:25` → `GTEnergyNodes.registerAll():123` → `add`，其 `GTBlockEntities.java:252–259` BE类型实际包含这些方块。Neo `GregTechNeoForge.java:106` → `GTEnergyNodes.register():59` → 专属DeferredRegister／ENERGY_NODE类型。两端都实际选择 `SolarPanelBlock`，不是仅存在一份没有注册的类。
- `EnergyNodeBlock.getTicker():139–147` 检查服务端／实际BE类型，调用 `EnergyNodeBlockEntity.serverTick():398–424`，solar分支直接到 `tickSolar():540–553`。已经显式投影15份共享Java的30份Forge／Neo视图，来源摘要保留条件展开结果；没有生成目标目录或初始化注册表。

## 已有天气、光照与时间合同

原 `MultiTileEntitySolarPanelElectric.generateEnergy():125–167` 与当前 `SolarPanelEnergy.tick()/generation():19–38` 的普通数值对应。以下均指已缓存到上方见天空，且未停机；数值是按源码手算，不是世界实测。

| 条件 | 硅 / 锗 EU/t |
| --- | --- |
| 晴朗白天 | 8 / 16 |
| 湿润群系雨天 | 1 / 2 |
| 晴朗夜间 | 1 / 2 |
| 湿润群系雨夜 | 0 / 0 |
| downfall为0的群系遇雨 | 沿晴朗昼夜分支，8/16或1/2 |
| 雷暴，或上方不见天空 | 0 / 0 |
| 暮色维度、见天空、无雷暴 | 4 / 8，先于昼夜／降雨分支 |

原 `Root.getSky():258–262` 排除hasNoSky后读 `canBlockSeeTheSky`；solar通过 `getSkyAtSide(TOP)` 取上方。当前原生适配 `tickSolar():543–547` 实际读 `dimensionType.hasSkyLight()`、`canSeeSky(pos.above())`、`level.isDay()/isRaining()/isThundering()`和当前群系 `getModifiedClimateSettings().downfall()`。没有把方块光照等级、红石亮度、朝向太阳或邻灯作为发电门槛；原生成方法也没有这些输入。现代原生 `Level.isDay()` 的fixed-time／skyDarken语义是适配边界，本稿未对所有自定义维度宣称完全相同。

天空首次请求和timer%600==5刷新已有；原版还接受mBlockUpdated，当前 `SolarPanelBlock.neighborChanged():37–41` → `checkSolarSky()` → core `checkSky()`。重新开启也请求刷新；当前条件本身每tick重新取，但天空结果按缓存使用。远处天空遮挡、跨区块未加载、昼夜切换与邻居通知并未实机验收。

暮色森林具体身份沿用已核的 `twilightforest:twilight_forest`。历史原始内容回执核过对应官方两版本维度ID／skylight，本稿不重新上网、没有安装或执行可选模组；也没有把没有移植所有旧可选模组能源接口当太阳能整族缺失。

## 输出、覆盖板与启停已经有真实消费者

原EU不在 `TD.Energy.ALL_SIZE_IRRELEVANT`（TD:221），因此一次发一个实际大小能量包。当前 `tickSolar():547` 同样调用 `EnergyTransfer.emitEnergyToNetwork(EU, energy, 1, this)`，后者逐面询问真实 `isEnergyEmittingTo` 后向接收者注入。夜间硅板是1EU的一个包，锗板是2EU的一个包，并非累计到8／16后发；拒收下一tick也只重新算1／2，不累加成2／4。接收后本tickenergy归零。

原输出只面向mFacing；当前 BE:733–739 按输出类型／正面／shutter门控，`EnergyNodeBlock` 放置采取点击面的反面，非法UP回落DOWN，扳手允许DOWN及四个水平面；BE:121–124还把旧UP载入为DOWN。顶面始终收集面，转正面不改变读天空的位置。

原运行可能=true、被动=mActive、主动=mEmitsEnergy（Solar:173–177）。当前 BE:61–62的coverPossible=true、machineControl:939–946分别读core active／emitting，覆盖板可以通过真实getter/setter改变启停。`hasControlPanels`／`PanelCoverInteraction` 有实际方块交互入口，`tickSolar` 前后调用PanelCoverRuntime；shutter在发送判断实际消费。只有注册一个面板物品不能证明控制，但这里的调用链已接上。通用覆盖板时序、独立检测完成状态等已在既有控制／显示报告限定，本稿不重报。

原停机跳过生成，保留energy和active/emitting；当前core:20–22同样早退，不能把停机后仍显示旧生成状态自动当成新错误。core.extract:52–57保留整次请求成功或0、绝对包大小、模拟不扣除，并额外拒绝非法／溢出请求；没有要求恢复原long乘法溢出。BE:870–875实际转发提取；输出最小为额定/8、最大／推荐为额定。BE:892–904专门不声明电容器，Jade EnergyBuffer/provider也按真实capacitor合同筛选，因此不显示虚构储能条是已有设计。

## 保存、模型和提示

原Solar:52–69读写energy／active／stopped／emits。当前 BE:1101–1105保存 `gt.buffer`、`gt.solar_stopped`、`gt.solar_active`、`gt.solar_emitting`，1170–1172恢复；core.restore:60–66对旧移植累加器按额定8／16封顶，保留标志并请求天空刷新。两端使用各自原生load/save签名，覆盖板也有各自ItemStack序列化。这是当前保存路径的源码证据，不是1.7.10原世界直接导入、旧版本NBT全量迁移或保存重启游戏验收。

原Solar:186–189固定顶面索引4，正面按水平／底部选facing纹理；当前共享维护资源有20模型（两种×5合法面×active两态）、2物品父模型、2状态JSON（各12条，含UP兼容）、15PNG。全部模型是16³、着色layer0／未染色layer1，顶面均top，活动层切overlay_active。方块ACTIVE由 `solarEnergy.active()`更新，不依赖是否找到接收者。raw composite loader在维护源为forge，`multiversion/build.gradle:4–15`的JSON专用规则切成Neo命名空间，不把原生Java字符串一起替换；本稿仅读规则及资源，不声称本批生成输出／烘焙已验收。

Forge `GregTechClient:232–235/484–490`、Neo `EnergyNodeClientSetup:6–7`实际给物品与方块注册材料tint，仅layer0着色。`EnergyNodeRenderer`另画六面覆盖板。原版资源快照当前没有对应太阳能PNG，未把15份现有纹理声称为本批逐字节对原作者PNG比较；原纹理名和几何／选面可以核，显示效果仍需实机。

Tooltip实际走 `EnergyNodeBlock:287` → `OriginalEnergyDeviceTooltipData:57–60`（无输入、min=output/8、额定=max、face.front）→ `EnergyDeviceTooltips.energyLine`。因此后面switch的旧SOLAR备用分支不是实际主提示路径；额定／分档／正面、EU蓝色及扳手提示已有，不能只看备用分支断言缺失。本稿没有核所有语言键逐字或Jade外观。

## ALIGN-218：最大堆叠16 → 64

原Loader:901/902的参数是16，`MultiTileEntityRegistry.add():148–149` → `MultiTileEntityClassContainer:41–44`把它写入mStackSize，`MultiTileEntityItemInternal.getItemStackLimit():241–248`消费。Solar及其基类链未见IMTE_GetMaxStackSize覆写，所以本例返回16。

当前 `GTEnergyNodes.java:57`（Neo）／`95–97`（Forge）都给普通太阳能创建plain `BlockItem`。唯一stacksTo(16)分支限定 `OriginalSteamTurbines.handles(spec)`；该方法要求Kind.TURBINE且steam_turbine前缀（OriginalSteamTurbines:37），两种太阳能不会命中。Forge实际映射依赖的Item.Properties构造字节码默认maxStackSize=64，Neo实际Item.Properties构建公共组件，`DataComponents:233–240`给MAX_STACK_SIZE=64；BlockItem把Properties直传，Neo ItemStack:420–422／IItemExtension:421–422实际读该组件。

定向检索当前共享与两端生产代码未见solar专属默认组件更正／堆叠覆写或全局默认组件事件补偿。故32个相同新硅板在普通堆叠约束下，原声明至少2槽、当前声明可1槽。这只是具体规格及槽数算术反例，未运行库存搬运；外部模组、命令覆写组件不在“新注册默认物品”范围。不要将同一注册默认推断扩展为所有能源机器的已核清单。

## 已知差异B：继承环境检查（归已有017，不新编号）

原Solar:157–159在active且timer%600==5调用Root.doDefaultStructuralChecks。EU属于弱火／弱水类型；Root:778–805在未受不透明覆盖板保护的面检查火（每面1/10）及除底面的邻液（符合条件即进入），再按各危险配置选择效果／声音。`TileEntityBase06Covers:217/219`实际把不透明覆盖板转成火／水保护。

Solar:169–170**明确覆写雨／雷保护为true**，所以这一宿主不能照抄普通电转换器“雨／雷也应炸”的结论。雨天发电降额、雷暴停发仍按上面的天气表执行，和结构风险免疫不同。举例：白天上方见天空、侧面裸露邻液、活动且恰逢检查相位、相关危险开关允许时，原版有检查／效果入口，当前tickSolar没有该调用或等价委派。当前新COMMON过载开关不是这条火／邻液配置合同的替代。

本缺口历史solar-output报告已经明示，纳入017可作为已知宿主补充；这轮不恢复爆炸或破坏，也不将取消waterlogged政策回滚。

## 证据规模与未验

本专题14份原版／40份当前来源，15份共享Java的30份平台视图，39份资源；两个既存原生工件的9个entry哈希，以及一次只读javap（Forge Properties）。原代码作者／LGPL声明与原资产CC0说明保留；本机原生依赖只作参考，不复制其方法体到产物。

没有运行生成器、Gradle、Java初始化／编译、新测试、游戏、实际网络／可选模组、世界保存重启或构建／推送。没有完成全能源接口、FE/IC2兼容、极值恶意提取、完整机器配方、世界阻挡更新、跨维天气／天空、面板点击／渲染、物品掉落NBT等全部验收；过去测试和构建结果只作为历史证据，未当本批新执行。
