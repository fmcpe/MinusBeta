package net.minusmc.minusbounce.features.module.modules.movement.speeds.grim

import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minusmc.minusbounce.event.PreMotionEvent
import net.minusmc.minusbounce.features.module.modules.movement.speeds.SpeedMode
import net.minusmc.minusbounce.features.module.modules.movement.speeds.SpeedType
import net.minusmc.minusbounce.utils.MovementUtils
import net.minusmc.minusbounce.value.BoolValue
import net.minusmc.minusbounce.value.IntegerValue
import kotlin.math.cos
import kotlin.math.sin


class GrimCollide: SpeedMode("GrimCollide", SpeedType.GRIM) {

    private val jump = BoolValue("Jump", false)
    private val boostSpeed = IntegerValue("BoostSpeed", 4, 0, 10)

    override fun onPreMotion(event: PreMotionEvent) {
        if (!MovementUtils.isMoving)
            return

        mc.thePlayer.movementInput.jump = mc.thePlayer.onGround && jump.get()

        var collisions = 0
        val grimPlayerBox = mc.thePlayer.entityBoundingBox.expand(1.0, 1.0, 1.0)
        for (entity in mc.theWorld.loadedEntityList) {
            if (canCauseSpeed(entity) && (grimPlayerBox.intersectsWith(entity.entityBoundingBox))) {
                collisions += 1
            }
        }
        val yaw = Math.toRadians(moveYaw())
        val boost = boostSpeed.get() / 100 * collisions
        mc.thePlayer.addVelocity(-sin(yaw) * boost, 0.0, cos(yaw) * boost)
    }

    private fun canCauseSpeed(entity: Entity): Boolean {
        return entity != mc.thePlayer && entity is EntityLivingBase
    }

    private fun moveYaw(): Double {
        return (MovementUtils.direction() * 180f / Math.PI)
    }
}