# 泵、传送带、机械臂与排水口覆盖板核查

日期：2026-10-19；基线 `2112f71fe`。原版来源根目录 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java`。主要来源为 CoverPump、CoverConveyor、CoverRobotArm、CoverDrain，以及 MultiItemTechnological、FL.move、WD.infiniteWater。原作者 GregTech-6 Team / Gregorius Techneticies；LGPL 原始文件只读，现有来源头保留。

本轮核对核心档位、方向、时钟和排水入口，不代表所有覆盖板、宿主侧面权限、过滤器、回滚和兼容行为已验收。

## 已对应的部分

- 原版十档泵吞吐公式 `250 << (2*i)`、传送带／机械臂周期 `max(1, 512 >> i)` 与当前 ComponentCoverRules 一致。十档范围是原版 MultiItemTechnological 循环 i<10，不扩展到任意非法 tier。
- 泵周期 20、相位 5；物品覆盖板按各自周期相位 0；stopped 禁止执行。输入模式允许插入而禁止抽取，输出模式反向；管道上的模式固定输入。机械臂的负数槽号表示源槽、非负表示目标槽，短整数上下界处理有对应规则。
- 当前 ComponentCoverRuntime 确实调用宿主和邻接能力搬运，传送带／机械臂每次寻找一组物品；不是只有目录和提示。宿主是否始终提供原版对应侧面权限，仍需逐类核对。
- 排水口已有普通源方块全量接收后移除、重力方向、雨水、经验球及原版动物的产液路径。保留了不删除含水固体方块的现代安全限制。三种 GT 水的 16000 批量路径及产物转换已有实现：河水→普通水、海水→海水、沼泽水→脏水，且不移除这些世界水方块。
- 经验球路径处理现代合并经验球的数量；旧机器注释仍说 XP／污水未移植，已纠正。OpenBlocks 玩家蹲下交经验和部分外部生物兼容仍不完整，不能因此否定已有经验球／动物实现。

## ALIGN-098：泵没有原版的“仍有进展就继续搬运”循环

原版 CoverPump.onTickPre 在当次吞吐预算内循环调用 FL.move，只要上一轮实际搬运大于零且预算仍有余量，就继续。FL.move_ 的单次过程模拟抽取、向目标填充、按成功数量执行抽取；泵外层负责多轮调用。

当前 ComponentCoverRuntime.moveFluid 只遍历源 handler 的每个 tank 一次。一次调用若只成功一部分，预算减去该量，直接进入下一个 tank，不重新尝试同一 tank。

条件例子：只有一个源槽，存量充足，目标每次 fill 至多接收 1000，当次泵预算 4000；原版可循环四次，当前单次只搬 1000。该例要求每次调用限额、但同一操作内允许多次的 handler，没有声称某个内置机器已经实测如此。普通能一次接收完整预算的罐不受此例影响。

当前仍共享多槽总预算，并对实际接收减少尝试返还；本项不是“泵没有吞吐限制”或“泵一定丢液”。第三方不遵守模拟结果时的回滚安全另待核查。

## ALIGN-099：排水口世界取液的覆盖范围收窄

原版 WD.infiniteWater 检查水位以下 15 格至水位，并识别 BIOMES_RIVER_LAKE。该集合除河流外明确列有 Lake、Twilight Lake、Tropical Lake、Oasis、Woodland Lake 等。当前相同高度窗只检查 BiomeTags.IS_RIVER，缺少独立湖泊／绿洲映射。湖泊若恰好被外部数据包加入河流标签可工作，不能断言所有第三方湖泊必然失败。

原版还有两条独立路径：

- BlockBaseFluid 按 mQuanta 和方块元数据逐份接收，部分成功后更新剩余量及相邻流体。
- 通用 IFluidBlock 先模拟 drain，完整接收后执行其 drain 回调。

当前 drainFluidBlock 先限定为 LiquidBlock，普通分支又限定 isSource，并以固定一桶删除为 AIR；上述量子流体与自定义 drain 回调没有对应适配。这里不是要求现代版本仍实现旧 IFluidBlock 类，而是记录可处理流体种类／回调语义的缺口。三种 GT 世界水走更早的 bulk 分支，流动状态仍能供液，不在“必须源方块”的限制内。

## ALIGN-100：排水口更新响应、宿主时钟与原版不一致

原版排水判断为 `aReceivedBlockUpdate || SERVER_TIME % 20 == 5`；雨水和经验球也使用同一个全局 SERVER_TIME。当前 tickDrain 只接收数值时钟，没有方块更新标志，现有调用点没有额外的立即取液入口，因此放入相邻液体后的更新不能触发原版的即时尝试，只能等下一周期。

时钟也随宿主变化：

| 当前入口 | 时钟 |
| --- | --- |
| BasicMachineBlockEntity 的排水口／通风口 | 每个机器自己的 coverTicks，已持久化 |
| FluidPipeBlockEntity 的排水口／通风口 | 有覆盖板时推进的本地 coverTicks，未保存 |
| PanelCoverRuntime 的其他宿主附件 | level.getGameTime |
| ComponentCoverRuntime 的泵／传送带／机械臂 | 调用点传入 level.getGameTime |

因此相同 `%20`／`%100` 周期不代表与原版全局相位一致；管道重载也会重置本地相位。这里只确认时钟选择和缺少更新参数，不把同周期的相位差说成稳定运行吞吐必然变小。通风口共用这些宿主计时路径，其环境选择尚未完整核查。

旧源码以“让手动 GameTest 循环可观察”为理由宣称原版周期未变，已纠正；测试方便不能证明调度对齐。

## ALIGN-101：雨水产量取了相邻位置的气候

原版 CoverDrain 从宿主 getBiome 取 rainfall／temperature，是否接到雨则检查覆盖板前方和支撑。当前 collectRain 读取 level.getBiome(front) 的 climate 计算产量，把气候取样位置移到了相邻格。

在群系边界处，这可能改变是否满足温度 0.2／降水量大于零的条件和实际产量；例如机器在温暖湿润群系、相邻取雨格在寒冷群系，原版的气候门控可通过，当前不能通过。需要其余无遮挡／支撑条件同时成立，未在游戏中构造边界复现。

纯 rainAmount 公式和雷暴翻倍本身已有对应；不能把位置选择问题误报成公式全部错误。原版 BlockMetaType 特例、现代高度／降水类型和季节模组适配仍待进一步核查。

## 标准化和验证

展开 ComponentCoverRules 的方法／分支，以及 ComponentCoverRuntime 的多操作压行；纠正泵遍历说明、排水附件文档位置和机器／管道的时钟说明。五份生产文件代码和字面量不变，四份共享原生文件分别展开 Forge／Neo 分支核对。

Java 17 只编译两个纯规则类和 ComponentCoverRulesAudit。十档规格、0–1023 时钟内三种组件的 due／stopped、模式与槽号边界、三水转换及雨量边界共 61484 个条件通过；这些是廉价规则枚举，不是 61484 次游戏测试，也没有运行原生搬运。既有 CoverDrainRegressionTests 的源方块、含水固体、批量水与接雨检查仅阅读，未重跑。

没有原生编译、完整构建、游戏、存档重启或推送；来源哈希、规则输出及源码等价见 [回执](verification/component-cover-audit-20261019.json)。过滤器／检索器、面板状态、宿主权限、安装几何和全覆盖板清单仍在后续范围内。
