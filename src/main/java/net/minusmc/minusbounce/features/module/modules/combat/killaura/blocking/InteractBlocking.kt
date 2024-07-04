package net.minusmc.minusbounce.features.module.modules.combat.killaura.blocking

import net.minecraft.util.MovingObjectPosition
import net.minusmc.minusbounce.features.module.modules.combat.killaura.KillAuraBlocking

class InteractBlocking: KillAuraBlocking("Interact") {
    override fun onPreAttack() {
        killAura.stopBlocking()
    }

    override fun onPostAttack() {
        if (mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
            mc.playerController.interactWithEntitySendPacket(mc.thePlayer, mc.objectMouseOver.entityHit)
        }
        killAura.startBlocking()
    }
}