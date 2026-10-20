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

Legacy entries are marked deprecated from the CE baseline `0.0`. Numeric material entries retain their `0.9` earliest removal deadline; the four legacy contributor/layout entries are deferred to `0.10` when importing crucible declarations. Resetting the current version does not bring removal forward.

| Entry | Deprecated | Replacement | Remove at |
| --- | --- | --- | --- |
| com.gregtech.gregtech.api.addon.GregTechMachineContributor | 0.0 | AddonMachineContributor | 0.10 |
| com.gregtech.gregtech.api.addon.GregTechMaterialContributor | 0.0 | AddonMaterialContributor | 0.10 |
| com.gregtech.gregtech.api.addon.AddonMachineDefinition | 0.0 | MachineDeclaration | 0.10 |
| com.gregtech.gregtech.api.addon.AddonMachineLayout | 0.0 | MachineLayout | 0.10 |
| com.gregtech.gregtech.api.addon.MaterialDeclaration#builder | 0.0 | MaterialDeclaration.builder(String) | 0.9 |
| com.gregtech.gregtech.api.addon.AddonMaterials#ADDON_ID_MIN | 0.0 | MaterialDeclaration.builder(String) | 0.9 |
| com.gregtech.gregtech.api.addon.AddonMaterials#ADDON_ID_MAX | 0.0 | MaterialDeclaration.builder(String) | 0.9 |

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

Saves store the name. Crucible contents are `{name, amount}` under `gt.materials`. The crucible display field `gt.display.material` is the name, or an empty string when the crucible is not showing a material. USB sticks and drive files keep the key `gt.replicator.data` and store the name as a string. A save written by an older build still loads: a numeric `id`, or a numeric `gt.replicator.data` / `gt.display.material`, resolves a built-in id and does not resolve an addon id. An unknown name is skipped, as an unknown id already was. There is no numeric suffix for an addon item-id collision.

Generated forms are registered in the **`gt6ce`** namespace, through the same `MaterialItemDefinitions` list both loaders already use. An addon’s own handwritten items, blocks and machines stay in the addon’s namespace. Both loaders also treat a `gt6ce:` id as GregTech content where they branch on that namespace, including the food-item lookup, so a generated addon form is covered by those checks. A new material receives only the prefixes that list already generates for its properties: `dust()` is what creates the dust items. `forms()` may name one of those prefixes. A pair the list already emits is not registered again. A prefix that is not in that list, including `crystal`, fails startup. That prefix exists as a validator, but the built-in directory never generates it, so it has no icon and no `item.gt6ce.crystal` translation. A generated prefix that `isValidFor` rejects still fails startup, except an explicit `plate` or `foil` of an addon material. Those two are emitted even when the material has no ingot, and they do not pull in double plates, dense plates or rods. A plate of diamond still fails, because diamond is built-in. `oreRaw` and any other rejected prefix still fail. Addons cannot add prefixes. Materials are fixed at startup; a datapack reload cannot add or remove them. An old numeric id is read when the block or item loads. The save is not rewritten until that block or item is saved again.

The translation key is `material.gt6ce.<lowercasename>`, from `GTMaterial.getTranslationKey()`. Put it in the addon’s own `assets/gt6ce/lang/en_us.json` and `zh_cn.json`. Minecraft merges those files with GregTech’s by key. A tooltip on an addon material uses `tooltip.gt6ce.material_from_addon` (“Added by the %s addon”), or `tooltip.gt6ce.material_from_addons` when several addons declared it. The shared Chinese file keeps the English fallback for both keys, because neither is an original GT6 translation.

A `dust()` material such as ammonium nitrate generates **6** items. Iron’s automatic forms are **96**. `metal()` is the declaration that produces that larger set. Phase C recipe generators walk every material, so a new material receives the furnace, macerator and similar rows its properties already imply. Removing the addon leaves those `gt6ce:` items missing under the usual Minecraft rules. A crucible or USB entry whose name is no longer loaded is skipped, so unloading an addon does not crash the load. A world reload after uninstall has not been run.

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

## Recipe machines

List an `AddonMachineContributor` in `META-INF/services/com.gregtech.gregtech.api.addon.AddonMachineContributor`. GregTech loads that file with its own class loader after materials are initialized and before it registers machine blocks. Return pure data from `recipeMaps()`, `machines()` and `layouts()`. Do not register blocks yourself. The casing material is a material name. Energy is the short name, such as `EU`.

