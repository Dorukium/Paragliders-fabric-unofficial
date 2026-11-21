package tictim.paraglider.event;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import org.jspecify.annotations.NullMarked;
import tictim.paraglider.ParagliderClientMod;
import tictim.paraglider.api.movement.Movement;
import tictim.paraglider.client.render.InGameStaminaWheelRenderer;
import tictim.paraglider.client.screen.ParagliderSettingsScreen;

/** Fabric replacement for the NeoForge client event bus listeners. */
@NullMarked
public final class ParagliderClientEventHandler {
	private ParagliderClientEventHandler() {}

	public static void registerClient() {
		ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
			if (ParagliderClientMod.instance().getParagliderSettingsKey().consumeClick()) {
				// Upstream binds Ctrl+P; Fabric's KeyMapping has no modifier field.
				boolean control = com.mojang.blaze3d.platform.InputConstants.isKeyDown(
						minecraft.getWindow(), org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL)
						|| com.mojang.blaze3d.platform.InputConstants.isKeyDown(
						minecraft.getWindow(), org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL);
				if (control && minecraft.gui.screen() == null) {
					minecraft.gui.setScreen(new ParagliderSettingsScreen());
				}
			}
		});

		// Fabric: returning TRUE cancels vanilla attack + block breaking.
		// Only intercept while paragliding (upstream RenderHandEvent/InteractionKeyMappingTriggered).
		ClientPreAttackCallback.EVENT.register((minecraft, player, count) ->
				Movement.get(player).state().paragliding());

		// Also cancel client-side right-clicks so the arm-swing animation does not play.
		net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player, level, hand, hit) ->
				level.isClientSide() && shouldCancelInteraction(player, hand) ?
						net.minecraft.world.InteractionResult.FAIL : net.minecraft.world.InteractionResult.PASS);
		net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.register((player, level, hand, entity, hit) ->
				level.isClientSide() && shouldCancelInteraction(player, hand) ?
						net.minecraft.world.InteractionResult.FAIL : net.minecraft.world.InteractionResult.PASS);
		net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.register((player, level, hand) ->
				level.isClientSide() && shouldCancelInteraction(player, hand) ?
						net.minecraft.world.InteractionResult.FAIL : net.minecraft.world.InteractionResult.PASS);

		net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.replaceElement(
				net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements.CROSSHAIR,
				original -> (graphics, deltaTracker) -> {
					if (!shouldHideCrosshair()) original.extractRenderState(graphics, deltaTracker);
				});

		net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents.BEFORE_BLOCK_OUTLINE.register(
				(context, state) -> !shouldHideBlockOutline());

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
				InGameStaminaWheelRenderer.get().reset());
	}

	/** Called from client render mixins. */
	public static boolean shouldCancelOffHandRender() {
		LocalPlayer player = Minecraft.getInstance().player;
		return player != null && Movement.get(player).state().paragliding();
	}

	/** Called from a crosshair GUI-layer mixin. */
	public static boolean shouldHideCrosshair() {
		return Minecraft.getInstance().gui.screen() instanceof tictim.paraglider.client.screen.BargainScreen;
	}

	/** Called from a block-outline render mixin. */
	public static boolean shouldHideBlockOutline() {
		LocalPlayer player = Minecraft.getInstance().player;
		return player != null && Movement.get(player).state().paragliding();
	}

	/** Called from client interaction callbacks. */
	public static boolean shouldCancelInteraction(net.minecraft.world.entity.player.Player player, InteractionHand hand) {
		return Movement.get(player).state().paragliding();
	}
}
