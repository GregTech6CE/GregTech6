# 联动

共享规格在 `core` 的 `content/compat`。平台只解析注册名、标签和 GT 形态，不引用第三方模组的类。目标模组没装时整组跳过，打一行日志，配方数量不变。装了模组但某个登记过的注册名找不到时，这一行不生成，按来源行号计数，不生成空气产物。

机器行在 `TagsUpdatedEvent`（`shouldUpdateStaticData()`）先清掉本批加过的对象再重建，`ServerStoppedEvent` 时清空。合成表新增和按配方 id 删除走内存数据包，位置在 TOP。Forge 用 `forge:false`，NeoForge 用 `neoforge:false`。后注册的模组数据包仍可能盖掉这份删除文件，所以配方管理器应用完之后，`AddReloadListenerEvent` 再按删除名单摘掉仍在的配方 id。不改 `GregTech.java` / `GregTechNeoForge.java`。检测用现代 mod id 问 `ModList`，不用 `ModReferences`。

开发期运行依赖用 `-PcompatRuntime=<id>`，默认关闭，不打进成品。一次只能开一个：`ie`、`mek`、`ae`、`pr` 或 `hc`。

每条原版引用的分类、来源文件 SHA-256、版权和现代目标在对应 JSON 里。人工说明放同目录的 markdown。

## 通用形态标签

带 `ingots`、`nuggets`、`gems`、`dusts`、`small_dusts`、`tiny_dusts`、`plates`、`rods` 之一、且只对应一种形态和一种材料的外来物品，会得到对应的 GT 材料数据。方块、齿轮、环、螺栓、螺丝、箔、导线不补。`minecraft` 和 `gregtech` 物品保持原样。决策在 [2026-10-07](../DECISIONS.md)。沉浸工程只用来验证这层，不另写机器配方账本。

## 这次只做了 Immersive Engineering

见 [immersiveengineering.md](immersiveengineering.md) 和 [immersiveengineering.json](immersiveengineering.json)。21 处实现，6 处已有通用路径覆盖，9 处外部机器桥不恢复，46 处推到别的批次，68 处不适用。合计 150 处 `MD.IE` / `IL.IE_`。

## Mekanism

见 [mekanism.md](mekanism.md)。原版染色的气球和塑料方块在 Mekanism 10 里已经没有注册名。还在的只有删掉 `mekanism:storage_blocks/salt` 这一个 2×2 盐块配方。

## Applied Energistics 2

见 [ae2.md](ae2.md)。冲压、切割、压缩、砸块和两张石英玻璃有序合成还在。磨粉机桥、假配方、水晶种子、天际石加工和压板复制不生成。开发期还要带上 AE 强制依赖的 GuideME。

## Project Red

见 [projectred.md](projectred.md)。硅晶坯锯切、硅片和注红石硅的有序合成，以及红铁化合物的替换还在。大理石和玄武岩的石头加工推后，Exploration 不加运行依赖。Forge 固定 `4.20.0` 并 `fg.deobf`，因为 `4.21.0` 会解析成另一边的 jar。

## Ender IO

见 [enderio.md](enderio.md)。没有配方类。合金归属、规范形态、Yeta 扳手和储罐材料数据都推后。不加运行依赖。

## PneumaticCraft

见 [pneumaticcraft.md](pneumaticcraft.md)。没有配方类。压缩铁的归属和锭、块、齿轮规范形态都推后。不加运行依赖。

## Storage Drawers

见 [storagedrawers.md](storagedrawers.md)。没有配方类。书本登记和木头材质都推后。JABBA 的木桶锯切不算进这里。不加运行依赖。

## HarvestCraft 2 Food Core

见 [harvestcraft.md](harvestcraft.md)。第二波先做 Food Core。向日葵粉碎、牛肉干、原味甜甜圈的糖粉和巧克力浴，以及面粉加四种水做面团还在。作物、树木和 Food Extended 不进这次运行依赖。

## 之后

全量账本还没有。上面的 150 处是手工对着原版源码分类的，不是参数化扫描。扫描工具要覆盖 59 个 `Compat_Recipes_*` 和散落引用，单独做。

数据侧按同一模板分批。Immersive Engineering 那次没有做下面这些；Mekanism 只做了盐块配方删除：

- 第一波双版：Mekanism 只做了盐块配方删除；AE2 做了还在的冲压、切割、压缩、砸块和石英玻璃；Project Red 做了硅片锯切和三张有序合成。Ender IO、PneumaticCraft、Storage Drawers 只写了账本，没有运行依赖。
- 第二波大内容：HarvestCraft 2 先做了 Food Core 里两边都还在的机器行；作物、树木、Food Extended、Biomes O' Plenty、暮色森林、Aether、Tropicraft、Railcraft Reborn 还没做。
- 单平台批次另开，不挡住另一边。
- 深层附属各自独立 jar：Forestry CE、CC:Tweaked、BuildCraft CE。

外部机器镜像（`RM.ic2_compressor`、`RM.pulverizing` 以及同类行）继续不恢复，理由在 `MachineRecipeMaps` 已有说明。旧存档兼容在对应批次实际用备份测过之前保持未验证。
