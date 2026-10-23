# Logistics and component crafting parity / 2026-10-02

基于 4c58c6f，185 条原始 JSON 原字节迁至 core：components 84、logistics 48、control_covers 12、hand_covers 6、panel_covers 12、recipe_keys 3、machines/multiblock 15、pipes 5。逐文件 SHA、可获得的来源哈希与保留基线见 core/provenance/routing-crafting-parity-20261002.json。

复用既有 EquipmentCraftingCatalog 与 Neo 原生数据包，不新增材料/设备/物流注册或底层；原配方图样、工具、材料、数量、镜像和重置语义保留。共有 419 条设备配方与原 38 条便携配方，数据包 457 条。

## 集中检查与限制

首次双版/检查源码编译在 17 秒失败，原因是新检查代码 var 多变量声明；拆为两个声明。修正后的下一次普通 Neo 专服构建启动同时覆盖 :compileJava、Native 生产代码和 bootstrap 编译。

实际 RecipeManager 查到全部 419 条设备配方、非空产物和每项非空原料的候选。选定实际 CraftingInput/assemble/getRemainingItems：橡木板+锯+软锤制作中木管（工具损耗各 100）；铝螺丝/板+锤/螺丝刀制作空覆盖板（400/100 损耗），锌箔+所得覆盖板制作过滤器覆盖物，再用该覆盖物/琥珀金管/机壳/工具制作物品过滤设备。重置配方返回无旧自定义名称的全新产物且不修改输入；空板序列化器网络往返保持匹配。实际镀锌钢杆/磁铁杆/铜线/锡缆电机配方和橡胶板传送带配方通过。

以上为真实配方匹配/装配/余物查询，没有玩家界面点击、全部逐件制作，也未在本批执行覆盖物安装或物流路由。普通 Neo DedicatedServer 在隔离世界运行 200 tick 后正常保存退出；本批未新增世界重启或客户端检查。日志另有 Mojang 公钥读取网络超时，离线开发服务器仍完成启动、上述明确成功回执及正常保存退出，不隐去该网络日志。完整生存、普通活塞引擎制作入口、Forge 对应运行、成品安装与旧存档仍未验。来源目录保持只读，不更改第三方授权，整体移植仍未完成。

## 最终产物

普通专服构建/启动 1m 54s；最终双 JAR 构建 3m 43s。573 个当前共享 class 字节一致、平台关键类与元数据正确、无重复/测试污染；本批全部 185 资源在两 JAR 中 SHA 与原件一致。

- `gregtech6-1.20.1-forge-0.0.0.jar`：38,535,701 字节；SHA-256 `4d69c3b9eb168bdd2462f0a2f881fdb2fed8433441bfe601cfa7feceed5897a2`。
- `gregtech6-neoforge-1.21.1-0.0.0.jar`：36,086,611 字节；SHA-256 `5f56179821ede2b20363dd8bff73158bbf69ffd905b62f6ce7466b79fc6dd470`。
