package com.gregtech.gregtech.integration.client;

import com.google.gson.JsonObject;
import com.gregtech.gregtech.block.machine.TankBlock;
import com.gregtech.gregtech.blockentity.machine.TankBlockEntity;
import com.gregtech.gregtech.api.material.ItemMaterialRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;

/** Ordinary installed-JAR method checks at the title screen; no world/harvest claim. */
final class TankItemDeliveryChecks {
    private static void require(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
    static JsonObject verify() {
        var lookup = net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        int clean = 0, stored = 0, emptyLoaderWrappers = 0;
        for (var block : BuiltInRegistries.BLOCK) if (block instanceof TankBlock) {
            var tank = (TankBlockEntity) ((TankBlock) block).newBlockEntity(BlockPos.ZERO, block.defaultBlockState());
            CompoundTag empty = tank.saveItemData(lookup);
            require(empty.isEmpty(), "installed sparse empty tank item data " + BuiltInRegistries.BLOCK.getKey(block));
            var full = tank.saveWithoutMetadata(lookup);
            require(full.contains("gt.temperature") && full.contains("gt.tank"), "installed complete world save");
            for (String key : java.util.List.of("ForgeCaps", "ForgeData"))
                if (full.contains(key, net.minecraft.nbt.Tag.TAG_COMPOUND) && full.getCompound(key).isEmpty()) emptyLoaderWrappers++;
            tank.getFluidTank().fill(new net.neoforged.neoforge.fluids.FluidStack(Fluids.WATER, 61), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            CompoundTag data = tank.saveItemData(lookup);
            require(data.contains("gt.tank") && data.getCompound("gt.tank").getLong("Amount") == 61, "installed tank contents retained");
            var item = new ItemStack(block);
            item.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA, net.minecraft.world.item.component.CustomData.of(data));
            require(ItemMaterialRegistry.hasStoredContents(item) && !ItemMaterialRegistry.canRecover(item), "installed stored shell guard");
            clean++; stored++;
        }
        require(clean == 32 && stored == 32, "installed existing tank identities");
        var result = new JsonObject();
        result.addProperty("cleanItemData", clean);
        result.addProperty("storedItemDataProtected", stored);
        result.addProperty("completeWorldSaveRetained", true);
        result.addProperty("observedEmptyLoaderWrappers", emptyLoaderWrappers);
        result.addProperty("scope", "installed methods at title screen; actual harvest tested separately in native server worlds");
        return result;
    }
}
