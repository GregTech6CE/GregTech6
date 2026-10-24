package gregtech.core;

import gregtech.api.addon.AddonMaterialContributor;
import gregtech.internal.addon.AddonMaterials;
import gregtech.api.addon.GregTechMaterialContributor;
import gregtech.api.addon.GregTechMaterials;
import gregtech.api.addon.MaterialDeclaration;
import gregtech.api.addon.MaterialFormRequest;
import gregtech.api.addon.OreVeinDeclaration;
import gregtech.api.addon.PrefixCondition;
import gregtech.api.addon.PrefixDeclaration;
import gregtech.api.addon.SmallOreDeclaration;
import gregtech.internal.machine.crucible.CrucibleInputRules;
import gregtech.api.material.GTValues;
import gregtech.internal.material.GTMaterial;
import gregtech.internal.material.MaterialProperty;
import gregtech.internal.material.GTMaterialRegistry;
import gregtech.internal.material.MaterialBlockDefinitions;
import gregtech.internal.prefix.BlockMaterialPrefix;
import gregtech.internal.prefix.BlockPrefixRegistry;
import gregtech.block.BlockMaterialWeights;
import gregtech.internal.material.MaterialDefinition;
import gregtech.internal.material.MaterialItemDefinitions;
import gregtech.data.generated.MaterialForms;
import gregtech.internal.mod.ModData;
import gregtech.internal.prefix.PrefixRegistry;
import gregtech.data.MaterialPrefix;
import gregtech.data.ModReferences;
import gregtech.worldgen.GTOreVeins;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/** Addon material queue, in its own JVM so a submission cannot change the core fingerprint process. */
public final class AddonMaterialContracts {
    private static final String ITEM_ID_SHA256 = "c3360a6cdc2ac4aa396b7a9bb778f11663171afb4e949709c65f3903f25a5fef";
    /** Core block ids in definition order, taken before addon ore blocks were appended. */
    private static final String BLOCK_ID_SHA256 = "2bf589f634a16f450fb0295757b342d7208793881c05d3315c36bde52c035735";
    private static int assertions;
    private static boolean linked;

