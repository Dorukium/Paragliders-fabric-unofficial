package tictim.paraglider.mixin.client;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import tictim.paraglider.client.ParagliderRenderStateExtension;

@Mixin(HumanoidRenderState.class)
public abstract class MixinHumanoidRenderState implements ParagliderRenderStateExtension {
	@Unique private boolean paraglider$paragliding;

	@Override public boolean paraglider$isParagliding() {
		return this.paraglider$paragliding;
	}

	@Override public void paraglider$setParagliding(boolean value) {
		this.paraglider$paragliding = value;
	}
}
