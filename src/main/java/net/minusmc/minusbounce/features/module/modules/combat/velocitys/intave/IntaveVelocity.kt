package net.minusmc.minusbounce.features.module.modules.combat.velocitys.intave

import net.minecraft.util.MovingObjectPosition
import net.minusmc.minusbounce.event.AttackEvent
import net.minusmc.minusbounce.event.KnockBackEvent
import net.minusmc.minusbounce.event.MoveInputEvent
import net.minusmc.minusbounce.features.module.modules.combat.velocitys.VelocityMode

class IntaveVelocity : VelocityMode("Intave") {
    private var attacked = false
    private var limitUntilJump = 0

    override fun onTick() {
        mc.objectMouseOver ?: return
        mc.thePlayer ?: return
        mc.theWorld ?: return

        if(mc.thePlayer.hurtTime > 0 && !attacked && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY){
            mc.thePlayer.isSprinting = false
            mc.thePlayer.motionX *= 0.6
            mc.thePlayer.motionZ *= 0.6
        }

        attacked = false
    }

    override fun onAttack(event: AttackEvent) {
        attacked = true
    }

    override fun onInput(event: MoveInputEvent) {
        if (mc.thePlayer.hurtTime != 9 || !mc.thePlayer.onGround || !mc.thePlayer.isSprinting || limitUntilJump < 1) {
            limitUntilJump++
            return
        }

        if(mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
            event.forward = 1.0F
        }

        event.jump = true
        limitUntilJump = 0
    }

    override fun onKnockBack(event: KnockBackEvent) {
        event.full = false
        event.reduceY = true
    }
}