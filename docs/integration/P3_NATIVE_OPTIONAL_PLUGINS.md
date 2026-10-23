# 双版本初期对齐：NeoForge Jade 与 JEI

按 D011，以既有 Forge 实现和 docs/JADE_TRANSLATION_CHECKLIST.md、现有配方/多方块阶段 MD 为参考，完整适配 12 个 Jade 插件/提供器及 11 个 JEI 插件、配方目录和结构预览类；不新增 donor 功能，不另建材料、机器和配方系统。

Jade 接实际机器进度、能量、流体、燃烧箱、锅炉、引擎、反应堆、坩埚/模具及结构状态。小/大坩埚使用实际 Native 实体，端口查询真实结构所有者；填充百分比和材料表摘要取实际内容。物品/流体使用 1.21.1 registry provider 和组件序列化，流体能力从世界查 NeoForge block capability。

JEI 保留有可见配方的原 RecipeMap 类别、概率/能耗说明、所有真实机器及手动站催化剂、工具装配与四类矿物信息；流体展示保留真实流体身份和组件。结构预览取当前 Native 控制器、原多方块几何与实际零件，包括原阀、锅炉、大坩埚、轴向发电机、VonDaGraagg、雷电杆、基岩钻机、塔和聚变结构。Native 既有 coke/cryo 世界匹配方法保持不变；补齐预览目录。渲染包装适配新的 packed-color addVertex/setNormal API，保留面亮度和既有旋转/层筛选控件。

Native 开发依赖固定为 JEI 19.57.0.450、Jade 15.10.6+neoforge，来自 BlameJared 官方 Maven 和 Jade Modrinth MC1.21.1/NeoForge 条目；Forge 原依赖版本保留。均为可选集成，生产模组不嵌入这些依赖。版本、原始下载哈希和源码来源见 core/provenance/native-optional-plugins-integration.json。

集中双版本编译通过，14 秒，Forge 未变为 up-to-date；一次既有 Neo 主菜单短启动和 Native 打包结果另外记录。本轮没有新增夹具或每个提供器反复启动。

主菜单启动不能验证 JEI 进入世界后注册全部类别、U/R 检索、结构实际渲染，也不能验证 Jade 实际提示、服务端数据传输。继承资源 variant/model 缺口、完整玩法、专服、独立进程重载、旧档和成品包启动继续待验。初期测试包用于快速人工试用；本阶段和原三源完整整合目标不标记完成。


2026-10-01 D011 Native Jade/JEI 对齐：完整12 Jade插件/提供器、11 JEI目录/预览类及2助手适配，27 Java来源记录；实际机器/手动站、坩埚/端口、组件流体数据和原结构几何复用现有底层。固定Native JEI19.57.0.450/Jade15.10.6+neoforge，Forge原版本保留、依赖不嵌入。集中编译14s通过；一次既有Neo客户端+Native打包2m1s，151帧/1280x720主菜单截图观察通过，日志证实GTJadePlugin实际发现/注册成功。新Native jar35560865 bytes、Forge旧包源码未变复用，570当前core类/67498资源字节检查通过。JEI进入世界注册/实际UI、Jade实际HUD、预览交互、成品jar启动、玩法/独立重载/旧档仍未验；继承模型variant警告仍存在。无新夹具或反复启动，整体移植仍未完成。详见P3_NATIVE_OPTIONAL_PLUGINS.md及verification对应三份回执。
