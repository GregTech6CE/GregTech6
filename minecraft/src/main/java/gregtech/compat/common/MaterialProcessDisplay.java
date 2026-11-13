package gregtech.compat.common;

import gregtech.api.addon.material.DataKey;
import gregtech.api.addon.recipe.RecipeOutputChances;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Client presentation values for an already bounded process query. This is not a source index or an execution model.
 * No material catalog, native recipe enumeration, or viewer API is accessed here.
 */
public final class MaterialProcessDisplay {
    public static final int STEPS_PER_PAGE = 2;
    public static final int MAX_STEPS = 64;
    public static final int MAX_PORTS = 64;
    public static final int MAX_ICONS = 16;
    public static final int MAX_CONNECTIONS = 128;

    /** Roles come from the producer; neither slot order nor quantity establishes a primary output. */
    public enum Role {
        CONSUMED,
        CATALYST,
        PRESENCE,
        PRIMARY,
        BYPRODUCT,
        UNKNOWN
    }

    /**
     * A snapshot-local resource reference, including form and native state. Icons are bounded display representatives,
     * not an expansion of a tag or unit family. Empty icons describe a symbolic selector.
     * Amount includes its unit; chance uses {@link RecipeOutputChances#CERTAIN}, or is explicitly unknown.
     */
    public record Port(
            DataKey resource,
            Component name,
            List<ItemStack> icons,
            Role role,
            boolean main,
            Component amount,
            OptionalInt chance,
            List<Component> details) {
        public Port {
            Objects.requireNonNull(resource);
            name = name.copy();
            amount = amount.copy();
            Objects.requireNonNull(role);
            Objects.requireNonNull(chance);
            if (icons.size() > MAX_ICONS) throw new IllegalArgumentException("Too many display representatives");
            icons = icons.stream().map(ItemStack::copy).toList();
            if (icons.stream().anyMatch(ItemStack::isEmpty))
                throw new IllegalArgumentException("Use an empty icon list for symbolic resources");
            if (chance.isPresent() && (chance.getAsInt() < 1 || chance.getAsInt() > RecipeOutputChances.CERTAIN))
                throw new IllegalArgumentException("Invalid output chance");
            details = copyText(details);
        }

        @Override
        public List<ItemStack> icons() {
            return icons.stream().map(ItemStack::copy).toList();
        }

        public boolean hasIcons() {
            return !icons.isEmpty();
        }

        public List<Component> tooltip() {
            var lines = new ArrayList<Component>();
            lines.add(name);
            lines.add(amount);
            lines.add(Component.translatable(
                    "gt6ce.jei.material_process.role." + role.name().toLowerCase(java.util.Locale.ROOT)));
            if (isOutput(role)) {
                lines.add(
                        chance.isPresent()
                                ? Component.translatable("gt6ce.jei.chance", chance.getAsInt() / 100.0)
                                : Component.translatable("gt6ce.jei.material_process.chance_unknown"));
            }
            lines.addAll(details);
            return List.copyOf(lines);
        }
    }

    /** Explicit edges from the producer, including branches and cycles. Unconfirmed edges are drawn dashed. */
    public record Connection(DataKey source, DataKey target, DataKey resource, boolean confirmed) {
        public Connection {
            Objects.requireNonNull(source);
            Objects.requireNonNull(target);
            Objects.requireNonNull(resource);
        }
    }

    /** All co-inputs and outputs belong to this one step. Main is a layout hint, separate from the output role. */
    public record Step(
            DataKey reference,
            Component name,
            List<Port> inputs,
            List<Port> outputs,
            List<Component> details,
            boolean resolved) {
        public Step {
            Objects.requireNonNull(reference);
            name = name.copy();
            inputs = List.copyOf(inputs);
            outputs = List.copyOf(outputs);
            validatePorts(inputs, false);
            validatePorts(outputs, true);
            details = copyText(details);
        }
    }

