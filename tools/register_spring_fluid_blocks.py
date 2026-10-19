"""Give the six GT6 spring fluids a world block (Forge `LiquidBlock`), plus their blockstates, models
and language keys.

The port created `ForgeFlowingFluid` sources/flowing fluids for every GT6 fluid but never attached a
`LiquidBlock`, so no GT6 fluid could exist in the world — which is why GT6's fluid springs (oil, gas,
geothermal water, lava) were never ported. Both sides are wired with lazy references, so the block and
the fluid can point at each other.
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import io
import json
import os

LOADER = os.path.join(str(native_path("src")), "main", "java", "com", "gregtech", "gregtech", "bootstrap", "content", "Loader_Fluids.java")
ASSETS = os.path.join(str(native_path("src")), "main", "resources", "assets", "gt6ce")
LANG = os.path.join(ASSETS, "lang")

# path (sanitized registry name) -> (texture, english, chinese)
FLUIDS = {
    "liquid_extra_heavy_oil": ("liquid_extra_heavy_oil", "Extra Heavy Oil", "特重油"),
    "liquid_heavy_oil": ("liquid_heavy_oil", "Heavy Oil", "重油"),
    "liquid_medium_oil": ("liquid_medium_oil", "Medium Oil", "中油"),
    "liquid_light_oil": ("liquid_light_oil", "Light Oil", "轻油"),
    "gas_natural_gas": ("gas_natural_gas", "Natural Gas", "天然气"),
    "watergeothermal": ("watergeothermal", "Geothermal Water", "地热水"),
}

written = []


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as handle:
        json.dump(obj, handle, indent=2)
        handle.write("\n")
    written.append(os.path.relpath(path, ASSETS).replace("\\", "/"))


text = io.open(LOADER, encoding="utf-8").read()

if "SPRING_FLUID_PATHS" not in text:
    text = text.replace(
        "import net.minecraftforge.fluids.ForgeFlowingFluid;",
        "import net.minecraftforge.fluids.FlowingFluid;\n"
        "import net.minecraftforge.fluids.ForgeFlowingFluid;\n"
        "import net.minecraft.world.level.block.LiquidBlock;\n"
        "import net.minecraft.world.level.block.SoundType;\n"
        "import net.minecraft.world.level.block.state.BlockBehaviour;\n"
        "import net.minecraft.world.level.material.MapColor;\n"
        "import net.minecraft.world.level.material.PushReaction;\n"
        "import java.util.Set;\n"
        "import java.util.concurrent.atomic.AtomicReference;", 1)

    text = text.replace(
        "public record Loader_Fluids(IEventBus bus) implements IGTLoader {",
        '''public record Loader_Fluids(IEventBus bus) implements IGTLoader {
    /**
     * Fluids that also exist as world blocks — GT6's fluid springs ({@code WorldgenFluidSpring},
     * Loader_Worldgen:782-797) place oil, natural gas, geothermal water and lava into the world, so
     * those six fluids need a {@link LiquidBlock}.
     */
    private static final Set<String> SPRING_FLUID_PATHS = Set.of(
            "liquid_extra_heavy_oil", "liquid_heavy_oil", "liquid_medium_oil", "liquid_light_oil",
            "gas_natural_gas", "watergeothermal");

    /** Both the block and the fluid reference each other, so both sides stay lazy. */
    private static ForgeFlowingFluid.Properties properties(RegistryObject<FluidType> type, String field,
                                                          AtomicReference<RegistryObject<LiquidBlock>> blockRef) {
        ForgeFlowingFluid.Properties properties = new ForgeFlowingFluid.Properties(type,
                () -> GTFluids.still(field).get(), () -> GTFluids.flowing(field).get());
        if (blockRef.get() != null) properties.block(() -> blockRef.get().get());
        return properties;
    }
''', 1)

    old = """            // Still fluid
            RegistryObject<Fluid> stillRO = GTFluids.FLUIDS.register(path, () ->
                    new ForgeFlowingFluid.Source(new ForgeFlowingFluid.Properties(
                            typeRO,
                            () -> GTFluids.still(field).get(),
                            () -> GTFluids.flowing(field).get())));

            // Flowing fluid
            RegistryObject<Fluid> flowingRO = GTFluids.FLUIDS.register(path + "_flowing", () ->
                    new ForgeFlowingFluid.Flowing(new ForgeFlowingFluid.Properties(
                            typeRO,
                            () -> GTFluids.still(field).get(),
                            () -> GTFluids.flowing(field).get())));"""
    new = """            // World block for the spring fluids (created first: the fluid references it).
            final AtomicReference<RegistryObject<Fluid>> stillRef = new AtomicReference<>();
            final AtomicReference<RegistryObject<LiquidBlock>> blockRef = new AtomicReference<>();
            if (SPRING_FLUID_PATHS.contains(path)) {
                blockRef.set(GTBlocks.BLOCKS.register(path, () -> new LiquidBlock(
                        () -> (FlowingFluid) stillRef.get().get(),
                        BlockBehaviour.Properties.of().mapColor(MapColor.WATER).replaceable()
                                .noCollission().strength(100.0F).pushReaction(PushReaction.DESTROY)
                                .noLootTable().liquid().sound(SoundType.EMPTY))));
            }

            // Still fluid
            RegistryObject<Fluid> stillRO = GTFluids.FLUIDS.register(path, () ->
                    new ForgeFlowingFluid.Source(properties(typeRO, field, blockRef)));
            stillRef.set(stillRO);

            // Flowing fluid
            RegistryObject<Fluid> flowingRO = GTFluids.FLUIDS.register(path + "_flowing", () ->
                    new ForgeFlowingFluid.Flowing(properties(typeRO, field, blockRef)));"""
    assert text.count(old) == 1, "fluid registration anchor"
    text = text.replace(old, new)

    if "import com.gregtech.gregtech.registry.GTBlocks;" not in text:
        text = text.replace("import com.gregtech.gregtech.registry.GTFluidItems;",
                            "import com.gregtech.gregtech.registry.GTBlocks;\n"
                            "import com.gregtech.gregtech.registry.GTFluidItems;", 1)

    io.open(LOADER, "w", encoding="utf-8", newline="\n").write(text)
    print("Loader_Fluids: spring fluids now get a LiquidBlock")
else:
    print("Loader_Fluids: already wired")

# Blockstate + models: a LiquidBlock uses vanilla water's 16 level variants.
for path, (texture, _, _) in FLUIDS.items():
    still = "gt6ce:block/fluids/" + texture
    variants = {}
    for level in range(16):
        variants["level=%d" % level] = {
            "model": "gt6ce:block/fluid/%s%s" % (path, "" if level == 0 else "_flow")}
    write_json(os.path.join(ASSETS, "blockstates", path + ".json"), {"variants": variants})
    for suffix in ("", "_flow"):
        write_json(os.path.join(ASSETS, "models", "block", "fluid", path + suffix + ".json"), {
            "parent": "minecraft:block/block",
            "textures": {"particle": still, "still": still, "flow": still},
            "elements": [
                {"from": [0, 0, 0], "to": [16, 16, 16], "faces": {
                    "down": {"texture": "#still", "cullface": "down"},
                    "up": {"texture": "#still", "cullface": "up"},
                    "north": {"texture": "#flow", "cullface": "north"},
                    "south": {"texture": "#flow", "cullface": "south"},
                    "west": {"texture": "#flow", "cullface": "west"},
                    "east": {"texture": "#flow", "cullface": "east"}}},
            ]})

for name, index in (("en_us.json", 1), ("zh_cn.json", 2)):
    file = os.path.join(LANG, name)
    lines = io.open(file, encoding="utf-8").readlines()
    anchor = next((i for i, line in enumerate(lines) if '"block.gt6ce.loot_crate"' in line), None)
    if anchor is None:
        raise SystemExit("%s: no anchor" % name)
    indent = lines[anchor][:len(lines[anchor]) - len(lines[anchor].lstrip())]
    added = 0
    inserts = []
    for path, values in FLUIDS.items():
        key = '"block.gt6ce.%s"' % path
        if any(key in line for line in lines):
            continue
        inserts.append('%s%s: "%s",\n' % (indent, key, values[index]))
        added += 1
    lines[anchor:anchor] = inserts
    if added:
        io.open(file, "w", encoding="utf-8", newline="\n").writelines(lines)
    print("%s: %d fluid block keys" % (name, added))

print("wrote %d asset files" % len(written))
