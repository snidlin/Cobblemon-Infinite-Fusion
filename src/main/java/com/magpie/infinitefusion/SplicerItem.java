package com.magpie.infinitefusion;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class SplicerItem extends Item {
    public SplicerItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return FusionDataHelper.hasStoredPokemon(stack) || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        StoredPokemon storedPokemon = FusionDataHelper.getStoredPokemon(stack);
        if (storedPokemon != null) {
            tooltipComponents.add(Component.translatable("tooltip.infinitefusion.holding", storedPokemon.displayName()).withStyle(ChatFormatting.AQUA));
        }
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        if (!(entity instanceof PokemonEntity pokemonEntity)) {
            return InteractionResult.PASS;
        }

        Pokemon targetPokemon = pokemonEntity.getPokemon();
        if (!targetPokemon.belongsTo(player)) {
            return InteractionResult.PASS;
        }

        if (!FusionDataHelper.hasStoredPokemon(stack)) {
            return loadDonor(stack, serverPlayer, hand, pokemonEntity, targetPokemon);
        }

        return applyFusion(stack, serverPlayer, hand, pokemonEntity, targetPokemon);
    }

    private InteractionResult loadDonor(ItemStack stack, ServerPlayer player, InteractionHand hand, PokemonEntity pokemonEntity, Pokemon donorPokemon) {
        if (FusionDataHelper.isFused(donorPokemon)) {
            FusionDataHelper.message(player, "message.infinitefusion.already_fused");
            return InteractionResult.SUCCESS;
        }

        FusionDataHelper.setStoredPokemon(stack, StoredPokemon.fromPokemon(donorPokemon, player.registryAccess()));
        FusionDataHelper.syncHeldItem(player, hand, stack);

        if (!FusionDataHelper.removeFromParty(player, donorPokemon)) {
            FusionDataHelper.clearStoredPokemon(stack);
            FusionDataHelper.syncHeldItem(player, hand, stack);
            return InteractionResult.FAIL;
        }
        pokemonEntity.discard();

        FusionDataHelper.message(player, "message.infinitefusion.loaded_splicer");
        return InteractionResult.SUCCESS;
    }

    private InteractionResult applyFusion(ItemStack stack, ServerPlayer player, InteractionHand hand, PokemonEntity hostEntity, Pokemon hostPokemon) {
        if (FusionDataHelper.isFused(hostPokemon)) {
            FusionDataHelper.message(player, "message.infinitefusion.already_fused");
            return InteractionResult.SUCCESS;
        }

        StoredPokemon storedPokemon = FusionDataHelper.getStoredPokemon(stack);
        if (storedPokemon == null) {
            return InteractionResult.FAIL;
        }

        Pokemon donorPokemon = storedPokemon.toPokemon(player.registryAccess());
        var maybeDefinition = FusionRegistry.findFusion(player, donorPokemon, hostPokemon);
        if (maybeDefinition.isEmpty()) {
            FusionDataHelper.message(player, "message.infinitefusion.invalid_pair");
            if (FusionDataHelper.returnToParty(player, donorPokemon)) {
                FusionDataHelper.clearStoredPokemon(stack);
                FusionDataHelper.syncHeldItem(player, hand, stack);
            } else {
                FusionDataHelper.message(player, "message.infinitefusion.make_room");
            }
            return InteractionResult.SUCCESS;
        }

        FusionDataHelper.applyFusion(hostPokemon, donorPokemon, maybeDefinition.get(), hostEntity);
        FusionDataHelper.clearStoredPokemon(stack);
        FusionDataHelper.syncHeldItem(player, hand, stack);
        FusionDataHelper.message(player, "message.infinitefusion.fused");
        return InteractionResult.SUCCESS;
    }
}