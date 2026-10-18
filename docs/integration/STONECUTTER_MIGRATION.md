# Stonecutter 完整迁移

用户要求在 `experiment/stonecutter-emi` 分支完整迁移，不以 EMI 试点作为验收终点。
Stonecutter 是构建和源码管理工具；Git 托管仍为现有 GitHub 仓库。本分支不自动推送或合并 main。

## 范围与验收

- [x] 固定 Stonecutter 0.7.11；版本节点为 1.20.1 Forge、1.21.1 NeoForge。
- [x] 保留纯 Java `core`，迁移所有原生 main、test、bootstrapGameTest、productionSmoke 源集。
- [x] 相同类共用源码，平台差异使用条件或版本专有类；迁移资源并处理复合模型 loader 差异。
- [x] 全量生成结果与迁移前基线核对：2,630 个 Java 输出、14,291 个资源输出。
- [x] 原生编译、打包、开发运行配置由版本节点持有；双版主代码编译通过。
- [x] 生成器和自检使用统一原生目录入口，修改能够回写受管理源树。
- [x] 更新 CI、交付构建、探针和其他启动工具，消除旧并行原生目录依赖。
- [x] 双版成品构建、验包、必要的定向启动回归，记录实际结果。
- [x] 更新开发说明和迁移清单，本地提交完整迁移。

## 目录

- `core/`：共享领域逻辑、API、通用资源，不引入 Minecraft 或加载器类型。
- `multiversion/src/<sourceSet>/`：共同的原生源码与资源，Forge 为 VCS 活动版本。
- `multiversion/versions/<version>/src/<sourceSet>/`：真正的平台专有源码、资源覆盖和元数据。
- `multiversion/versions/<version>/build/generated/stonecutter/`：Stonecutter 生成输出，不直接编辑。
- `gradle/platform-{forge,neoforge}.gradle`：加载器依赖、映射、运行和生产打包边界。
- `multiversion/migration-manifest.json`：迁移基线及全部原生文件的代码/字符串或资源签名。

## 已有证据及限制

基线为 `4e39348c6`（包含之前的 EMI 试点），完整源码签名保留了普通变速箱物品模型和服装分类修复。
`verifyStonecutterMigration` 对四个 Java 源集及全部资源逐文件核对迁移前签名，既检查缺失也检查重复/意外新增。
JSON 对比忽略格式，其他资源逐字节对比；Java 对比代码和字面量，保留原有版权注释。

日常 CI 使用 `verifyStonecutterSources` 检查**当前受管理源树**的完整输出与工具投影，
不强迫未来的正常功能修改继续匹配冻结的迁移签名。冻结的 migration-manifest 是历史审计证据，不应为掩盖业务差异重写。

## 日常命令

从仓库根目录运行，Gradle 启动 JDK 17，另用 `JAVA21_HOME` 提供 NeoForge JDK 21。

```text
./gradlew compileBothPlatforms
./gradlew assembleBothPlatforms
./gradlew build
./gradlew :multiversion:1.20.1-forge:runClient
./gradlew :multiversion:1.21.1-neoforge:runClient
./gradlew prepareNativeTools verifyStonecutterSources
```

产物在 `multiversion/versions/<version>/build/libs/`；Forge 仍先重混淆原生类，再嵌入未改写的 core。
`runServer`、`runGameTestServer`、`productionSmokeJar` 都由对应版本节点持有。
根 `runClient` 等兼容入口仅委托 Forge 节点，不保存第二套代码或加载器构建配置。

编辑跨版本类时修改 `multiversion/src`，使用 Stonecutter 条件；平台专有类修改版本节点的 `src`。
不要手改 `build/generated`。两个平台采用相同资源时保留共同文件；内容确有差异时才使用版本资源覆盖。
新增或修改 loader API 代码后运行对应节点编译，不要将 Minecraft 类型引入 core。

Stonecutter 活动版本可在 IDE 中切换，也可使用原生任务：

```text
./gradlew ":multiversion:Set active project to 1.21.1-neoforge"
./gradlew ":multiversion:Reset active project"
```

切换会处理共同文件中的条件注释，并双向转换复合模型 loader；提交前重置到 Forge VCS 版本。
已有生成器的回写入口采用 Forge 条件格式，因此运行写入型旧生成器前也应先重置；只读检查支持两种活动版本。

## 已有生成器与文本检查

284 个已有 Python 工具的原生路径接入 `tools/native_layout.py`，不再读写旧的 `src/`、`neoforge/src/`。
`prepareNativeTools` 先执行全部 Stonecutter 生成任务，再构造忽略目录 `build/native-tools/` 下的完整版本视图。
视图去掉非活动分支和条件标记，保留活动代码、字符串、普通注释和版权。全量 2,630 个 Java 投影另做语义签名核对。
临时测试仓库或外部原版参考源码没有迁移清单时，入口保持原路径；不会改写原作者目录。

只读检查可沿用原命令。会写原生内容的生成器使用统一入口：

```text
python tools/native_tool.py tools/<generator>.py [原有参数]
python tools/native_tool.py tools/integration/<generator>.py [原有参数]
```

生成器成功后，入口将视图改动回写成共同源码、版本条件或真正的版本覆盖；失败不会回写受管理原生文件。
两版相同内容自动共用，复合模型 loader 的命名空间差异保持为生成规则。常用写入型工具有直接调用保护，
避免把改动留在临时视图却误以为已修改源码。生成后重新运行 `prepareNativeTools`，核对 Git diff 再编译。
这些旧工具已有的其他参数、原始源码输入和适用范围不变；包含旧 `F:` 硬编码参考路径的历史脚本仍不可直接运行。

## 最终验收（2026-10-18）

全部源集生成、编译、双版成品和 SDK 验包通过。成品各含 823 个当前共享类；Forge 7,514、NeoForge 6,684 份当前原生处理资源逐字节与 JAR 一致。80 项 Python 定向检查、451 分组及四组共享合约通过。活动版本切至 NeoForge 后全量签名通过，已切回 Forge；删除代表性生成文件后，各源集能随编译自动恢复。

Forge 47.4.26 / Java 17 和 NeoForge 21.1.252 / Java 21 的真实成品客户端通过主界面及独立资源、材料数量、双语回调探针。NeoForge 21.1.243 开发节点 + EMI 1.1.24 + REMI 4.7.12 新世界探针通过：13 个普通变速箱模型、78 个可调变速箱贴图面、24 件服装归组、全部列表/搜索/展开收回及流体用途查询。

完整原始 2,630 Java 输出曾逐项匹配基线。最终另更新了七个独立成品探针（双版共 14 输出），使其核验此前已经去重的正式 ID；这部分旧/新签名与原因单列在 `migration-test-updates.json`，冻结迁移清单不改。全部 2,165 个原生 main 输出和 14,291 个资源输出保持迁移前语义/内容，core 主代码未改。

本轮没有触发远端 CI、合并 main、推送或运行完整生存/旧存档兼容测试。一次成品 NeoForge 在原生早期窗口初始化时退出，未改包重试通过；原因未定位，保留失败记录。

详见 [验收摘要](verification/stonecutter-full-summary-20261018.json)、[最终签名](verification/stonecutter-full-final-parity-20261018.json)、[验包](verification/stonecutter-full-packaging-20261018.json)、[开发实机](verification/stonecutter-full-neo-runtime-20261018.json)和双版 [Forge](verification/stonecutter-full-production-forge-20261018.json) / [NeoForge](verification/stonecutter-full-production-neoforge-20261018.json) 成品回执。

