package tictim.paraglider.event;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import org.jspecify.annotations.NullMarked;
import tictim.paraglider.ParagliderMod;
import tictim.paraglider.api.movement.Movement;
import tictim.paraglider.api.stamina.Stamina;
import tictim.paraglider.bargain.BargainHandler;
import tictim.paraglider.contents.item.consumeeffect.GiveExtraStaminaConsumeEffect;
import tictim.paraglider.contents.item.consumeeffect.GiveStaminaEfficiencyConsumeEffect;
import tictim.paraglider.contents.item.consumeeffect.RestoreStaminaConsumeEffect;
import tictim.paraglider.contents.mobeffect.StaminaEfficiencyMobEffect;
import tictim.paraglider.impl.movement.PlayerMovement;
import tictim.paraglider.impl.movement.ServerPlayerMovement;
import tictim.paraglider.network.ParagliderNetwork;

import java.text.DecimalFormat;
import java.util.List;

/** Fabric replacement for the NeoForge common event bus listeners. */
@NullMarked
public final class ParagliderEventHandler {
	private ParagliderEventHandler() {}

	public static void register() {
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> onInteract(player, hand));
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> onInteract(player, hand));
		UseItemCallback.EVENT.register((player, level, hand) -> onInteract(player, hand));
		AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> onInteract(player, hand));
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> onInteract(player, hand));

		EntityTrackingEvents.START_TRACKING.register((entity, player) -> {
			if (entity instanceof Player tracking && player instanceof ServerPlayer serverPlayer)
				ParagliderNetwork.get().syncRemoteMovement(tracking, serverPlayer,
						Movement.get(tracking).state().id());
		});

		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> {
			if (Movement.get(player) instanceof ServerPlayerMovement spm) spm.markForSync();
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> BargainHandler.update());

		ServerPlayerEvents.JOIN.register(player -> {
			ParagliderNetwork.get().syncStateMap(player, ParagliderMod.instance().getLocalPlayerStateMap());
			if (Movement.get(player) instanceof ServerPlayerMovement spm) spm.markForSync();
		});

		ItemTooltipCallback.EVENT.register(ParagliderEventHandler::addAttributeTooltips);
	}

	private static InteractionResult onInteract(Player player, InteractionHand hand) {
		if (hand == InteractionHand.OFF_HAND && Movement.get(player).state().paragliding())
			return InteractionResult.FAIL;
		return InteractionResult.PASS;
	}

	/** Called from {@code MixinPlayer} on every tick and when item use should be cancelled. */
	public static void afterPlayerTick(Player player) {
		if (Movement.get(player) instanceof PlayerMovement movement) movement.update();
		if (Movement.get(player).state().paragliding()) player.stopUsingItem();
	}

	/** Called from {@code MixinPlayer} when the player starts using an item. */
	public static boolean onStartUseItem(Player player) {
		return !Movement.get(player).state().paragliding();
	}

	private static final DecimalFormat D0 = new DecimalFormat("0.#");
	private static final DecimalFormat PCT = new DecimalFormat("+0%");

	private static void addAttributeTooltips(ItemStack stack, Item.@org.jetbrains.annotations.Nullable TooltipContext context,
	                                          net.minecraft.world.item.TooltipFlag tooltipFlag, List<Component> tooltip) {
		Consumable consumable = stack.get(DataComponents.CONSUMABLE);
		if (consumable == null) return;
		for (ConsumeEffect effect : consumable.onConsumeEffects()) {
			switch (effect) {
				case GiveExtraStaminaConsumeEffect(double amount) ->
						tooltip.add(Component.translatable("tooltip.paraglider.give_extra_stamina",
								Component.literal(D0.format(amount / Stamina.STAMINA_PER_WHEEL))
										.withStyle(ChatFormatting.YELLOW)
						).withStyle(ChatFormatting.GREEN));
				case RestoreStaminaConsumeEffect(double amount) ->
						tooltip.add(Component.translatable("tooltip.paraglider.restore_stamina",
								Component.literal(D0.format(amount / Stamina.STAMINA_PER_WHEEL))
										.withStyle(ChatFormatting.YELLOW)
						).withStyle(ChatFormatting.GREEN));
				case GiveStaminaEfficiencyConsumeEffect(int level, int duration) ->
						tooltip.add(Component.translatable("tooltip.paraglider.give_stamina_efficiency",
								Component.literal(PCT.format(
										StaminaEfficiencyMobEffect.EFFICIENCY_PER_LEVEL * (level + 1)
								)).withStyle(ChatFormatting.YELLOW),
								Component.literal(D0.format(duration / 20.0)).withStyle(ChatFormatting.YELLOW)
						).withStyle(ChatFormatting.GREEN));
				default -> {}
			}
		}
	}
}
