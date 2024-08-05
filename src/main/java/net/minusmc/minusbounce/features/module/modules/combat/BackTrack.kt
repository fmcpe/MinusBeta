package net.minusmc.minusbounce.features.module.modules.combat

import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.Packet
import net.minecraft.network.play.client.C0FPacketConfirmTransaction
import net.minecraft.network.play.server.*
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
import net.minusmc.minusbounce.utils.Constants
import net.minusmc.minusbounce.utils.PacketUtils
import net.minusmc.minusbounce.utils.render.RenderUtils
import net.minusmc.minusbounce.utils.timer.MSTimer
import net.minusmc.minusbounce.value.BoolValue
import net.minusmc.minusbounce.value.FloatValue
import net.minusmc.minusbounce.value.IntegerValue
import org.lwjgl.opengl.GL11
import java.awt.Color


@ModuleInfo("BackTrack", "Back Track", "Let you attack in their previous position", ModuleCategory.COMBAT)
class BackTrack : Module() {
    private val delay = IntegerValue("Delay", 400, 0, 10000)
    private val hitRange = FloatValue("Range", 3F, 0F, 10F)
    private val velocity = BoolValue("Velocity", true)
    private val explosion = BoolValue("Explosion", true)
    private val time = BoolValue("TimeUpdate", true)
    private val keepAlive = BoolValue("KeepAlive", true)
    private val esp = BoolValue("ESP", true)

    val packets = mutableListOf<Packet<*>>()
    val timer = MSTimer()

    override fun onEnable() {
        mc.thePlayer ?: return
        mc.theWorld ?: return

        for (e in mc.theWorld.loadedEntityList){
            if(e is EntityLivingBase){
                e.realPosX = e.serverPosX.toDouble()
                e.realPosY = e.serverPosY.toDouble()
                e.realPosZ = e.serverPosZ.toDouble()
            }
        }

        packets.clear()
    }

    override fun onDisable() {
        if(packets.size > 0){
            flushPackets()
        }

        packets.clear()
    }

    @EventTarget(priority = 5)
    fun onPacket(event: PacketEvent) {
        mc.thePlayer ?: return
        mc.theWorld ?: return
        mc.netHandler ?: return

        if (MinusBounce.moduleManager[Scaffold::class.java]!!.state) {
            packets.clear()
            return
        }

        val packet = event.packet

        if (packet::class.java !in Constants.serverPacketClasses)
            return

        when (packet) {
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

            is S08PacketPlayerPosLook, is S40PacketDisconnect -> {
                flushPackets()
                return
            }

            is S13PacketDestroyEntities -> {
                for (id in packet.entityIDs){
                    if(id == (target?.entityId ?: id)){
                        flushPackets()
                        return
                    }
                }
            }
        }

        if (target == null) {
            flushPackets()
            return
        }

        addPacket(event)
    }

    val target: EntityLivingBase?
        get() = MinusBounce.moduleManager[KillAura::class.java]?.target

    @EventTarget
    fun onGameLoop(event: GameLoop) {
        mc.thePlayer ?: return
        mc.theWorld ?: return
        val target = this.target ?: return

        if (target.realPosX == 0.0 || target.realPosY == 0.0 || target.realPosZ == 0.0)
            return

        if (target.width == 0f || target.height == 0f)
            return

        val realX = target.realPosX / 32
        val realY = target.realPosY / 32
        val realZ = target.realPosZ / 32

        val realDistance = mc.thePlayer.getDistance(realX, realY, realZ)
        val targetDistance = mc.thePlayer.getDistance(target.posX, target.posY, target.posZ)

        if (targetDistance >= realDistance || realDistance > hitRange.get())
            flushPackets()
        else if (timer.hasTimePassed(delay.get())) {
            timer.reset()
            flushPackets()
        }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (!esp.get())
            return

        mc.thePlayer ?: return
        mc.theWorld ?: return
        val target = this.target ?: return

        if (target.realPosX == 0.0 || target.realPosY == 0.0 || target.realPosZ == 0.0)
            return

        if (target.width == 0f || target.height == 0f)
            return

        var render = true
        val realX = target.realPosX / 32
        val realY = target.realPosY / 32
        val realZ = target.realPosZ / 32

        val realDistance = mc.thePlayer.getDistance(realX, realY, realZ)
        val targetDistance = mc.thePlayer.getDistance(target.posX, target.posY, target.posZ)
        if (targetDistance >= realDistance || realDistance > hitRange.get() || timer.hasTimePassed(delay.get()))
            render = false

        if (target != mc.thePlayer && !target.isInvisible && render) {
            val color = Color(119, 130, 190).rgb
            val x = realX - mc.renderManager.renderPosX
            val y = realY - mc.renderManager.renderPosY
            val z = realZ - mc.renderManager.renderPosZ

            GlStateManager.pushMatrix()
            RenderUtils.start3D()
            RenderUtils.color(color)
            RenderUtils.renderHitbox(AxisAlignedBB(x - target.width / 2, y, z - target.width / 2, x + target.width / 2, y + target.height, z + target.width / 2), GL11.GL_QUADS)
            RenderUtils.color(color)
            RenderUtils.renderHitbox(AxisAlignedBB(x - target.width / 2, y, z - target.width / 2, x + target.width / 2, y + target.height, z + target.width / 2), GL11.GL_LINE_LOOP)
            RenderUtils.stop3D()
            GlStateManager.popMatrix()
        }
    }

    private fun flushPackets() {
        if (packets.isNotEmpty()) {
            synchronized(packets) {
                while (packets.size > 0) {
                    when (val packet = packets.removeFirst()) {
                        is S32PacketConfirmTransaction -> handleConfirmTransaction(packet)
                        is S00PacketKeepAlive -> mc.netHandler.handleKeepAlive(packet)
                        else -> PacketUtils.processPacket(packet)
                    }
                }
            }
        }
    }

    private fun handleConfirmTransaction(packetIn: S32PacketConfirmTransaction) {
        when (packetIn.windowId) {
            0 -> mc.thePlayer.inventoryContainer
            mc.thePlayer.openContainer.windowId -> mc.thePlayer.openContainer
            else -> null
        }?.takeIf { !packetIn.func_148888_e() }?.let {
            mc.netHandler.addToSendQueue(C0FPacketConfirmTransaction(packetIn.windowId, packetIn.actionNumber, true))
        }
    }

    private fun addPacket(event: PacketEvent) {
        val packet = event.packet
        var freeze = true

        when (packet) {
            is S19PacketEntityStatus -> freeze = packet.logicOpcode != 2.toByte() || mc.theWorld.getEntityByID(packet.entityId) !is EntityLivingBase
            is S03PacketTimeUpdate -> freeze = !time.get()
            is S00PacketKeepAlive -> freeze = !keepAlive.get()
            is S12PacketEntityVelocity -> freeze = !velocity.get()
            is S27PacketExplosion -> freeze = !explosion.get()
        }

        synchronized(packets) {
            if (packet::class.java !in Constants.serverOtherPacketClasses && freeze) {
                packets.add(packet)
                event.cancelEvent()
                event.stopRunEvent = true
            }
        }
    }
}
