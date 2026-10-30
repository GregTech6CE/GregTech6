package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.api.material.MaterialProperty;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.data.MaterialPrefixes;
import gregtech.content.material.Materials;
import gregtech.content.registry.GTBlocks;
import gregtech.content.registry.GTItems;
import gregtech.content.registry.GTTechnological;
import gregtech.content.registry.GTWires;
import gregtech.engine.fluid.PortableFluidContainerSpec;
import gregtech.engine.material.ItemMaterialRegistry;
import gregtech.engine.material.MaterialUnification;
import gregtech.engine.prefix.PrefixRegistry;
import gregtech.engine.recipe.Recipe;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** GT6 {@code Loader_Recipes_Handlers.java:740-841}: prefix matrix, forging targets and both mold families. */
public final class SourceExtruderRecipes {
    private SourceExtruderRecipes() {}

    public record Entry(
            GTMaterial inputMaterial,
            GTMaterial outputMaterial,
            String inputForm,
            String outputForm,
            boolean lowHeatMold,
            boolean simple,
            long inputUnits,
            long outputUnits,
            Recipe recipe) {}

    private record Form(String name, long weight, ItemStack stack) {}

    private record Output(String form, String mold, boolean nonSmithable) {}

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static final List<Entry> PENDING = new ArrayList<>();
    private static final Set<GTMaterial> VISITED = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Map<GTMaterial, Map<String, Form>> FORMS = new IdentityHashMap<>();
    private static Map<GTMaterial, Map<String, Form>> physical;

    public static List<Entry> entries() {
        return Collections.unmodifiableList(ENTRIES);
    }

    // OP.java's EXTRUDER_FODDER, INGOT_BASED and GEM_BASED prefixes; all current DUST_BASED
    // members are also EXTRUDER_FODDER in this pinned source. Lookup never invents absent forms.
    private static final List<String> INPUTS = List.of(
            "dust",
            "dustSmall",
            "dustTiny",
            "dustDiv72",
            "ingotQuintuple",
            "ingotQuadruple",
            "ingotTriple",
            "ingotDouble",
            "ingot",
            "billet",
            "chunkGt",
            "nugget",
            "gem",
            "gemChipped",
            "gemFlawed",
            "gemFlawless",
            "gemExquisite",
            "gemLegendary",
            "bouleGt",
            "lens",
            "scrapGt",
            "plateSteamcraft",
            "plateDense",
            "plateQuintuple",
            "plateQuadruple",
            "plateTriple",
            "plateDouble",
            "plate",
            "plateGem",
            "plateTiny",
            "plateGemTiny",
            "plateCurved",
            "compressed",
            "sheetGt",
            "foil",
            "stick",
            "stickLong",
            "bolt",
            "screw",
            "round",
            "ring",
            "chain",
            "spring",
            "springSmall",
            "wireFine",
            "minecartWheels",
            "gearGt",
            "gearGtSmall",
            "railGt",
            "casingSmall",
            "casingMachine",
            "casingMachineDouble",
            "casingMachineQuadruple",
            "casingMachineDense",
            "frameGt",
            "capcellcon",
            "pipeTiny",
            "pipeSmall",
            "pipeMedium",
            "pipeLarge",
            "pipeHuge",
            "pipeQuadruple",
            "pipeNonuple",
            "wireGt16",
            "wireGt15",
            "wireGt14",
            "wireGt13",
            "wireGt12",
            "wireGt11",
            "wireGt10",
            "wireGt09",
            "wireGt08",
            "wireGt07",
            "wireGt06",
            "wireGt05",
            "wireGt04",
            "wireGt03",
            "wireGt02",
            "wireGt01",
            "blockGem",
            "blockDust",
            "blockIngot",
            "blockPlate",
            "blockPlateGem",
            "blockSolid");
    private static final List<Output> OUTPUTS = List.of(
            out("ingot", "ingot"),
            out("plate", "plate"),
            out("plateCurved", "curvedplate"),
            out("stick", "rod"),
            out("stickLong", "longrod"),
            out("bolt", "bolt"),
            out("ring", "ring"),
            out("wireGt01", "wire"),
            out("casingSmall", "casing"),
            out("toolHeadRawShovel", "shovelhead"),
            out("pipeTiny", "tinypipe"),
            out("pipeSmall", "smallpipe"),
            out("pipeMedium", "mediumpipe"),
            out("pipeLarge", "largepipe"),
            out("pipeHuge", "hugepipe"),
            out("toolHeadRawSword", "swordblade"),
            out("toolHeadRawHoe", "hoehead"),
            out("toolHeadRawSaw", "sawblade"),
            out("toolHeadRawPickaxe", "pickaxehead"),
            out("toolHeadRawAxe", "axehead"),
            out("toolHeadFile", "filehead"),
            out("toolHeadHammer", "hammerhead"),
            out("gearGt", "gear"),
            out("gearGtSmall", "smallgear"),
            out("blockSolid", "block"),
            out("capcellcon", "capsulecellcontainer"),
            out("plateTiny", "tinyplate"),
            new Output("foil", "foil", true),
            new Output("wireFine", "finewire", true));

