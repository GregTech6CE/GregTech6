# 垃圾桶／回收器物性、掉落和资源核查

2026-10-22，第一百一十八批，基线 `f6ee2c6cf`。上一轮补查 #78 的原版生存生成、整组保存及取出链，并提交复核记录，属于实际进展。本批新增 ALIGN-407–412；13份原版、22份当前生产Java、33段源码引用、两端12项原生工件条目／23段引用及18项当前资源见[回执](verification/garbage-physical-audit-20261022.json)。文件摘要用来定位证据，不代表列出的整个文件或所有生命周期已经验收。原树只读，GregTech-6 Team／Gregorius Techneticies和LGPL声明保留。

## 确认差异

| 编号 | 原版合同 | 当前路径与限制 |
| --- | --- | --- |
| ALIGN-407 | Loader_MultiTileEntities给垃圾桶设置硬度0.5／抗性6，回收器为6／6；Paintable基类实际读入并返回这些值。 | 两端分别注册3／3和8／1200。SourceBlockProperties是工具／等级数据，没有在这里把两项物性还原；两端实际原生strength／抗性getter已核。不据此声称实测了爆炸或采掘时间。 |
| ALIGN-408 | 回收器覆写getDrops返回空列表，世界MTE方块真实转发到该方法；原注册也列为非法掉落物。 | 当前回收器继承原生Block掉落入口，注册了普通BlockItem；内置BlockLootPack为它生成自身物品条目。缺少静态loot JSON不能推出“不掉落”，因为实际数据包会生成。限默认数据包和正确采集条件下的掉落路径；未跑生存破坏、其它事件／数据包或爆炸。与404的垃圾桶内部缓冲散落不是同一项。 |
| ALIGN-409 | 垃圾桶的两段setBlockBounds2只定义绘制边界；实际Root碰撞box()是完整方块，Covers的默认碰撞添加也调用它。 | 当前只定义两段并集SHAPE，原生getCollisionShape默认使用getShape，因此碰撞随外形缩小。按16格坐标枚举，原4096格、当前3376格；这只是源几何推导，未执行VoxelShape API或实体碰撞。没有把原选框也误写成完整方块。 |
| ALIGN-410 | 垃圾桶明确返回LIGHT_OPACITY_WATER，即3。 | 当前没有专用遮光覆写；noOcclusion、非完整形状和原生getter推得干燥时0、含水时1。GTWaterloggable实际提供空／原版水FluidState，不能把两种状态都算成0。未测实际照明或渲染。 |
| ALIGN-411 | 回收器三行用途说明后，还添加红色ADMIN_ONLY_CREATION，提示需要管理员生成或另行添加配方。 | 当前方块只添加前三行；共用tooltip收尾只处理采集、抗爆、可燃等，没有补这条创建说明。这里只是提示缺项，不推断原GUI要求管理员权限，也没有因提示缺失改变合成准入。 |
| ALIGN-412 | 两种方块注册时aStackSize均为16，ClassContainer保存后由内部物品getItemStackLimit返回；继承链无专用覆盖。 | 两端都用未设置stacksTo的普通BlockItem，当前原生默认值为64。Forge核属性构造，Neo核COMMON_ITEM_COMPONENTS和默认组件构造，未混用两种机制。限未改组件／NBT的默认物品形式，不改现有数量政策。 |

## 资源已有部分与未完成范围

两份方块状态和两份物品模型均指向真实的垃圾桶／回收器复合模型。每份模型都有着色实体底层和不着色透明覆盖层；垃圾桶两层分别使用顶盖 `(0,12,0)–(16,16,16)` 和桶身 `(1,0,1)–(15,12,15)`，回收器是完整立方体。桶身没有顶面、侧面不依赖邻块剔除，底面及顶盖相应面保留剔除声明，与原getTexture2的绘制条件分别核对。绘制盒相符不等于碰撞相符，所以409单列。

12张当前PNG均存在，文件头和IHDR尺寸为16×16，模型使用的贴图引用均能落到这些文件；这不是PNG完整解码、GPU绘制或原图像素一致性测试。原参考快照没有预期的endergarbage贴图目录，本批不能独立比对原PNG字节。共享JSON仍写forge:composite，现有Stonecutter配置有Neo复合loader转换；没有重新生成资源或构建来替代验收。

原注册ClassContainer还会把材料颜色补进NBT，当前有BlockEntityPaintMixin、BlockPaintColorMixin和ItemPaintColorMixin，不能误报整套喷漆功能缺失。不过默认未喷漆钢色回调和实际像素尚待专门核对，tintindex=0本身不能证明原色已经应用。完整配方／标签覆盖也不在本批通过范围；只确认当前原生存配方表已有垃圾桶条目。

## 源码整理与证据边界

整理两类方块、回收器菜单及Neo存储注册类，四源325→424行。展开两版交互入口、switch分支和压在一行的注册步骤，局部b／ctx／dir／sp等共14处改为registeredDrawer、blockContext、direction和serverPlayer；注册顺序、公开类型／签名、ID、属性、配方、数据键及玩法均保留。

全文件的保守新名称检查发现Neo已有context参数，因此没有直接使用ctx→context，改用blockContext；这不等于声称两个不同方法的同名参数原本会编译冲突。七份完整平台代码视图经列明的标识符归一后，有序代码、字面量位置、其它注释和Stonecutter指令保持。两条说明有意修正：绘制边界不能直接称为原碰撞边界，菜单也不能声称所有点击都会经过真实提取。用户WoodMaterials.java和CompoundMaterials.java字节摘要保持。

上一轮403的快捷栏交换路径仍是高优先级源码见证，本批没有执行原生菜单或游戏复制。没有运行Java编译器、Java应用探针、Gradle、GameTest、游戏、实际存档重启、构建、推送或GitHub写操作；只读取本地原生源码／反汇编并核摘要。物性、GUI事务、默认RGB、完整垃圾入口和全系统仍继续。累计412条包括已修项和保留政策，不是未修数或完成率，goal保持active。
