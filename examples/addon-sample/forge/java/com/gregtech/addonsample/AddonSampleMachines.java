package com.gregtech.addonsample;

import com.gregtech.gregtech.api.addon.AddonMachineContributor;
import com.gregtech.gregtech.api.addon.BehaviorMachineDeclaration;
import com.gregtech.gregtech.api.addon.CompressedGasTankDeclaration;
import com.gregtech.gregtech.api.addon.FluidPipeDeclaration;
import com.gregtech.gregtech.api.addon.ItemPipeDeclaration;
import com.gregtech.gregtech.api.addon.FuelGeneratorDeclaration;
import com.gregtech.gregtech.api.addon.FuelMapDeclaration;
import com.gregtech.gregtech.api.addon.MachineDeclaration;
import com.gregtech.gregtech.api.addon.MachineLayout;
import com.gregtech.gregtech.api.addon.RecipeMapDeclaration;
import com.gregtech.gregtech.api.addon.StructurePartDeclaration;
import com.gregtech.gregtech.api.addon.StructurePartEnergy;
import com.gregtech.gregtech.api.addon.StructureRole;
import com.gregtech.gregtech.api.addon.platform.AddonFluids;
import com.gregtech.gregtech.api.addon.platform.AddonRecipes;
import com.gregtech.gregtech.api.addon.platform.BehaviorMachineContributor;
import com.gregtech.gregtech.api.addon.platform.MachineBehavior;
import com.gregtech.gregtech.api.addon.platform.MachineBehaviorContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Sample press, detector, tank-wall multiblock, irrigator, miner, void hopper, rotation buffer, hand crank, transformer, solid boiler, gas compressor, compressed drum and pipes. */
public final class AddonSampleMachines implements AddonMachineContributor, BehaviorMachineContributor {
    @Override public String addonId() { return "addon_sample"; }

    @Override public List<RecipeMapDeclaration> recipeMaps() {
        return List.of(
                map("addon_sample.recipe.press", "Sample Press", "press", 2),
                new RecipeMapDeclaration("addon_sample.recipe.large_press", "Large Sample Press",
                        "addon_sample:textures/gui/machines/large_press",
                        1, 6, 1, 0, 1, 0, 1, true, false, false, false),
                new RecipeMapDeclaration("addon_sample.recipe.detector", "Sample Detector",
                        "addon_sample:textures/gui/machines/press",
                        1, 2, 1, 1, 1, 0, 1, true, false, false, false));
    }

    @Override public List<MachineDeclaration> machines() {
        return List.of(
                MachineDeclaration.builder("sample_press", "Steel")
                        .machineType("samplepress").energy("EU", 32).tier(1)
                        .recipeMap("addon_sample.recipe.press").basic()
                        .itemInputs(BehaviorMachineDeclaration.Face.TOP)
                        .itemOutputs(BehaviorMachineDeclaration.Face.BOTTOM)
                        .fluidInputs(BehaviorMachineDeclaration.Face.LEFT)
                        .fluidOutputs(BehaviorMachineDeclaration.Face.RIGHT)
                        .energyInputs(BehaviorMachineDeclaration.Face.BACK)
                        .itemAutoOutput(BehaviorMachineDeclaration.Face.BOTTOM)
                        .cheapOverclocking(true)
                        .minimumLightAbove(14)
                        .build(),
                MachineDeclaration.builder("sample_detector", "Steel")
                        .machineType("sampledetector").energy("EU", 32).tier(1)
                        .recipeMap("addon_sample.recipe.detector").basic()
                        .searchOutputTanks(true)
                        .exclusiveOutputs(true)
                        .build(),
                MachineDeclaration.builder("large_sample_press", "Steel")
                        .machineType("largesamplepress").energy("EU", 512).tier(1)
                        .recipeMap("addon_sample.recipe.large_press").largeRecipe()
                        .energyRange(256, 2048)
                        .structureKeys(List.of("addon_sample.structure.press.1"))
                        .itemOutputTarget(2, 0, 1, BehaviorMachineDeclaration.Face.RIGHT)
                        .fluidOutputTarget(-2, 0, 1, BehaviorMachineDeclaration.Face.LEFT)
                        .build());
    }

