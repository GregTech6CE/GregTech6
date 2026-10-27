/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Known CR.REV ingredients from Loader_MultiTileEntities and BlockGlass*. */
package gregtech.content.machine;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.api.material.MaterialComponent;
import gregtech.content.data.ImportedMaterialData;
import gregtech.content.data.OriginalSurvivalRecipes;
import gregtech.content.energy.BatteryBoxDefinitions;
import gregtech.content.energy.ChemicalBatterySpec;
import gregtech.content.material.Materials;
import gregtech.content.tool.FluidAttachmentSpec;
import gregtech.engine.material.ItemComposition;
import gregtech.engine.material.ReversibleCraftingData;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Source-known material components for devices omitted by the machine catalog. */
public final class OriginalDeviceMaterialData {
    private static final long U = GTValues.U;
    private static final List<FluidAttachmentSpec> ATTACHMENTS = FluidAttachmentSpec.all();
    private static final List<ChemicalBatterySpec> BATTERIES = ChemicalBatterySpec.all();
    private static final List<String> QUANTUM_IDS = List.of("quantum_energizer_ev", "quantum_energizer_iv",
            "quantum_energizer_luv", "quantum_energizer_zpm", "quantum_energizer_uv");
    private OriginalDeviceMaterialData() {}

    private static MaterialComponent part(GTMaterial material, long amount) {
        return MaterialComponent.of(material, amount);
    }
    private static ItemComposition data(String source, MaterialComponent... parts) {
        return ReversibleCraftingData.perItem(List.of(parts), 1, source);
    }

