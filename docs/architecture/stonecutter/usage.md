# Stonecutter 用法与生成

本文说明 `//?` 条件标记的语法规则、源码输入目录、构建生成与验证入口。

## 输入

| 输入 | 路径 | 是否经 Stonecutter |
| --- | --- | --- |
| 主模组共享输入 | `minecraft/src/<sourceSet>/` | 是 |
| 主模组节点输入 | `minecraft/versions/<node>/src/<sourceSet>/` | 否 |
| 游戏侧 API 共享输入 | `api/minecraft/src/main/` | 是，由 `:api-platform:<node>` 处理 |
| 公共 API | `api/common/` | 否 |

节点由 [settings.gradle](../../../settings.gradle) 定义：`1.20.1-forge` 与 `1.21.1-neoforge`。

## //? 语法与规则

`//?` 的定义与归属见[设计](design.md)；本节只列写法与规则。

```java
//? if >=1.21.1 {
/*        return handler.serializeNBT(lookup);
*///?} else {
        return handler.serializeNBT();
//?}
```

规则：

- **作用于共享输入**：将 `//?` 写在经 Stonecutter 处理的 `minecraft/src` 与 `api/minecraft/src` 目录；节点专属目录 `minecraft/versions/<node>/src` 直接编译。
- **共享调用直写**：两个版本拥有同名同义方法时直接调用共享方法，构造器、常量、字段和版本无关逻辑保持单份。
- **结构完整**：分支保持完整的方法签名、闭括号与字段声明，局部聚焦于差异片段。
- **双侧语义一致**：分支只切换实现路径并保持相同的输入/输出语义；确需不同行为时注明原因并分别验证。
- **非活动分支可编译**：保留清晰可读的非活动分支代码并清理占位注释，修改后核对失败节点的生成源码。
- **类级注解紧贴类声明**：`@EventBusSubscriber` / `@Mod.EventBusSubscriber` 的 `//?` 分支紧贴类声明之前，确保生成的注解直接作用于类定义。
- **分支注释放在 `/*` 之内**：分支体首行以 `/*` 开头，所有分支内注释放在 `/*` 与 `*/` 之间，避免活动节点失去注释标记。

当前绝大多数分支写作 `//? if >=1.21.1`；`//? if <1.21.1` 只用于少数反向条件。活动节点是 `1.20.1-forge`，非活动分支被 `/* ... */` 包住。

资源侧还有两条构建期规则：

- `stonecutter.filters.include('**/*.json', '**/*.mcmeta')`：只对这两类资源做条件处理（[minecraft/build.gradle](../../../minecraft/build.gradle)）。
- composite loader 名称在完整 JSON 文档中双向替换：`forge:composite` ↔ `neoforge:composite`。Java 文本块不参与替换。

## 生成

主模组由 `stonecutterGenerate`（主源集）与 `stonecutterGenerate<SourceSet>`（其他源集）生成到 `build/generated/stonecutter/<sourceSet>/`，再与节点源一起编译：

- Java：`build/generated/stonecutter/<sourceSet>/java`
- 资源：`build/generated/stonecutter/<sourceSet>/resources`

[minecraft/build.gradle](../../../minecraft/build.gradle) 把 `compileJava` 与 `processResources` 接到对应的生成任务，并把生成目录排在共享源目录之前。

生成任务保留 Stonecutter 的 `Sync` 契约，但使用增量同步（[incremental-stonecutter.gradle](../../../gradle/incremental-stonecutter.gradle)）：只有源文件变化时才写入，源文件消失时删除对应输出。

根任务入口（[stonecutter-native.gradle](../../../gradle/stonecutter-native.gradle)）：

| 任务 | 作用 |
| --- | --- |
| `compileBothPlatforms` | 生成两个节点的主源码并编译 |
| `assembleBothPlatforms` | 生成并组装两个发行包 |
| `compileJava`、`runClient`、`runServer`、`runData` 等 | 委托给 `1.20.1-forge` 节点 |

GameTest 模板只维护一份 `minecraft/src/test/templates/*.nbt`，由 `processTestResources` 按节点映射到 `data/gt6ce/structures` 或 `data/gt6ce/structure`。

主模组依赖 `:api`、`:core` 与两个节点项目，并把各自的输出嵌入最终包。

## 格式化与生成副本

Spotless 排除 `**/generated/**`，并排除含 `//?` 的共享文件；共享源在 `:minecraft` 统一格式化，节点项目沿用同一排除规则。删掉文件中的最后一个 `//?` 后，文件会进入格式化范围，diff 变大属于预期。

生成副本不是修复位置。生成结果中的错误应回到共享输入、条件分支或节点输入修改。

## vcsVersion 与 active

| 配置 | 位置 | 当前值 | 含义 |
| --- | --- | --- | --- |
| `vcsVersion` | [settings.gradle](../../../settings.gradle) | `1.20.1-forge` | 版本控制中保留的分支形态 |
| `stonecutter.active` | [minecraft/stonecutter.gradle](../../../minecraft/stonecutter.gradle)、[api/minecraft/stonecutter.gradle](../../../api/minecraft/stonecutter.gradle) | `1.20.1-forge` | 当前编译与生成的活动节点 |

两者当前一致，但职责不同：`vcsVersion` 决定提交进仓库的源码形态，`stonecutter.active` 决定本次构建使用哪个节点。日常开发保持活动版本不变；确需切换时先检查工作区、节点任务与生成改写范围。

## 验证

- 双平台编译：`./gradlew compileBothPlatforms`，或分别运行两个节点的 `compileJava`。
- 格式检查：`./gradlew spotlessCheck`。
- 生成源码检查：确认 `build/generated/stonecutter/<sourceSet>/` 与源输入一致；不在生成副本上修复。
- 具体检查项与命令见 [compile](../../development/validation/checks/compile.md) 与 [format](../../development/validation/checks/format.md)。
