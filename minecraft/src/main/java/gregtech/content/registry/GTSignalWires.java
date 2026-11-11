package gregtech.content.registry;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.content.block.energy.SignalWireBlock;
import gregtech.content.blockentity.energy.SignalWireBlockEntity;
import gregtech.engine.material.ItemMaterialRegistry;
import java.util.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** GT6 source IDs 27000/27006, 27050/27056, 27500/27506. These carry redstone, never EU. */
public final class GTSignalWires {
    private static final List<RegisteredBlock<SignalWireBlock>> ALL = new ArrayList<>();
    public static RegisteredBlockEntity<SignalWireBlockEntity> SIGNAL_WIRE;

    private GTSignalWires() {}

    public static List<RegisteredBlock<SignalWireBlock>> all() {
        return Collections.unmodifiableList(ALL);
    }

    public static void register(ContentRegistrar registrar) {
        for (var familySpec : gregtech.content.energy.SignalWireCatalog.families())
            registerFamily(
                    registrar, familySpec.name(), familySpec.material(), familySpec.range(), familySpec.luminous());
        SIGNAL_WIRE = registrar.blockEntity(
                gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE,
                "signal_wire",
                () -> BlockEntityType.Builder.of(
                                SignalWireBlockEntity::new,
                                all().stream().map(RegisteredBlock::get).toArray(Block[]::new))
                        .build(null));
    }

    private static void registerFamily(
            ContentRegistrar registrar, String name, GTMaterial material, int range, boolean luminous) {
        for (boolean insulated : new boolean[] {false, true}) {
            String id = (insulated ? "cable_01_" : "wire_01_") + name;
            var block = registrar.block(
                    gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE,
                    id,
                    () -> new SignalWireBlock(
                            material,
                            range,
                            insulated,
                            luminous && !insulated,
                            BlockBehaviour.Properties.of().strength(1, 2).noOcclusion()),
                    bare -> new BlockItem(bare, new Item.Properties()));
            ALL.add(block);
        }
    }

    /** Registers each signal wire's composition; call after item registration. */
    public static void registerCompositions() {
        for (var holder : ALL) {
            SignalWireBlock block = holder.get();
            if (block.material() == null || !block.material().isValid()) continue;
            ItemMaterialRegistry.register(block.asItem(), null, block.material(), GTValues.U / 2);
        }
    }
}
