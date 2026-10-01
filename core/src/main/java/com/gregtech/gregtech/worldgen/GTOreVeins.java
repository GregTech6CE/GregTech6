package com.gregtech.gregtech.worldgen;

import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.api.material.GTMaterial;

import java.util.List;

/**
 * Code-defined ore generation tables, ported 1:1 from GT6 {@code gregtech/loaders/b/Loader_Worldgen.java}
 * (the {@code ORE_OVERWORLD} large veins and {@code GEN_OVERWORLD} small ores).
 *
 * <p>Large veins ({@code WorldgenOresLarge}): layered blobs with four ore materials —
 * bottom layer (3 blocks tall), top layer (3 blocks tall), a "between" layer in the middle
 * and a "spread" ore scattered over the full 7-block height. One vein type is picked per
 * 3x3-chunk grid cell, weighted by {@link OreVein#weight}.
 *
 * <p>Small ores ({@code WorldgenOresSmall}): individual blocks scattered in every chunk,
 * roughly {@link SmallOre#amount} per chunk within the Y range.
 */
public final class GTOreVeins {
    private GTOreVeins() {}

    /** GT6 {@code WorldgenOresLarge} parameters (Y values are pre-1.18 absolute heights, still valid in 1.20.1). */
    public record OreVein(String name, int minY, int maxY, int weight, int density, int size,
                          GTMaterial top, GTMaterial bottom, GTMaterial between, GTMaterial spread) {}

    /** GT6 {@code WorldgenOresSmall} parameters. */
    public record SmallOre(String name, int minY, int maxY, int amount, GTMaterial material) {}

