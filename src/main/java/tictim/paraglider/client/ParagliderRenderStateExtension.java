package tictim.paraglider.client;

/** Extra render-state flag added by mixin; replaces NeoForge's arm-pose enum extension. */
public interface ParagliderRenderStateExtension {
	boolean paraglider$isParagliding();
	void paraglider$setParagliding(boolean value);
}
