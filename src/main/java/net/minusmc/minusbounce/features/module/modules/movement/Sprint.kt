/*
 * MinusBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/MinusMC/MinusBounce
 */
package net.minusmc.minusbounce.features.module.modules.movement

import net.minecraft.client.settings.GameSettings
import net.minusmc.minusbounce.MinusBounce
import net.minusmc.minusbounce.event.EventTarget
import net.minusmc.minusbounce.event.StrafeEvent
import net.minusmc.minusbounce.features.module.Module
import net.minusmc.minusbounce.features.module.ModuleCategory
import net.minusmc.minusbounce.features.module.ModuleInfo
import net.minusmc.minusbounce.features.module.modules.combat.SuperKnockback
import net.minusmc.minusbounce.features.module.modules.world.Scaffold


@ModuleInfo(name = "Sprint", description = "Automatically sprints all the time.", category = ModuleCategory.MOVEMENT)
class Sprint : Module(){
    @EventTarget(priority = -5)
    fun onStrafe(event: StrafeEvent){
        MinusBounce.moduleManager[SuperKnockback::class.java]?.let {
            if (it.stopSprint && it.stopTimer.hasTimePassed(it.delay / 2 + 50)) {
                it.stopSprint = false
            }
        }

        val scaffold = MinusBounce.moduleManager.getModule(Scaffold::class.java) ?: return
        if(!scaffold.state) {
            mc.gameSettings.keyBindSprint.pressed = true
        }

        if(MinusBounce.moduleManager[SuperKnockback::class.java]?.stopSprint == true || mc.thePlayer.reSprint == 2){
            mc.thePlayer.isSprinting = false
            mc.gameSettings.keyBindSprint.pressed = false
        }
    }

    override fun onDisable() {
        mc.gameSettings.keyBindSprint.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindSprint)
    }
}