package com.magpie.infinitefusion;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PartyPosition;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;

import java.util.LinkedHashSet;
import java.util.Set;

public final class FusionDataHelper {
    public static final String ROOT_KEY = InfiniteFusionMod.MOD_ID;
    public static final String DONOR_TAG = "DonorPokemon";
    public static final String DONOR_NAME = "DonorDisplayName";
    public static final String DONOR_SPECIES = "DonorSpecies";
    public static final String FUSION_ID = "FusionId";
    public static final String ORIGINAL_HOST_SPECIES = "OriginalHostSpecies";
    public static final String ORIGINAL_FORCED_ASPECTS = "OriginalForcedAspects";
    public static final String FUSION_ASPECT = "FusionAspect";

    private FusionDataHelper() {
    }

    public static StoredPokemon getStoredPokemon(ItemStack stack) {
        return stack.get(ModDataComponents.STORED_POKEMON.get());
    }

    public static void setStoredPokemon(ItemStack stack, StoredPokemon storedPokemon) {
        stack.set(ModDataComponents.STORED_POKEMON.get(), storedPokemon);
    }

    public static void clearStoredPokemon(ItemStack stack) {
        stack.remove(ModDataComponents.STORED_POKEMON.get());
    }

    public static boolean hasStoredPokemon(ItemStack stack) {
        return getStoredPokemon(stack) != null;
    }

    public static boolean isFused(Pokemon pokemon) {
        return pokemon.getPersistentData().contains(ROOT_KEY, Tag.TAG_COMPOUND);
    }

    public static StoredPokemon getStoredDonorFromFusion(Pokemon pokemon) {
        CompoundTag root = pokemon.getPersistentData().getCompound(ROOT_KEY);
        if (root.isEmpty() || !root.contains(DONOR_TAG, Tag.TAG_COMPOUND)) {
            return null;
        }

        return new StoredPokemon(
                root.getString(DONOR_SPECIES),
                root.getString(DONOR_NAME),
                root.getCompound(DONOR_TAG).toString()
        );
    }

    public static void applyFusion(Pokemon host, Pokemon donor, FusionDefinition definition, PokemonEntity hostEntity) {
        CompoundTag root = new CompoundTag();
        root.put(DONOR_TAG, donor.saveToNBT(hostEntity.level().registryAccess(), new CompoundTag()));
        root.putString(DONOR_NAME, donor.getDisplayName(false).getString());
        root.putString(DONOR_SPECIES, donor.getSpecies().getResourceIdentifier().toString());
        root.putString(FUSION_ID, definition.id());
        root.putString(FUSION_ASPECT, definition.resultAspect());
        root.putString(ORIGINAL_HOST_SPECIES, host.getSpecies().getResourceIdentifier().toString());

        ListTag forcedAspects = new ListTag();
        for (String aspect : host.getForcedAspects()) {
            forcedAspects.add(StringTag.valueOf(aspect));
        }
        root.put(ORIGINAL_FORCED_ASPECTS, forcedAspects);

        host.getPersistentData().put(ROOT_KEY, root);

        Species resultSpecies = FusionDefinition.resolveSpecies(definition.result().speciesId());
        if (resultSpecies != null && host.getSpecies() != resultSpecies) {
            host.setSpecies(resultSpecies);
        }

        Set<String> nextForcedAspects = new LinkedHashSet<>(host.getForcedAspects());
        nextForcedAspects.addAll(definition.result().aspects());
        nextForcedAspects.add(definition.resultAspect());
        host.setForcedAspects(Set.copyOf(nextForcedAspects));
        host.updateAspects();
        host.updateForm();
        host.onChange(null);
        hostEntity.setPokemon(host);
    }

    public static StoredPokemon extractFusion(Pokemon host, PokemonEntity hostEntity) {
        StoredPokemon donor = getStoredDonorFromFusion(host);
        if (donor == null) {
            return null;
        }

        CompoundTag root = host.getPersistentData().getCompound(ROOT_KEY);
        String originalHostSpecies = root.getString(ORIGINAL_HOST_SPECIES);
        ListTag originalForcedAspects = root.getList(ORIGINAL_FORCED_ASPECTS, Tag.TAG_STRING);

        host.getPersistentData().remove(ROOT_KEY);

        Species originalSpecies = FusionDefinition.resolveSpecies(originalHostSpecies);
        if (originalSpecies != null && host.getSpecies() != originalSpecies) {
            host.setSpecies(originalSpecies);
        }

        Set<String> restoredForcedAspects = new LinkedHashSet<>();
        for (Tag entry : originalForcedAspects) {
            restoredForcedAspects.add(entry.getAsString());
        }
        host.setForcedAspects(Set.copyOf(restoredForcedAspects));
        host.updateAspects();
        host.updateForm();
        host.onChange(null);
        hostEntity.setPokemon(host);
        return donor;
    }

    public static boolean returnToParty(ServerPlayer player, Pokemon pokemon) {
        PartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
        PartyPosition firstOpen = party.getFirstAvailablePosition();
        if (firstOpen == null) {
            return false;
        }

        party.set(firstOpen, pokemon);
        pokemon.onChange(null);
        return true;
    }

    public static boolean returnToParty(ServerPlayer player, StoredPokemon storedPokemon) {
        Pokemon pokemon = storedPokemon.toPokemon(player.registryAccess());
        return returnToParty(player, pokemon);
    }

    public static boolean removeFromParty(ServerPlayer player, Pokemon pokemon) {
        PartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
        pokemon.recall();
        return party.remove(pokemon);
    }

    public static void message(ServerPlayer player, String translationKey) {
        player.sendSystemMessage(Component.translatable(translationKey));
    }

    public static void syncHeldItem(ServerPlayer player, InteractionHand hand, ItemStack stack) {
        player.setItemInHand(hand, stack.copy());
        player.inventoryMenu.broadcastChanges();
        player.containerMenu.broadcastChanges();
    }
}
