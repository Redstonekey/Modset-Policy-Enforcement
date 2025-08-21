package de.joancode.anticheat.networking;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Empty server->client verification request payload. */
public record RequestVerificationPayload() implements CustomPayload {
    public static final Identifier IDENTIFIER = Identifier.of("anticheat", "request_verification");
    public static final CustomPayload.Id<RequestVerificationPayload> ID = new CustomPayload.Id<>(IDENTIFIER);
    public static final PacketCodec<RegistryByteBuf, RequestVerificationPayload> CODEC = PacketCodec.unit(new RequestVerificationPayload());

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
