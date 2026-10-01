package com.gregtech.gregtech.integration.gameplay;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.machine.crucible.CrucibleItemInput;
import com.gregtech.gregtech.api.machine.crucible.CrucibleMaterialStack;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.block.machine.GTFacingMachineBlock;
import com.gregtech.gregtech.blockentity.machine.MoldBlockEntity;
import com.gregtech.gregtech.blockentity.machine.SmeltingCrucibleBlockEntity;
import com.gregtech.gregtech.blockentity.machine.SolidBurningBoxBlockEntity;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.event.SmelteryInteractionHandler;
import com.gregtech.gregtech.item.GTToolItem;
import com.gregtech.gregtech.item.MaterialItem;
import com.gregtech.gregtech.registry.GTItems;
import com.gregtech.gregtech.registry.GTMachines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/** Given equipment and ingredients, exercise the actual ticking Forge casting chain. */
@GameTestHolder("gregtech_playflow")
@PrefixGameTestTemplate(false)
public final class ForgeBronzeChainTests {
    private static final long BRONZE_REACTION_K = 1357;
    private static final int DEADLINE = 9700;
    private static final BlockPos HEATER = new BlockPos(2, 1, 3);
    private static final BlockPos CRUCIBLE = HEATER.above();
    private static final BlockPos MOLD = CRUCIBLE.east();

    private ForgeBronzeChainTests() {}

    @GameTest(template = "test_bronze_chain", timeoutTicks = 9800)
    public static void coalCopperTinToFourBronzeIngots(GameTestHelper helper) {
        new BronzeFlow(helper).start();
    }

    /** One cold crucible above an air gap; another receives seventeen real copper ingots. */
    @GameTest(template = "test_bronze_chain", timeoutTicks = 480)
    public static void disconnectedHeatAndFullCrucibleRejectWithoutLoss(GameTestHelper helper) {
        BlockPos heaterPos = new BlockPos(2, 1, 2);
        BlockPos coldPos = heaterPos.above(2);
        BlockPos fullPos = new BlockPos(6, 2, 2);
        helper.setBlock(heaterPos.below(), Blocks.STONE);
        helper.setBlock(heaterPos.above(), Blocks.AIR);
        helper.setBlock(heaterPos, GTMachines.BURNING_BOX_SOLID_BRICK.get().defaultBlockState()
                .setValue(GTFacingMachineBlock.FACING, Direction.NORTH));
        helper.setBlock(coldPos, GTMachines.SMELTING_CRUCIBLE_CERAMIC.get());
        helper.setBlock(fullPos.below(), Blocks.STONE);
        helper.setBlock(fullPos, GTMachines.SMELTING_CRUCIBLE_CERAMIC.get());
        var heater = (SolidBurningBoxBlockEntity) helper.getBlockEntity(heaterPos);
        var cold = (SmeltingCrucibleBlockEntity) helper.getBlockEntity(coldPos);
        var full = (SmeltingCrucibleBlockEntity) helper.getBlockEntity(fullPos);
        Player player = helper.makeMockSurvivalPlayer();
        ItemEntity charge = dropCharge(helper, full, ingots(helper, Materials.Copper, 17), 0.5);
        try {
            helper.assertTrue(cold.getTemperature() == 293 && full.getTemperature() == 293,
                    "New ceramic crucibles must start cold at 293 K");
            fuelAndLight(helper, player, heater, 2);
        } catch (RuntimeException failure) {
            helper.setBlock(heaterPos, Blocks.AIR);
            throw failure;
        }
        helper.runAtTickTime(400, () -> {
            try {
                helper.assertTrue(heater.isBurning() && heater.getFuelStack().getCount() < 2,
                        "Disconnected heater must actually burn coal: " + heatState(heater));
                helper.assertTrue(cold.getEnergyBuffer() == 0 && cold.getTemperature() <= 293
                                && cold.getContentView().isEmpty(),
                        "One-block air gap must not conduct HU: " + crucibleState(cold));
                helper.assertTrue(full.getCrucibleContentAmount() == 16 * GTValues.U
                                && amount(full.getContentView(), Materials.Copper) == 16 * GTValues.U
                                && full.getCacheStack().getCount() == 1 && !charge.isAlive(),
                        "Capacity must retain the seventeenth ingot in the top cache: " + crucibleState(full));
                helper.assertTrue(full.getCrucibleContentAmount() + itemUnits(full.getCacheStack()) == 17 * GTValues.U,
                        "Capacity rejection must conserve all seventeen ingots");
                helper.assertTrue(full.getEnergyBuffer() == 0 && full.getTemperature() <= 293,
                        "Unconnected full crucible cannot acquire heat: " + crucibleState(full));
                GregTech.LOGGER.info("GT6_FORGE_PLAYFLOW_GATES tick={} disconnected={} capacity={}",
                        helper.getTick(), crucibleState(cold), crucibleState(full));
                helper.succeed();
            } finally {
                helper.setBlock(heaterPos, Blocks.AIR);
            }
        });
    }

