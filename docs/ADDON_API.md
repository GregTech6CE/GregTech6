# Addon API

[English](ADDON_API.md) | [中文](ADDON_API.zh-CN.md)

API version **0** is mod version **0.0.0** on Forge 1.20.1 and NeoForge 1.21.1. Compile against that loader's SDK jar. The SDK contains the addon API only. GregTech internals are not on it. Register a `GregTechAddon` in your own mod constructor for recipes. GregTech calls `onRecipesReady` once from common setup, after its material/item linking and built-in machine recipes. Callbacks run in ascending addon mod ID order. Duplicate IDs, late registration and a second dispatch fail explicitly; callback failures stop initialization and leave the lifecycle `FAILED`.

Legacy contributors still load from an installed GregTech jar. They are deprecated and are not in the SDK. New code uses `AddonMaterialContributor` and `AddonMachineContributor`.

## Version

Depend on mod id `gt6ce` with `versionRange="[0.0.0,)"` in `mods.toml` or `neoforge.mods.toml`. `context.apiVersion()` currently returns **0**. All capabilities documented here are included in this development baseline; do not gate them on the imported upstream version numbers. The API number does not replace the loader dependency range.

`gradle.properties` `mod_version` is `0.MINOR.PATCH`. `GregTechAddons.API_VERSION` is that MINOR. A patch release does not change this API. Before 1.0 a minor release may break addon callers. Breaking a documented entry waits on the removal table below, or the same release names the break in the change table.

Mark a doomed entry with `@Deprecated(since = "0.N", forRemoval = true)` and `@AddonApiRemoval(id = "<binary name>", removeAt = "0.M")` under one Javadoc. `id` is the package and type, plus `#member` for a method or field. The `@deprecated` note names the replacement with `{@link}` or `{@code}` and includes `0.M`. `M` is at least `N + 2`: an entry deprecated in `0.N` is still present in `0.(N+1)`. The build fails when `M` is less than or equal to the current minor. Delete the entry, or set a later `removeAt` and write the reason in this file. `@Deprecated` without `forRemoval = true` is not on this schedule.

## API changes

API **0** is the GT6CE development baseline and includes all the capabilities below. The following 1–7 labels describe imported upstream development, not GT6CE releases or runtime feature requirements.

| Upstream minor | Imported change |
| --- | --- |
| 8 | Crucible compositions, smelting targets and alloy rows. An addon names materials: a composition can opt into one derived crucible row, a smelting target sets what the material melts into, and explicit rows or removals change the alloy table. The table freezes at the end of material linking. Imported from upstream 0.8.0 and included in CE API 0. Legacy contributor removal moves to 0.10. |
| 7 | Fuel generators. An addon declares a fuel map and a fluid fuel machine: efficiency, one energy packet, tank capacity, and front, back, or 1 to 8 blocks behind. GregTech registers and runs the single block and the multiblock. Upstream mod version starts at 0.7.0. The numeric material entries stay until 0.9 so this release does not remove them in the same minor that adds the template. |
| 6 | Behavior machines. An addon declares slots, tanks, toggles, energy and a GUI sheet, and supplies a `MachineBehavior`. GregTech owns the block, inventory, energy, menu and save. Upstream mod version starts at 0.6.0. The version 3 contributor types stay until 0.8 so this release does not remove them in the same minor that adds the shell. |
| 4 | The addon API is its own source set and a per-loader compile-only SDK. Materials, prefixes, items, fluids, recipes, tools and energy use the facades below. Upstream mod version starts at 0.4.0. Version 3 registration and `onRecipesReady` stay. |
| 5 | Addon materials are declared by name. GregTech assigns the numeric id at startup, from 10000, and does not write it to saves. Matching names merge; a mismatched field or a case-only difference fails startup. `builder(int, String)` ignores the number. Upstream mod version starts at 0.5.0. |
| 3 | Recipe machines and large recipe machines. Upstream mod version starts at 0.3.0. Version 2 registration, materials and `onRecipesReady` stay. |
| 2 | Queued materials and extra forms. |
| 1 | Addon registration and `onRecipesReady`. |

## Deprecation and removal

Legacy entries are marked deprecated from the CE baseline `0.0`. Numeric material entries retain their `0.9` earliest removal deadline; the four legacy contributor and layout entries stay until `0.10`. The machine-declaration types and the seven old contributor methods stay through `0.1` and are removed at `0.2`. Resetting the current version does not bring removal forward.

| Entry | Deprecated | Replacement | Remove at |
| --- | --- | --- | --- |
| com.gregtech.gregtech.api.addon.GregTechMachineContributor | 0.0 | AddonMachineContributor | 0.10 |
| com.gregtech.gregtech.api.addon.GregTechMaterialContributor | 0.0 | AddonMaterialContributor | 0.10 |
| com.gregtech.gregtech.api.addon.AddonMachineDefinition | 0.0 | RecipeMachine | 0.10 |
| com.gregtech.gregtech.api.addon.AddonMachineLayout | 0.0 | StructureLayout | 0.10 |
| com.gregtech.gregtech.api.addon.MaterialDeclaration#builder | 0.0 | MaterialDeclaration.builder(String) | 0.9 |
| com.gregtech.gregtech.api.addon.AddonMaterials#ADDON_ID_MIN | 0.0 | MaterialDeclaration.builder(String) | 0.9 |
| com.gregtech.gregtech.api.addon.AddonMaterials#ADDON_ID_MAX | 0.0 | MaterialDeclaration.builder(String) | 0.9 |
| com.gregtech.gregtech.api.addon.MachineDeclaration | 0.0 | RecipeMachine | 0.2 |
| com.gregtech.gregtech.api.addon.MachineDeclaration.Builder | 0.0 | RecipeMachine.builder | 0.2 |
| com.gregtech.gregtech.api.addon.MachineDeclaration.Kind | 0.0 | RecipeMachine.basic() / largeRecipe() | 0.2 |
| com.gregtech.gregtech.api.addon.MachineDeclaration.Target | 0.0 | IoTarget | 0.2 |
| com.gregtech.gregtech.api.addon.BehaviorMachineDeclaration | 0.0 | BehaviorMachine | 0.2 |
| com.gregtech.gregtech.api.addon.BehaviorMachineDeclaration.Builder | 0.0 | BehaviorMachine.builder | 0.2 |
| com.gregtech.gregtech.api.addon.BehaviorMachineDeclaration.Face | 0.0 | MachineFace | 0.2 |
| com.gregtech.gregtech.api.addon.BehaviorMachineDeclaration.SlotRole | 0.0 | BehaviorMachine.SlotRole | 0.2 |
| com.gregtech.gregtech.api.addon.BehaviorMachineDeclaration.ItemSlot | 0.0 | BehaviorMachine.ItemSlot | 0.2 |
| com.gregtech.gregtech.api.addon.BehaviorMachineDeclaration.FluidSlot | 0.0 | BehaviorMachine.FluidSlot | 0.2 |
| com.gregtech.gregtech.api.addon.BehaviorMachineDeclaration.Toggle | 0.0 | BehaviorMachine.Toggle | 0.2 |
| com.gregtech.gregtech.api.addon.BehaviorMachineDeclaration.Progress | 0.0 | BehaviorMachine.Progress | 0.2 |
| com.gregtech.gregtech.api.addon.FuelGeneratorDeclaration | 0.0 | FuelGenerator | 0.2 |
| com.gregtech.gregtech.api.addon.FuelGeneratorDeclaration.Builder | 0.0 | FuelGenerator.builder | 0.2 |
| com.gregtech.gregtech.api.addon.FuelGeneratorDeclaration.Kind | 0.0 | FuelGenerator.single() / multi() | 0.2 |
| com.gregtech.gregtech.api.addon.FuelGeneratorDeclaration.Output | 0.0 | FuelOutput | 0.2 |
| com.gregtech.gregtech.api.addon.FuelGeneratorDeclaration.Output.Side | 0.0 | FuelOutput front, back, or behind | 0.2 |
| com.gregtech.gregtech.api.addon.MachineLayout | 0.0 | StructureLayout | 0.2 |
| com.gregtech.gregtech.api.addon.MachineLayout.Cell | 0.0 | LayoutCell | 0.2 |
| com.gregtech.gregtech.api.addon.StructurePartDeclaration | 0.0 | LayoutPart | 0.2 |
| com.gregtech.gregtech.api.addon.StructurePartEnergy | 0.0 | LayoutPart.energyOutput(EnergyOutput, capacity) | 0.2 |
| com.gregtech.gregtech.api.addon.RecipeMapDeclaration | 0.0 | RecipeTable.machine(name) | 0.2 |
| com.gregtech.gregtech.api.addon.FuelMapDeclaration | 0.0 | RecipeTable.fuel(name) | 0.2 |
| com.gregtech.gregtech.api.addon.AddonMachineContributor#machines | 0.0 | addonMachines() | 0.2 |
| com.gregtech.gregtech.api.addon.AddonMachineContributor#behaviorMachines | 0.0 | addonMachines() | 0.2 |
| com.gregtech.gregtech.api.addon.AddonMachineContributor#fuelGenerators | 0.0 | addonMachines() | 0.2 |
| com.gregtech.gregtech.api.addon.AddonMachineContributor#layouts | 0.0 | structureLayouts() | 0.2 |
| com.gregtech.gregtech.api.addon.AddonMachineContributor#parts | 0.0 | layoutParts() | 0.2 |
| com.gregtech.gregtech.api.addon.AddonMachineContributor#recipeMaps | 0.0 | recipeTables() | 0.2 |
| com.gregtech.gregtech.api.addon.AddonMachineContributor#fuelMaps | 0.0 | recipeTables() | 0.2 |

