package gregtech.compat.common;

import com.google.common.collect.ImmutableMultiset;
import gregtech.api.material.GTValues;
import gregtech.client.PreviewViewport;
import gregtech.client.gui.GuiTemplates;
import gregtech.compat.common.MaterialAtlas.*;
import gregtech.engine.recipe.Recipe;
import java.util.*;
import java.util.function.BiConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** One viewport and hit map for both viewers. All cells are positioned in document coordinates. */
public final class AtlasPanel {
    public static final int WIDTH = 174, TOP = 24;
    private static final int RIGHT = WIDTH - 8, INK = 0xff393939, RULE = 0xff888888;
    private final Page page;
    private final Cell header;
    private final List<Cell> cells = new ArrayList<>();
    private final List<Caption> captions = new ArrayList<>();
    private final List<Line> lines = new ArrayList<>();
    private int height, scroll, recipeRevision;
    private final int bottom;
    private final BiConsumer<ItemStack, Preview> openRecipe;
    private final Runnable openMaterial;
    private AtlasRecipeCard card;
    private Cell cardCell;

    private record Caption(Component text, int x, int y, int width) {}

    private record Line(int x, int y, int right, int bottom) {}

    private record Output(ItemStack stack, String detail) {
        @Override
        public boolean equals(Object other) {
            return other instanceof Output output
                    && ItemStack.matches(stack, output.stack)
                    && detail.equals(output.detail);
        }

        @Override
        public int hashCode() {
            return Objects.hash(stack.getItem(), stack.getCount(), detail);
        }
    }

    private record Choice(MachineRow row, List<Output> outputs) {}

    private static final int OUTPUT_COLS = 3, OUTPUT_PITCH = 22, OUTPUT_ROW = 26;
    private static final int ARROW_WIDTH = 14, ARROW_HEIGHT = 12, ARROW_GAP = 3;

    private static final class Cell {
        final int x, y, width;
        final ItemStack stack;
        final Assembly assembly;
        final Route route;
        final int outputRows;
        final List<Choice> choices;
        int variant;

        Cell(int x, int y, int width, ItemStack stack, Assembly assembly, Route route) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.stack = stack;
            this.assembly = assembly;
            this.route = route;
            int max = 0;
            if (route == null) choices = List.of();
            else {
                choices = new ArrayList<>();
                var seenOutputs = new HashSet<ImmutableMultiset<Output>>();
                for (var row : route.variants()) {
                    var outputs = outputsOf(row);
                    if (!seenOutputs.add(ImmutableMultiset.copyOf(outputs))) continue;
                    choices.add(new Choice(row, outputs));
                    max = Math.max(max, outputs.size());
                }
            }
            outputRows = (max + OUTPUT_COLS - 1) / OUTPUT_COLS;
        }

        private static List<Output> outputsOf(MachineRow row) {
            Recipe recipe = row.recipe();
            var list = new ArrayList<Output>();
            for (int i = 0; i < recipe.itemOutputs.length; i++) {
                var output = recipe.itemOutputs[i];
                if (output == null || output.isEmpty() || row.isHostDust(i)) continue;
                int chance = recipe.getOutputChance(i);
                String detail = chance == 10000
                        ? ""
                        : java.math.BigDecimal.valueOf(chance, 2)
                                        .stripTrailingZeros()
                                        .toPlainString() + "%";
                list.add(new Output(output, detail));
            }
            for (var fluid : recipe.fluidOutputs)
                if (fluid != null && !fluid.isEmpty())
                    list.add(new Output(
                            MaterialAtlas.fluid(fluid), gregtech.engine.fluid.FluidUnits.exact(fluid.getAmount())));
            return list;
        }

        Choice choice() {
            return choices.get(variant);
        }

        int arrowX(int direction) {
            return x + (width - 2 * ARROW_WIDTH - ARROW_GAP) / 2 + (direction > 0 ? ARROW_WIDTH + ARROW_GAP : 0);
        }

        int arrowAt(double mouseX, double docY) {
            if (choices.size() < 2 || docY < arrowY() || docY >= arrowY() + ARROW_HEIGHT) return 0;
            if (mouseX >= arrowX(-1) && mouseX < arrowX(-1) + ARROW_WIDTH) return -1;
            if (mouseX >= arrowX(1) && mouseX < arrowX(1) + ARROW_WIDTH) return 1;
            return 0;
        }

