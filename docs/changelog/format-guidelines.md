# 更新日志格式规范

本文定义 GT6CE 更新日志的内容模板、主题分组、引用链接写法与日常维护流程（收录范围与分类标准见[内容准则](content-guidelines.md)）。

## 内容模板

中英文版在版本、分节、主题分组与条目数上保持对应；无条目的分节直接省略，已发布版本统一使用二级标题（不使用 `<small>`）：

```markdown
## [未发布](https://github.com/GregTech6CE/GregTech6/compare/v1.0.8...master)

### 主要内容

* 统一材料工艺数据并在 JEI/EMI 新增“矿物处理”总览页。([PR #403](https://github.com/GregTech6CE/GregTech6/pull/403))
* 升级 Addon API 至 1.9.0：允许附属扩展现有材料与工艺规则。([PR #403](https://github.com/GregTech6CE/GregTech6/pull/403))

## [v1.0.8](https://github.com/GregTech6CE/GregTech6/releases/tag/v1.0.8) (2026-11-18)

### 次要内容

* 创造模式与 EMI 中的电动工具默认配备满电与精金工具头。([4562bf8](https://github.com/GregTech6CE/GregTech6/commit/4562bf8af))

### 错误修复与平衡改进

**配方与材料**

* 恢复链锯与电钻的电动工具头配方。([#407](https://github.com/GregTech6CE/GregTech6/issues/407))

**工具、机器与物流**

* 修复漏斗精准输出模式。([#373](https://github.com/GregTech6CE/GregTech6/issues/373))
```

正文直接从版本标题开始（不写顶部总标题与导航索引）；英文版对应标题为 `## [Unreleased](...)`、`### Major Changes`、`### Minor Changes`、`### Bug Fixes and Balance Improvements`。

## 主题分组

不使用 `(scope)` 条目前缀，也不允许在列表项行首写 `* **` 加粗前缀。同一小节条目较多且主题分散时，使用独立成行的加粗 `**主题**` 分组隔断（条目较少时直接列出）。

参考主题子标题（按版本实际分布组合选用）：

| 中文 | English |
| --- | --- |
| `**配方与材料**` | `**Recipes and Materials**` |
| `**工具、机器与物流**` | `**Tools, Machines, and Logistics**` |
| `**工具、世界与地牢**` | `**Tools, World, and Dungeons**` |
| `**世界生成、工具与机器**` | `**World Generation, Tools, and Machines**` |
| `**熔炼、流体与界面**` | `**Smelting, Fluids, and UI**` |
| `**机器、流体与视觉**` | `**Machines, Fluids, and Visuals**` |
| `**世界生成、流体与客户端**` | `**World Generation, Fluids, and Client**` |

## 引用链接规则

不再列出未关闭的“关联 issue”。每条条目末尾仅按以下**互斥优先级**标注 1 类最直接的追踪链接：

1. **有合入 PR**：只列出 `[PR #xxx]` 链接，不再附带 issue 或 commit hash。
2. **无 PR，但有已关闭的 post-reset issue**：只列出已关闭的 `[#xxx]` issue 链接，不再附带 commit hash。
3. **无 PR 且无已关闭 issue**：仅在内容变更或大范围修复时标注提交——单个提交标 1 个 7 位短 hash（`[abc1234](.../commit/...)`），多个提交取最主要/收尾的 1 个 commit 或使用按时间先后的 GitHub compare 范围链接（`[first...last](.../compare/first...last)`）。

示例：

```markdown
* （通过 PR 合入）([PR #385](https://github.com/GregTech6CE/GregTech6/pull/385))
* （仅对应已关闭 issue）([#379](https://github.com/GregTech6CE/GregTech6/issues/379))
* （仅对应单个核心提交）([e6065ad](https://github.com/GregTech6CE/GregTech6/commit/e6065ad5a))
* （仅对应多提交范围）([d4c11cf...9bf352a](https://github.com/GregTech6CE/GregTech6/compare/d4c11cf18...9bf352aad))
```

## 维护与校验

- **日常更新**：向 `master` 合入面向用户的变更后，同步更新对应系列顶部的 `未发布`（`Unreleased`）小节；发布新 tag 时将其转为正式版本标题、补齐日期与 Release 链接，并推进对比基线。
- **外链边界**：仓库根 `README.md` / `README.zh-CN.md` 仅链接更新日志正文，不向外暴露规范文档。
- **文档检查**：增删文档或调整链接后，在仓库根目录运行 `python tools/validation/route.py check` 确认相对链接与文档路由全部通过。
