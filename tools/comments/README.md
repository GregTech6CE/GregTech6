# comments

检查代码注释的可读性与改动范围。对应根 [AGENTS.md](../../AGENTS.md) 的注释规则：本仓库引用不写行号，上游 GT6 行号相对固定提交 `3703e40308c8c030763fd6297dea8b210d2a77b1`。

## check_comment_refs.py

扫描 `core/src`、`minecraft/src` 与两个版本节点的源码，报告：

| 类型 | 含义 |
| --- | --- |
| `repo-line-ref` | 指向本仓库代码的行号，每次编辑都会漂移 |
| `bare-line-ref` | 只有 `:123`，无法确定文件 |
| `unknown-line-ref` | `Foo.java:123` 的文件既不在本仓库也不在上游树 |
| `history` | 描述批次/改动而不是当前代码的注释 |

```bash
python tools/comments/check_comment_refs.py [--upstream PATH] [--json] [--strict]
```

`--upstream` 指向固定提交的 GT6 检出目录，启用后按文件长度校验上游行号。发现项会以退出码 1 结束；生成源码的发现只列出、不判失败，因为对应生成器不一定在本仓库。

## verify_comments_only.py

把工作区改动的 Java 文件与 `HEAD` 比较，先将两侧注释替换为空白再比对；除注释外的任何差异（代码、字面量、Stonecutter 标记）都会被列出。

```bash
python tools/comments/verify_comments_only.py [--base REV] [paths...]
```

不传路径时读取 `git diff --name-only <base>` 中的 `.java` 文件，默认 `--base HEAD`。
