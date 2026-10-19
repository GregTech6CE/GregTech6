package com.gregtech.gregtech.core;

import com.gregtech.gregtech.content.logistics.LongDistanceCatalog;
import com.gregtech.gregtech.content.logistics.LongDistanceLink;
import com.gregtech.gregtech.content.logistics.LongDistanceRules;

/** Pure core observations. Restored link ownership is not a native route-validity test. */
class LongDistanceAudit {
    public static void main(String[] args) {
        System.out.println("EXISTING_CORE_CONTRACTS|" + LongDistanceSourceContracts.verify());
        for (var spec : LongDistanceCatalog.ALL) {
            if (spec.kind().equals("TRANSFORMER") || spec.kind().equals("WIRE")) {
                System.out.println("DEVICE|" + spec.id() + "|" + spec.kind() + "|" + spec.material()
                        + "|" + spec.sourceMeta() + "|" + spec.voltage() + "|" + spec.stackLimit());
            }
        }
        for (long distance : new long[]{1, 511, 512, 519, 520, 4096, 5000}) {
            System.out.println("LOSS|" + distance + "|" + LongDistanceRules.loss(distance));
        }
        var source = new LongDistanceLink<Integer, String>(0, "source", () -> true);
        var receiver = new LongDistanceLink<Integer, String>(10, "receiver", () -> true);
        source.restore(10);
        System.out.println("RESTORE|known=" + source.known() + "|target=" + source.targetOwner());
        source.bind(receiver);
        System.out.println("REBIND|claim=" + source.claim() + "|target=" + source.targetOwner());
        source.reset();
        System.out.println("RESET|known=" + source.known() + "|receiverReceiving=" + receiver.receiving());
    }
}
