package com.gregtech.gregtech.content.multiblock;

import com.gregtech.gregtech.block.machine.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** Original part/controller factories shared by both loaders, keyed only by original GT6 IDs. */
public final class MultiblockBlockFactory {
    private MultiblockBlockFactory() {}
    public static Block create(SharedLargeMachineParts.Part part) {
        int id = part.originalId();
        var valve = TankValveSpec.find(id);
        if (valve != null) return new TankControllerBlock(valve, metal(valve.hardness(), valve.hardness()));
        if (id >= 17201 && id <= 17205) {
            var boiler = OriginalLargeBoilerSpecs.byOriginalId(id);
            return new OriginalLargeBoilerControllerBlock(boiler, metal(boiler.hardness(), boiler.hardness()));
        }
        if (id >= 17211 && id <= 17214) {
            var grade = AxialGeneratorDefinitions.STEAM.get(id - 17211);
            return new LargeTurbineControllerBlock(grade, metal(grade.materials().hardness(), grade.materials().hardness()));
        }
        if (id >= 17221 && id <= 17224) {
            var grade = AxialGeneratorDefinitions.DYNAMO.get(id - 17221);
            return new LargeDynamoControllerBlock(grade, metal(grade.materials().hardness(), grade.materials().hardness()));
        }
        if (id >= 17231 && id <= 17234) {
            var grade = GasTurbineDefinitions.GRADES.get(id - 17231);
            return new LargeGasTurbineControllerBlock(grade, metal(grade.hardness(), grade.hardness()));
        }
        var crucible = LargeCrucibleSpecs.all().stream().filter(spec -> spec.originalId() == id).findFirst().orElse(null);
        if (crucible != null) return new LargeCrucibleControllerBlock(crucible,
                metal(crucible.hardness(), crucible.hardness()).noOcclusion());
        return switch (id) {
            case 17996 -> new VonDaGraaggControllerBlock(metal(6, 6));
            case 17998 -> new OriginalLightningRodControllerBlock(metal(10, 10));
            case 17999 -> new OriginalBedrockDrillControllerBlock(metal(9, 9));
            default -> structural(OriginalMultiblockPartData.byId(id).orElseThrow());
        };
    }
    public static Block structural(OriginalMultiblockPartData.Part part) {
        return new MultiblockPortBlock(metal(part.hardness(), part.resistance()).requiresCorrectToolForDrops());
    }
    private static BlockBehaviour.Properties metal(float hardness, float resistance) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(hardness, resistance);
    }
}
