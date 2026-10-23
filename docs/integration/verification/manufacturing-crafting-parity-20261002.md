# Wire working and shaping parity / 2026-10-02

基于 a7b55da，将 1,316 条 wire_working 与 78 条 extruder_shapes 原 JSON 原字节迁至 core。来源目录只读；逐文件 SHA、可获得 donor 哈希和原保留基线见 core/provenance/manufacturing-crafting-parity-20261002.json。所有配方不新增条件、替代材料或重命名注册身份；Forge 原 recipes 路径及既有 Neo 原生 recipe 数据包共用目录。共有 1,813 条共享设备/制造配方，加原 38 条便携配方，数据包共 1,851 条。

## 集中运行检查

一次 :compileJava + :neoforge:runServer 同时编译双版生产代码及既有 bootstrap。普通 Neo DedicatedServer 全部 1,813 条设备/制造配方实际 RecipeManager 存在、产物非空、每项非空原料有候选。1,316 条实际 ShapelessRecipe 用真实原料候选执行 matches/assemble；输入输出 WireMaterialLike/WireSpec 比较导体原材质、家族和 materialAmount × count，拆分/合股/绝缘均守恒。绝缘只核对导体，不把橡胶消耗或材料全组成算成全面守恒，也未验证通电与触电。

实际制作查询：双碳化钨板+锤/锉/剪线钳→空白模具，锤 400、锉 100、剪线钳 400 损耗，板消耗；空白模具+剪线钳→杆模具，空白被消耗、工具返回且输入不被查询修改；杆模具→线模具，前模具被消耗。杆模具非镜像在真实原生序列化器网络往返后保持，选定杆图样只匹配 78 模具中的一条。未把一个图样检查声明为全部模具无碰撞。

先前锅炉、燃烧箱、变压器、坩埚和物流制作检查在同一启动中也通过。服务器运行 200 普通 tick，正常保存停止；本批不另做世界重载或客户端启动。日志有 Mojang 公钥获取网络超时，离线开发服务器仍完成明确的上述成功回执及保存停止，保留该日志。上述为真实配方查询，不代表玩家界面、全部模具制作、完整生存或实际线缆能量/触电运行。普通活塞引擎制作入口、Forge 对应运行、当前客户端、成品安装、全部系统及旧存档仍待验。保留原授权和来源记录，整体移植仍未完成。

## 最终产物

双版编译/Neo 专服构建启动 1m 56s；最终双 JAR 3m 35s。573 个当前共享 class 字节一致，平台关键类/品牌/ID/版本正确，无重复/测试污染；本批 1,394 资源 SHA 在两 JAR 中与原件一致。

- `gregtech6-1.20.1-forge-0.0.0.jar`：38,552,146 字节；SHA-256 `c3c943456c6ddd5d808c4b35ab800f68f3ec24355125934525d03b90b90e9ba2`。
- `gregtech6-neoforge-1.21.1-0.0.0.jar`：36,601,267 字节；SHA-256 `5b6fe039be003432b9e9e6ea9b79d8383651cee71634bf29a7c4f5623141bbc0`。
