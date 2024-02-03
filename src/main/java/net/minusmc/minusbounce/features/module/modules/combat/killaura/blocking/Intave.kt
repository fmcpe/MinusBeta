package net.minusmc.minusbounce.features.module.modules.combat.killaura.blocking

import net.minusmc.minusbounce.features.module.modules.combat.killaura.KillAuraBlocking
import net.minusmc.minusbounce.utils.PacketUtils
import net.minusmc.minusbounce.event.PacketEvent
import net.minecraft.network.play.client.C03PacketPlayer

class Intave: KillAuraBlocking("Intave") {
    override fun onPreAttack() {
        killAura.stopBlocking()
    }

    override fun onPostAttack() {
        killAura.startBlocking()
    }

}