package net.minusmc.minusbounce.features.module.modules.combat.killaura.blocking

import net.minusmc.minusbounce.features.module.modules.combat.killaura.KillAuraBlocking
import net.minusmc.minusbounce.utils.timer.MSTimer

class VulcanBlocking: KillAuraBlocking("Vulcan") {
    private val blockTimer = MSTimer()

    override fun onPreAttack() {
        if (blockTimer.hasTimePassed(50)) {
            killAura.startBlocking()
            blockTimer.reset()
        }
    }
}