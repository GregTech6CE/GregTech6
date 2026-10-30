/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.contracts;

import gregtech.api.addon.machine.BehaviorMachine;
import gregtech.api.addon.machine.FuelGenerator;
import gregtech.api.addon.machine.FuelOutput;
import gregtech.api.addon.machine.LayoutPart;
import gregtech.api.addon.machine.PartSlots;
import gregtech.api.addon.machine.PartTanks;
import gregtech.api.addon.recipe.RecipeMachine;
import gregtech.api.addon.recipe.RecipeTable;
import gregtech.api.energy.EnergyInput;
import gregtech.api.energy.EnergyOutput;
import gregtech.api.energy.EnergyType;
import gregtech.api.machine.IoTarget;
import gregtech.api.machine.MachineCasing;
import gregtech.api.machine.MachineFace;
import gregtech.api.machine.MachineIo;
import gregtech.api.mod.ModData;
import gregtech.api.multiblock.CellMatch;
import gregtech.api.multiblock.LayoutCell;
import gregtech.api.multiblock.LayoutRepeat;
import gregtech.api.multiblock.StructureLayout;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValueObjectContractTest {
    @BeforeAll
    static void bindModPresence() {
        try {
            ModData.bindPresence(id -> true);
        } catch (IllegalStateException alreadyBound) {
            // Another test in this JVM already bound it.
        }
    }

    @Test
    void recordsUseGeneratedValueMethods() {
        for (Class<?> type : List.of(
                RecipeMachine.class, RecipeTable.class, BehaviorMachine.class, FuelGenerator.class,
                FuelOutput.class, LayoutPart.class, PartSlots.class, PartTanks.class,
                EnergyInput.class, EnergyOutput.class, MachineCasing.class, MachineIo.class,
                IoTarget.class, StructureLayout.class, LayoutCell.class, LayoutRepeat.class)) {
            assertTrue(type.isRecord(), type.getSimpleName() + " must stay a record");
        }
    }

    @Test
    void nonRecordsCoverEveryField() {
        assertCovers("EnergyType", EnergyType.of("EU"),
                variant("name", EnergyType.of("Custom")), Set.of());
        assertCovers("ModData", new ModData("contract_base", "Contract"),
                variant("id", new ModData("contract_other", "Contract")),
                Set.of("name", "prefix", "loaded"));
        assertCovers("CellMatch.Part", CellMatch.part("casing"),
                variant("name", CellMatch.part("other")), Set.of());
        assertCovers("CellMatch.Block", CellMatch.block("minecraft:stone"),
                variant("id", CellMatch.block("minecraft:dirt")), Set.of());
        assertCovers("CellMatch.Tag", CellMatch.tag("minecraft:logs"),
                variant("id", CellMatch.tag("minecraft:planks")), Set.of());
        assertCovers("CellMatch.AnyFluid", CellMatch.anyFluid(),
                variant("tag", CellMatch.anyFluid("minecraft:water")), Set.of());
        assertCovers("CellMatch.OneOf", CellMatch.oneOf(CellMatch.part("a"), CellMatch.part("b")),
                variant("members", CellMatch.oneOf(CellMatch.part("a"), CellMatch.part("c"))), Set.of());
        assertCovers("CellMatch.Air", CellMatch.air(), Map.of(), Set.of());
        assertCovers("CellMatch.AnyOpaque", CellMatch.anyOpaque(), Map.of(), Set.of());
    }

    @Test
    void energyFacesAreCanonicalAndOrderIndependent() {
        EnergyInput inputA = EnergyInput.builder(EnergyType.EU, 32)
                .faces(MachineFace.FRONT, MachineFace.TOP).build();
        EnergyInput inputB = EnergyInput.builder(EnergyType.EU, 32)
                .faces(MachineFace.TOP, MachineFace.FRONT).build();
        assertEquals(inputA, inputB);
        assertEquals(List.of(MachineFace.TOP, MachineFace.FRONT), inputA.faces());

        EnergyOutput outputA = EnergyOutput.builder(EnergyType.EU, 32, 1)
                .faces(MachineFace.FRONT, MachineFace.TOP).build();
        EnergyOutput outputB = EnergyOutput.builder(EnergyType.EU, 32, 1)
                .faces(MachineFace.TOP, MachineFace.FRONT).build();
        assertEquals(outputA, outputB);
        assertEquals(List.of(MachineFace.TOP, MachineFace.FRONT), outputA.faces());
    }

    private static Map<String, Supplier<Object>> variant(String field, Object value) {
        return Map.of(field, () -> value);
    }

    private static void assertCovers(String label, Object base, Map<String, Supplier<Object>> variants,
                                     Set<String> excluded) {
        for (Field field : base.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) continue;
            String name = field.getName();
            if (excluded.contains(name)) continue;
            Supplier<Object> variant = variants.get(name);
            assertNotNull(variant, label + "." + name + " has no contract variant");
            Object other = variant.get();
            assertNotEquals(base, other, label + "." + name + " must affect equals");
            assertNotEquals(base.toString(), other.toString(), label + "." + name + " must affect toString");
            assertTrue(base.hashCode() != other.hashCode(), label + "." + name + " must affect hashCode");
        }
    }
}
