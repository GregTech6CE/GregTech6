# GregTech6 OmniOcular → Jade 对应表

更新：2026-10-26。73 条旧版方块匹配规则均已对照并接入当前 Jade 显示路径。
这是信息展示适配，不是一次机器行为重写；双版编译通过，游戏内视觉与服务器交互仍需验收。

## 资料与实现

参考用户提供的 `build/deliveries/20261025-ca4b8cd73/OmniOcular/GregTech6.xml`，
文件头标记 mordd、2024-03-01、GT6 6.17.01。
SHA256：`f970a426a70eeb60de36cda0662fcaaab033ab2bf425e766af7049d60cb8b439`。
只参考字段含义，不运行或复制旧 JS 脚本。IC2、AE2、原版 Minecraft 的其他 XML 不在本次 GT6 移植范围。

`OmniJadeProvider` 补充已有专用 Provider；`OmniMachineInfo`、`OmniEnergyInfo`、`OmniWorldInfo`
分别读取加工／手作、能源、植物／地表信息。`JadeInspection` 统一传输和客户端本地化。
服务端只读当前对象，新增机器方法是读取接口。没有更改 tick、配方搜索、随机产物、结构检测、能量传输、损耗、燃料消耗或爆炸规则。

## 适配差异与边界

- 旧脚本的数字 ID、NBT 名称、固定容量和缺字段猜测运行状态，改为当前对象的读取接口。额定值与实际交付标志分开；只有线缆／轴已有的上一 tick 计量值标为实际传输。
- 模具没有旧 `gt.connection` 状态：显示当前可输入面、自动输入面掩码及红石要求。当前浇筑口自动浇筑需要启用且收到红石；没有补写原版不存在于当前代码的模式。
- 液体泉当前实现随机尝试放置流体源，并未计量实际桶数；显示平均尝试间隔，不声称恒定 L/t。旋转泵没有旧历史状态，显示当前缓存和罐对应的等待条件。
- 大型蒸汽轮机当前没有可切换旋转方向字段，此旧字段没有伪造；单方块电动机／涡轮机仍显示真实方向及实体旋转角速度。
- 机器加工进度读取当前原生单位（普通能量加工为累积功，时间加工为 tick），不一律转换为秒。耐久同样使用原生数值，不声称每个配方都固定耗费一次。
- 待产物只展示已经抽取并保存的结果，不查询配方或再次抽奖。最多 24 堆待产物、16 个待流体；额外待物品堆数明确提示。每份补充提示最多 96 行，槽／通道序号保留。当前原生槽数在此范围内。
- 已装电池总储量与工作缓存分开；“当前无法放电”不等同于永久无用。普通、手作、热交换器与多方块流体按输入／输出角色或通道区分。
- 锅炉仪表条沿用前轮完成的连续渐变：前半灰色、50–75% 绿色、后续黄橙红；当前值左亮右暗，90% 为界面高压提醒。普通／大型锅炉分别采用实际危险阈值。

## 逐规则对应

表中的 `N` 表示原脚本重复编号的槽位或行，实际实现按槽位遍历。

