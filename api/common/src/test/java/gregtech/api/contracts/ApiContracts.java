/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.contracts;

import gregtech.api.addon.AddonLimits;
import gregtech.api.addon.AddonRuntime;
import gregtech.api.addon.GregTechAddons;
import gregtech.api.addon.InstalledService;
import gregtech.api.addon.machine.BehaviorMachine;
import gregtech.api.addon.machine.CompressedGasTankDeclaration;
import gregtech.api.addon.machine.LayoutPart;
import gregtech.api.addon.material.GregTechMaterials;
import gregtech.api.addon.material.MaterialComposition;
import gregtech.api.addon.material.MaterialDeclaration;
import gregtech.api.addon.material.MaterialFormRequest;
import gregtech.api.addon.material.PrefixDeclaration;
import gregtech.api.addon.material.SmeltingTargetDeclaration;
import gregtech.api.addon.pipe.FluidPipeDeclaration;
import gregtech.api.addon.pipe.ItemPipeDeclaration;
import gregtech.api.addon.recipe.CrucibleContent;
import gregtech.api.addon.recipe.RecipeMachine;
import gregtech.api.addon.recipe.RecipeTable;
import gregtech.api.addon.worldgen.OreVeinDeclaration;
import gregtech.api.addon.worldgen.SmallOreDeclaration;
import gregtech.api.energy.EnergyInput;
import gregtech.api.energy.EnergyType;
import gregtech.api.energy.GTVoltageTiers;
import gregtech.api.machine.BasicMachineParameters;
import gregtech.api.machine.BlockFace;
import gregtech.api.machine.MachineCasing;
import gregtech.api.machine.MachineFace;
import gregtech.api.machine.MachineFaceMasks;
import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.api.material.MaterialSentinels;
import gregtech.api.material.MaterialTextureSet;
import gregtech.api.mod.ModData;
import gregtech.api.multiblock.CellMatch;
import gregtech.api.multiblock.LayoutRepeat;
import gregtech.api.multiblock.StructureLayout;
import gregtech.api.multiblock.StructureRole;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.jar.JarFile;

/** Dependency-free consumer of the packaged API; deliberately has no host implementation. */
public final class ApiContracts {
    public static void main(String[] args) throws Exception {
        String[] version = args[0].split("\\.");
        check(GregTechAddons.apiVersion()
                        == Integer.parseInt(version[0]) * 1000 + Integer.parseInt(version[1]),
                "API version code must match the independently published artifact");
        check(GregTechAddons.apiMajor() == Integer.parseInt(version[0])
                        && GregTechAddons.apiMinor() == Integer.parseInt(version[1]),
                "API major and minor must match the independently published artifact");
        checkJarClosure();
        checkEnergyContracts();
        checkSharedValueContracts();
        checkInstalledServiceContracts();
        checkMachineTemplateContracts();
        checkUnifiedValidationContracts();
        checkMaterialContracts();
        checkMachineFaceMaskContracts();
        checkStructureLayoutContracts();
        checkWorldgenContracts();
        checkModDataContracts();
        checkLocaleContracts();
        try {
            AddonRuntime.services();
            throw new AssertionError("Host queries must fail before the runtime is bound");
        } catch (IllegalStateException expected) {
            check(expected.getMessage().contains("not installed"), "Missing host diagnostic");
        }
        System.out.println("Standalone API JAR and energy contracts passed");
    }

    private static void checkJarClosure() throws Exception {
        Path artifact = Path.of(GTMaterial.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        check(artifact.toString().endsWith(".jar"), "Consumer must use the JAR, not main class directories");
        int classes = 0;
        try (var jar = new JarFile(artifact.toFile())) {
            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (!name.endsWith(".class")) continue;
                check(name.startsWith("gregtech/api/"), "Implementation leaked into API JAR: " + name);
                if (name.endsWith("package-info.class")) continue;
                Class<?> type = Class.forName(name.substring(0, name.length() - 6).replace('/', '.'),
                        false, ApiContracts.class.getClassLoader());
                // Resolve every signature against only the packaged library and the JDK.
                type.getDeclaredConstructors();
                type.getDeclaredMethods();
                type.getDeclaredFields();
                classes++;
            }
        }
        check(classes > 0, "API JAR has no classes");
    }

