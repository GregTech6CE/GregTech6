# Registry

注册表查找、`ResourceLocation` 构造、标签目录命名、共享注册家族目录、holder 类型、菜单目录、创造栏内容源与物品类依赖在两个版本上不同。

## 子类型

| 子类型 | 说明 | 文档 | 关键词 |
| --- | --- | --- | --- |
| lookup | `BuiltInRegistries` ↔ `ForgeRegistries` | [lookup.md](lookup.md) | Mc.item |
| resource-location | `GregTech.id` ↔ `fromNamespaceAndPath` | [resource-location.md](resource-location.md) | ResourceLocation |
| tag-directory | `tags/items` ↔ `tags/item` | [tag-directory.md](tag-directory.md) | tags |
| registration-catalogs | 共享家族目录与 BE 注册门面 | [registration-catalogs.md](registration-catalogs.md) | registerAll, ContentRegistrar, RegisteredBlockEntity |
| holder-supplier | 用 `Supplier` 隐藏 `DeferredHolder` / `RegistryObject` | [holder-supplier.md](holder-supplier.md) | Supplier, DeferredHolder, RegistryObject |
| node-catalog-migration | 节点目录迁到共享源码集 | [node-catalog-migration.md](node-catalog-migration.md) | GTManualStations, initialize |
| catalog-split | 复合目录按内容家族拆分 | [catalog-split.md](catalog-split.md) | GTToolBlocks, GTBumbleBlocks |
| item-class-dependency | 物品类去掉 `MachineSpec` 依赖 | [item-class-dependency.md](item-class-dependency.md) | GTMachineBlockItem, MachineSpec |
| creative-tab-contents | 创造栏统一内容源 | [creative-tab-contents.md](creative-tab-contents.md) | OriginCreativeContents, CreativeTabBootstrap |
| menu-catalogs | 共享菜单目录、holder 隐藏与平台菜单工厂 | [menu-catalogs.md](menu-catalogs.md) | GTMenuTypes, MenuType, IForgeMenuType, IMenuTypeExtension |
