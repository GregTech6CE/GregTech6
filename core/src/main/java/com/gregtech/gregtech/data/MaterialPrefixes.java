package com.gregtech.gregtech.data;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.api.prefix.BlockMaterialPrefix;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.data.generated.MaterialForms;
import com.gregtech.gregtech.api.prefix.PrefixRegistry;
import com.gregtech.gregtech.api.prefix.BlockMaterialPrefix.SoundType;
import com.gregtech.gregtech.api.prefix.BlockMaterialPrefix.MapColor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Ore prefix registry from GT6 {@code OP.java}.
 * Item prefixes delegate to {@link MaterialPrefix}; block prefixes are defined here and delegate to {@link BlockMaterialPrefix}.
 */
public class MaterialPrefixes {
    protected MaterialPrefixes() {}

    public static final class Entry {
        private final String name;
        private final MaterialPrefix itemDelegate;
        private final BlockMaterialPrefix blockDelegate;

        Entry(String name, MaterialPrefix itemDelegate, BlockMaterialPrefix blockDelegate) {
            this.name = name;
            this.itemDelegate = itemDelegate;
            this.blockDelegate = blockDelegate;
        }

        public String getName() { return name; }
        public MaterialPrefix getDelegate() { return itemDelegate; }
        public BlockMaterialPrefix getBlockDelegate() { return blockDelegate; }
        public boolean hasDelegate() { return itemDelegate != null; }
        public boolean isBlock() { return blockDelegate != null; }
    }

    private static final Map<String, Entry> REGISTRY = new LinkedHashMap<>();
    private static boolean blocksDefined;

    private static Entry register(String name) {
        MaterialPrefix delegate = resolveDelegate(name);
        Entry entry = new Entry(name, delegate, null);
        REGISTRY.put(name, entry);
        return entry;
    }

    private static Entry registerBlock(BlockBuilder builder) {
        BlockMaterialPrefix prefix = builder.build();
        Entry entry = new Entry(builder.name, null, prefix);
        REGISTRY.put(builder.name, entry);
        return entry;
    }

    /** Fluent block prefix builder mirroring GT6 {@code OP.create(...)} chains. */
    private static final class BlockBuilder {
        private final String name;
        private String displayName = "";
        private String namePrefix = "";
        private String nameSuffix = "";
        private Predicate<GTMaterial> validator = m -> false;
        private float hardness = 1.0F;
        private float resistance = 3.0F;
        private int harvestOffset;
        private MapColor mapColor = MapColor.METAL;
        private SoundType sound = SoundType.METAL;
        private boolean falling;
        private long materialWeight = GTValues.U * 9;
        private String textureFileName;

        BlockBuilder(String name) {
            this.name = name;
        }

        BlockBuilder texture(String textureFileName) {
            this.textureFileName = textureFileName;
            return this;
        }

        BlockBuilder display(String plural, String prefix, String suffix) {
            this.displayName = plural;
            this.namePrefix = prefix;
            this.nameSuffix = suffix;
            return this;
        }

        BlockBuilder condition(Predicate<GTMaterial> predicate) {
            this.validator = predicate;
            return this;
        }

        BlockBuilder condition(MaterialPrefix prefix) {
            return condition(prefix::isValidFor);
        }

        BlockBuilder materialWeight(long weight) {
            this.materialWeight = weight;
            return this;
        }

        BlockBuilder physics(float hardness, float resistance, int harvestOffset,
                             MapColor mapColor, SoundType sound) {
            this.hardness = hardness;
            this.resistance = resistance;
            this.harvestOffset = harvestOffset;
            this.mapColor = mapColor;
            this.sound = sound;
            return this;
        }

        BlockBuilder falling() {
            this.falling = true;
            return this;
        }

        BlockMaterialPrefix build() {
            return BlockMaterialPrefix.define(name, displayName, namePrefix, nameSuffix, validator,
                    hardness, resistance, harvestOffset, mapColor, sound, falling, materialWeight, textureFileName);
        }
    }

    private static BlockBuilder block(String name) {
        return new BlockBuilder(name);
    }

    private static MaterialPrefix resolveDelegate(String name) {
        PrefixRegistry.ensurePrefixesLoaded();
        for (MaterialPrefix prefix : PrefixRegistry.all()) {
            if (prefix.getName().equals(name)) {
                return prefix;
            }
        }
        return null;
    }

