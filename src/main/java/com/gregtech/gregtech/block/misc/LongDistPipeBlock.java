package com.gregtech.gregtech.block.misc;

import net.minecraft.world.level.block.Block;

/** GT6 long-distance pipe, with separate item and fluid line variants. */
public class LongDistPipeBlock extends Block {
    public enum Kind { LEGACY_PIPE, ITEM_PIPE, FLUID_PIPE, WIRE }

    private final Kind kind;
    private final long maximumVoltage;

    public LongDistPipeBlock(boolean isWire, Properties properties) {
        this(isWire,4096,properties);
    }
    public LongDistPipeBlock(boolean isWire,long maximumVoltage,Properties properties) {
        this(isWire ? Kind.WIRE : Kind.LEGACY_PIPE, maximumVoltage, properties);
    }
    public LongDistPipeBlock(Kind kind, Properties properties) {
        this(kind, 4096, properties);
    }
    private LongDistPipeBlock(Kind kind, long maximumVoltage, Properties properties) {
        super(properties);
        this.kind = kind;
        this.maximumVoltage = maximumVoltage;
    }

    public boolean isWire() { return kind == Kind.WIRE; }
    public long maximumVoltage() { return maximumVoltage; }
    /** The old shared port block remains valid on either network for existing test worlds. */
    public boolean acceptsPipeline(boolean fluid) {
        return kind == Kind.LEGACY_PIPE || kind == (fluid ? Kind.FLUID_PIPE : Kind.ITEM_PIPE);
    }
}