    @Override public List<StructurePartDeclaration> parts() {
        return List.of(
                new StructurePartDeclaration("sample_wall", "Steel", "", 6.0F),
                new StructurePartDeclaration("sample_emitter", "Steel", "", 6.0F,
                        new StructurePartEnergy("EU", 100_000L,
                                List.of(BehaviorMachineDeclaration.Face.FRONT), 32, 4)));
    }

    @Override public List<FluidPipeDeclaration> fluidPipes() {
        return List.of(new FluidPipeDeclaration("teflon", "Teflon", 125, true, true, false, false, 600, 0));
    }

    @Override public List<ItemPipeDeclaration> itemPipes() {
        return List.of(new ItemPipeDeclaration("pvc", "PVC", 32768, 1));
    }

    @Override public List<CompressedGasTankDeclaration> compressedGasTanks() {
        return List.of(new CompressedGasTankDeclaration("sample_drum", "Steel", 12_800_000, false, false, false));
    }

    @Override public List<MachineLayout> layouts() {
        var cells = new ArrayList<MachineLayout.Cell>();
        for (int up = 0; up < 2; up++) {
            for (int right = -1; right <= 1; right++) {
                for (int back = 0; back <= 2; back++) {
                    if (right == 0 && up == 0 && back == 0) continue;
                    var role = back == 1 && right == 0
                            ? StructureRole.ENERGY_INPUT : StructureRole.ITEM_FLUID_IO;
                    String part = right == 1 && up == 0 && back == 0 ? "sample_wall" : "tank_wall";
                    cells.add(new MachineLayout.Cell(right, up, back, part, role));
                }
            }
        }
        return List.of(new MachineLayout("largesamplepress", cells),
                new MachineLayout("sample_turbine", List.of(
                        new MachineLayout.Cell(0, 0, 1, "tank_wall", StructureRole.FLUID_INPUT))),
                new MachineLayout("sample_transformer", List.of(
                        new MachineLayout.Cell(0, 0, 1, "sample_emitter", StructureRole.ENERGY_OUTPUT))));
    }

    @Override public List<FuelMapDeclaration> fuelMaps() {
        return List.of(new FuelMapDeclaration("addon_sample.fuel.gas", "Sample Gas",
                "addon_sample:textures/gui/machines/press",
                0, 0, 0, 1, 1, 1, 1, false, false, false, false),
                new FuelMapDeclaration("addon_sample.fuel.solid", "Sample Solid",
                        "addon_sample:textures/gui/machines/press",
                        1, 1, 1, 0, 0, 0, 1, true, false, false, false));
    }

    @Override public List<FuelGeneratorDeclaration> fuelGenerators() {
        return List.of(
                FuelGeneratorDeclaration.builder("sample_engine", "Steel")
                        .machineType("sample_engine").fuelMap("addon_sample.fuel.gas")
                        .efficiency(10_000).energy("RU", 32).tank(4_000).outputFront().single().build(),
                FuelGeneratorDeclaration.builder("sample_turbine", "Steel")
                        .machineType("sample_turbine").fuelMap("addon_sample.fuel.gas")
                        .efficiency(5_000).energy("RU", 32).tank(8_000).outputBehind(4).multi().build());
    }

    @Override public List<BehaviorMachineDeclaration> behaviorMachines() {
        return List.of(IRRIGATOR, MINER, VOID_HOPPER, ROTATION_BUFFER, HAND_CRANK, TRANSFORMER, SOLID_BOILER, COMPRESSOR);
    }

    @Override public Set<String> machineTypes() {
        return Set.of("irrigator", "sampleminer", "samplehopper", "ru_buffer", "hand_crank",
                "sample_transformer", "sampleboiler", "samplecompressor");
    }