    private static void checkEnergyContracts() {
        check(EnergyType.of("Custom").name().equals("Custom"), "Extensible energy names must remain supported");
        check(EnergyType.of("*").equals(EnergyType.ANY), "Dynamic behavior energy must remain supported");
        check(EnergyType.known("steam") == EnergyType.STEAM, "Known names canonicalize");
        check(EnergyType.of("ordo") == EnergyType.ORDO, "Aspect names are known constants");
        for (String type : new String[] {null, "", "*", "Custom", " EU", "SU"}) {
            reject(() -> EnergyType.known(type));
            reject(() -> BasicMachineParameters.defaultEnergyInMin(EnergyType.of(type), 32));
            reject(() -> BasicMachineParameters.defaultEnergyInMax(EnergyType.of(type), 32));
        }
        reject(() -> machine(EnergyType.of("Custom"), 32).build());
        reject(() -> machine(EnergyType.ANY, 32).build());

        var eu = machine(EnergyType.of("eu"), 32).build();
        check(eu.energy().type().equals(EnergyType.EU), "Builder must store the canonical energy name");
        range(eu, false, 16, 64);
        range(eu, true, 512, 4096);
        var hu = machine(EnergyType.HU, 32).build();
        range(hu, false, 16, 64);
        range(hu, true, 1, 4096);
        var tu = machine(EnergyType.TU, 32).build();
        range(tu, false, 1, 16);
        range(tu, true, 1, 16);
        var explicit = machine(EnergyType.EU, 32).energyRange(3, 9).build();
        range(explicit, false, 3, 9);
        range(explicit, true, 3, 9);
        var omitted = machine(EnergyType.HU, 32).omitEnergyRange().build();
        check(omitted.energy().energyInMin() == 0 && omitted.energy().energyInMax() == 0,
                "Omitted bounds stay undeclared");
        range(omitted, false, 16, 64);
        range(omitted, true, 1, 4096);
        reject(() -> machine(EnergyType.EU, 32).energyRange(10, 5).build());
        check(GTVoltageTiers.tierCount() == 16, "Sixteen voltage tiers");
        check(GTVoltageTiers.tierVoltage(1) == 32 && GTVoltageTiers.tierName(1).equals("LV"),
                "Tier 1 is LV at 32 V");
    }

    private static void checkInstalledServiceContracts() {
        InstalledService<String> service = new InstalledService<>();
        service.bind("first", "missing service", "already installed");
        check(service.get("missing service").equals("first"), "Bound service must be readable");
        try {
            service.bind("second", "missing service", "already installed");
            throw new AssertionError("A second bind must fail");
        } catch (IllegalStateException expected) {
            check(expected.getMessage().contains("first bound by"), "Duplicate bind must name the first binder");
            check(expected.getCause() != null, "Duplicate bind must carry the first binder stack");
        }
    }

    private static void checkSharedValueContracts() {
        EnergyInput input = EnergyInput.builder(EnergyType.EU, 32).build();
        check(input.withFaces(List.of(MachineFace.FRONT, MachineFace.TOP)).faces().size() == 2,
                "Resolved faces must be kept");
        reject(() -> input.withFaces(List.of(MachineFace.FRONT, MachineFace.FRONT)));
        EnergyInput zeroCapacity = EnergyInput.builder(EnergyType.EU, 32)
                .capacity(0).faces(MachineFace.FRONT).build();
        check(!zeroCapacity.capacitySpecified(), "Zero capacity is stored as unspecified");
        check(zeroCapacity.equals(zeroCapacity.withFaces(zeroCapacity.faces())),
                "withFaces must not change equality for a zero-capacity input");
        check(EnergyInput.builder(EnergyType.EU, 32).range(1, 4).build().rangeSpecified(),
                "A declared range is reported as specified");
        EnergyInput dynamic = EnergyInput.builder(EnergyType.EU, 32).anyType().build();
        check(dynamic.dynamic() && dynamic.type().wildcard(),
                "A dynamic input must use the wildcard type");
        reject(() -> EnergyInput.builder(EnergyType.ANY, 32).build());
        reject(() -> new EnergyInput(EnergyType.EU, 32, 0, 0, false, List.of(), 0, false, true, true));
        check(EnergyInput.builder(EnergyType.EU, 32).faces(MachineFace.FRONT, MachineFace.TOP).build()
                        .equals(EnergyInput.builder(EnergyType.EU, 32)
                                .faces(MachineFace.TOP, MachineFace.FRONT).build()),
                "Energy input faces must compare without order");

        MachineCasing casing = MachineCasing.builder("addon/casing", "iron").build();
        check(casing.resolve("addon_machine", true, 1).machineType().equals("addon_machine"),
                "Resolved machine type must be kept");
        reject(() -> casing.resolve("Bad Type!", true, -5));
    }

