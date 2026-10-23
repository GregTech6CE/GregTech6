# 红石导线的染色、自动连接与剪线钳入口

2026-10-22，第一百二十一批，基线 `eeb0ec769`。上一批完成红石数值共用及414／415记录并提交，属于实际进展。本批新增416–419，扩既有078的红石导线宿主；八原版、29当前生产Java、38源码引用及八资源留摘要，见[回执](verification/signal-wire-client-audit-20261022.json)。引用只支持列明合同，不代表整文件／整系统已对齐。原树只读，GregTech-6 Team／Gregorius Techneticies 的 LGPL 来源保留。

## 416：绝缘红石导线喷漆落到错误层

原 MultiTileEntityWireRedstoneInsulated.getTextureSide 使用已喷漆RGB或默认 RGB(96,64,64) 绘制绝缘层；getTextureConnected 的金属芯单独取材料纹理／材料色，外环取绝缘色。默认未喷漆色0x604040，当前值正确。

两端实际模型替换都创建 PipeWireBakedModel：侧面／绝缘环为tint1，金属芯为tint0。capFace确实分别产生这些层，不能只读静态模型断言芯没有画出来。两版颜色处理器给tint1固定0x604040；BlockPaintColorMixin只改tint0，BlockEntityPaintMixin／PaintTargets为当前GT实体提供真实喷漆状态与刷新。因此绝缘线喷漆后，外皮仍取默认色，金属芯反而使用喷漆色，与原版分层不同。正常裸线的主层喷漆不算此问题。

两版物品颜色Mixin也只识别tint0，但本项不据此宣称自然掉落物已保存喷漆；掉落保存与Neo物品alpha的既有272是不同合同。完整像素和玩家实际画面未验收。

## 417：新线放置时打开邻线原本关闭的端口

原 TileEntityBase09Connector.onPlaced 先连接放置面的反面；其它方向只在相容邻连接器的对面已经打开时接入。当前 SignalWireBlock.onPlace 遇到相邻SignalWireBlock，经过已加载／覆盖板准入后，无论邻方原连接位是什么，都会把本端和邻端设为true；相邻红石源也会额外打开本端。

有限源码条件：从上方对支撑面放置新线，水平方向已有相容线且对面端口关闭。原非放置反面分支跳过该闭端，当前打开双方。因此登记自动连接策略差异，未运行玩家线路，也未声称所有放置方向均不正确。

## 418：Neo剪线钳绕过覆盖板连接准入

原connector.connect先检查本端interceptConnect，也检查相邻连接器的对应覆盖面。AbstractCoverAttachmentTorch拒绝连接，并在安装时断开端口；火把／中继器两类共用该父类。

当前 ToolInteractions.canApply 在开端口前调用 CoverConnections.canConnect，检查两端的火把／中继器及关闭挡板。Forge红石线使用这条路径。Neo红石线调用平台 WireToolInteractions，实际步骤为工具／权限／区块检查、翻转连接、同步同类邻端、声音和损耗，缺覆盖板准入。

源码见证选择点击本线的空面，邻方绝缘线的对应面装有火把或中继器，双方端口原本关闭且加载／权限通过。原版和Forge检查邻方面板，Neo可写入双方连接位。本地面为空，避免把覆盖板配置点击误当连接操作；事件分派也另行追踪。这是入口和写状态的源码合同差异，未执行原生点击或线路测试。

## 419：裸红石线被排除火把／中继器安装资格

原 AbstractCoverAttachmentTorch.interceptCoverPlacement 只要求宿主属于 MultiTileEntityWireRedstoneInsulated。裸线 MultiTileEntityWireRedstone继承该类，也满足条件；通用Covers基类默认允许安装，线类没有另行拒绝。父类名里的Insulated不代表只允许绝缘线。

当前 PanelCoverRuntime.canAttach 明确要求 wire.insulated()，因此拒绝三种裸线。实际后备宿主和ComponentCoverStorage.attach会调用它，非未消费的死方法。登记安装资格缺失，不据此宣布两类覆盖板全部红石时序已核完。

## 扩围、误报与源码整理

原红石线同样继承可发泡连接器；当前无湿／干包覆状态，硬化剂／去除剂入口不能补齐。扩078的红石线范围，不重复报“全部泡沫未移植”。当前SignalWireBlock明确覆写getCollisionShape并使用CollisionContext.empty，手持剪线钳只扩大选框，不能据getShape推断实体碰撞变整块。静态六模型的tint0／1声明、实际动态模型与通用Mixin分别留证，不只凭静态资源推断最终模型。

展开Neo WireToolInteractions和SignalWireClientSetup：权限早返回、端口写入、邻端同步、颜色／模型事件逐步成行。heldTool／connectionSide／neighborPos／neighborState／wireEntry／blockId／fallbackTransforms等42处局部名称受控更改。公共事件方法名、平台差异、未使用的rubber纹理局部、声音／损耗和连接策略不变。两压行源54→111行。

Neo默认颜色方法与Forge物品／方块两个重复三元表达式集中为12行core SignalWireColors，只返回已有材料／默认绝缘色，不引入喷漆状态或改变alpha；只在tint0读取getColor，原生material()为无副作用字段getter。Forge大事件类保留其它排版；三旧源877→934行，加新helper12行。三完整平台流在明确更名及三处抽取归一后，有序token、字面量位置、其它注释保持；新helper完整正文另核。包名／方法名碰撞在预览时先被守卫拒绝，未写入生产。