    public static Optional<ItemComposition> block(String id) {
        if (id.startsWith("crucible_faucet_")) {
            if (id.equals("crucible_faucet_ceramic"))
                return Optional.of(data("GT6 ceramic faucet", part(Materials.Ceramic, 3 * U)));
            for (var row : OriginalSurvivalRecipes.ROWS) if (row.id().equals(id)) {
                GTMaterial material = id.equals("crucible_faucet_carbon") ? Materials.Graphene
                        : id.equals("crucible_faucet_quartz") ? Materials.NetherQuartz : row.material();
                return Optional.of(data("GT6 crucible faucet CR.REV", part(material, 3 * U)));
            }
        }
        if (id.startsWith("tap_") || id.startsWith("nozzle_") || id.startsWith("cap_nozzle_")
                || id.startsWith("fluid_funnel_"))
            for (var spec : ATTACHMENTS) if (spec.id().equals(id))
                return Optional.of(attachment(spec.shape(), spec.material()));
        if (id.equals("tap")) return Optional.of(attachment("tap", Materials.Ceramic));
        if (id.equals("nozzle")) return Optional.of(attachment("nozzle", Materials.Steel));
        if (id.equals("crank")) return Optional.of(data("GT6 hand crank CR.REV",
                part(ImportedMaterialData.Fe, 5 * U + U / 2)));
        if (id.equals("advanced_button")) return Optional.of(data("GT6 advanced button CR.REV",
                part(Materials.TinAlloy, 3 * U / 9 + U / 4), part(Materials.RedAlloy, U / 4),
                part(Materials.Lumium, U / 8)));
        if (id.equals("fluid_cup")) return Optional.of(data("GT6 porcelain cup",
                part(gregtech.content.material.SupplementalMaterials.porcelain(), U)));
        if (id.startsWith("fluid_barometer_gas_cylinder")) {
            for (var spec : gregtech.engine.fluid.PortableFluidContainerSpec.values())
                if (id.equals("fluid_" + spec.id())) return Optional.of(data("GT6 barometer gas cylinder CR.REV",
                        part(spec.material(), 3 * U + U / 2 + 2 * (U / 9))));
        }
        if (id.equals("dungeon_portal_nether")) return Optional.of(data("GT6 miniature Nether portal CR.REV",
                part(Materials.Obsidian, 8 * U)));
        if (id.equals("dungeon_portal_end")) return Optional.of(data("GT6 miniature End portal CR.REV",
                part(ImportedMaterialData.STONES.Endstone, 4 * U), part(Materials.EnderEye, 4 * U)));
        if (id.startsWith("glass_clear") || id.startsWith("glass_glow")) {
            boolean slab = id.startsWith("glass_clear_slab") || id.startsWith("glass_glow_slab");
            boolean glow = id.startsWith("glass_glow");
            long half = slab ? 2 : 1;
            return Optional.of(glow ? data("GT6 luminous glass", part(Materials.Glass, 9 * U / half), part(Materials.Glowstone, U / half))
                    : data("GT6 clear glass", part(Materials.Glass, 9 * U / half)));
        }
        if (id.startsWith("battery_"))
            for (var spec : BATTERIES) if (spec.id().equals(id)) return Optional.of(battery(spec));
        var transport = transport(id);
        if (transport.isPresent()) return transport;
        var tier = BatteryBoxDefinitions.TIERS;
        if (id.startsWith("crystal_charger_") || id.startsWith("large_crystal_charger_")
                || id.startsWith("laser_absorber_")) for (int i = 0; i < tier.size(); i++) {
            String suffix = tier.get(i);
            if (id.equals("crystal_charger_" + suffix) || id.equals("large_crystal_charger_" + suffix)) {
                int processors = id.startsWith("large_") ? 4 : 2;
                var parts = new ArrayList<MaterialComponent>();
                parts.add(part(BatteryBoxDefinitions.material(i), 8 * U));
                parts.add(part(Materials.Platinum, processors * U));
                parts.add(part(Materials.Emerald, processors * U));
                fieldGenerators(parts, i, processors * 2);
                return Optional.of(ReversibleCraftingData.perItem(parts, 1, "GT6 crystal charger known CR.REV inputs"));
            }
            if (i >= 1 && i <= 5 && id.equals("laser_absorber_" + suffix)) {
                GTMaterial[] wire = {ImportedMaterialData.Pb, ImportedMaterialData.Sn, ImportedMaterialData.Cu,
                        ImportedMaterialData.Au, ImportedMaterialData.Al, ImportedMaterialData.Pt};
                return Optional.of(data("GT6 laser absorber known CR.REV inputs",
                        part(BatteryBoxDefinitions.material(i), 8 * U), part(ImportedMaterialData.Si, 3 * U),
                        part(wire[i], U), part(Materials.Rubber, 2 * U),
                        part(Materials.Platinum, U), part(Materials.Sapphire, U)));
            }
        }
        int quantumTier = id.startsWith("quantum_energizer_") ? QUANTUM_IDS.indexOf(id) + 1 : 0;
        if (quantumTier > 0) {
            var parts = new ArrayList<MaterialComponent>();
            parts.add(part(Materials.Osmiridium, 8 * U));
            parts.add(part(Materials.Platinum, 4 * U));
            parts.add(part(Materials.Sapphire, 4 * U));
            fieldGenerators(parts, quantumTier, 2);
            signalDevices(parts, quantumTier);
            return Optional.of(ReversibleCraftingData.perItem(parts, 1, "GT6 quantum energizer known CR.REV inputs"));
        }
        if (id.equals("zpm_discharger_basic") || id.equals("zpm_discharger_advanced")) {
            var parts = new ArrayList<MaterialComponent>();
            parts.add(part(Materials.Osmiridium, 56 * U));
            parts.add(part(Materials.Platinum, 2 * U));
            parts.add(part(id.endsWith("basic") ? Materials.Ruby : Materials.Sapphire, 2 * U));
            fieldGenerators(parts, 6, 2);
            return Optional.of(ReversibleCraftingData.perItem(parts, 1, "GT6 ZPM decharger known CR.REV inputs"));
        }
        return Optional.empty();
    }

