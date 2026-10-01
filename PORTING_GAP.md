> 最新源码差距核对见 2026-09-12 大蓝图审计。本文件保留历史计划；注册数量不代表功能已完成。

> 2026-09-11 审计更新：当前状态、已发现行为缺陷和框架重构见 [PORTING_AUDIT.md](PORTING_AUDIT.md)。本文件的旧 Wave 完成标记不等于当前验收结果；旧「永久砍除」建议已被完整还原目标取代。

# GT6 源码对比缺口清单（src ↔ src）

> 对比 `gregtech6-master/src/main/java/gregtech`（562 个 Java 文件）与本仓库
> `src/main/java/com/gregtech/gregtech` 的功能覆盖。截至 Wave 25a。
> 粒度：GT6 包/类 → 移植状态。✅ 已移植（含等价重写）｜🔶 部分｜❌ 未移植。

## tileentity/（242 类，机器主体）

| GT6 包 | 类数 | 状态 | 说明 |
|---|---|---|---|
| tileentity/machines（basicmachines） | — | ✅ | 66 基础 + 13 大型（含并行）全部在 GTBasicMachines |
| tileentity/energy/converters | 52 中 ~12 | 🔶 | 电动机/发电机/蒸汽轮机/太阳能已做；**Flux(RF) 电机/发电机、燃液发动机 MotorLiquid、热力发电 hot_fluid、长距变压器**未做 |
| tileentity/energy/transformers | 〃 | 🔶 | 电力变压器升降压已做；**转动变压器 TransformerRotation** 未做 |
| tileentity/energy/storage | 〃 | 🔶 | 电池盒（装电池单元）/储能柜已做；**大型电池盒、激光晶体/ZPM 储能**未做 |
| tileentity/energy/generators | 〃 | 🔶 | 燃烧箱/蒸汽机已做；**核反应堆 reactor_core_1x1/2x2 + 燃料棒、闪电棒**未做 |
| tileentity/multiblocks | 35 | 🔶 | 蒸馏塔多方块+框架已做；**大锅炉/大型汽轮机/燃气轮机/大型发电机/热交换器/基岩钻机/坩埚多方块版/焦炉物理结构/激光器/物流核心/范德格拉夫**未做（框架可复用） |
| tileentity/batteries | 19 | ❌ | 可充电电池物品逻辑（我们的电池单元是静态容量，无充放电 NBT） |
| tileentity/sensors | 20 | ❌ | 显示型传感器/仪表覆盖板（温度计/液量计/能量表/物品计数器） |
| tileentity/tools | 30 | ❌ | 放置式工具方块（砧/磨刀器/打粉臼/搅拌碗/烟熏架等） |
| tileentity/inventories | 16 | ❌ | 抽屉/保险箱/储物柜/大宗存储/末影垃圾桶/物品分配器 |
| tileentity/tanks | 12 | 🔶 | 木桶/塑料罐/金属桶已做；**铁桶多方块罐、排水沟 drain** 未做 |
| tileentity/portals | 20 | ❌ | 传送门类（跨模组/末影），建议砍 |
| tileentity/extenders | 6 | ❌ | 延伸器（活塞式管道延长） |
| tileentity/panels | 6 | ❌ | 面板（太阳能板挂壁版等装饰功能板） |
| tileentity/placeables | 9 | ❌ | 可放置物（书/工具展示等） |
| tileentity/computer | 2 | ❌ | USB/HDD 数据系统 |
| tileentity/redstone | 1 | ❌ | 红石装置 |
| tileentity/autotools | 2 | ❌ | 自动工具台 |
| tileentity/plants | 4 | ❌ | 植物盆栽类 |
| tileentity/food | 1 | ❌ | 食物加工器 |
| tileentity/misc | 7 | ❌ | 杂项（垃圾桶等） |

## items/（105 类）

| 内容 | 状态 | 说明 |
|---|---|---|
| MultiItemTechnological | ✅ | GTTechnological ~300 个（电路/外壳/电池单元/激光器/挤压模具/集成电路） |
| RandomTools / Bottles / Food / Bumbles | ✅ | 901 个已注册 |
| **MultiItemCans（罐头）** | ❌ | 待单元格/食物体系（转译器加 cans 类别即可） |
| **MultiItemBooks（手册）** | ❌ | 文本内容量大 |
| tools/early + crafting | 🔶 | 手动工具已做（GTToolItems）；**electric/（电动工具）、guns/（弓弩枪械）、pocket/（口袋工具）、machine/** 未做 |
| behaviors/（物品行为） | 🔶 | 部分行为内联实现 |
| **单元格 CellItem（流体 1000mB 容器）** | ❌ | ④ 第一步，解锁化学+罐头 |

## blocks/（66 类）

| 内容 | 状态 |
|---|---|
| stone/（石头方块组） | ✅ |
| 矿石/小矿石 | ✅（单方块+NBT 方案） |
| BlockRailRoad / BlockPath / Bale / Sands / Grass | ✅（iconset） |
| **tree/（12 类：原木 A-D/防火/树叶/树苗）** | ❌ + 树木世界生成 |
| **plants/（FlowersA/B、Glowtus）** | 🔶 花已做 iconset，作物逻辑未做 |
| **BlockAsphalt / Concrete(+强化) / CFoam(+鲜) / GlassClear / GlassGlow / Diggable** | ❌ |
| fluids/（流体方块） | ✅ |

## worldgen/（46 类）

全部 ✅（区域石层/矿脉/小矿/矿床/基岩矿/表面沉积/下界末地专属/-64 下延）。
**例外**：❌ 树木世界生成（依赖 tree 方块）、❌ GT6 自定义虫洞/陨石类彩蛋（如有）。

## loaders/（37 类）

| Loader | 状态 |
|---|---|
| Ores / Rocks / Fluids / Materials / Worldgen / OreProcessing / MultiTileEntities | ✅ |
| Recipes_Alloys / Chem / Decomp / Extruder / Furnace / OreDict / Fuels | ✅（化学剩 ~276 条：染料数组循环为主，见 PORTING_TODO） |
| **Recipes_Crops / Food / Potions / Vanilla / Woods / Foreign(跨模组) / Replace / Temporary / Hints** | ❌ |
| **Books / Loot / BlockResistance / Rails / Woods / ItemIterator** | ❌ |

## compat/（59 类）与 asm/（18 类）

❌ 全部未移植——1.7.10 跨模组兼容（IC2/TE/林业/神秘时代）与字节码补丁，**1.20.1 不适用，建议永久砍除**（compat 可按需以 Forge capability 重写个别项，如 RF 桥接）。

## entities/（4 类）

❌ 闪电/炸药实体等，待闪电棒/炸药时一并处理。

## 渲染/GUI 专项缺口

- 覆盖板过滤器配置 GUI、显示型覆盖板渲染
- 机器 overlay_active_lf/ls/rf/rs 动画帧（电机/轮机的旋转动画，目前用静态 overlay）
- 电池盒充电条（bar.png）渲染
- 长距管线/变压器端点

## 建议优先级（结合用户已点名）

1. ④ 单元格 CellItem + 化学转译器循环展开（路径已记录在记忆/PORTING_TODO）
2. 轮机转子耐久、Flux(RF) 电机/发电机、核反应堆
3. 多方块余项（大锅炉/大型轮机——框架已就绪）
4. sensors / inventories / tools 三个 tileentity 大包（玩法密度高）
5. 树木系统（含世界生成）
6. 食物/作物/药水/书/战利品等外围 Loader
