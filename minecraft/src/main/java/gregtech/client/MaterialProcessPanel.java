package gregtech.client;

import com.mojang.math.Axis;
import gregtech.client.MaterialProcessOverview.Node;
import gregtech.compat.common.MaterialProcessDisplay.Page;
import gregtech.compat.common.MaterialProcessDisplay.Port;
import gregtech.compat.common.PreviewViewport;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** A readable viewport onto the cached topology layout and its native port instances. */
public final class MaterialProcessPanel {
    public static final int WIDTH = 280, HEIGHT = 216;
    private static final int BODY_TOP = 17;
    private static final int TEXT = 0xff383838, MUTED = 0xff777777, GOLD = 0xffffcc44;
    private static final double MIN_ZOOM = 0.0001, MAX_ZOOM = 3;
    private final Page page;
    private final MaterialProcessOverview overview;
    private Node focused, hovered;
    private Set<Node> highlight = Set.of();
    private int width = WIDTH, height = HEIGHT;
    private boolean fullscreen;
    private double zoom, panX, panY;

    public MaterialProcessPanel(Page page) {
        this.page = page;
        overview = new MaterialProcessOverview(page);
        resetView();
    }

    public void focus(Predicate<Port> matches) {
        focused = overview.find(matches);
        resetView();
    }

    public static int preferredWidth() {
        return Math.max(200, Minecraft.getInstance().getWindow().getGuiScaledWidth() - 160);
    }

    public static int preferredHeight() {
        return Math.max(80, Minecraft.getInstance().getWindow().getGuiScaledHeight() - 110);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    private int footer() {
        return height - 13;
    }

    /** Resizing keeps the same world point at the center, including when entering or leaving fullscreen. */
    public void resize(int width, int height) {
        int nextWidth = Math.max(160, width), nextHeight = Math.max(64, height);
        panX += (nextWidth - this.width) / 2.0;
        panY += (nextHeight - this.height) / 2.0;
        this.width = nextWidth;
        this.height = nextHeight;
    }

    void fullscreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
    }

    Component title() {
        return page.title();
    }

    public PreviewViewport.Bounds boundsAt(double x, double y) {
        Node node = hit(x, y);
        return node == null ? new PreviewViewport.Bounds(0, 0, 0, 0) : bounds(node);
    }

    private void drawFullscreenButton(GuiGraphics graphics) {
        int x = width - 16, y = 3;
        for (int dx : new int[] {0, 8})
            for (int dy : new int[] {0, 8}) {
                graphics.fill(x + dx, y + dy, x + dx + 4, y + dy + 1, TEXT);
                graphics.fill(
                        x + dx + (dx == 0 ? 0 : 3),
                        y + (dy == 0 ? 0 : dy - 3),
                        x + dx + (dx == 0 ? 1 : 4),
                        y + (dy == 0 ? 4 : dy + 1),
                        TEXT);
            }
    }

    private static void outline(GuiGraphics graphics, int left, int top, int right, int bottom, int color) {
        graphics.fill(left, top, right, top + 1, color);
        graphics.fill(left, bottom - 1, right, bottom, color);
        graphics.fill(left, top, left + 1, bottom, color);
        graphics.fill(right - 1, top, right, bottom, color);
    }

