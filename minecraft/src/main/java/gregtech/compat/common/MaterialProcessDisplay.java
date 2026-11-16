package gregtech.compat.common;

import com.mojang.logging.LogUtils;
import gregtech.api.addon.material.DataKey;
import gregtech.api.addon.recipe.RecipeOutputChances;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

/**
 * Client presentation values for a material process query. This is not a source index or an execution model.
 * No material catalog, native recipe enumeration, or viewer API is accessed here.
 */
public final class MaterialProcessDisplay {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** Only limits automatic icon sampling from selectors, never steps, ports or supplied representatives. */
    public static final int MAX_ICONS = 16;

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
     * Amount includes its unit; quantity is the producer-supplied compact badge, never parsed from amount.
     * Chance uses {@link RecipeOutputChances#CERTAIN}, or is explicitly unknown.
     */
    public record Port(
            DataKey resource,
            String material,
            Component name,
            List<ItemStack> icons,
            Role role,
            boolean main,
            Component amount,
            Component quantity,
            OptionalInt chance,
            List<Component> details,
            String identity) {
        public Port {
            Objects.requireNonNull(resource);
            Objects.requireNonNull(identity);
            if (material == null || material.isBlank()) {
                LOGGER.warn("Missing material for process resource {}; retaining it as unknown", resource);
                material = "unknown";
            }
            name = name.copy();
            amount = amount.copy();
            quantity = quantity.copy();
            Objects.requireNonNull(role);
            Objects.requireNonNull(chance);
            var validIcons = icons.stream()
                    .filter(Objects::nonNull)
                    .filter(stack -> !stack.isEmpty())
                    .toList();
            if (validIcons.size() != icons.size())
                LOGGER.warn("Discarded {} empty display icons for {}", icons.size() - validIcons.size(), resource);
            icons = validIcons.stream().map(ItemStack::copy).toList();
            if (chance.isPresent() && (chance.getAsInt() < 1 || chance.getAsInt() > RecipeOutputChances.CERTAIN)) {
                LOGGER.warn(
                        "Invalid display chance {} for {}; retaining an unknown chance", chance.getAsInt(), resource);
                chance = OptionalInt.empty();
            }
            details = copyText(details);
        }

        public Port(
                DataKey resource,
                String material,
                Component name,
                List<ItemStack> icons,
                Role role,
                boolean main,
                Component amount,
                Component quantity,
                OptionalInt chance,
                List<Component> details) {
            this(resource, material, name, icons, role, main, amount, quantity, chance, details, resource.toString());
        }

        public Port(
                DataKey resource,
                String material,
                Component name,
                List<ItemStack> icons,
                Role role,
                boolean main,
                Component amount,
                OptionalInt chance,
                List<Component> details) {
            this(resource, material, name, icons, role, main, amount, Component.empty(), chance, details);
        }

        @Override
        public List<ItemStack> icons() {
            return icons.stream().map(ItemStack::copy).toList();
        }

        public boolean hasIcons() {
            return !icons.isEmpty();
        }

        public int iconCount() {
            return icons.size();
        }

        public ItemStack icon() {
            return icons.isEmpty() ? ItemStack.EMPTY : icons.get(0).copy();
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

        /**
         * Structural identity for topology and folding. Concrete native states compare by display
         * identity, while selectors retain their producer-assigned resource key even when their
         * first icon is also the representative of another selector.
         */
        public String semanticIdentity() {
            return resource.path().startsWith("resource/native/selector/") ? resource.toString() : identity;
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

    /** All exact source/target associations for one resource, stored without allocating their cross-product. */
    public record Flow(DataKey resource, List<DataKey> sources, List<DataKey> targets) {
        public Flow {
            Objects.requireNonNull(resource);
            sources = List.copyOf(sources);
            targets = List.copyOf(targets);
        }

        public long connectionCount() {
            return (long) sources.size() * targets.size();
        }
    }

    /** All ports belong to one step. Process is the producer's stable category, independent of the translated name. */
    public record Step(
            DataKey reference,
            Component name,
            ItemStack icon,
            List<Port> inputs,
            List<Port> outputs,
            List<Component> details,
            java.util.Optional<SpecialProcess> specialProcess,
            boolean resolved,
            boolean worldGenerated,
            boolean genericRecovery,
            String process) {
        public Step {
            Objects.requireNonNull(reference);
            name = name.copy();
            icon = icon.copy();
            var validInputs = validPorts(inputs, false, reference);
            var validOutputs = validPorts(outputs, true, reference);
            resolved &= validInputs.size() == inputs.size() && validOutputs.size() == outputs.size();
            inputs = validInputs;
            outputs = validOutputs;
            details = copyText(details);
            Objects.requireNonNull(specialProcess);
            Objects.requireNonNull(process);
        }

        @Override
        public ItemStack icon() {
            return icon.copy();
        }

        public Step(
                DataKey reference,
                Component name,
                ItemStack icon,
                List<Port> inputs,
                List<Port> outputs,
                List<Component> details,
                java.util.Optional<SpecialProcess> specialProcess,
                boolean resolved,
                boolean worldGenerated,
                boolean genericRecovery) {
            this(
                    reference,
                    name,
                    icon,
                    inputs,
                    outputs,
                    details,
                    specialProcess,
                    resolved,
                    worldGenerated,
                    genericRecovery,
                    name.getString());
        }

        public Step(
                DataKey reference,
                Component name,
                ItemStack icon,
                List<Port> inputs,
                List<Port> outputs,
                List<Component> details,
                java.util.Optional<SpecialProcess> specialProcess,
                boolean resolved,
                boolean worldGenerated) {
            this(reference, name, icon, inputs, outputs, details, specialProcess, resolved, worldGenerated, false);
        }

        public Step(
                DataKey reference,
                Component name,
                ItemStack icon,
                List<Port> inputs,
                List<Port> outputs,
                List<Component> details,
                java.util.Optional<SpecialProcess> specialProcess,
                boolean resolved) {
            this(reference, name, icon, inputs, outputs, details, specialProcess, resolved, false);
        }

        public Step(
                DataKey reference,
                Component name,
                List<Port> inputs,
                List<Port> outputs,
                List<Component> details,
                java.util.Optional<SpecialProcess> specialProcess,
                boolean resolved) {
            this(reference, name, ItemStack.EMPTY, inputs, outputs, details, specialProcess, resolved, false);
        }

        public Step(
                DataKey reference,
                Component name,
                List<Port> inputs,
                List<Port> outputs,
                List<Component> details,
                boolean resolved) {
            this(
                    reference,
                    name,
                    ItemStack.EMPTY,
                    inputs,
                    outputs,
                    details,
                    java.util.Optional.empty(),
                    resolved,
                    false);
        }
    }

    /** Categories describe consumed material flow; catalysts and presence conditions cannot establish it. */
    public enum Section {
        SOURCES("gt6ce.jei.material_process.section.sources"),
        INTERNAL("gt6ce.jei.material_process.section.internal"),
        RECOVERY("gt6ce.jei.material_process.section.recovery"),
        USES("gt6ce.jei.material_process.section.uses");

        private final String label;

        Section(String label) {
            this.label = label;
        }

        public Component label() {
            return Component.translatable(label);
        }
    }

    public static java.util.Optional<Section> section(Step step, String material) {
        if (step.genericRecovery()) return java.util.Optional.of(Section.RECOVERY);
        boolean input = step.inputs().stream()
                .anyMatch(
                        port -> port.role() == Role.CONSUMED && port.material().equals(material));
        boolean output =
                step.outputs().stream().anyMatch(port -> port.material().equals(material));
        if (input && output) return java.util.Optional.of(Section.INTERNAL);
        boolean otherInput = step.inputs().stream()
                .anyMatch(
                        port -> port.role() == Role.CONSUMED && !port.material().equals(material));
        if (output && !input && (step.worldGenerated() || otherInput)) return java.util.Optional.of(Section.SOURCES);
        boolean otherOutput =
                step.outputs().stream().anyMatch(port -> !port.material().equals(material));
        if (input && !output && otherOutput) return java.util.Optional.of(Section.USES);
        return java.util.Optional.empty();
    }

    /** Stable method identity plus its translated display name. */
    public record SpecialProcess(DataKey id, Component name) {
        public SpecialProcess {
            Objects.requireNonNull(id);
            name = name.copy();
        }
    }

    /** Related viewer pages are navigation targets, with no processing ports or graph edges. */
    public record Information(
            DataKey reference, Component name, ItemStack icon, List<Component> details, List<Port> index) {
        public Information {
            Objects.requireNonNull(reference);
            name = name.copy();
            icon = icon.copy();
            details = copyText(details);
            index = List.copyOf(index);
        }

        @Override
        public ItemStack icon() {
            return icon.copy();
        }
    }

    /**
     * Bind callbacks to the same query session/generation as the page. Revalidate before resolving native references;
     * return false for expired or unsupported targets. Implementations run on the client thread.
     */
    public interface Navigation {
        default boolean valid() {
            return true;
        }

        Navigation NONE = new Navigation() {
            @Override
            public boolean openForm(DataKey resource, boolean uses) {
                return false;
            }
        };

        default boolean openPort(Port port, boolean uses) {
            return port.hasIcons() && openStack(port.icon(), uses);
        }

        default boolean openStack(ItemStack stack, boolean uses) {
            return false;
        }

        default boolean openInformation(DataKey reference) {
            return false;
        }

        boolean openForm(DataKey resource, boolean uses);
    }

    /** Coverage and truncation reasons are always visible through the footer tooltip, including for empty queries. */
    public record Page(
            Component title,
            String material,
            List<Step> steps,
            List<Connection> connections,
            List<Component> notices,
            int number,
            int total,
            Navigation navigation,
            List<Information> information,
            List<Flow> flows) {
        public Page {
            title = title.copy();
            material = Objects.requireNonNullElse(material, "");
            int suppliedSteps = steps.size(), suppliedConnections = connections.size(), suppliedFlows = flows.size();
            steps = validSteps(steps);
            var ports = new PortIndex(steps);
            connections = validConnections(connections, ports);
            flows = validFlows(flows, ports);
            notices = copyText(notices);
            if (notices.isEmpty())
                notices = List.of(Component.translatable("gt6ce.jei.material_process.partial_unknown"));
            int discarded = suppliedSteps
                    - steps.size()
                    + suppliedConnections
                    - connections.size()
                    + suppliedFlows
                    - flows.size();
            if (discarded > 0) {
                var coverage = new ArrayList<>(notices);
                coverage.add(Component.translatable("gt6ce.jei.material_process.coverage.invalid_data", discarded));
                notices = List.copyOf(coverage);
            }
            information = List.copyOf(information);
            Objects.requireNonNull(navigation);
            number = Math.max(1, number);
            total = Math.max(number, total);
        }

        public Page(
                Component title,
                String material,
                List<Step> steps,
                List<Connection> connections,
                List<Component> notices,
                int number,
                int total,
                Navigation navigation,
                List<Information> information) {
            this(title, material, steps, connections, notices, number, total, navigation, information, List.of());
        }

        public Page(
                Component title,
                String material,
                List<Step> steps,
                List<Connection> connections,
                List<Component> notices,
                int number,
                int total,
                Navigation navigation) {
            this(title, material, steps, connections, notices, number, total, navigation, List.of());
        }

        public Page withInformation(List<Information> information) {
            return new Page(
                    title, material, steps, connections, notices, number, total, navigation, information, flows);
        }

        /** Foreign reagents and byproducts belong to the recipe, not to this material's viewer index. */
        public List<Port> indexedPorts() {
            return java.util.stream.Stream.concat(
                            steps.stream()
                                    .flatMap(step -> java.util.stream.Stream.concat(
                                            step.inputs().stream(), step.outputs().stream())),
                            information.stream().flatMap(entry -> entry.index().stream()))
                    .filter(port -> port.material().equals(material) && port.hasIcons())
                    .toList();
        }
    }

    private MaterialProcessDisplay() {}

    /** Build one material page without dropping steps. Panning belongs to the viewer, not to data paging. */
    public static List<Page> pages(
            Component title,
            String material,
            List<Step> steps,
            List<Connection> connections,
            List<Component> notices,
            Navigation navigation) {
        return List.of(new Page(title, material, steps, connections, notices, 1, 1, navigation));
    }

    public static List<Page> unbound() {
        return pages(
                Component.translatable("gt6ce.jei.material_process.title"),
                "",
                List.of(),
                List.of(),
                List.of(Component.translatable("gt6ce.jei.material_process.unbound")),
                Navigation.NONE);
    }

    private static boolean isOutput(Role role) {
        return role == Role.PRIMARY || role == Role.BYPRODUCT || role == Role.UNKNOWN;
    }

    private static List<Port> validPorts(List<Port> ports, boolean output, DataKey reference) {
        var valid = ports.stream()
                .filter(Objects::nonNull)
                .filter(port -> isOutput(port.role()) == output)
                .toList();
        if (valid.size() != ports.size())
            LOGGER.warn(
                    "Discarded {} invalid {} ports for {}",
                    ports.size() - valid.size(),
                    output ? "output" : "input",
                    reference);
        return valid;
    }

    private static List<Step> validSteps(List<Step> steps) {
        var references = new LinkedHashMap<DataKey, Step>();
        for (Step step : steps) {
            if (step == null) {
                LOGGER.warn("Discarded null material process step");
            } else if (references.putIfAbsent(step.reference(), step) != null) {
                LOGGER.warn("Discarded duplicate material process step {}", step.reference());
            }
        }
        return List.copyOf(references.values());
    }

    private static final class PortIndex {
        private final java.util.Map<DataKey, java.util.Set<DataKey>> inputs = new HashMap<>(),
                outputs = new HashMap<>();

        private PortIndex(List<Step> steps) {
            for (Step step : steps) {
                inputs.put(step.reference(), resources(step.inputs()));
                outputs.put(step.reference(), resources(step.outputs()));
            }
        }

        private boolean contains(DataKey step, DataKey resource, boolean output) {
            return (output ? outputs : inputs)
                    .getOrDefault(step, java.util.Set.of())
                    .contains(resource);
        }
    }

    private static List<Connection> validConnections(List<Connection> connections, PortIndex ports) {
        var unique = new HashSet<Connection>();
        var valid = new ArrayList<Connection>();
        for (Connection connection : connections) {
            if (connection == null
                    || !ports.contains(connection.source(), connection.resource(), true)
                    || !ports.contains(connection.target(), connection.resource(), false)) {
                LOGGER.warn("Discarded material process connection with missing endpoint ports: {}", connection);
            } else if (unique.add(connection)) valid.add(connection);
        }
        return List.copyOf(valid);
    }

    private static List<Flow> validFlows(List<Flow> flows, PortIndex ports) {
        var valid = new ArrayList<Flow>();
        for (Flow flow : flows) {
            var sources = flow.sources().stream()
                    .filter(source -> ports.contains(source, flow.resource(), true))
                    .distinct()
                    .toList();
            var targets = flow.targets().stream()
                    .filter(target -> ports.contains(target, flow.resource(), false))
                    .distinct()
                    .toList();
            if (sources.size() != flow.sources().size()
                    || targets.size() != flow.targets().size())
                LOGGER.warn("Discarded invalid material process flow endpoints for {}", flow.resource());
            if (!sources.isEmpty() && !targets.isEmpty()) valid.add(new Flow(flow.resource(), sources, targets));
        }
        return List.copyOf(valid);
    }

    private static java.util.Set<DataKey> resources(List<Port> ports) {
        var result = new HashSet<DataKey>();
        ports.forEach(port -> result.add(port.resource()));
        return result;
    }

    private static List<Component> copyText(List<Component> lines) {
        return lines.stream().map(line -> (Component) line.copy()).toList();
    }
}
