/* Gregorius Techneticies / GregTech-6 Team source behavior, LGPL-3.0-or-later. */
package com.gregtech.gregtech.event;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.MaterialEquivalence;
import com.gregtech.gregtech.content.recipe.MaterialWashingRules;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.recipe.CraftingMaterialForms;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** GT_API_Proxy world END/LOWEST item listeners, including tagged items from other mods. */
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID)
public final class MaterialWashingHandler {
    private MaterialWashingHandler() {}
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void tick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) wash(level);
    }

    public static void wash(ServerLevel level) {
        // Snapshot existing items so newly dropped outputs do not enter this tick's listener pass.
        var items = new ArrayList<ItemEntity>();
        for (var entity : level.getAllEntities()) if (entity instanceof ItemEntity item && item.isAlive()) items.add(item);
        for (var item : items) {
            if (!item.isAlive()) continue;
            var input = item.getItem();
            var form = MaterialEquivalence.form(input);
            if (form == null || !form.material().isValid()) continue;
            var row = MaterialWashingRules.row(form.prefix());
            if (row == null) continue;
            var cell = MaterialWashingRules.cell(item.getX(), item.getY(), item.getZ());
            var pos = new BlockPos(cell.x(), cell.y(), cell.z());
            var state = level.getBlockState(pos);
            if (!state.is(Blocks.WATER_CAULDRON)) continue;
            var material = row.outputMaterial(form.material());
            var output = output(row.output(), material);
            var step = MaterialWashingRules.step(input.getCount(), state.getValue(LayeredCauldronBlock.LEVEL), !output.isEmpty());
            if (step == null) continue;
            LayeredCauldronBlock.lowerFillLevel(state, level, pos);
            if (row.byproduct() != null && row.givesByproduct(level.random.nextInt(row.chance()))) {
                int size = material.getByProducts().size();
                var byproduct = output(row.byproduct(), MaterialWashingRules.byproductMaterial(material,
                        size == 0 ? 0 : level.random.nextInt(size)));
                if (!byproduct.isEmpty()) drop(level, item, byproduct);
            }
            drop(level, item, output);
            item.setDeltaMovement(Vec3.ZERO);
            item.setPos(cell.x() + .5, cell.y() + .9, cell.z() + .5);
            if (step.remainingCount() == 0) item.discard();
            else {
                item.setItem(input.copyWithCount(step.remainingCount()));
                item.setPickUpDelay(40); // GT_API_Proxy resets the remaining source entity after a listener changes it.
            }
        }
    }
    private static ItemStack output(MaterialPrefix prefix, GTMaterial material) {
        var result = GTItems.getStack(prefix, material, 1);
        return result.isEmpty() ? CraftingMaterialForms.stack(prefix.getName(), material, 1) : result;
    }
    private static void drop(ServerLevel level, ItemEntity input, ItemStack output) {
        level.addFreshEntity(new ItemEntity(level, input.getX(), input.getY(), input.getZ(), output.copy()));
    }
}
