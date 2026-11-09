# Stonecutter 分类索引

本目录按问题类型索引 `//?` 分支的处理方法；每个分类的通用原因、子类型表与例子都在分类目录的 `README.md` 中。

先读[设计](design.md)与[用法](usage.md)。

## BlockEntity

摘要：方块实体的持久化与同步回调由 vanilla 固定签名，差异集中在 `saveAdditional` / `loadAdditional` / `getUpdateTag`。

关键词：BlockEntity, saveAdditional, loadAdditional, getUpdateTag, loadStatic, codecLookup

分类文档：[BlockEntity/README.md](BlockEntity/README.md)

## NBT

摘要：物品/流体栈与容器的 NBT 序列化入口不同，门面负责统一签名与返回类型。

关键词：NbtCompat, HandlerNbt, ItemStackHandler, ContainerHelper, SavedData, wire format

分类文档：[NBT/README.md](NBT/README.md)

## CompoundTag

摘要：物品自定义数据从可变 `CompoundTag` 改为 `CUSTOM_DATA` 组件，`StackData` 统一公共面。

关键词：CompoundTag, CUSTOM_DATA, CustomData, StackData, hasCustomData, hasAnyData

分类文档：[CompoundTag/README.md](CompoundTag/README.md)

## Fluid

摘要：流体栈在 1.21.1 也已组件化，身份比较、能力获取与 NBT 入口都需要门面。

关键词：FluidStack, StackCompat, FluidUtil, FluidTankGT, isFluidEqual

分类文档：[Fluid/README.md](Fluid/README.md)

## Provider

摘要：`HolderLookup.Provider` 是序列化上下文，不是全局单例；差异集中在参数传递与无 level 路径。

关键词：HolderLookup.Provider, RegistryAccess, RegistryContext, currentLookup

分类文档：[Provider/README.md](Provider/README.md)

## Trivial

摘要：两侧相同、一侧为空、只差 import 或常量的分支，优先折叠；能收进现有门面的收进门面。

关键词：identical, empty block, import, split duplicate, registry lookup, logger

分类文档：[Trivial/README.md](Trivial/README.md)
