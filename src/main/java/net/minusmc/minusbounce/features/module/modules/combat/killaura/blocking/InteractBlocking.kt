package net.minusmc.minusbounce.features.module.modules.combat.killaura.blocking

import net.minecraft.util.MovingObjectPosition
import net.minusmc.minusbounce.features.module.modules.combat.killaura.KillAuraBlocking

class InteractBlocking: KillAuraBlocking("Interact") {
    override fun onPostAttack() {
        var flag = true
        if (mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
            if (mc.playerController.isPlayerRightClickingOnEntity(
                    mc.thePlayer,
                    mc.objectMouseOver.entityHit,
                    mc.objectMouseOver
                )
            ) {
                flag = false
            } else if (mc.playerController.interactWithEntitySendPacket(
                    mc.thePlayer,
                    mc.objectMouseOver.entityHit
                )
            ) {
                flag = false
            }
        }
        if (flag) {
            if (mc.playerController.sendUseItem(
                    mc.thePlayer,
                    mc.theWorld,
                    mc.thePlayer.inventory.getCurrentItem()
                )
            ) {
                mc.entityRenderer.itemRenderer.resetEquippedProgress2()
            }
        }
    }
}