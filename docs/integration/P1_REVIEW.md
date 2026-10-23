# P1 独立静态审查

审查日期：2026-09-30。审查对象是当前整合工作树，尚未冻结提交；正在执行的构建和 bootstrap GameTest 由主任务记录。审查者未运行 Gradle、未修改源码/构建文件/来源目录，仅读取现有输出及执行内存中的验证器反例。

## 结论

共享 Java 17 core 与两个平台入口的方向可以继续。已核对的源集、JDK 选择、失败退出和 jar 的一次嵌入配置没有发现必须推倒重做的问题。初次审查发现的 R1/R2/R3 已按主任务要求修复并完成工具反例检查，见末尾修复记录；最终 Forge 整合 jar 和远程 CI 仍需要各自实测。修复后的打包检查不代替运行、玩法或授权结论。

## 初次发现（修复前，行号对应当时工作树）

### R1 / P2：打包验证器可接受错误 metadata 和同一份过期 core

证据：`tools/integration/verify_artifacts.py:16-17` 只确认 loader descriptor 路径存在；`:40-41` 从当前编译目录提取类名；`:19-22` 计算 jar 内对应条目的 hash；`:46-48` 仅比较两 jar 的 hash 彼此相等。

触发：两个输入 jar 都包含完整的当前类名集合，但 descriptor 声明另一 modId/版本，且类内容来自同一份旧编译输出。验证仍会返回 passed，因为没有解析 metadata，也没有把条目 hash 与本次 `core/build/classes/java/main` 的内容比较。这不能证明测试的是本次整合制品，重用旧 jar 时尤其容易给出错误证据。

复现：在内存创建两个 ZIP，分别含 Forge/NeoForge descriptor 路径，内容均为 `modId="wrong_mod"`、`version="0.0.0"`；完整 9 个 core 类路径的内容均为同一占位旧字节。现有 `inspect()` 接受两包，其 hash 字典相等，满足 `main()` 的唯一双包比较条件。整个反例未写入文件，且不把该占位字节称为可加载 class。

建议：解析 TOML 并核实唯一 `gregtech` mod、期望整合版本、对应 Minecraft/loader 依赖；确认输入文件名和生产输出的关系；验证完整 core 条目集合和当前编译输出。Forge reobfuscation 若重写无 loader 依赖的 class 字节，应先用实际新 Forge 包确定变换差异，再选择可解释的规范化比较/变换前后 provenance，不能跳过一致性证明，也不能以未知变换误判失败。增加错误 modId、错误版本、旧 core、缺 class、重复条目等反例。

### R2 / P2：CI 的打包门比本地门更弱，没有验证完整共享核心

证据：`.github/workflows/build.yml:89-90` 只检查 descriptor 路径，`:95-96` 只要求 `MachineWorkCost.class` 一个 core 类；`:91-94` 只检测重复 class 条目；`:128-137` 汇总的是矩阵 job 结果，没有下载/比较两个平台包。

触发：Neo 平台误漏 `GTValues`、`CrucibleMath` 或其余尚未在入口调用的类时，只要 `MachineWorkCost` 仍存在就可通过该检查；两腿嵌入不同 core 内容也没有由 CI 汇总门排除。`core:check` 通过只证明单独编译的 core 行为，不能证明各 jar 的嵌入内容。

建议：每个 job 调用同一严格检查逻辑，对当前 core 完整集合与 metadata 核实并输出 hash/provenance；汇总 job 核实两腿证据对应相同提交与 core。artifact 上传前还应检查所有 ZIP 条目的重复项及 test sourceSet 排除。缺少任一腿/任一 core 条目必须失败，而非靠测试入口碰巧引用来兜底。

### R3 / P1（未来公开远程启用前）：授权未决，CI 自动上传衍生二进制

证据：`.github/workflows/build.yml:3-6` 自动运行 push/PR，`:107-112` 在构建成功后无许可或仓库可见性条件上传 jar。`gradle.properties:58` 仍为 All Rights Reserved；`docs/integration/LICENSE_REVIEW.md` 记录项目1模板 LICENSE 不覆盖移植整体授权，公开分发门未通过。

影响：将此工作流启用在未来公开仓库后，测试 artifact 上传也可能向有仓库读取权限的人提供完整衍生 jar；“不发 Release/Maven”并不落实文档中尚未通过的二进制分发门。当前没有已配置远程/运行证据，此项不是声称已发生发布，也不阻止本地构建。

