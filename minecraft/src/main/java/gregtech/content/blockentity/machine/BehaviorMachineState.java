package gregtech.content.blockentity.machine;

import gregtech.api.addon.platform.BehaviorState;
import gregtech.engine.stack.NbtCompat;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

final class BehaviorMachineState implements BehaviorState {
    private final Map<String, Integer> ints = new LinkedHashMap<>();
    private final Map<String, Long> longs = new LinkedHashMap<>();
    private final Map<String, Boolean> bools = new LinkedHashMap<>();
    private final Map<String, String> strings = new LinkedHashMap<>();
    private final Map<String, ItemStack> items = new LinkedHashMap<>();
    private final Set<String> clientKeys;
    private final Runnable markChanged;
    private boolean clientDirty;

    BehaviorMachineState(Collection<String> clientKeys, Runnable markChanged) {
        this.clientKeys = Set.copyOf(clientKeys);
        this.markChanged = markChanged;
    }

    @Override
    public int getInt(String key) {
        return ints.getOrDefault(check(key), 0);
    }

    @Override
    public void setInt(String key, int value) {
        String checked = check(key);
        Integer previous = ints.put(checked, value);
        if (previous == null || previous != value) clientChanged(checked);
        markChanged.run();
    }

    @Override
    public long getLong(String key) {
        return longs.getOrDefault(check(key), 0L);
    }

    @Override
    public void setLong(String key, long value) {
        String checked = check(key);
        Long previous = longs.put(checked, value);
        if (previous == null || previous != value) clientChanged(checked);
        markChanged.run();
    }

    @Override
    public boolean getBoolean(String key) {
        return bools.getOrDefault(check(key), false);
    }

    @Override
    public void setBoolean(String key, boolean value) {
        String checked = check(key);
        Boolean previous = bools.put(checked, value);
        if (previous == null || previous != value) clientChanged(checked);
        markChanged.run();
    }

    @Override
    public String getString(String key) {
        return strings.getOrDefault(check(key), "");
    }

    @Override
    public void setString(String key, String value) {
        if (value == null) throw new IllegalArgumentException("null behavior string");
        if (value.length() > 256) throw new IllegalArgumentException("behavior string is too long");
        String checked = check(key);
        if (!value.equals(strings.put(checked, value))) clientChanged(checked);
        markChanged.run();
    }

    @Override
    public ItemStack item(String key) {
        ItemStack stack = items.get(check(key));
        return stack == null ? ItemStack.EMPTY : stack.copy();
    }

    @Override
    public void setItem(String key, ItemStack stack) {
        String checked = check(key);
        if (stack == null || stack.isEmpty()) items.remove(checked);
        else items.put(checked, stack.copy());
        markChanged.run();
    }

    void forEachStoredItem(Consumer<ItemStack> action) {
        for (ItemStack stack : items.values()) {
            if (!stack.isEmpty()) action.accept(stack.copy());
        }
    }

    CompoundTag save(HolderLookup.Provider lookup) {
        CompoundTag tag = new CompoundTag();
        CompoundTag intTag = new CompoundTag();
        ints.forEach(intTag::putInt);
        CompoundTag longTag = new CompoundTag();
        longs.forEach(longTag::putLong);
        CompoundTag boolTag = new CompoundTag();
        bools.forEach(boolTag::putBoolean);
        CompoundTag stringTag = new CompoundTag();
        strings.forEach(stringTag::putString);
        CompoundTag itemTag = new CompoundTag();
        items.forEach((key, stack) -> {
            if (!stack.isEmpty()) itemTag.put(key, NbtCompat.saveItem(stack, lookup));
        });
        tag.put("i", intTag);
        tag.put("l", longTag);
        tag.put("b", boolTag);
        tag.put("s", stringTag);
        tag.put("t", itemTag);
        return tag;
    }

    void load(CompoundTag tag, HolderLookup.Provider lookup) {
        ints.clear();
        longs.clear();
        bools.clear();
        strings.clear();
        items.clear();
        CompoundTag intTag = tag.getCompound("i");
        for (String key : intTag.getAllKeys()) ints.put(key, intTag.getInt(key));
        CompoundTag longTag = tag.getCompound("l");
        for (String key : longTag.getAllKeys()) longs.put(key, longTag.getLong(key));
        CompoundTag boolTag = tag.getCompound("b");
        for (String key : boolTag.getAllKeys()) bools.put(key, boolTag.getBoolean(key));
        CompoundTag stringTag = tag.getCompound("s");
        for (String key : stringTag.getAllKeys()) strings.put(key, stringTag.getString(key));
        CompoundTag itemTag = tag.getCompound("t");
        for (String key : itemTag.getAllKeys()) {
            ItemStack stack = NbtCompat.readItem(itemTag.getCompound(key), lookup);
            if (!stack.isEmpty()) items.put(key, stack);
        }
    }

    void requireClientKey(String key) {
        if (!clientKeys.contains(key))
            throw new IllegalArgumentException("Behavior machine client state is not declared: " + key);
    }

    boolean consumeClientChanges() {
        boolean changed = clientDirty;
        clientDirty = false;
        return changed;
    }

    void writeClientState(CompoundTag tag) {
        CompoundTag intTag = new CompoundTag();
        CompoundTag longTag = new CompoundTag();
        CompoundTag boolTag = new CompoundTag();
        CompoundTag stringTag = new CompoundTag();
        for (String key : clientKeys) {
            Integer integer = ints.get(key);
            if (integer != null) intTag.putInt(key, integer);
            Long value = longs.get(key);
            if (value != null) longTag.putLong(key, value);
            Boolean flag = bools.get(key);
            if (flag != null) boolTag.putBoolean(key, flag);
            String text = strings.get(key);
            if (text != null) stringTag.putString(key, text);
        }
        tag.put("i", intTag);
        tag.put("l", longTag);
        tag.put("b", boolTag);
        tag.put("s", stringTag);
    }

    void readClientState(CompoundTag tag) {
        CompoundTag intTag = tag.getCompound("i");
        for (String key : intTag.getAllKeys()) if (clientKeys.contains(key)) ints.put(key, intTag.getInt(key));
        CompoundTag longTag = tag.getCompound("l");
        for (String key : longTag.getAllKeys()) if (clientKeys.contains(key)) longs.put(key, longTag.getLong(key));
        CompoundTag boolTag = tag.getCompound("b");
        for (String key : boolTag.getAllKeys()) if (clientKeys.contains(key)) bools.put(key, boolTag.getBoolean(key));
        CompoundTag stringTag = tag.getCompound("s");
        for (String key : stringTag.getAllKeys())
            if (clientKeys.contains(key)) strings.put(key, stringTag.getString(key));
    }

    private void clientChanged(String key) {
        if (clientKeys.contains(key)) clientDirty = true;
    }

    private static String check(String key) {
        if (key == null || !key.matches("[a-z0-9_.]{1,64}"))
            throw new IllegalArgumentException("Invalid behavior state key: " + key);
        return key;
    }
}
