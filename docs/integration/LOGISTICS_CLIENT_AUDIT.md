# 物流线与CPU显示的客户端、形状和资源核查

第七十四批，2026-10-21，基线`015d87c81`。继续[核心状态](LOGISTICS_CORE_STATE_AUDIT.md)、[搬运](LOGISTICS_TRANSFER_AUDIT.md)与[网络生命周期](LOGISTICS_NETWORK_AUDIT.md)，新增ALIGN-261，扩126的物流线连接工具成本。18份原版、39份当前输入、54个平台视图、69项当前资源，以及本机两个原生工件4个entry／2份只读javap视图见[回执](verification/logistics-client-audit-20261021.json)。没有把客户端源码或资源存在当作实际画面验收。

原只读根为`C:/Dev/gregtech6-master/gregtech6-master/src/main/java`，作者和许可证头随来源留存，原目录未动。CompoundMaterials与WoodMaterials保留本轮开始时的字节摘要。

## ALIGN-261：CPU显示前景缺少原自亮属性

原四类CoverLogisticsDisplayCPU*各有0–10共11个前景帧，均调用BlockTextureDefault.get(path,T)，underlay则用普通get(path)。实际重载的boolean是aGlow，构造链设置mUseOwnBrightness、常量亮度240并关闭AO，最后传给ITexture.Util和Tessellator；不是随手把一个未知布尔值解释成发光。

当前[LogisticsCoverRenderer](../../multiversion/src/main/java/com/gregtech/gregtech/client/LogisticsCoverRenderer.java)先用邻块LevelRenderer.getLightColor取得light，再把同一个light传给underlay与动态前景。drawSurface最终将它写入Forge uv2／Neo setLight。实际两版原生RenderType.cutout都启用了LIGHTMAP，当前没有将前景切换到自亮的分支。

因此这四组放置后的CPU刻度前景会跟随环境光，缺少原前景不依赖环境的自亮合同。原240与现代packed light不是同一种编码，不以数值直接等同；本条也不要求给方块增加照亮世界的光源。只是前景层的源码差异，没有运行暗室、光影包或截图比较，本轮未修改渲染玩法。

## 已有的刻度、信号和客户端调用链

原核心在每秒门槛保留上次CPU使用量，再更新显示；当前previous Usage→updateDisplays→LogisticsCovers.setDisplay→signal／visual→保存／同步→共享渲染器的链路已有。停止显示资格另列252、失效统计另列253，不重复报“全部CPU表未做”。

原公式和当前core LogisticsDisplayRules在正常正总数分支一致。总数27、使用0／1／8／26／27／28时，有限源码算术分别为信号0／1／5／14／15／15，刻度0／1／3／9／10／10。超过总数的显示会饱和；这些例子没有调用编译后的目标类，也不证明使用计数来自完全相同的运行时点。

原显示覆盖板getRedstoneOutStrong／Weak同取value，当前线、核心和多方块部件的弱／强信号入口均调用LogisticsCoverSignals，读取对应面的displaySignal，原生query到覆盖面的方向有显式opposite转换。信号变化时setDisplay还通知邻居。这里只核入口与值消费，没有实际测红石或承认所有宿主的全部信号逻辑。

Forge的GregTechClient和Neo的LogisticsClientSetup均有客户端MOD事件订阅，并为核心、物流线及多方块端口注册同一个LogisticsCoverRenderer；其它覆盖板宿主还可从已有fallback／tank renderer转入renderFaces。不能只看到某个类没有自己注册renderer就判断整族未渲染。

## 资源与原生图集入口

四族各11帧＋underlay共48张PNG都存在；再加基座、两张线纹理和四张物品图标，共55张PNG。其PNG签名、chunk CRC与压缩流校验通过，记录了尺寸；这不是像素内容或画面的验收。本批还读取14份JSON，包括七个线中心／方向模型、multipart、物品模型及方块图集。

assets/minecraft/atlases/blocks.json已有block/machines/covers/logistics目录源。实际两版DirectoryLister从textures目录收集PNG，以prefix构建sprite ID；ArmRenderHelper从LOCATION_BLOCKS读取这些ID。不能将动态路径未写进普通方块模型误报为“所有CPU帧没入图集”。保留的原源码树在这次限定查找中没有找到对应原PNG输入，因此没有宣称当前图片与原版逐像素一致。

线multipart七项都指向实际存在的复合模型。源JSON中的forge:composite由multiversion/build.gradle声明的Stonecutter整份JSON规则在Neo端改写，源码集也明确连接生成任务；本批没有执行生成或烘焙，不能单看原维护源的forge前缀就报Neo无法使用。

## 物流线实体与工具边界

原24901注册直径6/16、硬度1、抗爆2；当前两端实际构造均为strength(1,2)，选择形状中心5..11并按连接方向延伸，模型也是对应6像素部件。只接受这些明确参数，不据此宣布采掘等级、所有掉落或绘制完全一致。Neo remaining-icon初始化读取LogisticsRegistries.queuedBlockIds并跳过已排队ID，后备工厂中的同名构造不代表第二个活动注册。

原始LogisticsWireBlock.getCollisionShape只返回线形状，但已配置的CoverCollisionMixin会在最终BlockState查询补CoverWorldInteraction.collision，包括物流覆盖板；完整选择立方体也由CoverPlacement与工具spec参与。CoverPlateGeometry是块内2像素厚，不能忽略这些消费者再报覆盖板无碰撞，亦不能把工具完整选框当成玩家碰撞变成实心块。

126扩围：原Base09Connector成功连接／断开返回10000，普通GT剪线钳Behavior_Tool因子100，经units请求损耗100。当前Forge通用ToolInteractions及Neo线块直接分支均调用damageForUse(1)。这里是成功操作的请求值，未将创造／附魔／材质处理后的实际损耗写死，也不套到电动工具。matchesTool实际检查电动工具canInteract，不能仅因Neo有直接分支就报告空电仍可用。

## 源码整理与未验收范围

展开线的Neo连接操作、平台交互方法、连接形状与更新条件；限定局部名改为neighborPos、updatedState、connectedHere／There、coverBlocksConnection和entityContext。渲染器be／vc／out改为blockEntity／vertices／surfaceOffset，整理坐标与顶点链。Neo注册和客户端入口展开链式构造／事件订阅；两处说明纠正为实际职责，不再声称线的注册类还注册了全部14种覆盖板。

四源码六份完整执行流在限定绑定归一后保持：线1462／1909，渲染器904／890，Neo注册449，Neo客户端196。原始字符串／数值位置、其余注释和Stonecutter指令另核；两份Neo专有边界只按Neo视图计算，没有虚构Forge也编译它们。线最长行206／275降为161，注册最长行307降为123。

未Java编译／目标初始化、Gradle、测试、游戏、构建或推送；javap只读取已有原生工件。实际贴板、采掘、红石、资源重载、客户端画面、存档重启和供电／调度时序仍需分别验收，其余全面核查与可读性整理继续，goal保持active。