```java
public final class MyMachines implements AddonMachineContributor {
    public String addonId() { return "my_addon"; }
    public List<RecipeMapDeclaration> recipeMaps() {
        return List.of(new RecipeMapDeclaration("my_addon.recipe.press", "Press",
                "my_addon:textures/gui/machines/press",
                1, 1, 1, 0, 0, 0, 1, true, false, false, false));
    }
    public List<MachineDeclaration> machines() {
        return List.of(MachineDeclaration.builder("press", "Steel")
                .machineType("mypress").energy("EU", 32).tier(1)
                .recipeMap("my_addon.recipe.press").basic().build());
    }
}
```

GregTech registers the block, item and block-entity type in the addon's namespace, on GregTech's own mod bus. The block id is `my_addon:press` and the block-entity id is `my_addon:be_press`. Built-in ids stay `gt6ce:<path>` and `gt6ce:be_<path>`. Forge can log `Potentially Dangerous alternative prefix` for that bus; the register call is still the Forge `DeferredRegister` API. NeoForge accepts `DeferredRegister.create(registry, addonNamespace)` on GregTech's bus. This choice comes from those loader APIs. It has not been confirmed by launching the game. One shared menu, `gt6ce:basic_machine`, serves every basic machine. The client menu looks up the recipe map by machine-type name.

`addonId()` is the loader mod id. `gt6ce`, `gregtech6` and `minecraft` are reserved. The registry path may repeat across namespaces. `machineType` (the machine name) is global: it must not be a built-in catalog or layout name. Two blocks may share a machine name when they are the same addon and agree on recipe map, controller kind, efficiency, the constant-energy flag, structure keys, face masks, the cheap-overclock declaration and the light gate. That is how two tiers of one machine share a GUI and a structure. A second addon cannot reuse the name.

A machine is `MachineDeclaration.Kind.BASIC` or `LARGE_RECIPE`. Distillation, matter fabricators and the other built-in controllers stay inside GregTech. `efficiency` is parts per 10000, from 1 to 100000. `constantEnergy` is the non-timed rule: while the machine is not a TU machine it draws a constant amount. Parallel limits use `MachineDeclaration.parallel`, default 1. Built-in machines keep the old name-based efficiency, constant-energy and parallel values; those rules are copied onto the spec when the built-in spec is constructed.

Leaving the face methods unset keeps six-side item and fluid access, six-side energy input, and no automatic face. Any face method makes the mask explicit. Faces are `BehaviorMachineDeclaration.Face`: bottom, top, right, front, left, back. Automatic faces default to none. A null automatic face is none. `cheapOverclocking(true)` enables the cheaper overclock. A basic machine that does not call it stays off, including when its machine name would have matched a built-in cheap machine. A large recipe machine that does not call it keeps the previous rule: cheap unless it uses TU. `minimumLightAbove(14)` refuses to start a new job unless `Level.getRawBrightness` of the block above is at least 14. That is the 1.7.10 `getBlockLightValue` reading: the greater of sky light and block light, without subtracting night. `0` does not check. A job that has already started keeps its progress.

A `LARGE_RECIPE` machine needs one `MachineLayout` with the same machine name. Cells use `right`, `up`, `back`, a part name and a `StructureRole`. The controller is the origin and must not be a cell. Part names are `SharedLargeMachineParts` names, `air`, or `tank_wall`, `tank_wall_dense`, `centrifuge_part`, `heat_transmitter`, `distillation_tower_part` and `electrolyzer_part`. There is no addon casing or port block. An empty layout, a duplicated position, an unknown part, a layout with no `LARGE_RECIPE` machine, or a `LARGE_RECIPE` machine with no layout fails startup and names the addon. Unknown energy types and unknown recipe maps do the same. Recipe map names use `[a-z0-9_.]+` and must be a map this contributor returned, or a built-in machine map. Fuel maps are not machine maps. `structureKeys` are translation keys (`[a-z0-9_.]+`) for the structure tooltip.

The GUI path keeps a namespace written before `:`. Both `textures/gui/machines/Press` and `gt6ce:textures/gui/machines/Gas` still resolve to `gt6ce:textures/gui/recipes/<file>.png`. An addon path `my_addon:textures/gui/machines/press` resolves to `my_addon:textures/gui/recipes/press.png`, so the file on disk is `assets/my_addon/textures/gui/recipes/press.png`. Block models resolve to `<namespace>:block/machine/basic/<machineName>` plus `_running` and `_active`. GregTech still generates item models only for the `gt6ce` namespace; the addon ships its own models and blockstates. The display name is `block.<namespace>.<id>`.

