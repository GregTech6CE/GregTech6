package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.tool.GTToolEnchantments;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.content.material.generated.WoodMaterials;
import com.gregtech.gregtech.item.GTToolItem;
import com.mojang.serialization.Lifecycle;
import io.netty.buffer.Unpooled;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Reproduces the creative-slot error using two registries with equal keys and distinct holders. */
@GameTestHolder("gt6ce_tool_enchantments")
@PrefixGameTestTemplate(false)
public final class ToolEnchantmentRegistryTests {
    private ToolEnchantmentRegistryTests() {}

    @GameTest(template = "empty", batch = "tool_enchantment_registries", timeoutTicks = 100)
    public static void materialToolPacketsUseCurrentRegistry(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        var registry = access.lookupOrThrow(Registries.ENCHANTMENT);
        require(GTToolEnchantments.currentLookup() == helper.getLevel().getServer().registryAccess(),
                "server tool creation selected client registries");
        var hammer = GTToolItem.create(GTToolType.HARD_HAMMER, Materials.Steel, WoodMaterials.Wood);
        require(hammer.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)
                .getLevel(registry.getOrThrow(Enchantments.SHARPNESS)) == 2, "steel hammer lost material Sharpness");
        encodeCreativeSlot(hammer, access);
        var knife = GTToolItem.create(GTToolType.KNIFE, Materials.Flint, WoodMaterials.Wood);
        require(knife.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)
                .getLevel(registry.getOrThrow(Enchantments.FIRE_ASPECT)) == 1, "flint knife lost material Fire Aspect");
        encodeCreativeSlot(knife, access);
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "tool_enchantment_registries", timeoutTicks = 100)
    public static void appliedToolsRebindWithoutLosingPlayerEnchantments(GameTestHelper helper) {
        var original = helper.getLevel().registryAccess();
        var originalEnchantments = original.registryOrThrow(Registries.ENCHANTMENT);
        var replacement = new MappedRegistry<Enchantment>(Registries.ENCHANTMENT, Lifecycle.stable());
        originalEnchantments.holders().forEach(holder ->
                Registry.register(replacement, holder.unwrapKey().orElseThrow(), holder.value()));
        replacement.freeze();
        var registries = new java.util.ArrayList<Registry<?>>();
        original.registries().forEach(entry -> registries.add(entry.key().equals(Registries.ENCHANTMENT) ? replacement : entry.value()));
        var access = new RegistryAccess.ImmutableRegistryAccess(registries).freeze();
        var registry = access.lookupOrThrow(Registries.ENCHANTMENT);
        require(originalEnchantments.getHolderOrThrow(Enchantments.SHARPNESS) != registry.getOrThrow(Enchantments.SHARPNESS),
                "probe did not produce a foreign holder");

        var hammer = GTToolItem.create(GTToolType.HARD_HAMMER, Materials.Steel, WoodMaterials.Wood);
        hammer.enchant(originalEnchantments.getHolderOrThrow(Enchantments.SHARPNESS), 5);
        hammer.enchant(originalEnchantments.getHolderOrThrow(Enchantments.UNBREAKING), 3);
        hammer.set(DataComponents.ENCHANTMENTS, hammer.get(DataComponents.ENCHANTMENTS).withTooltip(false));
        GTToolEnchantments.apply(hammer, access); // The material-applied marker is already true.
        requireCanonical(hammer, registry);
        var repaired = hammer.get(DataComponents.ENCHANTMENTS);
        require(repaired.getLevel(registry.getOrThrow(Enchantments.SHARPNESS)) == 5, "repair lost player Sharpness");
        require(repaired.getLevel(registry.getOrThrow(Enchantments.UNBREAKING)) == 3, "repair lost player Unbreaking");
        require(repaired.equals(repaired.withTooltip(false)), "repair changed enchantment tooltip visibility");
        GTToolEnchantments.apply(hammer, access);
        require(hammer.get(DataComponents.ENCHANTMENTS).equals(repaired), "repeated repair changed enchantments");
        encodeCreativeSlot(hammer, access);
        helper.succeed();
    }

    private static void requireCanonical(ItemStack stack, HolderLookup.RegistryLookup<Enchantment> registry) {
        for (var holder : stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).keySet())
            require(holder == registry.getOrThrow(holder.unwrapKey().orElseThrow()), "foreign holder remains on " + stack);
    }

    private static void encodeCreativeSlot(ItemStack stack, RegistryAccess access) {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), access);
        try {
            ServerboundSetCreativeModeSlotPacket.STREAM_CODEC.encode(buffer, new ServerboundSetCreativeModeSlotPacket(36, stack));
            var decoded = ServerboundSetCreativeModeSlotPacket.STREAM_CODEC.decode(buffer);
            require(ItemStack.isSameItemSameComponents(stack, decoded.itemStack()), "creative-slot codec changed tool components");
        } finally {
            buffer.release();
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
