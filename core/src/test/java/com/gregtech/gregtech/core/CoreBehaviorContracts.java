package com.gregtech.gregtech.core;

import com.gregtech.gregtech.api.definition.DefinitionCatalog;
import com.gregtech.gregtech.api.energy.EnergyPackets;
import com.gregtech.gregtech.api.energy.GTVoltageTiers;
import com.gregtech.gregtech.api.machine.crucible.CrucibleMath;
import com.gregtech.gregtech.api.material.AtomicProperties;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.multiblock.PartBindings;
import com.gregtech.gregtech.api.recipe.MachineWorkCost;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Fixed behavior samples: independent expected numbers, never a second copy of the algorithms. */
public final class CoreBehaviorContracts {
    private static int assertions;

    public static void main(String[] args) {
        workCostGoldens();
        machineEnergyGoldens();
        itemPipeRoutingAndDelivery();
        voltageGoldens();
        materialAndCrucibleUnits();
        signedPacketsAndFiniteBuffer();
        atomicMassBounds();
        definitionIdentityAndSnapshot();
        multiblockOwnershipLifecycle();
        addonLifecycle();
        System.out.println("Core behavior contracts passed: " + assertions + " assertions in 10 groups (Java 17; no game dependencies)");
    }

    /** Original BlueprintRegressionTests.java:276-283 numeric fixtures. */
    private static void workCostGoldens() {
        var normal = MachineWorkCost.calculate(32, 192, 1, true, 10000, 128, 512, false);
        check(normal != null, "ordinary machine work exists");
        equal(128, normal.minimumPower(), "normal overclock minimum power");
        equal(12288, normal.totalWork(), "normal overclock total energy");

        var cheap = MachineWorkCost.calculate(32, 192, 4, true, 5000, 512, 4096, true);
        check(cheap != null, "cheap parallel work exists");
        equal(32, cheap.minimumPower(), "cheap overclock retains recipe power");
        equal(49152, cheap.totalWork(), "four parallel operations at half efficiency");

        var timed = MachineWorkCost.calculate(0, 256, 64, false, 10000, 1, 16, false);
        check(timed != null, "time-based work exists");
        equal(1, timed.minimumPower(), "zero-power timed work minimum");
        equal(256, timed.totalWork(), "TU parallel recipes retain duration");
        check(MachineWorkCost.calculate(4096, Long.MAX_VALUE, 64, true, 2500, 512, 4096, true) == null,
                "overflow rejects before a platform consumes recipe ingredients");
        check(MachineWorkCost.calculate(513, 192, 1, true, 10000, 128, 512, false) == null,
                "insufficient voltage rejects work");
    }

    /** Original HazardDamageTests.java:208-222 tier boundaries and signed magnitude. */
    private static void voltageGoldens() {
        equal(8, GTVoltageTiers.VOLTAGES[0], "ULV volts");
        equal(32, GTVoltageTiers.VOLTAGES[1], "LV volts");
        equal(512, GTVoltageTiers.VOLTAGES[3], "HV volts");
        equal(8192, GTVoltageTiers.VOLTAGES[5], "IV volts");
        equal(8589934592L, GTVoltageTiers.VOLTAGES[15], "final voltage table entry");
        equal(0, GTVoltageTiers.tierMax(8), "ULV index determines zero wire hazard tier");
        equal(1, GTVoltageTiers.tierMax(9), "one volt above ULV selects LV");
        equal(1, GTVoltageTiers.tierMax(32), "exact LV upper bound");
        equal(3, GTVoltageTiers.tierMax(512), "exact HV upper bound");
        equal(5, GTVoltageTiers.tierMax(8192), "exact IV upper bound");
        equal(1, GTVoltageTiers.tierMin(32), "exact LV lower-tier lookup");
        equal(3, GTVoltageTiers.tierMin(512), "exact HV lower-tier lookup");
        equal(1, GTVoltageTiers.tierMin(40), "between LV and MV");
        equal(3, GTVoltageTiers.tierMin(520), "between HV and EV");
        equal(128, GTVoltageTiers.maxVoltageOf(40), "display voltage rounds up to MV");
        check("LV".equals(GTVoltageTiers.nameOf(32)), "LV label");
        check("PUV1".equals(GTVoltageTiers.nameOf(2097152)), "original post-ultimate label");
        check("XV".equals(GTVoltageTiers.nameOf(2147483648L)), "original maximum label");
        equal(2147483648L, com.gregtech.gregtech.data.GregTechConstants.V[14], "legacy voltage table uses original finite XV");
        equal(8589934592L, com.gregtech.gregtech.data.GregTechConstants.V[15], "legacy voltage table has no Long.MAX_VALUE sentinel");
        check(java.util.Arrays.equals(GTVoltageTiers.NAMES,com.gregtech.gregtech.data.GregTechConstants.VN), "one canonical voltage name table");
        equal(1, GTVoltageTiers.tierMax(-32), "signed voltage magnitude");
    }