    private AddonMaterialContracts() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Expected one scenario");
        Locale.setDefault(Locale.ROOT);
        switch (args[0]) {
            case "service" -> service();
            case "name" -> nameCollision();
            case "id" -> ignoredId();
            case "addons" -> addonMerge();
            case "range" -> ignoredHighId();
            case "form" -> ungeneratedForm();
            case "invalid" -> invalidForm();
            case "late" -> lateSubmitAndEmptyBand();
            case "mismatch" -> fieldMismatch();
            case "casefold" -> caseConflict();
            case "corecase" -> coreCaseConflict();
            case "order" -> nameOrder();
            case "named" -> namedDeclaration();
            case "scale" -> scale();
            case "softform" -> softPlate();
            case "softmiss" -> softPlateRejected();
            case "wires" -> wires();
            case "wireplate" -> wirePlateRejected();
            case "blockprefix" -> blockPrefix();
            case "blockform" -> blockFormRejected();
            case "oreraw" -> rawOreStillRejected();
            case "ores" -> ores();
            case "oremismatch" -> oreMismatch();
            case "orelist" -> oreListMismatch();
            case "oreunknown" -> oreUnknown();
            case "vein" -> vein();
            case "veinname" -> veinName();
            case "veinore" -> veinOre();
            case "veindup" -> veinDuplicate();
            case "veinbad" -> veinBad();
            case "prefix" -> prefix();
            case "prefixname" -> prefixName();
            case "prefixcase" -> prefixCase();
            case "prefixmiss" -> prefixMissing();
            case "prefixcond" -> prefixCondition();
            case "prefixexclude" -> prefixExclude();
            case "byproducts" -> byproducts();
            case "reads" -> reads();
            default -> throw new IllegalArgumentException("Unknown scenario: " + args[0]);
        }
        System.out.println(args[0] + " passed " + assertions);
    }

    private static void service() throws Exception {
        prepare();
        Path root = Files.createTempDirectory("gt-addon-materials");
        Path service = root.resolve("META-INF/services/gregtech.api.addon.GregTechMaterialContributor");
        Files.createDirectories(service.getParent());
        Files.writeString(service, SampleContributor.class.getName() + "\n", StandardCharsets.UTF_8);
        try (URLClassLoader loader = new URLClassLoader(new URL[]{root.toUri().toURL()},
                AddonMaterialContracts.class.getClassLoader())) {
            AddonMaterials.collectContributors(loader);
        }
        GTMaterialRegistry.init();
        GTMaterial added = GTMaterialRegistry.get("AmmoniumNitrate");
        check(added.isValid() && added.getId() == GTMaterialRegistry.ADDON_ID_START,
                "queued material is assigned the first addon id");
        check(!GTMaterialRegistry.isCoreMaterial(added), "queued material stays out of the core snapshot");
        check(GTMaterialRegistry.isCoreMaterial(GTMaterialRegistry.get("Iron")), "Iron remains core");
        check("aa_sample".equals(AddonMaterials.owner(added)), "the addon id is recorded on the material");
        check(added.getSourceMod() != null && "aa_sample".equals(added.getSourceMod().id), "source mod is the addon");
        check(linked, "addon link runs after the built-in directory");
        check(added.getTargetPulverMaterial() == GTMaterialRegistry.get("Iron"), "link can point at a core material");
        check(ITEM_ID_SHA256.equals(coreAutomaticFingerprint()), "extra forms do not change built-in automatic item ids");
        int nitrate = 0;
        int iron = 0;
        int ironDust = 0;
        Set<MaterialPrefix> automatic = Set.copyOf(MaterialItemDefinitions.candidatePrefixes());
        for (MaterialItemDefinitions.Definition definition : MaterialItemDefinitions.all()) {
            if ("AmmoniumNitrate".equals(definition.material().getName())) {
                nitrate++;
                check(!definition.hasIdSuffix(), "new material keeps its base item id: " + definition.itemId());
                check(automatic.contains(definition.prefix()), "new material items stay on generated prefixes: " + definition.itemId());
                check("aa_sample".equals(definition.namespace()),
                        "new material items use the addon namespace: " + definition.namespace() + ":" + definition.itemId());
            }
            if ("Iron".equals(definition.material().getName()) && automatic.contains(definition.prefix())) iron++;
            if ("dust_iron".equals(definition.itemId())) ironDust++;
        }
        check(nitrate > 0, "ammonium nitrate generates dust forms");
        check(ironDust == 1, "an already generated form is not emitted again");
        check(oldBandEmpty(), "addon ids stay above the old band");
        System.out.println("ammonium nitrate items=" + nitrate + " iron automatic items=" + iron);
    }

    private static void nameCollision() {
        prepare();
        AddonMaterials.submit("aa_name", List.of(MaterialDefinition.builder(4291, "Iron").dust().build()), List.of(), null);
        expectThrows("same name", "core material", "Iron", "aa_name");
    }

    private static void ignoredId() {
        prepare();
        var warnings = new ArrayList<String>();
        GTMaterialRegistry.setLogSink((warning, message) -> { if (warning) warnings.add(message); });
        AddonMaterials.submit("aa_id", List.of(MaterialDefinition.builder(260, "AddonIron").dust().build()), List.of(), null);
        GTMaterialRegistry.init();
        GTMaterial added = GTMaterialRegistry.get("AddonIron");
        check(added.isValid() && added.getId() == GTMaterialRegistry.ADDON_ID_START, "a built-in id is not claimed");
        check(GTMaterialRegistry.get(260).getName().equals("Iron"), "Iron keeps id 260");
        check(!GTMaterialRegistry.get(260).getName().equals("AddonIron"), "the declared number is not the addon id");
        check(warnings.stream().anyMatch(message -> message.contains("260") && message.contains("ignored")),
                "the ignored id is logged");
    }

    private static void addonMerge() {
        prepare();
        AddonMaterials.submit("aa_pair", List.of(MaterialDefinition.builder(4291, "SharedName").dust().build()),
                List.of(new MaterialFormRequest("Iron", "dust")), null);
        AddonMaterials.submit("zz_pair", List.of(MaterialDefinition.builder(4292, "SharedName").color(0xFFFFFF).dust().build()),
                List.of(new MaterialFormRequest("Iron", "dustSmall")), null);
        GTMaterialRegistry.init();
        GTMaterial shared = GTMaterialRegistry.get("SharedName");
        check(shared.isValid() && shared.getId() == GTMaterialRegistry.ADDON_ID_START, "matching declarations merge");
        check(AddonMaterials.owners(shared).equals(List.of("aa_pair", "zz_pair")), "both addons are recorded, in addon-id order");
        check(AddonMaterials.acceptedForms().stream().anyMatch(form -> form.prefixName().equals("dust"))
                        && AddonMaterials.acceptedForms().stream().anyMatch(form -> form.prefixName().equals("dustSmall")),
                "forms() is the union");
        for (var definition : MaterialItemDefinitions.all()) {
            if (!"SharedName".equals(definition.material().getName())) continue;
            check("aa_pair".equals(definition.namespace()),
                    "a shared material follows its first owner's namespace: " + definition.namespace());
        }
    }

    private static void ignoredHighId() {
        prepare();
        AddonMaterials.submit("aa_range", List.of(MaterialDefinition.builder(9998, "RangeProbe").dust().build()), List.of(), null);
        GTMaterialRegistry.init();
        GTMaterial added = GTMaterialRegistry.get("RangeProbe");
        check(added.isValid() && added.getId() == GTMaterialRegistry.ADDON_ID_START, "an old out-of-band id is not stored");
        check(!GTMaterialRegistry.get(9998).isValid(), "9998 stays empty");
    }

    private static void fieldMismatch() {
        prepare();
        AddonMaterials.submit("aa_pair", List.of(MaterialDefinition.builder(4291, "SharedName").color(0x111111).dust().build()), List.of(), null);
        AddonMaterials.submit("zz_pair", List.of(MaterialDefinition.builder(4292, "SharedName").color(0x222222).dust().build()), List.of(), null);
        expectThrows("field color", "aa_pair", "zz_pair", "SharedName");
    }

    private static void caseConflict() {
        prepare();
        AddonMaterials.submit("aa_case", List.of(MaterialDefinition.builder(1, "AddonCase").dust().build()), List.of(), null);
        AddonMaterials.submit("zz_case", List.of(MaterialDefinition.builder(1, "addoncase").dust().build()), List.of(), null);
        expectThrows("differs only by case", "AddonCase", "addoncase", "aa_case", "zz_case");
    }

    private static void nameOrder() {
        prepare();
        AddonMaterials.submit("aa_order", List.of(MaterialDefinition.builder(1, "OrderZeta").dust().build()), List.of(), null);
        AddonMaterials.submit("zz_order", List.of(MaterialDefinition.builder(9, "OrderAlpha").dust().build()), List.of(), null);
        GTMaterialRegistry.init();
        check(GTMaterialRegistry.get("OrderAlpha").getId() == GTMaterialRegistry.ADDON_ID_START, "names sort before addon-id order");
        check(GTMaterialRegistry.get("OrderZeta").getId() == GTMaterialRegistry.ADDON_ID_START + 1, "the later name gets the next id");
    }

    private static void coreCaseConflict() {
        prepare();
        AddonMaterials.submit("aa_iron", List.of(MaterialDefinition.builder(1, "iron").dust().build()), List.of(), null);
        expectThrows("differs only by case", "core material", "Iron", "aa_iron");
    }

    private static void namedDeclaration() throws Exception {
        prepare();
        var warnings = new ArrayList<String>();
        GTMaterialRegistry.setLogSink((warning, message) -> { if (warning) warnings.add(message); });
        Path root = Files.createTempDirectory("gt-addon-named");
        Path service = root.resolve("META-INF/services/gregtech.api.addon.AddonMaterialContributor");
        Files.createDirectories(service.getParent());
        Files.writeString(service, NamedContributor.class.getName() + "\n", StandardCharsets.UTF_8);
        try (URLClassLoader loader = new URLClassLoader(new URL[]{root.toUri().toURL()},
                AddonMaterialContracts.class.getClassLoader())) {
            AddonMaterials.collectContributors(loader);
        }
        GTMaterialRegistry.init();
        GTMaterial named = GTMaterialRegistry.get("NamedDust");
        GTMaterial numbered = GTMaterialRegistry.get("FromNumber");
        check(named.isValid() && named.getId() == GTMaterialRegistry.ADDON_ID_START + 1, "name order places FromNumber first");
        check(numbered.isValid() && numbered.getId() == GTMaterialRegistry.ADDON_ID_START, "the deprecated builder does not keep 7777");
        check(!GTMaterialRegistry.get(7777).isValid(), "7777 is not registered");
        check(MaterialDeclaration.builder(7, "Probe").build().id() == 0,
                "the deprecated builder stores no numeric id");
        check(warnings.stream().noneMatch(message -> message.contains("7777")),
                "the deprecated builder no longer reports a stored id");
        check(warnings.stream().anyMatch(message -> message.contains("7778") && message.contains("ZCanonical")),
                "a canonical declaration id is still reported as ignored");
        check(warnings.stream().noneMatch(message -> message.contains("NamedDust")), "the name builder does not warn");
    }

    private static void scale() {
        prepare();
        int dusts = 1000;
        int metals = 1000;
        var materials = new ArrayList<MaterialDefinition>();
        for (int i = 0; i < dusts; i++) {
            materials.add(MaterialDefinition.builder(1, String.format("ScaleDust%04d", i)).dust().build());
        }
        var metal = AddonMaterials.metalFlags();
        for (int i = 0; i < metals; i++) {
            materials.add(MaterialDefinition.builder(1, String.format("ScaleMetal%04d", i)).properties(metal).build());
        }
        AddonMaterials.submit("aa_scale", materials, List.of(), null);
        long started = System.nanoTime();
        GTMaterialRegistry.init();
        long initMs = (System.nanoTime() - started) / 1_000_000L;
        long itemsStarted = System.nanoTime();
        var definitions = MaterialItemDefinitions.all();
        long itemsMs = (System.nanoTime() - itemsStarted) / 1_000_000L;
        int dustItems = 0;
        int metalItems = 0;
        int coreItems = 0;
        for (var definition : definitions) {
            String name = definition.material().getName();
            if (name.startsWith("ScaleDust")) dustItems++;
            else if (name.startsWith("ScaleMetal")) metalItems++;
            else coreItems++;
        }
        System.gc();
        long heapMb = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024L * 1024L);
        System.out.println("scale dustMaterials=" + dusts + " metalMaterials=" + metals
                + " dustItems=" + dustItems + " metalItems=" + metalItems + " coreItems=" + coreItems
                + " initMs=" + initMs + " itemListMs=" + itemsMs + " heapMb=" + heapMb);
    }

    private static void softPlate() throws Exception {
        prepare();
        AddonMaterials.submitDeclared("aa_soft", List.of(MaterialDeclaration.builder("SoftRubber")
                        .displayName("Soft Rubber").color(0xE8E8E8).dust().polymer().build()),
                List.of(new MaterialFormRequest("SoftRubber", "plate"), new MaterialFormRequest("SoftRubber", "foil")));
        GTMaterialRegistry.init();
        boolean plate = false;
        boolean foil = false;
        boolean family = false;
        boolean wire = false;
        for (var definition : MaterialItemDefinitions.all()) {
            if (!"SoftRubber".equals(definition.material().getName())) continue;
            String prefix = definition.prefix().getName();
            if ("plate".equals(prefix)) plate = true;
            if ("foil".equals(prefix)) foil = true;
            if ("wireFine".equals(prefix)) wire = true;
            if ("plateDense".equals(prefix) || "plateDouble".equals(prefix)) family = true;
        }
        check(plate && foil, "polymer() lets a dust material request a plate and a foil");
        check(!family && !wire, "polymer() does not add the plate family or a wire");
        check(ITEM_ID_SHA256.equals(coreAutomaticFingerprint()), "explicit plates do not change built-in item ids");
    }

    private static void softPlateRejected() {
        prepare();
        AddonMaterials.submitDeclared("aa_soft", List.of(MaterialDeclaration.builder("PlainDust")
                        .displayName("Plain Dust").color(0x888888).dust().build()),
                List.of(new MaterialFormRequest("PlainDust", "plate")));
        expectThrows("not valid", "plate", "PlainDust");
    }

    private static void wires() throws Exception {
        prepare();
        AddonMaterials.submitDeclared("aa_wire", List.of(MaterialDeclaration.builder("SoftWire")
                        .displayName("Soft Wire").color(0x333333).dust().wires().build()),
                List.of(new MaterialFormRequest("SoftWire", "wireFine")));
        GTMaterialRegistry.init();
        boolean wire = false;
        boolean plate = false;
        for (var definition : MaterialItemDefinitions.all()) {
            if (!"SoftWire".equals(definition.material().getName())) continue;
            if ("wireFine".equals(definition.prefix().getName())) wire = true;
            if ("plate".equals(definition.prefix().getName())) plate = true;
        }
        check(wire && !plate, "wires() emits wireFine and not a plate");
        check(ITEM_ID_SHA256.equals(coreAutomaticFingerprint()), "wires do not change built-in item ids");
    }

    private static void wirePlateRejected() {
        prepare();
        AddonMaterials.submitDeclared("aa_wire", List.of(MaterialDeclaration.builder("SoftWire")
                        .displayName("Soft Wire").color(0x333333).dust().wires().build()),
                List.of(new MaterialFormRequest("SoftWire", "plate")));
        expectThrows("not valid", "plate", "SoftWire");
    }

    private static void blockPrefix() throws Exception {
        prepare();
        expectThrows(IllegalArgumentException.class,
                () -> PrefixDeclaration.block("sampleCasing", "", " Sample Casing", 8 * GTValues.U, "slab",
                        "block/sample_casing", "storage_blocks/sample_casing", List.of("SoftRubber")),
                "block shape must be cube");
        var withShape = PrefixDeclaration.block("sampleCasing", "", " Sample Casing", 8 * GTValues.U, "cube",
                "block/sample_casing", "storage_blocks/sample_casing", List.of("SoftRubber"));
        var withoutShape = PrefixDeclaration.block("sampleCasing", "", " Sample Casing", 8 * GTValues.U,
                "block/sample_casing", "storage_blocks/sample_casing", List.of("SoftRubber"));
        check(withShape.equals(withoutShape), "the block prefix overload without shape is cube");
        AddonMaterials.submitDeclared("aa_block", List.of(MaterialDeclaration.builder("SoftRubber")
                        .displayName("Soft Rubber").color(0xE8E8E8).dust().polymer().build()),
                List.of(new MaterialFormRequest("SoftRubber", "plate")),
                List.of(PrefixDeclaration.block("sampleCasing", "", " Sample Casing", 8 * GTValues.U, "cube",
                        "block/sample_casing", "storage_blocks/sample_casing", List.of("SoftRubber"))));
        GTMaterialRegistry.init();
        BlockMaterialPrefix prefix = BlockPrefixRegistry.byName("sampleCasing");
        GTMaterial material = GTMaterialRegistry.get("SoftRubber").resolve();
        check(prefix != null && prefix.isAddon() && "sampleCasing".equals(prefix.compositionName()),
                "the block prefix keeps its name for composition lookup");
        check("aa_block".equals(prefix.textureNamespace()) && "block/sample_casing".equals(prefix.addonTexturePath()),
                "the texture stays in the addon namespace");
        check("storage_blocks/sample_casing".equals(prefix.oreDictionary()), "the ore dictionary name is kept");
        var weights = BlockMaterialWeights.contained(material, prefix);
        check(weights.size() == 1 && "SoftRubber".equals(weights.get(0).material().getName())
                        && weights.get(0).amount() == 8 * GTValues.U,
                "the block contains the declared units");
        int lastCore = -1;
        int block = -1;
        var blocks = MaterialBlockDefinitions.all();
        for (int i = 0; i < blocks.size(); i++) {
            var definition = blocks.get(i);
            if (GTMaterialRegistry.isCoreMaterial(definition.material()) && !definition.prefix().isAddon()) lastCore = i;
            if (definition.prefix() == prefix && definition.material() == material) block = i;
        }
        check(block == blocks.size() - 1 && block > lastCore
                        && blocks.get(block).blockId().equals("sample_casing_softrubber"),
                "the addon block is appended after the built-in blocks");
        check(BLOCK_ID_SHA256.equals(coreBlockFingerprint()), "block prefixes do not change built-in block ids");
        boolean plate = false;
        for (var definition : MaterialItemDefinitions.all()) {
            if (definition.material() == material && "plate".equals(definition.prefix().getName())) plate = true;
        }
        check(plate, "the same material can still request a polymer plate");
    }

    private static void blockFormRejected() {
        prepare();
        AddonMaterials.submitDeclared("aa_block", List.of(MaterialDeclaration.builder("SoftRubber")
                        .displayName("Soft Rubber").color(0xE8E8E8).dust().build()),
                List.of(new MaterialFormRequest("SoftRubber", "sampleCasing")),
                List.of(PrefixDeclaration.block("sampleCasing", "", " Sample Casing", 8 * GTValues.U, "cube",
                        "block/sample_casing", "storage_blocks/sample_casing", List.of("SoftRubber"))));
        expectThrows("block prefix", "sampleCasing", "SoftRubber");
    }

    private static void rawOreStillRejected() {
        prepare();
        AddonMaterials.submit("aa_ore", List.of(MaterialDefinition.builder(1, "SoftOre").dust().build()),
                List.of(new MaterialFormRequest("SoftOre", "oreRaw")), null);
        expectThrows("not valid", "oreRaw", "SoftOre");
    }

    private static void ores() throws Exception {
        prepare();
        AddonMaterials.submitDeclared("aa_ore", List.of(
                MaterialDeclaration.builder("OreMetal").displayName("Ore Metal").color(0x336699).metal().ores()
                        .oreByproducts("Iron", "Copper").build(),
                MaterialDeclaration.builder("PlainDust").displayName("Plain Dust").color(0x888888).dust().build(),
                MaterialDeclaration.builder("SharedOre").metal().ores().build()),
                List.of(new MaterialFormRequest("OreMetal", "oreRaw")));
        AddonMaterials.submitDeclared("zz_ore", List.of(
                MaterialDeclaration.builder("SharedOre").metal().ores().build()), List.of());
        GTMaterialRegistry.init();
        GTMaterial ore = GTMaterialRegistry.get("OreMetal").resolve();
        GTMaterial plain = GTMaterialRegistry.get("PlainDust").resolve();
        GTMaterial shared = GTMaterialRegistry.get("SharedOre").resolve();
        check(MaterialForms.has(ore, "ORES") && AddonMaterials.hasOreForm(ore), "ores() is the ORES form");
        check(!MaterialForms.has(plain, "ORES") && !AddonMaterials.hasOreForm(plain),
                "a material without ores() has no ORES form");
        check(ore.getByProducts().stream().map(GTMaterial::getName).toList().equals(List.of("Iron", "Copper")),
                "byproducts keep declaration order");
        check(plain.getByProducts().isEmpty() && shared.getByProducts().isEmpty(),
                "omitted byproducts stay empty");
        check(AddonMaterials.owners(shared).equals(List.of("aa_ore", "zz_ore")),
                "matching ore declarations merge");
        var oreItems = new TreeSet<String>();
        int raw = 0;
        boolean plainRaw = false;
        for (var definition : MaterialItemDefinitions.all()) {
            if (definition.material() == ore) oreItems.add(definition.prefix().getName());
            if (definition.material() == ore && "oreRaw".equals(definition.prefix().getName())) raw++;
            if (definition.material() == plain && "oreRaw".equals(definition.prefix().getName())) plainRaw = true;
        }
        check(oreItems.containsAll(List.of("oreRaw", "crushed", "crushedTiny", "crushedPurified",
                "crushedPurifiedTiny", "crushedCentrifuged", "crushedCentrifugedTiny", "rockGt", "dustImpure")),
                "ores() generates raw ore, impure dust and the processing chain: " + oreItems);
        check(!oreItems.contains("rawOreChunk") && !oreItems.contains("dirtyGravel")
                        && !oreItems.contains("dustPure") && !oreItems.contains("dustRefined"),
                "ores() does not register the external ore forms: " + oreItems);
        check(raw == 1 && !plainRaw, "requesting oreRaw does not add a second item, and a plain dust has none");
        int lastCore = -1;
        int oreBlock = -1;
        int smallBlock = -1;
        boolean plainOre = false;
        boolean rawStorage = false;
        var blocks = MaterialBlockDefinitions.all();
        for (int i = 0; i < blocks.size(); i++) {
            var definition = blocks.get(i);
            if (GTMaterialRegistry.isCoreMaterial(definition.material())) lastCore = i;
            if (definition.material() == plain && ("ore".equals(definition.prefix().getName())
                    || "oreSmall".equals(definition.prefix().getName()))) plainOre = true;
            if (definition.material() != ore) continue;
            String prefix = definition.prefix().getName();
            if ("ore".equals(prefix)) oreBlock = i;
            if ("oreSmall".equals(prefix)) smallBlock = i;
            if ("blockRaw".equals(prefix) || "crateGtRaw".equals(prefix) || "crateGt64Raw".equals(prefix)) rawStorage = true;
        }
        check(oreBlock > lastCore && smallBlock == oreBlock + 1, "addon ore blocks follow every core block");
        check(blocks.get(oreBlock).blockId().equals("ore_oremetal")
                        && blocks.get(smallBlock).blockId().equals("ore_small_oremetal"),
                "addon ore block ids use the material name");
        check(!plainOre && !rawStorage, "no ores() means no ore block, and ores() adds no raw storage block");
        check(BLOCK_ID_SHA256.equals(coreBlockFingerprint()), "ores do not change built-in block ids");
        check(ITEM_ID_SHA256.equals(coreAutomaticFingerprint()), "ores do not change built-in item ids");
    }

    private static void oreMismatch() {
        prepare();
        AddonMaterials.submitDeclared("aa_ore", List.of(
                MaterialDeclaration.builder("SharedOre").metal().ores().build()), List.of());
        AddonMaterials.submitDeclared("zz_ore", List.of(
                MaterialDeclaration.builder("SharedOre").metal().build()), List.of());
        expectThrows("field ores", "ores", "SharedOre", "aa_ore", "zz_ore");
    }

    private static void oreListMismatch() {
        prepare();
        AddonMaterials.submitDeclared("aa_ore", List.of(
                MaterialDeclaration.builder("SharedOre").metal().ores().oreByproducts("Iron", "Copper").build()), List.of());
        AddonMaterials.submitDeclared("zz_ore", List.of(
                MaterialDeclaration.builder("SharedOre").metal().ores().oreByproducts("Copper", "Iron").build()), List.of());
        expectThrows("field oreByproducts", "oreByproducts", "SharedOre");
    }

    private static void oreUnknown() {
        prepare();
        AddonMaterials.submitDeclared("aa_ore", List.of(
                MaterialDeclaration.builder("OreMetal").metal().ores().oreByproducts("NotAMaterial").build()), List.of());
        expectThrows("unknown material", "NotAMaterial", "OreMetal");
    }

    private static void vein() throws Exception {
        prepare();
        AddonMaterials.submitWorldgen("aa_vein", List.of(
                MaterialDeclaration.builder("OreMetal").metal().ores().build()),
                List.of(new OreVeinDeclaration("ore_metal", OreVeinDeclaration.OVERWORLD, 10, 40, 20, 3, 16,
                        "OreMetal", "OreMetal", "Copper", "Tin")),
                List.of(new SmallOreDeclaration("ore_metal", SmallOreDeclaration.OVERWORLD, 20, 60, 4, "OreMetal"),
                        new SmallOreDeclaration("ore_metal_end", SmallOreDeclaration.END, 20, 60, 2, "OreMetal")));
        AddonMaterials.submitWorldgen("zz_vein", List.of(),
                List.of(new OreVeinDeclaration("late_metal", OreVeinDeclaration.OVERWORLD, 10, 40, 5, 3, 16,
                        "OreMetal", "Iron", "Copper", "Tin")),
                List.of());
        GTMaterialRegistry.init();
        var active = GTOreVeins.activeOverworldVeins();
        check(active.stream().map(GTOreVeins.OreVein::name).toList().equals(List.of("ore_metal", "late_metal")),
                "stone layers keep built-in large veins out, and addon veins follow addon id: " + active);
        check(active.get(0).between().getName().equals("Copper") && active.get(0).top().getName().equals("OreMetal"),
                "vein materials resolve");
        check(GTOreVeins.OVERWORLD_VEINS.size() == 31, "the built-in overworld vein table stays put");
        var small = GTOreVeins.activeOverworldSmallOres();
        check(small.size() == GTOreVeins.OVERWORLD_SMALL_ORES.size() + 1
                        && small.subList(0, GTOreVeins.OVERWORLD_SMALL_ORES.size()).equals(GTOreVeins.OVERWORLD_SMALL_ORES)
                        && small.get(small.size() - 1).name().equals("ore_metal"),
                "small ores are appended after the built-in table");
        var endSmall = GTOreVeins.activeEndSmallOres();
        check(endSmall.size() == GTOreVeins.END_SMALL_ORES.size() + 1
                        && endSmall.get(endSmall.size() - 1).name().equals("ore_metal_end"),
                "an end small ore is appended to the end table");
        check(GTOreVeins.activeEndVeins().equals(GTOreVeins.END_VEINS), "an overworld vein does not enter the end table");
        check(GTOreVeins.activeNetherSmallOres().equals(GTOreVeins.NETHER_SMALL_ORES),
                "an overworld small ore does not enter the nether table");
        check(ITEM_ID_SHA256.equals(coreAutomaticFingerprint()), "veins do not change built-in item ids");
        check(BLOCK_ID_SHA256.equals(coreBlockFingerprint()), "veins do not change built-in block ids");
    }

    private static void veinName() {
        prepare();
        AddonMaterials.submitWorldgen("aa_vein", List.of(
                MaterialDeclaration.builder("OreMetal").metal().ores().build()),
                List.of(new OreVeinDeclaration("iron", OreVeinDeclaration.OVERWORLD, 10, 40, 20, 3, 16,
                        "OreMetal", "OreMetal", "OreMetal", "OreMetal")),
                List.of());
        expectThrows("built-in", "iron", "aa_vein", "minecraft:overworld");
    }

    private static void veinOre() {
        prepare();
        AddonMaterials.submitWorldgen("aa_vein", List.of(
                MaterialDeclaration.builder("PlainDust").dust().build()),
                List.of(),
                List.of(new SmallOreDeclaration("plain", SmallOreDeclaration.OVERWORLD, 20, 60, 4, "PlainDust")));
        expectThrows("has no ore", "PlainDust", "aa_vein");
    }

    private static void veinDuplicate() {
        prepare();
        var vein = new OreVeinDeclaration("ore_metal", OreVeinDeclaration.OVERWORLD, 10, 40, 20, 3, 16,
                "OreMetal", "OreMetal", "OreMetal", "OreMetal");
        AddonMaterials.submitWorldgen("aa_vein", List.of(
                MaterialDeclaration.builder("OreMetal").metal().ores().build()), List.of(vein), List.of());
        AddonMaterials.submitWorldgen("zz_vein", List.of(
                MaterialDeclaration.builder("OreMetal").metal().ores().build()), List.of(vein), List.of());
        expectThrows("already", "zz_vein", "aa_vein", "ore_metal");
    }

    private static void veinBad() {
        expectThrows(IllegalArgumentException.class,
                () -> new OreVeinDeclaration("Iron", OreVeinDeclaration.OVERWORLD, 10, 40, 20, 3, 16,
                        "OreMetal", "OreMetal", "OreMetal", "OreMetal"),
                "lower-case");
        expectThrows(IllegalArgumentException.class,
                () -> new OreVeinDeclaration("sample", "minecraft:the_nether", 10, 40, 20, 3, 16,
                        "OreMetal", "OreMetal", "OreMetal", "OreMetal"),
                "the_nether");
        expectThrows(IllegalArgumentException.class,
                () -> new SmallOreDeclaration("sample", "twilight:forest", 10, 40, 4, "Copper"),
                "dimension");
        expectThrows(IllegalArgumentException.class,
                () -> new OreVeinDeclaration("sample", OreVeinDeclaration.OVERWORLD, 40, 10, 20, 3, 16,
                        "OreMetal", "OreMetal", "OreMetal", "OreMetal"),
                "Y range");
        expectThrows(IllegalArgumentException.class,
                () -> new SmallOreDeclaration("sample", SmallOreDeclaration.OVERWORLD, 10, 40, 0, "Copper"),
                "amount");
    }

    private static void ungeneratedForm() {
        prepare();
        AddonMaterials.submit("aa_form", List.of(), List.of(new MaterialFormRequest("Diamond", "crystal")), null);
        expectThrows("not a generated form", "crystal", "Diamond", "aa_form");
    }

    private static void invalidForm() {
        prepare();
        AddonMaterials.submit("aa_invalid", List.of(), List.of(new MaterialFormRequest("Diamond", "plate")), null);
        expectThrows("not valid", "plate", "Diamond", "aa_invalid");
    }

    private static void lateSubmitAndEmptyBand() throws Exception {
        prepare();
        GTMaterialRegistry.init();
        expectThrows(IllegalStateException.class,
                () -> AddonMaterials.submit("aa_late", List.of(), List.of(), null),
                "Addon material queue is closed");
        expectThrows(IllegalStateException.class,
                () -> GTMaterialRegistry.registerDefinition(MaterialDefinition.builder(4291, "LateDefinition").dust().build()),
                "Material definitions are closed");
        check(oldBandEmpty(), "empty startup leaves the old addon band free");
        check(ITEM_ID_SHA256.equals(coreAutomaticFingerprint()), "an empty addon queue keeps the core item id sample");
        check(BLOCK_ID_SHA256.equals(coreBlockFingerprint()), "an empty addon queue keeps the core block id sample");
        check(PrefixRegistry.addonPrefixes().isEmpty(), "an empty addon queue adds no prefix");
        check(GTOreVeins.activeOverworldVeins().isEmpty(), "an empty addon queue adds no overworld vein");
        check(GTOreVeins.activeOverworldSmallOres().equals(GTOreVeins.OVERWORLD_SMALL_ORES),
                "an empty addon queue keeps the small ore table");
        check(GTOreVeins.activeNetherSmallOres().equals(GTOreVeins.NETHER_SMALL_ORES),
                "an empty addon queue keeps the nether small ore table");
        check(GTOreVeins.activeEndSmallOres().equals(GTOreVeins.END_SMALL_ORES),
                "an empty addon queue keeps the end small ore table");
        check(GTOreVeins.activeEndVeins().equals(GTOreVeins.END_VEINS),
                "an empty addon queue keeps the end vein table");
        check(MaterialPrefix.foil.getMaterialWeight() == GTValues.U4, "built-in foil weight stays on the switch");
    }

    private static void prefix() throws Exception {
        expectThrows(IllegalArgumentException.class,
                () -> new PrefixDeclaration("counted", "", " Counted", 8, "item/counted", List.of("Iron"), Set.of()),
                "multiple of U");
        expectThrows(IllegalArgumentException.class,
                () -> new PrefixDeclaration("wide", "", " Wide", GTValues.U, "item/wide", List.of(), Set.of(PrefixCondition.NOT_WOOD)),
                "needs materials");
        prepare();
        AddonMaterials.submit("aa_prefix",
                List.of(MaterialDefinition.builder(1, "PrefixDust").dust().build()),
                List.of(), null, List.of(), List.of(), List.of(), List.of(),
                List.of(new PrefixDeclaration("sampleBlank", "", " Sample Blank", 8 * GTValues.U, "item/sample_blank",
                        List.of("Iron", "PrefixDust"), Set.of())));
        GTMaterialRegistry.init();
        MaterialPrefix prefix = PrefixRegistry.byName("sampleBlank");
        check(prefix != null && prefix.isAddon() && "aa_prefix".equals(prefix.itemNamespace()),
                "the prefix is registered in the addon namespace");
        check(prefix.getMaterialWeight() == 8 * GTValues.U, "the declared unit amount is 8U");
        check(MaterialPrefix.foil.getMaterialWeight() == GTValues.U4, "built-in foil weight stays on the switch");
        var melted = CrucibleInputRules.materialItem(GTMaterialRegistry.get("Iron"), prefix);
        check(melted.material.getName().equals("Iron") && melted.amount == 8 * GTValues.U,
                "the crucible reads the declared amount");
        int iron = -1;
        int dust = -1;
        var all = MaterialItemDefinitions.all();
        for (int i = 0; i < all.size(); i++) {
            var definition = all.get(i);
            if (!"sampleBlank".equals(definition.prefix().getName())) continue;
            check("aa_prefix".equals(definition.namespace()), "prefix items use the addon namespace");
            if ("Iron".equals(definition.material().getName())) iron = i;
            if ("PrefixDust".equals(definition.material().getName())) dust = i;
        }
        check(iron >= 0 && dust == iron + 1 && dust == all.size() - 1,
                "prefix items are appended in material-name order");
        check(ITEM_ID_SHA256.equals(coreAutomaticFingerprint()), "addon prefixes do not change built-in item ids");
    }

    private static void prefixName() {
        prepare();
        AddonMaterials.submit("aa_hit", List.of(), List.of(), null, List.of(), List.of(), List.of(), List.of(),
                List.of(new PrefixDeclaration("ingot", "Ingot ", "", GTValues.U, "item/hit", List.of("Iron"), Set.of())));
        expectThrows("already exists", "ingot");
    }

    private static void prefixCase() {
        prepare();
        AddonMaterials.submit("aa_case", List.of(), List.of(), null, List.of(), List.of(), List.of(), List.of(),
                List.of(new PrefixDeclaration("iNgot", "Ingot ", "", GTValues.U, "item/case", List.of("Iron"), Set.of())));
        expectThrows("already exists", "iNgot", "ingot");
    }

    private static void prefixMissing() {
        prepare();
        AddonMaterials.submit("aa_miss", List.of(), List.of(), null, List.of(), List.of(), List.of(), List.of(),
                List.of(new PrefixDeclaration("missingBlank", "", " Blank", GTValues.U, "item/missing",
                        List.of("NotAMaterial"), Set.of())));
        expectThrows("unknown material", "NotAMaterial");
    }

    private static void prefixExclude() throws Exception {
        prepare();
        AddonMaterials.submit("aa_exclude", List.of(), List.of(), null, List.of(), List.of(), List.of(), List.of(),
                List.of(new PrefixDeclaration("metalBit", "", " Metal Bit", GTValues.U, "item/metal_bit",
                                List.of(), Set.of(PrefixCondition.METAL), List.of("Iron", "Gold")),
                        new PrefixDeclaration("alloyBit", "", " Alloy Bit", GTValues.U, "item/alloy_bit",
                                List.of(), Set.of(PrefixCondition.ALLOY)),
                        new PrefixDeclaration("moltenBit", "", " Molten Bit", GTValues.U, "item/molten_bit",
                                List.of(), Set.of(PrefixCondition.MOLTEN)),
                        new PrefixDeclaration("overlapBit", "", " Overlap Bit", GTValues.U, "item/overlap_bit",
                                List.of("Iron", "Copper"), Set.of(), List.of("Iron"))));
        GTMaterialRegistry.init();
        MaterialPrefix metal = PrefixRegistry.byName("metalBit");
        check(metal != null && metal.isValidFor(GTMaterialRegistry.get("Copper")), "copper is selected as a metal");
        check(!metal.isValidFor(GTMaterialRegistry.get("Iron")), "iron is excluded by name");
        check(!metal.isValidFor(GTMaterialRegistry.get("Gold")), "gold is excluded by name");
        MaterialPrefix alloy = PrefixRegistry.byName("alloyBit");
        check(alloy != null && alloy.isValidFor(GTMaterialRegistry.get("Steel")), "steel is an alloy");
        check(!alloy.isValidFor(GTMaterialRegistry.get("Copper")), "copper is not an alloy");
        MaterialPrefix molten = PrefixRegistry.byName("moltenBit");
        check(molten != null && molten.isValidFor(GTMaterialRegistry.get("Carbon")), "carbon is molten");
        MaterialPrefix overlap = PrefixRegistry.byName("overlapBit");
        check(overlap != null && overlap.isValidFor(GTMaterialRegistry.get("Copper")),
                "an explicit list overlapping the exclusions keeps the other material");
        check(!overlap.isValidFor(GTMaterialRegistry.get("Iron")),
                "an excluded material wins over the explicit list");
        int metalItems = 0;
        for (var definition : MaterialItemDefinitions.all()) if (definition.prefix() == metal) metalItems++;
        check(metalItems > 0, "exclusions still leave metal items");
        check(ITEM_ID_SHA256.equals(coreAutomaticFingerprint()), "condition prefixes do not change built-in item ids");
    }

    private static void prefixCondition() throws Exception {
        prepare();
        AddonMaterials.submit("aa_cond", List.of(), List.of(), null, List.of(), List.of(), List.of(), List.of(),
                List.of(new PrefixDeclaration("toolBit", "", " Tool Bit", 2 * GTValues.U, "item/tool_bit", List.of(),
                        Set.of(PrefixCondition.HAS_TOOL_STATS, PrefixCondition.SMITHABLE, PrefixCondition.NOT_WOOD))));
        GTMaterialRegistry.init();
        MaterialPrefix prefix = PrefixRegistry.byName("toolBit");
        check(prefix.getMaterialWeight() == 2 * GTValues.U, "a condition prefix still uses its declared amount");
        check(prefix.isValidFor(GTMaterialRegistry.get("Steel")), "steel matches tool stats and smithable");
        check(!prefix.isValidFor(GTMaterialRegistry.get("Wood")), "wood is excluded");
        check(!prefix.isValidFor(GTMaterialRegistry.get("Coal")), "coal is not a smithable tool material");
        int items = 0;
        for (var definition : MaterialItemDefinitions.all()) if (definition.prefix() == prefix) items++;
        check(items > 0 && items < 2000, "the condition does not cover every material: " + items);
        check(ITEM_ID_SHA256.equals(coreAutomaticFingerprint()), "condition prefixes do not change built-in item ids");
    }

    private static void reads() {
        prepare();
        gregtech.internal.addon.AddonCoreServices.bind();
        GTMaterialRegistry.init();
        var table = GTMaterialRegistry.get("Iron");
        var iron = GregTechMaterials.find("Iron");
        check(iron.mass() == table.getMass() && table.getMass() == 56, "iron mass matches the table");
        check(iron.toolQuality() == table.getToolQuality() && table.getToolQuality() == 2, "iron tool quality matches the table");
        check(iron.toolSpeed() == table.getToolSpeed() && table.getToolSpeed() == 6F, "iron tool speed matches the table");
        check(iron.toolDurability() == table.getToolDurability() && table.getToolDurability() == 256L,
                "iron tool durability matches the table");
        var unknown = GregTechMaterials.find("NotRegisteredYet");
        check(!unknown.valid() && unknown.mass() == GregTechMaterials.invalid().mass(), "an unknown material reads the sentinel");
        var ingot = GregTechMaterials.StackComposition.of("ingot",
                List.of(new GregTechMaterials.Component("Iron", MaterialPrefix.ingot.getMaterialWeight())));
        check(ingot.present() && "ingot".equals(ingot.prefix()) && ingot.units().size() == 1
                        && "Iron".equals(ingot.units().get(0).material())
                        && ingot.units().get(0).amount() == GTValues.U,
                "an iron ingot is one iron unit");
        var dirt = GregTechMaterials.StackComposition.of(null, List.of());
        check(!dirt.present() && dirt.prefix().isEmpty() && dirt.units().isEmpty(), "an unknown stack is empty");
        check(!GregTechMaterials.StackComposition.of("ingot",
                List.of(new GregTechMaterials.Component("Iron", 0))).present(), "a zero amount is not a unit");
    }

    private static void byproducts() throws Exception {
        prepare();
        gregtech.internal.addon.AddonCoreServices.bind();
        var missing = GregTechMaterials.find("NotRegisteredYet");
        check(!missing.valid() && missing.byproducts().isEmpty(), "an unknown name has no byproducts and does not throw");
        var beforeLink = GregTechMaterials.find("Copper").byproducts();
        System.out.println("byproduct-scan before-link copper=" + beforeLink);
        AddonMaterials.submitDeclared("aa_ore", List.of(
                MaterialDeclaration.builder("OreMetal").displayName("Ore Metal").color(0x336699).metal().ores()
                        .oreByproducts("Iron", "Copper").build(),
                MaterialDeclaration.builder("PlainDust").displayName("Plain Dust").color(0x888888).dust().build()),
                List.of());
        GTMaterialRegistry.init();
        var expectedCopper = List.of("Cobaltite", "Gold", "Nickel", "Malachite", "Arsenic");
        GTMaterial copper = GTMaterialRegistry.get("Copper").resolve();
        var copperNames = copper.getByProducts().stream().map(item -> item.resolve().getName()).toList();
        check(copperNames.equals(expectedCopper), "copper byproducts stay Cobaltite, Gold, Nickel, Malachite, Arsenic: " + copperNames);
        check(GregTechMaterials.find("Copper").byproducts().equals(copperNames), "the API follows getByProducts");
        check(GregTechMaterials.find("OreMetal").byproducts().equals(List.of("Iron", "Copper")), "addon oreByproducts are readable");
        check(GregTechMaterials.find("PlainDust").byproducts().isEmpty(), "a material without byproducts reads empty");
        check(GregTechMaterials.invalid().byproducts().isEmpty(), "the invalid material reads empty");
        var dust = PrefixRegistry.byName("dust");
        for (String name : expectedCopper) check(dust.isValidFor(GTMaterialRegistry.get(name)), "dust is valid for " + name);
        var hidden = new TreeSet<String>();
        var alias = new TreeSet<String>();
        for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
            if (material.resolve() != material) continue;
            var expected = material.getByProducts().stream().map(item -> item.resolve().getName()).toList();
            var actual = GregTechMaterials.find(material.getName()).byproducts();
            check(actual.equals(expected), "byproducts match " + material.getName() + ": " + actual + " vs " + expected);
            for (GTMaterial byproduct : material.getByProducts()) {
                GTMaterial resolved = byproduct.resolve();
                if (resolved.has(MaterialProperty.HIDDEN)) hidden.add(material.getName() + "->" + resolved.getName());
                if (!byproduct.getName().equals(resolved.getName()))
                    alias.add(material.getName() + "->" + byproduct.getName() + "=" + resolved.getName());
            }
        }
        System.out.println("byproduct-scan hidden=" + hidden.size() + " alias=" + alias.size());
        if (!hidden.isEmpty()) System.out.println("byproduct-scan hidden-names " + hidden);
        if (!alias.isEmpty()) System.out.println("byproduct-scan alias-names " + alias);
        GTMaterialRegistry.get("PlainDust").addOreByProducts(GTMaterialRegistry.get("Tin"));
        check(GregTechMaterials.find("PlainDust").byproducts().equals(List.of("Tin")), "a byproduct added after link is visible");
    }

    private static void prepare() {
        ModData.bindPresence(id -> true);
        GTMaterialRegistry.setLogSink((warning, message) -> {});
        PrefixRegistry.ensurePrefixesLoaded();
        ModReferences.UNKNOWN.getClass();
        gregtech.data.MaterialGroups.Glowstone.getClass();
    }

    private static void expectThrows(String... parts) {
        expectThrows(IllegalStateException.class, GTMaterialRegistry::init, String.join(" ", parts));
        // init failure message is the assertion text only when the thrown message contains every part
    }

    private static void expectThrows(Class<? extends Throwable> expected, Runnable action, String message) {
        assertions++;
        try {
            action.run();
        } catch (Throwable failure) {
            if (!expected.isInstance(failure)) throw new AssertionError(message, failure);
            if (message.equals(failure.getMessage())) return;
            String text = failure.getMessage() == null ? "" : failure.getMessage();
            for (String part : message.split(" ")) {
                if (!text.contains(part)) throw new AssertionError(message + " but was: " + text, failure);
            }
            return;
        }
        throw new AssertionError(message);
    }

    private static String coreBlockFingerprint() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (MaterialBlockDefinitions.Definition definition : MaterialBlockDefinitions.all()) {
            if (!GTMaterialRegistry.isCoreMaterial(definition.material()) || definition.prefix().isAddon()) continue;
            String line = definition.blockId() + "\t" + definition.baseBlockId() + "\t"
                    + definition.prefix().getName() + "\t" + definition.material().getName() + "\t"
                    + definition.material().getId() + "\n";
            digest.update(line.getBytes(StandardCharsets.UTF_8));
        }
        StringBuilder text = new StringBuilder();
        for (byte value : digest.digest()) text.append(String.format("%02x", value));
        return text.toString();
    }

    private static String coreAutomaticFingerprint() throws Exception {
        Set<MaterialPrefix> automatic = Set.copyOf(MaterialItemDefinitions.candidatePrefixes());
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (MaterialItemDefinitions.Definition definition : MaterialItemDefinitions.all()) {
            if (!GTMaterialRegistry.isCoreMaterial(definition.material()) || !automatic.contains(definition.prefix())) continue;
            String line = definition.itemId() + "\t" + definition.baseItemId() + "\t"
                    + definition.prefix().getName() + "\t" + definition.material().getName() + "\t"
                    + definition.material().getId() + "\n";
            digest.update(line.getBytes(StandardCharsets.UTF_8));
        }
        StringBuilder text = new StringBuilder();
        for (byte value : digest.digest()) text.append(String.format("%02x", value));
        return text.toString();
    }

    private static boolean oldBandEmpty() {
        for (int id = AddonMaterials.ADDON_ID_MIN; id <= AddonMaterials.ADDON_ID_MAX; id++) {
            GTMaterial occupant = GTMaterialRegistry.get(id);
            if (occupant.isValid()) return false;
        }
        return true;
    }

    private static void check(boolean valid, String message) {
        assertions++;
        if (!valid) throw new AssertionError(message);
    }

    /** Loaded only through a temporary service file, never from the shared test classpath. */
    public static final class SampleContributor implements GregTechMaterialContributor {
        @Override public String addonId() { return "aa_sample"; }

        @Override public List<MaterialDefinition> materials() {
            return List.of(MaterialDefinition.builder(AddonMaterials.ADDON_ID_MIN, "AmmoniumNitrate")
                    .displayName("Ammonium Nitrate").color(0xC8D6C0).chemicalFormula("NH4NO3")
                    .meltingPointKelvin(443).boilingPointKelvin(483).density(1.72F).dust().build());
        }

        @Override public List<MaterialFormRequest> forms() {
            return List.of(new MaterialFormRequest("Iron", "dust"));
        }

        @Override public void link() {
            GTMaterialRegistry.get("AmmoniumNitrate").setPulver(GTMaterialRegistry.get("Iron"), 1);
            linked = true;
        }
    }

    /** Loaded only through a temporary service file, never from the shared test classpath. */
    public static final class NamedContributor implements AddonMaterialContributor {
        @Override public String addonId() { return "aa_named"; }

        @Override public List<MaterialDeclaration> materials() {
            return List.of(
                    MaterialDeclaration.builder("NamedDust").dust().build(),
                    MaterialDeclaration.builder(7777, "FromNumber").dust().build(),
                    new MaterialDeclaration(7778, "ZCanonical", "ZCanonical", 0xFFFFFF,
                            300, 1000, 1f, "", true, false, false, false, false, List.of(),
                            false, false, false, false));
        }

        @Override public List<MaterialFormRequest> forms() { return List.of(); }
    }
}
