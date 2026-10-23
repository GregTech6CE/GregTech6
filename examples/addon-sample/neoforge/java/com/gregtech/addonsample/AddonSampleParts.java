package com.gregtech.addonsample;

import com.gregtech.gregtech.api.addon.LayoutPart;
import com.gregtech.gregtech.api.addon.PartSlots;
import com.gregtech.gregtech.api.addon.platform.LayoutPartContributor;
import com.gregtech.gregtech.api.addon.platform.PartBehavior;
import com.gregtech.gregtech.api.addon.platform.PartContext;
import com.gregtech.gregtech.api.addon.platform.PartHit;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Four locked slots. Redstone is 1 power, diamond is 4. A controller starts and stops it. */
public final class AddonSampleParts implements LayoutPartContributor {
    static final LayoutPart PART = LayoutPart.builder("sample_compute", "Steel").hardness(6.0F)
            .slots(PartSlots.builder(4).stackLimit(1).build())
            .tick().use().field("power").field("running").build();

    @Override public String addonId() { return "addon_sample"; }

    @Override public Set<String> partNames() { return Set.of("sample_compute"); }

    @Override public PartBehavior create(String partName) {
        if (!"sample_compute".equals(partName)) throw new IllegalArgumentException(partName);
        return new ComputePart();
    }

    public static final class ComputePart implements PartBehavior {
        private int idle;

        public boolean tryStart(PartContext context, int needed) {
            if (context.state().getBoolean("running")) return power(context) >= needed;
            if (power(context) < needed) return false;
            context.state().setBoolean("running", true);
            return true;
        }

        public void stop(PartContext context) {
            if (context.state().getBoolean("running")) context.state().setBoolean("running", false);
        }

        @Override public void tick(PartContext context) {
            if (++idle < 20) return;
            idle = 0;
            if (context.controller() == null) stop(context);
        }

        @Override public boolean onUse(PartContext context, Player player, InteractionHand hand, PartHit hit) {
            if (context.state().getBoolean("running")) return true;
            int slot = slotOf(hit);
            if (slot < 0) return false;
            ItemStack held = player.getItemInHand(hand);
            ItemStack stored = context.inventoryAccess().snapshot(slot);
            if (held.isEmpty()) {
                if (stored.isEmpty()) return false;
                context.inventoryAccess().set(slot, ItemStack.EMPTY);
                if (!player.getInventory().add(stored)) player.drop(stored, false);
                return true;
            }
            if (powerOf(held) == 0 || !stored.isEmpty()) return false;
            ItemStack one = held.copy();
            one.setCount(1);
            context.inventoryAccess().set(slot, one);
            if (!player.getAbilities().instabuild) held.shrink(1);
            return true;
        }

        @Override public boolean canInsert(PartContext context, int slot, ItemStack stack) {
            return !context.state().getBoolean("running");
        }

        @Override public boolean canExtract(PartContext context, int slot) {
            return !context.state().getBoolean("running");
        }

        @Override public void onSlotsChanged(PartContext context, int slot) {
            int sum = 0;
            for (int index = 0; index < context.slots(); index++) sum += powerOf(context.inventoryAccess().snapshot(index));
            context.state().setInt("power", sum);
        }

        @Override public List<Component> describe(PartContext context) {
            int filled = 0;
            for (int index = 0; index < context.slots(); index++)
                if (!context.inventoryAccess().snapshot(index).isEmpty()) filled++;
            var lines = new ArrayList<Component>();
            lines.add(Component.literal(filled + "/" + context.slots()));
            lines.add(Component.literal("power " + context.state().getInt("power")));
            lines.add(Component.literal(context.state().getBoolean("running") ? "running" : "stopped"));
            return lines;
        }

        private static int power(PartContext context) { return context.state().getInt("power"); }

        private static int slotOf(PartHit hit) {
            Direction face = hit.face();
            if (face.getAxis() == Direction.Axis.Y) return -1;
            double fraction = face.getAxis() == Direction.Axis.X ? hit.z() : hit.x();
            int band = (int) Math.floor(fraction * 4.0);
            if (band < 0) return 0;
            return Math.min(band, 3);
        }

        private static int powerOf(ItemStack stack) {
            if (stack.is(Items.REDSTONE)) return 1;
            if (stack.is(Items.DIAMOND)) return 4;
            return 0;
        }
    }
}
