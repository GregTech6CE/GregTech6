package com.gregtech.gregtech.content.tool;

import com.gregtech.gregtech.api.tool.ToolDefinition;
import com.gregtech.gregtech.data.MaterialPrefix;

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
 *       GTToolAssemblyRecipe.</li>
 *   <li><b>One-piece tools</b> (wrench, knife, cutters, club, …) — GT6 writes a shaped
 *       {@code CR.shaped} pattern per material through {@code OreProcessing_Tool}, e.g.
 *       {@code WRENCH = {"PhP"," P "," P "}}. These used to be ported as one shapeless assembly,
 *       which is wrong twice: the shape is part of the recipe and the recipe never showed up in the
 *       crafting-table recipe list. GTToolCraftingRecipe registers GT6's pattern instead.</li>
 *   <li><b>Tool heads</b> — the {@code mToolHeadRecipes} of the same registrations (e.g.
 *       {@code PICKAXE = {"PII","f h"}}), which turn a material's plates/ingots into its head;
 *       GTToolHeadRecipe registers those.</li>
 * </ol>
 *
 * <p>Key letters are GT6's ({@code Loader_Tools:395-425} for tools, {@code CR.java:336-360} for the
 * bare tool keys): {@code A} the tool head, {@code H} the handle stick, {@code I} ingot, {@code P}
 * plate, {@code G} gem, {@code B} plateCurved, {@code C} plateGem, {@code S} stick, {@code T} screw,
 * {@code O} ring, {@code N} nugget, {@code R} rock, {@code V/W/X/Y/Z} the per-tool special object, and the lowercase letters are crafting tools ({@code h} hammer, {@code f}
 * file, {@code s} saw, {@code d} screwdriver, {@code r} soft hammer, {@code x} wire cutter, {@code k}
 * knife, {@code y} chisel, {@code z} bending cylinder, {@code w} wrench…).</p>
 *
 * <p>All of GT6's {@code OreProcessing_Tool} rows use {@code CR.DEF_NCC}, i.e. <b>no mirroring</b>;
 * only the hand-written early rows opt in with {@code CR.DEF_MIR}.</p>
 */
public final class ManualToolRecipeCatalog {

    /**
     * Extra rules of GT6's hand-written early rows ({@code Loader_Tools:255-290}): a slot can be a
     * fixed vanilla item, the tool's material can be fixed by that item, or the row can be limited to
     * one material or to the stone materials.
     */
    public record Gate(Map<Character, String> items,
                        String toolMaterial,  String onlyMaterial,
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
                          Map<Character, ToolDefinition> tools, boolean mirror, boolean normalHandle,
                          Gate gate) {

        public Pattern {
            // CraftingInput in 1.21 trims empty margins before recipe matching. GT6's CR.shaped
            // does the same; keep the stored pattern canonical on both platforms.
            int left = rows[0].length(), right = -1, top = rows.length, bottom = -1;
            for (int y = 0; y < rows.length; y++) for (int x = 0; x < rows[y].length(); x++) {
                if (rows[y].charAt(x) == ' ') continue;
                left = Math.min(left, x); right = Math.max(right, x);
                top = Math.min(top, y); bottom = Math.max(bottom, y);
            }
            if (right < left) throw new IllegalArgumentException("Empty tool pattern");
            String[] trimmed = new String[bottom - top + 1];
            for (int y = top; y <= bottom; y++) trimmed[y - top] = rows[y].substring(left, right + 1);
            rows = trimmed;
        }

        public Pattern(String[] rows, Map<Character, MaterialPrefix> forms,
                       Map<Character, ToolDefinition> tools, boolean mirror, boolean normalHandle) {
            this(rows, forms, tools, mirror, normalHandle, Gate.NONE);
        }

        public int width() { return rows[0].length(); }

        public int height() { return rows.length; }

        public char at(int x, int y) { return rows[y].charAt(x); }

        /** Every letter in the pattern, in row order. */
        public List<Character> letters() {
            return java.util.Arrays.stream(rows).flatMapToInt(String::chars).mapToObj(c -> (char) c)
                    .filter(c -> c != ' ')
                    .distinct()
                    .toList();
        }
    }

    private static final Map<ToolDefinition, List<Pattern>> SHAPED = new LinkedHashMap<>();
    private static final Map<ToolDefinition, List<Pattern>> HEADS = new LinkedHashMap<>();

