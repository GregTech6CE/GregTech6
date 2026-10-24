package gregtech.gametest;

import gregtech.block.energy.SignalWireBlock;
import gregtech.content.cover.CoverWorldInteraction;
import gregtech.content.cover.PanelCoverHost;
import gregtech.registry.GTSignalWires;
import gregtech.registry.GTTechnological;
import gregtech.content.cover.CoverPlacement;
import gregtech.content.cover.CoverItems;
import gregtech.block.misc.PileBlock;
import gregtech.blockentity.misc.PileBlockEntity;
import gregtech.api.tool.GTToolType;
import gregtech.item.GTToolItem;
import gregtech.api.material.GTMaterialRegistry;
import gregtech.registry.GTItems;
import gregtech.data.MaterialPrefix;
import java.util.LinkedHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Actual shared overlay predicates plus native pile interaction controls; no GPU assertion. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class PlatePileHighlightTests {
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private record Saved(BlockState state, CompoundTag data) {}
    private static SignalWireBlock wire(boolean insulated) {
        return GTSignalWires.all().stream().map(r -> r.get())
                .filter(b -> b.material().getName().equals("RedAlloy") && b.insulated()==insulated).findFirst().orElseThrow();
    }
    private static final class Fixture implements AutoCloseable {
        final GameTestHelper h;
        final BlockPos pos;
        final ServerPlayer player;
        final LinkedHashMap<BlockPos,Saved> saved = new LinkedHashMap<>();
        Fixture(GameTestHelper h,int index) {this(h,index,true);}
        Fixture(GameTestHelper h,int index,boolean insulated) {
            this.h=h; pos=new BlockPos(22_480_008+index*32,230,22_480_008);
//? if >=1.21.1 {
/*            player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),
*///?} else {
            player=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),
//?}
                    new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"[gt-pile-grid]"));
            player.setGameMode(GameType.SURVIVAL); player.getInventory().clearContent(); player.setShiftKeyDown(false);
            for(var mutable:BlockPos.betweenClosed(pos.offset(-2,-2,-2),pos.offset(2,2,2))) {
                var at=mutable.immutable(); var be=h.getLevel().getBlockEntity(at);
//? if >=1.21.1 {
/*                var data=be==null?null:be.saveWithoutMetadata(h.getLevel().registryAccess());
*///?} else {
                var data=be==null?null:be.saveWithoutMetadata();
//?}
                saved.put(at,new Saved(h.getLevel().getBlockState(at),data));
            }
            saved.keySet().forEach(at->set(at,Blocks.AIR.defaultBlockState()));
            set(pos,wire(insulated).defaultBlockState());
        }
        void set(BlockPos at,BlockState state) {
            h.getLevel().removeBlockEntity(at); h.getLevel().setBlock(at,Blocks.AIR.defaultBlockState(),FLAGS);
            h.getLevel().setBlock(at,state,FLAGS);
        }
        PanelCoverHost host(){return (PanelCoverHost)h.getLevel().getBlockEntity(pos);}
        void click(Direction side,ItemStack stack) {
            player.setItemInHand(InteractionHand.MAIN_HAND,stack);
            var normal=Vec3.atLowerCornerOf(side.getNormal()); var point=Vec3.atCenterOf(pos).add(normal.scale(.5));
            var stand=Vec3.atCenterOf(pos).add(normal.scale(2.5));player.setPos(stand.x,stand.y,stand.z);
            player.gameMode.useItemOn(player,h.getLevel(),stack,InteractionHand.MAIN_HAND,new BlockHitResult(point,side,pos,false));
        }
        @Override public void close() {
            player.closeContainer();player.getInventory().clearContent();player.discard();
            saved.keySet().forEach(at->set(at,Blocks.AIR.defaultBlockState()));
            for(var entry:saved.entrySet()) {
                set(entry.getKey(),entry.getValue().state());var be=h.getLevel().getBlockEntity(entry.getKey());
                if(be!=null&&entry.getValue().data()!=null) {
//? if >=1.21.1 {
/*                    be.loadWithComponents(entry.getValue().data(),h.getLevel().registryAccess());
*///?} else {
                    be.load(entry.getValue().data());
//?}
                }
            }
        }
    }
    private static ItemStack plate(String material) {
        return GTItems.getStack(MaterialPrefix.plate,GTMaterialRegistry.get(material));
    }
    @GameTest(template="test_empty",timeoutTicks=100)
    public static void loosePilesNeverSelectCoverGrid(GameTestHelper h) {
        try(var f=new Fixture(h,0)) {
            var matching=plate("Iron");var different=plate("Copper");
            h.assertTrue(!matching.isEmpty()&&!different.isEmpty()&&CoverItems.isCover(matching)&&CoverItems.isCover(different),"real plate cover candidates");
            boolean rejected=true;
            for(var kind:PileBlock.Kind.values()) {
                f.set(f.pos,kind.block().defaultBlockState());
                for(var held:new ItemStack[]{matching,different}) {
                    f.player.setItemInHand(InteractionHand.MAIN_HAND,held.copy());
                    rejected &= !CoverPlacement.coverGrid(h.getLevel(),f.pos,f.player);
                    rejected &= !CoverPlacement.fullSelection(h.getLevel(),f.pos,h.getLevel().getBlockState(f.pos),f.player);
                    f.player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
                    f.player.setItemInHand(InteractionHand.OFF_HAND,held.copy());
                    rejected &= !CoverPlacement.coverGrid(h.getLevel(),f.pos,f.player);
                    f.player.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
                }
            }
            f.set(f.pos,PileBlock.Kind.PLATE.block().defaultBlockState());
            var pile=(PileBlockEntity)h.getLevel().getBlockEntity(f.pos);
            f.click(Direction.UP,matching.copyWithCount(3));
            h.assertTrue(pile.count()==3&&f.player.getMainHandItem().isEmpty(),"native matching click stacks three plates");
            f.player.setItemInHand(InteractionHand.MAIN_HAND,matching.copy());
            rejected &= !CoverPlacement.coverGrid(h.getLevel(),f.pos,f.player);
            f.player.setItemInHand(InteractionHand.MAIN_HAND,different.copy());
            rejected &= !CoverPlacement.coverGrid(h.getLevel(),f.pos,f.player);
            f.click(Direction.UP,different.copy());
            h.assertTrue(pile.count()==2&&f.player.getMainHandItem().getCount()==1,"nonmatching plate retrieves one without consuming held plate");
            f.click(Direction.UP,ItemStack.EMPTY);
            h.assertTrue(pile.count()==1,"empty hand retrieves one");
            h.assertTrue(rejected,"loose piles have no cover grid/full targeting cube with main/offhand plates");
        }
        h.succeed();
    }
    @GameTest(template="test_empty",timeoutTicks=100)
    public static void legitimateCoverAndToolGridsRemain(GameTestHelper h) {
        try(var f=new Fixture(h,1)) {
            f.player.setItemInHand(InteractionHand.MAIN_HAND,plate("Iron"));
            h.assertTrue(CoverPlacement.coverGrid(h.getLevel(),f.pos,f.player),"ordinary wire retains plate cover grid");
            f.player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            f.player.setItemInHand(InteractionHand.OFF_HAND,plate("Iron"));
            h.assertTrue(CoverPlacement.coverGrid(h.getLevel(),f.pos,f.player),"empty main hand permits offhand plate grid");
            f.player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.APPLE));
            h.assertTrue(!CoverPlacement.coverGrid(h.getLevel(),f.pos,f.player),"nonempty main hand suppresses offhand cover grid");
            f.player.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            var crowbar=GTToolItem.create(GTToolType.CROWBAR,GTMaterialRegistry.get("Steel"),GTMaterialRegistry.get("Wood"));
            f.player.setItemInHand(InteractionHand.MAIN_HAND,crowbar);
            h.assertTrue(!CoverPlacement.coverGrid(h.getLevel(),f.pos,f.player),"crowbar without attached cover has no cover grid");
            h.assertTrue(f.host().attachCover(Direction.NORTH,new ItemStack(GTTechnological.get("blank_cover"))),"ordinary wire accepts cover");
            h.assertTrue(CoverPlacement.coverGrid(h.getLevel(),f.pos,f.player),"crowbar on attached cover retains removal grid");
            f.host().removeCover(Direction.NORTH);
            f.player.setItemInHand(InteractionHand.MAIN_HAND,GTToolItem.create(GTToolType.WIRE_CUTTER,GTMaterialRegistry.get("Steel"),GTMaterialRegistry.get("Wood")));
            h.assertTrue(CoverPlacement.fullSelection(h.getLevel(),f.pos,h.getLevel().getBlockState(f.pos),f.player),"wire cutter connection targeting remains enabled");
        }
        h.succeed();
    }
}
