package com.gregtech.gregtech.worldgen.dungeon;

/**
 * Port of GT6's {@code IDungeonChunk} ({@code gregapi/worldgen/dungeon/IDungeonChunk.java}): one cell of a
 * GT6 dungeon.
 *
 * <p>A cell is a 16×16 chunk-sized piece of the dungeon. {@link #generate(GTDungeonData)} returns whether
 * the cell accepted its assignment; GT6's dispatcher then tried the next candidate when a cell type
 * declined (for example a farm that needs water it cannot find, or a portal whose dimension does not
 * exist). The port keeps that contract.</p>
 */
public interface GTDungeonChunk {

    /** Builds this cell, returning false when the cell declines and another type should be tried. */
    boolean generate(GTDungeonData data);

    /** The port's id for logs and tests. */
    default String id() { return getClass().getSimpleName(); }
}
