# Capability

能力 API、`LazyOptional` / `Optional` 与共享 BE 的能力暴露在两个版本上不同。

## 子类型

| 子类型 | 说明 | 文档 | 关键词 |
| --- | --- | --- | --- |
| fluid-handler | `ForgeCapabilities` ↔ `Capabilities` | [fluid-handler.md](fluid-handler.md) | FluidUtil, LazyOptional |
| item-handler | `CapabilityItemHandler` ↔ `Capabilities.ItemHandler` | [item-handler.md](item-handler.md) | IItemHandler |
| exposure | 共享 BE 的能力暴露与节点注册 | [capability-exposure.md](capability-exposure.md) | RegisterCapabilitiesEvent, getCapability, LazyOptional, IItemHandler |
| consumption | 共享代码查询邻居能力与门面缓存 | [capability-consumption.md](capability-consumption.md) | getCapability, BlockCapabilityCache, AdjacentCapabilityCache, LazyOptional |
