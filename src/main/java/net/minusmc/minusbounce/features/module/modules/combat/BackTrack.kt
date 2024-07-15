package net.minusmc.minusbounce.features.module.modules.combat

import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.INetHandler
import net.minecraft.network.Packet
import net.minecraft.network.handshake.client.C00Handshake
import net.minecraft.network.login.client.C00PacketLoginStart
import net.minecraft.network.login.client.C01PacketEncryptionResponse
import net.minecraft.network.play.client.C01PacketChatMessage
import net.minecraft.network.play.server.S14PacketEntity
import net.minecraft.network.play.server.S18PacketEntityTeleport
import net.minecraft.network.status.client.C00PacketServerQuery
import net.minecraft.util.AxisAlignedBB
import net.minusmc.minusbounce.MinusBounce
import net.minusmc.minusbounce.event.EventTarget
import net.minusmc.minusbounce.event.GameLoop
import net.minusmc.minusbounce.event.PacketEvent
import net.minusmc.minusbounce.event.Render3DEvent
import net.minusmc.minusbounce.features.module.Module
import net.minusmc.minusbounce.features.module.ModuleCategory
import net.minusmc.minusbounce.features.module.ModuleInfo
import net.minusmc.minusbounce.features.module.modules.world.Scaffold
import net.minusmc.minusbounce.utils.render.ColorUtils
import net.minusmc.minusbounce.utils.render.RenderUtils
import net.minusmc.minusbounce.utils.timer.MSTimer
import net.minusmc.minusbounce.value.BoolValue
import net.minusmc.minusbounce.value.FloatValue
import org.lwjgl.opengl.GL11


@ModuleInfo("BackTrack", "Back Track", "Let you attack in their previous position", ModuleCategory.COMBAT)
class BackTrack : Module() {
    var packets = ArrayList<Packet<*>>()
    var timer = MSTimer()
    var delay = FloatValue("Delay", 400F, 0F, 1000F)
    var hitRange = FloatValue("Range", 3F, 0F, 10F)
    var esp = BoolValue("ESP", true)

    override fun onEnable() {
        packets.clear()
    }
    
    fun onPacket(e: PacketEvent) {
        val packet = e.packet
        if (
            mc.thePlayer == null
            || mc.theWorld == null
            || mc.netHandler.networkManager.netHandler == null
            || MinusBounce.moduleManager.getModule(Scaffold::class.java)?.state == true
        ) {
            packets.clear()
            return
        }

        when(packet) {
            is S14PacketEntity -> {
                val entity = mc.theWorld.getEntityByID(packet.entityId)

                if (entity is EntityLivingBase) {
                    entity.realPosX += packet.func_149062_c()
                    entity.realPosY += packet.func_149061_d()
                    entity.realPosZ += packet.func_149064_e()
                }
            }

            is S18PacketEntityTeleport -> {
                val entity = mc.theWorld.getEntityByID(packet.entityId)

                if (entity is EntityLivingBase) {
                    entity.realPosX = packet.x.toDouble()
                    entity.realPosY = packet.y.toDouble()
                    entity.realPosZ = packet.z.toDouble()
                }
            }
        }

        if (entity == null) {
            resetPackets(mc.netHandler.networkManager.netHandler)
        } else {
            addPackets(packet, e)
        }
    }

    private val entity: EntityLivingBase?
        get() = MinusBounce.moduleManager.getModule(KillAura::class.java)?.target

