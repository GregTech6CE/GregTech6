package gregtech.content.recipe;

import gregtech.api.material.GTValues;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * GT6's generifier rule: every material whose {@code mTargetGenerifying} differs from itself becomes
 * one {@code RecipeMapHandlerMaterial} on {@code RM.Generifier}, and that handler expands over every
 * prefix the condition {@code And(SIMPLIFIABLE, ingotHot.NOT)} accepts. Each row converts one item of
 * a form in the source material into one item of the same form in the target material; the duration
 * is the form's material weight in ticks, the EU cost is zero, and nothing converts back.
 *
 * <p>The material targets are {@code MT.java}'s {@code setGenerifying} calls plus the
 * {@code ANY.*.mToThis} loops that follow them, keyed by GT6 material id so the table does not depend
 * on display names. {@code ANY} groups that point at a plain material other than their own name
 * (ashes, gems, woods, wax, rubber, plastics, phosphorus, clay, silicon) are already folded into the
 * targets, because the loops overwrite the explicit rows they contain.</p>
 */
public final class GenerifyingRules {
    private GenerifyingRules() {}

    /** {@code MT.java} {@code setGenerifying} rows and {@code ANY.*.mToThis} loops, as source/target ids. */
    private static final int[] TARGET_IDS = {
        8196, 8218, 8197, 8218, 8198, 8218, 8199,
                8218, // Teflon->Plastic; PVC->Plastic; Bakelite->Plastic; HardPlastic->Plastic
        8201, 8200, 8202, 8200, 8222, 8221, 8224,
                8221, // DarkAshes->Ashes; VolcanicAshes->Ashes; WoodTreated->Wood; WoodRubber->Wood
        8227, 8221, 8236, 8235, 8237, 8235, 8238,
                8235, // PeanutWood->Wood; WaxBee->Wax; WaxRefractory->Wax; WaxParaffin->Wax
        8239, 8235, 8240, 8235, 8267, 8221, 8276,
                8215, // WaxPlant->Wax; WaxMagic->Wax; WoodPolished->Wood; ClayBrown->Clay
        8278, 8300, 8279, 8300, 8280, 8235, 8281,
                8235, // ManaDiamond->Diamond; ElvenDragonstone->Diamond; WaxAmnesic->Wax; WaxSoulful->Wax
        8286, 8221, 8289, 8221, 8290, 8221, 8291,
                8221, // Weedwood->Wood; Livingwood->Wood; Dreamwood->Wood; Skyroot->Wood
        8292, 8308, 8294, 8300, 8296, 8221, 8297,
                8221, // Zanite->Tanzanite; Gravitite->Diamond; Greatwood->Wood; Silverwood->Wood
        8302, 8304, 8305, 8304, 8307, 8306, 8314,
                8304, // Ruby->Sapphire; GreenSapphire->Sapphire; BlueTopaz->Topaz; OrangeSapphire->Sapphire
        8315, 8304, 8323, 8301, 8324, 8301, 8328,
                8304, // YellowSapphire->Sapphire; Aquamarine->Emerald; Morganite->Emerald; BlueSapphire->Sapphire
        8329, 8313, 8346, 8445, 8347, 8445, 8348,
                8347, // AmethystEnder->Amethyst; NetherQuartz->MilkyQuartz; CertusQuartz->MilkyQuartz;
        // ChargedCertusQuartz->CertusQuartz
        8368, 8341, 8369, 8341, 8370, 8341, 8371,
                8341, // GlowstoneCeres->Glowstone; GlowstoneIo->Glowstone; GlowstoneEnceladus->Glowstone;
        // GlowstoneProteus->Glowstone
        8372, 8341, 8374, 8445, 8375, 8333, 8376,
                8332, // GlowstonePluto->Glowstone; QuartzBlack->MilkyQuartz; Redstonia->Redstone; Palis->Lapis
        8377, 8300, 8378, 8334, 8379, 8301, 8380,
                260, // Diamantine->Diamond; VoidCrystal->Coal; Emeradic->Emerald; Enori->Iron
        8383, 8304, 8384, 8301, 8385, 8301, 8386,
                8301, // PurpleSapphire->Sapphire; Heliodor->Emerald; Goshenite->Emerald; Bixbite->Emerald
        8387, 8301, 8393, 8445, 8394, 8445, 8395,
                8445, // Maxixe->Emerald; SunnyQuartz->MilkyQuartz; LavenderQuartz->MilkyQuartz; RedQuartz->MilkyQuartz
        8396, 8445, 8397, 8445, 8398, 8445, 8399,
                8445, // BlazeQuartz->MilkyQuartz; SmokeyQuartz->MilkyQuartz; ManaQuartz->MilkyQuartz;
        // ElvenQuartz->MilkyQuartz
        8414, 8221, 8418, 8221, 8422, 8310, 8423,
                8300, // Shimmerwood->Wood; Bamboo->Wood; DominicanAmber->Amber; DiamondIndustrial->Diamond
        8424, 8300, 8425, 8309, 8426, 8309, 8427,
                8309, // PinkDiamond->Diamond; OceanJasper->RedJasper; RainforestJasper->RedJasper;
        // BlueJasper->RedJasper
        8428, 8309, 8429, 8309, 8431, 8430, 8432,
                8430, // GreenJasper->RedJasper; YellowJasper->RedJasper; CatsEye->TigerEye; DragonEye->TigerEye
        8433, 8430, 8434, 8430, 8435, 8430, 8436,
                9215, // HawksEye->TigerEye; BlackEye->TigerEye; TigerIron->TigerEye; RedFluorite->Fluorite
        8437, 9215, 8438, 9215, 8439, 9215, 8440,
                9215, // PinkFluorite->Fluorite; BlueFluorite->Fluorite; GreenFluorite->Fluorite;
        // BlackFluorite->Fluorite
        8441, 9215, 8442, 9215, 8443, 9215, 8444,
                9215, // WhiteFluorite->Fluorite; YellowFluorite->Fluorite; OrangeFluorite->Fluorite;
        // MagentaFluorite->Fluorite
        8454, 8316, 8455, 8215, 8456, 8341, 8457,
                8346, // Chimerite->Vinteum; ClayRed->Clay; Gloomstone->Glowstone; VoidQuartz->NetherQuartz
        8458, 8208, 8459, 8208, 8460, 8208, 8464,
                8300, // BluePhosphorus->Phosphorus; RedPhosphorus->Phosphorus; WhitePhosphorus->Phosphorus;
        // BlueDiamond->Diamond
        8465, 8300, 8466, 8300, 8467, 8300, 8468,
                8300, // GreenDiamond->Diamond; PurpleDiamond->Diamond; RedDiamond->Diamond; YellowDiamond->Diamond
        8469, 8310, 8501, 8500, 8503, 8502, 8504,
                8105, // GoldenAmber->Amber; Concrete->Stone; NetherBrick->Netherrack; Endstone->WhiteEndSand
        8505, 8500, 8506, 8500, 8507, 8500, 8508,
                8500, // Basalt->Stone; Marble->Stone; GraniteRed->Stone; GraniteBlack->Stone
        8509, 8500, 8511, 8500, 8512, 8500, 8513,
                8500, // Redrock->Stone; Diorite->Stone; SpaceStone->Stone; MoonStone->Stone
        8514, 8500, 8515, 8500, 8516, 8500, 8517,
                8500, // MoonTurf->Stone; MarsStone->Stone; MarsSand->Stone; Umber->Stone
        8518, 8500, 8519, 8500, 8520, 8500, 8521,
                8500, // Granite->Stone; Betweenstone->Stone; Pitstone->Stone; Livingrock->Stone
        8522, 8500, 8523, 8500, 8524, 8500, 8525,
                8500, // Holystone->Stone; Deadrock->Stone; Cragrock->Stone; Templerock->Stone
        8526, 8500, 8527, 8500, 8528, 8500, 8599,
                8500, // Mazestone->Stone; Castlerock->Stone; SkyStone->Stone; Bedrock->Stone
        8640, 290, 8642, 260, 8643, 260, 8644,
                260, // AnnealedCopper->Copper; PigIron->Iron; WroughtIron->Iron; IronCompressed->Iron
        8645, 260, 8646, 8630, 8647, 600, 8649,
                260, // IronMagnetic->Iron; SteelMagnetic->Steel; NeodymiumMagnetic->Neodymium; MeteoricIron->Iron
        8650, 8630, 8652, 740, 8674, 8630, 8675,
                8630, // MeteoricSteel->Steel; TungstenSintered->Tungsten; Knightmetal->Steel; FierySteel->Steel
        8676, 470, 8677, 790, 8678, 780, 8679,
                260, // AstralSilver->Silver; Midasium->Gold; Mithril->Platinum; Thaumium->Iron
        8680, 8679, 8690, 8631, 8691, 8632, 8692,
                8633, // DarkThaumium->Thaumium; MeteoricBlackSteel->BlackSteel; MeteoricBlueSteel->BlueSteel;
        // MeteoricRedSteel->RedSteel
        8693, 8650, 8694, 8690, 8695, 8691, 8696,
                8692, // MeteoflameSteel->MeteoricSteel; MeteoflameBlackSteel->MeteoricBlackSteel;
        // MeteoflameBlueSteel->MeteoricBlueSteel; MeteoflameRedSteel->MeteoricRedSteel
        8697, 8634, 8698, 8673, 8701, 8707, 8720,
                260, // FlamascusSteel->DamascusSteel; Fireleaf->Steeleaf; Aredrite->Ardite; Manasteel->Iron
        8734, 260, 8757, 260, 8769, 8620, 8803,
                260, // SpectreIron->Iron; Meteorite->Iron; Orichalcum->Brass; CastIron->Iron
        9000, 8500, 9153, 8215, 9154, 8215, 9167,
                8215, // Pumice->Stone; Bentonite->Clay; Palygorskite->Clay; Kaolinite->Clay
        9170, 8500, 9171, 8500, 9172, 8500, 9173,
                8500, // Gneiss->Stone; Greenschist->Stone; Greenstone->Stone; Greywacke->Stone
        9176, 8500, 9177, 8500, 9178, 8500, 9179,
                8500, // Gabbro->Stone; Komatiite->Stone; Siltstone->Stone; Rhyolite->Stone
        9180, 8500, 9181, 8500, 9182, 8500, 9184,
                8500, // Quartzite->Stone; Migmatite->Stone; Epidote->Stone; Blueschist->Stone
        9185, 8500, 9186, 8500, 9187, 8500, 9188,
                8500, // Bluestone->Stone; Chert->Stone; Dacite->Stone; Andesite->Stone
        9189, 8500, 9190, 8500, 9191, 8500, 9218,
                8500, // Limestone->Stone; Shale->Stone; Eclogite->Stone; Kimberlite->Stone
        9222, 8500, 9223, 8500, 9244, 8500, 9245,
                8500, // Slate->Stone; Blackstone->Stone; Grayschist->Stone; Pinkschist->Stone
        9248, 8500, 9249, 8500, 9250, 8500, 9251,
                8500, // Deepslate->Stone; PhobosRock->Stone; DeimosRock->Stone; VenusRock->Stone
        9252, 8500, 9253, 8500, 9254, 8500, 9255,
                8500, // MercuryRock->Stone; CeresRock->Stone; JupiterRock->Stone; IoRock->Stone
        9256, 8500, 9257, 8500, 9258, 8500, 9259,
                8500, // EuropaRock->Stone; GanymedeRock->Stone; CallistoRock->Stone; SaturnRock->Stone
        9260, 8500, 9261, 8500, 9262, 8500, 9263,
                8500, // RheaRock->Stone; TitanRock->Stone; OberonRock->Stone; IapetusRock->Stone
        9264, 8500, 9265, 8500, 9266, 8500, 9267,
                8500, // UranusRock->Stone; TitaniaRock->Stone; NeptuneRock->Stone; TritonRock->Stone
        9268, 8500, 9269, 8500, 9270, 8500, 9300,
                8221, // PlutoRock->Stone; ErisRock->Stone; Kepler22bRock->Stone; Oak->Wood
        9301, 8221, 9302, 8221, 9303, 8221, 9304, 8221, // Birch->Wood; Spruce->Wood; Junglewood->Wood; Acacia->Wood
        9305, 8221, 9306, 8221, 9307, 8221, 9308,
                8221, // DarkOak->Wood; Crimsonwood->Wood; Warpedwood->Wood; WoodCompressed->Wood
        9309, 8221, 9310, 8221, 9311, 8221, 9312,
                8221, // WoodDead->Wood; WoodRotten->Wood; WoodMossy->Wood; WoodFrozen->Wood
        9313, 8221, 9314, 8221, 9315, 8221, 9316, 8221, // Maple->Wood; Willow->Wood; BlueMahoe->Wood; Hazel->Wood
        9317, 8221, 9318, 8221, 9319, 8221, 9320,
                8221, // Cinnamonwood->Wood; Coconutwood->Wood; Rainbowood->Wood; Towerwood->Wood
        9321, 8221, 9322, 8221, 9323, 8221, 9324,
                8221, // Witchwood->Wood; Ogrewood->Wood; Wyvernwood->Wood; Aspen->Wood
        9325, 8221, 9326, 8221, 9327, 8221, 9328,
                8221, // DouglasFir->Wood; Sycamore->Wood; WhiteCedar->Wood; WhiteElm->Wood
        9329, 8221, 9330, 8221, 9331, 8221, 9332,
                8221, // Thorntree->Wood; SilverPine->Wood; Alder->Wood; Hawthorn->Wood
        9333, 8221, 9334, 8221, 9335, 8221, 9336, 8221, // Rowan->Wood; Autumnwood->Wood; Pine->Wood; Cypress->Wood
        9337, 8221, 9338, 8221, 9339, 8221, 9340,
                8221, // Fir->Wood; JapaneseMaple->Wood; RainbowEucalyptus->Wood; Redwood->Wood
        9341, 8221, 9342, 8221, 9343, 8221, 9344, 8221, // Sakura->Wood; Balsa->Wood; Baobab->Wood; Cherrywood->Wood
        9345, 8221, 9346, 8221, 9347, 8221, 9348,
                8221, // Chestnutwood->Wood; Citruswood->Wood; Cocobolowood->Wood; Ebony->Wood
        9349, 8221, 9350, 8221, 9351, 8221, 9352, 8221, // Giganteumwood->Wood; Greenheart->Wood; Ipe->Wood; Kapok->Wood
        9353, 8221, 9354, 8221, 9355, 8221, 9356, 8221, // Larch->Wood; Limewood->Wood; Mahoe->Wood; Mahogany->Wood
        9357, 8221, 9358, 8221, 9359, 8221, 9360, 8221, // Padauk->Wood; Palm->Wood; Papayawood->Wood; Plumwood->Wood
        9361, 8221, 9362, 8221, 9363, 8221, 9364, 8221, // Poplar->Wood; Sequoia->Wood; Teak->Wood; Walnutwood->Wood
        9365, 8221, 9366, 8221, 9367, 8221, 9368,
                8221, // Wenge->Wood; Zebrawood->Wood; Darkwood->Wood; Etherealwood->Wood
        9369, 8221, 9370, 8221, 9371, 8221, 9372,
                8221, // Goldwood->Wood; Hellbark->Wood; Jacaranda->Wood; Mangrove->Wood
        9373, 8221, 9374, 8221, 9375, 8221, 9376,
                8221, // SacredOak->Wood; Magicwood->Wood; Applewood->Wood; Ashwood->Wood
        9377, 8221, 9378, 8221, 9379, 8221, 9380,
                8221, // Beech->Wood; Boxwood->Wood; Brazilwood->Wood; Butternutwood->Wood
        9381, 8221, 9382, 8221, 9383, 8221, 9384, 8221, // Cedar->Wood; Elderwood->Wood; Elm->Wood; Eucalyptus->Wood
        9385, 8221, 9386, 8221, 9387, 8221, 9388, 8221, // Figwood->Wood; Gingko->Wood; Hemlock->Wood; Hickory->Wood
        9389, 8221, 9390, 8221, 9391, 8221, 9392, 8221, // Holly->Wood; Hornbeam->Wood; Iroko->Wood; Locust->Wood
        9393, 8221, 9394, 8221, 9395, 8221, 9396, 8221, // Logwood->Wood; Maclura->Wood; Olivewood->Wood; Pearwood->Wood
        9397, 8221, 9398, 8221, 9399, 8221, 9400,
                8221, // PinkIvory->Wood; Purpleheart->Wood; Rosewood->Wood; Sweetgum->Wood
        9401, 8221, 9402, 8221, 9403, 8221, 9404, 8221, // Syzgium->Wood; Whitebeam->Wood; Yew->Wood; WoodScorched->Wood
        9405, 8221, 9406, 8221, 9407, 8221, 9408,
                8221, // WoodVarnished->Wood; WoodTainted->Wood; WoodBleached->Wood; Foxfirewood->Wood
        9409, 8221, 9715, 8221, 9853, 9190, // BlueSpruce->Wood; Marshmallow->Wood; OilShale->Shale
    };

