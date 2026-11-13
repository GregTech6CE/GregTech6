# 材料重构 07-E：材料与形态注册项去留审计

## 范围与状态

- 主题：按最终运行时注册对象审计材料身份、物品形态、方块形态、获得途径和消费者，完成 [07-E](../development/material-system-refactor/07-cleanup-and-acceptance.md#07-e-定向审计记录) 的去留裁决。
- 状态：在下述双平台、开发环境、无外部附属内容范围内验证通过。08-D—E 的生产快照与 viewer 代码切片现已接入；07-F 的双平台重载、正式客户端展示、机器手工交互和最终发行包仍未执行。
- 源码状态：核对基线为 `525a6ecf6`；本报告包含当前工作区尚未提交的 `MaterialItemCompositions` 共享矿石成分修复与对应 GameTest。
- 平台：Minecraft 1.20.1／Forge 47.4.20／Java 17，Minecraft 1.21.1／NeoForge 21.1.243／Java 21。

## 口径与结果

审计使用最终 GameTest 写出的 `reload-items.tsv`、`reload-forms.tsv`、`reload-block-forms.tsv`、`reload-recipes.tsv` 和 `reload-tags.tsv`，汇总由 [material 形态审计工具](../../tools/material/audit_roster.py)完成。物品数是实际存在成分快照的原生物品，方块数是实际实现材料方块契约的原生方块；两者都不是 Java 源码字符串命中数。来源和消费者计数来自最终 `RecipeMap.mRecipeList`，并展开配方里的物品 tag；手工配方、世界生成、铸币、化石和特殊行为没有统一静态表，因此按下文点名源码补证。

| 对象 | Forge | NeoForge | 结论 |
| --- | ---: | ---: | --- |
| 材料目录 | 815 | 815 | 相同；核心 803 个，另含 12 个测试附属材料（其中 3 个为测试流体身份） |
| 材料物品 | 31,137 | 31,137 | 124 个形态族的逐族计数相同 |
| 材料方块形态 | 3,532 | 3,532 | 13 个方块形态族的逐族计数相同 |
| 有静态配方来源的形态族 | 115 | 115 | 其余 9 族有世界生成、手工、铸币或展示来源 |
| 有静态配方输入的形态族 | 121 | 121 | `coin` 由硬币堆／铸模消费；`oreSmall` 由世界生成和采掘消费 |

803 个核心材料身份中，734 个生成材料物品；另有 15 个有效但无物品形态的身份（8 个反物质、6 个粒子、1 个隐藏 Magic），以及 54 个 `id=-1` 的组／占位视图。`Empty` 是箭头杆、空弹壳和空玻璃管等物品使用的无效哨兵材料，属于预期身份而不是可删除注册项。12 个测试附属材料只存在于测试运行，不进入核心目录。

本轮没有任何材料、物品、方块、流体、NBT、配方或存档身份可依据“无来源、无终端”证明删除，因此全部保留。07-E 只修复了一处双平台成分遗漏：NeoForge 原先只为 16 个 `oreVanillastone` 物品提交成分，而 Forge 为 396 个；共享 `MaterialItemCompositions` 现在通过实际 `OreBlock`／`DenseOreBlock`／`VanillaOreBlock` 类型统一提交，注册 ID 与资源身份不变。最终两边均为 396。

## 无静态配方来源的九族

| 形态 | 获得途径 | 消费者／保留理由 |
| --- | --- | --- |
| `coin` | 铸币台压印；地牢生成硬币堆 | 硬币堆存取、掉落过期入堆、铸模和世界生成仍消费该身份 |
| `ore` | 世界生成矿石方块 | 破碎、洗矿、离心、金属与石材处理输入 |
| `oreSmall` | 小矿世界生成 | 小矿采掘／掉落终端；GT6 明确不为小矿伪造 RecipeMap 输入 |
| `oreDense` | 现有晶体／岩石矿石方块和地层生成 | 按 2U 进入矿石处理、热配方和坩埚输入 |
| `oreVanillastone` | 现有普通矿石方块身份 | 按标准矿石进入处理、熔炼和坩埚输入；本轮修复 NeoForge 缺失成分 |
| `pipeRestrictiveMedium` | 中型物品管道的手工配方 | 限制型物流路由与热回收输入 |
| `pipeRestrictiveLarge` | 大型物品管道的手工配方 | 限制型物流路由与热回收输入 |
| `pipeRestrictiveHuge` | 巨型物品管道的手工配方 | 限制型物流路由与热回收输入 |
| `unit` | 不是可获取的物理材料 | 查看器材料单位占位；挤压、熔炼和坩埚只把对应 289 个身份作为显示／配方单位，不把全部 745 个误当物理燃料 |

## 全量形态清单

“来源”为静态配方输出数，“消费者”为静态配方输入数；括号内分别记录物品／方块。未列入上述九族的形态都至少同时具有实际获得路径和实际输入终端。全部决定均为保留，故本阶段没有兼容迁移、存档迁移或 parity 删除记录。

| 形态 | 物品 | 方块 | 来源（物品／方块） | 消费者（物品／方块） |
| --- | ---: | ---: | ---: | ---: |
| `arrowGtPlastic` | 437 | 0 | 272 / 0 | 372 / 0 |
| `arrowGtWood` | 437 | 0 | 272 / 0 | 315 / 0 |
| `billet` | 258 | 0 | 258 / 0 | 258 / 0 |
| `blockDust` | 0 | 685 | 680 / 680 | 682 / 682 |
| `blockGem` | 0 | 154 | 149 / 149 | 154 / 154 |
| `blockIngot` | 0 | 258 | 254 / 254 | 258 / 258 |
| `blockPlate` | 0 | 350 | 350 / 350 | 350 / 350 |
| `blockPlateGem` | 0 | 151 | 151 / 151 | 151 / 151 |
| `blockRaw` | 0 | 420 | 420 / 420 | 420 / 420 |
| `blockSolid` | 0 | 258 | 223 / 223 | 258 / 258 |
| `bolt` | 473 | 0 | 473 / 0 | 473 / 0 |
| `bouleGt` | 11 | 0 | 11 / 0 | 11 / 0 |
| `bulletGtLarge` | 437 | 0 | 433 / 0 | 437 / 0 |
| `bulletGtMedium` | 437 | 0 | 433 / 0 | 437 / 0 |
| `bulletGtSmall` | 437 | 0 | 433 / 0 | 437 / 0 |
| `casingMachine` | 0 | 124 | 121 / 121 | 124 / 124 |
| `casingMachineDense` | 0 | 124 | 121 / 121 | 124 / 124 |
| `casingMachineDouble` | 0 | 124 | 121 / 121 | 124 / 124 |
| `casingMachineQuadruple` | 0 | 124 | 121 / 121 | 124 / 124 |
| `chain` | 407 | 0 | 407 / 0 | 407 / 0 |
| `chunkGt` | 258 | 0 | 184 / 0 | 258 / 0 |
| `coin` | 351 | 0 | 0 / 0 | 0 / 0 |
| `crushed` | 420 | 0 | 420 / 0 | 420 / 0 |
| `crushedCentrifuged` | 420 | 0 | 420 / 0 | 420 / 0 |
| `crushedCentrifugedTiny` | 420 | 0 | 420 / 0 | 420 / 0 |
| `crushedPurified` | 420 | 0 | 420 / 0 | 420 / 0 |
| `crushedPurifiedTiny` | 420 | 0 | 420 / 0 | 420 / 0 |
| `crushedTiny` | 420 | 0 | 420 / 0 | 420 / 0 |
| `dust` | 693 | 0 | 688 / 0 | 693 / 0 |
| `dustDiv72` | 688 | 0 | 688 / 0 | 688 / 0 |
| `dustSmall` | 688 | 0 | 688 / 0 | 688 / 0 |
| `dustTiny` | 688 | 0 | 687 / 0 | 688 / 0 |
| `foil` | 258 | 0 | 217 / 0 | 258 / 0 |
| `gearGt` | 407 | 0 | 253 / 0 | 407 / 0 |
| `gearGtSmall` | 407 | 0 | 253 / 0 | 407 / 0 |
| `gem` | 164 | 0 | 153 / 0 | 164 / 0 |
| `gemChipped` | 154 | 0 | 153 / 0 | 154 / 0 |
| `gemExquisite` | 150 | 0 | 150 / 0 | 150 / 0 |
| `gemFlawed` | 153 | 0 | 153 / 0 | 153 / 0 |
| `gemFlawless` | 150 | 0 | 150 / 0 | 150 / 0 |
| `gemLegendary` | 150 | 0 | 138 / 0 | 150 / 0 |
| `ingot` | 265 | 0 | 253 / 0 | 265 / 0 |
| `ingotDouble` | 220 | 0 | 203 / 0 | 220 / 0 |
| `ingotQuadruple` | 220 | 0 | 203 / 0 | 220 / 0 |
| `ingotQuintuple` | 220 | 0 | 203 / 0 | 220 / 0 |
| `ingotTriple` | 220 | 0 | 203 / 0 | 220 / 0 |
| `itemCasing` | 407 | 0 | 407 / 0 | 407 / 0 |
| `lens` | 108 | 0 | 108 / 0 | 108 / 0 |
| `minecartWheels` | 207 | 0 | 7 / 0 | 207 / 0 |
| `nugget` | 260 | 0 | 258 / 0 | 260 / 0 |
| `ore` | 0 | 380 | 0 / 0 | 380 / 380 |
| `oreDense` | 21 | 0 | 0 / 0 | 21 / 0 |
| `oreRaw` | 420 | 0 | 420 / 0 | 420 / 0 |
| `oreSmall` | 0 | 380 | 0 / 0 | 0 / 0 |
| `oreVanillastone` | 396 | 0 | 0 / 0 | 396 / 0 |
| `pipeHuge` | 50 | 0 | 49 / 0 | 50 / 0 |
| `pipeLarge` | 50 | 0 | 49 / 0 | 50 / 0 |
| `pipeMedium` | 50 | 0 | 50 / 0 | 50 / 0 |
| `pipeNonuple` | 31 | 0 | 31 / 0 | 31 / 0 |
| `pipeQuadruple` | 31 | 0 | 31 / 0 | 31 / 0 |
| `pipeRestrictiveHuge` | 19 | 0 | 0 / 0 | 19 / 0 |
| `pipeRestrictiveLarge` | 19 | 0 | 0 / 0 | 19 / 0 |
| `pipeRestrictiveMedium` | 19 | 0 | 0 / 0 | 19 / 0 |
| `pipeSmall` | 31 | 0 | 31 / 0 | 31 / 0 |
| `pipeTiny` | 31 | 0 | 30 / 0 | 31 / 0 |
| `plate` | 377 | 0 | 375 / 0 | 377 / 0 |
| `plateCurved` | 350 | 0 | 267 / 0 | 350 / 0 |
| `plateDense` | 222 | 0 | 219 / 0 | 222 / 0 |
| `plateDouble` | 220 | 0 | 206 / 0 | 220 / 0 |
| `plateGem` | 151 | 0 | 150 / 0 | 151 / 0 |
| `plateGemTiny` | 151 | 0 | 151 / 0 | 151 / 0 |
| `plateQuadruple` | 220 | 0 | 206 / 0 | 220 / 0 |
| `plateQuintuple` | 220 | 0 | 206 / 0 | 220 / 0 |
| `plateTiny` | 351 | 0 | 351 / 0 | 351 / 0 |
| `plateTriple` | 220 | 0 | 206 / 0 | 220 / 0 |
| `railGt` | 25 | 0 | 25 / 0 | 25 / 0 |
| `ring` | 407 | 0 | 361 / 0 | 407 / 0 |
| `rockGt` | 449 | 0 | 22 / 0 | 449 / 0 |
| `rotor` | 407 | 0 | 205 / 0 | 407 / 0 |
| `round` | 383 | 0 | 231 / 0 | 383 / 0 |
| `scrapGt` | 688 | 0 | 207 / 0 | 688 / 0 |
| `screw` | 473 | 0 | 473 / 0 | 473 / 0 |
| `spring` | 407 | 0 | 209 / 0 | 407 / 0 |
| `springSmall` | 407 | 0 | 209 / 0 | 407 / 0 |
| `stick` | 475 | 0 | 473 / 0 | 475 / 0 |
| `stickLong` | 473 | 0 | 286 / 0 | 473 / 0 |
| `toolHeadArrow` | 271 | 0 | 271 / 0 | 271 / 0 |
| `toolHeadAxe` | 275 | 0 | 275 / 0 | 275 / 0 |
| `toolHeadAxeDouble` | 275 | 0 | 275 / 0 | 275 / 0 |
| `toolHeadBuilderwand` | 185 | 0 | 8 / 0 | 185 / 0 |
| `toolHeadBuzzSaw` | 185 | 0 | 8 / 0 | 185 / 0 |
| `toolHeadChainsaw` | 185 | 0 | 8 / 0 | 17 / 0 |
| `toolHeadChisel` | 185 | 0 | 185 / 0 | 185 / 0 |
| `toolHeadConstructionPickaxe` | 275 | 0 | 8 / 0 | 275 / 0 |
| `toolHeadDrill` | 185 | 0 | 8 / 0 | 17 / 0 |
| `toolHeadFile` | 185 | 0 | 117 / 0 | 185 / 0 |
| `toolHeadHammer` | 266 | 0 | 147 / 0 | 266 / 0 |
| `toolHeadHoe` | 275 | 0 | 275 / 0 | 275 / 0 |
| `toolHeadPickaxe` | 275 | 0 | 275 / 0 | 275 / 0 |
| `toolHeadPickaxeGem` | 154 | 0 | 1 / 0 | 153 / 0 |
| `toolHeadPlow` | 275 | 0 | 275 / 0 | 275 / 0 |
| `toolHeadRawArrow` | 271 | 0 | 10 / 0 | 271 / 0 |
| `toolHeadRawAxe` | 275 | 0 | 155 / 0 | 275 / 0 |
| `toolHeadRawAxeDouble` | 275 | 0 | 8 / 0 | 275 / 0 |
| `toolHeadRawChisel` | 185 | 0 | 8 / 0 | 185 / 0 |
| `toolHeadRawHoe` | 275 | 0 | 155 / 0 | 275 / 0 |
| `toolHeadRawPickaxe` | 275 | 0 | 155 / 0 | 275 / 0 |
| `toolHeadRawPlow` | 275 | 0 | 10 / 0 | 275 / 0 |
| `toolHeadRawSaw` | 185 | 0 | 117 / 0 | 185 / 0 |
| `toolHeadRawSense` | 275 | 0 | 10 / 0 | 275 / 0 |
| `toolHeadRawShovel` | 275 | 0 | 155 / 0 | 275 / 0 |
| `toolHeadRawSpade` | 275 | 0 | 10 / 0 | 275 / 0 |
| `toolHeadRawSword` | 275 | 0 | 155 / 0 | 275 / 0 |
| `toolHeadRawUniversalSpade` | 185 | 0 | 8 / 0 | 185 / 0 |
| `toolHeadSaw` | 185 | 0 | 185 / 0 | 185 / 0 |
| `toolHeadScrewdriver` | 185 | 0 | 8 / 0 | 185 / 0 |
| `toolHeadSense` | 275 | 0 | 275 / 0 | 275 / 0 |
| `toolHeadShovel` | 275 | 0 | 275 / 0 | 275 / 0 |
| `toolHeadSpade` | 275 | 0 | 275 / 0 | 275 / 0 |
| `toolHeadSword` | 275 | 0 | 275 / 0 | 275 / 0 |
| `toolHeadUniversalSpade` | 185 | 0 | 185 / 0 | 185 / 0 |
| `toolHeadWrench` | 185 | 0 | 8 / 0 | 17 / 0 |
| `unit` | 745 | 0 | 0 / 0 | 289 / 0 |
| `wireFine` | 216 | 0 | 214 / 0 | 216 / 0 |

## 修复与回归

`MaterialItemCompositions.registerOreBlockComposition(Block)` 统一识别三类实际矿石方块：

- `DenseOreBlock` → `oreDense`
- `VanillaOreBlock` → `oreVanillastone`
- 非小型 `OreBlock` → `oreVanillastone`

共享 `registerCommon()` 遍历 `GTBlocks`，Forge 和 NeoForge 的平台图标方块注册器复用同一函数。这样平台注册位置仍由各自加载器决定，材料成分事实不再由 Forge 专属代码单独补齐。

新增 GameTest `oreBlocksUseTheirDeclaredMaterialForm` 遍历当前平台实际注册的所有上述矿石方块，检查其物品成分存在、形态正确且材料与方块声明一致。最终双平台快照中 `oreVanillastone` 均为 396，124 个形态族的物品和方块计数全部一致。

## 验证

执行且通过：

- `compileBothPlatforms`：Forge 与 NeoForge 生产源码编译通过。
- 定向 Spotless：四个改动源码文件输出 `IS CLEAN`。
- Forge `runGameTestServer`：`All 95 required tests passed`。
- NeoForge `runGameTestServer`：`All 100 required tests passed`。
- 双平台最终运行时快照：124 个形态族、31,137 个材料物品、3,532 个材料方块形态，逐族计数一致；`oreVanillastone` 均为 396。

执行但失败或阻塞：无。

尚未执行：

- 07-F 的完整来源／用途索引、有界处理图、远程客户端 tooltip／JEI／EMI 和机器手工交互。
- 最终 Forge `distributionJar`／NeoForge `jar` 的发行包内容检查。
- 真实外部附属材料与可选模组环境。

本报告证明的是 07-E 的材料／形态去留清单和本轮必要迁移，不等同于 07-F 的完整处理图验收。08-D—E 已把静态配方、活动世界生成和符号单位选择器接入同一快照，但动态族不展开为全体物理候选；完整边集合仍须按运行期清单确认覆盖范围。
