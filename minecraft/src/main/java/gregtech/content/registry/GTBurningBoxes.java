package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.block.machine.BurningBoxBlock;
import gregtech.content.block.machine.BurningBoxBlockItem;
import gregtech.content.block.machine.SolidBurningBoxBlock;
import gregtech.engine.machine.BurningBoxDefinitions;
import gregtech.engine.machine.BurningBoxFuelType;
import gregtech.engine.machine.InitialSmelteryDefinitions;
import gregtech.engine.machine.MachineSpec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** Shared solid and fluid burning box catalog. */
public final class GTBurningBoxes {
    private static final List<RegisteredBlock<SolidBurningBoxBlock>> SOLID = new ArrayList<>();
    private static final List<RegisteredBlock<BurningBoxBlock>> FUEL = new ArrayList<>();

    private GTBurningBoxes() {}

    public static List<RegisteredBlock<SolidBurningBoxBlock>> solidBoxes() {
        return List.copyOf(SOLID);
    }

    public static List<RegisteredBlock<BurningBoxBlock>> fuelBoxes() {
        return List.copyOf(FUEL);
    }

    public static RegisteredBlock<SolidBurningBoxBlock> findSolid(String id) {
        return SOLID.stream()
                .filter(block -> block.getId().getPath().equals(id))
                .findFirst()
                .orElse(null);
    }

    public static RegisteredBlock<BurningBoxBlock> findFuel(String id) {
        return FUEL.stream()
                .filter(block -> block.getId().getPath().equals(id))
                .findFirst()
                .orElse(null);
    }

    public static void registerAll(ContentRegistrar registrar) {
        if (!SOLID.isEmpty() || !FUEL.isEmpty()) return;
        var brick = InitialSmelteryDefinitions.brickBurningBox();
        SOLID.add(registrar.block(
                REGISTRY_NAMESPACE,
                brick.id(),
                () -> new SolidBurningBoxBlock(brick, solidProperties(brick)),
                box -> new BurningBoxBlockItem(box, new Item.Properties().stacksTo(16), brick)));
        for (var entry : BurningBoxDefinitions.all()) {
            if (entry.spec().id().equals("burning_box_solid_brick")) continue;
            if (entry.fuelType() == BurningBoxFuelType.SOLID) {
                SOLID.add(registrar.block(
                        REGISTRY_NAMESPACE,
                        entry.spec().id(),
                        () -> new SolidBurningBoxBlock(entry.spec(), solidProperties(entry.spec())),
                        box -> new BurningBoxBlockItem(box, new Item.Properties().stacksTo(16), entry.spec())));
            } else {
                FUEL.add(registrar.block(
                        REGISTRY_NAMESPACE,
                        entry.spec().id(),
                        () -> new BurningBoxBlock(
                                entry.fuelType(), entry.spec(), fuelProperties(entry.spec())),
                        box -> new BurningBoxBlockItem(box, new Item.Properties().stacksTo(16), entry.spec())));
            }
        }
    }

    private static BlockBehaviour.Properties solidProperties(MachineSpec spec) {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(spec.hardness(), spec.blastResistance())
                .requiresCorrectToolForDrops()
                .lightLevel(state -> state.getValue(SolidBurningBoxBlock.LIT) ? 13 : 0);
    }

    private static BlockBehaviour.Properties fuelProperties(MachineSpec spec) {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(spec.hardness(), spec.blastResistance())
                .requiresCorrectToolForDrops()
                .lightLevel(state -> state.getValue(BurningBoxBlock.LIT) ? 13 : 0);
    }
}
