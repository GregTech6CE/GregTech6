package com.gregtech.gregtech.core;

import com.gregtech.gregtech.api.definition.DefinitionCatalog;
import com.gregtech.gregtech.api.fluid.FluidPipeChannels;
import com.gregtech.gregtech.api.fluid.FluidPipeSafety;
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
        sourceSteamConversion();
        itemPipeRoutingAndDelivery();
        fluidPipeChannels();
        fluidPipeSafety();
        fluidPipeCatalog();
        voltageGoldens();
        materialAndCrucibleUnits();
        signedPacketsAndFiniteBuffer();
        atomicMassBounds();
        definitionIdentityAndSnapshot();
        multiblockOwnershipLifecycle();
        addonLifecycle();
        miniaturePortalSignals();
        bedrockAndBoilerSourceSamples();
        assertions += OriginWorldgenSamples.verify();
        System.out.println("Core behavior contracts passed: " + assertions + " assertions in 17 groups (Java 17; no game dependencies)");
    }

    private static void bedrockAndBoilerSourceSamples() {
        // WorldgenOresBedrock.generateVein: all 840 cells of the six-layer muffin are filled,
        // even when every random ore roll is blank. Absolute tail heights adapt the y=0 source.
        var filled = new HashSet<String>();
        int[] floorWrites = {0}, highestTail = {Integer.MIN_VALUE};
        boolean generated = com.gregtech.gregtech.worldgen.MineralWorldgenRules.bedrockVein(
                0, 0, -64, 63, bound -> bound - 1,
                new com.gregtech.gregtech.worldgen.MineralWorldgenRules.VeinSink() {
                    public boolean isBedrockFloor(int x,int y,int z) { return x == 8 && y == -64 && z == 8; }
                    public void prepareStone(int x,int y,int z) { filled.add(x + "," + y + "," + z); }
                    public boolean bedrock(int x,int y,int z,boolean small) { floorWrites[0]++; return y == -64; }
                    public boolean ore(int x,int y,int z,boolean small) { highestTail[0] = Math.max(highestTail[0],y); return true; }
                });
        check(generated, "a forced core counts even when all random core rolls are blank");
        equal(1, floorWrites[0], "blank bedrock rolls leave one forced large ore");
        equal(840, filled.size(), "GT6 six muffin layers contain 840 cells");
        check(filled.contains("8,-63,8"), "bedrock directly above the core becomes mother stone");
        check(!filled.contains("8,-64,8"), "mother stone never overwrites the floor");
        check(!filled.contains("0,-63,0"), "outside the source muffin remains untouched");
        check(filled.contains("0,-60,0"), "the fourth source layer spans the whole chunk");
        equal(62, highestTail[0], "small-ore trails reach one below the absolute sea level");

        // MultiTileEntityBoilerTank:165-175, 202-215, 242: independent threshold/cap samples.
        check(com.gregtech.gregtech.content.energy.BoilerHazards.contactDamage(2000,0) == 0, "boiler contact threshold is strictly above 2000 HU");
        check(com.gregtech.gregtech.content.energy.BoilerHazards.contactDamage(0,4002) > 1, "stored steam contributes half its amount to contact heat");
        check(com.gregtech.gregtech.content.energy.BoilerHazards.contactDamage(50000,0) == 10, "source contact damage caps at ten");
        check(com.gregtech.gregtech.content.energy.BoilerHazards.descalingDamage(50000,0,9999,15) == 25, "descaling damage has no contact cap");
        check(com.gregtech.gregtech.content.energy.BoilerHazards.descalingDamage(50000,0,10000,15) == 0, "clean boilers do not descale");
        check(com.gregtech.gregtech.content.energy.BoilerHazards.descalingDamage(50000,0,9999,16) == 0, "unsafe descaling explodes instead of applying heat");
        check(com.gregtech.gregtech.content.energy.BoilerHazards.explosionPower(9_000_000) == 30, "source boiler explosion has no invented eight-block strength cap");
    }

    private static void miniaturePortalSignals() {
        // MultiTileEntityMiniPortal:113-180: pinned inbox/phase/timeout trace, no game launch.
        var portal = new com.gregtech.gregtech.content.logistics.MiniPortalSignals();
        portal.receive(100, 4, 7, 3);
        portal.receive(100, 4, 11, 2);
        portal.receive(100, 5, 4, 9);
        portal.advance(100);
        equal(0, portal.redstone(4), "a scan never emits its own tick's input");
        // Receiving before the remote entity's tick must first apply the old inbox.
        portal.receive(101, 4, 2, 5);
        equal(11, portal.redstone(4), "same tick senders merge by maximum");
        equal(3, portal.comparator(4), "comparator merges independently");
        equal(9, portal.comparator(5), "opposite side inbox remains independent");
        portal.advance(101);
        equal(11, portal.redstone(4), "remote tick cannot apply an inbox twice");
        portal.advance(102);
        equal(2, portal.redstone(4), "next tick can lower redstone");
        equal(5, portal.comparator(4), "next tick advances comparator");
        for (long tick = 103; tick <= 122; tick++) portal.advance(tick);
        equal(2, portal.redstone(4), "twenty absent scans keep the previous redstone");
        portal.advance(123);
        equal(0, portal.redstone(4), "twenty-first absent scan clears disconnected redstone");
        equal(0, portal.comparator(4), "disconnected comparator also clears");
        portal.receive(123, 5, 12, 6);
        portal.disconnect(123);
        portal.advance(124);
        equal(0, portal.redstone(5), "counterpart deactivation overwrites a pending high input");
        portal.receive(124, 0, 15, 8);
        portal.advance(125);
        portal.clear();
        portal.advance(126);
        equal(0, portal.redstone(0), "deactivation clears outputs and pending inboxes");
        equal(0, portal.maximumComparator(), "deactivation clears every comparator side");
    }

    private static void sourceSteamConversion() {
        // MultiTileEntityTurbineSteam consumes a batch once, halves its energy over two ticks;
        // TE_Behavior_Energy_Converter emits one variable packet and wastes input maximum.
        var first = com.gregtech.gregtech.content.energy.SteamTurbineConversion.step(0,0,96,0,48);
        equal(48, first.energy(), "96L source steam first half");
        equal(48, first.pending(), "96L source steam pending half");
        equal(96, first.consumed(), "consume the whole steam batch once");
        equal(96, first.remainder(), "condensate remainder below 160L");
        var second = com.gregtech.gregtech.content.energy.SteamTurbineConversion.step(0,48,96,96,48);
        equal(48, second.energy(), "pending half restored next tick");
        equal(0, second.pending(), "pending half used once");
        equal(0, second.consumed(), "new steam waits for next batch");
        equal(16, com.gregtech.gregtech.content.energy.SteamTurbineConversion.output(48,48,16), "bronze turbine nominal RU");
        equal(32, com.gregtech.gregtech.content.energy.SteamTurbineConversion.output(96,48,16), "bronze turbine full packet");
        equal(22, com.gregtech.gregtech.content.energy.SteamTurbineConversion.output(32,32,22), "LV dynamo full packet EU");
        equal(11, com.gregtech.gregtech.content.energy.SteamTurbineConversion.output(16,32,22), "minimum dynamo packet EU");
        equal(0, com.gregtech.gregtech.content.energy.SteamTurbineConversion.waste(48,48), "turbine spends steam even without receiver");
        equal(4, com.gregtech.gregtech.content.energy.SteamTurbineConversion.waste(100,48), "subtract maximum input, not recommended input");
        var condensate = com.gregtech.gregtech.content.energy.SteamTurbineConversion.step(0,0,64,96,48);
        equal(1, condensate.condensate(), "160L accumulated source steam yields one distilled water");
        equal(0, condensate.remainder(), "160L condensate resets remainder");
        var stopped = com.gregtech.gregtech.content.energy.SteamTurbineConversion.step(7,48,96,96,48);
        equal(55, stopped.energy(), "stopping the inlet does not cancel a stored batch");
        equal(0, stopped.pending(), "stored second half is released");
        equal(0, stopped.consumed(), "staged half does not consume another steam batch");
        equal(0, com.gregtech.gregtech.content.energy.SteamTurbineConversion.step(0,0,47,0,48).consumed(), "batch below source input minimum waits");
    }

    private static void fluidPipeCatalog() {
        // Fixed source rows: GT6 Loader_MultiTileEntities:1846-1885. Gas/acid/plasma/magic bits.
        String[] rows = {
            "wood,50,0000,340",
            "treated_wood,75,0000,340",
            "plastic,100,1000,370",
            "rubber,100,1000,350",
            "copper,100,1000,0",
            "aluminium,100,1000,0",
            "tin_alloy,125,1000,0",
            "bronze,150,1000,0",
            "invar,200,1000,0",
            "steel,200,1000,0",
            "galvanized_steel,250,1000,0",
            "hsla,250,1000,0",
            "gold,100,1100,0",
            "chrome,200,1100,0",
            "stainless_steel,250,1100,0",
            "vanadium_steel,400,1100,0",
            "desh,200,1001,0",
            "tungsten_alloy,300,1001,0",
            "tungsten_steel,400,1001,0",
            "tungsten_carbide,450,1001,0",
            "desh_alloy,350,1001,0",
            "palladium,400,1001,0",
            "carbon,1000,1000,0",
            "tantalum_hafnium_carbide,300,1001,0",
            "titanium,300,1000,0",
            "tungsten,350,1101,0",
            "efrine,250,1011,0",
            "netherite,300,1111,0",
            "iridium,500,1101,0",
            "ironwood,200,1001,0",
            "thaumium,250,1101,0",
            "manasteel,250,1101,0",
            "void_metal,500,1101,0",
            "terrasteel,500,1101,0",
            "gaia_spirit,1000,1111,0",
            "bedrock_hsla,1000,1001,0",
            "adamantium,10000,1111,0",
            "draconium,2500,1111,0",
            "awakened_draconium,10000,1111,0",
            "infinity,1000000000,1111,0",
        };
        var specs = com.gregtech.gregtech.content.transport.fluid.FluidTransportDefinitions.pipes();
        equal(280, specs.size(), "40 original materials in seven sizes");
        Map<String, com.gregtech.gregtech.api.machine.PipeSpec> byId = new java.util.HashMap<>();
        for (var spec : specs) check(byId.put(spec.id(), spec) == null, "unique pipe registry ID");
        String[] sizes = {"tiny", "small", "medium", "large", "huge", "quadruple", "nonuple"};
        long[] multiples = {1, 2, 6, 12, 24, 6, 2};
        int[] channels = {1, 1, 1, 1, 1, 4, 9};
        for (String row : rows) {
            String[] fields = row.split(",");
            for (int i = 0; i < sizes.length; i++) {
                String id = "pipe_" + sizes[i] + "_" + fields[0];
                var spec = byId.get(id);
                check(spec != null, "expected pipe " + id);
                equal(Long.parseLong(fields[1]) * multiples[i], spec.capacity(), id + " capacity");
                equal(channels[i], spec.tankCount(), id + " channels");
                equal(fields[0].equals("wood") || fields[0].equals("treated_wood") ? 150 : 0,
                        spec.flammability(), id + " original flammability and fire spread");
                String flags = (spec.gasProof() ? "1" : "0") + (spec.acidProof() ? "1" : "0")
                        + (spec.plasmaProof() ? "1" : "0") + (spec.magicProof() ? "1" : "0");
                check(fields[2].equals(flags), id + " four independent proof flags");
                long explicit = Long.parseLong(fields[3]);
                if (explicit > 0) equal(explicit, spec.maxTemperature(), id + " explicit temperature");
            }
        }
        var steel = byId.get("pipe_medium_steel");
        var explicit = com.gregtech.gregtech.api.machine.PipeSpec.of("custom", steel.material(), steel.size(),
                200, true, false, false, true, 345);
        equal(345, explicit.maxTemperature(), "factory preserves explicit limit instead of recomputing it");
    }

    private static void fluidPipeSafety() {
        check(FluidPipeSafety.canIgnite(false, true, false, false), "air can ignite without checking flammability");
        check(FluidPipeSafety.canIgnite(false, true, true, true), "flammable GT noncolliding block may burn");
        check(!FluidPipeSafety.canIgnite(false, true, true, false), "nonflammable GT block remains protected");
        check(!FluidPipeSafety.canIgnite(true, true, false, true), "lava/fire/explicit protection wins over flammability");
        check(!FluidPipeSafety.canIgnite(false, false, false, true), "solid flammable blocks are not directly replaced");
        check(!FluidPipeSafety.canIgnite(false, false, true, false), "solid GT blocks stay intact");
        equal(4, FluidPipeSafety.magicLoss(true, false, false), "magic liquid loses four");
        equal(16, FluidPipeSafety.magicLoss(true, true, false), "magic gas loses sixteen before physical leakage");
        equal(0, FluidPipeSafety.magicLoss(true, false, true), "magic proof protects liquid");
        equal(0, FluidPipeSafety.magicLoss(true, true, true), "magic proof protects gas");
        equal(0, FluidPipeSafety.magicLoss(false, false, false), "ordinary liquid has no magic loss");
        equal(0, FluidPipeSafety.magicLoss(false, true, false), "ordinary gas has no magic loss");
        equal(24, FluidPipeSafety.magicLoss(true, true, false)
                + FluidPipeSafety.losses(true, false, false, false, false, false).gas(),
                "unprotected magical gas loses sixteen plus eight");
        var steelAcidGas = FluidPipeSafety.losses(true, false, true, true, false, false);
        equal(0, steelAcidGas.gas(), "gas proof excludes only gas loss");
        equal(16, steelAcidGas.acid(), "acid gas still corrodes steel");
        var acidProofGas = FluidPipeSafety.losses(true, false, true, false, false, true);
        equal(8, acidProofGas.gas(), "acid proof does not stop gas loss");
        equal(0, acidProofGas.acid(), "acid proof prevents chemical branch");
        var compound = FluidPipeSafety.losses(true, true, true, false, false, false);
        equal(88, compound.gas() + compound.plasma() + compound.acid(), "independent physical hazards accumulate");
        var proof = FluidPipeSafety.losses(true, true, true, true, true, true);
        equal(0, proof.gas() + proof.plasma() + proof.acid(), "all physical proofs stop losses");
        equal(300, FluidPipeSafety.observeTemperature(1300, 300, true), "first fluid replaces old hot temperature");
        equal(1300, FluidPipeSafety.observeTemperature(1300, 300, false), "later cold channel preserves hottest temperature");
        equal(1300, FluidPipeSafety.observeTemperature(300, 1300, false), "later hot channel raises temperature");
        equal(999, FluidPipeSafety.emptyTemperature(1000, 300), "empty pipe cools by one kelvin");
        equal(101, FluidPipeSafety.emptyTemperature(100, 300), "empty pipe warms by one kelvin");
        equal(300, FluidPipeSafety.emptyTemperature(300, 300), "ambient equilibrium");
        equal(Long.MAX_VALUE - 1, FluidPipeSafety.emptyTemperature(Long.MAX_VALUE, Long.MIN_VALUE), "extreme cooling cannot overflow");
        equal(Long.MIN_VALUE + 1, FluidPipeSafety.emptyTemperature(Long.MIN_VALUE, Long.MAX_VALUE), "extreme warming cannot overflow");
    }

    private static void fluidPipeChannels() {
        equal(2, FluidPipeChannels.select(4, i -> i == 2, i -> i == 0 || i == 3),
                "existing fluid wins over an earlier empty channel");
        equal(1, FluidPipeChannels.select(4, i -> false, i -> i == 1 || i == 3),
                "new fluid uses first empty channel");
        equal(-1, FluidPipeChannels.select(4, i -> false, i -> false), "occupied incompatible pipe refuses fluid");
        equal(0, FluidPipeChannels.select(1, i -> false, i -> true), "single-channel receiver is independent of source index");
        equal(-1, FluidPipeChannels.select(0, i -> false, i -> false), "absent channel rejected");
        equal(101, FluidPipeChannels.distributionLevel(101, new long[]{100}, 0), "odd shared mean rounds up");
        equal(200, FluidPipeChannels.distributionLevel(600, new long[]{0, 0}, 0), "three-way source-inclusive mean");
        equal(200, FluidPipeChannels.distributionLevel(600, new long[]{0}, 1), "machines share the same mean");
        equal(267, FluidPipeChannels.distributionLevel(600, new long[]{200}, 1), "round combined mean upward");
        equal(Long.MAX_VALUE, FluidPipeChannels.distributionLevel(Long.MAX_VALUE,
                new long[]{Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE}, 0), "long sum cannot overflow");
        equal(4611686018427387904L, FluidPipeChannels.distributionLevel(Long.MAX_VALUE, new long[]{}, 1),
                "large source and empty machine mean");
        equal(0, FluidPipeChannels.pressureShare(600, 1200, 2), "half capacity creates no pressure");
        equal(100, FluidPipeChannels.pressureShare(800, 1200, 2), "excess pressure split across pipes");
        equal(0, FluidPipeChannels.pressureShare(1200, 1200, 0), "no pipe pressure targets");
        equal(0, FluidPipeChannels.cauldronCost(0, 333), "partial cauldron step cannot consume fluid");
        equal(334, FluidPipeChannels.cauldronCost(0, 666), "one affordable cauldron step");
        equal(667, FluidPipeChannels.cauldronCost(0, 999), "two affordable cauldron steps");
        equal(1000, FluidPipeChannels.cauldronCost(0, 1000), "full cauldron costs one bucket");
        equal(667, FluidPipeChannels.cauldronCost(1, 1000), "two remaining steps paid together");
        equal(334, FluidPipeChannels.cauldronCost(2, 1000), "only last step paid");
        equal(0, FluidPipeChannels.cauldronCost(3, 1000), "full cauldron consumes nothing");
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
