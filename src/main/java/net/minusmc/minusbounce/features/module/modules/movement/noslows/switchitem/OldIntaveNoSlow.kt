package net.minusmc.minusbounce.features.module.modules.movement.noslows.other

import net.minecraft.item.ItemBow
import net.minecraft.item.ItemFood
import net.minecraft.item.ItemSword
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minusmc.minusbounce.event.PreMotionEvent
import net.minusmc.minusbounce.features.module.modules.movement.noslows.NoSlowMode
import net.minusmc.minusbounce.utils.BlinkUtils
import net.minusmc.minusbounce.utils.PacketUtils


class Intave: NoSlowMode("Intave") {
    private var usingItem: Boolean = false

    override fun onPreMotion(event: PreMotionEvent) {
        val item = mc.thePlayer.currentEquippedItem.item

        if (mc.thePlayer.isUsingItem) {
            if (item is ItemSword) {
                BlinkUtils.setBlinkState(all = true)

                if (mc.thePlayer.ticksExisted % 5 == 0) {
                    PacketUtils.sendPacketNoEvent(
                        C07PacketPlayerDigging(
                            C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                            BlockPos.ORIGIN,
                            EnumFacing.DOWN
                        )
                    )
                    BlinkUtils.setBlinkState(release = true)
                    mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer.getCurrentEquippedItem()))
                }
            } else if (item is ItemFood || item is ItemBow) {
                BlinkUtils.setBlinkState(all = true)
            }

            usingItem = true
        } else if (usingItem) {
            usingItem = false

            BlinkUtils.setBlinkState(off = true)
        }
    }
}