New materials and extra forms use a separate `ServiceLoader` entry, described below. Do not submit them from the mod constructor: on Forge 1.20.1 the addon constructor and the GregTech constructor can overlap.

```java
import com.gregtech.gregtech.api.addon.GregTechAddon;
import com.gregtech.gregtech.api.addon.GregTechAddons;

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

An example recipe callback, where `MyItems.MY_ORE` is registered by your addon:

```java
var map = com.gregtech.gregtech.api.addon.platform.AddonRecipes.map("gt.recipe.mortar");
var recipe = new com.gregtech.gregtech.api.addon.platform.AddonRecipes.Line(
        new net.minecraft.world.item.ItemStack[]{
            new net.minecraft.world.item.ItemStack(MyItems.MY_ORE.get())},
        new net.minecraft.world.item.ItemStack[]{
            com.gregtech.gregtech.api.addon.platform.AddonItems.stack("dust", "Copper")},
        null, null, 16, 16, 0);
if (map.add(recipe) == null)
    throw new IllegalStateException("my_addon mortar recipe rejected");
```

This uses the existing GT recipe system, not a parallel addon recipe registry. Choose inputs/outputs and power/duration appropriate for your addon. New rows are available to the existing machine recipe lookup and viewers; overlapping inputs retain that map's original precedence rules. `onRecipesReady` runs once at startup. Datapack and KubeJS changes use the overlay below; they do not call the addon callback again.

The seven-argument `Line` leaves every item output certain. Pass an `int` per item output, in parts per 10_000, when an output is not certain. `10000` is certain. An empty array is the same as omitting it. The length must match the item outputs, and each value is 1 to 10000. Fluid outputs stay certain. The machine, JEI, EMI and the datapack overlay all read that chance from the recipe row. A datapack item output already uses the same `chance` field.

`AddonItems.selectorTag(n)` is programmed circuit `n`: `integrated_circuit_0` through `integrated_circuit_24`, with a count of 1. A value outside 0..24 fails startup. The circuit is already a catalyst item, so the machine checks that it is present and does not consume it. Listing its index in `keptInputs` is allowed and does not change that.

`keptInputs` is the last array on `Line`: indexes of item inputs the machine must see and must not consume. Leave each count at 1 or more. Null and an empty array mark nothing, and that row matches the same inputs it did before. An index outside the item inputs, or a repeated index, fails startup. A count of 0 still fails startup, and the message names `keptInputs`. EMI draws a marked input as a catalyst, the same way it draws a built-in catalyst input. The datapack overlay is unchanged: a count of 0 there is still rejected, and the overlay has no kept-input field.

```java
var circuit = new com.gregtech.gregtech.api.addon.platform.AddonRecipes.Line(
        new net.minecraft.world.item.ItemStack[]{
            new net.minecraft.world.item.ItemStack(MyItems.MY_ORE.get()),
            com.gregtech.gregtech.api.addon.platform.AddonItems.selectorTag(3)},
        new net.minecraft.world.item.ItemStack[]{
            com.gregtech.gregtech.api.addon.platform.AddonItems.stack("dust", "Copper")},
        null, null, 16, 16, 0);
var kept = new com.gregtech.gregtech.api.addon.platform.AddonRecipes.Line(
        new net.minecraft.world.item.ItemStack[]{
            new net.minecraft.world.item.ItemStack(MyItems.MY_ORE.get()),
            new net.minecraft.world.item.ItemStack(MyItems.MY_LENS.get())},
        new net.minecraft.world.item.ItemStack[]{
            com.gregtech.gregtech.api.addon.platform.AddonItems.stack("dust", "Tin")},
        null, null, 16, 16, 0, null, new int[]{1});
```

## Machine recipe datapacks

A datapack can add or remove static rows on a machine recipe map that already exists. Put the JSON in the vanilla recipe directory: `data/<namespace>/recipes/` on Forge 1.20.1, and `data/<namespace>/recipe/` on NeoForge 1.21.1. It cannot create a map, and it does not see or edit dynamic providers. The same JSON is what KubeJS `event.custom()` submits. There is no KubeJS plugin. `event.remove({type: "gt6ce:machine"})` removes the marker recipes from the vanilla recipe manager; the next reload then rebuilds the machine maps from the markers that remain.

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

Development runs can add KubeJS with `-PkubejsRuntime=true`. The dependency stays out of the production jar and is independent of `-PcompatRuntime`.

## Signal wires

Signal wire block entities implement `GtSignalCarrier`. `GtSignalCarrier.UNIT` is one vanilla redstone level (`Integer.MAX_VALUE`). `signal()` is the fractional strength on that wire. Attenuation, the saved `mode`, and the 0–15 block-state level are unchanged. The wire still rebuilds its network from sources every server tick.

```java
if (level.getBlockEntity(pos) instanceof com.gregtech.gregtech.api.signal.GtSignalCarrier wire)
    long fractional = wire.signal();
