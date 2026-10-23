# 附属 API 统一化（2026-10-24）

本批针对现有 API 的对象归属、能源数量、配方构造和 SDK 边界进行兼容性修补。API 号保持 `0`，模组版本保持 `0.0.0`。没有删除已有公开方法、配方构造方法或改动机器存档格式。

## 统一入口

| 入口 | 本批契约 |
| --- | --- |
| `MachineBehaviorContext.inventoryAccess()`、`PartContext.inventoryAccess()` | 共用 `InventoryAccess`；`snapshot` 返回副本，`set` 复制参数并走宿主通知，`extract` 支持模拟。 |
| 两种上下文的 `fluidAccess()` | 共用 `FluidAccess`；容量和数量均为 mB，读／排出结果及灌入参数相互隔离，模拟不修改存储。 |
| `MachineBehaviorContext.energyAccess()` | `MachineEnergy` 区分从宿主储能转移与调用者自行记账的供能，返回 `EnergyTransfer`，准确保留包数、总能量和不足一包的余量。 |
| `AddonRecipes.Line.builder()` | 具名构造全部配方字段，沿用原有构造器校验；参数和读出的数组、物品、流体均隔离。 |
| `AddonRecipes.Dynamic` | 每个提供器获得独立快照，列表结构只读；修改局部栈不影响真实库存、后续提供器或下一次查询。 |

两套存储视图都检查下标与负数量；零数量的取出、灌入和排出不修改状态，设置空栈则会清空槽位。它们用于机器内部加工，不代替面向管道的能力，不执行输入面／槽位角色过滤。单次调用不构成跨物品、流体和能源的事务。视图仅在当前服务器行为回调期间有效。

旧方法的读取语义保留：机器 `item()` 可能返回内部对象，零件 `fluid()` 可能返回内部流体；新代码统一通过新视图读写。`set` 是替换操作，不是带容量检查的插入：零件会截断到声明限制，旧机器 setter 不保证截断。双平台样例的物品访问已迁移到新入口。

能源新入口对负数量、不可表示的包大小和乘法溢出在转移前拒绝。已有 KU／RU 有符号包政策不变。`emitStored`／`emitStoredTo` 扣宿主储能；`transferStoredToPart` 扣实际转入零件的单位；`supplyPart` 不扣宿主储能，调用者必须按 `transferredUnits()` 扣自己的来源。不得再按完整包数舍弃余量或重复扣能。

## SDK 与开发文档

- SDK 仍只含 core 公共类和目标加载器公共类，新增对应的源码包和 Javadoc 包。源码取该 Stonecutter 节点处理后的文件。
- `addonSdk` 构建三个包；普通 `assemble` 构建二进制和源码，文档按需生成。
- 排除 core 实现依赖的构建边，并移除 MixinGradle 对三个 SDK Jar 的自动注入；模组自身的 Mixin 打包保持。
- 打包检查拒绝非 API 类、缺失类、重复条目、运行时资源及模组／Mixin 清单声明。CI 区分二进制 SDK 与 `sources`／`javadoc` 附件。
- 双语指南、样例依赖范围统一为 `[0.0.0,0.1.0)`；开发版本号相同仍需配套的 SDK 和运行时。
- `bind`／`Services`／`dispatchRecipesReady` 明确属于 GT 启动内部入口，不因 Java 可见性而成为附属扩展点。

使用说明见 [中文 API 指南](../ADDON_API.zh-CN.md) 与 [英文 API 指南](../ADDON_API.md)。

## 范围与验证

本批没有把现有模板重写成任意数量能源端口的框架，也没有新增跨资源事务配方执行器或状态版本迁移机制。它建立现有 API 的一致调用边界，不代表通用扩展框架或 GT6 全面移植完成。

本批已完成以下验证，回执见 [API 统一化验证](verification/addon-api-unification-20261024.json)：

- core 能源结果 475 项、配方合同 37 项、版本合同 84 项断言通过；core 与双平台 API 依赖闭合检查通过。
- Forge 1.20.1／NeoForge 1.21.1 主代码、SDK 样例和测试源码编译通过；两端二进制、源码和 Javadoc SDK 构建通过。最终组合执行耗时 2 分 24 秒。
- NeoForge 限定 `gt6ce_api_contracts` 命名空间的 7 项原生 GameTest 全部通过：真实样例机器存储访问、零件适配、能源计量、配方快照、Builder 与动态提供器隔离。能源计量使用受控旧上下文，不把它当作完整能源网络验证。
- 两端 SDK 各 112 个 core 类与 46 个原生 API 类，包内字节与当前编译输出一致；没有运行时资源、Mixin 清单或实现类混入。
- 与外部附属保存的旧 NeoForge SDK 比较，128 个公开／受保护类、105 个字段、961 个方法及构造器的二进制签名保留；30 个旧接口没有新增必须实现的方法。这不保证依赖旧可变配方返回对象的行为仍相同：`Line` 现在返回副本，需要重建行才能改变数据。

日志位于 `work/api-unification-20261024/`。使用已缓存的相同版本依赖与本地仓库路由；未运行 Forge 原生 GameTest、客户端视觉验收、第三方附属实际加载或存档重启。未生成新的完整模组发行包，也未推送。
