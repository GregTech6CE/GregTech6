package com.gregtech.gregtech.jei;

import com.mojang.blaze3d.vertex.PoseStack;

public final class PreviewTransforms {
    private PreviewTransforms() {}

    public static void scaleForGui(PoseStack pose, float scale) {
        // PoseStack.scale normalizes using fastInvCubeRoot(1 / determinant).
        // A single negative axis makes that approximation invalid in 1.20.1.
        // Uniform size does not affect unit normals; only reflect their Y axis.
        pose.last().pose().scale(scale, -scale, scale);
        pose.last().normal().scale(1, -1, 1);
    }
}
