package net.minusmc.minusbounce.features.module.modules.world

import net.minusmc.minusbounce.event.EventTarget
import net.minusmc.minusbounce.event.UpdateEvent
import net.minusmc.minusbounce.features.module.Module
import net.minusmc.minusbounce.features.module.ModuleCategory
import net.minusmc.minusbounce.features.module.ModuleInfo
import net.minusmc.minusbounce.utils.ClientUtils
import net.minusmc.minusbounce.utils.Rotation
import net.minusmc.minusbounce.utils.RotationUtils
import net.minusmc.minusbounce.utils.extensions.minus

@ModuleInfo("Test", "", "", ModuleCategory.WORLD)
class Test: Module(){
    @EventTarget
    fun onUpdate(e: UpdateEvent){
        RotationUtils.setRotations(rotation= Rotation(135f, 75f), silent = false, speed = 180f)
        ClientUtils.displayChatMessage("${mc.objectMouseOver.hitVec - mc.objectMouseOver.blockPos}")
    }
}