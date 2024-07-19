package net.minusmc.minusbounce.features.module.modules.combat.killaura.blocking

import net.minusmc.minusbounce.features.module.modules.combat.killaura.KillAuraBlocking

class VanillaBlocking: KillAuraBlocking("Vanilla") {
    override fun onPostAttack(){
        if (killAura.hitTicks != 0) {
            killAura.startBlocking(false, true)
        }
    }
}