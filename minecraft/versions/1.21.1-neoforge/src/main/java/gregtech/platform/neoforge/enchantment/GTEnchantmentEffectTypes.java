package gregtech.platform.neoforge.enchantment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import gregtech.content.tool.GTEnchantmentEffects;
import gregtech.content.tool.GTEnchantmentKind;
import gregtech.engine.mod.GregTechIdentity;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Native data-driven post-attack effects delegate to the shared GT6 rules. */
public final class GTEnchantmentEffectTypes {
    private static final DeferredRegister<MapCodec<? extends EnchantmentEntityEffect>> TYPES =
            DeferredRegister.create(Registries.ENCHANTMENT_ENTITY_EFFECT_TYPE, GregTechIdentity.MOD_ID);

    static {
        TYPES.register("material_debuff", () -> MaterialDebuff.CODEC);
    }

    private GTEnchantmentEffectTypes() {}

    public static void register(IEventBus bus) {
        TYPES.register(bus);
    }

    public record MaterialDebuff(GTEnchantmentKind kind) implements EnchantmentEntityEffect {
        public static final MapCodec<MaterialDebuff> CODEC = Codec.STRING
                .comapFlatMap(
                        name -> java.util.Arrays.stream(GTEnchantmentKind.values())
                                .filter(kind -> kind.path().equals(name))
                                .findFirst()
                                .map(DataResult::success)
                                .orElseGet(() -> DataResult.error(() -> "Unknown GT6 enchantment: " + name)),
                        GTEnchantmentKind::path)
                .fieldOf("kind")
                .xmap(MaterialDebuff::new, MaterialDebuff::kind);

        @Override
        public void apply(
                ServerLevel level, int enchantmentLevel, EnchantedItemInUse item, Entity target, Vec3 origin) {
            GTEnchantmentEffects.apply(kind, enchantmentLevel, target);
        }

        @Override
        public MapCodec<? extends EnchantmentEntityEffect> codec() {
            return CODEC;
        }
    }
}
