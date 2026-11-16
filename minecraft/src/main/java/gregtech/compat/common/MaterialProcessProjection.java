package gregtech.compat.common;

import com.mojang.logging.LogUtils;
import gregtech.api.addon.material.DataKey;
import gregtech.api.addon.material.MaterialCatalogView;
import gregtech.api.addon.material.MaterialKey;
import gregtech.api.addon.material.MaterialView;
import gregtech.content.recipe.MaterialProcessSnapshot;
import gregtech.engine.material.MaterialProcessGraph;
import gregtech.engine.material.MaterialProcessGraph.AmountUnit;
import gregtech.engine.material.MaterialProcessGraph.Notice;
import gregtech.engine.material.MaterialProcessGraph.Port;
import gregtech.engine.material.MaterialProcessGraph.Resource;
import gregtech.engine.material.MaterialProcessGraph.Step;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

/** Converts one material-local semantic query into native viewer values. */
public final class MaterialProcessProjection {
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();

    private MaterialProcessProjection() {}

    public static List<MaterialProcessDisplay.Page> initialPages(
            MaterialProcessSnapshot snapshot, MaterialProcessDisplay.Navigation navigation) {
        var pages = new ArrayList<MaterialProcessDisplay.Page>();
        var workstations = RecipeViewerIcons.categories();
        for (MaterialProcessSnapshot.Root root : snapshot.roots()) {
            var rootPages = pages(snapshot, root.resource(), navigation, workstations);
            pages.addAll(rootPages);
        }
        return pages.isEmpty() ? MaterialProcessDisplay.unbound() : List.copyOf(pages);
    }

    public static List<MaterialProcessDisplay.Page> pages(
            MaterialProcessSnapshot snapshot,
            DataKey root,
            MaterialProcessDisplay.Navigation navigation) {
        return pages(snapshot, root, navigation, RecipeViewerIcons.categories());
    }

    private record IndexPort(DataKey resource, MaterialProcessGraph.Role role) {}

    /** Viewer registration needs unique material resource/role representatives, without any graph or layout. */
    public static List<MaterialProcessDisplay.Port> indexedPorts(MaterialProcessSnapshot snapshot, DataKey root) {
        Resource resource = snapshot.resource(root).orElse(null);
        if (resource == null) return List.of();
        var ports = new java.util.LinkedHashMap<IndexPort, MaterialProcessDisplay.Port>();
        for (Step step : snapshot.materialSteps(root)) {
            var recipe = snapshot.recipe(step.reference()).orElse(null);
            for (List<Port> side : List.of(step.inputs(), step.outputs()))
                for (Port port : side) {
                    if (!port.resource().material().equals(resource.material())) continue;
                    var key = new IndexPort(port.resource().key(), port.role());
                    var existing = ports.get(key);
                    if (existing != null && existing.hasIcons()) continue;
                    try {
                        ports.put(key, port(snapshot, port, recipe));
                    } catch (RuntimeException failure) {
                        LOGGER.warn("Discarded invalid material process index resource {}", port.resource().key(), failure);
                    }
                }
        }
        return List.copyOf(ports.values());
    }

    public static MaterialProcessDisplay.Page informationPage(String material,
            List<MaterialProcessDisplay.Information> information, MaterialProcessDisplay.Navigation navigation) {
        return new MaterialProcessDisplay.Page(
                Component.translatable("gt6ce.jei.material_process.material_title", materialName(material)), material,
                List.of(), List.of(), List.of(Component.translatable("gt6ce.jei.material_process.information_hint")),
                1, 1, navigation, information);
    }

