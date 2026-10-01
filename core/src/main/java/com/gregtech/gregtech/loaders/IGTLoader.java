package com.gregtech.gregtech.loaders;

/** GT6-style loader contract — each loader is one phase step, called in list order. */
@FunctionalInterface
public interface IGTLoader {
    void run();
}
