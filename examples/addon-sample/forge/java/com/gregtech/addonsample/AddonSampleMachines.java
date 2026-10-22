package com.gregtech.addonsample;

import com.gregtech.gregtech.api.addon.AddonMachine;
import com.gregtech.gregtech.api.addon.AddonMachineContributor;
import com.gregtech.gregtech.api.addon.BehaviorMachine;
import com.gregtech.gregtech.api.addon.CellMatch;
import com.gregtech.gregtech.api.addon.CompressedGasTankDeclaration;
import com.gregtech.gregtech.api.addon.EnergyInput;
import com.gregtech.gregtech.api.addon.EnergyOutput;
import com.gregtech.gregtech.api.addon.FluidPipeDeclaration;
import com.gregtech.gregtech.api.addon.FuelGenerator;
import com.gregtech.gregtech.api.addon.FuelOutput;
import com.gregtech.gregtech.api.addon.ItemPipeDeclaration;
import com.gregtech.gregtech.api.addon.LayoutPart;
import com.gregtech.gregtech.api.addon.MachineCasing;
import com.gregtech.gregtech.api.addon.MachineFace;
import com.gregtech.gregtech.api.addon.MachineIo;
import com.gregtech.gregtech.api.addon.RecipeMachine;
import com.gregtech.gregtech.api.addon.RecipeTable;
import com.gregtech.gregtech.api.addon.StructureLayout;
import com.gregtech.gregtech.api.addon.StructureRole;
import com.gregtech.gregtech.api.addon.platform.AddonFluids;
import com.gregtech.gregtech.api.addon.platform.AddonTool;
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

import java.util.List;
import java.util.Set;

/** Sample press, detector, tank-wall multiblock, irrigator, miner, void hopper, rotation buffer, hand crank, transformer, solid boiler, gas compressor, compressed drum and pipes. */
public final class AddonSampleMachines implements AddonMachineContributor, BehaviorMachineContributor {
    @Override public String addonId() { return "addon_sample"; }

    @Override public List<RecipeTable> recipeTables() {
        return List.of(
                map("addon_sample.recipe.press", "Sample Press", "press", 2),
                RecipeTable.machine("addon_sample.recipe.large_press")
                        .title("Large Sample Press")
                        .gui("addon_sample:textures/gui/machines/large_press")
                        .inputItems(1).outputItems(6).minimalInputItems(1)
                        .outputFluids(1).minimalInputs(1).needsOutputs(true).build(),
                RecipeTable.machine("addon_sample.recipe.detector")
                        .title("Sample Detector")
                        .gui("addon_sample:textures/gui/machines/press")
                        .inputItems(1).outputItems(2).minimalInputItems(1)
                        .inputFluids(1).outputFluids(1).minimalInputs(1).needsOutputs(true).build(),
                RecipeTable.fuel("addon_sample.fuel.gas")
                        .title("Sample Gas")
                        .gui("addon_sample:textures/gui/machines/press")
                        .inputFluids(1).outputFluids(1).minimalInputFluids(1).minimalInputs(1).build(),
                RecipeTable.fuel("addon_sample.fuel.solid")
                        .title("Sample Solid")
                        .gui("addon_sample:textures/gui/machines/press")
                        .inputItems(1).outputItems(1).minimalInputItems(1).minimalInputs(1)
                        .needsOutputs(true).build());
    }

