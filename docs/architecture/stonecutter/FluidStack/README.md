# FluidStack

流体栈的差异分四类：provider 签名、组件存储、线格式、身份比较。

| 差异 | 1.20.1 | 1.21.1 | 文档 |
| --- | --- | --- | --- |
| 写 NBT | `writeToNBT(CompoundTag)` | `save(HolderLookup.Provider)` | [provider.md](provider.md) |
| 读 NBT | `loadFluidStackFromNBT(CompoundTag)` | `parseOptional(HolderLookup.Provider, CompoundTag)` | [provider.md](provider.md) |
| 数据存储 | `getTag` / `setTag` | `getComponentsPatch` | [storage.md](storage.md) |
| 线格式 | `Tag` | `components` | [wire-format.md](wire-format.md) |
| 身份比较 | `isFluidEqual` | `isSameFluidSameComponents` | [identity.md](identity.md) |

## 子类型

| 子类型 | 说明 | 文档 | 关键词 |
| --- | --- | --- | --- |
| provider | `FluidStack#save` / `parseOptional`、`FluidTankGT` | [provider.md](provider.md) | save, parseOptional, writeTank |
| storage | `tag` ↔ components | [storage.md](storage.md) | getTag, getComponentsPatch |
| wire-format | `Tag` → `components` | [wire-format.md](wire-format.md) | FluidName, components |
| identity | `isFluidEqual` ↔ `isSameFluidSameComponents` | [identity.md](identity.md) | StackCompat.sameFluid |
