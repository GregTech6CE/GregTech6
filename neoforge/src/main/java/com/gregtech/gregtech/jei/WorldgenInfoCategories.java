package com.gregtech.gregtech.jei;


import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.worldgen.GTOreVeins;
import com.gregtech.gregtech.worldgen.GTStoneLayersGen;
import com.gregtech.gregtech.worldgen.GTWorldgenScale;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI categories describing GT worldgen: large ore veins (dimension, height,
 * probability, biomes), small ores (dimension, height, blocks per chunk) and
 * regional stone layers with their embedded ores.
 */
public final class WorldgenInfoCategories {
    private WorldgenInfoCategories() {}

    // ── data records shown as "recipes" ─────────────────────────────────────

    public record VeinInfo(String name, String dimension, int minY, int maxY, int weight, int totalWeight,
                           GTMaterial top, GTMaterial bottom, GTMaterial between, GTMaterial spread,
                           boolean remapped) {}

    public record SmallOreInfo(String name, String dimension, int minY, int maxY, int amount,
                               GTMaterial material, boolean remapped) {}

    public record LayerInfo(String stoneDisplay, ItemStack stone, List<GTMaterial> ores) {}

    public static final RecipeType<VeinInfo> VEIN_TYPE = RecipeType.create("gregtech", "ore_veins", VeinInfo.class);
    public static final RecipeType<SmallOreInfo> SMALL_ORE_TYPE = RecipeType.create("gregtech", "small_ores_info", SmallOreInfo.class);
    public static final RecipeType<LayerInfo> LAYER_TYPE = RecipeType.create("gregtech", "stone_layers_info", LayerInfo.class);

