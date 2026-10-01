package com.gregtech.gregtech.content.multiblock;

import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.block.machine.MultiblockPortBlock;
import com.gregtech.gregtech.registry.MachineBlockRegistration;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;
import java.util.*;

/** Named structural parts; original numeric IDs are confined to the layout translation boundary. */
public final class LargeMachineParts {
    private static final List<SharedLargeMachineParts.Part> DEFINITIONS=SharedLargeMachineParts.DEFINITIONS;
    private static final Map<Integer,RegistryObject<Block>> BLOCKS=new LinkedHashMap<>();
    private LargeMachineParts() {}
    public static void register() {
        for(var part:DEFINITIONS) {
            TankValveSpec valve = TankValveSpec.find(part.originalId());
            if (part.originalId() == 17996) {
                BLOCKS.put(part.originalId(), MachineBlockRegistration.block(part.name(),
                        com.gregtech.gregtech.block.machine.VonDaGraaggControllerBlock::new)
                        .strength(8, 8).register());
            } else if (part.originalId() == 17998) {
                BLOCKS.put(part.originalId(), MachineBlockRegistration.block(part.name(),
                        com.gregtech.gregtech.block.machine.OriginalLightningRodControllerBlock::new)
                        .strength(10, 10).register());
            } else if (part.originalId() == 17999) {
                BLOCKS.put(part.originalId(), MachineBlockRegistration.block(part.name(),
                        com.gregtech.gregtech.block.machine.OriginalBedrockDrillControllerBlock::new)
                        .strength(9, 9).register());
            } else if (valve != null) {
                BLOCKS.put(part.originalId(), MachineBlockRegistration.block(part.name(),
                        properties -> new com.gregtech.gregtech.block.machine.TankControllerBlock(valve, properties))
                        .strength(valve.hardness(), valve.hardness()).register());
            } else if (part.originalId() >= 17201 && part.originalId() <= 17205) {
                var boiler = OriginalLargeBoilerSpecs.byOriginalId(part.originalId());
                BLOCKS.put(part.originalId(), MachineBlockRegistration.block(part.name(),
                        properties -> new com.gregtech.gregtech.block.machine.OriginalLargeBoilerControllerBlock(boiler, properties))
                        .strength(boiler.hardness(), boiler.hardness()).register());
            } else if (part.originalId() >= 17302 && part.originalId() <= 17312) {
                var crucible = LargeCrucibleSpecs.byOriginalId(part.originalId());
                BLOCKS.put(part.originalId(), MachineBlockRegistration.block(part.name(),
                        properties -> new com.gregtech.gregtech.block.machine.LargeCrucibleControllerBlock(crucible, properties))
                        .strength(crucible.hardness(), crucible.hardness()).register());
            } else if (part.originalId() >= 17211 && part.originalId() <= 17214) {
                var grade = AxialGeneratorDefinitions.STEAM.get(part.originalId() - 17211);
                BLOCKS.put(part.originalId(), MachineBlockRegistration.block(part.name(),
                        properties -> new com.gregtech.gregtech.block.machine.LargeTurbineControllerBlock(grade, properties))
                        .strength(grade.materials().hardness(), grade.materials().hardness()).register());
            } else if (part.originalId() >= 17221 && part.originalId() <= 17224) {
                var grade = AxialGeneratorDefinitions.DYNAMO.get(part.originalId() - 17221);
                BLOCKS.put(part.originalId(), MachineBlockRegistration.block(part.name(),
                        properties -> new com.gregtech.gregtech.block.machine.LargeDynamoControllerBlock(grade, properties))
                        .strength(grade.materials().hardness(), grade.materials().hardness()).register());
            } else if (part.originalId() >= 17231 && part.originalId() <= 17234) {
                var grade = GasTurbineDefinitions.GRADES.get(part.originalId() - 17231);
                BLOCKS.put(part.originalId(), MachineBlockRegistration.block(part.name(),
                        properties -> new com.gregtech.gregtech.block.machine.LargeGasTurbineControllerBlock(grade, properties))
                        .strength(grade.hardness(), grade.hardness()).register());
            } else {
                BLOCKS.put(part.originalId(),MachineBlockRegistration.block(part.name(),MultiblockPortBlock::new)
                        .strength(part.originalId()==18025?100:part.originalId()==18023?12.5f:8,
                                  part.originalId()==18025?100:part.originalId()==18023?12.5f:8).register());
            }
        }
    }
    public static Block block(int originalId) { return Objects.requireNonNull(find(originalId),"Unknown original part "+originalId).get(); }

    /**
     * Non-throwing lookup of the block that stands in for a GT6 multiblock part id.
     * <p>
     * Parts owned by {@code GTMultiblocks} live outside {@link #DEFINITIONS}; the ids below are
     * the ones GT6 uses in its "Multiblock Machines" crafting recipes. GT6's {@code 18000}
     * ("Fire Bricks") has no dedicated block in this port, so the Coke Oven wall stands in.
     * </p>
     *
     * @return the registered block, or {@code null} when this port has no counterpart yet
     */
    public static RegistryObject<Block> find(int originalId) {
        RegistryObject<Block> hosted = switch (originalId) {
            case 18000 -> com.gregtech.gregtech.registry.GTMultiblocks.COKE_OVEN_WALL;           // GT6 "Fire Bricks"
            case 18002 -> com.gregtech.gregtech.registry.GTMultiblocks.TANK_WALL;                // GT6 "Stainless Steel Wall"
            case 18022 -> com.gregtech.gregtech.registry.GTMultiblocks.TANK_WALL_DENSE;          // GT6 "Dense Stainless Steel Wall"
            case 18023 -> com.gregtech.gregtech.registry.GTMultiblocks.IMPLOSION_COMPRESSOR_WALL;
            case 18041 -> com.gregtech.gregtech.registry.GTMultiblocks.LARGE_NIOBIUM_TITANIUM_COIL; // §113
            case 18100 -> com.gregtech.gregtech.registry.GTMultiblocks.CENTRIFUGE_PART;
            case 18101 -> com.gregtech.gregtech.registry.GTMultiblocks.HEAT_TRANSMITTER;
            case 18102 -> com.gregtech.gregtech.registry.GTMultiblocks.DISTILLATION_TOWER_PART;
            case 18105 -> com.gregtech.gregtech.registry.GTMultiblocks.ELECTROLYZER_PART;
            default -> null;
        };
        return hosted != null ? hosted : BLOCKS.get(originalId);
    }
    public static List<RegistryObject<Block>> blocks() {return List.copyOf(BLOCKS.values());}
    public static Integer tint(Block block) {
        for(var part:DEFINITIONS) {
            var registered=BLOCKS.get(part.originalId());
            if(registered!=null&&registered.isPresent()&&registered.get()==block) return GTMaterialRegistry.get(part.material()).getColor();
        }
        return null;
    }
}
