package net.minusmc.minusbounce.features.module.modules.combat.killaura.blocking

import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minusmc.minusbounce.features.module.modules.combat.killaura.KillAuraBlocking
import net.minusmc.minusbounce.utils.PacketUtils
import net.minusmc.minusbounce.utils.timer.MSTimer

class VulcanBlocking: KillAuraBlocking("Vulcan") {
    private val blockTimer = MSTimer()

    override fun onPostAttack(){
        if (blockTimer.hasTimePassed(50)) {
            PacketUtils.sendPacketNoEvent(C08PacketPlayerBlockPlacement(mc.thePlayer.inventory.getCurrentItem()))
            blockTimer.reset()
        }
    }
}