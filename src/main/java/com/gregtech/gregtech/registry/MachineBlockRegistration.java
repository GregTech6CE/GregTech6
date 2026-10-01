package com.gregtech.gregtech.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;
import java.util.Objects;
import java.util.function.Function;

/** Registers a controller/part block and its item together, with the same stable ID. */
public final class MachineBlockRegistration {
    private MachineBlockRegistration() {}
    public static Builder block(String id, Function<BlockBehaviour.Properties, ? extends Block> factory) {
        return new Builder(id, factory);
    }

    public static final class Builder {
        private final String id;
        private final Function<BlockBehaviour.Properties, ? extends Block> factory;
        private float hardness = 6;
        private float resistance = 6;
        private boolean registered;
        private Builder(String id, Function<BlockBehaviour.Properties, ? extends Block> factory) {
            if (id == null || !id.matches("[a-z0-9/._-]+")) throw new IllegalArgumentException("Invalid block ID: " + id);
            this.id = id; this.factory = Objects.requireNonNull(factory);
        }
        public Builder strength(float hardness, float resistance) {
            if (!Float.isFinite(hardness) || !Float.isFinite(resistance)) throw new IllegalArgumentException("Invalid strength for " + id);
            this.hardness = hardness; this.resistance = resistance; return this;
        }
        public RegistryObject<Block> register() {
            if (registered) throw new IllegalStateException("Block definition already registered: " + id);
            registered = true;
            // Capture a snapshot: later changes to the builder cannot change deferred construction.
            float blockHardness = hardness, blockResistance = resistance;
            RegistryObject<Block> block = GTBlocks.BLOCKS.register(id, () -> factory.apply(
                    BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                            .strength(blockHardness, blockResistance).requiresCorrectToolForDrops()));
            GTBlocks.BLOCK_ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
            return block;
        }
    }
}