    /**
     * Bind callbacks to the same query session/generation as the page. Revalidate before resolving native references;
     * return false for expired or unsupported targets. Implementations run on the client thread.
     */
    public interface Navigation {
        Navigation NONE = new Navigation() {
            @Override
            public boolean openRecipe(DataKey reference) {
                return false;
            }

            @Override
            public boolean openForm(DataKey resource, boolean uses) {
                return false;
            }
        };

        boolean openRecipe(DataKey reference);

        boolean openForm(DataKey resource, boolean uses);
    }

    /** Coverage and truncation reasons are always visible through the footer tooltip, including for empty queries. */
    public record Page(
            Component title,
            List<Step> steps,
            List<Connection> connections,
            List<Component> notices,
            int number,
            int total,
            Navigation navigation) {
        public Page {
            title = title.copy();
            steps = List.copyOf(steps);
            connections = List.copyOf(connections);
            notices = copyText(notices);
            Objects.requireNonNull(navigation);
            if (steps.size() > STEPS_PER_PAGE
                    || connections.size() > MAX_CONNECTIONS
                    || number < 1
                    || total < number
                    || notices.isEmpty()) throw new IllegalArgumentException("Invalid process display page");
        }
    }

    private MaterialProcessDisplay() {}

    /** Split a bounded, producer-ordered result without dropping steps. No graph traversal or candidate expansion. */
    public static List<Page> pages(
            Component title,
            List<Step> steps,
            List<Connection> connections,
            List<Component> notices,
            Navigation navigation) {
        if (steps.size() > MAX_STEPS) throw new IllegalArgumentException("Process query exceeds display budget");
        if (connections.size() > MAX_CONNECTIONS) throw new IllegalArgumentException("Too many process connections");
        var references = new HashMap<DataKey, Step>();
        for (var step : steps)
            if (references.putIfAbsent(step.reference(), step) != null)
                throw new IllegalArgumentException("Duplicate process reference");
        var uniqueConnections = new HashSet<Connection>();
        for (var connection : connections) {
            if (!references.containsKey(connection.source())
                    || !references.containsKey(connection.target())
                    || !uniqueConnections.add(connection))
                throw new IllegalArgumentException("Invalid process connection");
            if (references.get(connection.source()).outputs().stream()
                            .noneMatch(port -> port.resource().equals(connection.resource()))
                    || references.get(connection.target()).inputs().stream()
                            .noneMatch(port -> port.resource().equals(connection.resource())))
                throw new IllegalArgumentException("Connection does not match process ports");
        }
        int total = Math.max(1, (steps.size() + STEPS_PER_PAGE - 1) / STEPS_PER_PAGE);
        var pages = new ArrayList<Page>(total);
        for (int index = 0; index < total; index++) {
            int start = index * STEPS_PER_PAGE;
            pages.add(new Page(
                    title,
                    steps.subList(start, Math.min(start + STEPS_PER_PAGE, steps.size())),
                    connections,
                    notices,
                    index + 1,
                    total,
                    navigation));
        }
        return List.copyOf(pages);
    }

    public static List<Page> unbound() {
        return pages(
                Component.translatable("gt6ce.jei.material_process.title"),
                List.of(),
                List.of(),
                List.of(Component.translatable("gt6ce.jei.material_process.unbound")),
                Navigation.NONE);
    }

    private static boolean isOutput(Role role) {
        return role == Role.PRIMARY || role == Role.BYPRODUCT || role == Role.UNKNOWN;
    }

    private static void validatePorts(List<Port> ports, boolean output) {
        if (ports.size() > MAX_PORTS || ports.stream().filter(Port::main).count() > 1)
            throw new IllegalArgumentException("Invalid process ports");
        for (var port : ports)
            if (isOutput(port.role()) != output) throw new IllegalArgumentException("Port role on wrong side");
    }

    private static List<Component> copyText(List<Component> lines) {
        return lines.stream().map(line -> (Component) line.copy()).toList();
    }
}