    @Override public MachineBehavior create(String machineType) {
        return switch (machineType) {
            case "irrigator" -> new Irrigator();
            case "sampleminer" -> new Miner();
            case "samplehopper" -> new VoidHopper();
            case "ru_buffer" -> new RotationBuffer();
            case "hand_crank" -> new HandCrank();
            case "sample_transformer" -> new Transformer();
            case "sampleboiler" -> new SolidBoiler();
            case "samplecompressor" -> new Compressor();
            default -> throw new IllegalArgumentException(machineType);
        };
    }

    private static RecipeMapDeclaration map(String name, String title, String gui) {
        return map(name, title, gui, 1);
    }

    private static RecipeMapDeclaration map(String name, String title, String gui, int inputItems) {
        return new RecipeMapDeclaration(name, title, "addon_sample:textures/gui/machines/" + gui,
                inputItems, 1, 1, 0, 0, 0, 1, true, false, false, false);
    }

    private static final BehaviorMachineDeclaration IRRIGATOR = BehaviorMachineDeclaration.builder("irrigator", "Steel")
            .machineType("irrigator").energy("EU", 32).tier(1)
            .gui("addon_sample:textures/gui/machine.png")
            .item(44, 26, BehaviorMachineDeclaration.SlotRole.INPUT)
            .item(116, 26, BehaviorMachineDeclaration.SlotRole.OUTPUT)
            .fluid(8000, 152, 8, BehaviorMachineDeclaration.SlotRole.INPUT)
            .toggle("addon_sample.irrigator.water", 26, 64, true)
            .build();

    /** Breaks one block under itself per tick and keeps the scan index. */
    private static final BehaviorMachineDeclaration MINER = BehaviorMachineDeclaration.builder("sample_miner", "Steel")
            .machineType("sampleminer").energy("EU", 32).tier(1)
            .gui("addon_sample:textures/gui/machine.png")
            .item(116, 26, BehaviorMachineDeclaration.SlotRole.OUTPUT)
            .maxRange(2)
            .build();

    /** Pulls item entities into the output slot, or voids them while the switch is on. */
    private static final BehaviorMachineDeclaration VOID_HOPPER = BehaviorMachineDeclaration.builder("sample_hopper", "Steel")
            .machineType("samplehopper").energy("EU", 32).tier(1)
            .gui("addon_sample:textures/gui/machine.png")
            .item(116, 26, BehaviorMachineDeclaration.SlotRole.OUTPUT)
            .toggle("addon_sample.hopper.void", 26, 64, false)
            .maxRange(4)
            .build();

    /** Stores RU from the sides and, while the switch is on, emits one packet from the back. */
    private static final BehaviorMachineDeclaration ROTATION_BUFFER = BehaviorMachineDeclaration.builder("ru_buffer", "Steel")
            .machineType("ru_buffer").energy("RU", 32).tier(1)
            .gui("addon_sample:textures/gui/machine.png")
            .energyFaces(List.of(BehaviorMachineDeclaration.Face.LEFT, BehaviorMachineDeclaration.Face.RIGHT,
                    BehaviorMachineDeclaration.Face.TOP, BehaviorMachineDeclaration.Face.BOTTOM))
            .energyCapacity(32_000)
            .energyOutputs(BehaviorMachineDeclaration.Face.BACK)
            .energyOut("RU").energyOutMax(32, 1)
            .toggle("addon_sample.ru_buffer.release", 116, 64, false)
            .build();

    /** Receives EU on the free faces and moves it into the emitter one block behind. */
    private static final BehaviorMachineDeclaration TRANSFORMER = BehaviorMachineDeclaration.builder("sample_transformer", "Steel")
            .machineType("sample_transformer").energy("EU", 32).tier(1)
            .gui("addon_sample:textures/gui/machine.png")
            .energyFaces(List.of(BehaviorMachineDeclaration.Face.FRONT, BehaviorMachineDeclaration.Face.LEFT,
                    BehaviorMachineDeclaration.Face.RIGHT, BehaviorMachineDeclaration.Face.TOP,
                    BehaviorMachineDeclaration.Face.BOTTOM))
            .energyCapacity(32_000)
            .build();

