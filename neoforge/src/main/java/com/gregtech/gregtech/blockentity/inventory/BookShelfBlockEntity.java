package com.gregtech.gregtech.blockentity.inventory;

import com.gregtech.gregtech.content.book.GTBookList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;



import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;

/**
 * GT6 {@code MultiTileEntityBookShelf} ({@code gregapi.tileentity.inventories}): 28 slots that
 * only take books (and the other display items of {@code BooksGT.BOOK_REGISTER}), with the shelf's
 * enchanting power computed from what is stored ({@code getEnchantPowerBonus}: +1 per book, +2 per
 * enchanted book).
 */
public class BookShelfBlockEntity extends BlockEntity {
    private net.minecraft.resources.ResourceLocation frontLoot;
    private net.minecraft.resources.ResourceLocation backLoot;
    private long lootSeed;
    /** GT6: -1 for a latching lever, 1..120 for a button, 0 for off. */
    private byte redstoneDelay;

    public int redstoneSignal() {
        return redstoneDelay == 0 ? 0 : 15;
    }

    public void pressButton() {
        redstoneDelay = 120;
        changedRedstone();
    }

    public void toggleLever() {
        redstoneDelay = redstoneDelay == 0 ? (byte) -1 : 0;
        changedRedstone();
    }

    /** Called by the block entity ticker; only positive button timers count down. */
    public void tickRedstone() {
        if (redstoneDelay > 0 && --redstoneDelay == 0) changedRedstone();
    }

    /** GT6 displays delayed dungeon books once a player comes within 32 blocks, every 300 ticks. */
    public void serverTick() {
        tickRedstone();
        if (level != null && !level.isClientSide && level.getGameTime() % 300 == 0
                && (frontLoot != null || backLoot != null)
                && !level.getEntitiesOfClass(Player.class, new AABB(worldPosition).inflate(32)).isEmpty()) {
            generateDungeonLoot();
        }
    }

    private void changedRedstone() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void setDungeonLoot(net.minecraft.resources.ResourceLocation front,
                               net.minecraft.resources.ResourceLocation back, long seed) {
        frontLoot = front;
        backLoot = back;
        lootSeed = seed;
        setChanged();
    }

    /** GT6 rolls a fourteen-slot dummy chest for each face, preserving keys already on the shelf. */
    public void generateDungeonLoot() {
        if (!(level instanceof net.minecraft.server.level.ServerLevel server)) return;
        for (int face = 0; face < 2; face++) {
            var id = face == 0 ? frontLoot : backLoot;
            if (id == null) continue;
            var table = server.getServer().reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,id));
            if (table == net.minecraft.world.level.storage.loot.LootTable.EMPTY) continue;
            var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(server)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,
                            net.minecraft.world.phys.Vec3.atCenterOf(worldPosition))
                    .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
            var dummy = new net.minecraft.world.SimpleContainer(14);
            long seed = lootSeed ^ (face * 0x9e3779b97f4a7c15L);
            table.fill(dummy, params, seed);
            // Consume the pending table before inventory callbacks, and persist that state.
            if (face == 0) frontLoot = null; else backLoot = null;
            setChanged();
            var random = net.minecraft.util.RandomSource.create(seed);
            for (int entry = 0; entry < 14; entry++) {
                int slot = face * 14 + random.nextInt(14);
                if (!inventory.getStackInSlot(slot).isEmpty()) continue;
                var stack = dummy.getItem(entry);
                inventory.setStackInSlot(slot, GTBookList.canPlace(stack) ? stack.copyWithCount(1)
                        : new ItemStack(net.minecraft.world.item.Items.BOOK));
            }
        }
    }
    private final ItemStackHandler inventory = new ItemStackHandler(GTBookList.SLOTS) {
        @Override
        public int getSlotLimit(int slot) {
            return 1; // GT6 MultiTileEntityBookShelf.getInventoryStackLimit.
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return GTBookList.canPlace(stack);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return GTBookList.canAutoExtract(getStackInSlot(slot))
                    ? super.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            }
        }
    };
    public BookShelfBlockEntity(BlockPos pos, BlockState state) {
        super(com.gregtech.gregtech.registry.GTBlockEntities.BOOKSHELF.get(), pos, state);
    }

    public ItemStackHandler inventory() {
        return inventory;
    }

    /** GT6's {@code getEnchantPowerBonus}: the bookshelf's contribution to an enchanting table. */
    public int enchantPower() {
        return GTBookList.enchantPower(inventory);
    }

    /** GT6's shelf returns what is stored when broken. */
    public java.util.List<ItemStack> contents() {
        generateDungeonLoot();
        java.util.List<ItemStack> drops = new java.util.ArrayList<>();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) drops.add(stack);
        }
        return drops;
    }

    @Override
    protected void loadAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag,lookup);
        inventory.deserializeNBT(lookup,tag.getCompound("inventory"));
        frontLoot = tag.contains("gt.dungeonloot.front") ? net.minecraft.resources.ResourceLocation.tryParse(tag.getString("gt.dungeonloot.front")) : null;
        backLoot = tag.contains("gt.dungeonloot.back") ? net.minecraft.resources.ResourceLocation.tryParse(tag.getString("gt.dungeonloot.back")) : null;
        lootSeed = tag.getLong("gt.shelf.loot.seed");
        redstoneDelay = tag.getByte("gt.shelf.redstone.delay");
    }

    @Override
    protected void saveAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag,lookup);
        tag.put("inventory", inventory.serializeNBT(lookup));
        if (frontLoot != null) tag.putString("gt.dungeonloot.front", frontLoot.toString());
        if (backLoot != null) tag.putString("gt.dungeonloot.back", backLoot.toString());
        tag.putLong("gt.shelf.loot.seed", lootSeed);
        if (redstoneDelay != 0) tag.putByte("gt.shelf.redstone.delay", redstoneDelay);
    }

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider lookup) {
        return saveWithoutMetadata(lookup);
    }

    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

}