    @Override public List<AddonMachine> addonMachines() {
        var allFaces = MachineFace.all().toArray(MachineFace[]::new);
        return List.of(
                RecipeMachine.builder(MachineCasing.builder("sample_press", "Steel").machineType("samplepress").tier(1).build())
                        .energy(EnergyInput.builder("EU", 32).faces(MachineFace.BACK).build())
                        .io(MachineIo.builder()
                                .itemInputs(MachineFace.TOP)
                                .itemOutputs(MachineFace.BOTTOM)
                                .fluidInputs(MachineFace.LEFT)
                                .fluidOutputs(MachineFace.RIGHT)
                                .itemAutoOutput(MachineFace.BOTTOM)
                                .build())
                        .recipeMap("addon_sample.recipe.press").basic()
                        .cheapOverclocking(true)
                        .minimumLightAbove(14)
                        .build(),
                RecipeMachine.builder(MachineCasing.builder("sample_detector", "Steel").machineType("sampledetector").tier(1).build())
                        .energy(EnergyInput.builder("EU", 32).build())
                        .recipeMap("addon_sample.recipe.detector").basic()
                        .searchOutputTanks(true)
                        .exclusiveOutputs(true)
                        .build(),
                RecipeMachine.builder(MachineCasing.builder("large_sample_press", "Steel").machineType("largesamplepress").tier(1).build())
                        .energy(EnergyInput.builder("EU", 512).range(256, 2048).build())
                        .io(MachineIo.builder()
                                .itemInputs(allFaces).itemOutputs(allFaces)
                                .fluidInputs(allFaces).fluidOutputs(allFaces)
                                .itemOutputTarget(2, 0, 1, MachineFace.RIGHT)
                                .fluidOutputTarget(-2, 0, 1, MachineFace.LEFT)
                                .build())
                        .recipeMap("addon_sample.recipe.large_press").largeRecipe()
                        .structureKeys(List.of("addon_sample.structure.press.1"))
                        .build(),
                FuelGenerator.builder(MachineCasing.builder("sample_engine", "Steel").machineType("sample_engine").build())
                        .fuelMap("addon_sample.fuel.gas").efficiency(10_000)
                        .output(FuelOutput.builder("RU", 32).front().build())
                        .tank(4_000).single().build(),
                FuelGenerator.builder(MachineCasing.builder("sample_motor", "Steel").machineType("sample_motor").build())
                        .fuelMap("addon_sample.fuel.gas").efficiency(10_000)
                        .output(FuelOutput.builder("RU", 32).front().build())
                        .amperes(4).tank(4_000).single().build(),
                FuelGenerator.builder(MachineCasing.builder("sample_turbine", "Steel").machineType("sample_turbine").build())
                        .fuelMap("addon_sample.fuel.gas").efficiency(5_000)
                        .output(FuelOutput.builder("RU", 32).behind(4).build())
                        .tank(8_000).multi().build(),
                IRRIGATOR, MINER, VOID_HOPPER, ROTATION_BUFFER, HAND_CRANK, TRANSFORMER, CLUSTER, SOLID_BOILER, COMPRESSOR, WATER_WHEEL, HAMMER_SWITCH, INLET);
    }

