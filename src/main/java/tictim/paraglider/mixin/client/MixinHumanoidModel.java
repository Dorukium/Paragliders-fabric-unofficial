package tictim.paraglider.mixin.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tictim.paraglider.client.ParagliderRenderStateExtension;
import tictim.paraglider.client.ParaglidingArmPose;

@Mixin(HumanoidModel.class)
public abstract class MixinHumanoidModel {
	@Inject(method = "setupAnim", at = @At("TAIL"))
	private void paraglider$applyParaglidingArmPose(HumanoidRenderState state, CallbackInfo info) {
		if (state instanceof ParagliderRenderStateExtension extension && extension.paraglider$isParagliding()) {
			@SuppressWarnings("unchecked")
			HumanoidModel<HumanoidRenderState> model = (HumanoidModel<HumanoidRenderState>)(Object)this;
			ParaglidingArmPose.applyTransform(model, state, HumanoidArm.LEFT);
			ParaglidingArmPose.applyTransform(model, state, HumanoidArm.RIGHT);
		}
	}
}
