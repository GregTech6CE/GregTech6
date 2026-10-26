package gregtech.gametest;

import gregtech.content.blockentity.energy.ReactorCoreBlockEntity;
import gregtech.content.nuclear.ReactorCoolants;
import gregtech.content.nuclear.RodPhysics;
import gregtech.content.registry.GTEnergyNodes;
import gregtech.content.registry.GTFuelRods;
import gregtech.content.registry.GTToolItems;
import gregtech.internal.tool.GTToolType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
//?}

/** Actual rod execution, native player inventory delivery, and core/item serialization. */
@GameTestHolder("gt6ce_nuclear_fixes")
@PrefixGameTestTemplate(false)
public final class ReactorRodStateTests {
    private static final int[] BREEDERS = {9410, 9420, 9430, 9440};

    private static ReactorCoreBlockEntity progressedCore(GameTestHelper h, boolean four, int id) {
        var pos = h.absolutePos(new BlockPos(3, 3, 3));
        h.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        var block = four ? GTEnergyNodes.REACTOR_CORE_2X2.get() : GTEnergyNodes.REACTOR_CORE_BLOCK.get();
        h.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
        var core = (ReactorCoreBlockEntity) h.getLevel().getBlockEntity(pos);
        int slot = four ? 3 : 0;
        h.assertTrue(core.insertRod(slot, GTFuelRods.stack(id)), "registered breeder inserted");
        core.input.fill(new FluidStack(ReactorCoolants.INDUSTRIAL.inputFluid(), 64000), FluidAction.EXECUTE);
        core.setStopped(false);
        core.neutrons[slot] = 1234;
        core.react();
        core.setStopped(true);
        h.assertTrue(!core.failed && RodPhysics.life(core.rods[slot]) == RodPhysics.definition(core.rods[slot]).life() - 1234,
                "real reactor tick writes long breeding progress: " + id);
        return core;
    }

    private static boolean same(ItemStack a, ItemStack b) {
//? if >=1.21.1 {
        /*return a.getCount() == b.getCount() && ItemStack.isSameItemSameComponents(a, b);
*///?} else {
        return a.getCount() == b.getCount() && ItemStack.isSameItemSameTags(a, b);
//?}
    }

    private static BlockHitResult top(ReactorCoreBlockEntity core) {
        var pos = core.getBlockPos();
        return new BlockHitResult(new Vec3(pos.getX() + .75, pos.getY() + 1, pos.getZ() + .75), Direction.UP, pos, false);
    }

    @GameTest(template="test_empty", timeoutTicks=100)
    public static void pincersRetainBreederProgressWithFreshRodsAndFullCreativeInventory(GameTestHelper h) {
        for (boolean four : new boolean[]{false, true}) for (int id : BREEDERS)
            for (boolean creative : new boolean[]{false, true}) for (boolean full : new boolean[]{false, true}) {
                var core = progressedCore(h, four, id);
                int slot = four ? 3 : 0;
                var expected = core.rods[slot].copy();
                var player = TankItemLifecycleTests.player(h, core.getBlockPos());
                player.gameMode.changeGameModeForPlayer(creative ? GameType.CREATIVE : GameType.SURVIVAL);
                for (int i = 0; i < player.getInventory().items.size(); i++)
                    player.getInventory().setItem(i, new ItemStack(Items.STONE, 64));
                var fresh = GTFuelRods.stack(id);
                player.getInventory().setItem(1, fresh.copy());
                if (!full) player.getInventory().setItem(2, ItemStack.EMPTY);
                player.setItemInHand(InteractionHand.MAIN_HAND, GTToolItems.empty(GTToolType.PINCERS));
                h.assertTrue(!same(expected, fresh), "progressed rod differs from an unused rod");
                if (full && creative) {
                    var lost = expected.copy();
                    h.assertTrue(player.addItem(lost) && lost.isEmpty(),
                            "native creative inventory reproduces the old destructive delivery path");
                }
                h.assertTrue(player.gameMode.useItemOn(player, h.getLevel(), player.getMainHandItem(),
                        InteractionHand.MAIN_HAND, top(core)).consumesAction(), "native pincers interaction handled");
                h.assertTrue(core.rodCount() == 0, "pincers remove the intended slot");
                var drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(player.blockPosition()).inflate(2));
                var recovered = new java.util.ArrayList<ItemStack>();
                for (var stack : player.getInventory().items) if (same(expected, stack)) recovered.add(stack);
                for (var entity : drops) if (same(expected, entity.getItem())) recovered.add(entity.getItem());
                h.assertTrue(recovered.size() == 1 && RodPhysics.life(recovered.get(0)) == RodPhysics.life(expected),
                        "exactly one progressed rod survives native inventory delivery or overflow drop");
                h.assertTrue(same(player.getInventory().getItem(1), fresh), "unused rods are not merged with progressed rods");
                h.assertTrue(full == drops.stream().anyMatch(e -> same(expected, e.getItem())),
                        "a full inventory drops the preserved rod in either game mode");
                var returned = recovered.get(0).copy();
                for (var entity : drops) entity.discard();
                player.getInventory().clearContent();
                player.setItemInHand(InteractionHand.MAIN_HAND, returned);
                h.assertTrue(player.gameMode.useItemOn(player, h.getLevel(), returned, InteractionHand.MAIN_HAND,
                        top(core)).consumesAction(), "native rod reinsertion handled");
                h.assertTrue(same(core.rods[slot], expected), "reinsertion preserves exact long life and components");
                core.removeRod(slot);
            }
        h.succeed();
    }

    @GameTest(template="test_empty", timeoutTicks=100)
    public static void breederProgressSurvivesAutomationAndNativeCoreAndItemSerialization(GameTestHelper h) {
        for (boolean four : new boolean[]{false, true}) for (int id : BREEDERS) {
            var core = progressedCore(h, four, id);
            int slot = four ? 3 : 0;
            var expected = core.rods[slot].copy();
//? if >=1.21.1 {
            /*var saved = core.saveWithId(h.getLevel().registryAccess());
            core.restorePersistentState(saved, h.getLevel().registryAccess());
*///?} else {
            var saved = core.saveWithoutMetadata();
            core.load(saved);
//?}
            h.assertTrue(same(expected, core.rods[slot]), "native core save/load retains breeding progress");
            h.assertTrue(same(expected, core.items.extractItem(slot, 1, true)) && core.rodCount() == 1,
                    "automated extraction simulation preserves source");
            var extracted = core.items.extractItem(slot, 1, false);
            h.assertTrue(same(expected, extracted) && core.rodCount() == 0, "automated extraction retains progress");
//? if >=1.21.1 {
            /*var restored = ItemStack.parseOptional(h.getLevel().registryAccess(), (net.minecraft.nbt.CompoundTag) extracted.saveOptional(h.getLevel().registryAccess()));
*///?} else {
            var restored = ItemStack.of(extracted.save(new net.minecraft.nbt.CompoundTag()));
//?}
            h.assertTrue(same(expected, restored), "native item save/load retains progress above signed int range");
            h.assertTrue(core.items.insertItem(slot, restored, false).isEmpty() && same(expected, core.rods[slot]),
                    "automated reinsertion retains exact long life");
            core.removeRod(slot);
        }
        h.succeed();
    }
}