```

Implement `GtSignalSource` on an addon block entity to feed a wire a fractional value. The wire calls `gtSignal(side)` instead of vanilla `Level.getSignal`. `side` is `Direction.ordinal()`: 0 DOWN, 1 UP, 2 NORTH, 3 SOUTH, 4 WEST, 5 EAST. It is the direction from the wire toward the addon block, the same direction passed to `Level.getSignal(neighborPos, side)`. Return a strength in `UNIT` units. The wire clamps that result to `[0, 15 * UNIT]` and then applies its existing loss. A value below one level stays fractional on the wire; vanilla output to other blocks still rounds up to 0–15.

```java
public final class MySourceBlockEntity extends BlockEntity implements GtSignalSource {
    @Override public long gtSignal(int side) {
        if (side != Direction.WEST.ordinal()) return 0;
        return GtSignalCarrier.UNIT / 2;
    }
}
```

A block that only receives power implements `GtSignalReceiver` on its block entity, or adds its block to the `gt6ce:signal_wire_receivers` block tag. Either one keeps a strongly powered solid block from being treated as a source. A block that matches both is a receiver. The tag ships empty; add entries from the addon's datapack:

```json
{ "replace": false, "values": ["my_addon:display"] }
```

Vanilla dispensers (including droppers), hoppers, pistons, TNT, note blocks, trapdoors, doors, powered rails (including activator rails), and redstone lamps stay on the existing `instanceof` checks and are not part of this tag. Do not reference `SignalWireBlockEntity`; it is a platform class and is not in the addon SDK.

## Multiple dynamic providers

Static rows are still searched first. Dynamic providers run only when none of those rows match, in this order: the built-in provider installed with `RecipeMap.dynamicRecipes`, then each provider passed to `RecipeMap.addDynamicRecipes`, in call order. The first provider that returns a non-null recipe is the dynamic answer, and later providers are not asked. Returning null means the provider does not handle that input. `dynamicRecipes` replaces only the built-in provider. `addDynamicRecipes` appends one provider and does not replace the built-in provider or any earlier addon provider.

`MachineRecipeMaps.Plantalyzer` has no built-in provider. An addon can append its own, for example a crop seed scan, without removing the Bumblelyzer, molecular scanner, printer or replicator providers.

## Viewer rows

`AddonRecipes.Map.addDisplay` stores a display row for JEI and EMI. Lookup skips display rows, so a machine never crafts them. Addons can use it to show an example of a dynamic result, the same way the Bumblelyzer shows one scan on its recipe page. A display row does not replace a provider: the machine still needs `addDynamic` to compute the real output.

## Materials and forms

List an `AddonMaterialContributor` in `META-INF/services/com.gregtech.gregtech.api.addon.AddonMaterialContributor`. GregTech loads that file with its own class loader at the start of material initialization, before the built-in directory is declared. Return pure data. Do not touch GregTech material tables from the service constructor or from `materials()` / `forms()`. `compositions()`, `smeltingTargets()`, `crucibleReactions()` and `crucibleRemovals()` are read after the built-in directory has linked. `link()` runs at the same point for anything those lists do not cover.

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

A name is matched exactly. Two spellings that differ only by case fail startup, because generated item ids are lowercased. The failure names both spellings and, when they come from addons, both addon ids. The same exact name as a built-in material also fails. To add a form of a built-in material, use `forms()`.

Two addons may declare the same name. The display name, color, texture, melting point, boiling point, density, formula, properties, atomic counts and alpha must all match. `forms()` is the union of the requests. The material records every declaring addon, in addon-id order. One addon uses `tooltip.gt6ce.material_from_addon`. More than one uses `tooltip.gt6ce.material_from_addons` (“Added by the %s addons”), with the addon ids joined by a comma. A field that differs fails startup and names both addons and the field.

GregTech assigns numeric ids after the built-in directory, sorted by name, starting at 10000. Built-in ids do not change. The assigned id exists only in memory: a different set of addons can number the same name differently, and nothing in a save reads that number back. `GregTechMaterials.find(int)` still resolves whatever id this session assigned. `dust()` generates the dust items. `metal()` generates those dusts plus the ingot, plate, rod, gear and related forms that the built-in directory already knows how to generate. It does not add a prefix.

`ores()` gives the material GregTech's `ORES` form. That is what makes the raw ore item, the rock, the impure dust, and the crushed, washed and centrifuged forms pass the existing checks. Purified and refined dusts stay off, because those still require the unused `DIRTY_DUSTS` flag. It also registers the `ore` and `oreSmall` blocks. Those two are appended after the blocks the directory already emits, so built-in block ids stay put. The host rock is a block state on that one block, the same as a built-in ore. `ores()` does not register a raw-ore storage block or a raw-ore crate. Veins are a separate declaration, below. `oreByproducts("Copper", "Tin")` turns `ores()` on and sets the byproducts in that order; the first name is the wash byproduct. Leave it out and the byproduct list is empty. The wash and centrifuge recipes still follow the built-in fallback. A byproduct name has to be a material that exists at startup, and it cannot be hidden. Two addons that share the material have to agree on `ores()` and on that list. `forms()` cannot name `ore` or `oreSmall`. `forms()` of `oreRaw` still fails when the material did not declare `ores()`.

`GregTechMaterials.Material.byproducts()` returns that list as material names, after `resolve()`, in the same order. Built-in materials and addon materials both answer. Read it from `onRecipesReady`, after materials have linked. A call before that returns the names stored at that moment and does not throw. An unknown material returns an empty list. An addon cannot change a built-in material's byproducts. API version stays 0.

Saves store the name. Crucible contents are `{name, amount}` under `gt.materials`. The crucible display field `gt.display.material` is the name, or an empty string when the crucible is not showing a material. USB sticks and drive files keep the key `gt.replicator.data` and store the name as a string. A save written by an older build still loads: a numeric `id`, or a numeric `gt.replicator.data` / `gt.display.material`, resolves a built-in id and does not resolve an addon id. An unknown name is skipped, as an unknown id already was. There is no numeric suffix for an addon item-id collision.

Generated forms are registered in the **`gt6ce`** namespace, through the same `MaterialItemDefinitions` list both loaders already use. An addon’s own handwritten items, blocks and machines stay in the addon’s namespace. Both loaders also treat a `gt6ce:` id as GregTech content where they branch on that namespace, including the food-item lookup, so a generated addon form is covered by those checks. A new material receives only the prefixes that list already generates for its properties: `dust()` is what creates the dust items. `forms()` may name one of those prefixes. A pair the list already emits is not registered again. A prefix that is not in that list, including `crystal`, fails startup. That prefix exists as a validator, but the built-in directory never generates it, so it has no icon and no `item.gt6ce.crystal` translation. A generated prefix that `isValidFor` rejects still fails startup, except an explicit `plate` or `foil` of an addon material. Those two are emitted even when the material has no ingot, and they do not pull in double plates, dense plates or rods. A plate of diamond still fails, because diamond is built-in. `oreRaw` still fails on a material that did not declare `ores()`. `ore` and `oreSmall` are block prefixes, so naming them in `forms()` fails. Any other rejected prefix still fails. A new prefix is declared with `prefixes()`, in the next section. `forms()` still cannot name a prefix outside the generated list. Materials are fixed at startup; a datapack reload cannot add or remove them. An old numeric id is read when the block or item loads. The save is not rewritten until that block or item is saved again.

The translation key is `material.gt6ce.<lowercasename>`, from `GTMaterial.getTranslationKey()`. Put it in the addon’s own `assets/gt6ce/lang/en_us.json` and `zh_cn.json`. Minecraft merges those files with GregTech’s by key. A tooltip on an addon material uses `tooltip.gt6ce.material_from_addon` (“Added by the %s addon”), or `tooltip.gt6ce.material_from_addons` when several addons declared it. The shared Chinese file keeps the English fallback for both keys, because neither is an original GT6 translation.

A `dust()` material such as ammonium nitrate generates **6** items. Iron’s automatic forms are **96**. `metal()` is the declaration that produces that larger set. Phase C recipe generators walk every material, so a new material receives the furnace, macerator and similar rows its properties already imply. Removing the addon leaves those `gt6ce:` items missing under the usual Minecraft rules. A crucible or USB entry whose name is no longer loaded is skipped, so unloading an addon does not crash the load. A world reload after uninstall has not been run.

## Prefixes

`prefixes()` on `AddonMaterialContributor` declares item prefixes. GregTech registers them after the built-in prefixes and before it checks `forms()`. A name that already exists fails startup. That includes a different capitalisation, the same registry path, and an alias such as `casingSmall`. Two addons cannot declare the same name. The failure names the addon and the prefix that already owns the name.

```java
public List<PrefixDeclaration> prefixes() {
    return List.of(new PrefixDeclaration("sampleBlank", "", " Sample Blank", 8L,
            "item/sample_blank", List.of("Iron", "SampleAlloy"), Set.of()));
}
```

`unitAmount` is a positive count of `U`. One sample blank is 8U in the crucible and in the shredder. Built-in prefix amounts stay on the existing table. `texture` is a path in this addon's namespace: `item/sample_blank` reads `assets/<addon>/textures/item/sample_blank.png`. The item has that one layer, tinted with the material colour. There is no texture-set folder and no overlay.

`displayPrefix` and `displaySuffix` are the English spelling around the material name. An empty prefix and `" Sample Blank"` read as "Iron Sample Blank". A translation `item.<addonId>.<snake_name>` with one `%s` replaces that spelling. At least one of the two display strings is required.

`materials` names the materials that receive the prefix, including built-in materials. `conditions` is optional. The set is `PrefixCondition`: `HAS_TOOL_STATS`, `SMITHABLE`, `NOT_WOOD`, `NOT_COAL`, `NOT_STONE`, `NOT_FOOD` and `NOT_MEAT`. Every selected condition has to match. There is no custom predicate. An empty material list needs `HAS_TOOL_STATS` or `SMITHABLE`, because a negative condition on its own would cover almost every material. With one of those two, every matching material is included as well as the named ones. Without them, the negative conditions only check the named materials, and a name that fails is a startup error.

`NOT_COAL` is Coal, Charcoal, CoalCoke, Lignite, LigniteCoke, Anthracite, HydratedCoal and PetroleumCoke. Food is the GregTech food flag. Meat is MeatRaw, MeatCooked and MeatRotten.

The items are registered in the **addon's** namespace, after the generated `gt6ce:` forms. The order is addon id, then declaration order, then material name. The path has the same shape as a built-in form, `sample_blank_iron`. A path collision inside that namespace fails startup. The item does not take a `gt6ce:` id and does not change the built-in item id list. `forms()` cannot name this prefix; the material list on the declaration chooses the items. `GregTechPrefixes.exists` is true for the name after startup. `AddonItems.stack` returns the item.

GregTech does not add crafting recipes for the prefix. A block prefix is not part of this entry. API version stays 0.

## World generation

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

Stone layers stay the GregTech table. Addon ores are not inserted into a stone layer. With stone layers on, the legacy overworld large veins stay off, and the addon large veins are the ones that generate. They are picked by weight together with whatever built-in veins that table still contains. Adding weight changes `nextInt` for that table, so the same seed can place a different built-in vein. That happens for the End, where the built-in large veins stay on. Small ores are appended, and each row consumes its own random draws, so the built-in small ores keep their positions. API version stays 0.

## Crucible

Throwing a GregTech material form into a crucible already melts it. The amount is the form's material weight. Once the crucible is hot enough, the stack becomes that material's smelting target. With no declaration the target is the material itself, one unit out for one unit in, and the crucible smelting page does not list it. Molds still cast whatever material is molten into the mold's existing form, when that form exists.

`compositions()`, `smeltingTargets()`, `crucibleReactions()` and `crucibleRemovals()` are pure data on `AddonMaterialContributor`. GregTech reads them after the built-in directory has linked and before recipes load. Names are material names. `Cr` and `Chromium` are the same material. Unknown names, a non-positive yield, ratio or divider, an empty input list on a new row, or an output that is also an input fail startup and name the addon.

A composition may be set only on a material this addon declared. Part amounts are positive multiples of `GTValues.U`. The divider is stored, and the tooltip formula then comes from the composition. `crucible = true` also adds one alloy row: each ratio is `amount / U`, and the yield is the divider. `crucible = false` stores the composition and adds no row. Two owners of a merged material must declare the same composition, including that flag.

A smelting target may also be set only on this addon's material. `amount` is the output for each `U` of input, so `U` means one to one. The declaration marks the material as melting, which is what puts it on the crucible smelting page. Omitting it leaves the default: melt into itself, with no melting flag.

An explicit alloy row uses coefficients, not units. Three annealed copper and one tin producing four bronze is yield `4` and ratios `3` and `1`. The output may be an existing material. A removal names the output and the input materials. An empty input list removes every row of that output. Otherwise the input names must be the same set; order and ratios are ignored. Removals run after every addon has added its rows, so one addon can drop another addon's row. A removal that matches nothing is a warning, not a failed startup, and each removed row is logged with the addon id.

Assembly order is: built-in explicit rows, built-in composition rows, addon explicit rows in addon-id order, addon composition rows, then removals. The reaction rule is unchanged: one winning row per tick, at most one solid input, no reaction below an output or input melting point, and the greatest output wins. An addon row whose inputs and ratios exactly match another remaining row, with the same output and yield, fails startup. The same inputs and ratios with a different yield or output are kept, and startup warns that the greater output can take the other row over. Remove the old row first when that is the goal.

The table is frozen at the end of material linking. Reading it earlier fails startup. There is no datapack and no `/reload` for these rows. Mixer recipes that produce the same alloy are separate; remove those with `gt6ce:machine_removal`. This entry does not add crucible blocks, hulls, faucets, molds, or a way to say what an arbitrary non-GregTech item melts into. It does not write a crucible's temperature or contents.

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

## Registering machines

List an `AddonMachineContributor` in `META-INF/services/com.gregtech.gregtech.api.addon.AddonMachineContributor`. GregTech loads that file with its own class loader after materials are initialized and before it registers machine blocks. Return pure data from `addonMachines()`, `structureLayouts()`, `layoutParts()` and `recipeTables()`. Do not register blocks yourself. `fluidPipes()`, `itemPipes()` and `compressedGasTanks()` stay on this contributor. Pipes and compressed drums are in the sections of those names.

`addonId()` is the loader mod id. `gt6ce`, `gregtech6` and `minecraft` are reserved. The registry path may repeat across namespaces. `machineType` is global: it must not be a built-in catalog or layout name. Two blocks of the same addon may share a machine type when they agree on the signature for that template. A second addon cannot reuse the name.

GregTech registers the block, item and block-entity type in the addon's namespace, on GregTech's own mod bus. The block id is `<addonId>:<id>`. A recipe machine's block-entity id is `<addonId>:be_<id>`, using the id as declared. A behavior machine with an empty block-entity id, and every fuel generator, uses `be_<id>` with each `/` replaced by `_`. Built-in ids stay `gt6ce:<path>` and `gt6ce:be_<path>`. Forge can log `Potentially Dangerous alternative prefix` for that bus; the register call is still the Forge `DeferredRegister` API. NeoForge accepts `DeferredRegister.create(registry, addonNamespace)` on GregTech's bus. This choice comes from those loader APIs. It has not been confirmed by launching the game.

`gt6ce:basic_machine` is the menu for every recipe machine. The client looks up the recipe table by machine-type name. `gt6ce:behavior_machine` is the menu for every behavior machine. A fuel generator uses the ordinary machine stop switch and has no menu of its own.

Block models resolve to `<namespace>:block/machine/basic/<machineType>` plus `_running` and `_active`. GregTech still generates item models only for the `gt6ce` namespace; the addon ships its own models and blockstates. The display name is `block.<namespace>.<id>`.

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

## Casings

`MachineCasing.builder(id, material)` is the shell of all three templates. `id` matches `[a-z0-9/._-]+` and is the block path. `material` is non-blank. Startup requires a material that already exists. A layout part uses the same material lookup.

`machineType`, when set, matches `[a-z0-9_]+`. Leaving it unset is not the same on every template. A recipe machine and a fuel generator copy `id` unchanged and do not replace `/`. An id that contains `/` then fails, because the machine type still has to match `[a-z0-9_]+`. A behavior machine replaces each `/` with `_`.

`tier` defaults to 1 on the templates that accept it. A recipe machine only requires `tier >= 1`. A behavior machine requires 1 to 255. A fuel generator has no tier. A casing that called `tier` fails when that fuel generator is built.

## Energy

Energy coming in is an `EnergyInput`. Energy leaving a behavior machine, or an energy structure part, is an `EnergyOutput`. A fuel generator's packet is a `FuelOutput`. A recipe machine rejects an `EnergyOutput` and a `FuelOutput`. A behavior machine rejects a `FuelOutput`. A fuel generator rejects an `EnergyInput` and an `EnergyOutput`.

`EnergyInput.builder(type, perTick)` and `FuelOutput.builder(type, perTick)` take the short name, such as `EU` or `RU`. The builder only requires the name to be non-blank. Startup resolves it through the same energy-tag lookup. The direction is not one field. On a recipe machine and a behavior machine the short name on `EnergyInput` is input. On a fuel generator the short name on `FuelOutput` is output. A behavior machine that also emits writes the output short name on its `EnergyOutput`, separate from the input.

The nominal amount is not one range. A recipe machine's `perTick` is at least 0 and has no upper bound. A behavior machine's `perTick` is from 1 to 1,000,000,000,000. When its capacity is 0, that number is also the amount drained each tick. A fuel generator has no input nominal. `FuelOutput.perTick` is one output packet, from 1 to 1,000,000,000,000, and the builder requires it.

`EnergyInput.range(min, max)` is the accepted packet size. Both values are longs. Not calling it leaves both bounds at 0 and means the range is not declared. A declared range needs `1 <= min <= max`. Anything else fails at `build()`. The nominal `perTick` may sit outside the range. A recipe machine may declare a range. A TU recipe machine that declares one fails at `build()`. A behavior machine that calls `range` fails at `build()`. A fuel generator has no input range.

A single-block recipe machine that does not declare a range accepts half to double its nominal input. TU accepts 1 to 16. A large recipe machine that does not declare a range does not use half to double. It uses the built-in large-machine rule: 1 to 16 for TU, 1 to 4096 for HU, and 512 to 4096 for every other type, including EU and RU. Built-in distillation towers, the implosion compressor and matter fabricators keep the limits on their own classes. A behavior machine cannot declare a range. At startup GregTech uses half to double `perTick`, and TU is 1 to 16. That matches an undeclared single-block recipe machine. It does not match a large recipe machine.

A packet larger than the maximum is consumed and then follows the overload rule. With the default settings that does not destroy the machine. A recipe whose power is below the minimum is still selected. `MachineWorkCost` charges four times as much when the power on hand is below the minimum. The buffer cap is the maximum. The item tooltip, Jade, and the machine shown as a JEI or EMI catalyst all read that minimum and maximum. The recipe page still shows the recipe cost, not this range. Energy parts in a large recipe structure already read the controller's minimum and maximum. Two blocks that share a machine type must declare the same range. Both leaving it unset counts as the same range.

Faces are `MachineFace`: `BOTTOM`, `TOP`, `RIGHT`, `FRONT`, `LEFT`, `BACK`. `index()` and `bit()` are the mask positions. A face list on a new declaration rejects a repeated face.

`EnergyInput.faces` does not follow `MachineIo`, and the default is not the same on the two templates. On a recipe machine, leaving faces unset is all six. An empty list is allowed and means there is no energy-input face. On a behavior machine, leaving faces unset is every face except the front. An empty list fails. Setting `MachineIo` does not change these faces.

An older recipe entry that called any face method and did not set `energyInputs` is converted to an empty `faces()` list. A new `EnergyInput` that omits `faces()` is still all six, including when the machine also sets `MachineIo`. That is the only default that differs between the older entry and this one.

`EnergyInput.capacity` belongs to a behavior machine. It is at least 0, and the default is 0, including a call of `capacity(0)`. Zero means the buffer equals the rated input: GregTech drains that amount after the behavior returns, so unused energy does not carry into the next tick. A capacity above 0 keeps what remains, capped at that capacity. A recipe machine that calls `capacity` fails. An energy part does not use this field. Its capacity is the second argument of `LayoutPart.energyOutput` and must be at least 1.

The output shape is two objects. A behavior machine and an energy part share one `EnergyOutput`: a type, one or more faces, `maxSize` and `maxAmount`. All four are required. Both limits are at least 1. The face list must not be empty or repeated. The part adds the positive capacity above. A fuel generator is not that shape. `FuelOutput` is one `perTick` packet and a position: `front()` is the default, `back()` is the back face, and `behind(distance)` injects that many steps past the front. Distance is 1 to `FuelGenerator.MAX_BEHIND`, which is 8. Front and back keep the distance at 0. The packet is not a size multiplied by a count. Those three positions are not `MachineFace` values.

## Items and fluids

`MachineIo` is only for a recipe machine. A behavior machine that sets one fails. A fuel generator that sets one fails. A behavior machine moves items and fluid through slots, in Behavior machines. A fuel generator does not declare faces. A single block opens its fluid capability on all six sides. A multiblock opens only the front. Both still scan all six sides when they pull from an adjacent compressed drum.

Leaving `io` unset keeps six-side item and fluid access and no automatic face. Once a `MachineIo` is set, a channel that was not named is an empty face list, not the six-side default. Automatic faces default to none. A null automatic face fails at `build()`.

```java
MachineIo.builder()
        .itemInputs(MachineFace.TOP)
        .itemOutputs(MachineFace.BOTTOM)
        .itemAutoOutput(MachineFace.BOTTOM)
        .build()
