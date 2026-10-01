package com.gregtech.gregtech.api.inventory;

import net.minecraft.core.Direction;
import java.util.List;

/** Pixel coordinates on the original GT6 front texture: x rightwards, y downwards. */
public final class MassStorageFace {
    public record Button(int left, int top, int amount) {
        public boolean contains(double x, double y) { return x >= left && x <= left + 2 && y >= top && y <= top + 2; }
    }
    public static final List<Button> BUTTONS = List.of(new Button(1,6,8), new Button(13,6,64),
            new Button(1,9,4), new Button(13,9,32), new Button(1,12,1), new Button(13,12,16));
    public static final double COUNT_X = 8, COUNT_Y = 2, ICON_X = 8, ICON_Y = 10;
    private MassStorageFace() {}
    public static double x(Direction front, double x, double z) {
        if (!front.getAxis().isHorizontal()) throw new IllegalArgumentException("Horizontal front required");
        return com.gregtech.gregtech.api.block.FaceCoordinates.pixels(front,x,0,z).x();
    }
    public static boolean active(double x, double y) { return x >= 1 && x <= 15 && y >= 1 && y <= 15; }
    public static int withdrawal(double x, double y) {
        for (Button button : BUTTONS) if (button.contains(x,y)) return button.amount();
        return 0;
    }
    public static boolean depositInventory(double x, double y) { return active(x,y) && x >= 4 && x <= 12 && y >= 6; }
}