    private static void checkMachineTemplateContracts() {
        MachineCasing plain = MachineCasing.builder("addon_recipe", "iron").build();
        RecipeMachine valid = RecipeMachine.builder(plain)
                .energy(EnergyInput.builder(EnergyType.EU, 32).build())
                .recipeMap("test_map")
                .build();
        check(valid.casing().machineType().equals("addon_recipe"), "Recipe machine keeps the id as machine type");
        RecipeMachine zeroCapacity = RecipeMachine.builder(plain)
                .energy(EnergyInput.builder(EnergyType.EU, 32).capacity(0).build())
                .recipeMap("test_map")
                .build();
        check(zeroCapacity.casing().machineType().equals("addon_recipe"),
                "A zero capacity is unset for a recipe machine");

        MachineCasing tiered = MachineCasing.builder("addon_tiered", "iron").tier(256).build();
        reject(() -> RecipeMachine.builder(tiered)
                .energy(EnergyInput.builder(EnergyType.EU, 32).build())
                .recipeMap("test_map")
                .build());
        reject(() -> RecipeMachine.builder(plain)
                .energy(EnergyInput.builder(EnergyType.EU, 0).build())
                .recipeMap("test_map")
                .build());
        reject(() -> RecipeMachine.builder(plain)
                .energy(EnergyInput.builder(EnergyType.EU, AddonLimits.MAX_PER_TICK + 1).build())
                .recipeMap("test_map")
                .build());
        reject(() -> RecipeMachine.builder(plain)
                .energy(EnergyInput.builder(EnergyType.EU, 32).range(64, 128).build())
                .recipeMap("test_map")
                .build());

        try {
            RecipeMachine.builder(MachineCasing.builder("addon/slash", "iron").build())
                    .energy(EnergyInput.builder(EnergyType.EU, 32).build())
                    .recipeMap("test_map")
                    .build();
            throw new AssertionError("A slash id must fail without an explicit machine type");
        } catch (IllegalArgumentException expected) {
            check(expected.getMessage().contains("machineType("), "Slash ids should point at machineType(...)");
        }

        try {
            BehaviorMachine.builder(MachineCasing.builder("addon/slash_behavior", "iron").build())
                    .energy(EnergyInput.builder(EnergyType.EU, 32).build())
                    .gui("gt6ce:textures/gui/test.png")
                    .build();
            throw new AssertionError("A behavior slash id must fail without an explicit machine type");
        } catch (IllegalArgumentException expected) {
            check(expected.getMessage().contains("machineType("),
                    "Behavior slash ids should point at machineType(...)");
        }
    }

