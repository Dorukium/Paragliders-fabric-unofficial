package tictim.paraglider.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import tictim.paraglider.client.GuiGraphicsExtractorExtension;

@Mixin(GuiGraphicsExtractor.class)
public abstract class MixinGuiGraphicsExtractor implements GuiGraphicsExtractorExtension {
	@Shadow private GuiRenderState guiRenderState;

	@Override
	public void paraglider$submitGuiElement(GuiElementRenderState state) {
		this.guiRenderState.addGuiElement(state);
	}
}
