package net.minusmc.minusbounce.features.module.modules.combat.killaura.blocking

import net.minusmc.minusbounce.features.module.modules.combat.killaura.KillAuraBlocking

class VanillaBlocking: KillAuraBlocking("Vanilla") {
    override fun onPostAttack(){
        mc.rightClickMouse()
        blockingStatus = true
    }
}