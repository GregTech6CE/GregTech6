"""Fix the DidYouKnow hint rows: use only specs the port resolves.

The first version named the GT6 machines through `tech:<id>` specs, but the port registers those
blocks with ids of its own (several with per-tier names), so all five rows were skipped. The rows now
show the materials/items/fluids the port definitely has and name the machines in the GT6 labels.
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()


import io
import os

PATH = os.path.join(str(native_path("src")), "main", "java", "com", "gregtech", "gregtech", "content", "recipe", "GTMainRecipes.java")
text = io.open(PATH, encoding="utf-8").read()

start = text.index("            // ---- GT6's \"Did you know...?\" hint pages")
end = text.index("            new Row(\"ScannerVisuals\"", start)
rows = '''            // ---- GT6's "Did you know...?" hint pages (Loader_Recipes_Hints) ----
            // The labels name the GT6 machines; the shown stacks are the materials, books and
            // fluids the port registers (several GT6 machine blocks have different ids here).
            new Row("DidYouKnow", true, 0, 0,
                    "i:dust:Cinnabar:3|Throw three Units of Cinnabar into the Crucible;"
                            + "tech:clay_crucible:1|Wait until it melts into Mercury;"
                            + "v:glass_bottle:1|Rightclick the Crucible with an Empty Bottle;"
                            + "v:redstone_ore:1|Mine Vanilla Redstone Ore with a Club for Cinnabar",
                    "tech:mercury_bottle:1|A Bottle of Mercury", "", "", "Loader_Recipes_Hints:38"),
            new Row("DidYouKnow", true, 0, 0,
                    "i:ingot:Fe:1|Throw some Iron into the Crucible (leave room for Air);"
                            + "tech:clay_crucible:1|Blow Air in with a running Engine;"
                            + "i:ingot:WroughtIron:1|Wrought Iron works too",
                    "i:ingot:Steel:1|Wait until it all turns into Steel;i:dust:Steel:1;i:plate:Steel:1;"
                            + "i:stick:Steel:1;i:gearGt:Steel:1",
                    "", "", "Loader_Recipes_Hints:44"),
            new Row("DidYouKnow", true, 0, 0,
                    "i:ingot:Zn:1|Dump some Zinc into the Crucible;"
                            + "tech:clay_faucet:1|Pour the Zinc with a Faucet on the Crucible;"
                            + "i:plate:Steel:1|Put your Steel object into the Bathing Pot below it",
                    "i:plate:SteelGalvanized:1|Galvanized Steel Plate;"
                            + "i:stick:SteelGalvanized:1;i:screw:SteelGalvanized:1",
                    "f:Zn:144", "f:Zn:144", "Loader_Recipes_Hints:50"),
            new Row("DidYouKnow", true, 0, 0,
                    "v:book:1|Insert a basic empty Book into the Printer to get a Manual",
                    "book:Manual_Printer", "f:Dye_Chemical_Black:1000", "", "Loader_Recipes_Hints:56"),
'''
text = text[:start] + rows + text[end:]
io.open(PATH, "w", encoding="utf-8", newline="\n").write(text)
print("rewrote the hint rows to resolvable specs")
