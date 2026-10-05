/**
 * Copyright (c) 2021 GregTech-6 Team
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */

package com.gregtech.gregtech.content.logistics;

import java.util.function.BooleanSupplier;

/** Source endpoint ownership: one receiver claims one live sender, without rejecting network forks. */
public final class LongDistanceLink<P, O> {
    private final P position;
    private final O owner;
    private final BooleanSupplier live;
    private P targetPosition;
    private LongDistanceLink<P, O> target, sender;
    public LongDistanceLink(P position, O owner, BooleanSupplier live) {
        this.position = position; this.owner = owner; this.live = live;
    }
    public P targetPosition() { return targetPosition; }
    public O targetOwner() { return target != null && target != this && target.live.getAsBoolean() ? target.owner : null; }
    public O senderOwner() { return receiving() ? sender.owner : null; }
    public boolean receiving() { return sender != null && sender.live.getAsBoolean() && sender.target == this; }
    public boolean known() { return targetPosition != null; }
    public void reset() { targetPosition = null; target = null; sender = null; }
    public void restore(P position) { reset(); targetPosition = position; }
    /** A receiving endpoint cannot be turned into another sender by a soft-hammer scan. */
    public boolean beginScan() {
        if (receiving()) return false;
        targetPosition = position; target = this; sender = null;
        return true;
    }
    public void bind(LongDistanceLink<P, O> peer) { target = peer; targetPosition = peer.position; }
    public boolean claim() {
        if (target == null || target == this || !target.live.getAsBoolean()) return false;
        var previous = target.sender;
        if (previous == null || !previous.live.getAsBoolean() || previous.target == null || !previous.target.live.getAsBoolean()) target.sender = this;
        return target.sender == this;
    }
}
