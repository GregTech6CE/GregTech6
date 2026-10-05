package com.gregtech.gregtech.integration.client;

import com.google.gson.*;
import com.gregtech.gregtech.content.cover.*;
import com.gregtech.gregtech.blockentity.machine.*;
import com.gregtech.gregtech.blockentity.inventory.MetalChestBlockEntity;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import java.util.*;

/** Finite checks for issues 13–18 against loaded native registries and capabilities. */
final class NewIssuesRuntimeSmoke {
    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath("gregtech",path);}
    private static ItemStack item(String path){var key=id(path);require(BuiltInRegistries.ITEM.containsKey(key),"missing item "+path);return new ItemStack(BuiltInRegistries.ITEM.get(key));}
    private static Block block(String path){return BuiltInRegistries.BLOCK.get(id(path));}
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException("New issues: "+message);}
    private static IFluidHandler fluids(ServerLevel level,BlockPos pos,Direction side){return level.getBlockEntity(pos).getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER,side).orElse(null);}
    private static net.minecraftforge.items.IItemHandler items(ServerLevel level,BlockPos pos,Direction side){return level.getBlockEntity(pos).getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER,side).orElse(null);}
    private static void place(ServerLevel level,BlockPos pos,Block block){level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);require(block!=Blocks.AIR,"unregistered fixture");level.setBlock(pos,block.defaultBlockState(),3);}
    static void server(MinecraftServer server,JsonObject receipt){
        var level=server.overworld();var pos=new BlockPos(224,235,224);level.getChunkAt(pos);level.getChunkAt(pos.east());
        var actor=net.minecraftforge.common.util.FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(UUID.fromString("3c4f7d46-1316-4c7e-a240-d0c32aab4158"),"IssuesCheckpoint"));
        actor.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);actor.moveTo(224.5,235,227.5);actor.getInventory().clearContent();
        var rock=com.gregtech.gregtech.registry.GTItems.getStack(com.gregtech.gregtech.data.MaterialPrefix.rockGt,com.gregtech.gregtech.api.material.GTMaterialRegistry.get("GraniteBlack"),3);
        int placed=0;
        for(var collected:List.of(rock,new ItemStack(Items.RAW_COPPER,3))){
            require(!collected.isEmpty(),"missing rock fixture");level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),3);
            actor.setShiftKeyDown(true);actor.setItemInHand(InteractionHand.MAIN_HAND,collected);
            var hit=new BlockHitResult(Vec3.atCenterOf(pos.below()),Direction.UP,pos.below(),false);
            var result=com.gregtech.gregtech.block.misc.PilePlacementHandler.tryPlace(level,actor,InteractionHand.MAIN_HAND,hit);
            require(result.consumesAction()&&collected.getCount()==2,"sneak rock placement/count "+collected);
            var drops=Block.getDrops(level.getBlockState(pos),level,pos,level.getBlockEntity(pos));
            require(drops.size()==1&&drops.get(0).is(collected.getItem())&&drops.get(0).getCount()==1,"rock placement rerolled contents/ore bonus");
            actor.setShiftKeyDown(false);
            require(com.gregtech.gregtech.block.misc.PilePlacementHandler.tryPlace(level,actor,InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.PASS,"non-sneak replacement");placed++;
        }
        receipt.addProperty("collectedRockPlacements",placed);
        actor.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        var target=pos.east();place(level,pos,block("drum_adamantium"));place(level,target,block("drum_adamantium"));
        var tank=(TankBlockEntity)level.getBlockEntity(pos);var dest=(TankBlockEntity)level.getBlockEntity(target);
        var cached=fluids(level,pos,Direction.EAST);require(cached!=null,"tank side handler");
        tank.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000),IFluidHandler.FluidAction.EXECUTE);
        var pump=item("compact_electric_pump_ulv");require(tank.attachCover(Direction.EAST,pump),"tank pump attach");
        require(ComponentCoverRuntime.visual(tank.getCover(Direction.EAST))==0,"normal pump default output");
        require(cached.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,1),IFluidHandler.FluidAction.SIMULATE)==0,"cached handler bypasses output cover");
        tank.panels().beforeTick();ComponentCoverRuntime.tick(tank,Direction.EAST,5);
        require(tank.getFluidInTank(0).getAmount()==750&&dest.getFluidInTank(0).getAmount()==250,"ULV pump output/throughput");
        CoverStackData.putInt(tank.getCover(Direction.EAST),ComponentCoverRuntime.VISUAL,1);
        require(cached.drain(1,IFluidHandler.FluidAction.SIMULATE).isEmpty(),"cached handler bypasses input cover");
        ComponentCoverRuntime.tick(tank,Direction.EAST,25);
        require(tank.getFluidInTank(0).getAmount()==1000&&dest.getFluidInTank(0).isEmpty(),"pump direction input/conservation");
        var saved=tank.saveWithoutMetadata();place(level,pos,block("drum_adamantium"));tank=(TankBlockEntity)level.getBlockEntity(pos);tank.load(saved);
        require(ComponentCoverRuntime.visual(tank.getCover(Direction.EAST))==1&&tank.getFluidInTank(0).getAmount()==1000,"native tank save roundtrip");
        require(tank.getUpdateTag().contains("gt_cover_5"),"tank cover missing client packet");
        require(tank.attachCover(Direction.DOWN,item("cover_controller")),"tank pause controller");
        tank.panels().beforeTick();require(tank.panels().stopped(),"unpowered controller pause");
        ComponentCoverRuntime.tick(tank,Direction.EAST,45);require(tank.getFluidInTank(0).getAmount()==1000,"paused pump moved");
        tank.removeCover(Direction.DOWN);tank.panels().beforeTick();require(!tank.panels().stopped(),"controller removal remains paused");
        receipt.addProperty("pumpNativeChecks",9);
        var chestBlock=BuiltInRegistries.BLOCK.stream().filter(b->b instanceof com.gregtech.gregtech.block.inventory.MetalChestBlock).findFirst().orElseThrow();
        place(level,pos,chestBlock);place(level,target,Blocks.CHEST);
        var chest=(MetalChestBlockEntity)level.getBlockEntity(pos);var own=chest.componentItems(Direction.EAST);var other=items(level,target,Direction.WEST);
        own.insertItem(0,new ItemStack(Items.DIAMOND,64),false);own.insertItem(1,new ItemStack(Items.EMERALD,32),false);
        require(chest.attachCover(Direction.EAST,item("compact_electric_conveyor_xv")),"chest conveyor attach");
        var cache=items(level,pos,Direction.EAST);require(cache.insertItem(2,new ItemStack(Items.DIAMOND),true).getCount()==1,"conveyor input intercept");
        chest.panels().beforeTick();ComponentCoverRuntime.tick(chest,Direction.EAST,512);
        require(own.getStackInSlot(0).isEmpty()&&own.getStackInSlot(1).getCount()==32&&other.getStackInSlot(0).getCount()==64,"conveyor must move one group");
        chest.removeCover(Direction.EAST);require(chest.attachCover(Direction.EAST,item("compact_robot_arm_xv")),"chest arm attach");
        CoverStackData.putInt(chest.getCover(Direction.EAST),ComponentCoverRuntime.SLOT,7);
        ComponentCoverRuntime.tick(chest,Direction.EAST,513);
        require(other.getStackInSlot(7).is(Items.EMERALD)&&other.getStackInSlot(7).getCount()==32,"arm target slot");
        other.insertItem(3,new ItemStack(Items.GOLD_INGOT,17),false);
        CoverStackData.putInt(chest.getCover(Direction.EAST),ComponentCoverRuntime.VISUAL,1);
        CoverStackData.putInt(chest.getCover(Direction.EAST),ComponentCoverRuntime.SLOT,-4);
        ComponentCoverRuntime.tick(chest,Direction.EAST,514);
        require(own.getStackInSlot(0).is(Items.GOLD_INGOT)&&own.getStackInSlot(0).getCount()==17&&other.getStackInSlot(3).isEmpty(),"arm signed source slot");
        require(cache.extractItem(0,1,true).isEmpty(),"cached conveyor handler did not adopt arm input mode");
        var state=chest.saveWithoutMetadata();var clientState=chest.getUpdateTag();chest.load(state);chest.handleUpdateTag(clientState);
        require(chest.componentItems(Direction.EAST).getStackInSlot(0).getCount()==17&&ComponentCoverRuntime.slot(chest.getCover(Direction.EAST))==-4,"chest update packet erases contents");
        receipt.addProperty("conveyorArmNativeChecks",8);
        var coverFace=new BlockHitResult(Vec3.atCenterOf(pos),Direction.EAST,pos,false);
        actor.setItemInHand(InteractionHand.MAIN_HAND,com.gregtech.gregtech.item.GTToolItem.create(com.gregtech.gregtech.api.tool.GTToolType.SCREWDRIVER,com.gregtech.gregtech.content.material.Materials.Steel,com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Spruce")));
        actor.setShiftKeyDown(false);require(ComponentCoverInteraction.use(chest,actor,InteractionHand.MAIN_HAND,coverFace).consumesAction(),"arm screw action");
        require(ComponentCoverRuntime.slot(chest.getCover(Direction.EAST))==-3,"arm screw increment");
        actor.setShiftKeyDown(true);ComponentCoverInteraction.use(chest,actor,InteractionHand.MAIN_HAND,coverFace);
        require(ComponentCoverRuntime.slot(chest.getCover(Direction.EAST))==-4,"arm sneak decrement");actor.setShiftKeyDown(false);
        actor.setItemInHand(InteractionHand.MAIN_HAND,com.gregtech.gregtech.item.GTToolItem.create(com.gregtech.gregtech.api.tool.GTToolType.MONKEY_WRENCH,com.gregtech.gregtech.content.material.Materials.Steel,com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Spruce")));
        ComponentCoverInteraction.use(chest,actor,InteractionHand.MAIN_HAND,coverFace);require(ComponentCoverRuntime.visual(chest.getCover(Direction.EAST))==0,"arm monkey direction");
        actor.setItemInHand(InteractionHand.MAIN_HAND,com.gregtech.gregtech.item.GTToolItem.create(com.gregtech.gregtech.api.tool.GTToolType.WRENCH,com.gregtech.gregtech.content.material.Materials.Steel,com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Spruce")));
        var facing=chest.getBlockState();require(ComponentCoverInteraction.use(chest,actor,InteractionHand.MAIN_HAND,coverFace).consumesAction()&&chest.getBlockState().equals(facing),"cover wrench rotates host");
        actor.setItemInHand(InteractionHand.MAIN_HAND,com.gregtech.gregtech.item.GTToolItem.create(com.gregtech.gregtech.api.tool.GTToolType.CROWBAR,com.gregtech.gregtech.content.material.Materials.Steel,com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Spruce")));
        ComponentCoverInteraction.use(chest,actor,InteractionHand.MAIN_HAND,coverFace);require(chest.getCover(Direction.EAST).isEmpty(),"crowbar cannot remove component");
        receipt.addProperty("componentToolInteractions",5);

        int fluidPipes=0,itemPipes=0,machines=0,batteries=0;
        for(var b:BuiltInRegistries.BLOCK){
            if(!BuiltInRegistries.BLOCK.getKey(b).getNamespace().equals("gregtech"))continue;
            if(b instanceof com.gregtech.gregtech.block.machine.FluidPipeBlock&&fluidPipes==0){
                place(level,pos,b);var pipe=(FluidPipeBlockEntity)level.getBlockEntity(pos);
                require(pipe.attachCover(Direction.EAST,pump),"fluid pipe pump attach");require(ComponentCoverRuntime.visual(pipe.getCover(Direction.EAST))==1,"pipe must force input");
                require(!pipe.attachCover(Direction.WEST,item("compact_electric_conveyor_ulv")),"item cover on inventory-less pipe");fluidPipes++;
            }else if(b instanceof com.gregtech.gregtech.block.machine.ItemPipeBlock&&itemPipes==0){
                place(level,pos,b);var pipe=(ItemPipeBlockEntity)level.getBlockEntity(pos);
                require(pipe.attachCover(Direction.EAST,item("compact_robot_arm_xv")),"item pipe robot attach");
                require(ComponentCoverRuntime.visual(pipe.getCover(Direction.EAST))==1&&ComponentCoverRuntime.slot(pipe.getCover(Direction.EAST))==-1,"pipe arm source slot");itemPipes++;
            }else if(b instanceof com.gregtech.gregtech.block.machine.BasicMachineBlock&&machines==0){
                place(level,pos,b);var machine=(BasicMachineBlockEntity)level.getBlockEntity(pos);
                if(machine.componentItems(Direction.EAST).getSlots()>0){require(machine.attachCover(Direction.EAST,item("compact_electric_conveyor_xv")),"basic machine conveyor attach");machines++;}
            }
            if(fluidPipes>0&&itemPipes>0&&machines>0)break;
        }
        place(level,pos,block("battery_box_ev"));var battery=(PanelCoverHost)level.getBlockEntity(pos);
        require(battery.attachCover(Direction.EAST,item("compact_robot_arm_xv")),"battery box arm attach");batteries++;
        receipt.addProperty("componentHostFixtures",fluidPipes+itemPipes+machines+batteries+2);
        fallbackHosts(level,pos.offset(0,0,4),server,receipt);
        var empty=new JsonArray();int inspected=0;
        for(var recipe:server.getRecipeManager().getRecipes()){
            if(!recipe.getId().getNamespace().equals("gregtech"))continue;
            for(var ingredient:recipe.getIngredients()){
                if(ingredient==net.minecraft.world.item.crafting.Ingredient.EMPTY)continue;
                inspected++;
                if(ingredient.getItems().length==0){var row=new JsonObject();row.addProperty("recipe",recipe.getId().toString());row.add("ingredient",ingredient.toJson());empty.add(row);}
            }
        }
        receipt.addProperty("nativeRecipeIngredientsInspected",inspected);receipt.add("emptyNativeRecipeIngredients",empty);
        try{java.nio.file.Files.writeString(java.nio.file.Path.of("new-issues-empty-ingredients.json"),new GsonBuilder().setPrettyPrinting().create().toJson(empty));}catch(java.io.IOException error){throw new RuntimeException(error);}
    }

    private static void noCoverItemCopy(ServerLevel level,net.minecraft.world.level.block.entity.BlockEntity owner){
        for(var drop:Block.getDrops(owner.getBlockState(),level,owner.getBlockPos(),owner)){
            var tag=drop.getTagElement("BlockEntityTag");
            if(tag!=null)for(int side=0;side<6;side++)require(!tag.contains("gt_cover_"+side),"fallback block item duplicated cover "+owner.getClass());
        }
    }
    private static final BlockPos FALLBACK_DISPLAY=new BlockPos(224,180,232);
    private static void fallbackHosts(ServerLevel level,BlockPos pos,MinecraftServer server,JsonObject receipt){
        var classes=new HashSet<Class<?>>();var rows=new JsonArray();int faces=0;
        for(var b:BuiltInRegistries.BLOCK){
            if(!BuiltInRegistries.BLOCK.getKey(b).getNamespace().equals("gregtech")||!(b instanceof EntityBlock entity))continue;
            var owner=entity.newBlockEntity(pos,b.defaultBlockState());
            if(owner==null||!ComponentCoverFallback.uses(owner)||!classes.add(owner.getClass()))continue;
            owner.setLevel(level);if(!ComponentCoverFallback.eligible(owner))continue;
            var host=(PanelCoverHost)owner;int accepted=0;
            for(var side:Direction.values()){
                var fluid=host.componentFluids(side);var inventory=host.componentItems(side);
                boolean water=fluid!=null&&fluid.getTanks()>0,items=inventory!=null&&inventory.getSlots()>0;
                require(host.attachCover(side,item("compact_electric_pump_ulv"))==water,"fallback fluid placement "+owner.getClass()+" "+side);
                if(water){if(accepted==0)noCoverItemCopy(level,owner);require(host.removeCover(side).is(item("compact_electric_pump_ulv").getItem()),"fallback pump removal");accepted++;}
                require(host.attachCover(side,item("compact_electric_conveyor_xv"))==items,"fallback item placement "+owner.getClass()+" "+side);
                if(items){if(accepted==0)noCoverItemCopy(level,owner);host.removeCover(side);accepted++;}
                require(host.attachCover(side,item("compact_robot_arm_xv"))==items,"fallback arm placement "+owner.getClass()+" "+side);
                if(items){if(accepted==0)noCoverItemCopy(level,owner);host.removeCover(side);accepted++;}
            }
            if(accepted>0){var row=new JsonObject();row.addProperty("class",owner.getClass().getName());row.addProperty("acceptedFaces",accepted);rows.add(row);faces+=accepted;}
        }
        require(faces>=30,"missing generic native hosts");
        place(level,pos,Blocks.CHEST);var vanilla=(PanelCoverHost)level.getBlockEntity(pos);
        require(!vanilla.attachCover(Direction.EAST,item("compact_electric_conveyor_xv")),"vanilla chest accepted GT cover");
        var hopperBlock=BuiltInRegistries.BLOCK.stream().filter(b->b.getClass()==com.gregtech.gregtech.block.machine.HopperBlock.class).findFirst().orElseThrow();
        place(level,pos,hopperBlock);place(level,pos.north(),Blocks.CHEST);
        var owner=level.getBlockEntity(pos);var host=(PanelCoverHost)owner;
        require(ComponentCoverFallback.uses(owner)&&ComponentCoverFallback.eligible(owner),"hopper fallback not selected");
        var raw=(net.minecraftforge.items.IItemHandler)owner;raw.insertItem(0,new ItemStack(Items.DIAMOND,32),false);
        var cached=items(level,pos,Direction.NORTH);require(!cached.extractItem(0,1,true).isEmpty(),"hopper extraction fixture");
        require(host.attachCover(Direction.NORTH,item("compact_electric_conveyor_xv")),"hopper conveyor attach");
        ComponentCoverFallback.tick(level);
        require(raw.getStackInSlot(0).isEmpty()&&items(level,pos.north(),Direction.SOUTH).getStackInSlot(0).getCount()==32,"fallback tick conveyor transfer");
        raw.insertItem(0,new ItemStack(Items.EMERALD,17),false);
        CoverStackData.putInt(host.getCover(Direction.NORTH),ComponentCoverRuntime.VISUAL,1);
        require(cached.extractItem(0,1,true).isEmpty(),"cached fallback capability ignores input mode");
        host.removeCover(Direction.NORTH);require(host.attachCover(Direction.NORTH,item("compact_robot_arm_xv")),"hopper arm");
        CoverStackData.putInt(host.getCover(Direction.NORTH),ComponentCoverRuntime.SLOT,8);
        var saved=owner.saveWithFullMetadata();
        var restored=net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos,owner.getBlockState(),saved);require(restored!=null,"native fallback loadStatic");restored.setLevel(level);
        var loaded=(PanelCoverHost)restored;
        require(ComponentCoverRuntime.slot(loaded.getCover(Direction.NORTH))==8&&((net.minecraftforge.items.IItemHandler)restored).getStackInSlot(0).getCount()==17,"fallback NBT lost arm slot/inventory");
        ComponentCoverFallback.read(restored,new net.minecraft.nbt.CompoundTag());
        require(ComponentCoverRuntime.slot(loaded.getCover(Direction.NORTH))==8,"partial native update erased fallback cover");
        // setRemoved during unloading must leave attached covers in the saved entity.
        owner.setRemoved();require(!host.getCover(Direction.NORTH).isEmpty(),"chunk unload dropped fallback cover");owner.clearRemoved();
        var box=new AABB(pos).inflate(2);for(var entity:level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,box))entity.discard();
        level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
        long dropped=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,box).stream().filter(e->e.getItem().is(item("compact_robot_arm_xv").getItem())).mapToLong(e->e.getItem().getCount()).sum();
        require(dropped==1,"fallback removal must drop exactly one cover: "+dropped);
        place(level,FALLBACK_DISPLAY,hopperBlock);var display=(PanelCoverHost)level.getBlockEntity(FALLBACK_DISPLAY);
        require(display.attachCover(Direction.SOUTH,item("compact_robot_arm_xv")),"fallback client display attach");
        CoverStackData.putInt(display.getCover(Direction.SOUTH),ComponentCoverRuntime.SLOT,8);
        ComponentCoverFallback.tick(level);
        for(var player:server.getPlayerList().getPlayers()){
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            for(int x=223;x<=225;x++)for(int z=235;z<=237;z++)level.setBlock(new BlockPos(x,179,z),Blocks.STONE.defaultBlockState(),3);
            player.teleportTo(level,224.5,180,236.5,180,14);
        }
        receipt.add("fallbackNativeHostClasses",rows);receipt.addProperty("fallbackAcceptedFaces",faces);
        receipt.addProperty("fallbackLifecycleChecks",9);
    }

    private static int visibleFrames;
    private static java.util.concurrent.CompletableFuture<Void> reload;
    static boolean client(net.minecraft.client.Minecraft minecraft,JsonObject receipt){
        if(reload==null){minecraft.options.languageCode="zh_cn";minecraft.getLanguageManager().setSelected("zh_cn");minecraft.getLanguageManager().onResourceManagerReload(minecraft.getResourceManager());reload=java.util.concurrent.CompletableFuture.completedFuture(null);return false;}
        if(!reload.isDone()||minecraft.getOverlay()!=null)return false;reload.join();
        var language=net.minecraft.locale.Language.getInstance();var resource=ResourceLocation.fromNamespaceAndPath("gregtech","lang/zh_cn.json");
        JsonObject entries;try(var reader=minecraft.getResourceManager().getResource(resource).orElseThrow().openAsReader()){entries=JsonParser.parseReader(reader).getAsJsonObject();}catch(java.io.IOException error){throw new RuntimeException(error);}
        int stones=0,tabs=0,forms=0;
        for(var nativeItem:BuiltInRegistries.ITEM){
            String key=null;
            if(nativeItem instanceof com.gregtech.gregtech.api.material.MaterialFormItem form)
                key="oredict."+form.getPrefix().getName()+form.getMaterial().getName();
            else if(nativeItem instanceof BlockItem blockItem && blockItem.getBlock() instanceof com.gregtech.gregtech.block.MaterialBlockLike form)
                key="oredict."+form.prefix().getName()+form.material().getName();
            if(key!=null&&entries.has(key)){
                require(new ItemStack(nativeItem).getHoverName().getString().equals(language.getOrDefault(key)),"native material form ignores source name "+key);forms++;
            }
        }
        for(var b:BuiltInRegistries.BLOCK)if(b instanceof com.gregtech.gregtech.block.stone.GTStoneBlock||b instanceof com.gregtech.gregtech.block.stone.GTStoneSlabBlock){
            var key=b.getDescriptionId();require(entries.has(key),"missing typed stone name "+key);require(new ItemStack(b).getHoverName().getString().equals(entries.get(key).getAsString()),"stone item name still generic "+key);stones++;
        }
        for(var key:entries.keySet())if(key.startsWith("itemGroup.gregtech.")){require(language.has(key),"missing native tab "+key);tabs++;}
        for(var kind:ComponentCoverRules.Kind.values())for(int mode=0;mode<2;mode++){
            var texture=id("block/machines/covers/"+ComponentCoverRules.texture(kind,mode));
            var sprite=minecraft.getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(texture);
            require(!sprite.contents().name().equals(net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation()),"missing cover atlas "+texture);
        }
        receipt.addProperty("sourceMaterialFormNamesChecked",forms);
        receipt.addProperty("typedStoneNamesChecked",stones);receipt.addProperty("translatedTabKeysChecked",tabs);receipt.addProperty("actualClientLanguage",minecraft.getLanguageManager().getSelected());receipt.addProperty("componentCoverAtlasSprites",6);
        var display=minecraft.level.getBlockEntity(FALLBACK_DISPLAY);
        if(display==null||((PanelCoverHost)display).getCover(Direction.SOUTH).isEmpty())return false;
        require(ComponentCoverRuntime.slot(((PanelCoverHost)display).getCover(Direction.SOUTH))==8,"fallback client sync slot");
        require(ComponentCoverFallback.active().contains(display),"fallback renderer missing active client host");
        if(++visibleFrames<20)return false;
        receipt.addProperty("fallbackClientSync",true);
        return true;
    }
}
