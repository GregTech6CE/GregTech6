# Client world and machine GUI parity / 2026-10-02

本批实际进入 NeoForge 1.21.1 开发客户端世界，使用原专服检查存档的完整隔离副本，保留原 UUID 识别箱与停机青铜粉碎机、搅拌机。通过真实右键数据包打开两机器界面，玩家真实 shift-click 把附自定义名称的碎钻石转移到粉碎机，等待服务器处理后核对两端库存；搅拌机原三枚命名铜锭与 1000mB 命名水恢复后，检查提供另外 500mB 同 components 水，经实际 broadcastChanges / PacketSyncFluids 显示 1500mB。没有直接创建界面或客户端写入虚拟机器槽。

正常断开玩家连接，原版内置服务器保存玩家/三维度区块并停止。第二独立 JVM 读取同一世界，在任何检查补料前确认粉碎机命名输入、玩家已转出的空槽、搅拌机 1500mB 原名水，再实际打开菜单检查。两个阶段各三张真实世界/粉碎机/搅拌机截图已解码并查看；原专服存档每个文件重新核对原 SHA，完全未变。

截图发现 saltnya 原界面标题压住搅拌机顶排输入槽。两版 BasicMachineScreen 将标题绘制到面板上方，保留所有槽位、图样、纹理和菜单协议。prepare 截图保留原问题；verify 在同一存档验证修正后标题及槽位画面。液体格使用原紧凑显示，1500mB 写作 1k；精确量/components 由真实菜单和服务器断言验证，不能从该缩写推断精确量。

## 失败与修正

首轮检查读服务器过早，真实网络包尚未处理，失败是 probe 的竞态，未据此改机器库存实现。第二轮完成转移/三个画面，却在退出等待；线程与原版 PauseScreen 源码证明检查漏掉 ClientLevel.disconnect。该检查 JVM 被终止，失败保存/重启不计通过，日志与副本保留。修正实际原版调用顺序、检查错误 reason 与 world phase 字段、服务端等待和 simulationDistance 最小值后，最终 prepare/verify 均正常退出。

## 范围

复用既有 bootstrap 客户端检查，未新增独立系统夹具。平台生产修复只有两个标题绘制行，注册/保存 ID、物品/fluid components 和配方保持。测试供给设备、地面、命名输入和补水；机器停机，不声称完整生存或客户端动力制作完成。仅在隔离副本确认 experimental 元数据提示，不替接受其他提示或改变 .minecraft。当前运行是开发源集，成品安装、Forge 世界流程、全部机器/贴图与旧存档迁移待验。此前制作和专服动力链证据分开保留；完整三源 整体移植仍未完成。

## 运行与构建

- Neo prepare：JVM 66180，SUCCESS 104588ms，Gradle 2m 25s，实际退出码 0；3 张画面，正常服务器保存停止。
- Neo verify：JVM 76976，SUCCESS 98156ms，Gradle 2m 6s，实际退出码 0；3 张画面，正常服务器保存停止。
- 同一世界 `client-world-20261002-30b50c7a-feff-4e2c-aeb2-189d8551adcc`，原专服 22 个文件 SHA 未变。
- 两版集中最终构建 1m 26s；573 个共享 class 与当前 core 编译输出一致，无重复/检查污染，两最终 GUI class 核对当前 Native 编译/Forge 重映射归档。
- gregtech6-1.20.1-forge-0.0.0.jar：38,566,653 字节，SHA-256 `2b7fa38439d5f3629ee48461df89c029211af4729c0fa1605a7f62b5c1b8ad8b`。
- gregtech6-neoforge-1.21.1-0.0.0.jar：36,756,979 字节，SHA-256 `b6f38a40042c1ea04be85650eac2f037eb93c705fcf7a8ba33b64cf93d3aa7a0`。

命令：准备真实专服世界的独立完整副本（排除 session.lock），两次串行 `-PdirectCoreResources=true -PdirectCoreClasspath=true -PclientWorldSmoke=true -PclientWorldPhase=prepare|verify -PclientWorldName=<同一副本名> -PclientWorldId=<同一 UUID> -PclientSmokeTimeout=300 -PclientSmokeHeap=4g :neoforge:runClient`。正式打包不使用 directCoreClasspath：`-PdirectCoreResources=true :distributionJar :neoforge:jar`。Java17 Gradle launcher，Native 实际 Adoptium21 工具链；4g 是检查配置。
