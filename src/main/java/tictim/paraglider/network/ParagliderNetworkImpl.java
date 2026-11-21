package tictim.paraglider.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;
import tictim.paraglider.ParagliderMod;
import tictim.paraglider.bargain.BargainCatalog;
import tictim.paraglider.bargain.BargainContext;
import tictim.paraglider.impl.movement.PlayerStateMap;
import tictim.paraglider.network.message.*;
import tictim.paraglider.wind.WindChunk;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("SameParameterValue")
@NullMarked
public class ParagliderNetworkImpl implements ParagliderNetwork {
	public static final String NETVERSION = "4";

	private static boolean commonRegistered;
	private static boolean clientRegistered;
	private static @Nullable MinecraftServer server;

	public ParagliderNetworkImpl() {
		registerCommon();
	}

	public static void setServer(@Nullable MinecraftServer server) {
		ParagliderNetworkImpl.server = server;
	}

	private static synchronized void registerCommon() {
		if (commonRegistered) return;
		commonRegistered = true;

		PayloadTypeRegistry.clientboundPlay().register(SyncPlayerStateMapMsg.TYPE, SyncPlayerStateMapMsg.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(SyncMovementMsg.TYPE, SyncMovementMsg.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(SyncRemoteMovementMsg.TYPE, SyncRemoteMovementMsg.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(SyncVesselMsg.TYPE, SyncVesselMsg.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(SyncCanUseParagliderMsg.TYPE, SyncCanUseParagliderMsg.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(BargainInitMsg.TYPE, BargainInitMsg.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(SyncCatalogMsg.TYPE, SyncCatalogMsg.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(SyncLookAtMsg.TYPE, SyncLookAtMsg.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(BargainDialogMsg.TYPE, BargainDialogMsg.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(SyncWindMsg.TYPE, SyncWindMsg.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(SetParaglidingMsg.TYPE, SetParaglidingMsg.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(BargainEndMsg.TYPE, BargainEndMsg.CODEC);

		PayloadTypeRegistry.serverboundPlay().register(ApplyParagliderItemCooldownMsg.TYPE, ApplyParagliderItemCooldownMsg.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(BargainMsg.TYPE, BargainMsg.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(SetParaglidingMsg.TYPE, SetParaglidingMsg.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(BargainEndMsg.TYPE, BargainEndMsg.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(ApplyParagliderItemCooldownMsg.TYPE,
				ServerPacketHandler::handleApplyParagliderItemCooldown);
		ServerPlayNetworking.registerGlobalReceiver(BargainMsg.TYPE, ServerPacketHandler::handleBargain);
		ServerPlayNetworking.registerGlobalReceiver(SetParaglidingMsg.TYPE, ServerPacketHandler::handleSetParagliding);
		ServerPlayNetworking.registerGlobalReceiver(BargainEndMsg.TYPE, ServerPacketHandler::handleBargainEnd);
	}

	/** Client receivers must be registered from the client entrypoint only. */
	public static synchronized void registerClient() {
		if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) return;
		if (clientRegistered) return;
		clientRegistered = true;

		ClientPlayNetworking.registerGlobalReceiver(SyncPlayerStateMapMsg.TYPE, (msg, ctx) -> ClientPacketHandler.handleSyncPlayerStateMap(msg));
		ClientPlayNetworking.registerGlobalReceiver(SyncMovementMsg.TYPE, (msg, ctx) -> ClientPacketHandler.handleSyncMovement(msg));
		ClientPlayNetworking.registerGlobalReceiver(SyncRemoteMovementMsg.TYPE, (msg, ctx) -> ClientPacketHandler.handleSyncRemoteMovement(msg));
		ClientPlayNetworking.registerGlobalReceiver(SyncVesselMsg.TYPE, (msg, ctx) -> ClientPacketHandler.handleSyncVessel(msg));
		ClientPlayNetworking.registerGlobalReceiver(SyncCanUseParagliderMsg.TYPE, (msg, ctx) -> ClientPacketHandler.handleSyncCanUseParaglider(msg));
		ClientPlayNetworking.registerGlobalReceiver(BargainInitMsg.TYPE, (msg, ctx) -> ClientPacketHandler.handleBargainInit(msg));
		ClientPlayNetworking.registerGlobalReceiver(SyncCatalogMsg.TYPE, (msg, ctx) -> ClientPacketHandler.handleSyncCatalog(msg));
		ClientPlayNetworking.registerGlobalReceiver(SyncLookAtMsg.TYPE, (msg, ctx) -> ClientPacketHandler.handleSyncLookAt(msg));
		ClientPlayNetworking.registerGlobalReceiver(BargainDialogMsg.TYPE, (msg, ctx) -> ClientPacketHandler.handleBargainDialog(msg));
		ClientPlayNetworking.registerGlobalReceiver(SyncWindMsg.TYPE, (msg, ctx) -> ClientPacketHandler.handleSyncWind(msg));
		ClientPlayNetworking.registerGlobalReceiver(SetParaglidingMsg.TYPE, (msg, ctx) -> ClientPacketHandler.handleSetParagliding(msg));
		ClientPlayNetworking.registerGlobalReceiver(BargainEndMsg.TYPE, (msg, ctx) -> ClientPacketHandler.handleBargainEnd(msg));
	}

	@Override public void syncStateMap(ServerPlayer player, PlayerStateMap stateMap) {
		CustomPacketPayload msg = new SyncPlayerStateMapMsg(stateMap);
		traceSendToPlayer(Kind.MOVEMENT, player, msg);
		ServerPlayNetworking.send(player, msg);
	}

	@Override public void syncStateMapToAll(PlayerStateMap stateMap) {
		CustomPacketPayload msg = new SyncPlayerStateMapMsg(stateMap);
		traceSendToAll(Kind.MOVEMENT, msg);
		if (server == null) return;
		for (ServerPlayer player : PlayerLookup.all(server)) ServerPlayNetworking.send(player, msg);
	}

	@Override public void syncMovement(ServerPlayer player, Identifier state, double stamina, double extraStamina,
	                                   boolean depleted, int recoveryDelay, double efficiency) {
		CustomPacketPayload msg = new SyncMovementMsg(state, stamina, extraStamina, depleted, recoveryDelay, efficiency);
		traceSendToPlayer(Kind.MOVEMENT, player, msg);
		ServerPlayNetworking.send(player, msg);
		syncRemoteMovement(player, state);
	}

	@Override public void syncRemoteMovement(Entity entity, Identifier state) {
		CustomPacketPayload msg = new SyncRemoteMovementMsg(entity.getUUID(), state);
		traceSendToTracking(Kind.MOVEMENT, entity, msg);
		for (ServerPlayer player : PlayerLookup.tracking(entity)) ServerPlayNetworking.send(player, msg);
	}

	@Override public void syncRemoteMovement(Entity entity, ServerPlayer target, Identifier state) {
		CustomPacketPayload msg = new SyncRemoteMovementMsg(entity.getUUID(), state);
		traceSendToPlayer(Kind.MOVEMENT, target, msg);
		ServerPlayNetworking.send(target, msg);
	}

	@Override public void syncVessels(ServerPlayer player, double stamina, double extraStamina, boolean depleted,
	                                  int heartContainers, int staminaVessels) {
		CustomPacketPayload msg = new SyncVesselMsg(stamina, extraStamina, depleted, heartContainers, staminaVessels);
		traceSendToPlayer(Kind.VESSEL, player, msg);
		ServerPlayNetworking.send(player, msg);
	}

	@Override public void syncCanUseParaglider(ServerPlayer player, boolean canUseParaglider, boolean canRideUpdraft) {
		CustomPacketPayload msg = new SyncCanUseParagliderMsg(canUseParaglider, canRideUpdraft);
		traceSendToPlayer(Kind.MOVEMENT, player, msg);
		ServerPlayNetworking.send(player, msg);
	}

	@Override public void setParaglidingToClient(ServerPlayer player, boolean paragliding) {
		CustomPacketPayload msg = new SetParaglidingMsg(paragliding);
		traceSendToPlayer(Kind.MOVEMENT, player, msg);
		ServerPlayNetworking.send(player, msg);
	}

	@Override public void setParaglidingToServer(boolean paragliding) {
		CustomPacketPayload msg = new SetParaglidingMsg(paragliding);
		traceSendToServer(Kind.MOVEMENT, msg);
		ClientPlayNetworking.send(msg);
	}

	@Override public void applyParagliderItemCooldown() {
		CustomPacketPayload msg = ApplyParagliderItemCooldownMsg.INSTANCE;
		traceSendToServer(Kind.MOVEMENT, msg);
		ClientPlayNetworking.send(msg);
	}

	@Override public void initBargain(BargainContext ctx, @Nullable Component dialog) {
		CustomPacketPayload msg = new BargainInitMsg(ctx.sessionId(), ctx.makeCatalog(),
				Optional.ofNullable(ctx.lookAt()), Optional.ofNullable(dialog));
		traceSendToPlayer(Kind.BARGAIN, ctx.player(), msg);
		ServerPlayNetworking.send(ctx.player(), msg);
	}

	@Override public void syncBargainCatalog(BargainContext ctx, List<BargainCatalog> catalog) {
		CustomPacketPayload msg = new SyncCatalogMsg(ctx.sessionId(), catalog);
		traceSendToPlayer(Kind.BARGAIN, ctx.player(), msg);
		ServerPlayNetworking.send(ctx.player(), msg);
	}

	@Override public void syncBargainLookAt(BargainContext ctx, @Nullable Vec3 lookAt) {
		CustomPacketPayload msg = new SyncLookAtMsg(ctx.sessionId(), Optional.ofNullable(lookAt));
		traceSendToPlayer(Kind.BARGAIN, ctx.player(), msg);
		ServerPlayNetworking.send(ctx.player(), msg);
	}

	@Override public void displayBargainDialog(BargainContext ctx, Component dialog) {
		CustomPacketPayload msg = new BargainDialogMsg(ctx.sessionId(), dialog);
		traceSendToPlayer(Kind.BARGAIN, ctx.player(), msg);
		ServerPlayNetworking.send(ctx.player(), msg);
	}

	@Override public void bargain(int sessionId, Identifier bargain) {
		CustomPacketPayload msg = new BargainMsg(sessionId, bargain);
		traceSendToServer(Kind.BARGAIN, msg);
		ClientPlayNetworking.send(msg);
	}

	@Override public void bargainEndToClient(BargainContext ctx) {
		CustomPacketPayload msg = new BargainEndMsg(ctx.sessionId());
		traceSendToPlayer(Kind.BARGAIN, ctx.player(), msg);
		ServerPlayNetworking.send(ctx.player(), msg);
	}

	@Override public void bargainEndToServer(int sessionId) {
		CustomPacketPayload msg = new BargainEndMsg(sessionId);
		traceSendToServer(Kind.BARGAIN, msg);
		ClientPlayNetworking.send(msg);
	}

	@Override public void syncWind(MinecraftServer server, LevelChunk chunk, WindChunk windChunk) {
		CustomPacketPayload msg = new SyncWindMsg(windChunk);
		traceSendToTracking(Kind.WIND, chunk, msg);
		if (chunk.getLevel() instanceof ServerLevel serverLevel) {
			for (ServerPlayer player : PlayerLookup.tracking(serverLevel, chunk.getPos()))
				ServerPlayNetworking.send(player, msg);
		} else {
			ParagliderMod.LOGGER.warn("Failed to send packet {}, not a server level", msg);
		}
	}

	protected static void traceSendToAll(Kind kind, CustomPacketPayload msg) {
		if (kind.isTraceEnabled()) ParagliderMod.LOGGER.debug("Dispatching {} to clients", msg);
	}
	protected static void traceSendToPlayer(Kind kind, ServerPlayer player, CustomPacketPayload msg) {
		if (kind.isTraceEnabled()) ParagliderMod.LOGGER.debug("Dispatching {} to {}", msg, player);
	}
	protected static void traceSendToTracking(Kind kind, Entity entity, CustomPacketPayload msg) {
		if (kind.isTraceEnabled()) ParagliderMod.LOGGER.debug("Dispatching {} to clients tracking entity {}", msg, entity);
	}
	protected static void traceSendToTracking(Kind kind, LevelChunk chunk, CustomPacketPayload msg) {
		if (kind.isTraceEnabled()) ParagliderMod.LOGGER.debug("Dispatching {} to clients tracking chunk {}", msg, chunk);
	}
	protected static void traceSendToServer(Kind kind, CustomPacketPayload msg) {
		if (kind.isTraceEnabled()) ParagliderMod.LOGGER.debug("Dispatching {} to server", msg);
	}
}
