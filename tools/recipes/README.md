# recipes：配方数据审计

`audit_recipes.py` 对随模组分发的数据包配方做静态审计。它只读
`core/src/main/resources/data/gt6ce/recipes`、`tags/items`、`assets/*/models/item`
和生成配方的 Java 表，不启动游戏。

## 运行

```bash
python tools/recipes/audit_recipes.py
python tools/recipes/audit_recipes.py --report work/recipe-audit/report.json
python tools/recipes/audit_recipes.py --strict   # 有 error 时返回 1
```

输出分两部分：stdout 的汇总与逐条摘要，以及 `--report` 指定的完整 JSON
（建议放在被忽略的 `work/` 下，作为一次性证据）。默认不因 error 返回非零，
适合人工排查；`--strict` 供本地门禁使用。

## 检查项

- **解析与类型**：JSON 是否可解析、`type` 是否属于已注册的配方类型。
- **形状**：`pattern` 行列是否一致、图案字符是否都在 `key` 中定义、`key` 条目是否合法、
  shapeless/smelting 的必填字段、结果（含 `"result": "id"` 简写）是否合法。
- **标签**：配方引用的 `gt6ce:` 标签是否有静态定义或运行时生成。运行时生成的家庭
  从 `MaterialTagPack`、`MaterialPrefixes`、`MaterialPrefix`、`BlockPrefixRegistry`
  的源码里提取（材料形态、`wire_NN`/`cable_NN`、`pipe_*`、`molds/forms/*`、
  `ammunition/*`、`technology/*`、`rechargeable_batteries/*` 等）。提取不到的第一段
  会作为 error 报出。
- **电路家族**：ingredient 位置出现 `circuit_basic|good|advanced|elite|master|ultimate`、
  `integrated_circuit_*`、`crystal_circuit_*` 时报 error——这些位置应使用累计标签
  `#gt6ce:circuits_tier_N_plus`、`#gt6ce:integrated_circuits` 或正确的处理器物品。
- **歧义配方**：同 `type`+`pattern`+`key` 却产出不同物品时报 error；`construction_color`
  区分颜色的变体不报错。
- **未知字段**：`_comment`、`_mirror_flags`、`_autocrafting_source` 之外的未知顶层字段
  报 warning（防拼写错误）。
- **目录清单**：`EquipmentCraftingCatalog`、`ClayMoldCatalog`、`PortableCraftingCatalog`、
  `AmmunitionCraftingCatalog`、`BookCraftingCatalog`、`BumbleCraftingCatalog`、
  `NuclearCraftingCatalog` 列出的文件必须存在（error）。
- **NeoForge 覆盖**：1.21.1 只读取 `recipe/`（单数），`recipes/`（复数）文件必须由
  `PortableCraftingRecipePack` 等重新发布，或由运行时生成器（
  `FormConversionCraftingRecipeInjector` 等）覆盖；两者都不是的文件报
  `neoforge-unlisted` warning。已知例外只有 `tools/repair.json`
  （由 `ManualToolRecipePack` 加载）。
- **物品模型**：结果物品在 `assets/*/models/item` 或 `blockstates` 中找不到模型时
  报 warning；材料物品可能有运行时模型，warning 只作提示。

## 维护约定

- 新增配方类型、字段或运行时标签家庭时，同步更新本脚本的常量，否则审计会误报。
- 生成器（从上游 GT6 源码导入数据、再生成提交表）按根目录规则留在被忽略的
  `work/local-tools/`，不要放进这里：它们依赖固定上游快照，放进仓库会持续腐烂。
  这里只放随本仓库数据演进的检查工具。
- 报告里 `neoforge-unlisted` 是真实缺陷：把文件加入相应 catalog，或确认它确实由
  生成器覆盖后更新 `NEOFORGE_GENERATED_PREFIXES`。
