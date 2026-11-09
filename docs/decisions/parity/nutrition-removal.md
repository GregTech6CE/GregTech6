# 营养系统移除

本端移除 GT6 的酒精、咖啡因、脱水、糖、脂肪五项食物统计，连同其阈值效果、衰减、过量伤害、五种伤害类型与两个配置项；辐射作为第六项统计保留。与五项统计绑定的 13 条纯闭环饮料流体与孤立流体 `Tea_Ice` 一并停注，对应瓶装物品保留为已注册但不可饮用的物品；本次不新增饮料 API，也不提供恢复附属。RFC 见 [#363](https://github.com/GregTech6CE/GregTech6/issues/363)。

| 内容 | 差异 | 原因 |
| --- | --- | --- |
| 五项统计 | 删除酒精、咖啡因、脱水、糖、脂肪的累积、每 100 tick 衰减、每 50 tick 阈值效果与 100 点过量伤害；`gt.props.food` 不再读取，旧存档中的该复合标签保留为孤儿数据。 | 系统没有 HUD、GUI、tooltip 数值、命令或成就，写入点只有吃食物、喝饮料与三明治/罐头三处，与机器、能源、材料、世界生成无交互；RFC #363 判定其玩法深度与反馈不足。 |
| 伤害类型 | 删除 `alcohol`、`caffeine`、`dehydration`、`sugar`、`fat` 五个 `damage_type` JSON、`GTDamageTypes` 的常量/工厂/`ALL` 条目、四个共享伤害标签中的成员，以及中英文各 10 条死亡键；radiation 与其标签/语言键保留。 | 五个伤害类型只被五项统计的过量伤害使用，统计移除后没有调用方。 |
| 配置 | 删除 `general.NutritionSystem` 与 `general.DeathByOverdosingCertainFoods`；保留 `general.AFK_Hunger` 与 `general.drinks_always_drinkable`。 | 原配置只关闭脱水/糖/脂肪的效果，不能真正关闭系统；移除后不再保留半失效开关。旧 TOML 键由加载器忽略。 |
| 食物与饮料数据 | `FoodStatisticRows` 从 191 行缩为 5 条辐射行；`GTFoodItemsGen` 251 行、`CannedFoodCatalog` 57 行、`DrinkNutritionRows`/`GTDrinksGen` 199 行、`SandwichBottleFoods` 29 行删除五项统计列，保留食物值、饱和度、容器、罐头/三明治行为、饮料即时效果、`invisibleParticles`、`temperatureEffect` 与 radiation。 | 五项统计与普通食物/饮料行为解耦；删列不影响 `FoodProperties`、空容器返还、罐头腐烂、三明治层、饮料 milk/extinguish/explosive 或饮用音效。 |
| 辐射 | 保留 `PlayerRadiation` 的剂量、症状、伤害与克隆逻辑；食物侧保留 5 条辐射行（banana、banana_slice、banana_ice_cream、radaway、cure_all），饮料侧保留 4 条香蕉类辐射行（各 +5）。`cure_all` 的 `-999` 是重置哨兵，经 0..127 clamp 后归零。 | 辐射不属于五项营养统计，且与核反应堆、防护装备、Radaway 与 Cure All 绑定；移除会改变核玩法。 |
| AFK 饥饿 | 从 `PlayerFoodStats.tick` 迁到 `AfkHungerEvents`：仍按 `player.tickCount % 2400 == 1200`、背包/盔甲重量与 `general.AFK_Hunger` 结算。 | AFK 饥饿是 GT6 的独立分支，不属于五项统计；迁移保持触发相位、权重与配置语义不变。 |
| 纯闭环饮料流体 | 停注 13 条流体：`Sake`、`Dressing`、11 个无产出 Smoothie；同时删除对应 `GTDrinksGen` 行、Canner 装/倒配方、语言键与流体贴图。清单见[附录A](#附录a-停注的饮料流体)。 | 连通性审计确认它们只有“瓶装物品 ↔ 流体”的 Canner 双向边，没有作物、冰/水、Biomass、燃料、世界生成、冰激凌、染料或三明治流体消费者；删流体不会改变 C 类饮料链。 |
| 瓶装物品保留 | 保留 `GTBottlesGen` 的 13 条映射、`GTMultiItemsGen` 的物品 id、模型/贴图、`item.gt6ce.*` 语言键、堆组与创造页位置。`BottleItem.fluid()` 返回 null，物品右键不消耗、不动作；`BottleMachineRecipes.missing()` 记录这 13 条。 | 保留旧存档物品身份与可引用性；本次不新增饮料专属 API，也不做恢复附属，未来附属可通过现有公开类恢复。 |
| 孤立流体 `Tea_Ice` | 停注 `Tea_Ice`（`icetea`）、其 `GTDrinksGen` 行、语言键、贴图与动画描述；没有对应瓶装物品。 | 无瓶装、无配方、无消费者，属于无人可获得的孤立饮料流体。 |
| 不处理的 C 类流体 | 保留 Fig/Mango/Papaya/Persimmon/Starfruit 等来源缺失果汁、葡萄/土豆/椰子/猕猴桃/醋栗、酒/醋/茶/咖啡/奶/蜂蜜/酱汁、Biomass 输入、燃料与地牢流体。 | 这些分量至少有一个外部锚点（Ice/Water、作物、Biomass、燃料、世界生成、冰激凌、染料或三明治）；删除会连带移除下游配方或世界生成。 |
| 资源清理 | 删除 14 条流体的 `fluid.gt6ce.*`、`fluid_type.gt6ce.*`、中文遗留 `fluid.*` 与 `gt.drink.*` 键（英文 28 条、中文 56 条），以及 14 组流体贴图与 `.png.mcmeta`；保留物品侧语言键、模型与贴图。 | 避免无主语言键与无主贴图；物品保留时不能删除物品侧资源。 |
| 存档兼容 | 旧 `gt.props.food` 不再读取；旧储罐/机器中的 14 条已停注流体按本端既有流体处理方式静默清空；13 个瓶装物品 ID 不变；旧 TOML 的两个键被忽略。 | 本端没有流体迁移机制，且物品身份优先保留；不新增迁移工具。 |
| API 与附属 | 不新增 `AddonDrinks` 或物品级饮料表，不改 API 版本；保留 `GTDrinks`、`GTBottles`、`BottleItem`、`DrinkNutritionRows` 等现有公开类可被引用。 | 本次只做移除；恢复 Sake/Dressing/冰沙的饮用行为与来源由后续独立附属按现有公开类处理。 |

## 附录A 停注的饮料流体

| 流体 | 注册名 | 瓶装物品 | 保留的 Canner 行 |
| --- | --- | --- | --- |
| `Sake` | `potion.sake` | `sake` | 无，`GTBottlesGen` 条目保留 |
| `Dressing` | `potion.dressing` | `dressing` | 无，物品仍作三明治层 |
| `Smoothie_Apricot` | `apricotsmoothie` | `apricot_smoothie` | 无 |
| `Smoothie_Cherry` | `cherrysmoothie` | `cherry_smoothie` | 无 |
| `Smoothie_Elderberry` | `elderberrysmoothie` | `elderberry_smoothie` | 无 |
| `Smoothie_Grapefruit` | `grapefruitsmoothie` | `grapefruit_smoothie` | 无 |
| `Smoothie_Lime` | `limesmoothie` | `lime_smoothie` | 无 |
| `Smoothie_Orange` | `orangesmoothie` | `orange_smoothie` | 无 |
| `Smoothie_Peach` | `peachsmoothie` | `peach_smoothie` | 无 |
| `Smoothie_Pear` | `pearsmoothie` | `pear_smoothie` | 无 |
| `Smoothie_Plum` | `plumsmoothie` | `plum_smoothie` | 无 |
| `Smoothie_Grape_Red` | `redgrapesmoothie` | `grape_smoothie_2` | 无 |
| `Smoothie_Grape_White` | `whitegrapesmoothie` | `grape_smoothie` | 无 |
| `Tea_Ice` | `icetea` | 无 | 无 |

## 附录B 保留的辐射行

| 食物 | 辐射值 |
| --- | ---: |
| `gt6ce:banana` | 4 |
| `gt6ce:banana_slice` | 1 |
| `gt6ce:banana_ice_cream` | 2 |
| `gt6ce:radaway` | -50 |
| `gt6ce:cure_all` | -999（重置哨兵，clamp 后为 0） |

饮料侧保留 `bananasmoothie`、`binnie.juicebanana`、`binnie.liqueurbanana`、`binnie.winebanana` 四条，辐射值均为 5。

## 附录C 数据规模

| 项目 | 变化 |
| --- | --- |
| `FoodStatisticRows` | 191 → 5 |
| `GTFoodItemsGen` | 251 行，删除统计列 |
| `CannedFoodCatalog` | 57 行，删除统计列 |
| `GTDrinksGen` | 199 → 185（删除 14 条） |
| `SandwichBottleFoods` | 29 行，删除统计列 |
| `GTBottlesGen` | 137 条不变 |
| 营养伤害类型 | 5 删除，radiation 保留 |
| 营养死亡键 | 每语言 10 条删除，radiation 保留 |
| 营养配置键 | 2 删除 |
| 流体语言键 | 英文 28、中文 56 删除 |
| 流体贴图 | 14 组（28 个文件）删除 |
