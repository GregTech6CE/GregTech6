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

import java.util.*;

/** Source breadth-first same-metadata scan, selecting the first correctly oriented receiver. */
public final class LongDistanceNetwork {
    private LongDistanceNetwork() {}
    public interface World<P, E> {
        String identity(P position);
        Iterable<P> neighbors(P position);
        E endpoint(P position);
        P front(E endpoint);
    }
    public record Connection<P, E>(E receiver, int distance, Set<P> lines) {}
    public static <P, E> Connection<P, E> scan(P origin, P start, String identity, World<P, E> world) {
        var queue = new ArrayDeque<P>();
        var distances = new HashMap<P, Integer>();
        var lines = new LinkedHashSet<P>();
        if (!identity.equals(world.identity(start))) return null;
        queue.add(start); distances.put(start, 0);
        while (!queue.isEmpty()) {
            var current = queue.remove(); lines.add(current);
            for (var next : world.neighbors(current)) {
                if (identity.equals(world.identity(next))) {
                    if (!distances.containsKey(next)) { distances.put(next, distances.get(current) + 1); queue.add(next); }
                } else if (!origin.equals(next)) {
                    var endpoint = world.endpoint(next);
                    if (endpoint != null && lines.contains(world.front(endpoint)))
                        return new Connection<>(endpoint, distances.get(current) + 1, Set.copyOf(lines));
                }
            }
        }
        return null;
    }
    /** UT.Code.getSideForPlayerPlacing uses 65 degrees, not vanilla nearest-axis placement. */
    public static int placement(float pitch, int horizontalOpposite) {
        return pitch >= 65 ? 1 : pitch <= -65 ? 0 : horizontalOpposite;
    }
    public static int activity(long history, boolean stopped, boolean unavailable) {
        return stopped ? 0 : unavailable ? 3 : history == -1L ? 1 : history == 0 ? 0 : 2;
    }
    /** The original retains its recent activity while stopped or waiting for a remembered target. */
    public static long nextHistory(long history, boolean active, boolean stopped, boolean unavailable) {
        return stopped || unavailable ? history : (history << 1) | (active ? 1L : 0L);
    }
}
