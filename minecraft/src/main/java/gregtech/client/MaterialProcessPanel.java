package gregtech.client;

import gregtech.api.addon.recipe.RecipeOutputChances;
import gregtech.compat.common.MaterialProcessDisplay.Page;
import gregtech.compat.common.MaterialProcessDisplay.Port;
import gregtech.compat.common.MaterialProcessDisplay.Role;
import gregtech.compat.common.MaterialProcessDisplay.Step;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Viewer-independent geometry and rendering; native ingredient widgets use exactly these slot positions. */
public final class MaterialProcessPanel {
    public static final int WIDTH = 280, HEIGHT = 216;
    public static final int HEADER = 30, STEP_HEIGHT = 80, FOOTER = 196;
    private static final int SIDE_LIMIT = 6;
    private static final int MAIN_X = 132, STEP_X = 103, STEP_WIDTH = 74;
    private static final int TEXT = 0xff404040, MUTED = 0xff707070, ACCENT = 0xff537962;

    public record Slot(Port port, int x, int y, boolean output) {
        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
        }
    }

    private MaterialProcessPanel() {}

    public static List<Slot> slots(Step step, int row) {
        var slots = new ArrayList<Slot>();
        addSlots(slots, step.inputs(), row, false);
        addSlots(slots, step.outputs(), row, true);
        return List.copyOf(slots);
    }

    private static void addSlots(List<Slot> slots, List<Port> ports, int row, boolean output) {
        int side = 0;
        for (var port : ports) {
            if (port.main()) {
                slots.add(new Slot(port, MAIN_X, HEADER + row * STEP_HEIGHT + (output ? 57 : 0), output));
            } else if (side < SIDE_LIMIT) {
                slots.add(new Slot(
                        port,
                        (output ? 202 : 10) + side % 3 * 23,
                        HEADER + row * STEP_HEIGHT + 23 + side / 3 * 24,
                        output));
                side++;
            }
        }
    }

    public static void draw(Page page, GuiGraphics graphics, double mouseX, double mouseY) {
        text(graphics, page.title(), 2, 1, WIDTH - 42, TEXT);
        text(graphics, Component.literal(page.number() + "/" + page.total()), WIDTH - 38, 1, 38, MUTED);
        text(graphics, Component.translatable("gt6ce.jei.material_process.inputs"), 5, 16, 90, MUTED);
        text(graphics, Component.translatable("gt6ce.jei.material_process.route"), 103, 16, 75, ACCENT);
        text(graphics, Component.translatable("gt6ce.jei.material_process.outputs"), 194, 16, 86, MUTED);
        for (int row = 0; row < page.steps().size(); row++) {
            var step = page.steps().get(row);
            int top = HEADER + row * STEP_HEIGHT;
            int color = step.resolved() ? ACCENT : MUTED;
            if (step.inputs().stream().anyMatch(Port::main))
                line(graphics, MAIN_X + 8, top + 17, MAIN_X + 8, top + 26, color, !step.resolved());
            graphics.fill(
                    STEP_X,
                    top + 26,
                    STEP_X + STEP_WIDTH,
                    top + 45,
                    processContains(row, mouseX, mouseY) ? 0xffd6e2d9 : 0xffe7e7e7);
            text(graphics, step.name(), STEP_X + 3, top + 32, STEP_WIDTH - 6, color);
            var displayed = slots(step, row);
            for (var slot : displayed) {
                int portColor = slot.port().role() == Role.BYPRODUCT ? 0xffb68a42 : color;
                boolean uncertain = !step.resolved()
                        || slot.output()
                                && (slot.port().chance().isEmpty()
                                        || slot.port().chance().getAsInt() < RecipeOutputChances.CERTAIN);
                if (slot.port().main() && slot.output()) {
                    arrow(graphics, MAIN_X + 8, top + 53, portColor);
                    line(graphics, MAIN_X + 8, top + 45, MAIN_X + 8, top + 53, portColor, uncertain);
                }
                if (!slot.port().main()) {
                    int middle = slot.output() ? 188 : 88;
                    line(
                            graphics,
                            slot.output() ? STEP_X + STEP_WIDTH : STEP_X - 2,
                            top + 35,
                            middle,
                            top + 35,
                            portColor,
                            uncertain);
                    line(graphics, middle, top + 35, middle, slot.y() + 8, portColor, uncertain);
                    line(
                            graphics,
                            middle,
                            slot.y() + 8,
                            slot.output() ? slot.x() - 2 : slot.x() + 17,
                            slot.y() + 8,
                            portColor,
                            uncertain);
                }
            }
            for (var slot : displayed) {
                graphics.fill(slot.x() - 1, slot.y() - 1, slot.x() + 17, slot.y() + 17, 0xffa0a0a0);
                graphics.fill(slot.x(), slot.y(), slot.x() + 16, slot.y() + 16, 0xffeeeeee);
                if (!slot.port().hasIcons())
                    text(graphics, Component.literal("?"), slot.x() + 5, slot.y() + 4, 10, MUTED);
                if (!step.resolved()
                        || slot.output()
                                && (slot.port().chance().isEmpty()
                                        || slot.port().chance().getAsInt() < RecipeOutputChances.CERTAIN))
                    graphics.fill(slot.x() + 14, slot.y() - 1, slot.x() + 17, slot.y() + 2, 0xffc79c42);
            }
            overflow(graphics, step.inputs(), 5, top + 68);
            overflow(graphics, step.outputs(), 202, top + 68);
            long links = page.connections().stream()
                    .filter(connection -> connection.source().equals(step.reference())
                            || connection.target().equals(step.reference()))
                    .count();
            if (links > 0)
                text(
                        graphics,
                        Component.translatable("gt6ce.jei.material_process.links", links),
                        153,
                        top + 65,
                        45,
                        MUTED);
            if (row + 1 < page.steps().size()) {
                var next = page.steps().get(row + 1);
                for (var connection : page.connections()) {
                    if (!connection.source().equals(step.reference())
                            || !connection.target().equals(next.reference())) continue;
                    boolean mainOutput = step.outputs().stream()
                            .anyMatch(port -> port.main() && port.resource().equals(connection.resource()));
                    boolean mainInput = next.inputs().stream()
                            .anyMatch(port -> port.main() && port.resource().equals(connection.resource()));
                    if (mainOutput && mainInput) {
                        boolean uncertain = !connection.confirmed()
                                || !step.resolved()
                                || !next.resolved()
                                || step.outputs().stream()
                                        .filter(Port::main)
                                        .anyMatch(port -> port.chance().isEmpty()
                                                || port.chance().getAsInt() < RecipeOutputChances.CERTAIN);
                        line(graphics, MAIN_X + 8, top + 74, MAIN_X + 8, top + 77, ACCENT, uncertain);
                        arrow(graphics, MAIN_X + 8, top + 77, ACCENT);
                    }
                }
            }
        }
        if (page.steps().isEmpty()) {
            var font = Minecraft.getInstance().font;
            graphics.drawWordWrap(
                    font, Component.translatable("gt6ce.jei.material_process.empty"), 14, 66, WIDTH - 28, MUTED);
            graphics.drawWordWrap(font, page.notices().get(0), 14, 90, WIDTH - 28, MUTED);
        }
        graphics.fill(0, FOOTER - 3, WIDTH, FOOTER - 2, 0xffb0b0b0);
        text(graphics, page.notices().get(0), 2, FOOTER, WIDTH - 4, MUTED);
        text(graphics, Component.translatable("gt6ce.jei.material_process.controls"), 2, FOOTER + 11, WIDTH - 4, MUTED);
    }

    public static List<Component> tooltip(Page page, double mouseX, double mouseY) {
        if (mouseY >= FOOTER - 3) return page.notices();
        if (mouseY < HEADER) return List.of(page.title());
        for (int row = 0; row < page.steps().size(); row++) {
            var step = page.steps().get(row);
            if (processContains(row, mouseX, mouseY)
                    || mouseX >= 153
                            && mouseX < 198
                            && mouseY >= HEADER + row * STEP_HEIGHT + 64
                            && mouseY < HEADER + row * STEP_HEIGHT + 77) {
                var lines = new ArrayList<Component>();
                lines.add(step.name());
                lines.addAll(step.details());
                if (!step.resolved()) lines.add(Component.translatable("gt6ce.jei.material_process.unresolved"));
                for (var connection : page.connections()) {
                    if (connection.source().equals(step.reference())
                            || connection.target().equals(step.reference()))
                        lines.add(Component.translatable(
                                "gt6ce.jei.material_process.connection",
                                connection.source().toString(),
                                connection.target().toString(),
                                connection.resource().toString(),
                                Component.translatable(
                                        connection.confirmed()
                                                ? "gt6ce.jei.material_process.confirmed"
                                                : "gt6ce.jei.material_process.unresolved")));
                }
                lines.add(Component.translatable("gt6ce.jei.material_process.open_recipe"));
                return lines;
            }
            for (var slot : slots(step, row))
                if (slot.contains(mouseX, mouseY)) return slot.port().tooltip();
            int top = HEADER + row * STEP_HEIGHT;
            if (mouseY >= top + 67 && mouseY < top + 79) {
                var ports = mouseX < STEP_X ? step.inputs() : mouseX >= 194 ? step.outputs() : List.<Port>of();
                var hidden = ports.stream()
                        .filter(port -> !port.main())
                        .skip(SIDE_LIMIT)
                        .toList();
                if (!hidden.isEmpty()) {
                    var lines = new ArrayList<Component>();
                    lines.add(Component.translatable("gt6ce.jei.material_process.more", hidden.size()));
                    for (var port : hidden) lines.addAll(port.tooltip());
                    return lines;
                }
            }
        }
        return List.of();
    }

    public static boolean click(Page page, double mouseX, double mouseY, int button) {
        if (button != 0 && button != 1) return false;
        for (int row = 0; row < page.steps().size(); row++) {
            var step = page.steps().get(row);
            if (button == 0 && step.resolved() && processContains(row, mouseX, mouseY))
                return page.navigation().openRecipe(step.reference());
            for (var slot : slots(step, row))
                if (slot.contains(mouseX, mouseY))
                    return page.navigation().openForm(slot.port().resource(), button == 1);
        }
        return false;
    }

    private static boolean processContains(int row, double mouseX, double mouseY) {
        int top = HEADER + row * STEP_HEIGHT;
        return mouseX >= STEP_X && mouseX < STEP_X + STEP_WIDTH && mouseY >= top + 26 && mouseY < top + 45;
    }

    private static void overflow(GuiGraphics graphics, List<Port> ports, int x, int y) {
        long count = ports.stream().filter(port -> !port.main()).count() - SIDE_LIMIT;
        if (count > 0)
            text(graphics, Component.translatable("gt6ce.jei.material_process.more", count), x, y, 76, MUTED);
    }

    private static void text(GuiGraphics graphics, Component value, int x, int y, int width, int color) {
        var font = Minecraft.getInstance().font;
        graphics.drawString(
                font,
                net.minecraft.locale.Language.getInstance().getVisualOrder(font.substrByWidth(value, width)),
                x,
                y,
                color,
                false);
    }

    private static void arrow(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x - 2, y, x + 3, y + 1, color);
        graphics.fill(x - 1, y + 1, x + 2, y + 2, color);
        graphics.fill(x, y + 2, x + 1, y + 3, color);
    }

    private static void line(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color, boolean dashed) {
        int length = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        for (int offset = 0; offset <= length; offset++) {
            if (dashed && offset % 5 >= 3) continue;
            int x = x1 + Integer.signum(x2 - x1) * offset;
            int y = y1 + Integer.signum(y2 - y1) * offset;
            graphics.fill(x, y, x + 1, y + 1, color);
        }
    }
}
