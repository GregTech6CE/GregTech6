package gregtech.gametest;

import gregtech.api.recipe.*;
import gregtech.internal.recipe.*;
import gregtech.internal.tool.*;
import gregtech.block.machine.FluidPipeBlock;
import gregtech.block.misc.*;
import gregtech.blockentity.CFoamBlockEntity;
import gregtech.blockentity.energy.EnergyNodeBlockEntity;
import gregtech.blockentity.inventory.MassStorageBlockEntity;
import gregtech.blockentity.machine.*;
import gregtech.data.*;
import gregtech.registry.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

/** Only the current player feedback, using real registrations, capability faces and server ticks. */
@GameTestHolder("gt6ce_storage_direction_feedback")
@PrefixGameTestTemplate(false)
public final class StorageAndDirectionFeedbackTests {
    @GameTest(template="test_empty", timeoutTicks=40)
    public static void monkeyWrenchSwitchesMassStorageAndActualBottomOutput(GameTestHelper h) {
        BlockPos pos = h.absolutePos(new BlockPos(3, 3, 3));
        var storageBlock = BuiltInRegistries.BLOCK.stream().filter(block -> block instanceof gregtech.block.inventory.MassStorageBlock).findFirst().orElseThrow();
        h.getLevel().setBlockAndUpdate(pos, storageBlock.defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos.below(), Blocks.CHEST.defaultBlockState());
        var storage = (MassStorageBlockEntity)h.getLevel().getBlockEntity(pos);
        var actor = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());
        var toolItem = BuiltInRegistries.ITEM.stream().filter(item -> item instanceof gregtech.item.GTToolItem tool
                && tool.toolType() == GTToolType.MONKEY_WRENCH).findFirst().orElseThrow();
        var tool = GTToolHelper.write(new ItemStack(toolItem), gregtech.content.material.Materials.Steel,
                gregtech.internal.material.GTMaterialRegistry.get("Wood"));
        actor.setItemInHand(InteractionHand.MAIN_HAND, tool);
        var state = h.getLevel().getBlockState(pos);
        state.useItemOn(tool, h.getLevel(), actor, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false));
        h.assertTrue(storage.autoOutput(), "real monkey-wrench click toggles automatic downward output");
        var bottom = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.DOWN);
        h.assertTrue(bottom.insertItem(0, new ItemStack(Items.DIAMOND), false).getCount() == 1,
                "auto-output face refuses external insertion");
        storage.insert(new ItemStack(Items.DIAMOND, 64)); storage.tickAutomaticOutput();
        var chest = (ChestBlockEntity)h.getLevel().getBlockEntity(pos.below());
        h.assertTrue(storage.stored() == 0 && chest.getItem(0).getCount() == 64, "one real stack reaches the lower chest");
        storage.toggleAutoOutput(); storage.toggleOverflow();
        var saved = storage.saveWithoutMetadata(h.getLevel().registryAccess());
        saved.putLong("gt.stored", MassStorageBlockEntity.CAPACITY + 80);
        storage.loadWithComponents(saved, h.getLevel().registryAccess());
        chest.clearContent(); storage.tickAutomaticOutput();
        h.assertTrue(storage.stored() == MassStorageBlockEntity.CAPACITY && chest.getItem(0).getCount() == 64
                && chest.getItem(1).getCount() == 16, "overflow emits only the extra eighty items");
        h.succeed();
    }

    @GameTest(template="test_empty", timeoutTicks=210)
    public static void sleepingHoppersRetryOrdinaryChestsAndNeverExtractMachineInputs(GameTestHelper h) {
        var hoppers = new ArrayList<net.minecraft.world.level.block.entity.BlockEntity>();
        var sources = new ArrayList<ChestBlockEntity>();
        var targets = new ArrayList<ChestBlockEntity>();
        for (String id : new String[]{"hopper_lead", "queue_hopper_lead"}) {
            BlockPos pos = h.absolutePos(new BlockPos(id.startsWith("queue") ? 9 : 3, 3, 3));
            var block = block(id);
            h.getLevel().setBlockAndUpdate(pos, block.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.DOWN));
            h.getLevel().setBlockAndUpdate(pos.above(), Blocks.CHEST.defaultBlockState());
            h.getLevel().setBlockAndUpdate(pos.below(), Blocks.CHEST.defaultBlockState());
            var hopper = h.getLevel().getBlockEntity(pos);
            h.assertTrue(hopper != null, "actual hopper registration " + id); hoppers.add(hopper);
            sources.add((ChestBlockEntity)h.getLevel().getBlockEntity(pos.above()));
            targets.add((ChestBlockEntity)h.getLevel().getBlockEntity(pos.below()));
        }
        BlockPos machinePos = h.absolutePos(new BlockPos(14, 3, 3));
        h.getLevel().setBlockAndUpdate(machinePos, block("sluice_bronze").defaultBlockState());
        var machine = (BasicMachineBlockEntity)h.getLevel().getBlockEntity(machinePos);
        machine.inventory().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
        machine.inventory().setStackInSlot(machine.inputSlots(), new ItemStack(Items.EMERALD, 5));
        for (Direction side : Direction.values()) {
            var handler = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, machinePos, side);
            if (handler == null) continue;
            h.assertTrue(handler.extractItem(0, 64, false).isEmpty(), "input slot never exported through " + side);
        }
        h.assertTrue(machine.inventory().getStackInSlot(0).getCount() == 7, "input survives all external extraction attempts");
        h.runAfterDelay(35, () -> sources.forEach(chest -> chest.setItem(0, new ItemStack(Items.DIAMOND, 3))));
        h.runAfterDelay(125, () -> {
            for (int i = 0; i < targets.size(); i++) {
                h.assertTrue(sources.get(i).getItem(0).isEmpty() && targets.get(i).getItem(0).getCount() == 3,
                        "idle hopper notices ordinary chest refill and delivers items: " + i);
            }
            for (var chest : targets) for (int slot = 0; slot < chest.getContainerSize(); slot++)
                chest.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
            sources.forEach(chest -> chest.setItem(0, new ItemStack(Items.DIAMOND, 2)));
        });
        h.runAfterDelay(160, () -> targets.forEach(ChestBlockEntity::clearContent));
        h.runAfterDelay(195, () -> {
            for (int i = 0; i < targets.size(); i++)
                h.assertTrue(targets.get(i).getItem(0).getCount() == 2, "output retries when ordinary chest gains room: " + i);
            h.succeed();
        });
    }

    @GameTest(template="test_empty", timeoutTicks=40)
    public static void flatColorsPreserveOldStacksRecipesSlabsAndWetFoamAge(GameTestHelper h) {
        h.assertTrue(GTConstructionVariants.all().size() == 160, "ten palettes each have sixteen real identities");
        for (var registered : GTConstructionVariants.all()) {
            var block = registered.get();
            h.assertTrue(block instanceof FixedConstructionColor && !block.defaultBlockState().hasProperty(ConcreteBlock.COLOR), "color is part of ID: " + registered.getId());
            h.assertTrue(new ItemStack(block).getComponentsPatch().isEmpty(), "new color item needs no custom components");
            var family = ((FixedConstructionColor)block).family();
            var tag = net.minecraft.tags.ItemTags.create(ResourceLocation.parse("gt6ce:construction/" + family));
            h.assertTrue(new ItemStack(block).is(tag), "actual NeoForge singular-path palette tag is bound: " + family);
        }
        var legacy = new ItemStack(block("concrete"), 13);
        legacy.set(DataComponents.BLOCK_STATE, new BlockItemStateProperties(Map.of("color", "red")));
        legacy.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Saved name"));
        var restored = ItemStack.parseOptional(h.getLevel().registryAccess(), (net.minecraft.nbt.CompoundTag)legacy.save(h.getLevel().registryAccess()));
        h.assertTrue(restored.is(block("concrete_red").asItem()) && restored.getCount() == 13
                && !restored.has(DataComponents.BLOCK_STATE) && restored.getHoverName().getString().equals("Saved name"), "native old stack decode preserves name/count and upgrades color ID");
        var bare = new ItemStack(block("concrete"));
        h.assertTrue(RecipeInputs.matches(bare, new ItemStack(block("concrete_blue")))
                && !RecipeInputs.matches(bare, new ItemStack(block("cfoam_blue"))), "machine legacy ingredients accept exactly their palette family");
        var recipe = new Recipe(new ItemStack[]{bare}, new ItemStack[]{new ItemStack(block("cfoam"))}, null,
                null, null, null, 1, 1, 0);
        h.assertTrue(recipe.viewerInputAlternatives(0).size() == 16 && recipe.mOutputs[0].is(block("cfoam_white").asItem()), "machine viewer alternatives and outputs use real IDs");
        var panel = h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("gt6ce:decorative_panels/panel_concrete_red")).orElseThrow().value();
        h.assertTrue(panel.getIngredients().stream().anyMatch(ingredient -> ingredient.test(new ItemStack(block("concrete_red"))))
                && panel.getIngredients().stream().noneMatch(ingredient -> ingredient.test(new ItemStack(block("concrete_blue")))), "real loaded colored panel recipe accepts only its chosen color");
        BlockPos pos = h.absolutePos(new BlockPos(3, 3, 3));
        h.getLevel().setBlockAndUpdate(pos, block("cfoam_fresh_blue").defaultBlockState());
        var wet = (CFoamBlockEntity)h.getLevel().getBlockEntity(pos); wet.restoreTimer(75);
        h.assertTrue(FixedConstructionColor.recolor(h.getLevel(), pos, DyeColor.RED), "spray switches registered color");
        wet = (CFoamBlockEntity)h.getLevel().getBlockEntity(pos);
        h.assertTrue(wet.timer() == 75 && wet.dry() && h.getLevel().getBlockState(pos).is(block("cfoam_red")), "drying retains repainted identity and age");
        h.succeed();
    }

    @GameTest(template="test_empty", timeoutTicks=310)
    public static void fourBoilersSmallInvarPipeAndTurbineRunInAllHorizontalDirections(GameTestHelper h) {
        record Setup(Direction facing, EnergyNodeBlockEntity turbine, BasicMachineBlockEntity washer, List<BoilerTankBlockEntity> boilers) {}
        var setups = new ArrayList<Setup>();
        int index = 0;
        for (Direction facing : new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            BlockPos pos = h.absolutePos(new BlockPos(7 + (index % 2) * 16, 4, 7 + (index / 2) * 16)); index++;
            h.getLevel().setBlockAndUpdate(pos, block("steam_turbine_invar").defaultBlockState().setValue(DirectionalBlock.FACING, facing));
            BlockPos washerPos = pos.relative(facing);
            h.getLevel().setBlockAndUpdate(washerPos, block("sluice_bronze").defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing));
            var boilers = new ArrayList<BoilerTankBlockEntity>();
            Direction lateral = facing.getClockWise();
            for (int placed = 0; placed < 4; placed++) {
                // Deliberately reverse placement/ticker registration on the other axis.
                int i = facing.getAxis() == Direction.Axis.X ? 3 - placed : placed;
                BlockPos pipe = pos.relative(facing.getOpposite()).relative(lateral, i);
                var pipeState = block("pipe_small_invar").defaultBlockState().setValue(FluidPipeBlock.DOWN, true);
                if (i == 0) pipeState = pipeState.setValue(FluidPipeBlock.propFor(facing), true);
                if (i > 0) pipeState = pipeState.setValue(FluidPipeBlock.propFor(lateral.getOpposite()), true);
                if (i < 3) pipeState = pipeState.setValue(FluidPipeBlock.propFor(lateral), true);
                h.getLevel().setBlockAndUpdate(pipe, pipeState);
                h.getLevel().setBlockAndUpdate(pipe.below(), block("steam_boiler_bronze").defaultBlockState());
                boilers.add((BoilerTankBlockEntity)h.getLevel().getBlockEntity(pipe.below()));
            }
            setups.add(new Setup(facing, (EnergyNodeBlockEntity)h.getLevel().getBlockEntity(pos),
                    (BasicMachineBlockEntity)h.getLevel().getBlockEntity(washerPos), boilers));
        }
        h.onEachTick(() -> {
            for (var setup : setups) for (var boiler : setup.boilers())
                boiler.steamTank().setFluid(new FluidStack(GTFluids.still("Steam").get(), 1), boiler.steamTank().capacity() / 2 + 1000);
        });
        h.runAfterDelay(260, () -> {
            for (var setup : setups) {
                var turbine = setup.turbine();
                h.assertTrue(h.getLevel().getBlockEntity(turbine.getBlockPos()) == turbine, "turbine survives " + setup.facing());
                h.assertTrue(setup.washer().averageEnergyInput() > 0, "actual average RU arrives in " + setup.facing());
                int overloads = turbine.converterOverloads();
                h.assertTrue(overloads == 0 && setup.washer().averageEnergyInput() == 64.0,
                        "four nominal bronze boilers deliver a steady maximum 64 RU/t without spurious overload: " + setup.facing());
                com.mojang.logging.LogUtils.getLogger().info("GT6_DIRECTION_FEEDBACK facing={} averageRU={} overloads={} steam={}",
                        setup.facing(), setup.washer().averageEnergyInput(), overloads, turbine.turbineSteamAmount());
            }
            h.succeed();
        });
    }

    private static Block block(String id) {
        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("gt6ce:" + id));
        if (block == Blocks.AIR) throw new AssertionError("Missing block " + id);
        return block;
    }
}