    private static List<MaterialProcessDisplay.Page> pages(
            MaterialProcessSnapshot snapshot,
            DataKey root,
            MaterialProcessDisplay.Navigation navigation,
            java.util.Map<gregtech.engine.recipe.RecipeMap, ItemStack> workstations) {
        var query = snapshot.queryMaterial(root, MaterialProcessGraph.Budgets.MATERIAL_PAGE);
        var steps = new ArrayList<MaterialProcessDisplay.Step>();
        for (Step step : query.steps()) {
            try {
                steps.add(step(snapshot, step, workstations));
            } catch (RuntimeException failure) {
                LOGGER.warn("Discarded invalid material process display step {}", step.reference(), failure);
            }
        }
        List<MaterialProcessDisplay.Flow> flows = List.of();
        var connections = query.edges().stream()
                .map(edge -> new MaterialProcessDisplay.Connection(
                        edge.source(), edge.target(), edge.resource(), edge.confirmed()))
                .toList();
        List<Component> notices = new ArrayList<>(query.notices().stream().map(MaterialProcessProjection::notice).toList());
        if (steps.size() != query.steps().size())
            notices.add(Component.translatable("gt6ce.jei.material_process.coverage.invalid_data", query.steps().size() - steps.size()));
        if (notices.isEmpty()) notices = List.of(Component.translatable("gt6ce.jei.material_process.partial_unknown"));
        Resource resource = snapshot.resource(query.root()).orElse(null);
        Component title = title(query.root(), resource);
        String material = resource == null ? "" : resource.material();
        return List.of(new MaterialProcessDisplay.Page(
                title, material, steps, connections, notices, 1, 1, navigation, List.of(), flows));
    }

