# 材料物品 ID 先核心后 addon — 2026-10-14

`MaterialItemDefinitions.all()` 先给 `GTMaterialRegistry.init()` 完成时的核心材料分配物品 ID，再给之后注册的材料分配。addon 材料如果要拿走已经分配的基础 ID，直接抛 `IllegalStateException`，不再改成 `基础ID_材料数字ID`。核心材料之间原来的后缀回退还在。

核心集合取在 `link()` 之后。`MaterialGroups.init()` 在 link 阶段也会 `createMaterial`，这些分组材料属于内置目录。后面的材料入口如果插在 `declare()` 和 `link()` 之间，不能把这批材料当成 addon。

改动前用当时的类导出了 `work/material-item-id-golden-20261014.txt`：56085 行，带数字后缀的 0 行。SHA-256 为 `c3360a6cdc2ac4aa396b7a9bb778f11663171afb4e949709c65f3903f25a5fef`。行格式是物品 ID、基础 ID、形态名、材料名、材料数字 ID。

`:core:materialBehaviorContracts` 通过，7959 条断言。其中核对了这份指纹；注册只带 `DUST` 的 `Aaaaddon` 后，核心指纹不变，它自己的物品没有后缀；再注册名字为 `iron` 的材料时，`all()` 拒绝抢走 `dust_iron`。随后 `:core:check` 通过：源码门禁、四组行为契约（材料契约复用上次结果）和 `:core:test`。

第一次编译两个平台没有成功，原因是网络，不是这批代码。Forge `:compileJava` 连 `repo.sleeping.town` 超时，JEI、EMI、Jade 的映射依赖没有解析到。NeoForge `:neoforge:compileJava` 连 `maven.neoforged.net` 超时。当时的离线重试也报没有缓存。

缓存其实在。`modules-2` 里已有 EMI 和 `neoform-runtime`，`work/local-maven` 里已有 EMI 的 POM 和 API。离线模式认不出 `modules-2` 里的那份，因为当前仓库的解析记录对不上，Gradle 仍要去 HEAD `maven.terraformersmc.com`。本机 Gradle 用户目录设了 `127.0.0.1:7890` 代理，没有写进仓库。Java 直接请求能通过这个代理拿到 EMI POM；Gradle 自己的客户端对同一地址的 HEAD 仍然握手失败。

随后用已有的 `work/local-maven.init.gradle` 把 EMI 指到 `work/local-maven`。Gradle 8.8 在 Java 25 上重编构建脚本会遇到 class file 69，所以这次启动器用的是 Java 21。Forge `:compileJava` 在 22:28 全量通过，1502 个 class。NeoForge `:neoforge:compileJava` 在 22:43 增量通过，1605 个 class 里刷新了 198 个。紧接着再跑这两个任务是 UP-TO-DATE，`BUILD SUCCESSFUL`。

没有启动游戏，没有打成品包，没有做存档重载。现有目录里没有 addon 材料，所以这次本身不改变已注册物品。