    /**
     * GT6 {@code OP.java} prefixes carrying {@code TD.Prefix.SIMPLIFIABLE}, under the port's prefix
     * name ({@code OP.casingSmall} is {@code itemCasing} and {@code OP.boule} is {@code bouleGt}).
     * {@code OP.ingotHot} carries no SIMPLIFIABLE tag, which is how hot ingots stay out of the rule.
     */
    private static final Set<String> SIMPLIFIABLE = Set.of(
            "arrowGtPlastic",
            "arrowGtWood",
            "billet",
            "blockDust",
            "blockGem",
            "blockIngot",
            "blockPlate",
            "blockPlateGem",
            "blockSolid",
            "bolt",
            "bouleGt",
            "bulletGtLarge",
            "bulletGtMedium",
            "bulletGtSmall",
            "cableGt01",
            "cableGt02",
            "cableGt04",
            "cableGt08",
            "cableGt12",
            "capcellcon",
            "casingMachine",
            "casingMachineDense",
            "casingMachineDouble",
            "casingMachineQuadruple",
            "itemCasing",
            "chain",
            "chunkGt",
            "compressed",
            "dust",
            "dustDiv72",
            "dustSmall",
            "dustTiny",
            "foil",
            "frameGt",
            "gearGt",
            "gearGtSmall",
            "gem",
            "gemChipped",
            "gemExquisite",
            "gemFlawed",
            "gemFlawless",
            "gemLegendary",
            "ingot",
            "ingotDouble",
            "ingotQuadruple",
            "ingotQuintuple",
            "ingotTriple",
            "lens",
            "minecartWheels",
            "nugget",
            "pipeHuge",
            "pipeLarge",
            "pipeMedium",
            "pipeNonuple",
            "pipeQuadruple",
            "pipeRestrictiveHuge",
            "pipeRestrictiveLarge",
            "pipeRestrictiveMedium",
            "pipeRestrictiveSmall",
            "pipeRestrictiveTiny",
            "pipeSmall",
            "pipeTiny",
            "plate",
            "plateCurved",
            "plateDense",
            "plateDouble",
            "plateGem",
            "plateGemTiny",
            "plateQuadruple",
            "plateQuintuple",
            "plateSteamcraft",
            "plateTiny",
            "plateTriple",
            "railGt",
            "ring",
            "rotor",
            "round",
            "scrapGt",
            "screw",
            "sheetGt",
            "spring",
            "springSmall",
            "stick",
            "stickLong",
            "wireFine",
            "wireGt01",
            "wireGt02",
            "wireGt03",
            "wireGt04",
            "wireGt05",
            "wireGt06",
            "wireGt07",
            "wireGt08",
            "wireGt09",
            "wireGt10",
            "wireGt11",
            "wireGt12",
            "wireGt13",
            "wireGt14",
            "wireGt15",
            "wireGt16");

    private static final Map<Integer, Integer> TARGETS;

    static {
        Map<Integer, Integer> targets = new HashMap<>(TARGET_IDS.length / 2);
        for (int i = 0; i < TARGET_IDS.length; i += 2) targets.put(TARGET_IDS[i], TARGET_IDS[i + 1]);
        TARGETS = Map.copyOf(targets);
    }

    /** The target material id, or {@code materialId} itself when the source has no conversion. */
    public static int target(int materialId) {
        Integer target = TARGETS.get(materialId);
        return target == null ? materialId : target;
    }

    /** Whether the source applies the material conversion to this form at all. */
    public static boolean simplifiable(String prefixName) {
        return SIMPLIFIABLE.contains(prefixName);
    }

    /** {@code RecipeMapHandlerMaterial.getCosts}: the form's material weight in ticks, at least one. */
    public static long duration(long formWeight) {
        return Math.max(1, (formWeight + GTValues.U - 1) / GTValues.U);
    }
}
