package gregtech.compat.emi;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.*;
import dev.emi.emi.api.stack.*;
import dev.emi.emi.api.widget.WidgetHolder;
import gregtech.compat.common.OreGenerationClient;
import gregtech.compat.common.RecipeViewerIcons;
import gregtech.compat.common.WorldgenInfoData;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;

/** Native, non-craftable geology pages sourced from the active world generation catalogs. */
public final class WorldgenEmiRecipe implements EmiRecipe {
    private final EmiRecipeCategory category;
    private final ResourceLocation id;
    private final List<EmiStack> outputs;
    private final List<EmiStack> displayedOutputs;
    private List<List<Component>> outputTooltips = List.of();
    private final List<EmiIngredient> catalysts;
    private final List<Component> captions;

    private WorldgenEmiRecipe(
            EmiRecipeCategory category,
            int index,
            List<ItemStack> outputs,
            List<ItemStack> catalysts,
            List<Component> captions) {
        this.category = category;
        id = ResourceLocation.fromNamespaceAndPath(
                "gt6ce", "/worldgen/" + category.getId().getPath() + "/" + index);
        this.displayedOutputs =
                outputs.stream().filter(s -> !s.isEmpty()).map(EmiStack::of).toList();
        this.outputs = new ArrayList<>(displayedOutputs);
        this.catalysts = catalysts.stream()
                .filter(s -> !s.isEmpty())
                .map(s -> (EmiIngredient) EmiStack.of(s))
                .toList();
        this.captions = List.copyOf(captions);
    }

    private WorldgenEmiRecipe withAliases(List<ItemStack> aliases) {
        for (ItemStack stack : aliases) {
            if (!stack.isEmpty()) {
                EmiStack alias = EmiStack.of(stack);
                if (!outputs.contains(alias)) outputs.add(alias);
            }
        }
        return this;
    }

    private WorldgenEmiRecipe withOutputTooltips(List<List<Component>> tooltips) {
        outputTooltips = List.copyOf(tooltips);
        return this;
    }

    private static EmiRecipeCategory category(EmiRegistry registry, String path, String title, ItemStack icon) {
        var category = new EmiRecipeCategory(ResourceLocation.fromNamespaceAndPath("gt6ce", path), EmiStack.of(icon)) {
            @Override
            public Component getName() {
                return Component.translatable("gt6ce.jei.category." + title);
            }
        };
        registry.addCategory(category);
        return category;
    }

    private static Component line(String key, Object... args) {
        return Component.translatable("gt6ce.jei.info." + key, args);
    }

    public static void register(EmiRegistry registry) {
        var atlas = OreGenerationClient.beginBuild();
        var veins = category(registry, "ore_veins", "ore_veins", RecipeViewerIcons.item("ore_iridium"));
        int index = 0;
        for (var vein : WorldgenInfoData.buildVeins()) {
            var previews = WorldgenInfoData.veinPreviews(vein);
            registry.addRecipe(new WorldgenEmiRecipe(
                            veins,
                            index++,
                            previews.stream()
                                    .map(WorldgenInfoData.OrePreview::stack)
                                    .toList(),
                            WorldgenInfoData.veinHosts(vein),
                            vein.layer() == null
                                    ? WorldgenInfoData.veinCaptions(vein)
                                    : WorldgenInfoData.stratumCaptions(vein))
                    .withAliases(WorldgenInfoData.veinAliases(vein))
                    .withOutputTooltips(previews.stream()
                            .map(WorldgenInfoData.OrePreview::tooltip)
                            .toList()));
        }
        var small = category(registry, "small_ores_info", "small_ores", RecipeViewerIcons.item("ore_small_iridium"));
        index = 0;
        for (var ore : WorldgenInfoData.buildSmallOres()) {
            registry.addRecipe(new WorldgenEmiRecipe(
                            small,
                            index++,
                            ore.ores().stream()
                                    .map(WorldgenInfoData.OrePreview::stack)
                                    .toList(),
                            List.of(),
                            ore.captions())
                    .withAliases(ore.aliases())
                    .withOutputTooltips(ore.ores().stream()
                            .map(WorldgenInfoData.OrePreview::tooltip)
                            .toList()));
        }
        var layers = category(registry, "stone_layers_info", "stone_layers", RecipeViewerIcons.item("block_ore_salt"));
        index = 0;
        for (var layer : WorldgenInfoData.buildLayers()) {
            var previews = WorldgenInfoData.layerPreviews(layer);
            registry.addRecipe(new WorldgenEmiRecipe(
                            layers,
                            index++,
                            previews.stream()
                                    .map(WorldgenInfoData.OrePreview::stack)
                                    .toList(),
                            List.of(layer.stone()),
                            List.of(
                                    line("layer_stone", layer.stone().getHoverName()),
                                    line(layer.ores().isEmpty() ? "layer_no_ores" : "layer_ores")))
                    .withAliases(WorldgenInfoData.layerAliases(layer))
                    .withOutputTooltips(previews.stream()
                            .map(WorldgenInfoData.OrePreview::tooltip)
                            .toList()));
        }
        var bedrock = category(registry, "bedrock_ores", "bedrock_ores", RecipeViewerIcons.bedrockOre());
        index = 0;
        for (var ore : WorldgenInfoData.buildBedrockOres()) {
            registry.addRecipe(new WorldgenEmiRecipe(
                            bedrock,
                            index++,
                            List.of(WorldgenInfoData.bedrockOreStack(ore.material())),
                            List.of(ore.flower()),
                            WorldgenInfoData.bedrockCaptions(ore))
                    .withAliases(WorldgenInfoData.bedrockAliases(ore))
                    .withOutputTooltips(List.of(WorldgenInfoData.oreSourceCaptions(ore.material()))));
        }
        OreGenerationClient.finishBuild(atlas);
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return category;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return List.of();
    }

    @Override
    public List<EmiIngredient> getCatalysts() {
        return catalysts;
    }

    @Override
    public List<EmiStack> getOutputs() {
        return outputs;
    }

    @Override
    public boolean supportsRecipeTree() {
        return false;
    }

    @Override
    public boolean hideCraftable() {
        return true;
    }

    @Override
    public int getDisplayWidth() {
        return 176;
    }

    @Override
    public int getDisplayHeight() {
        return 40 + ((displayedOutputs.size() + catalysts.size() + 7) / 8) * 18 + captions.size() * 10;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        var stacks = new ArrayList<EmiIngredient>(catalysts);
        stacks.addAll(displayedOutputs);
        for (int i = 0; i < stacks.size(); i++) {
            var slot = widgets.addSlot(stacks.get(i), 1 + i % 8 * 19, 1 + i / 8 * 19);
            if (i >= catalysts.size()) slot.recipeContext(this);
            for (var caption : captions) slot.appendTooltip(caption);
            int output = i - catalysts.size();
            if (output >= 0 && output < outputTooltips.size()) {
                for (Component caption : outputTooltips.get(output)) slot.appendTooltip(caption);
            }
        }
        int y = 24 + ((stacks.size() + 7) / 8 - 1) * 19;
        for (var caption : captions) {
            var font = Minecraft.getInstance().font;
            widgets.addText(
                    Component.literal(font.plainSubstrByWidth(caption.getString(), 172)), 1, y, 0xff404040, false);
            y += 10;
        }
    }
}
