package com.gregtech.addonsample;

import com.gregtech.gregtech.api.addon.AddonMachineContributor;
import com.gregtech.gregtech.api.addon.BehaviorMachineDeclaration;
import com.gregtech.gregtech.api.addon.FluidPipeDeclaration;
import com.gregtech.gregtech.api.addon.ItemPipeDeclaration;
import com.gregtech.gregtech.api.addon.FuelGeneratorDeclaration;
import com.gregtech.gregtech.api.addon.FuelMapDeclaration;
import com.gregtech.gregtech.api.addon.MachineDeclaration;
import com.gregtech.gregtech.api.addon.MachineLayout;
import com.gregtech.gregtech.api.addon.RecipeMapDeclaration;
import com.gregtech.gregtech.api.addon.StructurePartDeclaration;
import com.gregtech.gregtech.api.addon.StructureRole;
import com.gregtech.gregtech.api.addon.platform.BehaviorMachineContributor;
import com.gregtech.gregtech.api.addon.platform.MachineBehavior;
import com.gregtech.gregtech.api.addon.platform.MachineBehaviorContext;
import net.minecraft.core.BlockPos;
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

/** Sample press, tank-wall multiblock, irrigator, rotation buffer, hand crank and pipes. */
public final class AddonSampleMachines implements AddonMachineContributor, BehaviorMachineContributor {
    @Override public String addonId() { return "addon_sample"; }

    @Override public List<RecipeMapDeclaration> recipeMaps() {
        return List.of(
                map("addon_sample.recipe.press", "Sample Press", "press", 2),
                map("addon_sample.recipe.large_press", "Large Sample Press", "large_press"));
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
                MachineDeclaration.builder("large_sample_press", "Steel")
                        .machineType("largesamplepress").energy("EU", 512).tier(1)
                        .recipeMap("addon_sample.recipe.large_press").largeRecipe()
                        .structureKeys(List.of("addon_sample.structure.press.1")).build());
    }

    @Override public List<StructurePartDeclaration> parts() {
        return List.of(new StructurePartDeclaration("sample_wall", "Steel", "", 6.0F));
    }

    @Override public List<FluidPipeDeclaration> fluidPipes() {
        return List.of(new FluidPipeDeclaration("teflon", "Teflon", 125, true, true, false, false, 600, 0));
    }

    @Override public List<ItemPipeDeclaration> itemPipes() {
        return List.of(new ItemPipeDeclaration("pvc", "PVC", 32768, 1));
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
                        new MachineLayout.Cell(0, 0, 1, "tank_wall", StructureRole.FLUID_INPUT))));
    }

    @Override public List<FuelMapDeclaration> fuelMaps() {
        return List.of(new FuelMapDeclaration("addon_sample.fuel.gas", "Sample Gas",
                "addon_sample:textures/gui/machines/press",
                0, 0, 0, 1, 1, 1, 1, false, false, false, false));
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
        return List.of(IRRIGATOR, ROTATION_BUFFER, HAND_CRANK);
    }

    @Override public Set<String> machineTypes() { return Set.of("irrigator", "ru_buffer", "hand_crank"); }

    @Override public MachineBehavior create(String machineType) {
        return switch (machineType) {
            case "irrigator" -> new Irrigator();
            case "ru_buffer" -> new RotationBuffer();
            case "hand_crank" -> new HandCrank();
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

    /** A click stores one RU packet and pushes it out the front. */
    private static final BehaviorMachineDeclaration HAND_CRANK = BehaviorMachineDeclaration.builder("hand_crank", "Steel")
            .machineType("hand_crank").energy("RU", 32).tier(1)
            .gui("addon_sample:textures/gui/machine.png")
            .energyFaces(List.of(BehaviorMachineDeclaration.Face.BOTTOM))
            .energyCapacity(32)
            .energyOutputs(BehaviorMachineDeclaration.Face.FRONT)
            .energyOut("RU").energyOutMax(32, 1)
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
}
