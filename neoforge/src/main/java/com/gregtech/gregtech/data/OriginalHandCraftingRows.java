package com.gregtech.gregtech.data;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.gregtech.gregtech.api.machine.*;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.MaterialEquivalence;
import com.gregtech.gregtech.api.prefix.BlockMaterialPrefix;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.content.transport.fluid.FluidTransportRegistries;
import com.gregtech.gregtech.registry.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Original Forge registration-pattern hand recipes, emitted through the existing Native reloadable pack. */
public final class OriginalHandCraftingRows {
    private OriginalHandCraftingRows() {}
    public static void add(Map<ResourceLocation, byte[]> data) {
        int gearboxes = customGearboxes(data), transformers = transformerGearboxes(data);
        int engines = rotationEngines(data), pumps = rotationalPumps(data);
        int wire = wires(data), pipe = pipes(data), anvil = anvils(data), bar = bars(data);
        int circuit = circuits(data), cell = cells(data), store = storage(data);
        com.mojang.logging.LogUtils.getLogger().info("[gregtech] Original native hand crafting: gearboxes={} transformers={} rotation-engines={} pumps={} wire={} pipes={} anvils={} bars={} circuits={} cells={} storage={}",
                gearboxes, transformers, engines, pumps, wire, pipe, anvil, bar, circuit, cell, store);
    }
    private static java.util.List<net.neoforged.neoforge.registries.DeferredHolder<Block, com.gregtech.gregtech.block.machine.EngineBlock>> rotationEngines() {
        return GTEngines.all().stream().filter(holder -> holder.get().engineType() == EngineType.ROTATION).toList();
    }
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("gregtech", path); }

    private static int customGearboxes(Map<ResourceLocation, byte[]> recipes) {
        if (GTGearboxes.allGearboxes().size() != 13)
            throw new IllegalStateException("Expected 13 GT6 custom gearboxes; got "
                    + GTGearboxes.allGearboxes().size());
        int added = 0;
        for (var holder : GTGearboxes.allGearboxes()) {
            if (!holder.isBound()) throw new IllegalStateException("Unbound custom gearbox " + holder.getId());
            var spec = holder.get().spec();
            GTMaterial material = spec.material();
            String path = "gearbox/" + spec.id();
            ItemStack axle = requiredMediumAxle(material, path);
            boolean wooden = isTreatedWood(material);
            Ingredient lubricant = lubricant(earlyLubricant(material));
            Map<Character, Ingredient> key;
            String[] pattern;
            if (wooden) {
                // GT6 24809: "PsP" / "ALA" / "PAP", where s is a saw.
                ItemStack plate = requiredMaterial(MaterialPrefix.plate, material, path);
                pattern = new String[]{"PsP", "ALA", "PAP"};
                key = Map.of('P', Ingredient.of(plate), 'A', Ingredient.of(axle),
                        's', requiredTool(GTToolType.SAW), 'L', lubricant);
            } else {
                // GT6 24779-24899: "wAL" / "AMA", with a same-material machine casing.
                ItemStack casing = requiredBlock(BlockMaterialPrefix.casingMachine, material, path);
                pattern = new String[]{"wAL", "AMA"};
                key = Map.of('A', Ingredient.of(axle), 'M', Ingredient.of(casing),
                        'w', requiredTool(GTToolType.WRENCH), 'L', lubricant);
            }
            added += requiredAxle(recipes, path, pattern, key,
                    new ItemStack(holder.get().asItem()), false);
        }
        if (added != 13) throw new IllegalStateException("Expected 13 GT6 custom gearbox recipes; got " + added);
        return added;
    }

    private static int transformerGearboxes(Map<ResourceLocation, byte[]> recipes) {
        if (GTGearboxes.allTransformers().size() != 13)
            throw new IllegalStateException("Expected 13 GT6 transformer gearboxes; got "
                    + GTGearboxes.allTransformers().size());
        int added = 0;
        for (var holder : GTGearboxes.allTransformers()) {
            if (!holder.isBound()) throw new IllegalStateException("Unbound transformer gearbox " + holder.getId());
            var spec = holder.get().spec();
            GTMaterial material = spec.material();
            String path = "rotation_transformer/" + spec.id();
            ItemStack axle = requiredMediumAxle(material, path);
            ItemStack smallGear = requiredMaterial(MaterialPrefix.gearGtSmall, material, path);
            ItemStack gear = requiredMaterial(MaterialPrefix.gearGt, material, path);
            boolean wooden = isTreatedWood(material);
            Ingredient lubricant = lubricant(earlyLubricant(material));
            ItemStack hull = wooden
                    ? requiredMaterial(MaterialPrefix.plate, material, path)
                    : requiredBlock(BlockMaterialPrefix.casingMachineDouble, material, path);
            // GT6 24808 uses a treated-wood plate at P; 24778-24898 use a double casing at M.
            String[] pattern = wooden
                    ? new String[]{"ASL", "SGS", "PSA"}
                    : new String[]{"ASL", "SGS", "MSA"};
            Map<Character, Ingredient> key = Map.of(
                    'A', Ingredient.of(axle), 'S', Ingredient.of(smallGear),
                    'G', Ingredient.of(gear), wooden ? 'P' : 'M', Ingredient.of(hull),
                    'L', lubricant);
            added += requiredAxle(recipes, path, pattern, key,
                    new ItemStack(holder.get().asItem()), false);
        }
        if (added != 13) throw new IllegalStateException("Expected 13 GT6 transformer gearbox recipes; got " + added);
        return added;
    }

    private static boolean isTreatedWood(GTMaterial material) {
        return material == com.gregtech.gregtech.content.material.generated.WoodMaterials.WoodTreated;
    }

    private static boolean earlyLubricant(GTMaterial material) {
        return isTreatedWood(material) || material == Materials.Bronze || material == Materials.Brass
                || material == Materials.ArsenicCopper || material == Materials.ArsenicBronze;
    }

    // ── GT6's 13 RU → KU engines and four RU rotational pumps ──────────────

    private static int rotationEngines(Map<ResourceLocation, byte[]> recipes) {
        if (rotationEngines().size() != 13)
            throw new IllegalStateException("Expected 13 GT6 rotation engines; got "
                    + rotationEngines().size());
        int added = 0;
        for (var holder : rotationEngines()) {
            if (!holder.isBound()) throw new IllegalStateException("Unbound rotation engine " + holder.getId());
            RotationEngineSpec spec = holder.get().engineSpec(RotationEngineSpec.class);
            GTMaterial material = spec.material();
            boolean wooden = isTreatedWood(material);
            boolean early = earlyLubricant(material);
            String path = "engine_rotation/" + spec.id();
            ItemStack smallGear = requiredMaterial(MaterialPrefix.gearGtSmall, material, path);
            ItemStack gear = requiredMaterial(MaterialPrefix.gearGt, material, path);
            ItemStack axle = requiredMediumAxle(material, path);
            Ingredient lubricant = lubricant(early);
            Ingredient wrench = requiredTool(GTToolType.WRENCH);
            Map<Character, Ingredient> key;
            String[] pattern;
            if (wooden) {
                // GT6: "PSP" / "wAL" / "GAG" (treated wood plates, not a metal casing).
                ItemStack plate = requiredMaterial(MaterialPrefix.plate, material, path);
                pattern = new String[]{"PSP", "wAL", "GAG"};
                key = Map.of('P', Ingredient.of(plate), 'S', Ingredient.of(smallGear),
                        'G', Ingredient.of(gear), 'A', Ingredient.of(axle),
                        'w', wrench, 'L', lubricant);
            } else {
                // GT6: "SAS" / "wML" / "GAG", with the same material's machine casing.
                ItemStack casing = requiredBlock(BlockMaterialPrefix.casingMachine, material, path);
                pattern = new String[]{"SAS", "wML", "GAG"};
                key = Map.of('M', Ingredient.of(casing), 'S', Ingredient.of(smallGear),
                        'G', Ingredient.of(gear), 'A', Ingredient.of(axle),
                        'w', wrench, 'L', lubricant);
            }
            added += requiredAxle(recipes, path, pattern, key, new ItemStack(holder.get().asItem()), false);
        }
        if (added != 13) throw new IllegalStateException("Expected 13 GT6 rotation engine recipes; got " + added);
        return added;
    }

    private static int rotationalPumps(Map<ResourceLocation, byte[]> recipes) {
        if (GTPumps.all().size() != 4)
            throw new IllegalStateException("Expected four GT6 rotational pumps; got " + GTPumps.all().size());
        int added = 0;
        for (var holder : GTPumps.all()) {
            if (!holder.isBound()) throw new IllegalStateException("Unbound rotational pump " + holder.getId());
            var spec = holder.get().spec();
            String path = "rotational_pump/" + spec.id();
            PipeSpec.PipeSize pipeSize = switch (spec.tier()) {
                case 1 -> PipeSpec.PipeSize.SMALL;
                case 2 -> PipeSpec.PipeSize.MEDIUM;
                case 3 -> PipeSpec.PipeSize.LARGE;
                case 4 -> PipeSpec.PipeSize.HUGE;
                default -> throw new IllegalStateException("Unknown GT6 rotational pump tier " + spec.tier());
            };
            ItemStack casing = requiredBlock(BlockMaterialPrefix.casingMachineDouble, spec.material(), path);
            ItemStack gear = requiredMaterial(MaterialPrefix.gearGt, spec.material(), path);
            ItemStack rotor = requiredMaterial(MaterialPrefix.rotor, Materials.StainlessSteel, path);
            ItemStack pipe = requiredStainlessPipe(pipeSize, path);
            added += requiredAxle(recipes, path, new String[]{"GwG", "PMP", "RPR"},
                    Map.of('G', Ingredient.of(gear), 'M', Ingredient.of(casing),
                            'R', Ingredient.of(rotor), 'P', Ingredient.of(pipe),
                            'w', requiredTool(GTToolType.WRENCH)),
                    new ItemStack(holder.get().asItem()), false);
        }
        if (added != 4) throw new IllegalStateException("Expected four GT6 rotational pump recipes; got " + added);
        return added;
    }

    private static ItemStack requiredMaterial(MaterialPrefix form, GTMaterial material, String recipe) {
        ItemStack stack = GTItems.getStack(form, material, 1);
        if (stack.isEmpty()) throw new IllegalStateException(recipe + " lacks GT6 " + form.getName()
                + " of " + material.getName());
        return stack;
    }

    private static ItemStack requiredBlock(BlockMaterialPrefix form, GTMaterial material, String recipe) {
        ItemStack stack = GTBlocks.getStack(form, material);
        if (stack.isEmpty()) throw new IllegalStateException(recipe + " lacks GT6 " + form.getName()
                + " of " + material.getName());
        return stack;
    }

    private static ItemStack requiredMediumAxle(GTMaterial material, String recipe) {
        for (var holder : GTAxles.all()) {
            if (holder.isBound() && holder.get().spec().size() == 2
                    && holder.get().spec().material() == material) return new ItemStack(holder.get().asItem());
        }
        throw new IllegalStateException(recipe + " lacks its GT6 medium axle of " + material.getName());
    }

    private static ItemStack requiredStainlessPipe(PipeSpec.PipeSize size, String recipe) {
        for (var holder : FluidTransportRegistries.pipes()) {
            if (holder.isBound() && holder.get().spec().size() == size
                    && holder.get().spec().material() == Materials.StainlessSteel)
                return new ItemStack(holder.get().asItem());
        }
        throw new IllegalStateException(recipe + " lacks GT6 " + size + " StainlessSteel fluid pipe");
    }

    private static Ingredient lubricant(boolean early) {
        // GT6 LoaderOreDictReRegistrations:978-989: itemLubricant is filled lubricant,
        // itemLubricantEarly additionally accepts eight seed/vegetable/animal oil bottles.
        // BottleItem returns the GT6 empty bottle from getCraftingRemainingItem.
        String[] names = early
                ? new String[]{"lubricant_bottle", "olive_oil", "sunflower_oil", "nut_oil",
                    "seed_oil", "hemp_oil", "lin_oil", "fish_oil", "whale_oil"}
                : new String[]{"lubricant_bottle"};
        ItemStack[] bottles = new ItemStack[names.length];
        for (int i = 0; i < names.length; i++) {
            Item item = BuiltInRegistries.ITEM.get(id(names[i]));
            if (item == null || item == Items.AIR)
                throw new IllegalStateException("Missing GT6 itemLubricant bottle " + names[i]);
            bottles[i] = new ItemStack(item);
        }
        return Ingredient.of(bottles);
    }

    private static int wires(Map<ResourceLocation, byte[]> recipes) {
        Map<String, GTMaterial> families = new HashMap<>();
        for (var holder : GTWires.allWires()) {
            if (!holder.isBound()) continue;
            var spec = holder.get().spec();
            families.putIfAbsent(spec.id(), spec.material());
        }
        int added = 0;
        for (var family : families.entrySet()) {
            ItemStack plate = plate(family.getValue());
            ItemStack cutter = tool(GTToolType.WIRE_CUTTER);
            if (plate.isEmpty() || cutter.isEmpty()) continue;
            added += register(recipes, "wire/" + family.getKey(), new String[]{"Px"},
                    Map.of('P', Ingredient.of(plate), 'x', Ingredient.of(cutter)),
                    wireItem(family.getKey(), 1));
        }
        return added;
    }

    // ── pipes: the aRecipe patterns of the pipe helpers ─────────────────────

    private static int pipes(Map<ResourceLocation, byte[]> recipes) {
        int added = 0;
        for (var holder : FluidTransportRegistries.pipes()) {
            if (!holder.isBound()) continue;
            PipeSpec spec = holder.get().spec();
            GTMaterial material = spec.material();
            ItemStack output = new ItemStack(holder.get().asItem(),
                    spec.size() == PipeSpec.PipeSize.TINY ? 2 : 1);
            added += pipe(recipes, "fluid/" + spec.id(), spec.size(), material, output);
        }
        for (var holder : GTItemPipes.all()) {
            if (!holder.isBound()) continue;
            ItemPipeSpec spec = holder.get().spec();
            ItemStack output = new ItemStack(holder.get().asItem());
            if (spec.size().restrictive()) {
                ItemStack plain = plainPipe(spec.material(), spec.size());
                if (plain.isEmpty()) continue;
                String[] pattern = switch (spec.size()) {
                    case RESTRICTIVE_MEDIUM -> new String[]{" h ", "RPR", " R "};
                    case RESTRICTIVE_LARGE -> new String[]{"hR ", "RPR", " R "};
                    default -> new String[]{" h ", "RPR", "RRR"};
                };
                added += register(recipes, "pipe/restrictive/" + spec.id(), pattern,
                        Map.of('P', Ingredient.of(plain), 'R', Ingredient.of(steelRing()),
                                'h', Ingredient.of(tool(GTToolType.HARD_HAMMER))),
                        output);
                continue;
            }
            PipeSpec.PipeSize size = switch (spec.size()) {
                case MEDIUM -> PipeSpec.PipeSize.MEDIUM;
                case LARGE -> PipeSpec.PipeSize.LARGE;
                default -> PipeSpec.PipeSize.HUGE;
            };
            added += pipe(recipes, "item/" + spec.id(), size, spec.material(), output);
        }
        return added;
    }

    /** GT6's five pipe patterns; huge pipes use a double plate instead of a curved one. */
    private static int pipe(Map<ResourceLocation, byte[]> recipes, String path, PipeSpec.PipeSize size,
                            GTMaterial material, ItemStack output) {
        boolean huge = size == PipeSpec.PipeSize.HUGE;
        ItemStack plate = GTItems.getStack(huge ? MaterialPrefix.plateDouble : MaterialPrefix.plateCurved,
                material, 1);
        if (plate.isEmpty() || output.isEmpty()) return 0;
        String[] pattern = switch (size) {
            case TINY -> new String[]{"sP ", "wzh"};
            case SMALL -> new String[]{" P ", "wzh"};
            case MEDIUM -> new String[]{"PPP", "wzh"};
            default -> new String[]{"PPP", "wzh", "PPP"};
        };
        Map<Character, Ingredient> key = Map.of(
                'P', Ingredient.of(plate),
                's', Ingredient.of(tool(GTToolType.SAW)),
                'w', Ingredient.of(tool(GTToolType.WRENCH)),
                'z', Ingredient.of(tool(GTToolType.BENDING_CYLINDER)),
                'h', Ingredient.of(tool(GTToolType.HARD_HAMMER)));
        return register(recipes, "pipe/" + path, pattern, key, output);
    }

    // ── anvils: "RRR" / "hR " / "RRR" ──────────────────────────────────────

    private static int anvils(Map<ResourceLocation, byte[]> recipes) {
        int added = 0;
        for (var holder : GTManualStations.ANVILS) {
            if (!holder.isBound()) continue;
            var block = holder.get();
            GTMaterial material = block.material();
            ItemStack ring = anvilIngredient(material);
            if (ring.isEmpty()) continue;
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            added += register(recipes, "anvil/" + (id == null ? material.getName() : id.getPath()),
                    new String[]{"RRR", "hR ", "RRR"},
                    Map.of('R', Ingredient.of(ring),
                            'h', Ingredient.of(tool(GTToolType.HARD_HAMMER))),
                    new ItemStack(block.asItem()));
        }
        return added;
    }

    /** Stone anvils use vanilla stone or their own stone block, metal anvils use ingots. */
    private static ItemStack anvilIngredient(GTMaterial material) {
        String name = material.getName();
        if (name.equals("Stone")) return new ItemStack(Items.STONE);
        // the port's stone blocks are "stone_<type>_stone" (Loader_Blocks.registerStoneBlocks)
        String type = MaterialEquivalence.materialName(material);
        for (String candidate : new String[]{type, type.replace("_", "")}) {
            Item item = BuiltInRegistries.ITEM.get(
                    ResourceLocation.fromNamespaceAndPath("gregtech", "stone_" + candidate + "_stone"));
            if (item != null && item != Items.AIR) return new ItemStack(item);
        }
        return GTItems.getStack(MaterialPrefix.ingot, material, 1);
    }

    // ── BlockBaseBars: six rods and two reusable hand tools -> three segments ──
    private static int bars(Map<ResourceLocation, byte[]> recipes) {
        int added = 0;
        Object[][] entries = {
                {"iron", Materials.Iron, GTBars.get("bars_iron")},
                {"steel", Materials.Steel, GTBars.get("bars_steel")},
                {"brass", Materials.Brass, GTBars.get("bars_brass")},
                {"bronze", Materials.Bronze, GTBars.get("bars_bronze")},
                {"wrought_iron", Materials.WroughtIron, GTBars.get("bars_wrought_iron")},
                {"stainless", Materials.StainlessSteel, GTBars.get("bars_stainless")},
                {"tungsten_steel", Materials.Tungstensteel, GTBars.get("bars_tungsten_steel")},
        };
        for (Object[] entry : entries) {
            ItemStack rod = GTItems.getStack(MaterialPrefix.stick, (GTMaterial) entry[1], 1);
            if (rod.isEmpty()) continue;
            added += register(recipes, "bars/" + entry[0], new String[]{"BBB", "h w", "BBB"},
                    Map.of('B', Ingredient.of(rod),
                            'h', Ingredient.of(tool(GTToolType.HARD_HAMMER)),
                            'w', Ingredient.of(tool(GTToolType.WRENCH))),
                    new ItemStack((Block) entry[2], 3), false, true);
        }
        return added;
    }

    private static ItemStack plainPipe(GTMaterial material, ItemPipeSpec.ItemPipeSize size) {
        PipeSpec.PipeSize wanted = switch (size) {
            case RESTRICTIVE_MEDIUM -> PipeSpec.PipeSize.MEDIUM;
            case RESTRICTIVE_LARGE -> PipeSpec.PipeSize.LARGE;
            default -> PipeSpec.PipeSize.HUGE;
        };
        for (var holder : FluidTransportRegistries.pipes()) {
            if (!holder.isBound()) continue;
            var spec = holder.get().spec();
            if (spec.material() == material && spec.size() == wanted) {
                return new ItemStack(holder.get().asItem());
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack steelRing() {
        ItemStack ring = GTItems.getStack(MaterialPrefix.ring, Materials.Steel, 1);
        if (!ring.isEmpty()) return ring;
        Item item = BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("forge", "steel_ingot"));
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static ItemStack plate(GTMaterial material) {
        return GTItems.getStack(MaterialPrefix.plate, material, 1);
    }

    private static ItemStack tagOrPlate(String group, GTMaterial material, MaterialPrefix prefix) {
        ItemStack direct = GTItems.getStack(prefix, material, 1);
        return direct;
    }

    private static ItemStack wireItem(String family, int size) {
        for (var holder : GTWires.allWires()) {
            if (!holder.isBound()) continue;
            var spec = holder.get().spec();
            if (spec.id().equals(family) && spec.size() == size) return new ItemStack(holder.get().asItem());
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack tool(GTToolType type) {
        var item = GTToolItems.get(type);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static Ingredient requiredTool(GTToolType type) {
        ItemStack stack = tool(type);
        if (stack.isEmpty()) throw new IllegalStateException("Missing axle crafting tool " + type.id());
        return Ingredient.of(stack);
    }

    private static int circuits(Map<ResourceLocation, byte[]> recipes) {
        ItemStack circuit = tech("circuit_ultimate");
        ItemStack plate = tech("circuit_plate_platinum");
        ItemStack laser = tech("laser_emitter_heliumneon");
        if (circuit.isEmpty() || plate.isEmpty() || laser.isEmpty()) return 0;
        return register(recipes, "circuit/crystal_processor_socket",
                new String[]{"CLC", "LBL", "CLC"},
                Map.of('C', Ingredient.of(circuit), 'L', Ingredient.of(laser), 'B', Ingredient.of(plate)),
                tech("crystal_processor_socket"));
    }

    private static ItemStack tech(String id) {
        var item = com.gregtech.gregtech.registry.GTTechnological.get(id);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    // ── battery cells (MultiItemTechnological:464-484) ─────────────────────

    /**
     * The five empty battery cells; the filled ones come from the Canning Machine
     * ({@code :396-403} style rows, registered in {@code ElectronicsRecipes}). GT6's {@code 'x'} keys are
     * wire cutters and {@code 'h'} hard hammers; the alkaline/NiCd cells use an iron wire, which the port
     * registers as its fine wire form.
     */
    private static int cells(Map<ResourceLocation, byte[]> recipes) {
        int added = 0;
        ItemStack alloy = GTItems.getStack(MaterialPrefix.plateCurved, Materials.BatteryAlloy, 1);
        if (alloy.isEmpty()) return 0;
        added += register(recipes, "cell/lead_acid",
                new String[]{" Fh", "FPF", "xF "},
                Map.of('P', Ingredient.of(alloy), 'F', Ingredient.of(plate("Lead")),
                        'h', Ingredient.of(tool(GTToolType.HARD_HAMMER)),
                        'x', Ingredient.of(tool(GTToolType.WIRE_CUTTER))),
                tech("lead_acid_cell_empty"));

        ItemStack stainless = GTItems.getStack(MaterialPrefix.plateCurved, Materials.StainlessSteel, 1);
        Ingredient plasticRing = tagIngredient("forge:rings/plastic",
                GTItems.getStack(MaterialPrefix.ring, Materials.Plastic, 1));
        ItemStack ironWire = GTItems.getStack(MaterialPrefix.wireFine, Materials.Iron, 1);
        if (!stainless.isEmpty() && !ironWire.isEmpty()) {
            added += register(recipes, "cell/alkaline",
                    new String[]{"KSM", "OPF", "CWZ"},
                    Map.of('P', Ingredient.of(alloy), 'F', Ingredient.of(plate("Aluminium")),
                            'S', Ingredient.of(stainless), 'O', plasticRing,
                            'W', Ingredient.of(ironWire), 'C', Ingredient.of(dust("Carbon")),
                            'K', Ingredient.of(dust("KOH")), 'Z', Ingredient.of(dust("Zinc")),
                            'M', Ingredient.of(dust("MnO2"))),
                    tech("alkaline_button_cell_empty"));
            added += register(recipes, "cell/nickel_cadmium",
                    new String[]{"KSM", "OPF", "CWZ"},
                    Map.of('P', Ingredient.of(alloy), 'F', Ingredient.of(plate("Aluminium")),
                            'S', Ingredient.of(stainless), 'O', plasticRing,
                            'W', Ingredient.of(ironWire), 'C', Ingredient.of(stick("Graphite")),
                            'K', Ingredient.of(dust("KOH")), 'Z', Ingredient.of(curved("Nickel")),
                            'M', Ingredient.of(curved("Cadmium"))),
                    tech("nickel_cadmium_cell_empty"));
        }
        ItemStack plasticFoil = plate("Plastic");
        ItemStack lithium = dust("LiClO4");
        ItemStack chromium = curved("Chromium");
        if (!plasticFoil.isEmpty() && !lithium.isEmpty() && !chromium.isEmpty()) {
            added += register(recipes, "cell/lithium_cobalt",
                    new String[]{"CLF", "XSG", "FLP"},
                    Map.of('P', Ingredient.of(alloy), 'X', Ingredient.of(stick("Cobalt")),
                            'G', Ingredient.of(stick("Graphite")), 'L', Ingredient.of(lithium),
                            'S', Ingredient.of(chromium), 'F', Ingredient.of(plate("Plastic")),
                            'C', Ingredient.of(tech("circuit_elite"))),
                    tech("lithium_cobalt_cell_empty"));
            added += register(recipes, "cell/lithium_manganese",
                    new String[]{"CLF", "XSG", "FLP"},
                    Map.of('P', Ingredient.of(alloy), 'X', Ingredient.of(stick("Manganese")),
                            'G', Ingredient.of(stick("Graphite")), 'L', Ingredient.of(lithium),
                            'S', Ingredient.of(chromium), 'F', Ingredient.of(plate("Plastic")),
                            'C', Ingredient.of(tech("circuit_ultimate"))),
                    tech("lithium_manganese_cell_empty"));
        }
        return added;
    }

    private static Ingredient tagIngredient(String tag, ItemStack fallback) {
        if (!fallback.isEmpty()) return Ingredient.of(fallback);
        return Ingredient.of(ItemTags.create(ResourceLocation.parse(tag)));
    }

    private static ItemStack plate(String material) {
        return materialItem(MaterialPrefix.plate, material);
    }

    private static ItemStack curved(String material) {
        return materialItem(MaterialPrefix.plateCurved, material);
    }

    private static ItemStack stick(String material) {
        return materialItem(MaterialPrefix.stick, material);
    }

    private static ItemStack dust(String material) {
        return materialItem(MaterialPrefix.dust, material);
    }

    private static ItemStack materialItem(MaterialPrefix prefix, String material) {
        var resolved = com.gregtech.gregtech.api.material.GTMaterialRegistry.get(material);
        return resolved == null ? ItemStack.EMPTY : GTItems.getStack(prefix, resolved, 1);
    }


    private static int storage(Map<ResourceLocation, byte[]> recipes) {
        int added = 0;
        // GT6 Loader_MultiTileEntities:184, treated-plank bottle crate 8762.
        var woodBolts=GTItems.creativeEntries(MaterialPrefix.bolt).stream()
                .filter(entry->entry.material().has(com.gregtech.gregtech.api.material.MaterialProperty.WOOD))
                .map(entry->new ItemStack(entry.item().get())).toArray(ItemStack[]::new);
        added += register(recipes,"storage/bottle_crate",new String[]{"sfr","PGP","BPB"},
                Map.of('s',Ingredient.of(tool(GTToolType.SAW)), 'f',Ingredient.of(tool(GTToolType.FILE)),
                        'r',Ingredient.of(tool(GTToolType.SOFT_HAMMER)),
                        'P',Ingredient.of(BuiltInRegistries.ITEM.get(id("planks_treated"))),
                        'G',Ingredient.of(ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge","glue"))),
                        'B',Ingredient.of(woodBolts)),new ItemStack(GTStorageContainers.BOTTLE_CRATE.get()));
        for (GTStorageMetals.Spec spec : GTStorageMetals.ALL) {
            GTMaterial material = spec.material();
            ItemStack plate = tagOrPlate("plates", material, MaterialPrefix.plate);
            ItemStack ring = tagOrPlate("rings", material, MaterialPrefix.ring);
            ItemStack stick = tagOrPlate("rods", material, MaterialPrefix.stick);
            ItemStack screw = tagOrPlate("screws", material, MaterialPrefix.screw);
            ItemStack casing = GTBlocks.getStack(BlockMaterialPrefix.casingMachine, material);
            ItemStack chest = metalChest(spec);
            ItemStack storage = massStorage(spec);
            if(!screw.isEmpty()){
                ItemStack cable=GTWires.allCables().stream().map(net.neoforged.neoforge.registries.DeferredHolder::get)
                        .filter(wire->wire.spec().material()==Materials.Gold&&wire.spec().size()==4).findFirst().map(block->new ItemStack(block)).orElseThrow();
                added += register(recipes,"storage/charging_crafting_table_"+spec.suffix(),new String[]{"WTW","dMx","WTW"},
                        Map.of('W',Ingredient.of(cable),'T',Ingredient.of(screw),'M',Ingredient.of(new ItemStack(advancedCraftingTable(material))),
                                'd',Ingredient.of(tool(GTToolType.SCREWDRIVER)),'x',Ingredient.of(tool(GTToolType.WIRE_CUTTER))),
                        new ItemStack(chargingCraftingTable(material)));
            }
            if(!plate.isEmpty()&&!screw.isEmpty()){
                added += register(recipes,"storage/advanced_crafting_table_"+spec.suffix(),new String[]{"PdP","TWT","PCP"},
                        Map.of('P',Ingredient.of(plate),'T',Ingredient.of(screw),'W',Ingredient.of(Items.CRAFTING_TABLE),
                                'C',Ingredient.of(net.neoforged.neoforge.common.Tags.Items.CHESTS),'d',Ingredient.of(tool(GTToolType.SCREWDRIVER))),
                        new ItemStack(advancedCraftingTable(material)));
            }
            if (!screw.isEmpty() && !chest.isEmpty()) {
                added += register(recipes, "storage/drawer_quad_" + spec.suffix(),
                        new String[]{"CTC", "TdT", "CTC"},
                        Map.of('C', Ingredient.of(chest), 'T', Ingredient.of(screw),
                                'd', Ingredient.of(tool(GTToolType.SCREWDRIVER))), new ItemStack(drawer(material)));
            }
            ItemStack thickPlate = GTItems.getStack(MaterialPrefix.plateQuintuple, material, 1);
            ItemStack smallGear = GTItems.getStack(MaterialPrefix.gearGtSmall, material, 1);
            ItemStack gear = GTItems.getStack(MaterialPrefix.gearGt, material, 1);
            if (!thickPlate.isEmpty() && !smallGear.isEmpty() && !gear.isEmpty() && !stick.isEmpty()) {
                for (boolean keyed : new boolean[]{false, true}) {
                    added += register(recipes, "storage/" + (keyed ? "key_safe_" : "safe_") + spec.suffix(),
                            new String[]{"PGP", keyed ? "OGS" : "GOS", "PGP"},
                            Map.of('P', Ingredient.of(thickPlate), 'G', Ingredient.of(smallGear), 'O', Ingredient.of(gear), 'S', Ingredient.of(stick)),
                            new ItemStack(GTSafes.safe(material, keyed)));
                }
            }
            if (!ring.isEmpty() && !stick.isEmpty()) {
                // GT6 Loader_MultiTileEntities:133, reinforced wooden chest.
                added += register(recipes, "storage/reinforced_wood_chest_" + spec.suffix(),
                        new String[]{"sSw", "RCR", "SSS"},
                        Map.of('S', Ingredient.of(stick), 'R', Ingredient.of(ring),
                                'C', Ingredient.of(net.neoforged.neoforge.common.Tags.Items.CHESTS_WOODEN),
                                's', Ingredient.of(tool(GTToolType.SAW)), 'w', Ingredient.of(tool(GTToolType.WRENCH))),
                        new ItemStack(GTMetalChests.chest(material, true)));
            }
            if (!plate.isEmpty() && !ring.isEmpty() && !stick.isEmpty() && !chest.isEmpty()) {
                // Loader_MultiTileEntities:132 - "sPw" / "RSR" / "PPP"
                added += register(recipes, "storage/chest_" + spec.suffix(),
                        new String[]{"sPw", "RSR", "PPP"},
                        Map.of('P', Ingredient.of(plate), 'R', Ingredient.of(ring), 'S', Ingredient.of(stick),
                                's', Ingredient.of(tool(GTToolType.SAW)),
                                'w', Ingredient.of(tool(GTToolType.WRENCH))),
                        chest.copy());
            }
            if (!screw.isEmpty() && !chest.isEmpty() && !casing.isEmpty() && !storage.isEmpty()) {
                // Loader_MultiTileEntities:141 - "TCT" / "wMd" / "TCT"
                added += register(recipes, "storage/mass_storage_" + spec.suffix(),
                        new String[]{"TCT", "wMd", "TCT"},
                        Map.of('T', Ingredient.of(screw), 'C', Ingredient.of(chest), 'M', Ingredient.of(casing),
                                'w', Ingredient.of(tool(GTToolType.WRENCH)),
                                'd', Ingredient.of(tool(GTToolType.SCREWDRIVER))),
                        storage.copy());
            }
            ItemStack logisticsStorage = logisticsMassStorage(spec);
            ItemStack genericBus = tech("generic_logistics_storage_bus");
            if (!screw.isEmpty() && !storage.isEmpty() && !genericBus.isEmpty()
                    && !logisticsStorage.isEmpty()) {
                // Loader_MultiTileEntities:142 - GT6 6200+aID, including OD_CIRCUITS[4].
                // Tier-four-or-higher circuits are exposed through a tag so other mods may opt in.
                added += register(recipes, "storage/logistics_mass_storage_" + spec.suffix(),
                        new String[]{"TQT", "wCd", "TMT"},
                        Map.of('T', Ingredient.of(screw), 'Q', Ingredient.of(genericBus),
                                'C', Ingredient.of(ItemTags.create(id("circuits_tier_4_plus"))),
                                'M', Ingredient.of(storage),
                                'w', Ingredient.of(tool(GTToolType.WRENCH)),
                                'd', Ingredient.of(tool(GTToolType.SCREWDRIVER))),
                        logisticsStorage);
            }
        }
        // Loader_MultiTileEntities:140/138 - the drawer and locker use a metal chest as their core
        ItemStack anyChest = GTMetalChests.METAL.isEmpty() ? ItemStack.EMPTY
                : metalChest(GTStorageMetals.ALL.get(Math.min(1, GTStorageMetals.ALL.size() - 1)));
        if (GTStorageContainers.LOCKER != null && !anyChest.isEmpty()) {
            ItemStack screw = GTItems.getStack(MaterialPrefix.screw, Materials.Steel, 1);
            ItemStack stick = GTItems.getStack(MaterialPrefix.stick, Materials.Steel, 1);
            ItemStack casing = GTBlocks.getStack(BlockMaterialPrefix.casingMachine, Materials.Steel);
            added += register(recipes, "storage/locker",
                    new String[]{"SdS", "LCL", "TMT"},
                    Map.of('C', Ingredient.of(anyChest), 'S', Ingredient.of(stick), 'T', Ingredient.of(screw),
                            'L', Ingredient.of(new ItemStack(Items.LEATHER)), 'M', Ingredient.of(casing),
                            'd', Ingredient.of(tool(GTToolType.SCREWDRIVER))),
                    new ItemStack(GTStorageContainers.LOCKER.get().asItem()));
        }
        return added;
    }

    private static com.gregtech.gregtech.block.inventory.DrawerQuadBlock drawer(GTMaterial material) {
        for (var entry : GTStorageContainers.DRAWERS) if (entry.get().material() == material) return entry.get();
        throw new IllegalArgumentException("No drawer for " + material.getName());
    }
    private static com.gregtech.gregtech.block.misc.AdvancedCraftingTableBlock advancedCraftingTable(GTMaterial material) {
        for (var entry : GTCraftingTables.ADVANCED) if (entry.get().material() == material) return entry.get();
        throw new IllegalArgumentException("No advanced crafting table for " + material.getName());
    }
    private static com.gregtech.gregtech.block.misc.ChargingCraftingTableBlock chargingCraftingTable(GTMaterial material) {
        for (var entry : GTCraftingTables.CHARGING) if (entry.get().material() == material) return entry.get();
        throw new IllegalArgumentException("No charging crafting table for " + material.getName());
    }
    private static ItemStack metalChest(GTStorageMetals.Spec spec) {
        return new ItemStack(GTMetalChests.chest(spec.material(), false));
    }
    private static ItemStack massStorage(GTStorageMetals.Spec spec) {
        return new ItemStack(BuiltInRegistries.ITEM.get(id("mass_storage_" + spec.suffix())));
    }
    private static ItemStack logisticsMassStorage(GTStorageMetals.Spec spec) {
        return new ItemStack(BuiltInRegistries.ITEM.get(id("logistics_mass_storage_" + spec.suffix())));
    }

    private static int requiredAxle(Map<ResourceLocation, byte[]> recipes, String path, String[] pattern,
                                    Map<Character, Ingredient> keys, ItemStack output, boolean creosote) {
        if (creosote) throw new IllegalArgumentException("Finite creosote axle is owned by the existing axle builder");
        int made = register(recipes, path, pattern, keys, output);
        if (made != 1) throw new IllegalStateException("Original hand recipe could not be registered: " + path);
        return made;
    }
    private static int register(Map<ResourceLocation, byte[]> recipes, String path, String[] pattern,
                                Map<Character, Ingredient> keys, ItemStack output) {
        return register(recipes, path, pattern, keys, output, false, false);
    }
    private static int register(Map<ResourceLocation, byte[]> recipes, String path, String[] pattern,
                                Map<Character, Ingredient> keys, ItemStack output, boolean creosote, boolean mirror) {
        if (output.isEmpty()) return 0;
        var key = new JsonObject();
        // Source builds the grid directly and tolerates unused tool keys; vanilla's JSON codec rejects them.
        var used = new java.util.LinkedHashSet<Character>();
        for (String row : pattern) for (char symbol : row.toCharArray()) if (symbol != ' ') used.add(symbol);
        for (char symbol : used) {
            var ingredient = keys.get(symbol);
            if (ingredient == null || ingredient == Ingredient.EMPTY) return 0;
            key.add(String.valueOf(symbol), Ingredient.CODEC.encodeStart(JsonOps.INSTANCE, ingredient).getOrThrow());
        }
        var json = new JsonObject();
        json.addProperty("type", "gregtech:tool_shaped"); json.addProperty("category", "misc");
        json.addProperty("group", "gt.hand"); json.addProperty("allow_mirror", mirror);
        var rows = new com.google.gson.JsonArray(); for (String row : pattern) rows.add(row);
        json.add("pattern", rows); json.add("key", key);
        var result = new JsonObject(); result.addProperty("id", BuiltInRegistries.ITEM.getKey(output.getItem()).toString());
        result.addProperty("count", output.getCount()); json.add("result", result);
        var location = id("recipe/hand/" + path + ".json");
        if (recipes.putIfAbsent(location, json.toString().getBytes(StandardCharsets.UTF_8)) != null)
            throw new IllegalStateException("Duplicate original hand recipe " + location);
        return 1;
    }
}
