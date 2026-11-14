package gregtech.client;

import com.mojang.math.Axis;
import gregtech.compat.common.MaterialProcessDisplay.Page;
import gregtech.compat.common.MaterialProcessOverview;
import gregtech.compat.common.MaterialProcessOverview.Node;
import gregtech.compat.jei.PreviewViewport;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** A readable viewport onto the cached topology layout and its native port instances. */
public final class MaterialProcessPanel {
    public static final int WIDTH = 280, HEIGHT = 216;
    private static final int BODY_TOP = 17, FOOTER = 203;
    private static final int TEXT = 0xff383838, MUTED = 0xff777777, GOLD = 0xffffcc44;
    private static final double MIN_ZOOM = 0.0001, MAX_ZOOM = 3;
    private final Page page;
    private final MaterialProcessOverview overview;
    private double zoom, panX, panY;

    public MaterialProcessPanel(Page page) {
        this.page = page;
        overview = new MaterialProcessOverview(page);
        fit();
    }

    public void draw(GuiGraphics graphics, double mouseX, double mouseY) {
        graphics.fill(0, 0, WIDTH, HEIGHT, 0xffc6c6c6);
        text(graphics, page.title(), 4, 2, WIDTH - 122, TEXT);
        text(graphics, Component.literal("−"), WIDTH - 114, 2, 12, TEXT);
        text(
                graphics,
                Component.literal(java.math.BigDecimal.valueOf(zoom * 100)
                                .setScale(zoom < 0.01 ? 2 : 0, java.math.RoundingMode.HALF_UP)
                                .stripTrailingZeros()
                                .toPlainString()
                        + "%"),
                WIDTH - 98,
                2,
                33,
                MUTED);
        text(graphics, Component.literal("+"), WIDTH - 61, 2, 12, TEXT);
        text(graphics, Component.translatable("gt6ce.jei.material_process.reset"), WIDTH - 28, 2, 26, MUTED);
        graphics.flush();
        var viewport = PreviewViewport.screenBounds(graphics.pose().last().pose(), 0, BODY_TOP, WIDTH, FOOTER);
        graphics.enableScissor(viewport.left(), viewport.top(), viewport.right(), viewport.bottom());
        Node hovered = hit(mouseX, mouseY);
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
                drawEdges(graphics, hovered);
                for (var grid : overview.grids()) {
                    graphics.fill(grid.left(), grid.top(), grid.right(), grid.bottom(), 0xff777777);
                    for (int y = grid.top(); y < grid.bottom(); y += 24)
                        for (int x = grid.left(); x < grid.right(); x += 24)
                            graphics.fill(x + 1, y + 1, x + 23, y + 23, 0xffb0b0b0);
                }
                for (Node node : overview.nodes()) {
                    if (!visible(node)) continue;
                    if (related(node, hovered))
                        graphics.fill(node.x - 10, node.y - 10, node.x + 11, node.y + 11, 0xffe5e5e5);
                    ItemStack icon = node.machine()
                            ? selected(node).icon()
                            : node.port().icons().stream().findFirst().orElse(ItemStack.EMPTY);
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
        if (overview.nodes().isEmpty())
            graphics.drawWordWrap(
                    Minecraft.getInstance().font,
                    Component.translatable("gt6ce.jei.material_process.empty"),
                    12,
                    BODY_TOP + 28,
                    WIDTH - 24,
                    MUTED);
        text(graphics, Component.literal("i"), 4, FOOTER + 3, 12, MUTED);
    }

    private gregtech.compat.common.MaterialProcessDisplay.Step selected(Node node) {
        return node.steps.get(0);
    }

    private static boolean related(Node node, Node hovered) {
        return node == hovered
                || hovered != null
                        && !node.machine()
                        && !hovered.machine()
                        && node.port().identity().equals(hovered.port().identity());
    }

    private boolean visible(Node node) {
        return node.x + 12 >= worldX(0)
                && node.x - 12 <= worldX(WIDTH)
                && node.y + 12 >= worldY(BODY_TOP)
                && node.y - 14 <= worldY(FOOTER);
    }

    private Node hit(double mouseX, double mouseY) {
        if (mouseX < 0 || mouseX >= WIDTH || mouseY < BODY_TOP || mouseY >= FOOTER) return null;
        double x = worldX(mouseX), y = worldY(mouseY);
        for (Node node : overview.nodes())
            if (x >= node.x - 11 && x < node.x + 12 && y >= node.y - 12 && y < node.y + 12) return node;
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
                (int) Math.min(WIDTH, Math.ceil(panX + (node.x + 12) * zoom)),
                (int) Math.min(FOOTER, Math.ceil(BODY_TOP + panY + (node.y + 12) * zoom)));
    }

    public List<ItemStack> stacks(Node node) {
        if (!page.navigation().valid()) return List.of();
        if (node.machine())
            return selected(node).icon().isEmpty()
                    ? List.of()
                    : List.of(selected(node).icon());
        return node.port().hasIcons() ? List.of(node.port().icons().get(0)) : List.of();
    }

