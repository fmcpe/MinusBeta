/*
 * MinusBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/MinusMC/MinusBounce
 */
package net.minusmc.minusbounce.features.module.modules.movement.noslows.intave

import net.minecraft.item.ItemPotion
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minusmc.minusbounce.event.PreMotionEvent
import net.minusmc.minusbounce.features.module.modules.movement.noslows.NoSlowMode
import net.minusmc.minusbounce.utils.MovementUtils

class IntaveSwitch : NoSlowMode("IntaveSwitch") {
    private var lastUsingItem = false

    override fun onPreMotion(event: PreMotionEvent) {
        if (!mc.thePlayer.isUsingItem || noslow.heldItem == null) {
            lastUsingItem = false
            return
        }

        if(MovementUtils.isMoving){
            if(noslow.isEating){
                if(!lastUsingItem){
                    mc.netHandler.addToSendQueue(C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.UP))
                }
            } else {
                if (MovementUtils.isMoving && (mc.thePlayer.isUsingItem || mc.thePlayer.isEating || mc.thePlayer.isBlocking)) {
                    if(mc.thePlayer.heldItem.item !is ItemPotion && !ItemPotion.isSplash(mc.thePlayer.heldItem.metadata)){
                        if(mc.thePlayer.ticksExisted % 3 == 0 || mc.thePlayer.getItemInUseCount() < 3) {
                            mc.thePlayer.sendQueue.addToSendQueue(C09PacketHeldItemChange(if (mc.thePlayer.inventory.currentItem - 1 < 0) 8 else mc.thePlayer.inventory.currentItem - 1))
                            mc.thePlayer.sendQueue.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
                        }
                    }
                }
            }
        }

        lastUsingItem = true
    }

    override fun onDisable() {
        lastUsingItem = false
    }
}