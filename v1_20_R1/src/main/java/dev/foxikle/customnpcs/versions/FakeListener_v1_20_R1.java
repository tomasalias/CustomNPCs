package dev.foxikle.customnpcs.versions;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.jetbrains.annotations.NotNull;

public class FakeListener_v1_20_R1 extends ServerGamePacketListenerImpl {
    /**
     * <p> Creates a fake ServerGamePacketListenerImpl for NPCs
     * </p>
     * @param server The server
     * @param connection The connection
     * @param npc The NPC
     */
    public FakeListener_v1_20_R1(MinecraftServer server, Connection connection, ServerPlayer npc) {
        super(server, connection, npc);
    }
    /**
     * <p> Overrides the default ServerGamePacketListenerImpl's send packet method
     * </p>
     * @param packet The packet that won't be sent.
     */
    @Override
    public void send(@NotNull Packet<?> packet) {}
}