    private ManualToolRecipeCatalog() {}

    // ── keys ──────────────────────────────────────────────────────────────

    private static Map<Character, MaterialPrefix> forms(Object... entries) {
        Map<Character, MaterialPrefix> map = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) map.put((Character) entries[i], (MaterialPrefix) entries[i + 1]);
        return Map.copyOf(map);
    }

    private static Map<Character, ToolDefinition> tools(Object... entries) {
        Map<Character, ToolDefinition> map = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) map.put((Character) entries[i], (ToolDefinition) entries[i + 1]);
        return Map.copyOf(map);
    }

    private static Pattern pattern(String[] rows, Map<Character, MaterialPrefix> forms,
                                   Map<Character, ToolDefinition> tools, boolean mirror, boolean normalHandle) {
        return new Pattern(rows, forms, tools, mirror, normalHandle);
    }

    private static Pattern tool(String[] rows, Map<Character, MaterialPrefix> forms,
                                Map<Character, ToolDefinition> tools) {
        return pattern(rows, forms, tools, false, false);
    }

    private static Pattern head(String[] rows, Map<Character, MaterialPrefix> forms,
                                Map<Character, ToolDefinition> tools) {
        return pattern(rows, forms, tools, false, true);
    }

    /** GT6's hand-written early row: mirror flag, normal handle, and the row's gate. */
    private static Pattern early(String[] rows, Map<Character, MaterialPrefix> forms, boolean mirror,
                                 Gate gate) {
        return new Pattern(rows, forms, Map.of(), mirror, true, gate);
    }

    private static Gate flint() {
        return new Gate(Map.of('X', "minecraft:flint"), "Flint", null, false, true);
    }

    private static Gate bone() {
        return new Gate(Map.of('X', "minecraft:bone"), "Bone", null, false, true);
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
        // Loader_Tools:317-319. Flint is the striker; H uses the normal material handle.
        var gunForms=forms('X',MaterialPrefix.plateCurved,'T',MaterialPrefix.screw);
        var gunTools=tools('d',ToolDefinition.SCREWDRIVER,'h',ToolDefinition.HARD_HAMMER);
        var gunGate=new Gate(Map.of('V',"minecraft:flint"),null,null,false,false);
        SHAPED.put(ToolDefinition.PISTOL,List.of(new Pattern(new String[]{"XXV"," TH","d h"},gunForms,gunTools,false,true,gunGate)));
        SHAPED.put(ToolDefinition.CARBINE,List.of(new Pattern(new String[]{"XXV","THH","d h"},gunForms,gunTools,false,true,gunGate)));
        SHAPED.put(ToolDefinition.RIFLE,List.of(new Pattern(new String[]{"XXX","HHV","dTh"},gunForms,gunTools,false,true,gunGate)));
        // Loader_Tools:354; all seven blades and rings share one primary material, handle Blue.
        SHAPED.put(ToolDefinition.POCKET_MULTITOOL,List.of(tool(new String[]{"AXO","ZPV","OWY"},
            forms('A',MaterialPrefix.toolHeadScrewdriver,'X',MaterialPrefix.toolHeadSaw,'Y',MaterialPrefix.toolHeadChisel,
                'Z',MaterialPrefix.toolHeadFile,'V',MaterialPrefix.toolHeadSword,'W',MaterialPrefix.toolHeadSword,'P',MaterialPrefix.plate,'O',MaterialPrefix.ring),Map.of())));
        // Loader_Tools:305-320 — the one-piece tools, both the metal and the gem variant.
        SHAPED.put(ToolDefinition.WRENCH, List.of(
                tool(new String[]{"PhP", " P ", " P "}, forms('P', MaterialPrefix.plate),
                        tools('h', ToolDefinition.HARD_HAMMER)),
                tool(new String[]{"CfC", " C ", " C "}, forms('C', MaterialPrefix.plateGem),
                        tools('f', ToolDefinition.FILE))));
        SHAPED.put(ToolDefinition.MONKEY_WRENCH, List.of(
                tool(new String[]{"PPd", "hPT", " P "},
                        forms('P', MaterialPrefix.plate, 'T', MaterialPrefix.screw),
                        tools('d', ToolDefinition.SCREWDRIVER, 'h', ToolDefinition.HARD_HAMMER)),
                tool(new String[]{"CCd", "fCT", " C "},
                        forms('C', MaterialPrefix.plateGem, 'T', MaterialPrefix.screw),
                        tools('d', ToolDefinition.SCREWDRIVER, 'f', ToolDefinition.FILE))));
        SHAPED.put(ToolDefinition.BENDING_CYLINDER, List.of(
                tool(new String[]{"sfh", "III", "III"}, forms('I', MaterialPrefix.ingot),
                        tools('s', ToolDefinition.SAW, 'f', ToolDefinition.FILE, 'h', ToolDefinition.HARD_HAMMER))));
        SHAPED.put(ToolDefinition.BENDING_CYLINDER_SMALL, List.of(
                tool(new String[]{"sfh", "III"}, forms('I', MaterialPrefix.ingot),
                        tools('s', ToolDefinition.SAW, 'f', ToolDefinition.FILE, 'h', ToolDefinition.HARD_HAMMER))));
        SHAPED.put(ToolDefinition.CROWBAR, List.of(
                tool(new String[]{"hVS", "VSV", "SVf"},
                        forms('V', MaterialPrefix.plate, 'S', MaterialPrefix.stick),
                        tools('h', ToolDefinition.HARD_HAMMER, 'f', ToolDefinition.FILE))));
        SHAPED.put(ToolDefinition.PLUNGER, List.of(
                tool(new String[]{"xVV", " SV", "S f"},
                        forms('V', MaterialPrefix.plate, 'S', MaterialPrefix.stick),
                        tools('x', ToolDefinition.WIRE_CUTTER, 'f', ToolDefinition.FILE))));
        SHAPED.put(ToolDefinition.PINCERS, List.of(
                tool(new String[]{"XhX", " T ", "SdS"},
                        forms('X', MaterialPrefix.plateCurved, 'T', MaterialPrefix.screw,
                                'S', MaterialPrefix.stick),
                        tools('h', ToolDefinition.HARD_HAMMER, 'd', ToolDefinition.SCREWDRIVER))));
        SHAPED.put(ToolDefinition.SCOOP, List.of(
                tool(new String[]{"SVS", "SSS", "xSh"},
                        forms('S', MaterialPrefix.stick, 'V', MaterialPrefix.plate),
                        tools('x', ToolDefinition.WIRE_CUTTER, 'h', ToolDefinition.HARD_HAMMER))));
        SHAPED.put(ToolDefinition.KNIFE, List.of(
                tool(new String[]{"fP", "hH"}, forms('P', MaterialPrefix.plate),
                        tools('f', ToolDefinition.FILE, 'h', ToolDefinition.HARD_HAMMER)),
                tool(new String[]{"fC", "hH"}, forms('C', MaterialPrefix.plateGem),
                        tools('f', ToolDefinition.FILE, 'h', ToolDefinition.HARD_HAMMER))));
        SHAPED.put(ToolDefinition.BUTCHERY_KNIFE, List.of(
                tool(new String[]{"fPP", "hPP", "  H"}, forms('P', MaterialPrefix.plate),
                        tools('f', ToolDefinition.FILE, 'h', ToolDefinition.HARD_HAMMER)),
                tool(new String[]{"fCC", " CC", "  H"}, forms('C', MaterialPrefix.plateGem),
                        tools('f', ToolDefinition.FILE))));
        SHAPED.put(ToolDefinition.WIRE_CUTTER, List.of(
                tool(new String[]{"PfP", "hPd", "STS"},
                        forms('P', MaterialPrefix.plate, 'S', MaterialPrefix.stick,
                                'T', MaterialPrefix.screw),
                        tools('f', ToolDefinition.FILE, 'h', ToolDefinition.HARD_HAMMER, 'd', ToolDefinition.SCREWDRIVER)),
                tool(new String[]{"CfC", "hCd", "STS"},
                        forms('C', MaterialPrefix.plateGem, 'S', MaterialPrefix.stick,
                                'T', MaterialPrefix.screw),
                        tools('f', ToolDefinition.FILE, 'h', ToolDefinition.HARD_HAMMER, 'd', ToolDefinition.SCREWDRIVER))));
        SHAPED.put(ToolDefinition.BRANCH_CUTTER, List.of(
                tool(new String[]{"PfP", "PdP", "STS"},
                        forms('P', MaterialPrefix.plate, 'S', MaterialPrefix.stick,
                                'T', MaterialPrefix.screw),
                        tools('f', ToolDefinition.FILE, 'd', ToolDefinition.SCREWDRIVER)),
                tool(new String[]{"CfC", "CdC", "STS"},
                        forms('C', MaterialPrefix.plateGem, 'S', MaterialPrefix.stick,
                                'T', MaterialPrefix.screw),
                        tools('f', ToolDefinition.FILE, 'd', ToolDefinition.SCREWDRIVER))));
        SHAPED.put(ToolDefinition.SCISSORS, List.of(
                tool(new String[]{"PfP", " T ", "OdO"},
                        forms('P', MaterialPrefix.plate, 'T', MaterialPrefix.screw, 'O', MaterialPrefix.ring),
                        tools('f', ToolDefinition.FILE, 'd', ToolDefinition.SCREWDRIVER)),
                tool(new String[]{"CfC", " T ", "OdO"},
                        forms('C', MaterialPrefix.plateGem, 'T', MaterialPrefix.screw, 'O', MaterialPrefix.ring),
                        tools('f', ToolDefinition.FILE, 'd', ToolDefinition.SCREWDRIVER))));
        SHAPED.put(ToolDefinition.CLUB, List.of(
                pattern(new String[]{" II", "III", "HI "}, forms('I', MaterialPrefix.ingot),
                        Map.of(), false, true),
                pattern(new String[]{" GG", "GGG", "HG "}, forms('G', MaterialPrefix.gem),
                        Map.of(), false, true),
                pattern(new String[]{" RR", "RRR", "HR "}, forms('R', MaterialPrefix.rockGt),
                        Map.of(), false, true)));
        SHAPED.put(ToolDefinition.HAND_DRILL, List.of(
                pattern(new String[]{"  X", "HYH", "YH "},
                        forms('X', MaterialPrefix.toolHeadArrow, 'Y', MaterialPrefix.bolt),
                        Map.of(), false, true)));
        SHAPED.put(ToolDefinition.UNIVERSAL_SPADE, List.of(
                tool(new String[]{"AT", "Sd"}, forms('S', MaterialPrefix.stick, 'T', MaterialPrefix.screw),
                        tools('d', ToolDefinition.SCREWDRIVER))));
        // Loader_Tools:245-247 — the rolling pin is one of the hand-written rows (CR.DEF_MIR).
        SHAPED.put(ToolDefinition.ROLLING_PIN, List.of(
                pattern(new String[]{"  S", " I ", "S f"},
                        forms('I', MaterialPrefix.ingot, 'S', MaterialPrefix.stick),
                        tools('f', ToolDefinition.FILE), true, false)));
        // Loader_Tools:225-243 — "T "/" F": a striking material above a piece of flint. The T key
        // accepts the material's gem, rock or nugget form; GTFlintAndTinderRecipe lists them.
        SHAPED.put(ToolDefinition.FLINT_AND_TINDER, List.of(
                pattern(new String[]{"T ", " F"}, forms('T', MaterialPrefix.gem),
                        Map.of(), true, false)));

        // Loader_Tools:255-290 — the hand-written early tools, one row per family, with the rock or
        // the flint itself as the material: flint (MT.Flint), bone (MT.Bone), obsidian, petrified
        // wood and every stone material (ANY.Stone.mToThis). The handle is one of GT6's handle loop
        // entries (wood stick, bamboo, bone, plastic), limited to those early handle families.
        addEarly(ToolDefinition.KNIFE, new String[]{"HX"}, flint(), true);
        addEarly(ToolDefinition.AXE, new String[]{"XX", "XH"}, flint(), true);
        addEarly(ToolDefinition.SHOVEL, new String[]{"X", "H"}, flint(), false);
        addEarly(ToolDefinition.PICKAXE, new String[]{"XXX", " H "}, flint(), false);
        addEarly(ToolDefinition.CLUB, new String[]{"  X", " X ", "H  "}, bone(), true);
        addEarly(ToolDefinition.KNIFE, new String[]{"HX"}, ofMaterial("Obsidian"), true);
        addEarly(ToolDefinition.AXE, new String[]{"XX", "XH"}, ofMaterial("Obsidian"), true);
        addEarly(ToolDefinition.SHOVEL, new String[]{"X", "H"}, ofMaterial("Obsidian"), false);
        addEarly(ToolDefinition.PICKAXE, new String[]{"XXX", " H "}, ofMaterial("Obsidian"), false);
        addEarly(ToolDefinition.AXE, new String[]{"XX", "XH"}, ofMaterial("PetrifiedWood"), true);
        addEarly(ToolDefinition.HOE, new String[]{"XX", " H"}, ofMaterial("PetrifiedWood"), true);
        addEarly(ToolDefinition.SHOVEL, new String[]{"X", "H"}, ofMaterial("PetrifiedWood"), false);
        addEarly(ToolDefinition.PICKAXE, new String[]{"XXX", " H "}, ofMaterial("PetrifiedWood"), false);
        addEarly(ToolDefinition.CLUB, new String[]{" XX", "XXX", "HX "}, ofMaterial("PetrifiedWood"), true);
        addEarly(ToolDefinition.HARD_HAMMER, new String[]{"XX ", "XXH", "XX "},
                ofMaterial("PetrifiedWood"), true);
        addEarly(ToolDefinition.AXE, new String[]{"XX", "XH"}, ofStone(), true);
        addEarly(ToolDefinition.HOE, new String[]{"XX", " H"}, ofStone(), true);
        addEarly(ToolDefinition.SHOVEL, new String[]{"X", "H"}, ofStone(), false);
        addEarly(ToolDefinition.PICKAXE, new String[]{"XXX", " H "}, ofStone(), false);
        addEarly(ToolDefinition.CLUB, new String[]{" XX", "XXX", "HX "}, ofStone(), true);
        addEarly(ToolDefinition.HARD_HAMMER, new String[]{"XX ", "XXH", "XX "}, ofStone(), true);

        // Loader_Tools:293-313 — the head recipes of the same registrations.
        HEADS.put(ToolDefinition.PICKAXE, List.of(
                head(new String[]{"PII", "f h"}, forms('P', MaterialPrefix.plate, 'I', MaterialPrefix.ingot),
                        tools('f', ToolDefinition.FILE, 'h', ToolDefinition.HARD_HAMMER)),
                head(new String[]{"CGG", "f  "}, forms('C', MaterialPrefix.plateGem, 'G', MaterialPrefix.gem),
                        tools('f', ToolDefinition.FILE))));
        HEADS.put(ToolDefinition.SHOVEL, List.of(
                head(new String[]{"fPh"}, forms('P', MaterialPrefix.plate),
                        tools('f', ToolDefinition.FILE, 'h', ToolDefinition.HARD_HAMMER)),
                head(new String[]{"fC "}, forms('C', MaterialPrefix.plateGem),
                        tools('f', ToolDefinition.FILE))));
        HEADS.put(ToolDefinition.SPADE, List.of(
                head(new String[]{"fPh", " s "}, forms('P', MaterialPrefix.plate),
                        tools('f', ToolDefinition.FILE, 'h', ToolDefinition.HARD_HAMMER, 's', ToolDefinition.SAW)),
                head(new String[]{"fC ", " s "}, forms('C', MaterialPrefix.plateGem),
                        tools('f', ToolDefinition.FILE, 's', ToolDefinition.SAW))));
        HEADS.put(ToolDefinition.AXE, List.of(
                head(new String[]{"PIh", "P  ", "f  "},
                        forms('P', MaterialPrefix.plate, 'I', MaterialPrefix.ingot),
                        tools('h', ToolDefinition.HARD_HAMMER, 'f', ToolDefinition.FILE)),
                head(new String[]{"CG ", "C  ", "f  "},
                        forms('C', MaterialPrefix.plateGem, 'G', MaterialPrefix.gem),
                        tools('f', ToolDefinition.FILE))));
        HEADS.put(ToolDefinition.DOUBLE_AXE, List.of(
                head(new String[]{"PIP", "P P", "f h"}, forms('P', MaterialPrefix.plate,
                                'I', MaterialPrefix.ingot),
                        tools('f', ToolDefinition.FILE, 'h', ToolDefinition.HARD_HAMMER)),
                head(new String[]{"CGC", "C C", "f  "}, forms('C', MaterialPrefix.plateGem,
                                'G', MaterialPrefix.gem),
                        tools('f', ToolDefinition.FILE))));
        HEADS.put(ToolDefinition.HOE, List.of(
                head(new String[]{"PIh", "f  "}, forms('P', MaterialPrefix.plate, 'I', MaterialPrefix.ingot),
                        tools('h', ToolDefinition.HARD_HAMMER, 'f', ToolDefinition.FILE)),
                head(new String[]{"CG ", "f  "}, forms('C', MaterialPrefix.plateGem, 'G', MaterialPrefix.gem),
                        tools('f', ToolDefinition.FILE))));
        HEADS.put(ToolDefinition.SWORD, List.of(
                head(new String[]{" P ", "fPh"}, forms('P', MaterialPrefix.plate),
                        tools('f', ToolDefinition.FILE, 'h', ToolDefinition.HARD_HAMMER)),
                head(new String[]{" C ", "fC "}, forms('C', MaterialPrefix.plateGem),
                        tools('f', ToolDefinition.FILE))));
        HEADS.put(ToolDefinition.HARD_HAMMER, List.of(
                head(new String[]{"II ", "IIh", "II "}, forms('I', MaterialPrefix.ingot),
                        tools('h', ToolDefinition.HARD_HAMMER)),
                head(new String[]{"GG ", "GGf", "GG "}, forms('G', MaterialPrefix.gem),
                        tools('f', ToolDefinition.FILE))));
        HEADS.put(ToolDefinition.SOFT_HAMMER, List.of(
                head(new String[]{"II ", "IIr", "II "}, forms('I', MaterialPrefix.ingot),
                        tools('r', ToolDefinition.SOFT_HAMMER)),
                head(new String[]{"GG ", "GGr", "GG "}, forms('G', MaterialPrefix.gem),
                        tools('r', ToolDefinition.SOFT_HAMMER))));
        HEADS.put(ToolDefinition.FILE, List.of(
                head(new String[]{" P ", " Pk"}, forms('P', MaterialPrefix.plate),
                        tools('k', ToolDefinition.KNIFE))));
        HEADS.put(ToolDefinition.SAW, List.of(
                head(new String[]{"PP", "fh"}, forms('P', MaterialPrefix.plate),
                        tools('f', ToolDefinition.FILE, 'h', ToolDefinition.HARD_HAMMER)),
                head(new String[]{"CC", "f "}, forms('C', MaterialPrefix.plateGem),
                        tools('f', ToolDefinition.FILE))));
        HEADS.put(ToolDefinition.CHISEL, List.of(
                head(new String[]{"hPf", " S "}, forms('P', MaterialPrefix.plate, 'S', MaterialPrefix.stick),
                        tools('h', ToolDefinition.HARD_HAMMER, 'f', ToolDefinition.FILE)),
                head(new String[]{"Cf", "S "}, forms('C', MaterialPrefix.plateGem, 'S', MaterialPrefix.stick),
                        tools('f', ToolDefinition.FILE))));
        HEADS.put(ToolDefinition.SCREWDRIVER, List.of(
                head(new String[]{"hS", "Sf"}, forms('S', MaterialPrefix.stick),
                        tools('h', ToolDefinition.HARD_HAMMER, 'f', ToolDefinition.FILE))));
        HEADS.put(ToolDefinition.SENSE, List.of(
                head(new String[]{"PPI", "f h"}, forms('P', MaterialPrefix.plate, 'I', MaterialPrefix.ingot),
                        tools('f', ToolDefinition.FILE, 'h', ToolDefinition.HARD_HAMMER)),
                head(new String[]{"CCG", "f  "}, forms('C', MaterialPrefix.plateGem, 'G', MaterialPrefix.gem),
                        tools('f', ToolDefinition.FILE))));
        HEADS.put(ToolDefinition.PLOW, List.of(
                head(new String[]{"PPP", "PPP", "f h"}, forms('P', MaterialPrefix.plate),
                        tools('f', ToolDefinition.FILE, 'h', ToolDefinition.HARD_HAMMER)),
                head(new String[]{"CCC", "CCC", "f  "}, forms('C', MaterialPrefix.plateGem),
                        tools('f', ToolDefinition.FILE))));
        HEADS.put(ToolDefinition.CONSTRUCTION_PICK, List.of(
                head(new String[]{"PIP", "f h"}, forms('P', MaterialPrefix.plate, 'I', MaterialPrefix.ingot),
                        tools('f', ToolDefinition.FILE, 'h', ToolDefinition.HARD_HAMMER)),
                head(new String[]{"CGC", "f  "}, forms('C', MaterialPrefix.plateGem, 'G', MaterialPrefix.gem),
                        tools('f', ToolDefinition.FILE))));
        HEADS.put(ToolDefinition.BUILDER_WAND, List.of(
                head(new String[]{" P ", "f h", " s "}, forms('P', MaterialPrefix.plate),
                        tools('f', ToolDefinition.FILE, 'h', ToolDefinition.HARD_HAMMER, 's', ToolDefinition.SAW)),
                head(new String[]{" C ", "f h", " s "}, forms('C', MaterialPrefix.plateGem),
                        tools('f', ToolDefinition.FILE, 'h', ToolDefinition.HARD_HAMMER, 's', ToolDefinition.SAW)),
                head(new String[]{" G ", "f h", " s "}, forms('G', MaterialPrefix.gem),
                        tools('f', ToolDefinition.FILE, 'h', ToolDefinition.HARD_HAMMER, 's', ToolDefinition.SAW))));
        // Loader_Tools:298 — GT6 also writes a head row for the wrench; the port's wrench is headless
        // (its tool row builds it straight from plates), so the head is only reachable through this row.
        HEADS.put(ToolDefinition.WRENCH, List.of(
                head(new String[]{"hPW", "PVP", "WPd"},
                        forms('P', MaterialPrefix.plate, 'W', MaterialPrefix.screw, 'V', MaterialPrefix.ring),
                        tools('h', ToolDefinition.HARD_HAMMER, 'd', ToolDefinition.SCREWDRIVER)),
                head(new String[]{"hCW", "CVC", "WCd"},
                        forms('C', MaterialPrefix.plateGem, 'W', MaterialPrefix.screw,
                                'V', MaterialPrefix.ring),
                        tools('h', ToolDefinition.HARD_HAMMER, 'd', ToolDefinition.SCREWDRIVER))));
    }

    // ── access ────────────────────────────────────────────────────────────

    /** Appends one hand-written early row to a tool's pattern list. */
    private static void addEarly(ToolDefinition type, String[] rows, Gate gate, boolean mirror) {
        List<Pattern> patterns = new ArrayList<>(SHAPED.getOrDefault(type, List.of()));
        patterns.add(early(rows, rock(), mirror, gate));
        SHAPED.put(type, List.copyOf(patterns));
    }

    /** GT6's shaped rows for the one-piece tools, in registration order. */
    public static List<Pattern> shaped(ToolDefinition type) {
        return SHAPED.getOrDefault(type, List.of());
    }

    /** GT6's shaped head rows ({@code mToolHeadRecipes}), in registration order. */
    public static List<Pattern> heads(ToolDefinition type) {
        return HEADS.getOrDefault(type, List.of());
    }

    /**
     * The form a tool's head is made of. Every tool with a head uses its own prefix; the wrench is
     * headless in the port but GT6 registers a head row for it, so the prefix is stated here.
     */
    public static MaterialPrefix headPrefix(ToolDefinition type) {
        if (type.headPrefix() != null) return type.headPrefix();
        if (type == ToolDefinition.WRENCH) return MaterialPrefix.toolHeadWrench;
        return null;
    }

    /** Every tool with a shaped GT6 recipe. */
    public static Map<ToolDefinition, List<Pattern>> allShaped() {
        return Map.copyOf(SHAPED);
    }

    /** Every tool whose head has a shaped GT6 recipe. */
    public static Map<ToolDefinition, List<Pattern>> allHeads() {
        return Map.copyOf(HEADS);
    }

    /** Loader_Tools' special objects are independent ingredients, not the tool's metal. */
    public static String specialItem(ToolDefinition type, char letter) {
        if (letter != 'V') return null;
        return switch (type) {
            case CROWBAR -> "minecraft:blue_dye";
            case SCOOP -> "#minecraft:wool";
            default -> null;
        };
    }

    public static String specialMaterial(ToolDefinition type, char letter) {
        if (type == ToolDefinition.PLUNGER && letter == 'V') return "Rubber";
        if (type == ToolDefinition.WRENCH && (letter == 'V' || letter == 'W')) return "Steel";
        return null;
    }

    /**
     * Whether GT6 registers the head + handle (shapeless {@code AdvancedCraftingTool}) recipe for this
     * tool. Head-based tools keep that row even when they also have shaped early rows — a pickaxe can
     * come from a head plus a stick <em>or</em> straight from a rock.
     */
    public static boolean isHeadAssembly(ToolDefinition type) {
        return type.requiresHeadAssembly();
    }

    /** The vanilla item a pattern letter stands for, or null when it is a material form. */

    public static String vanilla(char letter) {
        return letter == 'F' ? "minecraft:flint" : null;
    }
}