        int arrowY() {
            return y + 16;
        }

        int outputY() {
            return arrowY() + (choices.size() > 1 ? ARROW_HEIGHT : 0);
        }

        Preview recipe(Entry entry) {
            if (assembly != null) return assembly;
            return route == null ? entry.preview(stack) : choice().row();
        }

        int height() {
            return route == null ? width : outputY() - y + outputRows * OUTPUT_ROW;
        }
    }

    public AtlasPanel(
            Page page, int availableHeight, BiConsumer<ItemStack, Preview> openRecipe, Runnable openMaterial) {
        this.bottom = availableHeight - 2;
        this.openRecipe = openRecipe;
        this.openMaterial = openMaterial;
        this.page = page;
        header = new Cell(1, 2, 18, page.entry().icon(), null, null);
        if (page.kind() == Kind.ORE) oreChart();
        else materialChart();
    }

    private void section(Component title) {
        captions.add(new Caption(title, 3, height + 2, RIGHT - 5));
        height += 14;
    }

    private void grid(List<ItemStack> stacks, List<Assembly> tools) {
        for (int i = 0; i < stacks.size(); i++)
            cells.add(new Cell(
                    3 + i % 9 * 18,
                    height + i / 9 * 18,
                    18,
                    stacks.get(i),
                    tools.isEmpty() ? null : tools.get(i),
                    null));
        height += (stacks.size() + 8) / 9 * 18 + 3;
    }

    private void materialChart() {
        Entry e = page.entry();
        if (!e.material.getCompositionComponents().isEmpty()) {
            section(MaterialAtlas.label("composition"));
            for (var part : e.material.getCompositionComponents()) {
                var ingredient = e.component(part.material());
                if (ingredient != null) cells.add(new Cell(3, height, 18, ingredient.icon(), null, null));
                var amount = java.math.BigDecimal.valueOf(part.amount())
                        .divide(java.math.BigDecimal.valueOf(GTValues.U), 4, java.math.RoundingMode.HALF_UP)
                        .stripTrailingZeros()
                        .toPlainString();
                captions.add(new Caption(
                        Component.literal(amount + " × ").append(MaterialAtlas.name(part.material())),
                        25,
                        height + 5,
                        RIGHT - 26));
                height += 19;
            }
        }
        if (!e.tools.isEmpty()) {
            var handle = gregtech.content.tool.OriginalToolMaterials.requiredHandleMaterial(e.material);
            section(MaterialAtlas.label("handle_material", MaterialAtlas.name(handle)));
            enchantments(e, true);
            enchantments(e, false);
        }
        if (!e.forms.isEmpty()) {
            section(MaterialAtlas.label("forms", e.forms.size()));
            grid(e.forms.stream().map(Form::stack).toList(), List.of());
        }
        if (!e.tools.isEmpty()) {
            section(MaterialAtlas.label("tools", e.tools.size()));
            grid(e.tools.stream().map(Assembly::output).toList(), e.tools);
        }
        if (!e.products.isEmpty()) {
            section(MaterialAtlas.label("products", e.products.size()));
            grid(e.products, List.of());
        }
    }

    private void enchantments(Entry entry, boolean mining) {
        var levels = gregtech.content.tool.MaterialToolEnchantments.of(mining, !mining, false, entry.material);
        if (levels.isEmpty()) return;
        section(MaterialAtlas.label(mining ? "tool_enchantments" : "weapon_enchantments"));
        for (var level : levels.entrySet()) {
            var id = net.minecraft.resources.ResourceLocation.parse(
                    gregtech.content.tool.MaterialToolEnchantments.id(level.getKey()));
            var name = Component.translatable(id.toLanguageKey("enchantment"));
            captions.add(new Caption(name.append(" " + level.getValue()), 7, height, RIGHT - 8));
            height += 11;
        }
    }

    private void oreChart() {
        var entry = page.entry();
        recipeRevision = entry.recipeRevision;
        var byproducts = entry.byproducts;
        if (!byproducts.isEmpty()) {
            section(MaterialAtlas.label("byproducts"));
            int size = Math.min(18, Math.max(1, (RIGHT - 3) / byproducts.size()));
            int x = 3;
            for (var stack : byproducts) {
                cells.add(new Cell(x, height, size, stack, null, null));
                x += size;
            }
            height += size + 4;
        }
        for (var stage : MaterialAtlas.STAGES) {
            if (!MaterialAtlas.stage(stage).equals(stage)) continue;
            var branches = entry.routes.entrySet().stream()
                    .filter(r -> stage.equals(r.getKey().stage()))
                    .map(Map.Entry::getValue)
                    .toList();
            if (branches.isEmpty()) continue;
            var forms = entry.forms.stream()
                    .filter(f ->
                            MaterialAtlas.STAGES.contains(f.prefix()) && stage.equals(MaterialAtlas.stage(f.prefix())))
                    .sorted(Comparator.comparingInt(f -> MaterialAtlas.STAGES.indexOf(f.prefix())))
                    .toList();
            int fx = 3;
            for (var form : forms) {
                if (fx + 18 > RIGHT) {
                    height += 18;
                    fx = 3;
                }
                cells.add(new Cell(fx, height, 18, form.stack(), null, null));
                fx += 18;
            }
            Component title = forms.get(0).stack().getHoverName();
            if (stage.equals("ore") && forms.size() > 1) {
                boolean dense = forms.stream().anyMatch(form -> form.prefix().equals("oreDense"));
                title = MaterialAtlas.label(dense ? "ore_inputs" : "ore_and_raw");
            }
            captions.add(new Caption(title, fx + 3, height + 5, RIGHT - fx - 3));
            height += 18;
            int trunk = height;
            height += 3;
            for (int i = 0; i < branches.size(); i += 2) {
                int mid = height + 2;
                lines.add(new Line(12, trunk, 13, mid + 1));
                int right = i + 1 < branches.size() ? 125 : 47;
                lines.add(new Line(12, mid, right + 1, mid + 1));
                int rowHeight = 0;
                for (int j = 0; j < 2 && i + j < branches.size(); j++) {
                    int x = 17 + j * 78;
                    int tip = x + 30;
                    lines.add(new Line(tip, mid, tip + 1, mid + 5));
                    lines.add(new Line(tip - 1, mid + 3, tip + 2, mid + 4));
                    var route = branches.get(i + j);
                    var cell = new Cell(x, height + 7, 71, route.feed(), null, route);
                    cells.add(cell);
                    rowHeight = Math.max(rowHeight, cell.height());
                }
                trunk = mid;
                height += 10 + rowHeight;
            }
            height += 4;
        }
    }

    private int limit() {
        return Math.max(0, height - (bottom - TOP));
    }

    private void position(double value) {
        scroll = (int) Math.round(Math.min(limit(), Math.max(0, value)));
    }

    public int offset() {
        return scroll;
    }

    public boolean wheel(double y, double delta) {
        if (y < TOP || y >= bottom || limit() == 0 || delta == 0) return false;
        position(scroll - Math.copySign(Math.max(1, Math.abs(delta) * 18), delta));
        return true;
    }

    private int thumb() {
        return Math.max(10, (bottom - TOP) * (bottom - TOP) / Math.max(1, height));
    }

    private int thumbStart() {
        return TOP + scroll * (bottom - TOP - thumb()) / Math.max(1, limit());
    }

    public boolean scrollbar(double x, double y) {
        if (x < RIGHT || x >= WIDTH || y < TOP || y >= bottom || limit() == 0) return false;
        if (y < thumbStart() || y >= thumbStart() + thumb())
            position((y - TOP - thumb() / 2.0) * limit() / (bottom - TOP - thumb()));
        return true;
    }

    public void drag(double dy) {
        position(scroll + dy * limit() / Math.max(1, bottom - TOP - thumb()));
    }

    private Cell hit(double x, double y) {
        if (x >= header.x && x < header.x + 18 && y >= header.y && y < header.y + 18) return header;
        if (x < 0 || x >= RIGHT || y < TOP || y >= bottom) return null;
        double localY = y - TOP + scroll;
        for (var c : cells) if (x >= c.x && x < c.x + c.width && localY >= c.y && localY < c.y + c.height()) return c;
        return null;
    }

    private int outputAt(Cell cell, double x, double y) {
        double dx = x - cell.x - 4, dy = y - TOP + scroll - cell.outputY();
        if (dx < 0 || dy < 0 || dx >= OUTPUT_COLS * OUTPUT_PITCH || dx % OUTPUT_PITCH >= 18 || dy % OUTPUT_ROW >= 18)
            return -1;
        int index = (int) dy / OUTPUT_ROW * OUTPUT_COLS + (int) dx / OUTPUT_PITCH;
        return index < cell.choice().outputs().size() ? index : -1;
    }

    public boolean click(double x, double y, int button, boolean shiftDown, BiConsumer<ItemStack, Boolean> open) {
        if (button != 0 && button != 1) return false;
        var c = hit(x, y);
        if (c == null) return false;
        if (button == 0 && shiftDown && jump(x, y)) return true;
        if (c == header && page.kind() == Kind.ORE && button == 0) {
            openMaterial.run();
            return true;
        }
        var s = hovered(x, y);
        if (!s.isEmpty()) {
            open.accept(s, button == 1);
            return true;
        }
        if (c.choices.size() < 2) return false;
        double docY = y - TOP + scroll;
        int direction = button == 0 ? c.arrowAt(x, docY) : 0;
        if (docY < c.y + 14) direction = button == 0 ? 1 : -1;
        if (direction == 0) return false;
        c.variant = Math.floorMod(c.variant + direction, c.choices.size());
        return true;
    }

    public void draw(GuiGraphics g, double mouseX, double mouseY) {
        var e = page.entry();
        if (page.kind() == Kind.ORE && recipeRevision != e.recipeRevision) {
            cells.clear();
            captions.clear();
            lines.clear();
            height = 0;
            oreChart();
            position(scroll);
        }
        g.renderItem(e.icon(), 2, 3);
        if (hit(mouseX, mouseY) == header) g.fill(2, 3, 18, 19, 0x66ffffff);
        text(g, MaterialAtlas.name(e.material), 23, 1, WIDTH - 24);
        var detail = page.kind() == Kind.MATERIAL && e.material.getToolDurability() > 0
                ? MaterialAtlas.label(
                        "tool_stats",
                        e.material.getToolQuality(),
                        e.material.getToolSpeed(),
                        e.material.getToolDurability())
                : Component.literal(e.material.getTooltipChemical());
        text(g, detail, 23, 12, WIDTH - 24);
        g.fill(1, TOP - 3, WIDTH - 1, TOP - 2, RULE);
        var region = PreviewViewport.screenBounds(g.pose().last().pose(), 0, TOP, RIGHT, bottom);
        g.flush();
        g.pose().pushPose();
        g.pose().setIdentity();
        g.enableScissor(region.left(), region.top(), region.right(), region.bottom());
        g.pose().popPose();
        var selected = hit(mouseX, mouseY);
        try {
            g.pose().pushPose();
            g.pose().translate(0, TOP - scroll, 0);
            for (var line : lines)
                if (visible(line.y, line.bottom - line.y)) g.fill(line.x, line.y, line.right, line.bottom, RULE);
            for (var t : captions) if (visible(t.y, 10)) text(g, t.text, t.x, t.y, t.width);
            for (var c : cells)
                if (visible(c.y, c.height())) {
                    if (c.route != null) {
                        var choice = c.choice();
                        var recipe = choice.row();
                        if (!recipe.workstation().isEmpty()) {
                            g.pose().pushPose();
                            g.pose().translate(c.x + 1, c.y, 0);
                            g.pose().scale(14f / 16, 14f / 16, 1);
                            g.renderItem(recipe.workstation(), 0, 0);
                            g.pose().popPose();
                        }
                        text(g, recipe.title(), c.x + 17, c.y + 3, c.width - 17);
                        for (int i = 0; i < choice.outputs().size(); i++) {
                            var output = choice.outputs().get(i);
                            int sx = c.x + 4 + i % OUTPUT_COLS * OUTPUT_PITCH;
                            int sy = c.outputY() + i / OUTPUT_COLS * OUTPUT_ROW;
                            slot(g, output.stack(), sx, sy);
                            if (!output.detail().isEmpty()) {
                                var font = Minecraft.getInstance().font;
                                float scale = Math.min(.75f, (OUTPUT_PITCH - 1f) / font.width(output.detail()));
                                g.pose().pushPose();
                                g.pose().translate(sx, sy + 18, 0);
                                g.pose().scale(scale, scale, 1);
                                g.drawString(font, output.detail(), 0, 0, INK, false);
                                g.pose().popPose();
                            }
                            if (selected == c && outputAt(c, mouseX, mouseY) == i)
                                g.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0x66ffffff);
                        }
                        if (c.choices.size() > 1) {
                            int hovered = selected == c ? c.arrowAt(mouseX, mouseY - TOP + scroll) : 0;
                            drawArrow(g, c, -1, hovered == -1);
                            drawArrow(g, c, 1, hovered == 1);
                        }
                    } else {
                        if (c.width == 18) slot(g, c.stack, c.x, c.y);
                        else {
                            g.pose().pushPose();
                            g.pose().translate(c.x, c.y, 0);
                            g.pose().scale(c.width / 18f, c.width / 18f, 1);
                            slot(g, c.stack, 0, 0);
                            g.pose().popPose();
                        }
                        if (c == selected)
                            g.fill(c.x + 1, c.y + 1, c.x + c.width - 1, c.y + c.height() - 1, 0x66ffffff);
                    }
                }
            g.pose().popPose();
            g.flush();
        } finally {
            g.disableScissor();
        }
        if (limit() > 0) {
            g.fill(RIGHT + 2, TOP, WIDTH - 1, bottom, 0xff787878);
            g.fill(RIGHT + 2, thumbStart(), WIDTH - 1, thumbStart() + thumb(), 0xffe4e4e4);
            g.fill(WIDTH - 2, thumbStart() + 1, WIDTH - 1, thumbStart() + thumb(), RULE);
        }
    }

    private boolean visible(int y, int h) {
        return y + h > scroll && y < scroll + bottom - TOP;
    }

    private static void drawArrow(GuiGraphics g, Cell cell, int direction, boolean hovered) {
        int x = cell.arrowX(direction), y = cell.arrowY();
        var font = Minecraft.getInstance().font;
        String glyph = direction < 0 ? "<" : ">";
        if (hovered) g.fill(x, y, x + ARROW_WIDTH, y + ARROW_HEIGHT, RULE);
        int glyphX = x + (ARROW_WIDTH - font.width(glyph)) / 2;
        int glyphY = y + (ARROW_HEIGHT - font.lineHeight) / 2 + 1;
        g.drawString(font, glyph, glyphX, glyphY, hovered ? 0xffffffff : INK, false);
    }

    private static void text(GuiGraphics g, Component t, int x, int y, int width) {
        var font = Minecraft.getInstance().font;
        g.drawString(font, font.substrByWidth(t, width).getString(), x, y, INK, false);
    }

    static void item(GuiGraphics g, ItemStack s, int x, int y) {
        if (s.isEmpty()) return;
        g.renderItem(s, x, y);
        g.renderItemDecorations(Minecraft.getInstance().font, s, x, y);
    }

    static void slot(GuiGraphics g, ItemStack s, int x, int y) {
        GuiTemplates.sprite("slot/item").draw(g, x, y);
        item(g, s, x + 1, y + 1);
    }

    public ItemStack hovered(double x, double y) {
        var cell = hit(x, y);
        if (cell == null) return ItemStack.EMPTY;
        if (cell.route == null) return cell.stack;
        int index = outputAt(cell, x, y);
        return index < 0 ? ItemStack.EMPTY : cell.choice().outputs().get(index).stack();
    }

    public AtlasRecipeCard card(double x, double y) {
        var cell = hit(x, y);
        if (cell == null || (cell.route != null && outputAt(cell, x, y) < 0)) return null;
        var preview = cell.recipe(page.entry());
        if (preview == null) return null;
        if (cardCell != cell || card.preview() != preview) {
            card = new AtlasRecipeCard(preview, cell.route != null);
            cardCell = cell;
        }
        return card;
    }

    public boolean jump(double x, double y) {
        var selected = card(x, y);
        if (selected == null) return false;
        openRecipe.accept(hovered(x, y), selected.preview());
        return true;
    }
}
