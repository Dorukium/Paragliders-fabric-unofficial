package tictim.paraglider.event;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.NullMarked;
import tictim.paraglider.wind.WindChunk;
import tictim.paraglider.wind.WindLevel;
import tictim.paraglider.wind.WindLogic;

/** Fabric replacement for the NeoForge level/chunk event listeners. */
@NullMarked
public final class WindEventHandler {
	private WindEventHandler() {}

	/** Client levels have no load event in Fabric API; register/unregister them on level change. */
	public static void registerClient() {
		net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.START_CLIENT_TICK.register(minecraft -> {
			net.minecraft.client.multiplayer.ClientLevel level = minecraft.level;
			if (level != clientLevel) {
				if (clientLevel != null) WindLevel.unregisterLevel(clientLevel);
				if (level != null) WindLevel.registerLevel(level);
				clientLevel = level;
			}
		});
	}

	private static @org.jetbrains.annotations.Nullable net.minecraft.client.multiplayer.ClientLevel clientLevel;

	public static void register() {
		ServerLevelEvents.LOAD.register((server, level) -> WindLevel.registerLevel(level));
		ServerLevelEvents.UNLOAD.register((server, level) -> WindLevel.unregisterLevel(level));

		ServerTickEvents.END_LEVEL_TICK.register(level -> {
			if (!level.isClientSide()) WindLogic.updateWind(level);
		});

		ServerChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> {
			WindLevel wind = WindLevel.of(level);
			if (wind != null) wind.remove(chunk.getPos());
		});
	}

	/** Called from {@code MixinPlayerChunkTracking} when a player starts tracking a chunk. */
	public static void onChunkWatch(ServerLevel level, LevelChunk chunk) {
		WindLevel wind = WindLevel.of(level);
		if (wind == null) return;
		WindChunk windChunk = wind.getChunk(chunk.getPos());
		if (windChunk == null || windChunk.isEmpty()) return;
		tictim.paraglider.network.ParagliderNetwork.get().syncWind(level.getServer(), chunk, windChunk);
	}
}
