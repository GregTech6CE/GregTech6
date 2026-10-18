package com.gregtech.gregtech.emi;

import com.gregtech.gregtech.jei.WorldgenInfoData;
import com.gregtech.gregtech.worldgen.GTWorldgenScale;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.*;
import dev.emi.emi.api.stack.*;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import java.util.*;

/** Native, non-craftable geology pages sourced from the active world generation catalogs. */
public final class WorldgenEmiRecipe implements EmiRecipe {
    private final EmiRecipeCategory category;
    private final ResourceLocation id;
    private final List<EmiStack> outputs;
    private final List<EmiIngredient> catalysts;
    private final List<Component> captions;
    private WorldgenEmiRecipe(EmiRecipeCategory category, int index, List<ItemStack> outputs,
                             List<ItemStack> catalysts, List<Component> captions) {
        this.category = category;
        id = ResourceLocation.fromNamespaceAndPath("gregtech", "/worldgen/" + category.getId().getPath() + "/" + index);
        this.outputs = outputs.stream().filter(s -> !s.isEmpty()).map(EmiStack::of).toList();
        this.catalysts = catalysts.stream().filter(s -> !s.isEmpty()).map(s -> (EmiIngredient)EmiStack.of(s)).toList();
        this.captions = List.copyOf(captions);
    }
    private static EmiRecipeCategory category(EmiRegistry registry, String path, String title, Item icon) {
        var category = new EmiRecipeCategory(ResourceLocation.fromNamespaceAndPath("gregtech", path), EmiStack.of(new ItemStack(icon))) {
            @Override public Component getName() { return Component.translatable("gregtech.jei.category." + title); }
        };
        registry.addCategory(category);
        return category;
    }
    private static Component line(String key, Object... args) { return Component.translatable("gregtech.jei.info." + key, args); }
    public static void register(EmiRegistry registry) {
        var veins = category(registry, "ore_veins", "ore_veins", Items.IRON_ORE);
        int index = 0;
        for (var vein : WorldgenInfoData.buildVeins()) {
            int min = vein.remapped() ? GTWorldgenScale.remapY(vein.minY()) : vein.minY();
            int max = vein.remapped() ? GTWorldgenScale.remapY(vein.maxY()) : vein.maxY();
            registry.addRecipe(new WorldgenEmiRecipe(veins, index++,
                    List.of(WorldgenInfoData.oreStack(vein.top()), WorldgenInfoData.oreStack(vein.bottom()),
                            WorldgenInfoData.oreStack(vein.between()), WorldgenInfoData.oreStack(vein.spread())), List.of(),
                    List.of(line("vein_name", vein.name()), line("dimension", vein.dimension()), line("height", min, max),
                            line("chance", String.format(Locale.ROOT, "%.1f", 100d * vein.weight() / Math.max(1, vein.totalWeight()))), line("biome_none"))));
        }
        var small = category(registry, "small_ores_info", "small_ores", Items.RAW_GOLD);
        index = 0;
        for (var ore : WorldgenInfoData.buildSmallOres()) {
            int min = ore.remapped() ? GTWorldgenScale.remapY(ore.minY()) : ore.minY();
            int max = ore.remapped() ? GTWorldgenScale.remapY(ore.maxY()) : ore.maxY();
            registry.addRecipe(new WorldgenEmiRecipe(small, index++, List.of(WorldgenInfoData.smallOreStack(ore.material())), List.of(),
                    List.of(line("vein_name", ore.name()), line("dimension", ore.dimension()), line("height", min, max), line("per_chunk", ore.amount()))));
        }
        var layers = category(registry, "stone_layers_info", "stone_layers", Items.STONE);
        index = 0;
        for (var layer : WorldgenInfoData.buildLayers()) {
            registry.addRecipe(new WorldgenEmiRecipe(layers, index++, layer.ores().stream().map(WorldgenInfoData::oreStack).toList(),
                    List.of(layer.stone()), List.of(line("layer_stone", layer.stoneDisplay()), line(layer.ores().isEmpty() ? "layer_no_ores" : "layer_ores"))));
        }
        var bedrock = category(registry, "bedrock_ores", "bedrock_ores", Items.BEDROCK);
        index = 0;
        for (var ore : WorldgenInfoData.buildBedrockOres()) {
            registry.addRecipe(new WorldgenEmiRecipe(bedrock, index++, List.of(WorldgenInfoData.oreStack(ore.material())), List.of(ore.flower()),
                    List.of(line("vein_name", ore.name()), line("dimension", "minecraft:overworld"), line("bedrock_rarity", ore.chance()), line("bedrock_flower"))));
        }
    }
    @Override public EmiRecipeCategory getCategory() { return category; }
    @Override public ResourceLocation getId() { return id; }
    @Override public List<EmiIngredient> getInputs() { return List.of(); }
    @Override public List<EmiIngredient> getCatalysts() { return catalysts; }
    @Override public List<EmiStack> getOutputs() { return outputs; }
    @Override public boolean supportsRecipeTree() { return false; }
    @Override public boolean hideCraftable() { return true; }
    @Override public int getDisplayWidth() { return 176; }
    @Override public int getDisplayHeight() { return 40 + ((outputs.size() + catalysts.size() + 7) / 8) * 18 + captions.size() * 10; }
    @Override public void addWidgets(WidgetHolder widgets) {
        var stacks = new ArrayList<EmiIngredient>(catalysts); stacks.addAll(outputs);
        for (int i = 0; i < stacks.size(); i++) {
            var slot = widgets.addSlot(stacks.get(i), 1 + i % 8 * 19, 1 + i / 8 * 19);
            if (i >= catalysts.size()) slot.recipeContext(this);
            for (var caption : captions) slot.appendTooltip(caption);
        }
        int y = 24 + ((stacks.size() + 7) / 8 - 1) * 19;
        for (var caption : captions) {
            var font = Minecraft.getInstance().font;
            widgets.addText(Component.literal(font.plainSubstrByWidth(caption.getString(), 172)), 1, y, 0xff404040, false);
            y += 10;
        }
    }
}
