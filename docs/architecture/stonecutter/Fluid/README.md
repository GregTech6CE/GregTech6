# Fluid 通用原因

NeoForge 1.21.1 的 `FluidStack` 也已组件化，`getTag()` 不再可用；身份比较、能力获取与 NBT 入口都需要门面。Forge 1.20.1 仍使用 tag 与 `LazyOptional`。

| 操作 | 1.20.1 (Forge 47.4.20) | 1.21.1 (NeoForge 21.1) |
| --- | --- | --- |
| 身份比较 | `FluidStack#isFluidEqual` | `FluidStack.isSameFluidSameComponents` |
| 能力获取 | `ForgeCapabilities.FLUID_HANDLER` | `Capabilities.FluidHandler` |
| `FluidUtil` 返回 | `LazyOptional` / `Optional` | `Optional` |
| 写 NBT | `FluidStack#writeToNBT(CompoundTag)` | `FluidStack#save(HolderLookup.Provider)` |
| 读 NBT | `FluidStack.loadFluidStackFromNBT(CompoundTag)` | `FluidStack.parseOptional(HolderLookup.Provider, CompoundTag)` |
| 罐体写 | `FluidTankGT#writeToNBT(CompoundTag)` | `FluidTankGT#writeToNBT(CompoundTag, HolderLookup.Provider)` |
| 罐体读 | `FluidTankGT#readFromNBT(CompoundTag)` | `FluidTankGT#readFromNBT(CompoundTag, HolderLookup.Provider)` |

子类型见[分类索引](../categories.md#fluid)。