    private static ItemComposition attachment(String shape, GTMaterial material) {
        if (shape.equals("tap") || shape.equals("fluid_funnel"))
            return data("GT6 fluid attachment CR.REV", part(material, 3 * U));
        if (shape.equals("nozzle")) return data("GT6 nozzle CR.REV", part(material, 2 * U),
                part(metalNozzleRing(material) ? material : Materials.Rubber, U / 4));
        if (shape.equals("cap_nozzle")) {
            boolean metalRing = metalNozzleRing(material);
            GTMaterial nozzle = material == Materials.TantalumHafniumCarbide ? Materials.Tungsten : material;
            return data("GT6 cap nozzle CR.REV", part(nozzle, 2 * U),
                    part(metalNozzleRing(nozzle) ? nozzle : Materials.Rubber, U / 4),
                    part(material, 2 * (U / 9)), part(metalRing ? material : Materials.Rubber, U / 4));
        }
        throw new IllegalArgumentException("Unknown attachment: " + shape);
    }

    private static boolean metalNozzleRing(GTMaterial material) {
        return material == Materials.Tungsten || material == Materials.TantalumHafniumCarbide
                || material == Materials.Adamantium;
    }

    /** Each field generator's source recipe: two double plates, four wires, one gem. */
    private static void fieldGenerators(List<MaterialComponent> parts, int tier, int count) {
        parts.add(part(BatteryBoxDefinitions.material(tier), 4L * count * U));
        long osmium = tier == 0 ? U / 2 : tier == 1 ? 2 * U : 4L * (tier - 1) * U;
        parts.add(part(ImportedMaterialData.Os, count * osmium));
        parts.add(part(tier < 2 ? Materials.EnderPearl : tier < 4 ? Materials.EnderEye : Materials.NetherStar,
                count * U));
    }

    /** Known source parts of one tiered compact sensor plus one signal emitter. */
    private static void signalDevices(List<MaterialComponent> parts, int tier) {
        GTMaterial[] wire = {ImportedMaterialData.Pb, ImportedMaterialData.Sn, ImportedMaterialData.Cu,
                ImportedMaterialData.Au, ImportedMaterialData.Al, ImportedMaterialData.Pt};
        parts.add(part(BatteryBoxDefinitions.material(tier), 6 * U));
        parts.add(part(wire[tier], 5 * U + U / 2));
        parts.add(part(Materials.Rubber, 2 * U));
        GTMaterial gem = tier <= 2 ? ImportedMaterialData.SiO2 : tier == 3 ? Materials.Emerald
                : tier == 4 ? Materials.EnderPearl : Materials.EnderEye;
        parts.add(part(gem, 2 * U));
    }

    private static Optional<ItemComposition> transport(String id) {
        return switch (id) {
            case "inventory_extender", "inventory_bridge" -> Optional.of(data("GT6 inventory extender CR.REV",
                    part(Materials.Steel, 8 * U), part(Materials.Electrum, 6 * U)));
            case "tank_extender", "tank_bridge" -> Optional.of(data("GT6 tank extender CR.REV",
                    part(Materials.Steel, 14 * U)));
            case "inventory_tank_extender", "inventory_tank_bridge" -> Optional.of(data("GT6 combined extender CR.REV",
                    part(Materials.Steel, 20 * U), part(Materials.Electrum, 6 * U)));
            case "universal_extender", "universal_bridge" -> Optional.of(data("GT6 universal extender known CR.REV inputs",
                    part(Materials.StainlessSteel, 41 * U + U / 4), part(Materials.Platinum, 6 * U)));
            case "filter_items" -> Optional.of(filter(8 * U, 6 * U));
            case "filter_fluids" -> Optional.of(filter(14 * U, 0));
            case "filter_items_fluids", "filter_oredict" -> Optional.of(filter(20 * U, 6 * U));
            default -> Optional.empty();
        };
    }

