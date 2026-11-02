/* SPDX-License-Identifier: LGPL-3.0-or-later */
package gregtech.api.multiblock;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Per-cell transport restrictions. Omitted channels retain the role's original permissions. */
public record StructurePorts(Map<PortChannel, PortRule> rules) {
    public static final StructurePorts DEFAULT = new StructurePorts(Map.of());

    public StructurePorts {
        rules = Map.copyOf(rules);
        rules.forEach((channel, rule) -> {
            if (channel.energy() && (!rule.slots().isEmpty() || !rule.filters().isEmpty()))
                throw new IllegalArgumentException("Energy port rules only restrict faces: " + channel);
        });
    }

    public PortRule rule(PortChannel channel) { return rules.getOrDefault(channel, PortRule.ALL); }

    public void validate(StructureRole role) {
        for (var channel : rules.keySet())
            if (!role.allows(channel))
                throw new IllegalArgumentException("Role " + role + " has no " + channel + " port");
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private final Map<PortChannel, PortRule> rules = new EnumMap<>(PortChannel.class);
        public Builder restrict(PortChannel channel, PortRule rule) {
            if (rules.putIfAbsent(Objects.requireNonNull(channel), Objects.requireNonNull(rule)) != null)
                throw new IllegalArgumentException("Duplicate port rule: " + channel);
            return this;
        }
        public Builder itemInput(PortRule rule) { return restrict(PortChannel.ITEM_INPUT, rule); }
        public Builder itemOutput(PortRule rule) { return restrict(PortChannel.ITEM_OUTPUT, rule); }
        public Builder fluidInput(PortRule rule) { return restrict(PortChannel.FLUID_INPUT, rule); }
        public Builder fluidOutput(PortRule rule) { return restrict(PortChannel.FLUID_OUTPUT, rule); }
        public Builder energyInput(PortRule rule) { return restrict(PortChannel.ENERGY_INPUT, rule); }
        public Builder energyOutput(PortRule rule) { return restrict(PortChannel.ENERGY_OUTPUT, rule); }
        public StructurePorts build() { return new StructurePorts(rules); }
    }
}
