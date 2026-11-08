# Stonecutter 分类索引

本目录按问题类型索引 `//?` 分支的处理方法；每个分类有通用原因与若干子类型文档，子类型文档给出真实调用例子。

先读[设计](design.md)与[用法](usage.md)。

## BlockEntity

摘要：方块实体的持久化与同步回调由 vanilla 固定签名，差异集中在 `saveAdditional` / `loadAdditional` / `getUpdateTag`。

关键词：BlockEntity, saveAdditional, loadAdditional, getUpdateTag, loadStatic, codecLookup

通用原因：[BlockEntity/README.md](BlockEntity/README.md)

| 子类型 | 说明 | 文档 | 关键词 |
| --- | --- | --- | --- |
| 薄覆盖 | override 签名分支，方法体委托共享方法 | [thin-override.md](BlockEntity/thin-override.md) | saveData, loadData |
| codecLookup | 在签名分支里绑定局部 provider 别名 | [codec-lookup.md](BlockEntity/codec-lookup.md) | codecLookup, Provider |
| 同步 tag | `getUpdateTag` / `handleUpdateTag` 的签名差异 | [update-tag.md](BlockEntity/update-tag.md) | getUpdateTag, handleUpdateTag |
| 静态加载 | `loadStatic` 与 level 时序 | [load-static.md](BlockEntity/load-static.md) | loadStatic, saveWithoutMetadata |

## NBT

摘要：物品/流体栈与容器的 NBT 序列化入口不同，门面负责统一签名与返回类型。

关键词：NbtCompat, HandlerNbt, ItemStackHandler, ContainerHelper, SavedData, wire format

通用原因：[NBT/README.md](NBT/README.md)

| 子类型 | 说明 | 文档 | 关键词 |
| --- | --- | --- | --- |
| NbtCompat | 物品/流体栈的 save/parse 入口 | [nbtcompat.md](NBT/nbtcompat.md) | saveItem, readItem, saveFluid |
| HandlerNbt | `ItemStackHandler` 的 parent/key 重载 | [handler-nbt.md](NBT/handler-nbt.md) | serializeNBT, deserializeNBT, Size |
| 旧 tag 迁移 | 旧 `tag` 复合标签搬进 `CUSTOM_DATA` | [legacy-migration.md](NBT/legacy-migration.md) | tag, CUSTOM_DATA, DFU |
| 容器与存档数据 | `ContainerHelper` 与 `SavedData.Factory` | [container-saveddata.md](NBT/container-saveddata.md) | saveAllItems, computeIfAbsent |
| 线格式 | `{id, Count, tag}` → `{id, count, components}` | [wire-format.md](NBT/wire-format.md) | Count, count, components |

## CompoundTag

摘要：物品自定义数据从可变 `CompoundTag` 改为 `CUSTOM_DATA` 组件，`StackData` 统一公共面。

关键词：CompoundTag, CUSTOM_DATA, CustomData, StackData, hasCustomData, hasAnyData

通用原因：[CompoundTag/README.md](CompoundTag/README.md)

| 子类型 | 说明 | 文档 | 关键词 |
| --- | --- | --- | --- |
| StackData | 公共面统一为 `CompoundTag` | [stackdata.md](CompoundTag/stackdata.md) | read, write, update |
| 两个谓词 | `hasCustomData` 与 `hasAnyData` | [predicates.md](CompoundTag/predicates.md) | hasTag, getComponentsPatch |
| CoverStackData | 覆盖板侧的委托与专用键清理 | [cover-stackdata.md](CompoundTag/cover-stackdata.md) | detached, gt.cover.* |
| 类型化组件 | 只留在 1.21.1 分支的强类型组件 | [typed-components.md](CompoundTag/typed-components.md) | BLOCK_ENTITY_DATA, ENCHANTMENTS |

## Fluid

摘要：流体栈在 1.21.1 也已组件化，身份比较、能力获取与 NBT 入口都需要门面。

关键词：FluidStack, StackCompat, FluidUtil, FluidTankGT, isFluidEqual

通用原因：[Fluid/README.md](Fluid/README.md)

| 子类型 | 说明 | 文档 | 关键词 |
| --- | --- | --- | --- |
| 身份比较 | `isFluidEqual` 与 `isSameFluidSameComponents` | [identity.md](Fluid/identity.md) | sameFluid |
| 能力获取 | `ForgeCapabilities` 与 `Capabilities.FluidHandler` | [capability.md](Fluid/capability.md) | FluidUtil, IFluidHandlerItem |
| 罐体 NBT | `FluidTankGT` / `FluidStack` 的 save/parse | [tank-nbt.md](Fluid/tank-nbt.md) | writeTank, readTank |
| 显示绑定 | `FluidDisplayBinding` 的 provider 顺序 | [display-binding.md](Fluid/display-binding.md) | display, resolve |

## Provider

摘要：`HolderLookup.Provider` 是序列化上下文，不是全局单例；差异集中在参数传递与无 level 路径。

关键词：HolderLookup.Provider, RegistryAccess, RegistryContext, currentLookup

通用原因：[Provider/README.md](Provider/README.md)

| 子类型 | 说明 | 文档 | 关键词 |
| --- | --- | --- | --- |
| Provider 本身 | 是什么、从哪来、为什么不能缓存 | [provider.md](Provider/provider.md) | WorldLoader, registryAccess |
| 无 level 路径 | `getUpdateTag`、客户端 tooltip 的兜底 | [no-level.md](Provider/no-level.md) | RegistryContext, currentLookup |
| 注册表快照 | `Holder` 身份与跨快照重绑定 | [registry-snapshot.md](Provider/registry-snapshot.md) | Holder.Reference, ItemEnchantments |

## Trivial

摘要：两侧相同、一侧为空、只差 import 或常量的分支，优先折叠；能收进现有门面的收进门面。

关键词：identical, empty block, import, split duplicate, registry lookup, logger

通用原因：[Trivial/README.md](Trivial/README.md)

| 子类型 | 说明 | 文档 | 关键词 |
| --- | --- | --- | --- |
| 同义分支 | 两侧代码完全相同 | [identical.md](Trivial/identical.md) | identical |
| 空块与单侧 import | 一侧为空或只有 import | [empty-import.md](Trivial/empty-import.md) | empty, import |
| 拆分重复 | 同条件两个单侧块，或重复注册 | [split-duplicate.md](Trivial/split-duplicate.md) | split duplicate, registration |
| 注册表与 id | 注册表查找、命名空间、标签目录 | [registry-id.md](Trivial/registry-id.md) | Mc, ResourceLocation, tags |
| logger 与模型 | 日志门面与模型资源位置 | [logger-model.md](Trivial/logger-model.md) | Mc.logger, ModelResourceLocation |
| 身份与包装 | 身份比较、包装类、静态谓词 | [identity-wrapper.md](Trivial/identity-wrapper.md) | StackCompat, NeoToolBindings |
| 语义案例 | 看起来同义、实际承载行为 | [semantic-cases.md](Trivial/semantic-cases.md) | craftKind, plunger, igniter |
