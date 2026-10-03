package com.gregtech.gregtech.blockentity.inventory;

import com.gregtech.gregtech.block.inventory.SafeBlock;
import com.gregtech.gregtech.item.GTDungeonKeyItem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import javax.annotation.Nullable;
import java.util.UUID;

/** GT6 mechanical/key safe: 15 slots, front access, no automation capability. */
public class SafeBlockEntity extends BlockEntity {
    private final SimpleContainer inventory = new SimpleContainer(com.gregtech.gregtech.content.storage.SafeLockRules.SLOTS);
    @Nullable private UUID owner;
    private long keyId;
    private boolean opened;
    @Nullable private ResourceLocation dungeonLoot;
    private long lootSeed;
    private final java.util.List<ItemStack> overflow = new java.util.ArrayList<>();

    public SafeBlockEntity(BlockPos pos, BlockState state) {
        super(com.gregtech.gregtech.registry.GTBlockEntities.SAFE.get(), pos, state);
        inventory.addListener(c -> setChanged());
    }
    public SimpleContainer inventory() { return inventory; }
    public void setOwner(@Nullable UUID owner) { this.owner = owner; syncLock(); }
    public boolean keyLocked() { return ((SafeBlock) getBlockState().getBlock()).keyLocked(); }
    public long keyId() { return keyId; }
    public boolean opened() { return opened; }
    public void setKeyId(long id) { keyId = id; opened = false; syncLock(); }
    public boolean canOpen(Player player) {
        return com.gregtech.gregtech.content.storage.SafeLockRules.canOpen(keyLocked(), opened, owner, player.getUUID());
    }
    /** GT6 mechanical safe claims its first interacting player, or a sneaking placer. */
    public boolean claimAndOpen(Player player) {
        if (level != null && level.isClientSide) return canOpen(player);
        if (!keyLocked() && owner == null) setOwner(player.getUUID());
        return canOpen(player);
    }
    /** GT6 Behavior_Key: bind blank locks, toggle matching keys, copy only while unlocked. */
    public boolean useKey(ItemStack key) {
        if (!keyLocked() || !(key.getItem() instanceof GTDungeonKeyItem) || level == null || level.isClientSide) return false;
        long supplied = GTDungeonKeyItem.keyId(key), generated = 0;
        if (supplied == 0 && keyId == 0) do { generated = level.random.nextLong() & Long.MAX_VALUE; } while (generated == 0);
        var result = com.gregtech.gregtech.content.storage.SafeLockRules.useKey(keyId, opened, supplied, generated);
        if (!result.accepted()) return false;
        if (result.itemId() != supplied) GTDungeonKeyItem.setKeyId(key, result.itemId());
        if (!result.toggle()) return true;
        keyId = result.lockId(); opened = result.opened();
        syncLock();
        level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.LEVER_CLICK,
                net.minecraft.sounds.SoundSource.BLOCKS, 1, .25F);
        return true;
    }
    private void syncLock() {
        setChanged();
        if (level != null && !level.isClientSide && getBlockState().hasProperty(SafeBlock.OPEN))
            level.setBlock(worldPosition, getBlockState().setValue(SafeBlock.OPEN, opened), 3);
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
    @Override public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider lookup) {
        var tag = new CompoundTag(); if (owner != null) tag.putUUID("gt.owner", owner); tag.putBoolean("gt.open", opened); return tag;
    }
    @Override public void handleUpdateTag(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) { owner = tag.hasUUID("gt.owner") ? tag.getUUID("gt.owner") : null; opened = tag.getBoolean("gt.open"); }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() { return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this); }
    public void setDungeonLoot(ResourceLocation table, long seed) { dungeonLoot = table; lootSeed = seed; setChanged(); }
    /** GT6 rolls one candidate stack per empty slot; preserve preplaced keys and consume the table once. */
    public void generateDungeonLoot() {
        if (dungeonLoot == null || !(level instanceof net.minecraft.server.level.ServerLevel server)) return;
        var table = server.getServer().reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, dungeonLoot));
        if (table == net.minecraft.world.level.storage.loot.LootTable.EMPTY) return;
        var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(server)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,
                        net.minecraft.world.phys.Vec3.atCenterOf(worldPosition))
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
        var random = net.minecraft.util.RandomSource.create(lootSeed);
        dungeonLoot = null;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) if (inventory.getItem(slot).isEmpty()) {
            var candidates = table.getRandomItems(params, random.nextLong());
            if (!candidates.isEmpty()) inventory.setItem(slot, candidates.get(random.nextInt(candidates.size())).copy());
        }
        setChanged();
    }
    public com.gregtech.gregtech.client.gui.HopperContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory playerInventory) {
        // Keep contents from older 27-slot test saves; refill the 15-slot inventory as room becomes available.
        for (int i = overflow.size() - 1; i >= 0; i--) {
            var rest = inventory.addItem(overflow.get(i));
            if (rest.isEmpty()) overflow.remove(i); else overflow.set(i, rest);
        }
        setChanged();
        generateDungeonLoot();
        return new com.gregtech.gregtech.client.gui.HopperContainerMenu(com.gregtech.gregtech.registry.GTMenuTypes.forSlotCount(15),
                id, playerInventory, new net.neoforged.neoforge.items.wrapper.InvWrapper(inventory)) {
            @Override public boolean stillValid(Player player) {
                return !isRemoved() && level != null && level.getBlockEntity(worldPosition) == SafeBlockEntity.this
                        && canOpen(player) && player.distanceToSqr(worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5) <= 64;
            }
        };
    }
    public void dropContents() {
        if (level == null || level.isClientSide) return;
        generateDungeonLoot();
        com.gregtech.gregtech.util.GTItemDrops.dropContents(level, worldPosition, inventory);
        inventory.clearContent();
        for (var stack : overflow) com.gregtech.gregtech.util.GTItemDrops.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
        overflow.clear(); setChanged();
    }
    @Override protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        // Explicit slot indexes: SimpleContainer.createTag/fromTag compacts holes.
        var items = new net.minecraft.nbt.ListTag();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) if (!inventory.getItem(slot).isEmpty()) {
            var entry = (CompoundTag) inventory.getItem(slot).save(lookup); entry.putInt("Slot", slot); items.add(entry);
        }
        tag.put("gt.items", items);
        var extra = new net.minecraft.nbt.ListTag(); for (var stack : overflow) extra.add(stack.save(lookup)); tag.put("gt.overflow", extra);
        if (owner != null) tag.putUUID("gt.owner", owner);
        tag.putLong("gt.key", keyId); tag.putBoolean("gt.open", opened);
        if (dungeonLoot != null) tag.putString("gt.dungeonloot", dungeonLoot.toString());
        tag.putLong("gt.loot.seed", lootSeed);
    }
    @Override protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        inventory.clearContent();
        overflow.clear();
        var items = tag.getList("gt.items", 10);
        for (int i = 0; i < items.size(); i++) {
            var entry = items.getCompound(i); int slot = entry.contains("Slot") ? entry.getInt("Slot") : i;
            var stack = ItemStack.parseOptional(lookup,entry);
            if (slot >= 0 && slot < 15) inventory.setItem(slot, stack);
            else if (!stack.isEmpty()) overflow.add(stack);
        }
        var extra = tag.getList("gt.overflow", 10); for (int i = 0; i < extra.size(); i++) { var stack = ItemStack.parseOptional(lookup,extra.getCompound(i)); if (!stack.isEmpty()) overflow.add(stack); }
        owner = tag.hasUUID("gt.owner") ? tag.getUUID("gt.owner") : null;
        keyId = tag.getLong("gt.key"); opened = tag.getBoolean("gt.open");
        dungeonLoot = tag.contains("gt.dungeonloot") ? ResourceLocation.tryParse(tag.getString("gt.dungeonloot")) : null;
        lootSeed = tag.getLong("gt.loot.seed");
    }
}
