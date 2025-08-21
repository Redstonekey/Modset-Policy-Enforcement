package de.joancode.anticheat;

import de.joancode.anticheat.networking.RequestVerificationPayload;
import de.joancode.anticheat.networking.VerificationResponsePayload;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class AnticheatClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(RequestVerificationPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                var mods = FabricLoader.getInstance().getAllMods().stream()
                        .map(c -> c.getMetadata().getId())
                        .toList();
                Anticheat.LOGGER.info("Sending verification response ({} mods).", mods.size());
                ClientPlayNetworking.send(new VerificationResponsePayload(mods));
            });
        });
    }
}
