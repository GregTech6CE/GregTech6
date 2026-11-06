# registry-audit

审计物品/方块注册的来源、入口约定和命名空间写法，给注册结构收敛提供可重复的基线。它是按需运行的检查工具，不属于 [`docs/development/validation`](../../docs/development/validation/README.md) 的验收门禁，也不在 CI 中执行。

## audit_item_registration.py

扫描 `core/src/main/java`、`minecraft/src/main/java` 和两个版本节点的源码：

| 类型 | 含义 |
| --- | --- |
| DeferredRegister declarations | 按注册类型（ITEM、BLOCK、BLOCK_ENTITY_TYPE 等）和源码集统计声明 |
| Item registry declarations | 所有物品注册表声明，含 Stonecutter 两个分支和命名空间 |
| Entry-point conventions | `registerAll`、`initialize`、`register(bus)`、`bootstrap`、`submit` 的使用分布 |
| Hardcoded namespace | 注册相关包里直接写 `"gt6ce"` 的位置 |
| Duplicate literal ids | 同一平台、同一命名空间、同一注册类型下重复的字面量 ID |
| Unclassified registrations | 无法从接收者推断注册类型的 `.register("id")` 调用，留给人工判断 |
| Definition tables | 注册代码引用的 `*Catalog` / `*Definitions` / `*Rules` / `*Gen` 定义表 |

```bash
python tools/registry-audit/audit_item_registration.py
python tools/registry-audit/audit_item_registration.py --out work/registry-audit/2026-11-05
python tools/registry-audit/audit_item_registration.py --json
python tools/registry-audit/audit_item_registration.py --strict
```

脚本只依赖 Python 3 标准库，不修改仓库。`--out` 写出 `audit.json`（机器比对）和 `audit.md`（人工阅读）；基线快照放 `work/registry-audit/<日期>/`，不要提交。

### 阅读结果

- **Stonecutter 两个分支都会扫描。** 共享文件里 `//? if >=1.21.1 { ... //?} else { ... //?}` 的非活动分支也保留在源码中，脚本按行标注它属于 `1.21.1-neoforge` 还是 `1.20.1-forge`，因此能看到双平台差异；版本节点文件则固定为对应平台。
- **重复 ID 只统计字面量。** 用 `id()`、拼接或表驱动的注册不会进入重复检测；这类内容由定义表审计和运行时日志覆盖。
- **未分类列表不是错误。** BlockEntityType、KubeJS 注册事件、模型包等使用 `.register("...")` 的代码会出现在这里，人工确认是否与物品注册有关。
- **命名空间为 `(dynamic)` 表示不能静态求值。** 附属命名空间通常来自 `AddonMachines` 等运行期数据，脚本不会把它们和 `gt6ce` 的字面量注册混在一起比较。

`--strict` 在发现重复字面量 ID 或注册包内仍有硬编码 `"gt6ce"` 时以退出码 1 结束；默认只输出报告。
