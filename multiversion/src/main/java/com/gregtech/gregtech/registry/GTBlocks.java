package com.gregtech.gregtech.registry;

//? if >=1.21.1 {
//?} else {
import com.gregtech.gregtech.GregTech;
//?}
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.prefix.BlockMaterialPrefix;
import com.gregtech.gregtech.block.stone.GTStoneBlock;
import com.gregtech.gregtech.block.stone.GTStoneSlabBlock;
import com.gregtech.gregtech.block.stone.GTStoneSlabBlockItem;
import com.gregtech.gregtech.block.stone.StoneType;
import com.gregtech.gregtech.block.stone.StoneVariant;
import com.gregtech.gregtech.block.MaterialBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
*///?} else {
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
//?}

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class GTBlocks {
//? if >=1.21.1 {
    /*public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, "gt6ce");
    public static final DeferredRegister<Item> BLOCK_ITEMS = DeferredRegister.create(Registries.ITEM, "gt6ce");
*///?} else {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, GregTech.NAMESPACE);
    public static final DeferredRegister<Item> BLOCK_ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, GregTech.NAMESPACE);
//?}

//? if >=1.21.1 {
    /*private static final Map<String, DeferredHolder<Block, Block>> BY_KEY = new HashMap<>();
    private static final Map<String, DeferredHolder<Block, Block>> STONES_BY_ID = new HashMap<>();
    private static final Map<String, DeferredHolder<Block, Block>> STONE_SLABS_BY_ID = new HashMap<>();
*///?} else {
    private static final Map<String, RegistryObject<Block>> BY_KEY = new HashMap<>();
    private static final Map<String, RegistryObject<Block>> STONES_BY_ID = new HashMap<>();
    private static final Map<String, RegistryObject<Block>> STONE_SLABS_BY_ID = new HashMap<>();
//?}

    /** Ground pebble carrying a material (GT6 surface rocks — vein indicators). */
//? if >=1.21.1 {
    /*public static final DeferredHolder<Block, Block> ROCK = BLOCKS.register("rock", () ->
*///?} else {
    public static final RegistryObject<Block> ROCK = BLOCKS.register("rock", () ->
//?}
            new com.gregtech.gregtech.block.RockBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                    .strength(0.25f, 0f).noCollission().noOcclusion().sound(net.minecraft.world.level.block.SoundType.STONE)));

    /** Twigs on the forest floor (GT6 WorldgenSticks) — drop vanilla sticks. */
//? if >=1.21.1 {
    /*public static final DeferredHolder<Block, Block> TWIGS = BLOCKS.register("twigs", () ->
*///?} else {
    public static final RegistryObject<Block> TWIGS = BLOCKS.register("twigs", () ->
//?}
            new com.gregtech.gregtech.block.TwigBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                    .instabreak().noCollission().noOcclusion().sound(net.minecraft.world.level.block.SoundType.WOOD)));

    private GTBlocks() {}

//? if >=1.21.1 {
    /*public static void bind(BlockMaterialPrefix prefix, GTMaterial material, DeferredHolder<Block, Block> block) {
*///?} else {
    public static void bind(BlockMaterialPrefix prefix, GTMaterial material, RegistryObject<Block> block) {
//?}
        BY_KEY.put(key(prefix, material), block);
    }

//? if >=1.21.1 {
    /*public static void bindStone(String id, DeferredHolder<Block, Block> block) {
*///?} else {
    public static void bindStone(String id, RegistryObject<Block> block) {
//?}
        BY_KEY.put("stone/" + id, block);
        STONES_BY_ID.put(id, block);
    }

//? if >=1.21.1 {
    /*public static void bindStoneSlab(String id, DeferredHolder<Block, Block> block) {
*///?} else {
    public static void bindStoneSlab(String id, RegistryObject<Block> block) {
//?}
        BY_KEY.put("stone_slab/" + id, block);
        STONE_SLABS_BY_ID.put(id, block);
    }

    public static String key(BlockMaterialPrefix prefix, GTMaterial material) {
        return prefix.getName() + "/" + material.getName();
    }

