package tictim.paraglider.client;

import net.minecraft.client.renderer.state.gui.GuiElementRenderState;

/** Fabric bridge for NeoForge's {@code submitGuiElementRenderState} extension. */
public interface GuiGraphicsExtractorExtension {
	void paraglider$submitGuiElement(GuiElementRenderState state);
}
