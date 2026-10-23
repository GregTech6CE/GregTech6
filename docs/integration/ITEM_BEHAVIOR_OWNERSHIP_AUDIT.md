# 原版物品行为挂载盘点与手动工具边界共用

2026-10-22，第一百二十八批，基线 `9f3c27a20`。上一批反应堆收尾共用及427／428已经提交，属于进展。本批盘点原版行为类的类外引用，核实泡沫喷罐缺口，同时共用两端重复的工具边界。12原版／13当前重点Java、24段引用、五helper及四实际生成输入见[回执](verification/item-behavior-ownership-audit-20261022.json)。原树只读，保留GregTech-6 Team／Gregorius Techneticies的LGPL来源；未导入或执行原版代码。

## 429：放置用泡沫喷罐族未接入

原MultiItemRandomTools在16色循环里分别注册普通、带归属两组喷罐，各有全新／已用形态，共64种原物品形式。普通元数据1100–1131，带归属1132–1163。两组都挂载Behavior_Spray_Foam，构造参数256、内部乘10保存用量；行为提供潜行模式切换、颜色／归属和泡沫放置。这里只列明参数合同，不将其简化成所有模式固定“喷256次”。

当前原多物品表、技术物品表及对应实际工厂没有提供这一喷罐族；行为目录与ItemBehaviors只接泡沫去除剂、硬化剂等分派，没有放置／切换模式的泡沫喷罐入口。PaintSprayItem处理染色，不是泡沫喷罐。1828份所列维护Java的相关标识定位命中49份泡沫资料，另读实际目录／工厂／回调确认，未以类名搜索未命中独自作结论。

登记429，限原专用喷罐族／物品行为；现有湿／干泡沫方块、半砖、归属数据和硬化／去除功能不报成全部缺失，导线包覆仍单列078。没有执行实际注册表、游戏放置或补齐功能。

## 30类引用清单不等于30类移植验收

新增只读工具 `tools/integration/inventory_original_item_behaviors.py`，复用既有Java词法视图，排除注释／import，把构造、继承／类型引用与反射字符串分开。读取原1229份Java后，目录30个类中23个有类外代码引用、两个只有字符串线索、五个未发现类外引用，共55处代码引用。闭合输入身份与全部行见[原始盘点](verification/original-item-behavior-ownership-20261022.json)。动态反射、可选依赖、真实挂载仍须逐项审查，数字不是完成率。

下表“当前定位”是导航，除429和本轮共用helper实证外，不代表本轮重新验收了全部玩法。

| 原行为类（省略Behavior_） | 类外代码／字符串 | 当前定位与边界 |
| --- | --- | --- |
| Arrow | 1／0 | 原仅有继承引用，不证明已挂载；材料箭沿MaterialArrowItem／Entity及002继续。 |
| Arrow_Potion | 0／0 | 未找到类外挂载，不直接建立基础功能缺失项。 |
| Builderwand | 1／0 | BehaviorBuilderWand；多方块专用目标等233另列。 |
| Chunk_Remover | 1／0 | BehaviorChunkEraser，完整调度另核。 |
| Cropnalyzer | 3／0 | BehaviorCropnalyzer，可选作物接口另核。 |
| DataOrb | 0／0 | 原有旧数据编解码，不据类存在就认定有活跃数据球物品。 |
| DataStorage | 4／0 | BehaviorDataStorage／USB资料，既有专题边界保留。 |
| DataStorage16 | 4／0 | BehaviorDataStorage16／USB盘资料，保存与交换另核。 |
| Duct_Tape | 3／0 | BehaviorDuctTape；134／135等既有差异保留。 |
| FlintAndTinder | 1／0 | BehaviorFlintAndTinder；127／133等另列。 |
| Gun | 5／0 | GunToolItem等实际武器入口，非按同名类判断。 |
| Key | 10／0 | GTDungeonKeyItem／锁逻辑，非仅目录项。 |
| Lighter | 7／0 | BehaviorLighter，耗材与点火已有专题。 |
| Place_Dynamite | 1／0 | BehaviorPlaceDynamite及手钻工具，世界准入另核。 |
| Plunger_Essentia | 0／1 | 原反射字符串拼写／包名与此文件不同，旧兼容来源须另核。 |
| Plunger_Fluid | 1／0 | BehaviorPlungerFluid；251等宿主分派差异保留。 |
| Plunger_Item | 1／0 | 原确实挂载，但主要动作注释掉，实际回调返回false；不报缺少整套清物管功能。 |
| Remote | 2／0 | BehaviorRemote／原生物品，330／426等保留。 |
| Scanner | 2／0 | BehaviorScanner／ScannerItem，诊断来源等另核。 |
| Scoop | 0／1 | 反射确实指向本类，依赖Forestry蝴蝶接口；不能因无直接类名代码引用而称未挂载。 |
| SensorKit | 0／0 | 未找到类外引用，主要核控制接口代码也被原版注释。 |
| Sonictron | 0／0 | 未找到类外引用，原声音／设备交换主体也被注释。 |
| Spray_Color | 1／0 | PaintSprayItem，颜色／目标已有专题。 |
| Spray_Color_Remover | 1／0 | BehaviorSprayColorRemover，代理目标等273另列。 |
| Spray_Extinguisher | 1／0 | BehaviorSprayExtinguisher，实体与宿主另核。 |
| Spray_Foam | 2／0 | 本批确认专用喷罐缺口429。 |
| Spray_Foam_Hardener | 1／0 | BehaviorSprayFoamHardener，不等于已有放置喷罐。 |
| Spray_Foam_Remover | 1／0 | BehaviorSprayFoamRemover，包覆宿主078另列。 |
| Watering_Crops | 0／0 | 原实现依赖IC2作物接口，未找到类外挂载；外部使用仍待核。 |
| Worldgen_Debugger | 1／0 | BehaviorWorldgenDebugger，生成入口另核。 |

## 五个相同helper共用，公开类型不混并

ManualToolBehaviorAccess从Neo专用目录移到共享原生目录，包名／公有类名保持。ItemBehaviors的obstructed、creative、mayEdit、lightVanillaFire、primeTnt统一转给它；五段完整原Forge正文与原／新共享正文分别比对，Neo原来已经这样转发。遮挡四常量只保留共享定义，原说明搬过去；ItemBehaviors公开TNT_COST改为同值编译期别名，仍为10000L，移除四个不再使用的导入。

ItemBehaviors与ManualToolBehaviorAccess各自的Outcome／Ignitable仍是原公开类型，未把它们混成一个类型。Forge的igniteToolClick继续匹配自己的目标接口，不纳入这五项委托，避免“同形接口”误当同一身份。也没有修改权限、遮挡、点火、消耗品、已记录的078／127／132／135／251等玩法差异。

两源866→832行，四完整平台视图／五方法正文与注释受控保持；四实际Stonecutter输入等于当前投影。实际双版compileJava通过，1分57秒，使用未变的同版本本地API临时适配；生产构建配置和用户材料保持，不声称默认离线／CI恢复。迁移manifest是冻结历史，现行目录按实际共享／版本源码发现，没有改写旧基线。

未新增镜像测试、执行Minecraft方法、GameTest、游戏／客户端、存档重启、打包、推送或GitHub写操作。429是累计差异编号，含已修与策略，不是未修复数量。其它通用gregapi行为、全部物品／机器、可选兼容、实际运行与源码标准化继续，全面goal保持active。
