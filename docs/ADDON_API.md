# Addon API

[English](ADDON_API.md) | [中文](ADDON_API.zh-CN.md)

API version **2** provides two shared entries on Forge 1.20.1 and NeoForge 1.21.1. Register a `GregTechAddon` in your own mod constructor for recipes. GregTech calls `onRecipesReady` once from common setup, after its material/item linking and built-in machine recipes. Callbacks run in ascending addon mod ID order. Duplicate IDs, late registration and a second dispatch fail explicitly; callback failures stop initialization and leave the lifecycle `FAILED`.

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
        // Query existing GTMaterialRegistry / MaterialPrefix domain here.
        // Register additional rows into existing MachineRecipeMaps here.
    }
});
```

An example recipe callback, where `MyItems.MY_ORE` is registered by your addon:

```java
var recipe = new com.gregtech.gregtech.api.recipe.Recipe(
        new net.minecraft.world.item.ItemStack[]{
            new net.minecraft.world.item.ItemStack(MyItems.MY_ORE.get())},
        new net.minecraft.world.item.ItemStack[]{
            com.gregtech.gregtech.registry.GTItems.getStack(
                com.gregtech.gregtech.data.MaterialPrefix.dust,
                com.gregtech.gregtech.content.material.Materials.Copper)},
        null, null, null, null, 16, 16, 0);
if (com.gregtech.gregtech.data.MachineRecipeMaps.Mortar.addRecipe(recipe) == null)
    throw new IllegalStateException("my_addon mortar recipe rejected");
