package com.gregtech.gregtech.data;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.api.prefix.PrefixRegistry;
import com.gregtech.gregtech.data.generated.MaterialForms;
import com.gregtech.gregtech.content.material.generated.WoodMaterials;

import java.util.function.Predicate;

/**
 * Item form prefix, analogous to GregTech {@code OreDictPrefix}.
 * <p>
 * Prefixes registered in GT6 {@code Loader_Items} are declared here in dependency order
 * (parent prefixes before children).
 */
public final class MaterialPrefix {
    // --- dust / ore processing ---
    public static final MaterialPrefix dust;
    public static final MaterialPrefix dustSmall;
    public static final MaterialPrefix dustTiny;
    public static final MaterialPrefix dustDiv72;
    public static final MaterialPrefix dustImpure;
    public static final MaterialPrefix unit;
    public static final MaterialPrefix crushed;
    public static final MaterialPrefix crushedTiny;
    public static final MaterialPrefix crushedPurified;
    public static final MaterialPrefix crushedPurifiedTiny;
    public static final MaterialPrefix crushedCentrifuged;
    public static final MaterialPrefix crushedCentrifugedTiny;
    public static final MaterialPrefix rockGt;

    // --- gems ---
    public static final MaterialPrefix gemChipped;
    public static final MaterialPrefix gemFlawed;
    public static final MaterialPrefix gem;
    public static final MaterialPrefix gemFlawless;
    public static final MaterialPrefix gemExquisite;
    public static final MaterialPrefix gemLegendary;
    public static final MaterialPrefix bouleGt;

    // --- ingots ---
    public static final MaterialPrefix nugget;
    public static final MaterialPrefix chunkGt;
    public static final MaterialPrefix billet;
    public static final MaterialPrefix ingot;
    public static final MaterialPrefix ingotHot;
    public static final MaterialPrefix ingotDouble;
    public static final MaterialPrefix ingotTriple;
    public static final MaterialPrefix ingotQuadruple;
    public static final MaterialPrefix ingotQuintuple;

    // --- plates / foil ---
    public static final MaterialPrefix plateGemTiny;
    public static final MaterialPrefix plateGem;
    public static final MaterialPrefix plateTiny;
    public static final MaterialPrefix plate;
    public static final MaterialPrefix plateDouble;
    public static final MaterialPrefix plateTriple;
    public static final MaterialPrefix plateQuadruple;
    public static final MaterialPrefix plateQuintuple;
    public static final MaterialPrefix plateDense;
    public static final MaterialPrefix plateCurved;
    public static final MaterialPrefix foil;

    // --- misc forms ---
    public static final MaterialPrefix scrapGt;
    /** GT6 {@code OP.oreDense}: material identity for the existing crystal/rock ore blocks.
     * No standalone item is generated; the actual block items carry this prefix. */
    public static final MaterialPrefix oreDense;
    /** GT6 {@code OP.oreVanillastone}: the 16 vanilla-style ore block items, each 2U. */
    public static final MaterialPrefix oreVanillastone;
    public static final MaterialPrefix oreRaw;

    // --- rods / bolts / gears ---
    public static final MaterialPrefix stick;
    public static final MaterialPrefix stickLong;
    public static final MaterialPrefix bolt;
    public static final MaterialPrefix screw;
    public static final MaterialPrefix gearGt;
    public static final MaterialPrefix gearGtSmall;
    public static final MaterialPrefix ring;
    public static final MaterialPrefix chain;
    public static final MaterialPrefix spring;
    public static final MaterialPrefix springSmall;

    // --- machine parts ---
    public static final MaterialPrefix rotor;
    public static final MaterialPrefix lens;
    public static final MaterialPrefix round;
    public static final MaterialPrefix itemCasing;
    public static final MaterialPrefix wireFine;
    public static final MaterialPrefix minecartWheels;
    public static final MaterialPrefix railGt;

    // --- plants ---
    public static final MaterialPrefix plantGtBerry;
    public static final MaterialPrefix plantGtBlossom;
    public static final MaterialPrefix plantGtFiber;
    public static final MaterialPrefix plantGtTwig;
    public static final MaterialPrefix plantGtWart;

    // --- containers ---
    public static final MaterialPrefix glasstube;

    /**
     * GT6's coin form.
     *
     * <p>GT6 has no coin <em>prefix item</em> at all: {@code OP.coin} is declared
     * {@code unused("coin").setCategoryName("Coins")} ({@code gregapi/data/OP.java:473}), and a coin is
     * the placeable multi-tile {@code MultiTileEntityCoin} (id 32700, registered in
     * {@code Loader_MultiTileEntities.java:2240}) whose item stack carries the material in
     * {@code gt.material}, a flag in {@code gt.coin.unique} and the two 16x16 pixel-shape bitsets
     * {@code gt.coin.shape.0.<i>}/{@code gt.coin.shape.1.<i>} ({@code MultiTileEntityCoin.java:79-93}).
     * GT6 hands those stacks out through its {@code COIN_MAP} ({@code :124-133}, one coin per metal).
     * The port has no multi-tile items, so it registers one coin item per material like every other
     * form - {@code MaterialPrefixes.coin} keeps GT6's {@code OP.coin} entry and now resolves to this
     * prefix instead of staying delegateless.</p>
     */
    public static final MaterialPrefix coin;

