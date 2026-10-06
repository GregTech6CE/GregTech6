package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.ItemMaterialRegistry;
import com.gregtech.gregtech.api.machine.crucible.CrucibleItemInput;
import com.gregtech.gregtech.api.recipe.RecipeInputs;
import com.gregtech.gregtech.block.machine.TankBlock;
import com.gregtech.gregtech.blockentity.machine.TankBlockEntity;
import com.gregtech.gregtech.blockentity.machine.LogisticsTankBlockEntity;
import com.gregtech.gregtech.content.cover.MachineCoverSpec;
import com.gregtech.gregtech.content.cover.PanelCover;
import com.gregtech.gregtech.content.recipe.VanillaRecoveryRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import java.util.List;

/** Finite real BlockItem / survival harvest / recovery scenarios, excluded from ordinary jars. */
@GameTestHolder("gregtech_tank_items") @PrefixGameTestTemplate(false)
public final class TankItemLifecycleTests {
    private static ServerPlayer player(GameTestHelper h, BlockPos pos) {
        var profile = new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "TankItemTest");
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false);
        var player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), profile, cookie.clientInformation());
        player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(h.getLevel().getServer(),
                new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND), player, cookie) {
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) {}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet, net.minecraft.network.PacketSendListener listener) {}
        };
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(pos.getX() + 3, pos.getY(), pos.getZ() + .5);
        return player;
    }

    private static ItemStack item(String path) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("gregtech:" + path)));
    }
    private static CompoundTag worldData(GameTestHelper h, TankBlockEntity tank) { return tank.saveWithId(h.getLevel().registryAccess()); }
    private static CompoundTag itemData(ItemStack stack) { return stack.getOrDefault(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag(); }
    private static void setItemData(ItemStack stack, CompoundTag tag) { stack.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA, net.minecraft.world.item.component.CustomData.of(tag)); }

    private static TankBlockEntity place(GameTestHelper h, ServerPlayer p, BlockPos pos, ItemStack stack) {
        h.getLevel().setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        p.setItemInHand(InteractionHand.MAIN_HAND, stack.copyWithCount(1));
        var hit = new BlockHitResult(Vec3.atCenterOf(pos.below()).add(0, .5, 0), Direction.UP, pos.below(), false);
        h.assertTrue(p.getMainHandItem().getItem().useOn(new net.minecraft.world.item.context.UseOnContext(
                p, InteractionHand.MAIN_HAND, hit)).consumesAction(), "actual harvested BlockItem places " + stack);
        return (TankBlockEntity) h.getLevel().getBlockEntity(pos);
    }
    private static ItemStack harvest(GameTestHelper h, ServerPlayer p, TankBlockEntity tank) {
        var pos = tank.getBlockPos();
        for (var e : h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2))) e.discard();
        p.getInventory().clearContent();
        boolean correctTool = false;
        for (var tool : List.of(Items.DIAMOND_PICKAXE, Items.DIAMOND_AXE, Items.DIAMOND_SHOVEL)) {
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(tool));
            if (tank.getBlockState().canHarvestBlock(h.getLevel(), pos, p)) { correctTool = true; break; }
        }
        h.assertTrue(correctTool, "correct survival harvest tool for " + BuiltInRegistries.BLOCK.getKey(tank.getBlockState().getBlock()));
        var expected = tank.getBlockState().getBlock().asItem();
        h.assertTrue(p.gameMode.destroyBlock(pos), "actual survival game mode removes tank");
        var entities = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2));
        var candidates = new java.util.ArrayList<ItemStack>();
        for (var e : entities) candidates.add(e.getItem());
        candidates.addAll(p.getInventory().items);
        var tanks = candidates.stream().filter(s -> s.is(expected)).toList();
        h.assertTrue(tanks.stream().mapToInt(ItemStack::getCount).sum() == 1, "exactly one tank is harvested");
        var result = tanks.get(0).copy();
        h.assertTrue(entities.stream().allMatch(e -> e.getItem().is(expected)), "no duplicated attached covers");
        for (var e : entities) e.discard();
        return result;
    }
    private static void checkRecovery(GameTestHelper h, ItemStack stack, boolean allowed) {
        h.assertTrue(ItemMaterialRegistry.canRecover(stack) == allowed, "harvested shell recovery eligibility");
        var recipe = VanillaRecoveryRecipes.recipes().stream().filter(r -> r.mInputs[0].is(stack.getItem())).findFirst().orElseThrow(() -> new IllegalStateException("Missing audited recovery for " + stack));
        h.assertTrue((RecipeInputs.consume(recipe, List.of(stack), List.of(), 1) != null) == allowed,
                "actual shredder predicate protects contents and consumes a clean shell");
        h.assertTrue(CrucibleItemInput.parse(stack).isEmpty() != allowed, "actual crucible payload guard");
    }

    @GameTest(template="test_empty", timeoutTicks=100)
    public static void emptyTankHarvestReusesAndRecoversOriginalShell(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 4, 4));
        var p = player(h, pos);
        int tanks = 0, recovered = 0, drained = 0, legacy = 0;
        for (var block : BuiltInRegistries.BLOCK) if (block instanceof TankBlock) {
            var fresh = new ItemStack(block);
            var tank = place(h, p, pos, fresh);
            var complete = worldData(h, tank);
            h.assertTrue(complete.contains("gt.temperature") && complete.contains("gt.tank"), "complete world save retains defaults");
            var drop = harvest(h, p, tank);
            h.assertTrue(!ItemMaterialRegistry.hasStoredContents(drop) && itemData(drop).isEmpty(), "fresh empty tank has no storage metadata " + BuiltInRegistries.BLOCK.getKey(block) + " data=" + itemData(drop));
            h.assertTrue(ItemStack.isSameItemSameComponents(fresh, drop), "fresh harvested shell stacks with an unused shell");
            boolean recoverable = com.gregtech.gregtech.content.recipe.TransportMaterialRegistration.recoveryItems().contains(fresh.getItem());
            if (recoverable) { checkRecovery(h, drop, true); recovered++; }
            if (drop.is(item("drum_steel").getItem())) {
                setItemData(drop, complete);
                h.assertTrue(ItemMaterialRegistry.hasStoredContents(drop), "legacy complete item data stays guarded until placed");
                legacy++;
            }
            tank = place(h, p, pos, drop);
            h.assertTrue(tank.getFluidTank().isEmpty() && !tank.isAutoOutput() && !tank.getSoftHammerState(), "empty shell restores default state");
            if (!(tank instanceof LogisticsTankBlockEntity)) {
                h.assertTrue(tank.getFluidTank().fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE) == 1000, "placed empty tank fills");
                h.assertTrue(tank.getFluidTank().drain(1000, FluidAction.EXECUTE).getAmount() == 1000, "complete drain conserves water");
                var empty = harvest(h, p, tank);
                h.assertTrue(!ItemMaterialRegistry.hasStoredContents(empty), "drained ordinary tank becomes a clean shell again");
                if (recoverable) checkRecovery(h, empty, true);
                drained++;
            } else harvest(h, p, tank);
            tanks++;
        }
        h.assertTrue(tanks == 32 && recovered == 30 && drained == 31 && legacy == 1, "complete existing tank registry covered");
        System.out.println("TANK_ITEM_LIFECYCLE_EMPTY tanks=" + tanks + " recovery=" + recovered + " drained=" + drained + " legacy=" + legacy);
        h.succeed();
    }

    @GameTest(template="test_empty", timeoutTicks=100)
    public static void storedTankSettingsCoversAndLogisticsFilterSurviveHarvest(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 4, 4));
        var p = player(h, pos);
        var tank = place(h, p, pos, item("drum_steel"));
        tank.getFluidTank().fill(new FluidStack(Fluids.WATER, 4321), FluidAction.EXECUTE);
        var state = worldData(h, tank);
        state.putLong("gt.temperature", 333);
        state.putBoolean("gt.auto_output", true);
        state.putBoolean("gt.soft_hammer", true);
        state.putLong("gt.sealed_time", 120);
        state.putLong("gt.max_sealed_time", 600);
        state.getCompound("gt.tank").putLong("Capacity", tank.spec().capacity() + 17);
        var legacy = item("drum_steel");
        setItemData(legacy, state);
        // Place a legacy complete payload through BlockItem, rather than a synthetic NBT round trip.
        tank = place(h, p, pos, legacy);
        h.assertTrue(((com.gregtech.gregtech.api.tool.Paintable) (Object) tank).paint(0x123456), "actual native tank paint");
        h.assertTrue(tank.attachCover(Direction.NORTH, item(PanelCover.PROGRESS.id)), "native barrel accepts progress panel");
        h.assertTrue(tank.panels().configure(Direction.NORTH, false, false), "screwdriver inverts progress panel");
        h.assertTrue(MachineCoverSpec.inverted(tank.getCover(Direction.NORTH)), "panel is inverted before harvest");
        var drop = harvest(h, p, tank);
        checkRecovery(h, drop, false);
        tank = place(h, p, pos, drop);
        h.assertTrue(tank.getFluidTank().getAmount() == 4321 && tank.getFluidTank().getFluidLong().getFluid() == Fluids.WATER,
                "actual replacement keeps fluid identity and exact long amount");
        h.assertTrue(tank.getTemperature() == 333 && tank.isAutoOutput() && tank.getSoftHammerState()
                && tank.getSealedTime() == 120 && tank.getFluidTank().baseCapacity() == tank.spec().capacity() + 17,
                "nondefault heat, modes, progress and capacity survive actual placement");
        h.assertTrue(worldData(h, tank).getLong("gt.max_sealed_time") == 600, "fermentation duration retained");
        h.assertTrue(MachineCoverSpec.inverted(tank.getCover(Direction.NORTH)), "configured cover retained once");
        var painted = (com.gregtech.gregtech.api.tool.Paintable) (Object) tank;
        h.assertTrue(painted.isPainted() && painted.getPaint() == 0x123456, "actual paint flag and RGB survive replacement");
        tank.getFluidTank().drain(4321, FluidAction.EXECUTE);
        drop = harvest(h, p, tank);
        h.assertTrue(ItemMaterialRegistry.hasStoredContents(drop), "empty but configured tank stays data-bearing");
        checkRecovery(h, drop, false);

        var logisticsBlock = BuiltInRegistries.BLOCK.stream().filter(b -> b instanceof com.gregtech.gregtech.block.machine.LogisticsTankBlock).findFirst().orElseThrow();
        var logistics = (LogisticsTankBlockEntity) place(h, p, pos, new ItemStack(logisticsBlock));
        logistics.getFluidTank().fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE);
        h.assertTrue(logistics.getFluidTank().drain(1000, FluidAction.EXECUTE).getAmount() == 1000, "logistics barrel drains all contents");
        drop = harvest(h, p, logistics);
        h.assertTrue(ItemMaterialRegistry.hasStoredContents(drop), "zero-amount fluid filter remains in item data");
        logistics = (LogisticsTankBlockEntity) place(h, p, pos, drop);
        h.assertTrue(logistics.getFluidTank().getAmount() == 0 && logistics.fluidStorageFilter() == Fluids.WATER, "original retained logistics filter restored");
        h.assertTrue(logistics.getFluidTank().fill(new FluidStack(Fluids.LAVA, 1), FluidAction.EXECUTE) == 0,
                "different fluid cannot replace the restored filter");
        h.assertTrue(logistics.getFluidTank().fill(new FluidStack(Fluids.WATER, 27), FluidAction.EXECUTE) == 27, "filtered tank remains usable");
        harvest(h, p, logistics);
        var forged = item("drum_steel");
        setItemData(forged, new CompoundTag());
        checkRecovery(h, forged, false);
        System.out.println("TANK_ITEM_LIFECYCLE_STORED fluid=4321 progress=120 duration=600 covers=1 paint=123456 logisticsFilter=water arbitraryEmptyTag=protected");
        h.succeed();
    }
}
