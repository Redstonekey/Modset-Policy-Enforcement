package de.joancode.anticheat;

import de.joancode.anticheat.config.ConfigManager;
import de.joancode.anticheat.networking.RequestVerificationPayload;
import de.joancode.anticheat.networking.VerificationResponsePayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Anticheat implements ModInitializer {
	public static final String MOD_ID = "anticheat";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static Set<String> BANNED_MODS = Collections.emptySet();

	private static boolean PAYLOADS_REGISTERED = false;

	private final Set<UUID> verifiedPlayers = ConcurrentHashMap.newKeySet();
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();


	@Override
	public void onInitialize() {
		BANNED_MODS = ConfigManager.loadBannedMods();
		LOGGER.info("Loaded {} banned mod(s) from config.", BANNED_MODS.size());

		// Register custom payload codecs once (runs on both client + server because environment="*")
		if (!PAYLOADS_REGISTERED) {
			PayloadTypeRegistry.playS2C().register(RequestVerificationPayload.ID, RequestVerificationPayload.CODEC);
			PayloadTypeRegistry.playC2S().register(VerificationResponsePayload.ID, VerificationResponsePayload.CODEC);
			PAYLOADS_REGISTERED = true;
		}

		// Register the event for when a player joins
		// First, during connection initialization, immediately reject clients that do not have our mod (no negotiated channel)
		ServerPlayConnectionEvents.INIT.register((handler, server) -> {
			// If the client cannot receive our verification payload channel, the mod is not installed
			if (!ServerPlayNetworking.canSend(handler.getPlayer(), RequestVerificationPayload.ID)) {
				LOGGER.warn("Disconnecting {}: missing required AntiCheat client mod.", handler.getPlayer().getName().getString());
				handler.disconnect(Text.of("You must install the AntiCheat mod to join this server."));
			}
		});

		// On join, start verification timeout for clients that passed the initial mod-present check
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			final ServerPlayerEntity player = handler.getPlayer();
			final UUID playerUuid = player.getUuid();

			// Safety: if mod somehow not present (race / edge), double-check and kick
			if (!ServerPlayNetworking.canSend(player, RequestVerificationPayload.ID)) {
				LOGGER.warn("Player {} reached JOIN without AntiCheat mod; disconnecting.", player.getName().getString());
				handler.disconnect(Text.of("You must install the AntiCheat mod to join this server."));
				return;
			}

			// Schedule a task to kick the player if they don't verify in time
				scheduler.schedule(() -> {
				if (!verifiedPlayers.contains(playerUuid)) {
					// Use the server's main thread to kick the player
					server.execute(() -> {
						LOGGER.warn("Player {} failed to verify in time. Kicking.", player.getName().getString());
						handler.disconnect(Text.of("Please install the server's anti-cheat mod."));
					});
				}
			}, 10, TimeUnit.SECONDS); // 10-second timeout

			// Send verification request to the client (they have the channel, ensured above)
			ServerPlayNetworking.send(player, new RequestVerificationPayload());
		});

		// Register the handler for the client's response
		ServerPlayNetworking.registerGlobalReceiver(VerificationResponsePayload.ID, (payload, context) -> {
			var player = context.player();
			var mods = payload.installedMods();
			var offending = mods.stream().filter(BANNED_MODS::contains).toList();
			if (!offending.isEmpty()) {
				LOGGER.warn("Player {} has banned mods: {}", player.getName().getString(), offending);
				player.networkHandler.disconnect(Text.of("Banned mods: " + String.join(", ", offending)));
				return;
			}
			LOGGER.info("Player {} verified ({} mods).", player.getName().getString(), mods.size());
			verifiedPlayers.add(player.getUuid());
		});

		// Clean up verified players when they disconnect
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			verifiedPlayers.remove(handler.getPlayer().getUuid());
		});
	}
}
