package tictim.paraglider.mixin;

import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tictim.paraglider.network.ParagliderNetwork;
import tictim.paraglider.wind.WindChunk;
import tictim.paraglider.wind.WindLevel;

/** Fabric replacement for NeoForge's ChunkWatchEvent.Watch. */
@Mixin(ServerPlayer.class)
public abstract class MixinPlayerChunkTracking {
	@Unique private ChunkTrackingView paraglider$previousView;

	@Inject(method = "setChunkTrackingView", at = @At("HEAD"))
	private void paraglider$rememberView(ChunkTrackingView view, CallbackInfo info) {
		this.paraglider$previousView = ((ServerPlayer)(Object)this).getChunkTrackingView();
	}

	@Inject(method = "setChunkTrackingView", at = @At("TAIL"))
	private void paraglider$syncNewChunks(ChunkTrackingView view, CallbackInfo info) {
		ChunkTrackingView previous = this.paraglider$previousView;
		if (previous == null) return;
		ServerPlayer player = (ServerPlayer)(Object)this;
		ServerLevel level = player.level();
		WindLevel wind = WindLevel.of(level);
		if (wind == null) return;

		ChunkTrackingView.difference(previous, view, pos -> {
			WindChunk windChunk = wind.getChunk(pos);
			if (windChunk == null || windChunk.isEmpty()) return;
			LevelChunk chunk = level.getChunk(pos.x(), pos.z());
			ParagliderNetwork.get().syncWind(level.getServer(), chunk, windChunk);
		}, pos -> {});
	}
}
