package gregtech.content.registry;

import gregtech.engine.energy.WireSpec;
import gregtech.api.material.GTMaterial;
import gregtech.content.block.energy.ElectricWireBlock;
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
import net.minecraftforge.registries.RegistryObject;
//?}


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Native electric wire registrations populated from the shared WireCatalog on both platforms. */
public final class GTWires {

//? if >=1.21.1 {
/*    private static final List<DeferredHolder<Block,ElectricWireBlock>> ALL_WIRES = new ArrayList<>();
    private static final List<DeferredHolder<Block,ElectricWireBlock>> ALL_CABLES = new ArrayList<>();
    private static final List<DeferredHolder<Block,ElectricWireBlock>> ALL = new ArrayList<>();
*///?} else {
    private static final List<RegistryObject<ElectricWireBlock>> ALL_WIRES = new ArrayList<>();
    private static final List<RegistryObject<ElectricWireBlock>> ALL_CABLES = new ArrayList<>();
    private static final List<RegistryObject<ElectricWireBlock>> ALL = new ArrayList<>();
//?}



//? if >=1.21.1 {
/*    public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(Registries.BLOCK,"gt6ce");
    public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Registries.ITEM,"gt6ce");
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,"gt6ce");
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<gregtech.content.blockentity.energy.ElectricWireBlockEntity>> ELECTRIC_WIRE=ENTITIES.register("electric_wire",()->BlockEntityType.Builder.of(gregtech.content.blockentity.energy.ElectricWireBlockEntity::new,all().stream().map(DeferredHolder::get).toArray(Block[]::new)).build(null));
*///?} else {
//?}

    private GTWires() {}

//? if >=1.21.1 {
/*    public static void register(IEventBus bus){
        if(!ALL.isEmpty())throw new IllegalStateException("Electric wires registered twice");
        for(var spec:gregtech.content.energy.WireCatalog.specifications())register(spec.id(),spec.material(),spec.size(),spec.voltage(),spec.amperage(),spec.lossPerBlock(),spec.insulated(),spec.contactDamage());
        bootstrap();BLOCKS.register(bus);ITEMS.register(bus);ENTITIES.register(bus);
    }
*///?} else {
//?}


    public static void register(String idSuffix, GTMaterial material, int size,
                         long voltage, long amperage, long loss, boolean insulated, boolean contactDamage) {
        String prefix = insulated ? "cable" : "wire";
        String id = String.format("%s_%02d_%s", prefix, size, idSuffix);
        WireSpec spec = new WireSpec(idSuffix, material, size, voltage, amperage, loss, insulated, contactDamage);

//? if >=1.21.1 {
/*        DeferredHolder<Block,ElectricWireBlock> block = BLOCKS.register(id,
*///?} else {
        RegistryObject<ElectricWireBlock> block = GTBlocks.BLOCKS.register(id,
//?}

                () -> new ElectricWireBlock(spec, BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .strength(1.0f, 2.0f)
                        .sound(SoundType.METAL)
                        .noOcclusion()
                        // Arms depend on neighbour thickness, so no per-state shape cache.
                        .dynamicShape()));
        (insulated ? ALL_CABLES : ALL_WIRES).add(block);
        ALL.add(block);

//? if >=1.21.1 {
/*        ITEMS.register(id,
*///?} else {
        GTBlocks.BLOCK_ITEMS.register(id,
//?}

                () -> new gregtech.content.item.WireBlockItem(block.get(),
                        new Item.Properties().stacksTo(stackSize(size, insulated))));
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

//? if >=1.21.1 {
/*    public static List<DeferredHolder<Block,ElectricWireBlock>> allWires() {
*///?} else {
    public static List<RegistryObject<ElectricWireBlock>> allWires() {
//?}

        return Collections.unmodifiableList(ALL_WIRES);
    }

    /** All insulated cable blocks. */

//? if >=1.21.1 {
/*    public static List<DeferredHolder<Block,ElectricWireBlock>> allCables() {
*///?} else {
    public static List<RegistryObject<ElectricWireBlock>> allCables() {
//?}

        return Collections.unmodifiableList(ALL_CABLES);
    }

    /** All wire + cable blocks in registration order. */

//? if >=1.21.1 {
/*    public static List<DeferredHolder<Block,ElectricWireBlock>> all() {
*///?} else {
    public static List<RegistryObject<ElectricWireBlock>> all() {
//?}

        return Collections.unmodifiableList(ALL);
    }

    public static void bootstrap() {
        if (ALL_WIRES.isEmpty()) throw new IllegalStateException("GTWires failed to initialize");
    }
}
