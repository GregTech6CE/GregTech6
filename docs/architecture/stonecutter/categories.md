# Stonecutter 分类索引

本目录按作用对象划分；每个领域内部再按差异类型拆子文档。通用概念见[设计](design.md)与[用法](usage.md)。

| 领域 | 主要差异 |
| --- | --- |
| BlockEntity | `saveAdditional` / `loadAdditional` / `getUpdateTag` / `loadStatic` 的 provider 参数 |
| ItemStack | `ItemStack#save`、`tag` ↔ `CUSTOM_DATA`、线格式、旧 tag 迁移、身份比较 |
| FluidStack | `FluidStack#save`、`tag` ↔ components、线格式、身份比较 |
| Container | `ContainerHelper#saveAllItems` / `loadAllItems` 的 provider 参数 |
| SavedData | `SavedData.Factory` 的 provider 参数 |
| Registry | 注册表查找、`ResourceLocation` 构造、标签目录命名 |
| Capability | `ForgeCapabilities` ↔ `Capabilities`、`LazyOptional` ↔ `Optional` |
| ClientModel | `ModelResourceLocation`、baked model、客户端事件 |
| Facade | logger、包装类、静态谓词 |
| Trivial | 同义分支、空块、单侧 import、拆分重复 |
| Parity | CE 与原版行为差异，不是版本差异 |

## BlockEntity

摘要：方块实体回调的签名差异，全部由 provider 参数引起。

关键词：BlockEntity, saveAdditional, loadAdditional, getUpdateTag, loadStatic, codecLookup

领域文档：[BlockEntity/README.md](BlockEntity/README.md)

## ItemStack

摘要：物品栈的 provider 签名、自定义数据存储、线格式、旧数据迁移与身份比较。

关键词：ItemStack, save, CUSTOM_DATA, StackData, isSameItemSameTags, isSameItemSameComponents

领域文档：[ItemStack/README.md](ItemStack/README.md)

## FluidStack

摘要：流体栈的 provider 签名、组件存储、线格式与身份比较。

关键词：FluidStack, save, components, isFluidEqual, isSameFluidSameComponents

领域文档：[FluidStack/README.md](FluidStack/README.md)

## Container

摘要：`ContainerHelper` 的 provider 参数。

关键词：ContainerHelper, saveAllItems, loadAllItems

领域文档：[Container/README.md](Container/README.md)

## SavedData

摘要：`SavedData.Factory` 的 provider 参数。

关键词：SavedData, Factory, computeIfAbsent

领域文档：[SavedData/README.md](SavedData/README.md)

## Registry

摘要：注册表查找、`ResourceLocation` 构造与标签目录命名。

关键词：BuiltInRegistries, ForgeRegistries, ResourceLocation, tags

领域文档：[Registry/README.md](Registry/README.md)

## Capability

摘要：能力 API 与 `LazyOptional` / `Optional` 的差异。

关键词：ForgeCapabilities, Capabilities, LazyOptional, Optional

领域文档：[Capability/README.md](Capability/README.md)

## ClientModel

摘要：客户端模型资源位置与 baked model 的差异。

关键词：ModelResourceLocation, BakedModel, client event

领域文档：[ClientModel/README.md](ClientModel/README.md)

## Facade

摘要：日志、包装类与静态谓词的收敛。

关键词：logger, wrapper, static predicate

领域文档：[Facade/README.md](Facade/README.md)

## Trivial

摘要：两侧相同、一侧为空、只差 import 或常量的分支。

关键词：identical, empty block, import, split duplicate

领域文档：[Trivial/README.md](Trivial/README.md)

## Parity

摘要：CE 与原版的行为差异，不是版本差异；清理 Stonecutter 分支时不能误折叠。

关键词：craftKind, plunger, igniter, PanelCoverHost

领域文档：[Parity/README.md](Parity/README.md)
