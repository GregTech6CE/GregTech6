<!-- Exact conventions excerpt from 7f75798e7, before the six-fact patch. -->

Two calls with the same root can measure different things. `MachineBehaviorContext` names them apart from their units:

| Call | Unit |
| --- | --- |
| `capacity()` | stored-energy cap |
| `capacity(int)` / `tankCapacity(int)` | tank capacity in mB |
| `energy()` | main buffer |
| `energy(String)` / `auxiliaryEnergy(String)` | named auxiliary buffer |
| `emit(long size, long amount)` | accepted packet count |
| `emit(String type, long size, long amount)` / `emitToPart` | accepted energy units |

Boolean direction is split too. `simulate` means true does not execute (`FluidAccess`, `InventoryAccess`, `HeatHandler`, `MachineBehavior.acceptFluid`, the context `fill` and `drain`); `execute` means true does execute (`AddonPlatform.Services.inject` and `extract`).

Directions come in two vocabularies. `MachineFace` is machine-relative (`BOTTOM` through `BACK`); `Direction` is the world side. `canInsert`, `canFill`, `acceptsEnergy`, `onTool` and `gasValve` use `MachineFace`. `gtSignal`, `fillMoldAtSide` and `pushCompressedGas` use `Direction`. `CrucibleMold`, `GtSignalSource`, `ProgressSensorSource`, `CompressionSensorSource` and `TemperatureSource` are core types that cannot name `Direction`, so their `side` is `Direction.ordinal()`.

The three machine templates do not share their rules:

| | Machine type | Tier | Energy |
| --- | --- | --- | --- |
| `RecipeMachine` | id as written; `/` is invalid | at least 1, no upper bound | `energy(null)` throws |
| `BehaviorMachine` | `/` becomes `_` | 1 to 255 | may omit the main input when it emits |
| `FuelGenerator` | id as written; `/` is invalid | must be unset | `energy(non-null)` throws |

Builder conventions differ as well:

- `BehaviorMachine.Builder.recipeMaps`, `clientState`, `item`, `fluid` and `toggle` append. `MachineIo` face setters, `RecipeMachine.Builder.structureKeys` and `AddonRecipes.Line.Builder` field setters replace.
- A required field is often reported by `build()`, not by its setter: `LayoutPart` hardness, `PartTanks` capacity, `BehaviorMachine` gui, `RecipeMachine` energy and recipeMap, `FuelGenerator` fuelMap and output.
- Sentinels: `rangeSpecified`, `capacitySpecified`, `facesSpecified`, `tierSpecified`, `cheapOverclockingSpecified`, `hardnessSpecified`, `stackSpecified`; `LayoutCell.design = -1`, `LayoutRepeat.max = -1`, `FluidPipeDeclaration.maxTemperature = 0` (melting point x 1.25), `MaterialDeclaration.id = 0`, `EnergyType.ANY = "*"`.
