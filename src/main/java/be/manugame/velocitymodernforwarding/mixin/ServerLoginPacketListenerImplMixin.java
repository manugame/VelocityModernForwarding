package be.manugame.velocitymodernforwarding.mixin;

import be.manugame.velocitymodernforwarding.config.VelocityModernForwardingConfig;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.login.ClientboundHelloPacket;
import net.minecraft.network.protocol.login.ServerboundCustomQueryPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(ServerLoginPacketListenerImpl.class)
public abstract class ServerLoginPacketListenerImplMixin {

    @Shadow @Nullable public GameProfile gameProfile;
    @Shadow @Final private MinecraftServer server;

    @Shadow
    ServerLoginPacketListenerImpl.State state;
    @Shadow @Final public Connection connection;
    @Shadow @Final private byte[] nonce;

    @Shadow public abstract void disconnect(Component p_10054_);

    @Shadow @Final
    static Logger LOGGER;
    private int velocityLoginMessageId = -1; // Paper - Velocity support



    private static boolean isValidUsername(String p_203793_) {
        return p_203793_.chars().filter((p_203791_) -> {
            return p_203791_ <= 32 || p_203791_ >= 127;
        }).findAny().isEmpty();
    }

    @Inject(method = "handleHello", at = @At("HEAD"), cancellable = true)
    public void handleHello(ServerboundHelloPacket p_10047_, CallbackInfo ci) {
        Validate.validState(this.state == ServerLoginPacketListenerImpl.State.HELLO, "Unexpected hello packet");
        this.gameProfile = p_10047_.getGameProfile();
        Validate.validState(isValidUsername(this.gameProfile.getName()), "Invalid characters in username");
        if (this.server.usesAuthentication() && !this.connection.isMemoryConnection()) {
            this.state = ServerLoginPacketListenerImpl.State.KEY;
            this.connection.send(new ClientboundHelloPacket("", this.server.getKeyPair().getPublic().getEncoded(), this.nonce));
        } else {
            // Paper start - Velocity support
            if (VelocityModernForwardingConfig.isEnabled()) {
                this.velocityLoginMessageId = java.util.concurrent.ThreadLocalRandom.current().nextInt();
                System.out.println(velocityLoginMessageId);
                net.minecraft.network.FriendlyByteBuf buf = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
                buf.writeByte(com.destroystokyo.paper.proxy.VelocityProxy.MAX_SUPPORTED_FORWARDING_VERSION);
                net.minecraft.network.protocol.login.ClientboundCustomQueryPacket packet1 = new net.minecraft.network.protocol.login.ClientboundCustomQueryPacket(this.velocityLoginMessageId, com.destroystokyo.paper.proxy.VelocityProxy.PLAYER_INFO_CHANNEL, buf);
                this.connection.send(packet1);
                return;
            }
            // Paper end
            this.state = ServerLoginPacketListenerImpl.State.NEGOTIATING;
        }

        fireEvents();

        ci.cancel();
    }

    /**
     * @author test
     * @reason test
     */
    @Inject(method = "handleCustomQueryPacket", at = @At("HEAD"), cancellable = true)
    public void handleCustomQueryPacket(ServerboundCustomQueryPacket packet, CallbackInfo ci) {
        // Paper start - Velocity support
        if (VelocityModernForwardingConfig.isEnabled() && packet.getTransactionId() == this.velocityLoginMessageId) {
            net.minecraft.network.FriendlyByteBuf buf = packet.getData();
            if (buf == null) {
                this.disconnect(new TextComponent("This server requires you to connect with Velocity."));
                return;
            }

            if (!com.destroystokyo.paper.proxy.VelocityProxy.checkIntegrity(buf)) {
                this.disconnect(new TextComponent("Unable to verify player details"));
                return;
            }

            int version = buf.readVarInt();
            if (version > com.destroystokyo.paper.proxy.VelocityProxy.MAX_SUPPORTED_FORWARDING_VERSION) {
                throw new IllegalStateException("Unsupported forwarding version " + version + ", wanted upto " + com.destroystokyo.paper.proxy.VelocityProxy.MAX_SUPPORTED_FORWARDING_VERSION);
            }

            java.net.SocketAddress listening = this.connection.getRemoteAddress();
            int port = 0;
            if (listening instanceof java.net.InetSocketAddress) {
                port = ((java.net.InetSocketAddress) listening).getPort();
            }
            this.connection.address = new java.net.InetSocketAddress(com.destroystokyo.paper.proxy.VelocityProxy.readAddress(buf), port);

            this.gameProfile = com.destroystokyo.paper.proxy.VelocityProxy.createProfile(buf);

            //TODO Update handling for lazy sessions, might not even have to do anything?

            fireEvents();

            ci.cancel();
        }
        // Paper end
    }

    public void fireEvents() {
        // Paper start - Velocity support
        if (this.velocityLoginMessageId == -1 && VelocityModernForwardingConfig.isEnabled()) {
            disconnect(new TextComponent("This server requires you to connect with Velocity."));
            return;
        }

        this.LOGGER.info("UUID of player {} is {}", this.gameProfile.getName(), this.gameProfile.getId());
        this.state = ServerLoginPacketListenerImpl.State.NEGOTIATING;
        // Paper end
    }

}