```

`IoTarget.at(right, up, back, receiver)` is one large-recipe cell. `right` is clockwise from the facing, `up` is up, and `back` is opposite the facing. The receiver is a `MachineFace`. The builder methods are `itemOutputTarget`, `fluidOutputTarget`, `itemInputSource` and `fluidInputSource`. A declared output is pushed to that face. A declared input is pulled from that face. A channel left unset keeps its face rule. Naming a target leaves that channel's automatic face unset, so the same stack or fluid is not moved twice. Setting both fails at `build()`. Only `largeRecipe()` may name a target. A single block that names one fails. The cell does not have to be a structure part, and it may sit inside or outside the layout. An unloaded chunk skips that channel for that tick. Item transfer runs when the formed machine runs its automatic item transfer. Fluid output runs at the start of the machine tick, including while the machine is stopped or the structure is incomplete, matching the built-in large machines. Built-in controllers keep their own positions.

The three coordinate rules stay separate. A structure cell rejects the origin and a repeated position, and it has no ±16 limit. An `IoTarget` rejects the origin, and each of `right`, `up` and `back` is from -16 to 16, including both ends. A fuel `behind` distance is 1 to 8 steps past the front. It has no `right` or `up`.

## Structures

A `StructureLayout` is bound by name. The name equals the controller's `machineType`. Several machines may share one layout. The layout is not written inside the machine. The controller is the origin and is not a cell.

```java
StructureLayout.builder("mypress")
        .cell(0, 1, 1, CellMatch.part("sample_wall"), StructureRole.CASING)
        .cell(-1, 1, 2, CellMatch.block("minecraft:glass"), StructureRole.CASING)
        .build()
