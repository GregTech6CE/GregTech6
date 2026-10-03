package com.gregtech.gregtech.blockentity.tool;

import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;

/**
 * Manual tool stations (GT6 mortar / grind stone / sifting table): one work
 * slot for sifting; mortar and grindstone process the item in the player's hand.
 */
public class ManualToolBlockEntity extends BlockEntity {

    public enum Kind {
        MORTAR("Mortar"), GRINDSTONE("Sharpening"), SIFTING("Sifting");

        final String map;
        Kind(String map) { this.map = map; }
        public RecipeMap recipeMap() {
            return switch (this) {
                case MORTAR -> MachineRecipeMaps.Mortar;
                case GRINDSTONE -> MachineRecipeMaps.Sharpening;
                case SIFTING -> MachineRecipeMaps.Sifting;
            };
        }
    }

    private final net.minecraft.core.NonNullList<ItemStack> outputs = net.minecraft.core.NonNullList.withSize(com.gregtech.gregtech.content.tool.ManualWorkRules.OUTPUTS, ItemStack.EMPTY);
    private int stoneUses;
    private ItemStack lastHeld = ItemStack.EMPTY;
    private java.util.UUID siftingPlayer;

    private final Kind kind;
    private ItemStack work = ItemStack.EMPTY;
    private int progress;

    public ManualToolBlockEntity(BlockPos pos, BlockState state) {
        super(com.gregtech.gregtech.registry.GTBlockEntities.MANUAL_TOOL.get(), pos, state);
        this.kind = state.getBlock() instanceof com.gregtech.gregtech.block.tool.ManualToolBlock tool
                ? tool.kind() : Kind.MORTAR;
    }

    public Kind kind() { return kind; }
    public ItemStack work() { return work; }

    /** GT6's workshop grindstone starts with 1..4 abrasive uses (its NBT_STATE). */
    public boolean setDungeonGrindstoneUses(int uses) {
        if (kind != Kind.GRINDSTONE || uses < 1 || uses > 4 || stoneUses != 0) return false;
        stoneUses = uses;
        markUpdated();
        return true;
    }

    public int stoneUses() { return stoneUses; }

    private RecipeMap map() {
        return kind.recipeMap();
    }

    /** Keep old saved work retrievable while replacing the former generic four-click machine. */
    public ItemStack retrieve() {
        ItemStack out = work;
        work = ItemStack.EMPTY;
        siftingPlayer = null;
        progress = 0;
        markUpdated();
        return out;
    }