    /**
     * All GT6 large veins flagged {@code ORE_OVERWORLD}, in original order (Loader_Worldgen
     * lines 885-915), which is exactly GT6's registration order.
     *
     * <p>§51 added the five veins the port used to skip — {@code lignite}, {@code coal},
     * {@code bauxite}, {@code iodinesalt} and {@code rocksalt}. They were dropped early on with the
     * note "those resources generate as whole-block seam layers instead", but GT6 has <b>both</b>:
     * the same resources also appear as whole-block seams in its stone layers ({@code WorldgenStone}
     * / {@code BlockRockOres}, which the port keeps in {@link GTStoneLayersGen}), so skipping the
     * veins lost the vein half of GT6's generation.
     */
    public static final List<OreVein> OVERWORLD_VEINS = List.of(
        //          name             minY maxY weight den size  top                          bottom                       between                      spread
        new OreVein("lignite",        50, 130, 160, 8, 32, Materials.Lignite,                  Materials.Lignite,                  Materials.Lignite,                  Materials.Coal),
        new OreVein("coal",           50,  80,  80, 6, 32, Materials.Coal,                     Materials.Coal,                     Materials.Coal,                     Materials.Lignite),
        new OreVein("apatite",        40,  60,  60, 3, 16, Materials.Apatite,                  Materials.Apatite,                  Materials.BluePhosphorus,           Materials.Phosphate),
        new OreVein("lapis",          20,  50,  40, 5, 16, Materials.Lazurite,                 Materials.Sodalite,                 Materials.Lapis,                    Materials.Azurite),
        new OreVein("bauxite",        50,  90,  80, 4, 24, com.gregtech.gregtech.content.material.generated.OreMaterials.Bauxite,         com.gregtech.gregtech.content.material.generated.OreMaterials.Bauxite,         com.gregtech.gregtech.content.material.generated.OreMaterials.Bauxite,         com.gregtech.gregtech.content.material.generated.OreMaterials.Ilmenite),
        new OreVein("iodinesalt",     50,  60,  30, 3, 24, Materials.IodineSalt,               Materials.Salt,                     com.gregtech.gregtech.content.material.generated.OreMaterials.Borax,           com.gregtech.gregtech.content.material.generated.OreMaterials.Zeolite),
        new OreVein("rocksalt",       50,  60,  30, 3, 24, Materials.Sylvite,                  com.gregtech.gregtech.content.material.generated.OreMaterials.Coltan,          com.gregtech.gregtech.content.material.generated.OreMaterials.Lepidolite,      com.gregtech.gregtech.content.material.generated.OreMaterials.Spodumene),
        new OreVein("asbestos",       10,  40,  30, 3, 16, com.gregtech.gregtech.content.material.generated.OreMaterials.Chromite,         Materials.Talc,                     Materials.Gypsum,                   Materials.Asbestos),
        new OreVein("sapphire",       10,  40,  30, 3, 16, Materials.BlueSapphire,             Materials.OrangeSapphire,           Materials.YellowSapphire,           Materials.Ruby),
        new OreVein("sapphire2",      10,  40,  30, 3, 16, Materials.GreenSapphire,            Materials.Ruby,                     Materials.BlueSapphire,             Materials.PurpleSapphire),
        new OreVein("garnet",         10,  40,  60, 3, 16, Materials.Almandine,                Materials.Pyrope,                   Materials.Andradite,                Materials.Uvarovite),
        new OreVein("pitchblende",    10,  40,  40, 3, 16, com.gregtech.gregtech.content.material.generated.OreMaterials.Pitchblende,      com.gregtech.gregtech.content.material.generated.OreMaterials.Pitchblende,      com.gregtech.gregtech.content.material.generated.OreMaterials.Uraninite,        com.gregtech.gregtech.content.material.generated.OreMaterials.Uraninite),
        new OreVein("monazite",       20,  40,  30, 3, 16, com.gregtech.gregtech.content.material.generated.OreMaterials.Bastnasite,       com.gregtech.gregtech.content.material.generated.OreMaterials.Bastnasite,       Materials.Monazite,                 Materials.Neodymium),
        new OreVein("diamond",         5,  20,  40, 2, 16, Materials.Graphite,                 Materials.Graphite,                 Materials.Diamond,                  Materials.Graphite),
        new OreVein("galena",         30,  60,  40, 5, 16, com.gregtech.gregtech.content.material.generated.OreMaterials.Galena,           com.gregtech.gregtech.content.material.generated.OreMaterials.Galena,           Materials.Silver,                       Materials.Lead),
        new OreVein("quartz",         40,  80,  60, 3, 16, Materials.MilkyQuartz,              com.gregtech.gregtech.content.material.generated.OreMaterials.Barite,           Materials.CertusQuartz,             Materials.CertusQuartz),
        new OreVein("peridot",        10,  40,  60, 3, 16, com.gregtech.gregtech.content.material.generated.OreMaterials.Kyanite,          Materials.MagnesiumCarbonate,                    Materials.Peridot,                  com.gregtech.gregtech.content.material.generated.OreMaterials.Glauconite),
        new OreVein("gold",           20,  30,   5, 3, 16, Materials.Pyrite,                   com.gregtech.gregtech.content.material.generated.OreMaterials.Chalcopyrite,     com.gregtech.gregtech.content.material.generated.OreMaterials.Arsenopyrite,     Materials.Gold),
        new OreVein("platinum",       40,  50,   5, 3, 16, com.gregtech.gregtech.content.material.generated.OreMaterials.Cooperite,        Materials.Palladium,                       com.gregtech.gregtech.content.material.generated.OreMaterials.Sperrylite,       Materials.Iridium),
        new OreVein("molybdenum",     20,  50,   5, 3, 16, com.gregtech.gregtech.content.material.generated.OreMaterials.Wulfenite,        com.gregtech.gregtech.content.material.generated.OreMaterials.Molybdenite,      Materials.Molybdenum,                       com.gregtech.gregtech.content.material.generated.OreMaterials.Powellite),
        new OreVein("cassiterite",    40,  90, 170, 5, 24, com.gregtech.gregtech.content.material.generated.OreMaterials.Stannite,         com.gregtech.gregtech.content.material.generated.OreMaterials.Kesterite,        com.gregtech.gregtech.content.material.generated.OreMaterials.Huebnerite,       com.gregtech.gregtech.content.material.generated.OreMaterials.Cassiterite),
        new OreVein("tungstate",      20,  50,  10, 3, 16, com.gregtech.gregtech.content.material.generated.OreMaterials.Scheelite,        com.gregtech.gregtech.content.material.generated.OreMaterials.Russellite,       com.gregtech.gregtech.content.material.generated.OreMaterials.Tungstate,        com.gregtech.gregtech.content.material.generated.OreMaterials.Pinalite),
        new OreVein("manganese",      20,  30,  20, 3, 16, Materials.Grossular,                Materials.Spessartine,              Materials.Pyrolusite,                     com.gregtech.gregtech.content.material.generated.OreMaterials.Coltan),
        new OreVein("beryllium",       5,  30,  15, 3, 16, Materials.Aquamarine,               Materials.Maxixe,                   Materials.Emerald,                  Materials.Thorium),
        new OreVein("beryllium2",      5,  30,  15, 3, 16, Materials.Bixbite,                  Materials.Goshenite,                Materials.Heliodor,                 Materials.Morganite),
        new OreVein("titanium",       10,  40,  40, 3, 16, Materials.Rutile,                     Materials.Rutile,                     Materials.Zircon,                   com.gregtech.gregtech.content.material.generated.OreMaterials.Ilmenite),
        new OreVein("nickel",         10,  40,  40, 3, 16, com.gregtech.gregtech.content.material.generated.OreMaterials.Garnierite,       Materials.Nickel,                       com.gregtech.gregtech.content.material.generated.OreMaterials.Cobaltite,        com.gregtech.gregtech.content.material.generated.OreMaterials.Pentlandite),
        new OreVein("redstone",       10,  40,  60, 3, 24, Materials.Redstone,                 Materials.Redstone,                 Materials.Ruby,                     com.gregtech.gregtech.content.material.generated.OreMaterials.Cinnabar),
        new OreVein("tetrahedrite",   70, 120, 150, 4, 24, com.gregtech.gregtech.content.material.generated.OreMaterials.Tetrahedrite,     com.gregtech.gregtech.content.material.generated.OreMaterials.Tetrahedrite,     Materials.Copper,                       com.gregtech.gregtech.content.material.generated.OreMaterials.Stibnite),
        new OreVein("iron",           10,  40, 120, 4, 24, com.gregtech.gregtech.content.material.generated.OreMaterials.BrownLimonite,    com.gregtech.gregtech.content.material.generated.OreMaterials.YellowLimonite,   Materials.Hematite,                    com.gregtech.gregtech.content.material.generated.OreMaterials.Malachite),
        new OreVein("copper",         10,  30,  80, 4, 24, com.gregtech.gregtech.content.material.generated.OreMaterials.Chalcopyrite,     Materials.Hematite,                    Materials.Pyrite,                   Materials.Copper)
    );

