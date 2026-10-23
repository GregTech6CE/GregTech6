# 电池盒目录与齿轮设备注册整理

2026-10-22，第一百二十三批，基线 `268b8c10c`。上一批共用能源创建并扩39项堆叠记录，属于实际进展。本批没有新增编号，218再补20项电池盒；四源整理并共用齿轮设备的物性工厂，见[回执](verification/battery-gear-registration-audit-20261022.json)。16原／14当前生产Java、18引用和三原生条目／三引用留摘要。原继承链中的12份文件只核堆叠覆盖，不宣称完整玩法审查。GregTech-6 Team／Gregorius Techneticies 的 LGPL 声明保留，原树只读。

## 补核二十种电池盒默认物品形式

原 Loader_MultiTileEntities:892–896 明确循环 `i=0..9`，10080+i为普通四槽、10090+i为大型十六槽，两个声明均stack16。普通类继承TileEntityBase10EnergyBatBox，大型类继承普通类；完整类链没有专门的物品堆叠覆盖接口／方法。ClassContainer保存aStackSize，ItemInternal实际返回该值，不能只看注册参数推断最终上限。

当前BatteryBoxDefinitions仍为十档各普通／大型两项，ID分别为battery_box_和energy_storage_。大型前缀表示原大型电池盒，非另一套无限存储。槽位仍4／16；输入输出的`8L << (2*tier)`及材料表、标签和名字保持。真正注册由EnergyNodeDefinitions接到GTEnergyNodes，再经两端入口；其itemProperties只对蒸汽涡轮设16，这二十项用普通Properties。

本机Forge原生Properties构造器和Neo默认组件／消费入口均为64，三工件重新核摘要。因此218的明确核查范围从62增至82，20条旧ID→当前ID见回执。这里只核普通默认物品，不包含外部组件覆写、玩家库存实测、充电后的单体化学电池，亦不宣布整个储能系统已完成。电池盒的缓存／物品电量和既有061、017、105等继续沿[储能核查](BATTERY_STORAGE_AUDIT.md)，本轮未改变这些行为。

## 齿轮设备两份物性表达式共用

GTGearboxes的自定义齿轮箱和旋转变压器原来各自创建同样的木／金属Properties：mapColor、强度6／6、WOOD／METAL声音与工具要求。现用private machineProperties(boolean wooden)保留一份；两次调用仍各创建新对象，没有复用可变Properties实例。wooden依旧在原supplier外计算并捕获，未改变计算时机。

区别也保留：只有齿轮箱调用者追加noOcclusion，旋转变压器不追加；两类物品原已有明确stacksTo(16)，并未被扩进电池盒默认64的问题。注册类型、顺序、13+13检查、ID和模型入口均保留。四条确实仅在import出现的未使用类型导入清理，未删除实际材料引用。

旋转档位的t、input、output改为voltageTierIndex、inputVoltage、outputVoltage；core GearboxCatalog的十三行材料／数值表保留。BatteryBoxDefinitions展开目录、循环和材料列表，BatteryBoxBlock展开构造、指示灯0..2状态和红石回调。没有把指示灯当成电池内部电量，也没有改成新的红石逻辑。

## 保持性与验证

四源229→281行。Registry两端各把新helper反向展开成原精确表达式后，与旧完整代码流对应；其它源在明确局部名称归一后，六份完整native／core视图的有序代码和其它注释保持。原两处属性表达式与两平台helper完整正文分别核对。不是原始整文件字节相同，也不是用行数证明整套架构已完成。

最终core与Forge1.20.1／NeoForge1.21.1 compileJava实际通过，报告1分16秒。仍使用同版本本地API适配器，不改生产Gradle配置，也不声称默认离线元数据／GitHub CI恢复。未新增镜像测试、GameTest、游戏、原版1.7.10、存档重启、打包、推送或GitHub写操作。用户WoodMaterials.java和CompoundMaterials.java摘要保持。

419仍为累计差异编号，含已修和保留政策，不是待修数量。电池盒其它运行／保存／呈现、齿轮完整世界行为及全系统／源码标准化继续，goal保持active。