| 原规则（省略 gt.multitileentity.） | 原脚本信息 | 当前入口 | 实现与区别 |
| --- | --- | --- | --- |
| `anvil.simple` | 提示、剩余耐久、输入物品N | `OmniMachineInfo` | 耐久、瞄准顶面／大端／小端、两个工件；按当前 AnvilRules 判定 |
| `smeltery` | 警告、烫伤警告、当前温度、熔炼物种数、熔炼物N-N情况 | `CrucibleJadeProvider` | 温度、熔毁警告、材料种数、各材料数量与熔融状态、缓存、接触烫伤；大型坩埚采用其不封顶的伤害规则 |
| `mold` | 当前温度、铸造模具、连通方向、自动输入、铸造材料、输出产物 | `MoldJadeProvider` + `OmniMachineInfo` | 温度／耐温、形态、材料、固化产物；接受输入的面、各面的自动输入、模具红石要求 |
| `smeltery.drain` | 自动输出 | `OmniMachineInfo` | 浇筑口启用状态；明确当前实现还要求红石信号 |
| `basin` | 当前温度、连通方向、自动输入、铸造材料、输出产物 | `MoldJadeProvider` + `OmniMachineInfo` | 温度／耐温、形态、材料、固化产物；接受输入的面、各面的自动输入、模具红石要求 |
| `tank.boiler_steam` | 爆炸警告N、烫伤警告N、工作状态、效率、热量储量、水类型、水储量、蒸汽储量 | `BoilerJadeProvider` | 状态渐变条、水／蒸汽／热量、效率、拆除／除垢／超压风险；普通与大型规则分开 |
| `engine.kinetic_steam` | 过热警告、工作状态、输出功率、动能储量、蒸汽储量 | `EngineJadeProvider` | 真实工作／交付状态、储能、冲程设定、蒸汽批次、凝结水及满缓存保护 |
| `engine.kinetic_electricity` | 工作状态、输入功率、输出功率 | `EngineJadeProvider` + `OmniEnergyInfo` | 实际控制状态、交付标志、设定输入输出和输入能量缓存 |
| `turbines.rotation_steam` | 旋转警告、工作状态、输出功率、蒸汽储量、旋转方向、旋转速度 | `TurbineJadeProvider` + `OmniEnergyInfo` | 蒸汽、输入输出范围、启停、旋转方向、快慢档、向上旋转实体提示 |
| `heater.heat_electric` | 烫伤警告、工作状态、输入功率、输出功率 | `OmniEnergyInfo` | 状态、输入／输出额定值和包范围、模式、缓存；加热器接触伤害、制冷器双路 CU／HU 输出 |
| `motors.rotation_electric` | 旋转警告、工作状态、输入功率、输出功率、旋转方向、旋转速度 | `OmniEnergyInfo` | 状态、额定值、包范围、缓存、旋转方向、实体旋转速度与提示 |
| `magnet.magnet_electric` | 工作状态、输入功率、输出功率 | `OmniEnergyInfo` | 控制状态、额定输入输出、包大小范围、模式／反向传输（设备支持时）、缓存 |
| `transformers.transformer_electric` | 工作状态、输入功率、输出功率、输送方向 | `OmniEnergyInfo` | 控制状态、额定输入输出、包大小范围、模式／反向传输（设备支持时）、缓存 |
| `energystorages.battery_electric` | 工作状态、输出功率、可用电池、无用电池、总存电量、存电上限 | `OmniEnergyInfo` | 已装电池、可放电数、当前不能放电数、可供能量包、电池总储量和容量、独立工作缓存 |
| `energystorages.battery_electric_large` | 工作状态、输出功率、可用电池、无用电池、总存电量、存电上限 | `OmniEnergyInfo` | 已装电池、可放电数、当前不能放电数、可供能量包、电池总储量和容量、独立工作缓存 |
| `solarpanel.electric_8eu` | 遮盖警告、工作状态、电能储量 | `OmniEnergyInfo` | 启停、实际发电／交付状态、额定值、最近天空检测和本 tick 未输出能量；不是蓄电池 |
| `laser.laser_electric` | 工作状态、输入功率、输出功率 | `OmniEnergyInfo` | 控制状态、额定输入输出、包大小范围、模式／反向传输（设备支持时）、缓存 |
| `dynamo.electric_rotation` | 工作状态、输入功率、输出功率 | `OmniEnergyInfo` | 控制状态、额定输入输出、包大小范围、模式／反向传输（设备支持时）、缓存 |
| `quantumenergizer.quantum_laser` | 工作状态、输入功率、输出功率 | `OmniEnergyInfo` | 控制状态、额定输入输出、包大小范围、模式／反向传输（设备支持时）、缓存 |
| `energystorages.crystal_laser` | 工作状态、输出功率、可用电池、无用电池、总存电量、存电上限 | `OmniEnergyInfo` | 已装电池、可放电数、当前不能放电数、可供能量包、电池总储量和容量、独立工作缓存 |
| `generator.motor_liquid` | 工作状态、剩余能量、燃油储量、尾气储量 | `EngineJadeProvider` | 燃油引擎状态、RU、燃料、尾气、实际端口 |
| `generator.hot_fluid` | 工作状态、剩余热量、热流体储量、废液储量 | `OmniMachineInfo` | 单方块热交换器转化状态、HU、额定输出、热流体输入和冷流体输出 |
| `energystorages.crystal_laser_large` | 工作状态、输出功率、可用电池、无用电池、总存电量、存电上限 | `OmniEnergyInfo` | 已装电池、可放电数、当前不能放电数、可供能量包、电池总储量和容量、独立工作缓存 |
| `laserabsorbers.electric_laser` | 工作状态、输入功率、输出功率 | `OmniEnergyInfo` | 控制状态、额定输入输出、包大小范围、模式／反向传输（设备支持时）、缓存 |
| `cooler.cryo_electric` | 工作状态、输入功率、输出功率 | `OmniEnergyInfo` | 状态、输入／输出额定值和包范围、模式、缓存；加热器接触伤害、制冷器双路 CU／HU 输出 |
| `connector.pipe.fluid` | 内容物 | `FluidVesselJadeProvider` | 所有非空通道的流体、存量、容量；通道数、额定带宽、耐温 |
| `generator.burning_liquid` | 工作状态、剩余热量、燃料情况 | `OmniMachineInfo` | 燃烧状态、剩余热量、额定输出、缓存燃烧时长、燃料／辅助流体／灰烬；不硬编码方解石容量 |
| `generator.burning_gas` | 工作状态、剩余热量、燃料情况 | `OmniMachineInfo` | 燃烧状态、剩余热量、额定输出、缓存燃烧时长、燃料／辅助流体／灰烬；不硬编码方解石容量 |
| `generator.burning_brick` | 工作状态、剩余热量、剩余燃烧时间、灰烬情况 | `BurningBoxJadeProvider` + `OmniMachineInfo` | 燃烧状态、热量、额定输出、缓存燃烧时长、燃料与灰烬 |
| `multiblock.distillationtower` | 工作状态、处理进度、输入流体、输出流体N | `BasicMachineJadeProvider` + `OmniMachineInfo` + `MultiblockJadeProvider` | 实际工作／进度／配方能耗、并行数、物品输入输出、已确定待产物、蓝图、各输入输出流体；多方块追加结构状态 |
| `multiblock.crucible` | 当前温度、熔炼物种数、熔炼物N-N情况 | `CrucibleJadeProvider` | 温度、熔毁警告、材料种数、各材料数量与熔融状态、缓存、接触烫伤；大型坩埚采用其不封顶的伤害规则 |
| `multiblock.cryodistillationtower` | 工作状态、输入流体、输出流体N、处理进度 | `BasicMachineJadeProvider` + `OmniMachineInfo` + `MultiblockJadeProvider` | 实际工作／进度／配方能耗、并行数、物品输入输出、已确定待产物、蓝图、各输入输出流体；多方块追加结构状态 |
| `generator.burning_fluidbed` | 工作状态、剩余热量、剩余燃烧时间、熔融方解石供应、燃料情况、灰烬情况 | `OmniMachineInfo` | 燃烧状态、剩余热量、额定输出、缓存燃烧时长、燃料／辅助流体／灰烬；不硬编码方解石容量 |
| `pump.rotation` | 工作状态、流体存量、能量缓存 | `OmniEnergyInfo` | RU、罐内容、等待输出／已储能待搜索；不伪造旧 active.data 历史状态 |
| `generator.burning_solid` | 工作状态、剩余热量、剩余燃烧时间、剩余燃料、灰烬情况 | `BurningBoxJadeProvider` + `OmniMachineInfo` | 燃烧状态、热量、额定输出、缓存燃烧时长、燃料与灰烬 |
| `fluid.spring` | 生成速度、流体种类 | `OmniWorldInfo` | 流体种类、激活状态、当前随机尝试生成流体源的平均间隔 |
| `tank.barrel.*` | 流体存量 | `FluidVesselJadeProvider` | long 存量／容量、输出模式、密封／发酵进度和耐温；流体名称复用 Jade 原生显示 |
| `multiblock.tank.*` | 流体存量 | `MultiblockJadeProvider` | 结构、流体、long 存量和容量 |
| `multiblock.implosioncompressor` | 工作状态、工作进度、输入物品N、正在加工、输出物品N | `BasicMachineJadeProvider` + `OmniMachineInfo` + `MultiblockJadeProvider` | 实际工作／进度／配方能耗、并行数、物品输入输出、已确定待产物、蓝图、各输入输出流体；多方块追加结构状态 |
| `cell` | 流体存量 | `OmniMachineInfo` | 读取放置容器的物品副本能力，显示内容和容量 |
| `cup` | 流体存量 | `OmniMachineInfo` | 读取放置容器的物品副本能力，显示内容和容量 |
| `jug` | 流体存量 | `OmniMachineInfo` | 读取放置容器的物品副本能力，显示内容和容量 |
| `measuring.pot` | 流体存量 | `OmniMachineInfo` | 读取放置容器的物品副本能力，显示内容和容量 |
| `thermos.can` | 流体存量 | `OmniMachineInfo` | 读取放置容器的物品副本能力，显示内容和容量 |
| `bathing.*` | 输入物品N、输入流体、输出物品N、输出流体N | `OmniMachineInfo` | 每个输入／输出物品槽和流体槽；手作工具不冒充持续运行机器 |
| `mixing.*` | 输入物品N、输入流体N、输出物品、输出流体N | `OmniMachineInfo` | 每个输入／输出物品槽和流体槽；手作工具不冒充持续运行机器 |
| `juicer` | 输出流体 | `OmniMachineInfo` | 每个输入／输出物品槽和流体槽；手作工具不冒充持续运行机器 |
| `sapbag` | 收集物品 | `OmniWorldInfo` | 已收集物品、流体、存量和容量 |
| `heater.heat_flux` | 工作状态、输入功率、输出功率 | `OmniEnergyInfo` | 状态、输入／输出额定值和包范围、模式、缓存；加热器接触伤害、制冷器双路 CU／HU 输出 |
| `motors.rotation_flux` | 工作状态、输入功率、输出功率、旋转方向 | `OmniEnergyInfo` | 状态、额定值、包范围、缓存、旋转方向、实体旋转速度与提示 |
| `magnet.magnet_flux` | 工作状态、输入功率、输出功率 | `OmniEnergyInfo` | 控制状态、额定输入输出、包大小范围、模式／反向传输（设备支持时）、缓存 |
| `laser.laser_flux` | 工作状态、输入功率、输出功率 | `OmniEnergyInfo` | 控制状态、额定输入输出、包大小范围、模式／反向传输（设备支持时）、缓存 |
| `engine.kinetic_flux` | 工作状态、输入功率、输出功率 | `EngineJadeProvider` + `OmniEnergyInfo` | 实际控制状态、交付标志、设定输入输出和输入能量缓存 |
| `cooler.cryo_flux` | 工作状态、输入功率、HU输出功率、CU输出功率 | `OmniEnergyInfo` | 状态、输入／输出额定值和包范围、模式、缓存；加热器接触伤害、制冷器双路 CU／HU 输出 |
| `multiblock.drill.bedrock` | 警告、润滑油储量、能量储量 | `MultiblockJadeProvider` | 结构、RU、润滑油及容量 |
| `multiblock.boiler.steam` | 警告、爆炸警告N、烫伤警告、工作状态、效率、热量储量、水类型、水储量、蒸汽储量 | `BoilerJadeProvider` | 状态渐变条、水／蒸汽／热量、效率、拆除／除垢／超压风险；普通与大型规则分开 |
| `multiblock.turbine.steam` | 警告、工作状态、输出功率、蒸汽储量、旋转方向 | `MultiblockJadeProvider` + `OmniEnergyInfo` | 结构、实际工作／交付标志、额定值、缓存与流体；当前大型轮机没有旧脚本中的可切换旋转方向 |
| `multiblock.dynamo` | 警告、工作状态、输入功率、输出功率 | `MultiblockJadeProvider` + `OmniEnergyInfo` | 结构、实际工作／交付标志、额定值、缓存与流体；当前大型轮机没有旧脚本中的可切换旋转方向 |
| `multiblock.turbine.gas` | 警告、工作状态、燃料情况 | `MultiblockJadeProvider` + `OmniEnergyInfo` | 结构、实际工作／交付标志、额定值、缓存与流体；当前大型轮机没有旧脚本中的可切换旋转方向 |
| `funnel.dust` | 输出模式、物品存量、准备输出、正在输出 | `DustFunnelJadeProvider` + `OmniMachineInfo` | 模式、余量材料／数量、不足量提示、输入槽、目标形态、输出槽 |
| `sharpener.grindstone` | 工作状态 | `OmniMachineInfo` | 磨石磨料次数／手动进度；筛选台输入、12 个输出、等待操作／取走产物状态 |
| `sifter.table` | 工作状态、输入物品、输出产物N | `OmniMachineInfo` | 磨石磨料次数／手动进度；筛选台输入、12 个输出、等待操作／取走产物状态 |
| `multiblock.cokeoven` | 工作状态、输入警告、工作进程、正在加工、输入物品、输出物品、输出流体 | `BasicMachineJadeProvider` + `OmniMachineInfo` + `MultiblockJadeProvider` | 状态、结构、进度、已确定待产物、输入输出、杂酚油、点火和配方／空间提示 |
| `rock` | 所含物品 | `RockTwigJadeProvider` | 直接复用实际掉落物列表；包含物品模式、数量和基岩矿提示矿块 |
| `plant.bush` | 生长进度、光照提示、雨天提示、产出作物 | `OmniWorldInfo` | 阶段与计数器进度、成熟、实际光照／雨水条件、收获物和数量范围 |
| `machine.basic` | 工作状态、工作进程、能量需求、正在加工、物品输入、所放蓝图、流体输入、物品输出、流体输出 | `BasicMachineJadeProvider` + `OmniMachineInfo` + `MultiblockJadeProvider` | 实际工作／进度／配方能耗、并行数、物品输入输出、已确定待产物、蓝图、各输入输出流体；多方块追加结构状态 |
| `machine.basic.electric` | 工作状态、工作进程、能量需求、正在加工、物品输入、所放蓝图、流体输入、物品输出、流体输出 | `BasicMachineJadeProvider` + `OmniMachineInfo` + `MultiblockJadeProvider` | 实际工作／进度／配方能耗、并行数、物品输入输出、已确定待产物、蓝图、各输入输出流体；多方块追加结构状态 |
| `tree.*.*` | 工作状态 | `OmniWorldInfo` | 当前树洞树脂／树液是否可采集；不扫描树冠、不触发生长 |
| `multiblock.*` | 警告 | `MultiblockJadeProvider` | 当前结构追踪结果 |
| `generator.reactor.core.*` | 冷却介质、热流体输出、工作状态、热量输出、燃料棒N、中子数 | `ReactorJadeProvider` + `OmniMachineInfo` | 输入冷却剂／输出热流体、开关、热量、每个反应堆棒及中子数 |
| `multiblock.fusionreactor` | 配方需求能量、能量输出、配方进度、正在加工 | `MultiblockJadeProvider` + `BasicMachineJadeProvider` + `OmniMachineInfo` | 启动 LU、EU 输出、独立执行器的进度与已确定产物、实际输入输出；不读取基础执行器的空待产物 |
| `connector.wire.electric` | 功率 | `OmniEnergyInfo` | 上一 tick 实际传输；线缆额定电压、电流、损耗、过载积热；轴包大小和包数上限 |
| `connector.axle.rotation` | 功率 | `OmniEnergyInfo` | 上一 tick 实际传输；线缆额定电压、电流、损耗、过载积热；轴包大小和包数上限 |