    private static MaterialProcessDisplay.Step step(MaterialProcessSnapshot snapshot, Step step,
            java.util.Map<gregtech.engine.recipe.RecipeMap, ItemStack> workstations) {
        Optional<MaterialProcessDisplay.SpecialProcess> specialProcess = step.specialProcess()
                .flatMap(id -> specialMethod(id).map(method -> new MaterialProcessDisplay.SpecialProcess(
                        id, Component.translatable(method.displayNameKey()))));
        return new MaterialProcessDisplay.Step(
                step.reference(),
                Component.literal(step.name()),
                snapshot.recipe(step.reference())
                        .map(recipe -> workstations.getOrDefault(recipe.map(), new ItemStack(net.minecraft.world.item.Items.ANVIL)))
                        .orElseGet(() -> snapshot.worldgen(step.reference()).isPresent()
                                ? new ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE) : snapshot.viewerMachine(step.reference())),
                step.inputs().stream().map(port -> port(snapshot, port, snapshot.recipe(step.reference()).orElse(null))).toList(),
                step.outputs().stream().map(port -> port(snapshot, port, snapshot.recipe(step.reference()).orElse(null))).toList(),
                step.details().stream().map(MaterialProcessProjection::literal).toList(),
                specialProcess,
                step.resolved(),
                snapshot.worldgen(step.reference()).isPresent(),
                snapshot.recipe(step.reference())
                        .map(MaterialProcessSnapshot.NativeRecipe::genericRecovery)
                        .orElse(false),
                snapshot.process(step.reference()));
    }

    private static Optional<gregtech.api.addon.material.SpecialProcessMethod> specialMethod(
            DataKey id) {
        MaterialCatalogView catalog;
        try {
            catalog = gregtech.engine.material.MaterialViews.catalog();
        } catch (IllegalStateException notReady) {
            return Optional.empty();
        }
        return catalog.specialProcesses().find(id);
    }

    private static MaterialProcessDisplay.Port port(MaterialProcessSnapshot snapshot, Port port, MaterialProcessSnapshot.NativeRecipe nativeRecipe) {
        List<ItemStack> icons = snapshot.viewerIcons(port.resource().key());
        if (icons.isEmpty()) icons = nativeIcons(port.resource(), nativeRecipe);
        String identity = port.resource().state();
        if (!icons.isEmpty()) {
            ItemStack representative = icons.get(0);
            if (gregtech.engine.fluid.FluidDisplayBinding.hasPayload(representative)) {
//? if >=1.21.1 {
                /*var fluid = gregtech.engine.fluid.FluidDisplayBinding.resolveCurrent(representative);
*///?} else {
                var fluid = gregtech.engine.fluid.FluidDisplayBinding.resolve(representative);
//?}
                if (!fluid.isEmpty()) identity = MaterialProcessSnapshot.fluidDisplayState(fluid);
            } else identity = MaterialProcessSnapshot.itemDisplayState(representative);
        }
        return new MaterialProcessDisplay.Port(
                port.resource().key(),
                port.resource().material(),
                name(port.resource()),
                icons,
                role(port.role()),
                port.main(),
                amount(port.amount().value(), port.amount().unit()),
                quantity(port.amount().value(), port.amount().unit()),
                port.chance(),
                port.details().stream().map(MaterialProcessProjection::literal).toList(),
                identity);
    }

    private static Component title(DataKey root, Resource resource) {
        String material = resource == null ? pathMaterial(root) : resource.material();
        return Component.translatable("gt6ce.jei.material_process.material_title", materialName(material));
    }

    private static String pathMaterial(DataKey key) {
        String[] path = key.path().split("/");
        return path.length > 2 ? path[2].replace('_', ' ') : "unknown";
    }

    private static Component name(Resource resource) {
        String form = switch (resource.form()) {
            case "ore" -> "ore";
            case "oreRaw" -> "raw";
            case "crushed" -> "crushed";
            case "unit" -> "unit";
            case "fluid" -> resource.phase();
            default -> resource.form();
        };
        return Component.translatable(
                "gt6ce.jei.material_process.port_name", materialName(resource.material()), form);
    }

    private static Component materialName(String material) {
        MaterialCatalogView catalog;
        try {
            catalog = gregtech.engine.material.MaterialViews.catalog();
        } catch (IllegalStateException notReady) {
            return Component.literal(material);
        }
        Optional<MaterialView> view = catalog.find(new MaterialKey(material));
        if (view.isEmpty()) return Component.literal(material);
        return Component.translatable(view.get().info().translationKey());
    }

    private static Component literal(String value) {
        return Component.literal(value);
    }

    private static List<ItemStack> nativeIcons(Resource resource, MaterialProcessSnapshot.NativeRecipe row) {
        String state = resource.state();
        if (row != null) {
            var recipe = row.recipe();
            if (state.startsWith("tag:")) {
                String tag = state.substring("tag:".length());
                if (resource.form().equals("fluid")) {
                    for (int index = 0; index < recipe.mFluidInputs.length; index++) {
                        var key = recipe.fluidTag(index);
                        var stack = recipe.mFluidInputs[index];
                        if (key != null && key.location().toString().equals(tag) && stack != null && !stack.isEmpty()
                                && MaterialProcessSnapshot.fluidDisplayState(stack).contains("#")) {
                            var display = fluidDisplay(stack);
                            return display.isEmpty() ? List.of() : List.of(display);
                        }
                    }
                } else {
                    for (int index = 0; index < recipe.mInputs.length; index++) {
                        var key = recipe.itemTag(index);
                        var stack = recipe.mInputs[index];
                        if (key != null && key.location().toString().equals(tag) && stack != null && !stack.isEmpty()
                                && MaterialProcessSnapshot.itemDisplayState(stack).contains("#"))
                            return List.of(stack.copyWithCount(1));
                    }
                }
            }
            if (state.startsWith("fluid:")) {
                for (FluidStack stack : java.util.stream.Stream.concat(
                        java.util.Arrays.stream(recipe.mFluidInputs), java.util.Arrays.stream(recipe.mFluidOutputs)).toList())
                    if (stack != null && !stack.isEmpty() && MaterialProcessSnapshot.fluidDisplayState(stack).equals(state)) {
                        ItemStack display = fluidDisplay(stack);
                        return display.isEmpty() ? List.of() : List.of(display);
                    }
            }
            if (state.startsWith("item:") || state.startsWith("unit:")) {
                var candidates = new ArrayList<ItemStack>();
                candidates.addAll(java.util.Arrays.asList(recipe.mInputs));
                candidates.addAll(java.util.Arrays.asList(recipe.mOutputs));
                if (state.startsWith("unit:"))
                    for (int index = 0; index < recipe.mInputs.length; index++) {
                        var display = recipe.materialInputDisplay(index);
                        if (display != null) candidates.addAll(display.alternatives());
                    }
                var result = new ArrayList<ItemStack>();
                for (ItemStack stack : candidates) {
                    if (stack == null || stack.isEmpty()) continue;
                    boolean matches = state.startsWith("item:") ? MaterialProcessSnapshot.itemDisplayState(stack).equals(state)
                            : java.util.Arrays.asList(state.substring(state.lastIndexOf('|') + 1).split(","))
                                    .contains(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
                    if (matches && result.stream().noneMatch(existing -> gregtech.engine.stack.StackCompat.sameItem(existing, stack))) {
                        result.add(stack.copyWithCount(1));
                        if (result.size() >= MaterialProcessDisplay.MAX_ICONS) break;
                    }
                }
                if (!result.isEmpty()) return List.copyOf(result);
            }
        }
        // A fingerprint is not a serialized stack. Never replace a stateful value with an untagged look-alike.
        if ((state.startsWith("item:") || state.startsWith("fluid:")) && state.contains("#")) return List.of();
        return icons(resource);
    }

    private static List<ItemStack> icons(Resource resource) {
        var icons = new LinkedHashSet<ItemStack>();
        String state = resource.state();
        if (state.startsWith("item:")) {
            String id = state.substring("item:".length());
            int suffix = id.indexOf('#');
            if (suffix >= 0) id = id.substring(0, suffix);
            item(id).ifPresent(item -> icons.add(new ItemStack(item)));
        } else if (state.startsWith("unit:")) {
            String alternatives = state.substring("unit:".length());
            int separator = alternatives.lastIndexOf('|');
            if (separator >= 0) alternatives = alternatives.substring(separator + 1);
            for (String value : alternatives.split(",")) {
                if (icons.size() >= MaterialProcessDisplay.MAX_ICONS) break;
                item(value).ifPresent(item -> icons.add(new ItemStack(item)));
            }
        } else if (state.startsWith("tag:")) {
            ResourceLocation tag = ResourceLocation.tryParse(state.substring("tag:".length()));
            if (tag != null) {
                if (resource.form().equals("fluid")) {
                    var key =
                            net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.FLUID, tag);
                    for (var holder : BuiltInRegistries.FLUID.getTagOrEmpty(key)) {
                        if (icons.size() >= MaterialProcessDisplay.MAX_ICONS) break;
                        if (holder.value() == net.minecraft.world.level.material.Fluids.EMPTY) continue;
                        ItemStack display = fluidDisplay(new FluidStack(holder.value(), 1000));
                        if (!display.isEmpty()) icons.add(display);
                    }
                } else {
                    var key = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, tag);
                    for (var holder : BuiltInRegistries.ITEM.getTagOrEmpty(key)) {
                        if (icons.size() >= MaterialProcessDisplay.MAX_ICONS) break;
                        icons.add(new ItemStack(holder.value()));
                    }
                }
            }
        } else if (state.startsWith("fluid:")) {
            String value = state.substring("fluid:".length());
            int suffix = value.indexOf('#');
            if (suffix >= 0) value = value.substring(0, suffix);
            ResourceLocation id = ResourceLocation.tryParse(value);
            if (id != null) {
                Fluid fluid = BuiltInRegistries.FLUID.get(id);
                if (fluid != net.minecraft.world.level.material.Fluids.EMPTY) {
                    ItemStack display = fluidDisplay(new FluidStack(fluid, 1000));
                    if (!display.isEmpty()) icons.add(display);
                }
            }
        }
        return List.copyOf(icons);
    }

    private static Optional<Item> item(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) return Optional.empty();
        Item item = BuiltInRegistries.ITEM.get(location);
        return item == net.minecraft.world.item.Items.AIR ? Optional.empty() : Optional.of(item);
    }

