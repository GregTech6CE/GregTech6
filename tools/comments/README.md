# comments

本目录提供 Java 注释的静态合规检查脚本，按[Java 注释规范](../../docs/development/comments.md)检查行号引用、历史叙述与上游符号有效性，并支持验证一次提交是否仅修改注释。脚本只读运行，不修改文件，也不启动 Gradle。

## 依赖

Python 3.10 或更新版本，仅使用标准库。

## check_comment_refs.py

报告三类问题：

| 类型 | 含义 |
| --- | --- |
| `line-ref` | 注释里的行号：`Foo.java:123`、`Loader_X:123`、`line 123`、`at :123`、`tick():123` |
| `history` | 描述批次或改动，而不是当前代码 |
| `unknown-symbol` | `{@code Class#member}` / `{@code Class.member(...)}` 在 `--upstream` 里找不到（仅警告） |

纯数据表允许逐行上游行号：`generated/` 目录、带 GENERATED 文件头的文件，或在文件头写 `// source-rows` 的手写表。它们的问题会以 `(data table)` 列出，但不判失败。

### 命令

```bash
# 全仓检查（会报告历史遗留问题）
python tools/comments/check_comment_refs.py

# 只检查某个 diff 范围的新增行（CI 使用）
python tools/comments/check_comment_refs.py --diff origin/master...HEAD

# 同时校验上游符号是否存在
python tools/comments/check_comment_refs.py --upstream <path/to/gregtech6/src/main/java>
```

### 输出与退出码

每行一条 `path:line: kind: ref | text`，最后打印失败、符号警告与数据表问题的数量。非数据表的 `line-ref` 或 `history` 出现时退出码为 1；`unknown-symbol` 只警告。

### 局限

- 不判断注释是否有价值、是否说明了本端后果；那部分需要人工评审。
- 不自动识别 `Class, 123`、`Class 123` 等少见写法；规范禁止这些写法，但脚本不保证全部捕获。
- 不检查 JSON 的 `_comment` 字段、Kotlin 或其他语言。
- `--upstream` 只按文件名找类，不解析嵌套类与重载。

## verify_comments_only.py

把工作区改动的 Java 文件与指定提交比较，先将两侧注释替换为空白再比对；除注释外的任何差异（代码、字面量、Stonecutter 标记）都会被列出。

```bash
python tools/comments/verify_comments_only.py [--base REV] [paths...]
```

不传路径时读取 `git diff --name-only <base>` 中的 `.java` 文件，默认 `--base HEAD`。

## CI

`.github/workflows/build.yml` 的 verify job 用 `--diff` 只检查本次改动新增的 Java 注释行；历史遗留问题不会阻塞现有提交。
