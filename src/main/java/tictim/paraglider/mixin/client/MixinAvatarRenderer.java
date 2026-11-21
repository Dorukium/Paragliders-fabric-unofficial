package tictim.paraglider.mixin.client;

import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tictim.paraglider.ParagliderUtils;
import tictim.paraglider.api.movement.Movement;
import tictim.paraglider.client.ParagliderRenderStateExtension;

@Mixin(AvatarRenderer.class)
public abstract class MixinAvatarRenderer {
	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void paraglider$markParagliding(Avatar entity, AvatarRenderState state, float partialTick,
	                                        CallbackInfo info) {
		boolean paragliding = entity instanceof net.minecraft.world.entity.player.Player player &&
				ParagliderUtils.holdingUsableParaglider(player) &&
				Movement.get(player).state().paragliding();
		((ParagliderRenderStateExtension)(Object)state).paraglider$setParagliding(paragliding);
	}
}