    private static Output out(String form, String mold) {
        return new Output(form, mold, false);
    }

    /**
     * {@code Loader_Recipes_Extruder.java:202}: prefixes whose weight is at least {@code U64},
     * at most {@code U} and divides {@code U}. The special wax/glass/plastic/zirconium rows
     * consume one material unit per row, so larger blocks and non-divisible forms do not participate.
     */
    private static final List<String> SPECIAL_PREFIXES = List.of(
            "dust",
            "dustSmall",
            "dustTiny",
            "ingot",
            "chunkGt",
            "nugget",
            "gem",
            "gemChipped",
            "gemFlawed",
            "scrapGt",
            "plate",
            "plateGem",
            "plateTiny",
            "plateGemTiny",
            "plateCurved",
            "compressed",
            "sheetGt",
            "foil",
            "stick",
            "stickLong",
            "bolt",
            "screw",
            "round",
            "ring",
            "chain",
            "spring",
            "springSmall",
            "wireFine",
            "minecartWheels",
            "gearGtSmall",
            "railGt",
            "casingSmall",
            "capcellcon",
            "pipeTiny",
            "pipeSmall",
            "wireGt01",
            "wireGt02");

    public static int register(GTMaterial material) {
        if (!VISITED.add(material) || !material.isValid() || material.has(MaterialProperty.ANTIMATTER)) return 0;
        var profile = SourceExtrusionRules.profile(material);
        // Unknown native/addon materials retain only the former ordinary four-input matrix;
        // no original eligibility or low-heat permission is fabricated for them.
        if (profile == null) {
            if (!material.has(MaterialProperty.SMITHABLE)) return 0;
            profile = new SourceExtrusionRules.Profile(true, false, true, "", GTValues.U);
        }
        if (!profile.extrudable() || profile.forgingAmount() < GTValues.U) return 0;
        GTMaterial target = SourceExtrusionRules.target(material, profile);
        if (target == null || !target.isValid()) return 0;
        int before = PENDING.size();
        var inputs = forms(material);
        var outputs = forms(target);
        boolean nativeProfile = SourceExtrusionRules.profile(material) != null;
        for (String name : INPUTS) {
            if (!nativeProfile && !Set.of("ingot", "plate", "gem", "dust").contains(name)) continue;
            Form input = inputs.get(name);
            if (input == null) continue;
            for (Output row : OUTPUTS) {
                if (name.equals(row.form()) || row.nonSmithable() && profile.smithable()) continue;
                Form output = outputs.get(row.form());
                if (output == null) continue;
                long[] amounts = SourceExtrusionRules.amounts(input.weight(), output.weight());
                if (amounts == null) continue;
                long ticks = profile.simple()
                        ? SourceExtrusionRules.simpleTicks(output.weight() * amounts[2])
                        : SourceExtrusionRules.normalTicks(material, target, input.weight() * amounts[0]);
                add(material, target, input, output, row, amounts, ticks, profile.simple(), false);
                if (profile.simple()) add(material, target, input, output, row, amounts, ticks, true, true);
            }
        }
        return PENDING.size() - before;
    }