    public void interact(Player player, net.minecraft.world.InteractionHand hand, net.minecraft.core.Direction side) {
        if (level == null || level.isClientSide) return;
        ItemStack held = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && held.isEmpty() && !work.isEmpty()) {
            give(player, retrieve());
            return;
        }
        if (kind == Kind.SIFTING) {
            if (side != net.minecraft.core.Direction.UP) {
                for (int i = 0; i < outputs.size(); i++) { give(player, outputs.get(i)); outputs.set(i, ItemStack.EMPTY); }
                markUpdated();
                return;
            }
            if (work.isEmpty()) {
                if (recipe(held) == null) return;
                work = held.copy();
                if (!player.getAbilities().instabuild) held.setCount(0);
                progress = 0;
                markUpdated();
                return;
            }
            siftingPlayer = player.getUUID();
            return;
        }
        if (kind == Kind.GRINDSTONE) {
            if (held.isEmpty()) { progress = 0; return; }
            if (stoneUses <= 0) {
                stoneUses = abrasiveUses(held);
                if (stoneUses > 0) {
                    if (!player.getAbilities().instabuild) held.shrink(1);
                    progress = 0;
                    markUpdated();
                }
                return;
            }
            if (!ItemStack.isSameItemSameTags(lastHeld, held)) { progress = 0; lastHeld = held.copyWithCount(1); }
            if (!player.getAbilities().instabuild && ++progress < com.gregtech.gregtech.content.tool.ManualWorkRules.GRIND_CLICKS) return;
            int enchantmentXp = removableEnchantmentXp(held);
            if (enchantmentXp > 0) {
                ItemStack result = held.copyWithCount(1);
                int xp = enchantmentXp;
                result.removeTagKey("Enchantments");
                if (!player.getAbilities().instabuild) held.shrink(1);
                give(player, result);
                if (xp > 0 && level instanceof net.minecraft.server.level.ServerLevel server)
                    net.minecraft.world.entity.ExperienceOrb.award(server, net.minecraft.world.phys.Vec3.atCenterOf(worldPosition).add(0, .75, 0), xp);
                player.causeFoodExhaustion(.5F);
                progress = 0;
                markUpdated();
                return;
            }
        }
        craft(player, held, false);
    }

    private static int removableEnchantmentXp(ItemStack held) {
        // GT6 excludes its own tools and refuses cursed items; books use a different enchantment tag.
        if (held.getItem() instanceof com.gregtech.gregtech.item.GTToolItem || !held.isEnchanted()
                || held.is(net.minecraft.world.item.Items.ENCHANTED_BOOK)) return 0;
        var enchantments = net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantments(held);
        if (enchantments.keySet().stream().anyMatch(net.minecraft.world.item.enchantment.Enchantment::isCurse)) return 0;
        int sum = enchantments.entrySet().stream().mapToInt(entry -> entry.getKey().getMinCost(entry.getValue())).sum();
        return (sum + 1) / 2;
    }

    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, ManualToolBlockEntity tool) {
        if (tool.kind != Kind.SIFTING || tool.siftingPlayer == null || !com.gregtech.gregtech.content.tool.ManualWorkRules.siftDue(level.getGameTime())) return;
        Player player = level.getPlayerByUUID(tool.siftingPlayer);
        if (player == null || player.distanceToSqr(pos.getX()+.5, pos.getY()+.5, pos.getZ()+.5) > 64
                || !(player.pick(5, 1, false) instanceof net.minecraft.world.phys.BlockHitResult hit)
                || !hit.getBlockPos().equals(pos) || hit.getDirection() != net.minecraft.core.Direction.UP) {
            tool.siftingPlayer = null;
            return;
        }
        if (tool.work.isEmpty() || tool.outputs.stream().anyMatch(stack -> !stack.isEmpty())) return;
        var haste = player.getEffect(net.minecraft.world.effect.MobEffects.DIG_SPEED);
        var fatigue = player.getEffect(net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN);
        tool.progress += com.gregtech.gregtech.content.tool.ManualWorkRules.siftIncrement(haste==null?-1:haste.getAmplifier());
        int threshold = com.gregtech.gregtech.content.tool.ManualWorkRules.siftThreshold(fatigue==null?-1:fatigue.getAmplifier());
        if (player.getAbilities().instabuild || tool.progress >= threshold) tool.craft(player, tool.work, true);
        else tool.markUpdated();
    }

    private static int abrasiveUses(ItemStack stack) {
        if (stack.getItem() instanceof com.gregtech.gregtech.item.MaterialItem item && item.getPrefix().getName().equals("stone")) {
            return com.gregtech.gregtech.content.tool.ManualWorkRules.abrasiveUses(item.getMaterial().getName());
        }
        return stack.is(net.minecraftforge.common.Tags.Items.SANDSTONE) ? 8 : 0;
    }

    private Recipe recipe(ItemStack input) {
        if (input.isEmpty()) return null;
        Recipe recipe = map().findRecipe(List.of(input), List.<FluidStack>of(), false, 1, 12);
        return recipe != null && !recipe.mFakeRecipe && recipe.mEUt >= -com.gregtech.gregtech.content.tool.ManualWorkRules.MAX_GU && recipe.mEUt <= com.gregtech.gregtech.content.tool.ManualWorkRules.MAX_GU && recipe.mInputs.length == 1 ? recipe : null;
    }

    private void craft(Player player, ItemStack input, boolean retainOutputs) {
        Recipe recipe = recipe(input);
        progress = 0;
        if (recipe == null) { markUpdated(); return; }
        // Installed sifter input is inventory, even when its current operator is creative.
        if (retainOutputs || !player.getAbilities().instabuild) input.shrink(recipe.mInputs[0].getCount());
        for (int i = 0; i < recipe.mOutputs.length; i++) {
            long count = recipe.rollOutputCount(i, 1, level.random::nextInt);
            if (count == 0) continue;
            ItemStack out = recipe.getOutput(i);
            out.setCount((int) count);
            if (retainOutputs && i < outputs.size()) outputs.set(i, out);
            else give(player, out);
        }
        if (kind == Kind.GRINDSTONE && !player.getAbilities().instabuild) stoneUses--;
        double divisor = com.gregtech.gregtech.content.tool.ManualWorkRules.exhaustionDivisor(kind.name());
        player.causeFoodExhaustion((float) (Math.abs((double) recipe.mEUt) * recipe.mDuration / divisor));
        level.playSound(null, worldPosition, kind == Kind.SIFTING ? SoundEvents.SAND_BREAK : SoundEvents.STONE_HIT,
                SoundSource.BLOCKS, 1F, 1F);
        markUpdated();
    }

    private static void give(Player player, ItemStack stack) {
        if (!stack.isEmpty() && !player.addItem(stack)) player.drop(stack, false);
    }

    private void markUpdated() {
        setChanged();
        if (level != null && !level.isClientSide) {
            if (kind == Kind.GRINDSTONE) {
                var state = getBlockState();
                var property = com.gregtech.gregtech.block.tool.ManualToolBlock.STONE;
                if (state.getValue(property) != (stoneUses > 0)) level.setBlockAndUpdate(worldPosition, state.setValue(property, stoneUses > 0));
            }
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void dropContents() {
        if (level != null) for (int i = 0; i < outputs.size(); i++) {
            com.gregtech.gregtech.util.GTItemDrops.dropItemStack(level, worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5, outputs.get(i));
            outputs.set(i, ItemStack.EMPTY);
        }
        if (level != null && !work.isEmpty()) {
            com.gregtech.gregtech.util.GTItemDrops.dropItemStack(level, worldPosition.getX() + 0.5,
                    worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, work);
            work = ItemStack.EMPTY;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("gt.progress", progress);
        tag.putInt("gt.stone_uses", stoneUses);
        net.minecraft.world.ContainerHelper.saveAllItems(tag, outputs);
        if (!work.isEmpty()) tag.put("gt.work", work.save(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        progress = tag.getInt("gt.progress");
        stoneUses = Math.max(0, tag.getInt("gt.stone_uses"));
        outputs.clear();
        net.minecraft.world.ContainerHelper.loadAllItems(tag, outputs);
        work = tag.contains("gt.work") ? ItemStack.of(tag.getCompound("gt.work")) : ItemStack.EMPTY;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) { load(tag); }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        if (pkt.getTag() != null) load(pkt.getTag());
    }
}
