package com.gregtech.gregtech.integration.client;

import com.google.gson.JsonObject;
import com.gregtech.gregtech.content.multiblock.OriginalMultiblockPartData;
import com.gregtech.gregtech.client.CommonBlockTooltips;
import com.gregtech.gregtech.data.BlockHarvestPolicy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import java.util.List;

final class MultiblockPartDeliveryChecks {
    static JsonObject verify() {
        int parts = 0, rows = 0;
        for (var source : OriginalMultiblockPartData.ALL) {
            var id = ResourceLocation.parse("gregtech:" + source.path());
            require(BuiltInRegistries.ITEM.containsKey(id), "Original part registered " + id);
            var stack = new ItemStack(BuiltInRegistries.ITEM.get(id));
            var block = ((BlockItem) stack.getItem()).getBlock();
            require(block instanceof com.gregtech.gregtech.block.machine.MultiblockPortBlock, "Native port including niobium titanium coil " + id);
            var lines = stack.getTooltipLines(Item.TooltipContext.EMPTY,null,TooltipFlag.NORMAL);
            for (var key : OriginalMultiblockPartData.TOOLTIP_KEYS) {
                var found = lines.stream().filter(c -> CommonBlockTooltips.containsKey(List.of(c), key)).toList();
                require(found.size() == 1 && found.get(0).getStyle().getColor().equals(
                        net.minecraft.network.chat.TextColor.fromLegacyFormat(net.minecraft.ChatFormatting.DARK_GRAY)), "Single source tool row/color " + id + "/" + key);
                require(net.minecraft.client.resources.language.I18n.exists(key), "Loaded source language " + key);
                rows++;
            }
            require(block.getExplosionResistance() == source.resistance()
                    && block.defaultBlockState().getDestroySpeed(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO) == source.hardness(), "Actual original hardness / blast resistance " + id);
            for (var side : net.minecraft.core.Direction.values())
                require(block.defaultBlockState().getFlammability(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,BlockPos.ZERO,side) == source.flammability()
                        && block.defaultBlockState().getFireSpreadSpeed(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,BlockPos.ZERO,side) == source.flammability(), "Source fire properties on every side " + id);
            require(lines.stream().filter(c -> CommonBlockTooltips.containsKey(List.of(c),"gt.lang.flammable")).count() == (source.flammability() > 0 ? 1 : 0), "Wood wall has one original flammability row " + id);
            require(BlockHarvestPolicy.source(block).orElseThrow().sourceId() == source.originalId(), "Exact source harvest identity " + id);
            var expected = com.gregtech.gregtech.content.machine.MachineConstructionMaterials.block(source.path()).orElseThrow();
            var actual = com.gregtech.gregtech.api.material.ItemMaterialRegistry.get(stack).orElseThrow();
            require(actual.components().equals(expected.components()), "Actual known material quantities " + id);
            require(!CommonBlockTooltips.containsKey(lines,"tooltip.gregtech.contained_materials")
                    && stack.getTooltipLines(Item.TooltipContext.EMPTY,null,TooltipFlag.ADVANCED).stream().filter(c -> CommonBlockTooltips.containsKey(List.of(c),"tooltip.gregtech.contained_materials")).count() == 1,
                    "One advanced material section " + id);
            require(net.minecraft.client.resources.language.I18n.exists("block.gregtech." + source.path()), "Original part name " + id);
            parts++;
        }
        require(parts == 45 && rows == 90,"All 45 source parts /two rows");
        var result = new JsonObject(); result.addProperty("parts",parts); result.addProperty("sourceTooltipRows",rows);
        result.addProperty("scope","Actual installed tooltip/material/physical registration; structure binding and player hover are separate");
        return result;
    }
    private static void require(boolean ok,String message) { if (!ok) throw new IllegalStateException(message); }
}