    private static final class BronzeFlow {
        private final GameTestHelper helper;
        private final Player player;
        private final ItemStack chisel;
        private final ItemStack pincers;
        private SolidBurningBoxBlockEntity heater;
        private SmeltingCrucibleBlockEntity crucible;
        private MoldBlockEntity mold;
        private List<ItemEntity> charges;
        private int phase;
        private int castCount;
        private long previousObservedK;
        private long highestObservedK;
        private long alloyTick = -1;
        private boolean observedColdIngredients;
        private boolean observedLiveHeat;
        private boolean observedSubThreshold;
        private boolean removedHeater;
        private boolean finished;

        private BronzeFlow(GameTestHelper helper) {
            this.helper = helper;
            player = helper.makeMockSurvivalPlayer();
            chisel = GTToolItem.create(GTToolType.CHISEL, Materials.Steel, GTMaterialRegistry.get("Wood"));
            pincers = GTToolItem.create(GTToolType.PINCERS, Materials.Steel, GTMaterialRegistry.get("Wood"));
        }

        private void start() {
            try {
                helper.setBlock(HEATER.below(), Blocks.STONE);
                helper.setBlock(MOLD.below(), Blocks.STONE);
                helper.setBlock(HEATER, GTMachines.BURNING_BOX_SOLID_BRICK.get().defaultBlockState()
                        .setValue(GTFacingMachineBlock.FACING, Direction.NORTH));
                helper.setBlock(CRUCIBLE, GTMachines.SMELTING_CRUCIBLE_CERAMIC.get());
                Block moldBlock = ForgeRegistries.BLOCKS.getValue(GregTech.id("mold_ceramic"));
                helper.assertTrue(moldBlock != null && moldBlock != Blocks.AIR, "Formal mold_ceramic must exist");
                helper.setBlock(MOLD, moldBlock);
                heater = (SolidBurningBoxBlockEntity) helper.getBlockEntity(HEATER);
                crucible = (SmeltingCrucibleBlockEntity) helper.getBlockEntity(CRUCIBLE);
                mold = (MoldBlockEntity) helper.getBlockEntity(MOLD);
                check(crucible.getTemperature() == 293 && mold.getTemperature() == 293,
                        "New crucible and mold must start at 293 K");
                check(!heater.isBurning() && heater.getStoredHeat() == 0 && crucible.getEnergyBuffer() == 0,
                        "Cold equipment must have no supplied heat");
                check(!chisel.isEmpty() && !pincers.isEmpty() && !player.isCreative(),
                        "Given steel interaction tools must be registered and player must consume resources");
                carveIngot(helper, player, mold, chisel);
                check(chisel.getDamageValue() == 15, "Fifteen actual chisel clicks must wear the tool fifteen times");
                check(mold.getMoldRecipePrefix() == MaterialPrefix.ingot
                                && mold.getMoldRequiredMaterialUnits() == GTValues.U,
                        "Carved fifteen-cell shape must be the registered one-U ingot mold");
                charges = List.of(dropCharge(helper, crucible, ingots(helper, Materials.Copper, 3), 0.35),
                        dropCharge(helper, crucible, ingots(helper, Materials.Tin, 1), 0.65));
                previousObservedK = crucible.getTemperature();
                helper.onEachTick(this::tick);
            } catch (RuntimeException failure) {
                cleanupHeat();
                throw failure;
            }
        }

