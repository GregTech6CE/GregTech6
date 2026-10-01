package com.gregtech.gregtech.content.logistics;

/** A logistics network tile with six separately persisted cover faces. */
public interface LogisticsCoverHost extends LogisticsHost {
    LogisticsCovers logisticsCovers();
}
