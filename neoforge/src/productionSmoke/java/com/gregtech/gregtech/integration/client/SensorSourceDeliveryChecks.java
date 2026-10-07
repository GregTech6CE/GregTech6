package com.gregtech.gregtech.integration.client;

import com.google.gson.JsonObject;
import com.gregtech.gregtech.api.material.ItemMaterialRegistry;
import com.gregtech.gregtech.api.sensor.SensorCatalog;
import com.gregtech.gregtech.client.CommonBlockTooltips;
import com.gregtech.gregtech.content.machine.MachineConstructionMaterials;
import com.gregtech.gregtech.data.BlockHarvestPolicy;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import java.util.List;

final class SensorSourceDeliveryChecks {
    static JsonObject verify() {
        int items = 0, rows = 0;
        for (var entry : SensorCatalog.ALL) {
            var id = ResourceLocation.parse("gregtech:" + entry.id());
            require(BuiltInRegistries.ITEM.containsKey(id), "Source sensor item " + id);
            var stack = new ItemStack(BuiltInRegistries.ITEM.get(id));
            var block = ((BlockItem)stack.getItem()).getBlock();
            require(block instanceof com.gregtech.gregtech.block.sensor.SensorBlock sensor && sensor.kind().name().equals(entry.kind()), "Actual sensor source kind " + id);
            var lines = stack.getTooltipLines(Item.TooltipContext.EMPTY,null,TooltipFlag.NORMAL);
            var keys = List.of(entry.descriptionKey(), "gt.lang.nogui.rightclick.interact", "gt.tooltip.sensor.screwdrive.buttons",
                    "gt.tooltip.sensor.screwdrive.display", "gt.tooltip.sensor.screwdrive.modes", "gt.lang.use.monkey.wrench.to.set.input.side", "gt.lang.use.x.to.toggle.facing.pre");
            for (int i = 0; i < keys.size(); i++) {
                var key = keys.get(i);
                var line = lines.stream().filter(c -> CommonBlockTooltips.containsKey(List.of(c), key)).findFirst().orElseThrow();
                require(count(lines,key) == 1 && net.minecraft.client.resources.language.I18n.exists(key), "Loaded source row once " + id + "/" + key);
                require(line.getStyle().getColor().equals(net.minecraft.network.chat.TextColor.fromLegacyFormat(
                        i == 0 ? ChatFormatting.AQUA : i == 1 ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY)), "Source row color " + id + "/" + key);
                rows++;
            }
            require(count(lines,"gt.tooltip.sensor.common") == 0, "Old generic hint replaced " + id);
            require(block.getExplosionResistance() == 3.0f && BlockHarvestPolicy.handHarvestable(block)
                    && BlockHarvestPolicy.source(block).orElseThrow().sourceId() == entry.originalId(), "Original native physical / source harvest metadata " + id);
            require(ItemMaterialRegistry.get(stack).orElseThrow().components().equals(MachineConstructionMaterials.block(entry.id()).orElseThrow().components()), "Actual known sensor materials " + id);
            require(count(lines,"tooltip.gregtech.contained_materials") == 0
                    && count(stack.getTooltipLines(Item.TooltipContext.EMPTY,null,TooltipFlag.ADVANCED),"tooltip.gregtech.contained_materials") == 1, "Single advanced quantity section " + id);
            require(net.minecraft.client.resources.language.I18n.exists("block.gregtech." + entry.id()), "Loaded source name " + id);
            items++;
        }
        require(items == 21 && rows == 147, "All original sensors /seven rows each");
        var result = new JsonObject();
        result.addProperty("items",items); result.addProperty("sourceTooltipRows",rows);
        result.addProperty("scope","Installed native tooltip events and material/harvest registration only; measurements, player hover and save restart are separate");
        return result;
    }
    private static long count(List<Component> lines,String key) { return lines.stream().filter(c -> CommonBlockTooltips.containsKey(List.of(c),key)).count(); }
    private static void require(boolean ok,String message) { if (!ok) throw new IllegalStateException(message); }
}
