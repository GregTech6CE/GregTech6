# 双版本构建与 Git 上传说明

更新：2026-10-02。当前为整合开发版，目标为 Minecraft 1.20.1 Forge 和 Minecraft 1.21.1 NeoForge。源码对齐、编译、游戏启动、完整玩法及存档兼容是分别记录的状态，详见 `docs/integration/STATUS.md` 和 `VERIFICATION.md`。

## 项目结构：一个仓库，两个平台产物

目前不是两个完全独立的项目。`settings.gradle` 同时包含 `core` 和 `neoforge`，根项目本身是 Forge 平台。

```text
gregtech6/
├── src/                  Forge 1.20.1 平台代码和平台资源
├── core/                 两平台共享的材料、配方、能源、物流、热量等逻辑与公共资源
├── neoforge/             NeoForge 1.21.1 平台代码和平台资源
├── gradle/               Gradle wrapper
├── scripts/build.ps1     Windows 完整构建入口
├── .github/workflows/    两平台构建 CI
└── docs/integration/     来源、取舍、验证及缺口记录
```

Forge 和 NeoForge 的注册、网络、能力、物品组件及客户端 API 在各自平台实现，但消费同一份 `core`。两个最终模组 JAR 都嵌入共享核心，玩家不需要单独安装 core JAR。将当前平台目录直接拆成两个仓库，会丢失根构建配置和共享核心依赖；因此本次整理保持单仓库，分别构建、发布两个产物。

构建不需要本机 `F:\Dev\GregtTech6New\Libs` 中的三个来源目录。它们是审计参考；当前采用的代码、资源、原始许可与来源记录已经在整合仓库中。原来源目录保持原样。

## 模组名称与 ID

两版显示名称均为 `GregTech 6 Integrated`，加载 ID 均为 `gregtech6`，元数据读取根目录 `gradle.properties`。两平台入口和事件订阅使用共享的 `GregTechIdentity.MOD_ID`。

现有物品、方块、配方、模型、翻译和网络通道仍使用 `gregtech:` 内部命名空间。这与加载 ID 是不同用途；本次不批量改动注册身份和保存数据。配置文件默认名称可能随加载 ID 改变，原有自定义配置需手动核对迁移。旧世界和跨版本升级兼容仍未验证。

## 环境准备

- Gradle wrapper 8.8 已随源码提供，无需另装 Gradle。
- 安装完整 JDK 17 和 JDK 21，需包含 `bin/java` 与 `bin/javac`。
- Gradle 启动使用 Java 17；Forge 与 core 目标为 Java 17，NeoForge 使用 Java 21 toolchain。
- 固定平台依赖为 Forge 47.4.20、NeoForge 21.1.243，首次构建需要联网下载依赖。

Windows PowerShell 在仓库根目录设置：

```powershell
$env:JAVA17_HOME = 'C:\Program Files\Java\jdk-17.0.4'
$env:JAVA21_HOME = 'C:\Users\Asus\AppData\Roaming\.minecraft\runtime\java-runtime-delta'
$env:JAVA_HOME = $env:JAVA17_HOME
```

以上是当前开发机路径，其他电脑改为自己的 JDK 安装目录。环境变量指向 JDK 根目录，不要指向 `bin`。用户提供的 Java 21 `bin` 路径对应上面的根目录。

## 快速构建模组文件

以下任务编译并打包，不主动运行完整测试或启动游戏。两个平台可以单独构建；所有命令都从仓库根目录执行，不要同时运行多次 Gradle。

Forge 1.20.1：

```powershell
.\gradlew.bat --console=plain :distributionJar
```

当前输出：`build/libs/gregtech6-1.20.1-forge-0.1.0-integration.1.jar`。

NeoForge 1.21.1：

```powershell
.\gradlew.bat --console=plain :neoforge:jar
```

当前输出：`neoforge/build/libs/gregtech6-neoforge-1.21.1-0.1.0-integration.1.jar`。

一次生成两个版本：

