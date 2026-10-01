package com.gregtech.gregtech.content.tool;



/** Original MultiTileEntityAnvil workpiece bounds, in block-local coordinates. */
public final class AnvilWorkpieceBounds {
    public record Bounds(double minX,double minY,double minZ,double maxX,double maxY,double maxZ){}
    private AnvilWorkpieceBounds() {}
    public static int shape(String prefix) {
        if (prefix.startsWith("ingot") || prefix.equals("nugget") || prefix.equals("billet")) return 1;
        if (prefix.startsWith("plate") || prefix.startsWith("plank")) return 2;
        if (prefix.startsWith("stick") || prefix.startsWith("wire")) return 3;
        if (prefix.startsWith("chunk")) return 4;
        if (prefix.startsWith("ring")) return 5;
        if (prefix.startsWith("gem")) return 6;
        if (prefix.startsWith("ore") || prefix.startsWith("rock") || prefix.startsWith("crushed")) return 7;
        return 0;
    }
    public static Bounds bounds(int shape, int slot, boolean axisZ) {
        if (slot < 0 || slot > 1) throw new IllegalArgumentException("Horizontal anvil slot required");
        int x0 = 1, x1 = 7, z0 = 5, z1 = 11, y0 = 12, y1 = 16;
        switch (shape) {
            case 1 -> { x0 = 3; x1 = 6; y1 = 15; }
            case 2 -> y1 = 13;
            case 3 -> { z0 = 7; z1 = 9; y1 = 14; }
            case 4 -> { x0 = 2; x1 = 6; z0 = 6; z1 = 10; y1 = 14; }
            case 5 -> { x0 = 2; x1 = 6; z0 = 6; z1 = 10; y1 = 13; }
            case 6 -> { x0 = 2; x1 = 6; z0 = 6; z1 = 10; }
            case 8 -> { x0 = 2; x1 = 6; }
            case 9 -> { x0 = 1; x1 = 15; z0 = 7; z1 = 9; y0 = 13; y1 = 15; }
            default -> { }
        }
        if (slot == 1) { int old = x0; x0 = 16 - x1; x1 = 16 - old; }
        return axisZ
                ? new Bounds(x0 / 16d, y0 / 16d, z0 / 16d, x1 / 16d, y1 / 16d, z1 / 16d)
                : new Bounds(z0 / 16d, y0 / 16d, x0 / 16d, z1 / 16d, y1 / 16d, x1 / 16d);
    }
}