New maps are created before `onRecipesReady`, JEI, EMI and the datapack baseline, and they join `RecipeMap.RECIPE_MAP_LIST`. Add rows from `onRecipesReady` or with a `gt6ce:machine` datapack. JEI catalysts and the multiblock preview follow the registered machine list. A large machine with a layout is included in the preview. The crafting recipe is the addon's own datapack; GregTech does not generate one.

API 0 assigns addon material ids and stores material names in crucible and USB data. Existing GregTech fluid field names are available through `AddonFluids`. KubeJS still submits `gt6ce:machine` markers for maps that already exist, including a map this entry created. Built-in `OriginalLargeRecipeMachineData` name branches stay in place for built-in machines. An addon large machine uses the default port path.

## Behavior machines

Return `BehaviorMachineDeclaration`s from `AddonMachineContributor.behaviorMachines()`. Also list a `BehaviorMachineContributor` in `META-INF/services/com.gregtech.gregtech.api.addon.platform.BehaviorMachineContributor`. That interface lives in the loader API because `FluidStack` differs between Forge and NeoForge. `machineTypes()` must include every behavior `machineType` this addon declares. GregTech calls `create` once for each placed block entity. A missing factory fails startup and names the addon and the machine type.

The behavior does not extend a GregTech class. `MachineBehaviorContext` reads and writes items, tanks, energy, toggles, progress and the world. `BehaviorState` stores int, long, boolean, string and item values by key. GregTech writes that map to NBT. The behavior does not touch `CompoundTag`. A missing key reads as zero, false, an empty string or an empty item. Keys match `[a-z0-9_.]{1,64}`. Strings are at most 256 characters. GregTech drops inventory items and state items when the block breaks, after `beforeBreak`.

```java
public List<BehaviorMachineDeclaration> behaviorMachines() {
    return List.of(BehaviorMachineDeclaration.builder("irrigator", "Steel")
            .machineType("irrigator").energy("EU", 32).tier(1)
            .gui("my_addon:textures/gui/machine.png")
            .item(44, 26, BehaviorMachineDeclaration.SlotRole.INPUT)
            .item(116, 26, BehaviorMachineDeclaration.SlotRole.OUTPUT)
            .fluid(8000, 152, 8, BehaviorMachineDeclaration.SlotRole.INPUT)
            .toggle("my_addon.irrigator.water", 26, 64, true)
            .build());
}
```

One menu, `gt6ce:behavior_machine`, serves every behavior machine. GregTech draws the GUI from the declaration and the addon's texture. The texture is both the panel and the widget sheet. Widget pixels use these UV constants on `BehaviorMachineDeclaration`: slot 176,0 (18×18); energy empty 194,0 and full 204,0 (10×54); tank 214,0 and marks 232,0 (18×54); progress empty 176,54 and full 176,71 (24×17); toggle off 176,88 and on 194,88 (18×18). The panel is 176 to 256 wide and 166 to 512 tall. The player inventory starts 82 pixels above the bottom. Item slot coordinates are the inner 16 pixels; the frame is drawn one pixel up and left, so a slot needs `x` and `y` of at least 1. Widgets must end at or above the player inventory and must not overlap. At most 54 item slots, 8 tanks and 16 toggles. Tank capacity is 1 to 1,024,000,000 mB.

Slot roles are `INPUT`, `OUTPUT` and `ANY`. Players and pipes insert into `INPUT` and `ANY` only when `canInsert` or `canFill` allows it. Pipes extract items from `OUTPUT` and `ANY`. Pipes drain fluid only from `OUTPUT` and `ANY`, so an input tank stays input-only. `insertOutput` fills `OUTPUT` and then `ANY`, and it does not ask `canInsert`. `acceptFluid` is the pipe and container fill path. The default stores the offered fluid when `canFill` allows it. Override it when a concentrate should become the tank's fluid. `context.fill` stores the offered fluid when the tank is empty or already holds it, and it does not call `canFill`.

Energy reuses `BasicMachineEnergy` and the declared faces. The default faces are every side except the front. The nominal packet is `energyPerTick`. The accepted range is half to double that value, except TU. A larger packet destroys the machine when overvoltage explosions are enabled. GregTech polls adjacent emitters during the machine tick, then drains the buffer, so unused energy does not carry into the next tick. `consume` is how the behavior records a spend before that drain. `working(true)` lights the face. The running state means energy was present at the start of the tick.