    /** A click stores one RU packet and pushes it out the front. */
    private static final BehaviorMachineDeclaration HAND_CRANK = BehaviorMachineDeclaration.builder("hand_crank", "Steel")
            .machineType("hand_crank").energy("RU", 32).tier(1)
            .gui("addon_sample:textures/gui/machine.png")
            .energyFaces(List.of(BehaviorMachineDeclaration.Face.BOTTOM))
            .energyCapacity(32)
            .energyOutputs(BehaviorMachineDeclaration.Face.FRONT)
            .energyOut("RU").energyOutMax(32, 1)
            .build();

    /** Coal in, charcoal out. Produced EU leaves through the top. */
    private static final BehaviorMachineDeclaration SOLID_BOILER = BehaviorMachineDeclaration.builder("sample_boiler", "Steel")
            .machineType("sampleboiler").energy("EU", 32).tier(1)
            .gui("addon_sample:textures/gui/machine.png")
            .item(44, 26, BehaviorMachineDeclaration.SlotRole.INPUT)
            .item(116, 26, BehaviorMachineDeclaration.SlotRole.OUTPUT)
            .energyFaces(List.of(BehaviorMachineDeclaration.Face.BOTTOM, BehaviorMachineDeclaration.Face.LEFT,
                    BehaviorMachineDeclaration.Face.RIGHT, BehaviorMachineDeclaration.Face.BACK))
            .energyCapacity(10_000)
            .energyOutputs(BehaviorMachineDeclaration.Face.TOP)
            .energyOut("EU").energyOutMax(32, 1)
            .recipeMaps("addon_sample.fuel.solid")
            .build();

    /** KU on the bottom, gas in the tank, compressed drum on the back. */
    private static final BehaviorMachineDeclaration COMPRESSOR = BehaviorMachineDeclaration.builder("sample_compressor", "Steel")
            .machineType("samplecompressor").energy("KU", 32).tier(1)
            .gui("addon_sample:textures/gui/machine.png")
            .energyFaces(List.of(BehaviorMachineDeclaration.Face.BOTTOM))
            .fluid(8000, 152, 8, BehaviorMachineDeclaration.SlotRole.INPUT)
            .build();

    /** Waters one dry farmland within two blocks, spending water and EU. */
    private static final class Irrigator implements MachineBehavior {
        @Override public boolean canInsert(MachineBehaviorContext context, int slot, ItemStack stack) {
            return stack != null && stack.is(Items.BONE_MEAL);
        }

        @Override public boolean canFill(MachineBehaviorContext context, int tank, FluidStack stack) {
            return stack != null && !stack.isEmpty() && stack.getFluid() == Fluids.WATER;
        }

        @Override public void tick(MachineBehaviorContext context) {
            if (!context.toggle(0)) {
                context.progress(0);
                context.state().setInt("cycle", 0);
                return;
            }
            FluidStack water = context.fluid(0);
            if (context.energy() < 8 || water.isEmpty() || water.getAmount() < 100) {
                context.progress(0);
                context.state().setInt("cycle", 0);
                return;
            }
            int cycle = context.state().getInt("cycle") + 1;
            context.working(true);
            context.progress(cycle / 10.0F);
            if (cycle < 10) {
                context.state().setInt("cycle", cycle);
                return;
            }
            BlockPos dry = dryFarmland(context);
            context.state().setInt("cycle", 0);
            context.progress(0);
            if (dry == null || !context.consume(8)) return;
            context.drain(0, 100, false);
            var state = context.level().getBlockState(dry);
            context.level().setBlock(dry, state.setValue(FarmBlock.MOISTURE, 7), 3);
            context.state().setInt("watered", context.state().getInt("watered") + 1);
        }

