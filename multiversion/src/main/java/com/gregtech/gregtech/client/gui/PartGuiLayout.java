package com.gregtech.gregtech.client.gui;

import com.gregtech.gregtech.api.addon.LayoutPart;

/** Positions for one part menu. Tanks sit above the slots, and the player inventory stays at the bottom. */
public final class PartGuiLayout {
    public final int width = 176;
    public final int height;
    public final int[] slotX;
    public final int[] slotY;
    public final int[] tankX;
    public final int tankY;
    public final int playerY;
    public final int fieldY;

    private PartGuiLayout(LayoutPart part) {
        int tankCount = part.tanks() == null ? 0 : part.tanks().count();
        int slotCount = part.slots() == null ? 0 : part.slots().count();
        int slotRows = slotCount == 0 ? 0 : (slotCount + 8) / 9;
        tankY = 8;
        int cursor = 8;
        if (tankCount > 0) cursor += 58;
        int slotTop = cursor;
        cursor += slotRows * 18;
        fieldY = cursor;
        cursor += part.fields().size() * 10 + 4;
        playerY = Math.max(cursor, 84);
        height = Math.max(166, playerY + 82);
        slotX = new int[slotCount];
        slotY = new int[slotCount];
        for (int i = 0; i < slotCount; i++) {
            slotX[i] = 8 + (i % 9) * 18;
            slotY[i] = slotTop + (i / 9) * 18;
        }
        tankX = new int[tankCount];
        for (int i = 0; i < tankCount; i++) tankX[i] = 8 + i * 20;
    }

    public static PartGuiLayout of(LayoutPart part) { return new PartGuiLayout(part); }
}
