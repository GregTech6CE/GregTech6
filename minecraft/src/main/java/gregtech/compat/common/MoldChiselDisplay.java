package gregtech.compat.common;

import gregtech.content.item.GTToolItem;
import gregtech.content.recipe.MoldChiselPatterns;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.tool.GTToolType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Shared data and pixel coordinates for native JEI and EMI mold-chiseling pages. */
public final class MoldChiselDisplay {
    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("gt6ce", "textures/gui/machines/mold_chisel.png");
    public static final int WIDTH = 176, HEIGHT = 128;
    public static final int GRID_X = 16, GRID_Y = 16, CELL = 16;
    public static final int CHISEL_X = 112, CHISEL_Y = 30, OUTPUT_X = 137, OUTPUT_Y = 49;

    public record Info(MoldChiselPatterns.Pattern pattern, List<ItemStack> outputs) {
        public String id() {
            return pattern.id().toLowerCase(Locale.ROOT);
        }
    }

    private MoldChiselDisplay() {}

    public static ItemStack chisel() {
        return GTToolItem.create(GTToolType.CHISEL, GTMaterialRegistry.get("Iron"), GTMaterialRegistry.get("Wood"));
    }

    public static List<ItemStack> molds() {
        return BuiltInRegistries.BLOCK.stream()
                .filter(block -> block instanceof gregtech.content.block.machine.MoldBlock)
                .map(ItemStack::new)
                .filter(stack -> !stack.isEmpty())
                .toList();
    }

    public static List<Info> recipes() {
        var rows = new ArrayList<Info>();
        for (var pattern : MoldChiselPatterns.all()) {
            List<ItemStack> outputs;
            if (pattern.recipe().blockSolid()) {
                outputs = GTMaterialRegistry.allMaterials().stream()
                        .map(material -> gregtech.content.registry.GTBlocks.getStack(
                                gregtech.engine.prefix.BlockMaterialPrefix.blockSolid, material))
                        .filter(stack -> !stack.isEmpty())
                        .toList();
            } else {
                outputs = GTItems.creativeEntries(pattern.recipe().itemPrefix()).stream()
                        .sorted(Comparator.comparing(
                                entry -> !entry.material().getName().equals("Iron")))
                        .map(entry -> new ItemStack(entry.item().get()))
                        .filter(stack -> !stack.isEmpty())
                        .toList();
            }
            if (!outputs.isEmpty()) rows.add(new Info(pattern, outputs));
        }
        return List.copyOf(rows);
    }

    public static void draw(GuiGraphics graphics, Info info) {
        gregtech.client.gui.GuiTemplates.background(graphics, TEXTURE, 0, 0, WIDTH, HEIGHT);
        for (int row = 0; row < 5; row++)
            for (int col = 0; col < 5; col++) {
                if (!info.pattern().carved(col, row)) continue;
                int x = GRID_X + col * CELL, y = GRID_Y + row * CELL;
                gregtech.client.gui.GuiTemplates.sprite("special/moldchisel_dark_panel")
                        .region(graphics, x, y, x - 8, y - 8, CELL, CELL);
            }
        // The supplied texture has a tool stencil; the actual item occupies this same native slot.
        graphics.fill(CHISEL_X, CHISEL_Y, CHISEL_X + 16, CHISEL_Y + 16, 0xff8b8b8b);
        graphics.drawString(
                Minecraft.getInstance().font,
                Component.translatable("gt6ce.jei.mold_chisel.note"),
                8,
                111,
                0xff404040,
                false);
    }

    public static List<Component> tooltip(Info info, double mouseX, double mouseY) {
        if (mouseX < GRID_X || mouseY < GRID_Y || mouseX >= GRID_X + CELL * 5 || mouseY >= GRID_Y + CELL * 5)
            return List.of();
        int col = (int) (mouseX - GRID_X) / CELL, row = (int) (mouseY - GRID_Y) / CELL;
        return List.of(
                Component.translatable(
                        info.pattern().carved(col, row)
                                ? "gt6ce.jei.mold_chisel.carved"
                                : "gt6ce.jei.mold_chisel.retained"),
                Component.translatable("gt6ce.jei.mold_chisel.instructions"));
    }
}