```

This uses the existing GT recipe system, not a parallel addon recipe registry. Choose inputs/outputs and power/duration appropriate for your addon. New rows are available to the existing machine recipe lookup and viewers; overlapping inputs retain that map's original precedence rules. A recipe-ready callback is a startup extension, not a datapack reload callback.

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

A block that only receives power implements `GtSignalReceiver` on its block entity, or adds its block to the `gregtech:signal_wire_receivers` block tag. Either one keeps a strongly powered solid block from being treated as a source. A block that matches both is a receiver. The tag ships empty; add entries from the addon's datapack:

```json
{ "replace": false, "values": ["my_addon:display"] }
```

Vanilla dispensers (including droppers), hoppers, pistons, TNT, note blocks, trapdoors, doors, powered rails (including activator rails), and redstone lamps stay on the existing `instanceof` checks and are not part of this tag. Do not reference `SignalWireBlockEntity`; it is a platform class and is not in the addon SDK.

## Multiple dynamic providers

Static rows are still searched first. Dynamic providers run only when none of those rows match, in this order: the built-in provider installed with `RecipeMap.dynamicRecipes`, then each provider passed to `RecipeMap.addDynamicRecipes`, in call order. The first provider that returns a non-null recipe is the dynamic answer, and later providers are not asked. Returning null means the provider does not handle that input. `dynamicRecipes` replaces only the built-in provider. `addDynamicRecipes` appends one provider and does not replace the built-in provider or any earlier addon provider.

`MachineRecipeMaps.Plantalyzer` has no built-in provider. An addon can append its own, for example a crop seed scan, without removing the Bumblelyzer, molecular scanner, printer or replicator providers.

## Viewer rows

`RecipeMap.addFakeRecipe` stores a display row for JEI and EMI. `findRecipe` skips fake rows, so a machine never crafts them. Addons can use it to show an example of a dynamic result, the same way the Bumblelyzer shows one scan on its recipe page. A fake row does not replace a provider: the machine still needs `addDynamicRecipes` to compute the real output.

## Materials and forms

List a `GregTechMaterialContributor` in `META-INF/services/com.gregtech.gregtech.api.addon.GregTechMaterialContributor`. GregTech loads that file with its own class loader at the start of material initialization, before the built-in directory is declared. Return pure data. Do not touch `Materials` from the service constructor or from `materials()` / `forms()`. `link()` runs later, after the built-in directory has linked, and is the place for a composition that names another material.

```java
public final class MyMaterials implements GregTechMaterialContributor {
    public String addonId() { return "my_addon"; }
    public List<MaterialDefinition> materials() {
        return List.of(MaterialDefinition.builder(4291, "AmmoniumNitrate")
                .displayName("Ammonium Nitrate").color(0xC8D6C0).chemicalFormula("NH4NO3")
                .dust().build());
    }
    public List<MaterialFormRequest> forms() {
        return List.of();
    }
}
```

Material ids must be in **4291–7899** inclusive. That band is empty in the built-in directory. 7900–7912 are the vanilla-matter materials, and ore ids continue from 8000. A name or id that is already used fails startup and names both sides, core against addon or addon against addon. There is no numeric suffix for an addon item-id collision.

Generated forms are registered in the **`gregtech`** namespace, through the same `MaterialItemDefinitions` list both loaders already use. An addon’s own handwritten items, blocks and machines stay in the addon’s namespace. Both loaders also treat a `gregtech:` id as GregTech content where they branch on that namespace, including the food-item lookup, so a generated addon form is covered by those checks. A new material receives only the prefixes that list already generates for its properties: `dust()` is what creates the dust items. `forms()` may name one of those prefixes. A pair the list already emits is not registered again. A prefix that is not in that list, including `crystal`, fails startup. That prefix exists as a validator, but the built-in directory never generates it, so it has no icon and no `item.gregtech.crystal` translation. A generated prefix that `isValidFor` rejects, such as a plate of diamond, also fails startup. Addons cannot add prefixes. Materials are fixed at startup; a datapack reload cannot add or remove them. Old saves are not migrated.

The translation key is `material.gregtech.<lowercasename>`, from `GTMaterial.getTranslationKey()`. Put it in the addon’s own `assets/gregtech/lang/en_us.json` and `zh_cn.json`. Minecraft merges those files with GregTech’s by key. A tooltip on an addon material uses `tooltip.gregtech.material_from_addon` (“Added by the %s addon”). The shared Chinese file keeps the English fallback for that new key, because it is not an original GT6 translation.

A `dust()` material such as ammonium nitrate generates **6** items. Iron’s automatic forms are **96**. A metal can therefore add on the order of a hundred items. Phase C recipe generators walk every material, so a new material receives the furnace, macerator and similar rows its properties already imply. Removing the addon leaves those `gregtech:` items missing under the usual Minecraft rules. Crucible contents store the numeric material id and skip an id that no longer resolves, so a saved addon material becomes the invalid sentinel instead of crashing the load. That crucible path is covered by the existing NBT reader; a world reload after uninstall has not been run.

## Building an addon

Run `./gradlew :core:addonApiJar` (Windows: `gradlew.bat :core:addonApiJar`). The shared compile-only SDK is `core/build/libs/gregtech6-addon-api-0.0.0.jar`. It contains the shared domain/API classes and notices, without Minecraft assets. Use `compileOnly files("libs/gregtech6-addon-api-0.0.0.jar")`; never shade or bundle these classes, since the installed GregTech mod supplies them. For the platform-specific recipe/stack/energy adapters, also use the corresponding GregTech mod sources/development dependency in a Forge or NeoForge workspace. A production reobfuscated mod JAR is not a universal development dependency. No Maven coordinate is published yet.

Declare a required loader dependency on **`gregtech6`** (the internal content namespace remains **`gregtech`**). Build your addon separately for each loader, using that loader's supported Java/Minecraft toolchain. The shared SDK does not make a Forge addon binary load on NeoForge.

## Supported scope

- Shared addon registration, version/context and recipe-ready lifecycle.
- Queued materials and extra forms of existing prefixes, in the 4291–7899 id band, generated as `gregtech:` items.
- Existing `GTMaterialRegistry`, `MaterialPrefix`, `GTValues.U`, `GTVoltageTiers` and energy packet contracts.
- Existing platform `MachineRecipeMaps`, `Recipe`, item/material lookup and `IEnergyBlock` adapters.
- `GtSignalCarrier`, `GtSignalSource`, `GtSignalReceiver`, and the empty `gregtech:signal_wire_receivers` block tag.

API version 2 does **not** promise new prefixes, fluids, machine types, datapack reloads of the material table, old-save migration, cross-loader Minecraft stacks or a complete old GregAPI replacement. Only the documented entries are the compatibility surface; internal classes may change during the 0.0.0 integration. Use [the voltage guide](VOLTAGE_TIERS.md) rather than GT5/GTCE tier labels.

The SDK/project code license is LGPL-3.0-or-later. Preserve applicable source/asset notices; consult `LICENSE`, `COPYING` and `NOTICE` for the actual declarations.
