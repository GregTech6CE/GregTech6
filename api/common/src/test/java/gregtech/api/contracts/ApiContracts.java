package gregtech.api.contracts;

import gregtech.api.addon.AddonLimits;
import gregtech.api.addon.AddonRuntime;
import gregtech.api.addon.GregTechAddons;
import gregtech.api.addon.InstalledService;
import gregtech.api.addon.recipe.RecipeMachine;
import gregtech.api.energy.EnergyInput;
import gregtech.api.energy.EnergyType;
import gregtech.api.energy.GTVoltageTiers;
import gregtech.api.machine.BasicMachineParameters;
import gregtech.api.machine.MachineCasing;
import gregtech.api.machine.MachineFace;
import gregtech.api.machine.MachineFaceMasks;
import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialTextureSet;
import gregtech.api.mod.ModData;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.jar.JarFile;

/** Dependency-free consumer of the packaged API; deliberately has no host implementation. */
public final class ApiContracts {
    public static void main(String[] args) throws Exception {
        check(GregTechAddons.apiVersion() == Integer.parseInt(args[0].split("\\.")[1]),
                "API version must match the independently published artifact");
        checkJarClosure();
        checkEnergyContracts();
        checkSharedValueContracts();
        checkInstalledServiceContracts();
        checkMachineTemplateContracts();
        checkMaterialContracts();
        checkMachineFaceMaskContracts();
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
        check(EnergyType.require("Custom").equals("Custom"), "Extensible energy names must remain supported");
        check(EnergyType.require("*").equals("*"), "Dynamic behavior energy must remain supported");
        check(EnergyType.requireKnown("steam").equals(EnergyType.STEAM), "Known names canonicalize");
        for (String type : new String[] {null, "", "*", "Custom", " EU", "SU"}) {
            reject(() -> EnergyType.requireKnown(type));
            reject(() -> BasicMachineParameters.defaultEnergyInMin(type, 32));
            reject(() -> BasicMachineParameters.defaultEnergyInMax(type, 32));
        }
        reject(() -> machine("Custom", 32).build());
        reject(() -> machine("*", 32).build());

        var eu = machine("eu", 32).build();
        check(eu.energyType().equals(EnergyType.EU), "Builder must store the canonical energy name");
        range(eu, false, 16, 64);
        range(eu, true, 512, 4096);
        var hu = machine("hu", 32).build();
        range(hu, false, 16, 64);
        range(hu, true, 1, 4096);
        var tu = machine("tu", 32).build();
        range(tu, false, 1, 16);
        range(tu, true, 1, 16);
        var explicit = machine("EU", 32).energyRange(3, 9).build();
        range(explicit, false, 3, 9);
        range(explicit, true, 3, 9);
        var omitted = machine("HU", 32).omitEnergyRange().build();
        check(omitted.energyInMin() == 0 && omitted.energyInMax() == 0, "Omitted bounds stay undeclared");
        range(omitted, false, 16, 64);
        range(omitted, true, 1, 4096);
        reject(() -> machine("EU", 32).energyRange(10, 5).build());
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
        EnergyInput input = EnergyInput.builder("EU", 32).build();
        check(input.withFaces(List.of(MachineFace.FRONT, MachineFace.TOP)).faces().size() == 2,
                "Resolved faces must be kept");
        reject(() -> input.withFaces(List.of(MachineFace.FRONT, MachineFace.FRONT)));

        MachineCasing casing = MachineCasing.builder("addon/casing", "iron").build();
        check(casing.resolve("addon_machine", true, 1).machineType().equals("addon_machine"),
                "Resolved machine type must be kept");
        reject(() -> casing.resolve("Bad Type!", true, -5));
    }

