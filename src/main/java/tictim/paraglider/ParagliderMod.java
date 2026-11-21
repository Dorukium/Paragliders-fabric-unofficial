package tictim.paraglider;

import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import net.fabricmc.api.ModInitializer;
import net.minecraft.world.entity.EntityTypes;
import net.neoforged.fml.config.ModConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;
import tictim.paraglider.api.ParagliderAPI;
import tictim.paraglider.api.bargain.BargainPreview;
import tictim.paraglider.api.bargain.BargainType;
import tictim.paraglider.bargain.BargainRecipeChecker;
import tictim.paraglider.event.ParagliderEventHandler;
import tictim.paraglider.event.ParagliderClientEventHandler;
import tictim.paraglider.event.WindEventHandler;
import tictim.paraglider.config.*;
import tictim.paraglider.contents.BargainTypeRegistry;
import tictim.paraglider.contents.Contents;
import tictim.paraglider.contents.ParagliderVillageStructures;
import tictim.paraglider.impl.DefaultParagliderItemCapability;
import tictim.paraglider.impl.movement.PlayerStateConnectionMap;
import tictim.paraglider.impl.movement.PlayerStateMap;
import tictim.paraglider.impl.movement.PlayerStateMapLoader;
import tictim.paraglider.impl.stamina.StaminaEfficiencyLogicHandlerImpl;
import tictim.paraglider.impl.stamina.StaminaEfficiencyLogicLoader;
import tictim.paraglider.impl.stamina.StaminaLoader;
import tictim.paraglider.network.ParagliderNetwork;
import tictim.paraglider.network.ParagliderNetworkImpl;
import tictim.paraglider.plugin.ParagliderPluginLoader;
import tictim.paraglider.wind.WindSource;
import tictim.paraglider.wind.WindSourceRegistry;

@NullMarked
public class ParagliderMod implements ModInitializer {
	public static final Logger LOGGER = LogManager.getLogger("Paraglider");

	private static @Nullable ParagliderMod instance;

	public static ParagliderMod instance() {
		if (instance == null) throw new IllegalStateException("Mod instance not ready yet");
		return instance;
	}

	{
		if (instance != null) throw new IllegalStateException("Paraglider mod instantiated twice");
		instance = this;
	}

	private final CommonConfig commonCfg = new CommonConfig();
	private Contents contents;
	private final ServerConfig config = new ServerConfig();

	private ParagliderNetworkImpl network;
	private final ParagliderPluginLoader pluginLoader = new FabricParagliderPluginLoader();

	private PlayerStateMapConfig stateMapConfig;
	private PlayerStateConnectionMap connectionMap;

	private final WindSourceRegistry windSourceRegistry = new WindSourceRegistry();

	private @org.jetbrains.annotations.Nullable String staminaFactoryOrigin;

	@ApiStatus.Internal
	public @Nullable IClient client;

	@Override
	public void onInitialize() {
		Contents.bootstrapPlatformRegistries();

		this.contents = new Contents();

		ParagliderAPI.setMovementSupplier(p -> p.getAttachedOrCreate(Contents.get().playerMovement(),
				() -> tictim.paraglider.impl.AttachmentProvider.createPlayerMovement(p)));
		ParagliderAPI.setStaminaSupplier(p -> p.getAttachedOrCreate(Contents.get().playerMovement(),
				() -> tictim.paraglider.impl.AttachmentProvider.createPlayerMovement(p)).stamina());
		ParagliderAPI.setVesselContainerSupplier(p -> p.getAttachedOrCreate(Contents.get().vesselContainer()));

		var pair = PlayerStateMapLoader.loadStates();
		this.stateMapConfig = new PlayerStateMapConfig(pair.getFirst());
		this.connectionMap = pair.getSecond();
		var pair2 = StaminaLoader.loadStaminaFactory();
		ParagliderAPI.setStaminaFactory(pair2.getFirst());
		this.staminaFactoryOrigin = pair2.getSecond();
		ParagliderAPI.setStaminaEfficiencyLogicHandler(new StaminaEfficiencyLogicHandlerImpl(
				StaminaEfficiencyLogicLoader.loadStaminaEfficiencyLogics()));

		ParagliderAPI.setDefaultParagliderItemCapability(new DefaultParagliderItemCapability());

		ConfigRegistry.INSTANCE.register(ParagliderAPI.MODID, ModConfig.Type.COMMON, this.commonCfg.spec);
		ConfigRegistry.INSTANCE.register(ParagliderAPI.MODID, ModConfig.Type.SERVER, this.config.spec);
		ConfigRegistry.INSTANCE.register(ParagliderAPI.MODID, ModConfig.Type.COMMON,
				this.stateMapConfig.spec, PlayerStateMapConfig.FILENAME);

		net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
				EntityTypes.PLAYER, net.minecraft.world.entity.player.Player.createAttributes()
						.add(this.contents.maxStamina())
						.add(this.contents.staminaEfficiency())
						.add(this.contents.staminaRecovery())
						.add(this.contents.paraglidingStaminaEfficiency())
						.add(this.contents.runningStaminaEfficiency())
						.add(this.contents.swimmingStaminaEfficiency()));

		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			ParagliderNetworkImpl.setServer(server);
			this.stateMapConfig.removeCallbacks();
			this.stateMapConfig.reload(null);
			ParagliderUtils.printPlayerStates(this.stateMapConfig.stateMap(), getPlayerConnectionMap());
			this.stateMapConfig.addCallback(stateMap -> {
				ParagliderUtils.printPlayerStates(stateMap, getPlayerConnectionMap());
				ParagliderNetwork.get().syncStateMapToAll(stateMap);
			});
		});
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
			if (!success) return;
			var lookup = server.registryAccess();
			this.windSourceRegistry.computeWindSource(lookup);
			tictim.paraglider.contents.ParagliderVillageStructures.addVillageStructures(lookup);
			new BargainRecipeChecker(server.getRecipeManager(), lookup).check();
		});
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			ParagliderNetworkImpl.setServer(null);
			this.stateMapConfig.removeCallbacks();
		});

		tictim.paraglider.contents.loot.ParagliderLootTables.register();
		ParagliderEventHandler.register();
		WindEventHandler.register();
		net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register(
				(dispatcher, buildContext, selection) -> dispatcher.register(ParagliderCommands.register()));

		this.network = new ParagliderNetworkImpl();
	}

	public Cfg getConfig() { return this.config; }
	public DebugCfg getDebugConfig() { return this.commonCfg; }
	public FeatureCfg getFeatureConfig() { return this.commonCfg; }
	public Contents getContents() { return this.contents; }
	public ParagliderNetwork getNetwork() { return this.network; }
	public ParagliderPluginLoader getPluginLoader() { return this.pluginLoader; }

	public PlayerStateMap getPlayerStateMap() {
		if (this.client != null) {
			PlayerStateMap m = this.client.getSyncedStateMap();
			if (m != null) return m;
		}
		return getLocalPlayerStateMap();
	}

	public PlayerStateMap getLocalPlayerStateMap() { return this.stateMapConfig.stateMap(); }
	public PlayerStateConnectionMap getPlayerConnectionMap() { return this.connectionMap; }
	public PlayerStateMapConfig getPlayerStateMapConfig() { return this.stateMapConfig; }
	public WindSourceRegistry windSourceRegistry() { return this.windSourceRegistry; }
	public @org.jetbrains.annotations.Nullable String staminaFactoryOrigin() { return this.staminaFactoryOrigin; }

	@ApiStatus.Internal
	public interface IClient {
		@org.jetbrains.annotations.Nullable PlayerStateMap getSyncedStateMap();
	}
}
