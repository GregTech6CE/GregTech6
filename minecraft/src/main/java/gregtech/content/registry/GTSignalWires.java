package gregtech.content.registry;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.content.block.energy.SignalWireBlock;
import gregtech.engine.material.ItemMaterialRegistry;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.registries.*;
import net.neoforged.bus.api.IEventBus;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
*///?} else {
//?}
import java.util.*;

/** GT6 source IDs 27000/27006, 27050/27056, 27500/27506. These carry redstone, never EU. */
public final class GTSignalWires {
    private static final List<RegisteredBlock<SignalWireBlock>> ALL = new ArrayList<>();
//? if >=1.21.1 {
    /*public static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE);
    public static final DeferredHolder<BlockEntityType<?>,
            BlockEntityType<gregtech.content.blockentity.energy.SignalWireBlockEntity>> SIGNAL_WIRE = ENTITIES.register("signal_wire",
            () -> BlockEntityType.Builder.of(gregtech.content.blockentity.energy.SignalWireBlockEntity::new,
            all().stream().map(RegisteredBlock::get).toArray(Block[]::new)).build(null));
*///?}
    private GTSignalWires() {
    }
//? if >=1.21.1 {
    /*public static void register(ContentRegistrar registrar, IEventBus bus) {
        if (!ALL.isEmpty()) throw new IllegalStateException("Signal wires registered twice");
        register(registrar);
        ENTITIES.register(bus);
    }
*///?}
    public static List<RegisteredBlock<SignalWireBlock>> all() {
        return Collections.unmodifiableList(ALL);
    }
    public static void register(ContentRegistrar registrar) {
        for (var familySpec : gregtech.content.energy.SignalWireCatalog.families())
            registerFamily(registrar, familySpec.name(), familySpec.material(), familySpec.range(), familySpec.luminous());
    }

    private static void registerFamily(ContentRegistrar registrar, String name, GTMaterial material, int range, boolean luminous) {
        for (boolean insulated : new boolean[]{false, true}) {
            String id = (insulated ? "cable_01_" : "wire_01_") + name;
            var block = registrar.block(
                    gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE,
                    id,
                    () -> new SignalWireBlock(material, range, insulated,
                            luminous && !insulated, BlockBehaviour.Properties.of().strength(1, 2).noOcclusion()),
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
