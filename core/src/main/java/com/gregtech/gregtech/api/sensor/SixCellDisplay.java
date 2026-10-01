package com.gregtech.gregtech.api.sensor;

/** Six fixed two-pixel cells at texture coordinates (2..14, 2..4), as in GT6. */
public final class SixCellDisplay {
    public static final float TILE_UV = 1f/8f;
    public record Glyph(String texture, int rgb) {}
    private SixCellDisplay() {}
    private static Glyph glyph(String name, int color) { return name == null ? null : new Glyph(name,color); }
    public static Glyph[] storage(long count, long capacity) {
        return storage(count, capacity, 0xFFFFFF);
    }
    /** The logistics casing uses cyan numerals; a full store stays red on either casing. */
    public static Glyph[] storage(long count, long capacity, int normalColor) {
        Glyph[] cells = new Glyph[6];
        java.util.Arrays.fill(cells, glyph("0", normalColor));
        if (count <= 0) return cells;
        if (count >= capacity) {
            String[] full = {null,"1","0","0","percent",null};
            for (int i=0;i<6;i++) cells[i]=glyph(full[i],0xFF0000);
        } else {
            String digits = Long.toString(count);
            int start = Math.max(0,6-digits.length());
            for (int i=start;i<6;i++) cells[i]=glyph(String.valueOf(digits.charAt(digits.length()-6+i)),normalColor);
        }
        return cells;
    }
    public static Glyph[] sensor(long displayed, SensorControl.Mode mode, boolean hex, String unit, int unitColor) {
        Glyph[] cells = new Glyph[6];
        int value = (int)displayed & 65535;
        boolean full = mode == SensorControl.Mode.FULL || mode == SensorControl.Mode.NOT_FULL;
        String head = switch (mode) {
            case GREATER -> "greater"; case EQUAL, FULL -> "equal"; case SMALLER, NOT_FULL -> "smaller";
            case SCALE -> "scale"; default -> hex ? "hex" : Integer.toString(value/10000%10);
        };
        cells[0]=glyph(head,full?0xC00000:0xFFFFFF);
        for (int i=1;i<5;i++) {
            String name;
            if (full) name = new String[]{"1","0","0","percent"}[i-1];
            else if (hex) name = "0x" + Integer.toHexString((value >> ((4-i)*4)) & 15);
            else name = Integer.toString(value / (int)Math.pow(10,4-i) % 10);
            cells[i]=glyph(name,full?0xC00000:0xFFFFFF);
        }
        cells[5]=glyph(mode==SensorControl.Mode.PERCENT?"percent":unit,mode==SensorControl.Mode.PERCENT?0xFFFFFF:unitColor);
        return cells;
    }
}
