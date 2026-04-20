package com.magpie.infinitefusion;

import net.minecraft.core.component.DataComponentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(InfiniteFusionMod.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<StoredPokemon>> STORED_POKEMON = DATA_COMPONENTS.registerComponentType(
            "stored_pokemon",
            builder -> builder.persistent(StoredPokemon.CODEC)
    );

    private ModDataComponents() {
    }
}
