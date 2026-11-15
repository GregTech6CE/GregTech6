# L4：互操作协议

本页用于把附属自有方块、方块实体和配方对象接入 GT6CE 的能源、传输、传感、控制和配方系统。

返回[附属编写指南](README.md)。

L4 接口用于让附属自己的方块、方块实体或配方对象接入 GT6CE 已有系统。接口只定义识别和传输契约，不把附属内部类型暴露给 GT 核心。

| 接口 | 实现位置 | 宿主行为与限制 |
| --- | --- | --- |
| `EnergyBlock` / `AddonEnergy` | 方块实体或能源方块 | 按能源类型、面和包模型接受/发射；默认对推荐包大小附近设置上下限，具体注入规则可调用 `AddonEnergy` |
| `GtSignalSource` | 方块实体 | 相邻信号线读取 GT 分数量；`BlockFace` 是绝对方向 |
| `GtSignalReceiver` | 方块实体 | 接收信号线电力的标记；不自行参与信号源求解 |
| `GtSignalCarrier` | 方块实体 | 保存 GT 分数量并提供 clamp 规则；`UNIT` 对应一个原版红石等级 |
| `TemperatureSource` | 方块实体 | 温度计按绝对面读取温度和上限 |
| `ProgressSensorSource` | 方块实体 | 进度传感器按绝对面读取原始进度和最大值 |
| `WeightSensorSource` | 方块实体 | 重量传感器读取公斤数，不包含外壳 |
| `CompressionSensorSource` | 方块实体 | 压力传感器读取压缩体积和上限 |
| `ItemPipeConnectable` / `FluidPipeConnectable` | 方块或方块实体 | 自动连接 opt-in；返回 false 仍可用扳手手动打开，手动状态可跨邻居变化保留 |
| `MachineControl.Provider` | 方块实体 | 为覆盖板、中继器和放大镜提供可选控制面；通过 capability bit 区分开关、进度和模式支持 |
| `CropScanSource` | 作物方块实体 | 作物分析仪读取作物数据；扫描可以把发现等级提升到 4，再看支付逻辑 |
| `MachineRecipeDocumentProvider` | 附属配方对象 | 把自定义配方对象接入机器配方 overlay；必须配合已注册的 `MachineRecipeDocumentType` |
| `AddonSensorContributor` | 平台 SPI | 注册 `sensor_<id>` 方块和物品；附属提供 blockstate、模型、名称和读取逻辑 |

需要注意的互操作边界：

- `MachineControl` 的进度和能源是只读公开状态；当前没有“可工作/刚完成”显式状态，调用方不能从中推导完整 GT6 检测器支持。
- `MachineControl.Provider` 只提供可选能力面；没有该接口的方块不应被当作可控机器，覆盖板也不应把普通方块包装成可控机器。
- `AddonSensor` 的单位必须是 GT 已有 glyph 名；自定义传感器仍由宿主注册方块和物品，附属负责模型、状态和名称资源。
- `ModData` 当前标记为内部元数据，不是附属扩展契约；不要把它当作注册 API。

服务文件和加载侧见 [L0 SPI](l0-setup.md#八个-serviceloader-扩展点)；自有配方对象的桥接步骤见[配方文档 API](recipe-data.md#让附属自己的数据包配方进入-overlay)。