    // --- tool heads ---
    public static final MaterialPrefix toolHeadSword;
    public static final MaterialPrefix toolHeadRawSword;
    public static final MaterialPrefix toolHeadPickaxe;
    public static final MaterialPrefix toolHeadRawPickaxe;
    public static final MaterialPrefix toolHeadPickaxeGem;
    public static final MaterialPrefix toolHeadConstructionPickaxe;
    public static final MaterialPrefix toolHeadBuilderwand;
    public static final MaterialPrefix toolHeadShovel;
    public static final MaterialPrefix toolHeadRawShovel;
    public static final MaterialPrefix toolHeadSpade;
    public static final MaterialPrefix toolHeadRawSpade;
    public static final MaterialPrefix toolHeadAxe;
    public static final MaterialPrefix toolHeadRawAxe;
    public static final MaterialPrefix toolHeadAxeDouble;
    public static final MaterialPrefix toolHeadRawAxeDouble;
    public static final MaterialPrefix toolHeadHoe;
    public static final MaterialPrefix toolHeadRawHoe;
    public static final MaterialPrefix toolHeadHammer;
    public static final MaterialPrefix toolHeadFile;
    public static final MaterialPrefix toolHeadChisel;
    public static final MaterialPrefix toolHeadRawChisel;
    public static final MaterialPrefix toolHeadSaw;
    public static final MaterialPrefix toolHeadRawSaw;
    public static final MaterialPrefix toolHeadDrill;
    public static final MaterialPrefix toolHeadChainsaw;
    public static final MaterialPrefix toolHeadWrench;
    public static final MaterialPrefix toolHeadScrewdriver;
    public static final MaterialPrefix toolHeadUniversalSpade;
    public static final MaterialPrefix toolHeadRawUniversalSpade;
    public static final MaterialPrefix toolHeadSense;
    public static final MaterialPrefix toolHeadRawSense;
    public static final MaterialPrefix toolHeadPlow;
    public static final MaterialPrefix toolHeadRawPlow;
    public static final MaterialPrefix toolHeadBuzzSaw;
    public static final MaterialPrefix toolHeadArrow;
    public static final MaterialPrefix toolHeadRawArrow;

    // --- projectiles ---
    public static final MaterialPrefix arrowGtWood;
    public static final MaterialPrefix arrowGtPlastic;
    public static final MaterialPrefix bulletGtSmall;
    public static final MaterialPrefix bulletGtMedium;
    public static final MaterialPrefix bulletGtLarge;

    // GT6 OP.java:158/165: `dust = Or(DIRTY_DUSTS, DUSTS)` and `dustImpure = DIRTY_DUSTS`. `DUSTS` is
    // live (every G_WOOD/G_GEM/G_INGOT material carries it), `DIRTY_DUSTS` is **dead** — declared in
    // `TD.java:577` but used by no material in `MT.java`, so the original registers no impure dusts at
    // all. The port therefore consults only `DUSTS` here and keeps its own impure dusts (they drive the
    // port's ore-processing chain); see §23 of docs/PORTING_REMAINING_2026-09-14.md.
    private static final Predicate<GTMaterial> HAS_DUST = m ->
            m.hasAny(MaterialProperty.DUST, MaterialProperty.GENERATE_DUST, MaterialProperty.ELEMENT)
                    || MaterialForms.has(m, "DUSTS");
    private static final Predicate<GTMaterial> HAS_ORE_PROCESSING = m ->
            m.hasAny(MaterialProperty.ORE, MaterialProperty.GENERATE_ORE, MaterialProperty.GENERATE_ORE_PROCESSING)
                    || MaterialForms.has(m, "ORES");   // GT6 OP.ore = ORES
    private static final Predicate<GTMaterial> HAS_DIRTY_DUST = m ->
            m.has(MaterialProperty.GENERATE_DIRTY_DUST) || HAS_ORE_PROCESSING.test(m);
    private static final Predicate<GTMaterial> HAS_INGOT = m ->
            m.hasAny(MaterialProperty.INGOT, MaterialProperty.METAL, MaterialProperty.GENERATE_INGOT, MaterialProperty.ALLOY)
                    || MaterialForms.has(m, "INGOTS");   // GT6 OP.ingot = INGOTS
    private static final Predicate<GTMaterial> HAS_GEM = m ->
            m.has(MaterialProperty.GEM) || MaterialForms.has(m, "GEMS");   // GT6 OP.gem = GEMS
    private static final Predicate<GTMaterial> HAS_BOULE = m -> m.has(MaterialProperty.BOULE);
    private static final Predicate<GTMaterial> HAS_PLATE = m ->
            (HAS_INGOT.test(m) && !HAS_GEM.test(m))
                    || m.hasAny(MaterialProperty.WOOD, MaterialProperty.STONE)
                    // GT6 OP.plate = And(Or(ingot, gem.NOT), PLATES)
                    || (MaterialForms.has(m, "PLATES")
                            && (MaterialForms.has(m, "INGOTS") || !MaterialForms.has(m, "GEMS")));
    private static final Predicate<GTMaterial> HAS_STICKS = m ->
            m.has(MaterialProperty.GENERATE_STICKS)
                    || HAS_INGOT.test(m)
                    || m.hasAny(MaterialProperty.WOOD, MaterialProperty.STONE)
                    || MaterialForms.has(m, "STICKS");   // GT6 OP.stick = STICKS
    private static final Predicate<GTMaterial> HAS_PARTS = m ->
            m.has(MaterialProperty.GENERATE_PARTS) && (HAS_INGOT.test(m) || HAS_GEM.test(m))
                    || MaterialForms.has(m, "PARTS");   // GT6 OP.gearGt/ring/spring/rotor/casingSmall = PARTS
    private static final Predicate<GTMaterial> HAS_PLANT = m ->
            m.hasAny(MaterialProperty.GENERATE_PLANT, MaterialProperty.WOOD)
                    || MaterialForms.has(m, "PLANTS");   // GT6 OP.plantGt* = PLANTS
    /** GT6 {@code OP.PROJECTILES}: the flag behind the arrows and bullets. */
    private static final Predicate<GTMaterial> GT6_PROJECTILES = m -> MaterialForms.has(m, "PROJECTILES");
    private static final Predicate<GTMaterial> HAS_TOOL_HEAD = m ->
            m.has(MaterialProperty.TOOL_HEAD) && (HAS_INGOT.test(m) || HAS_GEM.test(m));
    /**
     * GT6's tool-head conditions are written over the material *type*, not over tool quality:
     * {@code OP.java:239-247} gives sword blades and the pickaxe/shovel/spade/axe/hoe/sense/plow heads
     * to {@code typemin(1)} — i.e. to every material the original registers, stone included, which is
     * where GT6's knapped stone tools come from — and the hammer to
     * {@code And(typemin(1), Or(BOUNCY, STRETCHY, WOOD, qualmin(1)))}.
     *
     * <p>The port registers material items from {@code MaterialProperty} values instead, so it reads
     * {@code typemin(1)}/{\@code qualmin(1)} as "the material has tool stats"
     * ({@link GTMaterial#getToolQuality()} ≥ 1, which the original's stone materials carry through
     * {@code .qual(1, …)}, e.g. {@code MT.java:1631 Stone}, {@code :3833 Blackstone}). Taking
     * {@code typemin(1)} literally would add a sword blade to dirt, water and every other non-material
     * (~25k items) — documented deviation, §24 of {@code docs/PORTING_REMAINING_2026-09-14.md}.</p>
     */
    private static final Predicate<GTMaterial> GT6_TOOL_HEAD = m -> m.getToolQuality() >= 1;
    /** GT6 {@code OP.java:247}: the hammer additionally accepts {@code WOOD} (whose quality is 0). */
    private static final Predicate<GTMaterial> GT6_HAMMER = GT6_TOOL_HEAD
            .or(m -> m.has(MaterialProperty.WOOD));
    private static final Predicate<GTMaterial> HAS_PROJECTILE = m ->
            m.has(MaterialProperty.GENERATE_PROJECTILE);

