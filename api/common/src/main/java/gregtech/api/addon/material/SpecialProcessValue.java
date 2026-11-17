/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

import java.util.List;
import java.util.Objects;

/**
 * A target or reagent rule for one declared special process method. Values are caller-owned and
 * must be treated as immutable after registration.
 */
public sealed interface SpecialProcessValue permits SpecialProcessValue.Target, SpecialProcessValue.Reagent {
    /**
     * One processable material's recipe plan. The process rule's form set names accepted inputs;
     * {@link #inputCounts()} supplies the exact item count for every accepted form.
     */
    record Target(
            java.util.Map<FormKey, Integer> inputCounts,
            List<ItemOutput> itemOutputs,
            List<FluidOutput> fluidOutputs,
            long reagentAmount,
            long duration,
            long euPerTick)
            implements SpecialProcessValue {
        public Target {
            inputCounts = java.util.Map.copyOf(inputCounts);
            if (inputCounts.isEmpty()) throw new IllegalArgumentException("Special process target has no input form");
            for (var entry : inputCounts.entrySet()) {
                Objects.requireNonNull(entry.getKey(), "input form");
                if (entry.getValue() < 1) throw new IllegalArgumentException("Invalid special process input count");
            }
            itemOutputs = List.copyOf(itemOutputs);
            fluidOutputs = List.copyOf(fluidOutputs);
            if (itemOutputs.isEmpty() && fluidOutputs.isEmpty())
                throw new IllegalArgumentException("Special process target has no output");
            if (reagentAmount < 1 || duration < 1 || euPerTick < 0)
                throw new IllegalArgumentException("Invalid special process target quantities");
        }
    }

    /** Marks one reagent material and its accepted forms through the enclosing process rule. */
    record Reagent() implements SpecialProcessValue {}

    /** One output material and ordered form alternatives. The first available form wins. */
    record ItemOutput(MaterialKey material, List<FormCount> forms, int chance) {
        public ItemOutput {
            Objects.requireNonNull(material, "output material");
            forms = List.copyOf(forms);
            if (forms.isEmpty()) throw new IllegalArgumentException("Item output has no form");
            if (chance < 1 || chance > 10_000) throw new IllegalArgumentException("Invalid output chance");
        }
    }

    /**
     * One certain fluid output. The first available phase wins and always emits {@link #millibuckets()}.
     * The legacy {@code forms} component retains its counts for compatibility. Fluid output uses only
     * the phase names in that list. Use {@link #FluidOutput(MaterialKey, int, List)} for new declarations.
     */
    record FluidOutput(MaterialKey material, List<FormCount> forms, int millibuckets) {
        public FluidOutput {
            Objects.requireNonNull(material, "output material");
            forms = List.copyOf(forms);
            if (forms.isEmpty() || millibuckets < 1)
                throw new IllegalArgumentException("Invalid fluid output");
        }

        public FluidOutput(MaterialKey material, int millibuckets, List<FormKey> phases) {
            this(material, phases.stream().map(phase -> new FormCount(phase, 1)).toList(), millibuckets);
        }

        public List<FormKey> phases() {
            return forms.stream().map(FormCount::form).toList();
        }
    }

    /** A form and the item count emitted in that form. */
    record FormCount(FormKey form, int count) {
        public FormCount {
            Objects.requireNonNull(form, "form");
            if (count < 1) throw new IllegalArgumentException("Form count must be positive");
        }
    }
}
