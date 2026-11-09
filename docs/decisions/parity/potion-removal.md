# 药水系统移除

本端移除 GT6 的整套药水系统——药水流体、药水机器配方表、药水罐装与饮用条目及其下游联动，药水身份改由原版药水物品承载。流体与物品侧的清单已分别并入[流体移除](fluid-removal.md)与[物品移除](item-removal.md)。

| 内容 | 差异 | 原因 |
| --- | --- | --- |
| 药水流体 | 停注全部 94 条带药水标志的流体，注册名见[附录A](#附录a-停注的药水流体)；药水流体标志常量一并删除。 | 药水身份完全交给原版药水物品，流体侧不再需要平行体系。该标志运行期没有读取者，删除只影响声明。 |
| 药水机器配方表 | 停注全部 490 行（蒸馏机 391、发酵机 69、混合机 30），路线构成见[附录B](#附录b-药水机器配方行的构成)，该表不再注册。 | 每行至少有一侧是药水流体，流体停注后不成立；留下来的酿造由发酵机走原版表。 |
| 机器批量酿造 | 发酵机（单方块与多方块共用同一配方图）改为执行原版酿造台的有效集合：药水物品 + 素材物品 → 药水物品；新增独立药水输入槽，机器变为物品、药水、流体三类槽位，药水槽在 GUI 中可见且只接受药水物品；药水输出复用现有物品输出槽。 | 原版酿造需要两个物品；药水输入槽与既有食物、化学发酵行共用的普通物品槽分开，不改变那些行的匹配与展示。 |
| 酿造速度 | 按现行流体路径等效：16 EU/t 与原有耗时不变，把毫升数换算成瓶数（750 mB 一操作 3 瓶、250 mB 一操作 1 瓶），两条路径都是 0.8 s/瓶、256 EU/瓶；药水配方不参与并行，单方块与大型发酵机都是每操作 1 瓶；混合机的龙息滞留行不换算，容器形态交给原版。 | 现状机器比原版酿造台快约 8 倍且不消耗烈焰粉，直接删除会让已在用这套系统的存档和自动化回退；上游 GT6 没有酿造配方图（酿造注册入口是返回失败的存根），机器批量酿造本就是移植端新增的便利，因此保留等价路径而不是复刻上游。药水物品原版最大堆叠 1，先按每操作 1 瓶保留每瓶成本，后续再考虑堆叠扩展。 |
| 配方来源 | 不手抄原版配方表，在运行期读取原版表的容器与 `Mix` 列表（1.20.1 的 `ALLOWED_CONTAINERS`/`POTION_MIXES`/`CONTAINER_MIXES`，1.21.1 的 `containers`/`potionMixes`/`containerMixes`），展开 `Ingredient` 后用 `getOutput`/`mix` 取输出；`BrewingRecipe` 自定义配方单独展开，无法枚举的 `IBrewingRecipe` 记录日志，匿名实现通过 `isValidIngredient`/`isIngredient` 回退覆盖。按注册表顺序枚举，不引入持久化配方 ID；本端不另外提供药水配方注册接口，addon 也经原版表注册后由探测继承。 | 两版原版表都在代码侧构建，没有公开的逐条 `Mix` 枚举 API（1.21.1 的 `potionMixes`/`containerMixes` 私有，NeoForge `getRecipes()` 只返回通用接口配方；1.20.1 Forge `getRecipes()` 只返回含 `VanillaBrewingRecipe` 桥接的 `IBrewingRecipe` 列表），因此通过访问器读取；探测法自动跟随 MC 版本与其他模组新增，手抄表会长期漂移。顺序确定只为让配方查看器条目稳定，机器匹配不依赖 ID。 |
| 额外原料 | 43 条 GT 素材路线中，目标为原版药水的 40 条注册进酿造台（萤石粉 1 条原版已有等价路线，实际新注册 39 条），3 条产饮料的随配方表取消；机器路径由探测自动继承，路线清单见[附录C](#附录c-额外原料路线)。 | 这是保留 GT 特色原料的唯一方式；只注册到机器会形成"机器能酿、酿造台不能酿"的双轨。酿造台接口接受标签，GT 粉料已有 `forge:dusts/*`、`forge:tiny_dusts/*`（1.21.1 另镜像 `c:`）。 |
| 与原版不一致的转化路线 | 取消浓药作基底、红石把 II 级降回 I 级、无素材发酵与 50→25/10 mB 体积损耗；腐化集合不再静态维护，由探测结果决定，取消清单见[附录D](#附录d-取消的转化路线)。 | 这些都是旧版 MC 与流体侧产物，物品化后无处安放。以原版为准，接受与 GT6 的 parity 分歧。 |
| 上游死配方行与死键 | 停注 18 条本就注册不出来的药水配方行，删除中文语言文件里的防火木料旧键，并把催化剂标签写法补进零数量识别；明细见[附录E](#附录e-上游死配方行与死键)。 | 素材在移植端不存在或指向未注册物品，删除不改变任何已注册内容；催化剂识别缺失只是被物品自带标记掩盖。 |
| 罐装与倒罐 | 只移除药水与原版药水物品互转的 16 种映射及其灌装配方分支；250 mB/瓶计量、瓶子能力、普通水瓶的识别及其灌装/倒罐配方保留，喷溅/滞留药水物品仍按瓶类物品识别但不再产出流体。 | 瓶子自动化与其余 138 个瓶装条目共用同一套能力（瓶装条目共 139 个，毒瓶占 1 个），整类删除等于删掉通用基础设施。 |
| 药水类饮用条目 | 饮料统计表中解析到药水流体的 93 行随流体停注，其余 203 行保留。 | 这些行的食物值与六种统计全为 0，只挂药水效果；物品化后效果由原版药水自带。 |
| 防火板条箱 | `crate_fireproof` 方块与它用药水流体作沐浴介质的配方一并取消，不再寻找替代介质。 | 它与普通板条箱的唯一差别就是熔岩可点燃；本端从未注册 GT6 的防火木料家族，防火板条箱是唯一仍在用药水沐浴的木制品，改随 lootbox 移植。 |
| 地牢夜视杯 | 图书馆地牢的夜视杯取消，不再放置杯子或药水物品。 | 杯子按流体字段名填充，流体停注后填充路径会静默失效；改为物品需要新的载体与展示，收益不足。 |
| 毒瓶与毒性木桶 | `bottle_of_poison` 与 9 个 `wooden_bucket_<band>_poison` 停注；三明治表里的毒瓶条目一并移除，因为该表按物品 ID 记录，无法区分原版药水的类型。 | 两者都绑定 `potion.poison.strong`；不删条目会留下解析为空头力的孤儿物品。 |
| 蜂房伤害药水 | 蜂房的 `potion.damage` 输出行留作死代码，不单独处理。 | 养蜂整体在移除计划内。 |
| 密封储罐 | 保留软锤密封与密封语义，密封后拒绝灌入与抽出；删除密封后的发酵——发酵 tick、配方查询、进度、相关 NBT、进度传感器与界面进度提示一并移除，不给密封储罐保留药水入口。 | 密封发酵是上游 GT6 的行为，但药水改为物品后桶内不可能再有药水；剩下的非药水无物品路线只有 17 条，保留半套只对食物与生物质生效的隐藏机制，收益低于维护与展示成本。这 17 条仍由发酵机执行，不受影响。 |
| 瓶筐经验瓶 | 经验瓶在瓶筐中的液面暂用水 + 手动颜色 #06FF44（绿色）渲染（中间临时替代方案）；原版经验瓶的液面颜色为紫色。 | 上游 GT6 的瓶筐用跳跃药水作为经验瓶的显示流体；跳跃药水停注后，水能保留“有液体”的显示，颜色取原贴图平均色，避免新注册流体或贴图。原版经验瓶为紫色，本端暂改为绿色，属于临时视觉方案。 |
| Potion_Tainted | 一并停注，不提供替代品。 | 上游用它承接外模组药水，移植端从未移植该路径。 |
| 保留的相似内容 | `Dragon_Breath` 流体保留；16 种 `potion.*` 命名的饮料流体及其贴图保留，id 清单见[附录F](#附录f-随移除清理的资源)。 | 只是注册名相似，这些流体不带药水标志、是带食物标志的饮料；龙息物品仍被原版喷溅药水和蜂房使用。`potion.dressing`、`potion.sake`、`potion.hotsauce`、`potion.diabolosauce`、`potion.diablosauce`、`potion.diablosauce.strong`、`potion.leninade`、`potion.dragonblood` 已由[营养系统移除](nutrition-removal.md)删除。 |
| 资源清理 | 两种语言各 188 条药水流体键、94 组流体贴图与动画描述，中文另有 187 条指向同一批流体的 GT6 时代旧键，加上毒瓶与 9 个毒性木桶的模型、贴图、堆组和相关语言键，明细见[附录F](#附录f-随移除清理的资源)。 | 沿用光谱露那次的清理范围，避免留下无主键与无主模型。 |
| 存档兼容 | 已发布存档（v1.0.0–v1.0.4 全部含药水内容）里储罐、板条箱与杯子中的药水流体载入后静默清空，不提供流体迁移；机器物品栏布局不变，药水槽单独持久化；密封储罐的发酵进度 NBT 不再读取，密封状态保留。 | 本端没有流体迁移机制，且药水身份改由物品承载。 |

## 附录A 停注的药水流体

| 类型 | 数量 | 详细信息 |
| --- | --- | --- |
| 基底 | 4 | `potion.awkward`、`potion.thick`、`potion.mundane`、`potion.tainted` |
| 即时效果 | 18 | `damage`、`health`、`jump` 三个家族，各取 6 个形态：`potion.<家族>`、`potion.<家族>.strong`、`potion.<家族>.splash`、`potion.<家族>.strong.splash`、`potion.<家族>.lingering`、`potion.<家族>.strong.lingering` |
| 两档时长效果 | 36 | `speed`、`strength`、`regen`、`poison` 四个家族，各取 9 个形态：上一行的 6 个，再加 `potion.<家族>.long`、`potion.<家族>.long.splash`、`potion.<家族>.long.lingering` |
| 单档时长效果 | 36 | `fireresistance`、`nightvision`、`weakness`、`slowness`、`waterbreathing`、`invisibility` 六个家族，各取 6 个形态：`potion.<家族>`、`potion.<家族>.long`、`potion.<家族>.splash`、`potion.<家族>.long.splash`、`potion.<家族>.lingering`、`potion.<家族>.long.lingering` |

## 附录B 药水机器配方行的构成

490 行按素材互斥归类，求和即总数；末行的 9 条已计入，[附录E](#附录e-上游死配方行与死键) 另列的 9 条 Netherlicious 行属于另一张配方表、不在本表内。

| 类型 | 数量 | 详细信息 |
| --- | --- | --- |
| 红石粉 | 156 | `dust:Redstone`、`dustSmall:Redstone`、`dustTiny:Redstone` 各 52 行；取消部分见[附录D](#附录d-取消的转化路线) |
| 腐化 | 139 | `v:fermented_spider_eye` 70 行 + `tag:0` 无素材发酵 69 行；取消部分见[附录D](#附录d-取消的转化路线) |
| 火药粉 | 90 | `dust:Gunpowder`、`dustSmall:Gunpowder`、`dustTiny:Gunpowder` 各 30 行，把普通形态打成喷溅形态 |
| 混合机龙息滞留 | 30 | 输入 `f:Dragon_Breath:1` 与 3 份喷溅形态，输出 3 份同家族滞留形态，30 个滞留形态各一行 |
| 原版素材 | 23 | `v:nether_wart` 1、`v:magma_cream` 2、`v:glistering_melon_slice` 3、`v:ghast_tear` 3、`v:spider_eye` 3、`tech:food_potato_poisonous` 4、`v:red_mushroom` 4、`v:pufferfish` 2、`v:golden_carrot` 1 |
| 其他 GT 素材 | 43 | 砷 12、糖 12、烈焰 3、Breeze 3、Blizz 3、Blitz 3、Basalz 3、Craponite 3、萤石粉 1；逐条见[附录C](#附录c-额外原料路线) |
| 上游死行 | 9 | `i:plantGtWart:Glowstone` 3、`tech:etfu_rabbit_foot` 3、`tech:tf_mushgloom` 3；逐条见[附录E](#附录e-上游死配方行与死键) |

## 附录C 额外原料路线

| 类型 | 数量 | 详细信息 |
| --- | --- | --- |
| 砷 | 12 | `dust:As`、`dustSmall:As`、`dustTiny:As` 各 4 行：蒸馏水 → 伤害 I、凡俗 → 伤害 I、笨拙 → 中毒 I、浓药 → 伤害 II |
| 糖 | 12 | `dust:Sugar`、`dustSmall:Sugar`、`dustTiny:Sugar`、`gemChipped:Sugar` 各 3 行：蒸馏水 → 凡俗、笨拙 → 速度 I、浓药 → 速度 II |
| 烈焰 | 3 | `dustTiny:Blaze`：蒸馏水 → 凡俗、笨拙 → 力量 I、浓药 → 力量 II |
| 风元素与弹元素 | 6 | `dustTiny:Breeze`、`dustTiny:Blitz` 各 3 行：蒸馏水 → 凡俗、笨拙 → 水下呼吸 I、浓药 → 水下呼吸延长 |
| 冰元素 | 3 | `dustTiny:Blizz`：蒸馏水 → 浓药、笨拙 → 抗火 I、凡俗 → 抗火延长 |
| 土元素 | 3 | `dustTiny:Basalz`：蒸馏水 → 浓药、笨拙 → 跳跃 I、凡俗 → 跳跃 II |
| 萤石粉 | 1 | `dust:Glowstone`：蒸馏水 → 浓药 |
| Craponite | 3 | `dust:Craponite`、`dustSmall:Craponite`、`dustTiny:Craponite` 各 1 行：凡俗 → `purpledrink` |

## 附录D 取消的转化路线

按路线语义切分，与[附录B](#附录b-药水机器配方行的构成)的素材归类互相重叠（例如浓药基底的 20 行里含 1 行发酵机腐化），不可求和。

| 类型 | 数量 | 详细信息 |
| --- | --- | --- |
| 浓药作基底 | 20 | 输入侧为 `f:Potion_Thick` 的全部行：19 条蒸馏机行（含[附录E](#附录e-上游死配方行与死键)的 3 条死行）+ 1 条发酵机腐化行 |
| 红石降级 | 63 | `dust`/`dustSmall`/`dustTiny:Redstone` 各 21 行：伤害、治疗、跳跃、速度、力量、再生、中毒 7 个家族的 II 级打回 I 级，普通、喷溅、滞留三种容器各一行 |
| 红石延长 | 90 | `dust`/`dustSmall`/`dustTiny:Redstone` 各 30 行：速度、力量、再生、中毒、抗火、夜视、虚弱、缓慢、水下呼吸、隐形 10 个家族的 I 级打成长时版，普通、喷溅、滞留三种容器各一行 |
| 红石配蒸馏水打底 | 3 | `dust`/`dustSmall`/`dustTiny:Redstone` 各 1 行：蒸馏水 → 凡俗 |
| 无素材发酵 | 69 | 输入 `tag:0` 催化剂与 50 mB 药水，输出腐化产物：I 级的普通/喷溅/滞留形态与三种基底走 64 tick 得 25 mB（30 行），长时版与 II 级走 128 tick 得 10 mB（39 行），其中 9 行把 II 级直接腐成 I 级（跳跃、速度等的 II 级 → 缓慢 I 级） |
| 发酵蜘蛛眼腐化 | 70 | 速度→缓慢 9、力量→虚弱 9、再生→中毒 9、中毒→伤害 9、治疗→伤害 6、跳跃→缓慢 6、抗火→缓慢 6、夜视→隐形 6、水下呼吸→伤害 6，另加蒸馏水→凡俗、浓药→虚弱、笨拙→虚弱、凡俗→虚弱 各 1 |
| 从蒸馏水直接产效果 | 6 | `tech:food_potato_poisonous`、`v:red_mushroom` + 蒸馏水 → 中毒 I；`v:pufferfish` + 蒸馏水 → 中毒 II；`dust`/`dustSmall`/`dustTiny:As` + 蒸馏水 → 伤害 I |

## 附录E 上游死配方行与死键

配方行 18 条分属两张表：前三组 9 条在药水表内（已计入[附录B](#附录b-药水机器配方行的构成)），Netherlicious 9 条在另一张杂项配方表内；语言键一行独立计数。

| 类型 | 数量 | 详细信息 |
| --- | --- | --- |
| 萤石疣 | 3 | `i:plantGtWart:Glowstone`：蒸馏水 → 凡俗、笨拙 → 跳跃 I、浓药 → 跳跃 II |
| Et Futurum 兔脚 | 3 | `tech:etfu_rabbit_foot`：蒸馏水 → 凡俗、笨拙 → 跳跃 I、浓药 → 跳跃 II |
| Twilight Forest 蘑菇 | 3 | `tech:tf_mushgloom`：蒸馏水 → 凡俗、笨拙 → 跳跃 I、浓药 → 跳跃 II |
| Netherlicious 素材 | 9 | `tech:neli_wart_crimson`：蒸馏水 → 笨拙；`tech:neli_wart_warped`：蒸馏水 → 浓药、笨拙 → 夜视 I；`tech:neli_wart_soggy`：蒸馏水 → 凡俗、笨拙 → 抗火 I；`tech:neli_wither_rose`：浓药 → 伤害 II；`tech:neli_bone_rose`：凡俗 → 跳跃 I；`tech:neli_soul_rose`：笨拙 → 隐形 I；`tech:neli_foxfire_powder`：凡俗 → 力量 II |
| 防火木料语言键 | 279 | `zh_cn.json` 中所有以 `gt.block.` 开头且含 `.fireproof.` 的键，分布为 `gt.block.beam.*.fireproof.*` 90、`gt.block.log.*.fireproof.*` 56、`gt.block.planks.fireproof.*` 17、`gt.block.planks.fireproof.slab.*.*` 102、`gt.block.planks2.fireproof.*` 2、`gt.block.planks2.fireproof.slab.*.*` 12；`en_us.json` 无对应键，其唯一含 fireproof 的 `block.gt6ce.crate_fireproof` 有对应方块，属于「防火板条箱」 |

## 附录F 随移除清理的资源

| 类型 | 数量 | 详细信息 |
| --- | --- | --- |
| 流体语言键 | 每语言 94 | `fluid.gt6ce.<id>`，`<id>` 取[附录A](#附录a-停注的药水流体)的 94 个注册名 |
| 流体类型语言键 | 每语言 94 | `fluid_type.gt6ce.<id 中非字母数字换成下划线>`，同样按附录A 的 94 个注册名派生 |
| 旧式显示回退键 | 中文 93 | `fluid.potion.<id 去掉前缀>`，即 `fluid.potion.awkward` 一类的 GT6 时代键；显示链先取上一行的 `fluid_type` 键，这些回退键只在中文文件里存在，94 条药水流体中除 `potion.poison` 外各有 1 条 |
| 旧饮品说明键 | 中文 94 | `gt.drink.potion.<id 去掉前缀>`，无任何代码读取，94 条药水流体各有 1 条；同前缀另有 15 条属于饮料 |
| 流体贴图 | 94 组 | `assets/gt6ce/textures/block/fluids/<id>.png` 与 `<id>.png.mcmeta`，按附录A 的注册名匹配 |
| 毒瓶 | 1 物品，每语言 2 键 | `assets/gt6ce/models/item/bottle_of_poison.json`、`assets/gt6ce/textures/item/bottles/bottle_of_poison.png`、`item.gt6ce.bottle_of_poison` 与 `item.gt6ce.bottle_of_poison.tooltip`；`assets/gt6ce/stack_groups/material_forms/functional_chemical_bottles.json` 中的 `item:gt6ce:bottle_of_poison` 一个条目，同文件其余条目不属于本次清理 |
| 毒性木桶 | 9 物品，每语言 19 键 | `wooden_bucket_<band>_poison`，`<band>` 取 `copper`、`tin`、`zinc`、`lead`、`bismuth`、`brass`、`bronze`、`bismuth_bronze`、`gold`；9 个 `assets/gt6ce/models/item/` 文件与 9 个 `assets/gt6ce/textures/item/wooden_buckets/` 文件、18 条 `item.gt6ce.wooden_bucket_<band>_poison` 与 `.tooltip` 键、`emi.group.gt6ce.wooden_bucket_poison` 键；`assets/gt6ce/stack_groups/material_forms/functional_wooden_bucket_poison.json`，该文件 9 个条目全是毒性木桶 |
| 同前缀保留项 | 16 流体 | `potion.mineralwater`、`potion.ambrosia`、`potion.coffee`、`potion.darkcoffee`、`potion.cafeaulait`、`potion.darkcafeaulait`、`potion.laitaucafe`、`potion.chocolatemilk`、`potion.darkchocolatemilk`、`potion.lemonade`、`potion.cavejohnsonsgrenadejuice`、`potion.alcopops`、`potion.goldenapplejuice`、`potion.goldencider`、`potion.idunsapplejuice`、`potion.notchesbrew`；它们对应 `fluid.gt6ce.potion.*` 剩下的 15 条（`potion.chocolatemilk` 无该键）、`fluid_type.gt6ce.potion_*` 剩下的 16 条与贴图目录剩下的 15 组（`potion.chocolatemilk` 无贴图），另有 `potion.png` 与 `potion.saltywater.png` 两组本就无主的贴图。其余 8 条 `potion.*` 饮料已由[营养系统移除](nutrition-removal.md)删除 |