    public void draw(GuiGraphics graphics, double mouseX, double mouseY) {
        graphics.fill(0, 0, width, height, 0xffc6c6c6);
        text(graphics, page.title(), 4, 2, width - 148, TEXT);
        text(graphics, Component.literal("−"), width - 138, 2, 12, TEXT);
        text(
                graphics,
                Component.literal(java.math.BigDecimal.valueOf(zoom * 100)
                                .setScale(zoom < 0.01 ? 2 : 0, java.math.RoundingMode.HALF_UP)
                                .stripTrailingZeros()
                                .toPlainString()
                        + "%"),
                width - 120,
                2,
                33,
                MUTED);
        text(graphics, Component.literal("+"), width - 82, 2, 12, TEXT);
        text(graphics, Component.translatable("gt6ce.jei.material_process.reset"), width - 58, 2, 35, MUTED);
        drawFullscreenButton(graphics);
        graphics.flush();
        var viewport = PreviewViewport.screenBounds(graphics.pose().last().pose(), 0, BODY_TOP, width, footer());
        graphics.enableScissor(viewport.left(), viewport.top(), viewport.right(), viewport.bottom());
        Node hit = hit(mouseX, mouseY);
        if (hit != hovered) {
            hovered = hit;
            highlight = overview.matchingNodes(hovered);
        }
        try {
            graphics.pose().pushPose();
            try {
                graphics.pose().translate(panX, BODY_TOP + panY, 0);
                graphics.pose().scale((float) zoom, (float) zoom, 1);
                for (var band : overview.bands()) {
                    graphics.fill(band.left() + 2, 2, band.right() - 2, 24, 0xffb8b8b8);
                    text(graphics, band.section().label(), band.left() + 6, 8, band.right() - band.left() - 12, TEXT);
                    if (band.left() > 0) graphics.fill(band.left(), 2, band.left() + 1, overview.height(), 0xff999999);
                }
                for (var frame : overview.frames()) {
                    graphics.fill(frame.left(), frame.top(), frame.right(), frame.bottom(), 0xffcacaca);
                    outline(graphics, frame.left(), frame.top(), frame.right(), frame.bottom(), 0xffe4e4e4);
                }
                drawEdges(graphics);
                if (!overview.information().isEmpty()) {
                    var first = overview.information().get(0);
                    var hoveredInformation = informationHit(mouseX, mouseY);
                    text(
                            graphics,
                            Component.translatable("gt6ce.jei.material_process.information"),
                            first.left(),
                            first.top() - 12,
                            152,
                            MUTED);
                    for (var card : overview.information()) {
                        if (card.right() < worldX(0)
                                || card.left() > worldX(width)
                                || card.bottom() < worldY(BODY_TOP)
                                || card.top() > worldY(footer())) continue;
                        boolean hover = card == hoveredInformation;
                        graphics.fill(
                                card.left(), card.top(), card.right(), card.bottom(), hover ? 0xffe0d6b5 : 0xffcfcbc0);
                        outline(graphics, card.left(), card.top(), card.right(), card.bottom(), 0xff9c9274);
                        if (!card.information().icon().isEmpty())
                            graphics.renderItem(card.information().icon(), card.left() + 4, card.top() + 4);
                        text(graphics, card.information().name(), card.left() + 24, card.top() + 7, 122, TEXT);
                    }
                }
                for (var grid : overview.grids()) {
                    graphics.fill(grid.left(), grid.top(), grid.right(), grid.bottom(), 0xff777777);
                    for (int y = grid.top(); y < grid.bottom(); y += 24)
                        for (int x = grid.left(); x < grid.right(); x += 24)
                            graphics.fill(x + 1, y + 1, x + 23, y + 23, 0xffb0b0b0);
                }
                for (Node node : overview.nodes()) {
                    if (!visible(node)) continue;
                    if (highlight.contains(node))
                        graphics.fill(node.x - 11, node.y - 11, node.x + 12, node.y + 12, 0xffe4e4e4);
                    if (node == focused) {
                        outline(graphics, node.x - 13, node.y - 13, node.x + 14, node.y + 14, 0xffa7dba0);
                        outline(graphics, node.x - 12, node.y - 12, node.x + 13, node.y + 13, 0xffbce7b6);
                    }
                    ItemStack icon =
                            node.machine() ? selected(node).icon() : node.port().icon();
                    if (icon.isEmpty()) text(graphics, Component.literal("?"), node.x - 3, node.y - 4, 10, MUTED);
                    else graphics.renderItem(icon, node.x - 8, node.y - 8);
                    if (!node.machine()) {
                        smallText(graphics, node.quantityBadge(), node.x + 10, node.y + 3, 0xffffffff, true);
                        smallText(graphics, node.chanceBadge(), node.x - 10, node.y - 10, GOLD, false);
                    }
                    if (node.machine() && node.extra()) smallText(graphics, "+", node.x + 9, node.y - 12, GOLD, false);
                    if (!node.machine() && (node.copy || node.returning))
                        smallText(graphics, node.returning ? "↩" : "↗", node.x + 8, node.y - 12, MUTED, false);
                }
            } finally {
                graphics.pose().popPose();
            }
        } finally {
            graphics.flush();
            graphics.disableScissor();
        }
        if (overview.nodes().isEmpty() && overview.information().isEmpty())
            graphics.drawWordWrap(
                    Minecraft.getInstance().font,
                    Component.translatable("gt6ce.jei.material_process.empty"),
                    12,
                    BODY_TOP + 28,
                    width - 24,
                    MUTED);
        text(graphics, Component.literal("i"), 4, footer() + 3, 12, MUTED);
    }

