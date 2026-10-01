package com.gregtech.gregtech.content.tool;

import com.google.gson.Gson;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** GT6 MultiTileEntityCoin's two 16x16 mint bit planes and world render bounds. */
public final class CoinGeometry {
    public static final String NBT_SHAPE = "gt.coin.shape.";
    private static final MintPattern DEFAULT_PATTERN = loadDefault();
    /** Identical custom dies share boxes across piles, without retaining arbitrary player NBT forever. */
    private static final Map<String, MintPattern> CUSTOM_PATTERNS = new LinkedHashMap<>(32, .75F, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, MintPattern> eldest) {
            return size() > 32;
        }
    };

    private static MintPattern loadDefault() {
        try (var input = CoinGeometry.class.getResourceAsStream("/assets/gregtech/geometry/coin_shape.json")) {
            if (input == null) throw new IllegalStateException("Missing GT6 coin shape");
            int[][] data = new Gson().fromJson(new InputStreamReader(input, StandardCharsets.UTF_8), int[][].class);
            if (data == null || data.length != 2 || data[0].length != 16 || data[1].length != 16)
                throw new IllegalStateException("Invalid GT6 coin shape");
            short[][] rows = new short[2][16];
            for (int plane = 0; plane < 2; plane++)
                for (int row = 0; row < 16; row++) rows[plane][row] = (short) data[plane][row];
            return new MintPattern(rows);
        } catch (java.io.IOException error) {
            throw new ExceptionInInitializerError(error);
        }
    }
    private CoinGeometry() {}

    public static MintPattern defaultPattern() { return DEFAULT_PATTERN; }

    /** True only when the stack carries at least one GT6 die row; other coin NBT keeps the default mint. */
    public static boolean hasCustomPattern(ItemStack coin) {
        CompoundTag tag = coin.getTag();
        if (tag == null) return false;
        for (int plane = 0; plane < 2; plane++)
            for (int row = 0; row < 16; row++)
                if (tag.contains(NBT_SHAPE + plane + "." + row, Tag.TAG_ANY_NUMERIC)) return true;
        return false;
    }

    /** An untagged registered coin uses GT6's default; a die-struck coin uses its NBT bit planes. */
    public static MintPattern pattern(ItemStack coin) {
        CompoundTag tag = coin.getTag();
        if (tag == null) return DEFAULT_PATTERN;
        boolean hasPattern = false;
        short[][] rows = new short[2][16];
        for (int plane = 0; plane < 2; plane++) {
            for (int row = 0; row < 16; row++) {
                String key = NBT_SHAPE + plane + "." + row;
                if (tag.contains(key, Tag.TAG_ANY_NUMERIC)) {
                    hasPattern = true;
                    rows[plane][row] = tag.getShort(key);
                }
            }
        }
        if (!hasPattern) return DEFAULT_PATTERN;
        StringBuilder bits = new StringBuilder(32);
        for (int plane = 0; plane < 2; plane++)
            for (int row = 0; row < 16; row++) bits.append((char) (rows[plane][row] & 0xFFFF));
        synchronized (CUSTOM_PATTERNS) {
            return CUSTOM_PATTERNS.computeIfAbsent(bits.toString(), ignored -> new MintPattern(rows));
        }
    }

    public static int depth(int x, int z) { return DEFAULT_PATTERN.pixelDepth(x, z); }
    public static List<AABB> pixels(int count) { return DEFAULT_PATTERN.pixelBoxes(count); }
    public static boolean exposedSide(int x, int z, Direction side) {
        return DEFAULT_PATTERN.isExposedSide(x, z, side);
    }

    /** Immutable die raster; box lists are built only for stack heights actually rendered. */
    public static final class MintPattern {
        private final short[][] rows = new short[2][16];
        @SuppressWarnings("unchecked")
        private final List<AABB>[] pixels = (List<AABB>[]) new List<?>[17];
        private List<AABB> inventoryPixels;

        private MintPattern(short[][] source) {
            for (int plane = 0; plane < 2; plane++)
                System.arraycopy(source[plane], 0, rows[plane], 0, 16);
            pixels[0] = List.of();
        }

        public int pixelDepth(int x, int z) {
            return ((rows[0][x] >>> z) & 1) + 2 * ((rows[1][x] >>> z) & 1);
        }

        public synchronized List<AABB> pixelBoxes(int count) {
            int height = Math.max(0, Math.min(16, count));
            if (pixels[height] != null) return pixels[height];
            var boxes = new ArrayList<AABB>();
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                int depth = pixelDepth(x, z);
                if (depth == 3) continue; // GT6 suppresses fully cut-away pixels.
                boxes.add(new AABB(x / 64.0, depth / 64.0, z / 64.0,
                        (x + 1) / 64.0, height / 16.0 - depth / 64.0, (z + 1) / 64.0));
            }
            return pixels[height] = List.copyOf(boxes);
        }

        /** Same 16x16 relief and bounds as coin_minted.json, for coins with a stack-specific die. */
        public synchronized List<AABB> inventoryPixelBoxes() {
            if (inventoryPixels != null) return inventoryPixels;
            var boxes = new ArrayList<AABB>();
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                int depth = pixelDepth(x, z);
                if (depth == 3) continue;
                boxes.add(new AABB(x / 16.0, 7 / 16.0, z / 16.0,
                        (x + 1) / 16.0, (9 - depth / 4.0) / 16.0, (z + 1) / 16.0));
            }
            return inventoryPixels = List.copyOf(boxes);
        }

        public boolean isExposedSide(int x, int z, Direction side) {
            int nextX = x + side.getStepX();
            int nextZ = z + side.getStepZ();
            if (nextX < 0 || nextX >= 16 || nextZ < 0 || nextZ >= 16) return true;
            return pixelDepth(x, z) < pixelDepth(nextX, nextZ);
        }
    }

    public static AABB cell(int face, int count) {
        double x = (face / 4) / 4.0, z = (face % 4) / 4.0;
        return new AABB(x, 0, z, x + .25, count / 16.0, z + .25);
    }

    /**
     * GT6's world renderer passes each pixel's bounds to RenderBlocks.renderFace*, which samples
     * the coin sprites at the pixel's coordinates in the entire block. The inventory coin occupies
     * the full 16x16 model and consequently samples the full sprite there. Arguments here are
     * block-relative coordinates, including the 4x4 cell offset.
     */
    public static float surfaceU(Direction side, float x, float z) {
        return switch (side) {
            case EAST -> 1.0F - z;
            case SOUTH -> 1.0F - x;
            case WEST -> z;
            default -> x;
        };
    }

    public static float surfaceV(Direction side, float y, float z) {
        return switch (side) {
            case UP -> z;
            case DOWN -> 1.0F - z;
            default -> 1.0F - y;
        };
    }

}
