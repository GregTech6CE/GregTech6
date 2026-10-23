# 生存加工、物品标准化与标准中文续批

## 本轮完成

- 红色合金线：已确认之前看错，继续保持所提供 GT6 源码的衰减行为，本轮没有改信号系统。
- 输送机模块 11 个注册 ID 从 compact__electric_conveyor_* 改为 compact_electric_conveyor_*。模型与现有制作配方同步更新；MissingMappingsEvent 对旧存档 ID 重映射，GTTechnological.get 兼容旧代码查询。保留旧模型资源作兼容；没有注册第二套重复物品。第三方数据包若硬编码旧 ID，仍应更新自己的 JSON。
- 标准中文：sync_standard_chinese.py 读取GregTech.lang，通过原版 MultiItem 数字 ID、原版注册英文名的唯一对应和明确的多方块 ID 映射导入。当前 1,090 条名称有直接来源，其中纠正 1,052 条旧译名。不是用英文单词表重新猜译。
- 输送机的标准名称为“输送机模块”，筛台为“筛选台”等。已有移植版 XV 额外等级并不存在于原版十级注册循环，因此沿用标准家族名称只替换等级后缀，在 standard-chinese-derived.json 单独注明，没有伪装成原文件原句。
- import_gt_lang.py、generate_tech_lang.py、complete_registered_lang.py、transpile_gt6_multiitems.py 在生成结束后执行标准中文校准，防止重新生成时覆盖标准名。本轮没有重跑具有资源清理操作的整套移植生成器。
- 新增 184 条 GT 木材与下界木加工记录（含冷却液变体）：八种现有 GT 树种的原木锯切、梁锯切、四种水去皮和木板车削；绯红/诡异菌柄及菌核的普通/去皮锯切。产物为对应树种木板、梁及粉末，下界木不凭空添加木炭燃料路线。
- 原木默认锯切 6 木板与树皮粉，梁默认锯切 7 木板与木粉，木板车削 2 木棍。GT 家族沿用 GT6 默认木工规则；下界木是明确的 1.20 适配，不称为原版原有内容。特殊树种的独有产率仍需进一步对照，当前没有新增生态生成或完整树脂采集。
- 206 处工作台材料引用改为具体 Forge 材质/形态标签，覆盖 67 个不同映射。只处理输入，不把配方结果变成标签；小粉/小齿轮/多重锭等不能被错误折算为普通形态。
- MaterialUnification 在机器配方发布时选择明确的原版物品输出：铁金铜锭、铁金粒及能严格对应的宝石、红石等。替换前核对组成和形态，数量与概率槽位保留，NBT/损伤物品不转换。现有背包物品不强制替换，其他模组物品不按任意优先级抢占输出。
- 钨钢规范标签使用 tungsten_steel，并保留 tungstensteel 标签别名。

## 维护入口

- tools/sync_standard_chinese.py；docs/standard-chinese-origins.json、standard-chinese-changes.json、standard-chinese-derived.json。
- tools/standardize_recipe_ingredients.py；docs/standardized-ingredients.json。
- api/material/MaterialUnification.java、MaterialEquivalence.java。
- registry/LegacyItemMappings.java、GTTechnological.java。
- content/recipe/RegisteredWoodSurvivalRecipes.java。

## 验收建议

1. 中文界面检查输送机模块、筛选台、打磨石、研钵和几种容器；名称有争议时按 origins.json 查 GregTech.lang 原键，不再凭感觉改名。
2. 旧存档中的输送机模块应仍存在，F3+H 显示新 ID；JEI 配方输出新 ID，图标正常。
3. 取橡胶木/枫木等 GT 原木，走压力清洗→梁→切割机，核对树种与产量；试绯红/诡异木的普通和去皮版本。
4. 用原版铁/金/铜与合适的其他模组同材质部件测试制作输入。查看机器产物是否优先给原版物品；不要把不同材质或不同形态当作等价。
5. 开启 JEI 检查输送机等元件实际制作，再测试旧存档迁移。本轮自动测试不能代替真实旧档和整合包验收。

## 范围与剩余

标准中文导入没有宣称覆盖全部 8,000 多个键；材料模板、动态机器名和无法确定原版对应的内容仍需继续映射。没有删除不确定译名造成缺键，也没有将未对应项当作原版标准翻译。1,090 是本次导入器可追溯名称数，不是全部材料翻译数量。

新增配方是现有 GT 树种的生存加工衔接，不代表所有机器、高科技和生态上游已经闭环。竹子、树脂采集、完整化工与高级材料制造、所有动态工艺仍在后续范围。

本轮以源码/runClient 为验收入口；上一轮 build/libs 中的 JAR 不含本轮更改。

## 最终验证

- `../survival-standard-verified.log`：205 项 required GameTest 全部通过，BUILD SUCCESSFUL。
- `../survival-standard-python-final.log`：96 项 Python 检查通过。
- `docs/ingredient-tag-mismatches.json` 为 `{}`；实际 Forge 标签包含所有本轮替换的原材料。
- 回归保留材料质量守恒与线缆宽度守恒检查，仅将旧测试的“必须是某个 GT 物品类/固定 item JSON”假设改为支持已验证的原版物品和具体材质标签。
- 未执行 JAR 打包，未进行图形客户端和真实整合包验收。
