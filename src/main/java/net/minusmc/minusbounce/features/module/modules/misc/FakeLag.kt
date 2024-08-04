/*
 * MinusBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/MinusMC/MinusBounce
 */
package net.minusmc.minusbounce.features.module.modules.misc

import net.minecraft.network.Packet
import net.minecraft.network.handshake.client.C00Handshake
import net.minecraft.network.login.client.C00PacketLoginStart
import net.minecraft.network.login.client.C01PacketEncryptionResponse
import net.minecraft.network.play.client.C01PacketChatMessage
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.status.client.C00PacketServerQuery
import net.minecraft.util.Vec3
import net.minusmc.minusbounce.event.*
import net.minusmc.minusbounce.features.module.Module
import net.minusmc.minusbounce.features.module.ModuleCategory
import net.minusmc.minusbounce.features.module.ModuleInfo
import net.minusmc.minusbounce.features.module.modules.player.Blink.drawBox
import net.minusmc.minusbounce.utils.PacketUtils.sendPacketNoEvent
import net.minusmc.minusbounce.utils.timer.MSTimer
import net.minusmc.minusbounce.value.IntegerValue
import java.util.concurrent.ConcurrentLinkedQueue


@ModuleInfo(name = "FakeLag", spacedName = "Fake Lag", description = "Abuse latency.", category = ModuleCategory.MISC)
class FakeLag : Module() {

    private val latency = IntegerValue("Latency", 200, 25, 1000, "ms")

    private var vec3: Vec3? = null
    private val packetQueue = ConcurrentLinkedQueue<TimedPacket>()

    override fun onEnable() {
        packetQueue.clear()
        vec3 = null
    }

    @EventTarget
    fun onRender(event: Render3DEvent) {
        if (vec3 != null) {
            if (mc.gameSettings.thirdPersonView == 0) return

            drawBox(vec3 ?: return)
        }
    }

    @EventTarget
    fun onTick(e: TickEvent){
        mc.thePlayer ?: return
        mc.theWorld ?: return

        sendPacket(true)
    }

    @EventTarget(priority = 5)
    fun onSendPacket(e: PacketEvent) {
        if(e.eventType == EventState.SEND){
            mc.thePlayer ?: return
            mc.theWorld ?: return

            val packet = e.packet
            if (packet is C00Handshake
                || packet is C00PacketLoginStart
                || packet is C00PacketServerQuery
                || packet is C01PacketEncryptionResponse
                || packet is C01PacketChatMessage
                || packet is C02PacketUseEntity
            ) {
                sendPacket(false)
                return
            }

            synchronized(packetQueue){
                if(!e.isCancelled) {
                    val time = MSTimer()
                    time.reset()

                    packetQueue.add(TimedPacket(e.packet, time))
                }
                e.cancelEvent()
            }
        }
    }

    fun sendPacket(delay: Boolean) {
        if(packetQueue.isEmpty())
            return

        synchronized(packetQueue) {
            while (packetQueue.isNotEmpty()) {
                if (!delay || packetQueue.element().timer.hasTimePassed(latency.get())) {
                    val packet = packetQueue.remove().packet

                    if(packet is C03PacketPlayer && packet.isMoving) {
                        vec3 = Vec3(packet.x, packet.y, packet.z)
                    }

                    sendPacketNoEvent(packet)
                } else {
                    break
                }
            }
        }
    }
}

data class TimedPacket(val packet: Packet<*>, val timer: MSTimer)
