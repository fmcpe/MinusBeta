/*
 * MinusBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/MinusMC/MinusBounce
 */
package net.minusmc.minusbounce.features.module.modules.misc

import net.minecraft.network.Packet
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S02PacketChat
import net.minecraft.network.play.server.S03PacketTimeUpdate
import net.minecraft.network.play.server.S19PacketEntityStatus
import net.minecraft.util.Vec3
import net.minusmc.minusbounce.event.*
import net.minusmc.minusbounce.features.module.Module
import net.minusmc.minusbounce.features.module.ModuleCategory
import net.minusmc.minusbounce.features.module.ModuleInfo
import net.minusmc.minusbounce.features.module.modules.player.Blink.drawBox
import net.minusmc.minusbounce.utils.PacketUtils
import net.minusmc.minusbounce.value.IntegerValue
import java.util.concurrent.ConcurrentLinkedQueue


@ModuleInfo(name = "FakeLag", spacedName = "Fake Lag", description = "Abuse latency.", category = ModuleCategory.MISC)
class FakeLag : Module() {
    private var serverPosition: Vec3? = null
    private val inboundPackets = ConcurrentLinkedQueue<TimedPacket>()
    private val outboundPackets = ConcurrentLinkedQueue<TimedPacket>()

    private val inboundDelay = IntegerValue("Inbound", 142, 0, 1000)
    private val outboundDelay = IntegerValue("Outbound", 250, 0, 1000)
    private val chance = IntegerValue("Chance", 100, 0, 100)

    override val tag: String
        get() = ("- [" + inboundDelay.get() + " | " + outboundDelay.get() + " ms]")

    override fun onEnable() {
        if (mc.thePlayer == null || mc.isIntegratedServerRunning) {
            toggle()
            return
        }

        if ((mc.thePlayer != null && !inboundPackets.isEmpty()) || (mc.thePlayer != null && !outboundPackets.isEmpty())) {
            clearPackets()
        }
    }

    override fun onDisable() {
        mc.thePlayer ?: return

        if (mc.thePlayer != null && !inboundPackets.isEmpty()) {
            inboundPackets.forEach{
                PacketUtils.processPacket(it.packet)
            }
            inboundPackets.clear()
        } else if (mc.thePlayer != null && !outboundPackets.isEmpty()) {
            outboundPackets.forEach{
                PacketUtils.sendPacketNoEvent(it.packet)
            }
            outboundPackets.clear()
        }
    }

    @EventTarget
    fun onRender(e: Render3DEvent){
        mc.thePlayer ?: return
        mc.theWorld ?: return

        if (mc.gameSettings.thirdPersonView != 0) {
            drawBox(serverPosition ?: return)
        }
    }

    @EventTarget
    fun onPacket(e: PacketEvent) {
        if (mc.thePlayer == null || mc.thePlayer.isDead || e.packet is S03PacketTimeUpdate || e.packet is S19PacketEntityStatus || e.packet is S02PacketChat) return

        if (chance.get() != 100) {
            if (Math.random() >= chance.get() / 100) {
                return
            }
        }

        if (e.eventType == EventState.RECEIVE) {
            inboundPackets.add(TimedPacket(e.packet))
            e.isCancelled = true

            while (inboundPackets.isNotEmpty()) {
                if (inboundPackets.peek().time + inboundDelay.get() <= System.currentTimeMillis()) {
                    PacketUtils.processPacket(inboundPackets.poll().packet)
                } else {
                    break
                }
            }
        }

        if (e.eventType == EventState.SEND) {
            outboundPackets.add(TimedPacket(e.packet))
            e.isCancelled = true

            while (outboundPackets.isNotEmpty()) {
                if (outboundPackets.peek().time + outboundDelay.get() <= System.currentTimeMillis()) {
                    val packet = outboundPackets.poll().packet
                    if(packet is C03PacketPlayer && packet.isMoving) {
                        serverPosition = Vec3(packet.x, packet.y, packet.z)
                    }
    
                    PacketUtils.sendPacketNoEvent(packet)
                } else {
                    break
                }
            }
        }
    }

    private fun clearPackets() {
        outboundPackets.clear()
        inboundPackets.clear()
    }
}

data class TimedPacket(val packet: Packet<*>, val time: Long = System.currentTimeMillis())