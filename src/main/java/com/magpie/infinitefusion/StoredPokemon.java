package com.magpie.infinitefusion;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;

public record StoredPokemon(String speciesId, String displayName, String pokemonData) {
    public static final Codec<StoredPokemon> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("species_id").forGetter(StoredPokemon::speciesId),
            Codec.STRING.fieldOf("display_name").forGetter(StoredPokemon::displayName),
            Codec.STRING.fieldOf("pokemon_data").forGetter(StoredPokemon::pokemonData)
    ).apply(instance, StoredPokemon::new));

    public static StoredPokemon fromPokemon(Pokemon pokemon, RegistryAccess registryAccess) {
        CompoundTag nbt = pokemon.saveToNBT(registryAccess, new CompoundTag());
        return new StoredPokemon(
                pokemon.getSpecies().getResourceIdentifier().toString(),
                pokemon.getDisplayName(false).getString(),
                nbt.toString()
        );
    }

    public Pokemon toPokemon(RegistryAccess registryAccess) {
        try {
            return Pokemon.Companion.loadFromNBT(registryAccess, TagParser.parseTag(this.pokemonData));
        } catch (CommandSyntaxException exception) {
            throw new IllegalStateException("Failed to parse stored donor Pokemon data", exception);
        }
    }
}
