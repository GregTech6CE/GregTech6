package gregtech.compat.jei;


//? if >=1.21.1 {
//?} else {
import gregtech.GregTech;
//?}
import gregtech.api.material.GTMaterial;
import gregtech.content.worldgen.GTOreVeins;
import gregtech.content.worldgen.GTStoneLayersGen;
import gregtech.content.worldgen.GTWorldgenScale;
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
//? if >=1.21.1 {
/*import net.minecraft.core.registries.BuiltInRegistries;
*///?} else {
import net.minecraftforge.registries.ForgeRegistries;
//?}

import java.util.ArrayList;
import java.util.List;

/**
 * JEI categories describing GT worldgen: large ore veins (dimension, height,
 * probability, biomes), small ores (dimension, height, blocks per chunk) and
 * regional stone layers with their embedded ores.
 */
public final class WorldgenInfoCategories extends WorldgenInfoData {
    private WorldgenInfoCategories() {}

    // ── data records shown as "recipes" ─────────────────────────────────────

//? if >=1.21.1 {
    /*public static final RecipeType<VeinInfo> VEIN_TYPE = RecipeType.create("gt6ce", "ore_veins", VeinInfo.class);
    public static final RecipeType<SmallOreInfo> SMALL_ORE_TYPE = RecipeType.create("gt6ce", "small_ores_info", SmallOreInfo.class);
    public static final RecipeType<LayerInfo> LAYER_TYPE = RecipeType.create("gt6ce", "stone_layers_info", LayerInfo.class);
*///?} else {
    public static final RecipeType<VeinInfo> VEIN_TYPE = RecipeType.create(GregTech.NAMESPACE, "ore_veins", VeinInfo.class);
    public static final RecipeType<SmallOreInfo> SMALL_ORE_TYPE = RecipeType.create(GregTech.NAMESPACE, "small_ores_info", SmallOreInfo.class);
    public static final RecipeType<LayerInfo> LAYER_TYPE = RecipeType.create(GregTech.NAMESPACE, "stone_layers_info", LayerInfo.class);
//?}

    // ── categories ───────────────────────────────────────────────────────────

    /** Large veins: 4 ore slots + dimension/height/chance/biome text. */
    public static final class VeinCategory implements IRecipeCategory<VeinInfo> {
        private final IDrawable background, icon, slot;

        public VeinCategory(IGuiHelper gui) {
            this.background = gui.createBlankDrawable(176, 84);
            this.slot = gui.getSlotDrawable();
            this.icon = gui.createDrawableItemStack(gregtech.compat.common.RecipeViewerIcons.item("ore_iridium"));
        }

