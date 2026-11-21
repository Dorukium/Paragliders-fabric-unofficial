package tictim.paraglider.mixin.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Exposes vanilla's private pipeline registration for the mod's custom stamina-wheel pipelines. */
@Mixin(RenderPipelines.class)
public interface MixinRenderPipelines {
	@Invoker("register")
	static RenderPipeline paraglider$registerPipeline(RenderPipeline pipeline) {
		throw new AssertionError();
	}
}
