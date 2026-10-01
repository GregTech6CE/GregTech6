package com.gregtech.gregtech.content.tool;

import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.tool.*;
import com.gregtech.gregtech.item.GTToolItem;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import java.util.List;

/** Visual identity must not depend on whether the stored tool still has durability. */
public final class AnvilHammerDisplay {
    public record Part(AABB bounds, GTMaterial material) {}
    private AnvilHammerDisplay() {}
    public static boolean isHammer(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof GTToolItem tool && tool.toolType() == GTToolType.HARD_HAMMER;
    }
    public static List<Part> parts(ItemStack stack, int slot, Direction facing) {
        if (!isHammer(stack)) return List.of();
        return List.of(new Part(AnvilWorkpieceGeometry.bounds(8,slot,facing), fallback(GTToolHelper.getHead(stack), "Steel")),
                new Part(AnvilWorkpieceGeometry.bounds(9,1-slot,facing), fallback(GTToolHelper.getHandle(stack), "Wood")));
    }
    private static GTMaterial fallback(GTMaterial material, String name) {
        return material == null || material == GTMaterialRegistry.get("NULL") ? GTMaterialRegistry.get(name) : material;
    }
}
