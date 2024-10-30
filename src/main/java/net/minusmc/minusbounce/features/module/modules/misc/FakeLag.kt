package net.minusmc.minusbounce.features.module.modules.misc

import net.minecraft.network.Packet
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.Vec3
import net.minusmc.minusbounce.event.EventState
import net.minusmc.minusbounce.event.EventTarget
import net.minusmc.minusbounce.event.PacketEvent
import net.minusmc.minusbounce.event.Render3DEvent
import net.minusmc.minusbounce.features.module.Module
import net.minusmc.minusbounce.features.module.ModuleCategory
import net.minusmc.minusbounce.features.module.ModuleInfo
import net.minusmc.minusbounce.utils.PacketUtils
import net.minusmc.minusbounce.utils.render.RenderUtils.glColor
import net.minusmc.minusbounce.value.BoolValue
import net.minusmc.minusbounce.value.IntegerValue
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import java.util.concurrent.ConcurrentLinkedQueue

@ModuleInfo(name = "FakeLag", spacedName = "Fake Lag", description = "Abuse latency.", category = ModuleCategory.MISC)
class FakeLag : Module() {
    private val inboundPackets = ConcurrentLinkedQueue<TimedPacket>()
    private val outboundPackets = ConcurrentLinkedQueue<TimedPacket>()
    private val positions = LinkedHashMap<Vec3, Long>()

    private val delay = IntegerValue("Delay", 550, 0, 1000)
    private val line = BoolValue("Line", true)
    private val red = IntegerValue("R", 0, 0, 255) { line.get() }
    private val green = IntegerValue("G", 255, 0, 255) { line.get() }
    private val blue = IntegerValue("B", 0, 0, 255) { line.get() }

    override fun onEnable() {
        if (mc.thePlayer == null || mc.isIntegratedServerRunning) {
            toggle()
            return
        }
        clearPackets()
    }

    override fun onDisable() {
        mc.thePlayer ?: return
        blinkPackets(inboundPackets)
        blinkPackets(outboundPackets)
    }

    private fun blinkPackets(queue: ConcurrentLinkedQueue<TimedPacket>) {
        queue.forEach {
            PacketUtils.sendPacketNoEvent(it.packet)
        }
        queue.clear()
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val player = mc.thePlayer ?: return
        if (event.packet is S08PacketPlayerPosLook || player.isDead) return

        if (event.eventType == EventState.RECEIVE) {
            inboundPackets.add(TimedPacket(event.packet))
            event.cancelEvent()
            processPacketQueue(inboundPackets, delay.get())
        } else if (event.eventType == EventState.SEND) {
            outboundPackets.add(TimedPacket(event.packet))
            event.cancelEvent()
            processPacketQueue(outboundPackets, delay.get())
        }
    }

    private fun processPacketQueue(queue: ConcurrentLinkedQueue<TimedPacket>, delay: Int) {
        while (queue.isNotEmpty()) {
            if (queue.peek().time + delay <= System.currentTimeMillis()) {
                val packet = queue.poll().packet
                if (packet is C03PacketPlayer && packet.isMoving) {
                    positions[Vec3(packet.x, packet.y, packet.z)] = System.currentTimeMillis()
                }
                PacketUtils.sendPacketNoEvent(packet)
            } else {
                break
            }
        }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (!line.get()) return

        val color =
            Color(red.get(), green.get(), blue.get())

        synchronized(positions.keys) {
            glPushMatrix()
            glDisable(GL_TEXTURE_2D)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            glEnable(GL_LINE_SMOOTH)
            glEnable(GL_BLEND)
            glDisable(GL_DEPTH_TEST)
            mc.entityRenderer.disableLightmap()
            glBegin(GL_LINE_STRIP)
            glColor(color)

            val renderPosX = mc.renderManager.viewerPosX
            val renderPosY = mc.renderManager.viewerPosY
            val renderPosZ = mc.renderManager.viewerPosZ

            for (pos in positions.keys) {
                glVertex3d(pos.xCoord - renderPosX, pos.yCoord - renderPosY, pos.zCoord - renderPosZ)
            }

            glColor4d(1.0, 1.0, 1.0, 1.0)
            glEnd()
            glEnable(GL_DEPTH_TEST)
            glDisable(GL_LINE_SMOOTH)
            glDisable(GL_BLEND)
            glEnable(GL_TEXTURE_2D)
            glPopMatrix()
        }
    }

    override val tag
        get() = inboundPackets.size.toString()

    private fun clearPackets() {
        inboundPackets.clear()
        outboundPackets.clear()
        positions.clear()
    }

    private data class TimedPacket(val packet: Packet<*>, val time: Long = System.currentTimeMillis())
}