        private void tick() {
            if (finished) return;
            try {
                check(helper.getBlockEntity(CRUCIBLE) == crucible && helper.getBlockEntity(MOLD) == mold,
                        "Equipment must survive without meltdown");
                highestObservedK = Math.max(highestObservedK, crucible.getTemperature());
                assertConservation();
                long bronze = amount(crucible.getContentView(), Materials.Bronze);
                if (crucible.getTemperature() < BRONZE_REACTION_K) {
                    observedSubThreshold = true;
                    check(bronze == 0 && mold.getMoldContentAmount() == 0 && bronzeCount(player) == 0,
                            "Bronze must not appear before the 1357 K copper/tin reaction threshold");
                }
                if (phase == 0) {
                    if (amount(crucible.getContentView(), Materials.Copper) == 3 * GTValues.U
                            && amount(crucible.getContentView(), Materials.Tin) == GTValues.U
                            && crucible.getCacheStack().isEmpty() && charges.stream().noneMatch(ItemEntity::isAlive)) {
                        observedColdIngredients = true;
                        check(crucible.getTemperature() <= 293 && crucible.getEnergyBuffer() == 0,
                                "Top item entities must load three copper and one tin while still cold");
                        fuelAndLight(helper, player, heater, 8);
                        phase = 1;
                    }
                    check(helper.getTick() < 100, "Top item entities did not enter the crucible cache in time");
                } else if (phase == 1) {
                    observedLiveHeat |= heater.isBurning() && heater.getFuelStack().getCount() < 8
                            && crucible.getTemperature() > 293;
                    if (bronze > 0) {
                        check(previousObservedK >= BRONZE_REACTION_K && bronze == 4 * GTValues.U
                                        && crucible.getContentView().size() == 1,
                                "Natural tick reaction must consume all copper/tin and create exactly four U bronze");
                        check(observedLiveHeat && observedColdIngredients && observedSubThreshold,
                                "Reaction must follow observed cold loading and actual coal/HU heating");
                        alloyTick = helper.getTick();
                        GregTech.LOGGER.info("GT6_FORGE_PLAYFLOW_ALLOY {}", state());
                        // Removing the spent heat source prevents a finished test from heating its neighbours.
                        cleanupHeat();
                        player.setItemInHand(InteractionHand.MAIN_HAND, pincers);
                        phase = 2;
                    }
                } else if (phase == 2) {
                    castOneOrPickup();
                    if (castCount == 4 && bronzeCount(player) == 4) {
                        check(pincers.getDamageValue() == 4, "Four hot-solid pickups must wear pincers four times");
                        check(crucible.getContentView().isEmpty() && crucible.getCacheStack().isEmpty()
                                        && mold.getMoldContentAmount() == 0 && mold.getSolidOutput().isEmpty(),
                                "All four bronze units must end in the player's four real ingots");
                        check(removedHeater && highestObservedK >= BRONZE_REACTION_K, "Actual heating must have crossed 1357 K");
                        phase = 3;
                    }
                } else if (phase == 3) {
                    // An extra normal interaction/tick must not mint a fifth ingot from empty equipment.
                    moldClick(player, mold, 0.5, 0.5);
                    check(bronzeCount(player) == 4 && mold.getMoldContentAmount() == 0
                                    && crucible.getContentView().isEmpty(), "Empty repeated pickup/pour must not duplicate bronze");
                    assertConservation();
                    finished = true;
                    GregTech.LOGGER.info("GT6_FORGE_PLAYFLOW_COMPLETE {}", state());
                    helper.succeed();
                }
                previousObservedK = crucible.getTemperature();
                check(helper.getTick() < DEADLINE || finished, "Bronze workflow exceeded its finite tick budget");
            } catch (RuntimeException failure) {
                cleanupHeat();
                finished = true;
                throw failure;
            }
        }

