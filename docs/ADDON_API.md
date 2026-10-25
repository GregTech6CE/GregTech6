# Addon API

[English](ADDON_API.md) | [中文](ADDON_API.zh-CN.md)

## Overview

API version **0** is mod version **0.0.0** on Forge 1.20.1 and NeoForge 1.21.1. Compile against that loader's SDK jar. The SDK contains the addon API only. GregTech internals are not on it. Register a `GregTechAddon` in your own mod constructor for recipes. GregTech calls `onRecipesReady` once from common setup, after its material/item linking and built-in machine recipes. Callbacks run in ascending addon mod ID order. Duplicate IDs, late registration and a second dispatch fail explicitly. A non-`Error` callback failure (including a checked exception escaping through sneaky throw) is wrapped as `IllegalStateException` and leaves the lifecycle `FAILED`. An `Error` is rethrown unchanged and also leaves `FAILED`. Addons that already ran are not rolled back.

Legacy contributors still load from an installed GregTech jar. They are deprecated and are not in the SDK. New code uses `AddonMaterialContributor` and `AddonMachineContributor`.

Some public Java members exist only so GT6CE's separate source sets can initialize one another. `AddonRuntime`, `AddonPlatform` and `AddonLoader` service binding, their `Services` implementations, and `GregTechAddons.dispatchRecipesReady` are bootstrap internals, not addon extension points. Do not bind or replace services, implement those service interfaces, or dispatch the lifecycle from an addon. Use `GregTechAddons.register`, contributors, and the documented material, item, fluid, recipe, tool and energy facades. Being present in the SDK does not make a bootstrap entry part of the supported addon contract.

## Contents