An empty `blockEntityId` becomes `be_<id>`, with `/` replaced by `_`. An explicit path lets several blocks share one block-entity type. They must be the same addon, the same machine type, and the same energy short name, item roles, fluid roles and toggle keys. Tank capacity, tier, EU per tick and widget positions may differ. A shared path that disagrees fails startup. A structure layout whose name is only a behavior machine still has no controller. This entry is single-block. It does not add a multiblock behavior machine or an addon-drawn GUI.

The block id is `<addon>:<id>`. Models resolve to `<addon>:block/machine/basic/<machineType>` plus `_running` and `_active`. The addon ships the blockstate, models and GUI texture. The display name is `block.<addon>.<id>`. Toggle labels are the translation keys on the declaration. GregTech's behavior GUI strings are `gui.gregtech.behavior.energy`, `progress`, `tank`, `on` and `off`.

A behavior machine is not a recipe machine. It does not create a `RecipeMap` and it is not a JEI or EMI catalyst. Recipe machines stay on `gt6ce:basic_machine`.

## Fuel generators

`fuelMaps()` returns `FuelMapDeclaration`. `fuelGenerators()` returns `FuelGeneratorDeclaration`. A fuel map is not a machine map. The name uses `[a-z0-9_.]+`, and the slot counts match a built-in fuel map. GregTech creates it before the datapack baseline, so it joins JEI, EMI and `gt6ce:machine`. A positive `eu_per_tick` on a fuel map is energy produced per tick and is stored negative. The row still needs an output. A generator may name an addon fuel map or a built-in map such as `gt.recipe.fuels.engine` or `gt.recipe.fuels.gas`. A recipe machine that names a fuel map fails as an unknown recipe map. A fuel map that reuses a machine-map or fuel-map name fails startup and names the addon.

`efficiency` is parts per 10000, from 1 to 100000. `energy` is a short name such as `RU`. `outputPerTick` is one packet, from 1 to 1,000,000,000,000. The buffer spends that packet only after a receiver accepts it. Tank capacity is 1 to 1,024,000,000 mB on the fuel tank and on each exhaust tank. The exhaust count is the fuel map's fluid output count. The fuel map needs at least one fluid input. `outputFront()` and `outputBack()` emit to the adjacent face. `outputBehind(distance)` injects into the block that many steps past the front. Distance is 1 to 8. A gas turbine's four blocks uses this form. There is no arbitrary offset.

`single()` is one block. `multi()` needs a `MachineLayout` whose name is `machineType`, built from existing GregTech parts. An unformed structure does not burn and does not emit. The block id is `<addon>:<id>`. The block entity is `be_<id>`, with `/` replaced by `_`. Models are `<addon>:block/machine/basic/<machineType>` plus `_running` and `_active`. The addon ships the blockstate and models. A multiblock is in the structure preview. Both kinds are JEI and EMI catalysts for their fuel map. The stop switch is the ordinary machine control.

```java
@Override public List<FuelMapDeclaration> fuelMaps() {
    return List.of(new FuelMapDeclaration("my_addon.fuel.gas", "Gas",
            "my_addon:textures/gui/machines/gas",
            0, 0, 0, 1, 1, 1, 1, false, false, false, false));
}

@Override public List<FuelGeneratorDeclaration> fuelGenerators() {
    return List.of(
            FuelGeneratorDeclaration.builder("engine", "Steel")
                    .fuelMap("my_addon.fuel.gas").efficiency(10_000)
                    .energy("RU", 32).tank(4_000).outputFront().single().build(),
            FuelGeneratorDeclaration.builder("turbine", "Steel")
                    .machineType("sample_turbine").fuelMap("gt.recipe.fuels.gas")
                    .efficiency(5_000).energy("RU", 32).tank(8_000).outputBehind(4).multi().build());
}
```

This entry does not convert one energy type into another, does not burn solid fuel, and leaves the built-in diesel engine and gas turbine on their own block entities.

## Building an addon

Build the loader you are targeting. Forge: `./gradlew :multiversion:1.20.1-forge:addonSdkJar` writes `multiversion/versions/1.20.1-forge/build/addon-sdk/gt6ce-addon-sdk-1.20.1-forge-0.0.0.jar`. NeoForge: `./gradlew :multiversion:1.21.1-neoforge:addonSdkJar` writes `multiversion/versions/1.21.1-neoforge/build/addon-sdk/gt6ce-addon-sdk-1.21.1-neoforge-0.0.0.jar`. On Windows the command is `gradlew.bat`. Each jar is that loader's compile-only SDK: the core API plus that loader's platform API, in Mojang names, plus the license notices. It has no assets and no mod descriptor. Use `compileOnly files(...)` on that one jar. Do not shade or bundle these classes. The installed GregTech mod supplies them at runtime. A production Forge jar is SRG and is not the compile dependency. There is no Maven coordinate yet; take the jar from the build or from CI artifacts.

