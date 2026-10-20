# 能源转换器对齐核查

2026-10-19。原版根目录为 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java`。本批核对电动机、发电机、单方块蒸汽涡轮和共享转换基类；不代表锅炉、燃料机、多方块涡轮、变压器、电池盒或整个能源网络完成验收。[源码摘要与观察回执](verification/energy-converter-audit-20261019.json)记录范围和结果。

## 确认的差异

### ALIGN-017：转换器缺少原版环境风险检查

原版 `TileEntityBase10EnergyConverter.onTick2:109` 在本机计时器每 600 tick 的第 5 tick，根据运行状态调用 `doDefaultStructuralChecks()`；非运行时才衰减过载保护次数。`TileEntityBase01Root:776–823` 按能量类型检查火、邻接液体、降雨和雷暴：火的概率为 1/10，液体无随机门，雨为 1/100，雷暴为 1/1000，水／天气不检查底面。实际破坏还受对应配置控制，不能解释为所有接水设备都会立即爆炸。

原版 `TileEntityBase06Covers:217–220` 用对应面的不透明覆盖板提供防护。`TD.Energy` 中水敏感集合为 EU/HU/CU，火敏感集合为 EU/CU，雷敏感集合为 EU。纯 STEAM→RU 蒸汽涡轮不因此自动具备同样风险。

当前 `GregTechTags` 保留这些集合，但活动 core／原生源码中没有相应消费。`EnergyNodeBlockEntity.serverTick → tickOriginalConverter` 只保留世界时间每 600 tick 的非运行过载计数衰减，没有运行环境检查；父类 `GTEnergyBlockEntity` 负责接口校验／同步，没有环境周期。覆盖板 tick 路径亦无此检查。确认转换器环境行为缺失，不将仅有属性声明算成已实现。源调用链核查，未进行游戏环境模拟。

### ALIGN-018：Flux 电动机／发电机各缺少后三档

原版 `Loader_MultiTileEntities.run:117` 调用 `unsorted2`，其 Flux 注册没有外部模组存在条件。以下六行没有进入当前 `EnergyNodeDefinitions.specifications()`；两版 `GTEnergyNodes` 都读取该目录，另加磁铁目录。活动源码也没有这些缺失档位的独立注册。

| 原版 ID | 应有设备档位 | 原版输入 → 输出 |
| --- | --- | --- |
| 11023 | Flux Motor，Electrum／HV | 2048 RF → 256 RU |
| 11024 | Flux Motor，EnderiumBase／EV | 8192 RF → 1024 RU |
| 11025 | Flux Motor，Enderium／IV | 32768 RF → 4096 RU |
| 11113 | Flux Dynamo，Electrum／HV | 512 RU → 1408 RF |
| 11114 | Flux Dynamo，EnderiumBase／EV | 2048 RU → 5632 RF |
| 11115 | Flux Dynamo，Enderium／IV | 8192 RU → 22528 RF |

原版对应 35 行（电动机 5、发电机 5、两类 Flux 各 5、蒸汽涡轮 15）；当前纯 core 枚举 29 项，存在项的输入／输出额定值及两倍输入容量全部匹配。这里的数字是指定五类目录范围，不是全模组能源设备总数，也不是 Minecraft 运行时注册表导出。缺失设备的模型、配方、材料含量与语言需一并补齐，不能只加显示条目。

## 已核对的相同行为与边界

| 项目 | 原版与当前证据 |
| --- | --- |
| 小包输入 | 原版 Root 的 `doEnergyInjection` 与当前 `EnergyBlockDefaults → EnergyGate` 都将过小 EU 包计为已消费而不入库；大小无关的 RF／STEAM 等跳过该最小值判断。不能直接绕过门函数测试 `State.inject` 后宣称低压被接受。 |
| 整包容量 | 原版 Stats 与当前 State 都允许最后一个完整包超过剩余容量。60/64 缓冲接收 16 大小的包后为 76，不是自动丢失余下 12。 |
| 停止与空载消耗 | 原版这些设备使用 WASTE_ENERGY。停机限制新输入和显示，仍转换剩余缓冲；下游拒收也按模式消耗缓冲。当前遵循该逻辑。 |
| 旋转与过载 | 当前具有电机反转、双倍输入上限、模拟注入不触发过载、输出过载前两 tick 宽限。九个独立 core 例子覆盖其中边界，未穷举所有状态。 |
| 蒸汽两刻转换 | 原版 `MultiTileEntityTurbineSteam:92–117` 与 `SteamTurbineConversion` 均先处理 pending，否则一次消耗整罐并各分配整数除以 2 的能量到当前／下一 tick。奇数蒸汽的截断属于原版行为。 |
| 蒸馏水 | 两者每 200 L 蒸汽产生 1 L 蒸馏水，余数保留；从轴侧四面向外提供，未接收部分丢弃。锅炉的 160 L 膨胀倍率不能替代涡轮 200 L 回收倍率。 |
| 槽位与方向 | 原版转子在制作配方中，不是运行插槽。当前十五种涡轮走 OriginalMotorBlock／OriginalRotaryConverter；从背面注入蒸汽、正面输出 RU，罐容量为输入额定值的八倍。方向与流体能力是源路径核对，未运行相邻管道测试。 |
| 保存字段 | 原版保存缓冲、罐、pending、蒸汽计数、停止／模式／反向／视觉状态。当前能定位相应保存读取字段，但没有执行世界保存重启，不能判定完整持久化通过。 |

没有将以下观察误判为缺失：原版涡轮使用 `FL.steam` 而非 `FL.anysteam`，因此不能仅凭当前采用指定 Steam 流体就断言原版允许所有蒸汽别名；原版双输出冷却器也没有覆盖第二输出类型的额定大小查询，所以当前 HU 查询为零不能单独认定为移植回归。这些跨模组行为仍需另行核对。

## 本批源码整理与验证范围

`EnergyNodeSpec` 构建器按职责分行，原先“所有设备都从非正面接收、每个输出包固定扣输入”的错误通用说明改为描述额定元数据与具体设备规则的边界。`SteamTurbineConversion` 解释两刻和冷凝单位；`EnergyGate` 删除已过时的 mdk／D3 定位说明，指向实际共用入口。

三份生产源码的代码与字面量相对 `57c86035e` 完全一致，Java 17 独立编译通过。`tools/integration/audit-probes/EnergyConverterAudit.java` 记录 29 项目录和九个边界例子；复用既有未变化的 core 编译依赖。没有运行 Gradle、客户端、GameTest 或重新打包。本批确认的两项玩法差异尚未修复。

后续继续核对锅炉供热／压力／结垢、燃烧设备、变压器模式和包数、蓄电池保存与充放电、多方块涡轮、网络路由及现代能力接口。转换器的声音、实际流体邻接与覆盖板防护也未完成运行验收。

## 第六十三批太阳能板与单块热交换器续核

[太阳能板](SOLAR_PANEL_AUDIT.md)确认天气分档、正面单包、启停／天空刷新／保存与模型路径已有，218只核两项堆叠16→64；原活动火／邻液检查扩017，原雨／雷防护true不应复制普通转换器风险。用户取消waterlogged保留。

[单块热交换器](HEAT_EXCHANGER_AUDIT.md)八规格和16hot燃料行有入口，普通充热／散热、流体和保存已有；219运行状态、220点火准入、221玩家通用排液优先另列，耐久请求沿126。大型设备这里只确认共用规则归属，未把单块结论当大型结构／8出口验收。

## 第六十四批旋转泵续核

[四旋转泵](ROTATIONAL_PUMP_AUDIT.md)实际注册／ticker／能力和普通旧液输出已有，新增222–228，218扩六对象。相连搜索被独立区域扫描、输入范围及下舍入、source准入会选到含水实体方块、可填出液能力、停启／检测与硬度规格分别记录；整桶模拟与移除成功保护保留。原泵声明RU，不在原环境易损集合，不能将017机械扩至它。未改玩法，继续其余能源／多方块及真实世界边界。
