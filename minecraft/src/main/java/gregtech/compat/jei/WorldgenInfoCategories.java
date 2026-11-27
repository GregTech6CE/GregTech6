package gregtech.compat.jei;

import gregtech.compat.common.RecipeViewerIcons;
import gregtech.compat.common.WorldgenInfoData;
import gregtech.content.worldgen.BoundaryGemOres;
import gregtech.engine.mod.GregTechIdentity;
import java.util.ArrayList;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * JEI categories for vein, small ore, bedrock and stone layer data.
 */
public final class WorldgenInfoCategories extends WorldgenInfoData {
    private WorldgenInfoCategories() {}

    // ── data records shown as "recipes" ─────────────────────────────────────

    public static final RecipeType<VeinInfo> VEIN_TYPE =
            RecipeType.create(GregTechIdentity.REGISTRY_NAMESPACE, "ore_veins", VeinInfo.class);
    public static final RecipeType<SmallOreInfo> SMALL_ORE_TYPE =
            RecipeType.create(GregTechIdentity.REGISTRY_NAMESPACE, "small_ores_info", SmallOreInfo.class);
    public static final RecipeType<LayerInfo> LAYER_TYPE =
            RecipeType.create(GregTechIdentity.REGISTRY_NAMESPACE, "stone_layers_info", LayerInfo.class);

    // ── categories ───────────────────────────────────────────────────────────

    /** Mixed veins and deposits hosted by a particular rock layer or contact. */
    public static final class VeinCategory implements IRecipeCategory<VeinInfo> {
        private final IDrawable background, icon, slot;

        public VeinCategory(IGuiHelper gui) {
            // The dungeon mine has up to four rows of slots followed by four lines of text.
            this.background = gui.createBlankDrawable(176, 4 + 4 * 18 + 4 * 10);
            this.slot = gui.getSlotDrawable();
            this.icon = gui.createDrawableItemStack(RecipeViewerIcons.item("ore_iridium"));
        }

        @Override
        public RecipeType<VeinInfo> getRecipeType() {
            return VEIN_TYPE;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("gt6ce.jei.category.ore_veins");
        }

        @Override
        public IDrawable getBackground() {
            return background;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, VeinInfo vein, IFocusGroup focuses) {
            builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStacks(vein.aliases());
            var stones = vein.hosts();
            for (int i = 0; i < stones.size(); i++) {
                builder.addSlot(RecipeIngredientRole.CATALYST, 1 + (i % 8) * 18, 1 + (i / 8) * 18)
                        .setBackground(slot, -1, -1)
                        .addItemStack(stones.get(i));
            }
            var previews = vein.previews();
            var common = vein.captions();
            for (int i = 0; i < previews.size(); i++) {
                var preview = previews.get(i);
                var captions = new ArrayList<>(common);
                captions.addAll(preview.tooltip(vein.summary()));
                int index = i + stones.size();
                builder.addSlot(RecipeIngredientRole.OUTPUT, 1 + (index % 8) * 18, 1 + (index / 8) * 18)
                        .setBackground(slot, -1, -1)
                        .addItemStack(preview.stack())
                        .addTooltipCallback((view, tooltip) -> tooltip.addAll(captions));
            }
        }

        @Override
        public void draw(VeinInfo vein, IRecipeSlotsView view, GuiGraphics g, double mx, double my) {
            var font = Minecraft.getInstance().font;
            int y = captionY(vein);
            for (Component caption : vein.captions()) {
                g.drawString(font, font.plainSubstrByWidth(caption.getString(), 174), 1, y, 0xFF404040, false);
                y += 10;
            }
        }

        private static int captionY(VeinInfo vein) {
            int slots = vein.previews().size() + vein.hosts().size();
            return 4 + Math.max(1, (slots + 7) / 8) * 18;
        }
    }

    /** Scattered ores and the shared boundary gem pool. */
    public static final class SmallOreCategory implements IRecipeCategory<SmallOreInfo> {
        private final IDrawable background, icon, slot;

        public SmallOreCategory(IGuiHelper gui) {
            int poolRows = (BoundaryGemOres.MATERIALS.size() + 7) / 8;
            int height = Math.max(62, 34 + poolRows * 18);
            this.background = gui.createBlankDrawable(176, height);
            this.slot = gui.getSlotDrawable();
            this.icon = gui.createDrawableItemStack(RecipeViewerIcons.item("ore_small_iridium"));
        }

        @Override
        public RecipeType<SmallOreInfo> getRecipeType() {
            return SMALL_ORE_TYPE;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("gt6ce.jei.category.small_ores");
        }

        @Override
        public IDrawable getBackground() {
            return background;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, SmallOreInfo ore, IFocusGroup focuses) {
            builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStacks(ore.aliases());
            for (int i = 0; i < ore.ores().size(); i++) {
                var preview = ore.ores().get(i);
                var captions = new ArrayList<>(ore.captions());
                captions.addAll(preview.tooltip(ore.summary()));
                builder.addSlot(RecipeIngredientRole.OUTPUT, 1 + (i % 8) * 18, 1 + (i / 8) * 18)
                        .setBackground(slot, -1, -1)
                        .addItemStack(preview.stack())
                        .addTooltipCallback((view, tooltip) -> tooltip.addAll(captions));
            }
        }

