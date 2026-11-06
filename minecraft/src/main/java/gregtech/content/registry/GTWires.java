package gregtech.content.registry;

import gregtech.engine.energy.WireSpec;
import gregtech.api.material.GTMaterial;
import gregtech.content.block.energy.ElectricWireBlock;
import gregtech.engine.material.ItemMaterialRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

//? if >=1.21.1 {
/*import net.neoforged.neoforge.registries.*;
import net.neoforged.bus.api.IEventBus;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
*///?} else {
//?}

/** Native electric wire registrations populated from the shared WireCatalog on both platforms. */
public final class GTWires {

    private static final List<RegisteredBlock<ElectricWireBlock>> ALL_WIRES = new ArrayList<>();
    private static final List<RegisteredBlock<ElectricWireBlock>> ALL_CABLES = new ArrayList<>();
    private static final List<RegisteredBlock<ElectricWireBlock>> ALL = new ArrayList<>();

//? if >=1.21.1 {
/*    public static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE);
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<gregtech.content.blockentity.energy.ElectricWireBlockEntity>> ELECTRIC_WIRE=ENTITIES.register("electric_wire",()->BlockEntityType.Builder.of(gregtech.content.blockentity.energy.ElectricWireBlockEntity::new,all().stream().map(RegisteredBlock::get).toArray(Block[]::new)).build(null));

    public static void submit(IEventBus bus) {
        ENTITIES.register(bus);
    }
*///?} else {
//?}

    private GTWires() {}

    /** Queues every wire and cable from the shared catalog. */
    public static void registerAll(ContentRegistrar registrar) {
        if (!ALL.isEmpty()) throw new IllegalStateException("Electric wires registered twice");
        for (var spec : gregtech.content.energy.WireCatalog.specifications())
            register(registrar, spec.id(), spec.material(), spec.size(), spec.voltage(), spec.amperage(),
                    spec.lossPerBlock(), spec.insulated(), spec.contactDamage());
        bootstrap();
    }

    public static void register(ContentRegistrar registrar, String idSuffix, GTMaterial material, int size,
                         long voltage, long amperage, long loss, boolean insulated, boolean contactDamage) {
        String prefix = insulated ? "cable" : "wire";
        String id = String.format("%s_%02d_%s", prefix, size, idSuffix);
        WireSpec spec = new WireSpec(idSuffix, material, size, voltage, amperage, loss, insulated, contactDamage);

        RegisteredBlock<ElectricWireBlock> block = registrar.block(
                gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE,
                id,
                () -> new ElectricWireBlock(spec, BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .strength(1.0f, 2.0f)
                        .sound(SoundType.METAL)
                        .noOcclusion()
                        // Arms depend on neighbour thickness, so no per-state shape cache.
                        .dynamicShape()),
                bare -> new gregtech.content.item.WireBlockItem(bare,
                        new Item.Properties().stacksTo(stackSize(size, insulated))));
        (insulated ? ALL_CABLES : ALL_WIRES).add(block);
        ALL.add(block);
    }

    /**
     * GT6's explicit stack limits (material content is separately size * U/2):
     * {@code MultiTileEntityWireElectric.addElectricWires} registers {@code 64/size}
     * (64, 32, 21, 16, 12, 10, 9, 8, 7, 6, 5, 4); its cables use 64/32/16/8/4 for sizes 1/2/4/8/12.
     */
    public static int stackSize(int size, boolean insulated) {
        return gregtech.content.energy.WireCatalog.stackSize(size,insulated);
    }

    /** All bare wire blocks for BlockEntityType registration. */
    public static List<RegisteredBlock<ElectricWireBlock>> allWires() {
        return Collections.unmodifiableList(ALL_WIRES);
    }

    /** All insulated cable blocks. */
    public static List<RegisteredBlock<ElectricWireBlock>> allCables() {
        return Collections.unmodifiableList(ALL_CABLES);
    }

    /** All wire + cable blocks in registration order. */
    public static List<RegisteredBlock<ElectricWireBlock>> all() {
        return Collections.unmodifiableList(ALL);
    }

    public static void bootstrap() {
        if (ALL_WIRES.isEmpty()) throw new IllegalStateException("GTWires failed to initialize");
    }

    /** Registers each wire and cable item's composition; call after item registration. */
    public static void registerCompositions() {
        for (var holder : ALL) {
            ElectricWireBlock block = holder.get();
            WireSpec spec = block.spec();
            if (spec.material() == null || !spec.material().isValid()) continue;
            ItemMaterialRegistry.register(block.asItem(), null, spec.material(), spec.materialAmount());
        }
    }
}
