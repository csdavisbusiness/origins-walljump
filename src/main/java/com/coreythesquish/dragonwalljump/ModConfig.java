package com.coreythesquish.dragonwalljump;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class ModConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static boolean autoRotation;
    public static double wallJumpHeight;
    public static int wallSlideDelay;
    public static int stopWallSlideDelay;
    public static int maxWallJumps;

    static {
        BUILDER.push("general");
        autoRotation = BUILDER.define("autoRotation", false);
        wallJumpHeight = BUILDER.defineInRange("wallJumpHeight", 0.55, 0.0, 1.0);
        wallSlideDelay = BUILDER.defineInRange("wallSlideDelay", 15, 0, Integer.MAX_VALUE);
        stopWallSlideDelay = BUILDER.defineInRange("stopWallSlideDelay", 72000, 0, Integer.MAX_VALUE);
        maxWallJumps = BUILDER.defineInRange("maxWallJumps", 72000, 0, Integer.MAX_VALUE);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}