    @EventTarget
    fun onGameLoop(e: GameLoop?) {
        if (
            entity != null &&
            entity!!.entityBoundingBox != null &&
            mc.thePlayer != null &&
            mc.theWorld != null &&
            entity!!.realPosX != 0.0 &&
            entity!!.realPosY != 0.0 &&
            entity!!.realPosZ != 0.0 &&
            entity!!.width != 0f &&
            entity!!.height != 0f
        ) {
            val realX = entity!!.realPosX / 32
            val realY = entity!!.realPosY / 32
            val realZ = entity!!.realPosZ / 32

            if (mc.thePlayer.getDistance(entity!!.posX, entity!!.posY, entity!!.posZ) > 3) {
                if (mc.thePlayer.getDistance(
                        entity!!.posX,
                        entity!!.posY,
                        entity!!.posZ
                    ) >= mc.thePlayer.getDistance(
                        realX,
                        realY, realZ
                    )
                ) {
                    resetPackets(mc.netHandler.networkManager.netHandler)
                }
            }

            if (mc.thePlayer.getDistance(realX, realY, realZ) > hitRange.get()
                || timer.hasTimeElapsed(delay.get().toDouble(), true)
            ) {
                resetPackets(mc.netHandler.networkManager.netHandler)
            }
        }
    }

    @EventTarget
    fun onRender3D(e: Render3DEvent) {
        if (
            entity != null &&
            entity!!.entityBoundingBox != null &&
            mc.thePlayer != null &&
            mc.theWorld != null &&
            entity!!.realPosX != 0.0 &&
            entity!!.realPosY != 0.0 &&
            entity!!.realPosZ != 0.0 &&
            entity!!.width != 0f &&
            entity!!.height != 0f &&
            esp.get()
        ) {
            var render = true
            val realX = entity!!.realPosX / 32
            val realY = entity!!.realPosY / 32
            val realZ = entity!!.realPosZ / 32

            if (mc.thePlayer.getDistance(entity!!.posX, entity!!.posY, entity!!.posZ) >= mc.thePlayer.getDistance(
                    realX,
                    realY, realZ
                )
            ) {
                render = false
            }

            if (mc.thePlayer.getDistance(realX, realY, realZ) > hitRange.get()
                || timer.hasTimeElapsed(delay.get().toDouble(), false)
            ) {
                render = false
            }

            if (
                entity != null &&
                entity != mc.thePlayer &&
                !entity!!.isInvisible &&
                entity!!.width != 0f &&
                entity!!.height != 0f &&
                render
            ) {
                val color = ColorUtils.getColor(210.0F, 0.7F, 0.75F)
                val x = entity!!.realPosX / 32.0 - mc.renderManager.renderPosX
                val y = entity!!.realPosY / 32.0 - mc.renderManager.renderPosY
                val z = entity!!.realPosZ / 32.0 - mc.renderManager.renderPosZ
                GlStateManager.pushMatrix()
                RenderUtils.start3D()
                RenderUtils.color(color)
                RenderUtils.renderHitbox(
                    AxisAlignedBB(
                        x - entity!!.width / 2,
                        y,
                        z - entity!!.width / 2,
                        x + entity!!.width / 2,
                        y + entity!!.height,
                        z + entity!!.width / 2
                    ), GL11.GL_QUADS
                )
                RenderUtils.color(color)
                RenderUtils.renderHitbox(
                    AxisAlignedBB(
                        x - entity!!.width / 2,
                        y,
                        z - entity!!.width / 2,
                        x + entity!!.width / 2,
                        y + entity!!.height,
                        z + entity!!.width / 2
                    ), GL11.GL_LINE_LOOP
                )
                RenderUtils.stop3D()
                GlStateManager.popMatrix()
            }
        }
    }

    private fun resetPackets(netHandler: INetHandler) {
        if (packets.size > 0) {
            synchronized(packets) {
                while (packets.size != 0) {
                    try {
                        (packets[0] as Packet<INetHandler>).processPacket(netHandler)
                    } catch (_: Exception) { }
                    packets.remove(packets[0])
                }
            }
        }
    }

    private fun addPackets(packet: Packet<*>, event: PacketEvent) {
        synchronized(packets) {
            if (this.blockPacket(packet)) {
                packets.add(packet)
                event.cancelEvent()
            }
        }
    }

    private fun blockPacket(packet: Packet<*>): Boolean = when(packet) {
        is C00Handshake, is C00PacketLoginStart, is C00PacketServerQuery,
        is C01PacketEncryptionResponse, is C01PacketChatMessage -> false
        else -> true
    }
}