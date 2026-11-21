package tictim.paraglider.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tictim.paraglider.event.ParagliderEventHandler;

/** Cancels item use while paragliding (NeoForge LivingEntityUseItemEvent.Start). */
@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity {
	@Inject(method = "startUsingItem", at = @At("HEAD"), cancellable = true)
	private void paraglider$cancelUseWhileParagliding(InteractionHand hand, CallbackInfo info) {
		@SuppressWarnings("DataFlowIssue")
		LivingEntity self = (LivingEntity)(Object)this;
		if (self instanceof net.minecraft.world.entity.player.Player player &&
				!ParagliderEventHandler.onStartUseItem(player)) {
			info.cancel();
		}
	}
}
