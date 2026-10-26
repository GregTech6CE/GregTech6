# 资源、生成内容与来源

返回[架构](README.md)。先判断文件是不是实际游戏输入，再判断能否重新生成；不要用“生成的”概括两类不同内容。

## 资源层次

| 路径 | 用途 |
| --- | --- |
| `core/src/main/resources/assets/gt6ce/` | 大量共享贴图、模型、声音及其他客户端资产 |
| `core/src/main/resources/data/` | 共享数据资源 |
| `minecraft/src/main/resources/` | 共享 Minecraft 资源输入，经 Stonecutter 处理 |
| `minecraft/versions/<node>/src/main/resources/` | 原生元数据、Mixin/插件声明、版本格式的数据与资源 |
| `core/src/main/resources/META-INF/gregtech6/`、`META-INF/licenses/` | 随模组分发的项目及第三方许可与来源声明 |

新增资源沿现有 namespace、命名和对应版本目录，检查语言键、模型引用、纹理引用与注册 ID。1.20.1 常见 `loot_tables`、`tags/items` 与 1.21.1 的 `loot_table`、`tags/item` 等路径不能混用；以目标节点现有文件及实际加载结果为准。

## 两类生成内容

- **维护的生成输入**：`core/src/main/java/gregtech/content/{data,material,recipe}/generated/` 等内容表参与生产编译，需要纳入版本控制；文件头可能引用历史转换工具。
- **可再生构建输出**：各节点 `build/generated/stonecutter/`、编译类、资源副本、JAR 与构建报告由 Gradle 生成，Git 忽略，不能手工维护。

例如 `MaterialRegistryExtras` 引用了历史 `tools/generate_material_metadata.py`，部分配方表引用 `tools/transpile_gt6_chem.py`。当前工作树未包含这些工具，不能把这些命令写成可直接运行的流程。需要改表时先确认上游来源与转换方式；恢复可维护生成器后放 `tools/`，注明输入版本、许可证、命令与输出范围。生成器尚缺失时，确需修表应说明依据、局部变更及无法重新生成的限制。

## 数据生成与打包

Forge `runData` 的 `--output` 当前直接指向 `minecraft/versions/1.20.1-forge/src/main/resources/`，见 [Forge 配置](../../gradle/platform-forge.gradle)。它会写生产输入，不能把整个运行结果无审查提交，也不能当成仅读取的验证命令。运行工作目录 `run-data/` 本身仍被忽略。

[增量资源脚本](../../gradle/incremental-resources.gradle)负责资源同步，共享资源体积较大。不要为了排查而随意复制整个目录、改 ZIP64、取消重复项失败检查或每次清缓存。新增资源后先检查来源，再检查两节点 `processResources`/发行 JAR。

资源验证至少覆盖：路径大小写、有效格式、目标版本路径、资源引用存在、没有私有资料/临时文件混入发行包。贴图/模型还需要游戏内观察。只在明确用途下保留必要的精简示例或文档插图，原始截图留 `work/`。

## 许可与来源

项目代码声明见仓库根 [LICENSE](../../LICENSE) 与 [GPL 全文](../../GPL-3.0.txt)；随模组分发的副本在 [META-INF/gregtech6](../../core/src/main/resources/META-INF/gregtech6)，第三方声明在 [META-INF/licenses](../../core/src/main/resources/META-INF/licenses)。导入或修改资产时保留适用声明与来源，不能因为主项目许可证就覆盖第三方资产条款。
