package gregtech.gametest;

import gregtech.blockentity.tool.BumbliaryBlockEntity;
import gregtech.content.bumble.BumbleBeeGenes;
import gregtech.content.bumble.BumbleBeeType;
import gregtech.content.bumble.GTBumbleSpecies;
import gregtech.registry.GTBumbleBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Native machine regressions for ALIGN-164/165/171; seeded NBT is fixture input, not a model. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class BumbleLifecycleIssueTests {
    private static CompoundTag genes() {
        CompoundTag genes = new CompoundTag();
        BumbleBeeGenes.setTemperatureMin(genes, 0);
        BumbleBeeGenes.setTemperatureMax(genes, 10000);
        BumbleBeeGenes.setHumidityMin(genes, 0);
        BumbleBeeGenes.setHumidityMax(genes, 1);
        BumbleBeeGenes.setOutsideActive(genes, true);
        BumbleBeeGenes.setInsideActive(genes, true);
        BumbleBeeGenes.setLifeSpan(genes, 1200);
        BumbleBeeGenes.setOffspring(genes, 2);
        BumbleBeeGenes.setAggressiveness(genes, 0);
        // Disable work, not environment survival: tests must not depend on product rolls/weather.
        BumbleBeeGenes.setDayActive(genes, false);
        BumbleBeeGenes.setNightActive(genes, false);
        return genes;
    }

    private static ItemStack bee(GameTestHelper h, BumbleBeeType type, CompoundTag genes, int count) {
        ItemStack bee = BumbleBeeType.stack(GTBumbleSpecies.byId(30), type, genes, count);
        h.assertTrue(!bee.isEmpty(), "registered normal bee fixture");
        return bee;
    }

    private static BumbliaryBlockEntity place(GameTestHelper h, boolean advanced) {
        BlockPos pos = h.absolutePos(new BlockPos(2, 2, 2));
        h.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos, (advanced ? GTBumbleBlocks.ADVANCED_BUMBLIARY
                : GTBumbleBlocks.BUMBLIARY).get().defaultBlockState());
        h.assertTrue(h.getLevel().getBlockEntity(pos) instanceof BumbliaryBlockEntity,
                "registered apiary creates native block entity");
        return (BumbliaryBlockEntity) h.getLevel().getBlockEntity(pos);
    }

    private static CompoundTag save(GameTestHelper h, BumbliaryBlockEntity bee) {
//? if >=1.21.1 {
        /*return bee.saveWithoutMetadata(h.getLevel().registryAccess());
*///?} else {
        return bee.saveWithoutMetadata();
//?}
    }

    private static void load(GameTestHelper h, BumbliaryBlockEntity bee, CompoundTag tag) {
//? if >=1.21.1 {
        /*bee.loadWithComponents(tag, h.getLevel().registryAccess());
*///?} else {
        bee.load(tag);
//?}
    }

    private static void seed(GameTestHelper h, BumbliaryBlockEntity bee, long life, long countdown,
                             ItemStack... brood) {
        CompoundTag tag = save(h, bee);
        tag.putLong(BumbliaryBlockEntity.NBT_PROGRESS, life);
        tag.putLong(BumbliaryBlockEntity.NBT_COOLDOWN, countdown);
        tag.putInt(BumbliaryBlockEntity.NBT_INV_OUT, brood.length);
        for (int i = 0; i < brood.length; i++) {
//? if >=1.21.1 {
            /*tag.put(BumbliaryBlockEntity.NBT_INV_OUT + "." + i,
                    brood[i].saveOptional(h.getLevel().registryAccess()));
*///?} else {
            tag.put(BumbliaryBlockEntity.NBT_INV_OUT + "." + i, brood[i].save(new CompoundTag()));
//?}
        }
        load(h, bee, tag);
    }

    @GameTest(template = "test_empty", timeoutTicks = 80)
    public static void ordinaryLifecyclePromotesLonePrincess(GameTestHelper h) {
        generation(h, false);
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 80)
    public static void advancedLifecyclePromotesLonePrincess(GameTestHelper h) {
        generation(h, true);
        h.succeed();
    }

    private static void generation(GameTestHelper h, boolean advanced) {
        BumbliaryBlockEntity apiary = place(h, advanced);
        var layout = apiary.layout();
        CompoundTag genes = genes();
        apiary.inventory().setStackInSlot(layout.royal(), bee(h, BumbleBeeType.PRINCESS, genes, 1));
        apiary.inventory().setStackInSlot(layout.drone(), bee(h, BumbleBeeType.DRONE, genes, 1));
        seed(h, apiary, 0, 1);
        apiary.serverTick();
        h.assertTrue(BumbleBeeType.of(apiary.inventory().getStackInSlot(layout.royal())) == BumbleBeeType.QUEEN
                && apiary.life() > 0 && apiary.brood().length > 0, "native pairing creates queen and brood");
        h.assertTrue(apiary.inventory().getStackInSlot(layout.drone()).isEmpty(), "single main drone consumed");

        // Pin the naturally possible one-princess brood independently of a lucky random seed.
        ItemStack princess = bee(h, BumbleBeeType.PRINCESS, genes, 1);
        ItemStack drones = bee(h, BumbleBeeType.DRONE, genes, 2);
        seed(h, apiary, 1, 1200, princess, drones);
        apiary.serverTick();
        h.assertTrue(apiary.endedQueen() && apiary.life() == 0 && apiary.brood().length == 0,
                "native death consumes pending brood exactly once");
        h.assertTrue(BumbleBeeType.of(apiary.inventory().getStackInSlot(layout.royal())) == BumbleBeeType.PRINCESS,
                "lone princess promoted from spare slots");
        h.assertTrue(apiary.inventory().getStackInSlot(layout.drone()).isEmpty(), "empty main slot stays empty");
        int living = 0;
        for (int slot : layout.drones()) living += apiary.inventory().getStackInSlot(slot).getCount();
        h.assertTrue(living == 2, "both drones retained and promoted princess removed from spare slot");
        seed(h, apiary, 0, 1);
        apiary.serverTick();
        h.assertTrue(apiary.life() > 0 && BumbleBeeType.of(apiary.inventory().getStackInSlot(layout.royal()))
                == BumbleBeeType.QUEEN, "next generation breeds using a spare drone");
    }

    @GameTest(template = "test_empty", timeoutTicks = 80)
    public static void deathClearsOldBeesButPreservesUnrelatedItems(GameTestHelper h) {
        for (boolean advanced : new boolean[]{false, true}) {
            BumbliaryBlockEntity apiary = place(h, advanced);
            var layout = apiary.layout();
            CompoundTag genes = genes();
            for (int slot : layout.dead()) apiary.inventory().setStackInSlot(slot,
                    bee(h, BumbleBeeType.DEAD, genes, 64));
            int unrelated = layout.dead()[layout.dead().length - 1];
            apiary.inventory().setStackInSlot(unrelated, new ItemStack(Items.STICK, 7));
            apiary.inventory().setStackInSlot(layout.royal(), bee(h, BumbleBeeType.QUEEN, genes, 1));
            apiary.inventory().setStackInSlot(layout.drones()[0], bee(h, BumbleBeeType.DRONE, genes, 2));
            seed(h, apiary, 1, 1200);
            apiary.serverTick();
            int dead = 0;
            for (int slot : layout.dead()) {
                ItemStack stack = apiary.inventory().getStackInSlot(slot);
                if (BumbleBeeType.of(stack) != null) {
                    h.assertTrue(BumbleBeeType.of(stack) == BumbleBeeType.DEAD, "new outputs are dead bees");
                    dead += stack.getCount();
                }
            }
            h.assertTrue(dead == 3, "only the newly dead queen and two spare drones remain");
            h.assertTrue(apiary.inventory().getStackInSlot(unrelated).is(Items.STICK)
                    && apiary.inventory().getStackInSlot(unrelated).getCount() == 7, "non-bee contents preserved");
            apiary.serverTick();
            h.assertTrue(!apiary.endedQueen(), "death is not repeated on the next tick");
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 80)
    public static void occupiedMainAndBlockedSpareSlotsKeepOriginalRules(GameTestHelper h) {
        for (boolean advanced : new boolean[]{false, true}) {
            BumbliaryBlockEntity apiary = place(h, advanced);
            var layout = apiary.layout();
            CompoundTag genes = genes();
            apiary.inventory().setStackInSlot(layout.royal(), bee(h, BumbleBeeType.QUEEN, genes, 1));
            apiary.inventory().setStackInSlot(layout.drone(), bee(h, BumbleBeeType.DRONE, genes, 2));
            seed(h, apiary, 1, 1200, bee(h, BumbleBeeType.PRINCESS, genes, 1),
                    bee(h, BumbleBeeType.DRONE, genes, 3));
            apiary.serverTick();
            h.assertTrue(apiary.inventory().getStackInSlot(layout.drone()).getCount() == 5,
                    "matching brood drones merge into occupied primary slot");
            h.assertTrue(BumbleBeeType.of(apiary.inventory().getStackInSlot(layout.royal())) == BumbleBeeType.PRINCESS,
                    "princess uses spare slot when main holds drones");

            apiary = place(h, advanced);
            layout = apiary.layout();
            apiary.inventory().setStackInSlot(layout.royal(), bee(h, BumbleBeeType.QUEEN, genes, 1));
            for (int slot : layout.drones()) apiary.inventory().setStackInSlot(slot, new ItemStack(Items.STICK, 64));
            seed(h, apiary, 1, 1200, bee(h, BumbleBeeType.PRINCESS, genes, 1));
            apiary.serverTick();
            h.assertTrue(apiary.inventory().getStackInSlot(layout.royal()).isEmpty()
                    && apiary.inventory().getStackInSlot(layout.drone()).isEmpty(),
                    "full spare slots do not redirect brood into empty main slot");
            for (int slot : layout.drones()) h.assertTrue(apiary.inventory().getStackInSlot(slot).is(Items.STICK)
                    && apiary.inventory().getStackInSlot(slot).getCount() == 64, "blocked spare slots untouched");
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 80)
    public static void unsuitableBroodDiesInsteadOfPromoting(GameTestHelper h) {
        for (boolean advanced : new boolean[]{false, true}) {
            BumbliaryBlockEntity apiary = place(h, advanced);
            var layout = apiary.layout();
            apiary.inventory().setStackInSlot(layout.royal(), bee(h, BumbleBeeType.QUEEN, genes(), 1));
            CompoundTag invalid = genes();
            BumbleBeeGenes.setInsideActive(invalid, false);
            BumbleBeeGenes.setOutsideActive(invalid, false);
            seed(h, apiary, 1, 1200, bee(h, BumbleBeeType.PRINCESS, invalid, 1));
            apiary.serverTick();
            h.assertTrue(apiary.inventory().getStackInSlot(layout.royal()).isEmpty(), "invalid princess is not promoted");
            int dead = 0;
            for (int slot : layout.dead()) dead += apiary.inventory().getStackInSlot(slot).getCount();
            h.assertTrue(dead == 2 && apiary.brood().length == 0, "queen and invalid child die exactly once");
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 80)
    public static void pureProgressTicksDirtyNativeChunkAndSerialize(GameTestHelper h) {
        for (boolean advanced : new boolean[]{false, true}) {
            BumbliaryBlockEntity apiary = place(h, advanced);
            apiary.inventory().setStackInSlot(apiary.layout().royal(), bee(h, BumbleBeeType.QUEEN, genes(), 1));
            seed(h, apiary, 100, 1200, bee(h, BumbleBeeType.PRINCESS, genes(), 1));
            var chunk = h.getLevel().getChunkAt(apiary.getBlockPos());
            chunk.setUnsaved(false);
            apiary.serverTick();
            h.assertTrue(apiary.life() == 99 && chunk.isUnsaved(), "pure life decrement dirties native chunk");
            CompoundTag saved = save(h, apiary);
            BumbliaryBlockEntity restored = new BumbliaryBlockEntity(apiary.getBlockPos(), apiary.getBlockState());
            load(h, restored, saved);
            h.assertTrue(restored.life() == 99 && restored.countdown() == 1200 && restored.brood().length == 1,
                    "native serialization retains latest life/countdown/brood (not a disk restart claim)");

            apiary.inventory().setStackInSlot(apiary.layout().royal(), bee(h, BumbleBeeType.PRINCESS, genes(), 1));
            seed(h, apiary, 0, 100);
            chunk.setUnsaved(false);
            apiary.serverTick();
            h.assertTrue(apiary.countdown() == 99 && chunk.isUnsaved(), "pure princess wait dirties native chunk");
            load(h, restored, save(h, apiary));
            h.assertTrue(restored.countdown() == 99, "updated pairing countdown serialized");
            seed(h, apiary, 0, 1);
            chunk.setUnsaved(false);
            apiary.serverTick();
            h.assertTrue(apiary.countdown() == 600 && chunk.isUnsaved(), "no-drone retry reset dirties native chunk");
            apiary.inventory().setStackInSlot(apiary.layout().royal(), ItemStack.EMPTY);
            seed(h, apiary, 55, 44, bee(h, BumbleBeeType.PRINCESS, genes(), 1));
            chunk.setUnsaved(false);
            apiary.serverTick();
            h.assertTrue(apiary.life() == 0 && apiary.countdown() == 1200 && apiary.brood().length == 0
                    && chunk.isUnsaved(), "orphan progress reset dirties native chunk without slot mutations");
            chunk.setUnsaved(false);
            apiary.serverTick();
            h.assertTrue(!chunk.isUnsaved(), "unchanged idle machine does not dirty chunk every tick");
        }
        h.succeed();
    }
}
