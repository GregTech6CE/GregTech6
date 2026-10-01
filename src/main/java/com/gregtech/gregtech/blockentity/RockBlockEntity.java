package com.gregtech.gregtech.blockentity;

import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Surface rock (GT6 {@code MultiTileEntityRock}): a small pebble lying on the ground
 * carrying a material — rocks above ore veins use the vein material, telling the
 * player what generates below. Drops the {@code rockGt} item of its material.
 */
public class RockBlockEntity extends BlockEntity {
    public static final String TAG_MATERIAL = "material";
    public static final String TAG_RAW_ORE = "rawOre";
    public static final String TAG_ITEM = "item";
    public static final String TAG_COUNT = "count";
    public static final String DEFAULT_MATERIAL = "Stone";
    public static final ModelProperty<String> MATERIAL_PROPERTY = new ModelProperty<>();

    private String material = DEFAULT_MATERIAL;
    /** Rocks above bedrock deposits hide a raw ore chunk of their material. */
    private boolean rawOre;
    /**
     * GT6's item-on-the-ground mode (multi-tile 32757): when set, the rock yields this item instead of
     * the {@code rockGt} of its material — the Nether scatter drops gems and flint that way.
     */
    private String itemId = "";
    /** Placed GT6 rocks can contain a full stack (for example the Sky Stone in dungeon barracks). */
    private int count = 1;

    public int count() { return count; }

    public void setCount(int count) {
        this.count = Math.max(1, Math.min(64, count));
        setChanged();
    }

    public String itemId() { return itemId; }

    public void setItemId(String id) {
        itemId = id == null ? "" : id;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /** The stack this rock hands out, when it carries an explicit item id. */
    public net.minecraft.world.item.ItemStack itemStack() {
        if (itemId.isEmpty()) return net.minecraft.world.item.ItemStack.EMPTY;
        var item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
                net.minecraft.resources.ResourceLocation.parse(itemId));
        return item == null ? net.minecraft.world.item.ItemStack.EMPTY : new net.minecraft.world.item.ItemStack(item, count);
    }

    public boolean hasRawOre() {
        return rawOre;
    }

    public void setRawOre(boolean rawOre) {
        this.rawOre = rawOre;
        setChanged();
    }

    public RockBlockEntity(BlockPos pos, BlockState state) {
        super(GTBlockEntities.ROCK.get(), pos, state);
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        if (material == null || material.isEmpty()) material = DEFAULT_MATERIAL;
        if (this.material.equals(material)) return;
        this.material = material;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        String s = tag.getString(TAG_MATERIAL);
        boolean changed = !s.isEmpty() && !s.equals(material);
        if (!s.isEmpty()) material = s;
        rawOre = tag.getBoolean(TAG_RAW_ORE);
        itemId = tag.getString(TAG_ITEM);
        count = tag.contains(TAG_COUNT) ? Math.max(1, Math.min(64, tag.getInt(TAG_COUNT))) : 1;
        if (changed && level != null && level.isClientSide) {
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 8);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!DEFAULT_MATERIAL.equals(material)) tag.putString(TAG_MATERIAL, material);
        if (rawOre) tag.putBoolean(TAG_RAW_ORE, true);
        if (!itemId.isEmpty()) tag.putString(TAG_ITEM, itemId);
        if (count != 1) tag.putInt(TAG_COUNT, count);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull ModelData getModelData() {
        return ModelData.builder().with(MATERIAL_PROPERTY, material).build();
    }
}