    private static void add(
            GTMaterial material,
            GTMaterial target,
            Form input,
            Form output,
            Output row,
            long[] amounts,
            long ticks,
            boolean simple,
            boolean lowHeat) {
        var shape = GTTechnological.get((lowHeat ? "low_heat_extruder_shape_" : "extruder_shape_") + row.mold());
        if (shape == null) throw new IllegalStateException("Missing registered extruder mold " + row.mold());
        ItemStack in = input.stack().copyWithCount((int) amounts[0]);
        int count = (int) amounts[1];
        ItemStack[] out = count > 64
                ? new ItemStack[] {
                    output.stack().copyWithCount(64), output.stack().copyWithCount(count - 64)
                }
                : new ItemStack[] {output.stack().copyWithCount(count)};
        Recipe recipe = new Recipe(
                        new ItemStack[] {in, new ItemStack(shape)},
                        out,
                        null,
                        null,
                        null,
                        null,
                        ticks,
                        simple ? 16 : 96,
                        0)
                .withCatalystInputs(1)
                .withItemInputGuard(0, stack -> !ItemMaterialRegistry.hasStoredContents(stack));
        PENDING.add(new Entry(
                material,
                target,
                input.name(),
                output.name(),
                lowHeat,
                simple,
                input.weight() * amounts[0],
                output.weight() * amounts[1],
                recipe));
    }

    /**
     * The explicit rows of {@code Loader_Recipes_Extruder.java:200-295}: wax pills, glass
     * panes/tubes/bottles, the plastic canister, tin-alloy food cans and the zirconium reactor-rod hull.
     */
    public static int registerSpecialRecipes() {
        int before = PENDING.size();
        ItemStack waxPill = registryItem("gt6ce:empty_wax_pill", 1);
        waxRows(Materials.WaxPlant, waxPill);
        waxRows(Materials.WaxParaffin, waxPill);
        waxRows(Materials.WaxBee, waxPill);
        glassRows();
        plasticCanisterRows();
        tinAlloyRows();
        zirconiumRodRows();
        return PENDING.size() - before;
    }

    private static void waxRows(GTMaterial material, ItemStack pill) {
        if (material == null || !material.isValid() || pill.isEmpty()) return;
        var forms = forms(material);
        for (String name : SPECIAL_PREFIXES) {
            Form form = forms.get(name);
            if (form == null) continue;
            ItemStack input = unitInput(form);
            if (input.isEmpty()) continue;
            addSpecial(material, name, input, "extruder_shape_bottle", "emptyWaxPill", pill, 64, 16);
            addSpecial(material, name, input, "low_heat_extruder_shape_bottle", "emptyWaxPill", pill, 64, 16);
        }
    }

    private static void glassRows() {
        GTMaterial glass = Materials.Glass;
        if (glass == null || !glass.isValid()) return;
        Form paneForm = forms(glass).get("plateGem");
        ItemStack pane = paneForm == null ? ItemStack.EMPTY : paneForm.stack().copyWithCount(1);
        ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
        var forms = forms(glass);
        for (String name : SPECIAL_PREFIXES) {
            Form form = forms.get(name);
            if (form == null) continue;
            ItemStack input = unitInput(form);
            if (input.isEmpty()) continue;
            if (!name.equals("plateGem") && !pane.isEmpty()) {
                addSpecial(glass, name, input, "extruder_shape_plate", "plateGem", pane, 64, 16);
                addSpecial(glass, name, input, "low_heat_extruder_shape_plate", "plateGem", pane, 64, 16);
            }
            addSpecial(glass, name, input, "extruder_shape_bottle", "glassBottle", bottle, 64, 16);
            addSpecial(glass, name, input, "low_heat_extruder_shape_bottle", "glassBottle", bottle, 64, 16);
        }
    }

    private static void plasticCanisterRows() {
        GTMaterial plastic = Materials.Plastic;
        if (plastic == null || !plastic.isValid()) return;
        ItemStack canister = registryItem("gt6ce:plastic_canister", 1);
        if (canister.isEmpty()) return;
        var forms = forms(plastic);
        for (String name : SPECIAL_PREFIXES) {
            Form form = forms.get(name);
            if (form == null) continue;
            ItemStack input = unitInput(form);
            if (input.isEmpty() || input.getCount() * 3 > input.getMaxStackSize()) continue;
            ItemStack batch = input.copyWithCount(input.getCount() * 3);
            addSpecial(plastic, name, batch, "extruder_shape_cell", "plasticCanister", canister, 192, 16);
            addSpecial(plastic, name, batch, "low_heat_extruder_shape_cell", "plasticCanister", canister, 192, 16);
        }
    }