    private gregtech.compat.common.MaterialProcessDisplay.Step selected(Node node) {
        return node.steps.get(0);
    }

    private boolean visible(Node node) {
        return node.x + 12 >= worldX(0)
                && node.x - 12 <= worldX(width)
                && node.y + 12 >= worldY(BODY_TOP)
                && node.y - 14 <= worldY(footer());
    }

    private Node hit(double mouseX, double mouseY) {
        if (mouseX < 0 || mouseX >= width || mouseY < BODY_TOP || mouseY >= footer()) return null;
        double x = worldX(mouseX), y = worldY(mouseY);
        for (Node node : overview.nodes())
            if (x >= node.x - 11 && x < node.x + 12 && y >= node.y - 12 && y < node.y + 12) return node;
        return null;
    }

    private MaterialProcessOverview.InformationCard informationHit(double mouseX, double mouseY) {
        if (mouseX < 0 || mouseX >= width || mouseY < BODY_TOP || mouseY >= footer()) return null;
        double x = worldX(mouseX), y = worldY(mouseY);
        for (var card : overview.information())
            if (x >= card.left() && x < card.right() && y >= card.top() && y < card.bottom()) return card;
        return null;
    }

    public List<ItemStack> stacksAt(double mouseX, double mouseY) {
        Node node = hit(mouseX, mouseY);
        return node == null ? List.of() : stacks(node);
    }

    public List<Node> nodes() {
        return overview.nodes();
    }

    public PreviewViewport.Bounds bounds(Node node) {
        return new PreviewViewport.Bounds(
                (int) Math.max(0, Math.floor(panX + (node.x - 11) * zoom)),
                (int) Math.max(BODY_TOP, Math.floor(BODY_TOP + panY + (node.y - 12) * zoom)),
                (int) Math.min(width, Math.ceil(panX + (node.x + 12) * zoom)),
                (int) Math.min(footer(), Math.ceil(BODY_TOP + panY + (node.y + 12) * zoom)));
    }

    public List<ItemStack> stacks(Node node) {
        if (!page.navigation().valid()) return List.of();
        if (node.machine())
            return selected(node).icon().isEmpty()
                    ? List.of()
                    : List.of(selected(node).icon());
        return node.port().hasIcons() ? List.of(node.port().icon()) : List.of();
    }

    public List<Component> tooltip(double mouseX, double mouseY) {
        if (mouseY >= 0 && mouseY < BODY_TOP && mouseX >= width - 144)
            return List.of(Component.translatable(
                    mouseX >= width - 20
                            ? fullscreen
                                    ? "gt6ce.jei.material_process.exit_fullscreen"
                                    : "gt6ce.jei.material_process.fullscreen"
                            : mouseX >= width - 63
                                    ? "gt6ce.jei.material_process.reset_hint"
                                    : "gt6ce.jei.material_process.zoom_hint"));
        if (mouseY >= footer()) {
            var lines = new ArrayList<>(page.notices());
            lines.add(Component.translatable("gt6ce.jei.material_process.pan"));
            return List.copyOf(lines);
        }
        var information = informationHit(mouseX, mouseY);
        if (information != null) {
            var lines = new ArrayList<Component>();
            lines.add(information.information().name());
            lines.addAll(information.information().details());
            lines.add(Component.translatable("gt6ce.jei.material_process.open_information"));
            return List.copyOf(lines);
        }
        Node node = hit(mouseX, mouseY);
        if (node == null) return List.of();
        ItemStack icon = node.machine() ? selected(node).icon() : node.port().icon();
        Component name = icon.isEmpty()
                ? (node.machine() ? selected(node).name() : node.port().name())
                : icon.getHoverName();
        if (!node.machine()) {
            List<List<Component>> variants = node.badgePorts().stream()
                    .map(port -> List.of(
                            port.amount(),
                            port.chance().isPresent()
                                    ? Component.translatable(
                                            "gt6ce.jei.chance", port.chance().getAsInt() / 100.0)
                                    : Component.translatable("gt6ce.jei.material_process.chance_unknown")))
                    .distinct()
                    .toList();
            if (variants.size() <= 1) return List.of(name);
            var lines = new ArrayList<Component>();
            lines.add(name);
            variants.forEach(lines::addAll);
            return List.copyOf(lines);
        }
        if (!node.extra() || worldX(mouseX) < node.x + 6 || worldY(mouseY) > node.y - 6) return List.of(name);
        var lines = new ArrayList<Component>();
        lines.add(name);
        if (node.steps.size() > 1)
            lines.add(Component.translatable("gt6ce.jei.material_process.variants", node.steps.size()));
        var names = new java.util.LinkedHashMap<String, Component>();
        for (var port : node.auxiliaries) {
            Component label = port.hasIcons() ? port.icon().getHoverName() : port.name();
            names.putIfAbsent(label.getString(), label);
        }
        lines.addAll(names.values().stream().limit(12).toList());
        if (names.size() > 12) lines.add(Component.literal("…"));
        var step = selected(node);
        step.specialProcess().ifPresent(process -> lines.add(process.name()));
        lines.addAll(step.details().stream().limit(8).toList());
        return List.copyOf(lines);
    }

