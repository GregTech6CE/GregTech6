# 营养系统移除

本端移除 GT6 的酒精、咖啡因、脱水、糖、脂肪五项食物统计，连同其阈值效果、衰减、过量伤害、五种伤害类型与两个配置项；辐射只保留反应堆/附魔来源与两条医疗清除行。饮料侧按“无独立生产配方来源”的闭包清理：世界生成与瓶装 Canner 环不算来源，死配方递归传播，共停注 66 条流体；13 个纯闭环物品与另外 49 个受影响瓶装物品保留，并通过包私有的 `BottleDrinkFallback` 维持原饮用数据。RFC 见 [#363](https://github.com/GregTech6CE/GregTech6/issues/363)。

| 内容 | 差异 | 原因 |
| --- | --- | --- |
| 五项统计 | 删除酒精、咖啡因、脱水、糖、脂肪的累积、每 100 tick 衰减、每 50 tick 阈值效果与 100 点过量伤害；`gt.props.food` 不再读取，旧存档中的该复合标签保留为孤儿数据。 | 系统没有 HUD、GUI、tooltip 数值、命令或成就，写入点只有吃食物、喝饮料与三明治/罐头三处，与机器、能源、材料、世界生成无交互；RFC #363 判定其玩法深度与反馈不足。 |
| 伤害类型 | 删除 `alcohol`、`caffeine`、`dehydration`、`sugar`、`fat` 五个 `damage_type` JSON、`GTDamageTypes` 的常量/工厂/`ALL` 条目、四个共享伤害标签中的成员，以及中英文各 10 条死亡键；radiation 与其标签/语言键保留。 | 五个伤害类型只被五项统计的过量伤害使用，统计移除后没有调用方。 |
| 配置 | 删除 `general.NutritionSystem` 与 `general.DeathByOverdosingCertainFoods`；保留 `general.AFK_Hunger` 与 `general.drinks_always_drinkable`。 | 原配置只关闭脱水/糖/脂肪的效果，不能真正关闭系统；移除后不再保留半失效开关。旧 TOML 键由加载器忽略。 |
| 食物与饮料数据 | `FoodStatisticRows` 从 191 行缩为 2 条医疗清除行；`GTFoodItemsGen` 251 行、`CannedFoodCatalog` 57 行、`DrinkNutritionRows`/`GTDrinksGen` 从 199 行降至 134 行、`SandwichBottleFoods` 29 行删除五项统计列，保留食物值、饱和度、容器、罐头/三明治行为、饮料即时效果、`invisibleParticles`、`temperatureEffect`。 | 五项统计与普通食物/饮料行为解耦；删列不影响 `FoodProperties`、空容器返还、罐头腐烂、三明治层、饮料 milk/extinguish/explosive 或饮用音效。 |
| 辐射 | 保留 `PlayerRadiation` 的剂量、症状、伤害与克隆逻辑；食物/饮料不再增加辐射，`FoodStatisticRows` 只保留 `radaway`（-50）与 `cure_all`（-999）两条医疗清除行；4 条香蕉饮料的辐射值置 0。`cure_all` 的 `-999` 是重置哨兵，经 0..127 clamp 后归零。 | 辐射来源只保留反应堆与附魔照射；食物/饮料的正向辐射与核玩法无关。Radaway/Cure All 是清除辐射的医疗物品，不属于食品辐射。 |
| AFK 饥饿 | 从 `PlayerFoodStats.tick` 迁到 `AfkHungerEvents`：仍按 `player.tickCount % 2400 == 1200`、背包/盔甲重量与 `general.AFK_Hunger` 结算。 | AFK 饥饿是 GT6 的独立分支，不属于五项统计；迁移保持触发相位、权重与配置语义不变。 |
| 无独立来源饮料流体 | 停注 66 条流体：13 条纯闭环 + 51 条无独立生产配方/死配方 + 孤立 `Sap`。删除对应 `GTDrinksGen` 行、Canner 装/倒配方、语言键与流体贴图。分组清单见[附录A](#附录a-停注的饮料流体)。 | 闭包规则：世界生成与瓶装 Canner 环不算来源；当死流体导致下游配方失去唯一输入时，下游流体一并删除。BAWLS/HolyWater/Purple_Drink 等原世界生成饮也按此规则删除，地牢流体改为现有饮品/水。 |
| 瓶装物品保留与回退 | 保留 `GTBottlesGen` 的 137 条映射、`GTMultiItemsGen` 的物品 id、模型/贴图、`item.gt6ce.*` 语言键、堆组与创造页位置。62 个受影响瓶装物品在 `BottleItem.fluid() == null` 时读取 `BottleDrinkFallback`（67 条，含 5 条原本无流体的金色瓶），右键仍可饮用；生存消耗并返还空瓶，创造模式不消耗、不返还。`BottleMachineRecipes.missing()` 记录这些物品。 | 原版 GT6 中这些瓶装物品可饮用；保留物品与饮用行为，避免移除流体引入新的可用性差异。创造模式不返还空瓶与原版 `FoodStat.onEaten` 的 `hasInfiniteItems` 行为一致。 |
| 地牢世界生成 | 地窖酒杯/兵营杯/工坊桶/储罐中引用已删流体的槽位替换为现有饮品或水，数组长度、选择下标与 RNG 顺序不变；`DungeonBottleSupplies.drinks` 补齐 `Rum_White`、`Wine_Lemon`、`Soda`、`Lemonade` 映射。 | 世界生成不再作为流体保留理由；替换保持结构布局与随机消耗不变，避免杯子/桶消失。后续附属可自行注入这些世界生成内容。 |
| Cure All | `cure_all` 从创造页隐藏集合移除，恢复创造模式与 EMI 可见；`gt.misc` 战利品表按原版 `Loader_Loot` 增加 `36|2|4` 行。辐射清除仍由 `-999` 行执行；原版的清药水效果、灭火与治疗僵尸尚未移植，记录为差异。 | 用户要求来源参照原版并补创造模式/EMI；当前最小对齐只恢复可获得性与可见性。 |
| 资源清理 | 删除 66 条流体的 `fluid.gt6ce.*`、`fluid_type.gt6ce.*`、中文遗留 `fluid.*` 与 `gt.drink.*` 键（英文 103 条、中文 205 条），以及 102 个流体贴图/mcmeta；保留物品侧语言键、模型与贴图。 | 避免无主语言键与无主贴图；物品保留时不能删除物品侧资源。 |
| 存档兼容 | 旧 `gt.props.food` 不再读取；旧储罐/机器中的已停注流体按本端既有流体处理方式静默清空；62 个瓶装物品 ID 不变；旧 TOML 的两个键被忽略。 | 本端没有流体迁移机制，且物品身份优先保留；不新增迁移工具。 |
| API 与附属 | 不新增公开饮料 API，不改 API 版本；`BottleDrinkFallback` 是 `content.item` 内的包私有表，只被 `BottleItem` 使用。保留 `GTDrinks`、`GTBottles`、`BottleItem`、`DrinkNutritionRows` 等现有公开类可被引用。 | 物品级回退只修复“移除流体导致物品不可用”的差异，不开放新的附属契约；生存来源与世界生成由附属后续处理。 |

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

### 第二批：无独立来源/死配方闭包（52 条）

| 类别 | 流体 |
| --- | --- |
| 无来源果汁/树液 | `Juice`、`Juice_Coconut`、`Juice_Currant`、`Juice_Fig`、`Juice_Kiwi`、`Juice_Mango`、`Juice_Papaya`、`Juice_Persimmon`、`Juice_Starfruit`、`Juice_Hellderberry`、`Juice_AppleGrC`、`Juice_Grape_Green`、`Juice_Grape_Purple`、`Juice_Potato`、`Sap`、`Sap_Maple`、`Sap_Rainbow` |
| 死冰沙 | `Smoothie_Fruit`、`Smoothie_Currant`、`Smoothie_Kiwi`、`Smoothie_Mango`、`Smoothie_Papaya`、`Smoothie_Persimmon`、`Smoothie_Starfruit`、`Smoothie_Grape_Green`、`Smoothie_Grape_Purple`、`Smoothie_Coconut` |
| 椰奶下游 | `Cream_Coconut`、`Pina_Colada` |
| 酒/醋/药 | `Wine_Grape_Green`、`Wine_Grape_Purple`、`Vinegar_Grape`、`Med_Laxative`、`Med_Heal`、`Vodka`、`Leninade`、`Beer_Dark`、`Beer_Dragonblood`、`Syrup_Maple` |
| 酱汁链 | `Sauce_Chili`、`Sauce_BBQ`、`Sauce_Hot`、`Sauce_Diabolo`、`Sauce_Diablo`、`Sauce_Cow_Level` |
| 无来源/世界生成饮 | `BAWLS`、`Holywater`、`Purple_Drink`、`Rotten_Drink`、`MilkSoy` |

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
| `GTDrinksGen` | 199 → 134（删除 65 行） |
| `SandwichBottleFoods` | 29 行，删除统计列 |
| `GTBottlesGen` | 137 条不变 |
| `BottleDrinkFallback` | 67 条（13 条首批 + 49 条第二批 + 5 条金色瓶） |
| 营养伤害类型 | 5 删除，radiation 保留 |
| 营养死亡键 | 每语言 10 条删除，radiation 保留 |
| 营养配置键 | 2 删除 |
| 流体语言键 | 英文 103、中文 205 删除 |
| 流体贴图 | 102 个文件（51 组）删除 |
| Cure All | 创造页/EMI 恢复可见，`gt.misc` 增加 `36|2|4` 行 |
| 地牢流体 | 杯/桶/储罐替换为现有饮品或水，数组长度与 RNG 顺序不变 |