## 锅炉仪表条布局（2026-10-26 修正）

- 压力渐变条替代 Jade 默认蒸汽罐条，保留真实水种类的水条；普通锅炉与大型锅炉共用。空罐也分别显示，不依赖默认流体聚合顺序。
- 仪表条高度为 14 GUI 像素，与标准流体条一致；按 Jade 传入的右边界减去左坐标计算宽度，避免把右下角坐标误作宽高。
- 预热时左半段为灰色；状态为 running 时改为绿色，与中段绿色连续。绿色主体区间从 50–75% 缩为 50–70%，绿色到黄色的过渡整体提前 5 个百分点（黄色在 80%）；高压渐变、明暗填充和安全警告保留，不改锅炉运行逻辑。

## 物品图标与加工分组（2026-10-26 修正）

- 输入、输出、加工中／待输出、蓝图各自显示标题和物品图标，每行最多 8 个，图标保留数量。流体按输入／输出／待产物分组，附图标与数量。
- 加工中／待输出优先于库存显示；读取开工时已经确定的产物，不重新抽取概率、不推算配方，也不要求输出槽已有物品。断电暂停、堵塞及重载后的待产物沿用实际任务缓存。
- 已绑定的多方块外壁／接口通过现有只读 sensorTarget 读取主控，不扫描或重建结构；没有绑定时不伪造主控数据。
- 分组接管机器库存和流体的默认 Jade 展示，避免另一组无分类内容重复堆叠；关闭 Omni 显示后保留默认展示。

