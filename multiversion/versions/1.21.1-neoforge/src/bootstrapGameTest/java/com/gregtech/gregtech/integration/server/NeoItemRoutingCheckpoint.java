package com.gregtech.gregtech.integration.server;

import com.gregtech.gregtech.block.machine.ItemPipeBlock;
import com.gregtech.gregtech.blockentity.machine.ItemPipeBlockEntity;
import com.gregtech.gregtech.content.transport.ItemPipeTransferAdapter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.items.ItemStackHandler;

/** One grouped weighted-routing/conservation checkpoint. No production-jar test registration. */
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID,value=Dist.DEDICATED_SERVER)
public final class NeoItemRoutingCheckpoint {
    private static final BlockPos ORIGIN=new BlockPos(128,100,128);
    private static final BlockPos FULL=new BlockPos(160,100,128);
    private static ServerLevel world;
    private static int ticks;
    private static boolean finished;

    private NeoItemRoutingCheckpoint() {}

    private static void require(boolean ok,String reason) {
        if(!ok)throw new IllegalStateException("Item routing checkpoint: "+reason);
    }
    private static ItemStack named(Item item,int count,String name) {
        ItemStack stack=new ItemStack(item,count);
        stack.set(DataComponents.CUSTOM_NAME,Component.literal(name));return stack;
    }
    private static ItemPipeBlock pipe(String name) {
        Block block=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gregtech",name));
        require(block instanceof ItemPipeBlock,"actual factory "+name);return (ItemPipeBlock)block;
    }
    private static void place(BlockPos pos,net.minecraft.world.level.block.state.BlockState state) {
        require(world.getBlockState(pos).isAir(),"fresh fixture position "+pos);
        require(world.setBlock(pos,state,Block.UPDATE_ALL),"place "+pos);
    }
    private static ItemPipeBlockEntity entity(BlockPos pos) {
        return (ItemPipeBlockEntity)world.getBlockEntity(pos);
    }
    private static ChestBlockEntity chest(BlockPos pos) {
        return (ChestBlockEntity)world.getBlockEntity(pos);
    }
    private static int total(ChestBlockEntity chest,Item item) {
        int count=0;for(int i=0;i<chest.getContainerSize();i++)if(chest.getItem(i).is(item))count+=chest.getItem(i).getCount();return count;
    }

    @SubscribeEvent public static void started(ServerStartedEvent event) {
        if(!Boolean.getBoolean("gregtech.integration.itemRoutingSmoke"))return;
        world=event.getServer().overworld();
        for(int[] chunk:new int[][]{{8,8},{8,7},{10,8}})world.setChunkForced(chunk[0],chunk[1],true);
        var ordinary=pipe("item_pipe_medium_ultimet").defaultBlockState();
        // BFS reaches the very heavy down branch before the cheaper extra-hop east branch.
        // A separate north candidate costs 8192; the correct east route costs 4096.
        place(ORIGIN,ordinary.setValue(ItemPipeBlock.WEST,true).setValue(ItemPipeBlock.NORTH,true).setValue(ItemPipeBlock.EAST,true));
        place(ORIGIN.north(),pipe("item_pipe_medium_platinum").defaultBlockState().setValue(ItemPipeBlock.SOUTH,true).setValue(ItemPipeBlock.NORTH,true));
        place(ORIGIN.north(2),Blocks.CHEST.defaultBlockState());
        place(ORIGIN.east(),ordinary.setValue(ItemPipeBlock.WEST,true).setValue(ItemPipeBlock.DOWN,true).setValue(ItemPipeBlock.EAST,true));
        place(ORIGIN.east().below(),pipe("item_pipe_restrictive_medium_brass").defaultBlockState().setValue(ItemPipeBlock.UP,true).setValue(ItemPipeBlock.DOWN,true));
        place(ORIGIN.east().below(2),Blocks.CHEST.defaultBlockState());
        place(ORIGIN.east(2),ordinary.setValue(ItemPipeBlock.WEST,true).setValue(ItemPipeBlock.EAST,true));
        place(ORIGIN.east(3),Blocks.CHEST.defaultBlockState());
        var ingress=world.getCapability(Capabilities.ItemHandler.BLOCK,ORIGIN,Direction.WEST);
        require(ingress!=null&&ingress.insertItem(0,named(Items.CARROT,4,"weighted-marker"),false).isEmpty(),"actual weighted ingress");

        place(FULL,ordinary.setValue(ItemPipeBlock.WEST,true).setValue(ItemPipeBlock.EAST,true));
        place(FULL.east(),ordinary.setValue(ItemPipeBlock.WEST,true).setValue(ItemPipeBlock.EAST,true));
        place(FULL.east(2),Blocks.CHEST.defaultBlockState());
        var blocked=chest(FULL.east(2));for(int i=0;i<blocked.getContainerSize();i++)blocked.setItem(i,new ItemStack(Items.COBBLESTONE,64));
        var fullIngress=world.getCapability(Capabilities.ItemHandler.BLOCK,FULL,Direction.WEST);
        require(fullIngress!=null&&fullIngress.insertItem(0,named(Items.CARROT,4,"full-exit-marker"),false).isEmpty(),"full-target ingress");

        BlockPos modePos=new BlockPos(144,100,128);
        place(modePos,ordinary.setValue(ItemPipeBlock.WEST,true));
        var mode=entity(modePos);var side=world.getCapability(Capabilities.ItemHandler.BLOCK,modePos,Direction.WEST);
        require(side!=null&&side.insertItem(0,named(Items.CARROT,2,"mask-marker"),false).isEmpty(),"side-mask setup");
        mode.toggleSideIO(Direction.WEST);require(side.extractItem(0,1,true).isEmpty()&&side.insertItem(0,named(Items.CARROT,1,"mask-marker"),true).isEmpty()&&side.insertItem(0,new ItemStack(Items.CARROT),true).getCount()==1,"output disabled and distinct components do not merge");
        mode.toggleSideIO(Direction.WEST);require(side.insertItem(0,named(Items.CARROT,1,"mask-marker"),true).getCount()==1&&side.extractItem(0,1,true).getCount()==1,"input disabled, output enabled");
        mode.toggleSideIO(Direction.WEST);require(side.insertItem(0,named(Items.CARROT,1,"mask-marker"),true).getCount()==1&&side.extractItem(0,1,true).isEmpty(),"both directions disabled");
        mode.toggleSideIO(Direction.WEST);require(side.insertItem(0,named(Items.CARROT,1,"mask-marker"),true).isEmpty()&&side.extractItem(0,1,true).getCount()==1,"both directions restored");

        checkRemainders();
        com.mojang.logging.LogUtils.getLogger().info("ITEM_ROUTING_CHECKPOINT_PREPARED remainderAccounting=true destinationRefresh=true partialException=true sideMasks=true");
    }

