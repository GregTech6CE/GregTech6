package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.prefix.BlockMaterialPrefix;
import com.gregtech.gregtech.api.prefix.BlockPrefixRegistry;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.api.prefix.PrefixRegistry;
import com.gregtech.gregtech.block.stone.StoneType;
import com.gregtech.gregtech.block.stone.StoneVariant;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.data.generated.GT6Materials;
import com.gregtech.gregtech.item.CreativeTabIconItem;
import com.gregtech.gregtech.item.GTToolItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class GTCreativeTabIcons {
    public static final DeferredRegister<Item> ICONS = DeferredRegister.create(ForgeRegistries.ITEMS, GregTech.NAMESPACE);

    private static final Map<MaterialPrefix, RegistryObject<Item>> BY_MATERIAL_PREFIX = new HashMap<>();
    private static final Map<BlockMaterialPrefix, RegistryObject<Item>> BY_BLOCK_PREFIX = new HashMap<>();
    public static RegistryObject<Item> TOOLS;
    public static RegistryObject<Item> STONES;

    private GTCreativeTabIcons() {}

    public static void register() {
        PrefixRegistry.ensurePrefixesLoaded();
        for (MaterialPrefix prefix : PrefixRegistry.all()) {
            if (prefix.isHiddenFromCreative()) {
                continue;
            }
            MaterialPrefix bound = prefix;
            BY_MATERIAL_PREFIX.put(prefix, ICONS.register("tab_icon_" + bound.getRegistryName(),
                    () -> new CreativeTabIconItem(new Item.Properties(), bound, null)));
        }

        BlockPrefixRegistry.ensurePrefixesLoaded();
        for (BlockMaterialPrefix prefix : BlockPrefixRegistry.all()) {
            if (prefix.isPartialCrate()) continue;
            BlockMaterialPrefix bound = prefix;
            BY_BLOCK_PREFIX.put(prefix, ICONS.register("tab_icon_block_" + bound.getRegistryName(),
                    () -> new CreativeTabIconItem(new Item.Properties(), null, bound)));
        }

        TOOLS = ICONS.register("tab_icon_tools",
                () -> new CreativeTabIconItem(new Item.Properties(), MaterialPrefix.toolHeadPickaxe, null));
        STONES = ICONS.register("tab_icon_stones",
                () -> new CreativeTabIconItem(new Item.Properties(), null, BlockMaterialPrefix.blockRaw));
    }

    public static ItemStack stackFor(MaterialPrefix prefix) {
        RegistryObject<Item> icon = BY_MATERIAL_PREFIX.get(prefix);
        return icon == null ? ItemStack.EMPTY : new ItemStack(icon.get());
    }

    public static ItemStack stackFor(BlockMaterialPrefix prefix) {
        RegistryObject<Item> icon = BY_BLOCK_PREFIX.get(prefix);
        return icon == null ? ItemStack.EMPTY : new ItemStack(icon.get());
    }

    public static ItemStack toolsStack() {
        ItemStack pick = GTToolItem.create(GTToolType.PICKAXE, com.gregtech.gregtech.content.material.generated.ElementMaterials.Iron, com.gregtech.gregtech.content.material.generated.WoodMaterials.Wood);
        return pick.isEmpty() && TOOLS != null ? new ItemStack(TOOLS.get()) : pick;
    }

    public static ItemStack stonesStack() {
        Block block = GTBlocks.getStone(StoneType.GRANITE_RED, StoneVariant.SMOOTH);
        if (block != null) {
            return new ItemStack(block);
        }
        return STONES == null ? ItemStack.EMPTY : new ItemStack(STONES.get());
    }
    public static Iterable<RegistryObject<Item>> allEntries() {
        return Collections.unmodifiableCollection(ICONS.getEntries());
    }
}
