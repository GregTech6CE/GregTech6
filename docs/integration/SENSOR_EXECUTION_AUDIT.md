# 独立传感器的采样与模式执行核查

日期：2026-10-19；基线 `b9b11884d`。原版参考 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java`，Gregorius Techneticies / GregTech-6 Team，原始 LGPL 文件只读。

本批检查独立传感器基类、比例模式、堆叠计、世界流体入口和极大上限，不等于手持 Sensor Kit、全部传感器类别或所有实际设备验收。已有温度／进度接口缺口继续沿用既有编号。

## ALIGN-113：独立传感器的比例红石分档错误

原版 MultiTileEntitySensorTE 的 SCALE 和 PERCENT 输出调用 UT.Code.scale：输入不大于零时为 0，达到上限时为 15，中间正值为 `1 + value * 14 / maximum`。比例模式的 maximum 使用用户阈值；百分比模式先计算整数百分比，再以 100 为上限计算红石。

当前 SensorControl.scaled 使用 `value * 15 / maximum` 向下取整，并在 maximum<=0 时直接返回 0。它可用于普通数值比例，却不等于原版这个红石分档函数。

| 条件 | 原版红石 | 当前红石 |
| --- | --- | --- |
| 百分比 1% | 1 | 0 |
| 百分比 50% | 8 | 7 |
| 比例模式，测量 50，阈值 100 | 8 | 7 |
| 比例模式，正测量 1，阈值 0 | 15 | 0 |

Java 17 直接执行当前两个纯规则类并与原版表达式比较，四行结果已观察。不能把修复范围扩展到所有面板的 scale：上一批 CoverScaleProgress／CoverScaleEnergy 使用另一个原版公式，半量输出 7 是正确的。

## ALIGN-114：堆叠计错误地按 64 个物品折算

原版 MultiTileEntityStackometer 统计可访问槽位中的非空堆数；单个槽里 1 个物品和 64 个物品均记一堆，排除原版流体展示物品。上限为可访问槽数。

当前 STACKOMETER 把所有 stack.getCount 相加，除以 64；上限也把 slotLimit 相加后除以 64。一槽只有 1 个物品时显示 0，而原版显示 1；两个槽各 32 个时当前显示 1，原版显示 2。对于槽上限为 1 的库存，当前最大值也可能被除成 0。

这些是实际 switch 分支的数量推导，没有启动游戏。现代 IItemHandler 可访问槽语义和原版 ISidedInventory 的 SIDE_ANY 回退、流体展示物品的识别差异仍要继续核对，不能只将除法改掉就宣称完全对齐。

## ALIGN-115：流体／桶量计缺少世界流体回退

原版 Fluidometer 在不能从目标 IFluidHandler 读量时，继续查看世界方块：原版水／岩浆源返回 1000 L，流动版本返回 0；IFluidBlock 则模拟 drain 获取量。Bucketometer 对应源方块返回 1 m³。

当前 FLUID、BUCKETOMETER、KILOBUCKETOMETER 只在 target!=null 的 switch 中查询方块实体能力。普通水源／岩浆源没有方块实体，因此不会进入分支，newValue 保持 0；后续只有亮度、玩家数和时钟的无实体测量，不补流体读取。

以 DISPLAY 模式对准水源时，源码预期原版流体计 1000、桶量计 1，当前均为 0。本批未放置世界测试。其他模组的世界流体应按现代流体源／模拟抽取能力处理，不能为补回读数而实际排空方块。

## ALIGN-116：采样值与最大值钳制范围不一致

原版基类在计算模式前同时对 getCurrentValue 和 getCurrentMax 执行 bindInt。当前 SensorAverage.sample 将测量值限制为有符号 int，但大多数分支的 maximum 保留 long；只有 PROGRESS 显式钳制两者。

因此大上限的百分比、比例和 FULL 判断会出现与原版不同的基准。当前 core WireCatalog 声明 1x 超导导线额定电压 8589934592、安培 4，上限乘积为 34359738368。将一次满额流量作为合成输入：当前样本钳制到 2147483647、上限仍为 34359738368，FULL 输出 0；原版两者都钳制到 2147483647，FULL 输出 15。第五行纯规则观察确认了这种数值结果，不等于实际搭建了满额超导网络。

保留更多精度可以作为现代改进，但应同时处理样本和上限，不能让一边 int、一边 long 后仍宣称百分比和满量合同对应。此次只记差异，没有改变取值。

## ALIGN-117：普通传感器从逐 tick 后采样降为十 tick 一次

原版 MultiTileEntitySensorTE.getTickRate 默认返回 1，onServerTickPost 在每次服务器后 tick 采样；传感器子类目录只有 TPSmeter 重写为 20。当前 SensorBlockEntity.serverTick 统一要求 level.gameTime % 10==0，然后在实体 tick 内 measure。

普通仪表的更新频率因此降低十倍，移动平均 N 个样本对应的实际时间也变为十倍；短于十 tick 的脉冲可能没有落入采样点。来自线缆／轴的上一轮传输值还受实体 tick 执行先后影响，不能只看 measure 的返回值判断运行中采样完整。

TPS 自己的 wall-clock 样本仍每 20 tick 更新，但显示／平均管线可在中间十 tick 点重复加入同一 TPS 值，不能说 TPS 的全部采样流程完全保持原版。原版 TPS 的 wall-clock 数学公式已有对应。本轮没有测服务器短脉冲或 tick 调度。

## ALIGN-085 补充：温度计的环境回退也缺失

原版 Thermometer 优先 ITileEntityTemperature，随后有旧 IC2 反应堆条件兼容，最终返回目标坐标的 WD.temperature 环境温度。当前仅识别坩埚，无目标实体或其他目标保持 0。这是既有温度接口缺口的补充，不再新增编号；旧 IC2 条件兼容不能直接要求现代版本提供同一个接口。

## 已有实现与待验边界

- 八种模式身份、比较阈值、十进制／十六进制按钮步长、最大 32767 个移动平均样本已有实现。
- SensorAverage 用 long 求和、整数除法，存取样本数组与下一索引；保存／读取入口存在，实际重启连续性未验。
- 独立输入面可配置且不能等于显示面，多方块部件有 sensorTarget 解引用；未以同名接口证明每个部件绑定都正确。
- 原始值会进入整数样本，FULL／NOT_FULL 的显示符号在 SixCellDisplay 中另行构造，不应仅看 displayed 字段就判画面不符。
- 原版 DISPLAY 沿用既有红石变量，而当前 evaluate 返回 0；常规切换模式会先清零，尚未建立普通交互可观察的差异，暂留边界而不报成已复现故障。
- 测重材料解析与原版 OM.weight、特殊库存／长期存储计数、红石输出面、全部资源与手持工具仍待继续。

## 可读性与验证

展开 SensorPanelRules 的布局、单位和颜色表，SensorAverage 的环形窗口状态更新，SensorControl 的按钮步长；修正 SensorBlockEntity 对原版采样频率和直接 GameTest 覆盖范围的说明，注明当前堆叠计及上限边界。四份生产源码相对基线保持执行代码／字面量相同，原生文件分别投影两版，共五份比较通过。

Java 17 仅编译三份无 Minecraft 依赖的 core 类和 SensorModeAudit，运行五个原版对比观察，共约 1.5 秒。未原生编译、完整测试、GameTest、游戏世界、保存重启、构建或推送。见 [来源、输出和验证回执](verification/sensor-execution-audit-20261019.json)。
