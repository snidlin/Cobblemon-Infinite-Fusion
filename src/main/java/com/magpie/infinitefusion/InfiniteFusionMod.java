package com.magpie.infinitefusion;

import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@Mod(InfiniteFusionMod.MOD_ID)
public final class InfiniteFusionMod {
    public static final String MOD_ID = "infinitefusion";

    public InfiniteFusionMod(IEventBus modEventBus) {
        ModItems.ITEMS.register(modEventBus);
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);

        modEventBus.addListener(InfiniteFusionMod::addCreativeTabContents);
    }

    private static void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.SPLICER.get());
            event.accept(ModItems.DESPLICER.get());
        }
    }
}