    private static void tinAlloyRows() {
        GTMaterial tinAlloy = Materials.TinAlloy;
        if (tinAlloy == null || !tinAlloy.isValid()) return;
        ItemStack can = registryItem("gt6ce:empty_food_can", 1);
        if (can.isEmpty()) return;
        var forms = forms(tinAlloy);
        for (String name : SPECIAL_PREFIXES) {
            Form form = forms.get(name);
            if (form == null) continue;
            ItemStack input = unitInput(form);
            if (input.isEmpty()) continue;
            addSpecial(tinAlloy, name, input, "extruder_shape_cell", "emptyFoodCan", can, 64, 16);
            addSpecial(tinAlloy, name, input, "low_heat_extruder_shape_cell", "emptyFoodCan", can, 64, 16);
        }
    }

    private static void zirconiumRodRows() {
        GTMaterial zirconium = Materials.Zirconium;
        if (zirconium == null || !zirconium.isValid()) return;
        ItemStack rod = registryItem("gt6ce:reactor_rod_9201", 1);
        if (rod.isEmpty()) return;
        // Loader_Recipes_Extruder.java:289 uses the kilogram weight of one material unit, not density.
        long ticks = Math.max(64, (long)
                (((zirconium.getMeltingPoint() - 293d) * (1d + zirconium.getDensity() * 111.111111d)) / 6144d));
        var forms = forms(zirconium);
        for (String name : SPECIAL_PREFIXES) {
            Form form = forms.get(name);
            if (form == null) continue;
            ItemStack input = unitInput(form);
            if (input.isEmpty()) continue;
            addSpecial(zirconium, name, input, "extruder_shape_cell", "reactorRod", rod, ticks, 96);
        }
    }

    /**
     * One material unit worth of the form, exactly as {@code U / prefix.mAmount}. Forms whose
     * weight does not divide {@code U} are outside the source loop's filter.
     */
    private static ItemStack unitInput(Form form) {
        long weight = form.weight();
        if (weight <= 0 || GTValues.U % weight != 0) return ItemStack.EMPTY;
        long amount = GTValues.U / weight;
        if (amount < 1 || amount > 64 || form.stack().isEmpty()) return ItemStack.EMPTY;
        return form.stack().copyWithCount((int) amount);
    }

    private static void addSpecial(
            GTMaterial material,
            String inputName,
            ItemStack input,
            String moldId,
            String outputName,
            ItemStack output,
            long ticks,
            long eut) {
        ItemStack mold = mold(moldId);
        if (input.isEmpty() || mold.isEmpty() || output.isEmpty()) return;
        // The original addRecipe2 calls pass optimize=true: common stack factors are divided out,
        // and the duration follows the same division (9 tiny dust + 3 tubes -> 3 + 1, 64 -> 21 t).
        long divisor = gcd(input.getCount(), output.getCount());
        ItemStack optimizedInput = divisor > 1 ? input.copyWithCount((int) (input.getCount() / divisor)) : input;
        ItemStack optimizedOutput = divisor > 1 ? output.copyWithCount((int) (output.getCount() / divisor)) : output;
        long optimizedTicks = divisor > 1 ? ticks / divisor : ticks;
        Recipe recipe = new Recipe(
                        new ItemStack[] {optimizedInput, mold},
                        new ItemStack[] {optimizedOutput},
                        null,
                        null,
                        null,
                        null,
                        optimizedTicks,
                        eut,
                        0)
                .withCatalystInputs(1);
        PENDING.add(new Entry(
                material,
                material,
                inputName,
                outputName,
                moldId.startsWith("low_heat_extruder_shape_"),
                true,
                GTValues.U,
                GTValues.U,
                recipe));
    }

    private static long gcd(long a, long b) {
        while (b != 0) {
            long t = a % b;
            a = b;
            b = t;
        }
        return a;
    }