    public boolean valid() {
        return page.navigation().valid();
    }

    public boolean click(double mouseX, double mouseY, int button) {
        if (!valid()) return false;
        if (clickControls(mouseX, mouseY, button)) return true;
        if (button != 0 && button != 1) return false;
        Node node = hit(mouseX, mouseY);
        if (node == null) return false;
        if (!node.machine()) return page.navigation().openPort(node.port(), button == 1);
        return page.navigation().openStack(selected(node).icon(), button == 1);
    }

    public boolean clickControls(double mouseX, double mouseY, int button) {
        if (button != 0 && button != 1) return false;
        var information = informationHit(mouseX, mouseY);
        if (information != null)
            return page.navigation().valid()
                    && page.navigation()
                            .openInformation(information.information().reference());
        if (button == 0 && mouseY >= 0 && mouseY < 14) {
            if (mouseX >= width - 20 && mouseX < width) {
                MaterialProcessScreen.toggle(this);
                return true;
            }
            if (mouseX >= width - 63 && mouseX < width - 22) {
                resetView();
                return true;
            }
            if (mouseX >= width - 144 && mouseX < width - 123) {
                zoomAt(width / 2.0, (BODY_TOP + footer()) / 2.0, zoom / 1.25);
                return true;
            }
            if (mouseX >= width - 87 && mouseX < width - 65) {
                zoomAt(width / 2.0, (BODY_TOP + footer()) / 2.0, zoom * 1.25);
                return true;
            }
        }
        return false;
    }

    public boolean press(double mouseX, double mouseY, int button) {
        return (button == 0 || button == 1)
                && mouseX >= 0
                && mouseX < width
                && mouseY >= BODY_TOP
                && mouseY < footer()
                && informationHit(mouseX, mouseY) == null
                && hit(mouseX, mouseY) == null;
    }

    public boolean drag(int button, double dx, double dy) {
        if (button != 0 && button != 1) return false;
        panX += dx;
        panY += dy;
        return true;
    }

    public boolean scroll(double mouseX, double mouseY, double delta) {
        if (mouseX < 0 || mouseX >= width || mouseY < BODY_TOP || mouseY >= footer() || delta == 0) return false;
        zoomAt(mouseX, mouseY, zoom * Math.pow(1.15, Math.max(-8, Math.min(8, delta))));
        return true;
    }

    private double worldX(double x) {
        return (x - panX) / zoom;
    }

    private double worldY(double y) {
        return (y - BODY_TOP - panY) / zoom;
    }