    // ---- GT6's own form conditions (OP.java) ------------------------------------------------
    //
    // GT6 decides which forms a material has from the item-generator flags it hands the material
    // (`TD.ItemGenerator`, imported by tools/extract_gt6_form_flags.py into MaterialForms); each
    // prefix carries that condition (`OP.java`, e.g. `plate = And(Or(ingot, gem.NOT), PLATES)`).
    // The port's properties approximate those flags, which is why forms the original registers were
    // missing (the quartz family's gems/plates/sticks, Asbestos' plates, Paper's multi-plates, …).
    // These predicates add the original's answer; they never remove a form the port already has.

    /** GT6 {@code OP.PLATES} materials. */
    private static final Predicate<GTMaterial> GT6_PLATES = m -> MaterialForms.has(m, "PLATES");
    /** GT6 {@code OP.GEMS}. */
    private static final Predicate<GTMaterial> GT6_GEMS = m -> MaterialForms.has(m, "GEMS");
    /** GT6 {@code OP.INGOTS}. */
    private static final Predicate<GTMaterial> GT6_INGOTS = m -> MaterialForms.has(m, "INGOTS");
    /** GT6 {@code OP.STICKS}. */
    private static final Predicate<GTMaterial> GT6_STICKS = m -> MaterialForms.has(m, "STICKS");
    /** GT6 {@code OP.FOILS}. */
    private static final Predicate<GTMaterial> GT6_FOILS = m -> MaterialForms.has(m, "FOILS");
    /** GT6 {@code OP.LENSES}. */
    private static final Predicate<GTMaterial> GT6_LENSES = m -> MaterialForms.has(m, "LENSES");
    /** GT6 {@code OP.PARTS}: gears, rings, springs, rotors, casings. */
    private static final Predicate<GTMaterial> GT6_PARTS = m -> MaterialForms.has(m, "PARTS");
    /** GT6 {@code OP.RAILS}. */
    private static final Predicate<GTMaterial> GT6_RAILS = m -> MaterialForms.has(m, "RAILS");
    /** GT6 {@code OP.ORES}: raw ore, crushed ore, rock. */
    private static final Predicate<GTMaterial> GT6_ORES = m -> MaterialForms.has(m, "ORES");
    /** GT6 {@code OP.PLANTS}: berry, blossom, fiber, twig, wart. */
    private static final Predicate<GTMaterial> GT6_PLANTS = m -> MaterialForms.has(m, "PLANTS");
    /** GT6 {@code OP.ingotHot}: {@code And(INGOTS_HOT, SMITHABLE, meltmin(800))}. */
    private static final Predicate<GTMaterial> GT6_INGOTS_HOT = m ->
            MaterialForms.has(m, "INGOTS_HOT") && m.has(MaterialProperty.SMITHABLE)
                    && m.getMeltingPoint() >= 800;
    /** GT6 {@code OP.ingotDouble} … {@code ingotQuintuple}. */
    private static final Predicate<GTMaterial> GT6_MULTIINGOTS = m -> MaterialForms.has(m, "MULTIINGOTS");
    /** GT6 {@code OP.plateDouble} … {@code plateQuintuple}. */
    private static final Predicate<GTMaterial> GT6_MULTIPLATES = m -> MaterialForms.has(m, "MULTIPLATES");
    /**
     * The four metals GT6 coins unconditionally ({@code MultiTileEntityCoin.java:286}):
     * {@code tMaterial != MT.Cu && tMaterial != MT.Ag && tMaterial != MT.Au && tMaterial != MT.Pt}.
     * The port's own names for them ({@code ElementMaterials.java:51}, {@code :69}, and Platinum at
     * {@code :100}; Gold is the element material of the same file).
     */
    private static final java.util.Set<String> GT6_COIN_METALS =
            java.util.Set.of("Copper", "Silver", "Gold", "Platinum");
    /**
     * GT6's coin condition ({@code MultiTileEntityCoin.java:285-287}): it walks every material and
     * keeps the ones whose tiny plate can be generated -
     * {@code if ((tMaterial.mHidden || !OP.plateTiny.canGenerateItem(tMaterial)) && tMaterial != MT.Cu
     * && … ) continue;} - so a coin exists for exactly the tiny-plate materials, plus Copper, Silver,
     * Gold and Platinum.
     *
     * <p>The port reads {@code OP.plateTiny} as {@link MaterialPrefix#plateTiny} (GT6's
     * {@code plateTiny = plate}, {@code OP.java:198}, with the port's forced
     * {@code OP.java:617 plateTiny.forceItemGeneration(MT.Paper)} case) and its
     * {@link MaterialPrefix#isValidFor} additionally rejects materials the port hides, which GT6's
     * coin loop only rejects for the non-precious ones - a hidden material therefore gets no port
     * coin even where GT6 would force one.</p>
     *
     * <p>A method and not a {@code Predicate} field: {@code plateTiny} is a blank final filled by the
     * static block below, and Java forbids reading such a field from the body of a lambda that itself
     * sits in a field initializer.</p>
     */
    private static boolean gt6Coin(GTMaterial m) {
        return plateTiny.isValidFor(m) || GT6_COIN_METALS.contains(m.getName());
    }
    /** GT6 {@code OP.plateDense}. */
    private static final Predicate<GTMaterial> GT6_DENSEPLATES = m -> MaterialForms.has(m, "DENSEPLATES");

