/*
 * MinusBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/MinusMC/MinusBounce
 */
package net.minusmc.minusbounce.features.module.modules.movement.noslows.intave

import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C0DPacketCloseWindow
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minusmc.minusbounce.event.PreMotionEvent
import net.minusmc.minusbounce.features.module.modules.movement.noslows.NoSlowMode
import net.minusmc.minusbounce.utils.MovementUtils
import net.minusmc.minusbounce.utils.PacketUtils

class IntaveMIX : NoSlowMode("IntaveMIX") {

    override fun onPreMotion(event: PreMotionEvent) {
        if(noslow.isEating) {
            if (mc.thePlayer.itemInUseDuration == 1 || mc.thePlayer.itemInUseDuration == 32) {
                PacketUtils.sendPacket(
                    C07PacketPlayerDigging(
                        C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                        BlockPos(mc.thePlayer),
                        EnumFacing.UP
                    )
                )
            }
        } else if (MovementUtils.isMoving && (mc.thePlayer.isUsingItem || mc.thePlayer.isBlocking)) {
            PacketUtils.sendPacketNoEvent(C0DPacketCloseWindow(mc.thePlayer.openContainer.windowId))
        }
    }
}