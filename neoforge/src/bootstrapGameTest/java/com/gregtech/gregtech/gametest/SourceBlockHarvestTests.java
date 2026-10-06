package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.tool.*;
import com.gregtech.gregtech.data.BlockHarvestPolicy;
import com.gregtech.gregtech.item.GTToolItem;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import java.util.UUID;

/** Two finite native harvest boundaries, prepared for pooled acceptance with other source batches. */
@net.neoforged.neoforge.gametest.GameTestHolder("gregtech_block_properties")
@net.neoforged.neoforge.gametest.PrefixGameTestTemplate(false)
public final class SourceBlockHarvestTests {
    @GameTest(template="test_empty",timeoutTicks=60)
    public static void originalWoodAndUtilityGroupsDropWithEmptyHands(GameTestHelper h) {
        var player = player(h); int x=2;
        for (String name : new String[]{"gearbox_wood","mortar_diamond","engine_steam_ironwood"}) {
            var pos = h.absolutePos(new BlockPos(x,3,3));
            var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("gregtech:"+name));
            var state = block.defaultBlockState();
            h.getLevel().setBlockAndUpdate(pos,state);
            player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            h.assertTrue(BlockHarvestPolicy.source(block).orElseThrow().handHarvestable(),"audited original exemption");
            h.assertTrue(player.hasCorrectToolForDrops(state,h.getLevel(),pos),"actual platform player harvest check permits hand group "+name);
            h.assertTrue(player.gameMode.destroyBlock(pos),"real survival block destruction");
            var drops = h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(0.8),e -> e.getItem().is(block.asItem()));
            h.assertTrue(drops.stream().mapToInt(e -> e.getItem().getCount()).sum() == 1,"one actual hand-harvest block drop "+name);
            drops.forEach(ItemEntity::discard); x+=3;
        }
        h.succeed();
    }
    @GameTest(template="test_empty",timeoutTicks=60)
    public static void originalIridiumMachineRejectsHandWrongToolAndLowerWrenchTier(GameTestHelper h) {
        var player = player(h); var pos = h.absolutePos(new BlockPos(3,3,3));
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("gregtech:gearbox_iridium"));
        var state = block.defaultBlockState(); h.getLevel().setBlockAndUpdate(pos,state);
        var low = GTToolItem.create(GTToolType.WRENCH,GTMaterialRegistry.get("Steel"),GTMaterialRegistry.get("Wood"));
        var high = GTToolItem.create(GTToolType.WRENCH,GTMaterialRegistry.get("Iridium"),GTMaterialRegistry.get("Wood"));
        h.assertTrue(BlockHarvestPolicy.level(block) > GTToolHelper.getHarvestLevel(low),"source Iridium exceeds Steel wrench tier");
        for (var wrong : new ItemStack[]{ItemStack.EMPTY,new ItemStack(net.minecraft.world.item.Items.NETHERITE_PICKAXE),low}) {
            player.setItemInHand(InteractionHand.MAIN_HAND,wrong);
            h.assertTrue(!(player.hasCorrectToolForDrops(state,h.getLevel(),pos)),"actual native harvest event rejects wrong/insufficient tool");
        }
        player.setItemInHand(InteractionHand.MAIN_HAND,high);
        h.assertTrue(player.hasCorrectToolForDrops(state,h.getLevel(),pos),"original matching Iridium wrench can harvest");
        player.gameMode.destroyBlock(pos);
        var drops = h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(0.8),e -> e.getItem().is(block.asItem()));
        h.assertTrue(drops.stream().mapToInt(e -> e.getItem().getCount()).sum() == 1,"real correct wrench produces one gearbox drop");
        h.assertTrue(high.getDamageValue() > 0,"native block harvest consumes wrench durability");
        h.succeed();
    }
    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h) {
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(),"GTSourceHarvest"));
        player.setGameMode(GameType.SURVIVAL); return player;
    }
}
