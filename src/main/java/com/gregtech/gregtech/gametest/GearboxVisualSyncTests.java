package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.block.energy.GearboxBlock;
import com.gregtech.gregtech.blockentity.energy.GearboxBlockEntity;
import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/** Server state must reach the chunk model after mounting, spinning and stopping. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class GearboxVisualSyncTests {
    @GameTest(template = "test_empty")
    public static void mountedGearsAxleAndRotationReachTheClientModel(GameTestHelper h) {
        GearboxBlock block = (GearboxBlock) ForgeRegistries.BLOCKS.getValue(GregTech.id("gearbox_bronze"));
        BlockPos source = new BlockPos(1, 1, 1);
        BlockPos receiver = new BlockPos(2, 1, 1);
        h.setBlock(source, block);
        h.setBlock(receiver, block);
        GearboxBlockEntity server = (GearboxBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(source));
        GearboxBlockEntity clientReplica = (GearboxBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(receiver));

        server.mountGear(Direction.NORTH);
        server.mountGear(Direction.UP);
        server.toggleAxis(Direction.Axis.Y);
        h.assertTrue(server.doEnergyInjection(GregTechTags.Energy.RU, Direction.NORTH, 32, 1, true) == 1,
                "configured gearbox accepts one packet to animate its mounted gears");
        var update = server.getUpdatePacket().getTag();
        h.assertTrue(update != null && (update.getByte("visualRotationMask") & 64) != 0,
                "packet carries the GT6 running flag");

        clientReplica.handleUpdateTag(update);
        Integer configuration = clientReplica.getModelData().get(GearboxBlockEntity.VISUAL_CONFIGURATION);
        Integer rotation = clientReplica.getModelData().get(GearboxBlockEntity.VISUAL_ROTATION);
        int expectedFaces = (1 << Direction.NORTH.ordinal()) | (1 << Direction.UP.ordinal());
        h.assertTrue(configuration != null && configuration == ((2 << 6) | expectedFaces),
                "client model receives physical gear faces and Y axle");
        h.assertTrue(rotation != null && (rotation & 64) != 0,
                "client model receives active rotation");

        server.toggleJammed();
        clientReplica.handleUpdateTag(server.getUpdateTag());
        h.assertTrue(clientReplica.getModelData().get(GearboxBlockEntity.VISUAL_ROTATION) == 0,
                "stop clears animated gear overlay without removing mounted gears");
        h.assertTrue(configuration.equals(clientReplica.getModelData().get(GearboxBlockEntity.VISUAL_CONFIGURATION)),
                "stop keeps the original mounted face layout");
        h.succeed();
    }
}