    public List<Component> tooltip(double mouseX, double mouseY) {
        if (mouseY < BODY_TOP && mouseX >= WIDTH - 118)
            return List.of(Component.translatable(
                    mouseX >= WIDTH - 29
                            ? "gt6ce.jei.material_process.reset_hint"
                            : "gt6ce.jei.material_process.zoom_hint"));
        if (mouseY >= FOOTER) {
            var lines = new ArrayList<>(page.notices());
            lines.add(Component.translatable("gt6ce.jei.material_process.pan"));
            return List.copyOf(lines);
        }
        Node node = hit(mouseX, mouseY);
        if (node == null) return List.of();
        ItemStack icon = node.machine()
                ? selected(node).icon()
                : node.port().icons().stream().findFirst().orElse(ItemStack.EMPTY);
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
            Component label = port.hasIcons() ? port.icons().get(0).getHoverName() : port.name();
            names.putIfAbsent(label.getString(), label);
        }
        lines.addAll(names.values().stream().limit(12).toList());
        if (names.size() > 12) lines.add(Component.literal("…"));
        return List.copyOf(lines);
    }

    public boolean click(double mouseX, double mouseY, int button) {
        if (clickControls(mouseX, mouseY, button)) return true;
        if (button != 0 && button != 1) return false;
        Node node = hit(mouseX, mouseY);
        if (node == null) return false;
        if (!node.machine()) return page.navigation().openPort(node.port(), button == 1);
        return page.navigation().openStack(selected(node).icon(), button == 1);
    }

    public boolean clickControls(double mouseX, double mouseY, int button) {
        if (button != 0 && button != 1) return false;
        if (button == 0 && mouseY >= 0 && mouseY < 14) {
            if (mouseX >= WIDTH - 29 && mouseX < WIDTH) {
                fit();
                return true;
            }
            if (mouseX >= WIDTH - 118 && mouseX < WIDTH - 101) {
                zoomAt(WIDTH / 2.0, (BODY_TOP + FOOTER) / 2.0, zoom / 1.25);
                return true;
            }
            if (mouseX >= WIDTH - 65 && mouseX < WIDTH - 46) {
                zoomAt(WIDTH / 2.0, (BODY_TOP + FOOTER) / 2.0, zoom * 1.25);
                return true;
            }
        }
        return false;
    }

    public boolean press(double mouseX, double mouseY, int button) {
        return (button == 0 || button == 1)
                && mouseX >= 0
                && mouseX < WIDTH
                && mouseY >= BODY_TOP
                && mouseY < FOOTER
                && hit(mouseX, mouseY) == null;
    }

    public boolean drag(int button, double dx, double dy) {
        if (button != 0 && button != 1) return false;
        panX += dx;
        panY += dy;
        return true;
    }

    public boolean scroll(double mouseX, double mouseY, double delta) {
        if (mouseX < 0 || mouseX >= WIDTH || mouseY < BODY_TOP || mouseY >= FOOTER || delta == 0) return false;
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

    private void fit() {
        zoom = Math.max(
                0.75,
                Math.min(
                        1,
                        Math.min(
                                (WIDTH - 12.0) / Math.max(1, overview.width()),
                                (FOOTER - BODY_TOP - 12.0) / Math.max(1, overview.height()))));
        if (overview.width() * zoom <= WIDTH && overview.height() * zoom <= FOOTER - BODY_TOP) {
            panX = (WIDTH - overview.width() * zoom) / 2;
            panY = (FOOTER - BODY_TOP - overview.height() * zoom) / 2;
        } else {
            Node anchor = overview.anchor();
            panX = WIDTH / 3.0 - (anchor == null ? 0 : anchor.x) * zoom;
            panY = (FOOTER - BODY_TOP) / 2.0 - (anchor == null ? 0 : anchor.y) * zoom;
        }
    }

    private void drawEdges(GuiGraphics graphics, Node hovered) {
        record Wire(List<MaterialProcessOverview.Point> path, boolean output) {}
        var wires = new java.util.LinkedHashMap<Wire, List<MaterialProcessOverview.Edge>>();
        for (var edge : overview.edges())
            wires.computeIfAbsent(new Wire(edge.path(), edge.endpoint().output()), ignored -> new ArrayList<>())
                    .add(edge);
        for (var entry : wires.entrySet()) {
            boolean highlighted = entry.getValue().stream()
                    .anyMatch(edge -> related(edge.from(), hovered) || related(edge.to(), hovered));
            boolean confirmed = entry.getValue().stream().allMatch(MaterialProcessOverview.Edge::confirmed);
            int color = highlighted ? 0xff986c26 : TEXT;
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
        if (!arrow || tx < worldX(0) || tx >= worldX(WIDTH) || ty < worldY(BODY_TOP) || ty >= worldY(FOOTER)) return;
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
                q = {x1 - worldX(-1), worldX(WIDTH + 1) - x1, y1 - worldY(BODY_TOP - 1), worldY(FOOTER + 1) - y1};
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
