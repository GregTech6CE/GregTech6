package com.gregtech.gregtech.core;

import com.gregtech.gregtech.api.addon.GregTechAddon;
import com.gregtech.gregtech.api.addon.GregTechAddons;

/** Addon recipe dispatch keeps Error unwrapped and leaves FAILED without rolling earlier addons back. */
public final class AddonLifecycleContracts {
    private static int assertions;

    private AddonLifecycleContracts() {}

    public static void main(String[] args) {
        if (args.length != 1) throw new IllegalArgumentException("Expected one scenario");
        switch (args[0]) {
            case "runtime" -> runtime();
            case "fatal" -> fatal();
            case "checked" -> checked();
            default -> throw new IllegalArgumentException("Unknown scenario: " + args[0]);
        }
        System.out.println(args[0] + " passed " + assertions);
    }

    private static void runtime() {
        var ran = new boolean[1];
        GregTechAddons.register(addon("aa_ok", () -> ran[0] = true));
        GregTechAddons.register(addon("zz_bad", () -> { throw new IllegalArgumentException("boom"); }));
        var context = new GregTechAddon.Context("forge", "1.20.1");
        try {
            GregTechAddons.dispatchRecipesReady(context);
            check(false, "runtime failure must stop dispatch");
        } catch (IllegalStateException failure) {
            check(failure.getMessage() != null && failure.getMessage().contains("zz_bad"),
                    "wrapped message names the addon");
            check(failure.getCause() instanceof IllegalArgumentException, "runtime stays as the cause");
            check(GregTechAddons.phase() == GregTechAddons.Phase.FAILED, "FAILED after a runtime failure");
            check(ran[0], "an earlier addon is not rolled back");
        }
    }

    private static void fatal() {
        Error boom = new Error("linkage-like");
        GregTechAddons.register(addon("aa_fatal", () -> { throw boom; }));
        var context = new GregTechAddon.Context("forge", "1.20.1");
        try {
            GregTechAddons.dispatchRecipesReady(context);
            check(false, "Error must stop dispatch");
        } catch (IllegalStateException wrapped) {
            check(false, "Error must not be wrapped: " + wrapped);
        } catch (Error failure) {
            check(failure == boom, "the same Error is rethrown");
            check(GregTechAddons.phase() == GregTechAddons.Phase.FAILED, "FAILED after an Error");
        }
    }

    private static void checked() {
        java.io.IOException boom = new java.io.IOException("checked-like");
        GregTechAddons.register(addon("zz_checked", () -> sneakyThrow(boom)));
        var context = new GregTechAddon.Context("forge", "1.20.1");
        try {
            GregTechAddons.dispatchRecipesReady(context);
            check(false, "checked failure must stop dispatch");
        } catch (IllegalStateException wrapped) {
            check(wrapped.getMessage() != null && wrapped.getMessage().contains("zz_checked"),
                    "wrapped message names the addon");
            check(wrapped.getCause() == boom, "checked failure stays as the cause");
            check(GregTechAddons.phase() == GregTechAddons.Phase.FAILED, "FAILED after a checked failure");
        }
    }

    private static void sneakyThrow(Throwable failure) {
        AddonLifecycleContracts.<RuntimeException>sneakyThrow0(failure);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow0(Throwable failure) throws T {
        throw (T) failure;
    }

    private static GregTechAddon addon(String id, Runnable body) {
        return new GregTechAddon() {
            @Override public String id() { return id; }
            @Override public void onRecipesReady(Context context) { body.run(); }
        };
    }

    private static void check(boolean passed, String message) {
        assertions++;
        if (!passed) throw new AssertionError(message);
    }
}
