# Registry

注册表查找、`ResourceLocation` 构造、标签目录命名与配方序列化器 API 形态在两个版本上不同。

## 子类型

| 子类型 | 说明 | 文档 | 关键词 |
| --- | --- | --- | --- |
| lookup | `BuiltInRegistries` ↔ `ForgeRegistries` | [lookup.md](lookup.md) | Mc.item |
| resource-location | `GregTech.id` ↔ `fromNamespaceAndPath` | [resource-location.md](resource-location.md) | ResourceLocation |
| tag-directory | `tags/items` ↔ `tags/item` | [tag-directory.md](tag-directory.md) | tags |
| recipe-serialization | `fromJson`/`fromNetwork`/`toNetwork` ↔ `codec()`/`streamCodec()`，包装类冗余委托 | [recipe-serialization.md](recipe-serialization.md) | RecipeSerializer, RecipeHolder |
