"""Bounded real-Java noise regression plus source guards for the native one-cell adapter."""
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
PACKAGE = Path("multiversion/src/bootstrapGameTest/java/com/gregtech/gregtech/gametest")


class StoneLayerSlabFixtureTests(unittest.TestCase):
    def test_actual_java_noise_and_pinned_fixture_contracts(self):
        def java_tool(name):
            if os.environ.get("JAVA_HOME"):
                path = Path(os.environ["JAVA_HOME"]) / "bin" / (name + (".exe" if os.name == "nt" else ""))
                if path.is_file():
                    return str(path)
            return shutil.which(name)

        javac, java = java_tool("javac"), java_tool("java")
        self.assertTrue(javac and java,
                        "JDK 17+ is required; source guards alone cannot satisfy the real-Java regression")
        sources = [
            ROOT / "core/src/main/java/com/gregtech/gregtech/worldgen/GTCellNoise.java",
            ROOT / "core/src/main/java/com/gregtech/gregtech/worldgen/GTStoneLayersGen.java",
            ROOT / PACKAGE / "StoneLayerSlabFixture.java",
            ROOT / "tools/integration/StoneLayerSlabFixtureContracts.java",
        ]
        with tempfile.TemporaryDirectory(prefix="gt-slab-noise-") as classes:
            compile_result = subprocess.run([javac, "--release", "17", "-d", classes, *map(str, sources)],
                                            capture_output=True, text=True, timeout=30)
            self.assertEqual(0, compile_result.returncode, compile_result.stdout + compile_result.stderr)
            result = subprocess.run([java, "-cp", classes,
                                     "com.gregtech.gregtech.gametest.StoneLayerSlabFixtureContracts"],
                                    capture_output=True, text=True, timeout=30)
            self.assertEqual(0, result.returncode, result.stdout + result.stderr)
            self.assertIn("checks passed", result.stdout)

    def test_native_fixture_stays_fail_closed_and_one_cell(self):
        source = (ROOT / PACKAGE / "StoneLayerSlabAuditTests.java").read_text()
        for forbidden in ("seedFor(", "100000", "method.invoke(level", "InvocationTargetException"):
            self.assertNotIn(forbidden, source)
        for guard in (
            "BlockPos p = h.absolutePos(new BlockPos(1, 1, 1))",
            ".isInside(p)", "level.hasChunkAt(p)", "level.getBlockEntity(p) == null",
            "read.equals(sample) ? level.getBlockState(p) : Blocks.AIR.defaultBlockState()",
            "h.assertTrue(write.equals(sample)", "return level.setBlock(p, (BlockState) args[1]",
            'throw new AssertionError("unsupported one-cell fixture callback:',
            "finally { level.setBlock(p, pristine, 18); }",
            "new GTStoneLayerFeature().place(", "RandomSource.create(42), sample, NoneFeatureConfiguration.INSTANCE)",
        ):
            self.assertIn(guard, source)

    def test_native_semantic_assertions_are_not_replaced_by_noise_only_checks(self):
        source = (ROOT / PACKAGE / "StoneLayerSlabAuditTests.java").read_text()
        for guard in (
            "!def.noDeep() && def.stoneType() != null",
            "!negative.noDeep() && negative.stoneType() == null",
            "generate(h, p, sample, seed, control).is(full)",
            "half == SlabType.DOUBLE && wet", "actual.equals(full.defaultBlockState())",
            "half == SlabType.TOP ? Direction.UP : Direction.DOWN",
            "GTStoneSlabBlock.FACING, facing", "Direction.values()",
            "existing GT slab orientation/material survives", "unrelated structures/stairs excluded",
            "native full-block control has no cobble target",
            "layer without cobble target preserves every half/water state",
        ):
            self.assertIn(guard, source)
        self.assertEqual(2, source.count("for (SlabType half : SlabType.values())"))


if __name__ == "__main__":
    unittest.main()
