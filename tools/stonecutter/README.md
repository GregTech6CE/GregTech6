# stonecutter

对共享输入中的 Stonecutter `//?` 条件块做闭环解析、二维归因分类、波次切片与硬违规检查。脚本只读，不修改源码，不调用 Gradle。

## 依赖

Python 3.10 或更新版本，仅使用标准库（UTF-8）。

## 输入

- 共享源码：`minecraft/src/main/java`、`api/minecraft/src`。
- 节点源码（仅用于 Wave 1a `BlockEntity` 节点子类检查）：`minecraft/versions/1.20.1-forge/src/main/java`、`minecraft/versions/1.21.1-neoforge/src/main/java`。

## 命令

```bash
# 全量闭环校验、二维交叉矩阵、Wave 汇总与硬违规列表
python -I tools/stonecutter/audit_stonecutter_matrix.py .

# 按波次导出文件清单与块数（Wave 1a/1b/1c、Wave 2a/2b/2c、Wave 3、Wave 4、Floor）
python -I tools/stonecutter/audit_stonecutter_matrix.py . --wave "Wave 1a"

# 仅列出硬违规项
python -I tools/stonecutter/audit_stonecutter_matrix.py . --violations

# 导出完整结构化 JSON（含 Wave 1a BlockEntity 父类分布与节点子类检查）
python -I tools/stonecutter/audit_stonecutter_matrix.py . --json ../work/machine-unification/matrix.json

# 门禁检查：存在硬违规或未闭合标记时返回非 0 退出码
python -I tools/stonecutter/audit_stonecutter_matrix.py . --check-hard-violations
```

## 输出

- **闭环统计**：扫描文件数、含 `//?` 文件数、总块数（双侧 / 单侧 / 双空）、嵌套块数、未闭合栈与孤立标记（必须为 0）。
- **二维矩阵**：按 `origin`（`MC_VERSION` | `LOADER` | `SELF_DEFECT` | `COMPAT`）× `disposition`（`BASE_CLASS` | `REMOVE` | `FACADE` | `FOLLOW_BODY` | `FLOOR_KEEP`）统计，并分离 `Actionable Backlog` 与方案 A 永久地板（`FLOOR_KEEP`）。
- **Wave 汇总与切片**：按 `Wave 1a`–`Wave 4` 与 `Floor` 输出文件数、块数及排序文件清单。
- **硬违规**：检测共享层引用 `gregtech.platform.{forge,neoforge}`、空块/纯注释残留、两侧同义分支、拆分重复与未闭合标记。
