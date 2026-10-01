package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.energy.AxleBlock;
import com.gregtech.gregtech.block.misc.AdvancedCraftingTableBlockEntity;
import com.gregtech.gregtech.blockentity.energy.AxleBlockEntity;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class AxleRepairTests {
    private static AxleBlock block(){return GTAxles.all().stream().map(net.minecraftforge.registries.RegistryObject::get).filter(b->b.spec().id().equals("axle_steel_1")).findFirst().orElseThrow();}
    private static AxleBlockEntity place(GameTestHelper h,BlockPos pos){h.setBlock(pos,block().defaultBlockState().setValue(AxleBlock.WEST,true).setValue(AxleBlock.EAST,true));return (AxleBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));}
    private static long send(AxleBlockEntity axle,Direction from,long speed,long power){return axle.doEnergyInjection(GregTechTags.Energy.RU,from,speed,power,true);}
    @GameTest(template="test_empty")
    public static void allOriginalAxleTiersHaveMatchingRatings(GameTestHelper h){
        String[] materials={"wood","bronze","brass","arsenic_copper","arsenic_bronze","steel","titanium","tungstensteel","iridium","iritanium","trinitanium","trinaquadalloy","adamantium"};
        long[] speeds={16,64,64,64,64,256,1024,4096,16384,65536,262144,1048576,4194304};
        long[] basePowers={1,2,2,2,3,4,8,16,32,64,128,256,512};
        h.assertTrue(GTAxles.all().size()==52,"all 13 original materials have four axle sizes");
        for(int tier=0;tier<materials.length;tier++)for(int size=1;size<=4;size++){
            String id="axle_"+materials[tier]+"_"+size;
            var block=ForgeRegistries.BLOCKS.getValue(GregTech.id(id));
            h.assertTrue(block instanceof AxleBlock,"registered axle: "+id);
            AxleBlock axle=(AxleBlock)block;
            h.assertTrue(axle.spec().maxSpeed()==speeds[tier]
                    && axle.spec().maxPower()==basePowers[tier]*(1L<<(size-1))
                    && axle.spec().size()==size && axle.spec().material()!=null,
                    "original speed, bandwidth, size and material: "+id);
        }
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void everyOriginalAxleHasObtainableHandRecipe(GameTestHelper h){
        var manager=h.getLevel().getRecipeManager();
        long generated=com.gregtech.gregtech.loaders.Loader_HandToolCraftingRecipes.registeredIds()
                .stream().filter(id->id.startsWith("gregtech:hand/axle/")).count();
        h.assertTrue(generated==52,"all 52 axle recipes are generated, not skipped for missing forms");
        for(var entry:GTAxles.all()){
            AxleBlock axle=entry.get();
            var id=GregTech.id("hand/axle/"+axle.spec().id());
            var recipe=manager.byKey(id).orElseThrow(()->new IllegalStateException("Missing axle recipe "+id));
            h.assertTrue(recipe instanceof net.minecraft.world.item.crafting.CraftingRecipe,"hand crafting recipe: "+id);
            h.assertTrue(recipe.getResultItem(h.getLevel().registryAccess()).is(axle.asItem()),"crafts matching axle: "+id);
            for(var ingredient:recipe.getIngredients())if(!ingredient.isEmpty())
                h.assertTrue(ingredient.getItems().length>0,"ingredient resolves to obtainable item: "+id);
        }
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void hugeWoodAxleConsumesFiniteCreosoteAndKeepsContainer(GameTestHelper h){
        var id=GregTech.id("hand/axle/axle_wood_4");
        var recipe=(com.gregtech.gregtech.recipe.CreosoteAxleRecipe)h.getLevel().getRecipeManager().byKey(id).orElseThrow();
        var menu=new net.minecraft.world.inventory.AbstractContainerMenu(null,0){
            @Override public net.minecraft.world.item.ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player,int slot){return net.minecraft.world.item.ItemStack.EMPTY;}
            @Override public boolean stillValid(net.minecraft.world.entity.player.Player player){return true;}
        };
        var grid=new net.minecraft.world.inventory.TransientCraftingContainer(menu,3,3);
        for(int y=0;y<recipe.getHeight();y++)for(int x=0;x<recipe.getWidth();x++){
            var ingredient=recipe.getIngredients().get(x+y*recipe.getWidth());
            if(ingredient.isEmpty())continue;
            var stack=ingredient.getItems()[0].copy();
            if(stack.getItem() instanceof com.gregtech.gregtech.item.GTToolItem)
                stack=com.gregtech.gregtech.item.GTToolItem.create(
                        com.gregtech.gregtech.api.tool.GTToolHelper.getType(stack),
                        com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Steel"),
                        com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Wood"));
            grid.setItem(x+y*3,stack);
        }
        var creosote=com.gregtech.gregtech.registry.GTFluids.still("Oil_Creosote").get();
        var filled=net.minecraftforge.fluids.FluidUtil.getFluidContained(grid.getItem(3)).orElse(net.minecraftforge.fluids.FluidStack.EMPTY);
        h.assertTrue(filled.getFluid()==creosote&&filled.getAmount()>=1000,"JEI example is a real finite creosote container");
        h.assertTrue(recipe.matches(grid,h.getLevel()),"filled vessel, beam and two valid tools craft huge wood axle");
        var remains=recipe.getRemainingItems(grid);
        var returned=net.minecraftforge.fluids.FluidUtil.getFluidContained(remains.get(3)).orElse(net.minecraftforge.fluids.FluidStack.EMPTY);
        h.assertTrue(!remains.get(3).isEmpty()&&filled.getAmount()-returned.getAmount()==1000,
                "craft drains exactly 1000 mB and returns the same vessel");
        h.assertTrue(net.minecraftforge.fluids.FluidUtil.getFluidContained(grid.getItem(3)).orElse(net.minecraftforge.fluids.FluidStack.EMPTY).getAmount()==filled.getAmount(),
                "recipe matching and remainder calculation leave input untouched");
        var proxy=com.gregtech.gregtech.registry.GTFluidItems.forFluid(creosote);
        grid.setItem(3,new net.minecraft.world.item.ItemStack(proxy));
        h.assertTrue(!recipe.matches(grid,h.getLevel()),"infinite display fluid cannot pay for creosote");
        var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try{
            com.gregtech.gregtech.recipe.CreosoteAxleRecipe.SERIALIZER.toNetwork(buffer,recipe);
            var copy=com.gregtech.gregtech.recipe.CreosoteAxleRecipe.SERIALIZER.fromNetwork(id,buffer);
            grid.setItem(3,remains.get(3));
            h.assertTrue(!copy.matches(grid,h.getLevel())&&buffer.readableBytes()==0,"client recipe codec retains finite creosote requirement");
        }finally{buffer.release();}
        h.succeed();
    }
    /** Test receiver exposes exactly what traversed the real server block-entity network. */
    private static final class Sink extends com.gregtech.gregtech.blockentity.GTEnergyBlockEntity {
        long packets,lastSpeed;
        Sink(BlockPos pos,BlockState state){super(GTBlockEntities.CHARGING_CRAFTING_TABLE.get(),pos,state);}
        @Override public boolean isEnergyType(GregTechTags.Tag energy,Direction side,boolean emitting){return !emitting&&energy==GregTechTags.Energy.RU;}
        @Override public long getEnergyDemanded(GregTechTags.Tag energy,Direction side,long size){return Long.MAX_VALUE;}
        @Override public long getEnergyOffered(GregTechTags.Tag energy,Direction side,long size){return 0;}
        @Override public long getEnergySizeInputRecommended(GregTechTags.Tag energy,Direction side){return 32;}
        @Override public long getEnergySizeOutputRecommended(GregTechTags.Tag energy,Direction side){return 0;}
        @Override public boolean isEnergyAcceptingFrom(GregTechTags.Tag energy,Direction side,boolean theoretical){return energy==GregTechTags.Energy.RU;}
        @Override public long doEnergyInjection(GregTechTags.Tag energy,Direction side,long size,long amount,boolean execute){if(energy!=GregTechTags.Energy.RU)return 0;if(execute){packets+=amount;lastSpeed=size;}return amount;}
    }
    private static Sink sink(GameTestHelper h,BlockPos pos){h.setBlock(pos,GTMiscBlocks.CHARGING_CRAFTING_TABLE.get());var absolute=h.absolutePos(pos);var sink=new Sink(absolute,h.getLevel().getBlockState(absolute));h.getLevel().setBlockEntity(sink);return sink;}
    @GameTest(template="test_empty")
    public static void threeAxlesSpinUpThenTransmitBothDirectionsWithoutSpeedLoss(GameTestHelper h){
        var left=sink(h,new BlockPos(0,1,1));var right=sink(h,new BlockPos(4,1,1));
        var a=place(h,new BlockPos(1,1,1));var b=place(h,new BlockPos(2,1,1));var c=place(h,new BlockPos(3,1,1));
        h.assertTrue(send(a,Direction.WEST,32,1)==0,"new axle waits for first tick");
        for(var axle:new AxleBlockEntity[]{a,b,c})axle.serverTick();
        for(int stage=0;stage<3;stage++){
            h.assertTrue(send(a,Direction.WEST,32,1)==1&&right.packets==0,"one additional axle spins up per interval, stage "+stage);
            for(var axle:new AxleBlockEntity[]{a,b,c})axle.serverTick();
        }
        h.assertTrue(send(a,Direction.WEST,32,1)==1&&right.packets==1&&right.lastSpeed==32,"three segments deliver unchanged speed after startup");
        h.assertTrue(send(c,Direction.EAST,-32,1)==1&&left.packets==1&&left.lastSpeed==-32,"reverse entry and signed rotation traverse entire line");
        h.assertTrue(a.lossPerBlock()==0&&a.getEnergyLossPerMeter(GregTechTags.Energy.RU)==0,"no invented axle loss");h.succeed();
    }
    @GameTest(template="test_empty")
    public static void axleSimulationAndDisconnectedFacesCannotConsumeEnergy(GameTestHelper h){
        var a=place(h,new BlockPos(1,1,1));a.serverTick();var before=a.saveWithoutMetadata();
        h.assertTrue(a.doEnergyInjection(GregTechTags.Energy.EU,Direction.WEST,32,5,true)==0&&a.doEnergyInjection(GregTechTags.Energy.RU,null,32,5,true)==0,"wrong energy and null face reject");
        h.assertTrue(send(a,Direction.UP,32,5)==0&&send(a,Direction.WEST,Long.MIN_VALUE,1)==0&&send(a,Direction.WEST,0,1)==0,"unconnected face and invalid speed reject");
        h.assertTrue(a.doEnergyInjection(GregTechTags.Energy.RU,Direction.WEST,32,5,false)==5&&before.equals(a.saveWithoutMetadata())&&a.transferredPower()==0,"simulation validates local connection but changes no rotation or counters");
        send(a,Direction.WEST,32,1);a.serverTick();
        h.assertTrue(send(a,Direction.WEST,32,1)==0,"spun-up open circuit does not consume another packet");
        var pos=a.getBlockPos();h.getLevel().setBlock(pos,a.getBlockState().setValue(AxleBlock.NORTH,true),3);
        h.assertTrue(send(a,Direction.WEST,32,1)==0,"invalid branched axle state cannot route around straight-axis restriction");h.succeed();
    }
    @GameTest(template="test_empty")
    public static void overloadPopsOffExactlyOnceAndRejectsRemovedEntity(GameTestHelper h){
        var pos=new BlockPos(1,1,1);var a=place(h,pos);a.serverTick();
        for(long i=0;i<a.maxPower();i++)h.assertTrue(send(a,Direction.WEST,32,1)==1&&!a.isRemoved(),"within cumulative rated power");
        h.assertTrue(send(a,Direction.WEST,32,7)==7&&a.isRemoved(),"overload consumes offer and detaches axle");
        h.assertTrue(send(a,Direction.WEST,32,7)==0,"removed object cannot duplicate drops");
        h.runAfterDelay(1,()->{int count=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(h.absolutePos(pos)).inflate(1)).stream().filter(e->e.getItem().is(block().asItem())).mapToInt(e->e.getItem().getCount()).sum();h.assertTrue(count==1,"overloaded axle drops itself exactly once");h.succeed();});
    }
    @GameTest(template="test_empty")
    public static void rotationPersistsSynchronizesAndStopsAfterInputCeases(GameTestHelper h){
        var a=place(h,new BlockPos(1,1,1));for(int i=0;i<6;i++)a.serverTick();send(a,Direction.WEST,32,1);a.serverTick();
        h.assertTrue(a.rotationDirection()==2&&a.transferredLast()==32,"west positive input uses original face/sign direction and startup energy");
        var restored=new AxleBlockEntity(a.getBlockPos(),a.getBlockState());restored.load(a.saveWithoutMetadata());
        h.assertTrue(restored.rotationDirection()==2&&restored.transferredLast()==0,"rotation persists, transient energy does not");
        h.assertTrue(a.getUpdatePacket().getTag().getByte("Rotation")==2&&a.getModelData().get(AxleBlockEntity.ROTATION)==2,"packet and model data expose the same rotation");
        h.runAfterDelay(3,()->{h.assertTrue(a.rotationDirection()==0&&a.getUpdateTag().getByte("Rotation")==0,"real server ticker stops and synchronizes after no input");h.succeed();});
    }
    @GameTest(template="test_empty")
    public static void farNeighborConnectionAndOriginalPhysicalProperties(GameTestHelper h){
        var left=place(h,new BlockPos(1,1,1));var center=place(h,new BlockPos(2,1,1));var right=place(h,new BlockPos(3,1,1));
        h.getLevel().setBlock(right.getBlockPos(),block().defaultBlockState(),3);center.autoConnectOnPlace(Direction.EAST);
        h.assertTrue(right.getBlockState().getValue(AxleBlock.WEST)&&!right.getBlockState().getValue(AxleBlock.EAST),"far neighbor connects toward placed axle, not away from it");
        for(var entry:GTAxles.all()) {var b=entry.get();int size=b.spec().size();double diameter=switch(size){case 1->6;case 2->9;case 3->12;default->16;};
            h.assertTrue(b.spec().thickness()==diameter&&b.spec().lossPerBlock()==0,"original diameter and no speed loss");
            h.assertTrue(b.asItem().getDefaultInstance().getMaxStackSize()==(size<=2?64:size==3?32:16),"original axle stack limit");
        }
        h.succeed();
    }
}