    /** Sum of all overworld vein weights (GT6 picks {@code random.nextInt(totalWeight)} per grid cell). */
    public static final int TOTAL_VEIN_WEIGHT = OVERWORLD_VEINS.stream().mapToInt(OreVein::weight).sum();

    /**
     * All GT6 small ores flagged {@code GEN_OVERWORLD} (Loader_Worldgen lines 800-847).
     * Mod-compat entries (Thaumcraft/AE2/IHL/HEX/... gated ores when the other mod is absent) are
     * omitted; §50 added the GT6 {@code ore.small.eudialyte} entry, which was skipped before because
     * the port had no Eudialyte material yet (§32 added it).
     */
    public static final List<SmallOre> OVERWORLD_SMALL_ORES = List.of(
        //           name            minY maxY amt  material
        new SmallOre("copper",        60, 120, 16, Materials.Copper),
        new SmallOre("chalcopyrite",  60, 120, 16, com.gregtech.gregtech.content.material.generated.OreMaterials.Chalcopyrite),
        new SmallOre("malachite",     40,  70,  8, com.gregtech.gregtech.content.material.generated.OreMaterials.Malachite),
        new SmallOre("tin",           60, 120, 16, Materials.Tin),
        new SmallOre("cassiterite",   60, 120, 16, com.gregtech.gregtech.content.material.generated.OreMaterials.Cassiterite),
        new SmallOre("zinc",          40,  70,  4, Materials.Zinc),
        new SmallOre("sphalerite",    30,  60, 12, com.gregtech.gregtech.content.material.generated.OreMaterials.Sphalerite),
        new SmallOre("smithsonite",   30,  60,  2, com.gregtech.gregtech.content.material.generated.OreMaterials.Smithsonite),
        new SmallOre("stibnite",      20,  40,  2, com.gregtech.gregtech.content.material.generated.OreMaterials.Stibnite),
        new SmallOre("bismuth",       80, 120,  8, Materials.Bismuth),
        new SmallOre("lead",          40,  80, 16, Materials.Lead),
        new SmallOre("galena",        40,  80, 16, com.gregtech.gregtech.content.material.generated.OreMaterials.Galena),
        new SmallOre("silver",        20,  40,  4, Materials.Silver),
        new SmallOre("gold",          20,  40,  4, Materials.Gold),
        new SmallOre("pyrite",        20,  40,  4, Materials.Pyrite),
        new SmallOre("hematite",      40,  80, 24, Materials.Hematite),
        new SmallOre("pyrolusite",    20,  40,  4, Materials.Pyrolusite),
        new SmallOre("garnierite",    20,  40,  4, com.gregtech.gregtech.content.material.generated.OreMaterials.Garnierite),
        new SmallOre("pentlandite",   20,  40,  4, com.gregtech.gregtech.content.material.generated.OreMaterials.Pentlandite),
        new SmallOre("scheelite",      5,  50,  1, com.gregtech.gregtech.content.material.generated.OreMaterials.Scheelite),
        new SmallOre("salt",          40,  80,  6, Materials.Salt),
        new SmallOre("rocksalt",      40,  80,  6, Materials.Sylvite),
        new SmallOre("borax",         10,  40,  4, com.gregtech.gregtech.content.material.generated.OreMaterials.Borax),
        new SmallOre("asbestos",      20,  40,  8, Materials.Asbestos),
        new SmallOre("diamond",        5,  10,  2, Materials.Diamond),
        new SmallOre("amber",          5,  70,  1, Materials.Amber),
        new SmallOre("craponite",      5, 250,  2, Materials.Craponite),
        new SmallOre("redstone",       5,  20, 16, Materials.Redstone),
        new SmallOre("lapis",         20,  40,  8, Materials.Lapis),
        new SmallOre("eudialyte",     20,  40,  4, com.gregtech.gregtech.content.material.generated.CompoundMaterials.Eudialyte),
        new SmallOre("azurite",       20,  40,  4, Materials.Azurite),
        new SmallOre("coal",          40, 100, 36, Materials.Coal),
        new SmallOre("graphite",       5,  10,  2, Materials.Graphite),
        new SmallOre("pollucite",      1, 250,  1, com.gregtech.gregtech.content.material.generated.OreMaterials.Pollucite),
        new SmallOre("zeolite",        1, 250,  1, com.gregtech.gregtech.content.material.generated.OreMaterials.Zeolite),
        new SmallOre("sulfur",         5,  15,  8, Materials.Sulfur)
    );

