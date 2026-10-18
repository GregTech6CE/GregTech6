"""FlowingFluid lives in net.minecraft.world.level.material, not in the Forge fluids package."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()


import io
import os

PATH = os.path.join(str(native_path("src")), "main", "java", "com", "gregtech", "gregtech", "loaders", "a", "Loader_Fluids.java")
text = io.open(PATH, encoding="utf-8").read()
text = text.replace("import net.minecraftforge.fluids.FlowingFluid;\n", "")
text = text.replace("import net.minecraft.world.level.block.LiquidBlock;",
                    "import net.minecraft.world.level.material.FlowingFluid;\n"
                    "import net.minecraft.world.level.block.LiquidBlock;", 1)
io.open(PATH, "w", encoding="utf-8", newline="\n").write(text)
print("FlowingFluid import fixed")
