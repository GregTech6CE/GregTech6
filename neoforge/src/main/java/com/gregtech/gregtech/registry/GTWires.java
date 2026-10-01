package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.api.energy.WireSpec;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.block.energy.ElectricWireBlock;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.*;
import net.neoforged.bus.api.IEventBus;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** GT6 electric wire blocks — storage holder populated by {@code Loader_MultiTileEntities}. */
public final class GTWires {
    private static final List<DeferredHolder<Block,ElectricWireBlock>> ALL_WIRES = new ArrayList<>();
    private static final List<DeferredHolder<Block,ElectricWireBlock>> ALL_CABLES = new ArrayList<>();
    private static final List<DeferredHolder<Block,ElectricWireBlock>> ALL = new ArrayList<>();

    public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(Registries.BLOCK,"gregtech");
    public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Registries.ITEM,"gregtech");
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,"gregtech");
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<com.gregtech.gregtech.blockentity.energy.ElectricWireBlockEntity>> ELECTRIC_WIRE=ENTITIES.register("electric_wire",()->BlockEntityType.Builder.of(com.gregtech.gregtech.blockentity.energy.ElectricWireBlockEntity::new,all().stream().map(DeferredHolder::get).toArray(Block[]::new)).build(null));
    private GTWires() {}
    public static void register(IEventBus bus){
        if(!ALL.isEmpty())throw new IllegalStateException("Electric wires registered twice");
        for(var spec:com.gregtech.gregtech.content.energy.WireCatalog.specifications())register(spec.id(),spec.material(),spec.size(),spec.voltage(),spec.amperage(),spec.lossPerBlock(),spec.insulated(),spec.contactDamage());
        bootstrap();BLOCKS.register(bus);ITEMS.register(bus);ENTITIES.register(bus);
    }

    public static void register(String idSuffix, GTMaterial material, int size,
                         long voltage, long amperage, long loss, boolean insulated, boolean contactDamage) {
        String prefix = insulated ? "cable" : "wire";
        String id = String.format("%s_%02d_%s", prefix, size, idSuffix);
        WireSpec spec = new WireSpec(idSuffix, material, size, voltage, amperage, loss, insulated, contactDamage);
        DeferredHolder<Block,ElectricWireBlock> block = BLOCKS.register(id,
                () -> new ElectricWireBlock(spec, BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .strength(1.0f, 2.0f)
                        .sound(SoundType.METAL)
                        .noOcclusion()));
        (insulated ? ALL_CABLES : ALL_WIRES).add(block);
        ALL.add(block);
        ITEMS.register(id,
                () -> new com.gregtech.gregtech.item.WireBlockItem(block.get(),
                        new Item.Properties().stacksTo(stackSize(size, insulated))));
    }

    /**
     * GT6's stack sizes: a wire carries one unit of material per size, so
     * {@code MultiTileEntityWireElectric.addElectricWires} registers {@code 64/size}
     * (64, 32, 21, 16, 12, 10, 9, 8, 7, 6, 5, 4); its cables use 64/32/16/8/4 for sizes 1/2/4/8/12.
     */
    public static int stackSize(int size, boolean insulated) {
        return com.gregtech.gregtech.content.energy.WireCatalog.stackSize(size,insulated);
    }

    /** All bare wire blocks for BlockEntityType registration. */
    public static List<DeferredHolder<Block,ElectricWireBlock>> allWires() {
        return Collections.unmodifiableList(ALL_WIRES);
    }

    /** All insulated cable blocks. */
    public static List<DeferredHolder<Block,ElectricWireBlock>> allCables() {
        return Collections.unmodifiableList(ALL_CABLES);
    }

    /** All wire + cable blocks in registration order. */
    public static List<DeferredHolder<Block,ElectricWireBlock>> all() {
        return Collections.unmodifiableList(ALL);
    }

    public static void bootstrap() {
        if (ALL_WIRES.isEmpty()) throw new IllegalStateException("GTWires failed to initialize");
    }
}
