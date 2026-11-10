# Stonecutter 设计：三层结构与归属

返回[跨版本要求与入口](../versions.md)。本文说明跨版本代码为什么按三层组织，以及新增代码应归入哪一层。

## 三层结构

```text
共享代码（两个版本都编译）
    │ 只依赖版本中立 API
    ▼
共享门面（共享代码访问节点能力的通道；内部 //? 或转接）
    │
    ▼
节点实现（只在一个加载器编译）
```

- **共享代码**：业务规则、内容定义、engine 逻辑，以及共享的 `client`/`mixin`/`event`/`network`/`compat` 包；两个版本都编译；不 import 节点类；新增公开签名不出现加载器类型（`GTBlocks`/`GTItems` 是已知例外）。
- **共享门面**：共享代码使用的版本中立 API；内部用 `//?` 吸收小差异，或转接节点实现；同名方法必须同义；新增门面必须有调用点。
- **节点实现**：只在一个加载器编译；加载器专属的注册、生命周期、capability、事件、网络、客户端入口、Mixin、KubeJS 接入。节点侧同名包只放加载器专属入口，共享同名包仍在共享输入。

平台能力的调用方向：共享代码 → 共享门面 → 节点实现。节点实现可以调用共享代码的业务逻辑，但不能把加载器类型泄漏回共享层。共享代码不得直接 import 节点实现；节点实现不引用另一节点专属实现。

三层的具体位置与验收见[平台边界](../packages/platforms.md)。

## 区分与归属

### 内容分类

- 材料、配方、机器规则、内容目录：共享代码。
- 通用 Block/BlockEntity/物品逻辑：共享代码。
- 共享 `client`/`mixin`/`event`/`network`/`compat` 逻辑：共享代码。
- 工具谓词、耐久、采集规则、栈数据、注册视图等版本中立 API：共享门面。
- 共享渲染/模型逻辑：共享代码。
- 加载器客户端入口：节点实现。
- 共享事件/网络逻辑：共享代码。
- 加载器事件/网络入口：节点实现。
- 注册、capability、生命周期、加载器专属 Mixin/KubeJS 接入：节点实现。

### 判断准则

- 两个版本都需要，且没有版本差异：共享代码。
- 两个版本都需要，差异只是签名、数据格式或注册归属：共享门面；门面必须是唯一路径，不能包两套注册路径。
- 两个版本都需要，但整类需要不同的加载器 API/生命周期：节点实现。
- 只有一个版本需要：节点实现。
- 共享代码需要 import 节点类：共享门面。
- 节点类只是转发共享代码中的方法：共享代码。
- 节点类只是共享代码的陈旧副本，或节点目录只是同一内容的第二套注册路径：共享代码。

差异的裁决顺序、统一目标和平台 API 例外见[跨版本统一方针](unification-policy.md)。

## 共享门面

门面只在共享代码确实需要跨版本能力时建立。判断标准：

- 公开签名只出现版本中立类型。
- 同名方法在两个版本同义；语义不同就改名，或在调用点显式选择。
- 门面必须做适配或有明确契约，不写纯转发。
- 新增门面必须有调用点；没有调用点的门面不构成边界。
- holder 类型只是 `DeferredHolder` / `RegistryObject` 的差异时，字段用 `Supplier<T>`；需要注册键时用 record 同时保存 key 与 supplier，或保留一个最小的 `//?`。

当前代表门面：

| 门面 | 所在包 | 职责 | 吸收的差异 |
| --- | --- | --- | --- |
| `Mc` | `platform` | 注册表查询与日志等平台 shim | 注册表 API、日志 API |
| `GTToolHelper` | `engine.tool` | 工具谓词、耐久、采集规则 | NBT 与数据组件、电动工具类型 |
| `StackCompat` | `engine.stack` | 物品/流体身份比较、流体 handler | 数据组件、流体能力 |
| `StackData` | `engine.stack` | 物品自定义数据读写 | NBT 与数据组件 |
| `NbtCompat` | `engine.stack` | 物品/流体栈 NBT | provider 签名、序列化形状 |
| `HandlerNbt` | `engine.stack` | `ItemStackHandler` NBT | provider 签名 |
| `CoverStackData` | `content.cover` | 覆盖板栈数据读写 | NBT 与数据组件 |
| `ContentRegistrar` | `content.registry` | 加载器中立的物品/方块/方块实体注册视图 | 注册 API、BE 注册表归属 |
| `GTEngines` / `GTBasicMachines` / `GTBurningBoxes` / `GTSmeltery` / `GTHoppers` | `content.registry` | 机器、燃烧箱、熔炉与 hopper 家族目录 | 注册归属、holder 类型 |
| `GTManualStations` / `GTFluidPipes` / `GTTanks` / `GTMachineBlockItem` | `content.registry` / `content.block.machine` | 手动工作台、流体管道、储罐与管道物品（共享目录/物品类） | 注册归属、holder 类型、物品类依赖 |
| `GTMenuTypes` | `content.registry` | 共享菜单目录 | 菜单工厂、holder 类型 |
| `BakedModelMap` | `client` | 模型 map key、variant 回退与 missing 哨兵 | map key 类型、missing 哨兵 key |
| `ClientModelEvents` | `client` | `RegisterAdditional` 的模型注册 | plain `ResourceLocation` ↔ `STANDALONE_VARIANT` |
| `ClientPackFacade` | `client` | pack 工厂与 geometry loader key | Pack 构造、`readMetaAndCreate`、loader key |

平台细节与验收见[共享门面](../packages/platforms/facade.md)。`CoverStackData` 目前是过渡门面，部分方法为纯转发；调用点迁移完成后应收敛。

## Provider 基础

`HolderLookup.Provider` 是 Codec 解析注册表条目时使用的上下文。1.20.5 起物品/流体栈的序列化改为 Codec 驱动，编解码需要它；1.20.1 的序列化方法不接收它。

- `HolderLookup.Provider` 在 1.20.1 已经存在，`RegistryAccess extends HolderLookup.Provider`；缺的是 vanilla 方法重载，不是类型本身。
- 服务端由 `WorldLoader` 构建，客户端在 1.21.1 由 `ClientboundRegistryDataPacket` 同步（1.20.1 随登录包下发）。
- 重载或切换世界后可能失效，不能缓存到静态字段。
- 无 level 路径（`getUpdateTag`、客户端 tooltip）需要按侧取；当前 `GTToolEnchantments` 与 `FluidDisplayBinding` 各自维护 `currentLookup()`，目标是统一的 `RegistryContext`（待建）。
- `Holder` 身份跨快照会变；`ItemEnchantments` 按身份比较，跨快照混用需要重新解析。

## //? 的归属

`//?` 是共享输入内的条件编译，不是一层。它只承载平台 API 运行时差异：API 签名、数据格式、注册表、能力、网络、事件生命周期。注册归属差异由共享门面收敛为唯一路径，不保留为 `//?`。

- 能收进门面的差异，不留在业务代码。
- 同一调用在两个版本都有同名同义方法时，直接调用共享方法。
- 分支两侧必须同义；确需不同行为时写明原因并分别验证。
- 具体语法、输入与生成见[用法](usage.md)。

## 与节点实现的关系

节点实现只在对应加载器编译，负责注册、生命周期、capability、事件、网络、客户端入口、Mixin 与 KubeJS 接入。调用方向与边界见[三层结构](#三层结构)。

节点页面：

- [平台边界](../packages/platforms.md)
- [Forge 实现](../packages/platforms/forge.md)
- [NeoForge 实现](../packages/platforms/neoforge.md)