    private static void checkUnifiedValidationContracts() {
        for (String bad : new String[] {"iron ingot", "gt:iron", ""}) {
            reject(() -> MachineCasing.builder("addon_machine", bad).build());
            reject(() -> LayoutPart.builder("part", bad).hardness(1).build());
            reject(() -> new ItemPipeDeclaration("fast", bad, 4, 1));
            reject(() -> new FluidPipeDeclaration("fast", bad, 1, true, true, true, true, 0, 0));
            reject(() -> new CompressedGasTankDeclaration("tank", bad, 1, true, true, true));
        }

        reject(() -> RecipeTable.machine("test_map").title("Test").gui("test.png").build());
        reject(() -> RecipeTable.machine("test_map").title("Test").gui(null).build());
        reject(() -> BehaviorMachine.builder(MachineCasing.builder("addon_behavior", "iron").build())
                .energy(EnergyInput.builder(EnergyType.EU, 32).build())
                .gui("test.png")
                .build());

        reject(() -> new SmeltingTargetDeclaration("Iron", "Steel", 1));
        reject(() -> new SmeltingTargetDeclaration(null, "Steel", GTValues.U));
        check(new SmeltingTargetDeclaration("Iron", "Steel", GTValues.U4).amount() == GTValues.U4,
                "Smelting targets may use fractions down to U72");

        reject(() -> new PrefixDeclaration(
                "ingot", null, " Ingot", GTValues.U, "material/ingot", List.of("Iron"), Set.of()));
        reject(() -> new PrefixDeclaration(
                "ingot", "", " Ingot", GTValues.U, "material/ingot", List.of("Iron"), null));
        reject(() -> new PrefixDeclaration(
                "ingot", "", " Ingot", GTValues.U, "material/ingot", List.of("Iron"), Set.of(), null));
        reject(() -> new MaterialDeclaration(0, "TestMaterial", "Test", 0xFFFFFF, 0, 0, 1,
                null, Set.of(), List.of()));
        reject(() -> new MaterialDeclaration(0, "TestMaterial", "Test", 0xFFFFFF, 0, 0, 1,
                "", Set.of(), null));
        reject(() -> LayoutPart.builder("part", "Iron").hardness(1).texture(null).build());
        reject(() -> new MaterialFormRequest(" Iron", "ingot"));
        reject(() -> new CrucibleContent(null, 0, 0));
        reject(() -> new GregTechMaterials.StackComposition(null, List.of()));
        reject(() -> new GregTechMaterials.StackComposition("", null));
        reject(() -> GregTechMaterials.StackComposition.of(null, List.of()));
        reject(() -> new GTMaterial(1, "Test", "Test", 0xFFFFFF).setTooltipChemical(null));
        reject(() -> RecipeMachine.builder(MachineCasing.builder("addon_recipe", "iron").build())
                .structureKeys(null));
        reject(() -> EnergyInput.builder(EnergyType.EU, 32).faces((List<MachineFace>) null));
    }

    private static void checkMaterialContracts() {
        MaterialDeclaration declared = MaterialDeclaration.builder("TestMaterial")
                .dust().metal().oreByproducts("Iron", "Nickel").build();
        check(declared.has(MaterialDeclaration.Feature.DUST), "A declared feature must be stored");
        check(declared.features().contains(MaterialDeclaration.Feature.ORES),
                "Ore byproducts must imply ORES");
        check(MaterialDeclaration.builder("TestMaterial").build().features().isEmpty(),
                "A material with no forms is valid");
        reject(() -> new MaterialDeclaration(0, "TestMaterial", "Test", 0xFFFFFF, 0, 0, 1, "",
                null, List.of()));
        reject(() -> new MaterialDeclaration(0, "TestMaterial", "Test", 0xFFFFFF, 0, 0, 1, "",
                Set.of(), List.of("Iron")));

        GTMaterial material = new GTMaterial(1, "Test", "Test", 0xFFFFFF);
        check(material.setOreMultiplier(0).getOreMultiplier() == 1, "Ore multiplier must stay at least 1");
        for (int multiplier : new int[] {200, 256, Integer.MAX_VALUE}) {
            check(material.setOreMultiplier(multiplier).getOreMultiplier() == Byte.MAX_VALUE,
                    "Ore multiplier must not overflow a byte: " + multiplier);
        }
        check(material.setOreProcessingMultiplier(0).getOreProcessingMultiplier() == 1,
                "Ore processing multiplier must stay at least 1");
        check(material.setOreProcessingMultiplier(300).getOreProcessingMultiplier() == Byte.MAX_VALUE,
                "Ore processing multiplier must not overflow a byte");

        GTMaterial alias = new GTMaterial(2, "Alias", "Alias", 0xFFFFFF);
        GTMaterial canonical = new GTMaterial(3, "Canonical", "Canonical", 0xFFFFFF);
        material.setCrushing(alias, GTValues.U);
        alias.setRegistration(canonical);
        check(material.getTargetCrushingMaterial() == canonical,
                "Material links must resolve after a later registration");
        reject(() -> material.setStats(-1, 1000, 1));
        reject(() -> material.setStats(300, 1000, Float.NaN));

        check(new MaterialComposition.Part("Iron", GTValues.U9).amount() == GTValues.U9,
                "Composition parts may use fractions down to U72");
        reject(() -> new MaterialComposition.Part("Iron", GTValues.U72 - 1));
        check(new MaterialComposition(
                                "TestMaterial",
                                1,
                                List.of(new MaterialComposition.Part("Iron", GTValues.U4)),
                                false)
                        .parts()
                        .get(0)
                        .amount()
                == GTValues.U4, "Non-crucible compositions may use fractional parts");
        reject(() -> new MaterialComposition(
                "TestMaterial", 1, List.of(new MaterialComposition.Part("Iron", GTValues.U4)), true));

        check(new PrefixDeclaration("dustTinyAddon", "Tiny ", " Dust", GTValues.U9, "dust_tiny", List.of("Iron"), Set.of())
                        .amount()
                == GTValues.U9, "Prefix amounts may use fractions down to U72");
        reject(() -> new PrefixDeclaration(
                "dustTinyAddon", "Tiny ", " Dust", GTValues.U72 - 1, "dust_tiny", List.of("Iron"), Set.of()));

        try {
            MaterialSentinels.INVALID.setStats(1, 2, 3);
            throw new AssertionError("Frozen sentinels must reject mutation");
        } catch (IllegalStateException expected) {
            // Expected.
        }
    }