        private static BlockPos dryFarmland(MachineBehaviorContext context) {
            BlockPos origin = context.pos();
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos pos = origin.offset(dx, dy, dz);
                        var state = context.level().getBlockState(pos);
                        if (state.is(Blocks.FARMLAND) && state.getValue(FarmBlock.MOISTURE) < 7) return pos;
                    }
                }
            }
            return null;
        }
    }

    /** Walks the cube under the machine. Air is skipped. One break spends 16 EU. */
    private static final class Miner implements MachineBehavior {
        @Override public boolean canInsert(MachineBehaviorContext context, int slot, ItemStack stack) { return false; }
        @Override public boolean canFill(MachineBehaviorContext context, int tank, FluidStack stack) { return false; }

        @Override public void tick(MachineBehaviorContext context) {
            if (context.energy() < 16) {
                context.progress(0);
                return;
            }
            int range = 2;
            int step = context.state().getInt("step");
            int width = range * 2 + 1;
            int span = range * width * width;
            for (int guard = 0; guard < 64; guard++) {
                int index = Math.floorMod(step, span);
                int layer = width * width;
                int dy = -1 - index / layer;
                int rest = index % layer;
                BlockPos pos = context.pos().offset(rest / width - range, dy, rest % width - range);
                if (!context.level().hasChunkAt(pos)) break;
                var state = context.level().getBlockState(pos);
                if (state.isAir() || state.getDestroySpeed(context.level(), pos) < 0) {
                    step++;
                    continue;
                }
                context.working(true);
                if (!context.breakBlock(pos)) break;
                context.consume(16);
                step++;
                break;
            }
            context.state().setInt("step", step);
            context.progress(span == 0 ? 0 : Math.floorMod(step, span) / (float) span);
        }
    }

    /** Collects item entities in a 4-block cube. The switch discards them instead. */
    private static final class VoidHopper implements MachineBehavior {
        @Override public boolean canInsert(MachineBehaviorContext context, int slot, ItemStack stack) { return false; }
        @Override public boolean canFill(MachineBehaviorContext context, int tank, FluidStack stack) { return false; }

        @Override public void tick(MachineBehaviorContext context) {
            if (context.energy() < 8) return;
            int taken = context.collectItems(4, context.toggle(0));
            if (taken > 0) {
                context.consume(8);
                context.working(true);
            }
        }
    }

    /** Keeps RU and releases one packet per tick from the back while the switch is on. */
    private static final class RotationBuffer implements MachineBehavior {
        @Override public boolean canInsert(MachineBehaviorContext context, int slot, ItemStack stack) { return false; }
        @Override public boolean canFill(MachineBehaviorContext context, int tank, FluidStack stack) { return false; }

        @Override public void tick(MachineBehaviorContext context) {
            if (context.toggle(0) && context.energy() >= 32) context.emit(32, 1);
        }
    }

    /** Right-click produces one RU packet and pushes it out the front. */
    private static final class HandCrank implements MachineBehavior {
        @Override public boolean canInsert(MachineBehaviorContext context, int slot, ItemStack stack) { return false; }
        @Override public boolean canFill(MachineBehaviorContext context, int tank, FluidStack stack) { return false; }

        @Override public void tick(MachineBehaviorContext context) {
            if (context.energy() >= 32) context.emit(32, 1);
        }

        @Override public boolean onUse(MachineBehaviorContext context, Player player, InteractionHand hand) {
            context.generate(32);
            if (context.energy() >= 32) context.emit(32, 1);
            return true;
        }
    }

    /** Moves stored EU into the emitter behind the controller and arms a 32 EU packet. */
    private static final class Transformer implements MachineBehavior {
        @Override public boolean canInsert(MachineBehaviorContext context, int slot, ItemStack stack) { return false; }
        @Override public boolean canFill(MachineBehaviorContext context, int tank, FluidStack stack) { return false; }

        @Override public void tick(MachineBehaviorContext context) {
            if (!context.structureFormed()) return;
            for (var part : context.energyParts()) {
                long room = part.capacity() - part.stored();
                long move = Math.min(context.energy(), Math.max(0, room));
                if (move > 0 && context.consume(move)) part.charge(move);
                if (part.stored() > 0) part.setOutput(32, 1);
            }
        }
    }

    /** Offers stored gas to the drum behind this block. The KU is spent even when the drum takes nothing. */
    private static final class Compressor implements MachineBehavior {
        @Override public boolean canInsert(MachineBehaviorContext context, int slot, ItemStack stack) { return false; }

        @Override public boolean canFill(MachineBehaviorContext context, int tank, FluidStack stack) {
            return AddonFluids.gas(stack);
        }

        @Override public void tick(MachineBehaviorContext context) {
            long ku = context.energy();
            if (ku <= 0) {
                context.working(false);
                return;
            }
            FluidStack stored = context.fluid(0);
            int offer = stored == null || stored.isEmpty() ? 0
                    : (int) Math.min(stored.getAmount(), Math.min(Integer.MAX_VALUE, ku * 10L));
            int storedAmount = 0;
            if (offer > 0) {
                FluidStack pushed = stored.copy();
                pushed.setAmount(offer);
                Direction back = context.facing().getOpposite();
                storedAmount = context.pushCompressedGas(back, pushed);
                if (storedAmount > 0) context.drain(0, storedAmount, false);
            }
            context.consume(ku);
            context.working(storedAmount > 0);
        }
    }

    /**
     * Looks up {@code addon_sample.fuel.solid}, spends the coal, stores the charcoal,
     * then generates the row's produced EU and emits it from the top.
     * {@code Line.euPerTick} on a fuel row is negative.
     */
    private static final class SolidBoiler implements MachineBehavior {
        private static final String MAP = "addon_sample.fuel.solid";
        private static final long PACKET = 32;

        @Override public boolean canInsert(MachineBehaviorContext context, int slot, ItemStack stack) {
            return stack != null && !stack.isEmpty();
        }

        @Override public boolean canFill(MachineBehaviorContext context, int tank, FluidStack stack) { return false; }

        @Override public void tick(MachineBehaviorContext context) {
            var state = context.state();
            int left = state.getInt("left");
            long eu = state.getLong("eu");
            if (left <= 0 || eu <= 0) {
                if (!start(context)) {
                    context.progress(0);
                    context.working(false);
                    return;
                }
                left = state.getInt("left");
                eu = state.getLong("eu");
            }
            context.working(true);
            context.generate(eu);
            context.emit(eu, 1);
            left--;
            state.setInt("left", left);
            int burn = state.getInt("burn");
            context.progress(burn <= 0 ? 0.0F : (burn - left) / (float) burn);
        }

        private static boolean start(MachineBehaviorContext context) {
            AddonRecipes.Line line = AddonRecipes.map(MAP).find(List.of(context.item(0)), List.of(), false, 1, 0);
            if (line == null) return false;
            long produced = -line.euPerTick();
            if (produced <= 0 || produced > PACKET) return false;
            if (!outputsFit(context.item(1), line)) return false;
            ItemStack input = context.item(0).copy();
            for (ItemStack required : line.itemInputs()) {
                if (required == null || required.isEmpty()) continue;
                if (input.getItem() != required.getItem() || input.getCount() < required.getCount()) return false;
                input.shrink(required.getCount());
            }
            context.setItem(0, input);
            context.setItem(1, withOutputs(context.item(1), line));
            int ticks = line.duration() > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) line.duration();
            context.state().setInt("burn", ticks);
            context.state().setInt("left", ticks);
            context.state().setLong("eu", produced);
            return true;
        }

        private static boolean outputsFit(ItemStack slot, AddonRecipes.Line line) {
            ItemStack cursor = slot == null ? ItemStack.EMPTY : slot.copy();
            for (ItemStack output : line.itemOutputs()) {
                if (output == null || output.isEmpty()) continue;
                if (cursor.isEmpty()) {
                    cursor = output.copy();
                    continue;
                }
                if (cursor.getItem() != output.getItem() || cursor.getCount() > cursor.getMaxStackSize() - output.getCount())
                    return false;
                cursor.grow(output.getCount());
            }
            return true;
        }

        private static ItemStack withOutputs(ItemStack slot, AddonRecipes.Line line) {
            ItemStack cursor = slot == null ? ItemStack.EMPTY : slot.copy();
            for (ItemStack output : line.itemOutputs()) {
                if (output == null || output.isEmpty()) continue;
                if (cursor.isEmpty()) cursor = output.copy();
                else cursor.grow(output.getCount());
            }
            return cursor;
        }
    }
}