    private static void machineEnergyGoldens() {
        equal(2, com.gregtech.gregtech.api.machine.BasicMachineEnergy.demanded(33, 64, 16), "GT6 whole-packet rounded demand");
        equal(2, com.gregtech.gregtech.api.machine.BasicMachineEnergy.accepted(33, 64, -16, 9), "signed packet admission");
        equal(64, com.gregtech.gregtech.api.machine.BasicMachineEnergy.add(33, 64, 16, 2), "inherited buffer clips last packet excess");
        equal(49, com.gregtech.gregtech.api.machine.BasicMachineEnergy.add(33, 64, 16, 1), "one admitted packet");
        equal(0, com.gregtech.gregtech.api.machine.BasicMachineEnergy.accepted(65, 64, 16, 9), "old over-capacity state never returns negative packets");
        equal(0, com.gregtech.gregtech.api.machine.BasicMachineEnergy.accepted(0, 64, Long.MIN_VALUE, 1), "unrepresentable signed magnitude rejected");
        equal(0, com.gregtech.gregtech.api.machine.BasicMachineEnergy.accepted(0, 64, 1, -1), "negative request rejected");
        equal(Long.MAX_VALUE, com.gregtech.gregtech.api.machine.BasicMachineEnergy.add(0, Long.MAX_VALUE, 2, Long.MAX_VALUE), "large packet train saturates without overflow");
        equal(32, com.gregtech.gregtech.api.machine.BasicMachineEnergy.drain(96,64), "per-tick rated drain keeps remaining input");
        equal(0, com.gregtech.gregtech.api.machine.BasicMachineEnergy.drain(16,64), "unused partial input discarded");
        equal(96, MachineWorkCost.advance(64,128,32), "ordinary recipe advances by supplied work");
        equal(128, MachineWorkCost.advance(120,128,32), "completed recipe stops at its cost");
        equal(Long.MAX_VALUE, MachineWorkCost.advance(Long.MAX_VALUE-4,Long.MAX_VALUE,32), "largest job can complete without wraparound");
    }

