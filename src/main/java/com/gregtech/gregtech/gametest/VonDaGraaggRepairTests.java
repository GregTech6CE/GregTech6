package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.multiblock.MultiblockLayout;
import com.gregtech.gregtech.blockentity.machine.VonDaGraaggControllerBlockEntity;
import com.gregtech.gregtech.content.multiblock.LargeMachineParts;
import com.gregtech.gregtech.content.multiblock.ControllerStructureLayouts;
import com.gregtech.gregtech.content.multiblock.VonDaGraaggSpawnInhibitor;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.block.stone.StoneType;
import com.gregtech.gregtech.block.stone.StoneVariant;
import com.gregtech.gregtech.registry.GTBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Original GT6 17996 geometry, EU budget and mossy-cobblestone spawn exception. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class VonDaGraaggRepairTests {
    private VonDaGraaggRepairTests() {}

    private static void placeStructure(GameTestHelper helper, BlockPos center) {
        var world = helper.getLevel();
        world.setBlock(center, LargeMachineParts.block(17996).defaultBlockState(), 3);
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            if (Math.abs(x * z) >= 4) continue;
            for (int y = 0; y <= 1; y++) {
                if (x == 0 && y == 0 && z == 0) continue;
                world.setBlock(center.offset(x, y, z), LargeMachineParts.block(18028).defaultBlockState(), 3);
            }
        }
        for (int y = 2; y <= 6; y++)
            world.setBlock(center.above(y), LargeMachineParts.block(18040).defaultBlockState(), 3);
        world.setBlock(center.above(7), LargeMachineParts.block(18029).defaultBlockState(), 3);
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            if (x == 0 && z == 0) continue;
            world.setBlock(center.offset(x, 6, z), LargeMachineParts.block(18029).defaultBlockState(), 3);
            if (x * z == 0) {
                world.setBlock(center.offset(x, 5, z), LargeMachineParts.block(18029).defaultBlockState(), 3);
                world.setBlock(center.offset(x, 7, z), LargeMachineParts.block(18029).defaultBlockState(), 3);
            }
        }
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void formedGeneratorConsumes4096EuAndBlocksSpawnsExceptAtMossyStone(GameTestHelper helper) {
        var world = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(7, 1, 7));
        placeStructure(helper, center);
        helper.assertTrue(world.getBlockEntity(center) instanceof VonDaGraaggControllerBlockEntity,
                "GT6 17996 must create a working controller entity, not a passive port");
        var machine = (VonDaGraaggControllerBlockEntity) world.getBlockEntity(center);
        helper.assertTrue(machine.isStructureOk(), "the 41-wall base, five-coil pole and steel crown must form");
        var preview = ControllerStructureLayouts.cells(LargeMachineParts.block(17996));
        helper.assertTrue(preview != null && preview.size() == 63
                        && preview.values().stream().filter(part -> part == LargeMachineParts.block(18028)).count() == 41
                        && preview.values().stream().filter(part -> part == LargeMachineParts.block(18040)).count() == 5
                        && preview.values().stream().filter(part -> part == LargeMachineParts.block(18029)).count() == 17,
                "JEI's three-component preview uses the complete original 63-part geometry");
        var drops = world.getBlockState(center).getDrops(new net.minecraft.world.level.storage.loot.LootParams.Builder(world));
        helper.assertTrue(drops.size() == 1 && drops.get(0).is(LargeMachineParts.block(17996).asItem()),
                "breaking the controller returns its original GT6 block item");
        helper.assertTrue(machine.injectPortEnergy(MultiblockLayout.Role.ENERGY_INPUT,
                        GregTechTags.Energy.ELECTRICITY, 2048, 2, true) == 2,
                "a galvanized base wall accepts two 2048 EU packets");
        VonDaGraaggControllerBlockEntity.serverTick(world, center, world.getBlockState(center), machine);
        helper.assertTrue(machine.currentRange() == 255 && machine.storedEnergy() == 0,
                "GT6 clamps 4096/16 to the byte maximum of 255 blocks of range, then spends 4096 EU each tick");

        BlockPos spawn = center.offset(2, 0, 0);
        var zombie = EntityType.ZOMBIE.create(world);
        helper.assertTrue(zombie != null, "a vanilla mob is available to verify the Forge spawn event");
        zombie.setPos(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
        var denied = new MobSpawnEvent.PositionCheck(zombie, world, MobSpawnType.NATURAL, null);
        VonDaGraaggSpawnInhibitor.onSpawnPositionCheck(denied);
        helper.assertTrue(denied.getResult() == Event.Result.DENY,
                "the charged generator prevents a natural mob spawn inside its square range");

        world.setBlock(spawn.below(), Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 3);
        var allowed = new MobSpawnEvent.PositionCheck(zombie, world, MobSpawnType.NATURAL, null);
        VonDaGraaggSpawnInhibitor.onSpawnPositionCheck(allowed);
        helper.assertTrue(allowed.getResult() == Event.Result.DEFAULT,
                "mossy cobblestone within five vertical blocks exempts the spawn");
        world.setBlock(spawn.below(), GTBlocks.getStone(StoneType.GRANITE_BLACK,
                StoneVariant.COBBLE_MOSSY).defaultBlockState(), 3);
        var gtStoneAllowed = new MobSpawnEvent.PositionCheck(zombie, world, MobSpawnType.NATURAL, null);
        VonDaGraaggSpawnInhibitor.onSpawnPositionCheck(gtStoneAllowed);
        helper.assertTrue(gtStoneAllowed.getResult() == Event.Result.DEFAULT,
                "GT6 mossy cobblestone variants also exempt mob spawns");
        world.setBlock(center.offset(2, 0, 0), Blocks.AIR.defaultBlockState(), 3);
        VonDaGraaggControllerBlockEntity.serverTick(world, center, world.getBlockState(center), machine);
        helper.assertTrue(machine.currentRange() == 0 && !machine.isStructureOk(),
                "a missing galvanized wall disables the machine and its inhibition range");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void multipleSameTickPacketsSustainRangeAcrossTicks(GameTestHelper helper) {
        var world = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(7, 1, 7));
        placeStructure(helper, center);
        var machine = (VonDaGraaggControllerBlockEntity) world.getBlockEntity(center);
        helper.assertTrue(machine.isStructureOk(), "the input wall is connected");
        helper.assertTrue(machine.injectPortEnergy(MultiblockLayout.Role.ENERGY_INPUT,
                        GregTechTags.Energy.ELECTRICITY, 2048, 4, true) == 4
                        && machine.storedEnergy() == 8192,
                "GT6 accumulates multiple same-tick packets above its advertised 4096 EU capacitor");
        VonDaGraaggControllerBlockEntity.serverTick(world, center, world.getBlockState(center), machine);
        helper.assertTrue(machine.currentRange() == 255 && machine.storedEnergy() == 4096,
                "first tick spends only 4096 EU");
        VonDaGraaggControllerBlockEntity.serverTick(world, center, world.getBlockState(center), machine);
        helper.assertTrue(machine.currentRange() == 255 && machine.storedEnergy() == 0,
                "the second tick consumes the carried-over packet energy");
        helper.succeed();
    }
}