建议：授权解决前仅在明确允许的私有协作范围保存 CI 二进制，或关闭公开仓库上的 jar 上传、继续构建及必要的摘要/诊断。公开上传需显式许可状态、实际采用分项的许可/NOTICE/署名和最终 jar 内容清单。不得通过更改根 LICENSE 或将资源笼统标为 CC0 来解除门禁。

## 已核实的配置与现有输出

- `settings.gradle:19-20` 包含单一 core 和 Neo 模块；wrapper 固定 Gradle 8.8。根 ForgeGradle 6.0.54、MixinGradle 0.7.38 与 Neo ModDevGradle 2.0.142 均为固定版本。Java 17 launcher、core/Forge Java 17、Neo Java 21 的分工明确。
- `scripts/build.ps1:24-28` 检查 java 与 javac 并核实 major；`:34-35` 要求所选平台的 JDK；`:46-48` 设 launcher/toolchain 环境；`:50-58` 顺序构建、捕获非零退出并抛错；`:60-64` 恢复环境和工作目录。未发现吞掉构建失败的路径。`gradlew.bat` 使用标准 JAVA_HOME/退出传播逻辑。静态审查没有实际执行脚本或证明其它机器环境可用。
- `core/build.gradle:11` 使用 `--release 17`；`:19-28` 将显式抛错的行为合约接入 check。不是只运行默认 test 后宣称 main 合约通过。抽取 provenance 保留来源文件和 hash，原 Forge 类路径已移动；当前仅有一个生产定义源。
- 根 `build.gradle:229-232` 与 `neoforge/build.gradle:87-90` 从同一 core sourceSet output 嵌入一次，duplicates FAIL、稳定时间/顺序；根 Forge 启用 Zip64 并 finalizedBy reobfJar（`:249-252`）。当前 core 全部依赖 java.base；没有 Minecraft、Forge 或 Neo 类进入 core。
- 已读取的 Neo jar `gregtech-neoforge-1.21.1-0.1.0-integration.1.jar` metadata 为 `gregtech` / `0.1.0-integration.1`；9 个共享 class 字节与当前 core 编译目录一致；未含 `CoreBehaviorContracts` 或 `SharedCoreBootstrapGameTests`。这是对该既有文件的打包检查，不是 Neo 运行结果。
- 审查时根 `build/libs` 仅有旧 `gregtech-1.0.0.jar`，整合 Forge jar 尚未得到审查。旧 Forge 基线与当前 Neo 包实际被验证器拒绝，9 个 core class 字节不同；没有将这个旧包比较结果归因于新 Forge 构建缺陷。
- CI 两个矩阵项、`fail-fast: false`、显式 JDK 环境、禁自动下载、`:core:check`、`bash` 管道失败传播及 aggregate job 的失败条件均可见。未执行 GitHub Actions，不宣称远程 CI 已通过。

## 其它较低风险工具缺口

- `core/verify-core.ps1:5-6,14-20` 创建但未清空 standalone class 输出。若测试源码以后被删除，旧 `CoreBehaviorContracts.class` 可能仍被运行。此辅助脚本的“通过”应要求清洁隔离输出；正式 Gradle check 不依赖它。当前未发现测试源码缺失。
- `tools/integration/verify_sources.py:23-26,61` 未要求 summary 存在请求的 author；若 summary 缺条目，`--source` 或 `all` 可零检查成功。当前 summary 实际含三源，因此不否定已完成的当前 SHA 核查；后续应核实 exactly-one 的三源集合、清单文件数和路径安全。`--list` 只验证清单自身 hash，不能称已核对源文件。

## 架构和验收门

先保留项目1完整 Forge，再逐个移动确实可独立的纯规则，是合理的可逆起点。当前八个纯 Java 类型并未覆盖材料注册、配方载入、机器持久化或第一条生存玩法。Neo 启动骨架不得被写成已支持项目1全功能。

下一阶段需坚持：稳定材料/前缀/配方 identity 由共享领域定义；平台实现只转换生命周期、能力、流体、网络、NBT 和渲染对象；跨版本单位和温度转换明确；不复制 brokestar/masson 的整套底层目录填补 Neo。每次迁移先保存 Forge 原行为与来源，再验证两个适配器调用同一规则。包内容去重不等于运行注册去重，应实际捕获 registry ID 与悬空配方/引用，并做客户端类对专服的隔离检查。

