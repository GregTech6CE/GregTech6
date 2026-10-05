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
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Cover-only sync preserves native inventories and uses registry-aware stack storage. */
public record PacketSyncComponentCovers(BlockPos pos,ResourceLocation entityType,CompoundTag tag) implements CustomPacketPayload {
    public static final Type<PacketSyncComponentCovers> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("gregtech","sync_component_covers"));
    public static final StreamCodec<RegistryFriendlyByteBuf,PacketSyncComponentCovers> STREAM_CODEC=StreamCodec.of(PacketSyncComponentCovers::encode,PacketSyncComponentCovers::decode);
    public PacketSyncComponentCovers{pos=pos.immutable();tag=tag.copy();}
    public static PacketSyncComponentCovers of(BlockEntity owner,CompoundTag tag){return new PacketSyncComponentCovers(owner.getBlockPos(),net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(owner.getType()),tag);}
    private static void encode(RegistryFriendlyByteBuf buf,PacketSyncComponentCovers packet){buf.writeBlockPos(packet.pos);buf.writeResourceLocation(packet.entityType);buf.writeNbt(packet.tag);}
    private static PacketSyncComponentCovers decode(RegistryFriendlyByteBuf buf){return new PacketSyncComponentCovers(buf.readBlockPos(),buf.readResourceLocation(),java.util.Objects.requireNonNull(buf.readNbt()));}
    @Override public Type<PacketSyncComponentCovers> type(){return TYPE;}
    public void apply(net.minecraft.world.level.Level level){
        if(level==null||!level.hasChunkAt(pos))return;
        var owner=level.getBlockEntity(pos);
        if(owner!=null&&entityType.equals(net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(owner.getType())))
            com.gregtech.gregtech.content.cover.ComponentCoverFallback.read(owner,tag,level.registryAccess());
    }
    public static void send(net.minecraft.server.level.ServerPlayer player,PacketSyncComponentCovers packet){net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,packet);}
}
