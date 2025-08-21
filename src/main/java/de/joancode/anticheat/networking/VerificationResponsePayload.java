package de.joancode.anticheat.networking;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/** Client->Server: sends the client's installed mod ids. */
public record VerificationResponsePayload(List<String> installedMods) implements CustomPayload {
    public static final Identifier IDENTIFIER = Identifier.of("anticheat", "verification_response");
    public static final CustomPayload.Id<VerificationResponsePayload> ID = new CustomPayload.Id<>(IDENTIFIER);
    public static final PacketCodec<RegistryByteBuf, VerificationResponsePayload> CODEC = new PacketCodec<>() {
        @Override
        public VerificationResponsePayload decode(RegistryByteBuf buf) {
            int size = buf.readVarInt();
            List<String> mods = new ArrayList<>(size);
            for (int i = 0; i < size; i++) mods.add(buf.readString());
            return new VerificationResponsePayload(mods);
        }
        @Override
        public void encode(RegistryByteBuf buf, VerificationResponsePayload value) {
            List<String> mods = value.installedMods();
            buf.writeVarInt(mods.size());
            for (String id : mods) buf.writeString(id);
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
