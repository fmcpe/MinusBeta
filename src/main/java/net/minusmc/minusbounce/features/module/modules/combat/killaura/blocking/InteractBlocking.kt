package net.minusmc.minusbounce.features.module.modules.combat.killaura.blocking

import net.minecraft.util.MovingObjectPosition
import net.minusmc.minusbounce.features.module.modules.combat.killaura.KillAuraBlocking
import net.minusmc.minusbounce.utils.timer.MSTimer

class InteractBlocking: KillAuraBlocking("Interact") {

    private val blockTimer = MSTimer()

    override fun onPreAttack() {
        if (blockTimer.hasTimePassed(50)) {
            if (mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
                mc.playerController.interactWithEntitySendPacket(mc.thePlayer, mc.objectMouseOver.entityHit)
            }
            blockTimer.reset()
        }
    }
}