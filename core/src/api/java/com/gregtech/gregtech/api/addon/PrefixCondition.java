package com.gregtech.gregtech.api.addon;

/**
 * SPDX-License-Identifier: LGPL-3.0-or-later.
 * Fixed filters for an addon prefix. An addon cannot pass its own predicate.
 */
public enum PrefixCondition {
    /** Tool quality is at least 1. */
    HAS_TOOL_STATS,
    /** The material can be forged. */
    SMITHABLE,
    /** Not a wood material. */
    NOT_WOOD,
    /**
     * Not coal, charcoal, coal coke, lignite, lignite coke, anthracite,
     * hydrated coal or petroleum coke.
     */
    NOT_COAL,
    /** Not a stone material. */
    NOT_STONE,
    /** Does not carry the GregTech food flag. */
    NOT_FOOD,
    /** Not raw, cooked or rotten meat. */
    NOT_MEAT;

    public boolean positive() {
        return this == HAS_TOOL_STATS || this == SMITHABLE;
    }
}
