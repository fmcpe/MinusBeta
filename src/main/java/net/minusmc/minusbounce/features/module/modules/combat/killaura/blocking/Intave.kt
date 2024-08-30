package net.minusmc.minusbounce.features.module.modules.combat.killaura.blocking

import net.minusmc.minusbounce.features.module.modules.combat.killaura.KillAuraBlocking
import net.minusmc.minusbounce.utils.extensions.getDistanceToEntityBox

class Intave: KillAuraBlocking("IntaveTest") {
    override fun onPreAttack() {
        val target = mc.objectMouseOver.entityHit ?: return

        if(mc.thePlayer.getDistanceToEntityBox(target) <= killAura.autoBlockRangeValue.get()){
            killAura.startBlocking(true, true)
        }
    }
}