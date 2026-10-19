package com.gregtech.gregtech.content.logistics;

/** Logistics template policy; single-item cover filters have separate matching rules. */
public final class FilterPolicy {
    private FilterPolicy() {}

    public static final int TEMPLATES = 54;

    public static boolean validSlot(int slot) {
        return slot >= 0 && slot < TEMPLATES;
    }

    public static boolean allows(boolean blacklist, boolean found) {
        return blacklist != found;
    }

    /** sameMetadata is the native stack tag/component comparison, not just item damage. */
    public static boolean matches(boolean present, boolean sameItem, boolean untagged, boolean sameMetadata) {
        return present && sameItem && (untagged || sameMetadata);
    }
}
