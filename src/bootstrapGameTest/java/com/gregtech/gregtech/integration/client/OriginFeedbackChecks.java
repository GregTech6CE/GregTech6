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
            level.getChunk(points[i][0]>>4,points[i][1]>>4); // Complete FEATURES before querying the biome.
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
        require(present==118&&optional==26&&missing.isEmpty(),"Source Nexus inventory still missing native tools: "+missing);
        nexusTools(server,receipt);
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
    private static void nexusTools(MinecraftServer server,JsonObject receipt){
        var level=server.overworld();var actor=net.minecraftforge.common.util.FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(java.util.UUID.fromString("0932c6ab-e537-4e52-aa26-50f40fe56c1e"),"NexusToolCheckpoint"));
        actor.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);actor.moveTo(4.5,140,4.5,0,0);actor.getInventory().clearContent();int gunChecks=0;
        var material=com.gregtech.gregtech.content.material.Materials.Steel;
        for(var type:java.util.List.of(com.gregtech.gregtech.api.tool.GTToolType.PISTOL,com.gregtech.gregtech.api.tool.GTToolType.CARBINE,com.gregtech.gregtech.api.tool.GTToolType.RIFLE)){
            var stack=com.gregtech.gregtech.item.GTToolItem.create(type,material,com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Spruce"));var gun=(com.gregtech.gregtech.item.GunToolItem)stack.getItem();
            actor.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);actor.getInventory().setItem(9,com.gregtech.gregtech.registry.GTItems.getStack(gun.definition().ammunition(),com.gregtech.gregtech.content.material.Materials.Lead,32));
            require(gun.reload(stack,actor),"Gun cannot reload: "+type);var loaded=gun.ammunition(stack,level);require(loaded.getCount()==gun.definition().magazine()&&actor.getInventory().getItem(9).getCount()==32-loaded.getCount(),"Gun ammo not conserved: "+type);gunChecks+=2;
            var target=net.minecraft.world.entity.EntityType.ZOMBIE.create(level);target.moveTo(4.5,140,9.5);target.setNoAi(true);level.addFreshEntity(target);float health=target.getHealth();
            try{actor.setShiftKeyDown(false);gun.activate(level,actor,net.minecraft.world.InteractionHand.MAIN_HAND);require(target.getHealth()<health,"Gun shot does not hurt target: "+type);require(gun.ammunition(stack,level).getCount()==gun.definition().magazine()-1&&stack.getDamageValue()==100,"Gun shot did not consume ammo and wear: "+type);gunChecks+=2;
                actor.setShiftKeyDown(true);gun.activate(level,actor,net.minecraft.world.InteractionHand.MAIN_HAND);require(gun.ammunition(stack,level).isEmpty(),"Gun cannot unload: "+type);gunChecks++;
            }finally{target.discard();actor.setShiftKeyDown(false);actor.getInventory().clearContent();}
        }
        receipt.addProperty("nexusGunUseChecks",gunChecks);
        int[] expectedSlots={7,8,9,9};int craftingRows=0;
        for(String kind:java.util.List.of("pistol","carbine","rifle","pocket_multitool")){
            var holder=server.getRecipeManager().byKey(id("tools/"+kind)).orElseThrow(()->new IllegalStateException("Missing source tool crafting: "+kind));var recipe=(com.gregtech.gregtech.recipe.GTToolCraftingRecipe)holder;
            require(recipe.getIngredients().stream().filter(i->i!=net.minecraft.world.item.crafting.Ingredient.EMPTY).count()==expectedSlots[craftingRows],"Source tool crafting has empty ingredient: "+kind);
            var grid=new net.minecraft.world.inventory.TransientCraftingContainer(new net.minecraft.world.inventory.AbstractContainerMenu(null,0){@Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player,int slot){return ItemStack.EMPTY;}@Override public boolean stillValid(net.minecraft.world.entity.player.Player player){return true;}},3,3);
            for(int slot=0;slot<9;slot++){var options=recipe.getIngredients().get(slot).getItems();if(options.length>0)grid.setItem(slot,options[0].copyWithCount(1));}
            require(recipe.matches(grid,level),"Source tool representative inputs do not match: "+kind);
            var assembled=recipe.assemble(grid,level.registryAccess());
            require(!assembled.isEmpty()&&assembled.getItem()==com.gregtech.gregtech.registry.GTToolItems.get(com.gregtech.gregtech.api.tool.GTToolType.valueOf(kind.toUpperCase(java.util.Locale.ROOT))),"Source tool assembly returns the wrong item: "+kind);
            craftingRows++;
        }
        receipt.addProperty("nexusGunAndPocketCraftingRows",craftingRows);
        var pocket=com.gregtech.gregtech.item.GTToolItem.create(com.gregtech.gregtech.api.tool.GTToolType.POCKET_MULTITOOL,material,material);pocket.setDamageValue(123);
        actor.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,pocket);actor.setShiftKeyDown(true);int modes=0;var pos=new BlockPos(4,139,4);var prior=level.getBlockState(pos);level.setBlock(pos,Blocks.STONE.defaultBlockState(),3);
        try{for(int i=0;i<8;i++){var before=actor.getMainHandItem();var item=(com.gregtech.gregtech.item.PocketToolItem)before.getItem();
            var context=new net.minecraft.world.item.context.UseOnContext(actor,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),Direction.UP,pos,false));
            item.onItemUseFirst(before,context);var after=actor.getMainHandItem();require(after.getItem()==com.gregtech.gregtech.registry.GTToolItems.get(com.gregtech.gregtech.item.PocketToolItem.MODES[(i+1)%8])&&after.getDamageValue()==123&&after.getMaxDamage()==pocket.getMaxDamage(),"Pocket mode loses identity or wear: "+i);modes++;
        }}finally{level.setBlock(pos,prior,3);actor.setShiftKeyDown(false);actor.getInventory().clearContent();}
        receipt.addProperty("nexusPocketModesChecked",modes);
        int powered=0;for(var definition:com.gregtech.gregtech.content.tool.ElectricToolCatalog.ALL){var tool=com.gregtech.gregtech.registry.GTElectricItems.get(definition.id());var stack=tool.assembled(material,definition.capacity());stack.getOrCreateTag().putLong("gt.charge",definition.capacity());
            if(definition.name().equals("Mining Drill"))require(tool.isCorrectToolForDrops(stack,Blocks.STONE.defaultBlockState())&&tool.isCorrectToolForDrops(stack,Blocks.DIRT.defaultBlockState()),"Mining drill cannot mine and shovel: "+definition.id());
            if(definition.name().equals("Trimmer"))require(tool.isCorrectToolForDrops(stack,Blocks.OAK_LEAVES.defaultBlockState()),"Trimmer cannot harvest leaves");
            if(definition.name().equals("BuzzSaw"))require(tool.isCorrectToolForDrops(stack,Blocks.IRON_BARS.defaultBlockState())&&!tool.isCorrectToolForDrops(stack,Blocks.OAK_LOG.defaultBlockState()),"Buzzsaw has wrong target policy");
            long before=tool.getEnergyStored(stack,com.gregtech.gregtech.data.GregTechTags.Energy.EU);tool.consumeInteractionEnergy(stack,100,actor);require(tool.getEnergyStored(stack,com.gregtech.gregtech.data.GregTechTags.Energy.EU)==before-100,"Powered tool does not consume EU: "+definition.id());powered++;
        }
        receipt.addProperty("nexusPoweredToolsChecked",powered);
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
        checkModel(minecraft,new ItemStack(BuiltInRegistries.ITEM.get(id("loot_crate"))),false);
        int nativeTools=0;for(String row:OriginTestInventory.ROWS)if(row.contains("getToolWithStats")){var stack=NativeOriginItems.stack(row);require(!stack.isEmpty(),"Nexus tool still absent: "+row);if(!stack.isEmpty()){checkModel(minecraft,stack,true);nativeTools++;}}
        receipt.addProperty("nexusToolInventoryModelsChecked",nativeTools);
        int ropes=0,filters=0;
        var colors=new JsonObject();
        for(var spec:com.gregtech.gregtech.content.tool.UtilityToolRules.ROPES) {
            var stack=new ItemStack(BuiltInRegistries.ITEM.get(id(spec.id())));
            checkTint(minecraft,stack,com.gregtech.gregtech.api.material.GTMaterialRegistry.get(spec.material()).getColor(),colors);
            checkModel(minecraft,stack,true);ropes++;
        }
        for(String name:java.util.List.of("filter_items","filter_fluids","filter_items_fluids","filter_oredict")) {
            var stack=new ItemStack(BuiltInRegistries.ITEM.get(id(name)));
            checkTint(minecraft,stack,com.gregtech.gregtech.content.material.Materials.SteelGalvanized.getColor(),colors);
            checkModel(minecraft,stack,true);filters++;
        }
        int automatic=0;
        for(var spec:com.gregtech.gregtech.content.tool.AutomaticToolRules.ALL) {
            var stack=new ItemStack(BuiltInRegistries.ITEM.get(id(spec.id())));
            checkTint(minecraft,stack,com.gregtech.gregtech.content.tool.AutomaticToolRules.material(spec).getColor(),colors);
            checkModel(minecraft,stack,true);automatic++;
        }
        for(String name:java.util.List.of("usb_switch","hdd_switch")) {
            var stack=new ItemStack(BuiltInRegistries.ITEM.get(id(name)));
            checkTint(minecraft,stack,com.gregtech.gregtech.content.material.Materials.SteelGalvanized.getColor(),colors);
            checkModel(minecraft,stack,true);automatic++;
        }
        receipt.addProperty("automaticAndDataSwitchColorsChecked",automatic);
        receipt.addProperty("ropeInventoryModelsChecked",ropes);
        receipt.addProperty("filterInventoryModelsChecked",filters);
        receipt.add("inventoryMaterialRgb",colors);
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
        // Match ItemRenderer: query every actual item pass and its entity-format
        // sheet. A vanilla three-argument query misses custom item-layer failures.
        for(var pass:model.getRenderPasses(stack,false))for(var layer:pass.getRenderTypes(stack,false))
            for(int side=-1;side<6;side++)for(var quad:pass.getQuads(null,side<0?null:Direction.from3DDataValue(side),random,
                    net.minecraftforge.client.model.data.ModelData.EMPTY,layer)) {
                require(!quad.getSprite().contents().name().getPath().contains("missing"),"Missing sprite: "+BuiltInRegistries.ITEM.getKey(stack.getItem()));
                quads++;if(quad.isTinted())tints++;
            }
        require(quads>0&&(!tinted||tints>0),"Empty/untinted inventory model: "+BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    private static void checkTint(net.minecraft.client.Minecraft minecraft,ItemStack stack,int expected,JsonObject colors) {
        String name=BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        int actual=minecraft.getItemColors().getColor(stack,0);
        require((actual&0xffffff)==(expected&0xffffff),"Inventory material RGB mismatch: "+name);
        var block=((BlockItem)stack.getItem()).getBlock();
        require((minecraft.getBlockColors().getColor(block.defaultBlockState(),minecraft.level,null,0)&0xffffff)==(expected&0xffffff),
                "World material RGB mismatch: "+name);
        colors.addProperty(name,String.format(java.util.Locale.ROOT,"%06x",actual&0xffffff));
    }
    /** Draw the affected items through the real GUI item renderer before the existing screenshot. */
    public static void renderInventory(net.minecraft.client.gui.GuiGraphics graphics,net.minecraft.client.Minecraft minecraft,JsonObject receipt) {
        var stacks=new java.util.ArrayList<ItemStack>();
        for(var entry:com.gregtech.gregtech.registry.GTGearboxes.allGearboxes())stacks.add(new ItemStack(entry.get()));
        for(var spec:com.gregtech.gregtech.content.tool.UtilityToolRules.ROPES)stacks.add(new ItemStack(BuiltInRegistries.ITEM.get(id(spec.id()))));
        for(String name:java.util.List.of("filter_items","filter_fluids","filter_items_fluids","filter_oredict","crank","asphalt"))
            stacks.add(new ItemStack(BuiltInRegistries.ITEM.get(id(name))));
        for(String row:OriginTestInventory.ROWS)if(row.contains("POCKET_MULTITOOL")||row.contains("ToolsGT.PISTOL")||row.contains("ToolsGT.CARBINE")||row.contains("ToolsGT.RIFLE")||row.contains("MININGDRILL_")||row.contains("MIXER_LV")||row.contains("BUZZSAW_LV")||row.contains("TRIMMER_LV")||row.contains("WRENCH_MV")||row.contains("WRENCH_HV")||row.contains("CHAINSAW_MV")||row.contains("CHAINSAW_HV")||row.contains("OP.cableGt01.mat(MT.Signalum")||row.contains("OP.wireGt01.mat(MT.Lumium"))stacks.add(NativeOriginItems.stack(row));
        for(var spec:com.gregtech.gregtech.content.tool.AutomaticToolRules.ALL)stacks.add(new ItemStack(BuiltInRegistries.ITEM.get(id(spec.id()))));
        for(String name:java.util.List.of("usb_switch","hdd_switch"))stacks.add(new ItemStack(BuiltInRegistries.ITEM.get(id(name))));
        stacks.add(new ItemStack(BuiltInRegistries.ITEM.get(id("loot_crate"))));
        stacks.add(new ItemStack(BuiltInRegistries.ITEM.get(id("crate"))));
        var sampled=new JsonArray();var untranslated=new JsonArray();
        graphics.fill(4,4,410,24+((stacks.size()+10)/11)*28,0xff121216);
        graphics.drawString(minecraft.font,"GT inventory + original Nexus tools",8,8,0xffffff,false);
        for(int i=0;i<stacks.size();i++) {
            var stack=stacks.get(i);String name=BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
            int x=8+(i%11)*36,y=24+(i/11)*28;
            graphics.fill(x,y,x+34,y+27,0xff30303a);
            graphics.renderItem(stack,x+9,y+2);
            String label=name.replace("gearbox_","").replace("rope_","R:").replace("filter_","F:");
            graphics.drawString(minecraft.font,minecraft.font.plainSubstrByWidth(label,31),x+2,y+19,0xffffff,false);
            sampled.add(name);
            var hover=stack.getHoverName();
            if(hover.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents translated
                    && hover.getString().equals(translated.getKey()))untranslated.add(translated.getKey());
        }
        receipt.add("renderedInventoryItems",sampled);receipt.add("untranslatedInventorySamples",untranslated);
    }
}
