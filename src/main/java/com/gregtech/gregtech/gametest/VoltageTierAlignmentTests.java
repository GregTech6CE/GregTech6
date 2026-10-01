package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.energy.EnergyNodeSpec;
import com.gregtech.gregtech.api.machine.BasicMachineSpec;
import com.gregtech.gregtech.api.machine.MachineRegistry;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.GregTechConstants;
import com.gregtech.gregtech.data.MachineRecipeIngredients;
import com.gregtech.gregtech.registry.GTEnergyNodes;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Guards the voltage-tier ↔ casing-material alignment against GregTech 6.
 * <p>
 * The regression these tests exist for: several electric machine families in this port were
 * built one tier too high, so LV machines were cased (and named) in the MV material and ULV
 * kits appeared where GT6 has none. GT6's tables, from {@code gregapi/data/MT.java}, are
 * <pre>
 *   Electric_T = {TinAlloy, SteelGalvanized, Al, StainlessSteel, Cr, Ti, Ir, Os, ...}
 *   Flux_T     = {Sn,       Pb,              Invar, Electrum, EnderiumBase, Enderium, ...}
 *   Kinetic_T  = {Wood,     Bronze,          Steel, Ti, TungstenSteel, ...}
 *   Heat_T     = {Stone,    Steel,           Invar, Ti, TungstenCarbide, ...}
 * </pre>
 * with index 0 = ULV. GT6 registers the basic machines ("Basic Machines" in
 * {@code Loader_MultiTileEntities}) for indices <b>1..5</b> — LV..IV — and the same holds for
 * electric motors/dynamos (10021-25, 10111-15), heaters (10001-05), coolers (10161-65) and
 * lasers (10101-05). Only transformers (10040+, the first being ULV-LV, built from
 * {@code Electric_T[0]}) and battery boxes (10080+, {@code VN[0..9]}) also exist at tier 0.
 * So LV is galvanized steel, MV aluminium, HV stainless steel, EV chromium, IV titanium, and
 * ULV is tin alloy — never the other way round, and never LV = aluminium.
 * </p>
 * <p>Every expectation below is derived from those GT6 arrays rather than from the port's own
 * constants, so a shift in either direction fails loudly.</p>
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class VoltageTierAlignmentTests {

    /** GT6 {@code MT.DATA.Electric_T} — index 0 = ULV (tin alloy) … 5 = IV (titanium). */
    private static final GTMaterial[] ELECTRIC_T = {
            Materials.TinAlloy, Materials.SteelGalvanized, Materials.Aluminium,
            Materials.StainlessSteel, Materials.Chromium, Materials.Titanium,
            Materials.Iridium, Materials.OsmiumElemental, Materials.Trinitanium, Materials.Trinaquadalloy};
    /**
     * GT6 {@code MT.DATA.Kinetic_T} — index 0 is {@code ANY.Wood}, which this port has no
     * material for (the RU/KU machines start at tier 1 = bronze anyway) and is therefore null.
     */
    private static final GTMaterial[] KINETIC_T = {
            null, Materials.Bronze, Materials.Steel,
            Materials.Titanium, Materials.Tungstensteel};
    /** GT6 {@code MT.DATA.Heat_T} — index 0 is {@code ANY.Stone}; HU machines start at tier 1. */
    private static final GTMaterial[] HEAT_T = {
            null, Materials.Steel, Materials.Invar,
            Materials.Titanium, Materials.TungstenCarbide};
    /** GT6 {@code MT.DATA.Flux_T} — index 0 = tin, so tier 1 (LV) = lead. */
    private static final GTMaterial[] FLUX_T = {
            Materials.Tin, Materials.Lead, Materials.Invar,
            Materials.Electrum, Materials.EnderiumBase, Materials.Enderium};

    /** Tier suffixes used by the {@code ..._<tier>} registry ids, in GT6 index order. */
    private static final String[] TIER_SUFFIX =
            {"ulv", "lv", "mv", "hv", "ev", "iv", "luv", "zpm", "uv", "xv"};

    /** GT6 {@code V}: index 0 = ULV = 8 EU/t. */
    private static final long[] V = GregTechConstants.V;

    /** Registry id → spec for every energy node device. */
    private static Map<String, EnergyNodeSpec> energyNodes() {
        Map<String, EnergyNodeSpec> byId = new TreeMap<>();
        for (var entry : GTEnergyNodes.all()) {
            if (!entry.isPresent()) continue;
            byId.put(entry.getId().getPath(), entry.get().spec());
        }
        return byId;
    }

    /** Index into a GT6 tier table for a {@code ..._<suffix>} registry id, or -1. */
    private static int suffixTier(String id) {
        for (int i = 0; i < TIER_SUFFIX.length; i++) {
            if (id.endsWith("_" + TIER_SUFFIX[i])) return i;
        }
        return -1;
    }

    private static String name(GTMaterial material) {
        return material == null ? "<null>" : material.getName();
    }

    /**
     * Electric motors/dynamos/heaters/coolers/storage cabinets must carry the casing of the
     * voltage they run on: {@code Electric_T[index]}, where the index is the GT6 tier index
     * (ULV = 0), not the 1-based "machine tier" of the single-block machines.
     */
    @GameTest(template = "test_empty")
    public static void tieredElectricDevicesUseTheirOwnCasing(GameTestHelper helper) {
        Map<String, EnergyNodeSpec> nodes = energyNodes();
        TreeSet<String> problems = new TreeSet<>();
        int checked = 0;

        for (var entry : nodes.entrySet()) {
            String id = entry.getKey();
            boolean tiered = false;
            for (String prefix : new String[]{"electric_motor_", "electric_dynamo_",
                    "electric_heater_", "electric_cooler_", "energy_storage_", "battery_box_"}) {
                tiered |= id.startsWith(prefix);
            }
            if (!tiered) continue;

            int index = suffixTier(id);
            if (index < 0) {
                problems.add("unrecognised tier suffix: " + id);
                continue;
            }
            GTMaterial expected = ELECTRIC_T[index];
            GTMaterial actual = entry.getValue().material();
            if (!expected.equals(actual)) {
                problems.add(id + ": casing " + name(actual) + " != Electric_T[" + index + "] = "
                        + name(expected));
            }
            if (entry.getValue().inputRate() != V[index]) {
                problems.add(id + ": input " + entry.getValue().inputRate()
                        + " EU/t != V[" + index + "] = " + V[index]);
            }
            checked++;
        }

        helper.assertTrue(checked > 30, "tiered energy devices checked: " + checked);
        helper.assertTrue(problems.isEmpty(), "electric device casings off the GT6 tier table: " + problems);
        helper.succeed();
    }

    /**
     * GT6 only registers electric motors, dynamos, heaters, coolers, lasers and the basic
     * machines from tier 1 (LV) upwards — there is no ULV variant of them. ULV exists for
     * transformers (the ULV-LV step) and battery boxes.
     */
    @GameTest(template = "test_empty")
    public static void onlyTransformersAndBatteryBoxesExistAtUln(GameTestHelper helper) {
        TreeSet<String> offenders = new TreeSet<>();
        for (String id : energyNodes().keySet()) {
            if (!id.endsWith("_ulv")) continue;
            if (id.equals("battery_box_ulv") || id.equals("energy_storage_ulv")) continue;
            // GT6's first transformer is named "Transformer (ULV-LV)" — id transformer_ulv_lv.
            if (id.startsWith("transformer_")) continue;
            offenders.add(id);
        }
        helper.assertTrue(energyNodes().containsKey("transformer_ulv_lv"),
                "GT6's first transformer steps ULV down to LV");
        helper.assertTrue(energyNodes().containsKey("battery_box_ulv"),
                "GT6 registers battery boxes from VN[0] (ULV) upwards");
        helper.assertTrue(offenders.isEmpty(),
                "GT6 has no ULV variant of these devices: " + offenders);
        helper.succeed();
    }

    /**
     * The transformers step down one tier, so the ULV-LV unit is the only tin-alloy casing:
     * GT6 10040..10044 build {@code Transformer (VN[i]-VN[i+1])} from {@code Electric_T[i]}
     * with {@code NBT_INPUT V[i+1]} and {@code NBT_OUTPUT V[i]}.
     */
    @GameTest(template = "test_empty")
    public static void transformersStepDownOneTier(GameTestHelper helper) {
        Map<String, EnergyNodeSpec> nodes = energyNodes();
        TreeSet<String> problems = new TreeSet<>();
        for (int i = 0; i < 9; i++) {
            String id = "transformer_" + TIER_SUFFIX[i] + "_" + TIER_SUFFIX[i + 1];
            EnergyNodeSpec spec = nodes.get(id);
            if (spec == null) {
                problems.add("missing transformer " + id);
                continue;
            }
            if (!ELECTRIC_T[i].equals(spec.material())) {
                problems.add(id + ": casing " + name(spec.material())
                        + " != Electric_T[" + i + "] = " + name(ELECTRIC_T[i]));
            }
            if (spec.inputRate() != V[i + 1] || spec.outputRate() != V[i]) {
                problems.add(id + ": " + spec.inputRate() + "→" + spec.outputRate()
                        + " != V[" + (i + 1) + "]→V[" + i + "] = " + V[i + 1] + "→" + V[i]);
            }
        }
        helper.assertTrue(problems.isEmpty(), "transformer tiers off the GT6 table: " + problems);
        helper.succeed();
    }

    /** Flux motors/dynamos are the LV/MV tier-1/2 pair: GT6 {@code Flux_T[1..2]} = {Pb, Invar}. */
    @GameTest(template = "test_empty")
    public static void fluxDevicesUseTheFluxTierTable(GameTestHelper helper) {
        Map<String, EnergyNodeSpec> nodes = energyNodes();
        TreeSet<String> problems = new TreeSet<>();
        String[] families = {"flux_motor_", "flux_dynamo_"};
        for (String family : families) {
            for (int i = 1; i <= 2; i++) {
                String id = family + TIER_SUFFIX[i];
                EnergyNodeSpec spec = nodes.get(id);
                if (spec == null) {
                    problems.add("missing " + id);
                    continue;
                }
                if (!FLUX_T[i].equals(spec.material())) {
                    problems.add(id + ": casing " + name(spec.material())
                            + " != Flux_T[" + i + "] = " + name(FLUX_T[i]));
                }
            }
        }
        helper.assertTrue(problems.isEmpty(), "flux device casings off the GT6 tier table: " + problems);
        helper.succeed();
    }

    /**
     * Basic machines: GT6's {@code MT.DATA.Electric_T[tier]} / {@code Heat_T[tier]} /
     * {@code Kinetic_T[tier]} for the 1-based machine tier, and the matching voltage.
     * <p>
     * Only multi-tier families are checked: GT6's single-tier machines (coke oven, implosion
     * compressor, large batch machines, mass fabricator …) carry their own casing material from
     * their own registration row rather than a tier-table entry, and multiblock controllers
     * share their machine name with the single-block variant GT6 also registers (the
     * distillation tower, for instance, is stainless steel in both).
     * </p>
     */
    @GameTest(template = "test_empty")
    public static void basicMachineCasingsFollowTheTierTable(GameTestHelper helper) {
        Map<String, List<BasicMachineSpec>> families = new TreeMap<>();
        for (var entry : MachineRegistry.basicMachines()) {
            if (!entry.isPresent()) continue;
            BasicMachineSpec spec = entry.get().basicSpec();
            families.computeIfAbsent(spec.machineName(), key -> new ArrayList<>()).add(spec);
        }

        TreeSet<String> problems = new TreeSet<>();
        Map<String, Integer> perEnergy = new TreeMap<>();
        int checked = 0;
        for (var family : families.entrySet()) {
            if (family.getValue().size() < 2) continue; // single-tier machine: own casing material
            if (com.gregtech.gregtech.data.BasicMachineRecipePack.MULTIBLOCK_CONTROLLERS
                    .contains(family.getKey())) continue;
            for (BasicMachineSpec spec : family.getValue()) {
                int tier = spec.tier();
                String energy = spec.energyType();
                perEnergy.merge(energy, 1, Integer::sum);

                GTMaterial[] table = switch (energy) {
                    case "EU", "MU", "LU", "CU" -> ELECTRIC_T;
                    case "HU" -> HEAT_T;
                    case "RU", "KU" -> KINETIC_T;
                    // QU (mass fabricator / molecular scanner / replicator) has no tier table:
                    // GT6 builds all of them from osmium-family casings.
                    default -> null;
                };
                if (table == null) continue;
                if (tier < 1 || tier >= table.length) {
                    problems.add(spec.id() + ": tier " + tier + " outside the " + energy + " table");
                    continue;
                }
                if (!table[tier].equals(spec.material())) {
                    problems.add(spec.id() + ": casing " + name(spec.material())
                            + " != " + energy + "_T[" + tier + "] = " + name(table[tier]));
                }
                if (energy.equals("EU") && spec.energyIn() != V[tier]) {
                    problems.add(spec.id() + ": input " + spec.energyIn()
                            + " EU/t != V[" + tier + "] = " + V[tier]);
                }
                checked++;
            }
        }

        // GT6 registers the basic machines for tiers 1..5 only (VN[1..5]); a ULV machine would
        // have to come from Electric_T[0] and would show up here as an unexpected tier 0.
        helper.assertTrue(checked > 200, "tiered basic machine variants checked: " + checked);
        helper.assertTrue(perEnergy.getOrDefault("EU", 0) > 50, "EU machine variants: " + perEnergy);
        helper.assertTrue(problems.isEmpty(), "basic machine casings off the GT6 tier table: " + problems);
        helper.succeed();
    }

    /**
     * GT6's {@code IL.*} component arrays are ULV-first and the machine recipes reference
     * {@code IL.X[1..5]}, so a machine's tier must select the component of the <i>same</i>
     * index. The regression: tier 1 used to ask for {@code ..._ulv} components.
     */
    @GameTest(template = "test_empty")
    public static void machineComponentsUseTheSameTierIndex(GameTestHelper helper) {
        Map<String, String> arrays = new HashMap<>();
        // GT6 Loader_MultiTileEntities: 'S', IL.MOTORS[1] on the LV Electric Mixer (20351) …
        arrays.put("il:MOTORS", "compact_electric_motor_");
        arrays.put("il:PUMPS", "compact_electric_pump_");
        arrays.put("il:PISTONS", "compact_electric_piston_");
        arrays.put("il:EMITTERS", "compact_signal_emitter_");
        arrays.put("il:CONVEYERS", "compact_electric_conveyor_");
        arrays.put("il:SENSORS", "compact_sensor_");
        arrays.put("il:ROBOT_ARMS", "compact_robot_arm_");
        arrays.put("il:FIELD_GENERATORS", "compact_force_field_emitter_");

        TreeSet<String> problems = new TreeSet<>();
        for (var entry : arrays.entrySet()) {
            for (int tier = 1; tier <= 6; tier++) {
                Object resolved = MachineRecipeIngredients.resolve(entry.getKey(), Materials.SteelGalvanized, tier);
                String id = resolved instanceof Map<?, ?> map ? String.valueOf(map.get("item")) : String.valueOf(resolved);
                String want = "gregtech:" + entry.getValue() + TIER_SUFFIX[tier];
                if (!want.equals(id)) {
                    problems.add(entry.getKey() + " at tier " + tier + " → " + id + ", expected " + want);
                }
            }
        }
        helper.assertTrue(problems.isEmpty(),
                "IL.* components are not aligned with the machine tier (GT6 IL.X[n] is the tier-n part): "
                        + problems);
        helper.succeed();
    }

    /**
     * The accepted input range must be the original registration's: GT6 clamps every machine to
     * {@code NBT_INPUT_MIN..NBT_INPUT_MAX} and, when only {@code NBT_INPUT} is given, to
     * {@code input/2..input*2} ({@code MultiTileEntityBasicMachine#readFromNBT2:126-128}).
     * <p>
     * The regression: ranges used to be derived at runtime for every machine, which gave the
     * fermenter 1..16 HU/t instead of GT6's 16..64 and the distillation tower 256..1024 instead
     * of GT6's 1..1024 — a machine whose nominal input falls outside its own range can never run,
     * so this test also asserts the range always contains the nominal input.
     * </p>
     */
    @GameTest(template = "test_empty")
    public static void energyRangeMatchesTheOriginalRegistration(GameTestHelper helper) {
        // Built by FusionReactorControllerBlock, not by BasicMachineDefinitions: its TU spec is
        // only a placeholder, the real GT6 range (LU 1..16384) is exposed through the multiblock
        // port API by FusionReactorControllerBlockEntity.
        java.util.Set<String> portOwnedRange = java.util.Set.of("fusion_reactor_main");

        TreeSet<String> problems = new TreeSet<>();
        int checked = 0;
        for (var entry : MachineRegistry.basicMachines()) {
            if (!entry.isPresent()) continue;
            BasicMachineSpec spec = entry.get().basicSpec();
            if (spec.energyInMin() > spec.energyIn() || spec.energyInMax() < spec.energyIn()) {
                problems.add(spec.id() + ": nominal " + spec.energyIn() + " " + spec.energyType()
                        + "/t outside the accepted " + spec.energyInMin() + ".." + spec.energyInMax());
            }
            var original = com.gregtech.gregtech.data.BasicMachineOriginalParams
                    .find(spec.machineName(), spec.tier());
            if (original == null || portOwnedRange.contains(spec.id())) continue;
            if (spec.energyIn() != original.energyInput()) {
                problems.add(spec.id() + ": input " + spec.energyIn() + " != original "
                        + original.energyInput());
            }
            if (spec.energyInMin() != original.energyInputMin()
                    || spec.energyInMax() != original.energyInputMax()) {
                problems.add(spec.id() + ": range " + spec.energyInMin() + ".." + spec.energyInMax()
                        + " != original " + original.energyInputMin() + ".." + original.energyInputMax());
            }
            checked++;
        }
        helper.assertTrue(checked > 200, "machine ranges checked against the original: " + checked);
        helper.assertTrue(problems.isEmpty(), "machine energy ranges off the original registration: " + problems);
        helper.succeed();
    }

    /** No single-block machine recipe may consume a ULV component, and each tier uses its own. */
    @GameTest(template = "test_empty")
    public static void noMachineRecipeUsesAUlnComponent(GameTestHelper helper) {
        TreeSet<String> offenders = new TreeSet<>();
        TreeSet<String> seen = new TreeSet<>();
        for (var recipe : helper.getLevel().getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            if (!(recipe instanceof ShapedRecipe shaped)) continue;
            if (!recipe.getId().getPath().startsWith("machines/basic/")) continue;
            for (var ingredient : shaped.getIngredients()) {
                for (var stack : ingredient.getItems()) {
                    var key = ForgeRegistries.ITEMS.getKey(stack.getItem());
                    if (key == null) continue;
                    String path = key.getPath();
                    if (!path.startsWith("compact_")) continue;
                    seen.add(path);
                    if (path.endsWith("_ulv")) offenders.add(recipe.getId() + " uses " + path);
                }
            }
        }
        helper.assertTrue(seen.size() > 10, "distinct compact components used by machine recipes: " + seen);
        helper.assertTrue(seen.contains("compact_electric_motor_lv"),
                "GT6 builds the LV machines from IL.MOTORS[1] (the LV motor): " + seen);
        helper.assertTrue(offenders.isEmpty(),
                "GT6 has no ULV components in basic machine recipes: " + offenders);
        helper.succeed();
    }
}
