# NeoForge 成品 JAR 独立专服启动与存档重载 — 2026-10-02

生产包构建提交：`bc89c720121a52dec61daf6110a0dcb87486cae9`。模组 GregTech 6 Community Edition 0.0.0，ID `gregtech6`；NeoForge 21.1.243 / Minecraft 1.21.1。

本批只检查现有成品包，不改生产源码，不重新构建。安装目录为忽略的 `work/installed-jars-neoforge-20261002-bc89c7201`，mods 中只有交付的 `gregtech6-neoforge-1.21.1-0.0.0.jar`，SHA-256 `dbccee862a5de45a0eba63796c4fa2f6502f8d039215a11eb6bf456605c94e33`。使用指定的 Microsoft Java21，运行官方安装器生成的 `@user_jvm_args.txt @libraries/net/neoforged/neoforge/21.1.243/win_args.txt nogui`，没有 Gradle、开发 classpath、bootstrap 测试模组或 SDK。

## 实际结果

- 官方安装器在线下载出现 TLS/读超时，发生在模组加载之前。用校验散列的缓存和下载补齐依赖、原版服务端，再使用原版官方安装器 `--offline --installServer`，退出码 0；未跳过散列校验或修改安装器。
- 新独立世界进程 PID 81208，71.9 秒，正常加载 `gregtech6` 和 API 1 空 Addon 列表；控制台放置青铜粉碎机和箱中的 3 个铜原矿，读取实际方块实体数据、`save-all flush`、`stop`，退出 0。
- 第二个独立 JVM PID 28688，73.5 秒，打开同一世界；只读取现存库存和粉碎机，未再次放置或注入库存。箱 NBT 与上次一致，机器保存身份为 `gregtech:be_crusher_bronze`，实际 `gt.inventory` 存在。再次保存并正常退出 0。
- 两次日志均有世界保存完成，无 ERROR 项；延期初始化约 16 秒、union 资源路径和隔离 offline 认证警告已记录，不据此宣称无任何警告。

## 范围

通过的是 **NeoForge 成品 JAR 专服加载和所选库存/方块的保存重载**。控制台给定设备和原料不代表生存取得、配方制作或动力加工。机器 tick 状态自然变化，未声明全部 NBT 精确相同。没有安装外部 Addon，仍需第三方 Addon 运行验证。

Forge 成品专服、两版成品客户端、自然世界生成、完整铜锡到青铜工具连续玩法、全部系统与旧/跨版本存档迁移仍待推进。原开发服务器玩法证据另见此前各批回执，不移作本次成品世界证据。整体移植仍未完成。

详细回执：[installed-neoforge-20261002.json](installed-neoforge-20261002.json)。运行日志保留在忽略的 work 目录，回执包含散列和实际命令。