//? if >=1.21.1 {
    /*private static ItemStack fluidDisplay(FluidStack stack) {
        var lookup = gregtech.engine.fluid.FluidDisplayBinding.currentLookup();
        return lookup == null ? ItemStack.EMPTY : gregtech.engine.fluid.FluidDisplayBinding.display(lookup, stack);
    }
*///?} else {
    private static ItemStack fluidDisplay(FluidStack stack) {
        return gregtech.engine.fluid.FluidDisplayBinding.display(stack);
    }
//?}

    private static MaterialProcessDisplay.Role role(MaterialProcessGraph.Role role) {
        return switch (role) {
            case CONSUMED -> MaterialProcessDisplay.Role.CONSUMED;
            case CATALYST -> MaterialProcessDisplay.Role.CATALYST;
            case PRESENCE -> MaterialProcessDisplay.Role.PRESENCE;
            case PRIMARY -> MaterialProcessDisplay.Role.PRIMARY;
            case BYPRODUCT -> MaterialProcessDisplay.Role.BYPRODUCT;
            case UNKNOWN -> MaterialProcessDisplay.Role.UNKNOWN;
        };
    }

    private static Component quantity(long value, AmountUnit unit) {
        return Component.literal(switch (unit) {
            case ITEM -> value == 1 ? "" : Long.toString(value);
            case MATERIAL_UNIT -> value + "U";
            case MILLIBUCKET -> value + "mB";
            case UNKNOWN -> "?";
        });
    }

    private static Component amount(long value, AmountUnit unit) {
        return switch (unit) {
            case ITEM -> Component.translatable("gt6ce.jei.material_process.amount.items", value);
            case MATERIAL_UNIT -> Component.translatable("gt6ce.jei.material_process.amount.units", value);
            case MILLIBUCKET -> Component.translatable("gt6ce.jei.material_process.amount.mb", value);
            case UNKNOWN -> Component.translatable("gt6ce.jei.material_process.amount.unknown");
        };
    }

    private static Component notice(Notice notice) {
        return switch (notice.code()) {
            case "coverage_final_recipes" ->
                Component.translatable("gt6ce.jei.material_process.coverage.final_recipes");
            case "coverage_active_worldgen" ->
                Component.translatable("gt6ce.jei.material_process.coverage.active_worldgen");
            case "coverage_viewer_recipes" ->
                Component.translatable("gt6ce.jei.material_process.coverage.viewer_recipes");
            case "partial_viewer_recipes" ->
                Component.translatable("gt6ce.jei.material_process.coverage.viewer_exclusions", notice.omitted());
            case "partial_dynamic_selectors" ->
                Component.translatable("gt6ce.jei.material_process.coverage.dynamic_selectors");
            case "partial_unknown_roles" ->
                Component.translatable("gt6ce.jei.material_process.coverage.unknown_roles");
            case "invalid_process_data" ->
                Component.translatable("gt6ce.jei.material_process.coverage.invalid_data", notice.omitted());
            case "node_limit" ->
                Component.translatable("gt6ce.jei.material_process.coverage.node_limit", notice.omitted());
            case "port_limit" ->
                Component.translatable("gt6ce.jei.material_process.coverage.port_limit", notice.omitted());
            case "edge_limit" ->
                Component.translatable("gt6ce.jei.material_process.coverage.edge_limit", notice.omitted());
            case "depth_limit" ->
                Component.translatable("gt6ce.jei.material_process.coverage.depth_limit", notice.omitted());
            case "no_sources" -> Component.translatable("gt6ce.jei.material_process.no_sources");
            case "no_uses" -> Component.translatable("gt6ce.jei.material_process.no_uses");
            case "no_both" -> Component.translatable("gt6ce.jei.material_process.no_both");
            default -> Component.literal(notice.code());
        };
    }
}
