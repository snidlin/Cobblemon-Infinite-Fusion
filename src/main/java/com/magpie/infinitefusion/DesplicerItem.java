package com.magpie.infinitefusion;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class DesplicerItem extends Item {
    public DesplicerItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        StoredPokemon storedPokemon = FusionDataHelper.getStoredPokemon(stack);
        if (storedPokemon != null) {
            tooltipComponents.add(Component.translatable("tooltip.infinitefusion.holding", storedPokemon.displayName()).withStyle(ChatFormatting.AQUA));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
        }

        if (!FusionDataHelper.hasStoredPokemon(stack)) {
            return new InteractionResultHolder<>(InteractionResult.PASS, stack);
        }

        if (returnStoredPokemon(stack, serverPlayer, usedHand)) {
            return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
        }

        return new InteractionResultHolder<>(InteractionResult.FAIL, stack);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        if (FusionDataHelper.hasStoredPokemon(stack)) {
            return returnStoredPokemon(stack, serverPlayer, hand) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }

        if (!(entity instanceof PokemonEntity pokemonEntity)) {
            return InteractionResult.PASS;
        }

        Pokemon pokemon = pokemonEntity.getPokemon();
        if (!pokemon.belongsTo(player) || !FusionDataHelper.isFused(pokemon)) {
            return InteractionResult.PASS;
        }

        StoredPokemon donor = FusionDataHelper.extractFusion(pokemon, pokemonEntity);
        if (donor == null) {
            return InteractionResult.FAIL;
        }

        FusionDataHelper.setStoredPokemon(stack, donor);
        FusionDataHelper.syncHeldItem(serverPlayer, hand, stack);
        FusionDataHelper.message(serverPlayer, "message.infinitefusion.unfused");
        return InteractionResult.SUCCESS;
    }

    private boolean returnStoredPokemon(ItemStack stack, ServerPlayer player, InteractionHand hand) {
        StoredPokemon storedPokemon = FusionDataHelper.getStoredPokemon(stack);
        if (storedPokemon == null) {
            return false;
        }

        if (FusionDataHelper.returnToParty(player, storedPokemon)) {
            FusionDataHelper.clearStoredPokemon(stack);
            FusionDataHelper.syncHeldItem(player, hand, stack);
            FusionDataHelper.message(player, "message.infinitefusion.returned_donor");
            return true;
        }

        FusionDataHelper.message(player, "message.infinitefusion.make_room");
        return false;
    }
}
