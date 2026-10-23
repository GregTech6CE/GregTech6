# P3 配方、制作入口与手册批次

更新：2026-10-01。遵循每几批集中小测、NeoForge 优先的要求。整合仍在进行。

## 采用来源与取舍

saltnya 原始九组 GTChemGen/GTOtherGen/GTPotionGen/GTFoodGen/GTExtruderGen/GTVanillaGen/GTTemporaryGen/GTOresGen/GTCombGen 配方行迁至 core，只有解析入口改为 GeneratedRecipeSink。同一份表分别由 Forge 与 Neo 原生 ItemStack/FluidStack/注册表解析；缺失 token 和碰撞仍按原 resolver 记录，生成表最后加载，保留先注册配方的优先级。ThreadLocal sink 在 finally 恢复，避免本轮绑定泄漏。未将其他项目的材料和注册底层重复导入。

GTMaterialFields/GTAlloyTable/GTEnchantmentTable/GTLootGen、完整 GTMultiItemsGen 元数据与 IGTLoader 原字节迁 core。原手册正文、元素和合金手册生成逻辑进入 GTBookContent，Forge 保留原 NBT 写入，Neo 使用 WrittenBookContent components；标题限制保留。原 96 GU/t 热挤压时间提取为 ExtrusionWorkRules，两平台使用同一公式。

Neo 接入原 Parts/Alloys/Decomp/Chem 与 VanillaProcessing/DyeProcessing/MaterialFormConversion/MortarGrinding/Sharpening/GTMainRecipes 共十个加载入口，再运行共享生成表。手写公式目前仍有平台重复，后续继续抽取纯逻辑；此处不能称全部配方逻辑已共享。

Neo 基础机器工作台配方采用同一 BasicMachineCraftingRecipes 表、原图案和 ingredient 翻译规则。PackLocationInfo/ResourcesSupplier 与 recipe/tag 单数目录适配 1.21.1；tool_shaped 用原生 codec 包装 ShapedRecipe，保留不允许镜像、空流体容器限制与已迁入工具的制作磨损。37 类原 GTToolType 制作损耗值进入 ToolCraftingRules，Forge 委托此表。完整手动/电动工具仍待迁入，不能因制作表存在宣称机器从生存资源可达。

注册表生成材料 tag pack：按真实已注册材料、前缀、块组成、密集矿与原生矿生成；保留 Forge tag 名供原制作表使用，并添加 c namespace 别名。八份原电路 tier/选择器 tag 字节复制，只改到单数 item 目录。不会把缺失电池注册伪装成已存在的 tag 成员。

对照 masson CraftingToolWear：它按原材料耐久计数将 GT6 工作台损耗除以 100，而本项目采用项目1耐久 ×100 基准；因此没有直接照搬其 8/4/1 数值和电动工具类型。本批保留项目1原损耗目录，电动工具 charge 行为仍待独立整合。此取舍是基于代码尺度证据，尚非三家工具全面运行比较。

来源与原始 SHA、适配清单在 core/provenance/generated-recipe-source-integration.json 和 machine-crafting-source-integration.json。原许可、作者和只读来源不变。

## 本次小检查

- 第一轮 :neoforge:compileJava：24 秒失败，两个错误为手册组件泛型不匹配、Parts 引用未迁入 CapsuleCellRecipes 的时间函数。
- 第二轮：修正泛型并提取共享挤压时间后通过，13 秒，5 条 deprecated API 警告。日志 work/p3-recipes-neoforge-checkpoint-20261001{,-r2}.log。
- Neo 首次隔离短专服检查：6 分 44 秒，绝大部分在首次资源复制/哈希；游戏 commonSetup 明确失败于 Missing dye ingredient gregtech:plant_remains，虽然 Gradle 返回 BUILD SUCCESSFUL。本账本按游戏失败处理。日志 work/p3-recipes-neoforge-startup-20261001.log。
- 补齐四种原始加工残渣，完整 GTMultiItemsGen 元数据目录共享 core。源 GTFoodItemsGen 四行均 hasFoodStat=false，保留不可食用 TechItem 默认行为；八份模型/纹理保留 SHA。其他 MultiItem 功能没有假注册。第一次修复脚本校验误把元数据行存在视为 FoodStat 存在，因此 r2 中途取消，没有验收结果；修正检查字段后 r3 真正执行修复版本。r3 成功（4 分 22 秒，包括资源处理与增量编译）：普通 DedicatedServer 完成配方/标签加载，运行 200 tick，正常保存退出。SERVER_SMOKE_SUCCESS 明确 normalStopObserved=true、restartWorldReadVerified=false；level.dat 和 region 已保存。回执 verification/p3-neoforge-recipe-startup.json；日志 work/p3-recipes-neoforge-startup-20261001-r3.log。未运行完整客户端、玩法或存档重载验收。Forge 本批未重新编译。
- 实际加载证据：挤压矩阵 57336 行、合金 76 行（缺流体跳过 21）、分解 405 行（跳过 65）、化学 19 行、研磨 1831 行、磨锐 8986 行、主类 7 行（跳过 16）、九组生成表新增 1688 行（跳过 420）。Minecraft 原生 recipe manager 加载 1527 条，机器 RecipeMap 是另一个目录；不能将两者混算为已验证玩法数量。制作 ingredient 原有替代规则产生 19 条提示，继续留作缺口。
- 运行有 legacy forge 标签提示（同时提供 c alias）和隔离 flat 配置 No key layers 错误；后者回退后成功启动，不证明正式世界生成配置正确，不隐去日志。

## 缺口

完整工具行为/供电和工具制作、finite fluid ingredient 扣液、电池物品与 rechargeable tag、黑沙和木材物种原生工厂/标签、多方块零件/制作和 USB 动态数据链待移植。RecipeMap 未解析条目的跳过统计是缺口证据，不能视作对应功能完成。客户端外观、实际机器加工、保存和重载、旧存档兼容及发布 jar 仍须后续验证。

## 2026-10-02 设备制作后续

234 条原 JSON 已共享，NeoForge 真实配方管理器加载、原料候选检查和选定锅炉/燃烧箱/变压器/坩埚工具制作查询通过。详细来源、选择、范围与构建产物见 verification/equipment-crafting-parity-20261002.md；先前本文件的运行缺口列表为当时记录，当前修复依据后续阶段证据，不把历史声明当当前验收。普通活塞引擎制作入口仍缺少定义证据。

## 2026-10-02 部件与物流后续

185 条部件/物流/覆盖物/木管/多方块 JSON 原字节共享，Neo 419 设备原料候选与选定入口制作查询通过；详见 verification/routing-crafting-parity-20261002.md。物流设备运行、所有逐件制作和普通活塞引擎生存入口仍待验。

## 2026-10-02 线材与模具后续

1,394 条线材/模具 JSON 共享；全部 1,316 线材制作查询导体守恒及选定空白→杆→线模具查询/工具磨损通过。详见 verification/manufacturing-crafting-parity-20261002.md。其实际通电、玩家制作与完整生存仍待验。

## 2026-10-02 剩余静态配方后续

共享剩余 333 原配方及七原料标签；当前 2,146 静态设备/制造配方通过严格非空格原料候选检查与选定实际制作查询。此前 Ingredient.isEmpty 可能跳过空标签，原 resolvedIngredients 回执有该证据限制。细节和首轮大麦反馈见 verification/survival-crafting-parity-20261002.md。
