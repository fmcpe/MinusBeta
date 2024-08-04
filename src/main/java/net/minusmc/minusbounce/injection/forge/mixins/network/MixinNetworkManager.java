/*
 * MinusBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/MinusMC/MinusBounce
 */
package net.minusmc.minusbounce.injection.forge.mixins.network;

import io.netty.channel.ChannelHandlerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.Vec3;
import net.minusmc.minusbounce.MinusBounce;
import net.minusmc.minusbounce.event.EventState;
import net.minusmc.minusbounce.event.PacketEvent;
import net.minusmc.minusbounce.features.module.modules.client.HUD;
import net.minusmc.minusbounce.utils.MinecraftInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NetworkManager.class)
public class MixinNetworkManager {

    @Inject(method = "channelRead0", at = @At("HEAD"), cancellable = true)
    private void read(ChannelHandlerContext context, Packet<?> packet, CallbackInfo callback) {
        final PacketEvent event = new PacketEvent(packet, EventState.RECEIVE);
        MinusBounce.eventManager.callEvent(event);

        if(event.isCancelled()) {
            callback.cancel();
        }
    }

    @Inject(method = "sendPacket(Lnet/minecraft/network/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void send(Packet<?> packet, CallbackInfo callback) {
        final PacketEvent event = new PacketEvent(packet, EventState.SEND);
        MinusBounce.eventManager.callEvent(event);

        if(event.isCancelled()) callback.cancel();

        if ((packet instanceof C03PacketPlayer.C04PacketPlayerPosition || packet instanceof C03PacketPlayer.C05PacketPlayerLook || packet instanceof C03PacketPlayer.C06PacketPlayerPosLook) && Minecraft.getMinecraft().theWorld != null && Minecraft.getMinecraft().thePlayer != null) {
            final C03PacketPlayer c03 = (C03PacketPlayer)packet;
            if (!(packet instanceof C03PacketPlayer.C05PacketPlayerLook)) {
                MinecraftInstance.Companion.setServerPosition(new Vec3(c03.getPositionX(), c03.getPositionY(), c03.getPositionZ()));
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

}