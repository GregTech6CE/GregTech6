/**
 * Copyright (c) 2021 GregTech-6 Team
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */

package com.gregtech.gregtech.network;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Cover-only sync preserves the native inventory/menu packet contract. */
public record PacketSyncComponentCovers(BlockPos pos,ResourceLocation type,CompoundTag tag) {
    public PacketSyncComponentCovers{pos=pos.immutable();tag=tag.copy();}
    public static PacketSyncComponentCovers of(BlockEntity owner,CompoundTag tag){return new PacketSyncComponentCovers(owner.getBlockPos(),net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(owner.getType()),tag);}
    public static void encode(PacketSyncComponentCovers packet,FriendlyByteBuf buf){buf.writeBlockPos(packet.pos);buf.writeResourceLocation(packet.type);buf.writeNbt(packet.tag);}
    public static PacketSyncComponentCovers decode(FriendlyByteBuf buf){return new PacketSyncComponentCovers(buf.readBlockPos(),buf.readResourceLocation(),java.util.Objects.requireNonNull(buf.readNbt()));}
    public static void handle(PacketSyncComponentCovers packet,java.util.function.Supplier<net.minecraftforge.network.NetworkEvent.Context> context){
        context.get().enqueueWork(()->net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->com.gregtech.gregtech.client.ComponentCoverFallbackRenderer.receive(packet)));
        context.get().setPacketHandled(true);
    }
    public static void send(net.minecraft.server.level.ServerPlayer player,PacketSyncComponentCovers packet){GTPackets.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(()->player),packet);}
}
