package tictim.paraglider;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import org.jspecify.annotations.NullMarked;
import tictim.paraglider.api.movement.MovementPlugin;
import tictim.paraglider.api.plugin.ParagliderPlugin;
import tictim.paraglider.api.plugin.ParagliderPluginBase;
import tictim.paraglider.api.plugin.PluginInstance;
import tictim.paraglider.api.stamina.StaminaPlugin;
import tictim.paraglider.plugin.ParagliderPluginLoader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** Entrypoint-based replacement for NeoForge's annotation scanner. */
@NullMarked
public final class FabricParagliderPluginLoader implements ParagliderPluginLoader {
	private static final Class<?>[] ALL_PLUGIN_TYPES = {StaminaPlugin.class, MovementPlugin.class};

	private boolean initialized;
	private final List<PluginInstance<StaminaPlugin>> staminaPlugins = new ArrayList<>();
	private final List<PluginInstance<MovementPlugin>> movementPlugins = new ArrayList<>();

	@Override public List<PluginInstance<StaminaPlugin>> getStaminaPlugins() {
		checkAndInitialize();
		return Collections.unmodifiableList(this.staminaPlugins);
	}

	@Override public List<PluginInstance<MovementPlugin>> getMovementPlugins() {
		checkAndInitialize();
		return Collections.unmodifiableList(this.movementPlugins);
	}

	private void checkAndInitialize() {
		if (this.initialized) return;
		this.initialized = true;

		List<PluginInstance<?>> plugins = new ArrayList<>();
		for (EntrypointContainer<ParagliderPluginBase> container : FabricLoader.getInstance()
				.getEntrypointContainers(ParagliderPlugin.FABRIC_ENTRYPOINT, ParagliderPluginBase.class)) {
			ParagliderPluginBase entrypoint = container.getEntrypoint();
			if (!checkPluginType(entrypoint)) {
				ParagliderMod.LOGGER.warn("Plugin {} does not implement any supported plugin type; supported types are:\n  {}",
						entrypoint.getClass(), Arrays.stream(ALL_PLUGIN_TYPES)
								.map(Class::getCanonicalName).collect(Collectors.joining("\n  ")));
				continue;
			}
			String modId = container.getProvider().getMetadata().getId();
			plugins.add(new PluginInstance<>(entrypoint, modId));
			ParagliderMod.LOGGER.debug("Loaded plugin {} associated with mod {}", entrypoint.getClass(), modId);
		}

		ParagliderMod.LOGGER.debug("Loaded {} plugins total", plugins.size());
		for (PluginInstance<?> plugin : plugins) {
			if (plugin.instance() instanceof StaminaPlugin) this.staminaPlugins.add(plugin.cast(StaminaPlugin.class));
			if (plugin.instance() instanceof MovementPlugin) this.movementPlugins.add(plugin.cast(MovementPlugin.class));
		}
	}

	private static boolean checkPluginType(ParagliderPluginBase instance) {
		for (Class<?> pluginType : ALL_PLUGIN_TYPES) {
			if (pluginType.isInstance(instance)) return true;
		}
		return false;
	}
}
