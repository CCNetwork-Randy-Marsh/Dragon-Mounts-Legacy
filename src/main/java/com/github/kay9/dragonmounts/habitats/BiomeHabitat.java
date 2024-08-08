package com.github.kay9.dragonmounts.habitats;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

public record BiomeHabitat(int points, TagKey<Biome> biomeTag) implements Habitat
{
    public static final MapCodec<BiomeHabitat> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Habitat.withPoints(2, BiomeHabitat::points),
            TagKey.codec(Registries.BIOME).fieldOf("biome_tag").forGetter(BiomeHabitat::biomeTag)
    ).apply(instance, BiomeHabitat::new));

    @Override
    public int getHabitatPoints(Level level, BlockPos pos)
    {
        return level.getBiome(pos).is(biomeTag)? points : 0;
    }

    @Override
    public ResourceLocation type()
    {
        return Habitat.BIOMES;
    }
}
