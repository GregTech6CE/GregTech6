package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.api.energy.EnergyNodeSpec;
import com.gregtech.gregtech.content.energy.GearboxCatalog.Tier;
import com.gregtech.gregtech.api.energy.GearboxSpec;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.block.energy.EnergyNodeBlock;
import com.gregtech.gregtech.block.energy.GearboxBlock;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.content.material.generated.WoodMaterials;
import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.world.item.BlockItem;
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

/** The 13 original GT6 custom gearboxes and RU rotation transformers. */
public final class GTGearboxes {
    private static final List<DeferredHolder<Block,GearboxBlock>> ALL_GEARBOXES = new ArrayList<>();
    private static final List<DeferredHolder<Block,EnergyNodeBlock>> ALL_TRANSFORMERS = new ArrayList<>();

    // GT6 Loader_MultiTileEntities.kinetic(): 24778–24899. Registry names of
    // the existing variants remain stable; the source's numeric IDs are in the
    // resource generator so that rates and names can be checked against GT6.
    private static final com.gregtech.gregtech.content.energy.GearboxCatalog.Tier[] TIERS=com.gregtech.gregtech.content.energy.GearboxCatalog.tiers();

    public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(Registries.BLOCK,"gregtech");
    public static final DeferredRegister<net.minecraft.world.item.Item> ITEMS=DeferredRegister.create(Registries.ITEM,"gregtech");
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,"gregtech");
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<com.gregtech.gregtech.blockentity.energy.GearboxBlockEntity>> GEARBOX=ENTITIES.register("gearbox",()->BlockEntityType.Builder.of(com.gregtech.gregtech.blockentity.energy.GearboxBlockEntity::new,allGearboxes().stream().map(DeferredHolder::get).toArray(Block[]::new)).build(null));
    private GTGearboxes() {}
    public static void register(IEventBus bus){if(!ALL_GEARBOXES.isEmpty())throw new IllegalStateException("Gearboxes registered twice");registerAll();bootstrap();BLOCKS.register(bus);ITEMS.register(bus);ENTITIES.register(bus);}


    public static List<DeferredHolder<Block,GearboxBlock>> allGearboxes() { return Collections.unmodifiableList(ALL_GEARBOXES); }
    public static List<DeferredHolder<Block,EnergyNodeBlock>> allTransformers() { return Collections.unmodifiableList(ALL_TRANSFORMERS); }

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
        GearboxSpec spec = com.gregtech.gregtech.content.energy.GearboxCatalog.gearbox(tier);
        boolean wooden = tier.voltageTier() == 0;
        DeferredHolder<Block,GearboxBlock> block = BLOCKS.register(id,
                () -> new GearboxBlock(spec, BlockBehaviour.Properties.of()
                        .mapColor(wooden ? MapColor.WOOD : MapColor.METAL)
                        .strength(6.0f, 6.0f)
                        .sound(wooden ? SoundType.WOOD : SoundType.METAL)
                        .requiresCorrectToolForDrops()
                        .noOcclusion()));
        ALL_GEARBOXES.add(block);
        ITEMS.register(id,
                () -> new BlockItem(block.get(), new Item.Properties().stacksTo(16)));
    }

    private static void registerTransformer(Tier tier) {
        // NBT_INPUT = V[t], NBT_OUTPUT = V[t-1] (wood: 2); multiplier 4.
        int t=tier.voltageTier();String id="rotation_transformer_"+tier.suffix();
        EnergyNodeSpec spec=com.gregtech.gregtech.content.energy.GearboxCatalog.transformer(tier);
        boolean wooden = t == 0;
        DeferredHolder<Block,EnergyNodeBlock> block = BLOCKS.register(id,
                () -> new EnergyNodeBlock(spec, BlockBehaviour.Properties.of()
                        .mapColor(wooden ? MapColor.WOOD : MapColor.METAL)
                        .strength(6.0f, 6.0f)
                        .sound(wooden ? SoundType.WOOD : SoundType.METAL)
                        .requiresCorrectToolForDrops()));
        ALL_TRANSFORMERS.add(block);
        ITEMS.register(id,
                () -> new BlockItem(block.get(), new Item.Properties().stacksTo(16)));
    }

    public static void bootstrap() {
        if (ALL_GEARBOXES.size() != 13 || ALL_TRANSFORMERS.size() != 13)
            throw new IllegalStateException("GTGearboxes must register the original 13+13 variants");
    }
}
