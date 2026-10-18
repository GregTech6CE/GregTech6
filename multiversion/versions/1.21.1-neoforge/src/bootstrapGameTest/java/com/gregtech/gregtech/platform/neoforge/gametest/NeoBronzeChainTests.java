package com.gregtech.gregtech.platform.neoforge.gametest;

import com.gregtech.gregtech.api.machine.crucible.CrucibleItemInput;
import com.gregtech.gregtech.api.machine.crucible.CrucibleMaterialStack;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.platform.neoforge.smeltery.*;
import com.gregtech.gregtech.registry.GTItems;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.List;

/** Existing stage chain now mines its raw charges with a real crafted starter pick; equipment is supplied. */
@GameTestHolder("gregtech_playflow")
@PrefixGameTestTemplate(false)
public final class NeoBronzeChainTests {
    @GameTest(template = "test_bronze_chain", timeoutTicks = 9800)
    public static void coalCopperTinToFourBronzeIngots(GameTestHelper helper) {
        BlockPos boxPos = new BlockPos(2, 1, 3), cruciblePos = boxPos.above(), moldPos = cruciblePos.east();
        helper.setBlock(boxPos.below(), Blocks.STONE);
        helper.setBlock(moldPos.below(), Blocks.STONE);
        helper.setBlock(boxPos, SmelteryRegistries.BRICK_BURNING_BOX.get());
        helper.setBlock(cruciblePos, SmelteryRegistries.CERAMIC_CRUCIBLE.get());
        helper.setBlock(moldPos, SmelteryRegistries.CERAMIC_MOLD.get());
        var box = (SolidBurningBoxEntity) helper.getBlockEntity(boxPos);
        var crucible = (SmeltingCrucibleEntity) helper.getBlockEntity(cruciblePos);
        var mold = (MoldEntity) helper.getBlockEntity(moldPos);
        var actor = net.neoforged.neoforge.common.util.FakePlayerFactory.get(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "[gt-raw-bronze]"));
        actor.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        actor.getInventory().clearContent();
        actor.getInventory().selected = 8;
        Player player = actor;
        List<ItemStack> minedCharges = harvestRawCharges(helper, actor);
        var wood = com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Wood");
        var chisel = SmelteryRegistries.CHISEL.get().assemble(Materials.Steel, wood);
        var pincers = SmelteryRegistries.PINCERS.get().assemble(Materials.Steel, wood);
        helper.assertTrue(!chisel.isEmpty() && !pincers.isEmpty() && !player.isCreative(), "Real assembled steel tools must consume wear");
        player.setItemInHand(InteractionHand.MAIN_HAND, chisel);
        for (int z = 0; z < 5; z++) for (int x = 0; x < 3; x++)
            click(helper, player, mold.getBlockPos(), Direction.UP, 0.125 + (x + 0.5) * 0.15, 0.1875, 0.125 + (z + 0.5) * 0.15);
        helper.assertTrue(chisel.getDamageValue() == 15 && mold.getMoldRequiredMaterialUnits() == GTValues.U, "Fifteen real chisel clicks carve one-U ingot form");
        List<ItemEntity> charges = List.of(drop(helper, crucible, minedCharges.get(0), 0.35),
                drop(helper, crucible, minedCharges.get(1), 0.65));
        int[] phase = {0}, pickups = {0};
        boolean[] finished = {false};
        helper.onEachTick(() -> {
            if (finished[0]) return;
            long units = CrucibleMaterialStack.total(crucible.getContentView()) + itemUnits(crucible.getCacheStack())
                    + mold.getMoldContentAmount() + bronzeCount(player) * GTValues.U;
            for (var entity : charges) if (entity.isAlive()) units += itemUnits(entity.getItem());
            helper.assertTrue(units == 4L * GTValues.U, "Copper/tin/bronze must conserve four U each tick");
            long bronze = crucible.getContentView().stream().filter(s -> s.material == Materials.Bronze.resolve()).mapToLong(s -> s.amount).sum();
            if (crucible.getTemperature() < 1357) helper.assertTrue(bronze == 0 && mold.getMoldContentAmount() == 0 && bronzeCount(player) == 0,
                    "No bronze may appear before the real 1357 K reaction");
            if (phase[0] == 0 && crucible.getCacheStack().isEmpty() && charges.stream().noneMatch(ItemEntity::isAlive)) {
                helper.assertTrue(crucible.getContentView().size() == 2 && crucible.getEnergyBuffer() == 0, "Inputs must enter cold without artificial HU");
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COAL, 8));
                click(helper, player, box.getBlockPos(), Direction.NORTH, 0.5, 0.5, 0);
                helper.assertTrue(player.getMainHandItem().isEmpty() && box.getFuelStack().getCount() == 8, "Front click must move real coal into inventory");
                ItemStack flint = new ItemStack(Items.FLINT_AND_STEEL);
                player.setItemInHand(InteractionHand.MAIN_HAND, flint);
                click(helper, player, box.getBlockPos(), Direction.NORTH, 0.5, 0.5, 0);
                helper.assertTrue(box.isBurning() && flint.getDamageValue() == 1, "Front ignition must wear actual flint and steel");
                phase[0] = 1;
            } else if (phase[0] == 1 && bronze > 0) {
                helper.assertTrue(bronze == 4L * GTValues.U && box.getFuelStack().getCount() < 8 && !box.getAshStack().isEmpty(),
                        "Natural coal heat and material ash must produce exactly four U bronze");
                LogUtils.getLogger().info("GT6_NEO_PLAYFLOW_ALLOY tick={} temperature={} amount={}", helper.getTick(), crucible.getTemperature(), bronze);
                helper.setBlock(boxPos, Blocks.AIR);
                player.setItemInHand(InteractionHand.MAIN_HAND, pincers);
                phase[0] = 2;
            } else if (phase[0] == 2) {
                long before = CrucibleMaterialStack.total(crucible.getContentView());
                int count = bronzeCount(player);
                click(helper, player, mold.getBlockPos(), Direction.UP, 0.5, 0.1875, 0.5);
                if (mold.getMoldContentAmount() > 0) {
                    long after = CrucibleMaterialStack.total(crucible.getContentView());
                    helper.assertTrue(mold.getMoldContentAmount() == GTValues.U && before - after <= GTValues.U, "Pour only one U; occupied mold refuses additional input");
                }
                if (bronzeCount(player) > count) pickups[0]++;
                if (pickups[0] == 4) {
                    helper.assertTrue(pincers.getDamageValue() == 4 && bronzeCount(player) == 4 && crucible.getContentView().isEmpty()
                            && mold.getMoldContentAmount() == 0, "Four real casts deliver four ingots with four tool wear");
                    phase[0] = 3;
                }
            } else if (phase[0] == 3) {
                click(helper, player, mold.getBlockPos(), Direction.UP, 0.5, 0.1875, 0.5);
                helper.assertTrue(bronzeCount(player) == 4 && mold.getMoldContentAmount() == 0, "Repeated empty interaction must not mint a fifth ingot");
                finished[0] = true;
                LogUtils.getLogger().info("GT6_NEO_PLAYFLOW_COMPLETE tick={} ingots=4 chiselWear=15 pincersWear=4", helper.getTick());
                verifyStackedBasin(helper, player, mold);
                helper.succeed();
            }
        });
    }
    private static List<ItemStack> harvestRawCharges(GameTestHelper helper,
            net.neoforged.neoforge.common.util.FakePlayer actor) {
        BlockPos bench = new BlockPos(1, 1, 1);
        helper.setBlock(bench, Blocks.CRAFTING_TABLE);
        var menu = new net.minecraft.world.inventory.CraftingMenu(77, actor.getInventory(),
                net.minecraft.world.inventory.ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(bench)));
        actor.containerMenu = menu;
        // Starter flint/stick and equipment are supplied; the actual recipe result must consume them.
        for (int slot = 1; slot <= 3; slot++) menu.getSlot(slot).set(new ItemStack(Items.FLINT));
        menu.getSlot(5).set(new ItemStack(Items.STICK));
        menu.clicked(0, 0, net.minecraft.world.inventory.ClickType.PICKUP, actor);
        ItemStack pick = menu.getCarried().copy();
        helper.assertTrue(com.gregtech.gregtech.api.tool.GTToolHelper.isUsable(pick)
                && com.gregtech.gregtech.api.tool.GTToolHelper.getHead(pick) == Materials.Flint,
                "Real workbench result must produce the starter flint pick");
        for (int slot = 1; slot <= 9; slot++) helper.assertTrue(menu.getSlot(slot).getItem().isEmpty(), "Workbench consumed actual inputs");
        menu.setCarried(ItemStack.EMPTY);
        actor.containerMenu = actor.inventoryMenu;
        actor.setItemInHand(InteractionHand.MAIN_HAND, pick);
        for (int i = 0; i < 4; i++) {
            var material = i == 3 ? Materials.Tin : Materials.Copper;
            BlockPos relative = new BlockPos(6 + i, 1, 3), absolute = helper.absolutePos(relative);
            var block = com.gregtech.gregtech.registry.GTBlocks.getObject(
                    com.gregtech.gregtech.api.prefix.BlockMaterialPrefix.ore, material);
            helper.assertTrue(block != null, "Registered actual raw ore block exists");
            helper.setBlock(relative.below(), Blocks.STONE);
            helper.setBlock(relative, block.get());
            actor.setPos(absolute.getX()+.5, absolute.getY(), absolute.getZ()+1.5);
            helper.assertTrue(pick.isCorrectToolForDrops(helper.getLevel().getBlockState(absolute))
                    && actor.gameMode.destroyBlock(absolute) && helper.getLevel().getBlockState(absolute).isAir(),
                    "Crafted starter pick actually harvests the placed ore");
            ItemStack expected = GTItems.getStack(MaterialPrefix.oreRaw, material);
            int found = 0;
            for (var entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                    new net.minecraft.world.phys.AABB(absolute).inflate(1))) {
                if (!ItemStack.isSameItemSameComponents(entity.getItem(), expected)) continue;
                found += entity.getItem().getCount();
                helper.assertTrue(actor.getInventory().add(entity.getItem().copy()), "Actual raw drops fit actor inventory");
                entity.discard();
            }
            helper.assertTrue(found == 1, "Each real harvest produces exactly one raw charge");
        }
        helper.assertTrue(pick.getDamageValue() == 300, "Four real harvests retain actual starter wear");
        actor.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        var result = new java.util.ArrayList<ItemStack>();
        for (var material : List.of(Materials.Copper, Materials.Tin)) {
            ItemStack expected = GTItems.getStack(MaterialPrefix.oreRaw, material);
            int required = material == Materials.Copper ? 3 : 1;
            ItemStack gathered = ItemStack.EMPTY;
            for (int slot = 0; slot < actor.getInventory().getContainerSize(); slot++) {
                var stack = actor.getInventory().getItem(slot);
                if (!ItemStack.isSameItemSameComponents(stack, expected)) continue;
                if (gathered.isEmpty()) gathered = stack.copyWithCount(0);
                gathered.grow(stack.getCount());
                actor.getInventory().setItem(slot, ItemStack.EMPTY);
            }
            helper.assertTrue(gathered.getCount() == required, "Use harvested raw stock without replacement");
            var display = com.gregtech.gregtech.data.MachineRecipeMaps.CrucibleSmelting.mRecipeList.stream()
                    .filter(recipe -> recipe.mInputs.length==1 && ItemStack.isSameItemSameComponents(recipe.mInputs[0],expected))
                    .findFirst().orElseThrow();
            // Recipe registration intentionally unifies copper to vanilla's copper ingot.
            var outputForm = display.mOutputs.length == 1
                    ? com.gregtech.gregtech.api.material.MaterialEquivalence.form(display.mOutputs[0]) : null;
            helper.assertTrue(display.mFakeRecipe && display.mOutputs.length==1
                    && outputForm != null && outputForm.prefix() == MaterialPrefix.ingot
                    && outputForm.material() == material.resolve() && display.mOutputs[0].getCount()==1
                    && display.mSpecialValue==material.getMeltingPoint(), "Actual recipe directory shows the real one-U raw yield and temperature");
            LogUtils.getLogger().info("GT6_NEO_RAW_PREVIEW material={} output={} count={} temperature={}",
                    material.getName(), net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(display.mOutputs[0].getItem()),
                    display.mOutputs[0].getCount(), display.mSpecialValue);
            result.add(gathered);
        }
        LogUtils.getLogger().info("GT6_NEO_PLAYFLOW_RAW_MINED actualHarvests=4 rawCopper=3 rawTin=1 flintPickWear=300 recipePreviewOneU=true");
        return result;
    }

    private static void verifyStackedBasin(GameTestHelper helper, Player player, MoldEntity mold) {
        var basinBlock = SmelteryRegistries.basins().stream()
                .filter(holder -> holder.get().spec().material().equals(Materials.Ceramic))
                .findFirst().orElseThrow().get();
        BlockPos basinPos = mold.getBlockPos().below();
        helper.getLevel().setBlockAndUpdate(basinPos, basinBlock.defaultBlockState());
        var basin = (com.gregtech.gregtech.blockentity.machine.MoldBasinBlockEntity)
                helper.getLevel().getBlockEntity(basinPos);
        helper.assertTrue(basin.fillMold(Materials.Bronze, 9L * GTValues.U, 1400, Direction.UP.ordinal())
                == 9L * GTValues.U, "Stacked basin accepts exactly nine U");
        mold.serverTick();
        helper.assertTrue(!mold.tryPickupWithPincers(player, true)
                && basin.getMoldContentAmount() == 9L * GTValues.U,
                "Ambient air must not make a hot basin immediately castable");
        for (int tick = 0; tick < 250; tick++)
            com.gregtech.gregtech.blockentity.machine.MoldBasinBlockEntity.serverTick(
                    helper.getLevel(), basinPos, basin.getBlockState(), basin);
        for (int cast = 1; cast <= 9; cast++) {
            click(helper, player, mold.getBlockPos(), Direction.UP, 0.5, 0.1875, 0.5);
            helper.assertTrue(bronzeCount(player) == 4 + cast
                    && basin.getMoldContentAmount() == (9L - cast) * GTValues.U,
                    "Each shaped output consumes exactly one U and retains every unused unit");
        }
        click(helper, player, mold.getBlockPos(), Direction.UP, 0.5, 0.1875, 0.5);
        helper.assertTrue(bronzeCount(player) == 13 && basin.getMoldContentAmount() == 0,
                "Empty basin must not produce a tenth ingot");
        LogUtils.getLogger().info("GT6_NEO_STACKED_BASIN_COMPLETE ingots=9 remaining=0 hotPickupRejected=true");
    }
    private static void click(GameTestHelper helper, Player player, BlockPos pos, Direction side, double x, double y, double z) {
        var hit = new BlockHitResult(new Vec3(pos.getX() + x, pos.getY() + y, pos.getZ() + z), side, pos, false);
        var state = helper.getLevel().getBlockState(pos);
        var event = new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock(
                player, InteractionHand.MAIN_HAND, pos, hit);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) return;
        state.useItemOn(player.getMainHandItem(), helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
    }
    private static ItemEntity drop(GameTestHelper helper, SmeltingCrucibleEntity crucible, ItemStack stack, double x) {
        helper.assertTrue(!stack.isEmpty(), "Registered input item must exist");
        BlockPos pos = crucible.getBlockPos();
        ItemEntity entity = new ItemEntity(helper.getLevel(), pos.getX() + x, pos.getY() + 1.1, pos.getZ() + 0.5, stack);
        entity.setDeltaMovement(Vec3.ZERO);
        helper.assertTrue(helper.getLevel().addFreshEntity(entity), "Actual input entity must spawn");
        return entity;
    }
    private static long itemUnits(ItemStack stack) { return CrucibleMaterialStack.total(CrucibleItemInput.parse(stack)) * stack.getCount(); }
    private static int bronzeCount(Player player) {
        ItemStack bronze = GTItems.getStack(MaterialPrefix.ingot, Materials.Bronze);
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (ItemStack.isSameItemSameComponents(stack, bronze)) count += stack.getCount();
        }
        return count;
    }
}
