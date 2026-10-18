package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.content.material.generated.WoodMaterials;
import com.gregtech.gregtech.api.energy.AxleSpec;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.block.energy.AxleBlock;
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

/** GT6 rotational axles — RU conductors in several material/size tiers. */
public final class GTAxles {
//? if >=1.21.1 {
    /*private static final List<DeferredHolder<Block,AxleBlock>> ALL = new ArrayList<>();
*///?} else {
    private static final List<RegistryObject<AxleBlock>> ALL = new ArrayList<>();
//?}

//? if >=1.21.1 {
    /*public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(Registries.BLOCK,"gt6ce");
    public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Registries.ITEM,"gt6ce");
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,"gt6ce");
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<com.gregtech.gregtech.blockentity.energy.AxleBlockEntity>> AXLE=ENTITIES.register("axle",()->BlockEntityType.Builder.of(com.gregtech.gregtech.blockentity.energy.AxleBlockEntity::new,all().stream().map(DeferredHolder::get).toArray(Block[]::new)).build(null));
*///?} else {
//?}
    private GTAxles() {}
//? if >=1.21.1 {
    /*public static void register(IEventBus bus){if(!ALL.isEmpty())throw new IllegalStateException("Axles registered twice");registerAll();bootstrap();BLOCKS.register(bus);ITEMS.register(bus);ENTITIES.register(bus);}
*///?} else {
//?}

//? if >=1.21.1 {

    /*public static List<DeferredHolder<Block,AxleBlock>> all() {
*///?} else {
    public static List<RegistryObject<AxleBlock>> all() {
//?}
        return Collections.unmodifiableList(ALL);
    }

    private static void add(AxleSpec spec) {
//? if >=1.21.1 {
        /*DeferredHolder<Block,AxleBlock> block = BLOCKS.register(spec.id(),
*///?} else {
        RegistryObject<AxleBlock> block = GTBlocks.BLOCKS.register(spec.id(),
//?}
                () -> new AxleBlock(spec, BlockBehaviour.Properties.of()
                        .mapColor(spec.material() == WoodMaterials.WoodTreated ? MapColor.WOOD : MapColor.METAL)
                        .strength(2.0f, 6.0f)
                        .sound(spec.material() == WoodMaterials.WoodTreated ? SoundType.WOOD : SoundType.METAL)
                        .noOcclusion()));
        ALL.add(block);
//? if >=1.21.1 {
        /*ITEMS.register(spec.id(),
*///?} else {
        GTBlocks.BLOCK_ITEMS.register(spec.id(),
//?}
                () -> new net.minecraft.world.item.BlockItem(block.get(), new Item.Properties().stacksTo(spec.size()<=2?64:spec.size()==3?32:16)));
    }

    public static void registerAll(){com.gregtech.gregtech.content.energy.AxleCatalog.specifications().forEach(GTAxles::add);}

    public static void bootstrap() {
        if (ALL.isEmpty()) throw new IllegalStateException("GTAxles failed to initialize");
    }
}
