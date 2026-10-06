package com.gregtech.gregtech.client;


import com.gregtech.gregtech.api.energy.FaceConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Shared tooltip formatting utilities for GT6 machines. */
public final class TooltipHelper {
    private TooltipHelper() {}

    private static final String MOD = "gregtech";

    // ── Direction names ────────────────────────────────────────────────────

    public static String dirName(int dir) {
        return switch (dir) {
            case FaceConfig.BOTTOM -> "Bottom";
            case FaceConfig.TOP    -> "Top";
            case FaceConfig.LEFT   -> "Left";
            case FaceConfig.RIGHT  -> "Right";
            case FaceConfig.FRONT  -> "Front";
            case FaceConfig.BACK   -> "Back";
            default -> "Unknown";
        };
    }

    /** Converts a direction bitmask to a human-readable string.
     *  All 6 directions = "Any Side", otherwise comma-separated. */
    public static String dirMaskToString(int mask) {
        if (mask == 0b111111) return "Any Side";
        List<String> names = new ArrayList<>(6);
        for (int d : new int[]{FaceConfig.BOTTOM, FaceConfig.TOP, FaceConfig.LEFT,
                FaceConfig.RIGHT, FaceConfig.FRONT, FaceConfig.BACK}) {
            if (FaceConfig.has(mask, d)) names.add(dirName(d));
        }
        return names.isEmpty() ? "None" : String.join(", ", names);
    }

    /** Returns "(auto)" if the auto-direction matches, "(no auto)" otherwise. */
    public static String autoLabel(int autoDir, int dir) {
        return FaceConfig.autoValid(autoDir) && autoDir == dir ? "(auto)" : "(no auto)";
    }

    // ── Energy formatting ──────────────────────────────────────────────────

    /** "32 HU/t (16 to 64, Bottom)" */
    public static Component energyInLine(long nominal, long min, long max, String unit, String dirLabel) {
        return Component.empty()
                .append(Component.translatable("tooltip." + MOD + ".machine.energy_in")
                        .withStyle(ChatFormatting.GREEN))
                .append(Component.literal(formatLong(nominal)).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" " + unit + "/t").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(" (" + formatLong(min) + " to " + formatLong(max) + ", " + dirLabel + ")")
                        .withStyle(ChatFormatting.WHITE));
    }

    /** "Energy OUT: 32 HU/t" */
    public static MutableComponent energyOutLine(long amount, String unit) {
        return Component.empty()
                .append(Component.translatable("tooltip." + MOD + ".machine.energy_out")
                        .withStyle(ChatFormatting.RED))
                .append(Component.literal(formatLong(amount) + " " + unit + "/t")
                        .withStyle(ChatFormatting.WHITE));
    }

    public static Component energyInNone() {
        return Component.translatable("tooltip." + MOD + ".machine.energy_in")
                .append(Component.translatable("tooltip." + MOD + ".machine.none")
                        .withStyle(ChatFormatting.WHITE))
                .withStyle(ChatFormatting.RED);
    }

    // ── I/O direction lines ────────────────────────────────────────────────

    /** "Items IN: Left (auto), Right (no auto)" */
    public static Component ioLine(String typeKey, int mask, int autoMask, ChatFormatting color) {
        if (mask == 0) return null;
        List<MutableComponent> parts = new ArrayList<>();
        for (int d : new int[]{FaceConfig.BOTTOM, FaceConfig.TOP, FaceConfig.LEFT,
                FaceConfig.RIGHT, FaceConfig.FRONT, FaceConfig.BACK}) {
            if (FaceConfig.has(mask, d)) {
                parts.add(Component.literal(dirName(d) + " " + autoLabel(autoMask, d)));
            }
        }
        MutableComponent list = null;
        for (int i = 0; i < parts.size(); i++) {
            if (i == 0) list = parts.get(i);
            else list = list.append(", ").append(parts.get(i));
        }
        return Component.empty()
                .append(Component.translatable("tooltip." + MOD + ".machine.io." + typeKey)
                        .withStyle(color))
                .append(list != null ? list.withStyle(ChatFormatting.WHITE) : Component.literal(""));
    }

    // ── Tool instructions ──────────────────────────────────────────────────

    public static void appendToolInstructions(List<Component> tooltip) {
        ChatFormatting g = ChatFormatting.GRAY;
        tooltip.add(Component.translatable("tooltip." + MOD + ".machine.tool.screwdriver").withStyle(g));
        tooltip.add(Component.translatable("tooltip." + MOD + ".machine.tool.monkey_wrench_in").withStyle(g));
        tooltip.add(Component.translatable("tooltip." + MOD + ".machine.tool.monkey_wrench_out").withStyle(g));
        tooltip.add(Component.translatable("tooltip." + MOD + ".machine.tool.soft_hammer").withStyle(g));
        tooltip.add(Component.translatable("tooltip." + MOD + ".machine.tool.magnifying_glass").withStyle(g));
        tooltip.add(Component.translatable("tooltip." + MOD + ".machine.tool.wrench_facing").withStyle(g));
    }

    // ── Blast resistance ────────────────────────────────────────────────

    public static void appendBlastResistance(float resistance, List<Component> tooltip) {
        if (!com.gregtech.gregtech.api.block.OriginalBlockTooltipRules.showMultitileBlast(resistance)) return;
        var rating = com.gregtech.gregtech.api.block.OriginalBlockTooltipRules.blastRating(resistance);
        ChatFormatting color = switch (rating) {
            case TERRIBLE, GHAST -> ChatFormatting.RED;
            case CREEPER -> ChatFormatting.YELLOW;
            case TNT, DYNAMITE -> ChatFormatting.GREEN;
            case IC2_NUKE_UNPROTECTED -> ChatFormatting.AQUA;
        };
        tooltip.add(Component.translatable("gt.lang.blastresistance").withStyle(ChatFormatting.WHITE)
                .append(Component.literal(com.gregtech.gregtech.api.block.OriginalBlockTooltipRules.blastNumber(resistance))
                        .withStyle(ChatFormatting.GOLD))
                .append(Component.literal(" "))
                .append(Component.translatable(rating.key()).withStyle(color)));
    }

    // ── Harvest tool ───────────────────────────────────────────────────

    public static void appendHarvestWrench(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip." + MOD + ".machine.harvest_wrench")
                .withStyle(ChatFormatting.WHITE));
    }

    // ── Number formatting ───────────────────────────────────────────────

    public static String formatLong(long value) {
        return String.format(Locale.US, "%,d", value);
    }

    public static String formatFloat(float value) {
        return String.format(Locale.US, "%.1f", value);
    }
}
