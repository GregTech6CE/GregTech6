# Placement

实现落点偏差：同一内容家族在两个版本中实现落点不同：注册归属、holder 类型、节点目录、复合目录、菜单落点、共享 BE 段、引用目录、混合目录、目录字段、物品类依赖、节点类拆分、创造栏装配、包装类、静态谓词、空/同义/拆分重复。差异来自我们自己的迁移与组织方式，不是平台 API 运行时差异，也不是 CE 与原版 GT6 的行为差异。

分类 README 与子样例格式见[模板](../templates.md)。

## 子类型

| 子类型 | 说明 | 文档 | 关键词 |
| --- | --- | --- | --- |
| registration-ownership | 注册归属、holder 类型、节点目录、复合目录、菜单落点、共享 BE 段、引用目录、混合目录、目录字段 | [registration-catalogs.md](registration-catalogs.md)、[holder-supplier.md](holder-supplier.md)、[node-catalog-migration.md](node-catalog-migration.md)、[catalog-split.md](catalog-split.md)、[menu-catalogs.md](menu-catalogs.md)、[shared-be-registrations.md](shared-be-registrations.md)、[reference-catalogs.md](reference-catalogs.md)、[mixed-catalog-split.md](mixed-catalog-split.md)、[catalog-field-over-lookup.md](catalog-field-over-lookup.md) | registerAll, Supplier, GTMenuTypes, shared BE, reference, mixed catalog, lookup |
| class-ownership | 类归属、物品类依赖、节点类拆分、节点 BE 类合并、节点专属迁移类、包装类、静态谓词 | [item-class-dependency.md](item-class-dependency.md)、[node-class-split.md](node-class-split.md)、[node-be-class-merge.md](node-be-class-merge.md)、[node-only-migration.md](node-only-migration.md)、[wrapper-classes.md](wrapper-classes.md)、[static-predicates.md](static-predicates.md) | GTMachineBlockItem, PoweredToolItem, SmeltingCrucibleBlockEntity, OreBlockEntity, NeoToolBindings, GTToolType |
| content-assembly | 创造栏等内容装配 | [creative-tab-contents.md](creative-tab-contents.md) | OriginCreativeContents |
| branch-noise | 空块、同义、拆分重复 | [identical.md](identical.md)、[empty-import.md](empty-import.md)、[split-duplicate.md](split-duplicate.md) | identical, empty, split duplicate |

## 与方针的对应

按[跨版本统一方针](../unification-policy.md)：

- **统一目标**：每个内容家族只保留一条共享注册/访问路径；尽量消除 `//?`、节点包装类、重复注册和同名异义类型；不为保留两版差异新增共享谓词、静态列表或转发层。
- **例外条件 3**：保留 `//?` 前必须排除我们自己的注册顺序、初始化顺序、缓存、包装层、重复注册和条件编译组合导致的差异。
- **决策流程第 4 步**：重复注册、陈旧包装、列表分叉或同义实现 → 统一到 `1.21.1-neoforge` 共享路径，删除 `1.20.1-forge` 路径。
- **反模式**：用共享门面包两套注册路径；把 `1.20.1-forge` 独立实现当成 parity；为保留边界行为新增共享谓词/静态列表/转发层；只改调用点不删旧路径；把平台 API 差异和自身逻辑差异混为一谈。

## 边界

- 平台 API 运行时差异留在原分类（BlockEntity、ItemStack、FluidStack、Container、SavedData、Registry、Capability、ClientModel、Facade）。
- CE 与原版 GT6 的行为差异见 [`docs/decisions/parity/`](../../../decisions/parity/README.md)；[Parity](../Parity/README.md) 只是折叠前守卫页。
- 本目录只处理结构、归属和重复路径；不改变行为。
