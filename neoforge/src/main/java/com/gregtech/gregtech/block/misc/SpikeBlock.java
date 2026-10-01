package com.gregtech.gregtech.block.misc;

import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.ToolInteractionSpec;
import com.gregtech.gregtech.api.tool.ToolInteractionTarget;
import com.gregtech.gregtech.api.tool.ToolInteractions;
import com.gregtech.gregtech.block.machine.MachineRotationType;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.damage.GTDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * GT6's five {@code BlockBaseSpike} families. The original's metadata bits encode a material
 * choice and wall/omni/falling shape; named blockstate properties preserve those in 1.20.1.
 * Worldgen's existing default {@code spike_steel} state is omni, as GT6's mob farm uses meta 6/14.
 */
public final class SpikeBlock extends Block implements ToolInteractionTarget, com.gregtech.gregtech.api.block.StatefulBlockLoot {
    @Override public java.util.List<String> lootStateProperties(){return java.util.List.of("mode","secondary");}
    public enum Family { METAL, STEEL, SHARP, FANCY, SUPER }
    public enum Mode implements StringRepresentable {
        WALL("wall"), OMNI("omni"), FALLING("falling");
        private final String id;
        Mode(String id) { this.id = id; }
        @Override public String getSerializedName() { return id; }
    }

    public static final DirectionProperty FACING = DirectionProperty.create("facing");
    public static final EnumProperty<Mode> MODE = EnumProperty.create("mode", Mode.class);
    public static final BooleanProperty SECONDARY = BooleanProperty.create("secondary");
    private static final VoxelShape OMNI_COLLISION = box(2, 2, 2, 14, 14, 14);
    private static final VoxelShape[] WALL_COLLISION = new VoxelShape[6];
    static {
        WALL_COLLISION[Direction.DOWN.ordinal()] = box(0, 0, 0, 16, 9.6, 16);
        WALL_COLLISION[Direction.UP.ordinal()] = box(0, 6.4, 0, 16, 16, 16);
        WALL_COLLISION[Direction.NORTH.ordinal()] = box(0, 0, 0, 16, 16, 9.6);
        WALL_COLLISION[Direction.SOUTH.ordinal()] = box(0, 0, 6.4, 16, 16, 16);
        WALL_COLLISION[Direction.WEST.ordinal()] = box(0, 0, 0, 9.6, 16, 16);
        WALL_COLLISION[Direction.EAST.ordinal()] = box(6.4, 0, 0, 16, 16, 16);
    }

    private final Family family;

