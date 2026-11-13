# Stonecutter 分类总览

本页是 Stonecutter 分类总览，按作用对象划分 13 个分类，设计、用法与模板见[设计](design.md)、[用法](usage.md)、[模板](templates.md)。

## 文件清单

| 文件 | 名称 |
| --- | --- |
| `docs/architecture/stonecutter/categories.md` | 分类总览 |
| `docs/architecture/stonecutter/<分类>/README.md` | 分类 README |
| `docs/architecture/stonecutter/<分类>/<子类型>.md` | 子样例 |

## 分类清单

| 分类 | 主要差异 |
| --- | --- |
| BlockEntity | `saveAdditional` / `loadAdditional` / `getUpdateTag` / `loadStatic` 的 provider 参数 |
| ItemStack | `ItemStack#save`、`tag` ↔ `CUSTOM_DATA`、线格式、旧 tag 迁移、身份比较、封面过滤器 provider 参数统一 |
| FluidStack | `FluidStack#save`、`tag` ↔ components、线格式、身份比较 |
| Container | `ContainerHelper#saveAllItems` / `loadAllItems` 的 provider 参数 |
| SavedData | `SavedData.Factory` 的 provider 参数 |
| Registry | 注册表查找、`ResourceLocation` 构造、标签目录命名 |
| Capability | `ForgeCapabilities` ↔ `Capabilities`、`LazyOptional` ↔ `Optional` |
| ClientModel | `ModelResourceLocation`、baked model、客户端事件 |
| Facade | logger、事件总线 |
| Placement | 注册归属、holder 类型、节点目录、菜单落点、物品类依赖、节点类拆分、创造栏装配、包装类、静态谓词、分支噪声 |
| BlockCodec（守卫） | 方块类型 codec 的来源、当前作用与 1.21-only stub 规则 |
| Interaction（守卫） | 方块交互签名 1.20.1 `use` ↔ 1.21.1 `useItemOn`/`useWithoutItem` 适配器 |
| Parity（守卫） | 行为差异见 decisions/parity；本目录只做折叠前检查 |

## BlockEntity

摘要：方块实体回调的签名差异，全部由 provider 参数引起。

关键词：BlockEntity, saveAdditional, loadAdditional, getUpdateTag, handleUpdateTag, loadStatic, codecLookup, saveData, loadData, saveWithoutMetadata

分类 README：[BlockEntity/README.md](BlockEntity/README.md)

## ItemStack

摘要：物品栈的 provider 签名、自定义数据存储、类型化组件门面、线格式、旧数据迁移、身份比较与封面过滤器 provider 参数统一。

关键词：ItemStack, ItemStack#save, ItemStack#parseOptional, CUSTOM_DATA, CustomData, StackData, BlockEntityData, BlockStateData, BLOCK_ENTITY_DATA, BLOCK_STATE, BlockItemStateProperties, BlockEntityTag, BlockStateTag, ItemStack#getTag, ItemStack#getOrCreateTag, ItemStack#setTag, ItemStack#hasTag, hasAnyData, hasCustomData, isSameItemSameTags, isSameItemSameComponents, StackCompat.sameItem, registryAccess, setItemFilter, setFluidFilter, 委托重载, save(tag), NbtCompat

分类 README：[ItemStack/README.md](ItemStack/README.md)

## FluidStack

摘要：流体栈的 provider 签名、组件存储、线格式与身份比较。

关键词：FluidStack, FluidStack#save, FluidStack#writeToNBT, FluidStack#loadFluidStackFromNBT, FluidStack#getTag, FluidStack#getComponentsPatch, FluidTankGT, isFluidEqual, isSameFluidSameComponents, StackCompat.sameFluid

分类 README：[FluidStack/README.md](FluidStack/README.md)

## Container

摘要：`ContainerHelper` 的 provider 参数。

关键词：ContainerHelper, saveAllItems, loadAllItems

分类 README：[Container/README.md](Container/README.md)

## SavedData

摘要：`SavedData.Factory` 的 provider 参数。

关键词：SavedData, Factory, computeIfAbsent

分类 README：[SavedData/README.md](SavedData/README.md)

## Registry

