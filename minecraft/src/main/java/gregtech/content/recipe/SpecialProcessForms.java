package gregtech.content.recipe;

import gregtech.api.addon.material.FormKey;
import gregtech.api.addon.material.MaterialFormBinding;
import gregtech.api.addon.material.MaterialFormKind;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/** Resolves published forms for special-process recipes and their tags. */
public final class SpecialProcessForms {
    private static final Comparator<FormKey> BY_NAME = Comparator.comparing(FormKey::name);

    private SpecialProcessForms() {}

    public static Item item(MaterialFormBinding binding) {
        if (binding.kind() == MaterialFormKind.FLUID || isFluid(binding.form())) return Items.AIR;
        var id = ResourceLocation.tryParse(binding.id());
        if (id == null) return Items.AIR;
        if (binding.kind() == MaterialFormKind.BLOCK)
            return BuiltInRegistries.BLOCK.get(id).asItem();
        return BuiltInRegistries.ITEM.get(id);
    }

    public static Fluid fluid(MaterialFormBinding binding) {
        if (binding.kind() != MaterialFormKind.FLUID || !isFluid(binding.form())) return Fluids.EMPTY;
        var id = ResourceLocation.tryParse(binding.id());
        return id == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.get(id);
    }

    public static List<FormKey> reagentForms(Set<MaterialFormBinding> bindings, Set<FormKey> declared) {
        if (!declared.isEmpty()) return declared.stream().sorted(BY_NAME).toList();
        var items = new TreeSet<>(BY_NAME);
        var blocks = new TreeSet<>(BY_NAME);
        var fluids = new TreeSet<>(BY_NAME);
        for (var binding : bindings) {
            if (item(binding) != Items.AIR) {
                var forms = binding.kind() == MaterialFormKind.BLOCK ? blocks : items;
                forms.add(binding.form());
            }
            if (fluid(binding) != Fluids.EMPTY) fluids.add(binding.form());
        }
        // An unrestricted reagent needs a single unit: item count or millibuckets.
        if (!fluids.isEmpty()) return items.isEmpty() && blocks.isEmpty() ? List.copyOf(fluids) : List.of();
        // Keep the original item reagent choice when a new block alternative is also available.
        blocks.removeAll(items);
        var forms = new ArrayList<>(items);
        forms.addAll(blocks);
        return List.copyOf(forms);
    }

    static boolean isFluid(FormKey form) {
        return form.name().equals("liquid")
                || form.name().equals("gas")
                || form.name().equals("molten");
    }
}
