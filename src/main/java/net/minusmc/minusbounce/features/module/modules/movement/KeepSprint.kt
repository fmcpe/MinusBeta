/*
 * MinusBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/MinusMC/MinusBounce
 */
package net.minusmc.minusbounce.features.module.modules.movement

import net.minusmc.minusbounce.MinusBounce
import net.minusmc.minusbounce.event.EventTarget
import net.minusmc.minusbounce.event.KnockBackEvent
import net.minusmc.minusbounce.features.module.Module
import net.minusmc.minusbounce.features.module.ModuleCategory
import net.minusmc.minusbounce.features.module.ModuleInfo
import net.minusmc.minusbounce.features.module.modules.combat.Velocity
import net.minusmc.minusbounce.value.BoolValue

@ModuleInfo(name = "KeepSprint", spacedName = "Keep Sprint", description = "Keep you sprint", category = ModuleCategory.MOVEMENT)
class KeepSprint: Module(){
    private val zero = BoolValue("IsUsingZeroVelocity", false)
    private val hurtTime = BoolValue("OnlyHurtTime", false)

    @EventTarget
    fun onKB(event: KnockBackEvent){
        val velocity = MinusBounce.moduleManager.getModule(Velocity::class.java) ?: return
        if(mc.thePlayer.hurtTime <= 0 || (zero.get() && velocity.state)){
            event.cancelEvent()
        }

        if(hurtTime.get() && mc.thePlayer.hurtTime == 0){
            event.stopCancel()
        }
    }
}