        @Override
        public void draw(SmallOreInfo ore, IRecipeSlotsView view, GuiGraphics g, double mx, double my) {
            var font = Minecraft.getInstance().font;
            int y = 4 + ((ore.ores().size() + 7) / 8) * 18;
            for (Component caption : ore.captions()) {
                g.drawString(font, font.plainSubstrByWidth(caption.getString(), 174), 1, y, 0xFF404040, false);
                y += 10;
            }
        }
    }

    /** Stone layers: stone block + its embedded ores. */
    public static final class LayerCategory implements IRecipeCategory<LayerInfo> {
        private final IDrawable background, icon, slot;

        public LayerCategory(IGuiHelper gui) {
            int rows = Math.max(1, (maximumLayerSlots() + 7) / 8);
            this.background = gui.createBlankDrawable(168, 20 + rows * 18);
            this.slot = gui.getSlotDrawable();
            this.icon = gui.createDrawableItemStack(RecipeViewerIcons.item("block_ore_salt"));
        }

        @Override
        public RecipeType<LayerInfo> getRecipeType() {
            return LAYER_TYPE;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("gt6ce.jei.category.stone_layers");
        }

        @Override
        public IDrawable getBackground() {
            return background;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, LayerInfo layer, IFocusGroup focuses) {
            builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStacks(layerAliases(layer));
            builder.addSlot(RecipeIngredientRole.CATALYST, 1, 1)
                    .setBackground(slot, -1, -1)
                    .addItemStack(layer.stone());
            int i = 0;
            for (var ore : layerPreviews(layer)) {
                builder.addSlot(RecipeIngredientRole.OUTPUT, 1 + (i % 8) * 18, 20 + (i / 8) * 18)
                        .setBackground(slot, -1, -1)
                        .addItemStack(ore.stack())
                        .addTooltipCallback((view, tooltip) -> tooltip.addAll(ore.tooltip(layer.summary())));
                i++;
            }
        }

        @Override
        public void draw(LayerInfo layer, IRecipeSlotsView view, GuiGraphics g, double mx, double my) {
            var font = Minecraft.getInstance().font;
            var title = Component.translatable(
                    "gt6ce.jei.info.layer_stone", layer.stone().getHoverName());
            g.drawString(font, font.plainSubstrByWidth(title.getString(), 144), 22, 5, 0xFF404040, false);
            if (!layer.ores().isEmpty()) {
                g.drawString(font, Component.translatable("gt6ce.jei.info.layer_ores"), 22, 14, 0xFF707070, false);
            } else {
                g.drawString(font, Component.translatable("gt6ce.jei.info.layer_no_ores"), 22, 14, 0xFF707070, false);
            }
        }
    }

    // ── bedrock ores ─────────────────────────────────────────────────────────

    public static final RecipeType<BedrockInfo> BEDROCK_TYPE =
            RecipeType.create(GregTechIdentity.REGISTRY_NAMESPACE, "bedrock_ores", BedrockInfo.class);

    /** Bedrock deposits: ore + the surface indicator flower above it. */
    public static final class BedrockCategory implements IRecipeCategory<BedrockInfo> {
        private final IDrawable background, icon, slot;

        public BedrockCategory(IGuiHelper gui) {
            this.background = gui.createBlankDrawable(176, 66);
            this.slot = gui.getSlotDrawable();
            this.icon = gui.createDrawableItemStack(RecipeViewerIcons.bedrockOre());
        }

        @Override
        public RecipeType<BedrockInfo> getRecipeType() {
            return BEDROCK_TYPE;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("gt6ce.jei.category.bedrock_ores");
        }

        @Override
        public IDrawable getBackground() {
            return background;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, BedrockInfo info, IFocusGroup focuses) {
            builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStacks(bedrockAliases(info));
            builder.addSlot(RecipeIngredientRole.OUTPUT, 1, 1)
                    .setBackground(slot, -1, -1)
                    .addItemStack(bedrockOreStack(info.material()))
                    .addTooltipCallback(
                            (view, tooltip) -> tooltip.addAll(oreSourceCaptions(info.material(), info.summary())));
            if (!info.flower().isEmpty())
                builder.addSlot(RecipeIngredientRole.CATALYST, 21, 1)
                        .setBackground(slot, -1, -1)
                        .addItemStack(info.flower());
        }

        @Override
        public void draw(BedrockInfo info, IRecipeSlotsView view, GuiGraphics g, double mx, double my) {
            var font = Minecraft.getInstance().font;
            int y = 24;
            for (Component caption : bedrockCaptions(info)) {
                g.drawString(font, caption, 1, y, 0xFF404040, false);
                y += 10;
            }
        }
    }
}
