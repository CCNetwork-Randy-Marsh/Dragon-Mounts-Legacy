package com.github.kay9.dragonmounts.data.providers;

import com.github.kay9.dragonmounts.DragonMountsLegacy;
import com.github.kay9.dragonmounts.client.DragonModel;
import com.github.kay9.dragonmounts.dragon.DragonBreed;
import com.google.gson.JsonParseException;
import com.mojang.serialization.JsonOps;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;

import java.util.concurrent.CompletableFuture;

public class ModelPropertiesProvider implements DataProvider
{
    private final DataGenerator gen;

    public ModelPropertiesProvider(DataGenerator gen)
    {
        this.gen = gen;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput pOutput)
    {
        return CompletableFuture.allOf(
                save(pOutput, DragonBreed.BuiltIn.FIRE.location().getPath(), new DragonModel.Properties(false, false, false)),
                save(pOutput, DragonBreed.BuiltIn.GHOST.location().getPath(), new DragonModel.Properties(true, false, true)),
                save(pOutput, DragonBreed.BuiltIn.WATER.location().getPath(), new DragonModel.Properties(true, true, false)));
    }

    private CompletableFuture<?> save(CachedOutput cache, String id, DragonModel.Properties instance)
    {
        var jsonObj = DragonModel.Properties.CODEC.encodeStart(JsonOps.INSTANCE, instance).getOrThrow(JsonParseException::new);
        var path = gen.getPackOutput().getOutputFolder().resolve("assets/" + DragonMountsLegacy.MOD_ID + "/models/entity/dragon/breed/properties/" + id + ".json");
        return DataProvider.saveStable(cache, jsonObj, path);
    }

    @Override
    public String getName()
    {
        return DragonMountsLegacy.MOD_ID + ": Dragon Model Properties";
    }
}
