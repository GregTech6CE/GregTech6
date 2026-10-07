package com.gregtech.gregtech.integration.client;

import com.google.gson.JsonObject;
import com.gregtech.gregtech.api.material.ItemMaterialRegistry;
import com.gregtech.gregtech.client.CommonBlockTooltips;
import com.gregtech.gregtech.content.machine.MachineConstructionMaterials;
import com.gregtech.gregtech.content.multiblock.OriginalUtilityControllerData;
import com.gregtech.gregtech.data.BlockHarvestPolicy;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import java.util.List;

final class UtilityControllerDeliveryChecks {
    static JsonObject verify() {
        int items = 0, rows = 0;
        for (var path : List.of("heat_exchanger_main", "lightning_rod_electric_output", "lightning_rod_main", "von_da_graagg_generator")) {
            boolean heat = path.equals("heat_exchanger_main");
            boolean inhibitor = path.equals("von_da_graagg_generator");
            var id = ResourceLocation.parse("gregtech:" + path);
            require(BuiltInRegistries.ITEM.containsKey(id), "Registered utility item " + id);
            var stack = new ItemStack(BuiltInRegistries.ITEM.get(id));
            var block = ((BlockItem) stack.getItem()).getBlock();
            var lines = stack.getTooltipLines(Item.TooltipContext.EMPTY, null, TooltipFlag.NORMAL);
            require(count(lines, "gt.lang.structure") == 1, "Source structure header once " + id);
            var keys = inhibitor ? OriginalUtilityControllerData.VON_DA_GRAAGG_STRUCTURE : heat ? OriginalUtilityControllerData.HEAT_STRUCTURE : OriginalUtilityControllerData.LIGHTNING_STRUCTURE;
            for (int i = 0; i < keys.size(); i++) {
                var key = keys.get(i);
                var color = inhibitor && i == 3 ? ChatFormatting.AQUA : !heat && i == 7 ? ChatFormatting.YELLOW : !heat && i == 8 ? ChatFormatting.GOLD : ChatFormatting.WHITE;
                require(count(lines, key) == 1 && net.minecraft.client.resources.language.I18n.exists(key)
                        && line(lines, key).getStyle().getColor().equals(net.minecraft.network.chat.TextColor.fromLegacyFormat(color)),
                        "Loaded source structure text / color " + key);
                rows++;
            }
            require(count(lines, "gt.lang.energy.input") == (inhibitor?1:0) && count(lines, "gt.lang.energy.output") == (inhibitor?0:1),
                    "Only original utility output energy row " + id);
            var output = line(lines, inhibitor ? "gt.lang.energy.input" : "gt.lang.energy.output");
            require(output.getStyle().getColor().equals(net.minecraft.network.chat.TextColor.fromLegacyFormat(heat ? ChatFormatting.RED : ChatFormatting.GREEN)),
                    "Source utility output header color " + id);
            require(count(lines, "gt.lang.recipes") == (heat ? 1 : 0)
                    && count(lines, "gt.lang.efficiency") == (heat ? 1 : 0)
                    && count(lines, "gt.lang.nogui.funnel.tap.tank") == (heat ? 1 : 0), "Source conditional recipe / efficiency / no-GUI rows " + id);
            if (inhibitor) {
                require(output.getString().contains("2048 EU/t (256 to 4096, ") && count(lines,"gt.lang.face.bottom")==1,
                        "Original bottom input recommended/min/max " + id);
                require(block.defaultBlockState().getDestroySpeed(null,net.minecraft.core.BlockPos.ZERO)==6 && block.getExplosionResistance()==6,
                        "Source galvanized steel controller hardness/resistance " + id);
            } else if (heat) {
                require(count(lines, "gt.recipe.fuels.hot") == 1 && net.minecraft.client.resources.language.I18n.exists("gt.recipe.fuels.hot")
                        && lines.stream().anyMatch(c -> c.getString().contains("100.00%")), "Source Hot Fuels and full efficiency " + id);
                require(count(lines, "gt.td.short.energy.heat") == 1 && output.getString().contains("16384 HU/t")
                        && !output.getString().contains("(") && !output.getString().contains(".."), "Fixed source heat output omits side and range " + id);
            } else {
                require(count(lines, "gt.td.short.energy.electricity") == 2 && output.getString().contains("32768 EU/p (up to 16 Amps)")
                        && lines.stream().anyMatch(c -> c.getString().equals("589824000 EU per Lightning Strike")), "Original hardcoded packet / amps / per-strike suffixes " + id);
                require(count(lines, "gt.tooltip.multiblock.lightningrod.output") == 0, "Superseded generic output removed " + id);
            }
            for (var key : List.of("gt.lang.use.builder.wand.to.ease.building", "gt.lang.use.magnifyingglass.to.detail", "gt.lang.use.x.to.toggle.facing.pre"))
                require(count(lines, key) == 1, "Inherited source tool hint once " + id);
            require(BlockHarvestPolicy.source(block).orElseThrow().sourceId() == (inhibitor ? 17996 : heat ? 17197 : 17998), "Actual source harvest identity " + id);
            require(ItemMaterialRegistry.get(stack).orElseThrow().components().equals(MachineConstructionMaterials.block(path).orElseThrow().components()),
                    "Actual precise controller material registration " + id);
            require(count(lines, "tooltip.gregtech.contained_materials") == 0
                    && count(stack.getTooltipLines(Item.TooltipContext.EMPTY, null, TooltipFlag.ADVANCED), "tooltip.gregtech.contained_materials") == 1,
                    "Only one advanced quantity section " + id);
            require(net.minecraft.client.resources.language.I18n.exists("block.gregtech." + path)
                    && stack.getHoverName().getString().equals(net.minecraft.client.resources.language.I18n.get("block.gregtech." + path)), "Original localized item name " + id);
            items++;
        }
        require(items == 4 && rows == 26, "Four actual utility identities /26 source structure rows");
        var result = new JsonObject();
        result.addProperty("items", items);
        result.addProperty("sourceStructureRows", rows);
        result.addProperty("scope", "Installed native tooltip calls/events and registration only; not fuel/strike generation, player hover or restart");
        return result;
    }
    private static Component line(List<Component> lines, String key) {
        return lines.stream().filter(c -> CommonBlockTooltips.containsKey(List.of(c), key)).findFirst().orElseThrow();
    }
    private static long count(List<Component> lines, String key) {
        return lines.stream().filter(c -> CommonBlockTooltips.containsKey(List.of(c), key)).count();
    }
    private static void require(boolean ok, String message) {
        if (!ok) throw new IllegalStateException(message);
    }
}