摘要：注册表查找、`ResourceLocation` 构造、标签目录命名与配方序列化器 API 形态。

关键词：BuiltInRegistries, ForgeRegistries, Mc.item, GregTech.id, fromNamespaceAndPath, ResourceLocation, tags/items, tags/item, GregTechIdentity, RecipeSerializer, RecipeHolder, fromJson, streamCodec

分类 README：[Registry/README.md](Registry/README.md)

## Capability

摘要：能力 API、`LazyOptional` / `Optional` 与共享 BE 的能力暴露、共享代码消费邻居能力的差异。

关键词：ForgeCapabilities, Capabilities, LazyOptional, Optional, FluidUtil, IFluidHandlerItem, CapabilityItemHandler, IItemHandler, RegisterCapabilitiesEvent, getCapability, BlockCapabilityCache, AdjacentCapabilityCache

分类 README：[Capability/README.md](Capability/README.md)

## ClientModel

摘要：客户端模型资源位置、baked model map、模型事件与 pack 构造的差异。

关键词：ModelResourceLocation, RegisterAdditional, STANDALONE_VARIANT, BakedModel, BakedModelMap, ModelBakery, missing sentinel, getPlain, find, ModelEvent, ModifyBakingResult, RegisterGeometryLoaders, EventBusSubscriber, SubscribeEvent, ClientModelBootstrap, ClientPackFacade, AbstractPackResources, Pack.readMetaAndCreate, inventory

分类 README：[ClientModel/README.md](ClientModel/README.md)

## Facade

摘要：日志与事件总线 API 的分支，优先收进共享门面。

关键词：logger, LogUtils, GregTech.LOGGER, Mc.logger, EventBusSubscriber, PlayerInteractEvent

分类 README：[Facade/README.md](Facade/README.md)

## Placement

摘要：同一内容家族在两个版本中实现落点不同，包括注册归属、holder 类型、节点目录、菜单落点、共享 BE 段、引用目录、混合目录、目录字段、物品类依赖、节点类拆分与合并、节点专属迁移类、创造栏装配、包装类、静态谓词和分支噪声。

关键词：registerAll, ContentRegistrar, Supplier, GTManualStations, GTMenuTypes, OriginCreativeContents, GTMachineBlockItem, PoweredToolItem, SmeltingCrucibleBlockEntity, OreBlockEntity, NeoToolBindings, GTToolType, shared BE, reference catalog, mixed catalog, registry lookup, node BE merge, legacy save, identical, identical constant, same, empty, one-sided, one-sided reference, call chain, one-sided import, node entry, import, split duplicate, duplicate registration

分类 README：[Placement/README.md](Placement/README.md)

## BlockCodec

摘要：方块类型 codec（`Block.codec()`、`MapCodec`、`BLOCK_TYPE`）仅 1.21 存在且无运行时消费端；守卫继承原版抽象基类被迫覆写 codec 时的 1.21-only stub 规则。

关键词：Block.codec, MapCodec, MapCodec.unit, simpleCodec, BLOCK_TYPE, BlockTypes, BlockListReport, DirectionalBlock, HorizontalDirectionalBlock, FallingBlock, BushBlock, BaseRailBlock, BaseEntityBlock, EntityBlock, stub codec

分类 README：[BlockCodec/README.md](BlockCodec/README.md)

## Interaction

摘要：方块交互签名 1.20.1 `use` ↔ 1.21.1 `useItemOn`/`useWithoutItem` 的平台差异；身体统一到共享 helper，`//?` 只承载签名适配与结果映射。

关键词：use, useItemOn, useWithoutItem, InteractionResult, ItemInteractionResult, interact, ToolInteractions, ToolInteractionSpec, PASS_TO_DEFAULT_BLOCK_INTERACTION, sidedSuccess

分类 README：[Interaction/README.md](Interaction/README.md)

## Parity

摘要：CE 与原版 GT6 的行为差异不在本目录记录；权威记录见 [`docs/decisions/parity/`](../../decisions/parity/README.md)。折叠 `//?` 分支前先查该目录。

关键词：craftKind, plunger, igniter, consumesTool

分类 README：[Parity/README.md](Parity/README.md)
