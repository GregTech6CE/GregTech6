package com.gregtech.gregtech.content.logistics;

import org.jetbrains.annotations.Nullable;

/** GT6's item-only semi-filter contract for an inventory behind a generic logistics bus. */
public interface LogisticsSemiFilteredItem {
    /** Null for a blacklist or an unselected prefix; an empty Filter whitelist remains non-null. */
    @Nullable LogisticsItemFilter logisticsItemFilter();
}
