package com.github.kay9.dragonmounts;

import com.github.kay9.dragonmounts.data.loot.DragonEggLootMod;
import com.google.common.collect.ImmutableMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.Map;

public class DMLConfig
{
    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.BooleanValue ALLOW_EGG_OVERRIDE;

    public static boolean allowEggOverride()
    {
        return ALLOW_EGG_OVERRIDE.get();
    }

    private static final ForgeConfigSpec.BooleanValue REPLENISH_EGGS;

    public static boolean replenishEggs()
    {
        return REPLENISH_EGGS.get();
    }

    private static final ForgeConfigSpec.BooleanValue USE_LOOT_TABLES;

    public static boolean useLootTables()
    {
        return USE_LOOT_TABLES.get();
    }

    private static final ForgeConfigSpec.BooleanValue UPDATE_HABITATS;

    public static boolean updateHabitats()
    {
        return UPDATE_HABITATS.get();
    }

    private static final Map<String, ForgeConfigSpec.DoubleValue> CHANCES;

    public static float getEggChanceFor(String configTarget)
    {
        var chance = CHANCES.get(configTarget);
        if (chance == null) return -1f;
        return chance.get().floatValue();
    }

    public static int getReproLimitFor(String configTarget)
    {

    }

    public static String formatEggTargetAsPath(ResourceLocation forBreed, ResourceLocation forTarget)
    {
        return String.format("%s_in_%s_chance",
                forBreed.getPath(),
                forTarget.getPath().substring(forTarget.getPath().lastIndexOf('/') + 1));
    }

    static
    {
        var configurator = new ForgeConfigSpec.Builder()
                .push("config");

        ALLOW_EGG_OVERRIDE = configurator.comment(
                        "Allow the vanilla ender egg to be interacted with? (Hatchable)",
                        "Useful to help with mod compatibility")
                .define("allow_egg_override", true);
        REPLENISH_EGGS = configurator.comment(
                        "Should Ender Dragon Eggs replenish on the exit portal after a respawned dragon is defeated?",
                        "Useful for multiplayer scenarios.")
                .define("replenish_eggs", true);
        USE_LOOT_TABLES = configurator.comment(
                        "Should dragon eggs generate in treasure chest loot tables?",
                        "Useful for multiplayer scenarios and offering alternative ways to obtain eggs.",
                        "Egg chance values can be modified below. If you'd like to change the loot tables the eggs appear",
                        "in, consider using a datapack, since the static nature of configs complicates things.")
                .define("use_loot_tables", false);
        UPDATE_HABITATS = configurator.comment("Should Dragon Eggs adapt to their environments and change breeds?")
                .define("update_habitats", true);

        configurator.pop();


        configurator.comment(
                        "THESE VALUES DO NOT TAKE EFFECT UNTIL `use_loot_tables` ABOVE IS SET TO `true` !!!",
                        "These entries define the chance values of which a dragon egg can appear in its respective loot table.",
                        "Due to the static nature of configs in general, DML cannot modify the chances of custom breed eggs",
                        "outside the built-in defaults, so those should be configured to use minecraft's built in random chance conditions.",
                        "(It is however, possible to point custom egg chances to the built-in values via the loot condition, therefore using a config chance)")
                .push("egg_loot_chances");

        var chances = ImmutableMap.<String, ForgeConfigSpec.DoubleValue>builder();
        for (var target : DragonEggLootMod.BUILT_IN_CHANCES)
        {
            var path = formatEggTargetAsPath(target.forBreed(), target.target());
            var entry = configurator
                    .comment(
                            String.format("The chance that a %s egg appears in %s.", target.forBreed().getPath(), target.target().getPath()),
                            "0 = Never Appears, 1 = Guaranteed")
                    .defineInRange(path, target.chance(), 0, 1);

            chances.put(path, entry);
        }
        CHANCES = chances.build();
        configurator.pop();


        SPEC = configurator.build();
    }

    public static boolean cameraFlight;

    public static boolean cameraFlight()
    {
        return cameraFlight;
    }
}
