/**
 * Copyright (c) 2025 GregTech-6 Team
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */

// Adapted vanilla/backported branches of Override_Drops.java.
package com.gregtech.gregtech.content.loot;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

/** Minecraft-independent rules for the original vanilla/backported entity branches. */
public final class MobDropRules {
    private MobDropRules() {}
    public enum Kind { OTHER, ZOMBIE, ZOMBIE_VILLAGER, PIG_ZOMBIE, SPIDER, SKELETON, WITCH,
        HOGLIN, ZOGLIN, STRIDER, WOLF, HORSE, DONKEY, MULE, ZOMBIE_HORSE, SKELETON_HORSE,
        SHEEP, COW, MOOSHROOM, PIG, CHICKEN, RABBIT }
    public record Context(Kind kind, int looting, boolean babyAnimal, boolean burning,
                          boolean playerKill, double jump, double health) {
        public Context { looting = Math.max(0, looting); }
    }
    public record Drop(String spec, int count, String name) {
        public Drop(String spec, int count) { this(spec, count, ""); }
    }
    public record Batch(int rotation, List<Drop> additions, boolean replaceIron) {}
    public record Fixed(Kind kind, String spec, int denominator, boolean playerOnly, boolean beforeRock) {}
    public record Part(Kind kind, String item, int attempts, int denominator) {}
    public static final List<Fixed> FIXED = List.of(
        new Fixed(Kind.ZOMBIE, "tech:bagged_sapling:1", 50, true, true),
        new Fixed(Kind.ZOMBIE, "tech:seed_pouch:1", 50, true, true),
        new Fixed(Kind.ZOMBIE, "v:stick:1", 5, true, false),
        new Fixed(Kind.ZOMBIE, "tech:mud_2:1", 10, true, false),
        new Fixed(Kind.ZOMBIE, "tech:match:1", 20, true, false),
        new Fixed(Kind.PIG_ZOMBIE, "i:stick:Crimson:1", 2, false, true),
        new Fixed(Kind.PIG_ZOMBIE, "v:bone:1", 5, true, false),
        new Fixed(Kind.PIG_ZOMBIE, "i:rockGt:Gold:1", 10, true, false),
        new Fixed(Kind.PIG_ZOMBIE, "tech:match:1", 20, true, false));
    public static final List<Part> PARTS = List.of(
        new Part(Kind.COW, "cow_hoof", 4, 100), new Part(Kind.COW, "cow_horn", 2, 100),
        new Part(Kind.MOOSHROOM, "cow_hoof", 4, 100), new Part(Kind.MOOSHROOM, "cow_horn", 2, 100),
        new Part(Kind.HOGLIN, "hoglin_tusk", 2, 100), new Part(Kind.ZOGLIN, "hoglin_tusk", 2, 200),
        new Part(Kind.HORSE, "horse_hoof", 4, 100), new Part(Kind.DONKEY, "donkey_hoof", 4, 100),
        new Part(Kind.MULE, "mule_hoof", 4, 100), new Part(Kind.ZOMBIE_HORSE, "horse_hoof", 4, 200),
        new Part(Kind.SKELETON_HORSE, "horse_hoof", 4, 200));
    public static final List<String> RAISINS = List.of("pomeraisins", "green_raisins", "purple_raisins", "white_raisins", "red_raisins");
    public static final List<String> COOKIES = List.of("cookie", "raisin_cookie");
    public static final List<String> SKELETON_RARE = List.of("tech:milk:1", "tech:milk:1", "tech:milk:1",
        "i:arrowGtWood:Bronze:1", "i:arrowGtWood:Bronze:1", "i:arrowGtWood:Bronze:1", "i:arrowGtWood:DamascusSteel:1");
    public static int rareBound(int looting) { return (int)Math.max(36L, 144L - 3L * looting); }
    public static int partChance(int looting, int denominator) {
        return (int)Math.min(denominator, 26L + 5L * Math.max(0, looting));
    }
    public static boolean horse(Kind kind) {
        return kind == Kind.HORSE || kind == Kind.DONKEY || kind == Kind.MULE
                || kind == Kind.ZOMBIE_HORSE || kind == Kind.SKELETON_HORSE;
    }
    public static Batch additions(Context context, IntUnaryOperator random) {
        int rotation = random.applyAsInt(rareBound(context.looting));
        var drops = new ArrayList<Drop>();
        Kind kind = context.kind;
        boolean replaceIron = kind != Kind.OTHER && kind != Kind.RABBIT;
        if (context.babyAnimal) return new Batch(rotation, List.of(), true);
        int loot = context.looting;
        switch (kind) {
            case PIG_ZOMBIE -> {
                fixed(drops, context, random, kind, true);
                if (context.playerKill) {
                    if (random.applyAsInt(2) == 0) add(drops, random.applyAsInt(2) == 0 ? "i:rockGt:Netherrack:1" : "v:flint:1", 1);
                    fixed(drops, context, random, kind, false);
                }
            }
            case ZOMBIE, ZOMBIE_VILLAGER -> {
                if (context.playerKill) {
                    fixed(drops, context, random, Kind.ZOMBIE, true);
                    if (random.applyAsInt(2) == 0) add(drops, random.applyAsInt(2) == 0 ? "i:rockGt:Stone:1" : "v:flint:1", 1);
                    fixed(drops, context, random, Kind.ZOMBIE, false);
                    if (rotation == 0) {
                        add(drops, "tech:" + RAISINS.get(random.applyAsInt(RAISINS.size())) + ":1", 1);
                    }
                    if (kind == Kind.ZOMBIE_VILLAGER) {
                        add(drops, "tech:dusty_guide_book:1", 1 + random.applyAsInt(3));
                        add(drops, "tech:dusty_material_dictionary:1", 1 + random.applyAsInt(3));
                    }
                }
            }
            case SPIDER -> {
                if (!context.playerKill) chance(drops, random, 4, "v:spider_eye:1");
                if (context.playerKill && rotation == 0) {
                    String cookie = COOKIES.get(random.applyAsInt(COOKIES.size()));
                    drops.add(new Drop("tech:" + cookie + ":1", 1, "Spider Cookie"));
                }
            }
            case SKELETON -> {
                if (context.playerKill && rotation == 0) {
                    // UT.Code.select(default, choices...) chooses only the choices array.
                    add(drops, SKELETON_RARE.get(random.applyAsInt(SKELETON_RARE.size())), 1);
                }
            }
            case WITCH -> {
                if (context.playerKill || rotation == 0)
                    add(drops, "tech:loot_bottle:1", 1 + random.applyAsInt(loot + 1));
            }
            case HOGLIN, ZOGLIN -> parts(drops, random, context);
            case WOLF -> {
                int count = random.applyAsInt(3) + (loot > 0 ? random.applyAsInt(loot + 1) : 0);
                meat(drops, context, "dogmeat", "grilled_dogmeat", count);
            }
            case HORSE, DONKEY, MULE, ZOMBIE_HORSE, SKELETON_HORSE -> {
                int count = 1 + random.applyAsInt(3) + (loot > 0 ? random.applyAsInt(loot + 1) : 0);
                if (random.applyAsInt(Math.max(1, 10 - (int)(context.jump * 10))) == 0)
                    count += 1 + random.applyAsInt(loot + 1) / 2;
                if (random.applyAsInt(Math.max(1, 30 - (int)context.health)) == 0)
                    count += 1 + random.applyAsInt(loot + 1) / 2;
                if (kind == Kind.ZOMBIE_HORSE || kind == Kind.SKELETON_HORSE)
                    add(drops, kind == Kind.ZOMBIE_HORSE ? "v:rotten_flesh:1" : "v:bone:1", count);
                else {
                    String animal = kind == Kind.DONKEY ? "donkey" : kind == Kind.MULE ? "mule" : "horse";
                    meat(drops, context, animal + "_meat", "grilled_" + animal + "_meat", count);
                }
                parts(drops, random, context);
            }
            case COW, MOOSHROOM -> {
                parts(drops, random, context);
            }
            case RABBIT -> {
                if (random.applyAsInt(100) <= 10 + loot) add(drops, "v:carrot:1", 1);
            }
            // Modern Minecraft already supplies the original optional sheep-meat backport.
            default -> {}
        }
        return new Batch(rotation, List.copyOf(drops), replaceIron);
    }
    private static void meat(List<Drop> drops, Context context, String raw, String cooked, int count) {
        // Original creates separate entities; replacement rotation and scrap chance are per entity.
        for (int i = 0; i < count; i++) add(drops, "tech:" + (context.burning ? cooked : raw) + ":1", 1);
    }
    private static void chance(List<Drop> drops, IntUnaryOperator random, int bound, String spec) {
        if (random.applyAsInt(bound) == 0) add(drops, spec, 1);
    }
    private static void fixed(List<Drop> drops, Context context, IntUnaryOperator random, Kind kind, boolean beforeRock) {
        for (var row : FIXED) if (row.kind == kind && row.beforeRock == beforeRock && (!row.playerOnly || context.playerKill))
            chance(drops, random, row.denominator, row.spec);
    }
    private static void parts(List<Drop> drops, IntUnaryOperator random, Context context) {
        for (var row : PARTS) if (row.kind == context.kind)
            for (int i = 0; i < row.attempts; i++) if (random.applyAsInt(row.denominator) < partChance(context.looting, row.denominator))
                add(drops, "tech:" + row.item + ":1", 1);
    }
    private static void add(List<Drop> drops, String spec, int count) { if (count > 0) drops.add(new Drop(spec, count)); }
    public static boolean headlessArrow(int looting, IntUnaryOperator random) {
        return random.applyAsInt(Math.max(4, looting * 2 + 4)) < 3;
    }
    public enum Meat { NONE, PORK, BEEF, HORSE }
    /** Null preserves the original stack, including components. Counts are per original entity. */
    public static Drop meatReplacement(Meat family, int count, boolean cooked, int rotation, IntUnaryOperator random) {
        String id = switch (family) {
            case PORK -> switch (rotation % 3) { case 0 -> cooked ? "cooked_ham" : "raw_ham";
                case 1 -> cooked ? "grilled_bacon" : "raw_bacon"; default -> ""; };
            case BEEF -> switch (rotation % 3) { case 0 -> cooked ? "grilled_ribs" : "raw_ribs";
                case 1 -> cooked ? "grilled_rib_eye_steak" : "raw_rib_eye_steak"; default -> ""; };
            case HORSE -> rotation % 2 == 0 ? (cooked ? "grilled_ribs" : "raw_ribs") : "";
            default -> "";
        };
        if (id.isEmpty()) return null;
        if (family == Meat.PORK && rotation % 3 == 1) count = (int)Math.min(64L, (long)count * (3 + random.applyAsInt(3)));
        return new Drop("tech:" + id + ":1", count);
    }
}
