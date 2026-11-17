# material：材料形态清单审计

`audit_roster.py` 只读 GameTest 写出的运行时快照，不启动游戏、不调用 Gradle、不修改仓库。

## 输入

传入包含以下文件的单个运行目录：

- `reload-items.tsv`
- `reload-forms.tsv`
- `reload-block-forms.tsv`
- `reload-recipes.tsv`
- `reload-tags.tsv`

这些文件来自已删除的历史材料基线 GameTest。当前测试集不再生成新快照；本工具仅用于复核仍保存在 `work/` 中的历史证据，不能把旧目录当作当前实现验证。

## 运行

```bash
python3 tools/material/audit_roster.py \
  work/material-system-refactor/runtime/1.20.1/gametest-forge

# 完整 JSON 证据放到被忽略的 work/
python3 tools/material/audit_roster.py \
  work/material-system-refactor/runtime/1.20.1/gametest-forge \
  --json work/material-system-refactor/07e/forge.json
```

输出按 prefix 汇总实际物品数、材料方块数、静态配方输出／输入对象数，以及各 `RecipeMap` 的命中数。物品 tag 输入会展开为其实际成员；同一个物品如果同时是材料物品和方块形态，会分别计入两个形态族。

## 边界

- 配方计数来自 `RecipeMap.mRecipeList`，不展开所有动态族。
- 手工配方、世界生成、铸币、化石和展示用途没有统一静态表，必须按源码或专门运行时测试补证。
- 本脚本证明注册清单和静态配方入口的一致性，不替代客户端展示或机器执行验收。

## 维护

快照列格式变化时同步修改 `recipe_items`、物品 prefix 读取和方块形态读取。若重新建立常驻快照入口，长期字段口径应写入对应的架构或验证文档。