`bootstrapGameTest` 是独立源集，生产 jar 默认仅包含 main 和 core 输出；Neo bootstrap 三项只测 core linkage/固定数学值。即使官方开发 GameTestServer 成功，也只可记录“开发 server bootstrap + 共享逻辑”证据。生产 jar 在实际专服环境加载、客户端画面/交互、可选 JEI/Jade 缺省/启用、真实取得原料并完成青铜链、磁盘保存/关服/重启、旧世界迁移仍须各自验收。NBT 内存往返、日志声明和编译不能替代这些门。

当前没有可证明的其它实质构建配置错误；正在变更的 bootstrap 配置及新 Forge 包需在最终冻结工作树后复核。此审查没有宣告 P1、授权门或完整整合目标完成。

## R1/R2/R3 修复记录

日期：2026-09-30。只修改 `tools/integration/verify_artifacts.py`、`.github/workflows/build.yml` 和本记录；没有改 Java、来源目录或其它构建域，没有运行 Gradle、提交或外部发布。

- R1：统一工具新增 `--platform forge|neoforge --jar PATH` 单腿模式，保留 `--forge PATH --neoforge PATH` 双腿模式；不完整/混用参数和缺失制品失败。Python 内置 tomllib 解析 descriptor，确认唯一 gregtech、根 properties 的整合版本/保留许可、javafml、对应 loaderVersion、目标 Minecraft 1.20.1 或 1.21.1、正确 loader 的 required/BOTH 依赖和准确版本范围。Neo 范围与当前固定模块 21.1.243 对齐，后续有意升级须同时更新门禁。另一个 loader descriptor、重复 dependency 和重复 ZIP 条目均拒绝。
- R1：当前 `core/build/classes/java/main` 的完整 class SHA 集合为权威，空输出失败；每个 class 必须在 jar 恰好出现一次且字节 hash 等于当前编译输出；双腿另比较彼此相等。两包同旧字节不再能靠彼此相等通过。测试源集路径从 core test 和两平台 bootstrapGameTest 收集，同时保留已知类的硬性排除；包含 inner class/资源污染也失败。未笼统拒绝 Forge 原有 main GameTests。
- R2：CI 显式安装 Python 3.12，所有单腿打包检查调用同一工具，不再保留只查 MachineWorkCost 的弱逻辑。每腿上传纯 JSON packaging evidence；汇总门必须拿到两个通过且对应本次 GitHub SHA 的报告，核实完整 core 集合/每腿 jar 与编译 core 相符、版本/identity 一致及两腿所有 core hash 相等。矩阵缺任腿、取消、失败仍使总门失败。
- R3：模组二进制 upload-artifact 仅在 `github.event.repository.private == true` 执行；公开仓库仍执行构建、统一验包、纯 JSON 证据上传和双腿汇总，并明确记录授权未决故未上传 jar。该条件限制 CI 二进制传播，不代表来源授权已解决，也不承诺公开源代码发布已获许可。当前没有已运行的 GitHub CI 或公开上传，根 LICENSE/第三方声明未改。

验证结果：

1. AST 检查：统一工具及两段 CI Python 均通过；现有本机 PyYAML 解析工作流，核实两条固定矩阵、构建无 private 条件、二进制上传有 private 条件。没有安装第三方包。
2. 工具正/负例 49 项通过：两平台正确 descriptor/完整 core；错误 modId、版本、MC、modLoader、loaderVersion、loader dependency、非 required 依赖、非法 TOML、第二 mod；缺少非 MachineWorkCost 的 core、旧字节、重复 class/资源、core 合约 inner class、两平台 bootstrap class/资源、混合 loader descriptor；空 class 目录、无制品、不完整/混用 CLI；双腿 JSON 及 hash 完整性；原有 Forge main GameTests 允许。
3. CI 嵌入程序执行回归 9 项通过：两条单腿严格检查、正确汇总；无 jar、缺腿、错误提交、jar/core 不一致、两腿 core 不一致、版本不一致均失败。测试在 `work/` 下隔离临时目录执行，临时 ZIP/报告已清除；没有执行 Actions。
4. 真实现有 Neo jar 通过：9 个 current core class、正确 metadata、无重复/test-only entries。真实旧 Forge 1.0.0 jar 因 mod version 与当前整合版本不同被拒绝。Forge 正例为仅用于验证工具逻辑的合成 ZIP，不能作为实际 Forge 构建或加载证据；最终整合 Forge jar 尚待主任务用严格工具检查。