        private void castOneOrPickup() {
            if (mold.getMoldContentAmount() == 0) {
                long before = crucible.getCrucibleContentAmount();
                check(before > 0 && crucible.getTemperature() >= Materials.Bronze.getMeltingPoint(),
                        "Remaining bronze must still be molten for the next real pour");
                moldClick(player, mold, 0.5, 0.5);
                check(mold.getMoldContentMaterial() == Materials.Bronze.resolve()
                                && mold.getMoldContentAmount() == GTValues.U
                                && crucible.getCrucibleContentAmount() == before - GTValues.U,
                        "Real right click must transfer exactly one bronze unit from crucible into mold");
                castCount++;
                // A filled mold must refuse another real pour without consuming a second unit.
                long retained = crucible.getCrucibleContentAmount();
                moldClick(player, mold, 0.5, 0.5);
                check(crucible.getCrucibleContentAmount() == retained && mold.getMoldContentAmount() == GTValues.U,
                        "Occupied mold must reject another pour without loss");
            } else if (mold.isContentSolidified()) {
                check(mold.getTemperature() < Materials.Bronze.getMeltingPoint(),
                        "Only naturally cooled solid bronze may be retrieved");
                int before = bronzeCount(player);
                moldClick(player, mold, 0.5, 0.5);
                check(bronzeCount(player) == before + 1 && mold.getMoldContentAmount() == 0,
                        "Pincers interaction must deliver one real bronze ingot and empty the mold");
            }
            assertConservation();
        }

        private void assertConservation() {
            for (CrucibleMaterialStack stack : crucible.getContentView()) {
                check(stack.material.resolve() == Materials.Copper.resolve()
                                || stack.material.resolve() == Materials.Tin.resolve()
                                || stack.material.resolve() == Materials.Bronze.resolve(),
                        "Copper/tin chain must not contain unrelated materials");
            }
            long units = crucible.getCrucibleContentAmount() + itemUnits(crucible.getCacheStack())
                    + mold.getMoldContentAmount() + itemUnits(mold.getSolidOutput())
                    + (long) bronzeCount(player) * GTValues.U;
            for (ItemEntity charge : charges) if (charge.isAlive()) units += itemUnits(charge.getItem());
            check(units == 4 * GTValues.U, "Four input units must be conserved across entities/cache/crucible/mold/player: " + units);
        }

        private void check(boolean condition, String message) {
            helper.assertTrue(condition, message + " | " + state());
        }

        private String state() {
            return "tick=" + helper.getTick() + ", phase=" + phase + ", cast=" + castCount
                    + ", alloyTick=" + alloyTick + ", maxK=" + highestObservedK
                    + ", previousK=" + previousObservedK + ", " + heatState(heater)
                    + ", " + crucibleState(crucible) + ", mold=" + (mold == null ? "unplaced"
                    : (mold.getMoldContentMaterial() == null ? "empty" : mold.getMoldContentMaterial().getName())
                    + "/" + mold.getMoldContentAmount() + "/"
                    + mold.getTemperature() + "K/solid=" + mold.isContentSolidified()
                    + "/shape=" + mold.getMoldShape()) + ", playerBronze=" + bronzeCount(player);
        }

        private void cleanupHeat() {
            if (!removedHeater) {
                helper.setBlock(HEATER, Blocks.AIR);
                removedHeater = true;
            }
        }
    }

    private static void carveIngot(GameTestHelper helper, Player player, MoldBlockEntity mold, ItemStack chisel) {
        helper.assertTrue(mold.getMoldShape() == 0, "Given mold starts uncarved");
        player.setItemInHand(InteractionHand.MAIN_HAND, chisel);
        int expectedMask = 0;
        for (int z = 0; z < 5; z++) {
            for (int x = 0; x < 3; x++) {
                double hitX = 2 / 16.0 + (x + 0.5) * (12 / 16.0 / 5);
                double hitZ = 2 / 16.0 + (z + 0.5) * (12 / 16.0 / 5);
                moldClick(player, mold, hitX, hitZ);
                expectedMask |= 1 << (z * 5 + x);
                helper.assertTrue(mold.getMoldShape() == expectedMask,
                        "Real chisel right-click must carve precisely cell " + x + "," + z);
            }
        }
    }

