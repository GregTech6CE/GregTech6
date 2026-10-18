package com.gregtech.gregtech.client;
import com.gregtech.gregtech.content.loot.LootViewerData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import java.util.*;
/** Both browsers show the same source header and per-output rule metadata. */
public final class LootPageCaptions {
    private LootPageCaptions() {}
    public static final int WIDTH = 176, HEIGHT = 144, COLUMNS = 9, VISIBLE_ROWS = 5, GRID_X = 2, GRID_Y = 40;
    public static List<Component> lines(LootViewerData.Row row) {
        var result = new ArrayList<Component>();
        result.add(Component.translatable("gregtech.loot.amount", row.min(), row.max()));
        result.add(row.chance() < 0 ? Component.translatable("gregtech.loot.conditional")
            : Component.translatable("gregtech.loot.chance", String.format(Locale.ROOT, "%.3f", row.chance() * 100)));
        result.addAll(row.notes());
        return List.copyOf(result);
    }
    public static void draw(GuiGraphics graphics, LootViewerData.Group group) {draw(graphics,group,HEIGHT);}
    public static void draw(GuiGraphics graphics, LootViewerData.Group group,int height) {
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, font.plainSubstrByWidth(group.source().getString(), WIDTH - 4), 2, 2, 0xff404040, false);
        graphics.drawString(font, Component.translatable("gregtech.loot.entries", group.rows().size()), 24, 22, 0xff404040, false);
        graphics.drawString(font, Component.translatable("gregtech.loot.grid_help"), 2, height-10, 0xff555555, false);
    }
}
