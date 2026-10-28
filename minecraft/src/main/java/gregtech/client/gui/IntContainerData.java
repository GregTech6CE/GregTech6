package gregtech.client.gui;

import net.minecraft.world.inventory.ContainerData;

/** Preserve full int values through the vanilla menu protocol's signed-short fields. */
final class IntContainerData implements ContainerData {
    private final ContainerData values;

    IntContainerData(ContainerData values) { this.values = values; }

    @Override public int get(int index) {
        return (values.get(index / 2) >>> (16 * (index % 2))) & 0xFFFF;
    }

    @Override public void set(int index, int value) {
        int shift = 16 * (index % 2);
        int previous = values.get(index / 2);
        values.set(index / 2, (previous & ~(0xFFFF << shift)) | ((value & 0xFFFF) << shift));
    }

    @Override public int getCount() { return values.getCount() * 2; }
}
