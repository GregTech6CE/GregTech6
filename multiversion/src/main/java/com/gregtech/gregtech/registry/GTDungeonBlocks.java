package com.gregtech.gregtech.registry;

//? if >=1.21.1 {
/*import com.gregtech.gregtech.registry.*;
*///?} else {
//?}
import com.gregtech.gregtech.block.misc.DungeonPortalBlock;
import com.gregtech.gregtech.content.plant.BedrockFlowers;

//? if >=1.21.1 {
//?} else {
import com.gregtech.gregtech.GregTech;
//?}
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.core.registries.BuiltInRegistries;
*///?} else {
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
//?}

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Craftable GT6 miniature portals (source multi-tiles 32766/32000).
 * These relay to another dimension; dungeon rooms separately use vanilla Nether/End portals.
 * Existing registry ids preserve saves. Source hardness and sixteen-item stack size are retained.
 */
public final class GTDungeonBlocks {

    /** GT6's {@code MultiTileEntityMiniPortalNether} (32766) as a craftable cross-dimension relay. */
//? if >=1.21.1 {
    /*public static DeferredHolder<Block,DungeonPortalBlock> PORTAL_NETHER;
*///?} else {
    public static RegistryObject<DungeonPortalBlock> PORTAL_NETHER;
//?}
    /** GT6's {@code MultiTileEntityMiniPortalEnd} (32000) as a craftable cross-dimension relay. */
//? if >=1.21.1 {
    /*public static DeferredHolder<Block,DungeonPortalBlock> PORTAL_END;
*///?} else {
    public static RegistryObject<DungeonPortalBlock> PORTAL_END;
//?}

//? if >=1.21.1 {
    /*private static final List<DeferredHolder<Block,? extends Block>> ALL = new ArrayList<>();
    private static final Map<String, DeferredHolder<Block,FlowerPotBlock>> POTTED_FLOWERS = new LinkedHashMap<>();
*///?} else {
    private static final List<RegistryObject<? extends Block>> ALL = new ArrayList<>();
    private static final Map<String, RegistryObject<FlowerPotBlock>> POTTED_FLOWERS = new LinkedHashMap<>();
//?}

    private GTDungeonBlocks() {}

//? if >=1.21.1 {
    /*public static List<DeferredHolder<Block,? extends Block>> all() {
*///?} else {
    public static List<RegistryObject<? extends Block>> all() {
//?}
        return Collections.unmodifiableList(ALL);
    }

    /** One GT6 indicator flower in a vanilla-shaped pot; the pot itself is not a separate inventory item. */
    public static Block pottedFlower(String flowerId) {
//? if >=1.21.1 {
        /*DeferredHolder<Block,FlowerPotBlock> pot = POTTED_FLOWERS.get(flowerId);
        if (pot == null || !pot.isBound())
*///?} else {
        RegistryObject<FlowerPotBlock> pot = POTTED_FLOWERS.get(flowerId);
        if (pot == null || !pot.isPresent())
//?}
            throw new IllegalStateException("Missing dungeon potted flower " + flowerId);
        return pot.get();
    }

    /** Forge keeps the pot-content map separate from block registration. */
    public static void registerPotPlants() {
        FlowerPotBlock empty = (FlowerPotBlock) Blocks.FLOWER_POT;
        for (String id : POTTED_FLOWERS.keySet()) {
//? if >=1.21.1 {
            /*Block flower = BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce",id));
*///?} else {
            Block flower = ForgeRegistries.BLOCKS.getValue(GregTech.id(id));
//?}
            if (flower == null) throw new IllegalStateException("Missing GT6 indicator flower " + id);
//? if >=1.21.1 {
            /*empty.addPlant(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce",id), POTTED_FLOWERS.get(id));
*///?} else {
            empty.addPlant(GregTech.id(id), POTTED_FLOWERS.get(id));
//?}
        }
    }

    /** Current portal properties: source hardness, with port-specific collision and lighting settings. */
    private static BlockBehaviour.Properties properties(float hardness, float resistance) {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(hardness, resistance)
                .sound(SoundType.GLASS)
                .noCollission()
                .noOcclusion()
                .lightLevel(state -> state.getValue(DungeonPortalBlock.ACTIVE) ? 11 : 0);
    }

//? if >=1.21.1 {
    /*private static DeferredHolder<Block,DungeonPortalBlock> reg(String id, Supplier<DungeonPortalBlock> block) {
        DeferredHolder<Block,DungeonPortalBlock> registered = GTBlocks.BLOCKS.register(id, block);
*///?} else {
    private static RegistryObject<DungeonPortalBlock> reg(String id, Supplier<DungeonPortalBlock> block) {
        RegistryObject<DungeonPortalBlock> registered = GTBlocks.BLOCKS.register(id, block);
//?}
        ALL.add(registered);
        GTBlocks.BLOCK_ITEMS.register(id, () -> new BlockItem(registered.get(), new Item.Properties().stacksTo(16)));
        return registered;
    }

    public static void registerAll() {
        if (!ALL.isEmpty()) return;
        PORTAL_NETHER = reg("dungeon_portal_nether",
                () -> new DungeonPortalBlock(DungeonPortalBlock.Target.NETHER, properties(50.0F, 1200.0F)));
        PORTAL_END = reg("dungeon_portal_end",
                () -> new DungeonPortalBlock(DungeonPortalBlock.Target.END, properties(3.0F, 9.0F)));
        for (BedrockFlowers.Flower flower : BedrockFlowers.ALL) {
            String id = flower.id();
//? if >=1.21.1 {
            /*DeferredHolder<Block,FlowerPotBlock> potted = GTBlocks.BLOCKS.register("potted_" + id,
*///?} else {
            RegistryObject<FlowerPotBlock> potted = GTBlocks.BLOCKS.register("potted_" + id,
//?}
                    () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT,
//? if >=1.21.1 {
                            /*() -> BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce",id)),
                            BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_POPPY)));
*///?} else {
                            () -> ForgeRegistries.BLOCKS.getValue(GregTech.id(id)),
                            BlockBehaviour.Properties.copy(Blocks.POTTED_POPPY)));
//?}
            POTTED_FLOWERS.put(id, potted);
            ALL.add(potted);
        }
    }
}
