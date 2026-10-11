import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.mod.ModData;
import com.gregtech.gregtech.data.SourceBlockProperties;
import com.gregtech.gregtech.data.FluidCatalog;
import com.gregtech.gregtech.data.generated.GT6Materials;
import com.gregtech.gregtech.content.fluid.FluidDefinitions;
import com.gregtech.gregtech.content.machine.BasicMachineCatalog;
import java.util.Locale;
import java.util.Map;

/** Development-only identity export. Run with the compiled shared core, never ship in the mod. */
class LanguageIdentities {
    public static void main(String[] args) throws ReflectiveOperationException {
        ModData.bindPresence(id -> id.equals("minecraft") || id.equals("gregtech"));
        GTMaterialRegistry.setLogSink((warning, message) -> {});
        GTMaterialRegistry.init();
        FluidDefinitions.prepare();
        // Export the actual native key and shared fallback; the importer checks it independently against OP/LH.
        for (var prefix : com.gregtech.gregtech.api.prefix.PrefixRegistry.all()) {
            prefixTemplate("item.gregtech." + prefix.getRegistryName(), prefix.getName());
            if (prefix.hasEmptyAmmunitionForm())
                row("@empty-template.item.gregtech." + prefix.getRegistryName() + "_empty", prefix.getName(),
                        com.gregtech.gregtech.api.material.OriginalMaterialNameRules.name(prefix.getName(),
                                com.gregtech.gregtech.api.material.MaterialSentinels.Empty, m -> "Empty", false));
        }
        for (var prefix : com.gregtech.gregtech.api.prefix.BlockPrefixRegistry.all())
            prefixTemplate("block.gregtech." + prefix.getRegistryName(), prefix.getName());
        for (var field : com.gregtech.gregtech.data.ImportedMaterialData.class.getFields()) {
            if (field.getType() != GTMaterial.class) continue;
            var material = (GTMaterial) field.get(null);
            row("@symbol.MT." + field.getName(), material.getName(), "");
        }
        row("@symbol.MT.Wood", GT6Materials.Woods.Wood.getName(), "");
        row("@symbol.MT.WoodTreated", GT6Materials.Woods.WoodTreated.getName(), "");
        // Retained structural aliases have the same original material + texture family.
        // These are display-name aliases only; source.json records their compatibility scope.
        for (String binding : new String[]{"boiler_wall|18002", "turbine_wall|18022",
                "large_crucible_wall|18000", "cryo_distillation_wall|18102",
                "large_gas_turbine_wall|18022", "large_dynamo_wall|18040",
                "heat_exchanger_wall|18101", "bedrock_drill_wall|18103",
                "lightning_rod_wall|18004", "fusion_reactor_wall|18045"}) {
            String[] parts = binding.split("\\|");
            row("block.gregtech." + parts[0], "gt.multitileentity." + parts[1], "");
        }
        // All four legacy variants relay adjacent items + fluids (not energy or wireless).
        for (String variant : new String[]{"basic", "advanced", "elite", "wireless"})
            row("block.gregtech.extender_" + variant, "gt.multitileentity.30002", "");
        // Original tree holes share one item name across their empty/full textures.
        // These retained pillar aliases inherit only that visual-family identity;
        // this export does not claim the original resin-generation behavior.
        for (String binding : new String[]{"log_hole_maple|32761", "log_sap_maple|32761",
                "log_hole_rainbowood|32760", "log_sap_rainbowood|32760",
                "log_hole_rubber|32762", "log_resin_rubber|32762"}) {
            String[] parts = binding.split("\\|");
            row("block.gregtech." + parts[0], "gt.multitileentity." + parts[1], "");
        }
        for (var wire : com.gregtech.gregtech.content.energy.WireCatalog.specifications())
            row("block.gregtech." + com.gregtech.gregtech.content.energy.WireCatalog.registryId(wire),
                    "@wire." + wire.material().getName() + "." + (wire.insulated() ? "cable" : "wire")
                            + "." + wire.size(), "");
        // Loader_MultiTileEntities redstone wires use six literal IDs, not the electric helper.
        for (String binding : new String[]{"redalloy|27000", "signalum|27050", "lumium|27500"}) {
            String[] parts = binding.split("\\|");
            int id = Integer.parseInt(parts[1]);
            row("block.gregtech.wire_01_" + parts[0], "gt.multitileentity." + id, "");
            row("block.gregtech.cable_01_" + parts[0], "gt.multitileentity." + (id + 6), "");
        }
        // Source constructor metadata, kept separate from alphabetical texture enumeration.
        String[] rockOres = {"anthracite", "lignite", "salt", "rocksalt", "bauxite", "oil", "gypsum", "milkyquartz", "netherquartz"};
        for (int i = 0; i < rockOres.length; i++)
            row("block.gregtech.block_ore_" + rockOres[i], "gt.block.rockores." + i, "");
        var ores = com.gregtech.gregtech.block.SpecialOreDefinitions.vanilla();
        for (int i = 0; i < ores.size(); i++)
            row("block.gregtech.block_" + ores.get(i).icon(), "gt.block.vanillaores.a." + i, "");
        var icons = com.gregtech.gregtech.block.SpecialOreDefinitions.ORDERED_ICONS;
        for (int i = 0; i < 12; i++)
            row("block.gregtech." + icons.get(i), "gt.block.crystalores." + i, "");
        String[] laserTiers = {"lv", "mv", "hv", "ev", "iv"};
        for (int i = 0; i < laserTiers.length; i++) {
            row("block.gregtech.co2_laser_" + laserTiers[i], "gt.multitileentity." + (10101 + i), "");
            row("block.gregtech.flux_laser_" + laserTiers[i], "gt.multitileentity." + (11101 + i), "");
            row("block.gregtech.laser_absorber_" + laserTiers[i], "gt.multitileentity." + (10151 + i), "");
        }
        // GTLasers keeps the old elite registry alias for the same quantum decharger.
        for (String binding : new String[]{"zpm|14999", "zpm_discharger_basic|11170",
                "zpm_discharger_advanced|11171", "zpm_discharger_elite|11170",
                "ingot_pile|32084", "plate_pile|32085", "plate_gem_pile|32086",
                "coin_pile|32700", "sandwich_block|32105", "sensor_kilogibblometer|31023"}) {
            String[] parts = binding.split("\\|");
            row("block.gregtech." + parts[0], "gt.multitileentity." + parts[1], "");
        }
        for (var pipe : com.gregtech.gregtech.content.transport.fluid.FluidTransportDefinitions.pipes())
            row("block.gregtech." + pipe.id(), "@pipe.fluid." + pipe.material().getName()
                    + "." + pipe.size().name(), "");
        for (var mat : com.gregtech.gregtech.content.transport.ItemPipeCatalog.ITEM_PIPE_MATS)
            for (var size : com.gregtech.gregtech.api.machine.ItemPipeSpec.ItemPipeSize.values())
                row("block.gregtech.item_pipe_" + size.name().toLowerCase(Locale.ROOT) + "_" + mat.idSuffix(),
                        "@pipe.item." + mat.material().getName() + "." + size.name(), "");
        for (var p : com.gregtech.gregtech.content.tool.AutomaticToolRules.ALL)
            row("block.gregtech." + p.id(), "gt.multitileentity."
                    + ((p.igniter() ? 15010 : 15000) + p.quality()), "");
        for (var p : com.gregtech.gregtech.content.logistics.ExtenderSpec.values())
            row("block.gregtech." + p.id, "gt.multitileentity." + p.originalId, "");
        for (var p : com.gregtech.gregtech.content.transport.fluid.CheapWoodBarrelCatalog.ENTRIES)
            row("block.gregtech." + p.id(), "gt.multitileentity." + p.sourceId(), "");
        for (var p : com.gregtech.gregtech.api.fluid.PortableFluidContainerSpec.values())
            if (p.shapeId().equals("cell")) row("block.gregtech.fluid_" + p.id(),
                    "@cell." + p.material().getName(), "");
        for (var p : com.gregtech.gregtech.content.transport.TrackCatalog.MATERIALS)
            for (String variant : new String[]{"", "booster", "detector"})
                row("block.gregtech.track_" + (variant.isEmpty() ? "" : variant + "_") + p.slug(),
                        "gt.block.rail." + (variant.isEmpty() ? "" : variant + ".") + p.slug(), "");
        for (var stone : com.gregtech.gregtech.block.stone.StoneType.values())
            for (var variant : com.gregtech.gregtech.block.stone.StoneVariant.values()) {
                String key = "block.gregtech." + stone.registryId() + "_" + variant.registrySuffix();
                row(key, stone.textureFolder() + "." + variant.meta(), "");
                row(key + "_slab", stone.textureFolder() + ".slab.0." + variant.meta(), "");
            }
        // Original bush 32759 has one name; the berry/cotton/material is its output tooltip.
        for (var berry : com.gregtech.gregtech.content.plant.BerryBushCatalog.worldgenTypes())
            row("block.gregtech." + com.gregtech.gregtech.content.plant.BerryBushCatalog.blockPath(berry.id()),
                    "gt.multitileentity.32759", "");
        var colors = com.gregtech.gregtech.worldgen.SurfaceFloraRules.GLOWTUS_COLOURS;
        for (int i = 0; i < colors.size(); i++)
            row("block.gregtech.glowtus_" + colors.get(i), "gt.block.lilypad.glowtus." + i, "");
        for (var p : com.gregtech.gregtech.content.logistics.LongDistanceCatalog.ALL) {
            if (p.sourceLine()) row("block.gregtech." + p.id(),
                    (p.kind().equals("WIRE") ? "gt.block.longdistwire.01." : "gt.block.longdistpipe.01.")
                            + p.sourceMeta(), "");
            else if (p.kind().equals("TRANSFORMER")) {
                int tier = 4;
                for (long voltage = 2048; voltage < p.voltage(); voltage *= 4) tier++;
                row("block.gregtech." + p.id(), "gt.multitileentity." + (10060 + tier), "");
            }
        }
        row("block.gregtech.fluid_jug", "gt.multitileentity.32740", "");
        // Original chest 32745 enumerates ST.LOOT_TABLES as item variants using the lootchest shell.
        for (String chest : com.gregtech.gregtech.content.loot.LootChestCatalog.ENTRIES) {
            row("block.gregtech.loot_chest_" + chest.substring(0, chest.indexOf('|')),
                    "gt.multitileentity.32745", "");
            String label = com.gregtech.gregtech.content.storage.OriginalStorageTooltipData.lootKey(
                    chest.substring(chest.indexOf('|') + 1));
            if (label == null) throw new IllegalStateException("Unbound original loot label: " + chest);
            row(label, label, "");
        }
        row("item.gregtech.coin", "gt.multitileentity.32700", "");
        // RandomTools' original dye index and paired full/used metadata are retained by PaintingRules.
        for (var dye : com.gregtech.gregtech.content.tool.PaintingRules.DYES) {
            row("item.gregtech." + dye.fullId(), "gt.multiitem.randomtools." + (1000 + 2 * dye.index()), "");
            row("item.gregtech." + dye.usedId(), "gt.multiitem.randomtools." + (1001 + 2 * dye.index()), "");
        }
        for (int configuration = 0; configuration < 25; configuration++)
            row("item.gregtech.integrated_circuit_" + configuration, "gt.integrated_circuit", "");
        row("gt.integrated_circuit.configuration", "gt.integrated_circuit.configuration", "");
        row("block.gregtech.fluid_cup", "gt.multitileentity.32739", "");
        row("block.gregtech.fluid_measuring_pot", "gt.multitileentity.32738", "");
        row("block.gregtech.fluid_measuring_pot_stainless_steel", "gt.multitileentity.32743", "");
        row("block.gregtech.fluid_thermos", "gt.multitileentity.32737", "");
        row("block.gregtech.fluid_barometer_gas_cylinder", "gt.multitileentity.32055", "");
        row("block.gregtech.fluid_barometer_gas_cylinder_stainless_steel", "gt.multitileentity.32056", "");
        // Loader_MultiTileEntities' literal wood-barrel and metal-drum registrations.
        for (String binding : new String[]{"wood_barrel_treated|32714", "wood_barrel_skyroot|32019",
                "wood_barrel_livingwood|32010", "wood_barrel_dreamwood|32009", "wood_barrel_shimmerwood|32016",
                "wood_barrel_ironwood|32734", "wood_barrel_greatwood|32017", "wood_barrel_silverwood|32018",
                "drum_invar|32064", "drum_desh|32070", "drum_syrmorite|32020", "drum_efrine|32093",
                "drum_thaumium|32022", "drum_manasteel|32023", "drum_tungsten_alloy|32090", "drum_netherite|32087",
                "drum_void_metal|32063", "drum_tantalum_hafnium_carbide|32083", "drum_gaia_spirit|32024",
                "drum_adamantium|32719", "drum_draconium|32021", "drum_awakened_draconium|32066", "drum_infinity|32067"}) {
            String[] parts = binding.split("\\|");
            row("block.gregtech." + parts[0], "gt.multitileentity." + parts[1], "");
        }
        row("block.gregtech.asphalt", "gt.block.asphalt.0", "");
        // BlockColored names include the original dye, with metadata in the inverse of modern DyeColor order.
        for (String binding : new String[]{"asphalt|asphalt|0", "concrete|concrete|7",
                "concrete_reinforced|concrete.reinforced|7", "cfoam|cfoam|15",
                "cfoam_fresh|cfoam.fresh|15",
                "glass_clear|glass|7", "glass_glow|glass.glow|7",
                "glass_glow_slab|glass.glow.slab.0|15", "cfoam_slab|cfoam.slab.0|15"}) {
            String[] parts = binding.split("\\|");
            row("block.gregtech." + parts[0], "gt.block." + parts[1] + "." + parts[2], "");
            for (int i = 0; i < colors.size(); i++)
                row("block.gregtech." + parts[0] + "." + colors.get(i), "gt.block." + parts[1] + "." + i, "");
        }
        // Retained sprite-only legacy blocks have no color state. Do not invent colored items for them.
        row("block.gregtech.cfoam_fresh_owned", "gt.block.cfoam.fresh.15", "");
        row("block.gregtech.cfoam_hardened", "gt.block.cfoam.15", "");
        row("block.gregtech.cfoam_hardened_owned", "gt.block.cfoam.15", "");
        row("block.gregtech.railroad", "gt.block.rail.road", "");
        // Flattened metadata still belongs to the original block/variant, not its sprite name.
        String[] grassBales = {"grass", "grass_dry", "grass_moldy", "grass_rotten"};
        String[] cropBales = {"rye", "oat", "barley", "rice"};
        for (int i = 0; i < 4; i++) {
            row("block.gregtech.bale_" + grassBales[i], "gt.block.bale.grass." + i, "");
            row("block.gregtech.bale_" + cropBales[i], "gt.block.bale.crop." + i, "");
        }
        var sands = com.gregtech.gregtech.block.BlackSandDefinitions.IDS;
        for (int i = 0; i < sands.size(); i++)
            row("block.gregtech." + sands.get(i), "gt.block.sands." + i, "");
        for (String material : new String[]{"brass", "steel", "tungsten_steel"})
            row("block.gregtech.bars_" + material, "gt.block.bars." + material.replace("_", "") + ".0", "");
        for (String type : new String[]{"sharp", "steel", "super", "metal", "fancy"})
            row("block.gregtech.spike_" + type, "gt.block.spikes." + type + ".0", "");
        // DiggableBlock.Variant retains BlockDiggable's metadata order, including legacy aliases.
        String[] diggables = {"mud", "clay_brown", "turf", "clay_red", "clay_yellow", "clay_blue", "clay_white"};
        for (int i = 0; i < diggables.length; i++)
            row("block.gregtech." + diggables[i], "gt.block.diggable." + i, "");
        row("block.gregtech.diggable_clay", "gt.block.diggable.1", "");
        row("block.gregtech.diggable_peat", "gt.block.diggable.2", "");
        // Textures.BlockIcons.GRASSES_TOP order used by original BlockGrass.
        String[] grassColors = {"medium", "light", "dark", "normal", "yellow", "brown"};
        for (int i = 0; i < grassColors.length; i++)
            row("block.gregtech.grassblock_" + grassColors[i], "gt.block.grass." + i, "");
        for (String binding : new String[]{"filter_items|30256", "filter_fluids|30257",
                "filter_items_fluids|30258", "filter_oredict|30259", "sensor_kilogibblometer|31023",
                "crank|32111", "tap|32728", "tap_stainless_steel|32730", "nozzle|32746",
                "nozzle_stainless_steel|32748", "cap_nozzle|32058", "cap_nozzle_stainless_steel|32060",
                "sap_bag|32736", "plant_pot|32065", "coin_mold|32701", "reactor_core|9300", "reactor_core_2x2|9200"}) {
            String[] parts = binding.split("\\|");
            row("block.gregtech." + parts[0], "gt.multitileentity." + parts[1], "");
        }
        for (var tool : com.gregtech.gregtech.api.tool.ToolDefinition.values()) {
            row("item.gregtech.tool." + tool.id(), "gt.metatool.01." + tool.gt6Id(), "");
            row(tool.translationKey(), "gt.metatool.01." + tool.gt6Id(), "");
            // Builder Wand already uses its original LH behavior instruction; its numbered
            // item description is empty and must not erase that separate instruction.
            if (tool.tooltipKey() != null && tool != com.gregtech.gregtech.api.tool.ToolDefinition.BUILDER_WAND)
                row("tooltip.gregtech.tool_hint." + tool.tooltipKey(), "gt.metatool.01." + tool.gt6Id() + ".tooltip", "");
        }
        row("tooltip.gregtech.machine.harvest.pickaxe", "gt.metatool.01.2", "");
        row("tooltip.gregtech.machine.harvest_wrench_short", "gt.metatool.01.16", "");
        row("tooltip.gregtech.smeltery.product.block_solid", "oredict.prefix.blockSolid", "");
        for (var tool : com.gregtech.gregtech.content.tool.ElectricToolCatalog.ALL)
            row("item.gregtech." + tool.id(), "@tool." + tool.original(), "");
        String[] components = {"electric_motor", "electric_pump", "electric_conveyor", "electric_piston",
                "robot_arm", "force_field_emitter", "signal_emitter", "sensor"};
        String[] tiers = {"ulv", "lv", "mv", "hv", "ev", "iv", "luv", "zpm", "uv", "puv1"};
        // MultiItemTechnological's 0..9 loop. The port-only XV rows have no source number.
        for (int c = 0; c < components.length; c++) for (int tier = 0; tier < tiers.length; tier++)
            row("item.gregtech.compact_" + components[c] + "_" + tiers[tier],
                    "gt.multiitem.technological." + (12000 + c * 20 + tier), "");
        for (var battery : com.gregtech.gregtech.content.energy.ChemicalBatterySpec.all()) {
            int sourceId = 14000 + battery.chemistry().ordinal() * 10 + battery.tier();
            row("block.gregtech." + battery.id(), "gt.multitileentity." + sourceId, "");
            if (battery.chemistry() == com.gregtech.gregtech.content.energy.ChemicalBatterySpec.Chemistry.LITHIUM_COBALT)
                row("block.gregtech.battery_eu_" + battery.voltage(), "gt.multitileentity." + sourceId, "");
        }
        // Original A/B metadata order differs from WoodSpecies enum order.
        String[] treeSpecies = {"rubber", "maple", "willow", "blue_mahoe", "hazel", "cinnamon", "coconut", "rainbowood"};
        for (int i = 0; i < treeSpecies.length; i++) {
            for (String kind : new String[]{"sapling", "leaves", "planks"})
                row("block.gregtech." + kind + "_" + treeSpecies[i], "gt.block." + kind + "." + i, "");
            for (String kind : new String[]{"log", "beam"})
                row("block.gregtech." + kind + "_" + treeSpecies[i], "gt.block." + kind + "." + (i < 4 ? "a" : "b") + "." + (i % 4), "");
            String iconSpecies = treeSpecies[i].replace("_", "");
            row("block.gregtech.sapling_small_" + iconSpecies, "gt.block.sapling." + i, "");
            row("block.gregtech.sapling_large_" + iconSpecies, "gt.block.sapling." + (i + 8), "");
            // Fast/fancy leaves are rendering variants of the same original named species.
            row("block.gregtech.leaves_opaque_" + iconSpecies, "gt.block.leaves." + i, "");
        }
        row("block.gregtech.sapling_small_bluespruce", "gt.block.sapling.cd.0", "");
        row("block.gregtech.sapling_large_bluespruce", "gt.block.sapling.cd.8", "");
        row("block.gregtech.leaves_opaque_bluespruce", "gt.block.leaves.cd.0", "");
        row("block.gregtech.leaves_bluespruce_xmas", "gt.block.leaves.cd.0", "");
        row("block.gregtech.leaves_opaque_bluespruce_xmas", "gt.block.leaves.cd.0", "");
        for (String season : new String[]{"brown", "orange", "red", "yellow"}) {
            row("block.gregtech.leaves_maple_" + season, "gt.block.leaves.1", "");
            row("block.gregtech.leaves_opaque_maple_" + season, "gt.block.leaves.1", "");
        }
        for (String kind : new String[]{"sapling", "leaves", "planks", "log", "beam"}) {
            String suffix = switch(kind) { case "sapling", "leaves" -> ".cd"; case "planks" -> "2"; default -> ".c"; };
            row("block.gregtech." + kind + "_bluespruce", "gt.block." + kind + suffix + ".0", "");
            row("block.gregtech." + kind + "_bluemahoe", "gt.block." + kind + (kind.equals("log") || kind.equals("beam") ? ".a.3" : ".3"), "");
        }
        String[] fallen = {"dry", "rotten", "mossy", "frozen"};
        for (int i = 0; i < fallen.length; i++) {
            row("block.gregtech.log_" + fallen[i], "gt.block.log.1." + i, "");
            row("block.gregtech.planks_" + fallen[i], "gt.block.planks." + (12 + i), "");
        }
        row("block.gregtech.planks_compressed", "gt.block.planks.8", "");
        row("block.gregtech.planks_wood", "gt.block.planks.9", "");
        row("block.gregtech.planks_treated", "gt.block.planks.10", "");
        row("block.gregtech.slab_bluespruce", "gt.block.planks2.slab.0.0", "");
        for (var flower : com.gregtech.gregtech.content.plant.BedrockFlowers.ALL)
            row("block.gregtech." + flower.id(), "gt.block.flower."
                    + Character.toLowerCase(flower.family()) + "." + flower.meta(), "");
        String[][] beams = {{"oak", "spruce", "birch", "jungle"},
                {"acacia", "darkoak", "rubberwood", "wood"},
                {"greatwood", "silverwood", "skyroot", "darkwood"}};
        for (int group = 0; group < beams.length; group++) for (int meta = 0; meta < 4; meta++)
            row("block.gregtech.beam_" + beams[group][meta], "gt.block.beam." + (group + 1) + "." + meta, "");
        row("block.gregtech.fluid_measuring_pot_tungsten", "gt.multitileentity.32744", "");
        row("block.gregtech.fluid_measuring_pot_tantalum_hafnium_carbide", "gt.multitileentity.32077", "");
        row("block.gregtech.fluid_barometer_gas_cylinder_tungsten", "gt.multitileentity.32057", "");
        row("block.gregtech.fluid_barometer_gas_cylinder_tantalum_hafnium_carbide", "gt.multitileentity.32078", "");
        row("block.gregtech.fluid_funnel", "gt.multitileentity.32723", "");
        row("block.gregtech.fluid_funnel_stainless_steel", "gt.multitileentity.32725", "");
        row("block.gregtech.loot_crate", "gt.multitileentity.32110", "");
        row("block.gregtech.bumble_hive", "gt.multitileentity.32755", "");
        for (var color : colors)
            row("block.gregtech.bumble_hive_" + color, "gt.multitileentity.32755", "");
        for (var spring : com.gregtech.gregtech.worldgen.FluidSpringRules.SPRINGS)
            row("block.gregtech." + com.gregtech.gregtech.worldgen.FluidSpringRules.blockPath(spring),
                    "gt.multitileentity.32763", "");
        row("block.gregtech.bottle_crate", "gt.multitileentity.8762", "");
        // Every original plank index uses the same generic name. Modern woods
        // reuse this family name; 8700 is not a fabricated per-wood source ID.
        for (var wood : com.gregtech.gregtech.content.storage.BottleCrateVariants.WOODS)
            row("block.gregtech." + wood.id(), "gt.multitileentity.8700", "");
        SourceBlockProperties.blocks().forEach((path, p) -> row("block.gregtech." + path,
                "gt.multitileentity." + p.sourceId(), ""));
        for (var p : BasicMachineCatalog.specifications())
            SourceBlockProperties.basic(p.machineName(), p.tier()).ifPresent(s -> row(
                    "block.gregtech." + p.id(), "gt.multitileentity." + s.sourceId(), ""));
        for (var m : GTMaterialRegistry.allMaterials()) {
            row(m.getTranslationKey(), "gt.material." + m.getName(), m.getDisplayNameFallback());
            row("@material-proof." + m.getTranslationKey(), "gt.material." + m.getName(), Integer.toString(m.getId()));
        }
        for (var holder : GT6Materials.class.getDeclaredClasses()) for (var field : holder.getFields()) {
            if (field.getType() != GTMaterial.class) continue;
            var material = (GTMaterial) field.get(null);
            row("material.gregtech." + field.getName().toLowerCase(Locale.ROOT),
                    "gt.material." + material.getName(), material.getDisplayNameFallback());
            row("@material-proof.material.gregtech." + field.getName().toLowerCase(Locale.ROOT),
                    "gt.material." + material.getName(), Integer.toString(material.getId()));
        }
        // MultiItemBottles explicitly fills these fluids under the same content name (no container noun).
        var sourceFluidNames = Map.of("grccore.saltwater", "gt.multiitem.bottles.1",
                "tropicswater", "gt.multiitem.bottles.1", "stagnantwater", "gt.multiitem.bottles.7",
                // Both port IDs bind material 8637; Loader_Fluids uses a literal space for MT.HSLA.
                "molten.hsla", "fluid.molten hsla", "molten.hslasteel", "fluid.molten hsla");
        FluidCatalog.all().values().stream().distinct().forEach(f -> {
            String key = "fluid_type.gregtech." + FluidCatalog.sanitizePath(f.registryName());
            String legacyKey = "fluid_type.gregtech." + f.registryName();
            String original = sourceFluidNames.getOrDefault(f.registryName(), "fluid." + f.registryName());
            row(key, original, "");
            if (!legacyKey.equals(key)) row(legacyKey, original, "");
            String material = f.materialKey() == null ? FluidCatalog.boundMaterial(f.registryName()) : f.materialKey();
            if (material != null) {
                var mat = GTMaterialRegistry.get(material).resolve();
                row("@fluid-proof." + key, mat.getName(), Integer.toString(mat.getId()));
                if (!legacyKey.equals(key)) row("@fluid-proof." + legacyKey, mat.getName(), Integer.toString(mat.getId()));
            }
        });
        com.gregtech.gregtech.api.prefix.PrefixRegistry.ensurePrefixesLoaded();
        for (var p : com.gregtech.gregtech.api.prefix.PrefixRegistry.all()) if (!p.isHiddenFromCreative()) {
            row("item.gregtech.tab_icon_" + p.getRegistryName(), "oredict.prefix." + com.gregtech.gregtech.api.prefix.PrefixRegistry.sourceName(p.getName()), p.getDisplayName());
            row("itemGroup.gregtech." + p.getRegistryName(), "itemGroup." + com.gregtech.gregtech.api.prefix.PrefixRegistry.sourceName(p.getName()), p.getDisplayName());
        }
        com.gregtech.gregtech.api.prefix.BlockPrefixRegistry.ensurePrefixesLoaded();
        for (var p : com.gregtech.gregtech.api.prefix.BlockPrefixRegistry.all()) if (!p.isPartialCrate()) {
            row("item.gregtech.tab_icon_block_" + p.getRegistryName(), "oredict.prefix." + com.gregtech.gregtech.api.prefix.PrefixRegistry.sourceName(p.getName()), p.getDisplayName());
            row("itemGroup.gregtech." + p.getRegistryName(), "itemGroup." + com.gregtech.gregtech.api.prefix.PrefixRegistry.sourceName(p.getName()), p.getDisplayName());
        }
    }
    private static void prefixTemplate(String key, String prefix) {
        String fallback = com.gregtech.gregtech.api.material.OriginalMaterialNameRules.name(prefix,
                com.gregtech.gregtech.api.material.MaterialSentinels.Invalid, m -> "%s", false);
        row("@prefix-template." + key, com.gregtech.gregtech.api.prefix.PrefixRegistry.sourceName(prefix),
                fallback == null ? "" : fallback);
    }
    private static void row(String key, String original, String fallback) {
        if ((key + original + fallback).matches("(?s).*[\\t\\r\\n].*"))
            throw new IllegalStateException("Identity export cannot contain TSV controls");
        System.out.println(key + "\t" + original + "\t" + fallback);
    }
}
