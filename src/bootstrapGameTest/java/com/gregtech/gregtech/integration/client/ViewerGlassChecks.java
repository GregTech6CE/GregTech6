package com.gregtech.gregtech.integration.client;
import com.google.gson.JsonObject;
import com.gregtech.gregtech.block.misc.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;
import java.nio.file.*;
import java.util.UUID;
final class ViewerGlassChecks {
    private static int phase,frames;
    private static java.util.concurrent.CompletableFuture<?> prepared;
    private static volatile Throwable failure;
    static boolean capture(Minecraft mc,JsonObject receipt) {
        if(failure!=null)throw new IllegalStateException("Actual glass scene failed",failure);
        if(phase==3)return true;
        var slab=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gregtech","glass_glow_slab"));
        if(phase==0) {
            if(!(slab instanceof ColoredGlassSlabBlock))throw new IllegalStateException("Glass half uses opaque foam class");
            var glass=(ColoredGlassSlabBlock)slab;var white=slab.defaultBlockState().setValue(CFoamSlabBlock.COLOR,DyeColor.WHITE);
            int models=0;
            for(var facing:Direction.values()) {
                var state=white.setValue(CFoamSlabBlock.FACING,facing);
                var types=mc.getBlockRenderer().getBlockModel(state).getRenderTypes(state,net.minecraft.util.RandomSource.create(1),net.minecraftforge.client.model.data.ModelData.EMPTY);
                if(!types.contains(net.minecraft.client.renderer.RenderType.translucent()))throw new IllegalStateException("Glass half did not bake translucent: "+facing);
                if(glass.getLightBlock(state,mc.level,BlockPos.ZERO)!=0||glass.getShadeBrightness(state,mc.level,BlockPos.ZERO)!=1)throw new IllegalStateException("Glass half blocks light");models++;
            }
            var down=white.setValue(CFoamSlabBlock.FACING,Direction.DOWN);var up=white.setValue(CFoamSlabBlock.FACING,Direction.UP);
            if(!glass.skipRendering(down,down,Direction.EAST)||glass.skipRendering(down,up,Direction.EAST)
                ||glass.skipRendering(down,down.setValue(CFoamSlabBlock.COLOR,DyeColor.RED),Direction.EAST)
                ||!glass.skipRendering(down,white.setValue(CFoamSlabBlock.FACING,Direction.WEST),Direction.EAST)
                ||glass.skipRendering(down,white.setValue(CFoamSlabBlock.FACING,Direction.EAST),Direction.EAST)
                ||glass.skipRendering(down,up,Direction.UP))throw new IllegalStateException("Glass half touching-face culling mismatch");
            receipt.addProperty("glassSlabTranslucentModelsChecked",models);receipt.addProperty("glassSlabTouchingFaceChecks",6);
            var server=mc.getSingleplayerServer();prepared=server.submit(()->{
                var player=server.getPlayerList().getPlayers().get(0);var level=player.serverLevel();var origin=player.blockPosition().offset(0,15,20);
                for(int x=-10;x<=10;x++)for(int y=-1;y<=5;y++)for(int z=-7;z<=11;z++)level.setBlock(origin.offset(x,y,z),Blocks.AIR.defaultBlockState(),3);
                for(int x=-9;x<=9;x++)for(int y=0;y<=4;y++)level.setBlock(origin.offset(x,y,-6),((x+y)%2==0?Blocks.BLUE_WOOL:Blocks.YELLOW_WOOL).defaultBlockState(),3);
                var full=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gregtech","glass_glow")).defaultBlockState();
                if(!full.hasProperty(ColoredGlassBlock.COLOR))throw new IllegalStateException("Source full glass unavailable");full=full.setValue(ColoredGlassBlock.COLOR,DyeColor.WHITE);
                for(int lane=0;lane<3;lane++)for(int row=0;row<3;row++)for(int x=0;x<3;x++)for(int y=1;y<=3;y++) {
                    var state=lane==0?full:lane==1?down:white.setValue(CFoamSlabBlock.FACING,Direction.WEST);
                    level.setBlock(origin.offset(-8+lane*6+x,y,-row*2),state,3);
                }
                player.setGameMode(GameType.SPECTATOR);
                player.connection.teleport(origin.getX()+.5,origin.getY()+2.25-1.62,origin.getZ()+10.5,180,0);
                level.setDayTime(6000);return null;
            });phase=1;return false;
        }
        if(phase==1) {
            if(!prepared.isDone())return false;prepared.join();
            if(++frames<80)return false;
            phase=2;String name="glass-halves-"+UUID.randomUUID()+".png";
            Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),ignored->{try{var path=mc.gameDirectory.toPath().resolve("screenshots").resolve(name).toAbsolutePath();if(!Files.isRegularFile(path)||Files.size(path)==0)throw new IllegalStateException("Glass scene screenshot missing");receipt.addProperty("glassSlabWorldScreenshot",path.toString());receipt.addProperty("glassSlabWorldLanes",3);phase=3;}catch(Throwable t){failure=t;}});
        }
        return false;
    }
}