    private static ItemStack mold(String id) {
        var item = GTTechnological.get(id);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static ItemStack registryItem(String id, int count) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    /** Flush before the next bootstrap loader; earlier extruder recipes retain precedence. */
    public static int finishRegistration() {
        var accepted = Collections.newSetFromMap(new IdentityHashMap<Recipe, Boolean>());
        accepted.addAll(MachineRecipeMaps.Extruder.addRecipes(
                PENDING.stream().map(Entry::recipe).toList()));
        for (Entry entry : PENDING) if (accepted.contains(entry.recipe())) ENTRIES.add(entry);
        PENDING.clear();
        return ENTRIES.size();
    }

    private static Map<String, Form> forms(GTMaterial material) {
        return FORMS.computeIfAbsent(material, m -> {
            if (physical == null) physical = physicalForms();
            Map<String, Form> result = new LinkedHashMap<>(physical.getOrDefault(m, Map.of()));
            Set<String> names = new LinkedHashSet<>(INPUTS);
            OUTPUTS.forEach(o -> names.add(o.form()));
            var blockPrefixes = MaterialPrefixes.all();
            for (String name : names) {
                // Forms the source stores as a vanilla item replace any GT stack of that form.
                ItemStack vanilla = MaterialUnification.vanillaFormStack(name, m);
                if (!vanilla.isEmpty()) {
                    MaterialPrefix vanillaPrefix = PrefixRegistry.byName(name);
                    long weight = vanillaPrefix != null
                            ? vanillaPrefix.getMaterialWeight()
                            : blockWeight(blockPrefixes.get(name));
                    if (weight > 0) result.put(name, new Form(name, weight, vanilla));
                    continue;
                }
                MaterialPrefix prefix = PrefixRegistry.byName(name);
                if (prefix != null) {
                    ItemStack stack = GTItems.getStack(prefix, m, 1);
                    if (!stack.isEmpty()) result.putIfAbsent(name, new Form(name, prefix.getMaterialWeight(), stack));
                }
                var entry = blockPrefixes.get(name);
                if (entry != null && entry.isBlock()) {
                    var block = entry.getBlockDelegate();
                    ItemStack stack = GTBlocks.getStack(block, m);
                    if (!stack.isEmpty()) result.putIfAbsent(name, new Form(name, block.getMaterialWeight(), stack));
                }
            }
            // A rock type's smooth block is the stone material's blockSolid stack (GT6 OP.blockSolid).
            ItemStack smooth = StoneAndToolSurvivalRecipes.smoothStoneBlocks().get(m);
            if (smooth != null) result.putIfAbsent("blockSolid", new Form("blockSolid", GTValues.U * 9, smooth));
            // GT6 OP.capcellcon is gated by PIPES. The port's forty cell items name exactly the
            // materials that gate covers, including item-pipe-only ones (Tin, waxes).
            ItemStack cell = cellStack(m);
            if (!cell.isEmpty()) result.putIfAbsent("capcellcon", new Form("capcellcon", GTValues.U9, cell));
            return result;
        });
    }

    private static long blockWeight(MaterialPrefixes.Entry entry) {
        return entry != null && entry.isBlock() ? entry.getBlockDelegate().getMaterialWeight() : 0;
    }

    private static ItemStack cellStack(GTMaterial material) {
        for (var spec : PortableFluidContainerSpec.values())
            if (spec.shapeId().equals("cell") && spec.material().resolve() == material)
                return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("gt6ce:fluid_" + spec.id())));
        return ItemStack.EMPTY;
    }

    private static Map<GTMaterial, Map<String, Form>> physicalForms() {
        Map<GTMaterial, Map<String, Form>> result = new IdentityHashMap<>();
        for (var holder : GTWires.allWires()) {
            var block = holder.get();
            int size = block.spec().size();
            String name = String.format(Locale.ROOT, "wireGt%02d", size);
            result.computeIfAbsent(block.spec().material().resolve(), k -> new LinkedHashMap<>())
                    .put(name, new Form(name, GTValues.U2 * size, new ItemStack(block)));
        }
        for (var holder : gregtech.content.registry.GTSignalWires.all()) {
            var block = holder.get();
            if (!block.insulated())
                result.computeIfAbsent(block.material().resolve(), k -> new LinkedHashMap<>())
                        .putIfAbsent("wireGt01", new Form("wireGt01", GTValues.U2, new ItemStack(block)));
        }
        return result;
    }
}
