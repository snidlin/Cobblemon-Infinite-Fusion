package com.magpie.infinitefusion;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(InfiniteFusionMod.MOD_ID);

    public static final DeferredHolder<Item, SplicerItem> SPLICER = ITEMS.register("splicer",
            () -> new SplicerItem(new Item.Properties().stacksTo(1))
    );

    public static final DeferredHolder<Item, DesplicerItem> DESPLICER = ITEMS.register("desplicer",
            () -> new DesplicerItem(new Item.Properties().stacksTo(1))
    );

    private ModItems() {
    }
}