```powershell
.\gradlew.bat --console=plain :distributionJar :neoforge:jar
```

Forge 应使用 `distributionJar` 的最终产物，它包含重混淆后的平台代码和未改写的 core；`build/forge-intermediates/` 内是平台中间包。NeoForge 最终 JAR 位于自己的 `build/libs/`。文件版本号取自 `gradle.properties` 的 `mod_version`。

只编译源码、暂不打包：

```powershell
.\gradlew.bat --console=plain :compileJava :neoforge:compileJava
```

## 完整构建与开发运行

需要执行共享核心行为合约时，使用已有脚本：

```powershell
.\scripts\build.ps1 -Target forge
.\scripts\build.ps1 -Target neoforge
# 或按顺序构建两平台：
.\scripts\build.ps1 -Target all
```

脚本会执行 `:core:check` 和相应平台的 `:build` / `:neoforge:build`，日志写入忽略的 `work/`。仅检查 JDK 可加 `-CheckEnvironmentOnly`。

客户端开发启动：

```powershell
.\gradlew.bat :runClient
.\gradlew.bat :neoforge:runClient
```

上述两个命令分别运行对应客户端，按需选一个。专用服务器对应 `:runServer` 和 `:neoforge:runServer`，首次需按 Minecraft 的要求处理服务器 EULA；开发启动不会自动证明成品 JAR、玩法或世界重载通过。

Linux/macOS 先设置 `JAVA17_HOME`、`JAVA21_HOME` 和 `JAVA_HOME="$JAVA17_HOME"`，再执行：

```sh
chmod +x gradlew
./gradlew --console=plain :distributionJar :neoforge:jar
# 包含共享行为合约的完整构建：
./gradlew --console=plain :core:check :build
./gradlew --console=plain :core:check :neoforge:build
```

不要安装两个平台的 JAR 到同一游戏实例；玩家只安装自己 Minecraft/加载器版本对应的一个包。JEI/Jade 适配已包含，依赖本身不捆绑；版本见 `gradle.properties`。

## 源码包和 Git 历史

本次交付包含：

- `gregtech6-source.zip`：当前已提交的源码、公共资源、构建脚本、CI、文档与署名；解压后顶层目录为 `gregtech6`。
- `gregtech6-history.bundle`：完整本地 Git refs 和可达提交，包括整合历史及可获得的来源历史，不包含本机 Git 配置、凭据或 hooks。
- 单独的本 MD、打包回执与校验和。

源码 ZIP 不包含 `.git`、Gradle 缓存、构建输出、测试世界或忽略的 `work/`。若要保留历史，优先从 bundle 恢复项目，而不是解压 ZIP 后重新 `git init`：

```powershell
git clone --branch main .\gregtech6-history.bundle .\gregtech6
cd .\gregtech6
# 可选：恢复 bundle 中保留的原来源 remote-tracking refs
git fetch ..\gregtech6-history.bundle '+refs/remotes/*:refs/remotes/*'
```

clone 会恢复主分支和可达标签；bundle 本身保留全部导出的 refs，可用 `git bundle list-heads ..\gregtech6-history.bundle` 查看。原始作者与已有来源标签保持不变，saltnya 仍是导入快照而非伪造的原作者历史。

在你自己创建的新空仓库中上传主分支，将示例 URL 改成实际仓库：

```powershell
git remote set-url origin https://github.com/YOUR_ACCOUNT/YOUR_REPOSITORY.git
git push -u origin main
```

源码包中没有预设你的 GitHub 归属；恢复后的 origin 初始指向本地 bundle，先修改再推送。不需要使用 `--mirror` 将全部上游 refs 推到新项目，也不要把 `upstream-*` 当成新项目发布地址。历史 bundle 保留在本地备份即可。

当前来源授权未决项保留在 `docs/integration/LICENSE_REVIEW.md`，未自动更换许可或开放公共发布。上传准备包不代表这项核查已完成。CI 含双平台构建矩阵；现有配置对公开仓库禁用模组二进制上传，不自动发布 Release。
