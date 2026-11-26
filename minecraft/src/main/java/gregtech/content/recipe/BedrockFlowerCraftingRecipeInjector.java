package gregtech.content.recipe;

import gregtech.content.data.MaterialPrefix;
import gregtech.content.plant.BedrockFlowers;
import gregtech.content.registry.GTItems;
import gregtech.content.registry.GTToolItems;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.mod.GregTechIdentity;
import gregtech.engine.tool.GTToolType;
import gregtech.platform.Mc;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;

/** GT6 BlockFlowersA/B shapeless dyes and B-family hand-cut wooden sticks. */
public final class BedrockFlowerCraftingRecipeInjector {
    private static final String[] A_DUST = {
        "Yellow", "Yellow", "Magenta", "Yellow", "Pink", "White", "LightBlue", "Brown"
    };
    private static final String[] B_DUST = {"Yellow", "Pink", "Yellow", "Purple"};

    private BedrockFlowerCraftingRecipeInjector() {}

    public static void add(GeneratedCraftingSink sink) {
        int added = 0;
        for (BedrockFlowers.Flower flower : BedrockFlowers.ALL) {
            ItemStack input = flower(flower);
            ItemStack output = shapelessOutput(flower);
            if (!output.isEmpty()) {
                sink.shapeless(
                        id(flower, "hand"),
                        "gt.bedrock_flowers",
                        CraftingBookCategory.MISC,
                        output,
                        List.of(Ingredient.of(input.getItem())));
                added++;
            }
            if (flower.family() == 'B' && (flower.meta() == 0 || flower.meta() == 1)) {
                ItemStack twoSticks = stick(flower, 2);
                for (GTToolType tool : new GTToolType[] {GTToolType.SAW, GTToolType.KNIFE}) {
                    var toolItem = GTToolItems.get(tool);
                    if (toolItem == null) throw new IllegalStateException("Missing flower crafting tool " + tool);
                    sink.shaped(
                            id(flower, tool.id()),
                            "gt.bedrock_flowers",
                            CraftingBookCategory.MISC,
                            twoSticks.copy(),
                            new String[] {"T", "F"},
                            Map.of('T', Ingredient.of(toolItem), 'F', Ingredient.of(input.getItem())),
                            false);
                    added++;
                }
            }
        }
        sink.commit();
        Mc.logger().info("Registered {} GT6 bedrock flower crafting recipes", added);
    }

    private static ItemStack shapelessOutput(BedrockFlowers.Flower flower) {
        if (flower.family() == 'A' && flower.meta() < A_DUST.length) return dust(A_DUST[flower.meta()]);
        if (flower.family() == 'B' && (flower.meta() == 0 || flower.meta() == 1)) return stick(flower, 1);
        if (flower.family() == 'B' && flower.meta() >= 2 && flower.meta() <= 5) return dust(B_DUST[flower.meta() - 2]);
        return ItemStack.EMPTY;
    }

    private static ItemStack stick(BedrockFlowers.Flower flower, int count) {
        String material = "Acacia";
        ItemStack output = GTItems.getStack(MaterialPrefix.stick, GTMaterialRegistry.get(material), count);
        if (output.isEmpty()) throw new IllegalStateException("Missing GT6 flower stick form " + material);
        return output;
    }

    private static ItemStack dust(String name) {
        ItemStack output = GTItems.getStack(MaterialPrefix.dust, GTMaterialRegistry.get(name));
        if (output.isEmpty()) throw new IllegalStateException("Missing GT6 flower dust form " + name);
        return output;
    }

    private static ItemStack flower(BedrockFlowers.Flower entry) {
        var item = Mc.item(ResourceLocation.fromNamespaceAndPath(GregTechIdentity.REGISTRY_NAMESPACE, entry.id()));
        if (item == null) throw new IllegalStateException("Missing GT6 flower " + entry.id());
        return new ItemStack(item);
    }

    private static ResourceLocation id(BedrockFlowers.Flower flower, String process) {
        return ResourceLocation.fromNamespaceAndPath(
                GregTechIdentity.REGISTRY_NAMESPACE, "bedrock_flowers/" + flower.id() + "/" + process);
    }
}
