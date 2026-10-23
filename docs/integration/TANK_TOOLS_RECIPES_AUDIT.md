# 储罐工具、25种主阀制作与说明入口核查

第七十七批，2026-10-21，基线`18ee675d1`，承接[主阀／保存](MULTIBLOCK_TANK_AUDIT.md)与[危害／输出](TANK_HAZARDS_AUDIT.md)。新增ALIGN-266钨主阀制作未保留ANY.W、267壁板温度计被坩埚分支截获，扩126／232／251／262。22份原版、52份当前输入、64个平台视图、本机两个原生工件9个entry／7份只读javap，以及25条制作声明和六个双语结构说明见[回执](verification/tank-tools-audit-20261021.json)。本批没有运行Java目标代码、存档或客户端。

原只读根为`C:/Dev/gregtech6-master/gregtech6-master/src/main/java`；作者／LGPL头及原输入摘要留存，原文件未改。CompoundMaterials和未提交的WoodMaterials保持本批起点摘要。原输入若已有替换字符单列来源计数，不把全文件哈希当作全部文字／代码已验收。

## ALIGN-266：4种钨主阀的制作定义未保留ANY.W组

原Loader_MultiTileEntities的25条主阀制作都有当前对应声明。逐条比较三行图案、明确M部件和R／P材料键、隐含工具键，当前[MultiblockCraftingRecipes](../../core/src/main/java/com/gregtech/gregtech/data/MultiblockCraftingRecipes.java)的图案与代表材料映射相符。物品ID和形态前缀按原字符串比较，只允许材料名称的明确代表映射与大小写差异，不用去除全部标点来掩盖非法ID。

其中17004、17024、17044、17064原aMat=ANY.W，分别用该组的ring／plate／plateDense。原ANY.W.addReRegistrationToThis含MT.W与MT.TungstenSintered；当前MaterialGroups.W也保留这两个成员。问题出在制作声明固定为`mat:ring@Tungsten`等，MachineRecipeIngredients.materialIngredient据具体Tungsten生成普通形态标签，没有选择W组。

MaterialTagPack的普通标签按真实单一材料生成；拼写别名只处理声明的名字，已存在的AnyTungsten组不会自动把烧结钨并入普通钨标签。GTMaterial.addReRegistrationToThis记录组成员，不等于setRegistration重定向；CraftingTools也只展开工具，不会将这些材料标签改成W组。因此制作定义缺少原组范围，不应把上一批的代表名称相同认作完整配方对齐。

这里不声称25条最终有效配方都已枚举，也不把外部数据包可追加成员当成内置定义已保留原组。实时注册、缺形态回退、工具余物、实际烧结钨制作与外部标签重载仍待验收；本批没有更改制作数据或接受范围。

## ALIGN-267：壁板温度计先命中坩埚接口，显示0 K

原MultiTileEntityMultiBlockPart对非扳手／撬棍工具获取目标，验证部件在结构内后调用主控onToolClickMultiBlock。Base10再进入主控工具方法，MultiTileEntityTank的thermometer使用FL.temperature(mTank)；水银温度计实际注册Behavior_Tool(TOOL_thermometer)，并非没有消费者的接口。

当前两版实际多物品注册都把水银温度计操作交给ItemBehaviors→[BehaviorThermometer](../../multiversion/src/main/java/com/gregtech/gregtech/item/behavior/BehaviorThermometer.java)。它首先判断ITileEntityCrucible，再判断其它罐类型。MultiblockPortBlockEntity确实实现该坩埚接口，不能误报为“温度计完全不认壁板”；但getCrucibleTemperature只有真实坩埚且角色为CRUCIBLE时返回温度，其余返回0。

形成的17007殷钢储罐装入373 K蒸汽，壁板角色为FLUID_IO。原点击壁板可转发并报告373 K，当前会先匹配坩埚接口并报告0 K；直接点击当前主阀仍有专用分支，可报告373 K。蒸汽373 K与该阀气密／熔点门槛已有源数据，例子不依赖先发生熔毁才能到达温度计。

