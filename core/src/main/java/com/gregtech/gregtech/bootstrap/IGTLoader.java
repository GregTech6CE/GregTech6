package com.gregtech.gregtech.bootstrap;

/** GT6-style loader contract — each loader is one phase step, called in list order. */
@FunctionalInterface
public interface IGTLoader {
    void run();
}
