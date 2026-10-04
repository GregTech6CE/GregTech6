package com.gregtech.gregtech.client;

import com.gregtech.gregtech.content.loot.LootViewerData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import java.util.*;

/** Shared captions used by the actual JEI and EMI widgets. */
public final class LootPageCaptions {
    private LootPageCaptions() {}
    public static final int WIDTH = 176, HEIGHT = 112;
    public static List<Component> lines(LootViewerData.Row row) {
        var result = new ArrayList<Component>();
        result.add(Component.translatable("gregtech.loot.amount", row.min(), row.max()));
        result.add(row.chance() < 0 ? Component.translatable("gregtech.loot.conditional")
            : Component.translatable("gregtech.loot.chance", String.format(Locale.ROOT, "%.3f", row.chance() * 100)));
        result.addAll(row.notes());
        return List.copyOf(result);
    }
    public static void draw(GuiGraphics graphics, LootViewerData.Row row) {
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, font.plainSubstrByWidth(row.source().getString(), WIDTH - 4), 2, 2, 0xff404040, false);
        graphics.drawString(font, "→", 80, 24, 0xff404040, false);
        int y = 48;
        for (var line : lines(row)) {
            if (y > HEIGHT - 10) break;
            graphics.drawString(font, font.plainSubstrByWidth(line.getString(), WIDTH - 4), 2, y, 0xff404040, false);
            y += 10;
        }
    }
}