- [Overview](#overview)
- [Getting started](#getting-started)
  - [Building against the SDK](#building-against-the-sdk)
  - [Registering an addon](#registering-an-addon)
  - [Adding recipes from `onRecipesReady`](#adding-recipes-from-onrecipesready)
- [Conventions](#conventions)
  - [Absent values](#absent-values)
  - [Null arguments](#null-arguments)
  - [Units on `MachineBehaviorContext`](#units-on-machinebehaviorcontext)
  - [Boolean direction](#boolean-direction)
  - [Faces and sides](#faces-and-sides)
  - [Machine templates](#machine-templates)
  - [Builders](#builders)
- [Limits](#limits)
  - [Shared limits](#shared-limits)
  - [Names](#names)
  - [Per type](#per-type)
- [Recipes](#recipes)
  - [Recipe construction](#recipe-construction)
  - [Machine recipe datapacks](#machine-recipe-datapacks)
  - [KubeJS](#kubejs)
  - [Viewer rows](#viewer-rows)
  - [Multiple dynamic providers](#multiple-dynamic-providers)
- [Materials](#materials)
  - [Materials and forms](#materials-and-forms)
  - [Prefixes](#prefixes)
  - [World generation](#world-generation)
  - [Crucible](#crucible)
- [Machines](#machines)
  - [Registering machines](#registering-machines)
  - [Casings](#casings)
  - [Items and fluids](#items-and-fluids)
  - [Recipe tables](#recipe-tables)
  - [Recipe machines](#recipe-machines)
  - [Machine controls](#machine-controls)
  - [Heat](#heat)
  - [Machine tuning](#machine-tuning)
  - [Fuel generators](#fuel-generators)
  - [Pipes](#pipes)
  - [Compressed gas tanks](#compressed-gas-tanks)
- [Behavior machines](#behavior-machines)
  - [Unified storage access](#unified-storage-access)
  - [Explicit energy accounting](#explicit-energy-accounting)
  - [Existing callbacks and declarations](#existing-callbacks-and-declarations)
- [Structures](#structures)
  - [Structure layout datapacks](#structure-layout-datapacks)
- [Energy](#energy)
- [Sensors and signals](#sensors-and-signals)
  - [Sensor sources](#sensor-sources)
  - [Signal wires](#signal-wires)
- [Reference](#reference)
  - [Internal types](#internal-types)
  - [SDK packages](#sdk-packages)
  - [Scope](#scope)
- [Versioning and compatibility](#versioning-and-compatibility)
  - [Version policy](#version-policy)
  - [API changes](#api-changes)
  - [Deprecation and removal](#deprecation-and-removal)
- [Appendix](#appendix)
  - [Upstream history](#upstream-history)
- [License](#license)
- [test branch package migration](#test-branch-package-migration)

## Getting started

Set up the dependency and the SDK jar, register the addon, then add recipes.

### Building against the SDK

Build the loader you are targeting. Forge: `./gradlew :multiversion:1.20.1-forge:addonSdk` writes `multiversion/versions/1.20.1-forge/build/addon-sdk/gt6ce-addon-sdk-1.20.1-forge-0.0.0.jar` plus matching `-sources.jar` and `-javadoc.jar` attachments. NeoForge: `./gradlew :multiversion:1.21.1-neoforge:addonSdk` writes the same three artifacts under `multiversion/versions/1.21.1-neoforge/build/addon-sdk/`, named `gt6ce-addon-sdk-1.21.1-neoforge-0.0.0`. On Windows the command is `gradlew.bat`.

Each SDK contains only the core API and that loader's platform API, in Mojang names, plus the license notices (`META-INF/gregtech6`). The source attachment contains the target node's processed API sources, so loader imports match its compiled classes. Javadoc uses Java 17 for Forge and Java 21 for NeoForge. There are no implementation classes, assets or mod descriptors. Attach the matching source and Javadoc jars in the IDE. Use `compileOnly files(...)` on the binary SDK only; do not shade or bundle its classes. The installed GT6CE mod supplies them at runtime. A production Forge jar is SRG and is not the compile dependency. There is no Maven coordinate yet; take the artifacts from the local build. CI includes them only when its workflow collects them.

`addonSdkJar`, `addonSdkSourcesJar` and `addonSdkJavadocJar` can also run independently; `addonSdkJavadoc` generates the HTML without packaging it. Normal `assemble` includes the binary and source SDKs. Javadoc is opt-in through `addonSdk` or `addonSdkJavadocJar`, so ordinary mod builds do not regenerate documentation.

[`examples/addon-sample/`](../examples/addon-sample/) (`forge/` and `neoforge/`) is a complete working sample covering materials, prefixes, worldgen, crucible rows, recipe machines, multiblocks, functional layout parts, behavior machines, fuel generators, pipes and compressed gas tanks. In a GT6CE checkout, compile and verify it with `./gradlew :multiversion:1.20.1-forge:compileAddonSample :multiversion:1.20.1-forge:addonApiClosureCheck` (or `:multiversion:1.21.1-neoforge:...`), and launch a dev game with the sample loaded by passing `-PaddonSample=true` to `runClient` or `runServer`.

In a standalone addon repository, add the SDK jar as a compile-only dependency and add a built GT6CE mod jar to the dev runtime. NeoForge 1.21.1 jars use Mojang names and can go in `run/mods/` or on `runtimeOnly files(...)` as they are. The Forge 1.20.1 release jar (`distributionJar`) is reobfuscated to SRG, while ForgeGradle dev runs use Mojang names; `fg.deobf` only remaps module coordinates (it warns and skips `files(...)`), so resolve the jar through a `flatDir` repository instead of dropping it into `run/mods/`:

```groovy
repositories {
    flatDir { dir "libs" }
}

dependencies {
    compileOnly files("libs/gt6ce-addon-sdk-1.20.1-forge-0.0.0.jar")
    runtimeOnly fg.deobf("blank:gt6ce-1.20.1-forge:0.0.0") // libs/gt6ce-1.20.1-forge-0.0.0.jar
}
```

Declare a required loader dependency on **`gt6ce`** with `versionRange="[0.0.0,0.1.0)"`, `ordering="NONE"` and `side="BOTH"`. Java packages use `gregtech.*`, the loader mod id and content/save namespace are **`gt6ce`**, and `gregtech6` is reserved only so an addon does not claim it as `addonId()`. Any addon that registers materials, prefixes, machines, layout parts, pipes or tanks adds registry entries and must be installed on both client and server (`side="BOTH"`); datapack-only recipe changes (`gt6ce:machine` and `gt6ce:machine_removal`) live on the server and sync to clients through vanilla recipe sync. Build your addon separately for each loader, using that loader's supported Java/Minecraft toolchain. One SDK jar does not make a Forge addon binary load on NeoForge.

```toml
# Forge: META-INF/mods.toml
[[dependencies.my_addon]]
modId="gt6ce"
mandatory=true
versionRange="[0.0.0,0.1.0)"
ordering="NONE"
side="BOTH"

# NeoForge: META-INF/neoforge.mods.toml
[[dependencies.my_addon]]
modId="gt6ce"
type="required"
versionRange="[0.0.0,0.1.0)"
ordering="NONE"
side="BOTH"
```

### Registering an addon

```java
import gregtech.api.addon.GregTechAddon;
import gregtech.api.addon.GregTechAddons;

// Call this from the constructor of your Forge or NeoForge @Mod class.
GregTechAddons.register(new GregTechAddon() {
    public String id() { return "my_addon"; }
    public void onRecipesReady(Context context) {
        // context.platform(): "forge" or "neoforge"
        // context.minecraftVersion(): "1.20.1" or "1.21.1"
        // context.apiVersion(): 0; all documented capabilities are available
        // Query GregTechMaterials and AddonItems here.
        // Add rows with AddonRecipes.map(name).add(...).
    }
});
```

New materials and extra forms use a separate `ServiceLoader` entry, described below. Do not submit them from the mod constructor: on Forge 1.20.1 the addon constructor and the GregTech constructor can overlap.

Startup runs in five stages:

1. **Mod construction (`@Mod` constructor)**: call `GregTechAddons.register(GregTechAddon)` and queue any addon-owned `DeferredRegister` entries. Do not query `GregTechMaterials`, `AddonItems` or `AddonRecipes` here.

2. **Material initialization (`GTMaterialRegistry.init`)**: GregTech loads `AddonMaterialContributor` via `ServiceLoader` and calls all of its list methods at once. It declares `materials()` after the built-in directory and links built-in materials. It then applies `compositions()`, `smeltingTargets()`, `crucibleReactions()` and `crucibleRemovals()`, calls `link()`, and publishes ore byproducts (`MaterialDeclaration.Builder.oreByproducts`), `oreVeins()`, `smallOres()`, `prefixes()` and `forms()`.

3. **Machine and pipe registration (`AddonMachineRuntime.install`)**: GregTech loads `AddonMachineContributor`, `BehaviorMachineContributor` and `LayoutPartContributor` via `ServiceLoader`, creates `recipeTables()`, and queues addon machines, layout parts, pipes and compressed gas tanks on GregTech's mod bus in `<addonId>:`.

4. **Common setup (`FMLCommonSetupEvent`)**: after material-item linking and built-in machine recipes load, GregTech calls `GregTechAddon.onRecipesReady(Context)` in ascending `id()` order, audits `exclusiveOutputs` tables, and resolves plain block ids in `StructureLayout`.

5. **Server start and `/reload`**: restores the startup recipe snapshot (built-in plus `onRecipesReady` rows), rebuilds tag-based ore and compat rows, and applies `gt6ce:machine_removal` and `gt6ce:machine` datapacks.

Every contributor class listed in `META-INF/services/...` must be `public` with a `public` no-argument constructor. One class may implement multiple contributor interfaces (for example `AddonMachineContributor` and `BehaviorMachineContributor` on the same class); `ServiceLoader` instantiates each service interface separately, so contributor classes must be stateless. Forge and NeoForge load every mod jar into the same game layer, so `ServiceLoader` on GregTech's class loader also sees the provider classes in your jar. The required `gt6ce` dependency makes sure GregTech is installed; it is not what makes the classes visible. When this guide says a check **fails startup**, GregTech throws an `IllegalArgumentException` or `IllegalStateException` in `logs/latest.log` and the loader crash report. The message names the addon id and the offending entry; an exception thrown inside a contributor method is wrapped with the contributor class name instead.

### Adding recipes from `onRecipesReady`

An example recipe callback, where `MyItems.MY_ORE` is registered by your addon:

```java
import gregtech.api.addon.platform.AddonItems;
import gregtech.api.addon.platform.AddonRecipes;
import net.minecraft.world.item.ItemStack;

var map = AddonRecipes.map("gt.recipe.mortar");
var recipe = AddonRecipes.Line.builder()
        .itemInputs(new ItemStack(MyItems.MY_ORE.get()))
        .itemOutputs(AddonItems.stack("dust", "Copper"))
        .duration(20)
        .euPerTick(16)
        .build();
if (map.add(recipe) == null)
    throw new IllegalStateException("my_addon mortar recipe rejected");
```

This uses the existing GT recipe system, not a parallel addon recipe registry. Choose inputs/outputs and power/duration appropriate for your addon. New rows are available to the existing machine recipe lookup and viewers; overlapping inputs retain that map's original precedence rules. `onRecipesReady` runs once at startup. Datapack and KubeJS changes use the overlay below; they do not call the addon callback again.

## Conventions

These rules apply across the SDK. Per-type details stay in the Javadoc.

### Absent values

| Call | When nothing matches |
| --- | --- |
| `GregTechMaterials.find(name)` / `find(id)` | `GregTechMaterials.invalid()`; `valid()` is false. Never null. |
| `AddonItems.stack(...)` | `ItemStack.EMPTY` (unknown prefix or material, null argument, count ≤ 0) |
| `AddonItems.composition(stack)` | `StackComposition.EMPTY` |
| `AddonFluids.fluid(...)` / `still(...)` | `FluidStack.EMPTY` (unknown field, null argument, amount ≤ 0) |
| `AddonItems.selectorTag(n)` | throws `IllegalArgumentException` (outside 0..24, or not registered) |
| `AddonRecipes.map(name)` | throws `IllegalArgumentException` |
| `AddonRecipes.Map.add` / `addDisplay` | `null`: the map rejected the row |
| `AddonRecipes.Map.collision` / `find`, `Dynamic.find` | `null`: no row |
| `MachineControl.find`, `BoundPart.behavior()` / `context()`, `PartContext.controller()` | `null` (marked `@Nullable` or documented) |
| `PartContext.crucible()` | `CrucibleContent.EMPTY` |
| `BehaviorState` getters, `PartClientView` / `BehaviorMachineClientView` getters | `0`, `false`, `""` or `ItemStack.EMPTY` for a key that was never written. A machine accepts any valid key; a part or a client view throws on a key it did not declare. |

A lookup that finds nothing does not throw unless the table says so. A calculation that cannot be done is not a missing lookup; the internal helpers that return `null` or `-1` for that are listed under [Internal types](#internal-types).

### Null arguments

- A record constructor (`MaterialDeclaration`, `PrefixDeclaration`, the worldgen and crucible records, …) rejects a required null with `NullPointerException` naming the field.

- Builder/factory validation commonly uses `IllegalArgumentException` for required values. Messages vary by field; `BehaviorMachine.Builder.progress(null)` is rejected by `build()` with `NullPointerException("progress")`. Consult the type Javadoc for exceptions.

- A null element inside a face list or a varargs argument is rejected with `IllegalArgumentException("Missing … face")`, not an NPE from `List.of`.

Optional null values may be accepted or normalized; see each type's Javadoc.

### Units on `MachineBehaviorContext`

Two calls with the same root can measure different things:

| Call | Unit |
| --- | --- |
| `capacity()` | stored-energy cap, in energy units |
| `capacity(int tank)` / `tankCapacity(int tank)` | tank capacity, in mB |
| `energy()` | main buffer, in energy units |
| `energy(String type)` / `auxiliaryEnergy(String type)` | auxiliary buffer of that type |
| `emit(long size, long amount)` | accepted **packet count** |
| `emit(String part, long size, long amount)` / `emitToPart(...)` | accepted **energy units** (a partial packet may count) |
| `pushToPart(String part, String type, long size, long amount)` | accepted **energy units**; the host buffer is not debited |

`energyAccess()` wraps these in `MachineEnergy`, whose methods return an `EnergyTransfer` with both counts.

### Boolean direction

| Parameter | `true` means | Where |
| --- | --- | --- |
| `simulate` | do **not** change anything | `FluidAccess`, `InventoryAccess.extract`, `HeatHandler`, `MachineBehavior.acceptFluid`, the context and part `fill` / `drain` |
| `execute` | **do** it | `AddonEnergy.inject` / `extract` |
| `doInject` / `doExtract` | **do** it | `EnergyBlock.doEnergyInjection` / `doEnergyExtraction`, `doInject` / `doExtract` |

### Faces and sides

- `MachineFace` (`BOTTOM`, `TOP`, `RIGHT`, `FRONT`, `LEFT`, `BACK`) is relative to the machine's horizontal facing. `index()` and `bit()` are mask positions.

- `Direction` is the world side.

- `MachineFace`: `canInsert`, `canFill`, `acceptsEnergy`, `onTool`, `gasValve`, `MachineIo`, `EnergyInput` / `EnergyOutput` faces, `PartSlots` / `PartTanks`. A null `MachineFace` on `canInsert`, `canFill` or `acceptsEnergy` means the menu or an unsided caller.

- `Direction`: `gtSignal`, `fillMoldAtSide`, `pushCompressedGas`, `emitTo`, `PartHit.face`, `SensorContext.side`, `EnergyBlock` (null means every side).

- `int side` = `Direction.ordinal()` (0 DOWN, 1 UP, 2 NORTH, 3 SOUTH, 4 WEST, 5 EAST): `CrucibleMold`, `GtSignalSource`, `ProgressSensorSource`, `CompressionSensorSource`, `TemperatureSource`. These are core types and cannot name `Direction`.

### Machine templates

| | Machine type | Tier | Main energy input |
| --- | --- | --- | --- |
| `RecipeMachine` | the id; `/` is invalid | at least 1, no upper bound; default 1 | required; `energy(null)` throws |
| `BehaviorMachine` | the id with `/` replaced by `_` | 1 to 255; default 1 | optional when the machine declares an output |
| `FuelGenerator` | the id; `/` is invalid | must stay unset | none; `energy(non-null)` throws |

`MachineCasing.machineType(...)` overrides the derived type in all three.

### Builders

- **Append:** `BehaviorMachine.Builder.recipeMaps`, `clientState`, `item`, `fluid`, `toggle`; `StructureLayout.Builder.cell`, `repeat`; `LayoutRepeat.Builder.cell`; `LayoutPart.Builder.field`.

- **Replace:** `MachineIo.Builder` face setters, `EnergyInput.Builder.faces`, `EnergyOutput.Builder.faces`, `PartSlots` / `PartTanks` `insert` / `extract`, `RecipeMachine.Builder.structureKeys`, every `AddonRecipes.Line.Builder` setter.

- **Required, checked by `build()`:** `LayoutPart` hardness; `PartTanks` capacity; `BehaviorMachine` gui; `RecipeMachine` energy and recipeMap; `FuelGenerator` fuelMap and output; `RecipeTable` title and gui.

- **"Was it set?" flags:** `EnergyInput.rangeSpecified()`, `capacitySpecified()`, `facesSpecified()`; `MachineCasing.tierSpecified()`; `RecipeMachine.cheapOverclockingSpecified()`. A flag is true when the setter was called, even with a default-looking value.

- **Sentinel values:** `LayoutCell.design() == -1` (no design index), `LayoutRepeat.max() == -1` (repeat until a layer fails, bounded by `MAX_COPIES`), `FluidPipeDeclaration.maxTemperature == 0` (melting point × 1.25), `MaterialDeclaration.id() == 0` (GregTech assigns the id), `EnergyType.ANY` (`"*"`, type locked by the first packet).

## Limits

A value outside these bounds fails at `build()`, in the record constructor, or at registration, with the field named in the message.

### Shared limits

| Limit | Value | Applies to |
| --- | --- | --- |
| Energy per tick | 1 to 1,000,000,000,000 | `BehaviorMachine` main and auxiliary input, `RecipeMachine` auxiliary input, `FuelOutput.perTick` | <!-- limit:per-tick -->
| Tank capacity | 1 to 1,024,000,000 mB | `BehaviorMachine` fluid slots, `PartTanks.capacity` (`MAX_CAPACITY`), `FuelGenerator.Builder.tank` (`MAX_TANK`), `CompressedGasTankDeclaration.capacity` (`MAX_CAPACITY`) | <!-- limit:capacity -->
| Efficiency | 1 to 100,000, in parts per 10,000; default 10,000 (100%) | `RecipeMachine`, `FuelGenerator`, `MachineTuning.processingEfficiency` | <!-- limit:efficiency -->
| Item output chance | 1 to 10,000 (`RecipeOutputChances.CERTAIN`) | `Line.outputChances` | <!-- limit:chance -->
| Programmed circuit | 0 to 24 (`SelectorTags.MIN` / `MAX`) | `AddonItems.selectorTag` | <!-- limit:selector -->

### Names

| Name | Pattern |
| --- | --- |
| `addonId()` | `[a-z][a-z0-9_]{1,63}` | <!-- pattern:addon-id -->
| Material, composition and byproduct names | `[A-Za-z][A-Za-z0-9_]*` | <!-- pattern:material -->
| Prefix name | `[a-z][A-Za-z0-9]{0,63}` | <!-- pattern:prefix -->
| Recipe map, fuel map, structure key | `[a-z0-9_.]+` | <!-- pattern:recipe-map -->
| Machine type | `[a-z0-9_]+` | <!-- pattern:machine-type -->
| Machine id (`MachineCasing`) | `[a-z0-9/._-]+` | <!-- pattern:machine-id -->
| Part name, pipe id suffix, gas tank id, vein and small-ore name, repeat key | `[a-z][a-z0-9_]{0,63}` | <!-- pattern:part-name -->
| Energy type | `[A-Za-z][A-Za-z0-9_]{0,15}`, or `*` | <!-- pattern:energy-type -->
| GUI texture | `namespace:path.png` |
| Behavior state key, client state key, part field | `[a-z0-9_.]{1,64}` | <!-- pattern:state-key -->
| Block entity id | `[a-z0-9_./-]{1,64}` | <!-- pattern:block-entity -->
| Prefix display prefix / suffix | at most 64 characters, no newline | <!-- limit:prefix-text -->
| Behavior state string value | non-null, at most 256 characters | <!-- limit:state-string -->

### Per type

| Type | Field | Bounds |
| --- | --- | --- |
| `BehaviorMachine` | tier | 1 to 255; default 1 | <!-- limit:behavior-tier -->
| `BehaviorMachine` | gui size | width 176 to 256, height 166 to 512 | <!-- limit:gui -->
| | item slots / fluid slots / toggles | at most 54 / 8 / 16 | <!-- limit:slots -->
| | client state keys | at most 32 | <!-- limit:client-keys -->
| | `maxRange` | 0 to 32; 0 closes the world actions | <!-- limit:range -->
| `RecipeMachine` | `parallel` | at least 1 | <!-- limit:recipe-parallel -->
| | `minimumLightAbove` | 0 to 15 | <!-- limit:light -->
| `FuelGenerator` | `amperes` | 1 to 64 (`MAX_AMPERES`); default 1 | <!-- limit:amperes -->
| | tank | default 4,000 mB | <!-- limit:fuel-tank -->
| `FuelOutput` | `behind` | 1 to 8 (`FuelGenerator.MAX_BEHIND`) | <!-- limit:behind -->
| `IoTarget` | each coordinate | −16 to 16, not the origin | <!-- limit:io-coordinate -->
| `LayoutCell` | design | 0 to 255 | <!-- limit:design -->
| `LayoutRepeat` | `min` / `max` | `min` ≥ 1; `max` ≥ `min`, at most 1,024 (`MAX_COPIES`), or −1 | <!-- limit:repeat -->
| `LayoutPart` | fields | at most 16 (`MAX_FIELDS`) | <!-- limit:fields -->
| `PartSlots` | count / stack limit | 1 to 16 (`MAX_SLOTS`) / 1 to 64 (`MAX_STACK`) | <!-- limit:part-slots -->
| `PartTanks` | count | 1 to 8 (`MAX_TANKS`) | <!-- limit:part-tanks -->
| `ItemPipeDeclaration` | `stepSize` / `invSize` | at least 4 / at least 1 | <!-- limit:pipe -->
| Ore vein, small ore | Y range | `0 ≤ minY < maxY ≤ 255`, before the overworld remap | <!-- limit:ore-height -->
| `MachineTuning` | tier / parallelLimit | tier ≥ 1; parallel ≥ 1 | <!-- limit:tuning -->
| `RecipeTable` | minimal inputs | `minimalInputItems ≤ inputItems`, `minimalInputFluids ≤ inputFluids` | <!-- limit:table-inputs -->

The upper bounds above are checked by the SDK. `ItemPipeDeclaration` also caps `stepSize` and `invSize` to avoid overflow; the message gives the exact value.

## Recipes

Add, display and remove machine recipe rows.

### Recipe construction

Prefer `AddonRecipes.Line.builder()` for named fields. It offers `itemInputs`, `itemOutputs`, `fluidInputs`, `fluidOutputs`, `duration`, `euPerTick`, `special`, `outputChances`, `keptInputs`, `keptFluidInputs` and `presenceFluidInputs`, followed by `build()`. Stack and index setters accept varargs, replace that field, and copy their arguments immediately. Omitted fields mean no inputs or outputs, one tick, zero power/special value, certain item outputs, and no kept/presence inputs. `build()` applies the same validation as the constructors. Reusing a builder creates independent rows; do not share its mutable builder between threads.

`AddonRecipes.Line` is a class with five constructors. Each constructor checks that every input amount is positive, on every thread. `Line.read` is the only entry that keeps an empty slot from a row already stored. `Line` is an immutable snapshot: construction and accessors copy every array and stack. Rows compare by value: two separately built rows with the same item stacks (item, count and native data), fluid stacks (fluid, amount and native data), scalars and index arrays are equal and have the same hash code. Editing `line.itemInputs()[0]`, a returned chance array or the original builder input does not edit the recipe. Construct a new row to express changed data; replacing a registered recipe still uses the existing removal/registration mechanisms. `Map.find` does not consume or modify the supplied input lists or stacks. The five existing constructors remain available:

- `Line(ItemStack[] itemInputs, ItemStack[] itemOutputs, FluidStack[] fluidInputs, FluidStack[] fluidOutputs, long duration, long euPerTick, long special)`

- `Line(..., int[] outputChances)`

- `Line(..., int[] outputChances, int[] keptInputs)`

- `Line(..., int[] outputChances, int[] keptInputs, int[] keptFluidInputs)`

- `Line(..., int[] outputChances, int[] keptInputs, int[] keptFluidInputs, int[] presenceFluidInputs)`

`itemInputs`, `itemOutputs`, `fluidInputs` and `fluidOutputs` may be `null` or empty when unused (`FluidStack` is `net.minecraftforge.fluids.FluidStack` on Forge and `net.neoforged.neoforge.fluids.FluidStack` on NeoForge). `duration` is ticks (clamped to at least 1). `euPerTick` is energy per tick (non-negative on machine tables; negative on fuel tables for produced energy). `special` is the map-specific extra parameter (`0` for ordinary recipes, or temperature in K on crucible display maps). The seven-argument `Line` leaves every item output certain. Pass an `int` per item output, in parts per 10_000, when an output is not certain. `10000` is certain. An empty array is the same as omitting it. The length must match the item outputs, and each value is 1 to 10000. Fluid outputs stay certain. The machine, JEI, EMI and the datapack overlay all read that chance from the recipe row. A datapack item output already uses the same `chance` field.

`AddonItems.selectorTag(n)` is programmed circuit `n`: `integrated_circuit_0` through `integrated_circuit_24`, with a count of 1. A value outside 0..24, or a circuit that is not registered, throws `IllegalArgumentException`. The circuit is already a catalyst item, so the machine checks that it is present and does not consume it. Listing its index in `keptInputs` is allowed and does not change that.

`keptInputs` lists item-input indexes the machine must see and must not consume. Leave each count at 1 or more. Null and an empty array mark nothing, and that row matches the same inputs it did before. An index outside the item inputs, or a repeated index, fails startup. A count of 0 makes the stack empty and fails startup with `Item input <i> is empty`; leave the count at 1 and list the index in `keptInputs` instead. EMI draws a marked input as a catalyst, the same way it draws a built-in catalyst input. The datapack overlay is unchanged: a count of 0 there is still rejected, and the overlay has no kept-input field.

`keptFluidInputs` lists fluid inputs whose positive declared quantity is reserved once and retained, independent of batch size. `presenceFluidInputs` instead lists presence-only conditions: matching checks the original tank contents, does not reserve or drain any quantity, and does not multiply by the batch. Repeated presence conditions may share the same fluid, including fluid consumed by another input. Missing fluid rejects the whole recipe. The two index arrays cannot overlap; duplicates and out-of-range indexes are rejected.

The datapack overlay expresses presence-only conditions with amount 0; negative inputs and zero outputs are rejected. Native `FluidStack` loses fluid identity at zero, so native `Line` callers must supply a positive representative (for example 1 mB) and list its index in `presenceFluidInputs`. That representative is not a required extra reserve. Do not pass `new FluidStack(fluid, 0)` through the native API. Existing constructors retain their previous positive-input behavior. Recipe viewers label these conditions as presence-only catalysts.

Presence-only rows are rejected (with a diagnostic) on the builtin direct-conversion fuel maps `gt.recipe.fuels.burn`, `gt.recipe.fuels.gas`, `gt.recipe.fuels.fluidbed`, `gt.recipe.fuels.engine`, and `gt.recipe.fuels.hot`; dynamic providers cannot bypass this restriction. Other fuel maps are not blanket-disabled. Heat exchangers also reject presence rows selected through a custom saved `gt.fuelmap`, without draining their fluid; they can still select an ordinary conversion row. Ordinary fermenters support presence conditions, but sealed barrels skip those rows because they replace their entire tank contents. Positive kept-fluid behavior in these specialized converters is unchanged and is not claimed to be supported by this fix.

```java
var circuit = new AddonRecipes.Line(
        new ItemStack[]{new ItemStack(MyItems.MY_ORE.get()), AddonItems.selectorTag(3)},
        new ItemStack[]{AddonItems.stack("dust", "Copper")},
        null, null, 20, 16, 0);
var kept = new AddonRecipes.Line(
        new ItemStack[]{new ItemStack(MyItems.MY_ORE.get()), new ItemStack(MyItems.MY_LENS.get())},
        new ItemStack[]{AddonItems.stack("dust", "Tin")},
        null, null, 20, 16, 0, null, new int[]{1});
var keptFluid = new AddonRecipes.Line(
        new ItemStack[]{new ItemStack(MyItems.MY_ORE.get())},
        new ItemStack[]{AddonItems.stack("dust", "Tin")},
        new FluidStack[]{new FluidStack(Fluids.WATER, 100)}, null,
        20, 16, 0, null, null, new int[]{0});
```

### Machine recipe datapacks

A datapack can add or remove static rows on a machine recipe map that already exists. Put the JSON in the vanilla recipe directory: `data/<namespace>/recipes/` on Forge 1.20.1, and `data/<namespace>/recipe/` on NeoForge 1.21.1. It cannot create a map, and it does not see or edit dynamic providers. KubeJS `event.custom()` and the schema below submit that same JSON. `event.remove({type: "gt6ce:machine"})` removes the marker recipes from the vanilla recipe manager; the next reload then rebuilds the machine maps from the markers that remain.

Rebuild order, after tags are bound:

1. Restore the startup snapshot. That snapshot is the built-in rows plus addon rows from `onRecipesReady`. Compat and tagged external-ore rows are left out of it.

2. Rebuild those compat and external-ore rows from the current tags.

3. Apply every `gt6ce:machine_removal`, sorted by recipe id.

4. Apply every `gt6ce:machine`, in the same id order.

A second `/reload` repeats that sequence, so additions do not pile up. Singleplayer keeps one static table: the integrated server applies the overlay, and the client skips its own pass. A dedicated client applies the markers it received from the vanilla recipe sync, then JEI and EMI read the maps again.

```json
{
  "type": "gt6ce:machine",
  "map": "gt.recipe.crusher",
  "item_inputs": [{ "item": "minecraft:heart_of_the_sea" }, { "tag": "minecraft:coals", "count": 1 }],
  "fluid_inputs": [{ "fluid": "minecraft:water", "amount": 1000 }],
  "item_outputs": [{ "item": "minecraft:emerald", "count": 1, "chance": 5000 }],
  "fluid_outputs": [{ "fluid": "minecraft:lava", "amount": 1 }],
  "eu_per_tick": 16,
  "duration": 64,
  "special": 0
}
```

`map` is an existing recipe-map name such as `gt.recipe.crusher`. An item input is either `item` or `tag`, never both. Counts default to 1. Fluid amounts are millibuckets and are required on an addition. Output `chance` is parts per 10000 and defaults to 10000; 0 is rejected. `eu_per_tick` is at least 0, `duration` is at least 1, and omitted `special` is 0. Item, tag and fluid ids are `namespace:path` using `[a-z0-9_./-]`. The map name uses `[a-z0-9_.]`. A tag input expands to one concrete row per item in the tag, sorted by item id. The row is rejected when the tag is empty, an id is missing, the slot counts do not fit the map, the expansion passes 4096 combinations, or the map already has a recipe whose inputs are covered by the new inputs. Remove the old row in the same pack; removals run first.

```json
{
  "type": "gt6ce:machine_removal",
  "map": "gt.recipe.crusher",
  "item_inputs": [{ "item": "minecraft:iron_ingot", "count": 1 }, { "tag": "minecraft:coals" }],
  "fluid_inputs": [{ "fluid": "minecraft:water", "amount": 1000 }],
  "item_outputs": [{ "item": "minecraft:emerald", "chance": 10000 }],
  "fluid_outputs": [{ "fluid": "minecraft:lava" }]
}
```

Every listed input and output must match a distinct slot. A count, amount, or chance of 0, or an omitted one, matches any value. A tag matches when the recipe's item is in that tag. `{"map":"gt.recipe.crusher","clear":true}` removes every static row on that map. `clear` cannot be combined with a match list, and a removal with neither is rejected. Fake display rows are static rows and can be removed. A running machine already stored its outputs, so a reload still finishes that job.

An unknown field rejects the recipe. Top-level fields that start with `_` are ignored, such as `_comment` or the `_kubejs_changed_marker` that KubeJS 7 adds to `event.custom()` JSON.

Built-in machine recipe map names (resolved by both `AddonRecipes.map(name)` and datapack `"map"`):

- Processing and forming: `mc.recipe.furnace`, `gt.recipe.microwave`, `gt.recipe.roaster`, `gt.recipe.distillery`, `gt.recipe.extruder`, `gt.recipe.smelter`, `gt.recipe.crystallisationcrucible`, `gt.recipe.drying`, `gt.recipe.laminator`, `gt.recipe.catalyticcracking`, `gt.recipe.steamcracking`, `gt.recipe.distillationtower`, `gt.recipe.cryodistillationtower`, `gt.recipe.shredder`, `gt.recipe.lathe`, `gt.recipe.cutter`, `gt.recipe.centrifuge`, `gt.recipe.rollingmill`, `gt.recipe.rollbender`, `gt.recipe.rollformer`, `gt.recipe.clustermill`, `gt.recipe.wiremill`, `gt.recipe.mixer`, `gt.recipe.loom`, `gt.recipe.sluice`, `gt.recipe.sharpener`, `gt.recipe.burnmixer`, `gt.recipe.pressurewasher`, `gt.recipe.crusher`, `gt.recipe.sifter`, `gt.recipe.squeezer`, `gt.recipe.compressor`, `gt.recipe.press`, `gt.recipe.electrolyzer`, `gt.recipe.canner`, `gt.recipe.injector`, `gt.recipe.printer`, `gt.recipe.scannervisuals`, `gt.recipe.scannermolecular`, `gt.recipe.autocrafting`, `gt.recipe.slicer`, `gt.recipe.nanofab`, `gt.recipe.plantalyzer`, `gt.recipe.bumblelyzer`, `gt.recipe.boxinator`, `gt.recipe.unboxinator`, `gt.recipe.polarizer`, `gt.recipe.magneticseparator`, `gt.recipe.laserengraver`, `gt.recipe.welder`, `gt.recipe.freezer`, `gt.recipe.cryomixer`, `gt.recipe.massfab`, `gt.recipe.replicator`, `gt.recipe.autoclave`, `gt.recipe.bath`, `gt.recipe.generifier`, `gt.recipe.coagulator`, `gt.recipe.fermenter`, `gt.recipe.melter`, `gt.recipe.cokeoven`, `gt.recipe.lightning`, `gt.recipe.implosioncompressor`, `gt.recipe.fusionreactor`, `gt.recipe.mortar`, `gt.recipe.cooker`, `gt.recipe.blastfurnace`, `gt.recipe.vacuumfreezer`, `gt.recipe.assembler`, `gt.recipe.cncmachine`, `gt.recipe.hammer`, `gt.recipe.chisel`, `gt.recipe.calciner`, `gt.recipe.juicer`, `gt.recipe.anvil`, `gt.recipe.anvil.bend.small`, `gt.recipe.anvil.bend.big`.

- Display and reference maps: `gt.recipe.cruciblealloying`, `gt.recipe.cruciblesmelting`, `gt.recipe.bedrockorelist`, `gt.recipe.byproductlist`, `gt.recipe.bumblequeen`, `gt.recipe.trees`, `gt.recipe.other`, `gt.recipe.toolhead`.

- Built-in fuel recipe maps: `gt.recipe.fuels.fluidbed`, `gt.recipe.fuels.burn`, `gt.recipe.fuels.gas`, `gt.recipe.fuels.hot`, `gt.recipe.fuels.plasma`, `gt.recipe.fuels.engine`, `gt.recipe.fuels.turbine`, `gt.recipe.fuels.magic`.

### KubeJS

When KubeJS is installed, GregTech registers a `gt6ce:machine` recipe schema. The plugin class in `kubejs.plugins.txt` is not loaded when KubeJS is absent. `event.custom()` and `event.remove({type: "gt6ce:machine"})` still work.

```js
ServerEvents.recipes(event => {
  event.recipes.gt6ce.machine('gt.recipe.crusher')
    .itemInputs('minecraft:iron_ore', '#minecraft:coals')
    .fluidInputs(Fluid.of('minecraft:water', 1000))
    .itemOutputs(Item.of('minecraft:emerald').withChance(5000))
    .eu(16).duration(64)
  GT6CE.removeMachine(event, { map: 'gt.recipe.crusher', itemInputs: ['minecraft:iron_ore'] })
  GT6CE.clearMap(event, 'gt.recipe.crusher')
})
```

`GT6CE.maps()` lists the built-in map names plus addon maps installed so far. A name that is not on that list fails while the script loads, before the overlay rejects it. KubeJS on 1.21.1 removed `Item.of().withChance`, so the chance is a field: `.itemOutputs({ item: 'minecraft:emerald', chance: 5000 })`. `withChance(5000)` is parts per 10000 on 1.20.1 only. A fraction greater than 0 and at most 1 is multiplied by 10000.

In a GT6CE repository checkout, KubeJS is always `compileOnly`. `-PkubejsRuntime=true` adds it to the development runtime only. The dependency stays out of the production jar and is independent of `-PcompatRuntime`.

The startup script `GT6CEStartupEvents.machines` can declare recipe tables, recipe machines, fuel generators, casings, layouts, and casing or energy parts. The content is registered in the `kubejs:` namespace. The client and the server must load the same startup script, or the registries diverge. KubeJS loads startup scripts from its own mod constructor, and GregTech reads the declarations while blocks are registered. If the scripts have not loaded by then, startup fails with a message containing `KubeJS startup scripts have not loaded`.

Behavior machines, functional parts with slots or a tick, pipes, compressed gas tanks and materials are not available from this script. Block states, models and language files belong in `kubejs/assets/kubejs/`. GregTech generates the default model for a structure part. The sample is `examples/kubejs-sample/`.

### Viewer rows

`AddonRecipes.Map.addDisplay` stores a display row for JEI and EMI. Lookup skips display rows, so a machine never crafts them. Addons can use it to show an example of a dynamic result, the same way the Bumblelyzer shows one scan on its recipe page. A display row does not replace a provider: the machine still needs `addDynamic` to compute the real output. A `RecipeTable` already becomes a viewer category with the declared slot counts, so machine-shaped rows need no further hook. There is no SDK entry for a custom viewer category or custom recipe widgets; an addon that needs those depends on JEI or EMI directly.

### Multiple dynamic providers

Static rows are still searched first. Dynamic providers run only when none of those rows match, in this order: the built-in provider installed with `RecipeMap.dynamicRecipes`, then each provider passed to `RecipeMap.addDynamicRecipes`, in call order. The first provider that returns a non-null recipe is the dynamic answer, and later providers are not asked. Returning null means the provider does not handle that input. `dynamicRecipes` replaces only the built-in provider. `addDynamicRecipes` appends one provider and does not replace the built-in provider or any earlier addon provider.

`MachineRecipeMaps.Plantalyzer` has no built-in provider. An addon can append its own, for example a crop seed scan, without removing the Bumblelyzer, molecular scanner, printer or replicator providers.

## Materials

Declare materials, forms, prefixes, ore generation and crucible behavior through `AddonMaterialContributor`.

### Materials and forms

List an `AddonMaterialContributor` in `META-INF/services/gregtech.api.addon.AddonMaterialContributor`. GregTech loads that file with its own class loader at the start of material initialization, before the built-in directory is declared. Return pure data. GregTech calls every list method once, right there: `materials()`, `forms()`, `prefixes()`, `compositions()`, `smeltingTargets()`, `crucibleReactions()`, `crucibleRemovals()`, `oreVeins()` and `smallOres()`. None of them, and not the service constructor, may touch GregTech material tables. `compositions()`, `smeltingTargets()`, `crucibleReactions()` and `crucibleRemovals()` are applied after the built-in directory has linked. `link()` runs at the same point for anything those lists do not cover.

```java
public final class MyMaterials implements AddonMaterialContributor {
    public String addonId() { return "my_addon"; }
    public List<MaterialDeclaration> materials() {
        return List.of(MaterialDeclaration.builder("AmmoniumNitrate")
                .displayName("Ammonium Nitrate").color(0xC8D6C0).chemicalFormula("NH4NO3")
                .dust().build());
    }
    public List<MaterialFormRequest> forms() {
        return List.of();
    }
}
```

Declare the material by name. `MaterialDeclaration.builder(String)` is the entry. `builder(int, String)` remains until 0.9 and ignores the number, after logging a warning. `AddonMaterials.ADDON_ID_MIN` and `ADDON_ID_MAX` remain until 0.9 and are not a range an addon can claim.

`MaterialDeclaration.Builder.centrifuge()` permits automatic Centrifuge decomposition of this material's separately declared composition; `electrolyzer()` permits Electrolyzer decomposition. Both default to off, and either or both may be enabled. Neither enables the other or creates missing forms: the input must have a dust form, and every output must be resolvable before a recipe is generated. For example, `.dust().centrifuge()` opts into the Centrifuge only. Automatically generated Electrolyzer recipes also require selector zero (`AddonItems.selectorTag(0)`); it is retained, not consumed. Automatically generated Centrifuge recipes do not require that selector. These permissions are part of the properties that shared declarations must agree on. The existing 16-argument `MaterialDeclaration` constructor remains available and sets both new permissions to `false`.

A name is matched exactly. Two spellings that differ only by case fail startup, because generated item ids are lowercased. The failure names both spellings and, when they come from addons, both addon ids. The same exact name as a built-in material also fails. To add a form of a built-in material, use `forms()`.

Two addons may declare the same name. The display name, color, texture, melting point, boiling point, density, formula, properties, atomic counts and alpha must all match. `forms()` is the union of the requests. The material records every declaring addon, in addon-id order. One addon uses `tooltip.gt6ce.material_from_addon`. More than one uses `tooltip.gt6ce.material_from_addons` (“Added by the %s addons”), with the addon ids joined by a comma. A field that differs fails startup and names both addons and the field.

GregTech assigns numeric ids after the built-in directory, sorted by name, starting at 10000. Built-in ids do not change. The assigned id exists only in memory: a different set of addons can number the same name differently, and nothing in a save reads that number back. `GregTechMaterials.find(int)` still resolves whatever id this session assigned. `dust()` generates the dust items. `metal()` generates those dusts plus the ingot, plate, rod, gear and related forms that the built-in directory already knows how to generate. It does not add a prefix.

`ores()` gives the material GregTech's `ORES` form. That is what makes the raw ore item, the rock, the impure dust, and the crushed, washed and centrifuged forms pass the existing checks. Purified and refined dusts stay off, because those still require the unused `DIRTY_DUSTS` flag. It also registers the `ore` and `oreSmall` blocks. Those two are appended after the blocks the directory already emits, so built-in block ids stay put. The host rock is a block state on that one block, the same as a built-in ore. `ores()` does not register a raw-ore storage block or a raw-ore crate.

Veins are a separate declaration, below. `oreByproducts("Copper", "Tin")` turns `ores()` on and sets the byproducts in that order; the first name is the wash byproduct. Leave it out and the byproduct list is empty. The wash and centrifuge recipes still follow the built-in fallback. A byproduct name has to be a material that exists at startup, and it cannot be hidden. Two addons that share the material have to agree on `ores()` and on that list. `forms()` cannot name `ore` or `oreSmall`. `forms()` of `oreRaw` still fails when the material did not declare `ores()`.

`GregTechMaterials.Material.byproducts()` returns that list as material names, after `resolve()`, in the same order. Built-in materials and addon materials both answer. Read it from `onRecipesReady`, after materials have linked. A call before that returns the names stored at that moment and does not throw. An unknown material returns an empty list. An addon cannot change a built-in material's byproducts.

`mass()`, `toolQuality()`, `toolSpeed()` and `toolDurability()` read those four fields from the material table. The numbers are not converted. Iron's mass is 56 and its tool quality is 2. An addon cannot write them.

`AddonItems.composition(ItemStack)` is the reverse lookup. It returns a `GregTechMaterials.StackComposition`: the prefix name, and one `Component` per material unit GregTech already stores for that item. Amounts are in `U` (`648648000`). One ingot is `1 U`. A material alias is returned as the resolved name, so a vanilla iron ingot is prefix `ingot` and one `Iron` unit. The stack count is not multiplied in. A damaged stack reports the remaining fraction of those units. An item with no stored data is empty: no prefix and no units. An empty stack is empty. Dirt is one `Soil` unit and has no prefix, because that is what the vanilla composition table stores. The lookup uses GregTech's item data. It does not read the item's name or NBT, and it does not expose the internal composition type.

Saves store the name. Crucible contents are `{name, amount}` under `gt.materials`. The crucible display field `gt.display.material` is the name, or an empty string when the crucible is not showing a material. USB sticks and drive files keep the key `gt.replicator.data` and store the name as a string. A save written by an older build still loads: a numeric `id`, or a numeric `gt.replicator.data` / `gt.display.material`, resolves a built-in id and does not resolve an addon id. An unknown name is skipped, as an unknown id already was. There is no numeric suffix for an addon item-id collision.

| Field | Default | Allowed / meaning | Failure / effect |
| --- | --- | --- | --- |
| Generated namespace | Owning addon | Built-ins remain `gt6ce`; handwritten content uses the addon namespace | Old generated `gt6ce:` ids are not remapped |
| Ownership | `AddonMaterials.owner` | First declaring addon alphabetically; materials created in `link()` follow link order | Shared declarations keep one owner |
| Form recognition | Shared `MaterialItemDefinitions` | Use `MaterialFormItem` or the shared list | Namespace checks for `gt6ce:` do not recognize generated addon forms |
| `forms()` | Only property-generated prefixes | An already generated pair is not registered again | Unknown or invalid prefixes fail startup |
| `crystal` | Validator only | The built-in directory generates no form, icon or `item.gt6ce.crystal` translation | Cannot be requested as a generated form |
| `polymer()` / `wires()` | Off | Addon materials may request `plate` / `foil`, or `wireFine`, without an ingot | Neither grants the remaining family or changes built-ins; untagged dust and diamond plates still fail |
| Built-in form tags | Directory properties | Rubber, plastic, PTFE, PVC, Bakelite and polycarbonate have both tags; graphene has plates, foils and wires; graphite has sticks only | Machine ingots supply plates/foils; wires are a separate tag |
| Ore forms | `ores()` required | `oreRaw` needs ores; `ore` / `oreSmall` are block prefixes | `forms()` cannot request the block prefixes |
| New prefixes | `prefixes()` | See [Prefixes](#prefixes) | Does not make prefixes outside the generated list valid for `forms()` |
| Material lifetime | Startup-fixed | Old numeric ids are read on load; names are written on the next save | Datapack reload cannot add or remove materials |

The translation key is `material.gt6ce.<lowercasename>`, from `GTMaterial.getTranslationKey()`. Put it in the addon’s own `assets/gt6ce/lang/en_us.json` and `zh_cn.json`. Minecraft merges those files with GregTech’s by key. A tooltip on an addon material uses `tooltip.gt6ce.material_from_addon` (“Added by the %s addon”), or `tooltip.gt6ce.material_from_addons` when several addons declared it. The shared Chinese file keeps the English fallback for both keys, because neither is an original GT6 translation.

A `dust()` material such as ammonium nitrate generates **6** items (`dust`, `dustSmall`, `dustTiny`, `dustDiv72`, `unit` and `scrapGt`; `dustImpure` comes with `ores()`). Iron’s automatic forms are **96**. `metal()` is the declaration that produces that larger set. Phase C recipe generators walk every material, so a new material receives the furnace, macerator and similar rows its properties already imply. Inspect registered materials at runtime with `GregTechMaterials.all()` and `GregTechMaterials.find(name)` (for example `Iron`, `Copper`, `AnnealedCopper`, `Steel`, `Tin`, `Bronze`, `StainlessSteel`, `Teflon` and `PVC`; chemical-symbol aliases such as `Cr` and `Chromium` resolve to the same material). The generated prefix list that `forms()` and `AddonItems.stack(prefix, material)` accept comprises:

- Dusts and ore processing: `dust`, `dustSmall`, `dustTiny`, `dustDiv72`, `dustImpure`, `unit`, `crushed`, `crushedTiny`, `crushedPurified`, `crushedPurifiedTiny`, `crushedCentrifuged`, `crushedCentrifugedTiny`, `rockGt`, `oreRaw`.

- Gems and optics: `gemChipped`, `gemFlawed`, `gem`, `gemFlawless`, `gemExquisite`, `gemLegendary`, `bouleGt`, `plateGemTiny`, `plateGem`, `lens`.

- Metals and components: `nugget`, `chunkGt`, `billet`, `ingot`, `ingotHot`, `ingotDouble`, `ingotTriple`, `ingotQuadruple`, `ingotQuintuple`, `plateTiny`, `plate`, `plateDouble`, `plateTriple`, `plateQuadruple`, `plateQuintuple`, `plateDense`, `plateCurved`, `foil`, `scrapGt`, `stick`, `stickLong`, `bolt`, `screw`, `gearGt`, `gearGtSmall`, `ring`, `chain`, `spring`, `springSmall`, `rotor`, `round`, `itemCasing` (alias `casingSmall`), `wireFine`, `minecartWheels`, `railGt`, `glasstube`, `coin`, `arrowGtWood`, `arrowGtPlastic`, `bulletGtSmall`, `bulletGtMedium`, `bulletGtLarge`, `plantGtBerry`, `plantGtBlossom`, `plantGtFiber`, `plantGtTwig`, `plantGtWart`, and the `toolHead*` prefixes (`toolHeadSword`, `toolHeadPickaxe`, `toolHeadShovel`, `toolHeadAxe`, `toolHeadHoe`, `toolHeadHammer`, `toolHeadFile`, `toolHeadChisel`, `toolHeadSaw`, `toolHeadDrill`, `toolHeadChainsaw`, `toolHeadWrench`, `toolHeadScrewdriver`, `toolHeadSpade`, `toolHeadUniversalSpade`, `toolHeadSense`, `toolHeadPlow`, `toolHeadBuzzSaw`, `toolHeadArrow`, plus their `toolHeadRaw*` counterparts).

Removing the addon leaves those `gt6ce:` items missing under the usual Minecraft rules. A crucible or USB entry whose name is no longer loaded is skipped, so unloading an addon does not crash the load (known limitation: saving and reloading an existing world after uninstalling an addon is not an automated test target).

### Prefixes

`prefixes()` on `AddonMaterialContributor` declares item prefixes. GregTech registers them after the built-in prefixes and before it checks `forms()`. A name that already exists fails startup. That includes a different capitalisation, the same registry path, and an alias such as `casingSmall`. Two addons cannot declare the same name. The failure names the addon and the prefix that already owns the name.

```java
public List<PrefixDeclaration> prefixes() {
    return List.of(new PrefixDeclaration("sampleBlank", "", " Sample Blank", 8L * GTValues.U,
            "item/sample_blank", List.of("Iron", "SampleAlloy"), Set.of()));
}
```

`amount` is GregTech units, the same unit as a composition part. One ingot is `GTValues.U` (`648_648_000L`). `GTValues` also defines `U2`, `U3`, `U4`, `U9` and `U72` for fractions used by smelting targets and by `GregTechMaterials.Component`. A prefix amount is a positive whole number of `U`, so 8U is `8 * GTValues.U`. A bare `8` is rejected. One sample blank is 8U in the crucible and in the shredder. Built-in prefix amounts stay on the existing table. `texture` is a path in this addon's namespace: `item/sample_blank` reads `assets/<addon>/textures/item/sample_blank.png`. The item has that one layer, tinted with the material colour. There is no texture-set folder and no overlay.

`displayPrefix` and `displaySuffix` are the English spelling around the material name. An empty prefix and `" Sample Blank"` read as "Iron Sample Blank". A translation `item.<addonId>.<snake_name>` with one `%s` replaces that spelling. At least one of the two display strings is required.

`materials` names the materials that receive the prefix, including built-in materials. `conditions` is optional. The set is `PrefixCondition`: `HAS_TOOL_STATS`, `SMITHABLE`, `NOT_WOOD`, `NOT_COAL`, `NOT_STONE`, `NOT_FOOD`, `NOT_MEAT`, `METAL`, `MOLTEN` and `ALLOY`. Every selected condition has to match. `MOLTEN` accepts a material with either `MOLTEN` or the earlier `MELTING` flag. There is no custom predicate. An empty material list needs a positive condition (`HAS_TOOL_STATS`, `SMITHABLE`, `METAL`, `MOLTEN` or `ALLOY`), because a negative condition on its own would cover almost every material. With a positive condition, every matching material is included as well as the named ones. Without one, the negative conditions only check the named materials, and a name that fails is a startup error. `excludedMaterials` is optional and removes names after the selection; an unknown name is ignored, and an excluded name in the explicit list is removed too.

`NOT_COAL` is Coal, Charcoal, CoalCoke, Lignite, LigniteCoke, Anthracite, HydratedCoal and PetroleumCoke. Food is the GregTech food flag. Meat is MeatRaw, MeatCooked and MeatRotten.

The items are registered in the **addon's** namespace, after the generated `gt6ce:` forms. The order is addon id, then declaration order, then material name. The path has the same shape as a built-in form, `sample_blank_iron`. A path collision inside that namespace fails startup. The item does not take a `gt6ce:` id and does not change the built-in item id list. `forms()` cannot name this prefix; the material list on the declaration chooses the items. `GregTechPrefixes.exists` is true for the name after startup. `AddonItems.stack` returns the item.

GregTech does not add crafting recipes for the prefix.

`PrefixDeclaration.block` is a solid cube, not an item prefix. Call the overload without `shape`. The older overload still requires `shape` to be `cube`. `texture` is a path in the addon namespace. `oreDictionary` is a tag path such as `storage_blocks/sample_casing`; each material is tagged `forge:<oreDictionary>/<material>`. The block and its item are registered in **`gt6ce`**, after the built-in material blocks and after addon ore blocks, so those ids stay put. The material list chooses the blocks. `forms()` cannot name this prefix. `AddonItems.composition` reports the prefix name and the declared units. A metal that already passes `isValidFor` does not need `polymer()` or `wires()` for the forms it already generates.

### World generation

`oreVeins()` and `smallOres()` on `AddonMaterialContributor` add large veins and small ores. GregTech appends them after the built-in rows of that dimension. The name is the identity inside one dimension and one kind: a large vein and a small ore may share a name, and the same name may be used in another dimension. A name that already belongs to a built-in row, or to another addon, fails startup and names both sides.

```java
public List<OreVeinDeclaration> oreVeins() {
    return List.of(new OreVeinDeclaration("sample_alloy", "minecraft:overworld",
            10, 40, 40, 3, 16, "SampleAlloy", "SampleAlloy", "Copper", "Tin"));
}

public List<SmallOreDeclaration> smallOres() {
    return List.of(new SmallOreDeclaration("sample_alloy", "minecraft:overworld",
            20, 60, 4, "SampleAlloy"));
}
```

The fields are the built-in ones. `weight`, `density`, `size` and `amount` are positive. `minY` and `maxY` use the same pre-remap heights as the built-in tables, inside 0..255, with min below max. The overworld generator stretches that range. A large vein names four materials: top, bottom, between and spread. A small ore names one. Each material has to exist and have an ore block. An addon material needs `ores()`. A built-in material has to be one that already receives an ore block. An unknown or hidden material fails startup.

Dimensions are `minecraft:overworld`, `minecraft:the_nether` and `minecraft:the_end`. A large vein cannot use the nether, because that generator does not place large veins. Any other dimension fails. There is no separate generator for it. The overworld table is also what generates in a dimension that is neither the nether nor the end.

Stone layers stay the GregTech table. Addon ores are not inserted into a stone layer. With stone layers on, the legacy overworld large veins stay off, and the addon large veins are the ones that generate. They are picked by weight together with whatever built-in veins that table still contains. Adding weight changes `nextInt` for that table, so the same seed can place a different built-in vein. That happens for the End, where the built-in large veins stay on. Small ores are appended, and each row consumes its own random draws, so the built-in small ores keep their positions.

### Crucible

Throwing a GregTech material form into a crucible already melts it. The amount is the form's material weight. Once the crucible is hot enough, the stack becomes that material's smelting target. With no declaration the target is the material itself, one unit out for one unit in, and the crucible smelting page does not list it. Molds still cast whatever material is molten into the mold's existing form, when that form exists.

`compositions()`, `smeltingTargets()`, `smeltingTargetRemovals()`, `crucibleReactions()` and `crucibleRemovals()` are pure data on `AddonMaterialContributor`. GregTech reads them after the built-in directory has linked and before recipes load. Names are material names. `Cr` and `Chromium` are the same material. Unknown names, a non-positive yield, ratio or divider, an empty input list on a new row, or an output that is also an input fail startup and name the addon.

A composition may be set only on a material this addon declared. Part amounts use the same GregTech units as a prefix `amount`: a positive multiple of `GTValues.U`, so 3U is `3 * GTValues.U`. The divider is stored, and the tooltip formula then comes from the composition. `crucible = true` also adds one alloy row: each ratio is `amount / U`, and the yield is the divider. `crucible = false` stores the composition and adds no row. Two owners of a merged material must declare the same composition, including that flag.

A composition alone does not permit automatic Centrifuge or Electrolyzer recipes. `crucible = true` adds the crucible reaction described above, but does not grant either machine permission. Opt in explicitly on the corresponding `MaterialDeclaration` with `.centrifuge()`, `.electrolyzer()`, or both. Explicitly declared machine recipes remain separate.

A smelting target may also be set only on this addon's material. `amount` is GregTech units produced for each `GTValues.U` of input, so `GTValues.U` is one to one. A fraction such as `GTValues.U4` is allowed. The declaration marks the material as melting, which is what puts it on the crucible smelting page. Omitting it leaves the default: melt into itself, with no melting flag. `smeltingTargetRemovals()` drops a target after every addon's `smeltingTargets()`, including a built-in one, so an ore can be left without a metal melt.

An explicit alloy row uses coefficients, not units. Three annealed copper and one tin producing four bronze is yield `4` and ratios `3` and `1`. The output may be an existing material. A removal names the output and the input materials. An empty input list removes every row of that output. Otherwise the input names must be the same set; order and ratios are ignored. Removals run after every addon has added its rows, so one addon can drop another addon's row. A removal that matches nothing is a warning, not a failed startup, and each removed row is logged with the addon id.

Assembly order is: built-in explicit rows, built-in composition rows, addon explicit rows in addon-id order, addon composition rows, then removals. The reaction rule is unchanged: one winning row per tick, at most one solid input, no reaction below an output or input melting point, and the greatest output wins. An addon row whose inputs and ratios exactly match another remaining row, with the same output and yield, fails startup. The same inputs and ratios with a different yield or output are kept, and startup warns that the greater output can take the other row over. Remove the old row first when that is the goal.

The table is frozen at the end of material linking. Reading it earlier fails startup. There is no datapack and no `/reload` for these rows. Mixer recipes that produce the same alloy are separate; remove those with `gt6ce:machine_removal`. This entry does not add crucible blocks, hulls, faucets, molds, or a way to say what an arbitrary non-GregTech item melts into. It does not write a crucible's temperature or contents.

A behavior machine can also be read as a crucible without taking anything. It names its molten material through `getCrucibleMaterial(context)` (an empty string when it holds none), next to `getCrucibleContentAmount` and `getCrucibleTemperature`. A functional part bound to a formed controller reads that through `PartContext.crucible()`. `CrucibleContent` carries `material()` (a name for `GregTechMaterials.find`), `amount()` in GregTech units, `temperature()` in Kelvin and `present()`. The view is empty before the structure forms, after it breaks, or when no controller is bound. Reading removes nothing and keeps no block entity; take material only through `fillMoldAtSide`.

```java
public List<MaterialComposition> compositions() {
    return List.of(new MaterialComposition("SampleCrucibleAlloy", 4, List.of(
            new MaterialComposition.Part("SampleAlloy", 3 * GTValues.U),
            new MaterialComposition.Part("Copper", GTValues.U)), true));
}
public List<SmeltingTargetDeclaration> smeltingTargets() {
    return List.of(new SmeltingTargetDeclaration("SampleAlloy", "SampleAlloy", GTValues.U));
}
public List<CrucibleReactionDeclaration> crucibleReactions() {
    return List.of(new CrucibleReactionDeclaration("SampleCrucibleAlloy", 2, List.of(
            new CrucibleReactionDeclaration.Part("SampleAlloy", 1),
            new CrucibleReactionDeclaration.Part("Tin", 1))));
}
public List<CrucibleReactionRemoval> crucibleRemovals() {
    return List.of(new CrucibleReactionRemoval("StainlessSteel",
            List.of("WroughtIron", "Nichrome", "Chromium", "Manganese")));
}
```

## Machines

Declare machines, structures, recipe tables, pipes and tanks through `AddonMachineContributor`. The first sections describe the shared building blocks; the later ones describe each template.

### Registering machines

List an `AddonMachineContributor` in `META-INF/services/gregtech.api.addon.AddonMachineContributor`. GregTech loads that file with its own class loader after materials are initialized and before it registers machine blocks. Return pure data from `addonMachines()`, `structureLayouts()`, `layoutParts()` and `recipeTables()`. Do not register blocks yourself. `fluidPipes()`, `itemPipes()` and `compressedGasTanks()` stay on this contributor. Pipes and compressed drums are in the sections of those names.

`addonId()` is the loader mod id. It matches `[a-z][a-z0-9_]{1,63}` (two to 64 characters). `gt6ce`, `gregtech6` and `minecraft` are reserved. The registry path may repeat across namespaces. `machineType` is global: it must not be a built-in catalog or layout name. Two blocks of the same addon may share a machine type when they agree on the signature for that template. A second addon cannot reuse the name.

GregTech registers the block, item and block-entity type in the addon's namespace, on GregTech's own mod bus. The block id is `<addonId>:<id>`. A recipe machine's block-entity id is `<addonId>:be_<id>`, using the id as declared. A behavior machine with an empty block-entity id, and every fuel generator, uses `be_<id>` with each `/` replaced by `_`. Built-in ids stay `gt6ce:<path>` and `gt6ce:be_<path>`. Forge logs a benign `Potentially Dangerous alternative prefix` warning when `DeferredRegister` registers `<addonId>:` entries on GregTech's mod bus; NeoForge accepts `DeferredRegister.create(registry, addonNamespace)` on GregTech's bus directly (exercised by the `addon_sample` GameTest server runs).

`gt6ce:basic_machine` is the menu for every recipe machine. The client looks up the recipe table by machine-type name. `gt6ce:behavior_machine` is the menu for every behavior machine. A fuel generator uses the ordinary machine stop switch and has no menu of its own.

Block models resolve to `<namespace>:block/machine/basic/<machineType>` plus `_running` and `_active`. GregTech still generates item models only for the `gt6ce` namespace; the addon ships its own models and blockstates. The display name is `block.<namespace>.<id>`.

Files an addon jar provides for its declarations:

- `META-INF/services/gregtech.api.addon.AddonMaterialContributor` (materials, prefixes, worldgen and crucible rules)

- `META-INF/services/gregtech.api.addon.AddonMachineContributor` (machines, recipe tables, layouts, layout parts, pipes and compressed gas tanks)

- `META-INF/services/gregtech.api.addon.platform.BehaviorMachineContributor` (when declaring any `BehaviorMachine`)

- `META-INF/services/gregtech.api.addon.platform.LayoutPartContributor` (when declaring any functional `LayoutPart`)

- `META-INF/services/gregtech.api.addon.platform.LayoutPartClient` (client side, when a functional part supplies a block-entity renderer)

- Machine blockstates at `assets/<addonId>/blockstates/<id>.json`, block models at `assets/<addonId>/models/block/machine/basic/<machineType>.json` (plus `<machineType>_running.json` and `<machineType>_active.json`), and item models at `assets/<addonId>/models/item/<id>.json` (GregTech generates blockstates and models for layout parts, pipes and compressed gas tanks unless the addon overrides them)

- Recipe-table GUI textures at `assets/<addonId>/textures/gui/recipes/<file>.png` (for `.gui("<addonId>:textures/gui/machines/<file>")`) and behavior-machine GUI sheets at `assets/<addonId>/textures/gui/<sheet>.png`

- Prefix textures at `assets/<addonId>/textures/<texture>.png` and optional custom layout-part textures at `assets/<addonId>/textures/<texture>.png`

- Language files at `assets/<addonId>/lang/en_us.json` (`block.<addonId>.<id>`, prefix `item.<addonId>.<snake_name>`, behavior toggle keys, structure tooltip keys) and `assets/gt6ce/lang/en_us.json` (`material.gt6ce.<lowercasename>`)

```java
public final class MyMachines implements AddonMachineContributor {
    public String addonId() { return "my_addon"; }
    public List<RecipeTable> recipeTables() {
        return List.of(RecipeTable.machine("my_addon.recipe.press")
                .title("Press").gui("my_addon:textures/gui/machines/press")
                .inputItems(1).outputItems(1).minimalInputItems(1).minimalInputs(1)
                .needsOutputs(true).build());
    }
    public List<AddonMachine> addonMachines() {
        return List.of(RecipeMachine.builder(
                MachineCasing.builder("press", "Steel").machineType("mypress").tier(1).build())
                .energy(EnergyInput.builder("EU", 32).build())
                .recipeMap("my_addon.recipe.press").basic().build());
    }
}
```

The casing material is a material name. Energy is the short name, such as `EU`.

### Casings

`MachineCasing.builder(id, material)` is the shell of all three templates. `id` matches `[a-z0-9/._-]+` and is the block path. `material` is non-blank. Startup requires a material that already exists. A layout part uses the same material lookup.

`machineType`, when set, matches `[a-z0-9_]+`. Leaving it unset is not the same on every template. A recipe machine and a fuel generator copy `id` unchanged and do not replace `/`. An id that contains `/` then fails, because the machine type still has to match `[a-z0-9_]+`. A behavior machine replaces each `/` with `_`.

`tier` defaults to 1 on the templates that accept it. A recipe machine only requires `tier >= 1`. A behavior machine requires 1 to 255. A fuel generator has no tier. A casing that called `tier` fails when that fuel generator is built.

### Items and fluids

`MachineIo` is for a recipe machine and a behavior machine. A fuel generator that sets one fails. A behavior machine moves items and fluid through slots, in Behavior machines, and may limit those slots by face. A fuel generator does not declare faces. A single block opens its fluid capability on all six sides. A multiblock opens only the front. Both still scan all six sides when they pull from an adjacent compressed drum.

Leaving `io` unset keeps six-side item and fluid access and no automatic face. Once a `MachineIo` is set, a channel that was not named is an empty face list, not the six-side default. Automatic faces default to none. A null automatic face fails at `build()`.

```java
MachineIo.builder()
        .itemInputs(MachineFace.TOP)
        .itemOutputs(MachineFace.BOTTOM)
        .itemAutoOutput(MachineFace.BOTTOM)
        .build()
```

`IoTarget.at(right, up, back, receiver)` is one large-recipe cell. `right` is clockwise from the facing, `up` is up, and `back` is opposite the facing. The receiver is a `MachineFace`. The builder methods are `itemOutputTarget`, `fluidOutputTarget`, `itemInputSource` and `fluidInputSource`. A declared output is pushed to that face. A declared input is pulled from that face. A channel left unset keeps its face rule. Naming a target leaves that channel's automatic face unset, so the same stack or fluid is not moved twice. Setting both fails at `build()`. Only `largeRecipe()` may name a target. A single block that names one fails.

The cell does not have to be a structure part, and it may sit inside or outside the layout. An unloaded chunk skips that channel for that tick. Item transfer runs when the formed machine runs its automatic item transfer. Fluid output runs at the start of the machine tick, including while the machine is stopped or the structure is incomplete, matching the built-in large machines. Built-in controllers keep their own positions.

The three coordinate rules stay separate. A structure cell rejects the origin and a repeated position, and it has no ±16 limit. An `IoTarget` rejects the origin, and each of `right`, `up` and `back` is from -16 to 16, including both ends. A fuel `behind` distance is measured past the front; see [Limits](#limits). It has no `right` or `up`.

### Recipe tables

`RecipeTable.machine(name)` and `RecipeTable.fuel(name)` take the same fields and do not share a name. A recipe machine names a machine table. A fuel generator names a fuel table. `recipeMaps` on a behavior machine may name either, and GregTech does not craft those rows for the behavior.

`name` matches `[a-z0-9_.]+`. `title` and `gui` are required. A missing value throws `IllegalArgumentException` at `build()`. A table `gui` is not a behavior sheet: it does not have to be a namespaced `.png`, and it is not checked against a widget rectangle. The behavior sheet rule is in Behavior machines. Counts default to 0 and must be at least 0: `inputItems`, `outputItems`, `minimalInputItems`, `inputFluids`, `outputFluids`, `minimalInputFluids` and `minimalInputs`. `needsOutputs`, `combinePower`, `bucketIn` and `bucketOut` default to false.

```java
RecipeTable.machine("my_addon.recipe.press")
        .title("Press")
        .gui("my_addon:textures/gui/machines/press")
        .inputItems(1).outputItems(1).minimalInputItems(1).minimalInputs(1)
        .needsOutputs(true)
        .build()
```

A positive `eu_per_tick` on a fuel table is energy produced per tick and is stored negative. `AddonRecipes.map` returns a fuel table the same way it returns a machine table. `find` returns a matching row. `Line.euPerTick` is the stored number, so a fuel row read back is negative. `AddonRecipes.Map.add` stores the value as written, so a fuel row added there is already negative. A non-negative stored value is not energy to produce. The row still needs an output. A recipe machine that names a fuel table fails as an unknown recipe map. Reusing a machine-table name or a fuel-table name fails startup and names the addon.

The GUI path keeps a namespace written before `:`. Both `textures/gui/machines/Press` and `gt6ce:textures/gui/machines/Gas` still resolve to `gt6ce:textures/gui/recipes/<file>.png`. An addon path `my_addon:textures/gui/machines/press` resolves to `my_addon:textures/gui/recipes/press.png`, so the file on disk is `assets/my_addon/textures/gui/recipes/press.png`.

New tables are created before `onRecipesReady`, JEI, EMI and the datapack baseline, and they join `RecipeMap.RECIPE_MAP_LIST`. Add rows from `onRecipesReady` or with a `gt6ce:machine` datapack. The recipe page draws the slots the table declared. A fuel table with one or more item inputs draws those item slots. A built-in fuel table that declares no item slots does not add a slot for an item that happens to be on a row.

### Recipe machines

`RecipeMachine.builder(MachineCasing)` is only the recipe knobs. The casing, the energy input, the item and fluid faces, and the layout are the types in the sections above.

`auxiliaryEnergy(EnergyInput)` adds a second input buffer to a recipe machine. Its rules match the behavior machine's auxiliary input: its own faces, a positive capacity, and either a packet range or `anyPacket()`; the same type on a shared face fails. `startupEnergy(true)` requires `auxiliaryEnergy`: each new recipe's `special` becomes a startup requirement, paid from the second buffer before progress starts. The requirement is stored as `gt.startup_energy` and the buffer as `gt.aux_energy`. The second buffer never replaces the main energy buffer.

`basic()` is the default and builds one block. `largeRecipe()` builds a large recipe machine. There is no kind enum. Distillation, matter fabricators and the other built-in controllers stay inside GregTech. `largeRecipe()` is the kind that requires a layout and the only kind that may name an `IoTarget`. When that layout is required, and when the structure counts as formed, is under Structures.

`recipeMap` is required. It matches `[a-z0-9_.]+` and must be a machine table this contributor returned, or a built-in machine map.

`parallel` defaults to 1 and is at least 1. `efficiency` uses the scale, default and bounds in [Limits](#limits). `constantEnergy` defaults to true. While the machine is not a TU machine it draws a constant amount. TU still does not. Built-in machines keep the old name-based efficiency, constant-energy and parallel values. Those rules are copied onto the spec when the built-in spec is constructed.

`cheapOverclocking(true)` enables the cheaper overclock and records that the addon set it. Leaving the method unset leaves that record false. A single block that does not call it stays off, including when its machine type would have matched a built-in cheap machine. A large recipe machine that does not call it stays cheap unless it uses TU.

`minimumLightAbove(14)` refuses to start a new job unless `Level.getRawBrightness` of the block above is at least 14. That is the 1.7.10 `getBlockLightValue` reading: the greater of sky light and block light, without subtracting night. The value is 0 to 15. `0`, the default, does not check. A job that has already started keeps its progress.

`searchOutputTanks(true)` runs a second recipe search on the output tanks when the input tanks do not match. Item slots stay the input slots. The job remembers which tanks matched and drains fluid from those tanks, including the parallel batch. A match on the input tanks is used as it stands. Fluid the recipe itself produces is still delivered to the output tanks. If the input tanks miss on the next search, that fluid can be consumed. Leaving the method unset keeps the search on the input tanks.

`exclusiveOutputs(true)` rolls the first item output once, in parts per 10000. A roll below that chance produces the first item. Any other roll produces the second. The produced count is that item's recipe count times the parallel batch. Fluid outputs stay certain. Every static row on that machine's recipe table needs exactly two non-empty item outputs. Any other row fails startup and names the addon and the machine. JEI and EMI show each of the two chances. The second chance is the hint. The roll does not read it. Leaving the method unset rolls each item output on its own.

`structureKeys` are translation keys for the structure tooltip. Each matches `[a-z0-9_.]+`. The list may be empty. They are not the layout.

Two blocks may share a machine type when they are the same addon and agree on the recipe table, single block or large, efficiency, the constant-energy flag, structure keys, face masks, whether cheap overclocking was set, the light gate, output-tank search, exclusive outputs, the energy range and the four positions. That is how two tiers of one machine share a GUI and a structure.

JEI catalysts and the multiblock preview follow the registered machine list. A large machine with a layout is included in the preview. The crafting recipe is the addon's own datapack. GregTech does not generate one. An addon large machine uses the default port path. Built-in name branches stay in place for built-in machines.

### Machine controls

`MachineControl` is in the SDK. A block entity that implements `MachineControl.Provider` returns a control for a face; `MachineControl.find(entity, side)` resolves it and returns `null` when that face has none. Relays resolve the target on every operation, so do not cache the control beyond the call.

```java
var control = MachineControl.find(level.getBlockEntity(pos), side);
if (control != null && control.available()) {
    boolean on = control.enabled();
    long energy = control.energy();
    long capacity = control.energyCapacity();
    long perTick = control.progressPerTick();
}
```

`enabled()`/`setEnabled(boolean)`, `running()`, `active()`, `progress()` and `progressMax()` are the switch and progress surface. `energy()` is the buffered energy, `energyCapacity()` its ceiling, and `progressPerTick()` the work the next active tick adds (0 while idle). These three are read-only.

`supportsWrite()` is `false` on built-in machines and `setProgress`/`setEnergy` return `false`: API version 0 does not let an addon rewrite a built-in machine's buffer or progress. An addon that supplies its own control should keep the same split and answer `supportsWrite()` from its own checks.

`externalSignal(side)` and `setExternalSignal(side, level)` are a control input, not a state write, so they work even while `supportsWrite()` is false. The level is 0–15 and is saved with the machine. It is combined with that face's cover signal (the larger value wins) before the cover's `allows` gate runs, so an external controller can stop or start a machine through a redstone switch cover on that face. A value outside 0–15 is rejected.

### Heat

`HeatHandler` is in the SDK at `gregtech.api.machine`. A block entity that implements it exposes a signed HU buffer directly. `TemperatureSource` (under Sensor sources) stays the read-only temperature interface; use `HeatHandler` when the addon also writes heat.

```java
if (level.getBlockEntity(pos) instanceof HeatHandler heat) {
    long stored = heat.heatStored();       // signed: cooling is negative
    long capacity = heat.heatCapacity();   // Long.MAX_VALUE when unbounded
    long accepted = heat.insertHeat(8_000, true);
    long removed = heat.extractHeat(2_000, false);
}
```

Amounts are signed heat units: a positive value heats and a negative value cools. Both setters return the signed amount actually accepted or removed. `simulate` must not change state and returns what the executed call would return. `temperatureK()` is never negative; machines that do not model temperature (boilers and heat exchangers) report the environment temperature, while the crucible reports its own Kelvin.

The adapters are the single-block and original large boilers, the single-block and large heat exchangers, and both crucible trees. Each keeps its own rules: an unformed large boiler refuses input, an overfilled boiler still explodes on its next tick, and a crucible still converts stored HU into Kelvin on its next tick. Writing heat is a direct bypass of the machine's normal energy input, not a new processing path.

### Machine tuning

`MachineTuningContributor` in `META-INF/services/gregtech.api.addon.MachineTuningContributor` overrides built-in machine variants. GregTech loads it before the built-in catalog is converted into specs. `MachineTuning` is keyed by `machineName` and `tier`; only non-null fields change: `parallelLimit`, `energyIn`, `energyInMin`, `energyInMax`, `processingEfficiency` (parts per 10000), `constantEnergy` and `cheapOverclocking`. `id`, material, energy type, recipe map, faces, structure and auxiliary energy stay fixed.

After omitted fields inherit built-in defaults, the effective nominal input must lie within the effective input range, including when only one field was overridden. An out-of-range result fails startup. The effective fields then go through the canonical `BasicMachineSpec` constructor.

A second addon may repeat the same value; a different value for the same `(machineName, tier, field)` fails startup and names both addons, the field and both values. A key that matches no built-in row also fails startup. A tuned `parallelLimit` is capped at 65536 and `energyIn × parallelLimit` must fit in a long. The patch exists only at runtime: it is recomputed on every startup, is never written into a save, and removing the addon restores the built-in values. Startup logs one `[gt6ce] tuning: ...` line per field and writes `config/gt6ce-tuning.txt`. Version 0 has no datapack form.

### Fuel generators

`FuelGenerator.builder(MachineCasing)` burns one fuel table. The packet and the place it leaves are a `FuelOutput`, under Energy. Do not rewrite that packet as a size times a count. `amperes(n)` says how many of those packets leave in one tick. The default is 1. The limit is 1 to `FuelGenerator.MAX_AMPERES`, which is 64. A tick that has fewer packets stored sends only those.

`efficiency` is parts per 10000, with range and default in [Limits](#limits). That is the same range as a recipe machine. `tank` is the fuel-tank capacity in millibuckets. It defaults to 4000. The fuel tank and every exhaust tank use the capacity bound in [Limits](#limits). A behavior fluid slot uses that same upper bound and has no default: the slot names its own capacity. The exhaust count is the fuel table's fluid output count. The fuel table needs at least one fluid input.

`single()` is the default. `multi()` is the multiblock. Which of them may own a layout, and when it starts formed, is under Structures. An unformed multiblock does not burn and does not emit. Both kinds are JEI and EMI catalysts for their fuel table. The stop switch is the ordinary machine control. A multiblock is in the structure preview.

```java
public List<RecipeTable> recipeTables() {
    return List.of(RecipeTable.fuel("my_addon.fuel.gas")
            .title("Gas").gui("my_addon:textures/gui/machines/gas")
            .inputFluids(1).outputFluids(1).minimalInputFluids(1).minimalInputs(1)
            .build());
}

public List<AddonMachine> addonMachines() {
    return List.of(
            FuelGenerator.builder(MachineCasing.builder("engine", "Steel").build())
                    .fuelMap("my_addon.fuel.gas").efficiency(10_000)
                    .output(FuelOutput.builder("RU", 32).front().build())
                    .tank(4_000).single().build(),
            FuelGenerator.builder(MachineCasing.builder("turbine", "Steel").machineType("sample_turbine").build())
                    .fuelMap("gt.recipe.fuels.gas")
                    .efficiency(5_000)
                    .output(FuelOutput.builder("RU", 32).behind(4).build())
                    .tank(8_000).multi().build());
}
```

A generator may name an addon fuel table or a built-in table such as `gt.recipe.fuels.engine` or `gt.recipe.fuels.gas`. The buffer spends the packet only after a receiver accepts it. This template does not convert one energy type into another, does not burn solid fuel, and leaves the built-in diesel engine and gas turbine on their own block entities. A behavior can burn a fuel row itself, including a row with an item input. That burn belongs to the behavior, not to this template.

### Pipes

`fluidPipes()` and `itemPipes()` add pipe rows. GregTech expands each row into the built-in sizes and appends those blocks after the built-in pipes.

```java
public List<FluidPipeDeclaration> fluidPipes() {
    return List.of(new FluidPipeDeclaration("teflon", "Teflon", 125, true, true, false, false, 600, 0));
}

public List<ItemPipeDeclaration> itemPipes() {
    return List.of(new ItemPipeDeclaration("pvc", "PVC", 32768, 1));
}
```

`idSuffix` is the material part of the block path. A fluid suffix that matches a built-in fluid pipe, or another addon's fluid pipe, fails startup and names both sides. Item pipes use the same rule inside the item-pipe catalog. The same suffix may be a fluid pipe and an item pipe, because the paths differ. `maxTemperature` of `0` means the material melting point times 1.25. `stepSize` is the medium item pipe's routing cost and must be at least 4.

A fluid row becomes seven blocks, tiny through nonuple. An item row becomes six, including the three restrictive sizes. The block ids are `<addonId>:pipe_<size>_<suffix>` and `<addonId>:item_pipe_<size>_<suffix>`. GregTech registers them on its own bus, in the addon's namespace, and does not insert them into the `gt6ce` pipe registers. They use the existing `gt6ce:fluid_pipe` and `gt6ce:item_pipe` block-entity types, so fluid and items save with the block.

Crafting, welder rows and material recovery use the built-in pipe rules. A material with no `plateCurved` skips the shaped recipe and the welder row. The addon ships `block.<namespace>.<path>` translations. GregTech emits a placeholder blockstate; the dynamic pipe model replaces it. These rows are not built-in pipes.

A block that implements `FluidPipeConnectable` or `ItemPipeConnectable` needs no wrench: a pipe connects to it when the pipe is placed against it, when the block is placed next to an existing pipe, and after a chunk reload. Implement the interface on the block or on its block entity; `side` is the face of that block facing the pipe, and `false` leaves the face closed. This is opt-in, so built-in machines and existing pipes keep their current connections.

A pipe remembers every face a wrench closed and saves that mask with the block. Automatic connection leaves those faces alone across neighbour changes, chunk reloads and saved-world restarts; using the wrench to reconnect clears the mark. Automatic connection only completes a missing link: it never disconnects a face, and two pipes still need one side to point at the other before they join.

### Compressed gas tanks

`AddonMachineContributor.compressedGasTanks()` returns `CompressedGasTankDeclaration`s. The id is a lower-case registry path. The material must already be registered. Capacity is absolute millibuckets, from the capacity bounds in [Limits](#limits). It is not a compression multiplier. Gas proof is always true. Acid, plasma and magic proof are the pressure rating. The temperature ceiling is the material melting point, resolved when machines install.

```java
@Override public List<CompressedGasTankDeclaration> compressedGasTanks() {
    return List.of(new CompressedGasTankDeclaration(
            "sample_drum", "Steel", 12_800_000, false, false, false));
}
```

Ordinary insertion is closed. `fill` from a pipe, funnel or container returns 0, and `isFluidValid` is false. Drain stays open, so pipes and fuel generators can take the gas. A fuel generator pulls at most 1000 mB per tick from an adjacent compressed-gas receiver, and only a gas its fuel map accepts. A drum gives gas on every side. A behavior machine gives it only through `gasValve`. Anything the fuel tank cannot hold is pushed back. Built-in gas turbines and diesel engines do not pull.

A compressor is a behavior machine. `pushCompressedGas` lives on the behavior context. There is no separate recipe shell. The sample compressor takes KU from the bottom and offers the gas in its tank to the drum on the back. The offer is that tick's KU times 10 mB. The KU is spent even when the drum stores nothing.

`AddonFluids.fluid` and `still` return `FluidStack.EMPTY` when the field is unknown, an argument is null, or the amount is not positive. They do not return null. `AddonFluids.gas` reports whether a fluid is a gas. An empty fluid is false. `AddonFluids.acid`, `AddonFluids.plasma` and `AddonFluids.magic` report acid, plasma and magic fluids; each has `Fluid` and `FluidStack` overloads, and null or empty is false. Temperature still comes from the fluid's own `getTemperature`.

The block id is `<addon>:<id>`. Hardness is 1, blast resistance is 6, and the item stacks to 16. The creative tab files a class whose name contains Tank under fluid containers. GregTech emits an iron-block model, which an addon may replace. The display name is `block.<addon>.<id>`. The tooltip keys are `gt6ce.tank.compressed.store` and `gt6ce.tank.compressed.compressor`. Neither key is an original GT6 translation, so the shared Chinese file keeps the English text. An invalid stored fluid is emptied on tick. There is no fermentation and no auto output. The large valve multiblock is not included.

## Behavior machines

Return each `BehaviorMachine` from `addonMachines()`. Also list a `BehaviorMachineContributor` in `META-INF/services/gregtech.api.addon.platform.BehaviorMachineContributor`. That interface lives in the loader API because `FluidStack` differs between Forge and NeoForge. `machineTypes()` must include every behavior machine type this addon declares. GregTech calls `create` once for each placed block entity. A missing factory fails startup and names the addon and the machine type.

### Unified storage access

New behavior code should use `context.inventoryAccess()` and `context.fluidAccess()`. Both `MachineBehaviorContext` and `PartContext` expose these same interfaces. They are host-owned processing views, not sided pipe capabilities: processing bypasses face, insertion-filter and input/output-role restrictions. Use them only during the current server callback, and never retain a view or context for a later tick or another thread.

| View | Operations | Contract |
| --- | --- | --- |
| `InventoryAccess` | `slots()`, `snapshot(slot)`, `set(slot, stack)`, `extract(slot, amount, simulate)` | Reads and extraction results are detached stacks. `set` copies its argument and notifies the host; null means empty. It follows the host-specific replacement policy, not capacity-checked insertion. |
| `FluidAccess` | `tanks()`, `snapshot(tank)`, `capacity(tank)`, `fill(tank, stack, simulate)`, `drain(tank, amount, simulate)` | Amounts/capacities are mB. Read/drain results are detached. Fill does not change its argument and returns accepted mB. |

Part-host replacement clamps to its stack limit; the legacy machine-host setter does not. Do not treat `set` as an insertion that reports overflow. Both views reject invalid indexes, including empty operations. Negative requested amounts throw; zero-amount `extract`, `fill` and `drain` do nothing. Setting an empty stack clears the slot. `simulate=true` does not change storage or notify the host. Calls are individual operations, not a transaction across slots, tanks or energy; a successful simulation does not reserve resources for a later callback.

```java
var inventory = context.inventoryAccess();
ItemStack preview = inventory.extract(0, 1, true); // leaves slot 0 unchanged
if (!preview.isEmpty()) {
    ItemStack taken = inventory.extract(0, 1, false); // persists the removal
    // Process the actual returned stack here.
}
var tanks = context.fluidAccess();
FluidStack contents = tanks.snapshot(0); // editing contents never changes the tank
int capacityMb = tanks.capacity(0);
```

Legacy reads retain their original ownership for existing addons: machine `item(slot)` can be a live stack while machine `fluid(tank)` is detached; part `item(slot)` is detached while part `fluid(tank)` can be live. Never modify those legacy results to change storage; write through the host or use the new views. `BehaviorState.item(key)` is also detached; persist a changed copy with `setItem(key, stack)`.

| Legacy call | Machine context | Part context |
| --- | --- | --- |
| `item(slot)` | live stack | detached copy |
| `setItem(slot, stack)` | stores a copy; null clears; does not cut to the slot limit | stores a copy; an out-of-range slot throws; count is cut to the slot limit |

### Explicit energy accounting

`MachineBehaviorContext.energyAccess()` returns `MachineEnergy`. Its names distinguish owned-buffer transfers from externally accounted supplies. Existing energy methods remain callable; the new view adapts them without changing declared channels, faces, packet windows or overload behavior. It executes immediately and has no simulation mode.

| Method | Ownership and returned quantity |
| --- | --- |
| `mainInputStored()`, `consumeMainInput(units)` | Read the main input or atomically spend the requested units; consumption returns success. An output-only machine has no main input to read. |
| `auxiliaryStored(type)`, `consumeAuxiliary(type, units)` | Address the existing auxiliary input only; unknown types return zero / false. |
| `generate(units)`, `capacity()`, `setCapacity(units)` | Add to or configure the host's existing main buffer. `generate` returns accepted units, including on output-only machines. |
| `emitStored(size, count)`, `emitStoredTo(pos, side, size, count)` | Debit the host for accepted whole packets. Return `EnergyTransfer`; `side` is the receiving block's world-facing side, or null for unsided. |
| `transferStoredToPart(part, size, count)` | Debit the host for units accepted by a bound part and arm its output. The part emits to neighbors on its own tick. |
| `supplyPart(part, type, size, count)` | Supply energy owned outside the host buffer. This does **not** debit the host; the caller must account for exactly the accepted units in its own source. |

`EnergyTransfer` reports `packetSize()`, `requestedPackets()`, `requestedUnits()`, `transferredUnits()`, `completePackets()`, `partialPacketUnits()` and `remainingUnits()`. Part buffers can accept part of a packet, so `transferredUnits()` is authoritative; do not account only `completePackets()`. Negative packet sizes retain the declared KU/RU direction; unit quantities remain nonnegative. Invalid negative unit/count requests and unrepresentable packet magnitudes are rejected before transfer; overflowing requests throw. Zero packets is a no-op. The result describes that call, not a reservation or a second operation to execute.

```java
var transfer = context.energyAccess().emitStored(32, 1); // requires compatible declared output
long deliveredUnits = transfer.transferredUnits();
long unsentUnits = transfer.remainingUnits();
// The host already debited deliveredUnits. Do not consume it again.
```

### Existing callbacks and declarations

Implement `MachineBehavior`; per-method details are in [SDK source Javadoc](../multiversion/src/api/java/gregtech/api/addon/platform/MachineBehavior.java).

| Callback / entry | When called | Default / responsibility |
| --- | --- | --- |
| `tick(context)` | Server tick | Required; implement machine logic |
| `canInsert` / `canFill` | Item/fluid input | Required unsided methods; face overload delegates to them; null face means menu/unsided |
| `acceptFluid` / `acceptsEnergy` | Pushed input | Optional acceptance hooks; see storage and energy rules |
| `MachineBehavior.onToggle` / `beforeBreak` / `onTool` / `onUse` | Toggle, break or interaction | Optional; inventory/state items drop after `beforeBreak` |
| `getCrucibleMaterial` | Crucible read | No material |
| `getCrucibleTemperature` / `getCrucibleContentAmount` | Crucible read | Zero |
| `fillMoldAtSide` | Mold, basin or faucet interaction | Refuses pouring; bridge supplied by GT, no addon block-entity subclass needed |
| `PartContext.crucible()` | Bound functional-part read | One view of controller material, amount and temperature |
| `CrucibleMold.fillMold` | Behavior chooses to pour | Returns accepted amount; subtract it from behavior-owned molten storage |

| Call | Destination | Accounting / result |
| --- | --- | --- |
| `pushToPart(part, type, size, amount)` | Controller → two-way part | Does not debit controller storage |
| `emit(part, size, amount)` / `emitToPart(...)` | Controller → part buffer | Spends controller storage; neighbor send runs on the part tick |
| `emit(size, amount)` | Controller → declared faces | Returns accepted packet count |
| `emitTo(target, side, size, amount)` | Controller → saved coordinate, entering `side` | Uses declared output type/packet limits, ignores declared faces, debits accepted packets, returns accepted packet count |
| Remote target | Coordinate in `BehaviorState` | Unloaded/missing/non-receiver targets accept nothing; target object is not retained |

`MachineBehaviorContext` exposes items, tanks, energy, toggles, progress and world access. Prefer the aliases `tankCapacity(tank)`, `auxiliaryEnergy(type)` and `emitToPart(...)`; the shorter names stay. `BehaviorState` persists int, long, boolean, string and item values to NBT without exposing `CompoundTag`. See [Conventions](#conventions) for absent values and [Limits](#limits) for keys and strings.

```java
public List<AddonMachine> addonMachines() {
    return List.of(BehaviorMachine.builder(
            MachineCasing.builder("irrigator", "Steel").machineType("irrigator").tier(1).build())
            .energy(EnergyInput.builder("EU", 32).build())
            .gui("my_addon:textures/gui/machine.png")
            .item(44, 26, BehaviorMachine.SlotRole.INPUT)
            .item(116, 26, BehaviorMachine.SlotRole.OUTPUT)
            .fluid(8000, 152, 8, BehaviorMachine.SlotRole.INPUT)
            .toggle("my_addon.irrigator.water", 26, 64, true)
            .build());
}
```

To make a behavior machine a molten-material source, keep the material and amount in
`BehaviorState` (or another behavior-owned store) and implement the four crucible
callbacks:

```java
@Override
public String getCrucibleMaterial(MachineBehaviorContext context) {
    return context.state().getString("material");
}

@Override
public long getCrucibleTemperature(MachineBehaviorContext context) {
    return context.state().getLong("temperature");
}

@Override
public long getCrucibleContentAmount(MachineBehaviorContext context) {
    return context.state().getLong("amount");
}

@Override
public boolean fillMoldAtSide(MachineBehaviorContext context, CrucibleMold mold,
                               Direction crucibleSide, Direction moldSide) {
    var state = context.state();
    var material = GregTechMaterials.find(state.getString("material"));
    long amount = state.getLong("amount");
    long accepted = amount <= 0 || !material.valid() ? 0
            : mold.fillMold(material, amount, getCrucibleTemperature(context), moldSide.ordinal());
    if (accepted <= 0) return false;
    state.setLong("amount", amount - accepted);
    return true;
}
```

GregTech draws the GUI from the declaration and the addon's texture. The texture is both the panel and the widget sheet. Widget pixels use the constants on `BehaviorMachine`: slot `UV_SLOT_U`,`UV_SLOT_V` at 176,0 (18×18); energy empty `UV_ENERGY_U`,`UV_ENERGY_V` at 194,0 and full `UV_ENERGY_FULL_U` at 204,0 (10×54); tank `UV_TANK_U`,`UV_TANK_V` at 214,0 and marks `UV_TANK_MARKS_U` at 232,0 (18×54); progress empty `UV_ARROW_U`,`UV_ARROW_V` at 176,54 and full `UV_ARROW_FULL_V` at 71 (24×17); toggle off `UV_TOGGLE_OFF_U` at 176 and on `UV_TOGGLE_ON_U` at 194, both at `UV_TOGGLE_V` 88 (18×18). The panel defaults to 176 by 166. `guiSize` bounds are listed in [Limits](#limits).

The energy gauge defaults to (8, 8). Progress defaults to (80, 35). The player inventory starts `PLAYER_INVENTORY_GAP` (82) pixels above the bottom. Item slot coordinates are the inner 16 pixels. The frame is drawn one pixel up and left, so a slot needs `x` and `y` of at least 1. Widgets must end at or above the player inventory and must not overlap. Slot, tank and toggle caps are in [Limits](#limits). A toggle key matches `[a-z0-9_.]+`.

A fluid slot names its own capacity. The capacity is the capacity bounds in [Limits](#limits) mB. The fuel tank's default is a different rule and is written under Fuel generators.

`io(MachineIo)` uses the same face lists as a recipe machine. Leaving it unset keeps item and fluid capabilities open on every face. Once it is set, a channel that was not named is closed, not the six-side default. A closed face has no capability. The menu and `items()` / `fluids()` stay unsided, so a player menu still reaches every slot role. A bucket uses the clicked face. An automatic face fails at `build()`. An `IoTarget` fails too. The declaration fixes the faces. There is no runtime edit.

`BehaviorMachine.SlotRole` is `INPUT`, `OUTPUT` and `ANY`. Players and pipes insert into `INPUT` and `ANY` only when `canInsert` or `canFill` allows it. A face listed as an input does not extract, and a face listed as an output does not insert. The slot role still applies on an open face. Pipes extract items from `OUTPUT` and `ANY`. Pipes drain fluid only from `OUTPUT` and `ANY`, so an input tank stays input-only. `insertOutput` fills `OUTPUT` and then `ANY`, and it does not ask `canInsert`. `acceptFluid` is the pipe and container fill path. The default stores the offered fluid when `canFill` allows it.

Override it when a concentrate should become the tank's fluid. `context.fill` stores the offered fluid when the tank is empty or already holds it, and it does not call `canFill`. `pushCompressedGas(face, stack)` stores gas in the compressed-gas receiver on that face. A single-block drum accepts on every side. A behavior machine accepts only on the face named by `gasValve(MachineFace)`, and can give gas back through that same face. The write is immediate and the method returns the millibuckets stored. The behavior removes that fluid from its own tank. A pipe or an ordinary tank accepts nothing.

`gasValve` does not open that face to pipes.

| Field | Default | Allowed / meaning | Failure / effect |
| --- | --- | --- | --- |
| Tick order | Poll adjacent emitters → behavior → rated drain | Input faces, range and capacity follow [Energy](#energy) | `consume` spends stored energy before the final drain |
| `generate(amount)` | Same buffer | Returns the amount that fits | Without a main input, generation/emission uses one full output packet buffer |
| `setCapacity` | Declared capacity | Changes this tick; first call starts persisting `gt.capacity` | Old saves without the key use the declaration |
| Stored energy over cap | Visible to behavior | Behavior may break the block; shell discards excess after callback | The shell itself does not explode merely for excess stored energy |
| Working state | `working(true)` lights face | Running means energy existed at tick start | Does not imply a recipe was processed |
| `acceptsEnergy` | true | Pushed packets ask after face and buffer checks; type distinguishes main/auxiliary | false rejects storage and overload; pulling from a neighbor does not ask |
| `overload(false)` | Overload enabled | Refuses packets outside the window | Overlarge packets may destroy the machine when overvoltage explosions are enabled |
| Undersized packet | Unset half-to-double window stores it with overload on | An explicit range refuses packets below its minimum | See [Energy](#energy) for packet windows |
| Omitted main input | Allowed with output | No receiving faces or energy bar; `energy()` / `consume(long)` return 0 / false | Neither input nor output fails; output buffer is `maxSize` × `maxAmount` |

`energyOutput(EnergyOutput)` declares the output described under Energy. `emit(size, amount)` spends the absolute packet size and pushes that packet through `EnergyTransfer.emitEnergyToSide` on each output face, in declaration order, until the requested amount is sent or the neighbors stop accepting. A negative size is sent with its sign and spends the absolute value, and only when `allowNegative()` was set. It returns how many packets left. A size outside `minSize`..`maxSize`, or an amount above the declared maximum, throws. GregTech does not clamp it. `emit` on a machine with no output throws. Neighbors do not pull this stored energy. The behavior pushes it. A `remote()` output may declare no face; `emit` then sends nothing, and `emitTo` is the path to a saved coordinate.

When the machine type has a layout, GregTech forms it before the behavior runs. `structureFormed()` is then true. `parts()` lists every bound addon part in that layout. `energyParts()` is the energy view of the same binding: output parts and condition parts together. A casing is in neither list. With no layout, formed becomes true on the first server tick and the part list is empty. `charge(amount)` fills the buffer that part has, up to its capacity, and returns what fit, or 0 when that part is no longer bound to this machine.

On a condition part that buffer is the input buffer, and `charge` does not check packet size. `setOutput(size, amount)` arms an output part's packet and clears its stopped flag. A request above the part's declared maximum throws. `setOutput` on a condition part throws, because that part declares no output. `stop()` stops an output part. `EnergyPart.condition()` is true for a condition part. `satisfied()` is whether that part paid its condition on its own tick. `conditionsMet()` is true when every bound condition part is satisfied. An unformed machine has no bound parts, so it is true.

`tick` still runs when a condition part is short.

`auxiliaryEnergy(EnergyInput)` adds a second input on a behavior machine. It needs its own faces, a positive capacity, and either a packet range or `anyPacket()`. Leaving both unset fails. A face shared with the main input fails when both use the same energy type. Different types may share a face. `perTick` is from the per-tick bounds in [Limits](#limits). When a range is set, `perTick` must sit inside it and is the recommended size. The main input accepts `range` and `anyPacket()` on those same packet rules. A main `anyPacket()` needs a positive capacity. A main range needs `perTick` inside it.

Packets on the auxiliary faces enter a second buffer. They do not enter the main buffer, and the tick-end drain does not take them. The buffer is saved as `gt.aux_energy` when the machine declares an auxiliary input. A missing key is empty. `energy(type)` and `consume(type, amount)` read and spend only that buffer. The match ignores case. Any other type, including the main type, reads 0 and cannot be consumed. The main buffer stays on `energy()` and `consume(long)`. `anyPacket()` accepts any packet size and does not overload. A ranged auxiliary input overloads above its own maximum, separately from the main input.

A packet below its minimum is refused.

A behavior machine's structure ports now forward GregTech input and output buses, fluid hatches and energy hatches. A formed cell with `ITEM_FLUID_IO`, `ITEM_FLUID_INPUT`, `FLUID_OUTPUT` and the other item or fluid roles reaches the behavior machine's own slots and tanks, limited by each slot or tank's `INPUT`/`OUTPUT`/`ANY` role. `ENERGY_INPUT`, `ENERGY_OUTPUT` and `ITEM_FLUID_ENERGY*` cells reach the main input, the auxiliary input or the output buffer. An unformed structure, a wrong role or a wrong type returns 0. Ports work by structure position and do not add the controller's own `MachineIo` face limits.

The behavior block entity stays an addon `EnergyBlock`. It does not also implement `IEnergyBlock`, because `getEnergyTypes` on the two interfaces erases to one method. Axles, laser fiber, relays, long-distance transformers, and the built-in machines that inject or extract resolve the neighbor with `EnergyBlockViews`. A built-in block still takes the same path it did before. A gearbox already emitted through `EnergyTransfer`, so it already reached an addon block that accepts energy. Scanner, Jade and cover readouts still look only at `IEnergyBlock`.

`onTool(context, tool, player, hand, face)` runs before a wrench can rotate the block, on the side that received the click. `tool` is one of the `AddonTool` values, and `face` is the clicked face relative to the machine front. Return true to consume the click. GregTech then spends the usual machine tool-click durability on the server, does not rotate the block, and does not open the menu. The default returns false. A tool that is not an `AddonTool` never calls this, so a wrench still rotates the block and any other GregTech tool still refuses the menu. `AddonTools.matches` uses GregTech's tool table, including an electric tool and a pocket tool in the matching mode. It does not compare item ids.

`onUse(context, player, hand)` runs on the server after that tool check, an unhandled wrench, and a fluid container, and before the menu. Return true to handle the click. The default returns false and the menu opens.

`maxRange(int)` is the cube half-extent for world actions. Its bounds are in [Limits](#limits). The default is 0, and both world methods then do nothing. Distance is Chebyshev: the largest of the three axis gaps. The machine block itself is outside that cube.

`breakBlock(pos)` breaks one block and puts `Block.getDrops` into the output slots. It returns false, and leaves the block, when the chunk is not loaded, the block is air or has negative destroy speed, the drops do not fit, or this tick already broke a block. Before the block is removed, GregTech posts the loader break event with the player who placed the machine. An online placer is used as that player. Otherwise a platform fake player stands on the machine and carries the saved profile, or a fixed profile when the machine was not placed by a player. A cancelled event returns false. The scan index belongs to the behavior. `BehaviorState` already saves it, so a miner resumes after a restart. One successful break is allowed per tick.

`collectItems(radius, voidThem)` takes item entities whose boxes meet the cube of that radius. The radius cannot exceed `maxRange`. `voidThem` discards them. Otherwise each stack moves through `insertOutput`, and an entity that does not fit completely keeps the remainder. At most 16 entities are taken per tick. There is no item filter on this method.

A water wheel does not need another entry. `level()` and `pos()` see the blocks next to the machine:

```java
private static boolean adjacentWater(MachineBehaviorContext context) {
    for (Direction side : Direction.values()) {
        if (context.level().getFluidState(context.pos().relative(side)).is(Fluids.WATER)) return true;
    }
    return false;
}
```

The default block-entity id is under Registering machines. An explicit `blockEntityId` lets several blocks share one block-entity type. The path matches `[a-z0-9_./-]{1,64}`. They must be the same addon, the same machine type, and the same input energy short name, auxiliary energy short name, output energy short name, item roles, fluid roles and toggle keys. No auxiliary input is an empty name. Tank capacity, storage capacity, tier, energy per tick, output faces and widget positions may differ. A shared path that disagrees fails startup. The addon ships the blockstate, models and GUI texture. Toggle labels are the translation keys on the declaration. GregTech's behavior GUI strings are `gui.gregtech.behavior.energy`, `progress`, `tank`, `on` and `off`.

A behavior machine is not a recipe machine. GregTech does not look up a row, spend inputs, or write outputs for it. Recipe machines stay on `gt6ce:basic_machine`.

`recipeMaps(String... names)` lists machine tables or fuel tables. The block becomes a JEI and EMI catalyst for each name. A name is a table this addon declared, or a built-in table such as `gt.recipe.compressor` or `gt.recipe.fuels.engine`. Another addon's table is unknown. An unknown name fails startup and names the addon and the machine. A repeated name, or a name outside `[a-z0-9_.]+`, fails at `build()`. Leaving this unset adds no catalyst, so the lists already built from recipe machines and fuel generators stay as they are. The names do not make GregTech run the table.

`signalOutput(true)` lets the machine act as a GT signal source. The behavior implements `gtSignal(context, side)` and returns a strength in `GtSignalCarrier.UNIT` units; `side` is a `Direction`, the direction from the wire toward this block. The default returns 0. A machine without `signalOutput` keeps the block's `isSignalSource` false and does not change any existing signal network. The wire still clamps the result to `[0, 15 * UNIT]`.

`clientState(keys...)` lists behavior state keys GregTech syncs to the client. Only the declared keys are sent, a missing key reads as zero, false or an empty string, and a key that was not declared throws. The key-count cap is in [Limits](#limits). A client-only `BehaviorMachineClient`, listed in `META-INF/services/gregtech.api.addon.platform.BehaviorMachineClient`, supplies a screen and a block entity renderer per machine type. Override `screen(machineType, menu, inventory, title)`; the older `screen(menu, inventory, title)` is deprecated and kept for existing clients. `screen` returns null to keep the default GUI; a returned `Screen` is hosted over the menu, so item slots and the player inventory stay usable, and it reads `energy`, `progress`, `isOn`, `tankAmount`, `tankFluid` and `containerId` through `BehaviorMachineMenuView`. `renderer` returns null when the machine has no extra drawing; it reads the synced state through `BehaviorMachineClientView`. Both run on the client only, so a server without the addon's client half still starts.

```java
@Override
public Screen screen(String machineType, BehaviorMachineMenuView menu,
                     Inventory inventory, Component title) {
    return null; // keep the default menu screen
}
```

```java
@Override public long gtSignal(MachineBehaviorContext context, Direction side) {
    return context.state().getBoolean("armed") ? 15L * GtSignalCarrier.UNIT : 0;
}
```

The sample solid boiler lists a fuel table with one item input and one item output. Coal becomes charcoal, 16 EU per tick for 20 ticks. The behavior finds the row, spends the coal, puts the charcoal in the output slot, then each burn tick calls `generate` and `emit` from the top. The counters `left`, `eu` and `burn` live in `BehaviorState`. There is no new shell field. A fuel row stores produced energy as a negative `euPerTick`, under Recipe tables. `generate` takes a positive amount, so the behavior negates that stored value.

```java
BehaviorMachine.builder(MachineCasing.builder("sample_boiler", "Steel").machineType("sampleboiler").tier(1).build())
        .energy(EnergyInput.builder("EU", 32).capacity(10_000).build())
        .gui("my_addon:textures/gui/machine.png")
        .item(44, 26, BehaviorMachine.SlotRole.INPUT)
        .item(116, 26, BehaviorMachine.SlotRole.OUTPUT)
        .energyOutput(EnergyOutput.builder("EU", 32, 1).faces(MachineFace.TOP).build())
        .recipeMaps("my_addon.fuel.solid")
        .build();
```

```java
AddonRecipes.Line line = AddonRecipes.map("my_addon.fuel.solid")
        .find(List.of(context.item(0)), List.of(), false, 1, 0);
if (line != null && line.euPerTick() < 0) {
    ItemStack fuel = context.item(0).copy();
    fuel.shrink(line.itemInputs()[0].getCount());
    context.setItem(0, fuel);
    context.generate(-line.euPerTick());
    context.emit(-line.euPerTick(), 1);
}
```

## Structures

A `StructureLayout` is bound by name. The name equals the controller's `machineType`. Several machines may share one layout. The layout is not written inside the machine. The controller is the origin and is not a cell.

```java
StructureLayout.builder("mypress")
        .cell(0, 1, 1, CellMatch.part("sample_wall"), StructureRole.CASING)
        .cell(-1, 1, 2, CellMatch.block("minecraft:glass"), StructureRole.CASING)
        .build()
```

`LayoutCell.at(right, up, back, match, role)` is one cell. The origin fails in `LayoutCell.at`. A repeated position fails in `StructureLayout` `build()`. `CellMatch` is `part(name)`, `air()`, `block(id)`, `tag(id)`, `anyFluid()`, `anyFluid(fluidTag)`, `anyOpaque()`, or `oneOf(...)`: one cell accepts two or more alternatives and forms when any one matches. Alternatives cannot nest or repeat; when any alternative is a plain block the whole cell still needs `StructureRole.CASING`. With a design index, every alternative part must support that index. `part` names a GregTech structure part, a structure casing, or a `layoutParts()` name.

Those three already share one name space, so an addon part cannot take a built-in name. `part("air")`, `part("anyfluid")`, `part("anyopaque")` and `part("anyfluid#tag")` take the reserved forms instead of a part of that name. `block` is a block id that contains a namespace. `tag` is a block tag without the leading `#`. `block`, `tag`, `anyFluid` and `anyOpaque` require `StructureRole.CASING`, which `LayoutCell.at` checks. `CellMatch.parse(String)` reads the string form: `air`, `anyfluid`, `anyfluid#<fluid tag>`, `anyopaque`, a leading `#` for a block tag, a `:` for a block id, and any other text as a part name.

A block id or tag is a resource location: a lower-case namespace, one colon, and a path of `[a-z0-9/._-]`. Any other spelling fails. These cells compare the block only. They do not need a block entity, they are not bound, and they do not carry items, fluid or energy. Block ids are checked in common setup, after other mods have registered blocks. An unknown id fails startup and names the addon and the layout. A tag is read when the structure is checked. An empty tag never forms and logs one warning. The structure preview shows the block. A tag shows the member whose registry name comes first, or a barrier while the tag is empty. Removing the block makes the next structure check fail.

`anyFluid` matches any block whose fluid state is non-empty; `anyFluid(fluidTag)` also requires that fluid to carry the tag. `anyOpaque` matches any block whose `isSolidRender` is true. Both are plain block cells with the same rules as `block` and `tag`. The preview shows water for `anyfluid` and stone for `anyopaque`.

`StructureRole` is `AIR`, `CASING`, `LOGISTICS`, `HEAT_INPUT`, `CRUCIBLE`, `FLUID_INPUT`, `FLUID_OUTPUT`, `FLUID_IO`, `ITEM_IO`, `ITEM_FLUID_IO`, `ITEM_FLUID_INPUT`, `ITEM_FLUID_OUTPUT`, `ENERGY_INPUT`, `ITEM_FLUID_ENERGY_INPUT`, `ITEM_FLUID_ENERGY` and `ENERGY_OUTPUT`. It is not a behavior slot role.

Whether a layout is required, and when the machine counts as formed, depends on the template. A large recipe machine must have a layout of the same machine type. A missing layout fails startup, and a failed structure check does not start a job. A multiblock fuel generator must have a layout of the same machine type. A single-block fuel generator cannot have a layout, and it starts formed. A behavior layout is optional. With none, the first server tick sets formed, and formed is false before that tick. With a layout, the cells are checked. All three checks match a cell the same way.

A part name that is not a block id or a tag is a `SharedLargeMachineParts` name, `air`, `tank_wall`, `tank_wall_dense`, `centrifuge_part`, `heat_transmitter`, `distillation_tower_part`, `electrolyzer_part`, or a name from `layoutParts()`. An empty layout, a duplicated position, an unknown part, a layout with no controller, or a large recipe machine with no layout fails startup and names the addon.

A repeat group is declared with `LayoutRepeat.builder(key, up, min, max)` and added with `StructureLayout.Builder.repeat(...)`. `step(-1)` repeats downward (for a drill that reaches bedrock); the default is upward. A cell's `up` is its distance from the anchor. `max` `-1` repeats until one layer fails, bounded by the world height (at most 1024 copies per check). A structure check reads the fixed cells first, then each repeat copy: a copy counts only when all its cells match, the first failing copy stops the walk, fewer than `min` copies leave the structure unformed, and layers past the last match are ignored. `MachineBehaviorContext.structureRepeats(key)` returns the matched copy count for this tick, or 0 when the structure is unformed or the key is unknown.

Structure checks do not run on every query. A controller attaches a tracker in `onLoad` and checks once; afterwards it rechecks at the end of a tick that changed a watched cell, after `/reload`, or on the 600-tick safety pass. `isStructureOk()` and port capability queries read the cached bit. A change that can alter the verdict clears the bit immediately, so a broken machine stops within the same tick. If any chunk of the shell is unloaded, the machine is unformed and keeps its claims until that chunk returns.

A part belongs to one controller at a time. The per-world claim table is the only authority, and a part owned by another controller cannot be claimed even while that controller is unloaded. A part that changed (placed, removed, replaced) is released and marks its owner dirty.

Addon machines whose matching-relevant data changes without a block state change (an internal tank level, a container's contents) must call `AddonStructures.notifyChanged(level, pos)` after the change. Pass the changed part's position, not the controller's. Call on the logical server thread: affected formed structures stop immediately and the normal end-of-tick scheduler rechecks them. The call does not load chunks or scan a structure immediately. Client calls and cells without watchers are no-ops; null arguments and off-server-thread calls are rejected. Notify only when matching-relevant data actually changes, not on every tick. A cell that only depends on the block state is detected automatically. Use this SDK facade, not the host's internal tracking classes or bootstrap services.

Where a controller has a magnifier entry (the large boiler and the heat exchanger today), the magnifier and the JEI/EMI assembly view use the same structure definition as the runtime check. The magnifier lists every rejected cell, not only the first, and the preview draws the same cells the check compiles.

### Structure layout datapacks

`data/<namespace>/gt6ce_layouts/<machineType>.json` replaces the cells of an addon or KubeJS machine that is already registered. It does not add a block or a machine. Built-in controllers cannot be replaced. A file that names a built-in controller, an unknown machine, an unknown part, an unknown block, or that fails to parse is skipped with one warning, and the reload continues.

```json
{
  "cells": [
    { "right": 1, "up": 0, "back": 0, "match": "press_casing", "role": "casing" },
    { "right": 0, "up": 0, "back": 1, "match": "tank_wall", "role": "energy_input" }
  ]
}
```

`match` uses `CellMatch.parse`: `air`, `anyfluid`, `anyfluid#<fluid tag>`, `anyopaque`, a leading `#` for a block tag, a `:` for a block id, and any other text as a part name. It may also be an array of alternatives. `role` is a `StructureRole` name, in any case. A plain block cell must be `casing`. The origin, a repeated cell, an empty list and an unknown field reject that file. When several files name the same machine, they are applied in resource-id order and the later file wins. Top-level fields that start with `_` are ignored.

The top level may also carry `repeats`, one entry per repeat group:

```json
{
  "cells": [
    { "right": 1, "up": 0, "back": 0, "match": "press_casing", "role": "casing" }
  ],
  "repeats": [
    { "key": "absorber", "up": 1, "min": 1, "max": 16,
      "cells": [
        { "right": 2, "up": 0, "back": 0, "match": "anyfluid", "role": "casing" }
      ] }
  ]
}
```

`up` is the anchor layer of the first copy, and a cell's `up` is relative to the anchor. `step` defaults to `1` (upward); `-1` goes downward. `max` `-1` repeats until a layer fails. A layout without `cells` must have at least one repeat. The preview shows `min` copies and marks each repeat group above the layer hint.

`/reload` restores the startup layouts, then applies the current datapack. A machine that was already formed is checked against the new layout on its next structure check. A part that is no longer in the layout is unbound. The server sends the overlay to the client. Structure preview, the JEI multiblock page and the hint text are rebuilt from the new cells. EMI reloads with them. If its reload entry cannot be called, that failure is logged once and the layout itself still applies.

KubeJS writes the same JSON. 1.20.1 uses `ServerEvents.highPriorityData`. 1.21.1 uses `ServerEvents.generateData('after_mods', ...)`. `GT6CE.layout(event, machine, cells)` takes the same cells and writes `data/kubejs/gt6ce_layouts/<machine>.json`; it also accepts `{cells: [...], repeats: [...]}` with the same repeat fields as a datapack. A cell may add `design`, the `gt.design` index written onto that GregTech part when the structure forms and restored when it breaks. Only a part whose original texture count includes the index accepts it. A plain block, a tag, air, an addon part, or a part with no designs fails at install and names the layout.

```js
ServerEvents.highPriorityData(event => {
  GT6CE.layout(event, 'largepress', [
    { right: 1, up: 0, back: 0, match: 'minecraft:iron_block', role: 'casing' },
    { right: 0, up: 0, back: 1, match: 'tank_wall', role: 'energy_input' }
  ])
})
```

`layoutParts()` returns casings, energy parts and functional parts:

```java
public List<LayoutPart> layoutParts() {
    return List.of(
            LayoutPart.builder("sample_wall", "Steel").hardness(6.0F).build(),
            LayoutPart.builder("sample_emitter", "Steel").hardness(6.0F)
                    .energyOutput(EnergyOutput.builder("EU", 32, 4).faces(MachineFace.FRONT).build(), 100_000)
                    .build());
}
```

| Field | Default | Allowed / meaning | Failure / effect |
| --- | --- | --- | --- |
| Part name | Required | See [Limits](#limits); global across built-in and addon parts | A built-in name such as `tank_wall`, or another addon name, fails startup and names both sides |
| Texture | Empty | Path without namespace: an untinted cube in the addon namespace | Empty uses a GregTech metal wall tinted by material color |
| Hardness | Required | Positive float, also used for blast resistance | Machine templates have no hardness field |
| Block registration | `<addonId>:<name>` | After built-in parts on GregTech's bus; generates blockstate and item model | Registry name is stored in layouts and compared against world blocks |
| Runtime ids | Start at 1000000 | Addon-id order, then declaration order | Not save data |
| Plain casing | No capabilities | No energy role or functional capability | No tanks, slots, energy or tick |
| Two-way part | Declare input and output | Input is controller-to-part only and shares the output buffer | Input faces or a condition fail `build()` |
| Two-way emission | `energyOutput.faces()` | Part emits from an `ENERGY_OUTPUT` cell | No neighbor-facing input on that input declaration |

`energyOutput(EnergyOutput, capacity)` makes an output part. `capacity` is at least 1. EU and RU differ only by the type string. An unknown short name fails startup and names the addon and the part. The block still uses `<addonId>:<name>`. It has a horizontal facing, a ticker, and the block entity `gt6ce:addon_energy_part`. Output faces are relative to that facing, with the same front, back, left and right rule as a behavior machine. The output buffer, the armed packet and the stopped flag are saved as `gt.part_energy`, `gt.part_output`, `gt.part_amount` and `gt.part_stopped`. Casings stay on `gt6ce:multiblock_port`.

The part pushes the armed packet on its output faces, in declaration order, while its cell role is `ENERGY_OUTPUT`, it is bound to this formed machine, and it is not stopped. Neighbors see the part through `EnergyBlockViews`. They do not pull the buffer, and they cannot inject into an output part. An `ENERGY_INPUT` cell keeps the output buffer for the controller and does not emit. One part belongs to one controller coordinate. A second structure does not form while that coordinate still claims it. Releasing the part, including when one cell of the structure breaks, stops it. The declaration has no method that asks how much of a packet fits, and none that checks the packet limits. The part runtime truncates to the remaining capacity and rejects a packet above the declared maximum.

`energyInput(EnergyInput)` together with `condition(perTick)` makes a condition part. An input without a condition fails. A condition without an input fails. `anyPacket()` fails. The input needs a short name, a packet range, at least one face, and a positive capacity. Its `perTick` is the recommended packet size and must sit inside that range. `condition` is at least 1 and at most the capacity. The part accepts energy on those faces. The cell role does not choose the faces, and the part accepts before it is bound. A packet below the minimum is refused. A packet above the maximum is consumed, follows `MachineOverload`, and is not stored. The buffer cap is the declared capacity. Each server tick, a buffer that holds at least `condition` pays that amount and is satisfied. A short buffer is left as it is and is not satisfied. The controller can see that result one tick later.

The input buffer and the satisfied flag are `gt.part_input` and `gt.part_satisfied`. An old save that only has `gt.part_energy` stays output-only. `gt.part_energy` is never read as the input buffer. A missing input key is an empty, unsatisfied buffer. The block state `satisfied` defaults to false. The server writes it and the client reads it. A satisfied part has light level 12. An output part leaves the property false.

A condition part binds to a behavior machine or to a large recipe controller. Built-in multiblock ports keep their own binding. An addon energy part does not bind to a built-in controller, and a built-in port does not bind to an addon controller. A large recipe machine does not run while any bound condition part is unsatisfied. Progress already made stays. With no condition part, that machine runs as before. A behavior machine still calls `tick` when a condition part is short.

`slots(PartSlots)`, `tanks(PartTanks)`, `gui(texture)`, `tick()`, `use()` and `field(key)` add item slots, fluid tanks, a menu, a server tick, a player click and named save fields. Any one of them makes the part functional. They combine with `energyOutput`, or with `energyInput` and `condition`. `PartSlots.builder(count)` and `stackLimit` use the bounds in [Limits](#limits); the stack limit defaults to its maximum. `insert` and `extract` name `MachineFace` values relative to the part's horizontal facing. An empty list does not accept pipes for that action. A null side does not insert or extract. `drops` defaults to true. When it is true, breaking the part spills the slot stacks and the item values stored in fields. A part with no slots still spills those field items. The block item still drops on its own. Fluid is not spilled.

`PartTanks.builder(count)` takes 1 to 8 tanks. `capacity` is required and is the capacity bounds in [Limits](#limits) mB. `insert` and `extract` use the same face rules as item slots. Pipes fill through the insert faces and then `PartBehavior.canFill`, and drain through the extract faces and then `canDrain`. `PartContext.fill` and `drain` write one tank without asking the face list or those checks. Tanks save as `gt.part_tanks`. A magnifying glass appends one line per tank after `describe()`.

`gui` is `namespace:path.png`, the same sheet a behavior machine uses for its panel and widgets. A right click opens a menu of this part's slots, tanks and fields after the wrench, the magnifying glass and `onUse`. `onUse` returning true consumes the click. The menu is not a recipe machine. Widget pixels are the `BehaviorMachine` slot and tank constants.

A field key matches `[a-z0-9_.]{1,64}`. The field-count cap is in [Limits](#limits). Invalid, repeated or excess keys fail at `build()`. GregTech does not invent a meaning for the key. `PartContext.state()` reads and writes the declared keys. An undeclared key throws. A missing named value is 0, false, an empty string or an empty item. String lengths follow [Limits](#limits).

The behavior is a `LayoutPartContributor`, listed in `META-INF/services/gregtech.api.addon.platform.LayoutPartContributor`. `partNames()` names every functional part. A casing or an energy-only part is not listed. `create` runs once for each placed block entity. A functional part with no factory fails startup and names the addon and the part. The addon does not subclass the block entity.

`PartBehavior` receives `tick`, `onUse`, `canInsert`, `canExtract`, `canFill`, `canDrain`, `onSlotsChanged`, `describe` and `onWrench`. `PartContext.setItem` replaces a slot without asking `canInsert` or the face list, and a count above the slot limit is cut to that limit. Pipes use the face list and then `canInsert` or `canExtract`. A player click is `onUse`, with a `PartHit` for the clicked face and the position as fractions from the low corner of the block. A wrench calls `onWrench` first. True consumes the click. False lets GregTech rotate the horizontal facing. A magnifying glass shows `describe()`. An empty list leaves that click unhandled.

Slots save as `gt.part_slots`, one compound per slot, in order. An empty slot is an empty compound. Tanks save as `gt.part_tanks`. Fields save as `gt.part_fields`, with the same `i`, `l`, `b`, `s` and `t` compounds as a behavior machine's state. Load skips a key that is no longer declared. A casing or an energy-only part does not write these keys. The energy keys stay `gt.part_energy`, `gt.part_output`, `gt.part_amount`, `gt.part_stopped`, `gt.part_input` and `gt.part_satisfied`.

A functional part with no energy role uses the block entity `gt6ce:addon_functional_part`. Its block state is the horizontal facing. A part that also emits or accepts energy stays on `gt6ce:addon_energy_part`, and that block entity stores the slots and fields beside the energy keys. An energy-only part does not send a block-entity update packet. A functional part does. The client reads a `PartClientView`. A `LayoutPartClient` may return a block-entity renderer for that part name. List it in `META-INF/services/gregtech.api.addon.platform.LayoutPartClient`. GregTech reads that file during client setup, calls it, and does not ask the addon to register the renderer. A part with no renderer still receives the view.

`MachineBehaviorContext.parts()` lists every addon part bound by the formed layout. A casing is not included. The list is empty when the machine is unformed. Do not keep a `BoundPart`, its `behavior()` or its `context()` after the callback. A cell that is both an energy part and a functional part appears once, with both `energy()` and `behavior()` set, and that cell is also in `energyParts()`. `energyParts()` remains that energy view. An energy-only part has a null `behavior()`. A functional part with no energy role has a null `energy()`. Cast `behavior()` to the addon's own type and pass `context()` into those methods.

## Energy

Energy coming in is an `EnergyInput`. Energy leaving a behavior machine, or an energy structure part, is an `EnergyOutput`. A fuel generator's packet is a `FuelOutput`. A recipe machine rejects an `EnergyOutput` and a `FuelOutput`. A behavior machine rejects a `FuelOutput`. A fuel generator rejects an `EnergyInput` and an `EnergyOutput`. Those setters throw immediately when a non-null value is passed. A recipe machine's `energy(null)` also fails immediately.

`EnergyInput.builder(type, perTick)` and `FuelOutput.builder(type, perTick)` take the short name, such as `EU` or `RU`. Builders canonicalize known names through `EnergyType` (`"eu"` becomes `"EU"`). `EnergyType.EU`, `RU`, `KU`, `HU`, `CU`, `LU`, `MU`, `NU`, `QU`, `TU`, `AU`, `RF`, `MJ` and `STEAM` are the constants. `EnergyType.same` and `EnergyType.signedDirection` ignore case. The name must match `[A-Za-z][A-Za-z0-9_]{0,15}` except `EnergyType.ANY` (`*`). Startup still resolves it through the same energy-tag lookup. Built-in energy short names and their packet semantics (`size` × `amount`) are:

- `EU` (Electric Energy — packet `size` is voltage, `amount` is amperage; see `GTVoltageTiers`. `VOLTAGES` and `NAMES` are copies; mutating them does not change lookups. `tierMax` returns `VOLTAGES.length` for a voltage above the table, so clamp it before using it as an index)

- `RU` (Rotation Energy — `size` is speed, `amount` is power)

- `KU` (Kinetic Energy, linear push/pull — `size` is push/pull stroke, `amount` is power)

- `HU` (Heat Energy — `size` is 1, `amount` is heat)

- `CU` (Cryo Energy — `size` is 1, `amount` is cooling)

- `LU` (Light Energy — `size` is beam strength, `amount` is frequency)

- `MU` (Magnetic Energy — `size` is field strength, `amount` is 1)

- `NU` (Neutron Energy — `size` is 1, `amount` is neutron flux)

- `QU` (Quantum Energy), `TU` (Time — `size` is 1, `amount` is ticks), `AU` (Air Pressure)

- Compat and vis energy tags: `Steam` (`2 Steam = 1 EU`), `RF` (`4 RF = 1 EU`), `MJ` (`1 MJ = 2.5 EU`), `Ordo`, `Aer`, `Aqua`, `Terra`, `Ignis` and `Perditio`

The direction is not one field. On a recipe machine and a behavior machine the short name on `EnergyInput` is input. On a fuel generator the short name on `FuelOutput` is output. A behavior machine that also emits writes the output short name on its `EnergyOutput`, separate from the input.

The nominal amount is not one range. A recipe machine's `perTick` is at least 0 and has no upper bound. A behavior machine's `perTick` is from the per-tick bounds in [Limits](#limits). When its capacity is 0, that number is also the amount drained each tick. A fuel generator has no input nominal. `FuelOutput.perTick` is one output packet, from the per-tick bounds in [Limits](#limits), and the builder requires it.

`EnergyInput.Builder.range(min, max)` is the accepted packet size. Both values are longs. Not calling it leaves both bounds at 0 and means the range is not declared. A declared range needs `1 <= min <= max`. Anything else fails at `build()`. The nominal `perTick` may sit outside the range. A recipe machine may declare a range. A TU recipe machine that declares one fails at `build()`. A behavior machine's main input may call `range`, and `perTick` must sit inside it. Its auxiliary input may call `range`. A fuel generator has no input range.

`EnergyInput.anyPacket()` does not check packet size and does not overload. It cannot be combined with `range`: `build()` fails. A recipe machine that calls it fails. A condition part that calls it fails. A behavior machine's main input may call it when the capacity is positive. `BehaviorMachine.Builder.auxiliaryEnergy` may call it too.

`EnergyInput.Builder.anyType()` lets a behavior machine's main input lock its energy type from the first accepted packet. The builder replaces the constructor type with `EnergyType.ANY` (`*`); `EnergyInput.builder("EU", 32).anyType()` is the usual form. Before the lock it accepts any type in `GregTechTags.Energy.ALL_GT`; after it, only the locked type, stored in `gt.energy_type`. A recipe machine, a condition part and an auxiliary input reject it.

A single-block recipe machine that does not declare a range accepts half to double its nominal input. TU accepts 1 to 16. A large recipe machine that does not declare a range does not use half to double. It uses the built-in large-machine rule: 1 to 16 for TU, 1 to 4096 for HU, and 512 to 4096 for every other type, including EU and RU. Built-in distillation towers, the implosion compressor and matter fabricators keep the limits on their own classes. A behavior machine's main input that leaves the range unset uses half to double `perTick` at startup, and TU is 1 to 16. That matches an undeclared single-block recipe machine. It does not match a large recipe machine. A declared main range uses those bounds. `anyPacket()` has no maximum. An auxiliary input uses the range it declares, or no range when it calls `anyPacket()`.

A packet larger than the maximum is consumed and then follows the overload rule. With the default settings that does not destroy the machine. A recipe whose power is below the minimum is still selected. GregTech charges four times as much when the power on hand is below the minimum. The buffer cap is the maximum. The item tooltip, Jade, and the machine shown as a JEI or EMI catalyst all read that minimum and maximum. The recipe page still shows the recipe cost, not this range. An addon energy part does not report the controller's minimum and maximum. An output part accepts nothing. A condition part reports the range on its own input. Two blocks that share a machine type must declare the same range. Both leaving it unset counts as the same range.

Faces are `MachineFace`: `BOTTOM`, `TOP`, `RIGHT`, `FRONT`, `LEFT`, `BACK`. `index()` and `bit()` are the mask positions. A face list on a new declaration rejects a repeated face. `faces()` keeps the written order. Two declarations with the same faces in a different order are equal. `EnergyInput.capacity(0)` on a behavior machine is the same as leaving capacity unset once the machine is built.

`EnergyInput.faces` does not follow `MachineIo`, and the default is not the same on the two templates. On a recipe machine, leaving faces unset is all six, including when the machine also sets `MachineIo`. An empty list is allowed and means there is no energy-input face. On a behavior machine, leaving faces unset is every face except the front. An empty list fails. Setting `MachineIo` does not change these faces.

`EnergyInput.capacity` is the stored-energy cap. On a behavior machine's main input it is at least 0, and the default is 0, including a call of `capacity(0)`. Zero means the buffer equals the rated input: GregTech drains that amount after the behavior returns, so unused energy does not carry into the next tick. A capacity above 0 keeps what remains, capped at that capacity. A recipe machine that calls `capacity` fails. An output part does not use this field. Its capacity is the second argument of `LayoutPart.Builder.energyOutput` and must be at least 1. A condition part and an auxiliary input both require a positive capacity. GregTech does not drain those two buffers with the main-input rule.

| Field | Default | Allowed / meaning | Failure / effect |
| --- | --- | --- | --- |
| `EnergyOutput` | Required type, faces, `maxSize`, `maxAmount` | Shared by behavior machines and energy parts; size and amount are positive | Missing or non-positive fields fail `build()` |
| `minSize` | 1 | Positive, at most `maxSize` | Invalid range fails `build()` |
| `allowNegative()` | Off | `KU` / `RU` only; sign denotes direction; `requireEmit` checks magnitude | `EU` / `HU` reject the flag; negative packets otherwise fail |
| Output faces | Required | Nonempty and unique | Empty or repeated faces fail unless a behavior output is remote |
| `remote()` | Off | Behavior output may retain faces for both paths or omit them for `emitTo` only | Energy parts reject it; an empty remote face list has no neighbor path |
| Energy-part capacity | Required | Positive independent output buffer | Invalid capacity fails `build()` |
| `FuelOutput` | `front()` | One `perTick` packet; `back()` or `behind(distance)` select placement | Uses `FuelOutput.Placement`, not `MachineFace`; no size × count declaration |
| Behind distance | 0 for front/back | See [Limits](#limits); past the front | Invalid distance fails `build()` |
| `amperes(n)` | See [Limits](#limits) | Number of fuel packets per tick | Outside the limit fails `build()` |

## Sensors and signals

### Sensor sources

`ProgressSensorSource`, `CompressionSensorSource` and `TemperatureSource` are in the core SDK, which cannot name `Direction`. A block entity that implements them is read by GregTech's progress, compressed-gas and thermometer sensor panels. Every `side` is `Direction.ordinal()`. `MachineFace` stays the machine-relative face and is not used here. `TemperatureSource` supplies both `temperatureK(side)` and `maximumK(side)`; the maximum drives percentage and scale modes.

`AddonSensorContributor`, listed in `META-INF/services/gregtech.api.addon.platform.AddonSensorContributor`, declares sensor kinds. GregTech loads it before block registration and registers `<addon>:sensor_<id>` as a block and block item in the addon's namespace. `AddonSensor` supplies `id`, `titleKey`, `unit`, `color`, `maximum`, `read(SensorContext)` and an optional `maximum(SensorContext)`. The sensor measures every ten game ticks; the context carries the sensor position, the target block entity (which may be null) and the target face as a `Direction` (`SensorContext.side`, not an ordinal). `unit` must be one of the existing GregTech glyph names (`liter`, `eu`, `scale`, `kelvin`, `ru`, `ton`, `gramm`, `kilogramm`, `kiloton`, `clock`, `cubicmeter`, `cubicdecameter`, `gibbl`, `lumin`, `greg`, `neutron`, `lu`), so no new texture is needed. The addon supplies the blockstate, model and `block.<addon>.sensor_<id>` name; GregTech supplies the tooltip hints, comparator output, display modes and save fields. Sensor ids are unique across addons, and a duplicate fails startup. The generated `gt6ce:sensor_recipes` pack does not add recipes for addon sensors.

### Signal wires

Signal wire block entities implement `GtSignalCarrier`. `GtSignalCarrier.UNIT` is one vanilla redstone level (`Integer.MAX_VALUE`). `signal()` is the fractional strength on that wire. Attenuation, the saved `mode`, and the 0–15 block-state level are unchanged. The wire still rebuilds its network from sources every server tick.

```java
if (level.getBlockEntity(pos) instanceof gregtech.api.signal.GtSignalCarrier wire) {
    long fractional = wire.signal();
    // ...
}
```

Implement `GtSignalSource` on an addon block entity to feed a wire a fractional value. The wire calls `gtSignal(side)` instead of vanilla `Level.getSignal`. `side` is `Direction.ordinal()`: 0 DOWN, 1 UP, 2 NORTH, 3 SOUTH, 4 WEST, 5 EAST. The core SDK cannot name `Direction`. It is the direction from the wire toward the addon block, the same direction passed to `Level.getSignal(neighborPos, side)`. A behavior machine does not implement this interface: `MachineBehavior.gtSignal` takes a `Direction`. Return a strength in `UNIT` units. The wire clamps that result to `[0, 15 * UNIT]` and then applies its existing loss. A value below one level stays fractional on the wire; vanilla output to other blocks still rounds up to 0–15.

```java
public final class MySourceBlockEntity extends BlockEntity implements GtSignalSource {
    @Override public long gtSignal(int side) {
        if (side != Direction.WEST.ordinal()) return 0;
        return GtSignalCarrier.UNIT / 2;
    }
}
```

When a behavior machine declares `signalOutput(true)`, the shell block entity already implements `GtSignalSource`; the behavior only implements `MachineBehavior.gtSignal`. An undeclared behavior machine is never read as a source.

Signal-wire ports follow the original explicit connector rules: placement attempts the clicked support face and joins compatible neighboring wires only if their facing port is already open. Use a wire cutter to open or close other faces. Implementing `GtSignalSource` makes a block entity readable through an open port even if its block does not override `Block#isSignalSource()`; it does not open unrelated or manually closed ports. Neighbor power changes and source removal preserve those port bits; the signal clears when the network next recomputes without a source. This corrects the earlier automatic source-adjacency behavior. The separate `FluidPipeConnectable` and `ItemPipeConnectable` addon opt-ins are unchanged.

A block that only receives power implements `GtSignalReceiver` on its block entity, or adds its block to the `gt6ce:signal_wire_receivers` block tag. Either one keeps a strongly powered solid block from being treated as a source. A block that matches both is a receiver. The tag ships empty; add entries from the addon's datapack:

```json
{ "replace": false, "values": ["my_addon:display"] }
```

Vanilla dispensers (including droppers), hoppers, pistons, TNT, note blocks, trapdoors, doors, powered rails (including activator rails), and redstone lamps stay on the existing `instanceof` checks and are not part of this tag. Do not reference `SignalWireBlockEntity`; it is a platform class and is not in the addon SDK.

## Reference

### Internal types

These classes live in the SDK because GregTech's own machine code uses them. They are not an addon contract: methods, arguments, NBT keys and numeric rules may change without notice. Do not call them from an addon.

`RecipeKeptInputs`, `BehaviorRangeRules`, `InstalledService`, `AddonFailures`.

### SDK packages

The SDK is `gregtech.api.*` from `core/src/api/java` plus `gregtech.api.*` from `multiversion/src/api/java` (platform facades, machine control and pipe connectivity). GregTech's implementation is under `gregtech.internal.*`; concrete game content is outside the API. Four deprecated contributor/layout types remain in the main jar's `gregtech.api.addon` as explicit legacy exceptions, excluded from the SDK. Read the SDK jar or its Javadoc for the supported surface. `AddonPlatform` holds loader-neutral entries. A method whose signature names a loader fluid type is in `AddonLoader`.

API version 0 includes crucible compositions, smelting targets and alloy rows, behavior-machine crucible callbacks, a read-only crucible view for bound parts, remote energy emission from a behavior machine to a saved coordinate, and still reads an old numeric material id as a built-in material, writing the name the next time that block or item is saved. It does **not** promise solid fuel, energy conversion, moving the built-in engines onto this template, arbitrary GUI widgets beyond a full `Screen` hosted over the menu, wireless compute parts, crucible blocks, writing a crucible's temperature or contents, melting an arbitrary non-GregTech item, datapack crucible rows, datapack-created recipe maps without a contributor, datapack reloads of the material table, one binary for both loaders, or a complete old GregAPI replacement. Only the documented entries are the compatibility surface. Use [the voltage guide](VOLTAGE_TIERS.md) rather than GT5/GTCE tier labels.

This unification adds explicit access/ownership contracts, immutable recipe snapshots and named construction. It does not add a general transactional recipe executor, arbitrary numbers of energy channels, or versioned behavior-state migrations. Existing recipe-machine templates and the main/auxiliary energy buffers keep their current scope.

### Scope

| Capability | Entry types | Guide |
| --- | --- | --- |
| Registration and lifecycle | `GregTechAddons`, `GregTechAddon` | [Getting started](#getting-started) |
| Materials, forms, prefixes, worldgen, crucible declarations | `AddonMaterialContributor` | [Materials](#materials) |
| Material/item/fluid reads | `GregTechMaterials`, `AddonItems`, `AddonFluids` | [Materials and forms](#materials-and-forms) |
| Recipes, viewer rows, dynamic providers | `AddonRecipes` | [Recipes](#recipes) |
| Recipe/fuel machines, maps, pipes, tanks | `AddonMachineContributor` | [Machines](#machines) |
| Custom server behavior and client screen/renderer | `BehaviorMachine`, `MachineBehavior`, `BehaviorMachineClient` | [Behavior machines](#behavior-machines) |
| Layouts and functional/energy parts | `StructureLayout`, `LayoutPart`, `PartBehavior` | [Structures](#structures) |
| Energy input/output and block bridge | `EnergyInput`, `EnergyOutput`, `AddonEnergy`, `EnergyBlock` | [Energy](#energy) |
| Machine control, heat, tuning | `MachineControl`, `HeatHandler`, `MachineTuning` | [Machines](#machines) |
| Sensors and signal sources | `SensorContext`, `GtSignalSource` | [Sensors and signals](#sensors-and-signals) |
| Tool identification | `AddonTools`, `AddonTool` | [Behavior machines](#behavior-machines) |

| Internal entry (unsupported) | Addon entry / boundary |
| --- | --- |
| `BasicMachineBlock`, `BasicMachineBlockEntity` | Declare machine data; do not subclass the implementation |
| `Recipe.mRecipeList`, `MachineRecipeMaps` | `AddonRecipes.Line` returned by `add`, and `AddonRecipes.map(name)` |
| `GTMaterial`, `GTMaterialRegistry`, material sentinels | `GregTechMaterials` |
| `GregTechTags`, `MaterialPrefix` constants | Energy short names and prefix name strings |
| Full internal tool catalog | Documented `AddonTool` values and `AddonTools` only |
| Bootstrap service binding / dispatch | Contributors and `GregTechAddons.register` |


## Versioning and compatibility

Reference for the version policy, the history of imported changes, and the removal schedule.

### Version policy

Depend on mod id `gt6ce` with `versionRange="[0.0.0,0.1.0)"` in `mods.toml` or `neoforge.mods.toml`. `context.apiVersion()` currently returns **0**. All capabilities documented here are included in this development baseline; do not gate them on the imported upstream version numbers. The API number does not replace the loader dependency range.

`gradle.properties` `mod_version` is `0.MINOR.PATCH`. `GregTechAddons.API_VERSION` is that MINOR. A patch release does not break the documented API contract. Before 1.0 a minor release may break addon callers. Breaking a documented entry waits on the removal table below, or the same release names the break in the change table. An addon targeting API 0 should use `versionRange="[0.0.0,0.1.0)"`, even when it calls no deprecated entries. Verify a new minor before raising the upper bound. Never infer compatibility with every future minor from the absence of deprecated calls. If the verified range includes a scheduled removal, stop the range before that removal until the addon has migrated. The API number and mod version remain **0** and **0.0.0** in this baseline.

Mark a doomed entry with `@Deprecated(since = "0.N", forRemoval = true)` and `@AddonApiRemoval(id = "<binary name>", removeAt = "0.M")` under one Javadoc. `id` is the package and type, plus `#member` for a method or field. The `@deprecated` note names the replacement with `{@link}` or `{@code}` and includes `0.M`. `M` is at least `N + 2`: an entry deprecated in `0.N` is still present in `0.(N+1)`. The build fails when `M` is less than or equal to the current minor. Delete the entry, or set a later `removeAt` and write the reason in this file. `@Deprecated` without `forRemoval = true` is not on this schedule.

### API changes

2026-10-24, API **0** / mod **0.0.0**: added `InventoryAccess`, `FluidAccess`, `MachineEnergy`, `EnergyTransfer`, `Line.builder()`, and matching SDK source/Javadoc artifacts. Existing context methods and recipe constructors remain available. `Line` array accessors now return defensive copies: callers that previously modified returned arrays must construct a new row instead. Recipe lookup no longer exposes caller-owned lists/stacks to dynamic providers. No existing machine storage format or declared energy channel was migrated by this change. The public size/UV constants on `BehaviorMachine`, `SelectorTags.MIN`/`MAX`, `FuelGenerator.MAX_BEHIND`/`MAX_AMPERES`/`MAX_TANK`, `PartTanks.MAX_CAPACITY` and `CompressedGasTankDeclaration.MAX_CAPACITY` are no longer compile-time constants. Addons compiled against this SDK read these fields from the runtime. Binaries compiled against an earlier SDK that exposed them as compile-time constants retain their already-inlined values until they are recompiled; updating the runtime alone cannot replace those copies. Source that used them as `switch` case labels or annotation arguments no longer compiles.

| Item | Previously | Now |
| --- | --- | --- |
| Composition units | Bare amounts were ambiguous | `PrefixDeclaration.amount` and `MaterialComposition.Part.amount` use GregTech units: one ingot is `GTValues.U`; 8U is `8 * GTValues.U`, not bare `8`. |
| Absent values | Scattered lookup rules | See [Conventions](#conventions). |
| World sides | Face vocabulary was ambiguous | `MachineBehavior.gtSignal`, `fillMoldAtSide` and `SensorContext.side` use `Direction`; core signal/sensor sources and `CrucibleMold.fillMold` use `Direction.ordinal()`. |
| Relative faces | Clarification | `MachineFace` is machine-relative; null on `canInsert` / `canFill` means menu or unsided access. |
| Recipe rows | Record / mutable-array assumptions | `AddonRecipes.Line` is a class. Constructors require positive inputs; `Line.read` may preserve empty stored slots. Array accessors return copies. |
| Callback failure | RuntimeException-only wording | Every non-`Error` failure, including sneaky checked exceptions, is wrapped as `IllegalStateException` and leaves `FAILED`; `Error` is rethrown unchanged and also leaves `FAILED`. |
| Row equality | Identity / array equality assumptions | Value comparison: items by item, count and native data; fluids by fluid, amount and native data. |
| Energy names and faces | Case / face order assumptions | Short names go through `EnergyType`; face lists compare as sets. |
| Fuel placement | Clarification | `FuelOutput.placement()` is `FRONT`, `BACK` or `BEHIND`. |
| Context aliases | Short names remain | `MachineBehaviorContext` adds `auxiliaryEnergy`, `emitToPart`, `tankCapacity`. |
| Addon id | Clarification | `addonId()` matches the pattern in [Limits](#limits). |

### Deprecation and removal

Legacy entries are marked deprecated from the CE baseline `0.0`. Numeric material entries retain their `0.9` earliest removal deadline; the four legacy contributor and layout entries stay until `0.10`. Resetting the current version does not bring removal forward.

| Entry | Deprecated | Replacement | Remove at |
| --- | --- | --- | --- |
| gregtech.api.addon.GregTechMachineContributor | 0.0 | AddonMachineContributor | 0.10 |
| gregtech.api.addon.GregTechMaterialContributor | 0.0 | AddonMaterialContributor | 0.10 |
| gregtech.api.addon.AddonMachineDefinition | 0.0 | RecipeMachine | 0.10 |
| gregtech.api.addon.AddonMachineLayout | 0.0 | StructureLayout | 0.10 |
| gregtech.api.addon.MaterialDeclaration#builder | 0.0 | MaterialDeclaration.builder(String) | 0.9 |
| gregtech.internal.addon.AddonMaterials#ADDON_ID_MIN | 0.0 | MaterialDeclaration.builder(String) | 0.9 |
| gregtech.internal.addon.AddonMaterials#ADDON_ID_MAX | 0.0 | MaterialDeclaration.builder(String) | 0.9 |

Deprecated without a removal date, so not in the table above: `BehaviorMachineClient.screen(BehaviorMachineMenuView, Inventory, Component)` since `0.0`. Override `screen(String machineType, ...)` instead; the old signature stays as a default method that returns null.

## Appendix

### Upstream history

API **0** is the GT6CE development baseline and includes all the capabilities below. The following 1–8 labels describe imported upstream development, not GT6CE releases or runtime feature requirements.

| Upstream minor | Imported change |
| --- | --- |
| 8 | Crucible compositions, smelting targets and alloy rows. An addon names materials: a composition can opt into one derived crucible row, a smelting target sets what the material melts into, and explicit rows or removals change the alloy table. The table freezes at the end of material linking. Imported from upstream 0.8.0 and included in CE API 0. Legacy contributor removal moves to 0.10. |
| 7 | Fuel generators. An addon declares a fuel map and a fluid fuel machine: efficiency, one energy packet, tank capacity, and front, back, or 1 to 8 blocks behind. GregTech registers and runs the single block and the multiblock. Upstream mod version starts at 0.7.0. The numeric material entries stay until 0.9 so this release does not remove them in the same minor that adds the template. |
| 6 | Behavior machines. An addon declares slots, tanks, toggles, energy and a GUI sheet, and supplies a `MachineBehavior`. GregTech owns the block, inventory, energy, menu and save. Upstream mod version starts at 0.6.0. The version 3 contributor types stay until 0.8 so this release does not remove them in the same minor that adds the shell. |
| 5 | Addon materials are declared by name. GregTech assigns the numeric id at startup, from 10000, and does not write it to saves. Matching names merge; a mismatched field or a case-only difference fails startup. `builder(int, String)` ignores the number. Upstream mod version starts at 0.5.0. |
| 4 | The addon API is its own source set and a per-loader compile-only SDK. Materials, prefixes, items, fluids, recipes, tools and energy use the facades listed under Scope. Upstream mod version starts at 0.4.0. Version 3 registration and `onRecipesReady` stay. |
| 3 | Recipe machines and large recipe machines. Upstream mod version starts at 0.3.0. Version 2 registration, materials and `onRecipesReady` stay. |
| 2 | Queued materials and extra forms. |
| 1 | Addon registration and `onRecipesReady`. |

## License

The SDK/project code license is LGPL-3.0-or-later. Preserve applicable source/asset notices; consult `LICENSE`, `COPYING` and `NOTICE` for the actual declarations.

## test branch package migration

The test branch changes the Java root from `com.gregtech.gregtech` to `gregtech` and moves implementation types to `gregtech.internal`. This breaks binary references: rebuild addons after updating imports and `META-INF/services` filenames and providers. Game resource IDs remain `gt6ce:`. Existing deprecation deadlines are unchanged. SDK bootstrap services are not addon extension points. See [package boundaries](integration/JAVA_PACKAGE_BOUNDARIES.md) and [per-type implementation moves](integration/PACKAGE_BOUNDARY_TYPES.tsv). This experimental branch does not establish old-addon binary compatibility.

The content-directory step moves root-level block, blockentity, item, data, registry, recipe, worldgen, world, entity and damage packages under `gregtech.content`. See [content source moves](integration/CONTENT_PACKAGE_MOVES.tsv). The SDK never includes these implementations.

Six helpers explicitly marked internal in the old SDK now ship only in the main mod: `MachineWorkCost`, `BasicMachineEnergy`, `AddonOverclock`, `BehaviorEnergyRules`, `BehaviorRecipeCatalysts`, and `PartEnergyRules`. Their packages are `gregtech.internal.recipe`, `gregtech.internal.machine`, or `gregtech.internal.addon`, respectively. Addons should use the documented API instead of importing these implementations. `AddonFailures`, `InstalledService`, `BehaviorRangeRules`, and `RecipeKeptInputs` remain SDK dependencies because its own facades and declaration validators use them; that does not make them supported extension points.
