/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.platform;

import gregtech.api.addon.InstalledService;
import gregtech.api.addon.material.GregTechMaterials;
import gregtech.api.energy.EnergyType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Platform entry installed by GregTech before material initialization.
 * Fluid stacks and recipe rows that name a loader fluid type live on {@code AddonLoader} instead.
 */
public final class AddonPlatform {
    private static final InstalledService<Services> INSTALLED = new InstalledService<>();

    private AddonPlatform() {}

    /** GT bootstrap only. Addons must not install or replace the host implementation. */
    public static void bind(Services implementation) {
        INSTALLED.bind(implementation, "addon platform services",
                "GregTech addon platform API is already installed");
    }

    public static Services services() {
        return INSTALLED.get("GregTech addon platform API is not installed");
    }

    /** GT implementation contract; addon consumers use the items, tools and energy facades. */
    public interface Services {
        /** Host bridge for AddonStructures; addons must not implement or call Services. */
        void structureChanged(Level level, BlockPos pos);

        ItemStack stack(String prefix, String material, int count);

        /** Integrated circuit {@code configuration}, or an empty stack when that item is not registered yet. */
        ItemStack selectorTag(int configuration);

        boolean matchesTool(ItemStack stack, AddonTool tool);

        void damageTool(ItemStack stack, int amount, LivingEntity user, EquipmentSlot slot);

        long inject(EnergyBlock block, EnergyType energy, @Nullable Direction side, long size, long amount, boolean execute);

        long extract(EnergyBlock block, EnergyType energy, @Nullable Direction side, long size, long amount, boolean execute);

        /** Prefix and material units already stored for this stack, or empty when the stack is unknown. */
        GregTechMaterials.StackComposition composition(ItemStack stack);
    }
}
