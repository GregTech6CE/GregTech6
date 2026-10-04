package com.gregtech.gregtech.integration.client;
import com.google.gson.*;
import com.gregtech.gregtech.worldgen.center.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;

/** Focused feedback checks in the one already-required fresh-world launch. */
public final class OriginFeedbackChecks {
    private OriginFeedbackChecks() {}
    private static ResourceLocation id(String name){return ResourceLocation.fromNamespaceAndPath("gregtech",name);}
    private static final java.util.List<String> failures=new java.util.concurrent.CopyOnWriteArrayList<>();
    private static void require(boolean value,String message){if(!value)failures.add(message);}
    public static void registry() {
        int[] facilities={7133,4033,6033,5033,8033,14999,26304,32048,32057,32062,32702,32703,32705,32707,32709,32711,32719,32722,32727,32732,32735,32737,32739,32744,32750,32764};
        var missing=new java.util.ArrayList<String>();
        for(int legacy:facilities)if(!BuiltInRegistries.BLOCK.containsKey(id(NativeOriginFacilities.block(legacy))))missing.add(legacy+":"+NativeOriginFacilities.block(legacy));
        if(!missing.isEmpty())throw new IllegalStateException("Missing original origin facilities: "+missing);
    }
    public static void server(MinecraftServer server,JsonObject receipt) {
        var level=server.overworld();int height=level.getSeaLevel()+3;
        String[] biomes={"snowy_plains","snowy_taiga","forest","plains","badlands","desert","jungle","swamp","river"};
        int[][] points={{-72,-72},{-40,-40},{-72,72},{-40,40},{72,-72},{40,-72},{40,40},{56,56},{8,40}};
        for(int i=0;i<biomes.length;i++) {
            var actual=level.getBiome(new BlockPos(points[i][0],height,points[i][1])).unwrapKey().orElseThrow().location().getPath();
            require(actual.equals(biomes[i]),"Original center biome mismatch: "+biomes[i]+" -> "+actual);
        }
        receipt.addProperty("originCenterBiomesChecked",biomes.length);
        var asphalt=BuiltInRegistries.BLOCK.get(id("asphalt"));
        var road=level.getBlockState(new BlockPos(-4,height,64));
        require(road.is(asphalt)&&road.getValue(com.gregtech.gregtech.block.misc.ConcreteBlock.COLOR)==DyeColor.GRAY,"Road is not source gray asphalt");
        require(level.getBlockState(new BlockPos(72,height-10,-72)).is(Blocks.TERRACOTTA),"Original uncolored mesa terracotta was dyed");
        int[] facilities={7133,4033,6033,5033,8033,14999,26304,32048,32057,32062,32702,32703,32705,32707,32709,32711,32719,32722,32727,32732,32735,32737,32739,32744,32750,32764};
        for(int legacy:facilities)require(BuiltInRegistries.BLOCK.containsKey(id(NativeOriginFacilities.block(legacy))),"Missing source facility "+legacy);
        require(level.getBlockState(new BlockPos(17,height+1,-46)).is(Blocks.END_PORTAL_FRAME),"Original Nexus portal frame was not generated");
        require(level.getBlockState(new BlockPos(34,height+15,-30)).is(BuiltInRegistries.BLOCK.get(id("glass_glow_slab"))),"Source test roof glow-glass slab missing");
        var grind=level.getBlockEntity(new BlockPos(42,height+2,-18));
        require(grind instanceof com.gregtech.gregtech.blockentity.tool.ManualToolBlockEntity station&&station.stoneUses()==4,"Source grindstone not preloaded");
        var button=level.getBlockEntity(new BlockPos(44,height+2,-20));
        require(button instanceof com.gregtech.gregtech.blockentity.tool.AdvancedButtonBlockEntity b&&b.isInverted()&&!b.isLampMode(),"Source inverted test button config lost");
        var certificate=level.getBlockEntity(new BlockPos(41,height+4,-18));
        require(certificate instanceof com.gregtech.gregtech.blockentity.misc.SupporterCertificateBlockEntity c&&c.owner().equals("Bear989Sr"),"Native certificate owner missing");
        var drawer=level.getBlockEntity(new BlockPos(39,height+2,-18));
        require(drawer instanceof com.gregtech.gregtech.blockentity.inventory.DrawerQuadBlockEntity,"Source test drawer was not generated");
        if(!(drawer instanceof com.gregtech.gregtech.blockentity.inventory.DrawerQuadBlockEntity))throw new IllegalStateException("Missing actual test drawer");
        var inventory=((com.gregtech.gregtech.blockentity.inventory.DrawerQuadBlockEntity)drawer).items();
        JsonArray missing=new JsonArray();int present=0,optional=0;
        for(int slot=0;slot<OriginTestInventory.ROWS.size();slot++) {
            String row=OriginTestInventory.ROWS.get(slot);var expected=NativeOriginItems.stack(row);
            var actual=inventory.getStackInSlot(slot);
            boolean absentIntegration=row.equals("NI")||row.contains("IL.TC_")||row.contains("MD.TC")||row.contains("IC2_Debug");
            if(absentIntegration){optional++;continue;}
            if(expected.isEmpty()){missing.add(row);continue;}
            require(!actual.isEmpty()&&actual.getItem()==expected.getItem()&&actual.getCount()==expected.getCount(),"Test inventory slot mismatch "+slot+": "+row);present++;
        }
        receipt.addProperty("originFacilityIdentitiesChecked",facilities.length);
        receipt.addProperty("originTestInventorySlots",OriginTestInventory.ROWS.size());
        receipt.addProperty("originTestInventoryPresent",present);receipt.addProperty("originTestInventoryOptionalEmpty",optional);
        receipt.add("originTestInventoryPendingTools",missing);
        var crank=BuiltInRegistries.BLOCK.get(id("crank"));
        var pos=new BlockPos(4,140,4);int checks=0;
        for(var handle:Direction.values()) {
            var mount=pos.relative(handle.getOpposite());var front=pos.relative(handle);
            var old=level.getBlockState(pos);var oldMount=level.getBlockState(mount);var oldFront=level.getBlockState(front);
            try {
                level.setBlock(mount,Blocks.REDSTONE_LAMP.defaultBlockState(),3);
                level.setBlock(front,Blocks.REDSTONE_LAMP.defaultBlockState(),3);
                var off=crank.defaultBlockState().setValue(com.gregtech.gregtech.block.tool.CrankBlock.FACING,handle);
                level.setBlock(pos,off,3);require(!level.hasNeighborSignal(mount),"Idle crank emits redstone");
                var on=off.setValue(com.gregtech.gregtech.block.tool.CrankBlock.ACTIVE,true);
                level.setBlock(pos,on,3);
                require(level.hasNeighborSignal(mount),"Active crank does not power mounting block: "+handle);
                require(!level.hasNeighborSignal(front),"Active crank powers handle neighbor: "+handle);
                require(on.getSignal(level,pos,handle)==15&&on.getDirectSignal(level,pos,handle)==15,"Crank weak/strong power mismatch");
                require(on.getSignal(level,pos,handle.getOpposite())==0,"Crank opposite query emits power");checks+=5;
            } finally {level.setBlock(pos,old,3);level.setBlock(mount,oldMount,3);level.setBlock(front,oldFront,3);}
        }
        receipt.addProperty("crankNativeSignalChecks",checks);
        // Actual native tool stack enchantment, not a tooltip/table-only assertion.
        var knife=com.gregtech.gregtech.item.GTToolItem.create(com.gregtech.gregtech.api.tool.GTToolType.KNIFE,
                com.gregtech.gregtech.content.material.Materials.Flint,com.gregtech.gregtech.content.material.generated.WoodMaterials.Wood);
        require(net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.FIRE_ASPECT,knife)==1,"Flint knife real Fire Aspect missing");
        receipt.addProperty("flintKnifeFireAspect",1);
        var materials=com.gregtech.gregtech.api.material.GTMaterialRegistry.class;
        var iron=com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Iron");
        var wrought=com.gregtech.gregtech.api.material.GTMaterialRegistry.get("WroughtIron");
        var steel=com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Steel");
        var air=com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Air");
        var contents=new java.util.ArrayList<com.gregtech.gregtech.api.machine.crucible.CrucibleMaterialStack>();
        contents.add(com.gregtech.gregtech.api.machine.crucible.CrucibleMaterialStack.of(iron,com.gregtech.gregtech.api.material.GTValues.U));
        require(!com.gregtech.gregtech.api.machine.crucible.CrucibleReactions.react(contents,wrought.getMeltingPoint()-1),"Iron purified too cold");
        require(com.gregtech.gregtech.api.machine.crucible.CrucibleReactions.react(contents,wrought.getMeltingPoint())&&contents.size()==1&&contents.get(0).material==wrought,"Iron -> wrought reaction missing");
        require(!com.gregtech.gregtech.api.machine.crucible.CrucibleReactions.react(contents,steel.getMeltingPoint()),"Wrought iron became steel without air");
        contents.add(com.gregtech.gregtech.api.machine.crucible.CrucibleMaterialStack.of(air,com.gregtech.gregtech.api.material.GTValues.U));
        require(com.gregtech.gregtech.api.machine.crucible.CrucibleReactions.react(contents,steel.getMeltingPoint())&&contents.size()==1&&contents.get(0).material==steel&&contents.get(0).amount==com.gregtech.gregtech.api.material.GTValues.U,"Air steel yield mismatch");
        receipt.addProperty("originalCrucibleReactionChecks",4);
        var pages=com.gregtech.gregtech.loaders.b.OriginCreativeContents.pages();
        require(pages.get("tool_blocks").stream().anyMatch(stack->stack.getItem()==crank.asItem()),"Crank missing from original tool-block page");
        require(pages.get("tools").stream().noneMatch(stack->stack.getItem()==crank.asItem()),"Crank remains in hand-tool page");
        require(pages.get("construction").stream().filter(stack->stack.getItem()==asphalt.asItem()).count()==16,"Asphalt creative colors missing");
        receipt.addProperty("originalCreativePages",pages.size());
    }
    public static void client(net.minecraft.client.Minecraft minecraft,JsonObject receipt) {
        var asphalt=BuiltInRegistries.BLOCK.get(id("asphalt"));
        var gray=com.gregtech.gregtech.block.misc.ConcreteBlock.coloredItem(asphalt,DyeColor.GRAY);
        int rgb=minecraft.getItemColors().getColor(gray,0)&0xffffff;
        require(rgb==0x808080,"Source gray asphalt item tint mismatch: "+Integer.toHexString(rgb));
        var crank=new ItemStack(BuiltInRegistries.ITEM.get(id("crank")));
        int crankRgb=minecraft.getItemColors().getColor(crank,0)&0xffffff;
        require(crankRgb==com.gregtech.gregtech.content.material.Materials.Iron.getColor(),"Crank source iron tint mismatch");
        checkModel(minecraft,gray,true);checkModel(minecraft,crank,true);
        int gearboxes=0;
        for(var entry:com.gregtech.gregtech.registry.GTGearboxes.allGearboxes()){checkModel(minecraft,new ItemStack(entry.get()),true);gearboxes++;}
        require(gearboxes==13,"Native original gearbox count mismatch");
        receipt.addProperty("gearboxInventoryModelsChecked",gearboxes);
        receipt.addProperty("asphaltItemRgb",Integer.toHexString(rgb));receipt.addProperty("crankItemRgb",Integer.toHexString(crankRgb));
        if(!failures.isEmpty()) {
            var failed=new JsonArray();failures.forEach(failed::add);receipt.add("originFeedbackFailures",failed);
            com.mojang.logging.LogUtils.getLogger().error("ORIGIN_FEEDBACK_FAILED {}",receipt);
            throw new IllegalStateException("Original feedback checks failed: "+failures);
        }
    }
    private static void checkModel(net.minecraft.client.Minecraft minecraft,ItemStack stack,boolean tinted) {
        var model=minecraft.getItemRenderer().getModel(stack,minecraft.level,minecraft.player,0);
        var random=net.minecraft.util.RandomSource.create(1);int quads=0,tints=0;
        for(int side=-1;side<6;side++)for(var quad:model.getQuads(null,side<0?null:Direction.from3DDataValue(side),random)) {
            require(!quad.getSprite().contents().name().getPath().contains("missing"),"Missing sprite: "+BuiltInRegistries.ITEM.getKey(stack.getItem()));
            quads++;if(quad.isTinted())tints++;
        }
        require(quads>0&&(!tinted||tints>0),"Empty/untinted inventory model: "+BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }
}
