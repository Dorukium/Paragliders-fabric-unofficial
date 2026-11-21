package tictim.paraglider;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;
import org.lwjgl.glfw.GLFW;
import tictim.paraglider.api.ParagliderAPI;
import tictim.paraglider.client.settings.ParagliderClientSettings;
import tictim.paraglider.client.settings.ParagliderClientSettingsIO;
import tictim.paraglider.impl.movement.PlayerStateMap;
import tictim.paraglider.network.ParagliderNetworkImpl;
import tictim.paraglider.event.ParagliderClientEventHandler;
import tictim.paraglider.impl.stamina.StaminaLoader;

import java.util.Objects;

@NullMarked
public class ParagliderClientMod implements ParagliderMod.IClient, ClientModInitializer {
	private static @Nullable ParagliderClientMod instance;

	public static ParagliderClientMod instance() {
		if (instance == null) throw new IllegalStateException("No client");
		return instance;
	}

	{
		if (instance != null) throw new IllegalStateException("Paraglider mod instantiated twice");
		instance = this;
	}

	private boolean removeStaminaWheel;
	private @org.jetbrains.annotations.Nullable String staminaWheelRemoverId;
	private @org.jetbrains.annotations.Nullable ParagliderClientSettings clientSettings;
	private @org.jetbrains.annotations.Nullable PlayerStateMap syncedStateMap;
	private @org.jetbrains.annotations.Nullable KeyMapping paragliderSettingsKey;

	@Override
	public void onInitializeClient() {
		ParagliderMod.instance().client = this;

		var pair = StaminaLoader.loadStaminaWheelRemoverId();
		this.removeStaminaWheel = pair.getFirst();
		this.staminaWheelRemoverId = pair.getSecond();

		this.paragliderSettingsKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.paraglider.paragliderSettings",
				InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_P, KeyMapping.Category.MISC));

		net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.addLast(
				tictim.paraglider.api.ParagliderAPI.id("stamina_wheel"),
				tictim.paraglider.client.ParagliderGuiLayers::staminaWheel);
		net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.addLast(
				tictim.paraglider.api.ParagliderAPI.id("movement_debug"),
				tictim.paraglider.client.ParagliderGuiLayers::movementDebug);

		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTING.register(
				server -> this.setSyncedStateMap(null));

		ParagliderNetworkImpl.registerClient();
		fuzs.forgeconfigapiport.fabric.api.v5.client.ConfigScreenFactoryRegistry.INSTANCE.register(
				ParagliderAPI.MODID,
				(modId, parent) -> new net.neoforged.neoforge.client.gui.ConfigurationScreen(modId, parent));

		ParagliderClientEventHandler.registerClient();
		tictim.paraglider.event.WindEventHandler.registerClient();
		tictim.paraglider.event.WindParticleEventHandler.register();
		tictim.paraglider.client.ParagliderRenderTypes.register();
		tictim.paraglider.client.WindParticleProvider.register();

		tictim.paraglider.mixin.client.MixinConditionalItemModelProperties.paraglider$idMapper()
				.put(ParagliderAPI.id("paragliding"),
						tictim.paraglider.client.ParaglidingItemProperty.CODEC);

		ParagliderClientSettingsIO.load(null);
	}

	public ParagliderClientSettings getSettings() {
		return Objects.requireNonNullElse(this.clientSettings, ParagliderClientSettings.DEFAULT);
	}

	public void setSettings(@org.jetbrains.annotations.Nullable ParagliderClientSettings settings) {
		this.clientSettings = settings;
	}

	public KeyMapping getParagliderSettingsKey() {
		if (this.paragliderSettingsKey == null)
			throw new IllegalStateException("paragliderSettingsKey is not available yet");
		return this.paragliderSettingsKey;
	}

	@Override public @org.jetbrains.annotations.Nullable PlayerStateMap getSyncedStateMap() {
		return this.syncedStateMap;
	}

	public void setSyncedStateMap(@org.jetbrains.annotations.Nullable PlayerStateMap stateMap) {
		this.syncedStateMap = stateMap;
	}

	public boolean removeStaminaWheel() {
		return this.removeStaminaWheel;
	}

	public @org.jetbrains.annotations.Nullable String staminaWheelRemoverId() {
		return this.staminaWheelRemoverId;
	}
}
