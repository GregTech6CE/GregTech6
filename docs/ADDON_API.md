# Addon API

[English](ADDON_API.md) | [中文](ADDON_API.zh-CN.md)

API version **1** provides one shared entry point on Forge 1.20.1 and NeoForge 1.21.1. Register a `GregTechAddon` in your own mod constructor. GregTech calls `onRecipesReady` once from common setup, after its material/item linking and built-in machine recipes. Callbacks run in ascending addon mod ID order. Duplicate IDs, late registration and a second dispatch fail explicitly; callback failures stop initialization and leave the lifecycle `FAILED`.

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

## Building an addon

Run `./gradlew :core:addonApiJar` (Windows: `gradlew.bat :core:addonApiJar`). The shared compile-only SDK is `core/build/libs/gregtech6-addon-api-0.0.0.jar`. It contains the shared domain/API classes and notices, without Minecraft assets. Use `compileOnly files("libs/gregtech6-addon-api-0.0.0.jar")`; never shade or bundle these classes, since the installed GregTech mod supplies them. For the platform-specific recipe/stack/energy adapters, also use the corresponding GregTech mod sources/development dependency in a Forge or NeoForge workspace. A production reobfuscated mod JAR is not a universal development dependency. No Maven coordinate is published yet.

Declare a required loader dependency on **`gregtech6`** (the internal content namespace remains **`gregtech`**). Build your addon separately for each loader, using that loader's supported Java/Minecraft toolchain. The shared SDK does not make a Forge addon binary load on NeoForge.

## Supported scope

- Shared addon registration, version/context and recipe-ready lifecycle.
- Existing `GTMaterialRegistry`, `MaterialPrefix`, `GTValues.U`, `GTVoltageTiers` and energy packet contracts.
- Existing platform `MachineRecipeMaps`, `Recipe`, item/material lookup and `IEnergyBlock` adapters.
- `GtSignalCarrier`, `GtSignalSource`, `GtSignalReceiver`, and the empty `gregtech:signal_wire_receivers` block tag.

API version 1 does **not** promise automatic registration of new GT material items/fluids, new machine types, cross-loader Minecraft stacks or a complete old GregAPI replacement. Those lifecycles need additional work before they can be advertised as supported. Only this addon entry point and documented contracts are the initial compatibility surface; internal classes may change during the 0.0.0 integration. Use [the voltage guide](VOLTAGE_TIERS.md) rather than GT5/GTCE tier labels.

The SDK/project code license is LGPL-3.0-or-later. Preserve applicable source/asset notices; consult `LICENSE`, `COPYING` and `NOTICE` for the actual declarations.