    private static void checkMachineFaceMaskContracts() {
        MachineFaceMasks masks = MachineFaceMasks.builder()
                .itemIn(MachineFace.TOP).itemOut(MachineFace.BOTTOM)
                .itemAutoIn(null).build();
        check(masks.itemInputs().contains(MachineFace.TOP), "Faces must be stored");
        check(masks.itemAutoInput() == null, "An automatic face may be absent");
        check(MachineFaceMasks.ALL_SIDES.energyInputs().size() == 6, "All sides enable every energy face");
        for (int index = 0; index < 6; index++) {
            check(BlockFace.of(index).index() == index, "Block faces keep the Direction ordinal");
        }
        reject(() -> BlockFace.of(-1));
        reject(() -> BlockFace.of(6));
        reject(() -> MachineFaceMasks.builder().itemIn((MachineFace) null));
    }

    private static void checkStructureLayoutContracts() {
        CellMatch casing = CellMatch.part("casing");
        LayoutRepeat tower = LayoutRepeat.builder("tower", 0, 1, -1)
                .cell(1, 0, 0, casing, StructureRole.CASING)
                .build();

        StructureLayout capped = StructureLayout.builder("test_tower")
                .cell(1, 3, 0, casing, StructureRole.CASING)
                .repeat(tower)
                .build();
        check(capped.repeats().size() == 1, "An unbounded repeat may sit below a fixed cap");

        reject(() -> StructureLayout.builder("test_tower")
                .cell(1, 0, 0, casing, StructureRole.CASING)
                .repeat(tower)
                .build());

        LayoutRepeat minimumThree = LayoutRepeat.builder("tower", 0, 3, -1)
                .cell(1, 0, 0, casing, StructureRole.CASING)
                .build();
        reject(() -> StructureLayout.builder("test_tower_min")
                .cell(1, 1, 0, casing, StructureRole.CASING)
                .repeat(minimumThree)
                .build());

        LayoutRepeat bounded = LayoutRepeat.builder("tower", 0, 1, 3)
                .cell(1, 0, 0, casing, StructureRole.CASING)
                .build();
        reject(() -> StructureLayout.builder("test_bounded")
                .cell(1, 2, 0, casing, StructureRole.CASING)
                .repeat(bounded)
                .build());
    }

