package com.gregtech.gregtech.content.transport;

/**
 * Simulate, refresh the destination, then account only validated actual delivery.
 * Adapted from masson's per-slot remainder and partial-commit contract. Minecraft
 * stack identity, native/Forge handlers and diagnostics stay in the adapters.
 */
public final class ItemPipeTransfer {
    private ItemPipeTransfer() {}

    public interface Port<S> {
        boolean begin(boolean simulate);
        int slots();
        S stack(int slot);
        int count(S stack);
        S copyWithCount(S stack, int count);
        boolean same(S first, S second);
        S insert(int slot, S offered, boolean simulate);
    }

    public record Result(int planned, int accepted, boolean handlerFailed) {}
    private record Offer(int accepted, boolean failed) {}

    public static <S> Result transfer(S offered, Port<S> port) {
        int count = port.count(offered);
        if (count <= 0) return new Result(0, 0, false);
        Offer planned = offer(offered, port, true);
        if (planned.failed() || planned.accepted() == 0) {
            return new Result(planned.accepted(), 0, planned.failed());
        }
        Offer actual = offer(port.copyWithCount(offered, planned.accepted()), port, false);
        return new Result(planned.accepted(), actual.accepted(), actual.failed());
    }

    private static <S> Offer offer(S template, Port<S> port, boolean simulate) {
        int committed = 0;
        try {
            if (!port.begin(simulate)) return new Offer(0, false);
            S remaining = port.copyWithCount(template, port.count(template));
            int slots = port.slots();
            // Preserve baseline merge-before-empty inventory ordering.
            boolean[] tried = new boolean[Math.max(0, slots)];
            for (int pass = 0; pass < 2; pass++) {
                for (int slot = 0; slot < slots && port.count(remaining) > 0; slot++) {
                    if (tried[slot]) continue;
                    if (pass == 0) {
                        S current = port.stack(slot);
                        if (port.count(current) == 0 || !port.same(current, remaining)) continue;
                    }
                    tried[slot] = true;
                    int offered = port.count(remaining);
                    S next = port.insert(slot, port.copyWithCount(remaining, offered), simulate);
                    if (next == null) return new Offer(committed, true);
                    int left = port.count(next);
                    if (left > 0 && !port.same(remaining, next)) return new Offer(committed, true);
                    committed += ItemTransferAmounts.accepted(offered, left);
                    remaining = next;
                }
            }
            return new Offer(committed, false);
        } catch (RuntimeException failure) {
            // Previously validated actual slot deliveries remain accounted; unconfirmed source stays.
            return new Offer(committed, true);
        }
    }
}