    private static void checkRemainders() {
        var offered=named(Items.CARROT,8,"remainder-marker");
        var raced=new ItemStackHandler(1) {
            @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate) {
                if(simulate)return ItemStack.EMPTY;
                int accepted=Math.min(3,stack.getCount());setStackInSlot(slot,stack.copyWithCount(accepted));
                return stack.copyWithCount(stack.getCount()-accepted);
            }
        };
        var result=ItemPipeTransferAdapter.transfer(offered,()->raced);
        require(result.planned()==8&&result.accepted()==3&&!result.handlerFailed()&&raced.getStackInSlot(0).getCount()==3
                &&ItemStack.isSameItemSameComponents(raced.getStackInSlot(0),offered)&&offered.getCount()==8,"simulation eight, actual three, offered copy preserved");
        int[] lookups={0};var disappeared=ItemPipeTransferAdapter.transfer(offered,()->++lookups[0]==1?raced:null);
        require(disappeared.planned()==8&&disappeared.accepted()==0&&lookups[0]==2,"destination refreshed and missing actual capability retains source");
        var partial=new ItemStackHandler(2) {
            @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate) {
                if(simulate)return ItemStack.EMPTY;
                if(slot==1)throw new IllegalStateException("intentional checkpoint endpoint exception");
                int accepted=Math.min(3,stack.getCount());setStackInSlot(slot,stack.copyWithCount(accepted));return stack.copyWithCount(stack.getCount()-accepted);
            }
        };
        var committed=ItemPipeTransferAdapter.transfer(offered,()->partial);
        require(committed.planned()==8&&committed.accepted()==3&&committed.handlerFailed()&&partial.getStackInSlot(0).getCount()==3,"validated first-slot delivery survives later handler exception");
    }

    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        if(world==null||finished)return;
        ticks++;
        if(ticks==80) {
            var destination=chest(ORIGIN.east(3));
            require(total(destination,Items.CARROT)==4&&total(chest(ORIGIN.north(2)),Items.CARROT)==0
                    &&total(chest(ORIGIN.east().below(2)),Items.CARROT)==0&&entity(ORIGIN).getStackInSlot(0).isEmpty(),"actual cheapest weighted route over BFS-first branch");
            require(ItemStack.isSameItemSameComponents(destination.getItem(0),named(Items.CARROT,4,"weighted-marker")),"weighted routed components");
            require(entity(FULL).getStackInSlot(0).getCount()==4&&entity(FULL.east()).getStackInSlot(0).isEmpty()
                    &&total(chest(FULL.east(2)),Items.COBBLESTONE)==27*64,"full endpoint rejected during route discovery without moving source");
            chest(FULL.east(2)).setItem(26,ItemStack.EMPTY);
        }
        if(ticks==120) {
            require(entity(FULL).getStackInSlot(0).isEmpty()&&entity(FULL.east()).getStackInSlot(0).isEmpty()
                    &&total(chest(FULL.east(2)),Items.CARROT)==4&&total(chest(FULL.east(2)),Items.COBBLESTONE)==26*64,"route resumes after actual capacity opens, no loss or duplication");
            finished=true;com.mojang.logging.LogUtils.getLogger().info("ITEM_ROUTING_CHECKPOINT_SUCCESS weightedWorldRoute=true acceptingTargets=true resumeAfterCapacity=true components=true remainderAccounting=true destinationRefresh=true partialException=true sideMasks=true observedTicks={}",ticks);
        }
    }
}
