/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.multiblock;

import gregtech.api.addon.AddonNames;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * What a structure cell matches. GregTech parts, structure casings and addon parts share
 * {@link #part(String)}. {@link #block(String)} and {@link #tag(String)} are plain blocks
 * and may only be placed with {@link StructureRole#CASING}.
 */
public sealed interface CellMatch permits CellMatch.Part, CellMatch.Air, CellMatch.Block, CellMatch.Tag,
        CellMatch.AnyFluid, CellMatch.AnyOpaque, CellMatch.OneOf {
    /**
     * {@code air} is air. {@code anyfluid} is any fluid block; {@code anyfluid#id} also requires
     * that fluid's tag. {@code anyopaque} is any opaque, solid-render block. A leading {@code #}
     * is a block tag. A colon is a block id. Anything else is a part name.
     */
    static CellMatch parse(String text) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("Missing structure part");
        if (text.equals("air")) return air();
        if (text.equals("anyfluid")) return anyFluid();
        if (text.equals("anyopaque")) return anyOpaque();
        if (text.startsWith("anyfluid#")) return anyFluid(text.substring("anyfluid#".length()));
        if (text.startsWith("#")) return tag(text.substring(1));
        if (text.indexOf(':') >= 0) return block(text);
        return part(text);
    }

    static CellMatch part(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Missing structure part");
        if (name.equals("air")) return air();
        if (name.equals("anyfluid")) return anyFluid();
        if (name.equals("anyopaque")) return anyOpaque();
        if (name.startsWith("anyfluid#")) return anyFluid(name.substring("anyfluid#".length()));
        return new Part(name);
    }

    static CellMatch air() { return Air.INSTANCE; }

    /** Any block whose fluid state is non-empty. */
    static CellMatch anyFluid() { return AnyFluid.ANY; }

    /** Any block whose fluid state is non-empty and carries {@code fluidTag}. */
    static CellMatch anyFluid(String fluidTag) {
        requireLocation(fluidTag, true);
        return new AnyFluid(fluidTag);
    }

    /** Any opaque block that renders as a solid cube. */
    static CellMatch anyOpaque() { return AnyOpaque.INSTANCE; }

    /** Any one of {@code members}. Nested groups and duplicates are rejected. */
    static CellMatch oneOf(CellMatch... members) {
        return oneOf(members == null ? null : List.of(members));
    }

    static CellMatch oneOf(List<CellMatch> members) {
        if (members == null || members.size() < 2)
            throw new IllegalArgumentException("A structure cell needs at least two alternatives");
        var copy = new ArrayList<CellMatch>();
        var seen = new HashSet<CellMatch>();
        for (CellMatch member : members) {
            if (member == null) throw new IllegalArgumentException("Missing structure alternative");
            if (member instanceof OneOf) throw new IllegalArgumentException("Nested structure alternatives");
            if (!seen.add(member)) throw new IllegalArgumentException("Duplicate structure alternative: " + member);
            copy.add(member);
        }
        return new OneOf(List.copyOf(copy));
    }

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
        String part = tag ? "#" + id : id;
        int colon = id.indexOf(':');
        if (colon <= 0 || colon != id.lastIndexOf(':') || colon == id.length() - 1
                || !AddonNames.matches(AddonNames.RESOURCE_NAMESPACE, id.substring(0, colon))
                || !AddonNames.matches(AddonNames.RESOURCE_PATH, id.substring(colon + 1)))
            throw new IllegalArgumentException("Invalid block name: " + part);
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

    /** Any fluid block. {@code tag} is null when the fluid tag is not restricted. */
    final class AnyFluid implements CellMatch {
        private static final AnyFluid ANY = new AnyFluid(null);
        private final String tag;

        private AnyFluid(String tag) { this.tag = tag; }

        /** Null when this matches every fluid. */
        public String tag() { return tag; }

        @Override public boolean equals(Object other) {
            return other instanceof AnyFluid that && Objects.equals(tag, that.tag);
        }

        @Override public int hashCode() { return Objects.hashCode(tag); }

        @Override public String toString() { return tag == null ? "anyfluid" : "anyfluid#" + tag; }
    }

    /** Any one of a fixed set of matches. */
    final class OneOf implements CellMatch {
        private final List<CellMatch> members;

        private OneOf(List<CellMatch> members) { this.members = members; }

        public List<CellMatch> members() { return members; }

        @Override public boolean equals(Object other) {
            return other instanceof OneOf that && members.equals(that.members);
        }

        @Override public int hashCode() { return members.hashCode(); }

        @Override public String toString() {
            return members.stream().map(Object::toString)
                    .collect(Collectors.joining(",", "[", "]"));
        }
    }

    /** Any opaque, solid-render block. */
    final class AnyOpaque implements CellMatch {
        private static final AnyOpaque INSTANCE = new AnyOpaque();

        private AnyOpaque() {}

        @Override public boolean equals(Object other) { return other instanceof AnyOpaque; }

        @Override public int hashCode() { return 1; }

        @Override public String toString() { return "anyopaque"; }
    }
}