    private static void checkMachineTemplateContracts() {
        MachineCasing plain = MachineCasing.builder("addon_recipe", "iron").build();
        RecipeMachine valid = RecipeMachine.builder(plain)
                .energy(EnergyInput.builder("EU", 32).build())
                .recipeMap("test_map")
                .build();
        check(valid.casing().machineType().equals("addon_recipe"), "Recipe machine keeps the id as machine type");

        MachineCasing tiered = MachineCasing.builder("addon_tiered", "iron").tier(256).build();
        reject(() -> RecipeMachine.builder(tiered)
                .energy(EnergyInput.builder("EU", 32).build())
                .recipeMap("test_map")
                .build());
        reject(() -> RecipeMachine.builder(plain)
                .energy(EnergyInput.builder("EU", 0).build())
                .recipeMap("test_map")
                .build());
        reject(() -> RecipeMachine.builder(plain)
                .energy(EnergyInput.builder("EU", AddonLimits.MAX_PER_TICK + 1).build())
                .recipeMap("test_map")
                .build());
        reject(() -> RecipeMachine.builder(plain)
                .energy(EnergyInput.builder("EU", 32).range(64, 128).build())
                .recipeMap("test_map")
                .build());

        try {
            RecipeMachine.builder(MachineCasing.builder("addon/slash", "iron").build())
                    .energy(EnergyInput.builder("EU", 32).build())
                    .recipeMap("test_map")
                    .build();
            throw new AssertionError("A slash id must fail without an explicit machine type");
        } catch (IllegalArgumentException expected) {
            check(expected.getMessage().contains("machineType("), "Slash ids should point at machineType(...)");
        }
    }

    private static void checkMaterialContracts() {
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
    }

    private static void checkMachineFaceMaskContracts() {
        MachineFaceMasks masks = MachineFaceMasks.builder()
                .itemIn(MachineFaceMasks.TOP).itemOut(MachineFaceMasks.BOTTOM)
                .itemAutoIn(MachineFaceMasks.AUTO_NONE).build();
        check(MachineFaceMasks.has(masks.itemInputs(), MachineFaceMasks.TOP), "Face bits must be stored");
        check(masks.itemAutoInput() == MachineFaceMasks.AUTO_NONE, "Auto direction -1 must stay allowed");
        for (int dir : new int[] {-1, 6, 32, Integer.MAX_VALUE}) {
            reject(() -> MachineFaceMasks.builder().itemIn(dir));
        }
        reject(() -> MachineFaceMasks.builder().fluidOut(7));
        reject(() -> MachineFaceMasks.builder().energyIn(-1));
        reject(() -> MachineFaceMasks.builder().itemAutoIn(6));
        reject(() -> MachineFaceMasks.builder().fluidAutoOut(-2));
    }

    private static void checkModDataContracts() throws InterruptedException {
        ModData.bindPresence(id -> true);
        Locale previous = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
        try {
            ModData mod = new ModData("IronMod", "Iron Mod");
            check(ModData.MODS.get("ironmod") == mod, "Lowercase mod keys must not depend on the default locale");
        } finally {
            Locale.setDefault(previous);
        }
        ModData.MODS.clear();
        int threads = 4;
        int perThread = 250;
        Thread[] workers = new Thread[threads];
        for (int t = 0; t < threads; t++) {
            int base = t * perThread;
            workers[t] = new Thread(() -> {
                for (int i = 0; i < perThread; i++) new ModData("mod" + (base + i), "Mod " + (base + i));
            });
        }
        try {
            for (Thread worker : workers) worker.start();
            for (Thread worker : workers) worker.join();
            check(ModData.MODS.size() == threads * perThread,
                    "Concurrent mod construction must not lose entries");
        } finally {
            ModData.MODS.clear();
        }
        try {
            ModData.bindPresence(id -> true);
            throw new AssertionError("Rebinding mod presence must fail");
        } catch (IllegalStateException expected) {
            check(expected.getMessage().contains("already bound"), "Missing rebind diagnostic");
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

    private static BasicMachineParameters.Builder machine(String energy, long amount) {
        return BasicMachineParameters.builder("test_machine", new GTMaterial(1, "Test", "Test", 0xFFFFFF))
                .energy(energy, amount);
    }

    private static void range(BasicMachineParameters parameters, boolean large, long min, long max) {
        check(parameters.acceptedEnergyInMin(large) == min, "Unexpected minimum for " + parameters.energyType());
        check(parameters.acceptedEnergyInMax(large) == max, "Unexpected maximum for " + parameters.energyType());
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
