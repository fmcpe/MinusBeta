/*
 * MinusBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/MinusMC/MinusBounce
 */
package net.minusmc.minusbounce.features.module.modules.combat

import net.minusmc.minusbounce.MinusBounce
import net.minusmc.minusbounce.event.EventTarget
import net.minusmc.minusbounce.event.PostMotionEvent
import net.minusmc.minusbounce.event.Render2DEvent
import net.minusmc.minusbounce.features.module.Module
import net.minusmc.minusbounce.features.module.ModuleCategory
import net.minusmc.minusbounce.features.module.ModuleInfo
import net.minusmc.minusbounce.utils.extensions.getDistanceToEntityBox
import net.minusmc.minusbounce.value.FloatValue
import net.minusmc.minusbounce.value.IntegerValue

@ModuleInfo(name = "TickBase", description = "Tick Base", category = ModuleCategory.COMBAT)
class TickBase : Module() {
    private var counter = -1
    var freezing = false

    private val ticks = IntegerValue("Ticks", 3, 1, 10)
    private val range = FloatValue("Range", 3.5F, 0.5F, 8.0F)

    override fun onEnable() {
        counter = -1
        freezing = false
    }

    fun getExtraTicks(): Int {
        MinusBounce.moduleManager[KillAura::class.java]?.let{
            val target = it.target ?: return@let

            if(it.state && mc.thePlayer.getDistanceToEntityBox(target) < range.get().toDouble()) return@let
            if(counter-- > 0) return -1 else freezing = false

            if(it.state && mc.thePlayer.getDistanceToEntityBox(target) == range.get().toDouble()) {
                counter = ticks.get()
                return counter
            }
        }

        return 0
    }

    @EventTarget
    fun onPostMotion(event: PostMotionEvent) {
        if (freezing) {
            mc.thePlayer.posX = mc.thePlayer.lastTickPosX
            mc.thePlayer.posY = mc.thePlayer.lastTickPosY
            mc.thePlayer.posZ = mc.thePlayer.lastTickPosZ
        }
    }

    @EventTarget
    fun onRender(event: Render2DEvent) {
        if (freezing) mc.timer.renderPartialTicks = 0F
    }
}