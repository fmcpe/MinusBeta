package net.minusmc.minusbounce.features.module.modules.combat

import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.Packet
import net.minecraft.network.play.client.C0FPacketConfirmTransaction
import net.minecraft.network.play.server.*
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.Vec3
import net.minusmc.minusbounce.MinusBounce
import net.minusmc.minusbounce.event.*
import net.minusmc.minusbounce.features.module.Module
import net.minusmc.minusbounce.features.module.ModuleCategory
import net.minusmc.minusbounce.features.module.ModuleInfo
import net.minusmc.minusbounce.features.module.modules.world.Scaffold
import net.minusmc.minusbounce.utils.Constants
import net.minusmc.minusbounce.utils.PacketUtils
import net.minusmc.minusbounce.utils.render.ColorUtils
import net.minusmc.minusbounce.utils.render.RenderUtils
import net.minusmc.minusbounce.utils.timer.MSTimer
import net.minusmc.minusbounce.value.BoolValue
import net.minusmc.minusbounce.value.FloatValue
import net.minusmc.minusbounce.value.IntegerValue
import org.lwjgl.opengl.GL11
import kotlin.math.abs


@ModuleInfo("BackTrack", "Back Track", "Let you attack in their previous position", ModuleCategory.COMBAT)
class BackTrack : Module() {
    private val delay = IntegerValue("Delay", 400, 0, 1000)
    private val hitRange = FloatValue("Range", 3F, 0F, 10F)
    val esp = BoolValue("ESP", true)

    val packets = mutableListOf<Packet<*>>()
    val timer = MSTimer()
    var lastVelocity: Vec3? = null

    override fun onEnable() {
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

            is S12PacketEntityVelocity -> {
                if (packet.entityID == mc.thePlayer.entityId) {
                    if (packets.isNotEmpty()) {
                        event.cancelEvent()
                        lastVelocity = Vec3(
                            packet.getMotionX() / 8000.0,
                            packet.getMotionY() / 8000.0,
                            packet.getMotionZ() / 8000.0
                        )
                    }
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

        if (targetDistance >= realDistance || realDistance > hitRange.get() || mc.thePlayer.hurtTime > 3)
            flushPackets()
        else if (timer.hasTimePassed(delay.get())) {
            timer.reset()
            flushPackets()
        }
    }

    @EventTarget
    fun onWorld(event: WorldEvent){
        flushPackets()
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

        if (targetDistance >= realDistance || realDistance > hitRange.get() || timer.hasTimePassed(delay.get()) || mc.thePlayer.hurtTime > 3)
            render = false

        if (target != mc.thePlayer && !target.isInvisible && render) {
            val color = ColorUtils.getColor(210.0F, 0.7F, 0.75F)
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

    fun flushPackets() {
        if (lastVelocity != null) {
            mc.thePlayer.motionX = lastVelocity!!.xCoord
            mc.thePlayer.motionY = lastVelocity!!.yCoord
            mc.thePlayer.motionZ = lastVelocity!!.zCoord
            lastVelocity = null
        }

        if (packets.isNotEmpty()) {
            synchronized(packets) {
                while (packets.size > 0) {
                    when (val packet = packets.removeFirst()) {
                        is S14PacketEntity -> handleEntityMovement(packet)
                        is S18PacketEntityTeleport -> handleEntityTeleport(packet)
                        is S32PacketConfirmTransaction -> handleConfirmTransaction(packet)
                        is S00PacketKeepAlive -> mc.netHandler.handleKeepAlive(packet)
                        else -> PacketUtils.processPacket(packet)
                    }
                }
            }
        }
    }

    private fun handleEntityMovement(packetIn: S14PacketEntity) {
        packetIn.getEntity(mc.netHandler.clientWorldController)?.apply {
            serverPosX += packetIn.posX
            serverPosY += packetIn.posY
            serverPosZ += packetIn.posZ
            val d0 = serverPosX / 32.0
            val d1 = serverPosY / 32.0
            val d2 = serverPosZ / 32.0
            val f = if (packetIn.func_149060_h()) (packetIn.yaw * 360) / 256.0f else rotationYaw
            val f1 = if (packetIn.func_149060_h()) (packetIn.pitch * 360) / 256.0f else rotationPitch
            setPositionAndRotation2(d0, d1, d2, f, f1, 3, false)
            onGround = packetIn.onGround
        }
    }

    fun handleEntityTeleport(packetIn: S18PacketEntityTeleport) {
        mc.netHandler.clientWorldController.getEntityByID(packetIn.entityId)?.apply {
            serverPosX = packetIn.x
            serverPosY = packetIn.y
            serverPosZ = packetIn.z
            val d0 = serverPosX / 32.0
            val d1 = serverPosY / 32.0
            val d2 = serverPosZ / 32.0
            val f = (packetIn.yaw * 360) / 256.0f
            val f1 = (packetIn.pitch * 360) / 256.0f

            val isCloseEnough = abs(posX - d0) < 0.03125 && abs(posY - d1) < 0.015625 && abs(posZ - d2) < 0.03125

            setPositionAndRotation2(if (isCloseEnough) posX else d0, d1, d2, f, f1, 3, true)
            onGround = packetIn.onGround
        }
    }

    fun handleConfirmTransaction(packetIn: S32PacketConfirmTransaction) {
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

        synchronized(packets) {
            if (packet::class.java !in Constants.serverOtherPacketClasses) {
                packets.add(packet)
                event.cancelEvent()
                event.stopRunEvent = true
            }
        }
    }
}