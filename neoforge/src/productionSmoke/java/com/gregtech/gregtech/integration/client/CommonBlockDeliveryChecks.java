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
        int overlappingMachines = 0;
        var checkedSourcePaths = new LinkedHashSet<String>();
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
            String path = BuiltInRegistries.BLOCK.getKey(block).getPath();
            boolean fixed = SourceBlockProperties.block(path).isPresent();
            if (fixed) { sourceBlocks++; checkedSourcePaths.add(path); }
            // Some named source blocks are also BasicMachineBlock instances; these sets overlap.
            if (block instanceof com.gregtech.gregtech.block.machine.BasicMachineBlock machine
                    && SourceBlockProperties.basic(machine.basicSpec().machineName(),machine.basicSpec().tier()).isPresent()) {
                sourceMachines++;
                if (fixed) overlappingMachines++;
            }
        }
        var missing = new LinkedHashSet<>(SourceBlockProperties.blocks().keySet());
        missing.removeAll(checkedSourcePaths);
        System.err.println("SOURCE_BLOCK_TOOLTIP_COVERAGE blocks="+sourceBlocks+" basicMachines="+sourceMachines
                +" overlap="+overlappingMachines+" missing="+missing);
        require(sourceBlocks == 1311 && missing.isEmpty() && sourceMachines >= 250,
                "all adopted original metadata identities present: blocks="+sourceBlocks+", basicMachines="+sourceMachines+", missing="+missing);
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
        result.addProperty("overlappingNamedBasicMachines",overlappingMachines);
        result.addProperty("nativeSavedCoverFaces",storedCovers);
        result.addProperty("scope","native installed item tooltip events, source policy and sparse saved face data; no hover screenshot, actual harvest or world restart claim");
        result.add("storageTooltips",verifyStorage());
        return result;
    }

    private static JsonObject verifyStorage() {
        int safes=0,drawers=0,tables=0,crates=0,mass=0,hoppers=0,shelves=0,scaffolds=0;
        for (var item : BuiltInRegistries.ITEM) {
            if (!(item instanceof BlockItem nativeItem)) continue;
            if (!BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("gregtech")) continue;
            var type=nativeItem.getBlock();
            if (!(type instanceof com.gregtech.gregtech.block.inventory.SafeBlock
                    || type instanceof com.gregtech.gregtech.block.inventory.DrawerQuadBlock
                    || type instanceof com.gregtech.gregtech.block.misc.AdvancedCraftingTableBlock
                    || type instanceof com.gregtech.gregtech.block.inventory.BottleCrateBlock
                    || type instanceof com.gregtech.gregtech.block.inventory.MassStorageBlock
                    || type instanceof com.gregtech.gregtech.block.BookShelfBlock
                    || type instanceof com.gregtech.gregtech.block.tool.ScaffoldBlock
                    || item instanceof com.gregtech.gregtech.block.machine.HopperBlockItem
                    || item instanceof com.gregtech.gregtech.block.machine.QueueHopperBlockItem)) continue;
            var block=nativeItem.getBlock(); var stack=new ItemStack(item); var lines=tooltip(stack);
            boolean facing=false;
            if (block instanceof com.gregtech.gregtech.block.BookShelfBlock shelf) {
                require(countKey(lines,"gt.lang.nogui.rightclick.interact")==1
                        && countKey(lines,"gt.lang.use.pincers.to.take")==1
                        && countKey(lines,"gt.lang.use.magnifyingglass.to.detail")==1,"original shelf interaction rows");
                require(lines.stream().anyMatch(row->CommonBlockTooltips.containsKey(List.of(row),"gt.lang.nogui.rightclick.interact")
                        && row.getStyle().getColor().getValue()==net.minecraft.ChatFormatting.GOLD.getColor()),"original shelf no-GUI orange tone");
                require(countKey(lines,"tooltip.gregtech.machine.nogui.click_front")==0
                        && countKey(lines,"tooltip.gregtech.smeltery.tool.pincers")==0
                        && countKey(lines,"tooltip.gregtech.machine.tool.magnifying_glass")==0,"obsolete guessed shelf rows absent");
                if(shelf.variant().metal())require(BlockHarvestPolicy.source(block).orElseThrow().sourceId()==shelf.variant().originalId(),"actual metal shelf source ID");
                shelves++;facing=true;
            } else if (block instanceof com.gregtech.gregtech.block.tool.ScaffoldBlock) {
                require(BlockHarvestPolicy.level(block)==0 && !BlockHarvestPolicy.handHarvestable(block),"actual scaffold explicit0 machine harvest rule");
                scaffolds++;facing=true;
            } else if (block instanceof com.gregtech.gregtech.block.inventory.SafeBlock safe) {
                require(countKey(lines,safe.keyLocked()?"gt.lang.key.controlled":"gt.lang.owner.controlled")==1,"source lock rule once "+stack);
                require(countKey(lines,"gt.tooltip.safe.1")==0 && countKey(lines,"gt.tooltip.safe.2")==0,"old guessed safe rows removed");
                safes++;facing=true;
            } else if (block instanceof com.gregtech.gregtech.block.inventory.DrawerQuadBlock) {
                require(countKey(lines,"gt.lang.use.monkey.wrench.to.toggle.inputs")==1,"source drawer input access tool");
                drawers++;facing=true;
            } else if (block instanceof com.gregtech.gregtech.block.misc.AdvancedCraftingTableBlock) {
                require(countKey(lines,"gt.lang.use.monkey.wrench.to.toggle.inputs")==1
                        && countKey(lines,"gt.lang.use.screwdriver.to.toggle")==1,"both source crafting table controls including charging inheritance");
                tables++;facing=true;
            } else if (block instanceof com.gregtech.gregtech.block.inventory.BottleCrateBlock) {
                require(countKey(lines,"gt.lang.nogui.rightclick.interact")==1,"source bottlecrate direct interaction row");
                crates++;facing=true;
            } else if (block instanceof com.gregtech.gregtech.block.inventory.MassStorageBlock) {
                require(countKey(lines,"gt.multitileentity.massstorage.tooltip.1")==1
                        && countKey(lines,"gt.multitileentity.massstorage.tooltip.2")==1,"source mass capacity/table rows");
                require(countKey(lines,"gt.lang.use.tape")==1 && countKey(lines,"gt.lang.use.untape")==0,"default untaped storage tools");
                mass++;facing=true;
            }
            var hopper=item instanceof com.gregtech.gregtech.block.machine.HopperBlockItem h?h.spec():
                    item instanceof com.gregtech.gregtech.block.machine.QueueHopperBlockItem q?q.spec():null;
            if (hopper!=null) {
                boolean queue=item instanceof com.gregtech.gregtech.block.machine.QueueHopperBlockItem;
                require(number(lines,"gt.multitileentity.hopper.tooltip.1",Math.max(queue?2:1,hopper.slotCount())),"actual source hopper inventory size "+stack);
                require(countKey(lines,"gt.multitileentity.hopper.tooltip.2")== (queue?1:0),"default saved hopper stack limit visibility");
                if(queue)require(number(lines,"gt.multitileentity.hopper.tooltip.2",64),"default queue source size64");
                require(com.gregtech.gregtech.data.BlockHarvestPolicy.level(nativeItem.getBlock())==0,"explicit0 native hopper metadata");
                var data=new CompoundTag();data.putByte("gt.mode",(byte)16);data.putBoolean("gt.exact",true);write(stack,data);
                var configured=tooltip(stack);
                require(number(configured,"gt.multitileentity.hopper.tooltip.2",16)
                        && countKey(configured,"gt.multitileentity.hopper.tooltip.3")== (queue?0:1),"actual saved16 and applicable exact flag");
                require(read(stack).equals(data),"hover does not change saved hopper mode");
                hoppers++;facing=true;
            }
            if(facing)require(countKey(lines,"gt.lang.use.x.to.toggle.facing.pre")==1,"source facing row appears once "+stack);
        }
        require(safes==120 && drawers==60 && tables==120 && hoppers==120,"all original native storage/tool families");
        require(mass==121 && crates==61+com.gregtech.gregtech.content.storage.BottleCrateVariants.WOODS.size(),"existing ordinary/logistics/legacy and wooden crate variants");
        require(shelves==com.gregtech.gregtech.content.book.BookShelfVariants.all().size() && scaffolds==60,"all actual native shelf/scaffold variants");
        var packed=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("gregtech:mass_storage_steel")));
        var template=new ItemStack(Items.APPLE); name(template,Component.literal("Storage probe"));
        for (int mode : new int[]{0,8}) for (long count : new long[]{0,1_000_257}) {
            var data=new CompoundTag();data.putInt("gt.mode",mode);data.putLong("gt.stored",count);data.put("gt.template",save(template));write(packed,data);
            var lines=tooltip(packed);
            require(lines.stream().anyMatch(row->row.getString().contains("Storage probe") && row.getString().endsWith(": "+count)),"actual saved template name/count including retained zero filter and legacy overflow");
            require(countKey(lines,"gt.lang.use.untape")== (mode==8?1:0)
                    && countKey(lines,"gt.lang.use.tape")== (mode==8?0:1)
                    && countKey(lines,"gt.lang.use.soft.hammer.to.reset")== (mode==8?0:1),"actual tape state controls");
            require(read(packed).equals(data) && !com.gregtech.gregtech.api.material.ItemMaterialRegistry.canRecover(packed),"hover preserves stored items and recovery rejects them");
        }
        var safe=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("gregtech:safe")));
        for (boolean explicit : new boolean[]{false,true}) {
            var data=new CompoundTag();
            data.putInt("gt.mode",0);if (explicit) data.putString("gt.dungeonloot","");write(safe,data);
            require(tooltip(safe).stream().noneMatch(row->row.getString().startsWith("Contains Loot of ")),"empty saved loot name has no phantom loot row");
            data.remove("gt.dungeonloot");write(safe,data);
            require(tooltip(safe).stream().noneMatch(row->row.getString().startsWith("Contains Loot of ")),"absent saved loot name has no phantom loot row");
        }
        var loot=new CompoundTag();loot.putString("gt.dungeonloot","minecraft:chests/simple_dungeon");write(safe,loot);
        require(countKey(tooltip(safe),"loot.dungeonChest")==1 && read(safe).equals(loot),"saved original loot label without generating its contents");
        loot.putString("gt.dungeonloot","other:custom");write(safe,loot);
        require(tooltip(safe).stream().anyMatch(row->row.getString().endsWith("other:custom")),"unknown table retains actual identity");
        var result=new JsonObject();result.addProperty("safes",safes);result.addProperty("drawers",drawers);result.addProperty("craftingTables",tables);
        result.addProperty("bottleCrates",crates);result.addProperty("massStorages",mass);result.addProperty("hoppers",hoppers);
        result.addProperty("bookShelves",shelves);result.addProperty("scaffolds",scaffolds);
        result.addProperty("savedMassConfigurations",4);result.addProperty("scope","actual native tooltip calls/events and saved item state; no world interaction or screen hover claim");return result;
    }
    private static boolean number(List<Component> lines,String key,int value) {
        return lines.stream().anyMatch(line->CommonBlockTooltips.containsKey(List.of(line),key) && line.getString().endsWith(Integer.toString(value)));
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
        require(lines.stream().filter(line -> line.getString().equals(row)).count() == 1,
                "single source harvest row " + stack + "; expected=" + row + "; actual=" + lines);
        require(countKey(lines,"gt.lang.blastresistance") <= 1, "blast row appended once " + stack);
        require(countKey(lines,"gt.lang.flammable") <= 1 && countKey(lines,"tooltip.gregtech.flammable") == 0,
                "single canonical source flame row " + stack);
        require(countKey(lines,"tooltip.gregtech.machine.harvest_wrench") == 0
                && countKey(lines,"tooltip.gregtech.machine.harvest.tool_label") == 0
                && countKey(lines,"tooltip.gregtech.crucible.harvest_pickaxe") == 0
                && countKey(lines,"tooltip.gregtech.wire.harvest_tool") == 0, "obsolete generic harvest instructions removed " + stack);
        if (block instanceof com.gregtech.gregtech.block.BookShelfBlock)
            require(countKey(lines,"gt.lang.enchantment.bonus") == 1,"original bookshelf interface hint");
        var second = new ArrayList<>(lines); CommonBlockTooltips.append(stack,second);
        require(second.stream().filter(line -> line.getString().equals(row)).count() == 1,"shared rows do not duplicate if reapplied");
    }
    private static long countKey(List<Component> lines,String key) {
        return lines.stream().filter(line -> CommonBlockTooltips.containsKey(List.of(line),key)).count();
    }
    // At the title screen no world/data-pack enchantment registry has been loaded.
    // EMPTY is Minecraft's supported context for tooltip calls without a world.
    private static List<Component> tooltip(ItemStack stack) { return stack.getTooltipLines(Item.TooltipContext.EMPTY,null,TooltipFlag.NORMAL); }
    private static CompoundTag save(ItemStack stack) { return (CompoundTag)stack.save(net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)); }
    private static void write(ItemStack stack,CompoundTag data) { stack.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.of(data)); }
    private static CompoundTag read(ItemStack stack) { return stack.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA).copyTag(); }
    private static void name(ItemStack stack,Component text) { stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,text); }
    private static void require(boolean value,String message) { if (!value) throw new IllegalStateException(message); }
}
