package gregtech.engine.addon;

import gregtech.api.addon.material.GregTechMaterials;
import gregtech.api.addon.recipe.SelectorTags;
import gregtech.api.addon.platform.AddonPlatform;
import gregtech.api.addon.platform.AddonTool;
import gregtech.api.addon.platform.EnergyBlock;
import gregtech.engine.energy.EnergyBlockDefaults;
import gregtech.api.material.GTMaterial;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.ItemMaterialRegistry;
import gregtech.engine.prefix.PrefixRegistry;
import gregtech.engine.tool.GTToolHelper;
import gregtech.engine.tool.GTToolType;
import gregtech.content.data.GregTechTags;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTItems;
import gregtech.content.registry.GTTechnological;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;

/** SPDX-License-Identifier: LGPL-3.0-or-later. Installs the shared platform addon entry. */
public final class AddonPlatformServices {
    private AddonPlatformServices() {}

    public static void bind() {
        AddonPlatform.bind(new Services());
    }

    static GregTechTags.Tag tag(String energy) {
        if (energy == null) throw new IllegalArgumentException("Missing energy type");
        return GregTechTags.Energy.ALL.stream()
                .filter(candidate -> candidate.getShortName().equalsIgnoreCase(energy))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown energy type: " + energy));
    }

    private static final class Services implements AddonPlatform.Services {
        @Override public void structureChanged(Level level, BlockPos pos) {
            java.util.Objects.requireNonNull(level, "level");
            java.util.Objects.requireNonNull(pos, "pos");
            if (level.isClientSide) return;
            if (!(level instanceof ServerLevel server))
                throw new IllegalArgumentException("Structure notifications require a server level");
            if (!server.getServer().isSameThread())
                throw new IllegalStateException("Structure notifications require the server thread");
            gregtech.engine.multiblock.tracking.StructureWatchIndex.touch(server, pos);
        }

        @Override public ItemStack stack(String prefix, String material, int count) {
            if (prefix == null || material == null || count <= 0) return ItemStack.EMPTY;
            MaterialPrefix form = PrefixRegistry.byName(prefix);
            if (form == null) return ItemStack.EMPTY;
            GTMaterial resolved = GTMaterialRegistry.get(material);
            if (!resolved.isValid()) return ItemStack.EMPTY;
            ItemStack stack = GTItems.getStack(form, resolved, count);
            return stack == null ? ItemStack.EMPTY : stack;
        }

        @Override public ItemStack selectorTag(int configuration) {
            SelectorTags.require(configuration);
            Item item = GTTechnological.selectorTag(configuration);
            return item == null ? ItemStack.EMPTY : new ItemStack(item);
        }

        @Override public boolean matchesTool(ItemStack stack, AddonTool tool) {
            return stack != null && tool != null && GTToolHelper.matchesTool(stack, GTToolType.valueOf(tool.name()));
        }

        @Override public void damageTool(ItemStack stack, int amount, LivingEntity user, EquipmentSlot slot) {
            if (stack == null || slot == null) return;
            GTToolHelper.damageForUse(stack, amount, user, slot);
        }

        @Override public long inject(EnergyBlock block, String energy, Direction side, long size, long amount, boolean execute) {
            return EnergyBlockDefaults.doEnergyInjection(AddonEnergyAdapter.of(block), tag(energy), side, size, amount, execute);
        }

        @Override public long extract(EnergyBlock block, String energy, Direction side, long size, long amount, boolean execute) {
            return EnergyBlockDefaults.doEnergyExtraction(AddonEnergyAdapter.of(block), tag(energy), side, size, amount, execute);
        }

        @Override public GregTechMaterials.StackComposition composition(ItemStack stack) {
            if (stack == null || stack.isEmpty()) return GregTechMaterials.StackComposition.EMPTY;
            var data = ItemMaterialRegistry.get(stack).orElse(null);
            if (data == null || data.components().isEmpty()) return GregTechMaterials.StackComposition.EMPTY;
            String prefix = data.prefix() == null ? "" : data.prefix().getName();
            if (prefix.isEmpty() && stack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem
                    && blockItem.getBlock() instanceof gregtech.content.block.MaterialBlockLike like
                    && like.prefix() != null)
                prefix = like.prefix().compositionName();
            var units = new ArrayList<GregTechMaterials.Component>();
            for (var component : data.components()) {
                if (component.material() == null || !component.material().isValid() || component.amount() <= 0) continue;
                units.add(new GregTechMaterials.Component(component.material().resolve().getName(), component.amount()));
            }
            return GregTechMaterials.StackComposition.of(prefix, units);
        }
    }
}