仍待验收：最终生产 Forge/Neo jar 的双腿检查，实际 GitHub runner 构建和汇总；开发/生产运行、客户端、生存玩法、磁盘存档重载及授权门仍独立记账。较低风险的 standalone 旧 class 残留与来源清单零检查问题不在此次获准修改范围内，仍保留待处理。

## Forge reobf 字节差异补充审查

2026-09-30，主任务已产出初次整合 Forge jar，严格门在 `DefinitionCatalog.class` 失败。此节只读研究该失败，未放宽门或修改构建/Java，也未运行 Gradle/javac。观察样本是 `build/libs/gregtech-1.20.1-forge-0.1.0-integration.1.jar`，SHA-256 `8b8d4618bc1e29a15be60b92bdf98dba74f5686d28d8ad6b22b29573f7ee9a2a`；80,435 个 ZIP 条目且含 Zip64 结束记录。后续重新组装的制品应使用新的校验和，不能借用此样本证据。

逐类结果（左侧当前 core、右侧 Forge reobf；CP count 包含 classfile 规定的保留槽位）：

- DefinitionCatalog：2,727 → 2,727 字节，CP 129 → 129；CP 内容多重集合不变，顺序/引用索引改变，2 处属性序列重排。
- EnergyPackets：770 → 750 字节，CP 44 → 42；仅删除未引用的 `java/lang/Long` Class 与 Utf8 项，2 处属性序列重排。
- GTVoltageTiers：1,739 → 1,739 字节，CP 116 → 116；CP 内容集合不变，4 处属性序列重排。
- CrucibleMath：1,076 → 1,076 字节，CP 43 → 43；CP 内容集合不变，2 处属性序列重排。
- AtomicProperties：2,412 → 2,412 字节，CP 98 → 98；CP 内容集合不变，2 处属性序列重排。
- GTValues：519 → 519 字节，CP 36 → 36；CP 内容集合不变，仅 CP 顺序/索引改变，属性序列未改变。
- PartBindings：2,546 → 2,546 字节，CP 111 → 111；CP 内容集合不变，1 处属性序列重排。
- MachineWorkCost$Cost：1,724 → 1,724 字节，CP 66 → 66；CP 内容集合不变，1 处 class 属性序列重排。
- MachineWorkCost：1,515 → 1,495 字节，CP 82 → 80；同样只删未引用的 `java/lang/Long` Class 与 Utf8 项，2 处属性序列重排。

验证方法与边界：JDK 17.0.4 `javap -p -c -s -l -constants` 比较，只有常量池编号和排版空白被规范化，9/9 输出相等。随后用只读内存 classfile 解析器完整解析全部字节，将 CP 引用解析为实际值并按名称比较属性；未遇到未解析属性。9/9 的 classfile 版本/flags/继承、成员名字/描述符/flags、逐指令 opcode/位置/操作数/引用符号、异常表、StackMap frames、LineNumberTable、LocalVariableTable/TypeTable、Signature、ConstantValue、Record、NestHost/NestMembers、InnerClasses、SourceFile 和 BootstrapMethods 均相等。Neo jar 的全部 9 类仍与当前 core 字节逐项相同。

属性重排实例：DefinitionCatalog.validated 的 Code 内由 `[LineNumberTable, LocalVariableTable, LocalVariableTypeTable, StackMapTable]` 改为 `[StackMapTable, LineNumberTable, LocalVariableTable, LocalVariableTypeTable]`；Cost record 的 class 属性由 `[SourceFile, NestHost, Record, BootstrapMethods, InnerClasses]` 改为 `[InnerClasses, SourceFile, BootstrapMethods, NestHost, Record]`。两个减小的类仍保留相同的已内联 Long.MIN_VALUE 常量，未改变实际 Long 调用。这次差异属于 classfile 序列化重写，没有观察到真实符号或指令变化；这项结论只绑定已分析的 9 类和样本，不是全 Forge 游戏行为通过，也不解除明确的字节一致门。

### 构建方案的可行性与风险

可采用主任务提出的链路：纯 Forge `jar`（只含 main）→ `reobfJar` → `distributionJar`（重新封装 reobf 后的平台内容，加未改写的当前 core output）。它把共享纯 Java 类放到不会经过 Forge renamer 的最后一步，可以保持两平台实际 class 字节完全一致，同时保留平台类必要的 reobf。

