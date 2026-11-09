package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.block.stone.StoneType;
import gregtech.content.block.stone.StoneVariant;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.item.CreativeTabIconItem;
import gregtech.content.item.GTToolItem;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.engine.prefix.BlockPrefixRegistry;
import gregtech.engine.prefix.PrefixRegistry;
import gregtech.engine.tool.GTToolType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class GTCreativeTabIcons {
    private static final Map<MaterialPrefix, RegisteredItem<? extends Item>> BY_MATERIAL_PREFIX = new HashMap<>();
    private static final Map<BlockMaterialPrefix, RegisteredItem<? extends Item>> BY_BLOCK_PREFIX = new HashMap<>();
    private static final List<RegisteredItem<? extends Item>> ALL_ICONS = new ArrayList<>();
    public static RegisteredItem<? extends Item> TOOLS;
    public static RegisteredItem<? extends Item> STONES;

    private GTCreativeTabIcons() {}

    public static void register(ContentRegistrar registrar) {
        if (!ALL_ICONS.isEmpty()) throw new IllegalStateException("Creative tab icons registered twice");
        PrefixRegistry.ensurePrefixesLoaded();
        for (MaterialPrefix prefix : PrefixRegistry.all()) {
            if (prefix.isHiddenFromCreative() || prefix.isAddon() || !GTItems.hasBoundItems(prefix)) {
                continue;
            }
            MaterialPrefix bound = prefix;
            var icon = registrar.item(
                    REGISTRY_NAMESPACE,
                    "tab_icon_" + bound.getRegistryName(),
                    () -> new CreativeTabIconItem(new Item.Properties(), bound, null));
            BY_MATERIAL_PREFIX.put(prefix, icon);
            ALL_ICONS.add(icon);
        }

        BlockPrefixRegistry.ensurePrefixesLoaded();
        for (BlockMaterialPrefix prefix : BlockPrefixRegistry.all()) {
            if (!GTBlocks.hasBoundBlocks(prefix)) {
                continue;
            }
            BlockMaterialPrefix bound = prefix;
            var icon = registrar.item(
                    REGISTRY_NAMESPACE,
                    "tab_icon_block_" + bound.getRegistryName(),
                    () -> new CreativeTabIconItem(new Item.Properties(), null, bound));
            BY_BLOCK_PREFIX.put(prefix, icon);
            ALL_ICONS.add(icon);
        }

        TOOLS = registrar.item(
                REGISTRY_NAMESPACE,
                "tab_icon_tools",
                () -> new CreativeTabIconItem(new Item.Properties(), MaterialPrefix.toolHeadPickaxe, null));
        STONES = registrar.item(
                REGISTRY_NAMESPACE,
                "tab_icon_stones",
                () -> new CreativeTabIconItem(new Item.Properties(), null, BlockMaterialPrefix.blockRaw));
        ALL_ICONS.add(TOOLS);
        ALL_ICONS.add(STONES);
    }

    public static ItemStack stackFor(MaterialPrefix prefix) {
        RegisteredItem<? extends Item> icon = BY_MATERIAL_PREFIX.get(prefix);
        return icon == null ? ItemStack.EMPTY : new ItemStack(icon.get());
    }

    public static ItemStack stackFor(BlockMaterialPrefix prefix) {
        RegisteredItem<? extends Item> icon = BY_BLOCK_PREFIX.get(prefix);
        return icon == null ? ItemStack.EMPTY : new ItemStack(icon.get());
    }

    public static ItemStack toolsStack() {
        ItemStack pick = GTToolItem.create(
                GTToolType.PICKAXE,
                gregtech.content.material.generated.ElementMaterials.Iron,
                gregtech.content.material.generated.WoodMaterials.Wood);
        return pick.isEmpty() && TOOLS != null ? new ItemStack(TOOLS.get()) : pick;
    }

    public static ItemStack stonesStack() {
        net.minecraft.world.level.block.Block block = GTBlocks.getStone(StoneType.GRANITE_RED, StoneVariant.SMOOTH);
        if (block != null) {
            return new ItemStack(block);
        }
        return STONES == null ? ItemStack.EMPTY : new ItemStack(STONES.get());
    }

    public static Iterable<RegisteredItem<? extends Item>> allEntries() {
        return Collections.unmodifiableCollection(ALL_ICONS);
    }
}
