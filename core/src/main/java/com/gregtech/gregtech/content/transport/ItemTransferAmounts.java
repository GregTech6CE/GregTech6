package com.gregtech.gregtech.content.transport;

/** Valid-remainder accounting from masson's item delivery contract, shared by both adapters. */
public final class ItemTransferAmounts {
    private ItemTransferAmounts() {}

    public static int accepted(int offered, int remainder) {
        if (offered < 0 || remainder < 0 || remainder > offered) {
            throw new IllegalArgumentException("Item handler returned an invalid remainder count");
        }
        return offered - remainder;
    }
}
