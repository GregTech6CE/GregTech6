package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.blockentity.tool.BumbliaryBlockEntity;
import com.gregtech.gregtech.content.bumble.*;
import com.gregtech.gregtech.registry.GTToolBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class BumbleStingTests {
    private static ItemStack queen(int species) {
        CompoundTag genes = new CompoundTag();
        BumbleBeeGenes.setHumidityMin(genes, 0);
        BumbleBeeGenes.setHumidityMax(genes, 1);
        BumbleBeeGenes.setTemperatureMin(genes, -1000);
        BumbleBeeGenes.setTemperatureMax(genes, 100000);
        BumbleBeeGenes.setInsideActive(genes, true);
        BumbleBeeGenes.setOutsideActive(genes, true);
        BumbleBeeGenes.setAggressiveness(genes, 10000);
        return BumbleBeeType.stack(GTBumbleSpecies.byId(species), BumbleBeeType.QUEEN, genes, 1);
    }

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void speciesStingsRespectDamageAndImmunity(GameTestHelper h) {
        var level = h.getLevel();
        var cow = EntityType.COW.create(level);
        h.assertTrue(BumbleSting.attack(queen(30), cow) && cow.getHealth() == 6,
                "level-three ordinary bee deals four points");
        cow.invulnerableTime = 0;
        cow.setHealth(10);
        h.assertTrue(BumbleSting.attack(queen(900), cow) && cow.getHealth() == 8,
                "desert bee doubles sting damage");
        h.assertTrue(!BumbleSting.attack(queen(800), cow), "mushroom bees never sting");
        h.assertTrue(!BumbleSting.attack(queen(30), EntityType.SKELETON.create(level)), "ordinary bee ignores skeletons");
        h.assertTrue(!BumbleSting.attack(queen(30), EntityType.SNOW_GOLEM.create(level)), "ordinary bee ignores snow golems");
        h.assertTrue(!BumbleSting.attack(queen(20000), EntityType.IRON_GOLEM.create(level)), "insect immunity protects iron golems even from special bees");
        cow.invulnerableTime = 0;
        cow.setHealth(10);
        h.assertTrue(BumbleSting.attack(queen(300), cow) && cow.getHealth() == 8 && cow.isOnFire(),
                "nether bee doubles damage and ignites target");
        h.assertTrue(cow.getLastDamageSource().is(net.minecraft.tags.DamageTypeTags.IS_FIRE)
                        && cow.getLastDamageSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR),
                "nether sting is fire damage and retains the bumble armor bypass");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void workingQueensStingAtOriginalRanges(GameTestHelper h) {
        BlockPos pos = h.absolutePos(new BlockPos(1, 1, 1));
        var cow = h.spawn(EntityType.COW, new BlockPos(4, 1, 1));
        cow.setPos(pos.getX() + 3.5, pos.getY(), pos.getZ() + .5);
        try {
            for (boolean advanced : new boolean[]{true, false}) {
                var block = advanced ? GTToolBlocks.ADVANCED_BUMBLIARY.get() : GTToolBlocks.BUMBLIARY.get();
                var machine = new BumbliaryBlockEntity(pos, block.defaultBlockState());
                machine.setLevel(h.getLevel());
                machine.inventory().setStackInSlot(machine.layout().royal(), queen(30));
                CompoundTag nbt = new CompoundTag();
                nbt.putLong(BumbliaryBlockEntity.NBT_PROGRESS, 151);
                machine.load(nbt);
                machine.tickLogic();
                h.assertTrue(cow.getHealth() == (advanced ? 10 : 6),
                        "attack range advanced=" + advanced + ", expected health=" + (advanced ? 10 : 6) + ", actual=" + cow.getHealth());
                nbt.putLong(BumbliaryBlockEntity.NBT_PROGRESS, 0);
                machine.load(nbt);
                cow.invulnerableTime = 0;
                h.assertTrue(!machine.attackEntity(cow), "expired queen cannot attack when opened");
            }
        } finally {
            cow.discard();
        }
        h.succeed();
    }
}
