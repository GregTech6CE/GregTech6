package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.registry.GTBlockEntities;
import com.gregtech.gregtech.registry.GTGearboxes;
import com.gregtech.gregtech.GregTech;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/** Ensures every GT6 gearbox family member can persist its block entity. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class GearboxRegistrationRepairTests {
    @GameTest(template = "test_empty")
    public static void allGearboxVariantsHaveMatchingBlockEntityTypes(GameTestHelper h) {
        h.assertTrue(GTGearboxes.allGearboxes().size() == 13,
                "Original GT6 has 13 custom gearboxes");
        h.assertTrue(GTGearboxes.allTransformers().size() == 13,
                "Original GT6 has 13 rotation transformers");

        for (var entry : GTGearboxes.allGearboxes()) {
            h.assertTrue(GTBlockEntities.GEARBOX.get().isValid(entry.get().defaultBlockState()),
                    "Missing gearbox block entity registration for " + entry.getId());
        }
        for (var entry : GTGearboxes.allTransformers()) {
            h.assertTrue(GTBlockEntities.ENERGY_NODE.get().isValid(entry.get().defaultBlockState()),
                    "Missing rotation transformer block entity registration for " + entry.getId());
        }
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void treatedWoodGearboxesRetainOriginalFlammability(GameTestHelper h) {
        BlockPos pos = h.absolutePos(new BlockPos(1, 1, 1));
        for (String id : new String[]{"gearbox_wood", "rotation_transformer_wood"}) {
            var block = ForgeRegistries.BLOCKS.getValue(GregTech.id(id));
            h.assertTrue(block != null, "Missing wooden gearbox " + id);
            var state = block.defaultBlockState();
            h.assertTrue(block.getFlammability(state, h.getLevel(), pos, Direction.NORTH) == 150
                            && block.getFireSpreadSpeed(state, h.getLevel(), pos, Direction.NORTH) == 150,
                    "GT6 NBT_FLAMMABILITY=150 for " + id);
        }
        h.succeed();
    }
}
