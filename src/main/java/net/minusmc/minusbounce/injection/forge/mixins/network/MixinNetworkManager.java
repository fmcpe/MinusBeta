/*
 * MinusBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/MinusMC/MinusBounce
 */
package net.minusmc.minusbounce.injection.forge.mixins.network;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import net.fmcpe.viaforge.ProtocolBase;
import net.fmcpe.viaforge.api.VFNetworkManager;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.LazyLoadBase;
import net.minecraft.util.Vec3;
import net.minusmc.minusbounce.MinusBounce;
import net.minusmc.minusbounce.event.EventState;
import net.minusmc.minusbounce.event.PacketEvent;
import net.minusmc.minusbounce.features.module.modules.client.HUD;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.net.InetAddress;

import static net.minusmc.minusbounce.utils.MinecraftInstance.*;

@Mixin(NetworkManager.class)
public class MixinNetworkManager implements VFNetworkManager {

    @Shadow
    private Channel channel;

    @Inject(method = "channelRead0*", at = @At("HEAD"), cancellable = true)
    private void read(ChannelHandlerContext context, Packet<?> packet, CallbackInfo callback) {
        final PacketEvent event = new PacketEvent(packet, EventState.RECEIVE);
        MinusBounce.eventManager.callEvent(event);

        if(event.isCancelled()) {
            callback.cancel();
        }
    }

    @Inject(method = "sendPacket(Lnet/minecraft/network/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void send(Packet<?> packet, CallbackInfo callback) {
        if (MinusBounce.handlePacket(packet)) {
            callback.cancel();
        }

        final PacketEvent event = new PacketEvent(packet, EventState.SEND);
        MinusBounce.eventManager.callEvent(event);

        if(event.isCancelled()) callback.cancel();

        if (packet instanceof C03PacketPlayer.C04PacketPlayerPosition || packet instanceof C03PacketPlayer.C05PacketPlayerLook || packet instanceof C03PacketPlayer.C06PacketPlayerPosLook) {
            final C03PacketPlayer p = (C03PacketPlayer)packet;

            rotIncrement = 3;
            if (!(packet instanceof C03PacketPlayer.C05PacketPlayerLook)) {
                lastServerPosition = serverPosition;
                serverPosition = new Vec3(p.x, p.y, p.z);
            }
        }
    }

    /**
     * show player head in tab bar
     * @author Liulihaocai, FDPClient
     */
    @Inject(method = "getIsencrypted", at = @At("HEAD"), cancellable = true)
    private void injectEncryption(CallbackInfoReturnable<Boolean> cir) {
        final HUD hud = MinusBounce.moduleManager.getModule(HUD.class);
        if(hud != null && hud.getTabHead().get()) {
            cir.setReturnValue(true);
        }
    }

    @Unique
    private ProtocolVersion viaForge$targetVersion;

    @Inject(method = "createNetworkManagerAndConnect", at = @At(value = "INVOKE", target = "Lio/netty/bootstrap/Bootstrap;group(Lio/netty/channel/EventLoopGroup;)Lio/netty/bootstrap/AbstractBootstrap;"), locals = LocalCapture.CAPTURE_FAILHARD)
    private static void trackSelfTarget(final InetAddress address, final int serverPort, final boolean useNativeTransport, final CallbackInfoReturnable<NetworkManager> cir, final NetworkManager networkmanager, final Class oclass, final LazyLoadBase lazyloadbase) {
        ((VFNetworkManager) networkmanager).viaForge$setTrackedVersion(ProtocolBase.getManager().targetVersion);
    }

    @Inject(method = "setCompressionTreshold", at = @At("RETURN"))
    public void reorderPipeline(final int p_setCompressionTreshold_1_, final CallbackInfo ci) {
        ProtocolBase.getManager().reorderCompression(channel);
    }

    @Override
    public ProtocolVersion viaForge$getTrackedVersion() {
        return viaForge$targetVersion;
    }

    @Override
    public void viaForge$setTrackedVersion(final ProtocolVersion version) {
        viaForge$targetVersion = version;
    }

}