    private static void itemPipeRoutingAndDelivery() {
        var edges = Map.of("first", List.of("join"), "second", List.of("join"));
        var costs = Map.of("first", 5L, "second", 1L, "join", 4L);
        var selected = com.gregtech.gregtech.content.transport.WeightedItemPipeRoutes.bestExit(
                List.of("first", "second"), n -> costs.get(n), n -> edges.getOrDefault(n,List.of()), "join"::equals, 32);
        check(selected != null && "second".equals(selected.firstHop()), "shared exit reached by cheapest first hop");
        equal(5, selected.cost(), "minimum weighted path includes both pipe steps");
        equal(9, com.gregtech.gregtech.content.transport.WeightedItemPipeRoutes.minimumExitCost(
                "first", n -> costs.get(n), n -> edges.getOrDefault(n,List.of()), "join"::equals,32), "single-hop API retained");
        var tied = com.gregtech.gregtech.content.transport.WeightedItemPipeRoutes.bestExit(
                List.of("first","second"), n -> "first".equals(n)?3: "join".equals(n)?7:10,
                n -> "first".equals(n)?List.of("join"):List.of(), n -> !"first".equals(n),32);
        check(tied != null && "first".equals(tied.firstHop()), "equal costs retain original first-face ordering despite longer path");
        var saturated = com.gregtech.gregtech.content.transport.WeightedItemPipeRoutes.bestExit(
                List.of("first"), n -> Long.MAX_VALUE, n -> List.of(), n -> true,32);
        check(saturated != null && saturated.cost()==Long.MAX_VALUE, "saturated valid route is selectable");
        check(com.gregtech.gregtech.content.transport.WeightedItemPipeRoutes.bestExit(
                List.of("first","second"), n -> costs.get(n), n -> edges.getOrDefault(n,List.of()), "join"::equals,1)==null,
                "visit limit applies once to the combined scan");
        var cycle = com.gregtech.gregtech.content.transport.WeightedItemPipeRoutes.bestExit(
                List.of("first"), n -> 0, n -> "first".equals(n)?List.of("second"):List.of("first"), "second"::equals,32);
        check(cycle != null && "first".equals(cycle.firstHop()) && cycle.cost()==0, "zero-step cycles terminate and retain ingress hop");

        int[] actualUnits={0};boolean[] fails={false};
        var port = new com.gregtech.gregtech.content.transport.ItemPipeTransfer.Port<int[]>() {
            public boolean begin(boolean simulate){return true;}
            public int slots(){return 1;}
            public int[] stack(int slot){return new int[]{0,0};}
            public int count(int[] stack){return stack[1];}
            public int[] copyWithCount(int[] stack,int count){return new int[]{stack[0],count};}
            public boolean same(int[] a,int[] b){return a[0]==b[0];}
            public int[] insert(int slot,int[] offered,boolean simulate){
                if(fails[0])throw new IllegalStateException("foreign handler failed");
                int accepted=Math.min(offered[1],simulate?4:2);
                if(!simulate)actualUnits[0]+=accepted;
                return copyWithCount(offered,offered[1]-accepted);
            }
        };
        var probe=com.gregtech.gregtech.content.transport.ItemPipeTransfer.simulate(new int[]{1,6},port);
        equal(4,probe.planned(),"exit probe validates partial remainder");
        equal(0,actualUnits[0],"exit probe never performs actual insertion");
        var delivery=com.gregtech.gregtech.content.transport.ItemPipeTransfer.transfer(new int[]{1,6},port);
        equal(4,delivery.planned(),"normal delivery uses same destination plan");
        equal(2,delivery.accepted(),"only smaller actual commit is charged to source");
        equal(2,actualUnits[0],"actual provider commit matches source bill");
        fails[0]=true;
        var bad=com.gregtech.gregtech.content.transport.ItemPipeTransfer.simulate(new int[]{1,6},port);
        check(bad.handlerFailed()&&bad.planned()==0,"exceptional exit safely rejected");
        equal(2,actualUnits[0],"exceptional probe cannot execute a delivery");
    }

    private static void addonLifecycle() {
        var calls = new ArrayList<String>();
        var context = new com.gregtech.gregtech.api.addon.GregTechAddon.Context("neoforge", "1.21.1");
        for (String id : List.of("zz_example", "aa_example"))
            com.gregtech.gregtech.api.addon.GregTechAddons.register(new com.gregtech.gregtech.api.addon.GregTechAddon() {
                public String id() { return id; }
                public void onRecipesReady(Context actual) {
                    check(actual.equals(context) && actual.apiVersion()==1, "addon receives exact platform context");
                    calls.add(id);
                }
            });
        check(!com.gregtech.gregtech.api.addon.GregTechAddons.recipesReady(), "registration does not mean recipe lifecycle completed");
        com.gregtech.gregtech.api.addon.GregTechAddons.dispatchRecipesReady(context);
        check(calls.equals(List.of("aa_example", "zz_example")), "deterministic once-only addon ordering");
        check(com.gregtech.gregtech.api.addon.GregTechAddons.recipesReady(), "successful addon lifecycle ready");
        boolean repeated=false;
        try { com.gregtech.gregtech.api.addon.GregTechAddons.dispatchRecipesReady(context); }
        catch (IllegalStateException expected) { repeated=true; }
        check(repeated && calls.size()==2, "second dispatch rejects without another callback");
    }

    /** Source GTValues and GregTechConstants.L=144: one ingot is 144 L and one nugget 16 L. */
    private static void materialAndCrucibleUnits() {
        equal(648648000, GTValues.U, "source material unit is unchanged");
        equal(324324000, GTValues.U2, "half unit for one conductor");
        equal(216216000, GTValues.U3, "third unit");
        equal(162162000, GTValues.U4, "quarter unit");
        equal(72072000, GTValues.U9, "nugget unit");
        equal(9009000, GTValues.U72, "tiny material unit");
        equal(648648000, CrucibleMath.units(144, 144, GTValues.U, false), "144 L is one material unit");
        equal(72072000, CrucibleMath.units(16, 144, GTValues.U, false), "16 L is one nugget");
        equal(1945944000, CrucibleMath.units(432, 144, GTValues.U, false), "three ingots keep copper amount");
        equal(144, CrucibleMath.unitsScaled(GTValues.U, GTValues.U, 144, false), "material to fluid direction");
        equal(66, CrucibleMath.scale(2, 3, 100, false), "display truncation retains original semantics");
        equal(67, CrucibleMath.scale(2, 3, 100, true), "display ceiling retains original semantics");
        equal(100, CrucibleMath.scale(5, 3, 100, false), "display clamps above capacity");
        equal(0, CrucibleMath.units(144, 0, GTValues.U, false), "invalid denominator contributes no material");
    }