    private static ItemComposition filter(long steel, long electrum) {
        var parts = new ArrayList<MaterialComponent>();
        parts.add(part(Materials.SteelGalvanized, steel));
        if (electrum > 0) parts.add(part(Materials.Electrum, electrum));
        // Two source filter covers, each made from a blank aluminium cover and four zinc foils.
        parts.add(part(ImportedMaterialData.Al, 2 * (U + U / 9)));
        parts.add(part(ImportedMaterialData.Zn, 2 * U));
        return ReversibleCraftingData.perItem(parts, 1, "GT6 filter known CR.REV inputs");
    }

    private static ItemComposition battery(ChemicalBatterySpec spec) {
        int tier = spec.tier();
        int cells = tier + 1;
        int plates = new int[]{1, 2, 2, 1, 1}[tier];
        int cables = tier < 2 ? 1 : 2;
        GTMaterial[] cableMaterials = {ImportedMaterialData.Pb, ImportedMaterialData.Sn,
                ImportedMaterialData.Cu, ImportedMaterialData.Au, ImportedMaterialData.Al};
        var parts = new ArrayList<MaterialComponent>();
        parts.add(part(Materials.BatteryAlloy, plates * U));
        parts.add(part(cableMaterials[tier], cables * (U / 2)));
        parts.add(part(Materials.Rubber, cables * U));
        switch (spec.chemistry()) {
            case LEAD_ACID -> {
                parts.add(part(ImportedMaterialData.Pb, cells * U));
                parts.add(part(Materials.BatteryAlloy, cells * U));
                parts.add(part(ImportedMaterialData.H2SO4, 2 * cells * U));
            }
            case ALKALINE, NICKEL_CADMIUM -> {
                parts.add(part(Materials.BatteryAlloy, cells * U));
                parts.add(part(ImportedMaterialData.Al, cells * (U / 4)));
                parts.add(part(Materials.StainlessSteel, cells * U));
                parts.add(part(Materials.Plastic, cells * (U / 4)));
                parts.add(part(ImportedMaterialData.Fe, cells * (U / 2)));
                parts.add(part(spec.chemistry() == ChemicalBatterySpec.Chemistry.ALKALINE
                        ? ImportedMaterialData.C : Materials.Graphite,
                        cells * (spec.chemistry() == ChemicalBatterySpec.Chemistry.ALKALINE ? U : U / 2)));
                parts.add(part(ImportedMaterialData.KOH, cells * U));
                parts.add(part(spec.chemistry() == ChemicalBatterySpec.Chemistry.ALKALINE
                        ? ImportedMaterialData.Zn : ImportedMaterialData.Ni, cells * U));
                parts.add(part(spec.chemistry() == ChemicalBatterySpec.Chemistry.ALKALINE
                        ? ImportedMaterialData.MnO2 : ImportedMaterialData.Cd, cells * U));
                parts.add(part(ImportedMaterialData.H2O, cells * U));
            }
            case LITHIUM_COBALT, LITHIUM_MANGANESE -> {
                parts.add(part(Materials.BatteryAlloy, cells * U));
                parts.add(part(spec.chemistry() == ChemicalBatterySpec.Chemistry.LITHIUM_COBALT
                        ? ImportedMaterialData.Co : ImportedMaterialData.Mn, cells * (U / 2)));
                parts.add(part(ImportedMaterialData.Cr, cells * U));
                parts.add(part(Materials.Plastic, cells * (U / 2)));
                parts.add(part(Materials.Graphite, cells * (U / 2)));
                parts.add(part(ImportedMaterialData.LiClO4, 2 * cells * U));
                parts.add(part(spec.chemistry() == ChemicalBatterySpec.Chemistry.LITHIUM_COBALT
                        ? ImportedMaterialData.HCl : ImportedMaterialData.HF, 2 * cells * U));
            }
        }
        return ReversibleCraftingData.perItem(parts, 1, "GT6 battery known CR.REV inputs");
    }
}
