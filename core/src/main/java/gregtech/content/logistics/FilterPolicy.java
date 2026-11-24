package gregtech.content.logistics;

/** Slot bounds and whitelist/blacklist policy for logistics filter blocks. */
public final class FilterPolicy {
    private FilterPolicy() {}

    public static final int TEMPLATES = 54;

    public static boolean validSlot(int slot) {
        return slot >= 0 && slot < TEMPLATES;
    }

    public static boolean allows(boolean blacklist, boolean found) {
        return blacklist != found;
    }
}