```

`LayoutCell.at(right, up, back, match, role)` is one cell. The origin fails in `LayoutCell.at`. A repeated position fails in `StructureLayout` `build()`. `CellMatch` is `part(name)`, `air()`, `block(id)` or `tag(id)`. `part` names a GregTech structure part, a structure casing, or a `layoutParts()` name. Those three already share one name space, so an addon part cannot take a built-in name. `block` is a block id that contains a namespace. `tag` is a block tag without the leading `#`. `block` and `tag` require `StructureRole.CASING`, which `LayoutCell.at` checks. `CellMatch.parse(String)` reads the string form: `air`, a leading `#` for a tag, a `:` for a block id, and any other text as a part name.

A block id or tag is a resource location: a lower-case namespace, one colon, and a path of `[a-z0-9/._-]`. Any other spelling fails. These cells compare the block only. They do not need a block entity, they are not bound, and they do not carry items, fluid or energy. Block ids are checked in common setup, after other mods have registered blocks. An unknown id fails startup and names the addon and the layout. A tag is read when the structure is checked. An empty tag never forms and logs one warning. The structure preview shows the block. A tag shows the member whose registry name comes first, or a barrier while the tag is empty. Removing the block makes the next structure check fail.

`StructureRole` is `AIR`, `CASING`, `LOGISTICS`, `HEAT_INPUT`, `CRUCIBLE`, `FLUID_INPUT`, `FLUID_OUTPUT`, `FLUID_IO`, `ITEM_IO`, `ITEM_FLUID_IO`, `ITEM_FLUID_INPUT`, `ITEM_FLUID_OUTPUT`, `ENERGY_INPUT`, `ITEM_FLUID_ENERGY_INPUT`, `ITEM_FLUID_ENERGY` and `ENERGY_OUTPUT`. It is not a behavior slot role.

Whether a layout is required, and when the machine counts as formed, depends on the template. A large recipe machine must have a layout of the same machine type. A missing layout fails startup, and a failed structure check does not start a job. A multiblock fuel generator must have a layout of the same machine type. A single-block fuel generator cannot have a layout, and it starts formed. A behavior layout is optional. With none, the first server tick sets formed, and formed is false before that tick. With a layout, the cells are checked. All three checks match a cell the same way.

A part name that is not a block id or a tag is a `SharedLargeMachineParts` name, `air`, `tank_wall`, `tank_wall_dense`, `centrifuge_part`, `heat_transmitter`, `distillation_tower_part`, `electrolyzer_part`, or a name from `layoutParts()`. An empty layout, a duplicated position, an unknown part, a layout with no controller, or a large recipe machine with no layout fails startup and names the addon.

`layoutParts()` returns casings and energy parts:

```java
public List<LayoutPart> layoutParts() {
    return List.of(
            LayoutPart.builder("sample_wall", "Steel").hardness(6.0F).build(),
            LayoutPart.builder("sample_emitter", "Steel").hardness(6.0F)
                    .energyOutput(EnergyOutput.builder("EU", 32, 4).faces(MachineFace.FRONT).build(), 100_000)
                    .build());
}
```

The name matches `[a-z][a-z0-9_]{0,63}`: it starts with a letter, it is at most 64 characters, and it contains no `/`. It must not be a built-in part name. The texture is a path in the addon's namespace, or empty. Empty uses the GregTech metal wall tinted with the material colour. A path is one untinted cube and must not include a namespace. Hardness is a positive float. The blast resistance is the same value. None of the three machine templates has a hardness field. The block id is `<addonId>:<name>`. GregTech registers it after the built-in multiblock parts, on GregTech's bus, in the addon's namespace, and generates the blockstate and item model. Names are global. A GregTech part name, including `tank_wall`, or another addon's name, fails startup and names both sides. Runtime ids start at 1000000, in addon id order and then declaration order. A structure stores the block registry name. Forming compares the blocks in the world. The number is not save data. A part that does not call `energyOutput` is a casing: no tank, no energy, no item slots and no tick.

`energyOutput(EnergyOutput, capacity)` makes an energy part. `capacity` is at least 1. EU and RU differ only by the type string. An unknown short name fails startup and names the addon and the part. The block still uses `<addonId>:<name>`. It has a horizontal facing, a ticker, and the block entity `gt6ce:addon_energy_part`. Output faces are relative to that facing, with the same front, back, left and right rule as a behavior machine. The buffer, the armed packet and the stopped flag are saved as `gt.part_energy`, `gt.part_output`, `gt.part_amount` and `gt.part_stopped`. Casings stay on `gt6ce:multiblock_port`.

The part pushes the armed packet on its output faces, in declaration order, while its cell role is `ENERGY_OUTPUT`, it is bound to this formed machine, and it is not stopped. Neighbors see the part through `EnergyBlockViews`. They do not pull the buffer, and they cannot inject into it. An `ENERGY_INPUT` cell keeps the buffer for the controller and does not emit. One part belongs to one controller coordinate. A second structure does not form while that coordinate still claims it. Releasing the part, including when one cell of the structure breaks, stops it. The declaration has no method that asks how much of a packet fits, and none that checks the packet limits. The part runtime truncates to the remaining capacity and rejects a packet above the declared maximum.

## Recipe tables

`RecipeTable.machine(name)` and `RecipeTable.fuel(name)` take the same fields and do not share a name. A recipe machine names a machine table. A fuel generator names a fuel table. `recipeMaps` on a behavior machine may name either, and GregTech does not craft those rows for the behavior.

`name` matches `[a-z0-9_.]+`. `title` and `gui` are required. A missing value fails at `build()`. A table `gui` is not a behavior sheet: it does not have to be a namespaced `.png`, and it is not checked against a widget rectangle. The behavior sheet rule is in Behavior machines. Counts default to 0 and must be at least 0: `inputItems`, `outputItems`, `minimalInputItems`, `inputFluids`, `outputFluids`, `minimalInputFluids` and `minimalInputs`. `needsOutputs`, `combinePower`, `bucketIn` and `bucketOut` default to false.

```java
RecipeTable.machine("my_addon.recipe.press")
        .title("Press")
        .gui("my_addon:textures/gui/machines/press")
        .inputItems(1).outputItems(1).minimalInputItems(1).minimalInputs(1)
        .needsOutputs(true)
        .build()
```

A positive `eu_per_tick` on a fuel table is energy produced per tick and is stored negative. `AddonRecipes.map` returns a fuel table the same way it returns a machine table. `find` returns a matching row. `Line.euPerTick` is the stored number, so a fuel row read back is negative. `AddonRecipes.add` stores the value as written, so a fuel row added there is already negative. A non-negative stored value is not energy to produce. The row still needs an output. A recipe machine that names a fuel table fails as an unknown recipe map. Reusing a machine-table name or a fuel-table name fails startup and names the addon.

The GUI path keeps a namespace written before `:`. Both `textures/gui/machines/Press` and `gt6ce:textures/gui/machines/Gas` still resolve to `gt6ce:textures/gui/recipes/<file>.png`. An addon path `my_addon:textures/gui/machines/press` resolves to `my_addon:textures/gui/recipes/press.png`, so the file on disk is `assets/my_addon/textures/gui/recipes/press.png`.

New tables are created before `onRecipesReady`, JEI, EMI and the datapack baseline, and they join `RecipeMap.RECIPE_MAP_LIST`. Add rows from `onRecipesReady` or with a `gt6ce:machine` datapack. The recipe page draws the slots the table declared. A fuel table with one or more item inputs draws those item slots. A built-in fuel table that declares no item slots does not add a slot for an item that happens to be on a row.

## Recipe machines

`RecipeMachine.builder(MachineCasing)` is only the recipe knobs. The casing, the energy input, the item and fluid faces, and the layout are the types in the sections above.

`basic()` is the default and builds one block. `largeRecipe()` builds a large recipe machine. There is no kind enum. Distillation, matter fabricators and the other built-in controllers stay inside GregTech. `largeRecipe()` is the kind that requires a layout and the only kind that may name an `IoTarget`. When that layout is required, and when the structure counts as formed, is under Structures.

`recipeMap` is required. It matches `[a-z0-9_.]+` and must be a machine table this contributor returned, or a built-in machine map.

`parallel` defaults to 1 and is at least 1. `efficiency` defaults to 10000. It is parts per 10000, from 1 to 100000. `constantEnergy` defaults to true. While the machine is not a TU machine it draws a constant amount. TU still does not. Built-in machines keep the old name-based efficiency, constant-energy and parallel values. Those rules are copied onto the spec when the built-in spec is constructed.

`cheapOverclocking(true)` enables the cheaper overclock and records that the addon set it. Leaving the method unset leaves that record false. A single block that does not call it stays off, including when its machine type would have matched a built-in cheap machine. A large recipe machine that does not call it stays cheap unless it uses TU.

`minimumLightAbove(14)` refuses to start a new job unless `Level.getRawBrightness` of the block above is at least 14. That is the 1.7.10 `getBlockLightValue` reading: the greater of sky light and block light, without subtracting night. The value is 0 to 15. `0`, the default, does not check. A job that has already started keeps its progress.

`searchOutputTanks(true)` runs a second recipe search on the output tanks when the input tanks do not match. Item slots stay the input slots. The job remembers which tanks matched and drains fluid from those tanks, including the parallel batch. A match on the input tanks is used as it stands. Fluid the recipe itself produces is still delivered to the output tanks. If the input tanks miss on the next search, that fluid can be consumed. Leaving the method unset keeps the search on the input tanks.

`exclusiveOutputs(true)` rolls the first item output once, in parts per 10000. A roll below that chance produces the first item. Any other roll produces the second. The produced count is that item's recipe count times the parallel batch. Fluid outputs stay certain. Every static row on that machine's recipe table needs exactly two non-empty item outputs. Any other row fails startup and names the addon and the machine. JEI and EMI show each of the two chances. The second chance is the hint. The roll does not read it. Leaving the method unset rolls each item output on its own.

`structureKeys` are translation keys for the structure tooltip. Each matches `[a-z0-9_.]+`. The list may be empty. They are not the layout.

Two blocks may share a machine type when they are the same addon and agree on the recipe table, single block or large, efficiency, the constant-energy flag, structure keys, face masks, whether cheap overclocking was set, the light gate, output-tank search, exclusive outputs, the energy range and the four positions. That is how two tiers of one machine share a GUI and a structure.

JEI catalysts and the multiblock preview follow the registered machine list. A large machine with a layout is included in the preview. The crafting recipe is the addon's own datapack. GregTech does not generate one. An addon large machine uses the default port path. Built-in name branches stay in place for built-in machines.

## Behavior machines

Return each `BehaviorMachine` from `addonMachines()`. Also list a `BehaviorMachineContributor` in `META-INF/services/com.gregtech.gregtech.api.addon.platform.BehaviorMachineContributor`. That interface lives in the loader API because `FluidStack` differs between Forge and NeoForge. `machineTypes()` must include every behavior machine type this addon declares. GregTech calls `create` once for each placed block entity. A missing factory fails startup and names the addon and the machine type.