    private static void checkWorldgenContracts() {
        var vein = new OreVeinDeclaration(
                "test_vein", "example:custom_dimension", 1, 64, 1, 1, 1, "Iron", "Iron", "Iron", "Iron");
        check(vein.dimension().equals("example:custom_dimension"), "Custom vein dimensions are accepted");
        reject(() -> new OreVeinDeclaration(
                "test_vein", "example", 1, 64, 1, 1, 1, "Iron", "Iron", "Iron", "Iron"));
        reject(() -> new OreVeinDeclaration(
                "test_vein", "example:custom_dimension", -1, 64, 1, 1, 1, "Iron", "Iron", "Iron", "Iron"));

        var ore = new SmallOreDeclaration("test_small", "example:custom_dimension", 1, 64, 1, "Iron");
        check(ore.dimension().equals("example:custom_dimension"), "Custom small-ore dimensions are accepted");
        reject(() -> new SmallOreDeclaration("test_small", "Bad Dimension!", 1, 64, 1, "Iron"));
    }

    private static void checkModDataContracts() throws InterruptedException {
        ModData.bindPresence(id -> true);
        Locale previous = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
        try {
            ModData mod = new ModData("IronMod", "Iron Mod");
            check(ModData.MODS.get("ironmod") == mod, "Lowercase mod keys must not depend on the default locale");
            check(ModData.MODS.get("IronMod") == mod, "Mixed-case lookups must resolve to the same mod");
            check(ModData.MODS.values().stream().filter(value -> value == mod).count() == 1,
                    "A mixed-case mod must occupy exactly one registry entry");
            check(ModData.MODS.values().stream().distinct().count() == ModData.MODS.size(),
                    "MODS must not expose duplicate mods");
            try {
                new ModData("ironmod", "Iron Mod Duplicate");
                throw new AssertionError("Case-insensitive duplicate mod ids must be rejected");
            } catch (IllegalArgumentException expected) {
                check(expected.getMessage().contains("already registered"), "Missing duplicate-id diagnostic");
            }
        } finally {
            Locale.setDefault(previous);
        }
        int before = ModData.MODS.size();
        int threads = 4;
        int perThread = 250;
        Thread[] workers = new Thread[threads];
        for (int t = 0; t < threads; t++) {
            int base = t * perThread;
            workers[t] = new Thread(() -> {
                for (int i = 0; i < perThread; i++) new ModData("mod" + (base + i), "Mod " + (base + i));
            });
        }
        for (Thread worker : workers) worker.start();
        for (Thread worker : workers) worker.join();
        check(ModData.MODS.size() == before + threads * perThread,
                "Concurrent mod construction must not lose entries");
        try {
            ModData.MODS.clear();
            throw new AssertionError("MODS must be read-only");
        } catch (UnsupportedOperationException expected) {
            // Expected.
        }
        try {
            ModData.bindPresence(id -> true);
            throw new AssertionError("Rebinding mod presence must fail");
        } catch (IllegalStateException expected) {
            check(expected.getMessage().contains("already bound"), "Missing rebind diagnostic");
            check(expected.getMessage().contains("first bound by"), "Rebind must name the first binder");
            check(expected.getCause() != null, "Rebind must carry the first binder's stack");
        }
    }

    private static void checkLocaleContracts() {
        Locale previous = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
        try {
            check(MaterialTextureSet.FIERY.folder().equals("fiery"), "Texture folders must not depend on the default locale");
            check(MaterialTextureSet.LIGNITE.folder().equals("lignite"), "Texture folders must not depend on the default locale");
            check(new GTMaterial(1, "Iron", "Iron", 0xFFFFFF).getTranslationKey().equals("material.gt6ce.iron"),
                    "Translation keys must not depend on the default locale");
        } finally {
            Locale.setDefault(previous);
        }
    }

    private static BasicMachineParameters.Builder machine(EnergyType energy, long amount) {
        return BasicMachineParameters.builder("test_machine", new GTMaterial(1, "Test", "Test", 0xFFFFFF))
                .energy(energy, amount);
    }

    private static void range(BasicMachineParameters parameters, boolean large, long min, long max) {
        check(parameters.energy().acceptedEnergyInMin(large) == min,
                "Unexpected minimum for " + parameters.energy().type());
        check(parameters.energy().acceptedEnergyInMax(large) == max,
                "Unexpected maximum for " + parameters.energy().type());
    }

    private static void reject(Runnable action) {
        try {
            action.run();
            throw new AssertionError("Invalid input was accepted");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
