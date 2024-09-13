package net.minusmc.minusbounce.features.module.modules.combat.velocitys.grim

import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minecraft.util.MovingObjectPosition
import net.minusmc.minusbounce.event.PostVelocityEvent
import net.minusmc.minusbounce.event.PreMotionEvent
import net.minusmc.minusbounce.features.module.modules.combat.velocitys.VelocityMode
import net.minusmc.minusbounce.utils.ClientUtils
import net.minusmc.minusbounce.utils.MovementUtils
import net.minusmc.minusbounce.utils.PacketUtils.sendPacket
import net.minusmc.minusbounce.utils.PacketUtils.sendPacketNoEvent
import net.minusmc.minusbounce.value.BoolValue
import net.minusmc.minusbounce.value.IntegerValue


class GrimReduce : VelocityMode("GrimReduce") {
    private var reduceCountEveryTime = IntegerValue("Reduce-count-eveny-time", 4, 1, 10)
    private var reduceTimes = IntegerValue("Reduce-time", 1, 1, 5)
    private var onlyWhileMoving = BoolValue("OnlyMove", false)
    private var is112 = BoolValue("Via112", false)
    private var debug = BoolValue("Debug", false)

    private var unReduceTimes = 0
    
    override fun onPreMotion(event: PreMotionEvent) {
        if (
            unReduceTimes > 0 &&
            mc.thePlayer.hurtTime > 0 &&
            (!onlyWhileMoving.get() || MovementUtils.isMoving) &&
            mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY &&
            mc.objectMouseOver.entityHit is EntityLivingBase
        ) {
            if (event.sprint) {
                sendPacket(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING))
                doReduce()
                sendPacket(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING))
            } else {
                doReduce()
            }

            if (debug.get()) ClientUtils.displayChatMessage(
                String.format(
                    "%d Reduced %.3f %.3f",
                    reduceTimes.get() - unReduceTimes,
                    mc.thePlayer.motionX,
                    mc.thePlayer.motionZ
                )
            )
            unReduceTimes--
        } else {
            unReduceTimes = 0
        }
    }

    private fun doReduce() {
        for (i in 0 .. reduceCountEveryTime.get()) {
            if (is112.get()) {
                sendPacketNoEvent(C0APacketAnimation())
            }
            sendPacketNoEvent(C0APacketAnimation())
            sendPacketNoEvent(C02PacketUseEntity(mc.objectMouseOver.entityHit, C02PacketUseEntity.Action.ATTACK))
            mc.thePlayer.motionX *= 0.6
            mc.thePlayer.motionZ *= 0.6
        }
    }

    override fun onPostVelocity(event: PostVelocityEvent) {
        unReduceTimes = reduceTimes.get()
    }
}