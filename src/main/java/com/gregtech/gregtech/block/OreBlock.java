package com.gregtech.gregtech.block;
import com.gregtech.gregtech.api.material.MaterialPresentation;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.prefix.BlockMaterialPrefix;
import com.gregtech.gregtech.blockentity.OreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Ore blocks (plain and small variants) that are always breakable and droppable
 * regardless of block tag membership. Mining speed still respects the tool quality.
 *
 * <p>§103.B: the background ("host") rock an ore generated in lives in the {@link #STONE} block
 * <em>state</em> property, so one registered ore block per material still renders with any stone
 * background (GT6 had one block per stone variant instead). It used to be the only field of an
 * {@link OreBlockEntity} — one block entity per ore block, ~497 per chunk and 1.8M in the
 * 3.6k-chunk session measured in §101/§102 (~0.3–0.5 GB). Ore blocks no longer create one.
 *
 * <p>Drops and pick-block carry the host rock on the item ({@code BlockStateTag}, plus the pre-§103.B
 * {@code BlockEntityTag}) and {@link #getStateForPlacement} puts it back on placement.
 *
 * <p><b>No block entity, on purpose — this class deliberately does not implement
 * {@code net.minecraft.world.level.block.EntityBlock}</b>, which is what {@code Block#hasBlockEntity()}
 * reports:
 * <ul>
 *   <li>worldgen writes a {@code DUMMY} placeholder tag for every block whose state has a block entity
 *       (vanilla {@code WorldGenRegion:267-281}), so an {@code EntityBlock} ore would carry one per
 *       placed block, and promoting it through a {@code newBlockEntity} that returns {@code null} logs
 *       {@code "Tried to load a block entity … but failed"} for each of them;</li>
 *   <li>{@code Level#getBlockEntity(BlockPos)} asks for an <em>immediate</em> creation
 *       ({@code LevelChunk:313-320}), so any block with {@code hasBlockEntity()} grows a fresh entity on
 *       the first lookup — exactly what §103.B removes.</li>
 * </ul>
 *
 * <p>Worlds generated before §103.B are still handled: the {@code gregtech:ore} block-entity type stays
 * registered, so the chunk loader creates the entity from its saved tag, sets its level
 * ({@code LevelChunk:538}) and calls {@link OreBlockEntity#onLoad()}, which folds the stone into this
 * block's {@link #STONE} property before the entity is dropped — {@code LevelChunk#setBlockEntity}
 * refuses to store it because the ore state no longer accepts a block entity.
 */
public class OreBlock extends Block implements MaterialBlockLike {
    /** The host rock of this ore — {@link OreHostStone#STONE} unless worldgen placed it elsewhere. */
    public static final EnumProperty<OreHostStone> STONE = EnumProperty.create("stone", OreHostStone.class);
    /** GT6's broken-host ore (the bedrock drill product) without another block per material. */
    public static final BooleanProperty BROKEN = BooleanProperty.create("broken");

    private final BlockMaterialPrefix prefix;
    private final GTMaterial material;

    public OreBlock(Properties properties, BlockMaterialPrefix prefix, GTMaterial material) {
        super(properties);
        this.prefix = prefix;
        this.material = material;
        registerDefaultState(defaultBlockState().setValue(STONE, OreHostStone.STONE).setValue(BROKEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STONE, BROKEN);
    }

    /** The state of this ore with the given host rock — what the worldgen placement path writes. */
    public BlockState stateFor(OreHostStone stone) {
        return defaultBlockState().setValue(STONE, stone == null ? OreHostStone.STONE : stone);
    }

    /** Mineable ore with a rubble/cobblestone host, equivalent to GT6 {@code oreBroken}. */
    public BlockState brokenStateFor(OreHostStone stone) {
        return stateFor(stone).setValue(BROKEN, true);
    }

    public static boolean isBroken(@Nullable BlockState state) {
        return state != null && state.hasProperty(BROKEN) && state.getValue(BROKEN);
    }

    public static boolean isBrokenStack(ItemStack stack) {
        CompoundTag stateTag = stack.getTagElement("BlockStateTag");
        return stateTag != null && "true".equals(stateTag.getString(BROKEN.getName()));
    }

    /** The host rock of a state; any state that is not an ore block reads as {@link OreHostStone#STONE}. */
    public static OreHostStone stoneOf(@Nullable BlockState state) {
        return state != null && state.hasProperty(STONE) ? state.getValue(STONE) : OreHostStone.STONE;
    }

    /** GT6 bedrock deposits: embedded in bedrock, so unbreakable and dropping nothing. */
    public static boolean isBedrockOre(@Nullable BlockState state) {
        return stoneOf(state).isBedrock();
    }

    @Override
    public BlockMaterialPrefix prefix() { return prefix; }

    @Override
    public GTMaterial material() { return material; }

    @Override
    public String getDescriptionId() {
        return "block." + GregTech.NAMESPACE + "." + prefix.getRegistryName();
    }

    @Override
    public MutableComponent getName() {
        return Component.translatable(getDescriptionId(), MaterialPresentation.name(material));
    }

    /** True for the {@code oreSmall} prefix variant. */
    public boolean isSmall() {
        return "oreSmall".equals(prefix.getName());
    }

    /**
     * Copies the host rock onto an item stack. Two tags on purpose:
     * <ul>
     *   <li>{@code BlockStateTag} — the vanilla format ({@code BlockItem#updateBlockStateFromTag}) and
     *       what {@link #stoneOfStack} reads when the stack is placed again;</li>
     *   <li>{@code BlockEntityTag} — the pre-§103.B format, kept on intact ore items silk-touched by
     *       older builds. New broken ore carries only {@code BlockStateTag}, since it has no block
     *       entity and must remain eligible for material recovery.</li>
     * </ul>
     */
    public static ItemStack withStone(ItemStack stack, @Nullable BlockState state) {
        OreHostStone stone = stoneOf(state);
        boolean broken = isBroken(state);
        if (stone == OreHostStone.STONE && !broken) return stack;
        CompoundTag stateTag = new CompoundTag();
        stateTag.putString(STONE.getName(), stone.id());
        if (broken) stateTag.putString(BROKEN.getName(), "true");
        stack.addTagElement("BlockStateTag", stateTag);
        if (!broken) {
            // Only intact ore predates the block-state migration. New broken ore has no legacy
            // entity, so do not mark it as a filled container in material recovery.
            CompoundTag entityTag = new CompoundTag();
            entityTag.putString(OreBlockEntity.TAG_STONE, stone.id());
            stack.addTagElement("BlockEntityTag", entityTag);
        }
        return stack;
    }

    /** GT6 bedrock drill output: a material ore block in its broken host-rock form. */
    public static ItemStack brokenStack(@Nullable GTMaterial material, OreHostStone stone) {
        if (material == null) return ItemStack.EMPTY;
        var registered = com.gregtech.gregtech.registry.GTBlocks.getObject(
                BlockMaterialPrefix.ore, material.resolve());
        if (registered == null || !registered.isPresent() || !(registered.get() instanceof OreBlock ore))
            return ItemStack.EMPTY;
        return withStone(new ItemStack(ore), ore.brokenStateFor(stone));
    }

    /** The host rock an ore item carries: {@code BlockStateTag} first, then the legacy {@code BlockEntityTag}. */
    public static OreHostStone stoneOfStack(ItemStack stack) {
        CompoundTag stateTag = stack.getTagElement("BlockStateTag");
        if (stateTag != null && stateTag.contains(STONE.getName())) {
            return OreHostStone.byId(stateTag.getString(STONE.getName()));
        }
        CompoundTag entityTag = stack.getTagElement("BlockEntityTag");
        if (entityTag != null) {
            return OreHostStone.byId(entityTag.getString(OreBlockEntity.TAG_STONE));
        }
        return OreHostStone.STONE;
    }

    /** Placing an ore item puts the host rock it carries back into the state. */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        ItemStack stack = context.getItemInHand();
        return stateFor(stoneOfStack(stack)).setValue(BROKEN, isBrokenStack(stack));
    }

    /** GT6 broken ores use a rubble host and fall, while intact worldgen ores remain anchored. */
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (isBroken(state)) level.scheduleTick(pos, this, 2);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction side, BlockState adjacent,
                                  net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos adjacentPos) {
        if (isBroken(state)) level.scheduleTick(pos, this, 2);
        return super.updateShape(state, side, adjacent, level, pos, adjacentPos);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (isBroken(state) && pos.getY() >= level.getMinBuildHeight()
                && FallingBlock.isFree(level.getBlockState(pos.below()))) {
            FallingBlockEntity.fall(level, pos, state);
        }
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        // Bedrock-embedded ores are indestructible and never drop (GT6 bedrock deposits).
        if (isBedrockOre(state)) return Collections.emptyList();
        // GT6 broken ore is already detached from its intact host and drops as the same 2U block.
        if (isBroken(state)) return Collections.singletonList(withStone(new ItemStack(this), state));
        if(isSmall()) {
            ItemStack tool=builder.getOptionalParameter(LootContextParams.TOOL);
            if(tool==null)tool=ItemStack.EMPTY;
            int fortune=net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.BLOCK_FORTUNE,tool);
            boolean silk=net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH,tool)>0;
            var origin=builder.getOptionalParameter(LootContextParams.ORIGIN);
            BlockPos pos=origin==null?BlockPos.ZERO:BlockPos.containing(origin);
            String host=stoneOf(state).id();
            if(host.startsWith("stone_"))host=host.substring(6);
            String name=switch(host){case "end_stone"->"Endstone";case "netherrack"->"Netherrack";default->host.substring(0,1).toUpperCase(java.util.Locale.ROOT)+host.substring(1);};
            return SmallOreDrops.drops(material,pos,fortune,silk,GTMaterialRegistry.get(name));
        }
        ItemStack tool = builder.getOptionalParameter(LootContextParams.TOOL);
        if (tool == null) tool = ItemStack.EMPTY;
        boolean silk = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH, tool) > 0;
        if (silk) return Collections.singletonList(withStone(new ItemStack(this), state));
        int fortune = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                net.minecraft.world.item.enchantment.Enchantments.BLOCK_FORTUNE, tool);
        var raw = com.gregtech.gregtech.registry.GTItems.getStack(com.gregtech.gregtech.data.MaterialPrefix.oreRaw,
                material, 1 + builder.getLevel().random.nextInt(Math.max(0, fortune) + 1));
        return raw.isEmpty() ? Collections.emptyList() : Collections.singletonList(raw);
    }

    @Override
    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos,
                                        net.minecraft.world.level.Explosion explosion) {
        if (isBedrockOre(state)) return 3600000.0F;
        float resistance = super.getExplosionResistance(state, level, pos, explosion);
        return isBroken(state) ? resistance * 0.5F : resistance;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
        return withStone(new ItemStack(this), state);
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (isBedrockOre(state)) return 0.0F;
        float progress = super.getDestroyProgress(state, player, level, pos);
        return isBroken(state) ? progress * 2.0F : progress;
    }
}
