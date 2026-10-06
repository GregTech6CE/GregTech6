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

        int clean = 0, stored = 0, emptyLoaderWrappers = 0;
        for (var block : BuiltInRegistries.BLOCK) if (block instanceof TankBlock) {
            var tank = (TankBlockEntity) ((TankBlock) block).newBlockEntity(BlockPos.ZERO, block.defaultBlockState());
            CompoundTag empty = tank.saveItemData();
            require(empty.isEmpty(), "installed sparse empty tank item data " + BuiltInRegistries.BLOCK.getKey(block));
            var full = tank.saveWithoutMetadata();
            require(full.contains("gt.temperature") && full.contains("gt.tank"), "installed complete world save");
            for (String key : java.util.List.of("ForgeCaps", "ForgeData"))
                if (full.contains(key, net.minecraft.nbt.Tag.TAG_COMPOUND) && full.getCompound(key).isEmpty()) emptyLoaderWrappers++;
            tank.getFluidTank().fill(new net.minecraftforge.fluids.FluidStack(Fluids.WATER, 61), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            CompoundTag data = tank.saveItemData();
            require(data.contains("gt.tank") && data.getCompound("gt.tank").getLong("Amount") == 61, "installed tank contents retained");
            var item = new ItemStack(block);
            item.addTagElement("BlockEntityTag", data);
            require(ItemMaterialRegistry.hasStoredContents(item) && !ItemMaterialRegistry.canRecover(item), "installed stored shell guard");
            require(item.getMaxStackSize() == 1, "installed filled barrel stack limit");
            var tooltipData = data.copy();
            tooltipData.getCompound("gt.tank").putLong("Capacity", 14789);
            tooltipData.putBoolean("gt.soft_hammer", true);
            tooltipData.putLong("gt.sealed_time", 91);
            var tooltipItem = new ItemStack(block);
            tooltipItem.addTagElement("BlockEntityTag", tooltipData);
            verifyStoredTankTooltip(tooltipItem);
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
        result.add("machineSourceMaterials", verifyMachineMaterials());
        result.addProperty("sourceFluidPipeTooltips", verifyFluidPipeTooltips());
        result.addProperty("storedTankTooltips", stored);
        return result;
    }

    private static JsonObject verifyMachineMaterials() {
        int blocks = 0, machines = 0;
        for (var entry : com.gregtech.gregtech.content.machine.MachineConstructionMaterials.blocks().entrySet()) {
            var id = net.minecraft.resources.ResourceLocation.parse("gregtech:" + entry.getKey());
            require(BuiltInRegistries.ITEM.containsKey(id), "installed source material item exists " + id);
            verifyComposition(new ItemStack(BuiltInRegistries.ITEM.get(id)), entry.getValue());
            blocks++;
        }
        for (var block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof com.gregtech.gregtech.block.machine.BasicMachineBlock machine)) continue;
            var spec = machine.basicSpec();
            var expected = com.gregtech.gregtech.content.machine.OriginalMachineMaterialData.find(spec.machineName(), spec.tier());
            if (expected.isEmpty()) continue;
            verifyComposition(new ItemStack(block), expected.get());
            machines++;
        }
        var out = new JsonObject();
        out.addProperty("sourceBlockIdentities", blocks);
        out.addProperty("nativeBasicMachinesAndControllers", machines);
        out.addProperty("singleAdvancedMaterialSection", true);
        out.addProperty("exactSourceComponentAmounts", true);
        out.addProperty("scope", "installed item methods and tooltip event at title screen; no recovery gameplay claim");
        return out;
    }
    private static void verifyComposition(ItemStack stack, com.gregtech.gregtech.api.material.ItemComposition expected) {
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        var actual = ItemMaterialRegistry.get(stack).orElseThrow(() -> new IllegalStateException("installed source material data " + id));
        require(actual.components().equals(expected.components()), "installed exact source components " + id);
        var lines = tooltip(stack, net.minecraft.world.item.TooltipFlag.ADVANCED);
        String header = net.minecraft.network.chat.Component.translatable("tooltip.gregtech.contained_materials").getString();
        require(lines.stream().filter(header::equals).count() == 1, "installed single material header " + id);
        for (var part : expected.components()) {
            String prefix = com.gregtech.gregtech.client.MaterialTooltips.displayUnits(part.amount()) + " "
                    + com.gregtech.gregtech.api.material.MaterialPresentation.name(part.material()).getString();
            require(lines.stream().filter(line -> line.startsWith(prefix + " (") || line.startsWith(prefix + " ")).count() == 1,
                    "installed exact source quantity tooltip " + id + " " + prefix);
        }
        require(tooltip(stack, net.minecraft.world.item.TooltipFlag.NORMAL).stream().noneMatch(header::equals),
                "installed quantities require advanced tooltips " + id);
    }
    private static void verifyStoredTankTooltip(ItemStack item) {
        var lines = tooltip(item, net.minecraft.world.item.TooltipFlag.NORMAL);
        require(lines.stream().anyMatch(line -> line.replace(",", "").replace("_", "").contains("61 L of ")
                && line.replace(",", "").replace("_", "").contains("Max: 14789 L)")), "installed saved barrel amount/capacity tooltip");
        require(lines.contains("Sealed (91)"), "installed saved barrel seal tooltip");
        for (String key : java.util.List.of("gt.lang.nogui.funnel.tap.tank", "gt.lang.no.powerconducting.fluids"))
            require(lines.contains(net.minecraft.network.chat.Component.translatable(key).getString()), "installed barrel original hint " + key);
    }
    private static int verifyFluidPipeTooltips() {
        int pipes = 0;
        for (var block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof com.gregtech.gregtech.block.machine.FluidPipeBlock pipe)) continue;
            var lines = tooltip(new ItemStack(block), net.minecraft.world.item.TooltipFlag.NORMAL);
            String bandwidth = net.minecraft.network.chat.Component.translatable("gt.lang.pipe.stats.bandwidth").getString()
                    + pipe.spec().bandwidthPerTank() + " L/t";
            require(lines.stream().anyMatch(line -> line.replace(",", "").replace("_", "").equals(bandwidth)), "installed per-tank source bandwidth");
            if (pipe.spec().tankCount() > 1) require(lines.contains(net.minecraft.network.chat.Component.translatable("gt.lang.pipe.stats.amount").getString()
                    + pipe.spec().tankCount()), "installed source channel count");
            String connection = net.minecraft.network.chat.Component.translatable("gt.lang.use.x.to.toggle.connection.pre")
                    .append(net.minecraft.network.chat.Component.translatable("gt.lang.tool.name.wrench"))
                    .append(net.minecraft.network.chat.Component.translatable("gt.lang.use.x.to.toggle.connection.post")).getString();
            require(lines.contains(connection), "installed source wrench pipe connection hint");
            pipes++;
        }
        require(pipes == 406, "installed fluid pipe tooltip identities");
        return pipes;
    }
    private static java.util.List<String> tooltip(ItemStack stack, net.minecraft.world.item.TooltipFlag flag) {
        return stack.getTooltipLines(null, flag).stream().map(net.minecraft.network.chat.Component::getString).toList();
    }
}
