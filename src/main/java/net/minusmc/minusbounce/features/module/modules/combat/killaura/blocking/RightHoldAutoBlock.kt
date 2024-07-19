package net.minusmc.minusbounce.features.module.modules.combat.killaura.blocking

import net.minusmc.minusbounce.features.module.modules.combat.killaura.KillAuraBlocking

class RightHoldBlocking: KillAuraBlocking("Legit") {
    override fun onPreUpdate() {
        if(!killAura.blockingStatus){
            mc.gameSettings.keyBindUseItem.pressed = true
            blockingStatus = true
        }
    }

    override fun onDisable() {
    	mc.gameSettings.keyBindUseItem.pressed = false
    }
}