package net.minusmc.minusbounce.features.module.modules.movement.flys.other

import net.minecraft.network.Packet
import net.minecraft.network.play.INetHandlerPlayServer
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S32PacketConfirmTransaction
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.Vec3
import net.minusmc.minusbounce.event.EventState
import net.minusmc.minusbounce.event.EventTarget
import net.minusmc.minusbounce.event.PacketEvent
import net.minusmc.minusbounce.event.UpdateEvent
import net.minusmc.minusbounce.features.module.Module
import net.minusmc.minusbounce.features.module.ModuleCategory
import net.minusmc.minusbounce.features.module.ModuleInfo
import net.minusmc.minusbounce.utils.PacketUtils
import net.minusmc.minusbounce.utils.PacketUtils.sendPacketNoEvent
import net.minusmc.minusbounce.utils.timer.MSTimer
import kotlin.math.sqrt

@ModuleInfo(name = "IntaveFly", description = "Abuses velocity setback for flying.", category = ModuleCategory.MOVEMENT)
class SetBackFly : Module() {

    private val blinkedPackets = mutableListOf<Packet<*>>()
    private val pulseDelay = 300L // Pulse delay in ms
    private var blinkStartPos: Vec3? = null
    private var flyActive = false
    private val pulseTimer = MSTimer()
    private val groundTimer = MSTimer()
    private var delay = false
    private val packets = mutableListOf<Packet<INetHandlerPlayServer>>()

    override fun onEnable() {
        reset()
    }

    override fun onDisable() {
        reset()
        mc.timer.timerSpeed = 1F

        if (mc.thePlayer != null)
            packets.map{it}.forEach { PacketUtils.receivePacketNoEvent(it)}

        packets.clear()
        delay = false
    }

    private fun reset() {
        synchronized(blinkedPackets) {
            for (packet in blinkedPackets) {
                sendPacketNoEvent(packet)
            }
        }
        blinkedPackets.clear()
        blinkStartPos = null
    }


    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (event.eventType != EventState.SEND || mc.thePlayer == null) return

        val packet = event.packet
        if (packet is C03PacketPlayer) {
            blinkedPackets.add(packet)
            event.cancelEvent()
        }
        if (mc.thePlayer == null || mc.thePlayer.ticksExisted < 20) {
            packets.clear()
            return
        }

        if (mc.thePlayer.capabilities.isFlying || mc.thePlayer.capabilities.allowFlying)
            if (!delay && packet is S08PacketPlayerPosLook)
                delay = true

        if (delay && packet is S32PacketConfirmTransaction) {
            packets.add(packet as Packet<INetHandlerPlayServer>)
            event.cancelEvent()
        }
    }


    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (mc.thePlayer == null || mc.theWorld == null) return

        handleVanillaKickBypass()
        mc.thePlayer.capabilities.isFlying = true

        if (blinkStartPos == null) {
            blinkStartPos = Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ)
        }

        val currentPos = Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ)
        val distance = calculateDistance(blinkStartPos!!, currentPos)

        // Logic phát xung Blink mỗi 300ms
        if (pulseTimer.hasTimePassed(pulseDelay)) {
            reset() // Gửi tất cả gói tin Blink đã lưu
            blinkStartPos = currentPos // Cập nhật vị trí bắt đầu Blink mới
            pulseTimer.reset() // Reset lại bộ đếm thời gian cho chu kỳ tiếp theo
        }

        if (distance >= 15) {
            flyBackward(15.0)
            flyDownward(8.0)

            state = false
        }
    }



    private fun handleVanillaKickBypass() {
        if (!groundTimer.hasTimePassed(1000)) return

        val ground = calculateGround()

        run {
            var posY = mc.thePlayer.posY
            while (posY > ground) {
                mc.netHandler.addToSendQueue(
                    C03PacketPlayer.C04PacketPlayerPosition(
                        mc.thePlayer.posX,
                        posY,
                        mc.thePlayer.posZ,
                        true
                    )
                )
                if (posY - 8.0 < ground) break
                posY -= 8.0
            }
        }

        mc.netHandler.addToSendQueue(
            C03PacketPlayer.C04PacketPlayerPosition(
                mc.thePlayer.posX,
                ground,
                mc.thePlayer.posZ,
                true
            )
        )

        var posY = ground
        while (posY < mc.thePlayer.posY) {
            mc.netHandler.addToSendQueue(
                C03PacketPlayer.C04PacketPlayerPosition(
                    mc.thePlayer.posX,
                    posY,
                    mc.thePlayer.posZ,
                    true
                )
            )
            if (posY + 8.0 > mc.thePlayer.posY) break
            posY += 8.0
        }
        mc.netHandler.addToSendQueue(
            C03PacketPlayer.C04PacketPlayerPosition(
                mc.thePlayer.posX,
                mc.thePlayer.posY,
                mc.thePlayer.posZ,
                true
            )
        )

        groundTimer.reset()
    }

    private fun calculateGround(): Double {
        val playerBoundingBox = mc.thePlayer.entityBoundingBox
        var blockHeight = 1.0
        var ground = mc.thePlayer.posY
        while (ground > 0.0) {
            val customBox = AxisAlignedBB(
                playerBoundingBox.minX, ground,
                playerBoundingBox.minZ,
                playerBoundingBox.maxX, ground + blockHeight,
                playerBoundingBox.maxZ
            )
            if (mc.theWorld.checkBlockCollision(customBox)) {
                if (blockHeight <= 0.05) return ground + blockHeight
                ground += blockHeight
                blockHeight = 0.05
            }
            ground -= blockHeight
        }
        return 0.0
    }

    private fun flyBackward(distance: Double) {
        val yawRad = Math.toRadians(mc.thePlayer.rotationYaw.toDouble())
        val dx = -Math.sin(yawRad) * distance
        val dz = Math.cos(yawRad) * distance

        mc.netHandler.addToSendQueue(
            C03PacketPlayer.C04PacketPlayerPosition(
                mc.thePlayer.posX + dx,
                mc.thePlayer.posY,
                mc.thePlayer.posZ + dz,
                true
            )
        )
    }

    private fun flyDownward(distance: Double) {
        mc.netHandler.addToSendQueue(
            C03PacketPlayer.C04PacketPlayerPosition(
                mc.thePlayer.posX,
                mc.thePlayer.posY - distance,
                mc.thePlayer.posZ,
                true
            )
        )
    }

    private fun calculateDistance(pos1: Vec3, pos2: Vec3): Double {
        val dx = pos2.xCoord - pos1.xCoord
        val dy = pos2.yCoord - pos1.yCoord
        val dz = pos2.zCoord - pos1.zCoord
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
}
