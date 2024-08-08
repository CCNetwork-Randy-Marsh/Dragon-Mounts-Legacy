package com.github.kay9.dragonmounts;

import com.github.kay9.dragonmounts.data.loot.DragonEggLootMod;
import com.github.kay9.dragonmounts.data.loot.conditions.RandomChanceByConfig;
import com.github.kay9.dragonmounts.dragon.DragonBreed;
import com.github.kay9.dragonmounts.dragon.DragonSpawnEgg;
import com.github.kay9.dragonmounts.dragon.TameableDragon;
import com.github.kay9.dragonmounts.dragon.egg.HatchableEggBlock;
import com.github.kay9.dragonmounts.dragon.egg.HatchableEggBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryManager;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static net.minecraftforge.registries.ForgeRegistries.Keys;

public class DMLRegistry
{
    protected static void init(IEventBus bus)
    {
        REGISTRIES.values().forEach(r -> r.register(bus));
        REGISTRIES.clear(); // Registration happens once, no need to stick around.
    }

    private static final Map<ResourceKey<? extends Registry<?>>, DeferredRegister<?>> REGISTRIES = new HashMap<>();

    public static final RegistryObject<Block> EGG_BLOCK = register("dragon_egg", Registries.BLOCK, HatchableEggBlock::new);

    public static final RegistryObject<Item> EGG_BLOCK_ITEM = register(EGG_BLOCK.getId().getPath(), Registries.ITEM, HatchableEggBlock.Item::new);
    public static final RegistryObject<Item> SPAWN_EGG = register("spawn_egg", Registries.ITEM, DragonSpawnEgg::new);

    public static final RegistryObject<SoundEvent> DRAGON_AMBIENT_SOUND = sound("entity.dragon.ambient");
    public static final RegistryObject<SoundEvent> DRAGON_STEP_SOUND = sound("entity.dragon.step");
    public static final RegistryObject<SoundEvent> DRAGON_DEATH_SOUND = sound("entity.dragon.death");
    public static final RegistryObject<SoundEvent> GHOST_DRAGON_AMBIENT = sound("entity.dragon.ambient.ghost");

    public static final RegistryObject<EntityType<TameableDragon>> DRAGON = register("dragon", Registries.ENTITY_TYPE, () -> EntityType.Builder.of(TameableDragon::new, MobCategory.CREATURE).sized(TameableDragon.BASE_WIDTH, TameableDragon.BASE_HEIGHT).clientTrackingRange(10).updateInterval(3).build(DragonMountsLegacy.MOD_ID + ":dragon"));

    public static final RegistryObject<BlockEntityType<HatchableEggBlockEntity>> EGG_BLOCK_ENTITY = register("dragon_egg", Registries.BLOCK_ENTITY_TYPE, () -> BlockEntityType.Builder.of(HatchableEggBlockEntity::new, EGG_BLOCK.get()).build(null));

    public static final RegistryObject<MapCodec<DragonEggLootMod>> EGG_LOOT_MODIFIER = register("dragon_egg_loot", Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, () -> DragonEggLootMod.CODEC);

    public static final RegistryObject<LootItemConditionType> RANDOM_CHANCE_CONFIG_CONDITION = register("random_chance_by_config", Registries.LOOT_CONDITION_TYPE, () -> new LootItemConditionType(RandomChanceByConfig.CODEC));

    public static final RegistryObject<DataComponentType<ResourceKey<DragonBreed>>> DRAGON_BREED_COMPONENT = register("dragon_breed", Registries.DATA_COMPONENT_TYPE, () -> DataComponentType.<ResourceKey<DragonBreed>>builder().persistent(ResourceKey.codec(DragonBreed.REGISTRY_KEY)).networkSynchronized(ResourceKey.streamCodec(DragonBreed.REGISTRY_KEY)).build());
    public static final RegistryObject<DataComponentType<DragonSpawnEgg.Colors>> SPAWN_EGG_COLORS_COMPONENT = register("spawn_egg_colors", Registries.DATA_COMPONENT_TYPE, () -> DataComponentType.<DragonSpawnEgg.Colors>builder().build());

    private static RegistryObject<SoundEvent> sound(String name)
    {
        return register(name, Registries.SOUND_EVENT, () -> SoundEvent.createVariableRangeEvent(DragonMountsLegacy.id(name)));
    }

    @SuppressWarnings("unchecked")
    private static <T, I extends T> RegistryObject<I> register(String name, ResourceKey<Registry<T>> forType, Supplier<I> sup)
    {
        var registry = (DeferredRegister<T>) REGISTRIES.computeIfAbsent(forType, t ->
        {
            var fr = RegistryManager.ACTIVE.getRegistry(forType);
            if (fr == null) return DeferredRegister.create(forType, DragonMountsLegacy.MOD_ID);
            return DeferredRegister.create(fr, DragonMountsLegacy.MOD_ID);
        });
        return registry.register(name, sup);
    }
}