        @Override public RecipeType<VeinInfo> getRecipeType() { return VEIN_TYPE; }
        @Override public Component getTitle() { return Component.translatable("gt6ce.jei.category.ore_veins"); }
        @Override public IDrawable getBackground() { return background; }
        @Override public IDrawable getIcon() { return icon; }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, VeinInfo vein, IFocusGroup focuses) {
            if (vein.layer() != null) {
                var captions = stratumCaptions(vein);
                builder.addSlot(RecipeIngredientRole.OUTPUT, 1, 1).setBackground(slot, -1, -1)
                        .addItemStack(oreStack(vein.top()))
                        .addTooltipCallback((view, tooltip) -> tooltip.addAll(captions));
                builder.addSlot(RecipeIngredientRole.OUTPUT, 19, 1).setBackground(slot, -1, -1)
                        .addItemStack(smallOreStack(vein.top()))
                        .addTooltipCallback((view, tooltip) -> tooltip.addAll(captions));
                return;
            }
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
            if (vein.layer() != null) {
                int y = 22;
                for (Component caption : stratumCaptions(vein)) {
                    g.drawString(font, font.plainSubstrByWidth(caption.getString(), 174), 1, y, 0xFF404040, false);
                    y += 10;
                }
                return;
            }
            int minY = vein.remapped() ? GTWorldgenScale.remapY(vein.minY()) : vein.minY();
            int maxY = vein.remapped() ? GTWorldgenScale.remapY(vein.maxY()) : vein.maxY();
            double pct = 100.0 * vein.weight() / Math.max(1, vein.totalWeight());
            int y = 22;
            g.drawString(font, Component.translatable("gt6ce.jei.info.vein_name", vein.name()), 1, y, 0xFF404040, false);
            g.drawString(font, Component.translatable("gt6ce.jei.info.dimension", vein.dimension()), 1, y + 10, 0xFF404040, false);
            g.drawString(font, Component.translatable("gt6ce.jei.info.height", minY, maxY), 1, y + 20, 0xFF404040, false);
            g.drawString(font, Component.translatable("gt6ce.jei.info.chance", String.format(java.util.Locale.ROOT, "%.2f", pct)), 1, y + 30, 0xFF404040, false);
            g.drawString(font, Component.translatable("gt6ce.jei.info.biome_none"), 1, y + 40, 0xFF404040, false);
        }
    }

    /** Small ores: 1 slot + dimension/height/amount text. */
    public static final class SmallOreCategory implements IRecipeCategory<SmallOreInfo> {
        private final IDrawable background, icon, slot;

        public SmallOreCategory(IGuiHelper gui) {
            this.background = gui.createBlankDrawable(150, 60);
            this.slot = gui.getSlotDrawable();
            this.icon = gui.createDrawableItemStack(gregtech.compat.common.RecipeViewerIcons.item("ore_small_iridium"));
        }

        @Override public RecipeType<SmallOreInfo> getRecipeType() { return SMALL_ORE_TYPE; }
        @Override public Component getTitle() { return Component.translatable("gt6ce.jei.category.small_ores"); }
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
            g.drawString(font, Component.translatable("gt6ce.jei.info.dimension", ore.dimension()), 1, y, 0xFF404040, false);
            g.drawString(font, Component.translatable("gt6ce.jei.info.height", minY, maxY), 1, y + 10, 0xFF404040, false);
            g.drawString(font, Component.translatable("gt6ce.jei.info.per_chunk", ore.amount()), 1, y + 20, 0xFF404040, false);
        }
    }

    /** Stone layers: stone block + its embedded ores. */
    public static final class LayerCategory implements IRecipeCategory<LayerInfo> {
        private final IDrawable background, icon, slot;

        public LayerCategory(IGuiHelper gui) {
            this.background = gui.createBlankDrawable(168, 56);
            this.slot = gui.getSlotDrawable();
            this.icon = gui.createDrawableItemStack(gregtech.compat.common.RecipeViewerIcons.item("block_ore_salt"));
        }

        @Override public RecipeType<LayerInfo> getRecipeType() { return LAYER_TYPE; }
        @Override public Component getTitle() { return Component.translatable("gt6ce.jei.category.stone_layers"); }
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
            g.drawString(font, Component.translatable("gt6ce.jei.info.layer_stone", layer.stone().getHoverName()), 22, 5, 0xFF404040, false);
            if (!layer.ores().isEmpty()) {
                g.drawString(font, Component.translatable("gt6ce.jei.info.layer_ores"), 22, 14, 0xFF707070, false);
            } else {
                g.drawString(font, Component.translatable("gt6ce.jei.info.layer_no_ores"), 22, 14, 0xFF707070, false);
            }
        }
    }

    // ── bedrock ores ─────────────────────────────────────────────────────────



//? if >=1.21.1 {
    /*public static final RecipeType<BedrockInfo> BEDROCK_TYPE = RecipeType.create("gt6ce", "bedrock_ores", BedrockInfo.class);
*///?} else {
    public static final RecipeType<BedrockInfo> BEDROCK_TYPE = RecipeType.create(GregTech.NAMESPACE, "bedrock_ores", BedrockInfo.class);
//?}



    /** Bedrock deposits: ore + the surface indicator flower above it. */
    public static final class BedrockCategory implements IRecipeCategory<BedrockInfo> {
        private final IDrawable background, icon, slot;

        public BedrockCategory(IGuiHelper gui) {
            this.background = gui.createBlankDrawable(150, 56);
            this.slot = gui.getSlotDrawable();
            this.icon = gui.createDrawableItemStack(gregtech.compat.common.RecipeViewerIcons.bedrockOre());
        }

        @Override public RecipeType<BedrockInfo> getRecipeType() { return BEDROCK_TYPE; }
        @Override public Component getTitle() { return Component.translatable("gt6ce.jei.category.bedrock_ores"); }
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
            g.drawString(font, Component.translatable("gt6ce.jei.info.dimension", "minecraft:overworld"), 1, y, 0xFF404040, false);
            g.drawString(font, Component.translatable("gt6ce.jei.info.bedrock_rarity", info.chance()), 1, y + 10, 0xFF404040, false);
            g.drawString(font, Component.translatable("gt6ce.jei.info.bedrock_flower"), 1, y + 20, 0xFF707070, false);
        }
    }
}
