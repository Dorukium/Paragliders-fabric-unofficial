package tictim.paraglider.mixin.client;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tictim.paraglider.event.ParagliderClientEventHandler;

/** Hides the off-hand while paragliding (NeoForge RenderHandEvent). */
@Mixin(ItemInHandRenderer.class)
public abstract class MixinItemInHandRenderer {
	@Inject(method = "evaluateWhichHandsToRender", at = @At("RETURN"), cancellable = true)
	private static void paraglider$hideOffHand(LocalPlayer player,
	                                           CallbackInfoReturnable<ItemInHandRenderer.HandRenderSelection> info) {
		if (ParagliderClientEventHandler.shouldCancelOffHandRender()) {
			info.setReturnValue(ItemInHandRenderer.HandRenderSelection.RENDER_MAIN_HAND_ONLY);
		}
	}
}
