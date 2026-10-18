# Stonecutter 可行性核对

阶段一，2026-10-16。对照提交 `9767ca5bb`。只读了构建脚本、插件源码和文档，没有跑 Gradle，没有改构建。

继续。Q1、Q3、Q4 都做得到。Q2 没有成功构建的记录，按卡面规则记为不能，阶段二不省掉换构建工具。门槛里的「不能」指这条路做不到；Q2 的「不能」只表示不能把 ForgeGradle 6 放进节点当成已经成立。阶段二在独立分支上把根项目的 ForgeGradle 换成 legacyforge `2.0.142`，源码和 Stonecutter 都不动。

`DECISIONS.md` 和 `docs/work/AGENTS.md` 等这条建议被接受、阶段二开始时再写。

## Q1 Forge 构建迁到 legacyforge：能

插件 `net.neoforged.moddev.legacyforge` 和现在 NeoForge 用的 `net.neoforged.moddev` 一起发布。`LegacyForgeExtension` 继承 `ModDevExtension`，运行配置、源集和 Parchment 都在这个基类上。依据：[LEGACY.md](https://github.com/neoforged/ModDevGradle/blob/main/LEGACY.md)，[`LegacyForgeExtension.java`](https://github.com/neoforged/ModDevGradle/blob/main/src/legacy/java/net/neoforged/moddevgradle/legacyforge/dsl/LegacyForgeExtension.java)，[`ModDevExtension.java`](https://github.com/neoforged/ModDevGradle/blob/main/src/main/java/net/neoforged/moddevgradle/dsl/ModDevExtension.java)。

版本钉死 `2.0.142`，和 `neoforge/build.gradle` 里的 `net.neoforged.moddev` 相同。插件门户上已经有 `2.0.148`，两个插件要一起升，阶段二不单升一个。

| 现在的 ForgeGradle 用法 | legacyforge |
| --- | --- |
| `minecraft { runs { client, server, data, gameTestServer } }`，系统属性、程序参数、JVM 参数、工作目录 | `legacyForge { version = "1.20.1-47.4.20"; runs { ... } }`。`client()` / `server()` / `data()` 是简写，`gameTestServer` 写 `type = "gameTestServer"`。属性换成 `systemProperty`，参数换成 `programArgument` / `jvmArgument`，目录换成 `gameDirectory`。见 [ModDevGradle 运行配置](https://docs.neoforged.net/toolchain/docs/plugins/mdg/) |
| `bootstrapGameTest`、`productionSmoke` 把 main 的编译和运行类路径加进来，每个运行都把它们收进模组源 | `addModdingDependenciesTo` 在基类上，NeoForge 子项目已经对这两个源集调用过。Forge 侧照同样的两句写：类路径追加，再 `addModdingDependenciesTo`。GameTest 继续用 main 的运行类路径，另把 `bootstrapGameTest` 加进 `mods`，保持现在「测试类跟着模组加载」的做法 |
| `fg.deobf` 18 处 | `modCompileOnly` / `modRuntimeOnly`。这些配置不传递，见下 |
| MixinGradle `mixin { add; config }`，开发期靠插件发现，成品 manifest 带 `MixinConfigs` | 同名 `mixin {}` 扩展还在。开发运行由插件自己加 `--mixin.config`，并设置 `mixin.env.remapRefMap` 指向它生成的 `intermediateToNamed.srg`，不再用 `build/createSrgToMcp/output.srg`。见 [`LegacyForgeModDevPlugin.enable`](https://github.com/neoforged/ModDevGradle/blob/e6008d5b/src/legacy/java/net/neoforged/moddevgradle/legacyforge/internal/LegacyForgeModDevPlugin.java) |
| `compileJava` 关闭增量，并把 `gregtech.refmap.json` 声明成输出 | 这是 `build.gradle` 自己的 `JavaCompile` 配置，照搬 |
| `jar` → `reobfJar` → `distributionJar`；core 在 reobf 之后并入 | `jar` 会自动 reobf。未混淆的 jar 改放到 `build/devlibs`，发布用混淆后的那份。`distributionJar` 读 reobf 任务的输出，再并入 `:core:classes`，并且不要把 `distributionJar` 交给 `reobfuscate` |
| `productionSmokeJar` 单独 reobf，并把 main 的 class 当重命名库 | `obfuscation.reobfuscate(productionSmokeJar, sourceSets.productionSmoke)`。这个源集的编译类路径已经含 main 的输出，继承方法的重命名库就在里面。探针继续排除 `gregtech.refmap.json`，并去掉 manifest 里的 `MixinConfigs` |
| `gradle/incremental-resources.gradle` 换掉 `processResources` | 插件源码没有替换这个任务。增量任务继续挂在 Java 插件的 `processResources` 上。`copyIdeResources` 是 ForgeGradle 的 IDEA 挂钩，legacyforge 没有同名开关 |
| `ensureRunClasspaths`、`prepareRun*Compile` | ForgeGradle 才写 `build/classpath/*_minecraftClasspath.txt`。legacyforge 走自己的 IDE 同步（`IdeIntegration`、`ideSyncTask`）。阶段二先不搬这个补丁，IDE 启动失败再按新的运行模型补 |
| `zip64`、UTF-8、`sourcePolicyCheck`、`evaluationDependsOn(':core')`、`implementation project(':core')` | 普通 Gradle，照留 |

映射通道现在是 `official`（`gradle.properties`）。legacyforge 默认就是 Mojang 官方名，阶段二不用 Parchment。

### 传递依赖：要手写

[LEGACY.md](https://github.com/neoforged/ModDevGradle/blob/main/LEGACY.md) 写明 `modImplementation`、`modRuntimeOnly`、`modCompileOnly` 不传递。`createRemappingConfiguration` 对项目依赖还会 `setTransitive(false)`。JEI、EMI、Jade 和 `-PcompatRuntime` 下的兼容模组，传递进来的模组和库都要逐条写成 `modCompileOnly` 或 `modRuntimeOnly`。这些依赖现在就不进成品 jar，漏掉只影响开发期运行。阶段二先记下当前 `fg.deobf` 解析出来的运行时类路径，换完再对一次差集。

### mixin 和 refmap：能保留

Forge 47.4.20 的 `mods.toml` 仍然没有 `[[mixins]]`。开发期发现改由插件写进运行参数，生产期要在 `jar` 的 manifest 里自己写 `MixinConfigs: gregtech.mixins.json`。现在这行是 MixinGradle 晚加的，`distributionJar` 再把整份 manifest 抄进来。换成显式属性之后，这段抄录可以留着。

refmap 仍由 Mixin 注解处理器写 `gregtech.refmap.json`。`mixin { add sourceSets.main, 'gregtech.refmap.json' }` 把这份 refmap 收进 jar，并交给 reobf。主编译关闭增量、把 refmap 声明成任务输出，这两句继续有效。`verify_artifacts.py` 现在按字节比较 refmap；阶段二改用下面 Q7 的解析比较，因为键顺序可能变。

### reobf 和 core

自动 reobf 只挂在 `jar` 上，`assemble` 依赖它。core 继续不要放进这个 jar。`implementation project(':core')` 不会把 core 打进 jar。插件会给所有 jar 工件标上 SRG 属性，用来强制重映射模组依赖；阶段二的门槛就是成品里的 core class 和 `:core:classes` 逐字节相同。对不上就给 core 加上 named 约束，或者改成直接引用 class 输出。`jarJar` 才会把项目依赖打进去并按 SRG 重映射，core 不要走 `jarJar`。

### 增量资源：能照用

`IncrementalProcessResources` 只替换 Java 插件的 `processResources`，输出目录仍是 `build/resources/main`。`platform-shared.gradle` 上的重复资源检查和 NeoForge 的 `forge:composite` 替换挂在同一个任务上，阶段二根项目还是 Forge，`project == rootProject` 这些判断不用动。IDE 是否还去读 `rootSpec`，阶段二用一次客户端启动确认；Gradle 运行读的是任务输出。

## Q2 ForgeGradle 6 放进 Stonecutter 节点：不能

不能把这件事当成已经能构建。接线本身有人写过，成功打包的记录没有。

[RedstoneElectronics `fc58fc96`](https://github.com/Codetoil/RedstoneElectronics/commit/fc58fc96ddafcae5c94914eac483fec85200356a) 在根脚本 `stonecutter.gradle.kts` 里 `apply false` 声明 `net.minecraftforge.gradle` `[6.0.46,6.2)`、MixinGradle 和 Parchment，Forge 分支脚本 `minecraftforge/build.gradle.kts` 再应用。那个脚本的 `minecraft.runs` 有 client、server、gameTestServer、data，和现在的根脚本是同一套。Stonecutter 是 `0.7.6`。能打开的一次 CI 是失败的：[build #141](https://github.com/Codetoil/RedstoneElectronics/actions/runs/19872434150)（2025-12-02，`stonecutter-dev`）在 `mergeMappings` / `extra[client]` 失败。这次失败不能证明插件拒绝在子项目里工作，也不能当成成功。

[easybot-mod 的 `stonecutter.gradle.kts`](https://github.com/easybot-team/easybot-mod/blob/develop/stonecutter.gradle.kts) 同样在根脚本里 `apply false` 声明了 ForgeGradle `6.0.46`、ModDevGradle `2.0.141` 和 Fabric Loom。README 只说明用 “Set active project” 和 `buildAndCollect`。没有看到 Forge 节点打出 jar 的记录。

因此阶段二不跳过。ForgeGradle 留在节点里仍然可以以后再试，不作为阶段二的方案。

## Q3 两个插件会不会冲突：能，版本必须相同

同一个构建里可以一个节点用 `net.neoforged.moddev`，另一个用 `net.neoforged.moddev.legacyforge`。每个节点只 apply 自己的那一个 ID。`LegacyForgeModDevPlugin.apply` 没有再 apply `net.neoforged.moddev`，它自己 apply NeoForm、Minecraft 依赖和 JarJar。

版本用同一个号。legacyforge 是同版本一起发布的附加插件。[rotgruengelb/stonecutter-mod-template](https://github.com/rotgruengelb/stonecutter-mod-template) 的 `gradle/libs.versions.toml` 把两个插件 ID 指到同一个 `neoforge-moddev` 版本（模板当时是 `2.0.147`），`settings.gradle.kts` 用 `build.forge.gradle.kts` 和 `build.neoforge.gradle.kts` 分开。Stonecutter 官方多加载器说明把这种拆开的构建脚本当作推荐做法：[Multiloader Techniques](https://stonecutter.kikugie.dev/wiki/v2/guides/tips/multiloader)。

阶段二就会遇到这个组合：根项目换成 legacyforge，`:neoforge` 仍是 `net.neoforged.moddev` `2.0.142`。legacyforge 也用 `2.0.142`。

## Q4 不切换当前版本能否构建另一个节点：能

Stonecutter 0.7 起，每个节点直接跑自己的 `build`。当前版本的源码连着共享 `src/`，其他版本用生成源码，生成结果在 `versions/*/build/generated/stonecutter/`，只读。切换任务会就地改写 `src/`。依据：[0.7 说明](https://stonecutter.kikugie.dev/blog/wall/0.7-update)，[控制器](https://stonecutter.kikugie.dev/dokka/stonecutter/dev.kikugie.stonecutter.controller/-stonecutter-controller-extension/active.html)，[版本管理](https://stonecutter.kikugie.dev/wiki/v2/reference/ij-plugin/version-management)。

`runClient`、`runServer`、`runGameTestServer` 都是该节点上的普通任务，和 `build` 一样不必先切换。官方仍建议玩游戏时切到那个版本：另一个节点跑的是生成副本，改生成文件不会写回 `src/`。调试器对上的是生成文件，对不上编辑器里正在看的那份活代码。

多个 agent 可以共用一个检出。仓库停在同一个 VCS 版本，谁都不执行切换。另一个版本的代码留在条件注释里，由那个节点的任务从 `src/` 重新生成再编译。一次只跑一个 Gradle，这是仓库现有约定，Q4 不放开它。

`active` 设成 `null` 是 detached：不连接 `src/`，所有版本都从生成源码构建，给 CI 用。日常开发保留一个当前版本，IDE 里才有一份真正参与编译的代码。

提交前如果有人切走过版本，用 IDE 插件的 “Reset active project” 回到 VCS 版本再提交。插件默认会在当前版本和 VCS 版本不一致时警告。

## Q5 编辑体验和条件怎么写

没有 IDE 插件时，非当前版本的代码就是注释。补全、检查、查找引用都只看见当前版本。全局优化导入会把「看起来没人用」的导入删掉，另一边的构建才会爆。

装了 Stonecutter 的 IntelliJ 插件之后，条件注释有高亮、语法检查、常量和版本谓词的补全、用条件包住选区，以及和生成源码对比。优化导入会跳过条件、替换规则保护着的导入。[代码编辑](https://stonecutter.kikugie.dev/wiki/v2/reference/ij-plugin/code-editing)。插件不把注释里的 Java 再编译一遍。另一边的类型错误要等那个节点的 `compileJava`。

阶段四开始改共享源码之前先装这个插件。

写法分两层，即使现在 Forge 正好是 1.20.1、NeoForge 正好是 1.21.1：

- 加载器差异用常量。控制器里 `constants.match(加载器名, "forge", "neoforge")`，源码写 `//? if forge`。模板就是这样做的，见 rotgruengelb 的 `stonecutter.gradle.kts`。
- Minecraft 接口差异用版本谓词，例如 `//? if >=1.21`。谓词比较的是节点在 `settings.gradle` 里登记的游戏版本。[条件语法](https://stonecutter.kikugie.dev/wiki/v2/reference/syntax/conditions)。

整行都能对上的重命名可以用替换，例如 `RegistryObject` 对 `DeferredHolder`、`super.load(tag)` 对 `super.loadAdditional(tag, lookup)`。替换写在构建脚本的 `replacements.string` 里，生成结果不留下注释。只在个别文件出现的，用文件里的 `//~` 局部替换。替换要求记号唯一，撞上别的用途就会改错。

参数列表对不上的覆盖（`use` 对 `useItemOn`，多一个 `lookup` 的存档方法）用 `//? if` / `elif` / `else` 的花括号块。只在一边存在的方法，整段放进对应常量的块。

JSON 模型不能塞 `//?`。条件语法覆盖 Java 和 JSON5，Minecraft 的模型、方块状态是严格 JSON，而当前版本的源文件会把条件行留在文件里。模型里已经在用的 `forge:composite` 继续交给 `platform-shared.gradle` 的字节替换。加载器描述文件（`mods.toml`、`neoforge.mods.toml`）保持各节点一份。

## Q6 core 和版本树：能

`core` 继续是 `settings.gradle` 里的普通 `include`，不放进 `stonecutter.create`。Stonecutter 只注册 `create` 里列出的节点；`core/build.gradle` 仍是纯 Java 17。两个节点用 `implementation project(':core')`，打成品时在 reobf 之后并入同一份 class 输出，和现在一样。

没有找到「Stonecutter 树旁边再挂一个非版本子项目」的模板，所以这是阶段三配置时的第一项检查，不是阶段二的事。阶段二根项目仍是 Forge，`:core` 的关系不变。

`platform-shared.gradle` 里有 `project == rootProject` 和 `project.path == ':neoforge'`。阶段三把 Forge 移出根项目时这两处要改。这也是阶段二先留在根项目上的原因。

## Q7 成品对照口径

阶段二换了 reobf 工具，class 字节可以和现在不同。对照分四层，任何一层里解释不了的差异都不算通过。

1. **core class 逐字节。** 和现在的 `verify_artifacts.py` 一样，成品里的 core class 对 `:core:classes` 做 SHA-256。这一层不允许差异。core 被重映射会在这里失败。
2. **平台 class 比结构。** 解析 class，比较类内部名、父类、接口、字段的名字和描述符、方法的名字、描述符、访问标志和异常。不比较常量池顺序、StackMap、行号表、局部变量表。然后再比较去掉这些调试属性、并把常量池下标换成名字之后的指令序列。指令序列不同必须逐条解释（重命名工具对继承方法的处理、帧的写法）。解释不了的，停止。
3. **refmap 解析后比较。** `gregtech.refmap.json` 的 `mappings` 和 `data.searge` 按映射比较，不按文件字节。键顺序无关。现有那条掉落物映射（`EntityFallingOreDropMixin` 的 `m_19998_`）继续作为必中的一条。manifest 里要有 `MixinConfigs: gregtech.mixins.json`。
4. **资源逐字节。** 语言、模型和方块状态对当前源码或当前 NeoForge 编译输出逐字节比较。`mods.toml` 只允许属性展开造成的差异。清单里允许多出来的 `MixinConfigs`，其他条目集合要能对上。

`tools/integration/compare_artifact_layout.py` 已经会去掉行号表和局部变量表再哈希。那是阶段四「只差行号」用的口径。阶段二的平台 class 用上面的结构加指令序列，因为两套 reobf 的字节不必相同。core 仍走第 1 层。

阶段二尽量让 `distributionJar` 的输出路径和文件名保持 `build/libs` 里现在的成品，这样 `verify_artifacts.py` 的元数据、许可证、测试类排除和 core 字节检查还能用。它现在从 `build/forge-intermediates` 读 Forge 的重映射 class；legacyforge 把未混淆 jar 放到 `build/devlibs`。阶段二要改这一处输入，改完仍跑这个脚本。

## Q8 工作量

同一提交 `9767ca5bb` 重数。`tools/` 和 `scripts/` 下 386 个 `.py` / `.ps1`。卡上的 233 是写死 `src/main` 的个数；这次去掉 `core/src/main`、`neoforge/src/main`、`platform-shared/src/main` 之后，仍指向 Forge 树 `src/main` 的有 236 个，对不上的 3 个没有再拆。另外 13 个只指向 `core/src/main`，1 个只指向 `neoforge/src`，16 个只指向 `platform-shared/src`。大量命中是原版检出或临时目录里的 `src/main/java/gregapi/...`，搬我们的平台树碰不到它们。

现在还会跑、而且路径会跟着平台目录变的，是这 6 个：

| 脚本 | 谁在调用 | 写死的路径 |
| --- | --- | --- |
| `tools/integration/check_source.py` | 根项目 `check`，CI 的源码测试会导入它 | `src/main`、`platform-shared/src/main`、`neoforge/src/main`，以及生成目录 |
| `tools/integration/verify_artifacts.py` | CI 验包 | 两边的 `bootstrapGameTest`、`productionSmoke` |
| `tools/tests/test_localization_coverage.py` | CI | `src/main/java`、`neoforge/src/main/java` |
| `tools/tests/test_jade_translations.py` | CI | `src/main/java/.../integration/jade` |
| `tools/tests/test_source_policy.py` | CI | 把这些前缀写成边界断言 |
| `tools/integration/import_numbered_localization.py` | 语言维护入口，不在 CI | 扫两边的 `src/main/java` |

`tools/localization.py` 和 `test_standard_chinese.py` 只指向 `core/src/main`，core 不动就不用改。`build_verified.ps1`、`compare_artifact_layout.py`、`publish_verified_artifacts.py`、`verify_addon_api.py` 没有平台 `src/main`。其余写死路径的脚本是一次性移植或资源生成，没有构建入口；阶段五只改上面 6 个，加上到时候仍要重跑的那些。16 个只指向 `platform-shared` 的资源脚本，等共享目录真正折进版本树再改。

合并脚本要处理的形状，这次用 `SequenceMatcher` 在去掉 import 行之后重数。main 下同名 Java 675 个：2 个已经相同，32 个只差 import，641 个正文不同。去掉 import 后的正文里，相同行 68781，变化行两边相加 15932，opcode 块 4276。卡上的 6.36 万 / 1.36 万 / 3083 是另一种 diff，数量级一致。bootstrap 同名 81 个，其中 76 个正文不同；productionSmoke 同名 19 个，全部正文不同。

脚本要覆盖的情况：

- **import。** 32 个 main 文件只差 import，另有正文也不同的文件带着自己的 import 差。共同的 import 留在外面，只属于一边的 import 放进加载器条件。IDE 插件会把这种 import 留在列表底部。
- **跨行调用。** 162 个 main 文件的差异块里有以 `,`、`(` 或 `.` 结尾的续行。条件用花括号包住整段调用，不能按物理行各写一条。嵌套块在被注释时会把 `/* */` 换成 `/^ ^/`，生成器要输出当前 VCS 版本的注释状态。
- **只在一边的方法。** 223 个 main 文件有方法名只出现在一边。例子：`GTFacingMachineBlock` 的 `use` 对 `useItemOn`，`PumpBlockEntity` 的 `load` 对 `loadAdditional`。整段方法放进条件。不要给另一边补一个空方法。
- **整行替换。** 重复出现、整行能对上的，优先替换而不是条件。计数最高的是 `super.load(tag)` 对 `super.loadAdditional(tag, lookup)`（52）、`GregTech.NAMESPACE` 对 `"gregtech"`（51）、`super.saveAdditional(tag)` 对带 `lookup` 的那行（47）、`RegistryObject` 对 `DeferredHolder`（25 以上，按类型拆开）。替换前要确认记号不会打到别的行。
- **只在一边的文件。** main Java 有 Forge 72 个、NeoForge 140 个。bootstrap 还有 Forge 248 个、NeoForge 16 个。已有清单是 486 个，含资源和其他源集，见 [只在一边的源码](platform-one-sided-sources.md)。这些文件整份放进条件，或者留在该节点自己的源目录。不补另一边的功能。
- **差异太大的文件。** 允许留在节点专有源码里，脚本输出一份超过阈值的清单，不强迫合成一个更难读的文件。
- **资源。** 严格 JSON 不走条件注释。加载器描述文件保持各一份。

脚本要能对主线上的新提交重跑：输入仍是两份平台树，输出是一份带条件的树。阶段四期间主线还在改两边的文件。

## 阶段二的范围

独立分支。根项目继续是 Forge 构建，不引入 Stonecutter，不把源码合并，不把 Forge 移成子项目。

- 去掉 `net.minecraftforge.gradle` `6.0.54` 和 `org.spongepowered.mixin` `0.7.38`。
- 加上 `net.neoforged.moddev.legacyforge` `2.0.142`。`:neoforge` 的插件版本不动。
- 按 Q1 的表改运行、源集、依赖、mixin、reobf 和 `distributionJar`。传递依赖按解析差集补全。
- `compileJava` 的 refmap 约定、增量 `processResources`、`zip64`、`:core` 依赖留着。
- 成品按 Q7 对照。core 逐字节必须相同。
- 开发环境用 Gradle 启动客户端、专服、GameTest 服务器各一次。再从 IDE 启动一次客户端，确认不再依赖 `*_minecraftClasspath.txt`。
- 用正式安装的 Forge 加载成品 jar 一次。
- `verify_artifacts.py` 通过。需要的话只改它读取 Forge 重映射 class 的目录。

过不了就停，把原因写进本卡，卡关闭为放弃。

## 阶段三再处理的事

这些不在阶段二里做，避免和换工具叠在一起。

- 根脚本拆成 `stonecutter.gradle` 加两份节点脚本。Forge 脚本从根项目搬进节点之后，`distributionJar`、增量资源、refmap 输出路径和 Q8 那 6 个脚本的路径都要改。`platform-shared.gradle` 里 `project == rootProject` 也要改。
- 两个节点的构建脚本都加上 `tasks.named("createMinecraftArtifacts") { dependsOn("stonecutterGenerate") }`。legacyforge 和现在的 NeoForge 插件都会走 NeoForm 的这个任务，Stonecutter 0.7 的说明和模板的两份脚本都有这句。
- `include 'core'` 和 `stonecutter.create` 放在同一个 `settings.gradle`，先确认能配置通过。
- VCS 版本建议停在 `1.20.1-forge`，让编辑器里的活代码仍是现在的 Forge 树。这是建议，加哪个新版本还没定。
