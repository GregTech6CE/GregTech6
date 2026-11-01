# validation

只读路由与一致性检查，供 [`docs/development/validation`](../../docs/development/validation/README.md) 使用。脚本不写文件，不启动游戏，不调用 Gradle。

## 依赖

- Python 3.10 或更新版本，仅使用标准库。
- 如果环境安装了 PyYAML，脚本优先使用 `yaml.safe_load`；没有安装时使用内置的受限 YAML 读取器。内置读取器只支持 `routes.yaml` 与特例 frontmatter 使用的子集：注释行、映射、列表、行内 `[a, b]`、单双引号字符串。不支持锚点、多行字符串和行内注释。

## 输入

- `docs/development/validation/routes.yaml`：路径到 `packages`、`checks`、`overrides` 的路由。
- `docs/architecture/packages/<package>.md`：package 文档，包含 `## 验收` 与 `## 自动化测试范围`。
- `docs/development/validation/checks/<id>.md`：检查文档，包含 `## 验证方式`、可选的 `## 验证原理`、`## 注意事项` 与 `## 运行命令`。
- `docs/development/validation/overrides/<id>.md`：特例文档，出现特例时创建；frontmatter 使用 `add` / `remove`。
- `git ls-files --cached --others --exclude-standard`：用于路径闭合检查，包含未跟踪但未忽略的新文件。

## 命令

```bash
# 计算改动路径对应的验收坐标
python tools/validation/route.py route --changed core/src/main/java/gregtech/content/recipe/Foo.java

# JSON 输出
python tools/validation/route.py route --changed core/src/main/java/gregtech/content/recipe/Foo.java --json

# 校验路由闭合、检查文档、特例文档与相对链接
python tools/validation/route.py check
```

`--changed` 可传多个路径；绝对路径会按仓库根转成相对路径，Windows 反斜杠会统一为 `/`。

## 输出

`route` 输出四项：

- `packages`：命中的 package。
- `checks`：需要自动执行的检查 id。
- `manual`：以 `manual-` 开头的检查 id，通常为 `manual-ingame`。
- `overrides`：命中的特例 id。

多个路由命中时按并集计算；特例的 `remove` 作用于并集，`add` 追加检查。同一检查同时被添加和删除时脚本报错。

## 校验内容

`check` 在以下情况返回退出码 1：

- `routes.yaml` 缺少 `dimensions` 或 `routes`，或字段类型错误。
- 路由引用未声明的维度值，或声明的维度值没有任何路由使用。
- 检查 id 没有对应的 `checks/<id>.md`，或检查文档的二级章节不是 `## 验证方式`、可选的 `## 验证原理`、`## 注意事项`、`## 运行命令`，或 `###` 小节不在 `## 验证原理` 下。
- 特例 id 没有对应的 `overrides/<id>.md`，或 frontmatter 未闭合。
- 某条路由没有匹配任何文件。
- `api/`、`core/`、`minecraft/` 下 `src/` 文件没有命中任何路由，或检查集不包含 `format`，或没有命中任何 package。
- 本目录 Markdown 的相对链接指向不存在的文件。

`check` 不检查锚点标题是否存在，也不检查外部链接、代码块内的链接或 `.gitignore` 规则本身。

## 维护

- 新增 package 时，先在 `routes.yaml` 的 `dimensions.packages` 登记，再在 `docs/architecture/packages/` 建立同名文档，写 `## 验收` 与 `## 自动化测试范围`，并加入路由。
- 新增 check 时，先建立 `checks/<id>.md`，写 `## 验证方式`、`## 注意事项` 与 `## 运行命令`；需要详细设计时在验证方式后加 `## 验证原理`，再在路由中引用。
- 特例只允许减项时也要写 `overrides/<id>.md`，并在 `dimensions.overrides` 登记；没有文档的豁免不会被脚本接受。
