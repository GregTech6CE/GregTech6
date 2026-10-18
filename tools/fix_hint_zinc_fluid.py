"""The galvanized steel hint's molten zinc: the port resolves it through the material fluid spec."""

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
assert text.count('"f:Zn:144", "f:Zn:144", "Loader_Recipes_Hints:50"') == 1
text = text.replace('"f:Zn:144", "f:Zn:144", "Loader_Recipes_Hints:50"',
                    '"m:Zn:144", "m:Zn:144", "Loader_Recipes_Hints:50"')
io.open(PATH, "w", encoding="utf-8", newline="\n").write(text)
print("molten zinc now uses the material fluid spec")