    /** AxialGeneratorTests.java:75-77 and AxleRepairTests.java:125,132 signed packet scenarios. */
    private static void signedPacketsAndFiniteBuffer() {
        equal(32, EnergyPackets.magnitude(-32, true), "negative RU remains a 32-unit packet");
        equal(0, EnergyPackets.magnitude(-32, false), "unsigned energy rejects negative packets");
        equal(0, EnergyPackets.magnitude(Long.MIN_VALUE, true), "signed overflow cannot become usable energy");
        equal(3, EnergyPackets.fitting(8, 100, 32), "buffer fits three whole LV packets, not fractions");
        equal(2, EnergyPackets.fitting(2, 100, 32), "requested packets cap buffer acceptance");
        equal(0, EnergyPackets.fitting(8, 31, 32), "a sub-packet buffer cannot accept one packet");
        equal(0, EnergyPackets.fitting(8, 100, 0), "zero packet magnitude rejected");
    }

    private static void atomicMassBounds() {
        equal(98, AtomicProperties.GT6_DEFAULT.mass(), "source default atomic mass");
        equal(0, AtomicProperties.ZERO.mass(), "zero-mass particle material");
        equal(-1, new AtomicProperties(0, 0, 0, -1).mass(), "negative additional mass remains allowed for Magic");
        rejects(IllegalArgumentException.class, () -> new AtomicProperties(-1, 0, 0, 0), "negative particle count");
        rejects(ArithmeticException.class, () -> new AtomicProperties(Long.MAX_VALUE, 0, 1, 0), "atomic mass overflow");
    }

    /** Original DefinitionCatalogTest.java:9-27 before the first platform registration. */
    private static void definitionIdentityAndSnapshot() {
        var source = new ArrayList<>(List.of("oven_steel", "electric_motor_lv"));
        var catalog = DefinitionCatalog.validated(source, id -> id);
        source.clear();
        check(catalog.equals(List.of("oven_steel", "electric_motor_lv")), "definition snapshot preserves stable IDs and order");
        rejects(UnsupportedOperationException.class, () -> catalog.add("extra"), "definition snapshot immutable");
        rejects(IllegalArgumentException.class,
                () -> DefinitionCatalog.validated(List.of("same", "same"), id -> id), "duplicate registration IDs");
        rejects(IllegalArgumentException.class,
                () -> DefinitionCatalog.validated(List.of("gregtech:oven"), id -> id), "namespace cannot enter a path ID");
    }

    private static void multiblockOwnershipLifecycle() {
        var bindings = new PartBindings<String, String>();
        var claimed = new HashSet<String>();
        var released = new HashSet<String>();
        check(bindings.update(Map.of("wall", "ENERGY_INPUT", "vent", "NONE"), p -> true,
                (p, role) -> claimed.add(p), released::add), "first valid structure claims parts");
        check(bindings.contains("wall") && bindings.contains("vent"), "structure keeps ownership for both parts");
        claimed.clear();
        check(!bindings.update(Map.of("wall", "ENERGY_INPUT", "foreign", "NONE"), p -> !p.equals("foreign"),
                (p, role) -> claimed.add(p), released::add), "foreign ownership invalidates structure");
        check(claimed.isEmpty(), "a failed structure cannot partially claim new parts");
        check(released.equals(Set.of("wall", "vent")), "invalidating a structure releases all its previous parts");
        check(!bindings.contains("wall") && !bindings.contains("vent"), "no stale claims after invalidation");
    }

    private static void equal(long expected, long actual, String behavior) {
        check(expected == actual, behavior + ": expected " + expected + ", actual " + actual);
    }

    private static void check(boolean passed, String behavior) {
        assertions++;
        if (!passed) throw new AssertionError(behavior);
    }

    private static void rejects(Class<? extends Throwable> expected, Runnable action, String behavior) {
        try {
            action.run();
        } catch (Throwable failure) {
            check(expected.isInstance(failure), behavior + ": unexpected " + failure);
            return;
        }
        check(false, behavior + ": expected " + expected.getSimpleName());
    }
}