    @Override public List<LayoutPart> layoutParts() {
        return List.of(
                LayoutPart.builder("sample_wall", "Steel").hardness(6.0F).build(),
                LayoutPart.builder("sample_emitter", "Steel").hardness(6.0F)
                        .energyOutput(EnergyOutput.builder("EU", 32, 4).faces(MachineFace.FRONT).build(), 100_000L)
                        .build(),
                AddonSampleParts.PART);
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

    @Override public List<StructureLayout> structureLayouts() {
        var press = StructureLayout.builder("largesamplepress");
        for (int up = 0; up < 2; up++) {
            for (int right = -1; right <= 1; right++) {
                for (int back = 0; back <= 2; back++) {
                    if (right == 0 && up == 0 && back == 0) continue;
                    if (right == -1 && up == 1 && back == 2) {
                        press.cell(right, up, back, CellMatch.block("minecraft:glass"), StructureRole.CASING);
                        continue;
                    }
                    var role = back == 1 && right == 0
                            ? StructureRole.ENERGY_INPUT : StructureRole.ITEM_FLUID_IO;
                    String part = right == 1 && up == 0 && back == 0 ? "sample_wall" : "tank_wall";
                    press.cell(right, up, back, CellMatch.part(part), role);
                }
            }
        }
        return List.of(press.build(),
                StructureLayout.builder("sample_turbine")
                        .cell(0, 0, 1, CellMatch.part("tank_wall"), StructureRole.FLUID_INPUT).build(),
                StructureLayout.builder("sample_transformer")
                        .cell(0, 0, 1, CellMatch.part("sample_emitter"), StructureRole.ENERGY_OUTPUT).build(),
                StructureLayout.builder("sample_cluster")
                        .cell(0, 0, 1, CellMatch.part("sample_compute"), StructureRole.CASING).build());
    }

    @Override public Set<String> machineTypes() {
        return Set.of("irrigator", "sampleminer", "samplehopper", "ru_buffer", "hand_crank",
                "sample_transformer", "sample_cluster", "sampleboiler", "samplecompressor", "sample_wheel", "sample_switch",
                "sample_inlet");
    }

    @Override public MachineBehavior create(String machineType) {
        return switch (machineType) {
            case "irrigator" -> new Irrigator();
            case "sampleminer" -> new Miner();
            case "samplehopper" -> new VoidHopper();
            case "ru_buffer" -> new RotationBuffer();
            case "hand_crank" -> new HandCrank();
            case "sample_transformer" -> new Transformer();
            case "sample_cluster" -> new Cluster();
            case "sampleboiler" -> new SolidBoiler();
            case "samplecompressor" -> new Compressor();
            case "sample_wheel" -> new WaterWheel();
            case "sample_switch" -> new HammerSwitch();
            case "sample_inlet" -> new Inlet();
            default -> throw new IllegalArgumentException(machineType);
        };
    }

    private static RecipeTable map(String name, String title, String gui) {
        return map(name, title, gui, 1);
    }

    private static RecipeTable map(String name, String title, String gui, int inputItems) {
        return RecipeTable.machine(name)
                .title(title).gui("addon_sample:textures/gui/machines/" + gui)
                .inputItems(inputItems).outputItems(1).minimalInputItems(1).minimalInputs(1)
                .needsOutputs(true).build();
    }

    private static final BehaviorMachine IRRIGATOR = BehaviorMachine.builder(
            MachineCasing.builder("irrigator", "Steel").machineType("irrigator").tier(1).build())
            .energy(EnergyInput.builder("EU", 32).build())
            .gui("addon_sample:textures/gui/machine.png")
            .item(44, 26, BehaviorMachine.SlotRole.INPUT)
            .item(116, 26, BehaviorMachine.SlotRole.OUTPUT)
            .fluid(8000, 152, 8, BehaviorMachine.SlotRole.INPUT)
            .toggle("addon_sample.irrigator.water", 26, 64, true)
            .build();

    /** Breaks one block under itself per tick and keeps the scan index. */
    private static final BehaviorMachine MINER = BehaviorMachine.builder(
            MachineCasing.builder("sample_miner", "Steel").machineType("sampleminer").tier(1).build())
            .energy(EnergyInput.builder("EU", 32).build())
            .gui("addon_sample:textures/gui/machine.png")
            .item(116, 26, BehaviorMachine.SlotRole.OUTPUT)
            .maxRange(2)
            .build();

    /** Pulls item entities into the output slot, or voids them while the switch is on. */
    private static final BehaviorMachine VOID_HOPPER = BehaviorMachine.builder(
            MachineCasing.builder("sample_hopper", "Steel").machineType("samplehopper").tier(1).build())
            .energy(EnergyInput.builder("EU", 32).build())
            .gui("addon_sample:textures/gui/machine.png")
            .item(116, 26, BehaviorMachine.SlotRole.OUTPUT)
            .toggle("addon_sample.hopper.void", 26, 64, false)
            .maxRange(4)
            .build();

    /** Stores RU from the sides and, while the switch is on, emits one packet from the back. */
    private static final BehaviorMachine ROTATION_BUFFER = BehaviorMachine.builder(
            MachineCasing.builder("ru_buffer", "Steel").machineType("ru_buffer").tier(1).build())
            .energy(EnergyInput.builder("RU", 32).capacity(32_000)
                    .faces(MachineFace.LEFT, MachineFace.RIGHT, MachineFace.TOP, MachineFace.BOTTOM).build())
            .gui("addon_sample:textures/gui/machine.png")
            .energyOutput(EnergyOutput.builder("RU", 32, 1).faces(MachineFace.BACK).build())
            .toggle("addon_sample.ru_buffer.release", 116, 64, false)
            .build();

    /** Receives EU on the free faces and moves it into the emitter one block behind. */
    private static final BehaviorMachine TRANSFORMER = BehaviorMachine.builder(
            MachineCasing.builder("sample_transformer", "Steel").machineType("sample_transformer").tier(1).build())
            .energy(EnergyInput.builder("EU", 32).capacity(32_000)
                    .faces(MachineFace.FRONT, MachineFace.LEFT, MachineFace.RIGHT, MachineFace.TOP, MachineFace.BOTTOM).build())
            .gui("addon_sample:textures/gui/machine.png")
            .build();

    /** Starts the compute part behind this block while the switch is on. */
    private static final BehaviorMachine CLUSTER = BehaviorMachine.builder(
            MachineCasing.builder("sample_cluster", "Steel").machineType("sample_cluster").tier(1).build())
            .energy(EnergyInput.builder("EU", 32).build())
            .gui("addon_sample:textures/gui/machine.png")
            .toggle("addon_sample.cluster.run", 44, 64, false)
            .build();

    /** A click stores one RU packet and pushes it out the front. */
    private static final BehaviorMachine HAND_CRANK = BehaviorMachine.builder(
            MachineCasing.builder("hand_crank", "Steel").machineType("hand_crank").tier(1).build())
            .energy(EnergyInput.builder("RU", 32).capacity(32).faces(MachineFace.BOTTOM).build())
            .gui("addon_sample:textures/gui/machine.png")
            .energyOutput(EnergyOutput.builder("RU", 32, 1).faces(MachineFace.FRONT).build())
            .build();

    /** Coal in, charcoal out. Produced EU leaves through the top. */
    private static final BehaviorMachine SOLID_BOILER = BehaviorMachine.builder(
            MachineCasing.builder("sample_boiler", "Steel").machineType("sampleboiler").tier(1).build())
            .energy(EnergyInput.builder("EU", 32).capacity(10_000)
                    .faces(MachineFace.BOTTOM, MachineFace.LEFT, MachineFace.RIGHT, MachineFace.BACK).build())
            .gui("addon_sample:textures/gui/machine.png")
            .item(44, 26, BehaviorMachine.SlotRole.INPUT)
            .item(116, 26, BehaviorMachine.SlotRole.OUTPUT)
            .energyOutput(EnergyOutput.builder("EU", 32, 1).faces(MachineFace.TOP).build())
            .recipeMaps("addon_sample.fuel.solid")
            .build();

    /** Emits one EU packet upward. No face accepts energy, and the menu has no energy bar. */
    private static final BehaviorMachine WATER_WHEEL = BehaviorMachine.builder(
            MachineCasing.builder("sample_wheel", "Steel").machineType("sample_wheel").tier(1).build())
            .gui("addon_sample:textures/gui/machine.png")
            .energyOutput(EnergyOutput.builder("EU", 32, 1).faces(MachineFace.TOP).build())
            .build();

    /** A soft hammer flips a saved flag. A wrench is left to GregTech and still rotates the block. */
    private static final BehaviorMachine HAMMER_SWITCH = BehaviorMachine.builder(
            MachineCasing.builder("sample_switch", "Steel").machineType("sample_switch").tier(1).build())
            .energy(EnergyInput.builder("EU", 32).build())
            .gui("addon_sample:textures/gui/machine.png")
            .build();

    /** Water only from the top. A saved {@code shut} flag refuses the next energy packet. */
    private static final BehaviorMachine INLET = BehaviorMachine.builder(
            MachineCasing.builder("sample_inlet", "Steel").machineType("sample_inlet").tier(1).build())
            .energy(EnergyInput.builder("EU", 32).capacity(10_000).build())
            .gui("addon_sample:textures/gui/machine.png")
            .fluid(8000, 152, 8, BehaviorMachine.SlotRole.INPUT)
            .io(MachineIo.builder().fluidInputs(MachineFace.TOP).build())
            .build();

    /** KU on the bottom, gas in the tank, compressed drum on the back. */
    private static final BehaviorMachine COMPRESSOR = BehaviorMachine.builder(
            MachineCasing.builder("sample_compressor", "Steel").machineType("samplecompressor").tier(1).build())
            .energy(EnergyInput.builder("KU", 32).faces(MachineFace.BOTTOM).build())
            .gui("addon_sample:textures/gui/machine.png")
            .fluid(8000, 152, 8, BehaviorMachine.SlotRole.INPUT)
            .build();

    /** Generates one EU packet and pushes it out the top. The main input cache stays empty. */
    private static final class WaterWheel implements MachineBehavior {
        @Override public boolean canInsert(MachineBehaviorContext context, int slot, ItemStack stack) { return false; }
        @Override public boolean canFill(MachineBehaviorContext context, int tank, FluidStack stack) { return false; }

        @Override public void tick(MachineBehaviorContext context) {
            var state = context.state();
            state.setLong("seen", context.energy());
            state.setLong("added", context.generate(32));
            state.setLong("after", context.energy());
            long sent = context.emit(32, 1);
            state.setLong("sent", sent);
            if (sent > 0) context.working(true);
        }
    }

    /** Soft hammer flips {@code armed}. Any other tool, including a wrench, is not handled. */
    private static final class HammerSwitch implements MachineBehavior {
        @Override public boolean canInsert(MachineBehaviorContext context, int slot, ItemStack stack) { return false; }
        @Override public boolean canFill(MachineBehaviorContext context, int tank, FluidStack stack) { return false; }
        @Override public void tick(MachineBehaviorContext context) {}

        @Override public boolean onTool(MachineBehaviorContext context, AddonTool tool, Player player,
                                        InteractionHand hand, MachineFace face) {
            if (tool != AddonTool.SOFT_HAMMER) return false;
            context.state().setBoolean("armed", !context.state().getBoolean("armed"));
            return true;
        }
    }

    /** Top fluid face only. {@code shut} makes {@link #acceptsEnergy} refuse the packet. */
    private static final class Inlet implements MachineBehavior {
        @Override public boolean canInsert(MachineBehaviorContext context, int slot, ItemStack stack) { return false; }
        @Override public boolean canFill(MachineBehaviorContext context, int tank, FluidStack stack) {
            return stack != null && !stack.isEmpty() && stack.getFluid() == Fluids.WATER;
        }
        @Override public void tick(MachineBehaviorContext context) {}

        @Override public boolean acceptsEnergy(MachineBehaviorContext context, String type, MachineFace face,
                                               long size, long amount) {
            return !context.state().getBoolean("shut");
        }
    }

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

    /** Turns the compute part behind this block on while the switch is on. */
    private static final class Cluster implements MachineBehavior {
        @Override public boolean canInsert(MachineBehaviorContext context, int slot, ItemStack stack) { return false; }
        @Override public boolean canFill(MachineBehaviorContext context, int tank, FluidStack stack) { return false; }

        @Override public void tick(MachineBehaviorContext context) {
            if (!context.structureFormed()) return;
            boolean run = context.toggle(0);
            for (var part : context.parts()) {
                if (!(part.behavior() instanceof AddonSampleParts.ComputePart compute) || part.context() == null) continue;
                if (run) compute.tryStart(part.context(), 1);
                else compute.stop(part.context());
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
