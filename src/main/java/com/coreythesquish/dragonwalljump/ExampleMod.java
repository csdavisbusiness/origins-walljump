package com.coreythesquish.dragonwalljump;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("l5rwalljump")
public class L5RWallJump {
    public L5RWallJump() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(this::onConfigLoad);
    }

    private void onConfigLoad(ModConfigEvent event) {
        ModConfig.SPEC.setConfig(event.getConfig().getConfigData());
    }
}