    static {
        dust = def("dust", "Dust", HAS_DUST);
        dustSmall = child("dustSmall", "Small Dust", dust);
        dustTiny = child("dustTiny", "Tiny Dust", dust);
        dustDiv72 = child("dustDiv72", "1/72 Dust", dust);
        dustImpure = tex("dustImpure", "Impure Dust", "dust", HAS_DIRTY_DUST);
        // Unit marker items exist for every material (recipe display / JEI bookkeeping).
        unit = def("unit", "Unit", m -> true);
        crushed = def("crushed", "Crushed Ore", HAS_ORE_PROCESSING);
        crushedTiny = child("crushedTiny", "Tiny Crushed Ore", crushed);
        crushedPurified = child("crushedPurified", "Purified Crushed Ore", crushed);
        crushedPurifiedTiny = child("crushedPurifiedTiny", "Tiny Purified Crushed Ore", crushedPurified);
        crushedCentrifuged = child("crushedCentrifuged", "Refined Crushed Ore", crushedPurified);
        crushedCentrifugedTiny = child("crushedCentrifugedTiny", "Tiny Refined Crushed Ore", crushedCentrifuged);
        rockGt = def("rockGt", "Rock", m -> HAS_ORE_PROCESSING.test(m) || m.has(MaterialProperty.STONE));

        // OP.gemChipped.forceItemGeneration(MT.Sugar): this one form does not make sugar a gem material.
        gemChipped = def("gemChipped", "Chipped Gem", HAS_GEM.or(GT6_GEMS).or(m -> m.getName().equals("Sugar")));
        gemFlawed = def("gemFlawed", "Flawed Gem", HAS_GEM.or(GT6_GEMS));
        gem = def("gem", "Gem", HAS_GEM.or(GT6_GEMS));
        gemFlawless = def("gemFlawless", "Flawless Gem", HAS_GEM.or(GT6_GEMS));
        gemExquisite = def("gemExquisite", "Exquisite Gem", HAS_GEM.or(GT6_GEMS));
        gemLegendary = def("gemLegendary", "Legendary Gem", HAS_GEM.or(GT6_GEMS));
        bouleGt = def("bouleGt", "Boule", HAS_BOULE, true);

        ingot = def("ingot", "Ingot", HAS_INGOT.or(GT6_INGOTS));
        nugget = child("nugget", "Nugget", ingot);
        chunkGt = child("chunkGt", "Chunk", ingot);
        billet = child("billet", "Billet", ingot);
        ingotHot = def("ingotHot", "Hot Ingot",
                m -> HAS_INGOT.test(m) && m.has(MaterialProperty.SMITHABLE) || GT6_INGOTS_HOT.test(m), true);
        ingotDouble = def("ingotDouble", "Double Ingot",
                m -> HAS_INGOT.test(m) && m.has(MaterialProperty.GENERATE_MULTIINGOT) || GT6_MULTIINGOTS.test(m));
        ingotTriple = def("ingotTriple", "Triple Ingot",
                m -> HAS_INGOT.test(m) && m.has(MaterialProperty.GENERATE_MULTIINGOT) || GT6_MULTIINGOTS.test(m));
        ingotQuadruple = def("ingotQuadruple", "Quadruple Ingot",
                m -> HAS_INGOT.test(m) && m.has(MaterialProperty.GENERATE_MULTIINGOT) || GT6_MULTIINGOTS.test(m));
        ingotQuintuple = def("ingotQuintuple", "Quintuple Ingot",
                m -> HAS_INGOT.test(m) && m.has(MaterialProperty.GENERATE_MULTIINGOT) || GT6_MULTIINGOTS.test(m));

        // GT6 OP.plateGem = And(Or(gem, bouleGt), PLATES)
        plateGem = def("plateGem", "Gem Plate", m ->
                (HAS_GEM.test(m) || HAS_BOULE.test(m)) && m.has(MaterialProperty.GENERATE_PLATE)
                        || (GT6_PLATES.test(m) && (GT6_GEMS.test(m) || HAS_BOULE.test(m))));
        plateGemTiny = child("plateGemTiny", "Tiny Gem Plate", plateGem);
        plate = def("plate", "Plate", HAS_PLATE);
        // GT6 `OP.java:198`: `plateTiny = plate`, and `OP.java:617` forces the Paper case
        // (`plateTiny.forceItemGeneration(MT.Paper)`) because Paper carries no PLATES flag
        // (`paper|DUSTS,MULTIPLATES,PLANTS`) — the port's `plate` misses it for the same reason.
        plateTiny = tex("plateTiny", "Tiny Plate", "plateTiny",
                m -> plate.isValidFor(m) || m.getName().equals("Paper"));
        plateDouble = def("plateDouble", "Double Plate", m ->
                HAS_PLATE.test(m) && m.has(MaterialProperty.GENERATE_MULTIPLATE) || GT6_MULTIPLATES.test(m));
        plateTriple = def("plateTriple", "Triple Plate", m ->
                HAS_PLATE.test(m) && m.has(MaterialProperty.GENERATE_MULTIPLATE) || GT6_MULTIPLATES.test(m));
        plateQuadruple = def("plateQuadruple", "Quadruple Plate", m ->
                HAS_PLATE.test(m) && m.has(MaterialProperty.GENERATE_MULTIPLATE) || GT6_MULTIPLATES.test(m));
        plateQuintuple = def("plateQuintuple", "Quintuple Plate", m ->
                HAS_PLATE.test(m) && m.has(MaterialProperty.GENERATE_MULTIPLATE) || GT6_MULTIPLATES.test(m));
        plateDense = def("plateDense", "Dense Plate", m ->
                HAS_PLATE.test(m) && m.has(MaterialProperty.GENERATE_DENSEPLATE) || GT6_DENSEPLATES.test(m));
        plateCurved = child("plateCurved", "Curved Plate", plate);
        foil = def("foil", "Foil", m -> m.has(MaterialProperty.GENERATE_FOIL)
                && (HAS_INGOT.test(m) || HAS_GEM.test(m) || m.has(MaterialProperty.WOOD)) || GT6_FOILS.test(m));

        scrapGt = def("scrapGt", "Scrap", m -> HAS_DUST.test(m) || HAS_INGOT.test(m) || HAS_GEM.test(m), true);
        oreDense = def("oreDense", "Dense Ore", m -> false, true);
        oreVanillastone = def("oreVanillastone", "Stone Ore", m -> false, true);
        oreRaw = tex("oreRaw", "Raw Ore", "oreraw", HAS_ORE_PROCESSING.or(GT6_ORES));

        stick = def("stick", "Rod", HAS_STICKS.or(GT6_STICKS));
        stickLong = child("stickLong", "Long Rod", stick);
        bolt = child("bolt", "Bolt", stick);
        screw = child("screw", "Screw", bolt);
        // GT6's wooden rotation engine uses both WoodTreated gears, although the material's
        // broad G_WOOD form flags omit PARTS. Open only gears, not rings/springs/rotors.
        gearGt = def("gearGt", "Gear", HAS_PARTS.or(GT6_PARTS)
                .or(m -> m == WoodMaterials.WoodTreated));
        gearGtSmall = child("gearGtSmall", "Small Gear", gearGt);
        ring = def("ring", "Ring", HAS_PARTS.or(GT6_PARTS));
        chain = child("chain", "Chain", ring);
        spring = def("spring", "Spring", HAS_PARTS.or(GT6_PARTS));
        springSmall = child("springSmall", "Small Spring", spring);

        rotor = def("rotor", "Rotor", HAS_PARTS.or(GT6_PARTS));
        lens = def("lens", "Lens", m -> m.has(MaterialProperty.GENERATE_LENS) && HAS_GEM.test(m)
                || GT6_LENSES.test(m));
        // GT6 OP.java:209: `round = Or(PARTS, And(PROJECTILES, nugget))` — rounds come from the parts
        // form, or from a nugget-bearing projectile material (the port's HAS_INGOT stands in for the
        // nugget form, which it generates for ingot materials).
        round = def("round", "Round", m -> HAS_PARTS.test(m) || GT6_PARTS.test(m)
                || (GT6_PROJECTILES.test(m) && HAS_INGOT.test(m)));
        itemCasing = tex("itemCasing", "Item Casing", "casingsmall", HAS_PARTS.or(GT6_PARTS));
        wireFine = def("wireFine", "Fine Wire", m -> m.has(MaterialProperty.GENERATE_WIRE)
                || (HAS_PARTS.test(m) && HAS_INGOT.test(m)) || MaterialForms.has(m, "WIRES"));
        minecartWheels = def("minecartWheels", "Minecart Wheels",
                m -> HAS_PARTS.test(m) && m.has(MaterialProperty.SMITHABLE));
        railGt = def("railGt", "Rail", m -> m.has(MaterialProperty.GENERATE_RAIL) && HAS_INGOT.test(m)
                || GT6_RAILS.test(m));

        plantGtBerry = def("plantGtBerry", "Berry", HAS_PLANT.or(GT6_PLANTS), true);
        plantGtBlossom = def("plantGtBlossom", "Blossom", HAS_PLANT.or(GT6_PLANTS), true);
        plantGtFiber = def("plantGtFiber", "Fiber", HAS_PLANT.or(GT6_PLANTS), true);
        plantGtTwig = def("plantGtTwig", "Twig", HAS_PLANT.or(GT6_PLANTS), true);
        plantGtWart = def("plantGtWart", "Wart", HAS_PLANT.or(GT6_PLANTS), true);

        glasstube = def("glasstube", "Glass Tube", m -> HAS_GEM.test(m) || HAS_PLATE.test(m));

        coin = def("coin", "Coin", MaterialPrefix::gt6Coin);

        // The tool-head family follows GT6's `typemin(1)` conditions (see GT6_TOOL_HEAD above): the
        // port's own HAS_TOOL_HEAD plus every material with tool stats, so stone/basalt/blackstone get
        // their knapped heads (and the recipes that press/forge them resolve).
        toolHeadSword = def("toolHeadSword", "Sword Blade", HAS_TOOL_HEAD.or(GT6_TOOL_HEAD));
        toolHeadRawSword = child("toolHeadRawSword", "Raw Sword Blade", toolHeadSword);
        toolHeadPickaxe = def("toolHeadPickaxe", "Pickaxe Head", HAS_TOOL_HEAD.or(GT6_TOOL_HEAD));
        toolHeadRawPickaxe = child("toolHeadRawPickaxe", "Raw Pickaxe Head", toolHeadPickaxe);
        toolHeadPickaxeGem = def("toolHeadPickaxeGem", "Gem Pickaxe Head", m -> gemFlawed.isValidFor(m));
        toolHeadConstructionPickaxe = child("toolHeadConstructionPickaxe", "Construction Pickaxe Head", toolHeadPickaxe);
        toolHeadBuilderwand = def("toolHeadBuilderwand", "Builder Wand Head", HAS_TOOL_HEAD);
        toolHeadShovel = def("toolHeadShovel", "Shovel Head", HAS_TOOL_HEAD.or(GT6_TOOL_HEAD));
        toolHeadRawShovel = child("toolHeadRawShovel", "Raw Shovel Head", toolHeadShovel);
        toolHeadSpade = def("toolHeadSpade", "Spade Head", HAS_TOOL_HEAD.or(GT6_TOOL_HEAD));
        toolHeadRawSpade = child("toolHeadRawSpade", "Raw Spade Head", toolHeadSpade);
        toolHeadAxe = def("toolHeadAxe", "Axe Head", HAS_TOOL_HEAD.or(GT6_TOOL_HEAD));
        toolHeadRawAxe = child("toolHeadRawAxe", "Raw Axe Head", toolHeadAxe);
        toolHeadAxeDouble = def("toolHeadAxeDouble", "Double Axe Head", HAS_TOOL_HEAD.or(GT6_TOOL_HEAD));
        toolHeadRawAxeDouble = child("toolHeadRawAxeDouble", "Raw Double Axe Head", toolHeadAxeDouble);
        toolHeadHoe = def("toolHeadHoe", "Hoe Head", HAS_TOOL_HEAD.or(GT6_TOOL_HEAD));
        toolHeadRawHoe = child("toolHeadRawHoe", "Raw Hoe Head", toolHeadHoe);
        toolHeadHammer = def("toolHeadHammer", "Hammer Head", HAS_TOOL_HEAD.or(GT6_HAMMER));
        toolHeadFile = def("toolHeadFile", "File Head", HAS_TOOL_HEAD);
        toolHeadChisel = def("toolHeadChisel", "Chisel Head", HAS_TOOL_HEAD);
        toolHeadRawChisel = child("toolHeadRawChisel", "Raw Chisel Head", toolHeadChisel);
        toolHeadSaw = def("toolHeadSaw", "Saw Blade", HAS_TOOL_HEAD);
        toolHeadRawSaw = child("toolHeadRawSaw", "Raw Saw Blade", toolHeadSaw);
        toolHeadDrill = def("toolHeadDrill", "Drill Head", HAS_TOOL_HEAD);
        toolHeadChainsaw = def("toolHeadChainsaw", "Chainsaw Head", HAS_TOOL_HEAD);
        toolHeadWrench = def("toolHeadWrench", "Wrench Head", HAS_TOOL_HEAD);
        toolHeadScrewdriver = def("toolHeadScrewdriver", "Screwdriver Head", HAS_TOOL_HEAD);
        toolHeadUniversalSpade = def("toolHeadUniversalSpade", "Universal Spade Head", HAS_TOOL_HEAD);
        toolHeadRawUniversalSpade = child("toolHeadRawUniversalSpade", "Raw Universal Spade Head", toolHeadUniversalSpade);
        toolHeadSense = def("toolHeadSense", "Sense Tool Head", HAS_TOOL_HEAD.or(GT6_TOOL_HEAD));
        toolHeadRawSense = child("toolHeadRawSense", "Raw Sense Tool Head", toolHeadSense);
        toolHeadPlow = def("toolHeadPlow", "Plow Head", HAS_TOOL_HEAD.or(GT6_TOOL_HEAD));
        toolHeadRawPlow = child("toolHeadRawPlow", "Raw Plow Head", toolHeadPlow);
        toolHeadBuzzSaw = def("toolHeadBuzzSaw", "Buzz Saw Blade", HAS_TOOL_HEAD);
        // GT6 `OP.java:254`: `toolHeadArrow` = And(PROJECTILES, typemin(1)) — arrow heads follow the
        // projectile flag, not tool quality (every GT6 metal and wood has them). The port keeps its
        // stricter HAS_TOOL_HEAD as an *additional* source so materials it gives tool heads to keep
        // their arrow heads, but GT6's own flag now decides too (Trinium/Naquadah, the renamed woods).
        toolHeadArrow = def("toolHeadArrow", "Arrow Head", HAS_TOOL_HEAD.or(GT6_PROJECTILES));
        toolHeadRawArrow = child("toolHeadRawArrow", "Raw Arrow Head", toolHeadArrow);

        arrowGtWood = def("arrowGtWood", "Wood Arrow",
                m -> HAS_PROJECTILE.test(m) || GT6_PROJECTILES.test(m));   // GT6: Or(toolHeadArrow, EMPTY)
        arrowGtPlastic = def("arrowGtPlastic", "Plastic Arrow",
                m -> HAS_PROJECTILE.test(m) || GT6_PROJECTILES.test(m));
        bulletGtSmall = def("bulletGtSmall", "Small Bullet",
                m -> HAS_PROJECTILE.test(m) || GT6_PROJECTILES.test(m));   // GT6: Or(PROJECTILES, EMPTY)
        bulletGtMedium = def("bulletGtMedium", "Medium Bullet",
                m -> HAS_PROJECTILE.test(m) || GT6_PROJECTILES.test(m));
        bulletGtLarge = def("bulletGtLarge", "Large Bullet",
                m -> HAS_PROJECTILE.test(m) || GT6_PROJECTILES.test(m));
    }

