package net.minusmc.minusbounce.features.module.modules.movement.noslows.other

import net.minecraft.item.ItemFood
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minusmc.minusbounce.event.EventState
import net.minusmc.minusbounce.event.PacketEvent
import net.minusmc.minusbounce.event.SlowDownEvent
import net.minusmc.minusbounce.features.module.modules.movement.noslows.NoSlowMode
import net.minusmc.minusbounce.utils.PacketUtils.sendPacketNoEvent


class Grim : NoSlowMode("Grim") {
    override fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (canFoodNoSlow() && event.eventType == EventState.SEND) {
            if (packet is C08PacketPlayerBlockPlacement) {
                event.cancelEvent()
                sendPacketNoEvent(packet)
                sendPacketNoEvent(
                    C07PacketPlayerDigging(
                        C07PacketPlayerDigging.Action.DROP_ITEM,
                        BlockPos.ORIGIN,
                        EnumFacing.DOWN
                    )
                )
                mc.gameSettings.keyBindUseItem.pressed = false
            } else if (packet is C07PacketPlayerDigging) {
                event.isCancelled = packet.status == C07PacketPlayerDigging.Action.RELEASE_USE_ITEM
            }
        }
    }

    private fun canFoodNoSlow(): Boolean {
        val item = mc.thePlayer.heldItem
        return item != null && item.item is ItemFood && item.stackSize > 1
    }

    override fun onSlowDown(event: SlowDownEvent) {
        val multiply = if(canFoodNoSlow()) 0.2F else 1F
        event.forward = multiply
        event.strafe = multiply
    }
}