The behavior does not extend a GregTech class. `MachineBehaviorContext` reads and writes items, tanks, energy, toggles, progress and the world. `BehaviorState` stores int, long, boolean, string and item values by key. GregTech writes that map to NBT. The behavior does not touch `CompoundTag`. A missing key reads as zero, false, an empty string or an empty item. Keys match `[a-z0-9_.]{1,64}`. Strings are at most 256 characters. GregTech drops inventory items and state items when the block breaks, after `beforeBreak`.

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

GregTech draws the GUI from the declaration and the addon's texture. The texture is both the panel and the widget sheet. Widget pixels use the constants on `BehaviorMachine`: slot `UV_SLOT_U`,`UV_SLOT_V` at 176,0 (18×18); energy empty `UV_ENERGY_U`,`UV_ENERGY_V` at 194,0 and full `UV_ENERGY_FULL_U` at 204,0 (10×54); tank `UV_TANK_U`,`UV_TANK_V` at 214,0 and marks `UV_TANK_MARKS_U` at 232,0 (18×54); progress empty `UV_ARROW_U`,`UV_ARROW_V` at 176,54 and full `UV_ARROW_FULL_V` at 71 (24×17); toggle off `UV_TOGGLE_OFF_U` at 176 and on `UV_TOGGLE_ON_U` at 194, both at `UV_TOGGLE_V` 88 (18×18). The panel defaults to 176 by 166. `guiSize` allows 176 to 256 wide and 166 to 512 tall. The energy gauge defaults to (8, 8). Progress defaults to (80, 35). The player inventory starts `PLAYER_INVENTORY_GAP` (82) pixels above the bottom. Item slot coordinates are the inner 16 pixels. The frame is drawn one pixel up and left, so a slot needs `x` and `y` of at least 1. Widgets must end at or above the player inventory and must not overlap. At most 54 item slots, 8 tanks and 16 toggles. A toggle key matches `[a-z0-9_.]+`.

A fluid slot names its own capacity. The capacity is 1 to 1,024,000,000 mB. The fuel tank's default is a different rule and is written under Fuel generators.

`BehaviorMachine.SlotRole` is `INPUT`, `OUTPUT` and `ANY`. Players and pipes insert into `INPUT` and `ANY` only when `canInsert` or `canFill` allows it. Pipes extract items from `OUTPUT` and `ANY`. Pipes drain fluid only from `OUTPUT` and `ANY`, so an input tank stays input-only. `insertOutput` fills `OUTPUT` and then `ANY`, and it does not ask `canInsert`. `acceptFluid` is the pipe and container fill path. The default stores the offered fluid when `canFill` allows it. Override it when a concentrate should become the tank's fluid. `context.fill` stores the offered fluid when the tank is empty or already holds it, and it does not call `canFill`. `pushCompressedGas(face, stack)` stores gas only in the compressed drum on that face. The write is immediate and the method returns the millibuckets stored. The behavior removes that fluid from its own tank. A pipe or an ordinary tank accepts nothing.

Input faces, the accepted range and a capacity of 0 are `EnergyInput`, under Energy. GregTech polls adjacent emitters, runs the behavior, then drains the buffer by the rated input. `consume` spends stored energy before that step. `generate(amount)` adds energy into the same buffer and returns how much fit. `setCapacity` changes the cap during the tick. GregTech writes it as `gt.capacity` only after that call. An old save without the key uses the declared capacity. The shell does not explode when stored energy is above the cap: the behavior can see that and break the block, and GregTech discards the excess after the behavior returns. `working(true)` lights the face. The running state means energy was present at the start of the tick. A packet larger than the accepted maximum destroys the machine when overvoltage explosions are enabled.

`energyOutput(EnergyOutput)` declares the output described under Energy. `emit(size, amount)` spends stored energy and pushes that packet through `EnergyTransfer.emitEnergyToSide` on each output face, in declaration order, until the requested amount is sent or the neighbors stop accepting. It returns how many packets left. A size or amount above the declared maximum throws. GregTech does not clamp it. `emit` on a machine with no output throws. Neighbors do not pull this stored energy. The behavior pushes it.

When the machine type has a layout, GregTech forms it before the behavior runs. `structureFormed()` is then true. `energyParts()` lists the energy parts in that layout. With no layout, formed becomes true on the first server tick and the part list is empty. `charge(amount)` fills a part up to its capacity and returns what fit, or 0 when that part is no longer bound to this machine. `setOutput(size, amount)` arms the part's packet and clears its stopped flag. A request above the part's declared maximum throws. `stop()` stops that part.

The behavior block entity stays an addon `EnergyBlock`. It does not also implement `IEnergyBlock`, because `getEnergyTypes` on the two interfaces erases to one method. Axles, laser fiber, relays, long-distance transformers, and the built-in machines that inject or extract resolve the neighbor with `EnergyBlockViews`. A built-in block still takes the same path it did before. A gearbox already emitted through `EnergyTransfer`, so it already reached an addon block that accepts energy. Scanner, Jade and cover readouts still look only at `IEnergyBlock`.

`onUse(context, player, hand)` runs on the server after a wrench and a fluid container, and before the menu. Return true to handle the click. The default returns false and the menu opens.

`maxRange(int)` is the cube half-extent for world actions. It is 0 to `BehaviorRangeRules.LIMIT`, which is 32. The default is 0, and both world methods then do nothing. Distance is Chebyshev: the largest of the three axis gaps. The machine block itself is outside that cube.

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

The default block-entity id is under Registering machines. An explicit `blockEntityId` lets several blocks share one block-entity type. The path matches `[a-z0-9_./-]{1,64}`. They must be the same addon, the same machine type, and the same input energy short name, output energy short name, item roles, fluid roles and toggle keys. Tank capacity, storage capacity, tier, energy per tick, output faces and widget positions may differ. A shared path that disagrees fails startup. The addon ships the blockstate, models and GUI texture. Toggle labels are the translation keys on the declaration. GregTech's behavior GUI strings are `gui.gregtech.behavior.energy`, `progress`, `tank`, `on` and `off`.

A behavior machine is not a recipe machine. GregTech does not look up a row, spend inputs, or write outputs for it. Recipe machines stay on `gt6ce:basic_machine`.

`recipeMaps(String... names)` lists machine tables or fuel tables. The block becomes a JEI and EMI catalyst for each name. A name is a table this addon declared, or a built-in table such as `gt.recipe.compressor` or `gt.recipe.fuels.engine`. Another addon's table is unknown. An unknown name fails startup and names the addon and the machine. A repeated name, or a name outside `[a-z0-9_.]+`, fails at `build()`. Leaving this unset adds no catalyst, so the lists already built from recipe machines and fuel generators stay as they are. The names do not make GregTech run the table.

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

## Fuel generators

`FuelGenerator.builder(MachineCasing)` burns one fuel table. The packet and the place it leaves are a `FuelOutput`, under Energy. Do not rewrite that packet as a size times a count.

`efficiency` is parts per 10000, from 1 to 100000, and defaults to 10000. That is the same range as a recipe machine. `tank` is the fuel-tank capacity in millibuckets. It defaults to 4000. The limit is 1 to `FuelGenerator.MAX_TANK`, which is 1,024,000,000, on the fuel tank and on each exhaust tank. A behavior fluid slot uses that same upper bound and has no default: the slot names its own capacity. The exhaust count is the fuel table's fluid output count. The fuel table needs at least one fluid input.

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

## Pipes

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

## Compressed gas tanks

`AddonMachineContributor.compressedGasTanks()` returns `CompressedGasTankDeclaration`s. The id is a lower-case registry path. The material must already be registered. Capacity is absolute millibuckets, from 1 to 1,024,000,000. It is not a compression multiplier. Gas proof is always true. Acid, plasma and magic proof are the pressure rating. The temperature ceiling is the material melting point, resolved when machines install.

```java
@Override public List<CompressedGasTankDeclaration> compressedGasTanks() {
    return List.of(new CompressedGasTankDeclaration(
            "sample_drum", "Steel", 12_800_000, false, false, false));
}
```