    /** Forces this class to load so static prefix fields register into {@link PrefixRegistry}. */
    public static void bootstrap() {
        if (dust == null || ingot == null || plate == null || gearGt == null || bulletGtLarge == null) {
            throw new IllegalStateException("MaterialPrefix constants failed to initialize");
        }
        // GT6 OP.java:578 — carrying a white-hot ingot burns its holder
        // (UT.Entities.getHeatDamageFromItem -> GTEntityHelper.heatDamageFromItem).
        ingotHot.heatDamage = 3.0F;
    }

private final String name;
    private final String displayName;
    private final Predicate<GTMaterial> validator;
    private final MaterialPrefix parent;
    private final String textureFileName;
    private final boolean hiddenFromCreative;
    private final String registryName;
    /** GT6 {@code OreDictPrefix.mHeatDamage}: damage dealt per item while carried (hot ingot = 3.0). */
    private float heatDamage;

    private MaterialPrefix(String name, String displayName, Predicate<GTMaterial> validator,
                         MaterialPrefix parent, String textureFileName, boolean hiddenFromCreative) {
        this.name = name;
        this.displayName = displayName;
        this.validator = validator;
        this.parent = parent;
        this.textureFileName = textureFileName;
        this.hiddenFromCreative = hiddenFromCreative;
        this.registryName = camelToSnake(name);
        PrefixRegistry.register(this);
    }

