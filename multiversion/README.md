# Stonecutter EMI 多版本试点

分支：`experiment/stonecutter-emi`。已同步 main 的服装分类和普通变速箱修复，原始原生代码基线为 `1e3a60d0a`。

此前只有生成空目录的脚手架。本轮接入 12 对实际 EMI 类：8 对相同，4 对涉及 NBT／DataComponentPatch、FluidStack 和注册表访问差异，使用 `//? if >=1.21.1` 条件维护。只保留 `src/main/java/gregtech/emi` 在本目录下的一份实现；原两个平台目录不再保留第二份。

## 修改位置和构建方式

- 日常改动：`multiversion/src/main/java/gregtech/emi/`。活动源码为 Forge 1.20.1；NeoForge 差异保存在条件注释中。
- 生成位置：`multiversion/versions/<节点>/build/generated/stonecutter/main/java/`。此目录忽略提交，不手改。
- Forge 和 NeoForge 的既有 `compileJava` 通过 Gradle 目录 artifact 自动依赖对应 `stonecutterGenerate`，普通 `:assemble` / `:neoforge:assemble` 也会生成需要的代码。
- 双版编译：`gradlew :compileBothPlatforms`。
- 双版打包：`gradlew :assembleBothPlatforms`。不启动游戏或执行测试。
- 源码核对：`gradlew :verifyStonecutterEmiSources`。生成两版，核对全部 24 个输出的代码／字符串签名，报告写到 `build/reports/stonecutter/emi-source-parity.json`。CI 平台构建已接入该任务。

签名与源文件哈希记录在 `emi-migration.json`，直接来自迁移前原生代码。检查不依赖 Git 历史，适用于 CI 的浅克隆；修改业务行为后应有意识地更新基线，不能把新签名自动覆盖当作验证。原有源策略检查已纳入本目录。

## 范围

Stonecutter 只承担原生版本源码的选择。共享 core、原生加载器构建、注册、资源和元数据路径维持现有分层。两个节点提供源码，不是两份独立模组；Jar 仍由原 Forge／NeoForge 项目生成。其它平台代码尚未迁入 Stonecutter，不宣称全项目已经单源码化。

保留 Stonecutter 0.7.11 与 Gradle 8.8。配置依据为官方 [GitHub 镜像的固定 0.7 源码](https://github.com/stonecutter-versioning/stonecutter/tree/fffcb7151d83bf6e40f3631131143fb08c06f230)，LGPL-3.0；没有复制插件实现。用户提供的 [Codeberg 仓库](https://codeberg.org/stonecutter/stonecutter)由网页读取工具访问时被 robots.txt 拒绝，本轮使用已保留的固定官方镜像源码核对 API。

## 验证边界

2026-10-18 已通过实际双版源码生成及 24 个输出的代码／字符串一致性核对，两个平台编译通过。完整打包结果见 `docs/integration/verification/stonecutter-emi-20261018.md`。没有启动游戏、验证旧存档或跑完整生存链。该试点仅在独立分支，是否继续扩大迁移以后续安排为准。