这是源码可达分支推导，未实际派发玩家操作或核对显示截图。原水银温度计Behavior_Tool默认成本0，当前可重复使用在这一点上没有缺项，不因主控返回10000就编造它需要100耐久。

## 排液器与漏斗／龙头的既有记录扩围

251扩储罐子情况：原GT_Tool_Plunger先注册Behavior_Tool专用路径，再注册通用Fluid回退；储罐专用方法是GarbageGT.trash(mTank,1000)，只清至多1000 L，与17999钻机的整罐合同不同。当前GTToolItem在专用多方块入口只处理建造／放大镜，随后先走BehaviorPlungerFluid。

当前Forge回退要求BE自身是IFluidHandler，主阀BE及实际原生父类／接口链均不是；capability存在不等于Java instanceof成立。Neo回退则查询注册能力。形成储罐存2000 L水的有限分支为原清1000、Forge清0、Neo清1000；未形成时Neo还受262门控。本批只读javap追到CapabilityProvider与全部有关能力／NBT接口，没有把父类名称猜测当作接口证据。

126扩此专用成本：原返回1000，普通排液器因子100，向上取整请求10；当前Neo通用回退请求100。两者是工具API请求，创造、材质和附魔处理后的实际损耗未运行；Forge没有成功动作，不套这个100请求。

262扩主阀手动入口：[FluidAttachmentBlock](../../multiversion/src/main/java/com/gregtech/gregtech/block/tool/FluidAttachmentBlock.java).attachedHandler会优先使用FluidToolTarget，再回退普通能力。储罐主阀及父类／配置Mixin没有实现该接口，因此漏斗／龙头也进入普通形成门控。未形成主阀的1000 L水灌入／抽取，原funnelFill／tapDrain直接访问mTank，当前为0。原壁板本就另有形成检查，不能把主阀例外推广到所有部件，也不为对齐删掉现代陈旧句柄／已加载保护。

## 物品说明、双语结构文字和Jade已有入口

TankControllerBlock.appendHoverText实际调用MultiblockTankTooltips；后者读取物品保存的罐内容，使用long容量／数量，按spec列出simple限制、防护与熔毁温度，并附结构、工具和朝向说明。六个3³／5³结构英文与原LH.add逐字相符，中文与指定`C:/Dev/GregTech_zh_cn.lang`相应S键逐字相符。只接受这六个键，未将其扩大为全语言验收。

原FluidTankGT.contentcap本来就包含结尾`L)`，当前保留相同形式，不能据观感擅自作为移植独有的括号回归修正。工具提示仍推荐建造棒／放大镜，但233已确认专用功能未接，复制说明不等于实现功能。

GTJadePlugin实际注册服务端BlockEntity数据和客户端Block组件；主阀通过MultiblockPortOwner实现StructureController。MultiblockJadeProvider.inspect写形成状态并读取流体能力列表，客户端再解码名称／数量／容量，不列为整套Jade缺失。232补充：该inspect本身也调用isStructureOk，是已有结构重扫的额外消费者。真实画面、超容量long内容的Jade表示、模型贴图／RGB／覆盖板和完整资源重载仍未验收。

## 源码整理与限制

展开FluidAttachmentBlock的构造形状旋转、原生交互、附着能力、访问包装与下方注入；构造的两个b局部绑定改bounds，说明改为实际接口优先关系。展开物品提示的内容／防护拼接、Jade各类状态和流体序列化。OriginalTankTooltipData的数字分组循环i改digitIndex，材料定义表未动。

四源七份完整执行流保持：core提示数据214，物品提示591／631，Jade1103／1144，附件2368／2614。仅归一明确局部绑定，成员调用和原始字符串／数值位置独立检查；一处说明纠正，其余注释／来源和Stonecutter指令保持。

未编译／目标初始化、Gradle、测试、游戏、构建或推送。声明比较、原生祖先检查和输入摘要不替代最终生存合成、工具交互、客户端或磁盘重启；其余全部原版系统与可读性队列继续，整体移植仍未完成。
