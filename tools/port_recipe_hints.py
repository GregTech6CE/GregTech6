"""Port GT6's "Did you know...?" recipe viewer page (RM.DidYouKnow).

GT6 puts its hint pages into a display-only recipe map that NEI shows on its own tab
(`Loader_Recipes_Hints.addFakeRecipe`). The port already has the machinery (a display-only map plus
the spec based row table of GTMainRecipes), so this tool wires the new map up and appends the hint
rows, including the per-ingredient labels GT6 attaches with `ST.make(stack, "name")`.
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()


import io
import os

MAPS = os.path.join(str(native_path("src")), "main", "java", "com", "gregtech", "gregtech", "data", "MachineRecipeMaps.java")
MAIN = os.path.join(str(native_path("src")), "main", "java", "com", "gregtech", "gregtech", "content", "recipe", "GTMainRecipes.java")

# 1. The map itself, right after the crucible smelting map.
maps = io.open(MAPS, encoding="utf-8").read()
if "gt.recipe.didyouknow" in maps:
    print("MachineRecipeMaps: DidYouKnow already declared")
else:
    lines = maps.split("\n")
    anchor = next(i for i, line in enumerate(lines) if "gt.recipe.cruciblesmelting" in line)
    declaration = (
        "    // GT6's hint pages (RM.DidYouKnow): a display-only map the recipe viewer shows on its own\n"
        "    // tab. GT6 shares the internal key \"gt.recipe.other\" with its generic Other map, which the\n"
        "    // port does not have, so the page gets its own key.\n"
        "    , DidYouKnow               = new RecipeMap(null, \"gt.recipe.didyouknow\"            , \"Did you know...?\"        , null                       , 6, 6,0, 3, 3,0, 1, false,true ,false,false)")
    lines[anchor + 1:anchor + 1] = [declaration]
    io.open(MAPS, "w", encoding="utf-8", newline="\n").write("\n".join(lines))
    print("MachineRecipeMaps: declared DidYouKnow")

# 2. Map lookup + labelled ingredients + the hint rows in GTMainRecipes.
main = io.open(MAIN, encoding="utf-8").read()

if 'case "DidYouKnow"' not in main:
    old = '            case "ScannerVisuals" -> MachineRecipeMaps.ScannerVisuals;'
    new = ('            case "ScannerVisuals" -> MachineRecipeMaps.ScannerVisuals;\n'
           '            case "DidYouKnow" -> MachineRecipeMaps.DidYouKnow;')
    assert main.count(old) == 1, "map() anchor"
    main = main.replace(old, new)
    print("GTMainRecipes: map() knows DidYouKnow")

# Labels: GT6 writes them with ST.make(stack, "text"); the port reads them from the spec's | suffix.
if "stripLabel" not in main:
    old = """    private static ItemStack[] items(String specs) {
        if (specs == null || specs.isEmpty()) return new ItemStack[0];
        List<ItemStack> out = new ArrayList<>();
        for (String spec : specs.split(";")) {
            ItemStack stack = GeneratedRecipeResolver.resolveSpec(spec);
            if (stack == null || stack.isEmpty()) return null;
            out.add(stack);
        }
        return out.toArray(ItemStack[]::new);
    }"""
    new = """    private static ItemStack[] items(String specs) {
        if (specs == null || specs.isEmpty()) return new ItemStack[0];
        List<ItemStack> out = new ArrayList<>();
        for (String spec : specs.split(";")) {
            ItemStack stack = GeneratedRecipeResolver.resolveSpec(stripLabel(spec));
            if (stack == null || stack.isEmpty()) return null;
            String label = labelOf(spec);
            if (label != null) {
                // GT6 labels hint ingredients with ST.make(stack, "text") for the recipe viewer.
                stack.setHoverName(net.minecraft.network.chat.Component.literal(label));
            }
            out.add(stack);
        }
        return out.toArray(ItemStack[]::new);
    }

    /** A spec may carry a display label after a {@code |} (the port's form of GT6's named stacks). */
    private static String stripLabel(String spec) {
        int pipe = spec.indexOf('|');
        return pipe < 0 ? spec : spec.substring(0, pipe);
    }

    private static String labelOf(String spec) {
        int pipe = spec.indexOf('|');
        return pipe < 0 ? null : spec.substring(pipe + 1);
    }"""
    assert main.count(old) == 1, "items() anchor"
    main = main.replace(old, new)
    for old_call, new_call in (('GeneratedRecipeResolver.resolveSpec(spec) == null', 'GeneratedRecipeResolver.resolveSpec(stripLabel(spec)) == null'),
                               ('GeneratedRecipeResolver.resolveFluidSpec(spec) == null', 'GeneratedRecipeResolver.resolveFluidSpec(stripLabel(spec)) == null')):
        main = main.replace(old_call, new_call)
    print("GTMainRecipes: ingredient labels supported")

if '"DidYouKnow"' not in main.split("private static final List<Row> ROWS")[1].split(");")[0]:
    rows = """
            // ---- GT6's "Did you know...?" hint pages (Loader_Recipes_Hints) ----
            new Row("DidYouKnow", true, 0, 0,
                    "i:dust:Cinnabar:3|Throw three Units of Cinnabar into the Crucible;"
                            + "tech:smelting_crucible_ceramic:1|Wait until it melts into Mercury;"
                            + "v:glass_bottle:1|Rightclick the Crucible with an Empty Bottle;"
                            + "tech:solid_burning_box:1|Heat the Crucible with a Burning Box",
                    "tech:mercury_bottle:1|A Bottle of Mercury", "", "", "Loader_Recipes_Hints:38"),
            new Row("DidYouKnow", true, 0, 0,
                    "i:ingot:Fe:1|Throw some Iron into the Crucible (leave room for Air);"
                            + "tech:combustion_engine:1|Point a running Engine into it to blow Air;"
                            + "tech:solid_burning_box:1|Heat the Crucible with a Burning Box",
                    "i:ingot:Steel:1|Wait until it all turns into Steel;i:dust:Steel:1;i:plate:Steel:1;"
                            + "i:stick:Steel:1;i:gearGt:Steel:1",
                    "", "", "Loader_Recipes_Hints:44"),
            new Row("DidYouKnow", true, 0, 0,
                    "i:ingot:Zn:1|Dump some Zinc into the Crucible;"
                            + "tech:crucible_faucet:1|Pour the Zinc using a Faucet on the Crucible;"
                            + "tech:bathing_pot:1|Place the Bathing Pot below the Faucet",
                    "i:plate:SteelGalvanized:1|Put your Steel object into the Bathing Pot;"
                            + "i:stick:SteelGalvanized:1;i:screw:SteelGalvanized:1",
                    "f:Zn:144", "f:Zn:144", "Loader_Recipes_Hints:50"),
            new Row("DidYouKnow", true, 0, 0,
                    "tech:printer:1|Get a cheap Printer and power it;"
                            + "v:book:1|Insert a basic empty Book to get a Manual",
                    "book:Manual_Printer", "f:Dye_Chemical_Black:1000", "", "Loader_Recipes_Hints:56"),
            new Row("DidYouKnow", true, 0, 0,
                    "tech:magnifying_glass:1|Scan blocks with the Magnifying Glass",
                    "book:Manual_Steam|Every GT6 machine answers to it", "", "", "Loader_Recipes_Hints:35"),
"""
    old = """            new Row("Boxinator", false, 16, 16, "v:paper:8;v:compass:1", "v:map:1", "", "",
                    "GT6_Main:351"),
"""
    assert main.count(old) == 1, "rows anchor"
    main = main.replace(old, old + rows)
    print("GTMainRecipes: appended 5 hint rows")
else:
    print("GTMainRecipes: hint rows already present")

io.open(MAIN, "w", encoding="utf-8", newline="\n").write(main)
print("wrote %s and %s" % (MAPS, MAIN))