    private static MaterialPrefix def(String name, String displayName, Predicate<GTMaterial> validator) {
        return new MaterialPrefix(name, displayName, validator, null, null, false);
    }

    private static MaterialPrefix def(String name, String displayName, Predicate<GTMaterial> validator, boolean hiddenFromCreative) {
        return new MaterialPrefix(name, displayName, validator, null, null, hiddenFromCreative);
    }

    private static MaterialPrefix tex(String name, String displayName, String textureFileName, Predicate<GTMaterial> validator) {
        return new MaterialPrefix(name, displayName, validator, null, textureFileName, false);
    }

    private static MaterialPrefix child(String name, String displayName, MaterialPrefix parent) {
        return new MaterialPrefix(name, displayName, m -> true, parent, null, parent.hiddenFromCreative);
    }

    /** Forge registry path segment: {@code gemChipped}  -> {@code gem_chipped}, {@code dust}  -> {@code dust}. */
    public static String camelToSnake(String id) {
        StringBuilder sb = new StringBuilder(id.length() + 4);
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** Amount of material (in {@link GTValues#U} units) represented by one item of this prefix. */
    public long getMaterialWeight() {
        return switch (registryName) {
            // dusts
            case "dust_small" -> GTValues.U4;
            case "dust_tiny", "nugget" -> GTValues.U9;
            case "dust_div72" -> GTValues.U72;
            // GT6's coin is the only form whose amount is written down outside the prefix table: the
            // coin multi-tile recycles itself as one ninth of a unit
            // (`OM.stack(mMaterial, U9)`, MultiTileEntityCoin.java:99).
            case "coin" -> GTValues.U9;
            case "unit" -> GTValues.U;
            // ore processing
            case "crushed" -> GTValues.U * 9 / 8;
            case "crushed_tiny" -> GTValues.U / 8;
            case "crushed_purified" -> GTValues.U * 5 / 4;
            case "crushed_purified_tiny" -> GTValues.U72 * 10;
            case "crushed_centrifuged" -> GTValues.U * 11 / 8;
            case "crushed_centrifuged_tiny" -> GTValues.U72 * 11;
            case "rock_gt" -> GTValues.U * 9 / 4;
            case "ore_dense" -> GTValues.U * 4;
            case "ore_vanillastone" -> GTValues.U * 2;
            case "ore_raw" -> GTValues.U * 2;
            // ingots/chunks
            case "chunk_gt" -> GTValues.U4;
            case "billet" -> GTValues.U3 * 2;
            case "ingot_double" -> GTValues.U * 2;
            case "ingot_triple" -> GTValues.U * 3;
            case "ingot_quadruple" -> GTValues.U * 4;
            case "ingot_quintuple" -> GTValues.U * 5;
            // rods / bolts / screws
            case "stick" -> GTValues.U2;
            case "stick_long" -> GTValues.U;
            case "bolt" -> GTValues.U / 8;
            case "screw" -> GTValues.U9;
            case "round" -> GTValues.U9;
            case "ring" -> GTValues.U4;
            case "spring_small" -> GTValues.U4;
            // wire / rail
            case "wire_fine" -> GTValues.U / 8;
            case "rail_gt" -> GTValues.U4;
            // plates / foil / lens
            case "foil" -> GTValues.U4;
            case "lens" -> GTValues.U * 3 / 4;
            case "plate_double" -> GTValues.U * 2;
            case "plate_triple" -> GTValues.U * 3;
            case "plate_quadruple" -> GTValues.U * 4;
            case "plate_quintuple" -> GTValues.U * 5;
            case "plate_dense" -> GTValues.U * 9;
            case "plate_gem_tiny" -> GTValues.U9;
            case "plate_tiny" -> GTValues.U9;
            // gems
            case "gem_chipped" -> GTValues.U4;
            case "gem_flawed" -> GTValues.U2;
            case "gem_flawless" -> GTValues.U * 2;
            case "gem_exquisite" -> GTValues.U * 4;
            case "gem_legendary" -> GTValues.U * 8;
            // misc
            case "scrap_gt" -> GTValues.U9;
            // gears / rotor
            case "gear_gt" -> GTValues.U * 4;
            case "gear_gt_small" -> GTValues.U;
            case "rotor" -> GTValues.U * 4 + GTValues.U4;
            // tool heads — finished (machined, material loss from filing)
            case "tool_head_sword", "tool_head_hoe", "tool_head_saw" -> GTValues.U * 2 - GTValues.U9;
            case "tool_head_shovel", "tool_head_spade" -> GTValues.U - GTValues.U9;
            case "tool_head_universal_spade" -> GTValues.U - GTValues.U9 * 2;
            case "tool_head_axe" -> GTValues.U * 3 - GTValues.U9;
            case "tool_head_axe_double" -> GTValues.U * 5 - GTValues.U9 * 2;
            case "tool_head_hammer" -> GTValues.U * 6;
            case "tool_head_file", "tool_head_raw_chisel" -> GTValues.U2 * 3;
            case "tool_head_chisel" -> GTValues.U2 * 3 - GTValues.U9;
            case "tool_head_wrench" -> GTValues.U * 4;
            case "tool_head_pickaxe", "tool_head_sense", "tool_head_builderwand" -> GTValues.U9 * 26;
            case "tool_head_plow" -> GTValues.U9 * 35;
            case "tool_head_arrow" -> GTValues.U9;
            case "tool_head_buzz_saw", "tool_head_drill" -> GTValues.U * 4;
            case "tool_head_chainsaw" -> GTValues.U * 2;
            case "tool_head_pickaxe_gem" -> GTValues.U;
            // tool heads — raw (unmachined, still have sprue material)
            case "tool_head_raw_sword", "tool_head_raw_hoe", "tool_head_raw_saw" -> GTValues.U * 2;
            case "tool_head_raw_shovel", "tool_head_raw_spade" -> GTValues.U;
            case "tool_head_raw_universal_spade" -> GTValues.U - GTValues.U9;
            case "tool_head_raw_axe" -> GTValues.U * 3;
            case "tool_head_raw_axe_double" -> GTValues.U * 5;
            case "tool_head_raw_pickaxe", "tool_head_raw_sense" -> GTValues.U * 3;
            case "tool_head_raw_plow" -> GTValues.U * 4;
            case "tool_head_raw_arrow" -> GTValues.U / 8;
            // containers
            case "glasstube" -> GTValues.U9;
            // casing
            case "item_casing" -> GTValues.U2;
            // projectiles
            case "arrow_gt_wood", "arrow_gt_plastic", "bullet_gt_small" -> GTValues.U9;
            case "bullet_gt_medium" -> GTValues.U9 * 2;
            case "bullet_gt_large" -> GTValues.U3;
            default -> GTValues.U;
        };
    }

    public String getRegistryName() {
        return registryName;
    }

    public boolean isHiddenFromCreative() {
        return hiddenFromCreative;
    }

    /** GT6 {@code OreDictPrefix.mHeatDamage} — see {@code OP.ingotHot.mHeatDamage = 3.0F}. */
    public float heatDamage() {
        return heatDamage;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isValidFor(GTMaterial material) {
        if (material == null || material.has(MaterialProperty.HIDDEN)) return false;
        if (!validator.test(material)) return false;
        return parent == null || parent.isValidFor(material);
    }

    public String getItemId(GTMaterial material) {
        return registryName + "_" + material.getName().toLowerCase();
    }

    public String getTagPath(GTMaterial material) {
        return registryName + "s/" + material.getName().toLowerCase();
    }

    /** PNG file name inside a material icon set folder (without {@code _overlay}), always lowercase for 1.20.1. */
    public String getTextureFileName() {
        if (textureFileName != null) {
            return textureFileName;
        }
        return name.toLowerCase();
    }
}
