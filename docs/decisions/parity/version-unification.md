# 跨版本统一

本页记录 `1.20.1-forge` 向 `1.21.1-neoforge` 基准收敛时的有意行为变更。裁决顺序见[跨版本统一方针](../../architecture/stonecutter/unification-policy.md)。

| 内容 | 1.20.1 变更 | 原因 |
| --- | --- | --- |
| 引擎注册 | 改用共享 `GTEngines` 与 `ContentRegistrar`；引擎物品类改为 `EngineItem`。 | 删除 `MachineRegistry` 引擎路径，统一到 1.21 基准。 |
| 基础机器注册 | 改用共享 `GTBasicMachines`；基础机器物品类改为 `BasicMachineItem`；fusion reactor / implosion compressor 进入基础机器列表。 | 删除 `MachineRegistry` 基础机器路径；1.21 已统一为 `BasicMachineBlock` + 专用 BE。 |
| 燃烧箱注册 | 改用共享 `GTBurningBoxes`；燃烧箱物品类改为 `BurningBoxBlockItem`。 | 删除 `GTMachines` / `MachineRegistry` 燃烧箱路径。 |
| 熔炉注册 | 改用共享 `GTSmeltery`；坩埚使用全部 39 个 `OriginalCrucibleDefinitions` 身份（原 29 个）。 | 1.21 的完整目录为基准。 |
| hopper 注册 | 改用共享 `GTHoppers` 与 `HopperCatalog.ALL`。 | 删除 `GTMachines` / `MachineRegistry` hopper 路径。 |
| 注册顺序 | 对齐 1.21 的 `ContentRegistration` 顺序。 | 单一共享顺序，避免两套初始化顺序。 |
| 平台 API 差异 | 保留 `//?` 的仅限加载器 API 运行时差异。 | 见统一方针的例外条款。 |

这些变更不是回归；后续按 1.21 playtest 与上报问题继续修正。
