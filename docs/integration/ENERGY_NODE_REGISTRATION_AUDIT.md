# 能源设备注册业务去重与物品上限扩围

2026-10-22，第一百二十二批，基线 `48576f18e`。上一批已提交默认染色共用、Neo源码整理及416–419记录，属于进展。本批没有增加差异编号，扩既有218的39项设备，并实际去除两版创建业务副本。来源／核验见[回执](verification/energy-node-registration-audit-20261022.json)：24原版、19当前生产Java、54引用、五实际原生条目及三复用原生引用。原版继承链中的20份文件只用于类型／堆叠覆盖检查，不等于这些文件已完整审查。GregTech-6 Team／Gregorius Techneticies 的 LGPL 声明保留，原树只读。

## 两版共用设备创建流程

GTEnergyNodes 原来在Neo分支压入一份长lambda，Forge分支又维护同样的 Properties／特殊方块选择。现在共用 private createBlock，先保留强度4／4、工具条件和太阳能noOcclusion，再按原顺序判断蒸汽涡轮、热转换器、旋转转换器、电磁铁、电池槽、变压器、太阳能和普通节点。创建仍发生在原注册supplier执行时，没有提早实例化。

private itemProperties 共用原“蒸汽涡轮stack16、其余普通Properties”的规则；private registerSpecifications 共用 EnergyNodeDefinitions 后 MagnetMachineDefinitions 的遍历顺序。Neo／Forge的注册对象类型、ALL列表、ID、物品和方块事件订阅、能力注册、重复注册守卫各留原位置。反应堆的静态／运行时注册时机和各自顺序也保留。这是当前模块业务去重，不是把原生Minecraft方块类型移进core。

两份旧工厂在限定归一后各为265个有序token，与新共用正文相同。归一只含已知block类的全限定名、Neo var props与Properties.of的实际返回类型，以及strength(float,float)上精确等值的4／4.0f。两端实际原生重载声明另核，未仅凭编译成功猜测两个不同重载等价。itemProperties和目录helper完整正文各对应旧表达式／旧两调用；去掉三精确抽取后，Forge518／Neo816个剩余token及其它注释保持。

Neo旧“Complete original node catalog”说明改为当前已注册能源节点，不再用该说明暗示414魔法场设备已经存在。其余注释保留。源码由127→179行，长注册、静态块和能力事件展开后增加可读行数，同时创建业务只有一份；行数不是架构完成比例。

## core目录的局部名称规范

EnergyNodeDefinitions 的局部Tier／Step记录把v、hi、lo明确为inputRate／outputRate，mat明确为material；Step循环的s、两组motorOut／dynamoOut也改为明确名称。值表、材料引用、ID组成、耗能／产能、顺序、英文名和大小写转换不变，表格空格保持。两个局部记录只在该方法内使用，没有改公共EnergyNodeSpec或外部材料定义。

本文件88→90行，完整core流经列明局部名称归一后保持。两源合计215→269行，三完整执行视图留证。受保护的用户WoodMaterials.java与CompoundMaterials.java摘要不变。

## 218扩充39项，而非新建39个问题

原Loader的逐行aClass分配、物品上限参数，以及实际类继承分别核查；不能把Flux当作Electric的同一个类。七种具体类向上共20份源码均未实现专用堆叠覆盖接口／方法，原容器的mStackSize由MultiTileEntityItemInternal实际返回。以下39条均声明stack16：

| 家族 | 原编号 | 当前ID前缀 | 数量 |
| --- | --- | --- | --- |
| 电动机 | 10021–10025 | electric_motor_ | 5 |
| Flux电动机 | 11021–11025 | flux_motor_ | 5 |
| 发电机 | 10111–10115 | electric_dynamo_ | 5 |
| Flux发电机 | 11111–11115 | flux_dynamo_ | 5 |
| 电磁铁 | 10031–10035 | electromagnet_ | 5 |
| Flux磁铁 | 11031–11035 | flux_magnet_ | 5 |
| 电变压器 | 10040–10048 | transformer_ | 9 |

当前真实目录的五档数组／九段变压器表生成这些ID，并沿两端注册入口创建普通BlockItem。它们不走蒸汽涡轮的stack16分支，默认上限64；原生Forge Properties构造器及Neo COMMON_ITEM_COMPONENTS和消费入口均有字节码／源码证据。完整39条旧ID→当前ID矩阵在回执中，不是实际注册器或库存的运行结果。

218此前已核23项，本批加入39个不重叠对象，明确范围扩为62。没有把所有能源设备、电池含电形态、其它组件覆写或其它模组改动后的上限都宣称核完。本轮保留现有64策略，未借去重改回16；蒸汽涡轮已有stack16也保留。

## 验证和待续

最终core、Forge1.20.1与NeoForge1.21.1 compileJava实际执行并通过，报告2分21秒。复用同版本本地API适配器，脚本和输入JAR留摘要；生产Gradle配置未改，不宣称默认离线元数据或GitHub CI恢复。

预览第一次发现Javadoc缩进被格式器改变后恢复原文，var／显式类型也经过专门比对。矩阵初稿把三个Flux类型误当Electric，原Loader类分配检查拒绝后补齐真实Flux继承链；错误假设未进入正式结论。没有新增镜像测试、GameTest、游戏、原版1.7.10、存档重启、打包、推送或GitHub写操作。

419仍为正式差异账本编号总数，含已修与保留政策，不是未修数量。目录完整性、其它原生注册与玩法消费者、现有差异和剩余架构／源码标准化继续，goal保持active。
