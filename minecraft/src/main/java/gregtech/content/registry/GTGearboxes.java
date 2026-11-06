package gregtech.content.registry;

import gregtech.engine.energy.EnergyNodeSpec;
import gregtech.content.energy.GearboxCatalog.Tier;
import gregtech.engine.energy.GearboxSpec;
import gregtech.content.block.energy.EnergyNodeBlock;
import gregtech.content.block.energy.GearboxBlock;
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
//?}

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Material variants of GT6 custom gearboxes and RU rotation transformers. */
public final class GTGearboxes {
    private static final List<RegisteredBlock<GearboxBlock>> ALL_GEARBOXES = new ArrayList<>();
    private static final List<RegisteredBlock<EnergyNodeBlock>> ALL_TRANSFORMERS = new ArrayList<>();

    // GT6 Loader_MultiTileEntities.kinetic(): 24778–24899. Registry names of
    // the existing variants remain stable; the source's numeric IDs are in the
    // resource generator so that rates and names can be checked against GT6.
    private static final gregtech.content.energy.GearboxCatalog.Tier[] TIERS = gregtech.content.energy.GearboxCatalog.tiers();

//? if >=1.21.1 {
    /*public static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE);
    public static final DeferredHolder<BlockEntityType<?>,
            BlockEntityType<gregtech.content.blockentity.energy.GearboxBlockEntity>> GEARBOX = ENTITIES.register("gearbox",
            () -> BlockEntityType.Builder.of(gregtech.content.blockentity.energy.GearboxBlockEntity::new,
            allGearboxes().stream().map(RegisteredBlock::get).toArray(Block[]::new)).build(null));
*///?}
    private GTGearboxes() {
    }
//? if >=1.21.1 {
    /*public static void submit(IEventBus bus) {
        ENTITIES.register(bus);
    }
*///?}

    public static List<RegisteredBlock<GearboxBlock>> allGearboxes() {
        return Collections.unmodifiableList(ALL_GEARBOXES);
    }

    public static List<RegisteredBlock<EnergyNodeBlock>> allTransformers() {
        return Collections.unmodifiableList(ALL_TRANSFORMERS);
    }

    public static void registerAll(ContentRegistrar registrar) {
        for (Tier tier : TIERS) {
            registerGearbox(registrar, tier);
            registerTransformer(registrar, tier);
        }
    }

    private static void registerGearbox(ContentRegistrar registrar, Tier tier) {
        String id = "gearbox_" + tier.suffix();
        // The original custom gearbox only has NBT_INPUT = VMAX[t]. maxPower
        // is a legacy display field in this port and must not limit transfers.
        GearboxSpec spec = gregtech.content.energy.GearboxCatalog.gearbox(tier);
        boolean wooden = tier.voltageTier() == 0;
        RegisteredBlock<GearboxBlock> block = registrar.block(
                gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE,
                id,
                () -> new GearboxBlock(spec, machineProperties(wooden).noOcclusion()),
                gearbox -> new BlockItem(gearbox, new Item.Properties().stacksTo(16)));
        ALL_GEARBOXES.add(block);
    }

    private static void registerTransformer(ContentRegistrar registrar, Tier tier) {
        // NBT_INPUT = V[t], NBT_OUTPUT = V[t-1] (wood: 2); multiplier 4.
        int voltageTierIndex = tier.voltageTier();
        String id = "rotation_transformer_" + tier.suffix();
        EnergyNodeSpec spec = gregtech.content.energy.GearboxCatalog.transformer(tier);
        boolean wooden = voltageTierIndex == 0;
        RegisteredBlock<EnergyNodeBlock> block = registrar.block(
                gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE,
                id,
                () -> new EnergyNodeBlock(spec, machineProperties(wooden)),
                transformer -> new BlockItem(transformer, new Item.Properties().stacksTo(16)));
        ALL_TRANSFORMERS.add(block);
    }

    public static void bootstrap() {
        if (TIERS.length == 0 || ALL_GEARBOXES.size() != TIERS.length || ALL_TRANSFORMERS.size() != TIERS.length)
            throw new IllegalStateException("GTGearboxes must register every catalog tier exactly once");
    }

    private static BlockBehaviour.Properties machineProperties(boolean wooden) {
        return BlockBehaviour.Properties.of()
                .mapColor(wooden ? MapColor.WOOD : MapColor.METAL)
                .strength(6.0f, 6.0f)
                .sound(wooden ? SoundType.WOOD : SoundType.METAL)
                .requiresCorrectToolForDrops();
    }
}