    public static final Entry
            ore = register("ore"),
            oreBlackgranite = register("oreBlackgranite"),
            oreRedgranite = register("oreRedgranite"),
            oreVanillastone = register("oreVanillastone"),
            oreVanillagranite = register("oreVanillagranite"),
            oreAndesite = register("oreAndesite"),
            oreDiorite = register("oreDiorite"),
            oreDeepslate = register("oreDeepslate"),
            oreBlackstone = register("oreBlackstone"),
            oreMoon = register("oreMoon"),
            oreMars = register("oreMars"),
            oreSpace = register("oreSpace"),
            orePhobos = register("orePhobos"),
            oreDeimos = register("oreDeimos"),
            oreVenus = register("oreVenus"),
            oreMercury = register("oreMercury"),
            oreCeres = register("oreCeres"),
            oreJupiter = register("oreJupiter"),
            oreIo = register("oreIo"),
            oreEuropa = register("oreEuropa"),
            oreGanymede = register("oreGanymede"),
            oreCallisto = register("oreCallisto"),
            oreSaturn = register("oreSaturn"),
            oreRhea = register("oreRhea"),
            oreTitan = register("oreTitan"),
            oreOberon = register("oreOberon"),
            oreIapetus = register("oreIapetus"),
            oreUranus = register("oreUranus"),
            oreTitania = register("oreTitania"),
            oreNeptune = register("oreNeptune"),
            oreTriton = register("oreTriton"),
            orePluto = register("orePluto"),
            oreEris = register("oreEris"),
            oreKepler22b = register("oreKepler22b"),
            oreHolystone = register("oreHolystone"),
            oreLivingrock = register("oreLivingrock"),
            oreDeadrock = register("oreDeadrock"),
            oreBetweenstone = register("oreBetweenstone"),
            orePitstone = register("orePitstone"),
            oreUmberstone = register("oreUmberstone"),
            oreKomatiite = register("oreKomatiite"),
            oreBasalt = register("oreBasalt"),
            oreMarble = register("oreMarble"),
            oreLimestone = register("oreLimestone"),
            oreSiltstone = register("oreSiltstone"),
            oreShale = register("oreShale"),
            oreSlate = register("oreSlate"),
            oreGreenschist = register("oreGreenschist"),
            oreBlueschist = register("oreBlueschist"),
            orePinkschist = register("orePinkschist"),
            oreGrayschist = register("oreGrayschist"),
            oreGneiss = register("oreGneiss"),
            oreLightprismarine = register("oreLightprismarine"),
            oreDarkprismarine = register("oreDarkprismarine"),
            oreKimberlite = register("oreKimberlite"),
            oreQuartzite = register("oreQuartzite"),
            oreNetherrack = register("oreNetherrack"),
            oreEndstone = register("oreEndstone"),
            oreSandstone = register("oreSandstone"),
            oreGravel = register("oreGravel"),
            oreStrangesand = register("oreStrangesand"),
            oreRedSand = register("oreRedSand"),
            oreSand = register("oreSand"),
            oreMud = register("oreMud"),
            oreBedrock = register("oreBedrock"),
            oreNether = register("oreNether"),
            oreDense = register("oreDense"),
            oreEnd = register("oreEnd"),
            oreRich = register("oreRich"),
            oreNormal = register("oreNormal"),
            oreSmall = register("oreSmall"),
            orePoor = register("orePoor"),
            oreRaw = register("oreRaw"),
            crushed = register("crushed"),
            crushedTiny = register("crushedTiny"),
            crushedPurified = register("crushedPurified"),
            crushedPurifiedTiny = register("crushedPurifiedTiny"),
            crushedCentrifuged = register("crushedCentrifuged"),
            crushedCentrifugedTiny = register("crushedCentrifugedTiny"),
            rockGt = register("rockGt"),
            rawOreChunk = register("rawOreChunk"),
            clump = register("clump"),
            cluster = register("cluster"),
            pebbles = register("pebbles"),
            rubble = register("rubble"),
            chunk = register("chunk"),
            crystalline = register("crystalline"),
            reduced = register("reduced"),
            cleanGravel = register("cleanGravel"),
            dirtyGravel = register("dirtyGravel"),
            dust = register("dust"),
            dustSmall = register("dustSmall"),
            dustTiny = register("dustTiny"),
            dustDiv72 = register("dustDiv72"),
            dustImpure = register("dustImpure"),
            dustPure = register("dustPure"),
            dustRefined = register("dustRefined"),
            ingotQuintuple = register("ingotQuintuple"),
            ingotQuadruple = register("ingotQuadruple"),
            ingotTriple = register("ingotTriple"),
            ingotDouble = register("ingotDouble"),
            ingotHot = register("ingotHot"),
            ingot = register("ingot"),
            billet = register("billet"),
            chunkGt = register("chunkGt"),
            nugget = register("nugget"),
            gem = register("gem"),
            gemChipped = register("gemChipped"),
            gemFlawed = register("gemFlawed"),
            gemFlawless = register("gemFlawless"),
            gemExquisite = register("gemExquisite"),
            gemLegendary = register("gemLegendary"),
            gemOre = register("gemOre"),
            gemRaw = register("gemRaw"),
            gemUncut = register("gemUncut"),
            gemPolished = register("gemPolished"),
            bouleGt = register("bouleGt"),
            crystalPure = register("crystalPure"),
            crystal = register("crystal"),
            lens = register("lens"),
            scrapGt = register("scrapGt"),
            plateSteamcraft = register("plateSteamcraft"),
            plateDense = register("plateDense"),
            plateQuintuple = register("plateQuintuple"),
            plateQuadruple = register("plateQuadruple"),
            plateTriple = register("plateTriple"),
            plateDouble = register("plateDouble"),
            plate = register("plate"),
            plateGem = register("plateGem"),
            plateTiny = register("plateTiny"),
            plateGemTiny = register("plateGemTiny"),
            plateCurved = register("plateCurved"),
            compressed = register("compressed"),
            sheetGt = register("sheetGt"),
            foil = register("foil"),
            stick = register("stick"),
            stickLong = register("stickLong"),
            bolt = register("bolt"),
            screw = register("screw"),
            round = register("round"),
            ring = register("ring"),
            chain = register("chain"),
            spring = register("spring"),
            springSmall = register("springSmall"),
            wireFine = register("wireFine"),
            minecartWheels = register("minecartWheels"),
            gearGt = register("gearGt"),
            gearGtSmall = register("gearGtSmall"),
            railGt = register("railGt"),
            itemCasing = register("itemCasing"),
            rotor = register("rotor"),
            glasstube = register("glasstube"),
            cell = register("cell"),
            bucket = register("bucket"),
            bottle = register("bottle"),
            capsule = register("capsule"),
            toolHeadSaw = register("toolHeadSaw"),
            toolHeadFile = register("toolHeadFile"),
            toolHeadChisel = register("toolHeadChisel"),
            toolHeadBuzzSaw = register("toolHeadBuzzSaw"),
            toolHeadChainsaw = register("toolHeadChainsaw"),
            toolHeadWrench = register("toolHeadWrench"),
            toolHeadDrill = register("toolHeadDrill"),
            toolHeadSword = register("toolHeadSword"),
            toolHeadPickaxe = register("toolHeadPickaxe"),
            toolHeadShovel = register("toolHeadShovel"),
            toolHeadSpade = register("toolHeadSpade"),
            toolHeadAxe = register("toolHeadAxe"),
            toolHeadHoe = register("toolHeadHoe"),
            toolHeadSense = register("toolHeadSense"),
            toolHeadPlow = register("toolHeadPlow"),
            toolHeadHammer = register("toolHeadHammer"),
            toolHeadScrewdriver = register("toolHeadScrewdriver"),
            toolHeadBuilderwand = register("toolHeadBuilderwand"),
            toolHeadConstructionPickaxe = register("toolHeadConstructionPickaxe"),
            toolHeadPickaxeGem = register("toolHeadPickaxeGem"),
            toolHeadAxeDouble = register("toolHeadAxeDouble"),
            toolHeadUniversalSpade = register("toolHeadUniversalSpade"),
            toolHeadArrow = register("toolHeadArrow"),
            toolHeadRawSaw = register("toolHeadRawSaw"),
            toolHeadRawChisel = register("toolHeadRawChisel"),
            toolHeadRawSword = register("toolHeadRawSword"),
            toolHeadRawPickaxe = register("toolHeadRawPickaxe"),
            toolHeadRawShovel = register("toolHeadRawShovel"),
            toolHeadRawSpade = register("toolHeadRawSpade"),
            toolHeadRawUniversalSpade = register("toolHeadRawUniversalSpade"),
            toolHeadRawAxe = register("toolHeadRawAxe"),
            toolHeadRawAxeDouble = register("toolHeadRawAxeDouble"),
            toolHeadRawHoe = register("toolHeadRawHoe"),
            toolHeadRawSense = register("toolHeadRawSense"),
            toolHeadRawPlow = register("toolHeadRawPlow"),
            toolHeadRawArrow = register("toolHeadRawArrow"),
            toolSword = register("toolSword"),
            toolPickaxe = register("toolPickaxe"),
            toolShovel = register("toolShovel"),
            toolAxe = register("toolAxe"),
            toolHoe = register("toolHoe"),
            toolShears = register("toolShears"),
            tool = register("tool"),
            bulletGtSmall = register("bulletGtSmall"),
            bulletGtMedium = register("bulletGtMedium"),
            bulletGtLarge = register("bulletGtLarge"),
            arrowGtWood = register("arrowGtWood"),
            arrowGtPlastic = register("arrowGtPlastic"),
            arrow = register("arrow"),
            armorHelmet = register("armorHelmet"),
            armorChestplate = register("armorChestplate"),
            armorLeggings = register("armorLeggings"),
            armorBoots = register("armorBoots"),
            armor = register("armor"),
            frameGt = register("frameGt"),
            capcellcon = register("capcellcon"),
            fluidPipeTiny = register("fluidPipeTiny"),
            fluidPipeSmall = register("fluidPipeSmall"),
            fluidPipeMedium = register("fluidPipeMedium"),
            fluidPipeLarge = register("fluidPipeLarge"),
            fluidPipeHuge = register("fluidPipeHuge"),
            fluidPipeQuadruple = register("fluidPipeQuadruple"),
            fluidPipeNonuple = register("fluidPipeNonuple"),
            pipeRestrictiveTiny = register("pipeRestrictiveTiny"),
            pipeRestrictiveSmall = register("pipeRestrictiveSmall"),
            pipeRestrictiveLarge = register("pipeRestrictiveLarge"),
            pipeRestrictiveHuge = register("pipeRestrictiveHuge"),
            pipe = register("pipe"),
            wireGt16 = register("wireGt16"),
            wireGt15 = register("wireGt15"),
            wireGt14 = register("wireGt14"),
            wireGt13 = register("wireGt13"),
            wireGt12 = register("wireGt12"),
            wireGt11 = register("wireGt11"),
            wireGt10 = register("wireGt10"),
            wireGt09 = register("wireGt09"),
            wireGt08 = register("wireGt08"),
            wireGt07 = register("wireGt07"),
            wireGt06 = register("wireGt06"),
            wireGt05 = register("wireGt05"),
            wireGt04 = register("wireGt04"),
            wireGt03 = register("wireGt03"),
            wireGt02 = register("wireGt02"),
            wireGt01 = register("wireGt01"),
            cableGt12 = register("cableGt12"),
            cableGt08 = register("cableGt08"),
            cableGt04 = register("cableGt04"),
            cableGt02 = register("cableGt02"),
            cableGt01 = register("cableGt01"),
            orebush = register("orebush"),
            oreberry = register("oreberry"),
            plantGtBerry = register("plantGtBerry"),
            plantGtTwig = register("plantGtTwig"),
            plantGtFiber = register("plantGtFiber"),
            plantGtWart = register("plantGtWart"),
            plantGtBlossom = register("plantGtBlossom"),
            compressedCobblestone = register("compressedCobblestone"),
            compressedStone = register("compressedStone"),
            compressedDirt = register("compressedDirt"),
            compressedGravel = register("compressedGravel"),
            compressedSand = register("compressedSand"),
            blockBamboo = register("blockBamboo"),
            blockGlass = register("blockGlass"),
            blockWool = register("blockWool"),
            block_ = register("block_"),
            block = register("block"),
            item_ = register("item_"),
            item = register("item"),
            glass = register("glass"),
            paneGlass = register("paneGlass"),
            stainedClay = register("stainedClay"),
            craftingTool = register("craftingTool"),
            crafting = register("crafting"),
            craft = register("craft"),
            slab = register("slab"),
            stair = register("stair"),
            fence = register("fence"),
            treeSapling = register("treeSapling"),
            treeLeaves = register("treeLeaves"),
            tree = register("tree"),
            log = register("log"),
            beam = register("beam"),
            plank = register("plank"),
            stoneCobble = register("stoneCobble"),
            stoneSmooth = register("stoneSmooth"),
            stoneMossyBricks = register("stoneMossyBricks"),
            stoneMossy = register("stoneMossy"),
            stoneBricks = register("stoneBricks"),
            stoneCracked = register("stoneCracked"),
            stoneChiseled = register("stoneChiseled"),
            stonePolished = register("stonePolished"),
            stone = register("stone"),
            cobblestone = register("cobblestone"),
            rock = register("rock"),
            record = register("record"),
            scraps = register("scraps"),
            scrap = register("scrap"),
            book = register("book"),
            paper = register("paper"),
            dye = register("dye"),
            dyeMixable = register("dyeMixable"),
            dyeCeramic = register("dyeCeramic"),
            batterySingleuse = register("batterySingleuse"),
            battery = register("battery"),
            circuit = register("circuit"),
            computer = register("computer"),
            shard = register("shard"),
            sand = register("sand"),
            wire = register("wire"),
            lamp = register("lamp"),
            cloth = register("cloth"),
            fabric = register("fabric"),
            quartz = register("quartz"),
            part = register("part"),
            torch = register("torch"),
            skull = register("skull"),
            plating = register("plating"),
            dinosaur = register("dinosaur"),
            travelgear = register("travelgear"),
            bauble = register("bauble"),
            grafter = register("grafter"),
            scoop = register("scoop"),
            frame = register("frame"),
            tome = register("tome"),
            junk = register("junk"),
            bee = register("bee"),
            rod = register("rod"),
            dirt = register("dirt"),
            grass = register("grass"),
            gravel = register("gravel"),
            mushroom = register("mushroom"),
            wood = register("wood"),
            drop = register("drop"),
            fuel = register("fuel"),
            panel = register("panel"),
            brick = register("brick"),
            seed = register("seed"),
            reed = register("reed"),
            sheetDouble = register("sheetDouble"),
            sheet = register("sheet"),
            crop = register("crop"),
            plant = register("plant"),
            coin = register("coin"),
            lumar = register("lumar"),
            ground = register("ground"),
            cable = register("cable"),
            component = register("component"),
            pole = register("pole"),
            desert = register("desert"),
            jungle = register("jungle"),
            savanna = register("savanna"),
            beach = register("beach"),
            forest = register("forest"),
            mountain = register("mountain"),
            plains = register("plains"),
            epiphyte = register("epiphyte"),
            water = register("water"),
            river = register("river"),
            ocean = register("ocean"),
            hanging = register("hanging"),
            floating = register("floating"),
            wetlands = register("wetlands"),
            fern = register("fern"),
            vine = register("vine"),
            fungus = register("fungus"),
            cactus = register("cactus"),
            bud = register("bud"),
            immersed = register("immersed"),
            bamboo = register("bamboo"),
            cones = register("cones"),
            consumable = register("consumable"),
            leafy = register("leafy"),
            leaf = register("leaf"),
            shrub = register("shrub"),
            berrybush = register("berrybush"),
            wax = register("wax"),
            wall = register("wall"),
            tube = register("tube"),
            list = register("list"),
            food = register("food"),
            gear = register("gear"),
            coral = register("coral"),
            flower = register("flower"),
            storage = register("storage"),
            material = register("material"),
            plasma = register("plasma"),
            element = register("element"),
            molecule = register("molecule"),
            wafer = register("wafer"),
            orb = register("orb"),
            handle = register("handle"),
            blade = register("blade"),
            head = register("head"),
            motor = register("motor"),
            bowl = register("bowl"),
            bit = register("bit"),
            shears = register("shears"),
            turbine = register("turbine"),
            fertilizer = register("fertilizer"),
            chest = register("chest"),
            raw = register("raw"),
            stainedGlass = register("stainedGlass"),
            mystic = register("mystic"),
            mana = register("mana"),
            rune = register("rune"),
            petal = register("petal"),
            pearl = register("pearl"),
            powder = register("powder"),
            soulsand = register("soulsand"),
            obsidian = register("obsidian"),
            glowstone = register("glowstone"),
            beans = register("beans"),
            essence = register("essence"),
            alloy = register("alloy"),
            cooking = register("cooking"),
            gate = register("gate"),
            ladder = register("ladder"),
            door = register("door"),
            trapdoor = register("trapdoor"),
            elven = register("elven"),
            reactor = register("reactor"),
            mffs = register("mffs"),
            projred = register("projred"),
            ganys = register("ganys"),
            liquid = register("liquid"),
            chipset = register("chipset"),
            boule = register("boule"),
            lump = register("lump"),
            pellet = register("pellet"),
            tiny = register("tiny"),
            bars = register("bars"),
            bar = register("bar");

