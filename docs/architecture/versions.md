# Stonecutter 与跨版本修改

先读[架构](README.md)。节点由 [settings.gradle](../../settings.gradle)定义：`1.20.1-forge` 与 `1.21.1-neoforge`；当前活动/VCS 节点为 Forge，见 [stonecutter.gradle](../../minecraft/stonecutter.gradle)。

## 输入与生成结果

共享输入是 `minecraft/src/<sourceSet>/`，节点输入是 `minecraft/versions/<node>/src/<sourceSet>/`。当前 [minecraft/build.gradle](../../minecraft/build.gradle)将节点编译/资源处理接到 `stonecutterGenerate*`，使用节点 `build/generated/stonecutter/<sourceSet>/` 与节点自身源码。core 独立编译并由平台打包嵌入。

要修复生成结果中的错误，回到共享输入、条件分支或节点输入。新增类之前检查同全限定名是否已在另一输入出现，不假定节点代码会自动覆盖共享同名类。

## 修改步骤

1. 检查两平台相关实现和现有条件写法，确认是 API 签名差异、数据格式差异还是行为差异。
2. 共用逻辑放共享输入；小差异使用现有 `//? if ...` / `else` 条件。不要删除注释中的非活动分支。
3. 原生实现确有整类差异时放节点目录，保持需要共享的契约清楚。
4. `./gradlew compileBothPlatforms`，检查失败节点实际生成源码，随后修改源输入。
5. 涉及资源或打包再运行 `assembleBothPlatforms`；涉及行为则分别运行游戏验证。

不为普通修改随意切换活动版本。确需切换时先检查工作区、节点任务与生成改写范围，完成后审查是否带入大规模无关格式/分支变动。

## 已知需要单独检查的边界

- Forge 使用 Java 17、NeoForge 使用 Java 21；共享代码不能无条件使用只在 21 可用的特性。
- 物品 NBT 与数据组件、资源标识构造、注册与能力 API 存在差异，沿已有适配处理。
- 两版本数据包目录命名不同，不能把一边 JSON 路径直接拷给另一边。
- 共享构建对完整 JSON 文档中的 composite loader 名称进行转换；这不是所有资源格式都会自动迁移的保证。
- Forge 发行包经 `distributionJar` 合并 core；NeoForge 在 `jar` 合并。SDK 与模组产物分开。

版本支持、依赖版本和任务入口以仓库配置为事实来源。升级工具或加载器属于单独任务，不能因文档示例而顺手修改。
