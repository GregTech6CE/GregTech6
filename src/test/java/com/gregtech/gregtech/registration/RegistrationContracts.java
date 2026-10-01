package com.gregtech.gregtech.registration;

import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.machine.BasicMachineSpec;
import com.gregtech.gregtech.api.energy.EnergyNodeSpec;
import com.gregtech.gregtech.api.fluid.FluidDefinition;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.content.machine.BasicMachineDefinitions;
import com.gregtech.gregtech.content.energy.EnergyNodeDefinitions;
import com.gregtech.gregtech.data.*;
import java.util.*;

/** Headless definition contracts. The runner supplies a test-only ModList fixture. */
@SuppressWarnings("deprecation")
public final class RegistrationContracts {
    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("material-memory")) {
            System.gc();
            Thread.sleep(150);
            long before = java.lang.management.ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
            GTMaterialRegistry.init();
            System.gc();
            Thread.sleep(150);
            long after = java.lang.management.ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
            System.out.printf(java.util.Locale.ROOT,
                    "MATERIAL_BOOTSTRAP_HEAP_DELTA %.2f MiB; %d materials (headless, includes initialization dependencies, not client RSS)%n",
                    (after - before) / 1048576.0, GTMaterialRegistry.allMaterials().size());
            return;
        }
        if (args.length == 2 && args[0].equals("catalog")) {
            GTMaterialRegistry.init();
            var rows = GTMaterialRegistry.allMaterials().stream()
                    .sorted(java.util.Comparator.comparing(GTMaterial::getName))
                    .map(m -> Map.of("name", m.getName(), "id", m.getId()))
                    .toList();
            var path = java.nio.file.Path.of(args[1]);
            java.nio.file.Files.createDirectories(path.getParent());
            java.nio.file.Files.writeString(path, new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(rows));
            System.out.println("Exported " + rows.size() + " material identities to " + path);
            return;
        }
        if (args.length > 0 && args[0].equals("references-first")) {
            if (Materials.Copper == null || Materials.Iron == null) throw new AssertionError("Early reference lost");
            checkReferences();
            System.out.println("PASS readable-reference-first initialization");
            return;
        }
        var builder = MaterialDefinition.builder(9999, "RegistrationTestMaterial").dust().color(0x102030);
        var definition = builder.build();
        builder.gas();
        if (definition.properties().contains(MaterialProperty.GAS)) throw new AssertionError("Mutable definition snapshot");
        definition.register();
        rejects(IllegalArgumentException.class, definition::register);
        rejects(IllegalArgumentException.class, () -> MaterialDefinition.builder(9999, "AnotherName").build().register());
        rejects(IllegalArgumentException.class, () -> MaterialDefinition.builder(9998, "RegistrationTestMaterial").build().register());
        rejects(IllegalArgumentException.class, () -> MaterialDefinition.builder(0, "BadId").build());
        GTMaterialRegistry.init();
        if (GTMaterialRegistry.registrationPhase() != GTMaterialRegistry.RegistrationPhase.READY)
            throw new AssertionError("Material linking did not complete");
        rejects(IllegalStateException.class, () -> MaterialDefinition.builder(9997, "TooLate").register());
        checkReferences();

        if (Materials.Copper.getProtons() != 29 || Materials.Iron.getProtons() != 26)
            throw new AssertionError("Element factory discarded atomic counts");
        if (Materials.Magic.getMass() != -1)
            throw new AssertionError("Magic must retain GT6 negative mass");
        var proton = com.gregtech.gregtech.content.material.ParticleMaterials.Proton;
        var electron = com.gregtech.gregtech.content.material.ParticleMaterials.Electron;
        if (proton.getId() != 4 || proton.getElectrons() != 0 || proton.getMass() != 1
                || electron.getElectrons() != 1 || electron.getAlpha() != 0
                || !proton.has(MaterialProperty.PARTICLE) || !proton.has(MaterialProperty.HIDDEN))
            throw new AssertionError("GT6 particle definitions changed");
        rejects(IllegalArgumentException.class, () -> new AtomicProperties(-1, 0, 0, 0));
        rejects(ArithmeticException.class, () -> new AtomicProperties(Long.MAX_VALUE, 0, 1, 0));

        var machines = BasicMachineDefinitions.specifications();
        if (machines.isEmpty()) throw new AssertionError("No built-in machines");
        var ids = new HashSet<String>();
        for (var machine : machines) {
            if (!ids.add(machine.id()) || machine.recipeMap() == null || machine.energyTag() == null)
                throw new AssertionError("Invalid built-in machine: " + machine.id());
        }
        var custom = BasicMachineSpec.builder("independent_recipe_machine", Materials.Steel)
                .recipes(MachineRecipeMaps.Mixer).energy(GregTechTags.Energy.RU, 32).parallel(7).build();
        if (custom.recipeMap() != MachineRecipeMaps.Mixer || custom.parallelLimit() != 7)
            throw new AssertionError("Explicit machine behavior not retained");
        rejects(IllegalArgumentException.class, () -> MachineRecipeMaps.byMachineName("typo_unknown_machine"));
        rejects(NullPointerException.class, () -> BasicMachineSpec.builder("missing_recipes", Materials.Steel)
                .energy(GregTechTags.Energy.EU, 32).build());
        rejects(IllegalArgumentException.class, () -> BasicMachineSpec.builder("invalid_parallel", Materials.Steel)
                .recipes(MachineRecipeMaps.Mixer).energy(GregTechTags.Energy.EU, 32).parallel(0).build());
        var energy = EnergyNodeDefinitions.specifications();
        if (energy.isEmpty()) throw new AssertionError("No energy definitions");
        rejects(IllegalArgumentException.class, () -> EnergyNodeSpec.builder("negative_energy", Materials.Steel)
                .texture("test").input(GregTechTags.Energy.EU, -1).output(GregTechTags.Energy.RU, 1)
                .names("Test", "Test").build());

        FluidDefinition.builder("registration_test.fluid").material(Materials.Copper).temperatureKelvin(400)
                .register("RegistrationTestFluid");
        rejects(IllegalArgumentException.class, () -> FluidDefinition.builder("registration_test_fluid")
                .register("RegistrationTestCollision"));
        rejects(IllegalArgumentException.class, () -> FluidDefinition.builder("registration_test_fluid_flowing")
                .register("RegistrationTestFlowingCollision"));
        rejects(IllegalArgumentException.class, () -> FluidDefinition.builder("negative_temperature").temperatureKelvin(-1).build());
        var malformed = FluidDefinition.builder("invalid_raw_entry").build().withTemperature(-1);
        rejects(IllegalArgumentException.class, () -> RegisteredFluids.registerDefinition("InvalidRawEntry", malformed));
        RegisteredFluids.closeDefinitions();
        rejects(IllegalStateException.class, () -> FluidDefinition.builder("too_late").register("TooLate"));
        System.out.println("PASS registration contracts: " + machines.size() + " machine variants; " + energy.size() + " energy devices");
    }

    private static void checkReferences() throws Exception {
        if (Materials.Copper != ImportedMaterialData.Cu || Materials.Iron != ImportedMaterialData.Fe || Materials.Magic != ImportedMaterialData.Ma)
            throw new AssertionError("Material alias identity changed");
        int count = 0;
        for (var field : Materials.class.getFields()) {
            if (field.getType() == GTMaterial.class) {
                if (field.get(null) == null) throw new AssertionError("Uninitialized material " + field.getName());
                count++;
            }
        }
        if (count < 700) throw new AssertionError("Incomplete readable material references");
    }

    private static void rejects(Class<? extends Throwable> expected, Runnable action) {
        try { action.run(); }
        catch (Throwable failure) {
            if (expected.isInstance(failure)) return;
            throw new AssertionError("Wrong exception, expected " + expected.getName(), failure);
        }
        throw new AssertionError("Expected " + expected.getName());
    }
}
