package gregtech.internal.tool;

import gregtech.content.tool.MaterialToolEnchantments;
//? if >=1.21.1 {
/*import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
*///?} else {
import net.minecraft.core.registries.BuiltInRegistries;
//?}
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
*///?} else {
import net.minecraft.world.item.enchantment.EnchantmentHelper;
//?}

/** Applies the original material table to real enchantment NBT, retaining player enchantments. */

/** 1.21 enchantments use the active data-pack registry, never detached holders. */
public final class GTToolEnchantments {
    private GTToolEnchantments() {}
    private static final String APPLIED = "gt.material_enchantments.v1";
//? if >=1.21.1 {
    /*private static volatile java.util.function.Supplier<HolderLookup.Provider> clientLookup = () -> null;

    public static void setClientLookup(java.util.function.Supplier<HolderLookup.Provider> lookup) {
        clientLookup = java.util.Objects.requireNonNull(lookup);
    }

    /^* Integrated-server holders cannot be encoded against the client connection's registry. ^/
    public static HolderLookup.Provider currentLookup() {
        if (net.neoforged.fml.util.thread.EffectiveSide.get().isClient()) return clientLookup.get();
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        return server == null ? null : server.registryAccess();
    }

    public static void applyCurrent(ItemStack stack) {
        var lookup = currentLookup();
        if (lookup != null) apply(stack, lookup);
    }
    public static void apply(ItemStack stack, HolderLookup.Provider lookup) {
        if (stack.isEmpty()) return;
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!data.contains(GTToolHelper.ROOT)) return;
        var registry = lookup.lookupOrThrow(Registries.ENCHANTMENT);
        var existing = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        var enchantments = new ItemEnchantments.Mutable(existing);
        boolean rebound = false;
        // Copies may outlive a world or cross from the integrated server's catalogue to its client.
        // Resolve by key without reapplying the material bonus or losing player levels/tooltip flags.
        for (var entry : existing.entrySet()) {
            var previous = entry.getKey();
            var key = previous.unwrapKey();
            if (key.isEmpty()) continue;
            var current = registry.getOrThrow(key.get());
            if (current == previous) continue;
            enchantments.removeIf(holder -> holder == previous);
            enchantments.upgrade(current, entry.getIntValue());
            rebound = true;
        }
        if (data.getBoolean(APPLIED)) {
            if (rebound) stack.set(DataComponents.ENCHANTMENTS, enchantments.toImmutable());
            return;
        }
*///?} else {
    public static void apply(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag() || !stack.getTag().contains(GTToolHelper.ROOT)
                || stack.getTag().getBoolean(APPLIED)) return;
        var enchantments = EnchantmentHelper.getEnchantments(stack);
//?}
        var material=GTToolHelper.getHead(stack);
        java.util.Map<String,Integer> applicable;
        if(stack.getItem() instanceof gregtech.content.item.ElectricToolItem electric) {
            String original=electric.definition().original();
            var flags=gregtech.content.tool.OriginalToolFlags.of(original);
            applicable=MaterialToolEnchantments.of(flags.mining(),flags.weapon(),flags.ranged(),material);
        } else applicable=MaterialToolEnchantments.of(GTToolHelper.getType(stack).definition(),material);
        for (var entry : applicable.entrySet()) {
//? if >=1.21.1 {
            /*var key = ResourceKey.<Enchantment>create(Registries.ENCHANTMENT, ResourceLocation.parse(MaterialToolEnchantments.id(entry.getKey())));
            registry.get(key).ifPresent(holder -> enchantments.upgrade(holder, entry.getValue()));
*///?} else {
            var enchantment = BuiltInRegistries.ENCHANTMENT.get(ResourceLocation.parse(MaterialToolEnchantments.id(entry.getKey())));
            if (enchantment != null) enchantments.merge(enchantment, entry.getValue(), Math::max);
//?}
        }
//? if >=1.21.1 {
        /*stack.set(DataComponents.ENCHANTMENTS, enchantments.toImmutable());
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(APPLIED, true));
*///?} else {
        EnchantmentHelper.setEnchantments(enchantments, stack);
        stack.getOrCreateTag().putBoolean(APPLIED, true);
//?}
    }
}
