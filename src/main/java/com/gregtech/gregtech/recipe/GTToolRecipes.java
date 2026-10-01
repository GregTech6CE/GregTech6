package com.gregtech.gregtech.recipe;

import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.data.MaterialPrefix;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GT6's <em>shaped</em> tool and tool-head recipes, as data ({@code Loader_Tools:255-380}).
 *
 * <p>GT6 has three tool families and this class covers two of them:</p>
 *
 * <ol>
 *   <li><b>Head + handle tools</b> (pickaxe, axe, sword, …) — GT6 registers them as an
 *       {@code AdvancedCraftingTool}, which <em>is</em> shapeless ({@code extends
 *       ShapelessOreRecipe}); the port keeps that and only makes it visible, see
 *       {@link GTToolAssemblyRecipe}.</li>
 *   <li><b>One-piece tools</b> (wrench, knife, cutters, club, …) — GT6 writes a shaped
 *       {@code CR.shaped} pattern per material through {@code OreProcessing_Tool}, e.g.
 *       {@code WRENCH = {"PhP"," P "," P "}}. These used to be ported as one shapeless assembly,
 *       which is wrong twice: the shape is part of the recipe and the recipe never showed up in the
 *       crafting-table recipe list. {@link GTToolCraftingRecipe} registers GT6's pattern instead.</li>
 *   <li><b>Tool heads</b> — the {@code mToolHeadRecipes} of the same registrations (e.g.
 *       {@code PICKAXE = {"PII","f h"}}), which turn a material's plates/ingots into its head;
 *       {@link GTToolHeadRecipe} registers those.</li>
 * </ol>
 *
 * <p>Key letters are GT6's ({@code Loader_Tools:395-425} for tools, {@code CR.java:336-360} for the
 * bare tool keys): {@code A} the tool head, {@code H} the handle stick, {@code I} ingot, {@code P}
 * plate, {@code G} gem, {@code B} plateCurved, {@code C} plateGem, {@code S} stick, {@code T} screw,
 * {@code O} ring, {@code N} nugget, {@code R} rock, {@code V/W/X/Y/Z} the special object (plate where
 * GT6 registers none), and the lowercase letters are crafting tools ({@code h} hammer, {@code f}
 * file, {@code s} saw, {@code d} screwdriver, {@code r} soft hammer, {@code x} wire cutter, {@code k}
 * knife, {@code y} chisel, {@code z} bending cylinder, {@code w} wrench…).</p>
 *
 * <p>All of GT6's {@code OreProcessing_Tool} rows use {@code CR.DEF_NCC}, i.e. <b>no mirroring</b>;
 * only the hand-written early rows opt in with {@code CR.DEF_MIR}.</p>
 */
public final class GTToolRecipes {

    /**
     * Extra rules of GT6's hand-written early rows ({@code Loader_Tools:255-290}): a slot can be a
     * fixed vanilla item, the tool's material can be fixed by that item, or the row can be limited to
     * one material or to the stone materials.
     */
    public record Gate(Map<Character, net.minecraft.world.item.Item> items,
                       @Nullable String toolMaterial, @Nullable String onlyMaterial,
                       boolean onlyStone, boolean skipHeadGate) {
        public static final Gate NONE = new Gate(Map.of(), null, null, false, false);
    }

    /**
     * One shaped row.
     *
     * @param rows         the pattern, top row first
     * @param forms        pattern letter → material form ({@code 'A'} means "this tool's head")
     * @param tools        pattern letter → the GT tool that has to be in the grid
     * @param mirror       GT6's {@code CR.MIR} for this row
     * @param normalHandle true when {@code H} is the handle <em>material's</em> stick (GT6
     *                     {@code mUseNormalHandle}), false when it is the material's own stick
     * @param gate         the early-row extras, {@link Gate#NONE} for GT6's ore-processing rows
     */
    public record Pattern(String[] rows, Map<Character, MaterialPrefix> forms,
                          Map<Character, GTToolType> tools, boolean mirror, boolean normalHandle,
                          Gate gate) {

        public Pattern(String[] rows, Map<Character, MaterialPrefix> forms,
                       Map<Character, GTToolType> tools, boolean mirror, boolean normalHandle) {
            this(rows, forms, tools, mirror, normalHandle, Gate.NONE);
        }

        public int width() { return rows[0].length(); }

        public int height() { return rows.length; }

        public char at(int x, int y) { return rows[y].charAt(x); }

        /** Every letter in the pattern, in row order. */
        public List<Character> letters() {
            return rows[0].chars().mapToObj(c -> (char) c)
                    .filter(c -> c != ' ')
                    .distinct()
                    .toList();
        }
    }

