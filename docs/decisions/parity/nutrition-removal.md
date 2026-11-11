# 营养系统移除

本端移除 GT6 的酒精、咖啡因、脱水、糖、脂肪五项食物统计，连同其阈值效果、衰减、过量伤害、五种伤害类型与两个配置项；辐射只保留反应堆/附魔来源与两条医疗清除行。饮料侧的对齐口径是 `F(饮料, 玩家, 配置)` 不变：饮料自身属性、玩家饱腹状态与 `general.drinks_always_drinkable` 共同决定能否饮用。按“无独立生产配方来源”的闭包停注 69 条流体；65 个受影响瓶装物品通过包私有的 `BottleDrinkFallback` 保持同一 F，5 个金色瓶保留原流体并按原版路径饮用。枫树液、彩虹树汁与枫树糖浆因取汁洞/彩虹染料链保留；`Ambrosia` 作为来源产物保留，`Med_Heal` 停注。RFC 见 [#363](https://github.com/GregTech6CE/GregTech6/issues/363)。

| 内容 | 差异 | 原因 |
| --- | --- | --- |
| 五项统计 | 删除酒精、咖啡因、脱水、糖、脂肪的累积、每 100 tick 衰减、每 50 tick 阈值效果与 100 点过量伤害；`gt.props.food` 不再读取，旧存档中的该复合标签保留为孤儿数据。 | 系统没有 HUD、GUI、tooltip 数值、命令或成就，写入点只有吃食物、喝饮料与三明治/罐头三处，与机器、能源、材料、世界生成无交互；RFC #363 判定其玩法深度与反馈不足。 |
| 伤害类型 | 删除 `alcohol`、`caffeine`、`dehydration`、`sugar`、`fat` 五个 `damage_type` JSON、`GTDamageTypes` 的常量/工厂/`ALL` 条目、四个共享伤害标签中的成员，以及中英文各 10 条死亡键；radiation 与其标签/语言键保留。 | 五个伤害类型只被五项统计的过量伤害使用，统计移除后没有调用方。 |
| 配置 | 删除 `general.NutritionSystem` 与 `general.DeathByOverdosingCertainFoods`；保留 `general.AFK_Hunger` 与 `general.drinks_always_drinkable`。 | 原配置只关闭脱水/糖/脂肪的效果，不能真正关闭系统；移除后不再保留半失效开关。旧 TOML 键由加载器忽略。 |
| 饮料可饮用判定 | 饮料侧只对齐 `F(饮料, 玩家, 配置)`：饮料自身属性（食物值、饱和度、即时效果、`alwaysEdible`、milk/extinguish/explosive 等）、玩家饱腹状态与 `general.drinks_always_drinkable` 决定右键与持续使用是否进入饮用。被停注流体的 65 个瓶装物品通过 `BottleDrinkFallback` 保持同一 F；5 个金色瓶保留原流体，走原版路径。 | 移除流体不应改变“这瓶饮料能不能喝”的结果；流体、世界生成等内容的清理不属于该判定口径。 |
| 食物与饮料数据 | `FoodStatisticRows` 从 191 行缩为 2 条医疗清除行；`GTFoodItemsGen` 251 行、`CannedFoodCatalog` 57 行、`DrinkNutritionRows`/`GTDrinksGen` 从 199 行降至 131 行、`SandwichBottleFoods` 29 行删除五项统计列，保留食物值、饱和度、容器、罐头/三明治行为、饮料即时效果、`invisibleParticles`、`temperatureEffect`。 | 五项统计与普通食物/饮料行为解耦；删列不影响 `FoodProperties`、空容器返还、罐头腐烂、三明治层、饮料 milk/extinguish/explosive 或饮用音效。 |
| 辐射 | 保留 `PlayerRadiation` 的剂量、症状、伤害与克隆逻辑；食物/饮料不再增加辐射，`FoodStatisticRows` 只保留 `radaway`（-50）与 `cure_all`（-999）两条医疗清除行；4 条香蕉饮料的辐射值置 0。`cure_all` 的 `-999` 是重置哨兵，经 0..127 clamp 后归零。 | 辐射来源只保留反应堆与附魔照射；食物/饮料的正向辐射与核玩法无关。Radaway/Cure All 是清除辐射的医疗物品，不属于食品辐射。 |
| AFK 饥饿 | 从 `PlayerFoodStats.tick` 迁到 `AfkHungerEvents`：仍按 `player.tickCount % 2400 == 1200`、背包/盔甲重量与 `general.AFK_Hunger` 结算。 | AFK 饥饿是 GT6 的独立分支，不属于五项统计；迁移保持触发相位、权重与配置语义不变。 |
| 无独立来源饮料流体 | 停注 69 条流体：14 条纯闭环/孤立（13 条纯闭环 + `Tea_Ice`）+ 49 条无独立生产配方/死配方闭包 + 6 条源头物品不可获得的死冰沙。删除对应 `GTDrinksGen` 行（68 行）、Canner 装/倒配方、语言键与流体贴图。分组清单见[附录A](#附录a-停注的饮料流体)。 | 闭包规则：世界生成与瓶装 Canner 环不算来源；当死流体导致下游配方失去唯一输入时，下游流体一并删除。BAWLS/HolyWater/Purple_Drink 等原世界生成饮也按此规则删除，地牢流体改为现有饮品/水。`Ambrosia` 因蜂巢与 Mixer 来源保留，`Med_Heal` 按饮料流体停注。 |
| 瓶装物品保留与回退 | 保留 `GTBottlesGen` 的 137 条映射、`GTMultiItemsGen` 的物品 id、模型/贴图、`item.gt6ce.*` 语言键、堆组与创造页位置。65 个受影响瓶装物品在 `BottleItem.fluid() == null` 时读取 `BottleDrinkFallback`（65 条），右键仍可饮用；回退行与正常路径共用 `GTDrinks.canEat`，因此 `F(饮料, 玩家, 配置)` 与停注前一致。5 个金色瓶（`golden_apple_juice`、`golden_cider`、`iduns_apple_juice`、`notches_brew`、`golden_carrot_juice`）的流体仍注册，走原版路径，不进入 fallback。生存消耗并返还空瓶，创造模式不消耗、不返还。`BottleMachineRecipes.missing()` 记录这 65 个物品。 | 原版 GT6 中这些瓶装物品可饮用；保留物品与 F 不变，避免移除流体引入新的可饮用性差异。5 个金色瓶本来就有流体，按原版表现处理，不额外补回退行。创造模式不返还空瓶与原版 `FoodStat.onEaten` 的 `hasInfiniteItems` 行为一致。 |
| 地牢世界生成 | 地窖酒杯/兵营杯/工坊桶/储罐中引用已删流体的槽位替换为现有饮品或水，数组长度、选择下标与 RNG 顺序不变；`DungeonBottleSupplies.drinks` 补齐 `Rum_White`、`Wine_Lemon`、`Soda`、`Lemonade` 映射。 | 世界生成不再作为流体保留理由；替换保持结构布局与随机消耗不变，避免杯子/桶消失。后续附属可自行注入这些世界生成内容。 |
| Cure All | `cure_all` 从创造页隐藏集合移除，恢复创造模式与 EMI 可见；`gt.misc` 战利品表按原版 `Loader_Loot` 增加 `36|2|4` 行。辐射清除仍由 `-999` 行执行；原版 Cure All 的直接使用仍只做辐射清除，清药水效果与灭火在 Cure All 作为三明治层时由 `SandwichNutrition` 执行，治疗僵尸尚未移植，记录为差异。 | 用户要求来源参照原版并补创造模式/EMI；当前最小对齐只恢复可获得性与可见性。 |
| 资源清理 | 删除 69 条流体的 `fluid.gt6ce.*`、`fluid_type.gt6ce.*`、中文遗留 `fluid.*` 与 `gt.drink.*` 键（英文 137 条、中文 273 条），以及 136 个流体贴图/mcmeta（68 组）；保留物品侧语言键、模型与贴图。 | 避免无主语言键与无主贴图；物品保留时不能删除物品侧资源。 |
| 存档兼容 | 旧 `gt.props.food` 不再读取；旧储罐/机器中的已停注流体按本端既有流体处理方式静默清空；65 个受影响瓶装物品 ID 不变；旧 TOML 的两个键被忽略。 | 本端没有流体迁移机制，且物品身份优先保留；不新增迁移工具。 |
| 无来源物品 | `chum`、`chum_burger`、`chum_on_a_stick`、`chum_ice_cream` 的物品 id、模型、贴图、语言键、堆组与创造页位置保留；`SingleItemSourceRecipes.chum()` 因输入 `Purple_Drink` 停注而删除，`chum` 与 `chum_ice_cream` 失去唯一来源，`chum_burger`/`chum_on_a_stick` 在本端原本就没有配方，因此四个物品均为惰性（已注册、不可合成）。 | 物品身份优先保留；不为死配方补替代输入，避免引入未声明的玩法变化。 |
| API 与附属 | 不新增公开饮料 API，不改 API 版本；`BottleDrinkFallback` 是 `content.item` 内的包私有表，只被 `BottleItem` 使用。保留 `GTDrinks`、`GTBottles`、`BottleItem`、`DrinkNutritionRows` 等现有公开类可被引用。`FoodNutritionRows.Entry`、`DrinkNutritionRows.Row`、`GTFoodStats.Row` 的 record 形状变化，直接引用这些内部类的附属需要重新编译；`api/` 子模块与 API 版本未动。 | 物品级回退只修复“移除流体导致物品不可用”的差异，不开放新的附属契约；生存来源与世界生成由附属后续处理。 |

## 附录A 停注的饮料流体

### 第一批：纯闭环与孤立流体（14 条）

| 流体 | 注册名 | 瓶装物品 |
| --- | --- | --- |
| `Sake` | `potion.sake` | `sake` |
| `Dressing` | `potion.dressing` | `dressing` |
| `Smoothie_Apricot` | `apricotsmoothie` | `apricot_smoothie` |
| `Smoothie_Cherry` | `cherrysmoothie` | `cherry_smoothie` |
| `Smoothie_Elderberry` | `elderberrysmoothie` | `elderberry_smoothie` |
| `Smoothie_Grapefruit` | `grapefruitsmoothie` | `grapefruit_smoothie` |
| `Smoothie_Lime` | `limesmoothie` | `lime_smoothie` |
| `Smoothie_Orange` | `orangesmoothie` | `orange_smoothie` |
| `Smoothie_Peach` | `peachsmoothie` | `peach_smoothie` |
| `Smoothie_Pear` | `pearsmoothie` | `pear_smoothie` |
| `Smoothie_Plum` | `plumsmoothie` | `plum_smoothie` |
| `Smoothie_Grape_Red` | `redgrapesmoothie` | `grape_smoothie_2` |
| `Smoothie_Grape_White` | `whitegrapesmoothie` | `grape_smoothie` |
| `Tea_Ice` | `icetea` | 无 |

### 第二批：无独立来源/死配方闭包（49 条）

| 类别 | 流体 |
| --- | --- |
| 无来源果汁/树液 | `Juice`、`Juice_Coconut`、`Juice_Currant`、`Juice_Fig`、`Juice_Kiwi`、`Juice_Mango`、`Juice_Papaya`、`Juice_Persimmon`、`Juice_Starfruit`、`Juice_Hellderberry`、`Juice_AppleGrC`、`Juice_Grape_Green`、`Juice_Grape_Purple`、`Juice_Potato`、`Sap` |
| 死冰沙 | `Smoothie_Fruit`、`Smoothie_Currant`、`Smoothie_Fig`、`Smoothie_Kiwi`、`Smoothie_Mango`、`Smoothie_Papaya`、`Smoothie_Persimmon`、`Smoothie_Starfruit`、`Smoothie_Grape_Green`、`Smoothie_Grape_Purple`、`Smoothie_Coconut` |
| 椰奶下游 | `Cream_Coconut`、`Pina_Colada` |
| 酒/醋/药 | `Mash_Hops`、`Wine_Grape_Green`、`Wine_Grape_Purple`、`Vinegar_Grape`、`Med_Laxative`、`Med_Heal`、`Vodka`、`Leninade`、`Beer_Dark`、`Beer_Dragonblood` |
| 酱汁链 | `Sauce_Chili`、`Sauce_BBQ`、`Sauce_Hot`、`Sauce_Diabolo`、`Sauce_Diablo`、`Sauce_Cow_Level` |
| 无来源/世界生成饮 | `BAWLS`、`Holywater`、`Purple_Drink`、`Rotten_Drink`、`MilkSoy` |

### 第三批：源头物品不可获得的死冰沙（6 条）

`gt6ce:banana`、`gt6ce:ananas`、`gt6ce:gooseberry`、`gt6ce:lemon`、`gt6ce:pomegranate`、`gt6ce:strawberry` 在本端只有食物条目/标签，没有作物、种子、浆果丛或世界生成来源，因此对应果汁与冰沙都是死配方。

| 流体 | 注册名 | 瓶装物品 |
| --- | --- | --- |
| `Smoothie_Ananas` | `pineapplesmoothie` | `ananas_smoothie` |
| `Smoothie_Banana` | `bananasmoothie` | `banana_smoothie` |
| `Smoothie_Gooseberry` | `gooseberrysmoothie` | `gooseberry_smoothie` |
| `Smoothie_Lemon` | `lemonsmoothie` | `lemon_smoothie` |
| `Smoothie_Pomegranate` | `pomegranatesmoothie` | `pomegranate_smoothie` |
| `Smoothie_Strawberry` | `strawberrysmoothie` | `strawberry_smoothie` |

`Water_Geothermal` 不属于饮料闭包，作为锅炉/热交换/化学环境水保留。

## 附录B 保留的辐射行

| 食物 | 辐射值 |
| --- | ---: |
| `gt6ce:radaway` | -50 |
| `gt6ce:cure_all` | -999（重置哨兵，clamp 后为 0） |

`banana`、`banana_slice`、`banana_ice_cream` 与 `bananasmoothie`、`binnie.juicebanana`、`binnie.liqueurbanana`、`binnie.winebanana` 的辐射值已全部置 0 或移除；食物/饮料不再增加辐射。

## 附录C 数据规模

| 项目 | 变化 |
| --- | --- |
| `FoodStatisticRows` | 191 → 2 |
| `GTFoodItemsGen` | 251 行，删除统计列 |
| `CannedFoodCatalog` | 57 行，删除统计列 |
| `GTDrinksGen` | 199 → 131（删除 68 行） |
| `SandwichBottleFoods` | 29 行，删除统计列 |
| `GTBottlesGen` | 137 条不变 |
| `BottleDrinkFallback` | 65 条（13 条首批 + 46 条第二批 + 6 条第三批） |
| 营养伤害类型 | 5 删除，radiation 保留 |
| 营养死亡键 | 每语言 10 条删除，radiation 保留 |
| 营养配置键 | 2 删除 |
| 流体语言键 | 英文 137、中文 273 删除 |
| 流体贴图 | 136 个文件（68 组）删除 |
| Cure All | 创造页/EMI 恢复可见，`gt.misc` 增加 `36|2|4` 行 |
| 地牢流体 | 杯/桶/储罐替换为现有饮品或水，数组长度与 RNG 顺序不变 |