    private static InteractionResult moldClick(Player player, MoldBlockEntity mold, double hitX, double hitZ) {
        BlockPos pos = mold.getBlockPos();
        var hit = new BlockHitResult(new Vec3(pos.getX() + hitX, pos.getY() + 3 / 16.0,
                pos.getZ() + hitZ), Direction.UP, pos, false);
        var event = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos, hit);
        SmelteryInteractionHandler.onRightClickBlock(event);
        return event.isCanceled() ? event.getCancellationResult() : InteractionResult.PASS;
    }

    private static void fuelAndLight(GameTestHelper helper, Player player, SolidBurningBoxBlockEntity heater, int coal) {
        BlockPos pos = heater.getBlockPos();
        var hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0, -0.5), Direction.NORTH, pos, false);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COAL, coal));
        var state = heater.getBlockState();
        state.getBlock().use(state, helper.getLevel(), pos, player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(player.getMainHandItem().isEmpty() && heater.getFuelStack().getCount() == coal,
                "Actual front interaction must consume coal into the burning box: " + heatState(heater));
        ItemStack flint = new ItemStack(Items.FLINT_AND_STEEL);
        player.setItemInHand(InteractionHand.MAIN_HAND, flint);
        state.getBlock().use(state, helper.getLevel(), pos, player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(heater.isBurning() && flint.getDamageValue() == 1,
                "Actual flint-and-steel front interaction must ignite and wear: " + heatState(heater));
    }

    private static ItemEntity dropCharge(GameTestHelper helper, SmeltingCrucibleBlockEntity crucible, ItemStack stack, double x) {
        BlockPos pos = crucible.getBlockPos();
        var entity = new ItemEntity(helper.getLevel(), pos.getX() + x, pos.getY() + 1.1, pos.getZ() + 0.5, stack);
        entity.setDeltaMovement(Vec3.ZERO);
        entity.setDefaultPickUpDelay();
        helper.assertTrue(helper.getLevel().addFreshEntity(entity), "Given registered ingots must spawn above the crucible");
        return entity;
    }

    private static ItemStack ingots(GameTestHelper helper, GTMaterial material, int count) {
        ItemStack stack = GTItems.getStack(MaterialPrefix.ingot, material, count);
        helper.assertTrue(!stack.isEmpty() && stack.getItem() instanceof MaterialItem item
                        && item.getPrefix() == MaterialPrefix.ingot && item.getMaterial().resolve() == material.resolve(),
                "Given charge must be a formal registered " + material.getName() + " ingot");
        return stack;
    }

    private static long itemUnits(ItemStack stack) {
        return stack.isEmpty() ? 0 : CrucibleMaterialStack.total(CrucibleItemInput.parse(stack)) * stack.getCount();
    }

    private static long amount(List<CrucibleMaterialStack> stacks, GTMaterial material) {
        return stacks.stream().filter(stack -> stack.material.resolve() == material.resolve()).mapToLong(stack -> stack.amount).sum();
    }

    private static int bronzeCount(Player player) {
        ItemStack bronze = GTItems.getStack(MaterialPrefix.ingot, Materials.Bronze, 1);
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && ItemStack.isSameItemSameTags(stack, bronze)) count += stack.getCount();
        }
        return count;
    }

    private static String heatState(SolidBurningBoxBlockEntity heater) {
        return heater == null ? "heater=unplaced" : "heater=" + heater.isBurning() + "/"
                + heater.getStoredHeat() + "HU/fuel=" + heater.getFuelStack() + "/ash=" + heater.getAshStack();
    }

    private static String crucibleState(SmeltingCrucibleBlockEntity crucible) {
        return crucible == null ? "crucible=unplaced" : "crucible=" + crucible.getTemperature()
                + "K/" + crucible.getEnergyBuffer() + "HU/content=" + crucible.getContentView().stream()
                .map(stack -> stack.material.getName() + "/" + stack.amount).toList()
                + "/cache=" + crucible.getCacheStack();
    }
}
