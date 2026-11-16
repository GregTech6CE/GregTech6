package gregtech.compat.common;

import gregtech.content.block.OreBlock;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.item.GTToolItem;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.recipe.RecipeMap;
import gregtech.engine.tool.GTToolType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/** Category identities shared by the two viewers, independent of workstation ordering. */
public final class RecipeViewerIcons {
    private static volatile java.util.Map<RecipeMap, ItemStack> cachedCategories;

    private RecipeViewerIcons() {}

    public static ItemStack item(String path) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce", path)));
    }

    public static ItemStack toolAssembly() {
        return GTToolItem.create(GTToolType.SWORD, GTMaterialRegistry.get("Steel"), GTMaterialRegistry.get("Birch"));
    }

    public static ItemStack bedrockOre() {
        var stack = item("ore_iridium");
        if (stack.getItem() instanceof BlockItem block)
            return OreBlock.withStone(
                    stack,
                    block.getBlock()
                            .defaultBlockState()
                            .setValue(OreBlock.STONE, gregtech.content.block.OreHostStone.BEDROCK));
        return stack;
    }

    /** Use the same category identity for the process graph and the native viewer. */
    public static ItemStack category(RecipeMap map, java.util.Map<RecipeMap, java.util.List<ItemStack>> machines) {
        ItemStack preferred = map(map);
        if (!preferred.isEmpty()) return preferred;
        var workstations = machines.getOrDefault(map, java.util.List.of());
        return workstations.isEmpty()
                ? new ItemStack(net.minecraft.world.item.Items.ANVIL)
                : workstations.get(0).copy();
    }

    public static java.util.Map<RecipeMap, ItemStack> categories() {
        var cached = cachedCategories;
        if (cached != null) return cached;
        synchronized (RecipeViewerIcons.class) {
            if (cachedCategories == null) {
                var machines = RecipeMachines.collect();
                var icons = new java.util.HashMap<RecipeMap, ItemStack>();
                for (RecipeMap map : RecipeMap.RECIPE_MAP_LIST) icons.put(map, category(map, machines));
                cachedCategories = java.util.Map.copyOf(icons);
            }
            return cachedCategories;
        }
    }

    public static ItemStack map(RecipeMap map) {
        if (map == MachineRecipeMaps.ByProductList)
            return GTItems.getStack(MaterialPrefix.dust, GTMaterialRegistry.get("Platinum"));
        if (map == MachineRecipeMaps.DidYouKnow) return item("book_bronze");
        if (map == MachineRecipeMaps.BedrockOreList) return bedrockOre();
        if (gregtech.content.recipe.CrucibleDisplayItems.isCrucible(map)) return item("smelting_crucible_ceramic");
        return ItemStack.EMPTY;
    }
}