Declare a required loader dependency on **`gt6ce`** with `versionRange="[0.0.0,)"` (the internal content namespace remains **`gt6ce`**). Build your addon separately for each loader, using that loader's supported Java/Minecraft toolchain. One SDK jar does not make a Forge addon binary load on NeoForge.

## Supported scope

- Shared addon registration, version/context and recipe-ready lifecycle.
- `AddonMaterialContributor` materials and extra forms of existing prefixes, declared by name and generated as `gt6ce:` items. `dust()` and `metal()` select the generated set. Read materials through `GregTechMaterials`. Test a prefix with `GregTechPrefixes.exists`. Numeric ids are assigned at startup from 10000 and are not save data. `compositions()`, `smeltingTargets()`, `crucibleReactions()` and `crucibleRemovals()` name materials and are applied when materials link.
- `AddonItems.stack(prefix, material, count)`, `AddonItems.selectorTag(n)` for programmed circuit 0..24, and `AddonFluids` for a GregTech fluid field name (`DistW`, not a material name). Still and flowing are separate.
- `AddonRecipes` on a map that already exists: add, display, collision, item-input check, dynamic provider and find. A null add means the map rejected the row. `keptInputs` names item inputs that stay in the machine; a count of 0 is still rejected.
- `AddonMachineContributor` recipe machines and large recipe machines. Energy is a short name such as `EU`. A multiblock uses existing GregTech part names. GregTech registers the blocks in the addon's namespace. Layout roles are `StructureRole`.
- `BehaviorMachineDeclaration` plus the loader's `BehaviorMachineContributor`, `MachineBehavior`, `MachineBehaviorContext` and `BehaviorState`. GregTech owns the single-block shell. The addon does not subclass it and does not touch NBT.
- `FuelMapDeclaration` and `FuelGeneratorDeclaration`. A fuel map joins JEI, EMI and the datapack overlay. A generator burns fluid and emits one packet per tick from the front, the back, or 1 to 8 blocks behind. A multiblock uses an existing GregTech part layout.
- `GTValues`, `GTVoltageTiers`, `BasicMachineEnergy` and `MachineWorkCost`.
- `EnergyBlock` for an addon energy block. `AddonEnergy` runs GregTech's packet gate. EU cable insert and extract accept this interface. Other energy networks still see only GregTech's own energy blocks.
- `AddonTool`: `SPADE`, `UNIVERSAL_SPADE` and `MAGNIFYING_GLASS`, plus `AddonTools.matches`, `isMagnifyingGlass` and `damageForUse`.
- Datapack types `gt6ce:machine` and `gt6ce:machine_removal` on maps that already exist.
- `GtSignalCarrier`, `GtSignalSource`, `GtSignalReceiver`, `CropScanSource`, and the empty `gt6ce:signal_wire_receivers` block tag.

These stay closed. `BasicMachineBlock` and `BasicMachineBlockEntity` are GregTech's machine implementation; an addon machine is data, not a subclass. `Recipe.mRecipeList` is the internal row list; production code uses the `AddonRecipes.Line` returned by `add`. `GTMaterial`, `GTMaterialRegistry` and the material sentinels are replaced by `GregTechMaterials`. `GregTechTags` is replaced by energy short names. `MachineRecipeMaps` fields are replaced by `AddonRecipes.map(name)`. `MaterialPrefix` constants are replaced by prefix name strings. The full tool catalog stays inside GregTech.

API version 0 includes crucible compositions, smelting targets and alloy rows, and still reads an old numeric material id as a built-in material, writing the name the next time that block or item is saved. It does **not** promise solid fuel, energy conversion, moving the built-in engines onto this template, multiblock behavior machines, addon-drawn GUIs, new prefixes, addon casings or crucible blocks, writing a crucible's temperature or contents, melting an arbitrary non-GregTech item, datapack crucible rows, datapack-created recipe maps without a contributor, datapack reloads of the material table, one binary for both loaders, or a complete old GregAPI replacement. Only the documented entries are the compatibility surface. Use [the voltage guide](VOLTAGE_TIERS.md) rather than GT5/GTCE tier labels.

The SDK/project code license is LGPL-3.0-or-later. Preserve applicable source/asset notices; consult `LICENSE`, `COPYING` and `NOTICE` for the actual declarations.
