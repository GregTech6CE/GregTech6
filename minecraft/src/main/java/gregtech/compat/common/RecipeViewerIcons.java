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

    public static ItemStack map(RecipeMap map) {
        if (map == MachineRecipeMaps.ByProductList)
            return GTItems.getStack(MaterialPrefix.dust, GTMaterialRegistry.get("Platinum"));
        if (map == MachineRecipeMaps.DidYouKnow) return item("book_bronze");
        if (map == MachineRecipeMaps.BedrockOreList) return bedrockOre();
        if (gregtech.content.recipe.CrucibleDisplayItems.isCrucible(map)) return item("smelting_crucible_ceramic");
        return ItemStack.EMPTY;
    }
}
