# ClientModel

客户端模型资源位置、baked model map、模型事件与 pack 构造在两个版本上不同。

## 子类型

| 子类型 | 说明 | 文档 | 关键词 |
| --- | --- | --- | --- |
| model-resource-location | `ModelResourceLocation` 构造与 `RegisterAdditional` | [model-resource-location.md](model-resource-location.md) | ModelResourceLocation, RegisterAdditional, STANDALONE_VARIANT |
| baked-model-map | baked model map key、variant 回退与 missing 哨兵 | [baked-model-map.md](baked-model-map.md) | BakedModelMap, BakedModel, ModelBakery, missing sentinel, getPlain, find |
| events | 模型事件注册、类级注解与顺序 | [events.md](events.md) | ModelEvent, EventBusSubscriber, SubscribeEvent, ClientModelBootstrap, ModifyBakingResult, RegisterGeometryLoaders |
| pack-facade | pack 构造与 geometry loader key | [pack-facade.md](pack-facade.md) | ClientPackFacade, AbstractPackResources, Pack.readMetaAndCreate, RegisterGeometryLoaders |