## Omni 显示层参考与格式（2026-10-26）

另下载上游 [exzhawk/OmniOcular](https://github.com/exzhawk/OmniOcular/tree/182d40a5bd8ca15361aeab59506e22dbdf414ad1) 至本地 `work/references/OmniOcular`，仅供源码对照，不执行旧脚本，也不作为模组依赖打包。

- `handler/JSHandler.java` 的 `getBody` / `setSpecialChar` 处理名称、TAB、ALIGNRIGHT、颜色、心形符号；用户的 `OmniOcular.xml` 指定左名称／右值。现在通过 Jade 对齐 API 实现通用详情行，状态与能量单位用 Component 样式，避免把旧 Waila 控制码直接塞入新界面。
- 用户 `GregTech6.xml` 的 `isRun` / `Unit` 对应运行绿、待机蓝、停机红，以及 EU 蓝、RU 绿、KU 深绿、HU 红等单位颜色。输入／输出／待产物分组各自着色。
- 上游 `name()` 实际解析为物品显示名；物品与流体的真实图标由当前 Jade 原生元素绘制，不依赖旧版整数物品 ID。空物品／空流体不制造无效图标。
- 爆炸强度、实体影响半径、接触／除垢伤害先格式化为 1 位小数，再通过 `%s` 本地化，避免现代 Minecraft 将数字格式改写后丢失精度。风险计算与机器逻辑不变。

## 验证记录

- 最终格式修正双版编译通过：`work/jade-omni-format-20261026.log`（1m41s）。随后绿色范围常量调整通过独立 Java 编译、渐变锚点／连续性与 115 项仪表边界检查。

- 紧凑仪表条及图标分组：双版 `compileBothPlatforms` 通过，日志 `work/jade-icons-gauge-20261026.log`（1m33s）；15 组不同坐标／填充率的绘制边界检查通过，预热灰色／运行绿色／高压红色检查通过。
- 当前 83 条 Omni 中英文键无重复、占位符一致。单方块洗矿厂共用 BasicMachine 的开工产物缓存；本轮未启动游戏复现用户产线。

- Forge 1.20.1 与 NeoForge 1.21.1 的 Java 编译通过。日志：`work/omni-jade-complete-20261026-final.log`（1m32s）。检查了两版生成源码和类文件包含最新变更。
- 79 个静态及动态 Omni 引用键均能在 `en_us`、`zh_cn` 解析；新增命名空间键占位符一致，两份 JSON 无重复键。
- 相关修改的 `git diff --check` 通过。此前锅炉边界／渐变定向检查记录保留于 `docs/OMNIOCULAR_JADE.md` 的历史版本。
- Omni 移植后的双版 JAR 已构建交付；上述紧凑仪表条修正另做双版编译检查。没有运行全量测试或启动客户端；仍需在游戏内确认长内容提示排版、不同语言、单人／专服同步及实际机器样本，编译通过不替代这些验收。