    public static ItemStack oreStack(GTMaterial material) {
        if (material == null || !material.resolve().isValid()) return new ItemStack(Items.STONE);
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gregtech",
                "ore_" + material.resolve().getName().toLowerCase()));
        return item == null ? new ItemStack(Items.STONE) : new ItemStack(item);
    }

    public static ItemStack smallOreStack(GTMaterial material) {
        if (material == null || !material.resolve().isValid()) return new ItemStack(Items.STONE);
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gregtech",
                "ore_small_" + material.resolve().getName().toLowerCase()));
        return item == null ? oreStack(material) : new ItemStack(item);
    }

    public static List<VeinInfo> buildVeins() {
        List<VeinInfo> out = new ArrayList<>();
        for (GTOreVeins.OreVein v : GTOreVeins.OVERWORLD_VEINS) {
            out.add(new VeinInfo(v.name(), "minecraft:overworld", v.minY(), v.maxY(), v.weight(),
                    GTOreVeins.TOTAL_VEIN_WEIGHT, v.top(), v.bottom(), v.between(), v.spread(), true));
        }
        for (GTOreVeins.OreVein v : GTOreVeins.END_VEINS) {
            out.add(new VeinInfo(v.name(), "minecraft:the_end", v.minY(), v.maxY(), v.weight(),
                    GTOreVeins.TOTAL_END_VEIN_WEIGHT, v.top(), v.bottom(), v.between(), v.spread(), false));
        }
        return out;
    }

    public static List<SmallOreInfo> buildSmallOres() {
        List<SmallOreInfo> out = new ArrayList<>();
        for (GTOreVeins.SmallOre o : GTOreVeins.OVERWORLD_SMALL_ORES) {
            out.add(new SmallOreInfo(o.name(), "minecraft:overworld", o.minY(), o.maxY(), o.amount(), o.material(), true));
        }
        for (GTOreVeins.SmallOre o : GTOreVeins.NETHER_SMALL_ORES) {
            out.add(new SmallOreInfo(o.name(), "minecraft:the_nether", o.minY(), o.maxY(), o.amount(), o.material(), false));
        }
        for (GTOreVeins.SmallOre o : GTOreVeins.END_SMALL_ORES) {
            out.add(new SmallOreInfo(o.name(), "minecraft:the_end", o.minY(), o.maxY(), o.amount(), o.material(), false));
        }
        return out;
    }

    public static List<LayerInfo> buildLayers() {
        List<LayerInfo> out = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (GTStoneLayersGen.LayerDef def : GTStoneLayersGen.LAYERS) {
            ItemStack stone;
            String display;
            if (def.stoneType() != null) {
                String id = "stone_" + def.stoneType().toLowerCase() + "_stone";
                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gregtech", id));
                if (item == null) continue;
                stone = new ItemStack(item);
                display = def.material();
            } else if (def.blockId() != null) {
                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gregtech", def.blockId()));
                if (item == null) continue;
                stone = new ItemStack(item);
                display = def.material();
            } else {
                continue; // vanilla stone layers carry no display block
            }
            List<GTMaterial> ores = new ArrayList<>();
            for (GTStoneLayersGen.OreDef ore : def.ores()) {
                GTMaterial material = com.gregtech.gregtech.api.material.GTMaterialRegistry.get(ore.material());
                if (material != null && material.resolve().isValid()) ores.add(material.resolve());
            }
            String key = display + "/" + ores.size();
            if (!seen.add(key)) continue; // collapse duplicate layer entries
            out.add(new LayerInfo(display, stone, ores));
        }
        return out;
    }

    // ── categories ───────────────────────────────────────────────────────────

    /** Large veins: 4 ore slots + dimension/height/chance/biome text. */
    public static final class VeinCategory implements IRecipeCategory<VeinInfo> {
        private final IDrawable background, icon, slot;

        public VeinCategory(IGuiHelper gui) {
            this.background = gui.createBlankDrawable(150, 70);
            this.slot = gui.getSlotDrawable();
            this.icon = gui.createDrawableItemStack(new ItemStack(Items.IRON_ORE));
        }

        @Override public RecipeType<VeinInfo> getRecipeType() { return VEIN_TYPE; }
        @Override public Component getTitle() { return Component.translatable("gregtech.jei.category.ore_veins"); }
        @Override public IDrawable getBackground() { return background; }
        @Override public IDrawable getIcon() { return icon; }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, VeinInfo vein, IFocusGroup focuses) {
            GTMaterial[] mats = {vein.top(), vein.bottom(), vein.between(), vein.spread()};
            for (int i = 0; i < 4; i++) {
                builder.addSlot(RecipeIngredientRole.OUTPUT, 1 + i * 18, 1)
                        .setBackground(slot, -1, -1)
                        .addItemStack(oreStack(mats[i]));
            }
        }

        @Override
        public void draw(VeinInfo vein, IRecipeSlotsView view, GuiGraphics g, double mx, double my) {
            var font = net.minecraft.client.Minecraft.getInstance().font;
            int minY = vein.remapped() ? GTWorldgenScale.remapY(vein.minY()) : vein.minY();
            int maxY = vein.remapped() ? GTWorldgenScale.remapY(vein.maxY()) : vein.maxY();
            double pct = 100.0 * vein.weight() / Math.max(1, vein.totalWeight());
            int y = 22;
            g.drawString(font, Component.translatable("gregtech.jei.info.vein_name", vein.name()), 1, y, 0xFF404040, false);
            g.drawString(font, Component.translatable("gregtech.jei.info.dimension", vein.dimension()), 1, y + 10, 0xFF404040, false);
            g.drawString(font, Component.translatable("gregtech.jei.info.height", minY, maxY), 1, y + 20, 0xFF404040, false);
            g.drawString(font, Component.translatable("gregtech.jei.info.chance", String.format("%.1f", pct)), 1, y + 30, 0xFF404040, false);
            g.drawString(font, Component.translatable("gregtech.jei.info.biome_none"), 1, y + 40, 0xFF404040, false);
        }
    }

    /** Small ores: 1 slot + dimension/height/amount text. */
    public static final class SmallOreCategory implements IRecipeCategory<SmallOreInfo> {
        private final IDrawable background, icon, slot;

        public SmallOreCategory(IGuiHelper gui) {
            this.background = gui.createBlankDrawable(150, 60);
            this.slot = gui.getSlotDrawable();
            this.icon = gui.createDrawableItemStack(new ItemStack(Items.RAW_GOLD));
        }

        @Override public RecipeType<SmallOreInfo> getRecipeType() { return SMALL_ORE_TYPE; }
        @Override public Component getTitle() { return Component.translatable("gregtech.jei.category.small_ores"); }
        @Override public IDrawable getBackground() { return background; }
        @Override public IDrawable getIcon() { return icon; }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, SmallOreInfo ore, IFocusGroup focuses) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 1, 1)
                    .setBackground(slot, -1, -1)
                    .addItemStack(smallOreStack(ore.material()));
        }

        @Override
        public void draw(SmallOreInfo ore, IRecipeSlotsView view, GuiGraphics g, double mx, double my) {
            var font = net.minecraft.client.Minecraft.getInstance().font;
            int minY = ore.remapped() ? GTWorldgenScale.remapY(ore.minY()) : ore.minY();
            int maxY = ore.remapped() ? GTWorldgenScale.remapY(ore.maxY()) : ore.maxY();
            int y = 22;
            g.drawString(font, Component.translatable("gregtech.jei.info.dimension", ore.dimension()), 1, y, 0xFF404040, false);
            g.drawString(font, Component.translatable("gregtech.jei.info.height", minY, maxY), 1, y + 10, 0xFF404040, false);
            g.drawString(font, Component.translatable("gregtech.jei.info.per_chunk", ore.amount()), 1, y + 20, 0xFF404040, false);
        }
    }

    /** Stone layers: stone block + its embedded ores. */
    public static final class LayerCategory implements IRecipeCategory<LayerInfo> {
        private final IDrawable background, icon, slot;

        public LayerCategory(IGuiHelper gui) {
            this.background = gui.createBlankDrawable(168, 56);
            this.slot = gui.getSlotDrawable();
            this.icon = gui.createDrawableItemStack(new ItemStack(Items.STONE));
        }

        @Override public RecipeType<LayerInfo> getRecipeType() { return LAYER_TYPE; }
        @Override public Component getTitle() { return Component.translatable("gregtech.jei.category.stone_layers"); }
        @Override public IDrawable getBackground() { return background; }
        @Override public IDrawable getIcon() { return icon; }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, LayerInfo layer, IFocusGroup focuses) {
            builder.addSlot(RecipeIngredientRole.CATALYST, 1, 1)
                    .setBackground(slot, -1, -1)
                    .addItemStack(layer.stone());
            int i = 0;
            for (GTMaterial ore : layer.ores()) {
                if (i >= 16) break;
                builder.addSlot(RecipeIngredientRole.OUTPUT, 1 + (i % 8) * 18, 20 + (i / 8) * 18)
                        .setBackground(slot, -1, -1)
                        .addItemStack(oreStack(ore));
                i++;
            }
        }

        @Override
        public void draw(LayerInfo layer, IRecipeSlotsView view, GuiGraphics g, double mx, double my) {
            var font = net.minecraft.client.Minecraft.getInstance().font;
            g.drawString(font, Component.translatable("gregtech.jei.info.layer_stone", layer.stoneDisplay()), 22, 5, 0xFF404040, false);
            if (!layer.ores().isEmpty()) {
                g.drawString(font, Component.translatable("gregtech.jei.info.layer_ores"), 22, 14, 0xFF707070, false);
            } else {
                g.drawString(font, Component.translatable("gregtech.jei.info.layer_no_ores"), 22, 14, 0xFF707070, false);
            }
        }
    }

    // ── bedrock ores ─────────────────────────────────────────────────────────

    public record BedrockInfo(String name, GTMaterial material, int chance, ItemStack flower) {}

    public static final RecipeType<BedrockInfo> BEDROCK_TYPE = RecipeType.create("gregtech", "bedrock_ores", BedrockInfo.class);

    public static List<BedrockInfo> buildBedrockOres() {
        List<BedrockInfo> out = new ArrayList<>();
        for (com.gregtech.gregtech.worldgen.GTBedrockOres.BedrockOre ore
                : com.gregtech.gregtech.worldgen.GTBedrockOres.OVERWORLD) {
            Item flower = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gregtech", ore.flowerId()));
            out.add(new BedrockInfo(ore.name(), ore.material(), ore.chance(),
                    flower == null ? new ItemStack(Items.POPPY) : new ItemStack(flower)));
        }
        return out;
    }

    /** Bedrock deposits: ore + the surface indicator flower above it. */
    public static final class BedrockCategory implements IRecipeCategory<BedrockInfo> {
        private final IDrawable background, icon, slot;

        public BedrockCategory(IGuiHelper gui) {
            this.background = gui.createBlankDrawable(150, 56);
            this.slot = gui.getSlotDrawable();
            this.icon = gui.createDrawableItemStack(new ItemStack(Items.BEDROCK));
        }

        @Override public RecipeType<BedrockInfo> getRecipeType() { return BEDROCK_TYPE; }
        @Override public Component getTitle() { return Component.translatable("gregtech.jei.category.bedrock_ores"); }
        @Override public IDrawable getBackground() { return background; }
        @Override public IDrawable getIcon() { return icon; }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, BedrockInfo info, IFocusGroup focuses) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 1, 1)
                    .setBackground(slot, -1, -1)
                    .addItemStack(oreStack(info.material()));
            builder.addSlot(RecipeIngredientRole.CATALYST, 21, 1)
                    .setBackground(slot, -1, -1)
                    .addItemStack(info.flower());
        }

        @Override
        public void draw(BedrockInfo info, IRecipeSlotsView view, GuiGraphics g, double mx, double my) {
            var font = net.minecraft.client.Minecraft.getInstance().font;
            int y = 24;
            g.drawString(font, Component.translatable("gregtech.jei.info.dimension", "minecraft:overworld"), 1, y, 0xFF404040, false);
            g.drawString(font, Component.translatable("gregtech.jei.info.bedrock_rarity", info.chance()), 1, y + 10, 0xFF404040, false);
            g.drawString(font, Component.translatable("gregtech.jei.info.bedrock_flower"), 1, y + 20, 0xFF707070, false);
        }
    }
}