    /** Block prefixes from GT6 {@code OP} (storage blocks, casings, partial crates). */
    public static Entry casingMachine;
    public static Entry casingMachineDouble;
    public static Entry casingMachineQuadruple;
    public static Entry casingMachineDense;
    public static Entry crateGtRaw;
    public static Entry crateGtGem;
    public static Entry crateGtDust;
    public static Entry crateGtIngot;
    public static Entry crateGtPlate;
    public static Entry crateGtPlateGem;
    public static Entry crateGt64Raw;
    public static Entry crateGt64Gem;
    public static Entry crateGt64Dust;
    public static Entry crateGt64Ingot;
    public static Entry crateGt64Plate;
    public static Entry crateGt64PlateGem;
    public static Entry blockRaw;
    public static Entry blockGem;
    public static Entry blockDust;
    public static Entry blockIngot;
    public static Entry blockPlate;
    public static Entry blockPlateGem;
    public static Entry blockSolid;
    public static Entry oreBlock;
    public static Entry oreSmallBlock;

    static {
        defineBlocks();
    }

    private static void defineBlocks() {
        if (blocksDefined) {
            return;
        }
        PrefixRegistry.ensurePrefixesLoaded();

        // GT6 OP.java:186-…: the machine casings are `PARTS` — not "smithable parts of an ingot/gem
        // material" as the port approximated, which left PARTS materials such as the element metals
        // (Trinium, Naquadah, Adamantium …) and the plastics (Teflon, PVC, Bakelite) without casings.
        Predicate<GTMaterial> hasSmithableParts = m ->
                m.has(MaterialProperty.GENERATE_PARTS)
                        && m.has(MaterialProperty.SMITHABLE)
                        && (MaterialPrefix.ingot.isValidFor(m) || MaterialPrefix.gem.isValidFor(m))
                || MaterialForms.has(m, "PARTS");

        casingMachine = registerBlock(block("casingMachine")
                .display("Machine Casings", "", " Machine Casing")
                .condition(hasSmithableParts)
                .materialWeight(GTValues.U * 8)
                .physics(1.0F, 3.0F, 0, MapColor.METAL, SoundType.METAL));
        BlockMaterialPrefix.casingMachine = casingMachine.getBlockDelegate();

        casingMachineDouble = registerBlock(block("casingMachineDouble")
                .display("Robust Machine Casings", "Robust ", " Machine Casing")
                .condition(hasSmithableParts)
                .materialWeight(GTValues.U * 14)
                .physics(2.0F, 6.0F, 0, MapColor.METAL, SoundType.METAL));
        BlockMaterialPrefix.casingMachineDouble = casingMachineDouble.getBlockDelegate();

        casingMachineQuadruple = registerBlock(block("casingMachineQuadruple")
                .display("Reinforced Machine Casings", "Reinforced ", " Machine Casing")
                .condition(hasSmithableParts)
                .materialWeight(GTValues.U * 26)
                .physics(4.0F, 12.0F, 0, MapColor.METAL, SoundType.METAL));
        BlockMaterialPrefix.casingMachineQuadruple = casingMachineQuadruple.getBlockDelegate();

        casingMachineDense = registerBlock(block("casingMachineDense")
                .display("Dense Machine Casings", "Dense ", " Machine Casing")
                .condition(hasSmithableParts)
                .materialWeight(GTValues.U * 56)
                .physics(9.0F, 18.0F, 0, MapColor.METAL, SoundType.METAL));
        BlockMaterialPrefix.casingMachineDense = casingMachineDense.getBlockDelegate();

        crateGtRaw = registerBlock(block("crateGtRaw")
                .display("Crates of Ore", "Partial Crate of ", " Ore")
                .condition(MaterialPrefix.oreRaw)
                .materialWeight(GTValues.U * 32)
                .physics(1.0F, 0.2F, 0, MapColor.WOOD, SoundType.WOOD));
        BlockMaterialPrefix.crateGtRaw = crateGtRaw.getBlockDelegate();

        crateGtGem = registerBlock(block("crateGtGem")
                .display("Crates of Gems", "Partial Crate of ", " Gems")
                .condition(MaterialPrefix.gem)
                .materialWeight(GTValues.U * 16)
                .physics(1.0F, 0.2F, 0, MapColor.WOOD, SoundType.WOOD));
        BlockMaterialPrefix.crateGtGem = crateGtGem.getBlockDelegate();

        crateGtDust = registerBlock(block("crateGtDust")
                .display("Crates of Dust", "Partial Crate of ", " Dusts")
                .condition(MaterialPrefix.dust)
                .materialWeight(GTValues.U * 16)
                .physics(1.0F, 0.2F, 0, MapColor.WOOD, SoundType.WOOD));
        BlockMaterialPrefix.crateGtDust = crateGtDust.getBlockDelegate();

        crateGtIngot = registerBlock(block("crateGtIngot")
                .display("Crates of Ingots", "Partial Crate of ", " Ingots")
                .condition(MaterialPrefix.ingot)
                .materialWeight(GTValues.U * 16)
                .physics(1.0F, 0.2F, 0, MapColor.WOOD, SoundType.WOOD));
        BlockMaterialPrefix.crateGtIngot = crateGtIngot.getBlockDelegate();

        crateGtPlate = registerBlock(block("crateGtPlate")
                .display("Crates of Plates", "Partial Crate of ", " Plates")
                .condition(MaterialPrefix.plate)
                .materialWeight(GTValues.U * 16)
                .physics(1.0F, 0.2F, 0, MapColor.WOOD, SoundType.WOOD));
        BlockMaterialPrefix.crateGtPlate = crateGtPlate.getBlockDelegate();

        crateGtPlateGem = registerBlock(block("crateGtPlateGem")
                .display("Crates of Gem Plates", "Partial Crate of ", " Gem Plates")
                .condition(MaterialPrefix.plateGem)
                .materialWeight(GTValues.U * 16)
                .physics(1.0F, 0.2F, 0, MapColor.WOOD, SoundType.WOOD));
        BlockMaterialPrefix.crateGtPlateGem = crateGtPlateGem.getBlockDelegate();

        crateGt64Raw = registerBlock(block("crateGt64Raw")
                .display("Crates of Ore", "Crate of ", " Ore")
                .texture("crateGtRaw")
                .condition(MaterialPrefix.oreRaw)
                .materialWeight(GTValues.U * 128)
                .physics(1.0F, 0.2F, 0, MapColor.WOOD, SoundType.WOOD));
        BlockMaterialPrefix.crateGt64Raw = crateGt64Raw.getBlockDelegate();

        crateGt64Gem = registerBlock(block("crateGt64Gem")
                .display("Crates of Gems", "Crate of ", " Gems")
                .texture("crateGtGem")
                .condition(MaterialPrefix.gem)
                .materialWeight(GTValues.U * 64)
                .physics(1.0F, 0.2F, 0, MapColor.WOOD, SoundType.WOOD));
        BlockMaterialPrefix.crateGt64Gem = crateGt64Gem.getBlockDelegate();

        crateGt64Dust = registerBlock(block("crateGt64Dust")
                .display("Crates of Dust", "Crate of ", " Dusts")
                .texture("crateGtDust")
                .condition(MaterialPrefix.dust)
                .materialWeight(GTValues.U * 64)
                .physics(1.0F, 0.2F, 0, MapColor.WOOD, SoundType.WOOD));
        BlockMaterialPrefix.crateGt64Dust = crateGt64Dust.getBlockDelegate();

        crateGt64Ingot = registerBlock(block("crateGt64Ingot")
                .display("Crates of Ingots", "Crate of ", " Ingots")
                .texture("crateGtIngot")
                .condition(MaterialPrefix.ingot)
                .materialWeight(GTValues.U * 64)
                .physics(1.0F, 0.2F, 0, MapColor.WOOD, SoundType.WOOD));
        BlockMaterialPrefix.crateGt64Ingot = crateGt64Ingot.getBlockDelegate();

        crateGt64Plate = registerBlock(block("crateGt64Plate")
                .display("Crates of Plates", "Crate of ", " Plates")
                .texture("crateGtPlate")
                .condition(MaterialPrefix.plate)
                .materialWeight(GTValues.U * 64)
                .physics(1.0F, 0.2F, 0, MapColor.WOOD, SoundType.WOOD));
        BlockMaterialPrefix.crateGt64Plate = crateGt64Plate.getBlockDelegate();

        crateGt64PlateGem = registerBlock(block("crateGt64PlateGem")
                .display("Crates of Gem Plates", "Crate of ", " Gem Plates")
                .texture("crateGtPlateGem")
                .condition(MaterialPrefix.plateGem)
                .materialWeight(GTValues.U * 64)
                .physics(1.0F, 0.2F, 0, MapColor.WOOD, SoundType.WOOD));
        BlockMaterialPrefix.crateGt64PlateGem = crateGt64PlateGem.getBlockDelegate();

        blockRaw = registerBlock(block("blockRaw")
                .display("Blocks of Ore", "Block of ", " Ore")
                .condition(MaterialPrefix.oreRaw)
                .materialWeight(GTValues.U * 18)
                .physics(1.5F, 4.5F, 0, MapColor.STONE, SoundType.GRAVEL));
        BlockMaterialPrefix.blockRaw = blockRaw.getBlockDelegate();

        blockGem = registerBlock(block("blockGem")
                .display("Blocks of Gems", "Block of ", "")
                .condition(MaterialPrefix.gem)
                .materialWeight(GTValues.U * 9)
                .physics(1.5F, 4.5F, 0, MapColor.STONE, SoundType.STONE));
        BlockMaterialPrefix.blockGem = blockGem.getBlockDelegate();

        blockDust = registerBlock(block("blockDust")
                .display("Blocks of Dusts", "Block of ", " Dust")
                .condition(m -> MaterialPrefix.dust.isValidFor(m)
                        && !m.has(MaterialProperty.STONE)
                        && !"Sand".equals(m.getName())
                        && !"RedSand".equals(m.getName()))
                .materialWeight(GTValues.U * 9)
                .physics(0.5F, 4.5F, -2, MapColor.SAND, SoundType.SAND)
                .falling());
        BlockMaterialPrefix.blockDust = blockDust.getBlockDelegate();

        blockIngot = registerBlock(block("blockIngot")
                .display("Blocks of Ingots", "Block of ", " Ingots")
                .condition(MaterialPrefix.ingot)
                .materialWeight(GTValues.U * 9)
                .physics(1.0F, 3.0F, 0, MapColor.METAL, SoundType.METAL));
        BlockMaterialPrefix.blockIngot = blockIngot.getBlockDelegate();

        blockPlate = registerBlock(block("blockPlate")
                .display("Blocks of Plates", "Block of ", " Plates")
                .condition(MaterialPrefix.plate)
                .materialWeight(GTValues.U * 9)
                .physics(1.0F, 3.0F, 0, MapColor.METAL, SoundType.METAL));
        BlockMaterialPrefix.blockPlate = blockPlate.getBlockDelegate();

        blockPlateGem = registerBlock(block("blockPlateGem")
                .display("Blocks of Gem Plates", "Block of ", " Gem Plates")
                .condition(MaterialPrefix.plateGem)
                .materialWeight(GTValues.U * 9)
                .physics(1.0F, 3.0F, 0, MapColor.METAL, SoundType.METAL));
        BlockMaterialPrefix.blockPlateGem = blockPlateGem.getBlockDelegate();

        blockSolid = registerBlock(block("blockSolid")
                .display("Blocks of Cast Metal", "Block of solid ", "")
                .condition(m -> blockIngot.getBlockDelegate().isValidFor(m))
                .materialWeight(GTValues.U * 9)
                .physics(1.7F, 5.0F, 0, MapColor.METAL, SoundType.METAL));
        BlockMaterialPrefix.blockSolid = blockSolid.getBlockDelegate();

        // Standard yields: ore = 2 units, small ore = 1 unit (specials may differ per material).
        // GT6 OP.java:152/153: both ore prefixes are `ORES` — every material GT6 gives an ore to has
        // one (all of `G_INGOT_MACHINE_ORES`, 483 of the 1061 joined port materials, of which 358 are
        // declared through a non-ore factory here), so the flag is consulted next to the port property.
        oreBlock = registerBlock(block("ore")
                .display("Ores", "", " Ore")
                .condition(m -> m.has(MaterialProperty.GENERATE_ORE) || MaterialForms.has(m, "ORES"))
                .materialWeight(GTValues.U * 2)
                .physics(3.0F, 3.0F, 0, MapColor.STONE, SoundType.STONE));
        BlockMaterialPrefix.ore = oreBlock.getBlockDelegate();

        oreSmallBlock = registerBlock(block("oreSmall")
                .display("Small Ores", "Small ", " Ore")
                .condition(m -> m.has(MaterialProperty.GENERATE_ORE) || MaterialForms.has(m, "ORES"))
                .materialWeight(GTValues.U)
                .physics(3.0F, 3.0F, 0, MapColor.STONE, SoundType.STONE));
        BlockMaterialPrefix.oreSmall = oreSmallBlock.getBlockDelegate();

        blocksDefined = true;
    }

    public static Map<String, Entry> all() {
        return Map.copyOf(REGISTRY);
    }

    public static void bootstrap() {
        PrefixRegistry.ensurePrefixesLoaded();
        defineBlocks();
        if (dust == null || blockIngot == null || blockIngot.getBlockDelegate() == null) {
            throw new IllegalStateException("OP registry failed to initialize");
        }
    }
}