本批包含上批120的core、Forge1.20.1、NeoForge1.21.1 compileJava实际执行通过，首次报告1分19秒；复核后保留按层懒读取材料颜色，最终增量编译26秒通过。复用已记录的同版本本地API文件适配器，脚本／输入JAR摘要重核，生产构建配置未改；不声称默认离线解析或GitHub CI恢复。无新增镜像测试、GameTest、游戏、原版1.7.10、存档重启、打包、推送或GitHub写操作。用户两份材料摘要保持。

419是累计差异编号，含已修、部分修复和保留政策，不是未修数量或完成率。剩余资源／物理／生命周期、独立设备、其它全系统与源码标准化继续，goal保持active。

## 2026-10-23：原生复现并修复显式连接与裸红石线覆盖板（417–419）

本批以 `61d6ab692480b10a33b6474aee47b2b52e0d0bfb` 为生产基线。两份只读原树一致：`TileEntityBase09Connector.onPlaced` 先尝试点击支撑面的反向端口，其余方向只接入对面已经开放的相容连接器；红石线的 `canConnect=true` 允许显式连接普通支撑块，并不要求它是红石源。裸线 `MultiTileEntityWireRedstone` 继承 `MultiTileEntityWireRedstoneInsulated`，所以原 `AbstractCoverAttachmentTorch` 同时允许裸线、绝缘线安装原生红石火把／中继器。

修复把红石线放置连接放到 BlockItem 已建立实体、恢复方块状态及实体数据之后，验证本端及邻端覆盖板，再对同类线同步端口；删除根据邻居出现、消失或供电而重新推断端口的逻辑。火把／中继器资格仅移除错误的绝缘布尔限制，不放宽到EU线或机器；Neo剪线钳补齐两端覆盖板准入。剪线钳并非另造一套连接逻辑：Forge原已使用通用 `ToolInteractions`，Neo原已与EU线共用 `WireToolInteractions`；导致剪不开的覆盖逻辑是红石线邻居通知自动重开端口。

同批恢复物品管对点击箱子、流体管对点击原生流体处理器、EU线对点击允许EU输入或输出面的放置尝试；普通能力端点只影响点击面，不向其它面泛连。已有 `ItemPipeConnectable`／`FluidPipeConnectable` 的附属任意侧自动连接约定保持不变。`GtSignalSource` 则明确修正文档：它使实体能从已开放端口被读取，不授权打开无关面或重开手动关闭面；信号源移除保留端口位，信号随网络重算清除。

### 原生证据与测试修订

在未改生产代码时，三个命名空间合计355例：Forge182例中170通过、12失败；Neo173例中161通过、相同12项失败。此前299门禁未包含的 `gregtech_cover_issues` 9项与 `gregtech_pipe_auto` 9项现已在两端实际执行；旧“裸线拒装”和“邻接信号源必开／消失必关”断言按原源码显式修正，原始文件保留。严格报告检查器与CI同时要求这些18个身份及对应命名空间，防止未执行却被计入。

失败记录包括六方向错误保存为全部端口 `mask=63`、手动关闭后被重开、源移除清端口、裸线原生覆盖板拒装、点击覆盖板阻挡面仍连接，以及点击原生管道／EU端点不连接。邻端火把阻挡对照中，Forge剪线钳已正确拒连，Neo则绕过检查，二者放置都错误通过。

首次Forge中的旧保留端口例先撞到另一处源码可解释的问题：`onPlace` 递归修改世界状态后，外层原生 `LevelChunk` 将实体状态覆回初始闭端；再写相同世界状态是空操作。该运行没有证明“移除后端口”断言。保留首次XML／日志和测试副本后，仅将夹具改为初次就显式开放EAST，并增加世界／实体状态一致前置检查；重跑两端均通过读取附属信号源的前置对照，最终到达预期移除断言。未放松最终合同。

后续用户报告“两根相连的红色合金线剪不开”，另加一个共享例，因此固定版门禁为357（Forge183＋Neo174）。该例是**基线之后新增**，不能称为355基线失败证据：裸线／绝缘线各六方向都经过两个真实BlockItem放置、原生剪线钳切断双端、邻居通知及信号活动后保持断开，再验证原生重连和传输。失败观察先保存，独立重连对照不会掩盖切断失败。

固定版已完成双端完整构建、打包／附属API／remi及Python守卫，随后Forge183、Neo174全部通过，零失败／错误／跳过；两端严格报告检查通过，输入／结束源码快照一致。源码、原树、XML及各阶段退出码摘要见[357项本地回执](verification/signal-wire-pipe-placement-20261023.json)。Forge XML SHA-256为 `ca7ade8f6d3a30e54338e602e00122c9371e450c4cc0a91d5cf275944c843f07`，Neo为 `ebbcb23e121199e4f556c2329614c89a9aa276b46fc08cd8b07828477a7d8a11`。此为本地原生验证，不冒称尚未执行的本次提交GitHub CI结果。

### 范围限制

红石线放置、剪线钳、覆盖板使用真实 `ServerPlayer.gameMode.useItemOn`；物品管／流体管／EU线例直接调用注册物品的原生 `BlockItem.place`，验证放置回调与消耗，但不是完整网络包路径。测试使用隔离高位夹具，结束恢复方块与实体状态。显式失源例使用模式失效后立即重算，不据此宣称独立的自然下一tick时序证明；也未据已加载邻居守卫宣称所有跨区块加载／卸载场景已验证。覆盖板选框／承托面、相邻板片及支管渲染、其他网络图保护留在下一有界批次，没有夹带进本批修复。