    // ── The End (GT6 ORE_END large veins; platinum/molybdenum/cassiterite are shared with the
    //    overworld, naquadah/trinium are End-exclusive) ──

    /**
     * GT6's {@code ORE_END} veins in registration order. §51 added the three shared ones
     * (platinum, molybdenum, cassiterite) — they carry exactly the overworld parameters.
     */
    public static final List<OreVein> END_VEINS = List.of(
        new OreVein("platinum",       40,  50,   5, 3, 16, com.gregtech.gregtech.content.material.generated.OreMaterials.Cooperite,        Materials.Palladium,                       com.gregtech.gregtech.content.material.generated.OreMaterials.Sperrylite,       Materials.Iridium),
        new OreVein("molybdenum",     20,  50,   5, 3, 16, com.gregtech.gregtech.content.material.generated.OreMaterials.Wulfenite,        com.gregtech.gregtech.content.material.generated.OreMaterials.Molybdenite,      Materials.Molybdenum,                       com.gregtech.gregtech.content.material.generated.OreMaterials.Powellite),
        new OreVein("cassiterite",    40,  90, 170, 5, 24, com.gregtech.gregtech.content.material.generated.OreMaterials.Stannite,         com.gregtech.gregtech.content.material.generated.OreMaterials.Kesterite,        com.gregtech.gregtech.content.material.generated.OreMaterials.Huebnerite,       com.gregtech.gregtech.content.material.generated.OreMaterials.Cassiterite),
        new OreVein("naquadah",       10,  60,  10, 4, 32, Materials.Naquadah, Materials.Naquadah, Materials.Naquadah, Materials.Naquadah),
        new OreVein("trinium",        10,  90, 100, 1, 12, Materials.Trinium, Materials.Trinium, Materials.Trinium, Materials.Trinium)
    );
    public static final int TOTAL_END_VEIN_WEIGHT = END_VEINS.stream().mapToInt(OreVein::weight).sum();

    /** GT6 small ores flagged {@code GEN_NETHER}. */
    public static final List<SmallOre> NETHER_SMALL_ORES = List.of(
        new SmallOre("cassiterite",   60, 120, 16, com.gregtech.gregtech.content.material.generated.OreMaterials.Cassiterite),
        new SmallOre("bismuth",       80, 120,  8, Materials.Bismuth),
        new SmallOre("gold",          20,  40,  4, Materials.Gold),
        new SmallOre("scheelite",      5,  50,  1, com.gregtech.gregtech.content.material.generated.OreMaterials.Scheelite),
        new SmallOre("salt",          40,  80,  6, Materials.Salt),
        new SmallOre("rocksalt",      40,  80,  6, Materials.Sylvite),
        new SmallOre("borax",         10,  40,  4, com.gregtech.gregtech.content.material.generated.OreMaterials.Borax),
        new SmallOre("asbestos",      20,  40,  8, Materials.Asbestos),
        new SmallOre("diamond",        5,  10,  2, Materials.Diamond),
        new SmallOre("craponite",      5, 250,  2, Materials.Craponite),
        new SmallOre("redstone",       5,  20, 16, Materials.Redstone),
        new SmallOre("graphite",       5,  10,  2, Materials.Graphite),
        new SmallOre("pollucite",      1, 250,  1, com.gregtech.gregtech.content.material.generated.OreMaterials.Pollucite),
        new SmallOre("zeolite",        1, 250,  1, com.gregtech.gregtech.content.material.generated.OreMaterials.Zeolite),
        new SmallOre("coltan",         1, 250,  4, com.gregtech.gregtech.content.material.generated.OreMaterials.Coltan),
        new SmallOre("niter",         10, 120, 32, Materials.Niter),
        new SmallOre("efrine",        90, 120,  8, Materials.Efrine),
        new SmallOre("cinnabar",       5, 250, 16, com.gregtech.gregtech.content.material.generated.OreMaterials.Cinnabar)
    );

