package com.gregtech.gregtech.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

/** Server interaction entry points require a server actor with an explicit game mode. */
final class ToolInteractionActors {
    static ServerPlayer survival(GameTestHelper h, BlockPos near) {
        var player=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"[gt-tool-test]"));
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().clearContent();
        player.setShiftKeyDown(false);
        player.setPos(near.getX()+.5,near.getY()+1,near.getZ()-4.5);
        return player;
    }
}
