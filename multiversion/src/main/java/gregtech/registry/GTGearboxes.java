package gregtech.registry;

import gregtech.internal.energy.EnergyNodeSpec;
import gregtech.content.energy.GearboxCatalog.Tier;
import gregtech.internal.energy.GearboxSpec;
import gregtech.block.energy.EnergyNodeBlock;
import gregtech.block.energy.GearboxBlock;
import net.minecraft.world.item.BlockItem;
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

/** The 13 original GT6 custom gearboxes and RU rotation transformers. */
public final class GTGearboxes {
//? if >=1.21.1 {
    /*private static final List<DeferredHolder<Block, GearboxBlock>> ALL_GEARBOXES = new ArrayList<>();
    private static final List<DeferredHolder<Block, EnergyNodeBlock>> ALL_TRANSFORMERS = new ArrayList<>();
*///?} else {
    private static final List<RegistryObject<GearboxBlock>> ALL_GEARBOXES = new ArrayList<>();
    private static final List<RegistryObject<EnergyNodeBlock>> ALL_TRANSFORMERS = new ArrayList<>();
//?}

    // GT6 Loader_MultiTileEntities.kinetic(): 24778–24899. Registry names of
    // the existing variants remain stable; the source's numeric IDs are in the
    // resource generator so that rates and names can be checked against GT6.
    private static final gregtech.content.energy.GearboxCatalog.Tier[] TIERS = gregtech.content.energy.GearboxCatalog.tiers();

//? if >=1.21.1 {
    /*public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, "gt6ce");
    public static final DeferredRegister<net.minecraft.world.item.Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6ce");
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "gt6ce");
    public static final DeferredHolder<BlockEntityType<?>,
            BlockEntityType<gregtech.blockentity.energy.GearboxBlockEntity>> GEARBOX = ENTITIES.register("gearbox",
            () -> BlockEntityType.Builder.of(gregtech.blockentity.energy.GearboxBlockEntity::new,
            allGearboxes().stream().map(DeferredHolder::get).toArray(Block[]::new)).build(null));
*///?} else {
//?}
    private GTGearboxes() {
    }
//? if >=1.21.1 {
    /*public static void register(IEventBus bus) {
        if (!ALL_GEARBOXES.isEmpty()) throw new IllegalStateException("Gearboxes registered twice");
        registerAll();
        bootstrap();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
    }
*///?} else {
//?}

//? if >=1.21.1 {

    /*public static List<DeferredHolder<Block, GearboxBlock>> allGearboxes() {
        return Collections.unmodifiableList(ALL_GEARBOXES);
    }

    public static List<DeferredHolder<Block, EnergyNodeBlock>> allTransformers() {
        return Collections.unmodifiableList(ALL_TRANSFORMERS);
    }
*///?} else {
    public static List<RegistryObject<GearboxBlock>> allGearboxes() {
        return Collections.unmodifiableList(ALL_GEARBOXES);
    }

    public static List<RegistryObject<EnergyNodeBlock>> allTransformers() {
        return Collections.unmodifiableList(ALL_TRANSFORMERS);
    }
//?}

    public static void registerAll() {
        for (Tier tier : TIERS) {
            registerGearbox(tier);
            registerTransformer(tier);
        }
    }

    private static void registerGearbox(Tier tier) {
        String id = "gearbox_" + tier.suffix();
        // The original custom gearbox only has NBT_INPUT = VMAX[t]. maxPower
        // is a legacy display field in this port and must not limit transfers.
        GearboxSpec spec = gregtech.content.energy.GearboxCatalog.gearbox(tier);
        boolean wooden = tier.voltageTier() == 0;
//? if >=1.21.1 {
        /*DeferredHolder<Block, GearboxBlock> block = BLOCKS.register(id,
*///?} else {
        RegistryObject<GearboxBlock> block = GTBlocks.BLOCKS.register(id,
//?}
                () -> new GearboxBlock(spec, machineProperties(wooden).noOcclusion()));
        ALL_GEARBOXES.add(block);
//? if >=1.21.1 {
        /*ITEMS.register(id,
*///?} else {
        GTBlocks.BLOCK_ITEMS.register(id,
//?}
                () -> new BlockItem(block.get(), new Item.Properties().stacksTo(16)));
    }

    private static void registerTransformer(Tier tier) {
        // NBT_INPUT = V[t], NBT_OUTPUT = V[t-1] (wood: 2); multiplier 4.
        int voltageTierIndex = tier.voltageTier();
        String id = "rotation_transformer_" + tier.suffix();
        EnergyNodeSpec spec = gregtech.content.energy.GearboxCatalog.transformer(tier);
        boolean wooden = voltageTierIndex == 0;
//? if >=1.21.1 {
        /*DeferredHolder<Block, EnergyNodeBlock> block = BLOCKS.register(id,
*///?} else {
        RegistryObject<EnergyNodeBlock> block = GTBlocks.BLOCKS.register(id,
//?}
                () -> new EnergyNodeBlock(spec, machineProperties(wooden)));
        ALL_TRANSFORMERS.add(block);
//? if >=1.21.1 {
        /*ITEMS.register(id,
*///?} else {
        GTBlocks.BLOCK_ITEMS.register(id,
//?}
                () -> new BlockItem(block.get(), new Item.Properties().stacksTo(16)));
    }

    public static void bootstrap() {
        if (ALL_GEARBOXES.size() != 13 || ALL_TRANSFORMERS.size() != 13)
            throw new IllegalStateException("GTGearboxes must register the original 13+13 variants");
    }

    private static BlockBehaviour.Properties machineProperties(boolean wooden) {
        return BlockBehaviour.Properties.of()
                .mapColor(wooden ? MapColor.WOOD : MapColor.METAL)
                .strength(6.0f, 6.0f)
                .sound(wooden ? SoundType.WOOD : SoundType.METAL)
                .requiresCorrectToolForDrops();
    }
}
