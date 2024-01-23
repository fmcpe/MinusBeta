package net.minusmc.minusbounce.features.module.modules.combat.velocitys.normal

import net.minusmc.minusbounce.event.*
import net.minusmc.minusbounce.utils.*
import net.minusmc.minusbounce.utils.extensions.*
import net.minecraft.util.*
import net.minusmc.minusbounce.features.module.modules.combat.velocitys.VelocityMode
import net.minecraft.network.play.server.S12PacketEntityVelocity
import kotlin.math.*

class LegitVelocity : VelocityMode("Legit") {
    private var yaw: Float? = null

    override fun onEnable(){
        yaw = null
    }

    override fun onStrafe(event: StrafeEvent){
        if(mc.thePlayer.hurtTime < 8) {
            yaw = null
            return
        }

        event.yaw = (yaw ?: return)
        event.correction = false
    }

    override fun onJump(event: JumpEvent){
        yaw ?: return

        event.yaw = yaw!!
        event.correction = false
    }

    override fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (packet is S12PacketEntityVelocity && packet.entityID == mc.thePlayer.entityId)
            yaw = BlockPos(mc.thePlayer).getRotations().yaw
    }

    fun BlockPos.getRotations(): Rotation {
        val (x, y, z) = Vec3(
            this.x - eyesPos.xCoord,
            this.y - eyesPos.yCoord, 
            this.z - eyesPos.zCoord
        )

        val dist = MathHelper.sqrt_double(x * x + z * z).toDouble()
        val yaw = (atan2(z, x) * 180.0 / 3.141592653589793).toFloat() - 90.0f
        val pitch = (-(atan2(y, dist) * 180.0 / 3.141592653589793)).toFloat()
        return Rotation(yaw, pitch)
    }

    val eyesPos: Vec3
        get() = Vec3(mc.thePlayer.posX, mc.thePlayer.entityBoundingBox.minY + mc.thePlayer.eyeHeight, mc.thePlayer.posZ)
}
