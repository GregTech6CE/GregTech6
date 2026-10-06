package com.gregtech.gregtech.integration.client;

import com.google.gson.JsonObject;
import com.gregtech.gregtech.client.CommonBlockTooltips;
import com.gregtech.gregtech.data.BlockHarvestPolicy;
import com.gregtech.gregtech.data.SourceBlockProperties;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import java.util.*;

/** Native tooltip event checks prepared for pooled installed-JAR acceptance; no world claim. */
final class CommonBlockDeliveryChecks {
    private CommonBlockDeliveryChecks() {}
    static JsonObject verify() {
        int sourceBlocks = 0, sourceMachines = 0;
        var representatives = new LinkedHashMap<Class<?>, BlockItem>();
        for (var item : BuiltInRegistries.ITEM) {
            if (!(item instanceof BlockItem blockItem)
                    || !BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("gregtech")) continue;
            var block = blockItem.getBlock();
            representatives.putIfAbsent(block.getClass(), blockItem);
            var source = BlockHarvestPolicy.source(block);
            if (source.isEmpty()) continue;
            verifyRows(new ItemStack(item));
            require(BlockHarvestPolicy.tool(block).name().equalsIgnoreCase(source.get().tool())
                    && BlockHarvestPolicy.level(block) == source.get().level()
                    && BlockHarvestPolicy.handHarvestable(block) == source.get().handHarvestable(),
                    "installed native policy uses original group and metadata " + BuiltInRegistries.ITEM.getKey(item));
            if (SourceBlockProperties.block(BuiltInRegistries.BLOCK.getKey(block).getPath()).isPresent()) sourceBlocks++;
            else sourceMachines++;
        }
        require(sourceBlocks == 591 && sourceMachines >= 250, "all adopted original metadata identities present");
        for (var item : representatives.values()) verifyRows(new ItemStack(item));
        int storedCovers = 0;
        var machine = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("gregtech:electric_motor_lv")));
        var emptyData = new CompoundTag(); emptyData.putBoolean("gt.component_covers",true);
        write(machine,emptyData);
        require(!CommonBlockTooltips.hasAttachedCover(machine) && !CommonBlockTooltips.containsKey(tooltip(machine),"gt.lang.use.crowbar.to.uncover"),
                "empty fallback cover marker has no crowbar row");
        for (String prefix : List.of("gt_cover_", "gt.logistics.cover.")) for (int side=0;side<6;side++) {
            var data = new CompoundTag(); data.put(prefix+side,save(new ItemStack(Items.REDSTONE_TORCH)));
            write(machine,data);
            require(CommonBlockTooltips.hasAttachedCover(machine), "actual native serialized cover on face " + side);
            var lines = tooltip(machine);
            require(countKey(lines,"gt.lang.use.crowbar.to.uncover") == 1, "one original crowbar hint for native saved cover");
            require(read(machine).equals(data), "tooltip preserves actual saved block item data");
            storedCovers++;
        }
        for (var face : List.of(save(new ItemStack(Items.DIRT)), new CompoundTag())) {
            var data = new CompoundTag(); data.put("gt_cover_0",face); write(machine,data);
            require(!CommonBlockTooltips.hasAttachedCover(machine), "empty or non-cover saved face does not invent crowbar instructions");
        }
        var zero = save(new ItemStack(Items.REDSTONE_TORCH)); zero.putInt("count",0);
        var data = new CompoundTag(); data.put("gt_cover_5",zero); write(machine,data);
        require(!CommonBlockTooltips.hasAttachedCover(machine), "zero-count cover is empty");
        var named = machine.copy();
        named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,Component.literal("Hand-Harvestable"));
        require(tooltip(named).get(0).getString().equals("Hand-Harvestable"),"player-renamed item title survives common row cleanup");
        var defaultFace = save(new ItemStack(Items.REDSTONE_TORCH)); defaultFace.remove("count");
        data = new CompoundTag(); data.put("gt_cover_3",defaultFace); write(machine,data);
        require(CommonBlockTooltips.hasAttachedCover(machine), "source native codec default count1 accepted");
        var foreign = new ItemStack(Items.STONE); var before = tooltip(foreign); var after = new ArrayList<>(before);
        CommonBlockTooltips.append(foreign,after);
        require(after.equals(before), "vanilla block tooltips untouched");
        var result = new JsonObject(); result.addProperty("sourceBlockAndHopperIdentities",sourceBlocks);
        result.addProperty("actualSourceBasicMachines",sourceMachines); result.addProperty("nativeBlockClasses",representatives.size());
        result.addProperty("nativeSavedCoverFaces",storedCovers);
        result.addProperty("scope","native installed item tooltip events, source policy and sparse saved face data; no hover screenshot, actual harvest or world restart claim");
        return result;
    }
    private static void verifyRows(ItemStack stack) {
        var block = ((BlockItem)stack.getItem()).getBlock();
        var lines = tooltip(stack);
        String row = CommonBlockTooltips.harvestLine(block).getString();
        int level = BlockHarvestPolicy.level(block);
        if (!BlockHarvestPolicy.handHarvestable(block) && level > 1) {
            var references = Map.of(2,"iron",3,"diamond",4,"netherite",5,"adamantium",15,"infinity");
            String suffix = references.containsKey(level)
                    ? " (" + level + ", " + Component.translatable("material.gregtech." + references.get(level)).getString() + ")"
                    : " (" + level + ")";
            require(row.endsWith(suffix), "source LH tier reference is a translated material name " + stack);
        }
        require(lines.stream().filter(line -> line.getString().equals(row)).count() == 1, "single source harvest row " + stack);
        require(countKey(lines,"gt.lang.blastresistance") <= 1, "blast row appended once " + stack);
        require(countKey(lines,"gt.lang.flammable") <= 1 && countKey(lines,"tooltip.gregtech.flammable") == 0,
                "single canonical source flame row " + stack);
        require(countKey(lines,"tooltip.gregtech.machine.harvest_wrench") == 0
                && countKey(lines,"tooltip.gregtech.machine.harvest.tool_label") == 0, "obsolete generic harvest instructions removed " + stack);
        if (block instanceof com.gregtech.gregtech.block.BookShelfBlock)
            require(countKey(lines,"gt.lang.enchantment.bonus") == 1,"original bookshelf interface hint");
        var second = new ArrayList<>(lines); CommonBlockTooltips.append(stack,second);
        require(second.stream().filter(line -> line.getString().equals(row)).count() == 1,"shared rows do not duplicate if reapplied");
    }
    private static long countKey(List<Component> lines,String key) {
        return lines.stream().filter(line -> CommonBlockTooltips.containsKey(List.of(line),key)).count();
    }
    private static List<Component> tooltip(ItemStack stack) { return stack.getTooltipLines(Item.TooltipContext.EMPTY,null,TooltipFlag.NORMAL); }
    private static CompoundTag save(ItemStack stack) { return (CompoundTag)stack.save(net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)); }
    private static void write(ItemStack stack,CompoundTag data) { stack.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.of(data)); }
    private static CompoundTag read(ItemStack stack) { return stack.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA).copyTag(); }
    private static void require(boolean value,String message) { if (!value) throw new IllegalStateException(message); }
}
