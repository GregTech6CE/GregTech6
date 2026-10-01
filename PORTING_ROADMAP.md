> 最新源码差距核对见 2026-09-12 大蓝图审计。本文件保留历史计划；注册数量不代表功能已完成。

> 2026-09-11 审计更新：当前状态、已发现行为缺陷和框架重构见 [PORTING_AUDIT.md](PORTING_AUDIT.md)。本文件的旧 Wave 完成标记不等于当前验收结果；旧「永久砍除」建议已被完整还原目标取代。

# GregTech6 → 1.20.1 移植路线图

原版源码：`F:/Dev/GregTech6/gregtech6-master/gregtech6-master/src/main`（1228 个 Java 文件）
移植项目：本仓库（`com.gregtech.gregtech`，Forge 47.4.20，官方映射）

状态图例：✅ 完成 ｜ 🔶 部分完成 ｜ ❌ 未开始

## 基础设施
| 子系统 | 原版位置 | 状态 | 备注 |
|---|---|---|---|
| 材料系统（~3000 材料） | gregapi/oredict, gregapi/data/MT | ✅ | 经 tools/transpile_gt6_materials.py 转译为 data/generated/GT6Materials.java |
| 前缀系统（~300 前缀） | gregapi/data/OP | ✅ | data/MaterialPrefix + OP |
| 材料物品/方块动态注册 | Loader_Items/Blocks | ✅ | loaders/a |
| 流体（452 种） | gregapi/data/FL | ✅ | FL.java + Loader_Fluids |
| 配方表框架（85 表） | gregapi/recipes | 🔶 | RM.java 已定义；findRecipe 子类行为未移植 |
| 矿物处理数据（副产物/破碎熔炼目标/倍率） | OreDictMaterial.ores()/setSmelting()/setCrushing() | ✅ | GTMaterial 扩展 + 转译器自动生成 |
| 矿石方块注册（250 ore + 125 ore_small = 375 方块） | gregapi/block | ✅ | OreBlock 类 + 石材质感模型 |
| 本地化 en_us/zh_cn | gregapi/lang | ✅ | ~300KB/每语言 |

## 内容子系统
| 子系统 | 原版位置 | 规模 | 状态 |
|---|---|---|---|
| 矿物处理链配方 | loaders/b/Loader_OreProcessing | 557 行 + 工具层 | ❌ |
| 通用材料加工配方（板/杆/齿轮/线材等） | loaders/c/Loader_Recipes_* (20 个) | ~20000 行 | ❌ |
| 合金/化学/分解配方 | Loader_Recipes_{Alloys,Chem,Decomp} | 大 | ❌ |
| 挤压机/食物/木头/原版配方 | Loader_Recipes_{Extruder,Food,Woods,Vanilla} | 大 | ❌ |
| 燃料表（8 类） | gregapi/data/FM + Loader_Fuels | 中 | 🔶 FM.java 占位 |
| 基础机器（~80 种单方块） | gregtech/tileentity/machines | 大 | 🔶 框架+部分机器 |
| 能源系统（EU/RU/KU/HU/蒸汽） | gregapi/tileentity/energy | 大 | 🔶 EnergyNet/引擎已有 |
| 发电机/转换器（21 种） | tileentity/energy | 大 | 🔶 引擎 4 类已有 |
| 电池/储能（19 种） | tileentity/batteries | 中 | ❌ |
| 核反应堆 | tileentity/reactors | 中 | ❌ |
| 多方块（35 种） | tileentity/multiblocks | 很大 | ❌ |
| 管道/线缆/物流 | tileentity/connectors | 大 | 🔶 物品/流体管+导线已有 |
| 储罐/桶/抽屉（28 种） | tileentity/tanks, inventories | 中 | 🔶 Tank 已有 |
| 覆盖板系统（65 类） | gregapi/cover | 大 | ❌ |
| 传感器/检测器（20 种） | tileentity/sensors | 中 | ❌ |
| 工具（71 类，手动+电动） | gregtech/items/tools | 很大 | 🔶 工具头物品已有，逻辑未移植 |
| 多功能物品（食物/科技/杂项） | gregtech/items/MultiItem* | 很大 | ❌ |
| 机器 GUI | gregapi/gui | 中 | ❌ |
| 世界生成：矿脉/石层 | gregtech/worldgen, gregapi/worldgen | 大 | ❌ |
| 世界生成：树木（8 种）/植物 | worldgen trees + blocks/tree* | 大 | ❌ |
| 石头/沥青/混凝土等装饰方块 | gregtech/blocks | 中 | 🔶 石头变体已有 |
| 铁轨/栏杆/尖刺 | blocks + Loader_Rails | 中 | ❌ |
| 附魔（5 种） | gregapi/enchants | 小 | ❌ |
| 实体（材料箭等） | gregtech/entities | 小 | ❌ |
| 传送门（20 种） | tileentity/portals | 中 | ❌（多为跨模组，低优先） |
| 计算机/红石 | tileentity/computers 等 | 小 | ❌ |
| 战利品/地牢 | Loader_Loot | 小 | ❌ |
| 跨模组兼容（~59 类） | gregtech/compat | 很大 | ❌（1.20.1 生态不同，按需重做） |
| JEI 集成 | NEI_* | 中 | ❌（改用 JEI 插件） |

## 移植顺序（当前计划）
1. ✅ 基线编译 + git 版本控制
2. 矿物处理数据（GTMaterial 扩展 + 转译器扩展 + 重新生成）
3. 配方工具层（RM.addRecipe1/2、pulverizing、add_smelting、OM 等价物）
4. Loader_OreProcessing → 矿物处理链
5. 通用材料加工配方（板材/杆/螺栓/齿轮/线材/挤压）
6. 世界生成（矿脉 + 石层）
7. 机器 GUI + JEI 配方显示
8. 工具逻辑（耐久/挖掘/合成）
9. 合金/化学配方、燃料表
10. 多方块框架 + 高频多方块（坩埚类已有，补蒸馏塔/大锅炉/电解机等）
11. 覆盖板系统
12. 其余（传感器、储能、反应堆、树木、附魔、JEI、杂项）

> 每完成一项请更新本表并提交 git。