已查本机官方 ForgeGradle 6.0.54 sources JAR：`net/minecraftforge/gradle/userdev/tasks/RenameJarInPlace.java:33,48-50,87` 使用内部 temp `output.jar`，完成后复制覆盖 `getInput()`；`UserDevPlugin.java:329-337` 创建 reobf task、将 assemble 依赖它，并从目标 Jar task 的 archiveFile provider 提供 input。因此最终拼装应依赖 `reobfJar` 完成后再读取 `jar.archiveFile`，不能假设存在可引用的独立公开 reobf output provider。这些来源在 `C:/Users/Asus/.gradle/caches/modules-2/files-2.1/net.minecraftforge.gradle/ForgeGradle/6.0.54/f4b8de79ffba85421e277a39c2bfb098466d1399/ForgeGradle-6.0.54-sources.jar` 内；未改写或提取第三方源码。

实施门禁：

1. intermediate Jar 输出放入 `build/intermediates/forge` 等独立目录，避免 CI 单制品选择将它认作最终 mod；该中间包必须确实不含任何 core class。保留 root 的 `implementation project(':core')` 和编译依赖。通过 lazy provider/closure 读取 ZIP，不能在配置期读取尚未生成的包或提前读取 reobf 前字节。
2. distributionJar 显式依赖 reobfJar 和 core classes；assemble/build 指向最终包。不要使 jar 或 reobfJar 反向依赖 distributionJar，也不要将 distributionJar 注册到 reobf 容器，否则形成循环或再次改写 core。保留一种明确的依赖链，不用多个相互依赖的 finalizer 拼接排序。
3. 从重映射包复制所有实际平台内容；复制时排除 `META-INF/MANIFEST.MF`，由最终 Jar 合并/生成一份完整 manifest。现有 manifest 除版本、作者等字段还有 `MixinConfigs: gregtech.mixins.json`，不能只保留新手写的几个字段。`gregtech.mixins.json`、`gregtech.refmap.json`、descriptor、pack.mcmeta 和全部实际资源必须保留。80,435 条目的最终包继续 Zip64、duplicates FAIL、稳定时间及顺序。
4. local Maven publication 当前 `artifact jar` 必须改为最终 distributionJar；任何 outgoing artifact/测试选择也要明确最终包。不能出现 build 门验的是最终包，而 publication 仍取未嵌入 core 的 intermediate。授权未决门继续保持，修改发布任务指向不等于现在执行发布。
5. 构建后按严格工具验证每个 current core 条目恰好一次且 SHA 相符、两腿互等、descriptor/test 污染门；另核实最终 manifest/mixin/refmap 和平台资源未丢失。dev GameTest 使用源集输出，成功不能证明最终 production jar 的重组/重映射/加载正确；最终包在生产客户端和专服的加载仍必须独立验证。

此节提出可核查的修复路径，尚未声称该路径已经配置、构建或加载通过。主任务报告 Neo 三项 bootstrap 已实际通过，正式运行账本由主任务另记；本审查未借此宣告生产专服、客户端、玩法或存档验收通过。

### MixinGradle 对 distributionJar 的后续检查

主任务实施两阶段 Jar 后出现 `addMixinsToDistributionJar`。这不是新 reobf 注册：MixinGradle 为每个 Jar 建贡献任务，实际只向 reobf input/producer 中与 remappedJar 相同的目标加入 refmap 与 manifest；其官方源码可见 [MixinExtension.groovy](https://raw.githubusercontent.com/SpongePowered/MixinGradle/master/src/main/groovy/org/spongepowered/asm/gradle/plugins/MixinExtension.groovy) 的 AddMixinsToJarTask 和 configure 部分。

另用 javap 读取本机固定 0.7.38 的闭包字节码，核实相同的 `dependsOn`/`ValueSupplier`/`visitProducerTasks` 与 `compareEqual` 过滤（本机 LineNumberTable 对应 groovy:186-204、1025-1034；远程 master 的行号/文本不是该制品的精确版本证据）。ForgeGradle 本机 sources:283-289 只自动注册 jar 和 JarJar 的 reobf。当前 distributionJar 不在 reobf input producer 中，贡献任务应无实际附加内容。

最小边界是保留 duplicates FAIL、从中间包复制单份 refmap/mixin 文件与原 manifest，并检查实际最终 ZIP。无需关闭整个 Mixin 插件或排除 refmap来掩盖重复。若贡献出现，应先核实 reobf input/依赖是否错误指向 distributionJar。以上是源码/字节码判断；正在进行的最终构建结果由主任务记录。