    private void zoomAt(double x, double y, double requested) {
        double wx = worldX(x), wy = worldY(y);
        zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, requested));
        panX = x - wx * zoom;
        panY = y - BODY_TOP - wy * zoom;
    }

    private void resetView() {
        zoom = 1;
        Node anchor = focused == null ? overview.anchor() : focused;
        if (anchor == null && !overview.information().isEmpty()) {
            var first = overview.information().get(0);
            panX = width / 2.0 - (first.left() + first.right()) / 2.0;
            panY = (footer() - BODY_TOP) / 2.0 - (first.top() + first.bottom()) / 2.0;
            return;
        }
        panX = width / 2.0 - (anchor == null ? overview.width() / 2.0 : anchor.x);
        panY = (footer() - BODY_TOP) / 2.0 - (anchor == null ? overview.height() / 2.0 : anchor.y);
    }

    private void drawEdges(GuiGraphics graphics) {
        record Wire(List<MaterialProcessOverview.Point> path, boolean output) {}
        var wires = new java.util.LinkedHashMap<Wire, List<MaterialProcessOverview.Edge>>();
        for (var edge : overview.edges())
            wires.computeIfAbsent(new Wire(edge.path(), edge.endpoint().output()), ignored -> new ArrayList<>())
                    .add(edge);
        for (var entry : wires.entrySet()) {
            boolean confirmed = entry.getValue().stream().allMatch(MaterialProcessOverview.Edge::confirmed);
            int color = TEXT;
            var path = entry.getKey().path();
            for (int index = 1; index < path.size(); index++) {
                var from = path.get(index - 1);
                var to = path.get(index);
                segment(
                        graphics,
                        from.x(),
                        from.y(),
                        to.x(),
                        to.y(),
                        !confirmed,
                        entry.getKey().output() && index == path.size() - 1,
                        color);
            }
        }
    }

    private void segment(
            GuiGraphics graphics,
            double sx,
            double sy,
            double tx,
            double ty,
            boolean dashed,
            boolean arrow,
            int color) {
        stroke(graphics, sx, sy, tx, ty, color, dashed);
        if (!arrow || tx < worldX(0) || tx >= worldX(width) || ty < worldY(BODY_TOP) || ty >= worldY(footer())) return;
        double angle = Math.atan2(ty - sy, tx - sx);
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(tx, ty, 0);
            graphics.pose().mulPose(Axis.ZP.rotationDegrees((float) Math.toDegrees(angle)));
            MaterialProcessArrows.head(graphics, color);
        } finally {
            graphics.pose().popPose();
        }
    }

    private static void smallText(GuiGraphics graphics, String value, int x, int y, int color, boolean right) {
        if (value.isEmpty()) return;
        var font = Minecraft.getInstance().font;
        float scale = Math.min(0.65F, 22F / Math.max(1, font.width(value)));
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 200);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawString(font, value, right ? -font.width(value) : 0, 0, color, true);
        graphics.pose().popPose();
    }

    private static void text(GuiGraphics graphics, Component value, int x, int y, int width, int color) {
        if (width <= 0) return;
        var font = Minecraft.getInstance().font;
        graphics.drawString(
                font,
                net.minecraft.locale.Language.getInstance().getVisualOrder(font.substrByWidth(value, width)),
                x,
                y,
                color,
                false);
    }

    private void stroke(GuiGraphics graphics, double x1, double y1, double x2, double y2, int color, boolean dashed) {
        double dx = x2 - x1, dy = y2 - y1, from = 0, to = 1;
        double[] p = {-dx, dx, -dy, dy},
                q = {x1 - worldX(-1), worldX(width + 1) - x1, y1 - worldY(BODY_TOP - 1), worldY(footer() + 1) - y1};
        for (int side = 0; side < 4; side++) {
            if (p[side] == 0) {
                if (q[side] < 0) return;
            } else {
                double ratio = q[side] / p[side];
                if (p[side] < 0) from = Math.max(from, ratio);
                else to = Math.min(to, ratio);
                if (from > to) return;
            }
        }
        double length = Math.hypot(dx, dy), visible = (to - from) * length;
        if (visible < 0.1) return;
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(x1 + from * dx, y1 + from * dy, 0);
            graphics.pose().mulPose(Axis.ZP.rotationDegrees((float) Math.toDegrees(Math.atan2(dy, dx))));
            if (!dashed) graphics.fill(0, 0, (int) Math.ceil(visible), 1, color);
            else
                for (int x = 0; x < visible; x += 6)
                    graphics.fill(x, 0, Math.min(x + 3, (int) Math.ceil(visible)), 1, color);
        } finally {
            graphics.pose().popPose();
        }
    }
}
