# Stonecutter 分类索引

本目录按作用对象划分；每个领域内部再按差异类型拆子文档。通用概念见[设计](design.md)与[用法](usage.md)。`HolderLookup.Provider` 的通用概念见设计文档的“Provider 基础”；各领域的 provider 签名见对应领域。

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
| Facade | logger |
| Placement | 注册归属、holder 类型、节点目录、菜单落点、物品类依赖、创造栏装配、包装类、静态谓词、分支噪声 |
| Parity | CE 与原版行为差异，不是版本差异 |

## BlockEntity

摘要：方块实体回调的签名差异，全部由 provider 参数引起。

关键词：BlockEntity, saveAdditional, loadAdditional, getUpdateTag, handleUpdateTag, loadStatic, codecLookup, saveData, loadData, saveWithoutMetadata

领域文档：[BlockEntity/README.md](BlockEntity/README.md)

## ItemStack

摘要：物品栈的 provider 签名、自定义数据存储、线格式、旧数据迁移与身份比较。

关键词：ItemStack, ItemStack#save, ItemStack#parseOptional, CUSTOM_DATA, CustomData, StackData, ItemStack#getTag, ItemStack#getOrCreateTag, ItemStack#setTag, ItemStack#hasTag, hasAnyData, hasCustomData, isSameItemSameTags, isSameItemSameComponents, StackCompat.sameItem, BLOCK_ENTITY_DATA

领域文档：[ItemStack/README.md](ItemStack/README.md)

## FluidStack

摘要：流体栈的 provider 签名、组件存储、线格式与身份比较。

关键词：FluidStack, FluidStack#save, FluidStack#writeToNBT, FluidStack#loadFluidStackFromNBT, FluidStack#getTag, FluidStack#getComponentsPatch, FluidTankGT, isFluidEqual, isSameFluidSameComponents, StackCompat.sameFluid

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

关键词：BuiltInRegistries, ForgeRegistries, Mc.item, GregTech.id, fromNamespaceAndPath, ResourceLocation, tags/items, tags/item, GregTechIdentity

领域文档：[Registry/README.md](Registry/README.md)

## Capability

摘要：能力 API 与 `LazyOptional` / `Optional` 的差异。

关键词：ForgeCapabilities, Capabilities, LazyOptional, Optional, FluidUtil, IFluidHandlerItem, CapabilityItemHandler, IItemHandler

领域文档：[Capability/README.md](Capability/README.md)

## ClientModel

摘要：客户端模型资源位置、baked model map、模型事件与 pack 构造的差异。

关键词：ModelResourceLocation, RegisterAdditional, STANDALONE_VARIANT, BakedModel, BakedModelMap, ModelBakery, missing sentinel, getPlain, find, ModelEvent, ModifyBakingResult, RegisterGeometryLoaders, EventBusSubscriber, SubscribeEvent, ClientModelBootstrap, ClientPackFacade, AbstractPackResources, Pack.readMetaAndCreate, inventory

领域文档：[ClientModel/README.md](ClientModel/README.md)

## Facade

摘要：日志 API 的收敛。

关键词：logger, LogUtils, GregTech.LOGGER, Mc.logger

领域文档：[Facade/README.md](Facade/README.md)

## Placement

摘要：同一内容家族在两个版本中实现落点不同，包括注册归属、holder 类型、节点目录、菜单落点、物品类依赖、创造栏装配、包装类、静态谓词和分支噪声。

关键词：registerAll, ContentRegistrar, Supplier, GTManualStations, GTMenuTypes, OriginCreativeContents, GTMachineBlockItem, NeoToolBindings, GTToolType, identical, same, empty, one-sided, import, split duplicate, duplicate registration

领域文档：[Placement/README.md](Placement/README.md)

## Parity

摘要：CE 与原版的行为差异，不是版本差异；清理 Stonecutter 分支时不能误折叠。

关键词：craftKind, isInteractionTool, isTool, plunger, igniter, consumesTool, PanelCoverHost

领域文档：[Parity/README.md](Parity/README.md)
