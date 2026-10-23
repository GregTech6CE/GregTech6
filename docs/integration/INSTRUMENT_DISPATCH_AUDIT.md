# 手持仪表分派与反应堆读数时序核查

2026-10-22，第一百二十七批，基线 `9a3725f07`。上批遥控包装共享及425／426已提交，属于进展。本批继续核对仪表物品与机器两端的实际消费者，同时共用反应堆每世界tick的收尾业务。八原版／17当前Java、31段引用、两实际原生事件条目及六生成输入见[回执](verification/instrument-dispatch-audit-20261022.json)。原树只读，保留GregTech-6 Team／Gregorius Techneticies的LGPL来源；原生实现仅阅读，没有导入生产代码。

## 427：电力计、转速计未接原手持读数链

原MultiItemRandomTools的10003／10004分别安装电力计／转速计的Behavior_Tool；该行为经IBlockToolable分派实际工具点击。原电线对应分支返回mWattageLast EU/t，轴返回mTransferredLast RU/t，齿轮箱另有转速计分支。因此两者原本不只是合成传感器的配件。

当前两个ID都已注册，也被传感器制作行引用；但Forge通用物品和Neo MultiBehaviorItem实际调用的ItemBehaviors.useOn没有这两类分支，未识别ID最终拒绝专用行为。当前工厂没有这两件的专用Item适配，GTToolType也无对应类型。1766份所列维护生产目录Java的关键字定位只命中目录／属性／制作／独立传感器资料，另读完整实际回调、工厂和分派来确认，未凭搜索未命中就下结论。

登记427，范围是内置手持测量功能链缺失，不是物品没有注册、完全没有配方、独立传感器不能工作，或机器拿任意物品右键永远不显示状态。外部Addon可能补充的功能没有算作内置实现，也没有进行游戏右键验收。

## 428：停机后盖革计读数立即清零的策略差异

原父类软锤分支和setStateOnOff只修改mStopped，不清除oNeutronCounts；手持盖革计读该旧计数数组，后续由正常反应／刷新路径改变它。当前软锤经ReactorInteraction调用setStopped；该方法在停机时立即清零neutrons，盖革计正好读取这个数组。

有限源码操作序列：已有一个运行中单棒堆芯，读数16；调用停机，然后在进一步tick／刷新之前读取。原返回16且显示关闭，当前返回0且显示关闭。这里没有运行游戏、塞入伪造测试实体或断言玩家实际可感知多长时间的延迟，不把局部操作序列扩大成整个网络采样故障。

登记为停机／读数时序策略差异，保留当前立即清零。原运行中采样的完整相位、委托宿主和实际世界调度仍继续核查，119的上限缺口也没有因此解决。

## 排除：反应堆温度计不应改读剩余储热

初查容易把原oEnergy当成mEnergy的旧快照。实际两种原堆芯都先保存tEnergy=mEnergy，再反应、应用冷却剂产热除数，随后设置oEnergy=mEnergy−tEnergy，之后才处理冷却。原温度计输出oEnergy，表示本次产热，不是冷却后剩余储热。

当前lastHeat同样是本次反应产热并应用heatDivider，storedHeat才是独立累计储热；温度计读lastHeat的数值类别对应。未新增“缺少剩余储热读数”问题，也没有把它改成storedHeat。这里核的是读数字段含义，不代表所有冷却剂公式、文本或温度计宿主已全部验收；085／267等既有宿主差异保留。

## 收尾业务共用与可读性

ReactorNetwork两端事件入口只保留事件类型、阶段和客户端门控，余下收尾集中为private finishTick(Level)：先移出pending条目、无条目即返回，再过滤已移除／未加载核心、按位置排序、19／20相位交换，最后逐个反应。两段完整旧正文与新helper分别对应，未调整交换周期、加载规则、停机安全、危险处理或WeakHashMap清理策略。

原生Forge事件的level为final字段，实际Neo事件getLevel只读取其final level字段，已由本地实际通用包字节码核实。局部mod只在exchange方法内改为isModerated，避免误改api.mod包名；预览阶段曾发现这一名称冲突，生产写入前已限制作用域并为其它改名加入成员／包名位置守卫。coreEntry、rodSlot、neutronEmissions、rodModeration、neighborX／Z等名称取代c／i／emit／mods／nx／nz；ReactorInteraction的掉落／命中坐标和温度计目标／流体条目名也明确化。

三源175→241行，六完整平台视图在列明局部名与收尾抽取归一后有序代码／字面量、剩余注释保持。两个旧收尾正文和两个原生薄入口另核；六实际Stonecutter编译输入与当前投影代码一致。实际双版compileJava通过，1分43秒，使用未变的同版本本地API临时适配，生产构建配置和用户材料未改，不声称默认离线／CI恢复。

没有新增镜像测试、执行Minecraft方法、GameTest、游戏／客户端、存档重启、打包、推送或GitHub写操作。428是累计记录数，含已修与保留政策，不是未修复数量或完成率。其它仪表委托／运行采样、全部独立物品、完整系统与源码标准化继续，全面goal保持active。
