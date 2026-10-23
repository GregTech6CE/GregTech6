# Stonecutter EMI 分支试验

分支 `experiment/stonecutter-emi`，同步 main 修复提交 `1e3a60d0a` 后继续此前 `3b8d41fde` 的空脚手架。服装分类和普通变速箱的根因修复已在 main 独立保存；本试验不自动合并回 main、不推送远端。

## 本次落地

- 两平台 EMI 目录的 12 对 Java 类迁入 `multiversion/src/main/java/com/gregtech/gregtech/emi/`，删除 24 份原生副本。
- 8 对完全相同，4 对存在 API 差异：`EmiCatalogRepair`、`FluidDisplayEmiStack`、`MachineEmiIngredients`、`MachineEmiRecipe`。NBT／组件、流体类型、注册表访问通过 Stonecutter 版本条件选择；原配方、索引和界面逻辑不重写。
- 两个现有加载器项目通过 Gradle consumable 目录 artifact 接入各自的 `stonecutterGenerate`，任务依赖随 artifact 传递，生成目录不提交。
- `compileBothPlatforms` / `assembleBothPlatforms` 是统一的双版编译／打包入口；原单版入口仍可直接使用。
- 迁移前每个文件的 SHA-256 和代码／字符串签名保存在 `multiversion/emi-migration.json`。`verifyStonecutterEmiSources` 生成两版并比较全部 24 个结果，不依赖完整 Git 历史，CI 构建接入该任务。
- 源策略检查纳入新增源码目录，保持 core 不依赖 Minecraft 或加载器。

## 依据和许可

沿用已固定的 Stonecutter 0.7.11 / Gradle 8.8，参考官方镜像 `0.7` 提交 `fffcb7151d83bf6e40f3631131143fb08c06f230` 的 `StonecutterBuildImpl`、`StonecutterBuildTasksImpl`、`SCPrepareTask` 了解生成路径和任务依赖。插件源码为 LGPL-3.0，本次没有复制插件实现；迁入的 EMI 代码来自本项目上述基线，原作者／许可不变。指定的 Codeberg 地址被网页工具按 robots.txt 拒绝读取，使用前次已保留的官方镜像源码核对，未要求补页面。

## 核验与边界

源码生成／签名核对 16 秒通过：Forge 和 NeoForge 均为 12 个类，代码和字符串与各自迁移前版本一致。源策略检查通过：2629 Java 源文件。`compileBothPlatforms assembleBothPlatforms` 实际执行，两个平台编译／打包 8m48s 通过。首次切换分支触发 72193 份 core 资源及 7514 份 Forge 资源全量处理，不将该总时长视为 Stonecutter 预处理耗时。两份成品 ZIP CRC 通过，各包含 12 个 EMI 类、13 份普通变速箱修复及 24 件服装分类；没有打入构建插件类，旧分类绘制回退已移除。最终打包回执见本目录的 `stonecutter-emi-build-20261018.json`。

普通变速箱与可调变速箱的双版 52 个物品根模型父链定向检查、451 分组一致性已通过，详见 [main 修复记录](transformer-gearbox-clothing-20261018.md)。本轮未运行游戏或完整测试，物品 GUI 视觉、旧存档、生存链独立待验。

试点完成范围是 EMI 的单份源码与真实双版构建接线；其它原生平台类、资源和注册边界没有迁入 Stonecutter。继续扩大迁移时按功能批次处理，不能仅因本试点通过就宣称整个项目已完成单源码多版本迁移。

具体修改位置与命令见 [使用说明](../../../multiversion/README.md)。
