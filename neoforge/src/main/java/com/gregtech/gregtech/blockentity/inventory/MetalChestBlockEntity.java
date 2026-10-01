package com.gregtech.gregtech.blockentity.inventory;

import com.gregtech.gregtech.client.gui.HopperContainerMenu;
import com.gregtech.gregtech.registry.GTMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;

/** GT6 metal chest: a plain 54-slot chest. */
public class MetalChestBlockEntity extends BlockEntity implements MenuProvider {

    private final net.minecraft.world.level.block.entity.ChestLidController lid = new net.minecraft.world.level.block.entity.ChestLidController();
    private final net.minecraft.world.level.block.entity.ContainerOpenersCounter openers = new net.minecraft.world.level.block.entity.ContainerOpenersCounter() {
        protected void onOpen(net.minecraft.world.level.Level level, BlockPos pos, BlockState state) {
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.CHEST_OPEN, net.minecraft.sounds.SoundSource.BLOCKS, .5F, 1F);
        }
        protected void onClose(net.minecraft.world.level.Level level, BlockPos pos, BlockState state) {
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.CHEST_CLOSE, net.minecraft.sounds.SoundSource.BLOCKS, .5F, 1F);
        }
        protected void openerCountChanged(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, int oldCount, int count) {
            level.blockEvent(pos, state.getBlock(), 1, count);
        }
        protected boolean isOwnContainer(Player player) {
            return !player.isSpectator() && player.containerMenu instanceof ChestMenu menu && menu.chest == MetalChestBlockEntity.this;
        }
    };

    public void recheckOpeners() {
        if (level != null && !isRemoved()) openers.recheckOpeners(level, worldPosition, getBlockState());
    }
    public float openness(float partialTick) { return lid.getOpenness(partialTick); }
    public static void clientTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, MetalChestBlockEntity chest) {
        chest.lid.tickLid();
    }
    @Override public boolean triggerEvent(int id, int value) {
        if (id == 1) { lid.shouldBeOpen(value > 0); return true; }
        return super.triggerEvent(id, value);
    }
    @Override public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider lookup) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Open", openers.getOpenerCount() > 0);
        return tag;
    }
    @Override public void handleUpdateTag(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) { lid.shouldBeOpen(tag.getBoolean("Open")); }

    private final ItemStackHandler inventory = new ItemStackHandler(54) {
        @Override
        protected void onContentsChanged(int slot) { setChanged(); }
    };

    public MetalChestBlockEntity(BlockPos pos, BlockState state) {
        super(com.gregtech.gregtech.registry.GTBlockEntities.METAL_CHEST.get(), pos, state);
    }

    public ItemStackHandler inventory() { return inventory; }

    /** Whether this chest already rolled its GT6 loot table (persisted, like GT6's cleared name). */
    private boolean lootGenerated;
    private net.minecraft.resources.ResourceLocation dungeonLoot;
    private long dungeonLootSeed;

    public void setDungeonLoot(net.minecraft.resources.ResourceLocation table, long seed) {
        dungeonLoot = table;
        dungeonLootSeed = seed;
        lootGenerated = false;
        setChanged();
    }

    /**
     * GT6 loot chest behaviour ({@code MultiTileEntityChest:262}): a chest that carries a
     * {@code gt.dungeonloot} table fills itself from it when it is first opened, and spawns the same
     * five experience orbs (5..14 each) the original does.
     *
     * @return how many stacks were placed
     */
    public int generateLootIfNeeded() {
        if (lootGenerated || level == null || level.isClientSide) return 0;
        if (dungeonLoot != null && level instanceof net.minecraft.server.level.ServerLevel server) {
            var table = server.getServer().reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,dungeonLoot));
            if (table == net.minecraft.world.level.storage.loot.LootTable.EMPTY) return 0;
            var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(server)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,
                            net.minecraft.world.phys.Vec3.atCenterOf(worldPosition))
                    .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
            var container = new net.minecraft.world.SimpleContainer(inventory.getSlots());
            for (int slot = 0; slot < inventory.getSlots(); slot++) container.setItem(slot, inventory.getStackInSlot(slot).copy());
            table.fill(container, params, dungeonLootSeed);
            lootGenerated = true;
            dungeonLoot = null;
            int placed = 0;
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                inventory.setStackInSlot(slot, container.getItem(slot));
                if (!container.getItem(slot).isEmpty()) placed++;
            }
            setChanged();
            if (placed > 0) spawnLootExperience();
            return placed;
        }
        if (!(getBlockState().getBlock()
                instanceof com.gregtech.gregtech.block.inventory.LootChestBlock lootChest)) return 0;
        lootGenerated = true;
        setChanged();
        int placed = com.gregtech.gregtech.content.loot.GTLootTables.fillInto(
                lootChest.lootTable(), inventory, level.random);
        if (placed > 0) spawnLootExperience();
        return placed;
    }

    private void spawnLootExperience() {
            for (int i = 0; i < 5; i++) {
                int value = 5 + level.random.nextInt(5) + level.random.nextInt(5);
                net.minecraft.world.entity.ExperienceOrb orb =
                        new net.minecraft.world.entity.ExperienceOrb(level,
                                worldPosition.getX() + 0.4 + level.random.nextDouble() * 0.2,
                                worldPosition.getY() + 1.25,
                                worldPosition.getZ() + 0.4 + level.random.nextDouble() * 0.2, value);
                level.addFreshEntity(orb);
            }
    }

    /** True when the chest still holds loot from its table (tests read it). */
    public boolean lootGenerated() { return lootGenerated; }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        generateLootIfNeeded();
        return new ChestMenu(containerId, playerInv, this);
    }

    private static final class ChestMenu extends HopperContainerMenu {
        private final MetalChestBlockEntity chest;
        private boolean closed;
        ChestMenu(int id, Inventory playerInv, MetalChestBlockEntity chest) {
            super(GTMenuTypes.forSlotCount(54), id, playerInv, chest.inventory);
            this.chest = chest;
            if (!playerInv.player.isSpectator() && chest.level != null)
                chest.openers.incrementOpeners(playerInv.player, chest.level, chest.worldPosition, chest.getBlockState());
        }
        @Override public boolean stillValid(Player player) {
            return !chest.isRemoved() && chest.level != null && chest.level.getBlockEntity(chest.worldPosition) == chest
                    && player.distanceToSqr(chest.worldPosition.getX() + .5, chest.worldPosition.getY() + .5, chest.worldPosition.getZ() + .5) <= 64;
        }
        @Override public void removed(Player player) {
            super.removed(player);
            if (!closed && !player.isSpectator() && chest.level != null)
                chest.openers.decrementOpeners(player, chest.level, chest.worldPosition, chest.getBlockState());
            closed = true;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag,lookup);
        tag.put("Inventory", inventory.serializeNBT(lookup));
        tag.putBoolean("GTLootGenerated", lootGenerated);
        if (dungeonLoot != null) tag.putString("gt.dungeonloot", dungeonLoot.toString());
        tag.putLong("GTDungeonLootSeed", dungeonLootSeed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag,lookup);
        CompoundTag storedInventory = tag.getCompound("Inventory").copy();
        storedInventory.putInt("Size", 54);
        inventory.deserializeNBT(lookup,storedInventory);
        lootGenerated = tag.getBoolean("GTLootGenerated");
        dungeonLoot = tag.contains("gt.dungeonloot") ? net.minecraft.resources.ResourceLocation.tryParse(tag.getString("gt.dungeonloot")) : null;
        dungeonLootSeed = tag.getLong("GTDungeonLootSeed");
    }
}
