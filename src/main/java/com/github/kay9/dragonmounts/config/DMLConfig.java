package com.github.kay9.dragonmounts.config;

import com.github.kay9.dragonmounts.dragon.TameableDragon;
import net.minecraftforge.common.ForgeConfigSpec;

public class DMLConfig
{
    public static final ForgeConfigSpec COMMON;

    private static final ForgeConfigSpec.BooleanValue ALLOW_EGG_OVERRIDE;

    public static boolean allowEggOverride()
    {
        return ALLOW_EGG_OVERRIDE.get();
    }


    //    static final ForgeConfigSpec CLIENT;

    public static boolean cameraFlight;

    public static boolean cameraFlight()
    {
        return cameraFlight;
    }


    public static final ForgeConfigSpec SERVER;

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

    private static final ForgeConfigSpec.BooleanValue CONFIG_LOOT_CHANCES;

    public static boolean useConfigLootValues()
    {
        return CONFIG_LOOT_CHANCES.get();
    }

    private static final ForgeConfigSpec.BooleanValue UPDATE_HABITATS;

    public static boolean updateHabitats()
    {
        return UPDATE_HABITATS.get();
    }

    private static final ForgeConfigSpec.IntValue REPRO_LIMIT;

    public static int reproLimit()
    {
        return REPRO_LIMIT.get();
    }

    static // common
    {
        var configurator = new ForgeConfigSpec.Builder().push("common");

        ALLOW_EGG_OVERRIDE = configurator.comment("Allow the vanilla ender egg to be interacted with? (Hatchable)",
                        "Useful to help with mod compatibility")
                .define("allow_egg_override", true);

        COMMON = configurator.pop().build();
    }

    static // server
    {
        var configurator = new ForgeConfigSpec.Builder().push("server");

        REPLENISH_EGGS = configurator.comment("Should Ender Dragon Eggs replenish on the exit portal after a respawned dragon is defeated?",
                        "Useful for multiplayer scenarios.")
                .define("replenish_eggs", true);
        USE_LOOT_TABLES = configurator.comment("Should dragon eggs generate in treasure chest loot tables?",
                        "Useful for multiplayer scenarios and offering alternative ways to obtain eggs.",
                        "Different types of egg breeds can be found in different chests (if configured.)")
                .define("use_loot_tables", false);
        CONFIG_LOOT_CHANCES = configurator.comment(
                "Should we instead use the loot table chance values found in the config instead of the datapack values?",
                "Enabling this will cause the built-in dragon eggs to use the config chance values found in `egg-loot.toml`",
                "(found in the same folder) INSTEAD of the datapack presets.",
                "Due to the static nature of configs in general, DML cannot modify the chances of custom breed eggs",
                "outside of the built-in defaults, so those will continue to use their datapack presets instead.",
                "(It is however, possible to point custom breed egg chances to the built-in values via loot modifier.)")
                .define("use_config_loot_values", false);
        UPDATE_HABITATS = configurator.comment("Should Dragon Eggs adapt to their environments and change breeds?")
                .define("update_habitats", true);
        REPRO_LIMIT = configurator.comment("Number of times a dragon is able to breed.")
                .defineInRange("breed_limit", TameableDragon.BASE_REPRO_LIMIT, 0, Integer.MAX_VALUE);

        SERVER = configurator.pop().build();
    }
}