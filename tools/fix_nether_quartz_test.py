"""Fix the quartz test: the GameTestHolder import and a direct call to the (now public) seam pass."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()


import io
import os

FEATURE = os.path.join(str(native_path("src")), "main", "java", "com", "gregtech", "gregtech", "worldgen",
                       "GTNetherDepositFeature.java")
TEST = os.path.join(str(native_path("src")), "main", "java", "com", "gregtech", "gregtech", "gametest",
                    "NetherQuartzTests.java")

text = io.open(FEATURE, encoding="utf-8").read()
old = "    private boolean placeSeams(WorldGenLevel level, GTCellNoise noise, BlockPos origin) {"
new = "    public boolean placeSeams(WorldGenLevel level, GTCellNoise noise, BlockPos origin) {"
assert text.count(old) == 1, "seam pass anchor"
io.open(FEATURE, "w", encoding="utf-8", newline="\n").write(text.replace(old, new))

test = io.open(TEST, encoding="utf-8").read()
test = test.replace("import net.minecraft.gametest.GameTestHolder;\n", "")
test = test.replace("import net.minecraftforge.gametest.PrefixGameTestTemplate;",
                    "import net.minecraftforge.gametest.GameTestHolder;\n"
                    "import net.minecraftforge.gametest.PrefixGameTestTemplate;")

block = """        var feature = new GTNetherDepositFeature();
        // The seam pass is private; the whole feature runs it, so run the feature on this chunk.
        var context = new net.minecraft.world.level.levelgen.feature.FeaturePlaceContext<>(
                new net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration(),
                gen, level.getChunkSource().getGenerator(), level.random, new BlockPos(minX, 64, minZ));
        feature.place(context);"""
replacement = """        // The pass is public so the test can drive GT6's formula on a prepared chunk.
        var seam = new GTNetherDepositFeature();
        seam.placeSeams(gen, noise, new BlockPos(minX, 64, minZ));"""
assert test.count(block) == 1, "seam call anchor"
test = test.replace(block, replacement)
io.open(TEST, "w", encoding="utf-8", newline="\n").write(test)
print("quartz test wired to the public seam pass")
