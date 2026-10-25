# Issue #118：JEI 多方块搭建查询

日期：2026-10-25—26。问题：[任意多方块查看搭建时总会显示所有多方块](https://github.com/GregTech6CE/GregTech6/issues/118)。基线为 `origin/main` 的 `810a855b4`，修复分支为 `codex/fix-118-jei-multiblock`。

## 原因与修复

`GregTechJEIPlugin.registerRecipeCatalysts` 将每个多方块控制器注册为整个 `multiblock_assembly` 分类的催化剂。JEI 查询分类催化剂时会返回该分类的全部结构，因此控制器的用途查询会扩散到所有搭建图。

删除这组分类注册，在 `MultiblockInfoCategory.setRecipe` 中将控制器登记为该条配方的不可见 `CATALYST`。JEI 的用途快捷键查询 `INPUT` 和 `CATALYST`，现在只关联该控制器的结构；已有的控制器 `OUTPUT` 槽和搭建部件 `INPUT` 槽继续参与索引，配方快捷键及共享部件反查保持可用。若某控制器也被另一结构用作真实部件，用途查询仍应列出这两个相关结构。

本机缓存的 JEI 15.20.0.119 与 19.57.0.450 均支持相同的不可见催化剂接口；逐配方索引不会写入分类催化剂表。此修复没有引入第三方源码、资产或依赖。

## 定向回归入口

`MultiblockJeiRuntimeSmoke` 通过实际 JEI runtime 检查完整结构目录、全部控制器的 `OUTPUT` / `CATALYST` / `INPUT` / 用途双 focus 查询，以及全部唯一搭建部件的 `INPUT` 查询。另保留两个控制器单结构页和一个共享部件多结构页的截图。无关物品不应命中搭建图。

复用隔离的世界创建及配方展示 smoke，设置 `gregtech.integration.multiblockJeiRuntimeOnly=true`。该分派只执行本项 JEI 检查，世界创建入口仍保留通用物品规范化和实际配方注册表前置检查。

临时 init 内容：

```groovy
gradle.afterProject { p, state ->
    if (p.path == ':multiversion:1.21.1-neoforge') {
        p.neoForge.runs.named('client') {
            systemProperty 'gregtech.integration.multiblockJeiRuntimeOnly', 'true'
        }
    }
}
```

命令：

```powershell
.\gradlew.bat --offline --console=plain -I work/issue118-jei.init.gradle -PdirectCoreClasspath=true -PdirectCoreResources=true -PclientCreateWorldSmoke=true -PclientSmoke=false -PrecipePresentationRuntimeOnly=true -PclientSmokeTimeout=600 -PclientSmokeHeap=4g :multiversion:1.21.1-neoforge:runClient
```

首轮使用 3GB / 360 秒预算，NeoForge 主源码和 bootstrap 测试源码编译通过；客户端在资源加载阶段以 `-805306369` 退出，尚未进入世界或执行 JEI 查询，没有成功回执，不计运行通过。日志保留于 `work/issue118-jei-neoforge-first-exit.log` 和 `work/issue118-jei-neoforge-first-client.log`。现有日志没有给出异常退出原因，不将其认定为 JEI 查询失败或内存不足。

4GB / 600 秒重试期间，用户明确表示本机内存不足并要求不再尝试，已停止客户端重试和本任务启动的 Gradle daemon。没有成功回执，也未完成 JEI 查询断言或截图验收。后续不在本机自动重跑。

最终验证范围：NeoForge 主源码及 bootstrap 测试源码编译通过；客户端 JEI 行为未验证。未运行 Forge、全量 build/check/GameTest、成品包或存档重启。