    public SpikeBlock(Family family, Properties properties) {
        super(properties.noOcclusion());
        this.family = family;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.DOWN)
                .setValue(MODE, Mode.OMNI).setValue(SECONDARY, false));
    }

    public Family family() { return family; }

    public int materialTint(boolean secondary) {
        return switch (family) {
            case METAL -> secondary ? Materials.Lead.getColor() : Materials.Copper.getColor();
            case STEEL -> secondary ? Materials.RedSteel.getColor() : Materials.BlueSteel.getColor();
            case SHARP -> secondary ? Materials.Titanium.getColor() : Materials.Steel.getColor();
            case FANCY -> secondary ? Materials.Silver.getColor() : Materials.Gold.getColor();
            case SUPER -> secondary ? Materials.Adamantium.getColor()
                    : com.gregtech.gregtech.api.material.GTMaterialRegistry.get("TungstenSteel").getColor();
        };
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, MODE, SECONDARY);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        // GT6 onBlockPlaced stores the opposite face: floor-mounted wall spikes point upwards.
        return defaultBlockState().setValue(MODE, Mode.WALL)
                .setValue(FACING, context.getClickedFace().getOpposite());
    }

    @Override public ItemStack getCloneItemStack(BlockState state,net.minecraft.world.phys.HitResult hit,net.minecraft.world.level.LevelReader level,BlockPos pos,Player player){return packed(state);}
    private ItemStack packed(BlockState state){ItemStack stack=new ItemStack(this);stack.set(net.minecraft.core.component.DataComponents.BLOCK_STATE,new net.minecraft.world.item.component.BlockItemStateProperties(java.util.Map.of("mode",state.getValue(MODE).getSerializedName(),"secondary",Boolean.toString(state.getValue(SECONDARY)))));return stack;}
    @Override public java.util.List<ItemStack> getDrops(BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder builder){return java.util.List.of(packed(state));}

    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        if (state.getValue(MODE) == Mode.FALLING) level.scheduleTick(pos, this, 2);
        super.onPlace(state, level, pos, oldState, moved);
    }

    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(MODE) == Mode.FALLING) level.scheduleTick(pos, this, 2);
        return super.updateShape(state, side, neighbor, level, pos, neighborPos);
    }

    @Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(MODE) == Mode.FALLING && pos.getY() >= level.getMinBuildHeight()
                && FallingBlock.isFree(level.getBlockState(pos.below()))) {
            FallingBlockEntity.fall(level, pos, state);
        }
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // GT6 deliberately selects the full cube, despite the actual thin collision shape.
        return Shapes.block();
    }

    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                                   CollisionContext context) {
        if (context instanceof EntityCollisionContext entityContext) {
            Entity entity = entityContext.getEntity();
            if (entity instanceof ItemEntity || entity instanceof ExperienceOrb || entity instanceof Projectile)
                return Shapes.empty();
        }
        return state.getValue(MODE) == Mode.WALL
                ? WALL_COLLISION[state.getValue(FACING).ordinal()] : OMNI_COLLISION;
    }

    @Override public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (entity instanceof LivingEntity
                && (state.getValue(MODE) != Mode.WALL || state.getValue(FACING) != Direction.UP)) {
            entity.setDeltaMovement(entity.getDeltaMovement().multiply(com.gregtech.gregtech.block.SpikeRules.WALK_FACTOR,1.0,com.gregtech.gregtech.block.SpikeRules.WALK_FACTOR));
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && entity instanceof LivingEntity living) {
            float damage = damageFor(state, living);
            if (damage > 0) entity.hurt(GTDamageTypes.spike(level), damage);
        }
        super.entityInside(state, level, pos, entity);
    }

    /** Exact GT6 damage table without TFC's optional ×80 health compatibility multiplier. */
    public float damageFor(BlockState state, LivingEntity living) {
        return com.gregtech.gregtech.block.SpikeRules.damage(family.name(),state.getValue(SECONDARY),state.getValue(MODE)!=Mode.WALL,living instanceof IronGolem,living instanceof Skeleton,living instanceof Slime,living.getType().is(EntityTypeTags.ARTHROPOD),living.getType().is(EntityTypeTags.UNDEAD),isEnderOrWere(living));
    }

    private static boolean isEnderOrWere(LivingEntity living) {
        String name = living.getClass().getName().toLowerCase(java.util.Locale.ROOT);
        return living instanceof EnderMan || name.contains("ender") || name.contains("werewolf")
                || name.contains("wolfman") || name.contains("minotaur") || name.contains("yeti");
    }

    @Override public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        if (family == Family.FANCY) return state.getValue(SECONDARY)
                ? !(entity instanceof EnderDragon) : !(entity instanceof WitherBoss);
        if (family == Family.SUPER && state.getValue(SECONDARY))
            return !(entity instanceof EnderDragon || entity instanceof WitherBoss);
        return super.canEntityDestroy(state, level, pos, entity);
    }

    @Override public ToolInteractionSpec toolInteraction(BlockState state, ItemStack tool) {
        return state.getValue(MODE) == Mode.WALL && GTToolHelper.isMachineWrench(tool)
                ? ToolInteractionSpec.facing(FACING, MachineRotationType.ALL) : null;
    }

    @Override public net.minecraft.world.ItemInteractionResult useItemOn(ItemStack held,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){return ToolInteractions.use(state,level,pos,player,hand,hit)?level.isClientSide?net.minecraft.world.ItemInteractionResult.SUCCESS:net.minecraft.world.ItemInteractionResult.CONSUME:net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;}
}
