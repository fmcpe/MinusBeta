/*
 * MinusBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/MinusMC/MinusBounce
 */
package net.minusmc.minusbounce.features.module.modules.movement.noslows.intave

import net.minecraft.network.play.client.C0DPacketCloseWindow
import net.minusmc.minusbounce.event.PreMotionEvent
import net.minusmc.minusbounce.features.module.modules.movement.noslows.NoSlowMode
import net.minusmc.minusbounce.utils.MovementUtils
import net.minusmc.minusbounce.utils.PacketUtils

class IntaveNew : NoSlowMode("IntaveNew") {

    override fun onPreMotion(event: PreMotionEvent) {
        if (MovementUtils.isMoving && (mc.thePlayer.isUsingItem || mc.thePlayer.isEating || mc.thePlayer.isBlocking)) {
            PacketUtils.sendPacketNoEvent(C0DPacketCloseWindow(mc.thePlayer.openContainer.windowId))
        }
    }
}