//? if >=1.21.1 {
    /*public static DeferredHolder<Block, Block> getObject(BlockMaterialPrefix prefix, GTMaterial material) {
*///?} else {
    public static RegistryObject<Block> getObject(BlockMaterialPrefix prefix, GTMaterial material) {
//?}
        return BY_KEY.get(key(prefix, material));
    }

    public static ItemStack getStack(BlockMaterialPrefix prefix, GTMaterial material) {
//? if >=1.21.1 {
        /*DeferredHolder<Block, Block> block = getObject(prefix, material);
        if (block == null || !block.isBound()) {
*///?} else {
        RegistryObject<Block> block = getObject(prefix, material);
        if (block == null || !block.isPresent()) {
//?}
            return ItemStack.EMPTY;
        }
        return new ItemStack(block.get(), 1);
    }

    public static ItemStack getCreativeStack(BlockMaterialPrefix prefix, GTMaterial material) {
//? if >=1.21.1 {
        /*DeferredHolder<Block, Block> block = getObject(prefix, material);
        if (block == null || !block.isBound()) {
*///?} else {
        RegistryObject<Block> block = getObject(prefix, material);
        if (block == null || !block.isPresent()) {
//?}
            return ItemStack.EMPTY;
        }
        return new ItemStack(block.get().asItem(), 1);
    }

//? if >=1.21.1 {
    /*public static Collection<DeferredHolder<Block, Block>> allEntries() {
*///?} else {
    public static Collection<RegistryObject<Block>> allEntries() {
//?}
        return Collections.unmodifiableCollection(BY_KEY.values());
    }

    public static boolean hasBoundBlocks(BlockMaterialPrefix prefix) {
        String prefixKey = prefix.getName() + "/";
        for (String key : BY_KEY.keySet()) {
            if (key.startsWith(prefixKey)) {
                return true;
            }
        }
        return false;
    }

//? if >=1.21.1 {
    /*public static DeferredHolder<Item, Item> registerBlockItem(String id, DeferredHolder<Block, Block> block) {
        return BLOCK_ITEMS.register(id, () -> block.get() instanceof net.minecraft.world.level.block.WaterlilyBlock
                ? new net.minecraft.world.item.PlaceOnWaterBlockItem(block.get(), new Item.Properties())
                : new com.gregtech.gregtech.block.MaterialBlockItem(block.get(), new Item.Properties().stacksTo(materialStackLimit(block.get()))));
*///?} else {
    public static RegistryObject<Item> registerBlockItem(String id, RegistryObject<Block> block) {
        return BLOCK_ITEMS.register(id, () -> new com.gregtech.gregtech.block.MaterialBlockItem(block.get(), new Item.Properties().stacksTo(materialStackLimit(block.get()))));
//?}
    }

    private static int materialStackLimit(Block block) {
        if(block instanceof com.gregtech.gregtech.block.MaterialBlockLike material) {
            var prefix=material.prefix();
            if(prefix==BlockMaterialPrefix.casingMachine)return 8;
            if(prefix==BlockMaterialPrefix.casingMachineDouble)return 4;
            if(prefix==BlockMaterialPrefix.casingMachineQuadruple)return 2;
            if(prefix==BlockMaterialPrefix.casingMachineDense)return 1;
        }
        return 64;
    }

//? if >=1.21.1 {
    /*public static DeferredHolder<Item, Item> registerStoneSlabItem(String id, DeferredHolder<Block, Block> block) {
*///?} else {
    public static RegistryObject<Item> registerStoneSlabItem(String id, RegistryObject<Block> block) {
//?}
        return BLOCK_ITEMS.register(id, () -> new GTStoneSlabBlockItem(block.get(), new Item.Properties()));
    }

    @javax.annotation.Nullable
    public static Block getStoneSlab(StoneType type, StoneVariant variant) {
        String id = type.registryId() + "_" + variant.registrySuffix() + "_slab";
//? if >=1.21.1 {
        /*DeferredHolder<Block, Block> block = STONE_SLABS_BY_ID.get(id);
        return block == null || !block.isBound() ? null : block.get();
*///?} else {
        RegistryObject<Block> block = STONE_SLABS_BY_ID.get(id);
        return block == null || !block.isPresent() ? null : block.get();
//?}
    }

    @javax.annotation.Nullable
    public static Block getStone(StoneType type, StoneVariant variant) {
        String id = type.registryId() + "_" + variant.registrySuffix();
//? if >=1.21.1 {
        /*DeferredHolder<Block, Block> block = STONES_BY_ID.get(id);
        return block == null || !block.isBound() ? null : block.get();
*///?} else {
        RegistryObject<Block> block = STONES_BY_ID.get(id);
        return block == null || !block.isPresent() ? null : block.get();
//?}
    }

    @javax.annotation.Nullable
    public static BlockState getStoneState(StoneType type, StoneVariant variant) {
        Block block = getStone(type, variant);
        return block == null ? null : block.defaultBlockState();
    }
}
