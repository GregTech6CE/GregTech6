package com.gregtech.gregtech.api.addon;

import java.util.Objects;

/** SPDX-License-Identifier: LGPL-3.0-or-later */

/**
 * What a structure cell matches. GregTech parts, structure casings and addon parts share
 * {@link #part(String)}. {@link #block(String)} and {@link #tag(String)} are plain blocks
 * and may only be placed with {@link StructureRole#CASING}.
 */
public sealed interface CellMatch permits CellMatch.Part, CellMatch.Air, CellMatch.Block, CellMatch.Tag {
    /**
     * {@code air} is air. A leading {@code #} is a block tag. A colon is a block id.
     * Anything else is a part name. This is the rule {@link MachineLayout.Cell} uses.
     */
    static CellMatch parse(String text) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("Missing structure part");
        if (text.equals("air")) return air();
        if (text.startsWith("#")) return tag(text.substring(1));
        if (text.indexOf(':') >= 0) return block(text);
        return part(text);
    }

    static CellMatch part(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Missing structure part");
        if (name.equals("air")) return air();
        return new Part(name);
    }

    static CellMatch air() { return Air.INSTANCE; }

    static CellMatch block(String id) {
        requireLocation(id, false);
        return new Block(id);
    }

    /** {@code id} has no leading {@code #}. */
    static CellMatch tag(String id) {
        requireLocation(id, true);
        return new Tag(id);
    }

    private static void requireLocation(String id, boolean tag) {
        if (id == null || id.isBlank() || id.startsWith("#") || id.indexOf(':') < 0)
            throw new IllegalArgumentException("Invalid block name: " + id);
        MachineLayout.checkPlainBlock(tag ? "#" + id : id);
    }

    final class Part implements CellMatch {
        private final String name;

        private Part(String name) { this.name = name; }

        public String name() { return name; }

        @Override public boolean equals(Object other) {
            return other instanceof Part that && name.equals(that.name);
        }

        @Override public int hashCode() { return name.hashCode(); }

        @Override public String toString() { return name; }
    }

    final class Air implements CellMatch {
        private static final Air INSTANCE = new Air();

        private Air() {}

        @Override public boolean equals(Object other) { return other instanceof Air; }

        @Override public int hashCode() { return 0; }

        @Override public String toString() { return "air"; }
    }

    final class Block implements CellMatch {
        private final String id;

        private Block(String id) { this.id = id; }

        public String id() { return id; }

        @Override public boolean equals(Object other) {
            return other instanceof Block that && id.equals(that.id);
        }

        @Override public int hashCode() { return id.hashCode(); }

        @Override public String toString() { return id; }
    }

    final class Tag implements CellMatch {
        private final String id;

        private Tag(String id) { this.id = id; }

        public String id() { return id; }

        @Override public boolean equals(Object other) {
            return other instanceof Tag that && id.equals(that.id);
        }

        @Override public int hashCode() { return id.hashCode(); }

        @Override public String toString() { return "#" + id; }
    }
}
