package gregtech.api.addon.recipe;

import java.util.List;

/** A material amount, measured in GTValues.U, with explicitly permitted physical forms. */
public record MaterialUnitIngredient(String material, long amount, List<String> forms, Rounding rounding) {
    public enum Rounding {
        /** Consume whole items; floor the number of complete products, without a remainder buffer. */
        WHOLE_OUTPUTS,
        /** Use the smallest whole-item batch whose item/fluid outputs are all integral. */
        EXACT_RATIO
    }

    public MaterialUnitIngredient {
        if (material == null || material.isBlank() || amount <= 0 || rounding == null)
            throw new IllegalArgumentException("Material unit ingredient");
        forms = List.copyOf(forms);
        if (forms.isEmpty() || forms.stream().anyMatch(String::isBlank))
            throw new IllegalArgumentException("Material forms must be explicit");
    }
}
