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
            require(item.getMaxStackSize() == 1, "installed filled barrel stack limit");
            clean++; stored++;
        }
        require(clean == 35 && stored == 35, "installed existing tank identities");
        int cheap = 0;
        for (var entry : com.gregtech.gregtech.content.transport.fluid.CheapWoodBarrelCatalog.ENTRIES) {
            var item = new ItemStack(BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("gregtech:" + entry.id())));
            require(item.getMaxStackSize() == 16, "installed empty barrel stack limit");
            var block = (TankBlock) ((net.minecraft.world.item.BlockItem) item.getItem()).getBlock();
            require(block.spec().capacity() == 8000 && block.spec().maxTemperature() == 340, "installed cheap barrel specification");
            var data = ItemMaterialRegistry.get(item).orElseThrow();
            require(data.components().size() == 2 && data.components().stream().anyMatch(c -> c.material() == entry.rod() && c.amount() == com.gregtech.gregtech.api.material.GTValues.U * 2), "installed distinct cheap rod composition");
            require(com.gregtech.gregtech.content.creative.SourceCreativeCatalog.entry(entry.id()) != null, "installed source creative entry");
            var model = net.minecraft.client.Minecraft.getInstance().getItemRenderer().getModel(item, null, null, 0);
            require(model != net.minecraft.client.Minecraft.getInstance().getModelManager().getMissingModel(), "installed cheap barrel item model");
            cheap++;
        }
        var logistics = new ItemStack(BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("gregtech:logistics_tank")));
        require(ItemMaterialRegistry.get(logistics).orElseThrow().components().size() == 7, "installed complete known logistics composition");
        var result = new JsonObject();
        result.addProperty("cheapSourceVariants", cheap);
        result.addProperty("knownLogisticsComponents", 7);
        result.addProperty("cleanItemData", clean);
        result.addProperty("storedItemDataProtected", stored);
        result.addProperty("completeWorldSaveRetained", true);
        result.addProperty("observedEmptyLoaderWrappers", emptyLoaderWrappers);
        result.addProperty("scope", "installed methods at title screen; actual harvest tested separately in native server worlds");
        return result;
    }
}
