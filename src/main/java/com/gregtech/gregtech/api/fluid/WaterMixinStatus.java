package com.gregtech.gregtech.api.fluid;

/**
 * Records that vanilla water actually ran through GregTech's Mixin. A missing development launch
 * argument otherwise leaves the game playable but restores a visible seam at GT water borders.
 */
public final class WaterMixinStatus {
    private static volatile boolean invoked;

    private WaterMixinStatus() {}

    public static void markInvoked() {
        invoked = true;
    }

    public static boolean wasInvoked() {
        return invoked;
    }
}
