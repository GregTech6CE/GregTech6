package gregtech.compat.common;

import gregtech.api.addon.material.DataKey;
import gregtech.api.addon.material.MaterialCatalogView;
import gregtech.api.addon.material.MaterialKey;
import gregtech.api.addon.material.MaterialView;
import gregtech.content.recipe.MaterialProcessSnapshot;
import gregtech.engine.material.MaterialProcessGraph;
import gregtech.engine.material.MaterialProcessGraph.AmountUnit;
import gregtech.engine.material.MaterialProcessGraph.Budgets;
import gregtech.engine.material.MaterialProcessGraph.Direction;
import gregtech.engine.material.MaterialProcessGraph.Notice;
import gregtech.engine.material.MaterialProcessGraph.Port;
import gregtech.engine.material.MaterialProcessGraph.Query;
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

/** Converts one already bounded semantic query into native viewer values. */
public final class MaterialProcessProjection {
    private static final Budgets INITIAL_BUDGET = new Budgets(4, 10, 64, 128);

    private MaterialProcessProjection() {}

    public static List<MaterialProcessDisplay.Page> initialPages(
            MaterialProcessSnapshot snapshot, MaterialProcessDisplay.Navigation navigation) {
        var pages = new ArrayList<MaterialProcessDisplay.Page>();
        for (MaterialProcessSnapshot.Root root : snapshot.roots()) {
            var rootPages = pages(snapshot, root.resource(), Direction.BOTH, INITIAL_BUDGET, navigation);
            pages.addAll(rootPages);
        }
        return pages.isEmpty() ? MaterialProcessDisplay.unbound() : List.copyOf(pages);
    }

    public static List<MaterialProcessDisplay.Page> pages(
            MaterialProcessSnapshot snapshot,
            DataKey root,
            Direction direction,
            MaterialProcessDisplay.Navigation navigation) {
        return pages(snapshot, root, direction, Budgets.DEFAULT, navigation);
    }

    private static List<MaterialProcessDisplay.Page> pages(
            MaterialProcessSnapshot snapshot,
            DataKey root,
            Direction direction,
            Budgets budgets,
            MaterialProcessDisplay.Navigation navigation) {
        Query query = snapshot.query(root, direction, budgets);
        var steps = query.steps().stream().map(MaterialProcessProjection::step).toList();
        var connections = query.edges().stream()
                .map(edge -> new MaterialProcessDisplay.Connection(
                        edge.source(), edge.target(), edge.resource(), edge.confirmed()))
                .toList();
        var notices = query.notices().stream().map(MaterialProcessProjection::notice).toList();
        if (notices.isEmpty()) notices = List.of(Component.translatable("gt6ce.jei.material_process.partial_unknown"));
        Component title = title(query.root(), snapshot.resource(query.root()).orElse(null));
        return MaterialProcessDisplay.pages(title, steps, connections, notices, navigation);
    }

    private static MaterialProcessDisplay.Step step(Step step) {
        return new MaterialProcessDisplay.Step(
                step.reference(),
                Component.literal(step.name()),
                step.inputs().stream().map(MaterialProcessProjection::port).toList(),
                step.outputs().stream().map(MaterialProcessProjection::port).toList(),
                step.details().stream().map(MaterialProcessProjection::literal).toList(),
                step.resolved());
    }

    private static MaterialProcessDisplay.Port port(Port port) {
        return new MaterialProcessDisplay.Port(
                port.resource().key(),
                name(port.resource()),
                icons(port.resource()),
                role(port.role()),
                port.main(),
                amount(port.amount().value(), port.amount().unit()),
                port.chance(),
                port.details().stream().map(MaterialProcessProjection::literal).toList());
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
            int separator = alternatives.indexOf('|');
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
            case "partial_dynamic_selectors" ->
                Component.translatable("gt6ce.jei.material_process.coverage.dynamic_selectors");
            case "partial_unknown_roles" ->
                Component.translatable("gt6ce.jei.material_process.coverage.unknown_roles");
            case "node_limit" ->
                Component.translatable("gt6ce.jei.material_process.coverage.node_limit", notice.omitted());
            case "port_limit" ->
                Component.translatable("gt6ce.jei.material_process.coverage.port_limit", notice.omitted());
            case "edge_limit" ->
                Component.translatable("gt6ce.jei.material_process.coverage.edge_limit", notice.omitted());
            case "depth_limit" ->
                Component.translatable("gt6ce.jei.material_process.coverage.node_limit", notice.omitted());
            case "no_sources" -> Component.translatable("gt6ce.jei.material_process.no_sources");
            case "no_uses" -> Component.translatable("gt6ce.jei.material_process.no_uses");
            case "no_both" -> Component.translatable("gt6ce.jei.material_process.no_both");
            default -> Component.literal(notice.code());
        };
    }
}
