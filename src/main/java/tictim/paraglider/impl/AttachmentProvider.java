package tictim.paraglider.impl;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import org.jspecify.annotations.NullMarked;
import tictim.paraglider.impl.movement.ClientPlayerMovement;
import tictim.paraglider.impl.movement.PlayerMovement;
import tictim.paraglider.impl.movement.RemotePlayerMovement;
import tictim.paraglider.impl.movement.ServerPlayerMovement;

@NullMarked
public final class AttachmentProvider {
	private AttachmentProvider() {}

	public static PlayerMovement createPlayerMovement(Player player) {
		if (player instanceof ServerPlayer sp) return new ServerPlayerMovement(sp);
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT)
			return ClientImpl.createPlayerMovement(player);
		return new RemotePlayerMovement(player);
	}

	private static final class ClientImpl {
		static PlayerMovement createPlayerMovement(Player player) {
			return player instanceof LocalPlayer localPlayer ?
					new ClientPlayerMovement(localPlayer) :
					new RemotePlayerMovement(player);
		}
	}

	/** Lazily attaches a player-scoped value, mirroring NeoForge's holder-aware attachment initializer. */
	public static <T> T getOrCreate(Player player, AttachmentType<T> type,
	                               java.util.function.Function<Player, T> factory) {
		return player.getAttachedOrCreate(type, () -> factory.apply(player));
	}
}
