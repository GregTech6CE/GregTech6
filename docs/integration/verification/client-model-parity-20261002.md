# Client model parity / 2026-10-02

本批修复材料、流体、基础机器、分类图标及材料方块的模型加载边界。共享模型与原材质继续作为唯一几何来源；两版低优先级内建资源包仅提供原静态资源缺失的库存引用/方块状态，几何加载器调用 ModelBaker 的真实模板缓存，保留现有染色、硬币、流体覆盖及方块物品显示。不是以 missing/dummy 模型替代内容，未新增三套底层或逐件 JSON 文件。

两版共享 legacy items 图集目录，修正 gas/plasma 单帧 PNG 的非法 20 帧定义。补充三个树洞默认无树液的真实库存父模型，两个矿石分类图标沿用 NONE 组的原矿石基底/覆盖贴图。Blue Mahoe 树苗拼写指向已存在的原 bluemahoe 贴图，注册及保存 ID 不变。

## 反馈与运行

初始完整 Native 启动进入主菜单，但有 57,462 次模型 JSON 加载失败、45 个缺失材料方块状态以及无效动画帧。共享别名计数为零缺失不足以证明所有资源请求正常。首轮新增 Forge geometry 适配编译因接口参数/泛型未对齐失败，修正实际签名后双版编译通过。第一次修正后的客户端因 bootstrap 画廊写错 gear_bronze ID 失败（Gradle 仍返回 0）；不是生产资源缺失，已改为原注册 ID gear_gt_bronze，失败如实保留。第二次画廊错误选择 fluid_item_water，而原版水沿用原版流体、没有该独立注册项；改用日志证实的原 fluid_item_reedwater，未虚构新增注册或把失败改为成功。

最终 Native 同一短启动检查确认所有 56,036 材料物品与 925 流体物品最终库存模型均非 missing sentinel，实际渲染铁锭、铜板、青铜齿轮、金币、芦苇水流体显示物品和粉碎机到主菜单上的 bootstrap 画廊，截图生成、解码、人工查看及正常停止。无模型 JSON 加载失败、无缺失方块状态/非法动画帧；仍有 Pine/Ebony/White Mahoe 木辞典种类原美术缺口，没有移除注册或用任意替代图伪装完成。

首轮 Forge 隔离目录未预置首次辅助提示选项，超时停在 AccessibilityOnboardingScreen（尽管 Gradle 返回 0）；该首轮不算通过。另外发现 Forge 还注册 40 组不存在的 ore/oreSmall 模板，改为仅加载实际新增 NONE 分类图标模板，世界矿石继续使用专用入口。仅在 build/client-smoke-run/options.txt 关闭首次辅助提示/静音，不触碰.minecraft。重试 Forge 相同生产适配在实际开发客户端进入主菜单，截图解码/人工查看/正常停止。Forge 本批未增加全目录模型/六物品画廊断言。两最终运行加载 Blue Mahoe 世界模型原贴图路径更正，但库存 JSON 有另一旧拼写；Native 成功日志与首轮 Forge 日志仍报告该库存贴图缺失。本批随后修正库存引用并核对原 PNG/两包资源，最终 Forge 重试已无该缺失贴图引用；Native 未再逐件查看这棵树苗，不把该 Native 一项计为运行验证通过。

## 限度

画廊使用实际物品 renderer，但不等同玩家界面或进入世界的方块渲染。原目录还含 GIMP 编辑备注及两张非法资源路径的备份 PNG，游戏忽略其加载，保留原件，打包整理仍待后续处理。此批没有客户端世界/生存流程、客户端重载世界或成品 JAR 安装；不把所有模型非 sentinel 当成全部像素正确。原少数木辞典种类贴图仍缺，完整渲染与三源整合未完成。原专服与指定世界重启证据分开保留。警告数降低，不据此承诺启动性能提升。

## 构建与交付

最终双 JAR 构建 2m 3s；573 个当前共享 class 字节一致，平台关键类/元数据、无重复条目及 bootstrap 隔离检查通过。当前批生产模型资源与两包哈希核对通过。

- neoforge 客户端：1m 31s，缺失模型 JSON 0、缺失状态 0、非法动画帧 0；缺失贴图 ID 16（完整列表及独立日志 SHA 见 JSON）。
- forge 客户端：2m 28s，缺失模型 JSON 0、缺失状态 0、非法动画帧 0；缺失贴图 ID 15（完整列表及独立日志 SHA 见 JSON）。
- gregtech6-1.20.1-forge-0.0.0.jar：38,565,901 字节，SHA-256 `bb2bc63583bb66be7041b8c54377d771dcd2a93ae728dfb57031d3434fb699ba`。
- gregtech6-neoforge-1.21.1-0.0.0.jar：36,756,963 字节，SHA-256 `306749b43f785e13366773ae0c79691498db85c1075551e6d2b28c06d06d6fe0`。

命令：Neo `-PdirectCoreResources=true -PdirectCoreClasspath=true :compileJava :neoforge:compileJava -PclientSmoke=true -PclientModelSmoke=true -PclientSmokeTimeout=240 -PclientSmokeHeap=4g :neoforge:runClient`；Forge `-PdirectCoreResources=true -PdirectCoreClasspath=true -PclientSmoke=true -PclientSmokeTimeout=240 :runClient`；最终 `-PdirectCoreResources=true :distributionJar :neoforge:jar`。Java17 Gradle launcher，两平台按本机配置工具链；4g/6g 是隔离 smoke 的运行配置，不更改游戏设置。名称、0.0.0、gregtech6 与旧 gregtech: 资源/保存命名空间不变；未修改授权、来源目录或推送远端。整体移植仍未完成。
