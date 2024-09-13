package net.fmcpe.viaforge.api

import net.fmcpe.viaforge.api.AnimationUtils.animate
import net.minusmc.minusbounce.event.EventTarget
import net.minusmc.minusbounce.event.Listenable
import net.minusmc.minusbounce.event.MotionEvent
import net.minusmc.minusbounce.utils.MinecraftInstance

class PacketManager : MinecraftInstance(), Listenable {
    @EventTarget
    fun onMotion(event: MotionEvent){
        mc.leftClickCounter = 0

        val START_HEIGHT = 1.62f

        lastEyeHeight = eyeHeight

        val END_HEIGHT = if (ProtocolFixer.newerThanOrEqualsTo1_9() && ProtocolFixer.olderThanOrEqualsTo1_13_2()) 1.47f
        else if (ProtocolFixer.newerThanOrEqualsTo1_14()) 1.32f
        else 1.54f
        val delta = if (ProtocolFixer.newerThanOrEqualsTo1_9() && ProtocolFixer.olderThanOrEqualsTo1_13_2()) 0.147f
        else if (ProtocolFixer.newerThanOrEqualsTo1_14()) 0.132f
        else 0.154f

        if (mc.thePlayer.isSneaking) eyeHeight = animate(END_HEIGHT, eyeHeight, 4 * delta)
        else if (eyeHeight < START_HEIGHT) eyeHeight = animate(START_HEIGHT, eyeHeight, 4 * delta)
    }

    override fun handleEvents(): Boolean {
        return true
    }

    companion object {
        @JvmField
        var eyeHeight: Float = 0f
        @JvmField
        var lastEyeHeight: Float = 0f
    }
}