    private static final Map<GTToolType, List<Pattern>> SHAPED = new LinkedHashMap<>();
    private static final Map<GTToolType, List<Pattern>> HEADS = new LinkedHashMap<>();

    private GTToolRecipes() {}

    // ── keys ──────────────────────────────────────────────────────────────

    private static Map<Character, MaterialPrefix> forms(Object... entries) {
        Map<Character, MaterialPrefix> map = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) map.put((Character) entries[i], (MaterialPrefix) entries[i + 1]);
        return Map.copyOf(map);
    }

    private static Map<Character, GTToolType> tools(Object... entries) {
        Map<Character, GTToolType> map = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) map.put((Character) entries[i], (GTToolType) entries[i + 1]);
        return Map.copyOf(map);
    }

    private static Pattern pattern(String[] rows, Map<Character, MaterialPrefix> forms,
                                   Map<Character, GTToolType> tools, boolean mirror, boolean normalHandle) {
        return new Pattern(rows, forms, tools, mirror, normalHandle);
    }

    private static Pattern tool(String[] rows, Map<Character, MaterialPrefix> forms,
                                Map<Character, GTToolType> tools) {
        return pattern(rows, forms, tools, false, false);
    }

    private static Pattern head(String[] rows, Map<Character, MaterialPrefix> forms,
                                Map<Character, GTToolType> tools) {
        return pattern(rows, forms, tools, false, true);
    }

    /** GT6's hand-written early row: mirror flag, normal handle, and the row's gate. */
    private static Pattern early(String[] rows, Map<Character, MaterialPrefix> forms, boolean mirror,
                                 Gate gate) {
        return new Pattern(rows, forms, Map.of(), mirror, true, gate);
    }

    private static Gate flint() {
        return new Gate(Map.of('X', net.minecraft.world.item.Items.FLINT), "Flint", null, false, true);
    }

    private static Gate bone() {
        return new Gate(Map.of('X', net.minecraft.world.item.Items.BONE), "Bone", null, false, true);
    }

    private static Gate ofMaterial(String material) {
        return new Gate(Map.of(), null, material, false, true);
    }

    private static Gate ofStone() {
        return new Gate(Map.of(), null, null, true, true);
    }

    private static Map<Character, MaterialPrefix> rock() {
        return forms('X', MaterialPrefix.rockGt);
    }

    // ── data ──────────────────────────────────────────────────────────────

    static {
        // Loader_Tools:305-320 — the one-piece tools, both the metal and the gem variant.
        SHAPED.put(GTToolType.WRENCH, List.of(
                tool(new String[]{"PhP", " P ", " P "}, forms('P', MaterialPrefix.plate),
                        tools('h', GTToolType.HARD_HAMMER)),
                tool(new String[]{"CfC", " C ", " C "}, forms('C', MaterialPrefix.plateGem),
                        tools('f', GTToolType.FILE))));
        SHAPED.put(GTToolType.MONKEY_WRENCH, List.of(
                tool(new String[]{"PPd", "hPT", " P "},
                        forms('P', MaterialPrefix.plate, 'T', MaterialPrefix.screw),
                        tools('d', GTToolType.SCREWDRIVER, 'h', GTToolType.HARD_HAMMER)),
                tool(new String[]{"CCd", "fCT", " C "},
                        forms('C', MaterialPrefix.plateGem, 'T', MaterialPrefix.screw),
                        tools('d', GTToolType.SCREWDRIVER, 'f', GTToolType.FILE))));
        SHAPED.put(GTToolType.BENDING_CYLINDER, List.of(
                tool(new String[]{"sfh", "III", "III"}, forms('I', MaterialPrefix.ingot),
                        tools('s', GTToolType.SAW, 'f', GTToolType.FILE, 'h', GTToolType.HARD_HAMMER))));
        SHAPED.put(GTToolType.BENDING_CYLINDER_SMALL, List.of(
                tool(new String[]{"sfh", "III"}, forms('I', MaterialPrefix.ingot),
                        tools('s', GTToolType.SAW, 'f', GTToolType.FILE, 'h', GTToolType.HARD_HAMMER))));
        SHAPED.put(GTToolType.CROWBAR, List.of(
                tool(new String[]{"hVS", "VSV", "SVf"},
                        forms('V', MaterialPrefix.plate, 'S', MaterialPrefix.stick),
                        tools('h', GTToolType.HARD_HAMMER, 'f', GTToolType.FILE))));
        SHAPED.put(GTToolType.PLUNGER, List.of(
                tool(new String[]{"xVV", " SV", "S f"},
                        forms('V', MaterialPrefix.plate, 'S', MaterialPrefix.stick),
                        tools('x', GTToolType.WIRE_CUTTER, 'f', GTToolType.FILE))));
        SHAPED.put(GTToolType.PINCERS, List.of(
                tool(new String[]{"XhX", " T ", "SdS"},
                        forms('X', MaterialPrefix.plate, 'T', MaterialPrefix.screw,
                                'S', MaterialPrefix.stick),
                        tools('h', GTToolType.HARD_HAMMER, 'd', GTToolType.SCREWDRIVER))));
        SHAPED.put(GTToolType.SCOOP, List.of(
                tool(new String[]{"SVS", "SSS", "xSh"},
                        forms('S', MaterialPrefix.stick, 'V', MaterialPrefix.plate),
                        tools('x', GTToolType.WIRE_CUTTER, 'h', GTToolType.HARD_HAMMER))));
        SHAPED.put(GTToolType.KNIFE, List.of(
                tool(new String[]{"fP", "hH"}, forms('P', MaterialPrefix.plate),
                        tools('f', GTToolType.FILE, 'h', GTToolType.HARD_HAMMER)),
                tool(new String[]{"fC", "hH"}, forms('C', MaterialPrefix.plateGem),
                        tools('f', GTToolType.FILE, 'h', GTToolType.HARD_HAMMER))));
        SHAPED.put(GTToolType.BUTCHERY_KNIFE, List.of(
                tool(new String[]{"fPP", "hPP", "  H"}, forms('P', MaterialPrefix.plate),
                        tools('f', GTToolType.FILE, 'h', GTToolType.HARD_HAMMER)),
                tool(new String[]{"fCC", " CC", "  H"}, forms('C', MaterialPrefix.plateGem),
                        tools('f', GTToolType.FILE))));
        SHAPED.put(GTToolType.WIRE_CUTTER, List.of(
                tool(new String[]{"PfP", "hPd", "STS"},
                        forms('P', MaterialPrefix.plate, 'S', MaterialPrefix.stick,
                                'T', MaterialPrefix.screw),
                        tools('f', GTToolType.FILE, 'h', GTToolType.HARD_HAMMER, 'd', GTToolType.SCREWDRIVER)),
                tool(new String[]{"CfC", "hCd", "STS"},
                        forms('C', MaterialPrefix.plateGem, 'S', MaterialPrefix.stick,
                                'T', MaterialPrefix.screw),
                        tools('f', GTToolType.FILE, 'h', GTToolType.HARD_HAMMER, 'd', GTToolType.SCREWDRIVER))));
        SHAPED.put(GTToolType.BRANCH_CUTTER, List.of(
                tool(new String[]{"PfP", "PdP", "STS"},
                        forms('P', MaterialPrefix.plate, 'S', MaterialPrefix.stick,
                                'T', MaterialPrefix.screw),
                        tools('f', GTToolType.FILE, 'd', GTToolType.SCREWDRIVER)),
                tool(new String[]{"CfC", "CdC", "STS"},
                        forms('C', MaterialPrefix.plateGem, 'S', MaterialPrefix.stick,
                                'T', MaterialPrefix.screw),
                        tools('f', GTToolType.FILE, 'd', GTToolType.SCREWDRIVER))));
        SHAPED.put(GTToolType.SCISSORS, List.of(
                tool(new String[]{"PfP", " T ", "OdO"},
                        forms('P', MaterialPrefix.plate, 'T', MaterialPrefix.screw, 'O', MaterialPrefix.ring),
                        tools('f', GTToolType.FILE, 'd', GTToolType.SCREWDRIVER)),
                tool(new String[]{"CfC", " T ", "OdO"},
                        forms('C', MaterialPrefix.plateGem, 'T', MaterialPrefix.screw, 'O', MaterialPrefix.ring),
                        tools('f', GTToolType.FILE, 'd', GTToolType.SCREWDRIVER))));
        SHAPED.put(GTToolType.CLUB, List.of(
                pattern(new String[]{" II", "III", "HI "}, forms('I', MaterialPrefix.ingot),
                        Map.of(), false, true),
                pattern(new String[]{" GG", "GGG", "HG "}, forms('G', MaterialPrefix.gem),
                        Map.of(), false, true),
                pattern(new String[]{" RR", "RRR", "HR "}, forms('R', MaterialPrefix.rockGt),
                        Map.of(), false, true)));
        SHAPED.put(GTToolType.HAND_DRILL, List.of(
                pattern(new String[]{"  X", "HYH", "YH "},
                        forms('X', MaterialPrefix.plate, 'Y', MaterialPrefix.plate),
                        Map.of(), false, true)));
        SHAPED.put(GTToolType.UNIVERSAL_SPADE, List.of(
                tool(new String[]{"AT", "Sd"}, forms('S', MaterialPrefix.stick),
                        tools('d', GTToolType.SCREWDRIVER))));
        // Loader_Tools:245-247 — the rolling pin is one of the hand-written rows (CR.DEF_MIR).
        SHAPED.put(GTToolType.ROLLING_PIN, List.of(
                pattern(new String[]{"  S", " I ", "S f"},
                        forms('I', MaterialPrefix.ingot, 'S', MaterialPrefix.stick),
                        tools('f', GTToolType.FILE), true, false)));
        // Loader_Tools:225-243 — "T "/" F": a striking material above a piece of flint. The T key
        // accepts the material's gem, rock or nugget form; GTFlintAndTinderRecipe lists them.
        SHAPED.put(GTToolType.FLINT_AND_TINDER, List.of(
                pattern(new String[]{"T ", " F"}, forms('T', MaterialPrefix.gem),
                        Map.of(), true, false)));

        // Loader_Tools:255-290 — the hand-written early tools, one row per family, with the rock or
        // the flint itself as the material: flint (MT.Flint), bone (MT.Bone), obsidian, petrified
        // wood and every stone material (ANY.Stone.mToThis). The handle is one of GT6's handle loop
        // entries (wood stick, bamboo, bone, plastic), which the port expresses as "any valid stick".
        addEarly(GTToolType.KNIFE, new String[]{"HX"}, flint(), true);
        addEarly(GTToolType.AXE, new String[]{"XX", "XH"}, flint(), true);
        addEarly(GTToolType.SHOVEL, new String[]{"X", "H"}, flint(), false);
        addEarly(GTToolType.PICKAXE, new String[]{"XXX", " H "}, flint(), false);
        addEarly(GTToolType.CLUB, new String[]{"  X", " X ", "H  "}, bone(), true);
        addEarly(GTToolType.KNIFE, new String[]{"HX"}, ofMaterial("Obsidian"), true);
        addEarly(GTToolType.AXE, new String[]{"XX", "XH"}, ofMaterial("Obsidian"), true);
        addEarly(GTToolType.SHOVEL, new String[]{"X", "H"}, ofMaterial("Obsidian"), false);
        addEarly(GTToolType.PICKAXE, new String[]{"XXX", " H "}, ofMaterial("Obsidian"), false);
        addEarly(GTToolType.AXE, new String[]{"XX", "XH"}, ofMaterial("PetrifiedWood"), true);
        addEarly(GTToolType.HOE, new String[]{"XX", " H"}, ofMaterial("PetrifiedWood"), true);
        addEarly(GTToolType.SHOVEL, new String[]{"X", "H"}, ofMaterial("PetrifiedWood"), false);
        addEarly(GTToolType.PICKAXE, new String[]{"XXX", " H "}, ofMaterial("PetrifiedWood"), false);
        addEarly(GTToolType.CLUB, new String[]{" XX", "XXX", "HX "}, ofMaterial("PetrifiedWood"), true);
        addEarly(GTToolType.HARD_HAMMER, new String[]{"XX ", "XXH", "XX "},
                ofMaterial("PetrifiedWood"), true);
        addEarly(GTToolType.AXE, new String[]{"XX", "XH"}, ofStone(), true);
        addEarly(GTToolType.HOE, new String[]{"XX", " H"}, ofStone(), true);
        addEarly(GTToolType.SHOVEL, new String[]{"X", "H"}, ofStone(), false);
        addEarly(GTToolType.PICKAXE, new String[]{"XXX", " H "}, ofStone(), false);
        addEarly(GTToolType.CLUB, new String[]{" XX", "XXX", "HX "}, ofStone(), true);
        addEarly(GTToolType.HARD_HAMMER, new String[]{"XX ", "XXH", "XX "}, ofStone(), true);

        // Loader_Tools:293-313 — the head recipes of the same registrations.
        HEADS.put(GTToolType.PICKAXE, List.of(
                head(new String[]{"PII", "f h"}, forms('P', MaterialPrefix.plate, 'I', MaterialPrefix.ingot),
                        tools('f', GTToolType.FILE, 'h', GTToolType.HARD_HAMMER)),
                head(new String[]{"CGG", "f  "}, forms('C', MaterialPrefix.plateGem, 'G', MaterialPrefix.gem),
                        tools('f', GTToolType.FILE))));
        HEADS.put(GTToolType.SHOVEL, List.of(
                head(new String[]{"fPh"}, forms('P', MaterialPrefix.plate),
                        tools('f', GTToolType.FILE, 'h', GTToolType.HARD_HAMMER)),
                head(new String[]{"fC "}, forms('C', MaterialPrefix.plateGem),
                        tools('f', GTToolType.FILE))));
        HEADS.put(GTToolType.SPADE, List.of(
                head(new String[]{"fPh", " s "}, forms('P', MaterialPrefix.plate),
                        tools('f', GTToolType.FILE, 'h', GTToolType.HARD_HAMMER, 's', GTToolType.SAW)),
                head(new String[]{"fC ", " s "}, forms('C', MaterialPrefix.plateGem),
                        tools('f', GTToolType.FILE, 's', GTToolType.SAW))));
        HEADS.put(GTToolType.AXE, List.of(
                head(new String[]{"PIh", "P  ", "f  "},
                        forms('P', MaterialPrefix.plate, 'I', MaterialPrefix.ingot),
                        tools('h', GTToolType.HARD_HAMMER, 'f', GTToolType.FILE)),
                head(new String[]{"CG ", "C  ", "f  "},
                        forms('C', MaterialPrefix.plateGem, 'G', MaterialPrefix.gem),
                        tools('f', GTToolType.FILE))));
        HEADS.put(GTToolType.DOUBLE_AXE, List.of(
                head(new String[]{"PIP", "P P", "f h"}, forms('P', MaterialPrefix.plate,
                                'I', MaterialPrefix.ingot),
                        tools('f', GTToolType.FILE, 'h', GTToolType.HARD_HAMMER)),
                head(new String[]{"CGC", "C C", "f  "}, forms('C', MaterialPrefix.plateGem,
                                'G', MaterialPrefix.gem),
                        tools('f', GTToolType.FILE))));
        HEADS.put(GTToolType.HOE, List.of(
                head(new String[]{"PIh", "f  "}, forms('P', MaterialPrefix.plate, 'I', MaterialPrefix.ingot),
                        tools('h', GTToolType.HARD_HAMMER, 'f', GTToolType.FILE)),
                head(new String[]{"CG ", "f  "}, forms('C', MaterialPrefix.plateGem, 'G', MaterialPrefix.gem),
                        tools('f', GTToolType.FILE))));
        HEADS.put(GTToolType.SWORD, List.of(
                head(new String[]{" P ", "fPh"}, forms('P', MaterialPrefix.plate),
                        tools('f', GTToolType.FILE, 'h', GTToolType.HARD_HAMMER)),
                head(new String[]{" C ", "fC "}, forms('C', MaterialPrefix.plateGem),
                        tools('f', GTToolType.FILE))));
        HEADS.put(GTToolType.HARD_HAMMER, List.of(
                head(new String[]{"II ", "IIh", "II "}, forms('I', MaterialPrefix.ingot),
                        tools('h', GTToolType.HARD_HAMMER)),
                head(new String[]{"GG ", "GGf", "GG "}, forms('G', MaterialPrefix.gem),
                        tools('f', GTToolType.FILE))));
        HEADS.put(GTToolType.SOFT_HAMMER, List.of(
                head(new String[]{"II ", "IIr", "II "}, forms('I', MaterialPrefix.ingot),
                        tools('r', GTToolType.SOFT_HAMMER)),
                head(new String[]{"GG ", "GGr", "GG "}, forms('G', MaterialPrefix.gem),
                        tools('r', GTToolType.SOFT_HAMMER))));
        HEADS.put(GTToolType.FILE, List.of(
                head(new String[]{" P ", " Pk"}, forms('P', MaterialPrefix.plate),
                        tools('k', GTToolType.KNIFE))));
        HEADS.put(GTToolType.SAW, List.of(
                head(new String[]{"PP", "fh"}, forms('P', MaterialPrefix.plate),
                        tools('f', GTToolType.FILE, 'h', GTToolType.HARD_HAMMER)),
                head(new String[]{"CC", "f "}, forms('C', MaterialPrefix.plateGem),
                        tools('f', GTToolType.FILE))));
        HEADS.put(GTToolType.CHISEL, List.of(
                head(new String[]{"hPf", " S "}, forms('P', MaterialPrefix.plate, 'S', MaterialPrefix.stick),
                        tools('h', GTToolType.HARD_HAMMER, 'f', GTToolType.FILE)),
                head(new String[]{"Cf", "S "}, forms('C', MaterialPrefix.plateGem, 'S', MaterialPrefix.stick),
                        tools('f', GTToolType.FILE))));
        HEADS.put(GTToolType.SCREWDRIVER, List.of(
                head(new String[]{"hS", "Sf"}, forms('S', MaterialPrefix.stick),
                        tools('h', GTToolType.HARD_HAMMER, 'f', GTToolType.FILE))));
        HEADS.put(GTToolType.SENSE, List.of(
                head(new String[]{"PPI", "f h"}, forms('P', MaterialPrefix.plate, 'I', MaterialPrefix.ingot),
                        tools('f', GTToolType.FILE, 'h', GTToolType.HARD_HAMMER)),
                head(new String[]{"CCG", "f  "}, forms('C', MaterialPrefix.plateGem, 'G', MaterialPrefix.gem),
                        tools('f', GTToolType.FILE))));
        HEADS.put(GTToolType.PLOW, List.of(
                head(new String[]{"PPP", "PPP", "f h"}, forms('P', MaterialPrefix.plate),
                        tools('f', GTToolType.FILE, 'h', GTToolType.HARD_HAMMER)),
                head(new String[]{"CCC", "CCC", "f  "}, forms('C', MaterialPrefix.plateGem),
                        tools('f', GTToolType.FILE))));
        HEADS.put(GTToolType.CONSTRUCTION_PICK, List.of(
                head(new String[]{"PIP", "f h"}, forms('P', MaterialPrefix.plate, 'I', MaterialPrefix.ingot),
                        tools('f', GTToolType.FILE, 'h', GTToolType.HARD_HAMMER)),
                head(new String[]{"CGC", "f  "}, forms('C', MaterialPrefix.plateGem, 'G', MaterialPrefix.gem),
                        tools('f', GTToolType.FILE))));
        HEADS.put(GTToolType.BUILDER_WAND, List.of(
                head(new String[]{" P ", "f h", " s "}, forms('P', MaterialPrefix.plate),
                        tools('f', GTToolType.FILE, 'h', GTToolType.HARD_HAMMER, 's', GTToolType.SAW)),
                head(new String[]{" C ", "f h", " s "}, forms('C', MaterialPrefix.plateGem),
                        tools('f', GTToolType.FILE, 'h', GTToolType.HARD_HAMMER, 's', GTToolType.SAW)),
                head(new String[]{" G ", "f h", " s "}, forms('G', MaterialPrefix.gem),
                        tools('f', GTToolType.FILE, 'h', GTToolType.HARD_HAMMER, 's', GTToolType.SAW))));
        // Loader_Tools:298 — GT6 also writes a head row for the wrench; the port's wrench is headless
        // (its tool row builds it straight from plates), so the head is only reachable through this row.
        HEADS.put(GTToolType.WRENCH, List.of(
                head(new String[]{"hPW", "PVP", "WPd"},
                        forms('P', MaterialPrefix.plate, 'W', MaterialPrefix.plate, 'V', MaterialPrefix.plate),
                        tools('h', GTToolType.HARD_HAMMER, 'd', GTToolType.SCREWDRIVER)),
                head(new String[]{"hCW", "CVC", "WCd"},
                        forms('C', MaterialPrefix.plateGem, 'W', MaterialPrefix.plateGem,
                                'V', MaterialPrefix.plateGem),
                        tools('h', GTToolType.HARD_HAMMER, 'd', GTToolType.SCREWDRIVER))));
    }

    // ── access ────────────────────────────────────────────────────────────

    /** Appends one hand-written early row to a tool's pattern list. */
    private static void addEarly(GTToolType type, String[] rows, Gate gate, boolean mirror) {
        List<Pattern> patterns = new ArrayList<>(SHAPED.getOrDefault(type, List.of()));
        patterns.add(early(rows, rock(), mirror, gate));
        SHAPED.put(type, List.copyOf(patterns));
    }

    /** GT6's shaped rows for the one-piece tools, in registration order. */
    public static List<Pattern> shaped(GTToolType type) {
        return SHAPED.getOrDefault(type, List.of());
    }

    /** GT6's shaped head rows ({@code mToolHeadRecipes}), in registration order. */
    public static List<Pattern> heads(GTToolType type) {
        return HEADS.getOrDefault(type, List.of());
    }

    /**
     * The form a tool's head is made of. Every tool with a head uses its own prefix; the wrench is
     * headless in the port but GT6 registers a head row for it, so the prefix is stated here.
     */
    public static MaterialPrefix headPrefix(GTToolType type) {
        if (type.headPrefix() != null) return type.headPrefix();
        if (type == GTToolType.WRENCH) return MaterialPrefix.toolHeadWrench;
        return null;
    }

    /** Every tool with a shaped GT6 recipe. */
    public static Map<GTToolType, List<Pattern>> allShaped() {
        return Map.copyOf(SHAPED);
    }

    /** Every tool whose head has a shaped GT6 recipe. */
    public static Map<GTToolType, List<Pattern>> allHeads() {
        return Map.copyOf(HEADS);
    }

    /**
     * Whether GT6 registers the head + handle (shapeless {@code AdvancedCraftingTool}) recipe for this
     * tool. Head-based tools keep that row even when they also have shaped early rows — a pickaxe can
     * come from a head plus a stick <em>or</em> straight from a rock.
     */
    public static boolean isHeadAssembly(GTToolType type) {
        return type.requiresHeadAssembly();
    }

    /** The vanilla item a pattern letter stands for, or null when it is a material form. */
    @Nullable
    public static net.minecraft.world.item.Item vanilla(char letter) {
        return letter == 'F' ? Items.FLINT : null;
    }
}
