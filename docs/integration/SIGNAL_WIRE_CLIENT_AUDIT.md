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
