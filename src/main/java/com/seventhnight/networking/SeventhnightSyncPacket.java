package com.seventhnight.networking;

import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public class SeventhnightSyncPacket {

    public static final Identifier ID = new Identifier("seventhnight", "sync");

    public final boolean active;

    public SeventhnightSyncPacket(boolean active) {
        this.active = active;
    }

    // Esto lo usa el servidor para escribir el mensaje
    public PacketByteBuf toBuf() {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeBoolean(this.active);
        return buf;
    }

    // 🆕 Esto es lo que necesita el cliente para leer el mensaje
    public static SeventhnightSyncPacket fromBuf(PacketByteBuf buf) {
        return new SeventhnightSyncPacket(buf.readBoolean());
    }
}