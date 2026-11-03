# 流体移除

本端停注的重复流体、闲置兼容流体、联动残留，以及随材料停注不再生成的流体。材料身份与来源见[材料移除](material-removal.md)。

| 内容 | 差异 | 原因 |
| --- | --- | --- |
| 重复生成（[审计类型A](https://github.com/GregTech6CE/GregTech6/issues/249)） | 停注 `waterdistilled`。 | 材料 `WaterDistilled` 改为绑定 `DistW`（`ic2distilledwater`），蒸馏水只保留一个 ID。 |
| 污水 | 删除 `waterdirty` 及其 `_flowing`、污水瓶与木桶、Drying 配方、沼泽水体生成和相关世界液体效果；沼泽列保留原版水。 | 整体删除污水，不再保留独立流体、材料 `WaterDirty` 与 `DirtyWater` 别名。 |
| 概念重复（[审计类型B](https://github.com/GregTech6CE/GregTech6/issues/250)） | 停注 `swampwater`、`stagnantwater`、`rc oxygen`、`rc liquid oxygen`、`rc_heavy_water`、`ethanol`、`rc ethanol`、`ic2biomass`、`ender`、`redstone`、`glowstone`、`for.honey`、`grc.honey`、`grcmilk.milk`、`oliveoil`、`plantoil`、`lightoil`、`heavyoil`、`rc lubricant`、`brine`、`tropicswater`、`grccore.saltwater`、`volcanic_lava_fluid`。 | 同一物质或同一概念的多个 ID 只保留引用更多、属于 GT6 材料链或世界生成的 canonical ID；`ethanol`、`oliveoil` 是材料生成重复，分别改绑 `bioethanol`、`binnie.juiceolive`。 |
| 联动残留（[审计类型D](https://github.com/GregTech6CE/GregTech6/issues/252)） | 停注 `binnie.juicelime`、`binnie.juiceorange`、`binnie.juiceplum`、`binnie.juicepeach`、`binnie.juiceelderberry`、`binnie.juicegrapefruit`、`binnie.juiceapricot`、`binnie.juicepear`、`binnie.juiceredgrape`、`binnie.juicewhitegrape`、`binnie.juicecherry`、`binnie.mashwheat`、`binnie.mashcorn`、`binnie.mashrye`、`binnie.mashgrain`、`binnie.juice`、`binnie.wineagave`、`binnie.wineapricot`、`binnie.winecherry`、`binnie.winecitrus`、`binnie.wineelderberry`、`binnie.wineplum`、`binnie.winesparkling`、`binnie.winefortified`、`binnie.winered`、`binnie.winewhite`、`binnie.ciderpear`、`binnie.ciderpeach`、`binnie.whiskey`、`binnie.whiskeyrye`、`binnie.whiskeycorn`、`cactusfruitjuice`、`datejuice`、`blood`、`slime.blue`、`pinkslime`、`sludge`、`tar`、`sewage`、`xpjuice`、`xp`、`mobessence`、`lpg`、`liquidnitrogen`、`liquidmethane`、`cold_water`、`hot_water`、`ic2hotwater`、`boilingwater`、`canolaoil`、`hotcrude`、`rc jet fuel`、`biodiesel`、`poison`、`pyrotheum`、`cryotheum`、`petrotheum`、`aerotheum`、`mana`。 | 原版只做 `exists()` 判断或端口没有生产链；Mana（Botania）与 `pyrotheum`、`cryotheum`、`petrotheum`、`aerotheum`（Thermal Foundation）由人工判断移除。 |
| 光谱露（[审计类型D](https://github.com/GregTech6CE/GregTech6/issues/252) 漏项） | 停注 `spectral_dew`，并移除 `spectral_dew` 瓶、9 种 `wooden_bucket_*_spectral_dew` 木桶、对应模型、贴图、堆组和语言键；引用 `SpDew` 的水与冷却剂配方行一并移除或从候选列表删除。 | 原版 `FL.SpDew` 依附 Netherlicious/Thaumcraft 联动，端口没有来源；按类型D补删。 |
| 麦芽浆与酒类链 | 停注 `beer`、`binnie.whiskeywheat`、`glenmckenner`、`wheathopsmash`。 | `binnie.mashwheat` 等麦芽浆没有作物来源，啤酒与威士忌链一并停用。 |
| 闲置兼容酒类 | 停注未使用的 Binnie 啤酒、利口酒、白兰地等。 | 无配方用途的兼容占位不再注册；威士忌、葡萄酒和果酒链见类型D。 |
| 其他闲置流体 | 停注 `ic2steam`、`ic2superheatedsteam`、`myst.ink.black`、`ooze`、`purelava`、`endergoo`。 | 无实际用途的兼容占位。 |
| IndustrialCraft 2 流体 | 停注 `ic2uumatter`、`uuamplifier`、`uumatter`。 | 本端未接入该模组，UU 材料与对应流体一并停注。 |
| 其他随材料停注的显式流体 | 停注 `indigo`、`peanutbutter`、`ricewater`、`ricevinegar`、`whaleoil`、`molten.hsla`、`molten.waxmagic`、`molten.waxrefractory`、`molten.waxamnesic`、`molten.waxsoulful`。 | 对应染料、作物、蜂蜡与合金材料停注，见[材料移除](material-removal.md)。 |
| GT6 自创材料生成流体 | 停注 `ectoplasm`、`martianvitriol`。 | 材料没有物品、配方或方块来源，随材料停注。 |
| Metallurgy 熔融流体 | 停注 `molten.alduorite`、`molten.amordrine`、`molten.angmallen`、`molten.aredrite`、`molten.astralsilver`、`molten.atlarus`、`molten.celenegil`、`molten.ceruclase`、`molten.deepiron`、`molten.desichalkos`、`molten.eximite`、`molten.haderoth`、`molten.ignatius`、`molten.infuscolium`、`molten.inolashite`、`molten.kalendrite`、`molten.lemurite`、`molten.meutoite`、`molten.midasium`、`molten.oureclase`、`molten.prometheum`、`molten.rubracium`、`molten.sanguinite`、`molten.shadowiron`、`molten.shadowsteel`、`molten.tartarite`、`molten.vulcanite`、`molten.vyroxeres`。 | 本端未接入该模组，材料没有物品来源与配方。 |
| Ender IO 熔融流体 | 停注 `molten.claycompound`、`molten.crystallinealloy`、`molten.endsteel`、`molten.energeticalloy`、`molten.energeticsilver`、`molten.melodicalloy`、`molten.pulsatingiron`、`molten.redstonealloy`、`molten.stellaralloy`、`molten.vividalloy`。 | 本端未接入该模组，材料没有物品来源与配方。 |
| The Betweenlands 熔融流体 | 停注 `molten.octine`、`molten.syrmorite`。 | 本端未接入该模组，材料与方块没有来源。 |
| Thaumcraft 熔融流体 | 停注 `molten.darkthaumium`、`molten.tallow`。 | 本端未接入该系列模组，材料没有物品来源与配方。 |
| 太空类模组熔融流体 | 停注 `molten.desh`、`molten.frozeniron`、`molten.kreknorite`、`molten.marsstone`、`molten.moonstone`、`molten.spacestone`、`molten.glowstoneceres`、`molten.glowstoneenceladus`、`molten.glowstoneio`、`molten.glowstonepluto`、`molten.glowstoneproteus`。 | 本端没有对应天体、陨石与行星岩层，材料与方块没有来源。 |
| AbyssalCraft 熔融流体 | 停注 `molten.abyssalnite`、`molten.coralium`、`molten.dreadium`、`molten.ethaxium`。 | 本端未接入该模组，材料没有物品来源。 |
| Alfheim 熔融流体 | 停注 `molten.elvorium`、`molten.mauftrium`、`molten.muspelheimpower`、`molten.niflheimpower`。 | 本端未接入该模组，材料没有物品来源与配方。 |
| Netherlicious 与 Enderlicious 熔融流体 | 停注 `molten.gloomstone`、`molten.voidmetal`。 | 原模组是高版本下界与末地内容的回移植，本端改用现代 MC 原版内容。 |
| Big Reactors 熔融流体 | 停注 `molten.blutonium`、`molten.ludicrite`、`molten.yellorium`。 | 本端未接入该模组，材料没有物品来源；反应堆燃料棒一并停注。 |
| Mekanism 熔融流体 | 停注 `molten.glowstonerefined`、`molten.obsidianrefined`。 | 机器配方仍接入该模组，但材料本身没有物品来源，停注。 |
| Tinkers Construct 熔融流体 | 停注 `molten.ardite`、`molten.manyullyn`。 | 本端未接入该模组，材料没有物品来源。 |
| HBM's Nuclear Tech Mod 熔融流体 | 停注 `molten.mingrade`、`molten.workersalloy`。 | 本端未接入该模组，材料没有物品来源。 |
| GT6 自有合金与钢种熔融流体 | 停注 `molten.bedrockhslaalloy`、`molten.electrumflux`、`molten.gildediron`、`molten.hepatizon`、`molten.hslaspringsteel`、`molten.hslasteel`、`molten.hslatungstenalloy`、`molten.obsidiansteel`、`molten.pigiron`、`molten.titaniumaluminide`、`molten.titaniumgold`。 | 本端未保留这些合金与钢种的方块、物品与配方，停注。 |
| 陨铁与陨钢熔融流体 | 停注 `molten.meteoflameblacksteel`、`molten.meteoflamebluesteel`、`molten.meteoflameredsteel`、`molten.meteoflamesteel`、`molten.meteoricblacksteel`、`molten.meteoricbluesteel`、`molten.meteoriciron`、`molten.meteoricredsteel`、`molten.meteoricsteel`、`molten.meteorite`。 | 整条链依附太空类模组的陨石与陨铁，本端没有来源，随太空材料一并停注。 |
| 其他单条模组熔融流体 | 停注 `molten.darkiron`、`molten.endium`、`molten.goldinductive`、`molten.ironcompressed`、`molten.purplealloy`、`molten.spectreiron`、`molten.sunnarium`。 | 本端均未接入对应模组，材料没有物品来源。 |
| 外部作品熔融流体 | 停注 `molten.adamantite`、`molten.carmot`、`molten.mithril`、`molten.orichalcum`、`molten.oriharukon`。 | 名称取自《星际迷航》、托尔金作品与神话，相关模组未接入本端。 |
| 现实名称化合物熔融流体 | 停注 `molten.ferrite`、`molten.niobiumnitride`。 | 本端未保留对应配方与物品形态，停注。 |
| 药水系统（整套移除） | 停注全部 94 条带药水标志的流体，含基底、即时效果、时长效果及喷溅/滞留形态；注册名与数量见[药水系统移除](potion-removal.md)附录A。 | 药水身份改由原版药水物品承载，流体侧保持单一体系；该标志没有运行期读取者。 |
| 无来源/无消费端材料熔融流体 | 停注 `molten.bedrockium`、`molten.crystallinepinkslime`、`molten.unstable`。 | 对应材料停注，见[材料移除](material-removal.md)。 |

随材料停注不再生成的流体共 113 条，包括 110 条 `molten.*`、`ectoplasm`、`martianvitriol` 和 `uumatter`。[类型C](https://github.com/GregTech6CE/GregTech6/issues/251) 的 `molten.brass`、`molten.zinc`、`molten.calcite` 静态声明已删除，但材料生成器以同一 ID 重建，不属于流体移除；属性改由材料熔点与亮度 10 决定。

另清理不再对应任何已注册流体的 `fluid_type.gt6ce.molten_*` 语言键：英文 824 条、中文 826 条。通用回退键 `fluid_type.gt6ce.molten_material` 保留，供没有专用显示名的熔融流体使用。
