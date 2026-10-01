package com.gregtech.gregtech.util;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.MaterialStackItemHelper;
import com.gregtech.gregtech.data.MaterialPrefix;
import net.minecraft.world.item.ItemStack;

import static com.gregtech.gregtech.api.material.GTValues.*;

/**
 * 1.20.1 port of {@code gregapi.util.OM} ("OredictManager") item-lookup helpers.
 *
 * <p>Given a material and an amount of material units ({@code U} = one ingot/dust worth),
 * these pick the best matching prefix item (dust/dustSmall/dustTiny/dustDiv72,
 * ingot/chunkGt/nugget, gem/gemFlawed/gemChipped) using the original GT6 selection logic.
 * Unlike the original (which returned {@code null}), unresolvable lookups return
 * {@link ItemStack#EMPTY}; recipe helpers tolerate and skip empty stacks.</p>
 */
public final class OM {
    private OM() {}

    /** GT6 {@code UT.Code.bindStack}: clamps a computed stack size into [1, 64]. */
    private static int bindStack(long aValue) {
        return (int) Math.max(1, Math.min(64, aValue));
    }

    private static ItemStack mat(MaterialPrefix aPrefix, GTMaterial aMaterial, long aStackSize) {
        return MaterialStackItemHelper.mat(aPrefix, aMaterial, bindStack(aStackSize));
    }

    // ── Dusts ─────────────────────────────────────────────────────────────

    /** One unit (U) of dust. */
    public static ItemStack dust(GTMaterial aMaterial) {
        return aMaterial == null ? ItemStack.EMPTY : dust(aMaterial, U);
    }

    /** Amount of dust measured with the given prefix's material weight. */
    public static ItemStack dust(GTMaterial aMaterial, MaterialPrefix aPrefix) {
        return aMaterial == null || aPrefix == null ? ItemStack.EMPTY : dust(aMaterial, aPrefix.getMaterialWeight());
    }

    /**
     * Chooses the best dust/dustSmall/dustTiny/dustDiv72 stack for the given U amount.
     * Faithful port of GT6 {@code OM.dust(OreDictMaterial, long)} (block tier omitted —
     * the port has no blockDust prefix).
     */
    public static ItemStack dust(GTMaterial aMaterial, long aMaterialAmount) {
        if (aMaterial == null || aMaterialAmount < U72) return ItemStack.EMPTY;
        if (aMaterialAmount >= U && (aMaterialAmount >= U * 16 || aMaterialAmount % U == 0)) {
            ItemStack rStack = mat(MaterialPrefix.dust, aMaterial, aMaterialAmount / U);
            if (!rStack.isEmpty()) return rStack;
        }
        if (aMaterialAmount >= U4 && (aMaterialAmount >= U * 8 || aMaterialAmount % U4 <= aMaterialAmount % U9)) {
            ItemStack rStack = mat(MaterialPrefix.dustSmall, aMaterial, (aMaterialAmount * 4) / U);
            if (!rStack.isEmpty()) return rStack;
        }
        if (aMaterialAmount >= U9 && (aMaterialAmount >= U || aMaterialAmount % U9 <= aMaterialAmount % U72)) {
            ItemStack rStack = mat(MaterialPrefix.dustTiny, aMaterial, (aMaterialAmount * 9) / U);
            if (!rStack.isEmpty()) return rStack;
        }
        return mat(MaterialPrefix.dustDiv72, aMaterial, (aMaterialAmount * 72) / U);
    }

    // ── Ingots ────────────────────────────────────────────────────────────

    /** One unit (U) of ingot. */
    public static ItemStack ingot(GTMaterial aMaterial) {
        return aMaterial == null ? ItemStack.EMPTY : ingot(aMaterial, U);
    }

    /** Amount of ingot measured with the given prefix's material weight. */
    public static ItemStack ingot(GTMaterial aMaterial, MaterialPrefix aPrefix) {
        return aMaterial == null || aPrefix == null ? ItemStack.EMPTY : ingot(aMaterial, aPrefix.getMaterialWeight());
    }

    /** Chooses the best ingot/chunkGt/nugget stack for the given U amount (block tier omitted). */
    public static ItemStack ingot(GTMaterial aMaterial, long aMaterialAmount) {
        if (aMaterial == null || aMaterialAmount < U9) return ItemStack.EMPTY;
        if (aMaterialAmount >= U && (aMaterialAmount >= U * 16 || aMaterialAmount % U == 0)) {
            ItemStack rStack = mat(MaterialPrefix.ingot, aMaterial, aMaterialAmount / U);
            if (!rStack.isEmpty()) return rStack;
        }
        if (aMaterialAmount >= U4 && (aMaterialAmount >= U * 8 || aMaterialAmount % U4 <= aMaterialAmount % U9)) {
            ItemStack rStack = mat(MaterialPrefix.chunkGt, aMaterial, (aMaterialAmount * 4) / U);
            if (!rStack.isEmpty()) return rStack;
        }
        return mat(MaterialPrefix.nugget, aMaterial, (aMaterialAmount * 9) / U);
    }

    // ── Gems ──────────────────────────────────────────────────────────────

    /** One unit (U) of gem. */
    public static ItemStack gem(GTMaterial aMaterial) {
        return aMaterial == null ? ItemStack.EMPTY : gem(aMaterial, U);
    }

    /** Amount of gem measured with the given prefix's material weight. */
    public static ItemStack gem(GTMaterial aMaterial, MaterialPrefix aPrefix) {
        return aMaterial == null || aPrefix == null ? ItemStack.EMPTY : gem(aMaterial, aPrefix.getMaterialWeight());
    }

    /** Chooses the best gem/gemFlawed/gemChipped stack for the given U amount (block tier omitted). */
    public static ItemStack gem(GTMaterial aMaterial, long aMaterialAmount) {
        if (aMaterial == null || aMaterialAmount < U4) return ItemStack.EMPTY;
        if (aMaterialAmount >= U && (aMaterialAmount >= U * 32 || aMaterialAmount % U <= aMaterialAmount % U2)) {
            ItemStack rStack = mat(MaterialPrefix.gem, aMaterial, aMaterialAmount / U);
            if (!rStack.isEmpty()) return rStack;
        }
        if (aMaterialAmount >= U2 && (aMaterialAmount >= U * 16 || aMaterialAmount % U2 <= aMaterialAmount % U4)) {
            ItemStack rStack = mat(MaterialPrefix.gemFlawed, aMaterial, (aMaterialAmount * 2) / U);
            if (!rStack.isEmpty()) return rStack;
        }
        return mat(MaterialPrefix.gemChipped, aMaterial, (aMaterialAmount * 4) / U);
    }

    // ── Combinations ──────────────────────────────────────────────────────

    /** Prefers dust; falls back to ingot. */
    public static ItemStack dustOrIngot(GTMaterial aMaterial, long aMaterialAmount) {
        if (aMaterial == null || aMaterialAmount <= 0) return ItemStack.EMPTY;
        ItemStack rStack = dust(aMaterial, aMaterialAmount);
        return rStack.isEmpty() ? ingot(aMaterial, aMaterialAmount) : rStack;
    }

    /** Prefers ingot; falls back to dust. */
    public static ItemStack ingotOrDust(GTMaterial aMaterial, long aMaterialAmount) {
        if (aMaterial == null || aMaterialAmount <= 0) return ItemStack.EMPTY;
        ItemStack rStack = ingot(aMaterial, aMaterialAmount);
        return rStack.isEmpty() ? dust(aMaterial, aMaterialAmount) : rStack;
    }
}