    /** GT6 small ores flagged {@code GEN_END} (incl. End-exclusive endium/sugilite/naquadah/trinium). */
    public static final List<SmallOre> END_SMALL_ORES = List.of(
        new SmallOre("copper",        60, 120, 16, Materials.Copper),
        new SmallOre("chalcopyrite",  60, 120, 16, com.gregtech.gregtech.content.material.generated.OreMaterials.Chalcopyrite),
        new SmallOre("malachite",     40,  70,  8, com.gregtech.gregtech.content.material.generated.OreMaterials.Malachite),
        new SmallOre("tin",           60, 120, 16, Materials.Tin),
        new SmallOre("cassiterite",   60, 120, 16, com.gregtech.gregtech.content.material.generated.OreMaterials.Cassiterite),
        new SmallOre("zinc",          40,  70,  4, Materials.Zinc),
        new SmallOre("sphalerite",    30,  60, 12, com.gregtech.gregtech.content.material.generated.OreMaterials.Sphalerite),
        new SmallOre("smithsonite",   30,  60,  2, com.gregtech.gregtech.content.material.generated.OreMaterials.Smithsonite),
        new SmallOre("stibnite",      20,  40,  2, com.gregtech.gregtech.content.material.generated.OreMaterials.Stibnite),
        new SmallOre("lead",          40,  80, 16, Materials.Lead),
        new SmallOre("galena",        40,  80, 16, com.gregtech.gregtech.content.material.generated.OreMaterials.Galena),
        new SmallOre("silver",        20,  40,  4, Materials.Silver),
        new SmallOre("gold",          20,  40,  4, Materials.Gold),
        new SmallOre("pyrite",        20,  40,  4, Materials.Pyrite),
        new SmallOre("hematite",      40,  80, 24, Materials.Hematite),
        new SmallOre("pyrolusite",    20,  40,  4, Materials.Pyrolusite),
        new SmallOre("garnierite",    20,  40,  4, com.gregtech.gregtech.content.material.generated.OreMaterials.Garnierite),
        new SmallOre("pentlandite",   20,  40,  4, com.gregtech.gregtech.content.material.generated.OreMaterials.Pentlandite),
        new SmallOre("scheelite",      5,  50,  1, com.gregtech.gregtech.content.material.generated.OreMaterials.Scheelite),
        new SmallOre("salt",          40,  80,  6, Materials.Salt),
        new SmallOre("rocksalt",      40,  80,  6, Materials.Sylvite),
        new SmallOre("borax",         10,  40,  4, com.gregtech.gregtech.content.material.generated.OreMaterials.Borax),
        new SmallOre("craponite",      5, 250,  2, Materials.Craponite),
        new SmallOre("coltan",         1, 250,  4, com.gregtech.gregtech.content.material.generated.OreMaterials.Coltan),
        new SmallOre("platinum",      20,  40,  6, Materials.Platinum),
        new SmallOre("iridium",       20,  40,  6, Materials.Iridium),
        new SmallOre("sperrylite",    20,  40,  4, com.gregtech.gregtech.content.material.generated.OreMaterials.Sperrylite),
        new SmallOre("cooperite",     20,  40,  4, com.gregtech.gregtech.content.material.generated.OreMaterials.Cooperite),
        new SmallOre("naquadah",      10,  80,  6, Materials.Naquadah),
        new SmallOre("trinium",       10,  80, 12, Materials.Trinium),
        new SmallOre("endium",        10,  80, 32, Materials.Endium),
        new SmallOre("sugilite",      10,  80, 16, Materials.Sugilite)
    );
}