Ordinary insertion is closed. `fill` from a pipe, funnel or container returns 0, and `isFluidValid` is false. Drain stays open, so pipes and fuel generators can take the gas. A fuel generator pulls at most 1000 mB per tick from an adjacent compressed drum, and only a gas its fuel map accepts. Anything the fuel tank cannot hold is pushed back. Built-in gas turbines and diesel engines do not pull.

A compressor is a behavior machine. `pushCompressedGas` lives on the behavior context. There is no separate recipe shell. The sample compressor takes KU from the bottom and offers the gas in its tank to the drum on the back. The offer is that tick's KU times 10 mB. The KU is spent even when the drum stores nothing.

`AddonFluids.gas` reports whether a fluid is a gas. An empty fluid is false.

The block id is `<addon>:<id>`. Hardness is 1, blast resistance is 6, and the item stacks to 16. The creative tab files a class whose name contains Tank under fluid containers. GregTech emits an iron-block model, which an addon may replace. The display name is `block.<addon>.<id>`. The tooltip keys are `gt6ce.tank.compressed.store` and `gt6ce.tank.compressed.compressor`. Neither key is an original GT6 translation, so the shared Chinese file keeps the English text. An invalid stored fluid is emptied on tick. There is no fermentation and no auto output. The large valve multiblock is not included.

## Building an addon

Build the loader you are targeting. Forge: `./gradlew :multiversion:1.20.1-forge:addonSdkJar` writes `multiversion/versions/1.20.1-forge/build/addon-sdk/gt6ce-addon-sdk-1.20.1-forge-0.0.0.jar`. NeoForge: `./gradlew :multiversion:1.21.1-neoforge:addonSdkJar` writes `multiversion/versions/1.21.1-neoforge/build/addon-sdk/gt6ce-addon-sdk-1.21.1-neoforge-0.0.0.jar`. On Windows the command is `gradlew.bat`. Each jar is that loader's compile-only SDK: the core API plus that loader's platform API, in Mojang names, plus the license notices. It has no assets and no mod descriptor. Use `compileOnly files(...)` on that one jar. Do not shade or bundle these classes. The installed GregTech mod supplies them at runtime. A production Forge jar is SRG and is not the compile dependency. There is no Maven coordinate yet; take the jar from the build or from CI artifacts.

Declare a required loader dependency on **`gt6ce`** with `versionRange="[0.0.0,)"` (the internal content namespace remains **`gt6ce`**). Build your addon separately for each loader, using that loader's supported Java/Minecraft toolchain. One SDK jar does not make a Forge addon binary load on NeoForge.

## Supported scope

- Shared addon registration, version/context and recipe-ready lifecycle.
- `AddonMaterialContributor` materials and extra forms of existing prefixes, declared by name and generated as `gt6ce:` items. `dust()` and `metal()` select the generated set. `prefixes()` adds an item prefix in the addon's namespace. `oreVeins()` and `smallOres()` append large veins and small ores after the built-in rows of `minecraft:overworld`, `minecraft:the_nether` or `minecraft:the_end`. Read materials through `GregTechMaterials`, including `byproducts()`. Test a prefix with `GregTechPrefixes.exists`. Numeric ids are assigned at startup from 10000 and are not save data. `compositions()`, `smeltingTargets()`, `crucibleReactions()` and `crucibleRemovals()` name materials and are applied when materials link.
- `AddonItems.stack(prefix, material, count)`, `AddonItems.selectorTag(n)` for programmed circuit 0..24, and `AddonFluids` for a GregTech fluid field name (`DistW`, not a material name). Still and flowing are separate. `AddonFluids.gas` reports whether that fluid is a gas.
- `AddonRecipes` on a map that already exists: add, display, collision, item-input check, dynamic provider and find. A null add means the map rejected the row. `keptInputs` names item inputs that stay in the machine; a count of 0 is still rejected.
- `AddonMachineContributor` recipe machines and large recipe machines, including `searchOutputTanks`, `exclusiveOutputs`, `EnergyInput.range(min, max)`, and one optional cell each for item output, fluid output, item input and fluid input on a large recipe machine. An undeclared large machine is not half to double its nominal input. Energy is a short name such as `EU`. A multiblock names a GregTech part, a casing, or an energy part from `layoutParts()`. GregTech registers the machine and the part in the addon's namespace. Layout roles are `StructureRole`. A cell may also name a block id or a `#` block tag; that cell is only a `CASING`, and it is not bound. `fluidPipes()` and `itemPipes()` add one material row each; GregTech expands them into the built-in sizes in the addon's namespace.
- `BehaviorMachine` plus the loader's `BehaviorMachineContributor`, `MachineBehavior`, `MachineBehaviorContext` and `BehaviorState`. GregTech owns the shell. The addon does not subclass it and does not touch NBT. `recipeMaps` names machine or fuel maps and makes the block a JEI and EMI catalyst; the behavior still does the work. A fuel row read by `find` stores produced energy as a negative `euPerTick`. `EnergyInput.capacity`, `emit`, `generate`, `setCapacity`, `onUse`, `maxRange`, `breakBlock` and `collectItems` are part of that shell. A layout with the same machine name makes `structureFormed()` and `energyParts()` available. `charge`, `setOutput` and `stop` write the energy part. GregTech stores a changed capacity as `gt.capacity`. The part stores its own buffer.
- `RecipeTable.fuel` and `FuelGenerator`. A fuel map joins JEI, EMI and the datapack overlay. A generator burns fluid and emits one packet per tick from the front, the back, or 1 to 8 blocks behind. A multiblock layout may name a GregTech part or a casing from `layoutParts()`.
- `compressedGasTanks()`. Ordinary insertion is closed. `pushCompressedGas` fills only an adjacent compressed drum, and a fuel generator pulls at most 1000 mB per tick from one.
- `GTValues`, `GTVoltageTiers`, `BasicMachineEnergy` and `MachineWorkCost`.
- `EnergyBlock` for an addon energy block. `AddonEnergy` runs GregTech's packet gate. EU cable insert and extract accept this interface. Axles, laser fiber, relays, long-distance transformers, and built-in injection or extraction resolve neighbors through `EnergyBlockViews`, so they also see this interface. Scanner, Jade and cover readouts still see only GregTech's own energy blocks.
- `AddonTool`: `SPADE`, `UNIVERSAL_SPADE` and `MAGNIFYING_GLASS`, plus `AddonTools.matches`, `isMagnifyingGlass` and `damageForUse`.
- Datapack types `gt6ce:machine` and `gt6ce:machine_removal` on maps that already exist.
- `GtSignalCarrier`, `GtSignalSource`, `GtSignalReceiver`, `CropScanSource`, and the empty `gt6ce:signal_wire_receivers` block tag.

These stay closed. `BasicMachineBlock` and `BasicMachineBlockEntity` are GregTech's machine implementation; an addon machine is data, not a subclass. `Recipe.mRecipeList` is the internal row list; production code uses the `AddonRecipes.Line` returned by `add`. `GTMaterial`, `GTMaterialRegistry` and the material sentinels are replaced by `GregTechMaterials`. `GregTechTags` is replaced by energy short names. `MachineRecipeMaps` fields are replaced by `AddonRecipes.map(name)`. `MaterialPrefix` constants are replaced by prefix name strings. The full tool catalog stays inside GregTech.

API version 0 includes crucible compositions, smelting targets and alloy rows, and still reads an old numeric material id as a built-in material, writing the name the next time that block or item is saved. It does **not** promise solid fuel, energy conversion, moving the built-in engines onto this template, addon-drawn GUIs, structure parts other than casings and energy parts, crucible blocks, writing a crucible's temperature or contents, melting an arbitrary non-GregTech item, datapack crucible rows, datapack-created recipe maps without a contributor, datapack reloads of the material table, one binary for both loaders, or a complete old GregAPI replacement. Only the documented entries are the compatibility surface. Use [the voltage guide](VOLTAGE_TIERS.md) rather than GT5/GTCE tier labels.

The SDK/project code license is LGPL-3.0-or-later. Preserve applicable source/asset notices; consult `LICENSE`, `COPYING` and `NOTICE` for the actual declarations.
