package net.minusmc.minusbounce.features.module.modules.combat.killaura.blocking

import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minusmc.minusbounce.event.PreUpdateEvent
import net.minusmc.minusbounce.features.module.modules.combat.killaura.KillAuraBlocking

class VulcanBlocking: KillAuraBlocking("VulcanTest") {
    private var canBlock = false
    override fun onPreUpdate(event: PreUpdateEvent) {
        if(canBlock){
            val slot = mc.thePlayer.inventory.currentItem
            mc.netHandler.addToSendQueue(C09PacketHeldItemChange(slot % 8 + 1))
            mc.netHandler.addToSendQueue(C09PacketHeldItemChange(slot % 7 + 2))
            mc.netHandler.addToSendQueue(C09PacketHeldItemChange(slot))
            killAura.startBlocking(true, true)
        }

        canBlock = false
    }

    override fun onPostAttack() {
        canBlock = true
    }
}